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
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.CN;

import java.util.List;

import java.io.IOException;
import java.io.OutputStream;

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

                // A port that cannot bind blobs reports it as IOException, like every other
                // database failure, rather than as an unchecked RuntimeException.
                IOException blobFailure = assertThrows(IOException.class,
                        () -> database.execute("insert into users values(?)", new Object[]{new byte[]{1, 2}}));
                assertTrue(blobFailure.getMessage().contains("byte[]"),
                        "the message should name the offending parameter type: " + blobFailure.getMessage());
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
    @FormTest
    void anEmptyFileIsAPlaintextDatabaseRatherThanAnEncryptedOne() throws IOException {
        // SQLite writes nothing until the first change, so a database that has been created and
        // not yet written to is a zero byte file -- a valid empty plaintext database. Reading the
        // end of the file as a failed header read reported it as encrypted, which is the state
        // openOrCreate() followed by close() leaves behind.
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        Database db = impl.openOrCreateDB("test_is_encrypted_empty.db");
        db.close();
        writeDatabaseFile("test_is_encrypted_empty.db", new byte[0]);

        assertFalse(Database.isEncrypted("test_is_encrypted_empty.db"),
                "an empty database file is plaintext");
    }

    @FormTest
    void aPlaintextHeaderReadsAsPlaintextAndAnythingElseDoesNot() throws IOException {
        // The two ends of the same sniff, so the empty case above is not the only thing holding
        // this method's contract in place.
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.openOrCreateDB("test_is_encrypted_plain.db").close();
        impl.openOrCreateDB("test_is_encrypted_short.db").close();
        impl.openOrCreateDB("test_is_encrypted_cipher.db").close();

        byte[] header = "SQLite format 3".getBytes("UTF-8");
        byte[] plaintext = new byte[header.length + 1];
        System.arraycopy(header, 0, plaintext, 0, header.length);
        writeDatabaseFile("test_is_encrypted_plain.db", plaintext);
        // Some bytes, but not a header. Unlike the empty file this really could be a short read of
        // something encrypted or corrupt, and the conservative answer stands.
        writeDatabaseFile("test_is_encrypted_short.db", new byte[] {'S', 'Q', 'L'});
        writeDatabaseFile("test_is_encrypted_cipher.db",
                new byte[] {(byte) 0xD9, 0x4F, (byte) 0xA3, 0x11, 0x22, 0x33, 0x44, 0x55,
                    0x66, 0x77, (byte) 0x88, (byte) 0x99, 0x01, 0x02, 0x03, 0x04});

        assertFalse(Database.isEncrypted("test_is_encrypted_plain.db"),
                "the plaintext SQLite header is plaintext");
        assertTrue(Database.isEncrypted("test_is_encrypted_short.db"),
                "a short file that is not a header stays conservative");
        assertTrue(Database.isEncrypted("test_is_encrypted_cipher.db"),
                "and ciphertext has no header at all");
    }

    @FormTest
    void adatabaseWhoseBytesCannotBeReadIsNotReportedAsPlaintext() throws IOException {
        // The direction that matters. A port can fail to answer -- the browser's engine may not
        // load at all when another tab holds its storage pool -- and the fallback here is a header
        // read that then fails too. Reporting "not encrypted" from that would tell an application
        // its encrypted database is in the clear, which is the one thing this method must never
        // get wrong; an existing database it cannot read is reported as encrypted instead.
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.openOrCreateDB("test_is_encrypted_unreadable.db").close();
        // Deliberately no file written, so the header read fails the way an unreachable store does.

        assertTrue(Database.isEncrypted("test_is_encrypted_unreadable.db"),
                "a database that exists but cannot be read is not called plaintext");
    }

    /// Puts bytes where the implementation says this database lives.
    private void writeDatabaseFile(String databaseName, byte[] content) throws IOException {
        String path = Database.getDatabasePath(databaseName);
        assertNotNull(path, "the test implementation has to report a path for " + databaseName);
        OutputStream out = FileSystemStorage.getInstance().openOutputStream(path);
        try {
            out.write(content);
        } finally {
            out.close();
        }
    }

}
