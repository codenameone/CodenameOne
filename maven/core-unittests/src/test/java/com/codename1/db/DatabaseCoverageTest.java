package com.codename1.db;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.CN;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseCoverageTest extends UITestBase {

    private interface DatabaseHandler {
        void accept(Database database, TestCodenameOneImplementation.TestDatabase backing) throws Exception;
    }

    private void withDatabases(String baseName, DatabaseHandler handler) throws Exception {
        String databaseName = baseName + "-database.db";

        Database database = implementation.openOrCreateDB(databaseName);
        TestCodenameOneImplementation.TestDatabase backing = implementation.getTestDatabase(databaseName);

        try {
            handler.accept(database, backing);
        } finally {
            database.close();
            Database.delete(databaseName);
        }
    }

    @FormTest
    void existsAndDatabasePathUsesImplementation() throws Exception {
        implementation.setDatabaseCustomPathSupported(false);
        assertThrows(IllegalArgumentException.class, () -> Database.exists("folder/data.db"));
        assertThrows(IllegalArgumentException.class, () -> Database.getDatabasePath("folder/data.db"));

        implementation.setDatabaseCustomPathSupported(true);
        String dbName = "coveragePath.db";
        assertFalse(Database.exists(dbName));
        assertNull(Database.getDatabasePath(dbName));

        Database database = Database.openOrCreate(dbName);
        assertTrue(Database.exists(dbName));
        assertEquals(dbName, Database.getDatabasePath(dbName));

        database.close();
        Database.delete(dbName);
        assertFalse(Database.exists(dbName));
        assertNull(Database.getDatabasePath(dbName));
    }

    @FormTest
    void executeVarargsForwardsParametersToUnderlyingDatabase() throws Exception {
        withDatabases("execute-varargs", new DatabaseHandler() {
            public void accept(Database database, TestCodenameOneImplementation.TestDatabase backing) throws Exception {
                database.execute("update users set name=?", (Object[]) null);
                database.execute("insert into users values(?, ?, ?)", new Object[]{"7", null, 5d});

                List<String> statements = backing.getExecutedStatements();
                List<String[]> parameters = backing.getExecutedParameters();
                assertEquals(2, statements.size());
                assertEquals("update users set name=?", statements.get(0));
                assertNull(parameters.get(0));
                assertArrayEquals(new String[]{"7", null, "5.0"}, parameters.get(1));

                assertThrows(RuntimeException.class, () -> database.execute("insert into users values(?)", new Object[]{new byte[]{1, 2}}));
            }
        });
    }

    @FormTest
    void executeQueryVarargsCaptureParametersAndResults() throws Exception {
        withDatabases("query-varargs", new DatabaseHandler() {
            public void accept(Database database, TestCodenameOneImplementation.TestDatabase backing) throws Exception {
                backing.setQueryResult(new String[]{"id", "name"}, new Object[][]{{"1", "One"}});
                Cursor cursor = database.executeQuery("select * from sample where id=?", new Object[]{1});
                assertTrue(cursor.first());
                assertEquals("One", cursor.getRow().getString(1));
                cursor.close();

                List<String> queries = backing.getExecutedQueries();
                List<String[]> parameters = backing.getExecutedQueryParameters();
                assertEquals(1, queries.size());
                assertEquals("select * from sample where id=?", queries.get(0));
                assertArrayEquals(new String[]{"1"}, parameters.get(0));

                backing.setQueryResult(new String[]{"total"}, new Object[][]{});
                Cursor noParamsCursor = database.executeQuery("select count(*) from sample", (Object[]) null);
                assertFalse(noParamsCursor.first());
                noParamsCursor.close();

                queries = backing.getExecutedQueries();
                parameters = backing.getExecutedQueryParameters();
                assertEquals(2, queries.size());
                assertEquals("select count(*) from sample", queries.get(1));
                assertNull(parameters.get(1));
            }
        });
    }

    @FormTest
    void wasNullAndSupportsWasNullHandledAcrossDatabases() throws Exception {
        withDatabases("was-null", new DatabaseHandler() {
            public void accept(Database database, TestCodenameOneImplementation.TestDatabase backing) throws Exception {
                backing.setRowExtSupported(false);
                backing.setQueryResult(new String[]{"value"}, new Object[][]{{"data"}});
                Cursor cursor = database.executeQuery("select value from table");
                assertTrue(cursor.first());
                Row row = cursor.getRow();
                assertFalse(Database.supportsWasNull(row));
                assertFalse(Database.wasNull(row));
                cursor.close();

                backing.setRowExtSupported(true);
                backing.setQueryResult(new String[]{"maybe_null"}, new Object[][]{{null}, {"text"}});
                Cursor extCursor = database.executeQuery("select maybe_null from table");
                assertTrue(extCursor.first());
                Row extRow = extCursor.getRow();
                extRow.getString(0);
                assertTrue(Database.supportsWasNull(extRow));
                assertTrue(Database.wasNull(extRow));

                assertTrue(extCursor.next());
                Row secondRow = extCursor.getRow();
                secondRow.getString(0);
                assertFalse(Database.wasNull(secondRow));
                extCursor.close();
            }
        });
    }
}
