package com.codename1.db;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseIntegrationTest extends UITestBase {

    @FormTest
    void databaseTracksStatementsAndCursorNavigation() throws Exception {
        Database db = implementation.openOrCreateDB("integration.db");
        TestCodenameOneImplementation.TestDatabase testDb = implementation.getTestDatabase("integration.db");
        assertNotNull(testDb);
        db.execute("create table sample(id int, name varchar)");
        db.execute("insert into sample values(?, ?)", new String[]{"1", "One"});
        db.execute("insert into sample values(?, ?)", new String[]{"2", "Two"});

        testDb.setQueryResult(new String[]{"id", "name"}, new Object[][]{{"1", "One"}, {"2", "Two"}});
        Cursor cursor = db.executeQuery("select * from sample");
        assertTrue(cursor.first());
        Row first = cursor.getRow();
        assertEquals("1", first.getString(0));
        assertTrue(cursor.next());
        Row second = cursor.getRow();
        assertEquals("Two", second.getString(1));
        assertFalse(cursor.next());

        assertEquals(3, testDb.getExecutedStatements().size());
        assertTrue(testDb.getExecutedStatements().get(2).contains("insert"));
        db.close();
        assertTrue(testDb.isClosed());
    }
}
