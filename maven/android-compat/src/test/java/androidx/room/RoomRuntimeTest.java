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
package androidx.room;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.JdbcContexts;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.codename1.androidcompat.runtime.RoomSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/// The Room runtime with a hand-written stand-in for what the build
/// generates: opening, migrations, the identity check, transactions and
/// live queries.
public class RoomRuntimeTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private Context ctx;

    @Before
    public void installLog() {
        com.codename1.io.Log.install(new com.codename1.io.Log() {
            @Override
            protected void print(String text, int level) {
            }
        });
        ctx = JdbcContexts.create();
    }

    /// What `@Database(version = n)` over one `note` table would generate.
    static class NotesDb extends RoomDatabase {
        final int version;
        final String extraColumn;

        NotesDb(int version, String extraColumn) {
            this.version = version;
            this.extraColumn = extraColumn;
        }

        @Override
        protected int roomSchemaVersion() {
            return version;
        }

        @Override
        protected String[] roomCreateStatements() {
            return new String[] {"CREATE TABLE IF NOT EXISTS `note` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + " `title` TEXT" + (extraColumn == null ? "" : ", `" + extraColumn + "` INTEGER") + ")"};
        }

        @Override
        protected String[] roomTableNames() {
            return new String[] {"note"};
        }

        @Override
        protected String[][] roomTableColumns() {
            return extraColumn == null ? new String[][] {{"note", "id", "title"}}
                    : new String[][] {{"note", "id", "title", extraColumn}};
        }

        long insert(String title) {
            long id = RoomSupport.insert(this, "INSERT INTO `note` (`title`) VALUES (?)", new Object[] {title});
            getInvalidationTracker().notifyTablesChanged("note");
            return id;
        }

        List<String> titles() {
            Cursor c = RoomSupport.query(this, "SELECT title FROM note ORDER BY id", new ArrayList<Object>());
            try {
                List<String> out = new ArrayList<String>();
                while (c.moveToNext()) {
                    out.add(c.getString(0));
                }
                return out;
            } finally {
                c.close();
            }
        }
    }

    private NotesDb open(int version, String extra, RoomDatabase.Builder<NotesDb> b) {
        return b.allowMainThreadQueries().buildWith(new NotesDb(version, extra));
    }

    private RoomDatabase.Builder<NotesDb> builder() {
        return Room.databaseBuilder(ctx, NotesDb.class, "notes.db");
    }

    @Test
    public void createsTablesOnFirstOpenAndReadsBack() {
        NotesDb db = open(1, null, builder());
        assertEquals(1L, db.insert("a"));
        db.insert("b");
        assertEquals(Arrays.asList("a", "b"), db.titles());
        db.close();
        NotesDb again = open(1, null, builder());
        assertEquals(Arrays.asList("a", "b"), again.titles());
    }

    @Test
    public void buildWithoutGeneratedImplementationFails() {
        try {
            builder().build();
            fail();
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("NotesDb_Impl does not exist"));
        }
    }

    @Test
    public void runsMigrationsOnVersionChange() {
        open(1, null, builder()).insert("kept");
        final List<String> ran = new ArrayList<String>();
        Migration m12 = new Migration(1, 2) {
            @Override
            public void migrate(SupportSQLiteDatabase database) {
                ran.add("1-2");
                database.execSQL("ALTER TABLE note ADD COLUMN `stars` INTEGER");
            }
        };
        NotesDb v2 = open(2, "stars", builder().addMigrations(m12));
        // Opening is lazy, as in Room: the first access migrates.
        assertTrue(ran.isEmpty());
        assertEquals(Arrays.asList("kept"), v2.titles());
        assertEquals(Arrays.asList("1-2"), ran);
        assertEquals(2, v2.getOpenHelper().getWritableDatabase().getVersion());
    }

    /// A migration that forgets a column the entity now has is refused, and
    /// the database is not stamped with the new version.
    @Test
    public void migrationThatMissesAColumnIsRefused() {
        open(1, null, builder()).insert("kept");
        Migration forgetful = new Migration(1, 2) {
            @Override
            public void migrate(SupportSQLiteDatabase database) {
            }
        };
        try {
            open(2, "stars", builder().addMigrations(forgetful)).titles();
            fail("a migration that left out `stars` was accepted");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("Migration didn't properly handle: note"));
            assertTrue(expected.getMessage(), expected.getMessage().contains("stars"));
        }
        assertEquals(Arrays.asList("kept"), open(1, null, builder()).titles());
    }

    @Test
    public void missingMigrationThrowsUnlessDestructive() {
        open(1, null, builder()).insert("x");
        try {
            open(2, "stars", builder()).titles();
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("A migration from 1 to 2 was required"));
        }
        NotesDb wiped = open(2, "stars", builder().fallbackToDestructiveMigration());
        assertEquals(new ArrayList<String>(), wiped.titles());
    }

    @Test
    public void schemaChangeWithoutVersionBumpIsRefused() {
        open(1, null, builder()).insert("x");
        try {
            open(1, "stars", builder()).titles();
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("Room cannot verify the data integrity"));
        }
    }

    @Test
    public void migrationPathChoosesTheLongestStep() {
        RoomDatabase.MigrationContainer c = new RoomDatabase.MigrationContainer();
        Migration m12 = noop(1, 2);
        Migration m23 = noop(2, 3);
        Migration m13 = noop(1, 3);
        Migration m34 = noop(3, 4);
        c.addMigrations(m12, m23, m13, m34);
        assertEquals(Arrays.asList(m13, m34), c.findMigrationPath(1, 4));
        assertEquals(Arrays.asList(m23), c.findMigrationPath(2, 3));
        assertNull(c.findMigrationPath(4, 1));
    }

    private static Migration noop(int a, int b) {
        return new Migration(a, b) {
            @Override
            public void migrate(SupportSQLiteDatabase database) {
            }
        };
    }

    @Test
    public void transactionRollsBackAndDefersInvalidation() {
        final NotesDb db = open(1, null, builder());
        final List<String> seen = new ArrayList<String>();
        db.getInvalidationTracker().addObserver(new InvalidationTracker.Observer("NOTE") {
            @Override
            public void onInvalidated(java.util.Set<String> tables) {
                seen.add(tables.toString());
            }
        });
        try {
            db.runInTransaction(new Runnable() {
                @Override
                public void run() {
                    db.insert("gone");
                    assertTrue(seen.isEmpty());
                    throw new IllegalArgumentException("boom");
                }
            });
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("boom", expected.getMessage());
        }
        assertEquals(new ArrayList<String>(), db.titles());
        assertEquals(Arrays.asList("[note]"), seen);
        Integer n = db.runInTransaction(new Callable<Integer>() {
            @Override
            public Integer call() {
                db.insert("kept");
                return Integer.valueOf(db.titles().size());
            }
        });
        assertEquals(1, n.intValue());
        assertEquals(2, seen.size());
    }

    @Test
    public void liveQueryRefreshesOnWrite() {
        // Queries run on the query executor and post their value, as in
        // AndroidX; a direct executor and a drain make each step observable.
        final NotesDb db = open(1, null, builder().setQueryExecutor(new java.util.concurrent.Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        }));
        LiveData<List<String>> live = db.getInvalidationTracker().createLiveData(new String[] {"note"},
                false, new Callable<List<String>>() {
                    @Override
                    public List<String> call() {
                        return db.titles();
                    }
                });
        final List<List<String>> values = new ArrayList<List<String>>();
        Observer<List<String>> obs = new Observer<List<String>>() {
            @Override
            public void onChanged(List<String> v) {
                values.add(v);
            }
        };
        live.observeForever(obs);
        MainThreadRule.drain();
        db.insert("one");
        MainThreadRule.drain();
        db.insert("two");
        MainThreadRule.drain();
        live.removeObserver(obs);
        db.insert("three");
        MainThreadRule.drain();
        assertEquals(3, values.size());
        assertEquals(Arrays.asList("one", "two"), values.get(2));
    }

    @Test
    public void clearAllTablesEmptiesEveryEntity() {
        NotesDb db = open(1, null, builder());
        db.insert("a");
        db.clearAllTables();
        assertEquals(new ArrayList<String>(), db.titles());
    }
}
