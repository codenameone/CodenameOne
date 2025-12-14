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
}
