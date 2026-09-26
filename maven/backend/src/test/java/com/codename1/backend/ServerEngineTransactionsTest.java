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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the transaction, scheduler-lock and session-store code sends to a SERVER
 * engine, which is where it differs: savepoints and read-only transactions go
 * through MySQL's text protocol, and the lock and session tables are DDL built
 * from each dialect's column types.
 *
 * Runs only when pointed at a database -- CN1_TX_POSTGRES or CN1_TX_MYSQL, a
 * postgres:// or mysql:// URL -- because SQLite, which TransactionsTest covers,
 * is the engine that needs none of this.
 */
class ServerEngineTransactionsTest {

    private static DataSource open(String engine) throws IOException {
        String url = System.getenv("CN1_TX_" + engine);
        Assumptions.assumeTrue(url != null && url.length() > 0,
                "CN1_TX_" + engine + " is unset; this engine is not exercised");
        DataSource pool = DataSource.open(url, 4, 5000, 10000);
        pool.execute("DROP TABLE IF EXISTS cn1_tx_probe", null);
        pool.execute("CREATE TABLE cn1_tx_probe (v VARCHAR(40))", null);
        return pool;
    }

    private static int rows(DataSource pool) throws IOException {
        Map row = pool.queryOne("SELECT COUNT(*) AS n FROM cn1_tx_probe", null);
        return ((Number)row.get("n")).intValue();
    }

    private static void insert(DataSource pool, String v) throws IOException {
        pool.execute("INSERT INTO cn1_tx_probe (v) VALUES (?)", new Object[] {v});
    }

    @ParameterizedTest
    @ValueSource(strings = {"POSTGRES", "MYSQL"})
    void commitRollbackAndSavepoints(String engine) throws Exception {
        DataSource pool = open(engine);
        try {
            Transactions.Transaction tx = Transactions.begin(Transactions.REQUIRED, false, -1);
            insert(pool, "a");
            Transactions.commit(tx);
            assertEquals(1, rows(pool));

            tx = Transactions.begin(Transactions.REQUIRED, false, -1);
            insert(pool, "b");
            Transactions.afterThrow(tx, true);
            assertEquals(1, rows(pool), "a rolled-back insert was kept");

            Transactions.Transaction outer = Transactions.begin(Transactions.REQUIRED, false, -1);
            insert(pool, "kept");
            Transactions.Transaction nested = Transactions.begin(Transactions.NESTED, false, -1);
            insert(pool, "undone");
            Transactions.afterThrow(nested, true);
            Transactions.commit(outer);
            assertEquals(2, rows(pool), "the savepoint did not undo exactly its own insert");

            // REQUIRES_NEW on a second connection commits while the outer
            // transaction -- which a server engine lets both write -- rolls back.
            outer = Transactions.begin(Transactions.REQUIRED, false, -1);
            insert(pool, "outer");
            Transactions.Transaction inner = Transactions.begin(Transactions.REQUIRES_NEW, false, -1);
            insert(pool, "inner");
            Transactions.commit(inner);
            Transactions.afterThrow(outer, true);
            assertEquals(3, rows(pool), "REQUIRES_NEW did not commit independently");
        } finally {
            pool.execute("DROP TABLE IF EXISTS cn1_tx_probe", null);
            pool.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"POSTGRES", "MYSQL"})
    void aReadOnlyTransactionRefusesWrites(String engine) throws Exception {
        DataSource pool = open(engine);
        try {
            final DataSource p = pool;
            Transactions.Transaction tx = Transactions.begin(Transactions.REQUIRED, true, -1);
            assertThrows(IOException.class, () -> insert(p, "x"),
                    "the engine accepted a write in a read-only transaction");
            Transactions.afterThrow(tx, true);
            assertEquals(0, rows(pool));
        } finally {
            pool.execute("DROP TABLE IF EXISTS cn1_tx_probe", null);
            pool.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"POSTGRES", "MYSQL"})
    void theSessionStoreRoundTrips(String engine) throws Exception {
        DataSource pool = open(engine);
        try {
            pool.execute("DROP TABLE IF EXISTS cn1_http_session", null);
            Sessions.Jdbc store = new Sessions.Jdbc(pool);
            long now = System.currentTimeMillis();
            HttpSession s = new HttpSession("abc", now, now, 60);
            s.setAttribute("user", "ada");
            s.setAttribute("visits", new Long(3));
            store.save(s, null);
            HttpSession back = store.load("abc");
            assertNotNull(back);
            assertEquals("ada", back.getAttribute("user"));
            assertEquals(3L, ((Number)back.getAttribute("visits")).longValue());
            store.save(back, null);                        // the update path
            HttpSession renamed = new HttpSession("def", now, now, 60);
            store.save(renamed, "abc");
            assertNull(store.load("abc"), "the old id survived a rename");
            HttpSession stale = new HttpSession("old", now - 120000, now - 120000, 60);
            store.save(stale, null);
            assertTrue(store.purgeExpired(now) >= 1);
            assertNull(store.load("old"));
        } finally {
            pool.execute("DROP TABLE IF EXISTS cn1_http_session", null);
            pool.execute("DROP TABLE IF EXISTS cn1_tx_probe", null);
            pool.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"POSTGRES", "MYSQL"})
    void theSchedulerLockIsExclusive(String engine) throws Exception {
        DataSource pool = open(engine);
        try {
            pool.execute("DROP TABLE IF EXISTS cn1_scheduler_lock", null);
            final int[] ran = new int[2];
            final Scheduler first = new Scheduler(pool);
            final Scheduler second = new Scheduler(pool);
            final java.util.concurrent.CountDownLatch holding = new java.util.concurrent.CountDownLatch(1);
            final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
            first.fixedDelay("job", 1000000, 1000000, null, Tasks.PLATFORM, "job", 60000,
                    new Runnable() {
                        public void run() {
                            ran[0]++;
                            holding.countDown();
                            try {
                                release.await();
                            } catch (InterruptedException ignored) {
                                // test
                            }
                        }
                    });
            second.fixedDelay("job", 1000000, 1000000, null, Tasks.PLATFORM, "job", 60000,
                    new Runnable() {
                        public void run() {
                            ran[1]++;
                        }
                    });
            first.start();
            second.start();
            assertTrue(first.trigger("job"));
            assertTrue(holding.await(10, java.util.concurrent.TimeUnit.SECONDS));
            assertTrue(second.trigger("job"));
            long deadline = System.currentTimeMillis() + 5000;
            while(System.currentTimeMillis() < deadline
                    && ((Number)((Map)second.describe().get(0)).get("skipped")).longValue() == 0) {
                Thread.sleep(20);
            }
            assertEquals(0, ran[1], "the second instance ran while the first held the lock");
            release.countDown();
            first.stop(5000);
            second.stop(5000);
            // Released: now the second one may run.
            Scheduler third = new Scheduler(pool);
            final boolean[] thirdRan = new boolean[1];
            third.fixedDelay("job", 1000000, 1000000, null, Tasks.PLATFORM, "job", 60000,
                    new Runnable() {
                        public void run() {
                            thirdRan[0] = true;
                        }
                    });
            third.start();
            assertTrue(third.trigger("job"));
            deadline = System.currentTimeMillis() + 5000;
            while(!thirdRan[0] && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            third.stop(5000);
            assertTrue(thirdRan[0], "a released lock could not be claimed again");
            assertFalse(ran[0] == 0);
        } finally {
            Tasks.shutdown(2000);
            pool.execute("DROP TABLE IF EXISTS cn1_scheduler_lock", null);
            pool.execute("DROP TABLE IF EXISTS cn1_tx_probe", null);
            pool.close();
        }
    }
}
