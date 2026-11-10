/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend.messaging;

import com.codename1.backend.Base64;
import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.Transactions;
import com.codename1.backend.sql.Dialect;
import com.codename1.impl.backend.BackendAccess;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/// A broker transport with a transactional outbox in front of it.
///
/// A send inside a database transaction is written to `cn1_outbox` in that
/// transaction and relayed to the broker once it commits -- the transactional
/// outbox pattern, so a rollback sends nothing and a commit never loses the
/// message. A row whose relay did not happen (the process died between the
/// commit and the send) is sent by the sweep every server runs, after
/// `cn1.messaging.outbox.sweep` milliseconds (10000); a message can therefore
/// be sent twice, never zero times, which at-least-once delivery allows.
final class OutboxTransport extends MessageTransport {
    static final String TABLE = "cn1_outbox";

    private final MessageTransport broker;
    private final DataSource db;
    private final long sweepAfter;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Object pause = new Object();
    private boolean created;
    private Thread sweeper;

    OutboxTransport(MessageTransport broker, DataSource db) {
        this(broker, db, 10000);
    }

    OutboxTransport(MessageTransport broker, DataSource db, long sweepAfterMillis) {
        this.broker = broker;
        this.db = db;
        this.sweepAfter = Math.max(100, sweepAfterMillis);
    }

    @Override
    public String getName() {
        return broker.getName();
    }

    @Override
    public void send(Message message) throws IOException {
        broker.send(message);
    }

    @Override
    public Subscription subscribe(String destination, String group, int concurrency,
                                  MessageHandler handler) throws IOException {
        startSweeper();
        return broker.subscribe(destination, group, concurrency, handler);
    }

    @Override
    public boolean isHealthy() {
        return broker.isHealthy();
    }

    private void ensureTable() throws IOException {
        synchronized (this) {
            if (created) {
                return;
            }
        }
        Dialect d = db.dialect();
        db.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " (id "
                + d.generatedKeyColumn(Dialect.BIGINT)
                + ", destination VARCHAR(255) NOT NULL, msg_key VARCHAR(255), headers "
                + d.columnType(Dialect.TEXT) + ", payload " + d.columnType(Dialect.TEXT)
                + ", created_at " + d.columnType(Dialect.BIGINT) + " NOT NULL)", new Object[0]);
        synchronized (this) {
            created = true;
        }
    }

    /// Writes `message` to the outbox in the calling thread's transaction and
    /// relays it once that commits.
    void stage(final Message message) throws IOException {
        ensureTable();
        startSweeper();
        final long id = db.insert("INSERT INTO " + TABLE + " (destination, msg_key, headers,"
                + " payload, created_at) VALUES (?, ?, ?, ?, ?)",
                new Object[]{message.getDestination(), message.getKey(),
                    Json.write(message.headersForTransport()),
                    Base64.encode(message.getPayload()),
                    Long.valueOf(System.currentTimeMillis())}, "id");
        Transactions.registerSynchronization(new Transactions.Synchronization() {
            @Override
            public void afterCommit() {
                relay(Long.valueOf(id), message);
            }
        });
    }

    /// Sends one outbox row and deletes it. Claims it first, so the sweep of
    /// another process and this commit cannot both send it.
    private boolean relay(Object id, Message message) {
        try {
            if (db.execute("DELETE FROM " + TABLE + " WHERE id = ?", new Object[]{id}) != 1) {
                return false;
            }
        } catch (IOException err) {
            System.err.println("cn1: the outbox could not be updated; the sweep sends the "
                    + "message later: " + err.getMessage());
            return false;
        }
        try {
            broker.send(message);
            return true;
        } catch (IOException err) {
            // The row is gone, so put the message back for the sweep.
            try {
                db.execute("INSERT INTO " + TABLE + " (destination, msg_key, headers, payload,"
                        + " created_at) VALUES (?, ?, ?, ?, ?)",
                        new Object[]{message.getDestination(), message.getKey(),
                            Json.write(message.headersForTransport()),
                            Base64.encode(message.getPayload()), Long.valueOf(0)});
            } catch (IOException again) {
                System.err.println("cn1: a message to " + message.getDestination()
                        + " was lost: the broker refused it (" + err.getMessage()
                        + ") and the outbox could not keep it (" + again.getMessage() + ")");
            }
            return false;
        }
    }

    /// Relays every row older than the sweep age; answers how many it sent.
    int sweep() throws IOException {
        ensureTable();
        List rows = db.query("SELECT id, destination, msg_key, headers, payload FROM " + TABLE
                + " WHERE created_at <= ? ORDER BY id " + db.dialect().limit(100, 0),
                new Object[]{Long.valueOf(System.currentTimeMillis() - sweepAfter)});
        int sent = 0;
        for (Object o : rows) {
            Map row = (Map) o;
            Map<String, String> headers = new LinkedHashMap<String, String>();
            Object h = row.get("headers");
            if (h instanceof String) {
                for (Object e : Json.parseObject((String) h).entrySet()) {
                    Map.Entry entry = (Map.Entry) e;
                    if (entry.getValue() != null) {
                        headers.put(String.valueOf(entry.getKey()),
                                String.valueOf(entry.getValue()));
                    }
                }
            }
            Object key = row.get("msg_key");
            Object payload = row.get("payload");
            Message m = new Message(String.valueOf(row.get("destination")),
                    key instanceof String ? (String) key : null,
                    payload instanceof String ? Base64.decode((String) payload) : new byte[0],
                    headers);
            if (relay(row.get("id"), m)) {
                sent++;
            }
        }
        return sent;
    }

    private void startSweeper() {
        synchronized (this) {
            if (sweeper != null || !running.get()) {
                return;
            }
            sweeper = new Thread(BackendAccess.get().inServer(new Runnable() {
                @Override
                public void run() {
                    sweepLoop();
                }
            }), "cn1-outbox");
            sweeper.setDaemon(true);
            sweeper.start();
        }
    }

    private void sweepLoop() {
        while (running.get()) {
            try {
                sweep();
            } catch (IOException err) {
                System.err.println("cn1: the outbox sweep failed: " + err.getMessage());
            }
            synchronized (pause) {
                try {
                    if (running.get()) {
                        pause.wait(sweepAfter);
                    }
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        synchronized (pause) {
            pause.notifyAll();
        }
        broker.close();
    }
}
