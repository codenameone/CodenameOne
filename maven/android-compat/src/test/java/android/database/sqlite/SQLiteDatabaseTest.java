/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package android.database.sqlite;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SQLiteDatabaseTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private SQLiteDatabase db;
    private final StringBuilder log = new StringBuilder();

    @Before
    public void open() throws Exception {
        // Codename One's default log needs a running implementation; this
        // one records what the database layer reports instead.
        com.codename1.io.Log.install(new com.codename1.io.Log() {
            @Override
            protected void print(String text, int level) {
                log.append(text).append('\n');
            }

            @Override
            protected void logThrowable(Throwable t) {
                log.append("Exception: ").append(t.getMessage()).append('\n');
            }
        });
        db = SQLiteDatabase.wrap(new JdbcDatabase(), "test.db", null, null);
        db.execSQL("CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT UNIQUE NOT NULL, "
                + "rating REAL, body BLOB)");
    }

    @After
    public void close() {
        if (db.isOpen()) {
            db.close();
        }
    }

    private static ContentValues note(String title, double rating) {
        ContentValues v = new ContentValues();
        v.put("title", title);
        v.put("rating", Double.valueOf(rating));
        return v;
    }

    @Test
    public void insertQueryUpdateDelete() {
        assertEquals(1, db.insert("notes", null, note("first", 1.5)));
        assertEquals(2, db.insert("notes", null, note("second", 4)));
        Cursor c = db.query("notes", new String[] {"_id", "title", "rating", "body"}, "rating > ?",
                new String[] {"1"}, null, null, "title DESC");
        assertEquals(2, c.getCount());
        assertTrue(c.moveToLast());
        assertEquals("first", c.getString(c.getColumnIndexOrThrow("TITLE")));
        assertEquals(1.5, c.getDouble(2), 0);
        assertEquals(1, c.getInt(2));
        assertTrue(c.isNull(3));
        assertEquals(Cursor.FIELD_TYPE_NULL, c.getType(3));
        assertEquals(Cursor.FIELD_TYPE_FLOAT, c.getType(2));
        assertTrue(c.moveToPrevious());
        assertEquals(2, c.getLong(0));
        c.close();

        ContentValues change = new ContentValues();
        change.put("rating", Integer.valueOf(5));
        assertEquals(2, db.update("notes", change, null, null));
        assertEquals(1, db.delete("notes", "title = ?", new String[] {"second"}));
        assertEquals(1, DatabaseUtils.queryNumEntries(db, "notes"));
        assertEquals(5, DatabaseUtils.longForQuery(db, "SELECT rating FROM notes", null));
    }

    @Test
    public void conflictsFollowAndroid() {
        db.insert("notes", null, note("dup", 1));
        assertEquals(-1, db.insert("notes", null, note("dup", 2)));
        assertTrue(log.toString(), log.indexOf("Error inserting title=dup rating=2.0") >= 0);
        try {
            db.insertOrThrow("notes", null, note("dup", 2));
            fail();
        } catch (SQLiteConstraintException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().indexOf("UNIQUE") >= 0);
        }
        assertEquals(-1, db.insertWithOnConflict("notes", null, note("dup", 3), SQLiteDatabase.CONFLICT_IGNORE));
        long replaced = db.insertWithOnConflict("notes", null, note("dup", 9), SQLiteDatabase.CONFLICT_REPLACE);
        assertEquals(1, DatabaseUtils.queryNumEntries(db, "notes"));
        assertEquals(replaced, DatabaseUtils.longForQuery(db, "SELECT _id FROM notes WHERE title='dup'", null));
        assertEquals("9.0", DatabaseUtils.stringForQuery(db, "SELECT rating FROM notes WHERE title='dup'", null));
    }

    @Test
    public void nullColumnHackInsertsAnEmptyRow() {
        db.execSQL("CREATE TABLE log (id INTEGER PRIMARY KEY, msg TEXT)");
        assertEquals(1, db.insert("log", "msg", new ContentValues()));
        Cursor c = db.rawQuery("SELECT msg FROM log", null);
        assertTrue(c.moveToFirst());
        assertNull(c.getString(0));
        c.close();
    }

    @Test
    public void nestedTransactionRollsBackWhenAnInnerLevelFails() {
        db.beginTransaction();
        db.insert("notes", null, note("outer", 1));
        db.beginTransaction();
        db.insert("notes", null, note("inner", 1));
        db.endTransaction();
        db.setTransactionSuccessful();
        db.endTransaction();
        assertFalse(db.inTransaction());
        assertEquals(0, DatabaseUtils.queryNumEntries(db, "notes"));

        db.beginTransaction();
        db.beginTransaction();
        db.insert("notes", null, note("both", 1));
        db.setTransactionSuccessful();
        db.endTransaction();
        db.setTransactionSuccessful();
        db.endTransaction();
        assertEquals(1, DatabaseUtils.queryNumEntries(db, "notes"));
    }

    @Test
    public void transactionMisuseThrows() {
        try {
            db.endTransaction();
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().startsWith("Cannot perform this operation because there is no"));
        }
        db.beginTransaction();
        db.setTransactionSuccessful();
        try {
            db.setTransactionSuccessful();
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().indexOf("already been marked successful") > 0);
        }
        db.endTransaction();
    }

    @Test
    public void transactionListenerIsTold() {
        final List<String> events = new ArrayList<String>();
        SQLiteTransactionListener l = new SQLiteTransactionListener() {
            @Override
            public void onBegin() {
                events.add("begin");
            }

            @Override
            public void onCommit() {
                events.add("commit");
            }

            @Override
            public void onRollback() {
                events.add("rollback");
            }
        };
        db.beginTransactionWithListener(l);
        db.endTransaction();
        db.beginTransactionWithListener(l);
        db.setTransactionSuccessful();
        db.endTransaction();
        assertEquals("[begin, rollback, begin, commit]", events.toString());
    }

    @Test
    public void compiledStatements() {
        SQLiteStatement insert = db.compileStatement("INSERT INTO notes (title, rating) VALUES (?, ?)");
        insert.bindString(1, "a");
        insert.bindDouble(2, 2);
        assertEquals(1, insert.executeInsert());
        insert.clearBindings();
        insert.bindString(1, "b");
        insert.bindNull(2);
        assertEquals(2, insert.executeInsert());
        try {
            insert.bindLong(3, 1);
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().startsWith("Cannot bind argument at index 3"));
        }
        insert.close();
        SQLiteStatement count = db.compileStatement("SELECT count(*) FROM notes WHERE rating IS NULL");
        assertEquals(1, count.simpleQueryForLong());
        SQLiteStatement none = db.compileStatement("SELECT title FROM notes WHERE 0");
        try {
            none.simpleQueryForString();
            fail();
        } catch (SQLiteDoneException expected) {
            // no rows: Android throws exactly this
        }
        SQLiteStatement del = db.compileStatement("DELETE FROM notes");
        assertEquals(2, del.executeUpdateDelete());
    }

    @Test
    public void blobsRoundTrip() {
        byte[] body = {0, 1, (byte) 0xff, (byte) 0x80, 'x', 0};
        ContentValues v = note("blob", 1);
        v.put("body", body);
        db.insert("notes", null, v);
        db.insert("notes", null, note("other", 2));
        Cursor c = db.rawQuery("SELECT body FROM notes ORDER BY _id", null);
        assertTrue(c.moveToLast());
        assertNull(c.getBlob(0));
        assertTrue(c.moveToFirst());
        assertArrayEquals(body, c.getBlob(0));
        c.close();
        db.execSQL("UPDATE notes SET body = ? WHERE title = ?", new Object[] {new byte[] {7}, "other"});
        c = db.rawQuery("SELECT body FROM notes WHERE title = 'other'", null);
        c.moveToFirst();
        assertArrayEquals(new byte[] {7}, c.getBlob(0));
        c.close();
    }

    @Test
    public void badSqlFailsWhenTheQueryIsMade() {
        try {
            db.rawQuery("SELEC nonsense", null);
            fail();
        } catch (SQLiteException expected) {
            assertTrue(expected.getMessage().indexOf("SELEC nonsense") > 0);
        }
    }

    @Test
    public void requerySeesNewRows() {
        Cursor c = db.rawQuery("SELECT title FROM notes", null);
        assertEquals(0, c.getCount());
        db.insert("notes", null, note("late", 1));
        assertTrue(c.requery());
        assertEquals(1, c.getCount());
        c.close();
        assertTrue(c.isClosed());
    }

    @Test
    public void versionPragma() {
        assertEquals(0, db.getVersion());
        db.setVersion(7);
        assertEquals(7, db.getVersion());
        assertTrue(db.needUpgrade(8));
    }

    @Test
    public void closedDatabaseRefusesWork() {
        db.close();
        assertFalse(db.isOpen());
        try {
            db.rawQuery("SELECT 1", null);
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().startsWith("attempt to re-open an already-closed object"));
        }
    }

    /// The helper's create, upgrade and downgrade flow, over one engine
    /// that outlives the helpers the way a database file would.
    @Test
    public void openHelperCreatesUpgradesAndRefusesDowngrade() throws Exception {
        final JdbcDatabase engine = new JdbcDatabase();
        Context ctx = new ContextWrapper(null) {
            @Override
            public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory,
                                                       DatabaseErrorHandler errorHandler) {
                return SQLiteDatabase.wrap(engine, name, factory, errorHandler);
            }
        };
        final List<String> calls = new ArrayList<String>();
        SQLiteOpenHelper v1 = new Helper(ctx, 1, calls);
        SQLiteDatabase d1 = v1.getWritableDatabase();
        assertTrue(d1 == v1.getReadableDatabase());
        assertEquals(1, d1.getVersion());
        Helper v2 = new Helper(ctx, 2, calls);
        assertEquals(2, v2.getWritableDatabase().getVersion());
        assertEquals("[configure, create, open, configure, upgrade 1->2, open]", calls.toString());
        Helper v1again = new Helper(ctx, 1, calls);
        try {
            v1again.getWritableDatabase();
            fail();
        } catch (SQLiteException expected) {
            assertEquals("Can't downgrade database from version 2 to 1", expected.getMessage());
        }
    }

    /// A database older than the minimum supported version is handed to
    /// `onBeforeDelete` while it is still the old one, so the application
    /// can read or export it -- not the empty replacement.
    @Test
    public void onBeforeDeleteSeesTheObsoleteDatabase() throws Exception {
        final JdbcDatabase[] engine = {new JdbcDatabase()};
        Context ctx = new ContextWrapper(null) {
            @Override
            public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory,
                                                       DatabaseErrorHandler errorHandler) {
                return SQLiteDatabase.wrap(engine[0], name, factory, errorHandler);
            }

            @Override
            public boolean deleteDatabase(String name) {
                try {
                    engine[0] = new JdbcDatabase();
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(e);
                }
                return true;
            }
        };
        SQLiteDatabase old = new Helper(ctx, 1, new ArrayList<String>()).getWritableDatabase();
        old.execSQL("INSERT INTO t (x) VALUES ('legacy')");
        final List<String> calls = new ArrayList<String>();
        SQLiteOpenHelper v3 = new SQLiteOpenHelper(ctx, "helper.db", null, 3, 2, null) {
            @Override
            public void onBeforeDelete(SQLiteDatabase db) {
                calls.add("beforeDelete v" + db.getVersion() + " "
                        + DatabaseUtils.stringForQuery(db, "SELECT x FROM t", null));
            }

            @Override
            public void onCreate(SQLiteDatabase db) {
                calls.add("create v" + db.getVersion());
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                calls.add("upgrade");
            }
        };
        assertEquals(3, v3.getWritableDatabase().getVersion());
        assertEquals("[beforeDelete v1 legacy, create v0]", calls.toString());
    }

    /// A transaction belongs to the thread that began it. Another thread's
    /// `beginTransaction` used to become a nested level of this one, so its
    /// `endTransaction` popped this thread's level and its work committed or
    /// rolled back with ours.
    @Test
    public void anotherThreadDoesNotNestIntoThisThreadsTransaction() throws Exception {
        db.beginTransaction();
        final boolean[] otherSawTransaction = new boolean[1];
        final Throwable[] otherFailure = new Throwable[1];
        Thread other = new Thread(new Runnable() {
            @Override
            public void run() {
                otherSawTransaction[0] = db.inTransaction();
                try {
                    db.beginTransaction();
                    db.endTransaction();
                } catch (Throwable t) {
                    otherFailure[0] = t;
                }
            }
        });
        other.start();
        other.join();
        assertFalse("this thread's transaction is not the other thread's", otherSawTransaction[0]);
        assertTrue(String.valueOf(otherFailure[0]), otherFailure[0] instanceof SQLiteDatabaseLockedException);
        assertTrue(db.inTransaction());
        db.insert("notes", null, note("mine", 1));
        db.setTransactionSuccessful();
        db.endTransaction();
        assertFalse(db.inTransaction());

        // Once it ended, the other thread has transactions of its own.
        Thread later = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    db.beginTransaction();
                    try {
                        db.insert("notes", null, note("theirs", 2));
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                } catch (Throwable t) {
                    otherFailure[0] = t;
                }
            }
        });
        otherFailure[0] = null;
        later.start();
        later.join();
        assertNull(otherFailure[0]);
        assertEquals(2, DatabaseUtils.queryNumEntries(db, "notes"));
    }

    /// A database opened `OPEN_READONLY` refuses writes through the Android
    /// API. It used to report `isReadOnly()` and still insert, update, drop
    /// tables and bump the version.
    @Test
    public void readOnlyDatabaseRefusesWrites() throws Exception {
        JdbcDatabase engine = new JdbcDatabase();
        SQLiteDatabase rw = SQLiteDatabase.wrap(engine, "ro.db", null, null);
        rw.execSQL("CREATE TABLE t (v INTEGER)");
        rw.execSQL("INSERT INTO t VALUES (1)");
        SQLiteDatabase ro = SQLiteDatabase.wrap(engine, "ro.db", SQLiteDatabase.OPEN_READONLY, null, null);
        assertTrue(ro.isReadOnly());
        ContentValues v = new ContentValues();
        v.put("v", Integer.valueOf(2));
        try {
            ro.insertOrThrow("t", null, v);
            fail("insert into a read-only database");
        } catch (SQLiteReadOnlyDatabaseException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().indexOf("readonly") >= 0);
        }
        assertEquals(-1, ro.insert("t", null, v));
        try {
            ro.update("t", v, null, null);
            fail("update in a read-only database");
        } catch (SQLiteReadOnlyDatabaseException expected) {
            // refused
        }
        try {
            ro.delete("t", null, null);
            fail("delete in a read-only database");
        } catch (SQLiteReadOnlyDatabaseException expected) {
            // refused
        }
        try {
            ro.execSQL("DROP TABLE t");
            fail("schema change in a read-only database");
        } catch (SQLiteReadOnlyDatabaseException expected) {
            // refused
        }
        try {
            ro.setVersion(7);
            fail("version change in a read-only database");
        } catch (SQLiteReadOnlyDatabaseException expected) {
            // refused
        }
        // Reads, settings pragmas and transactions still work.
        ro.setForeignKeyConstraintsEnabled(true);
        ro.beginTransaction();
        ro.endTransaction();
        assertEquals(0, ro.getVersion());
        assertEquals(1, DatabaseUtils.longForQuery(ro, "SELECT COUNT(*) FROM t", null));
        assertEquals(1, DatabaseUtils.longForQuery(ro, "SELECT v FROM t", null));
        rw.close();
    }

    private static final class Helper extends SQLiteOpenHelper {
        private final List<String> calls;

        Helper(Context ctx, int version, List<String> calls) {
            super(ctx, "helper.db", null, version);
            this.calls = calls;
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            calls.add("configure");
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            calls.add("create");
            db.execSQL("CREATE TABLE IF NOT EXISTS t (x)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            calls.add("upgrade " + oldVersion + "->" + newVersion);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            calls.add("open");
        }
    }
}
