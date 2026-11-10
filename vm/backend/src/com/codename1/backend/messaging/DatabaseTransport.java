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
import com.codename1.backend.sql.Dialect;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/// Queues in the application's own database: `cn1.messaging.url=db:`, and
/// the default whenever the server has a data source. Needs nothing but the
/// database the application already has, and a message sent inside a
/// transaction is part of it -- it is seen only if the transaction commits, and
/// never seen if it rolls back.
///
/// Every message is a row of `cn1_messages`, created on first use. A consumer
/// claims a row by moving its `available_at` past a lease
/// (`cn1.messaging.lease`, 60 seconds), handles it, and deletes it; a
/// consumer that dies mid-message leaves the row to come back when the lease
/// runs out. The claim is a compare-and-set on that column, so it is safe on
/// every engine; PostgreSQL claims a batch in one statement with
/// `FOR UPDATE SKIP LOCKED`.
///
/// Consumers poll every `cn1.messaging.db.poll` milliseconds (500) while their
/// destination is empty, and at once after a send from the same process.
/// Times are the processes' own clocks, so replicas should run NTP.
final class DatabaseTransport extends PollingTransport {
    /// The table, unquoted so every engine folds it the same way.
    static final String TABLE = "cn1_messages";

    private final DataSource db;
    private final long lease;
    private final long pollInterval;
    private final Object wake = new Object();
    private final AtomicLong claims = new AtomicLong();
    /// Sends from this process, so a waiting consumer knows one came.
    private final AtomicLong sends = new AtomicLong();
    private final String node;
    private boolean created;

    DatabaseTransport(DataSource db, RetryPolicy retry, long leaseMillis, long pollMillis) {
        super(retry);
        this.db = db;
        this.lease = Math.max(1000, leaseMillis);
        this.pollInterval = Math.max(10, pollMillis);
        this.node = Long.toString(System.currentTimeMillis(), 36) + "-"
                + Integer.toString(System.identityHashCode(this), 36);
    }

    @Override
    public String getName() {
        return "db";
    }

    @Override
    public boolean isTransactional() {
        return true;
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
                + ", attempt " + d.columnType(Dialect.INTEGER) + " NOT NULL"
                + ", available_at " + d.columnType(Dialect.BIGINT) + " NOT NULL"
                + ", claim VARCHAR(64))", new Object[0]);
        try {
            db.execute("CREATE INDEX cn1_messages_ready ON " + TABLE
                    + " (destination, available_at)", new Object[0]);
        } catch (IOException exists) { // NOPMD EmptyCatchBlock - the index is there already
        }
        synchronized (this) {
            created = true;
        }
    }

    @Override
    public void send(Message message) throws IOException {
        ensureTable();
        Map<String, String> headers = new LinkedHashMap<String, String>(
                message.headersForTransport());
        int attempt = message.getAttempt();
        headers.remove(Message.ATTEMPT);
        long availableAt = System.currentTimeMillis();
        String nb = headers.remove(Message.NOT_BEFORE);
        if (nb != null) {
            try {
                availableAt = Math.max(availableAt, Long.parseLong(nb));
            } catch (NumberFormatException ignored) { // NOPMD EmptyCatchBlock - sent now
            }
        }
        long id = db.insert("INSERT INTO " + TABLE + " (destination, msg_key, headers, payload,"
                + " attempt, available_at) VALUES (?, ?, ?, ?, ?, ?)",
                new Object[]{message.getDestination(), message.getKey(),
                    headers.isEmpty() ? null : Json.write(headers),
                    Base64.encode(message.getPayload()), Integer.valueOf(attempt),
                    Long.valueOf(availableAt)}, "id");
        message.setId(Long.toString(id));
        sends.incrementAndGet();
        synchronized (wake) {
            wake.notifyAll();
        }
    }

    @Override
    protected List<Delivery> poll(String destination, String group, int max, long waitMillis)
            throws IOException {
        ensureTable();
        long deadline = System.currentTimeMillis() + waitMillis;
        while (true) {
            long seen = sends.get();
            List<Delivery> got = claim(destination, max);
            long now = System.currentTimeMillis();
            if (!got.isEmpty() || now >= deadline) {
                return got;
            }
            synchronized (wake) {
                try {
                    if (sends.get() == seen) {
                        wake.wait(Math.max(1, Math.min(pollInterval, deadline - now)));
                    }
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                    return got;
                }
            }
        }
    }

    private List<Delivery> claim(String destination, int max) throws IOException {
        long now = System.currentTimeMillis();
        String token = node + "-" + claims.incrementAndGet();
        Long until = Long.valueOf(now + lease);
        List rows;
        if (Dialect.POSTGRES.getName().equals(db.dialect().getName())) {
            rows = db.query("UPDATE " + TABLE + " SET available_at = ?, claim = ? WHERE id IN"
                    + " (SELECT id FROM " + TABLE + " WHERE destination = ? AND available_at <= ?"
                    + " ORDER BY id LIMIT " + max + " FOR UPDATE SKIP LOCKED)"
                    + " RETURNING id, msg_key, headers, payload, attempt",
                    new Object[]{until, token, destination, Long.valueOf(now)});
        } else {
            List candidates = db.query("SELECT id FROM " + TABLE
                    + " WHERE destination = ? AND available_at <= ? ORDER BY id "
                    + db.dialect().limit(max * 4, 0),
                    new Object[]{destination, Long.valueOf(now)});
            List<Object> mine = new ArrayList<Object>();
            for (Iterator it = candidates.iterator(); it.hasNext() && mine.size() < max;) {
                Object id = ((Map) it.next()).get("id");
                if (db.execute("UPDATE " + TABLE + " SET available_at = ?, claim = ?"
                        + " WHERE id = ? AND available_at <= ?",
                        new Object[]{until, token, id, Long.valueOf(now)}) == 1) {
                    mine.add(id);
                }
            }
            rows = new ArrayList();
            for (Object id : mine) {
                Map row = db.queryOne("SELECT id, msg_key, headers, payload, attempt FROM "
                        + TABLE + " WHERE id = ? AND claim = ?", new Object[]{id, token});
                if (row != null) {
                    rows.add(row);
                }
            }
        }
        List<Delivery> out = new ArrayList<Delivery>(rows.size());
        for (Object o : rows) {
            Map row = (Map) o;
            out.add(delivery(destination, row, token));
        }
        return out;
    }

    private static Delivery delivery(String destination, Map row, String token)
            throws IOException {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        Object h = row.get("headers");
        if (h instanceof String) {
            Map parsed = Json.parseObject((String) h);
            for (Object e : parsed.entrySet()) {
                Map.Entry entry = (Map.Entry) e;
                if (entry.getValue() != null) {
                    headers.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }
        Object attempt = row.get("attempt");
        headers.put(Message.ATTEMPT, attempt == null ? "1" : String.valueOf(
                attempt instanceof Number ? ((Number) attempt).intValue() : 1));
        Object payload = row.get("payload");
        Object key = row.get("msg_key");
        Message m = new Message(destination, key instanceof String ? (String) key : null,
                payload instanceof String ? Base64.decode((String) payload) : new byte[0],
                headers);
        Object id = row.get("id");
        m.setId(String.valueOf(id));
        return new Delivery(m, new Object[]{id, token});
    }

    @Override
    protected void ack(String destination, String group, Delivery delivery) throws IOException {
        Object[] h = (Object[]) delivery.handle;
        db.execute("DELETE FROM " + TABLE + " WHERE id = ? AND claim = ?", new Object[]{h[0], h[1]});
    }

    @Override
    protected void redeliver(String destination, String group, Delivery delivery, int attempt,
                             long notBefore) throws IOException {
        Object[] h = (Object[]) delivery.handle;
        db.execute("UPDATE " + TABLE + " SET attempt = ?, available_at = ?, claim = NULL"
                + " WHERE id = ? AND claim = ?",
                new Object[]{Integer.valueOf(attempt), Long.valueOf(notBefore), h[0], h[1]});
    }

    @Override
    protected void deadLetter(String destination, String group, Delivery delivery, String error)
            throws IOException {
        Object[] h = (Object[]) delivery.handle;
        Map<String, String> headers = new LinkedHashMap<String, String>(
                delivery.message.headersForTransport());
        headers.remove(Message.ATTEMPT);
        headers.put(Message.ERROR, error);
        headers.put(Message.ORIGIN, destination);
        // One statement moves the row, so a crash cannot leave the message both
        // in its queue and dead-lettered.
        db.execute("UPDATE " + TABLE + " SET destination = ?, headers = ?, attempt = 1,"
                + " available_at = ?, claim = NULL WHERE id = ? AND claim = ?",
                new Object[]{RetryPolicy.deadLetter(destination), Json.write(headers),
                    Long.valueOf(System.currentTimeMillis()), h[0], h[1]});
    }

    /// Messages waiting in `destination`, ready or not.
    int depth(String destination) throws IOException {
        ensureTable();
        Map row = db.queryOne("SELECT COUNT(*) AS n FROM " + TABLE + " WHERE destination = ?",
                new Object[]{destination});
        Object n = row == null ? null : row.get("n");
        return n instanceof Number ? ((Number) n).intValue() : 0;
    }

    @Override
    public boolean isHealthy() {
        try {
            ensureTable();
            return true;
        } catch (IOException err) {
            return false;
        }
    }

    @Override
    public void close() {
        stopSubscriptions();
    }
}
