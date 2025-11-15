package com.codename1.db;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest extends UITestBase {

    @FormTest
    void testDatabaseOperationsUseImplementation() throws IOException {
        implementation.setDatabaseCustomPathSupported(false);
        assertThrows(IllegalArgumentException.class, () -> Database.openOrCreate("folder/data.db"));

        implementation.setDatabaseCustomPathSupported(true);
        String name = "file://app/test.db";
        Database db = Database.openOrCreate(name);
        assertNotNull(db);

        TestCodenameOneImplementation.TestDatabase testDb = implementation.getTestDatabase(name);
        assertNotNull(testDb);
        Object[][] rows = new Object[][]{{1, "Alice"}, {2, "Bob"}};
        testDb.setQueryResult(new String[]{"id", "name"}, rows);

        Cursor cursor = db.executeQuery("select * from users");
        assertTrue(cursor.first());
        Row row = cursor.getRow();
        assertEquals(1, row.getInteger(0));
        assertEquals("Alice", row.getString(1));
        assertTrue(cursor.next());
        row = cursor.getRow();
        assertEquals(2, row.getInteger(0));
        assertEquals("Bob", row.getString(1));
        cursor.close();

        db.beginTransaction();
        assertTrue(testDb.isInTransaction());
        db.commitTransaction();
        assertFalse(testDb.isInTransaction());

        db.beginTransaction();
        db.rollbackTransaction();
        assertFalse(testDb.isInTransaction());

        db.execute("insert into users values(?, ?)", new String[]{"3", "Cara"});
        assertEquals("insert into users values(?, ?)", testDb.getExecutedStatements().get(0));

        db.close();
        assertTrue(testDb.isClosed());

        Database.delete(name);
        assertNull(implementation.getTestDatabase(name));
    }
}
