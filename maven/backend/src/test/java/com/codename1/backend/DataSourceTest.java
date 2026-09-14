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
package com.codename1.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.codename1.backend.sql.Dialect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pool, over the engine that needs no server.
 *
 * <p>What is engine-specific here is deliberately small: the pool opens
 * connections from a URL and hands them out, and the only thing it knows about
 * SQLite is that an in-memory database cannot be pooled. The wire engines are
 * exercised end to end by vm/backend's dbcheck demo under vm/tests, which runs
 * against real servers.
 */
class DataSourceTest {

    @Test
    @DisplayName("an in-memory database is pooled at one, because a second would be a second database")
    void memoryPoolsAtOne() throws Exception {
        DataSource pool = DataSource.open(":memory:");
        try {
            assertEquals(1, pool.getMaxSize());
            assertSame(Dialect.SQLITE, pool.dialect());
            pool.execute("CREATE TABLE t (a INTEGER)", null);
            pool.execute("INSERT INTO t (a) VALUES (?)", new Object[] {Long.valueOf(7)});
            // The SAME database on the next borrow, which is the whole reason the
            // size is forced: each connection to ":memory:" gets its own.
            List rows = pool.query("SELECT a FROM t", null);
            assertEquals(1, rows.size());
            assertEquals(Long.valueOf(7), ((Map)rows.get(0)).get("a"));
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a file database gets several connections")
    void fileGetsSeveralConnections(@TempDir File dir) throws Exception {
        String path = new File(dir, "app.db").getAbsolutePath();
        DataSource pool = DataSource.open(path);
        try {
            assertTrue(pool.getMaxSize() > 1, "a file database should pool");
            Database first = pool.borrow();
            Database second = pool.borrow();
            assertNotSame(first, second);
            pool.release(second);
            pool.release(first);
            assertEquals(2, pool.getOpenCount());
            assertEquals(2, pool.getIdleCount());
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a borrow that cannot be served fails rather than hanging forever")
    void borrowTimesOut() throws Exception {
        // A handler blocked forever on an empty pool is a request that never
        // answers and a host thread that never runs another. A refusal is a 500,
        // which is a thing an operator can see.
        DataSource pool = DataSource.open(":memory:", 1, 0, 150);
        try {
            Database held = pool.borrow();
            IOException err = assertThrows(IOException.class, () -> pool.borrow());
            assertTrue(err.getMessage().contains("became free"), err.getMessage());
            pool.release(held);
            // And the pool still works once the connection comes back.
            pool.release(pool.borrow());
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a pool of more than one over an in-memory database is refused")
    void refusesAPooledMemoryDatabase() {
        // Each connection to ":memory:" gets its OWN database, so a second one is
        // not more capacity: it is a second empty database, and which one a
        // request sees depends on borrow order. Rows written through the first
        // connection are simply missing, or the table is. Refused rather than
        // clamped in silence, because the setting says something the deployment
        // believes.
        IOException err = assertThrows(IOException.class, () -> DataSource.open(":memory:", 4));
        assertTrue(err.getMessage().contains("cannot be pooled"), err.getMessage());
        assertTrue(err.getMessage().contains(Config.DATASOURCE_POOL_SIZE), err.getMessage());
    }

    @Test
    @DisplayName("refusing an in-memory pool opens no connection to leak")
    void refusesBeforeOpeningAnything() throws Exception {
        // The refusal used to come after the first connection was made, so a
        // process that catches configuration errors and retries leaked a native
        // SQLite handle per attempt. The URL alone answers the question, so
        // there is nothing to open in order to ask it.
        for(int iter = 0 ; iter < 50 ; iter++) {
            assertThrows(IOException.class, () -> DataSource.open(":memory:", 2));
        }
        // And the legal configuration still works afterwards.
        DataSource pool = DataSource.open(":memory:");
        try {
            assertEquals(1, pool.getMaxSize());
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a file whose name contains mode=memory is a file")
    void doesNotMistakeAPathForAnInMemoryDatabase(@TempDir File dir) throws Exception {
        // The refusal matched "mode=memory" anywhere in the URL, so an ordinary
        // path -- and any server URL whose password or database name happened to
        // contain that text -- could not be pooled at all. It is a SQLite URI
        // PARAMETER, and only ever a question about SQLite.
        String path = new File(dir, "mode=memory.db").getAbsolutePath();
        DataSource pool = DataSource.open(path, 2);
        try {
            assertEquals(2, pool.getMaxSize());
            pool.execute("CREATE TABLE t (a INTEGER)", null);
        } finally {
            pool.close();
        }
        // And the real thing is still refused, spelled either way.
        assertThrows(IOException.class, () -> DataSource.open(":memory:", 2));
        assertThrows(IOException.class,
                () -> DataSource.open("file:app?mode=memory&cache=shared", 2));
    }

    @Test
    @DisplayName("dropping a dead connection wakes a borrower waiting for capacity")
    void discardingWakesAWaiter() throws Exception {
        // The pool is full and every connection is out, so a borrower waits. The
        // holder then finds its connection dead and releases it: that creates
        // capacity without putting anything in the idle list, and it is the one
        // path that used not to notify. With borrowTimeoutMillis at the
        // documented 0 -- wait forever -- the waiter never woke at all.
        final DataSource pool = DataSource.open(":memory:", 1, 0, 0);
        try {
            Database held = pool.borrow();
            final Database[] got = new Database[1];
            final Throwable[] failed = new Throwable[1];
            final java.util.concurrent.CountDownLatch waiting =
                    new java.util.concurrent.CountDownLatch(1);
            final java.util.concurrent.CountDownLatch done =
                    new java.util.concurrent.CountDownLatch(1);
            Thread waiter = new Thread(new Runnable() {
                public void run() {
                    waiting.countDown();
                    try {
                        got[0] = pool.borrow();
                    } catch (Throwable err) {
                        failed[0] = err;
                    } finally {
                        done.countDown();
                    }
                }
            });
            waiter.start();
            assertTrue(waiting.await(5, java.util.concurrent.TimeUnit.SECONDS));
            // Long enough that the waiter is parked rather than about to park.
            Thread.sleep(200);
            held.close();                  // as a connection the server hung up on
            pool.release(held);
            assertTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS),
                    "the waiter was never woken when the dead connection was dropped");
            assertNull(failed[0], String.valueOf(failed[0]));
            assertNotNull(got[0]);
            assertTrue(got[0].isOpen());
            pool.release(got[0]);
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a closed pool hands out nothing, and takes back what it lent")
    void closedPoolRefuses() throws Exception {
        DataSource pool = DataSource.open(":memory:");
        Database borrowed = pool.borrow();
        pool.close();
        assertThrows(IOException.class, () -> pool.borrow());
        // The borrower was still running when the pool shut down; its release
        // must close the connection rather than repopulate a dead pool.
        pool.release(borrowed);
        assertFalse(borrowed.isOpen());
        assertEquals(0, pool.getIdleCount());
    }

    @Test
    @DisplayName("a connection that died is replaced rather than handed out again")
    void replacesADeadConnection(@TempDir File dir) throws Exception {
        String path = new File(dir, "app.db").getAbsolutePath();
        DataSource pool = DataSource.open(path, 2, 5000, 1000);
        try {
            Database first = pool.borrow();
            first.close();                 // as an error that cannot resynchronize would
            pool.release(first);
            Database next = pool.borrow();
            assertNotSame(first, next, "a closed connection must not come back out");
            assertTrue(next.isOpen());
            pool.release(next);
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a connection somebody else opened is not this pool's to close")
    void doesNotCloseABorrowedOwner() throws Exception {
        Database db = Database.open(":memory:");
        try {
            DataSource pool = DataSource.of(db);
            assertSame(db, pool.borrow());
            pool.release(db);
            pool.close();
            assertTrue(db.isOpen(), "of() wraps a connection it does not own");
        } finally {
            db.close();
        }
    }

    @Test
    @DisplayName("the configuration decides the database, and refuses to invent one in production")
    void fromConfiguration() throws Exception {
        Properties dev = new Properties();
        DataSource pool = DataSource.fromConfig(Config.of(dev, "dev"));
        try {
            assertSame(Dialect.SQLITE, pool.dialect());
        } finally {
            pool.close();
        }
        // Defaulting silently anywhere else is how a service comes up healthy,
        // writes everything into a database inside its own process, and loses it
        // at the next deploy.
        IOException err = assertThrows(IOException.class,
                () -> DataSource.fromConfig(Config.of(new Properties(), "production")));
        assertTrue(err.getMessage().contains(Config.DATASOURCE_URL), err.getMessage());
        assertTrue(err.getMessage().contains("production"), err.getMessage());
    }

    @Test
    @DisplayName("an insert that inserted nothing answers no key")
    void anIgnoredInsertHasNoKey() throws Exception {
        // last_insert_rowid() answers for the CONNECTION, not the statement, so
        // after a conflict that was ignored it still holds the previous row's
        // id. Reported as this insert's key, it makes the caller write another
        // row's id into the object it believes it just stored.
        DataSource pool = DataSource.open(":memory:");
        try {
            pool.execute("CREATE TABLE t (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT UNIQUE)", null);
            long first = pool.insert("INSERT INTO t (name) VALUES (?)",
                    new Object[] {"once"}, "id");
            assertTrue(first > 0);
            long ignored = pool.insert("INSERT OR IGNORE INTO t (name) VALUES (?)",
                    new Object[] {"once"}, "id");
            assertEquals(0L, ignored, "the conflicting insert reported an earlier row's key");
            assertEquals(1, pool.query("SELECT id FROM t", null).size());
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a pool describes itself without its password")
    void describesWithoutTheUrl() throws Exception {
        DataSource pool = DataSource.open(":memory:");
        try {
            assertTrue(pool.toString().contains("sqlite"), pool.toString());
            assertTrue(pool.toString().contains("pool of 1"), pool.toString());
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a transaction rolls back on the connection it opened")
    void transactionsRollBack(@TempDir File dir) throws Exception {
        String path = new File(dir, "app.db").getAbsolutePath();
        DataSource pool = DataSource.open(path, 2, 5000, 2000);
        try {
            pool.execute("CREATE TABLE t (a INTEGER)", null);
            assertThrows(IllegalStateException.class, () -> pool.inTransaction(new DataSource.Work() {
                public Object run(Database db) throws Exception {
                    db.execute("INSERT INTO t (a) VALUES (?)", new Object[] {Long.valueOf(1)});
                    throw new IllegalStateException("no");
                }
            }));
            assertEquals(0, pool.query("SELECT a FROM t", null).size());
            pool.inTransaction(new DataSource.Work() {
                public Object run(Database db) throws Exception {
                    db.execute("INSERT INTO t (a) VALUES (?)", new Object[] {Long.valueOf(2)});
                    return null;
                }
            });
            assertEquals(1, pool.query("SELECT a FROM t", null).size());
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("an insert answers with the key the database generated")
    void insertAnswersTheGeneratedKey() throws Exception {
        DataSource pool = DataSource.open(":memory:");
        try {
            pool.execute("CREATE TABLE t (id INTEGER PRIMARY KEY AUTOINCREMENT, a TEXT)", null);
            long first = pool.insert("INSERT INTO t (a) VALUES (?)", new Object[] {"x"}, "id");
            long second = pool.insert("INSERT INTO t (a) VALUES (?)", new Object[] {"y"}, "id");
            assertEquals(1L, first);
            assertEquals(2L, second);
        } finally {
            pool.close();
        }
    }
}
