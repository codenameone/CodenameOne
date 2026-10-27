/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.io.outbox;

import com.codename1.impl.orm.Attribute;
import com.codename1.impl.orm.SessionImpl;
import com.codename1.impl.orm.SqlAccess;
import com.codename1.orm.session.PersistenceException;
import com.codename1.orm.session.Session;
import java.io.IOException;
import java.util.List;

/// A durable queue of outgoing messages, kept in the application's own database,
/// for a device whose link to the server comes and goes.
///
/// The device records what it has to say with [#enqueue(String, byte[])] and
/// sends it with [#flush(Transport)] whenever the link might be up. Messages
/// survive a restart or a power cut, leave in the order they were enqueued, and
/// are removed only once the transport reports them delivered.
///
/// The table lives in the database of the [Session] the outbox was opened on,
/// and an `enqueue` inside one of that session's transactions belongs to the
/// transaction: a reading saved through the ORM and the message announcing it
/// commit together or not at all. That is the point of keeping the queue there
/// rather than in a file of its own.
///
/// ```java {class="cn1-no-try"}
/// Outbox outbox = Outbox.open(session, "readings@" + deviceId);
/// session.beginTransaction();
/// session.persist(reading);
/// outbox.enqueue("/v1/readings", body);
/// session.commitTransaction();
///
/// // Later, from a timer or when the network comes back. postToServer is the
/// // application's own call: Rest on a client, Web on the backend.
/// outbox.flush(message -> {
///     int status = postToServer(message.getTopic(), message.getKey(), message.getPayload());
///     return status < 300 ? Outbox.DELIVERED : status < 500 ? Outbox.REJECTED : Outbox.RETRY;
/// });
/// ```
///
/// Delivery is at least once. A message is removed after the transport answers
/// [#DELIVERED]; if the answer never arrives the same message, with the same
/// [OutboxMessage#getKey()], is sent again, and the receiver drops the copy.
/// Several devices feeding one server should put their identity in the outbox
/// name, which is the first half of the key.
///
/// Like its session, an outbox is used from one thread at a time. The outbox
/// works on the client and on the backend alike.
@com.codename1.impl.SharedWithBackend
public final class Outbox {
    /// The receiver has the message; it is removed.
    public static final int DELIVERED = 0;
    /// Try again later. The message stays at the head, and the flush stops so
    /// that nothing overtakes it.
    public static final int RETRY = 1;
    /// The receiver will never take this message; it is dropped and counted in
    /// [#getRejected()], and the flush moves on to the next one.
    public static final int REJECTED = 2;

    /// Sends one message and says what became of it.
    public interface Transport {
        /// Delivers a message.
        ///
        /// @param message the message at the head of the outbox
        /// @return [#DELIVERED], [#RETRY] or [#REJECTED]
        /// @throws IOException when the link failed, which is read as [#RETRY]
        int deliver(OutboxMessage message) throws IOException;
    }

    private static final String TABLE = "cn1_outbox";
    private static final int[] HEAD_KINDS = {Attribute.BIGINT, Attribute.TEXT, Attribute.BLOB, Attribute.INTEGER,
        Attribute.BIGINT, Attribute.BIGINT};
    private static final int[] COUNT_KINDS = {Attribute.BIGINT};

    private final Session session;
    private final SqlAccess sql;
    private final String name;
    private final String table;
    private long minBackoff = 1000L;
    private long maxBackoff = 300000L;
    private int maxAttempts;
    private int capacity;
    private boolean dropOldest;
    private long rejected;

    private Outbox(Session session, SqlAccess sql, String name) {
        this.session = session;
        this.sql = sql;
        this.name = name;
        this.table = sql.quote(TABLE);
    }

    /// Opens an outbox, creating its table on first use. Outboxes of different
    /// names share the table and never see each other's messages.
    ///
    /// @param session a session from `EntityManager.openSession()`
    /// @param name the outbox name, which also starts every message key
    /// @return the outbox
    /// @throws PersistenceException if the table cannot be created
    public static Outbox open(Session session, String name) {
        if (!(session instanceof SessionImpl)) {
            throw new IllegalArgumentException("An outbox needs a session from EntityManager.openSession()");
        }
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("An outbox needs a name");
        }
        Outbox outbox = new Outbox(session, ((SessionImpl) session).sqlAccess(), name);
        outbox.createTable();
        return outbox;
    }

    private String q(String column) {
        return sql.quote(column);
    }

    private void createTable() {
        try {
            if (!sql.describe(TABLE).isEmpty()) {
                return;
            }
            sql.execute("CREATE TABLE " + table + " ("
                    + q("id") + " " + sql.generatedKeyColumn(Attribute.BIGINT, q("id")) + ", "
                    + q("box") + " " + sql.columnType(Attribute.TEXT) + " NOT NULL, "
                    + q("topic") + " " + sql.columnType(Attribute.TEXT) + " NOT NULL, "
                    + q("payload") + " " + sql.columnType(Attribute.BLOB) + " NOT NULL, "
                    + q("attempts") + " " + sql.columnType(Attribute.INTEGER) + " NOT NULL, "
                    + q("created") + " " + sql.columnType(Attribute.BIGINT) + " NOT NULL, "
                    + q("due") + " " + sql.columnType(Attribute.BIGINT) + " NOT NULL)", new Object[0]);
        } catch (IOException e) {
            // Another outbox (another process, on a server) may have created it
            // between the two statements; only a table that is still missing is
            // a failure.
            try {
                if (!sql.describe(TABLE).isEmpty()) {
                    return;
                }
            } catch (IOException ignored) {
                // Reported below with the first error.
            }
            throw new PersistenceException("Cannot create the outbox table: " + e.getMessage(), e);
        }
    }

    /// Sets how long a message waits after a failed delivery: `min` after the
    /// first, doubling with each further failure up to `max`. The defaults are
    /// one second and five minutes.
    ///
    /// @param min the first delay in milliseconds, 0 or more
    /// @param max the longest delay in milliseconds, at least `min`
    public void setBackoff(long min, long max) {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("Backoff needs 0 <= min <= max");
        }
        minBackoff = min;
        maxBackoff = max;
    }

    /// Sets how many deliveries a message gets before it is treated as
    /// [#REJECTED]. 0, the default, retries forever.
    ///
    /// @param attempts the limit, or 0 for none
    public void setMaxAttempts(int attempts) {
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts < 0");
        }
        maxAttempts = attempts;
    }

    /// Bounds the outbox. When it is full, `dropOldest` makes room by discarding
    /// the oldest messages, which suits telemetry where the newest reading
    /// matters most; otherwise [#enqueue(String, byte[])] refuses the message.
    /// 0, the default, is unbounded.
    ///
    /// @param messages the most messages kept, or 0 for no bound
    /// @param dropOldest discard the oldest instead of refusing the newest
    public void setCapacity(int messages, boolean dropOldest) {
        if (messages < 0) {
            throw new IllegalArgumentException("messages < 0");
        }
        capacity = messages;
        this.dropOldest = dropOldest;
    }

    /// Adds a message to the end of the outbox. Inside a transaction of the
    /// outbox's session the message commits or rolls back with it; otherwise it
    /// is committed before this returns.
    ///
    /// @param topic what the transport routes on, never null
    /// @param payload the message body, copied
    /// @return the message as it was stored
    /// @throws PersistenceException if it cannot be stored, or the outbox is full
    /// and set to refuse
    public OutboxMessage enqueue(String topic, byte[] payload) {
        if (topic == null || payload == null) {
            throw new IllegalArgumentException("topic/payload is null");
        }
        byte[] copy = new byte[payload.length];
        System.arraycopy(payload, 0, copy, 0, payload.length);
        boolean own = !session.isTransactionActive();
        if (own) {
            session.beginTransaction();
        }
        boolean committed = false;
        try {
            if (capacity > 0) {
                makeRoom();
            }
            long now = System.currentTimeMillis();
            long id = sql.insert("INSERT INTO " + table + " (" + q("box") + ", " + q("topic") + ", " + q("payload")
                    + ", " + q("attempts") + ", " + q("created") + ", " + q("due") + ") VALUES (?, ?, ?, ?, ?, ?)",
                    new Object[]{name, topic, copy, Integer.valueOf(0), Long.valueOf(now), Long.valueOf(now)}, "id");
            if (own) {
                session.commitTransaction();
            }
            committed = true;
            return new OutboxMessage(id, name + "/" + id, topic, copy, 0, now);
        } catch (IOException e) {
            throw new PersistenceException("Outbox " + name + ": " + e.getMessage(), e);
        } finally {
            if (own && !committed && session.isTransactionActive()) {
                session.rollbackTransaction();
            }
        }
    }

    private void makeRoom() throws IOException {
        long count = countRows();
        if (count < capacity) {
            return;
        }
        if (!dropOldest) {
            throw new PersistenceException("Outbox " + name + " is full (" + capacity + " messages)");
        }
        // The id of the newest message that has to go; everything up to it is dropped.
        List<Object[]> rows = sql.query("SELECT " + q("id") + " FROM " + table + " WHERE " + q("box") + " = ? ORDER BY "
                + q("id") + sql.limit(1, (int) (count - capacity)), new Object[]{name}, COUNT_KINDS);
        if (!rows.isEmpty()) {
            rejected += sql.execute("DELETE FROM " + table + " WHERE " + q("box") + " = ? AND " + q("id") + " <= ?",
                    new Object[]{name, Long.valueOf(number(rows.get(0)[0]))});
        }
    }

    private long countRows() throws IOException {
        List<Object[]> rows = sql.query("SELECT COUNT(*) FROM " + table + " WHERE " + q("box") + " = ?",
                new Object[]{name}, COUNT_KINDS);
        return rows.isEmpty() ? 0 : number(rows.get(0)[0]);
    }

    /// The number of messages waiting.
    ///
    /// @return the count
    public int size() {
        try {
            return (int) countRows();
        } catch (IOException e) {
            throw new PersistenceException("Outbox " + name + ": " + e.getMessage(), e);
        }
    }

    /// When the head of the outbox may next be sent, in milliseconds since the
    /// epoch, so a device can sleep until then; -1 when the outbox is empty.
    ///
    /// @return the time, or -1
    public long nextAttemptTime() {
        try {
            Object[] head = head();
            return head == null ? -1 : number(head[5]);
        } catch (IOException e) {
            throw new PersistenceException("Outbox " + name + ": " + e.getMessage(), e);
        }
    }

    /// Messages discarded so far by this outbox object: rejected by the
    /// transport, out of attempts, or dropped to make room.
    ///
    /// @return the count since [#open(Session, String)]
    public long getRejected() {
        return rejected;
    }

    private Object[] head() throws IOException {
        List<Object[]> rows = sql.query("SELECT " + q("id") + ", " + q("topic") + ", " + q("payload") + ", "
                + q("attempts") + ", " + q("created") + ", " + q("due") + " FROM " + table + " WHERE " + q("box")
                + " = ? ORDER BY " + q("id") + sql.limit(1, 0), new Object[]{name}, HEAD_KINDS);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /// Sends what is due, oldest first, until the outbox is empty, the head is
    /// waiting out a backoff, or a delivery has to be retried. Call it whenever
    /// the link may be up; a flush that cannot send costs one query.
    ///
    /// Not inside a transaction: each delivery is network I/O, and each removal
    /// commits by itself so that a crash mid-flush repeats at most one message.
    ///
    /// @param transport sends one message
    /// @return how many messages were delivered
    /// @throws IllegalStateException inside a transaction of the session
    public int flush(Transport transport) {
        if (session.isTransactionActive()) {
            throw new IllegalStateException("Outbox.flush runs outside a transaction");
        }
        int delivered = 0;
        try {
            for (;;) {
                Object[] head = head();
                if (head == null || number(head[5]) > System.currentTimeMillis()) {
                    return delivered;
                }
                long id = number(head[0]);
                int attempts = (int) number(head[3]);
                OutboxMessage message = new OutboxMessage(id, name + "/" + id,
                        head[1] instanceof String ? (String) head[1] : String.valueOf(head[1]),
                        head[2] instanceof byte[] ? (byte[]) head[2] : new byte[0], attempts, number(head[4]));
                int result;
                try {
                    result = transport.deliver(message);
                } catch (IOException e) {
                    result = RETRY;
                }
                if (result == DELIVERED) {
                    remove(id);
                    delivered++;
                } else if (result == REJECTED || maxAttempts > 0 && attempts + 1 >= maxAttempts) {
                    remove(id);
                    rejected++;
                } else {
                    sql.execute("UPDATE " + table + " SET " + q("attempts") + " = ?, " + q("due") + " = ? WHERE "
                            + q("id") + " = ?", new Object[]{Integer.valueOf(attempts + 1),
                                Long.valueOf(System.currentTimeMillis() + backoff(attempts)), Long.valueOf(id)});
                    return delivered;
                }
            }
        } catch (IOException e) {
            throw new PersistenceException("Outbox " + name + ": " + e.getMessage(), e);
        }
    }

    private void remove(long id) throws IOException {
        sql.execute("DELETE FROM " + table + " WHERE " + q("id") + " = ?", new Object[]{Long.valueOf(id)});
    }

    private long backoff(int failures) {
        long delay = minBackoff;
        for (int i = 0; i < failures && delay < maxBackoff; i++) {
            delay = delay == 0 ? 0 : delay * 2;
        }
        return delay < maxBackoff ? delay : maxBackoff;
    }

    private static long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
