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

import java.util.List;
import java.util.Map;

import com.codename1.backend.Db;
import com.codename1.backend.DbPool;

/**
 * Exercises DbPool from several threads at once against one WAL database, which
 * is the arrangement a pool exists for. Verifies the row count rather than just
 * that nothing threw: a pool that silently gave two threads the same connection
 * would still "work" until it corrupted a result.
 */
public class PoolCheck {
    private static final int THREADS = 8;
    private static final int PER_THREAD = 50;

    public static void main(String[] args) throws Exception {
        String path = System.getenv("CN1_DB_PATH");
        if(path == null) {
            System.out.println("SKIP: set CN1_DB_PATH");
            return;
        }
        final DbPool pool = DbPool.open(path, 4, 5000);
        Db setup = pool.borrow();
        setup.execute("DROP TABLE IF EXISTS counter", null);
        setup.execute("CREATE TABLE counter (id INTEGER PRIMARY KEY AUTOINCREMENT, who TEXT, n INTEGER)", null);
        pool.release(setup);

        final int[] failures = new int[1];
        Thread[] workers = new Thread[THREADS];
        for(int t = 0 ; t < THREADS ; t++) {
            final String who = "worker-" + t;
            workers[t] = new Thread(new Runnable() {
                public void run() {
                    for(int i = 0 ; i < PER_THREAD ; i++) {
                        final int n = i;
                        try {
                            pool.inTransaction(new Db.Work() {
                                public Object run(Db db) throws Exception {
                                    db.execute("INSERT INTO counter (who, n) VALUES (?, ?)",
                                            new Object[]{who, new Integer(n)});
                                    return null;
                                }
                            });
                        } catch (Exception err) {
                            synchronized(failures) {
                                failures[0]++;
                            }
                            System.err.println(who + " failed: " + err);
                        }
                    }
                }
            });
            workers[t].start();
        }
        for(int t = 0 ; t < THREADS ; t++) {
            workers[t].join();
        }

        Db check = pool.borrow();
        List rows = check.query("SELECT COUNT(*) AS c FROM counter", null);
        long count = ((Number)((Map)rows.get(0)).get("c")).longValue();
        List distinct = check.query("SELECT COUNT(DISTINCT who) AS c FROM counter", null);
        long writers = ((Number)((Map)distinct.get(0)).get("c")).longValue();
        pool.release(check);
        pool.close();

        int expected = THREADS * PER_THREAD;
        System.out.println("rows=" + count + " expected=" + expected
                + " writers=" + writers + " failures=" + failures[0]);
        System.out.println(count == expected && writers == THREADS && failures[0] == 0
                ? "POOL OK" : "POOL FAILED");
    }
}
