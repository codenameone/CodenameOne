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
package com.codename1.impl.backend;

import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The scheduler's database leases, which keep one replica running a locked job.
class SchedulerLeaseTest {
    @Test
    @DisplayName("a claim written after its lease ran out does not run the job")
    void aClaimThatLandsExpiredIsNotALease(@org.junit.jupiter.api.io.TempDir java.io.File dir)
            throws Exception {
        final String path = new java.io.File(dir, "late.db").getAbsolutePath();
        DataSource pool = DataSource.open(path, 2, 5000, 10000);
        try {
            Scheduler scheduler = new Scheduler(pool);
            Runnable nothing = new Runnable() {
                public void run() {
                }
            };
            Scheduler.Job job = new Scheduler.Job("late", Scheduler.FIXED_DELAY, null, 1000, 0,
                    null, BackendAccess.PLATFORM, "late", 300, nothing);
            // Creates the table and the row, then frees it.
            scheduler.release(job, scheduler.claim(job));
            // Another writer holds the database for longer than the lease, so the
            // claim's write waits past it, as a replica waiting on a lock would.
            final java.util.concurrent.CountDownLatch holding =
                    new java.util.concurrent.CountDownLatch(1);
            Thread blocker = new Thread() {
                public void run() {
                    try {
                        Database db = Database.open(path);
                        db.execute("BEGIN IMMEDIATE", null);
                        holding.countDown();
                        Thread.sleep(700);
                        db.execute("COMMIT", null);
                        db.close();
                    } catch (Exception err) {
                        holding.countDown();
                    }
                }
            };
            blocker.start();
            assertTrue(holding.await(5, TimeUnit.SECONDS));
            long started = System.currentTimeMillis();
            assertNull(scheduler.claim(job),
                    "a claim written after its lease had run out was returned as a lease");
            assertTrue(System.currentTimeMillis() - started >= 300,
                    "the claim did not wait on the held lock, so this proved nothing");
            blocker.join(5000);
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("an expired run releases only its own lease, not a later run's")
    void anExpiredRunDoesNotReleaseItsSuccessor(@org.junit.jupiter.api.io.TempDir java.io.File dir)
            throws Exception {
        DataSource pool = DataSource.open(new java.io.File(dir, "l.db").getAbsolutePath(),
                2, 5000, 10000);
        try {
            Scheduler scheduler = new Scheduler(pool);
            Runnable nothing = new Runnable() {
                public void run() {
                }
            };
            // Long enough to still be live once the claim is written -- a claim
            // that lands already expired is refused -- and short enough to run out.
            Scheduler.Job slow = new Scheduler.Job("slow", Scheduler.FIXED_DELAY, null, 1000, 0,
                    null, BackendAccess.PLATFORM, "shared", 200, nothing);
            Scheduler.Job next = new Scheduler.Job("next", Scheduler.FIXED_DELAY, null, 1000, 0,
                    null, BackendAccess.PLATFORM, "shared", 60000, nothing);
            String expired = scheduler.claim(slow);
            assertNotNull(expired);
            Thread.sleep(260);                       // the lease runs out
            String current = scheduler.claim(next);
            assertNotNull(current, "an expired lease was not taken over");
            scheduler.release(slow, expired);        // the overrun finally ends
            assertNull(scheduler.claim(slow),
                    "the overrun released the lease a later run now holds");
            scheduler.release(next, current);
            assertNotNull(scheduler.claim(slow), "releasing its own lease freed nothing");
        } finally {
            pool.close();
        }
    }
}
