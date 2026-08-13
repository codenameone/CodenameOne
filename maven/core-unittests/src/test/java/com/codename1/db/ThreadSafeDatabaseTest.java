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
}
