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

import com.codename1.backend.annotations.Isolation;
import com.codename1.backend.annotations.Propagation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TransactionSynchronizationTest {
    @TempDir File dir;

    private DataSource pool() throws IOException {
        DataSource pool = DataSource.open(new File(dir, "sync.db").getAbsolutePath(), 1, 1000, 10000);
        pool.execute("CREATE TABLE IF NOT EXISTS t (v INTEGER)", null);
        return pool;
    }

    private Transactions.Synchronization hook(final List<String> events, final String name) {
        return new Transactions.Synchronization() {
            public void beforeCommit(boolean readOnly) { events.add(name + ":before:" + readOnly); }
            public void afterCommit() { events.add(name + ":commit"); }
            public void afterCompletion(Transactions.Outcome outcome) { events.add(name + ":" + outcome); }
        };
    }

    @Test void orderedHooksAndResourcesWithoutDatabase() throws Exception {
        final List<String> events = new ArrayList<String>();
        final Object[] cleanup = new Object[2];
        Transactions.execute(Propagation.REQUIRED, true, -1, Isolation.DEFAULT, () -> {
            Transactions.bindResource("key", "value");
            assertThrows(TransactionException.IllegalState.class, () -> Transactions.bindResource("key", "other"));
            Transactions.registerSynchronization(hook(events, "a"));
            Transactions.registerSynchronization(hook(events, "b"));
            Transactions.registerSynchronization(new Transactions.Synchronization() {
                public void afterCompletion(Transactions.Outcome outcome) {
                    cleanup[0] = Transactions.getResource("key");
                    cleanup[1] = Transactions.unbindResource("key");
                }
            });
            return null;
        });
        assertArrayEquals(new Object[] {"value", "value"}, cleanup);
        assertEquals(Arrays.asList("a:before:true", "b:before:true", "a:commit", "b:commit",
                "a:COMMITTED", "b:COMMITTED"), events);
        assertNull(Transactions.getResource("key"));
        assertFalse(Transactions.isActive());
        assertThrows(TransactionException.IllegalState.class, () -> Transactions.bindResource("x", "y"));
    }

    @Test void requiresNewSuspendsAndRestoresCallbacksAndResources() throws Exception {
        final List<String> events = new ArrayList<String>();
        Transactions.execute(() -> {
            Transactions.bindResource("key", "outer");
            Transactions.registerSynchronization(hook(events, "outer"));
            Transactions.execute(Propagation.REQUIRES_NEW, false, -1, Isolation.SERIALIZABLE, () -> {
                assertNull(Transactions.getResource("key"));
                Transactions.bindResource("key", "inner");
                Transactions.registerSynchronization(hook(events, "inner"));
                return null;
            });
            assertEquals("outer", Transactions.getResource("key"));
            Transactions.setRollbackOnly();
            return null;
        });
        assertEquals(Arrays.asList("inner:before:false", "inner:commit", "inner:COMMITTED", "outer:ROLLED_BACK"), events);
        assertNull(Transactions.getResource("key"));
    }

    @Test void nestedRollbackDiscardsCommitHooksButCompletesOnceAtPhysicalEnd() throws Exception {
        final DataSource pool = pool();
        final List<String> events = new ArrayList<String>();
        try {
            Transactions.execute(() -> {
                Transactions.registerSynchronization(hook(events, "outer"));
                pool.execute("INSERT INTO t VALUES (1)", null);
                assertThrows(IOException.class, () -> Transactions.execute(Propagation.NESTED, false, -1, Isolation.DEFAULT, () -> {
                    Transactions.bindResource("nested", "retained");
                    Transactions.registerSynchronization(hook(events, "nested"));
                    pool.execute("INSERT INTO t VALUES (2)", null);
                    throw new IOException("undo savepoint");
                }));
                assertTrue(events.isEmpty());
                assertEquals("retained", Transactions.getResource("nested"));
                return null;
            });
            assertEquals(Arrays.asList("outer:before:false", "outer:commit", "outer:COMMITTED", "nested:COMMITTED"), events);
            assertEquals(1, ((Number) pool.queryOne("SELECT COUNT(*) AS n FROM t", null).get("n")).intValue());
        } finally { pool.close(); }
    }

    @Test void lazySavepointRollbackAlsoDiscardsCommitHooks() throws Exception {
        final List<String> events = new ArrayList<String>();
        Transactions.execute(() -> {
            Transactions.execute(Propagation.NESTED, false, -1, Isolation.DEFAULT, () -> {
                Transactions.registerSynchronization(hook(events, "nested"));
                Transactions.setRollbackOnly();
                return null;
            });
            return null;
        });
        assertEquals(Arrays.asList("nested:COMMITTED"), events);
    }

    @Test void beforeCommitFailureRollsBackAndCleansResources() throws Exception {
        final DataSource pool = pool();
        final List<String> events = new ArrayList<String>();
        try {
            assertThrows(TransactionException.class, () -> Transactions.execute(() -> {
                pool.execute("INSERT INTO t VALUES (1)", null);
                Transactions.bindResource("key", "value");
                Transactions.registerSynchronization(new Transactions.Synchronization() {
                    public void beforeCommit(boolean readOnly) { throw new IllegalArgumentException("before"); }
                });
                Transactions.registerSynchronization(hook(events, "later"));
                return null;
            }));
            assertEquals(Arrays.asList("later:ROLLED_BACK"), events);
            assertNull(Transactions.getResource("key"));
            assertFalse(Transactions.isActive());
            assertEquals(0, ((Number) pool.queryOne("SELECT COUNT(*) AS n FROM t", null).get("n")).intValue());
        } finally { pool.close(); }
    }

    @Test void afterCommitAndCompletionErrorsCannotPreventLaterCleanup() throws Exception {
        final List<String> events = new ArrayList<String>();
        TransactionException failure = assertThrows(TransactionException.class, () -> Transactions.execute(() -> {
            Transactions.registerSynchronization(new Transactions.Synchronization() {
                public void afterCommit() { throw new AssertionError("after"); }
                public void afterCompletion(Transactions.Outcome outcome) { throw new AssertionError("cleanup"); }
            });
            Transactions.registerSynchronization(hook(events, "later"));
            Transactions.bindResource("key", "value");
            return null;
        }));
        assertTrue(failure.getMessage().contains("committed"));
        assertEquals(Arrays.asList("later:before:false", "later:commit", "later:COMMITTED"), events);
        assertNull(Transactions.getResource("key"));
        assertFalse(Transactions.isActive());
    }

    @Test void commitFailureNeverRunsAfterCommitAndDiscardsConnection() throws Exception {
        final DataSource pool = pool();
        final List<String> events = new ArrayList<String>();
        try {
            pool.execute("CREATE TABLE parent (id INTEGER PRIMARY KEY)", null);
            pool.execute("CREATE TABLE child (id INTEGER REFERENCES parent(id) DEFERRABLE INITIALLY DEFERRED)", null);
            final Database[] borrowed = new Database[1];
            assertThrows(TransactionException.class, () -> Transactions.execute(() -> {
                borrowed[0] = pool.borrow();
                pool.execute("INSERT INTO child VALUES (5)", null);
                Transactions.registerSynchronization(hook(events, "tx"));
                return null;
            }));
            assertEquals(Arrays.asList("tx:before:false", "tx:UNKNOWN"), events);
            Database next = pool.borrow();
            assertNotSame(borrowed[0], next);
            assertFalse(next.isInTransaction());
            pool.release(next);
            assertEquals(0, ((Number) pool.queryOne("SELECT COUNT(*) AS n FROM child", null).get("n")).intValue());
        } finally { pool.close(); }
    }

    @Test void explicitIsolationRejectsIncompatibleJoinsAndNonTransactions() throws Exception {
        Transactions.execute(Propagation.REQUIRED, false, -1, Isolation.SERIALIZABLE, () -> {
            Transactions.execute(Propagation.REQUIRED, false, -1, Isolation.SERIALIZABLE, () -> null);
            for (final Propagation propagation : new Propagation[] {Propagation.REQUIRED, Propagation.NESTED, Propagation.SUPPORTS}) {
                assertThrows(TransactionException.IllegalState.class, () -> Transactions.execute(propagation,
                        false, -1, Isolation.READ_COMMITTED, () -> fail("body must not run")));
            }
            return null;
        });
        assertThrows(TransactionException.IllegalState.class, () -> Transactions.execute(Propagation.SUPPORTS,
                false, -1, Isolation.SERIALIZABLE, () -> null));
        Transactions.execute(() -> {
            assertThrows(TransactionException.IllegalState.class, () -> Transactions.execute(Propagation.REQUIRED,
                    false, -1, Isolation.SERIALIZABLE, () -> null));
            return null;
        });
    }

    @Test void sqliteIsolationAndPoolReuseAfterCommitRollbackAndUnsupportedRequest() throws Exception {
        final DataSource pool = pool();
        try {
            Database first = pool.borrow();
            pool.release(first);
            pool.inTransaction(Isolation.SERIALIZABLE, db -> { db.execute("INSERT INTO t VALUES (1)", null); return null; });
            assertThrows(IOException.class, () -> pool.inTransaction(Isolation.SERIALIZABLE, db -> { throw new IOException("rollback"); }));
            assertThrows(IOException.class, () -> pool.inTransaction(Isolation.READ_COMMITTED, db -> fail("unsupported")));
            Database next = pool.borrow();
            assertSame(first, next);
            assertFalse(next.isInTransaction());
            assertEquals(0, ((Number) next.queryOne("PRAGMA read_uncommitted", null).get("read_uncommitted")).intValue());
            next.execute("PRAGMA read_uncommitted = 1", null);
            assertThrows(IOException.class, () -> next.beginTransaction(false, Isolation.SERIALIZABLE));
            assertEquals(1, ((Number) next.queryOne("PRAGMA read_uncommitted", null).get("read_uncommitted")).intValue());
            pool.release(next);
        } finally { pool.close(); }
    }

    @Test void afterCommitRequiresNewCanUseSingleConnectionPool() throws Exception {
        final DataSource pool = pool();
        try {
            Transactions.execute(() -> {
                pool.execute("INSERT INTO t VALUES (1)", null);
                Transactions.registerSynchronization(new Transactions.Synchronization() {
                    public void afterCommit() {
                        assertThrows(TransactionException.IllegalState.class, () -> pool.query("SELECT * FROM t", null));
                        try {
                            Transactions.execute(Propagation.REQUIRES_NEW, false, -1, Isolation.DEFAULT, () -> {
                                pool.execute("INSERT INTO t VALUES (2)", null);
                                return null;
                            });
                        } catch (Exception e) { throw new RuntimeException(e); }
                    }
                });
                return null;
            });
            assertEquals(2, ((Number) pool.queryOne("SELECT COUNT(*) AS n FROM t", null).get("n")).intValue());
        } finally { pool.close(); }
    }

    @Test void beforeCommitCanWriteButCannotRegisterMoreHooks() throws Exception {
        final DataSource pool = pool();
        try {
            Transactions.execute(() -> {
                Transactions.registerSynchronization(new Transactions.Synchronization() {
                    public void beforeCommit(boolean readOnly) {
                        assertThrows(TransactionException.IllegalState.class, () -> Transactions.registerSynchronization(this));
                        try { pool.execute("INSERT INTO t VALUES (1)", null); }
                        catch (IOException e) { throw new RuntimeException(e); }
                    }
                });
                return null;
            });
            assertEquals(1, ((Number) pool.queryOne("SELECT COUNT(*) AS n FROM t", null).get("n")).intValue());
        } finally { pool.close(); }
    }

    @Test void joinedRollbackCompletesExactlyOnceEvenWhenCompletionCalledAgain() {
        final List<String> events = new ArrayList<String>();
        Transactions.Transaction tx = Transactions.begin(Transactions.REQUIRED, false, -1);
        Transactions.registerSynchronization(hook(events, "tx"));
        Transactions.Transaction joined = Transactions.begin(Transactions.REQUIRED, false, -1);
        Transactions.afterThrow(joined, true);
        assertThrows(TransactionException.UnexpectedRollback.class, () -> Transactions.commit(tx));
        Transactions.commit(tx);
        Transactions.afterThrow(tx, true);
        assertEquals(Arrays.asList("tx:ROLLED_BACK"), events);
    }
    @Test void failedRequiresNewRestoresOuterAndDoesNotRunItsCommitHooks() throws Exception {
        final List<String> events = new ArrayList<String>();
        Transactions.execute(() -> {
            Transactions.bindResource("key", "outer");
            assertThrows(IllegalArgumentException.class, () -> Transactions.execute(Propagation.REQUIRES_NEW,
                    false, -1, Isolation.DEFAULT, () -> {
                        Transactions.bindResource("key", "inner");
                        Transactions.registerSynchronization(hook(events, "inner"));
                        throw new IllegalArgumentException("inner body");
                    }));
            assertEquals("outer", Transactions.getResource("key"));
            Transactions.registerSynchronization(hook(events, "outer"));
            return null;
        });
        assertEquals(Arrays.asList("inner:ROLLED_BACK", "outer:before:false", "outer:commit", "outer:COMMITTED"), events);
    }

    @Test void successfulNestedAndRequiredHooksWaitForOuterCommit() throws Exception {
        final List<String> events = new ArrayList<String>();
        Transactions.execute(() -> {
            for (final Propagation propagation : new Propagation[] {Propagation.REQUIRED, Propagation.NESTED}) {
                Transactions.execute(propagation, false, -1, Isolation.DEFAULT, () -> {
                    Transactions.registerSynchronization(hook(events, propagation.name()));
                    return null;
                });
                assertTrue(events.isEmpty());
            }
            return null;
        });
        assertEquals(Arrays.asList("REQUIRED:before:false", "NESTED:before:false", "REQUIRED:commit", "NESTED:commit",
                "REQUIRED:COMMITTED", "NESTED:COMMITTED"), events);
    }

    @Test void rollbackFailureStillCompletesExactlyOnceAndDiscardsConnection() throws Exception {
        final DataSource pool = pool();
        final List<String> events = new ArrayList<String>();
        final Database[] borrowed = new Database[1];
        try {
            assertThrows(IOException.class, () -> Transactions.execute(() -> {
                borrowed[0] = pool.borrow();
                // End the engine transaction behind the facade to make its ROLLBACK fail.
                borrowed[0].execute("ROLLBACK", null);
                Transactions.registerSynchronization(hook(events, "tx"));
                throw new IOException("body failure stays primary");
            }));
            assertEquals(Arrays.asList("tx:UNKNOWN"), events);
            Database next = pool.borrow();
            assertNotSame(borrowed[0], next);
            pool.release(next);
            assertFalse(Transactions.isActive());
        } finally { pool.close(); }
    }

    @Test void beforeHookRollbackOnlySkipsCommitAndLaterCleanupStillRuns() throws Exception {
        final List<String> events = new ArrayList<String>();
        Transactions.execute(() -> {
            Transactions.registerSynchronization(new Transactions.Synchronization() {
                public void beforeCommit(boolean readOnly) { Transactions.setRollbackOnly(); }
            });
            Transactions.registerSynchronization(hook(events, "tx"));
            return null;
        });
        assertEquals(Arrays.asList("tx:before:false", "tx:ROLLED_BACK"), events);
    }

    @Test void timeoutDuringBeforeCommitRunsCompletionWithoutAfterCommit() {
        final List<String> events = new ArrayList<String>();
        assertThrows(TransactionException.TimedOut.class, () -> Transactions.execute(Propagation.REQUIRED,
                false, 1, Isolation.DEFAULT, () -> {
                    Transactions.registerSynchronization(new Transactions.Synchronization() {
                        public void beforeCommit(boolean readOnly) {
                            try { Thread.sleep(1100); }
                            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
                        }
                    });
                    Transactions.registerSynchronization(hook(events, "tx"));
                    return null;
                }));
        assertEquals(Arrays.asList("tx:before:false", "tx:ROLLED_BACK"), events);
        assertFalse(Transactions.isActive());
    }

    @Test void caughtBeginFailureStillPreventsCommitCallbacks() throws Exception {
        final DataSource pool = pool();
        final List<String> events = new ArrayList<String>();
        try {
            assertThrows(TransactionException.UnexpectedRollback.class, () -> Transactions.execute(Propagation.REQUIRED,
                    false, -1, Isolation.READ_COMMITTED, () -> {
                        Transactions.registerSynchronization(hook(events, "tx"));
                        assertThrows(IOException.class, () -> pool.query("SELECT * FROM t", null));
                        return null;
                    }));
            assertEquals(Arrays.asList("tx:ROLLED_BACK"), events);
            assertFalse(Transactions.isActive());
        } finally { pool.close(); }
    }

}
