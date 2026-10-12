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
package com.demo;

import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.Transactions;
import com.codename1.backend.TransactionException;
import com.codename1.backend.annotations.Isolation;
import com.codename1.backend.annotations.Propagation;
import java.io.IOException;

/// Standalone JVM/ParparVM transaction synchronization and isolation probe.
/// CN1_TXCHECK_URL selects an optional PostgreSQL/MySQL database; SQLite is default.
public class TransactionCheck {
    private static final StringBuilder events = new StringBuilder();
    private static int passed;

    public static void main(String[] args) throws Exception {
        String url = System.getenv("CN1_TXCHECK_URL");
        if (url == null || url.length() == 0) {
            url = ":memory:";
        }
        final DataSource pool = DataSource.open(url, 1, 1000, 10000);
        try {
            pool.execute("DROP TABLE IF EXISTS cn1_sync_probe", null);
            pool.execute("CREATE TABLE cn1_sync_probe (v INTEGER)", null);
            final Database first = pool.borrow();
            final String engine = first.dialect().getName();
            pool.release(first);
            Transactions.execute(Propagation.REQUIRED, false, -1, Isolation.SERIALIZABLE, new Transactions.Work() {
                public Object run() throws Exception {
                    Transactions.bindResource("key", "outer");
                    Transactions.registerSynchronization(hook("outer"));
                    Transactions.execute(Propagation.REQUIRES_NEW, true, -1, Isolation.SERIALIZABLE, new Transactions.Work() {
                        public Object run() throws Exception {
                            check(Transactions.getResource("key") == null, "independent resources");
                            Transactions.bindResource("key", "inner");
                            Transactions.registerSynchronization(hook("inner"));
                            pool.query("SELECT * FROM cn1_sync_probe", null);
                            return null;
                        }
                    });
                    check("outer".equals(Transactions.getResource("key")), "suspended resources restored");
                    pool.execute("INSERT INTO cn1_sync_probe VALUES (1)", null);
                    Transactions.execute(Propagation.NESTED, false, -1, Isolation.SERIALIZABLE, new Transactions.Work() {
                        public Object run() throws Exception {
                            Transactions.registerSynchronization(hook("nested"));
                            pool.execute("INSERT INTO cn1_sync_probe VALUES (2)", null);
                            Transactions.setRollbackOnly();
                            return null;
                        }
                    });
                    return null;
                }
            });
            check(events.toString().equals("inner:before:true;inner:commit;inner:COMMITTED;outer:before:false;outer:commit;outer:COMMITTED;nested:COMMITTED;"), "ordered hooks and savepoint rollback");
            check(((Number) pool.queryOne("SELECT COUNT(*) AS n FROM cn1_sync_probe", null).get("n")).intValue() == 1, "savepoint data rolled back");
            check(!Transactions.isActive() && Transactions.getResource("key") == null, "scope cleared");
            Database reused = pool.borrow();
            check(first == reused && !reused.isInTransaction(), "same connection reused cleanly");
            pool.release(reused);

            events.setLength(0);
            try {
                Transactions.execute(new Transactions.Work() {
                    public Object run() throws Exception {
                        Transactions.registerSynchronization(hook("rollback"));
                        pool.execute("INSERT INTO cn1_sync_probe VALUES (3)", null);
                        throw new IOException("body failed");
                    }
                });
                throw new RuntimeException("body failure was swallowed");
            } catch (IOException expected) { }
            check(events.toString().equals("rollback:ROLLED_BACK;"), "rollback completion only");

            events.setLength(0);
            try {
                Transactions.execute(new Transactions.Work() {
                    public Object run() throws Exception {
                        Transactions.registerSynchronization(new Transactions.Synchronization() {
                            public void beforeCommit(boolean readOnly) { throw new IllegalArgumentException("before"); }
                        });
                        Transactions.registerSynchronization(hook("before-failed"));
                        pool.execute("INSERT INTO cn1_sync_probe VALUES (4)", null);
                        return null;
                    }
                });
                throw new RuntimeException("before failure was swallowed");
            } catch (TransactionException expected) { }
            check(events.toString().equals("before-failed:ROLLED_BACK;"), "before failure cleanup");

            events.setLength(0);
            try {
                Transactions.execute(new Transactions.Work() {
                    public Object run() {
                        Transactions.registerSynchronization(new Transactions.Synchronization() {
                            public void afterCommit() { throw new IllegalArgumentException("after"); }
                            public void afterCompletion(Transactions.Outcome outcome) { throw new IllegalArgumentException("cleanup"); }
                        });
                        Transactions.registerSynchronization(hook("after-failed"));
                        return null;
                    }
                });
                throw new RuntimeException("after failure was swallowed");
            } catch (TransactionException expected) { }
            check(events.toString().equals("after-failed:before:false;after-failed:commit;after-failed:COMMITTED;"), "callback errors preserve remaining hooks");
            check(!Transactions.isActive(), "callback failure clears thread");

            if ("sqlite".equals(engine)) {
                try {
                    pool.inTransaction(Isolation.READ_COMMITTED, new DataSource.Work() {
                        public Object run(Database db) { throw new RuntimeException("unsupported isolation accepted"); }
                    });
                    throw new RuntimeException("unsupported isolation did not throw");
                } catch (IOException expected) { passed++; }
            } else if ("postgres".equals(engine) || "postgresql".equals(engine)) {
                final String baseline = String.valueOf(pool.queryOne("SHOW transaction_isolation", null).get("transaction_isolation"));
                pool.inTransaction(Isolation.SERIALIZABLE, new DataSource.Work() {
                    public Object run(Database db) throws Exception {
                        check("serializable".equals(db.queryOne("SHOW transaction_isolation", null).get("transaction_isolation")), "PostgreSQL isolation applied");
                        return null;
                    }
                });
                check(baseline.equals(pool.queryOne("SHOW transaction_isolation", null).get("transaction_isolation")), "PostgreSQL default restored");
            }

            // A deferred constraint makes COMMIT fail on both SQLite and PostgreSQL.
            if (!"mysql".equals(engine)) {
                pool.execute("DROP TABLE IF EXISTS cn1_sync_child", null);
                pool.execute("DROP TABLE IF EXISTS cn1_sync_parent", null);
                pool.execute("CREATE TABLE cn1_sync_parent (id INTEGER PRIMARY KEY)", null);
                pool.execute("CREATE TABLE cn1_sync_child (id INTEGER REFERENCES cn1_sync_parent(id) DEFERRABLE INITIALLY DEFERRED)", null);
                events.setLength(0);
                try {
                    Transactions.execute(new Transactions.Work() {
                        public Object run() throws Exception {
                            Transactions.registerSynchronization(hook("commit-failed"));
                            pool.execute("INSERT INTO cn1_sync_child VALUES (123)", null);
                            return null;
                        }
                    });
                    throw new RuntimeException("failed commit accepted");
                } catch (TransactionException expected) { }
                check(events.toString().equals("commit-failed:before:false;commit-failed:UNKNOWN;"), "failed commit skips after-commit");
                check(!Transactions.isActive(), "failed commit clears thread");
            }
            System.out.println("TXCHECK OK passed=" + passed);
        } finally {
            pool.close();
        }
    }

    private static Transactions.Synchronization hook(final String name) {
        return new Transactions.Synchronization() {
            public void beforeCommit(boolean readOnly) { events.append(name).append(":before:").append(readOnly).append(';'); }
            public void afterCommit() { events.append(name).append(":commit;"); }
            public void afterCompletion(Transactions.Outcome outcome) { events.append(name).append(':').append(outcome).append(';'); }
        };
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new RuntimeException(message + " events=" + events); }
        passed++;
    }
}
