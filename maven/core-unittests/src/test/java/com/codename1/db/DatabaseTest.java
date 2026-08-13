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
        assertEquals(2, cursor.getColumnCount());
        assertEquals("id", cursor.getColumnName(0));
        assertEquals(0, cursor.getColumnIndex("id"));
        assertTrue(cursor.first());
        Row row = cursor.getRow();
        assertEquals(1, row.getInteger(0));
        assertEquals("Alice", row.getString(1));
        assertEquals(1.0d, row.getDouble(0));
        assertTrue(cursor.next());
        row = cursor.getRow();
        assertEquals(2, row.getInteger(0));
        assertEquals("Bob", row.getString(1));
        assertEquals(2L, row.getLong(0));
        assertFalse(cursor.next());
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

    @FormTest
    void deleteIsRefusedWhileTheDatabaseIsOpen() throws IOException {
        // Every platform here unlinks an open file quite happily and leaves the handle attached
        // to something with no name: reopening the name makes a different database and whatever
        // the old handle writes goes away with it.
        String name = "delete-while-open.db";
        Database db = Database.openOrCreate(name);
        assertNotNull(db);
        try {
            Database.delete(name);
            fail("deleting a database that is still open has to be refused");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().indexOf("still open") >= 0,
                    "the message should say what is wrong: " + expected.getMessage());
        }
        assertNotNull(implementation.getTestDatabase(name), "and the database is still there");
        db.close();
        Database.delete(name);
        assertNull(implementation.getTestDatabase(name));
    }

    @FormTest
    void deleteIsRefusedWhileAnUnidentifiedConnectionIsOpen() throws IOException {
        // A connection wrapped by name-less means -- SEDatabase(Connection) over a URL that names
        // no file -- is counted without a key, so it cannot be matched against the database being
        // deleted. It could be this one, and unlinking underneath it loses its writes, so the
        // only safe reading of "I do not know" is to refuse. A key change already reads the same
        // counter this way.
        String name = "delete-with-unidentified.db";
        Database db = Database.openOrCreate(name);
        assertNotNull(db);
        UnidentifiedDatabase stranger = new UnidentifiedDatabase();
        try {
            db.close();
            Database.delete(name);
            fail("an unidentified connection has to block the delete");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().indexOf("opened from a connection") >= 0,
                    "the message should name the reason: " + expected.getMessage());
        } finally {
            stranger.close();
        }
        // With it gone the delete goes through, which is what shows the refusal was about that
        // connection and not about something left behind by the test.
        Database.delete(name);
        assertNull(implementation.getTestDatabase(name));
    }

    /// Stands in for a connection whose file cannot be named, which is what registering a null
    /// key means.
    private static final class UnidentifiedDatabase extends Database {
        private UnidentifiedDatabase() throws IOException {
            registerOpenDatabase(null);
        }

        @Override
        public void beginTransaction() throws IOException {
        }

        @Override
        public void commitTransaction() throws IOException {
        }

        @Override
        public void rollbackTransaction() throws IOException {
        }

        @Override
        public void close() throws IOException {
            releaseOpenDatabase(null);
        }

        @Override
        public void execute(String sql) throws IOException {
        }

        @Override
        public void execute(String sql, String[] params) throws IOException {
        }

        @Override
        public Cursor executeQuery(String sql, String[] params) throws IOException {
            return null;
        }

        @Override
        public Cursor executeQuery(String sql) throws IOException {
            return null;
        }
    }
}
