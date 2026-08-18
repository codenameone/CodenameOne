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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.Assertions;
import java.util.List;

public class ThreadSafeDatabaseTest extends UITestBase {

    @FormTest
    public void testDelegation() throws Exception {
        Database db = TestCodenameOneImplementation.getInstance().openOrCreateDB("test_threadsafe.db");
        ThreadSafeDatabase tsDb = new ThreadSafeDatabase(db);

        // Test execute
        tsDb.execute("CREATE TABLE foo (id INTEGER)");
        List<String> statements = ((TestCodenameOneImplementation.TestDatabase)db).getExecutedStatements();
        Assertions.assertTrue(statements.contains("CREATE TABLE foo (id INTEGER)"));

        // Test execute with params
        tsDb.execute("INSERT INTO foo VALUES (?)", new Object[]{1});
        // TestCodenameOneImplementation.TestDatabase doesn't store params with statements easily accessible in pairs
        // but we verify no exception and delegation occurs.

        // Test executeQuery
        ((TestCodenameOneImplementation.TestDatabase)db).setQueryResult(
            new String[]{"id"},
            new Object[][]{{1}}
        );
        Cursor c = tsDb.executeQuery("SELECT * FROM foo");
        Assertions.assertTrue(c.next());
        Row r = c.getRow();
        Assertions.assertEquals(1, r.getInteger(0));
        c.close();

        // Test executeQuery with params
        ((TestCodenameOneImplementation.TestDatabase)db).setQueryResult(
            new String[]{"id"},
            new Object[][]{{1}}
        );
        Cursor c2 = tsDb.executeQuery("SELECT * FROM foo WHERE id = ?", new Object[]{1});
        Assertions.assertTrue(c2.next());
        c2.close();

        // Test beginTransaction / commit
        tsDb.beginTransaction();
        tsDb.commitTransaction();
        tsDb.close();
    }

    @FormTest
    public void closingFromTheWorkerEndsTheWorker() throws Exception {
        // close() called from a task already running on the wrapper's own thread cannot hand the
        // close to that thread and wait for it, so it closes in place. It used to leave the
        // thread parked for the life of the process; now it asks it to stop once its queue has
        // drained, which is a stop that does not abandon work somebody is waiting on.
        Database db = TestCodenameOneImplementation.getInstance()
                .openOrCreateDB("test_threadsafe_worker_close.db");
        final ThreadSafeDatabase tsDb = new ThreadSafeDatabase(db);
        final boolean[] closedOnWorker = {false};
        tsDb.getThread().run(new Runnable() {
            public void run() {
                try {
                    tsDb.close();
                    closedOnWorker[0] = true;
                } catch (Exception err) {
                    closedOnWorker[0] = false;
                }
            }
        });
        long deadline = System.currentTimeMillis() + 5000;
        while (!tsDb.getThread().isFinished() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Assertions.assertTrue(closedOnWorker[0], "the close ran on the worker");
        Assertions.assertTrue(tsDb.getThread().isFinished(),
                "and the worker stopped rather than parking for the life of the process");

        // A call arriving after that gets the closed-database error rather than blocking on a
        // thread that will never take it.
        try {
            tsDb.execute("SELECT 1");
            Assertions.fail("a call after close has to be refused");
        } catch (java.io.IOException expected) {
            Assertions.assertTrue(expected.getMessage().indexOf("closed") >= 0,
                    "the message should say the database is closed: " + expected.getMessage());
        }
    }

    @FormTest
    public void killedThreadReportsItselfFinished() throws Exception {
        // kill() ends the loop through a different door than killWhenIdle(), and the flag the
        // blocking calls consult was set on only one of them: a thread killed this way looked
        // alive, so run() accepted work and waited for a worker that had gone.
        com.codename1.util.EasyThread et = com.codename1.util.EasyThread.start("test-kill-flag");
        Assertions.assertFalse(et.isFinished(), "a running thread is not finished");
        et.kill();
        long deadline = System.currentTimeMillis() + 5000;
        while (!et.isFinished() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Assertions.assertTrue(et.isFinished(), "a killed thread reports itself finished");
        try {
            et.runAndWait(new Runnable() {
                public void run() {
                }
            });
            Assertions.fail("work handed to a thread that has ended has to be refused");
        } catch (IllegalStateException expected) {
            Assertions.assertTrue(expected.getMessage().indexOf("stopped") >= 0,
                    "the message should say the thread was stopped: " + expected.getMessage());
        }
    }

    @FormTest
    public void closingAfterTheWorkerHasGoneStaysQuiet() throws Exception {
        // Two closes can race: one arriving on the worker through getThread(), one from anywhere
        // else. The worker's own path closes the database and then asks the thread to drain, and
        // the other caller can already have passed its closed check by then -- so its hand-off
        // reaches a thread that has stopped and is refused, unchecked. close() is idempotent by
        // contract, so that refusal must not reach the caller.
        //
        // Reached here through getThread().killWhenIdle() rather than by trying to lose a race.
        // That is the state the race produces -- a stopped worker and a wrapper that does not
        // know it yet -- and it is also a call any caller can make, since getThread() is public.
        Database db = TestCodenameOneImplementation.getInstance()
                .openOrCreateDB("test_threadsafe_close_after_worker.db");
        ThreadSafeDatabase tsDb = new ThreadSafeDatabase(db);
        tsDb.getThread().killWhenIdle();
        long deadline = System.currentTimeMillis() + 5000;
        while (!tsDb.getThread().isFinished() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Assertions.assertTrue(tsDb.getThread().isFinished(), "the worker has to have stopped");

        tsDb.close();

        // And the database really is closed, rather than the close having been quietly skipped:
        // there is no worker left to run it, so it has to have been closed directly. Asserted on
        // the state rather than on a later call being refused, because this test double answers
        // work whether it is open or not, so a refusal is not something this could observe.
        Assertions.assertTrue(
                ((TestCodenameOneImplementation.TestDatabase) db).isClosed(),
                "the underlying database has to have been closed, not just abandoned");

        // Still idempotent afterwards.
        tsDb.close();
    }
    @FormTest
    public void closingWhileTheWorkerIsStillDrainingWaitsForIt() throws Exception {
        // A stop that has been asked for is not a worker that has gone. EasyThread refuses new
        // work the moment killWhenIdle() is called, while whatever it already accepted keeps
        // running -- against this database. Reading that refusal as "there is nothing left to race
        // with" closed the database on the caller thread underneath a live operation, which is the
        // one thing this wrapper exists to prevent.
        Database db = TestCodenameOneImplementation.getInstance()
                .openOrCreateDB("test_threadsafe_close_while_draining.db");
        final TestCodenameOneImplementation.TestDatabase underlying =
                (TestCodenameOneImplementation.TestDatabase) db;
        ThreadSafeDatabase tsDb = new ThreadSafeDatabase(db);

        final boolean[] started = new boolean[1];
        final boolean[] finished = new boolean[1];
        final boolean[] closedMidTask = new boolean[1];
        tsDb.getThread().run(new Runnable() {
            public void run() {
                synchronized (started) {
                    started[0] = true;
                    started.notifyAll();
                }
                try {
                    // Long enough that a close which does not wait returns while this is running.
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
                closedMidTask[0] = underlying.isClosed();
                finished[0] = true;
            }
        });
        synchronized (started) {
            while (!started[0]) {
                started.wait(5000);
            }
        }

        // The state any caller can produce, since getThread() is public: still busy, no longer
        // accepting. close() below is refused rather than handed over.
        tsDb.getThread().killWhenIdle();

        tsDb.close();

        Assertions.assertTrue(finished[0],
                "close() returned while the worker was still running a task");
        Assertions.assertFalse(closedMidTask[0],
                "the database was closed underneath an operation that was still running");
        Assertions.assertTrue(underlying.isClosed(),
                "and the database still has to end up closed");
    }

}
