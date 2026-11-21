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
package com.codename1.db;

import com.codename1.db.sql.Dialect;
import com.codename1.db.sql.ServerSession;
import com.codename1.db.sql.TransactionIsolation;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ServerDatabase over a scripted session: what it binds, how it reads rows
 * back, and what it refuses before it connects.
 */
class ServerDatabaseTest extends UITestBase {

    /** Records what ServerDatabase sends and answers with fixed rows. */
    static final class StubSession extends ServerSession {
        final List<String> sent = new ArrayList<String>();
        final List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        boolean closed;

        @Override public Dialect dialect() { return Dialect.POSTGRES; }
        @Override public int execute(String sql, Object[] params) { sent.add(sql); return 3; }
        @Override public List query(String sql, Object[] params) { sent.add(sql); return rows; }
        @Override public boolean ownsTransactionControl() { return false; }
        @Override public void begin() { sent.add("BEGIN"); }
        @Override public void beginReadOnly() { sent.add("BEGIN READ ONLY"); }
        @Override public void begin(boolean readOnly, TransactionIsolation isolation) { sent.add("BEGIN " + isolation); }
        @Override public void commit() { sent.add("COMMIT"); }
        @Override public void rollback() { sent.add("ROLLBACK"); }
        @Override public void savepoint(String verb, String name) { sent.add(verb + " " + name); }
        @Override public long lastInsertId() { return 0; }
        @Override public void close() { closed = true; }
        @Override public boolean isClosed() { return closed; }
    }

    static final class StubConnector extends ServerSession.Connector {
        final StubSession session = new StubSession();
        String sslMode;

        @Override public String engine() { return "postgres"; }
        @Override public ServerSession connect(String host, int port, String database, String user,
                                               String password, String sslMode, String caFile,
                                               int timeoutMillis, int socketTimeoutMillis) {
            this.sslMode = sslMode;
            return session;
        }
    }

    static final class StubDatabase extends ServerDatabase {
        StubDatabase(StubConnector c, String sslMode) throws IOException {
            super(c, "h", 1, "d", "u", "p", sslMode, 1000);
        }
    }

    private static <T> T offEdt(final Callable<T> work) throws Exception {
        final Object[] out = new Object[1];
        final Exception[] err = new Exception[1];
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    out[0] = work.call();
                } catch (Exception e) {
                    err[0] = e;
                }
            }
        });
        t.start();
        t.join(10000);
        if (err[0] != null) {
            throw err[0];
        }
        @SuppressWarnings("unchecked") T r = (T) out[0];
        return r;
    }

    interface Callable<T> {
        T call() throws Exception;
    }

    @FormTest
    void refusesTheEdt() {
        IOException e = assertThrows(IOException.class,
                () -> new StubDatabase(new StubConnector(), null));
        assertTrue(e.getMessage().contains("EDT"), e.getMessage());
    }

    @Test
    void tlsModesAreRefusedAndPreferConnectsPlain() throws Exception {
        final StubConnector c = new StubConnector();
        offEdt(() -> new StubDatabase(c, "prefer"));
        assertEquals("disable", c.sslMode);
        offEdt(() -> new StubDatabase(c, null));
        assertEquals("disable", c.sslMode);
        for (final String mode : new String[] {"require", "verify-ca", "verify-full", "strict"}) {
            final StubConnector refused = new StubConnector();
            IOException e = assertThrows(IOException.class, () -> offEdt(() -> new StubDatabase(refused, mode)));
            assertTrue(e.getMessage().contains("demands TLS"), e.getMessage());
            assertNull(refused.sslMode, "never connected");
        }
    }

    @Test
    void bindsPortablePlaceholdersAndReadsRows() throws Exception {
        final StubConnector c = new StubConnector();
        Map<String, Object> r1 = new LinkedHashMap<String, Object>();
        r1.put("id", Long.valueOf(7));
        r1.put("name", "seven");
        r1.put("price", Double.valueOf(2.5));
        r1.put("photo", null);
        Map<String, Object> r2 = new LinkedHashMap<String, Object>();
        r2.put("id", Long.valueOf(8));
        r2.put("name", null);
        r2.put("price", "3.75");
        r2.put("photo", new byte[] {1, 2});
        c.session.rows.add(r1);
        c.session.rows.add(r2);
        offEdt(() -> {
            StubDatabase db = new StubDatabase(c, "disable");
            assertEquals(3, db.executeUpdate("UPDATE t SET a = ? WHERE b = ?", "x", Long.valueOf(1)));
            Cursor cur = db.executeQuery("SELECT * FROM t WHERE a = ?", "x");
            assertEquals(4, cur.getColumnCount());
            assertEquals("name", cur.getColumnName(1));
            assertEquals(2, cur.getColumnIndex("PRICE"));
            assertTrue(cur.next());
            Row row = cur.getRow();
            assertEquals(7L, row.getLong(0));
            assertEquals("seven", row.getString(1));
            assertEquals(2.5, row.getDouble(2), 0);
            assertNull(row.getBlob(3));
            assertTrue(((RowExt) row).wasNull());
            assertEquals(Database.COLUMN_NULL, row.getColumnType(3));
            assertTrue(cur.next());
            row = cur.getRow();
            assertNull(row.getString(1));
            assertEquals(3.75, row.getDouble(2), 0);
            assertEquals(Database.COLUMN_BLOB, row.getColumnType(3));
            assertFalse(cur.next());
            assertTrue(cur.prev());
            assertEquals(2, ((CursorExt) cur).getCount());
            cur.close();
            assertThrows(IOException.class, cur::next);
            assertThrows(IOException.class, () -> db.execute("INSERT INTO t VALUES (?)", new Object()));
            db.beginTransaction();
            assertTrue(db.isInTransaction());
            db.rollbackTransaction();
            assertFalse(db.isInTransaction());
            db.close();
            assertTrue(db.isClosed());
            return null;
        });
        assertEquals("UPDATE t SET a = $1 WHERE b = $2", c.session.sent.get(0));
        assertEquals("SELECT * FROM t WHERE a = $1", c.session.sent.get(1));
        assertEquals("BEGIN", c.session.sent.get(2));
        assertEquals("ROLLBACK", c.session.sent.get(3));
    }
}
