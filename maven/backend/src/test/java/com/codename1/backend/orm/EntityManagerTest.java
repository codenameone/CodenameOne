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
package com.codename1.backend.orm;

import com.codename1.impl.orm.Values;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Date;
import java.util.List;

import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.orm.TestEntities.Note;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ORM against a real database.
 *
 * <p>SQLite, because it needs nothing installed. The claim that the same entity
 * works on PostgreSQL and MySQL is not made here -- it is made by vm/backend's
 * ormcheck demo, which runs this same shape of assertions against real servers
 * on both runtimes. What these cover is everything that is engine independent:
 * the statements built from a definition, the values that survive a round trip,
 * the queries built from field names, and the transaction boundary.
 */
class EntityManagerTest {

    @BeforeAll
    static void registerDefinitions() {
        // What the generated cn1app.BackendDaoBootstrap does at start-up.
        EntityManager.register(new TestEntities.NoteDefinition());
        EntityManager.register(new TestEntities.TicketDefinition());
    }

    @Test
    @DisplayName("an entity round trips every type it can hold")
    void roundTripsEveryColumnType() throws Exception {
        DataSource pool = DataSource.open(":memory:");
        try {
            EntityManager em = EntityManager.open(pool);
            Dao<Note> notes = em.dao(Note.class);
            notes.createTable();

            Note note = new Note();
            note.title = "first";
            // A QUESTION MARK in a stored value: the placeholder rewrite runs on
            // every statement, and a value is not a statement.
            note.body = "how? like this";
            note.views = 3;
            note.pinned = true;
            note.score = 1.5;
            note.created = new Date(1700000000000L);
            note.payload = new byte[] {1, 2, 3};
            note.optional = null;
            notes.insert(note);
            assertTrue(note.id > 0, "the generated key is written back into the entity");

            Note back = notes.findById(Long.valueOf(note.id));
            assertNotNull(back);
            assertEquals("first", back.title);
            assertEquals("how? like this", back.body);
            assertEquals(3, back.views);
            assertTrue(back.pinned);
            assertEquals(1.5, back.score, 0.0);
            assertEquals(1700000000000L, back.created.getTime());
            assertArrayEquals(new byte[] {1, 2, 3}, back.payload);
            assertNull(back.optional, "a null column stays null in a boxed field");
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("update and delete say whether a row was there")
    void updateAndDeleteReportWhetherTheRowExisted() throws Exception {
        DataSource pool = DataSource.open(":memory:");
        try {
            EntityManager em = EntityManager.open(pool);
            Dao<Note> notes = em.dao(Note.class);
            notes.createTable();
            Note note = new Note();
            note.title = "first";
            notes.insert(note);

            note.title = "renamed";
            assertTrue(notes.update(note));
            assertEquals("renamed", notes.findById(Long.valueOf(note.id)).title);
            assertTrue(notes.delete(note));
            // The row that is not there is a case a handler turns into a 404,
            // which is why these answer rather than returning void.
            assertFalse(notes.delete(note));
            assertFalse(notes.update(note));
            assertNull(notes.findById(Long.valueOf(note.id)));
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a query names fields and the engine sees quoted columns")
    void queriesByFieldName() throws Exception {
        DataSource pool = DataSource.open(":memory:");
        try {
            EntityManager em = EntityManager.open(pool);
            Dao<Note> notes = em.dao(Note.class);
            notes.createTable();
            notes.insert(note("low", 1, false, 1000L));
            notes.insert(note("high", 10, true, 2000L));
            notes.insert(note("mid", 5, false, 3000L));

            assertEquals(3, notes.count());
            assertEquals(1, notes.query().gt("views", Integer.valueOf(5)).count());
            assertEquals(1, notes.query().eq("pinned", Boolean.TRUE).count());
            // `created` maps to a mixed-case column, which only works because the
            // generated SQL quotes it.
            assertEquals(2, notes.query().gte("created", new Date(2000L)).count());
            assertEquals(1, notes.query().like("title", "lo%").count());
            // like() is TEXT ONLY: measured, the same pattern against an int
            // column matches by coercion on SQLite and MySQL and is a type error
            // on PostgreSQL, so it is refused here rather than answering
            // differently per engine. The message names the stored type.
            IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                    () -> notes.query().like("views", "12%"));
            assertTrue(err.getMessage().contains("like() needs a text field"), err.getMessage());
            assertEquals(2, notes.query().in("title", new Object[] {"low", "mid"}).count());
            // An empty set matches nothing. A filter that silently disappeared
            // would return every row in the table.
            assertEquals(0, notes.query().in("title", new Object[0]).count());
            assertEquals(3, notes.query().isNull("body").count());

            assertThrows(IllegalArgumentException.class, () -> notes.query().limit(-1));
            assertThrows(IllegalArgumentException.class, () -> notes.query().limit(-2));
            assertThrows(IllegalArgumentException.class, () -> notes.query().offset(-1));
            assertEquals(0, notes.query().limit(0).list().size());
            assertEquals(3, notes.query().offset(0).list().size());

            List<Note> ordered = notes.query().orderBy("views", false).limit(2).list();
            assertEquals(2, ordered.size());
            assertEquals("high", ordered.get(0).title);
            assertEquals("mid", ordered.get(1).title);
            assertEquals("low", notes.query().orderBy("views", true).first().title);
            assertEquals("mid", notes.query().orderBy("views", true).offset(1).limit(1)
                    .list().get(0).title);

            // And the escape hatch, which is SQL and says so.
            assertEquals(1, notes.find("views > ?", new Object[] {Integer.valueOf(5)}).size());
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("an entity that is only a generated key can still be created")
    void storesAnEntityWithNothingButAKey() throws Exception {
        // The insert has no columns to name, and the construction that names
        // none -- INSERT INTO t () VALUES () -- is refused by SQLite and
        // PostgreSQL alike. Each engine spells this its own way.
        DataSource pool = DataSource.open(":memory:");
        try {
            EntityManager em = EntityManager.open(pool);
            Dao<TestEntities.Ticket> tickets = em.dao(TestEntities.Ticket.class);
            tickets.createTable();
            TestEntities.Ticket first = new TestEntities.Ticket();
            tickets.insert(first);
            assertTrue(first.id > 0, "the generated key is written back");
            TestEntities.Ticket second = new TestEntities.Ticket();
            tickets.insert(second);
            assertTrue(second.id > first.id, "each insert gets its own key");
            assertEquals(2, tickets.count());
            assertNotNull(tickets.findById(Long.valueOf(first.id)));
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("exact decimal text reaches a long field unrounded")
    void readsExactDecimalText() throws Exception {
        // PostgreSQL's NUMERIC and MySQL's DECIMAL come back as exact TEXT --
        // that is why those engines send text rather than a number -- and the
        // previous fallback put it through a double: 9007199254740993 came back
        // as ...992, and anything past the double range clamped silently.
        assertEquals(9007199254740993L, Values.asLong("9007199254740993", 0));
        assertEquals(9007199254740993L, Values.asLong("9007199254740993.00", 0));
        assertEquals(-9007199254740993L, Values.asLong("-9007199254740993.000", 0));
        assertEquals(12L, Values.asLong("12.", 0));
        // A real fraction in an integer field is a disagreement between the
        // entity and the table, and rounding it silently is how the wrong number
        // gets stored back.
        assertThrows(IOException.class, () -> Values.asLong("12.5", 0));
        // And out of range says so rather than clamping to Long.MAX_VALUE.
        assertThrows(IOException.class, () -> Values.asLong("92233720368547758080.00", 0));
    }

    @Test
    @DisplayName("a fractional or oversized value is refused however the engine encoded it")
    void refusesValuesThatWouldNotSurviveTheField() throws Exception {
        // SQLite has affinities rather than types: an INTEGER column holds 12.5
        // if a migration or another client put one there, and the driver returns
        // a Double. Truncating that while refusing the identical "12.5" as text
        // meant the same value was corrupted or refused depending only on how the
        // engine chose to encode it.
        assertThrows(IOException.class, () -> Values.asLong(Double.valueOf(12.5), 0));
        assertThrows(IOException.class, () -> Values.asLong(Float.valueOf(12.5f), 0));
        assertThrows(IOException.class, () -> Values.asLong("12.5", 0));
        // An integral double is an integer and passes.
        assertEquals(12L, Values.asLong(Double.valueOf(12.0), 0));
        assertThrows(IOException.class,
                () -> Values.asLong(Double.valueOf(Double.POSITIVE_INFINITY), 0));

        // And a narrowing conversion refuses rather than wrapping: 2147483648 in
        // an int field used to arrive as -2147483648.
        assertThrows(IOException.class, () -> Values.asInt(Long.valueOf(2147483648L), 0));
        assertThrows(IOException.class, () -> Values.asIntObject(Long.valueOf(-2147483649L)));
        assertThrows(IOException.class, () -> Values.asShort(Long.valueOf(32768L), (short)0));
        assertThrows(IOException.class, () -> Values.asByte(Long.valueOf(128L), (byte)0));
        // The edges themselves still fit.
        assertEquals(2147483647, Values.asInt(Long.valueOf(2147483647L), 0));
        assertEquals(-2147483648, Values.asInt(Long.valueOf(-2147483648L), 0));
        assertEquals((short)-32768, Values.asShort(Long.valueOf(-32768L), (short)0));
        assertEquals((byte)127, Values.asByte(Long.valueOf(127L), (byte)0));
    }

    @Test
    @DisplayName("findOne asks the database for one row")
    void findOneAsksForOneRow() throws Exception {
        DataSource pool = DataSource.open(":memory:");
        try {
            EntityManager em = EntityManager.open(pool);
            Dao<Note> notes = em.dao(Note.class);
            notes.createTable();
            for(int iter = 0 ; iter < 25 ; iter++) {
                notes.insert(note("n" + iter, iter, false, iter));
            }
            // The predicate matches everything; only one row may come back.
            Note one = notes.findOne("views >= ?", new Object[] {Integer.valueOf(0)});
            assertNotNull(one);
            assertTrue(one.title.startsWith("n"));
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a name that is not a field is refused, naming the ones that are")
    void refusesAnUnknownField() throws Exception {
        DataSource pool = DataSource.open(":memory:");
        try {
            EntityManager em = EntityManager.open(pool);
            Dao<Note> notes = em.dao(Note.class);
            IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                    () -> notes.query().eq("createdAt", new Date()));
            // The usual cause is the COLUMN name being used where the field name
            // belongs, so the message has to list the fields.
            assertTrue(err.getMessage().contains("created"), err.getMessage());
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a transaction commits together or not at all")
    void transactionsAreAtomic(@TempDir File dir) throws Exception {
        String path = new File(dir, "orm.db").getAbsolutePath();
        DataSource pool = DataSource.open(path, 2, 5000, 2000);
        try {
            EntityManager em = EntityManager.open(pool);
            em.dao(Note.class).createTable();

            assertThrows(IllegalStateException.class, () -> em.transaction(new EntityManager.Work() {
                public Object run(EntityManager tx) throws Exception {
                    tx.dao(Note.class).insert(note("rolled back", 1, false, 1L));
                    throw new IllegalStateException("no");
                }
            }));
            assertEquals(0, em.dao(Note.class).count());

            em.transaction(new EntityManager.Work() {
                public Object run(EntityManager tx) throws Exception {
                    tx.dao(Note.class).insert(note("committed", 1, false, 1L));
                    return null;
                }
            });
            assertEquals(1, em.dao(Note.class).count());
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a transaction inside a transaction joins it rather than opening another")
    void nestedTransactionsJoin(@TempDir File dir) throws Exception {
        // Every engine refuses a nested BEGIN, and a service method that works
        // alone should not break when another one calls it.
        String path = new File(dir, "orm.db").getAbsolutePath();
        DataSource pool = DataSource.open(path, 2, 5000, 2000);
        try {
            EntityManager em = EntityManager.open(pool);
            em.dao(Note.class).createTable();
            em.transaction(new EntityManager.Work() {
                public Object run(final EntityManager outer) throws Exception {
                    return outer.transaction(new EntityManager.Work() {
                        public Object run(EntityManager inner) throws Exception {
                            inner.dao(Note.class).insert(note("nested", 1, false, 1L));
                            return null;
                        }
                    });
                }
            });
            assertEquals(1, em.dao(Note.class).count());
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a manager over one connection still opens a real transaction")
    void transactionsWorkOverASingleConnection(@TempDir File dir) throws Exception {
        // Both kinds of manager are pinned to one connection, and telling them
        // apart is the whole of this: the one a transaction created must JOIN
        // rather than nest, and the one the CALLER opened is not in a
        // transaction at all. Treating the second like the first ran the body
        // with no BEGIN, so the writes before a failure stayed committed.
        Database db = Database.open(new File(dir, "pinned.db").getAbsolutePath());
        try {
            EntityManager em = EntityManager.open(db);
            final Dao<Note> notes = em.dao(Note.class);
            notes.createTable();
            assertThrows(IllegalStateException.class, () -> em.transaction(new EntityManager.Work() {
                public Object run(EntityManager tx) throws Exception {
                    tx.dao(Note.class).insert(note("rolled back", 1, false, 1L));
                    assertEquals(1, tx.dao(Note.class).count(), "the write is visible inside");
                    throw new IllegalStateException("no");
                }
            }));
            assertEquals(0, notes.count(), "the failed transaction left a row behind");

            em.transaction(new EntityManager.Work() {
                public Object run(EntityManager tx) throws Exception {
                    tx.dao(Note.class).insert(note("committed", 1, false, 1L));
                    // And a transaction inside it still joins rather than nesting.
                    return tx.transaction(new EntityManager.Work() {
                        public Object run(EntityManager inner) throws Exception {
                            inner.dao(Note.class).insert(note("also committed", 1, false, 1L));
                            return null;
                        }
                    });
                }
            });
            assertEquals(2, notes.count());
        } finally {
            db.close();
        }
    }

    @Test
    @DisplayName("closing a manager closes what it was opened over, and nothing it borrowed")
    void closesWhatItOwns(@TempDir File dir) throws Exception {
        // The contract is "the pool, or the connection, this manager was opened
        // over". A manager over a caller's Database used to close nothing, so
        // repeated open/use/close cycles leaked a handle each time -- while the
        // manager handed to a transaction body must close nothing at all,
        // because the connection under it belongs to a transaction that is not
        // over.
        Database db = Database.open(new File(dir, "owned.db").getAbsolutePath());
        EntityManager em = EntityManager.open(db);
        em.dao(Note.class).createTable();
        final EntityManager[] insideTransaction = new EntityManager[1];
        em.transaction(new EntityManager.Work() {
            public Object run(EntityManager tx) throws Exception {
                insideTransaction[0] = tx;
                tx.close();                       // must be a no-op
                tx.dao(Note.class).insert(note("still works", 1, false, 1L));
                return null;
            }
        });
        assertTrue(db.isOpen(), "a transaction-scoped manager closed the caller's connection");
        assertEquals(1, em.dao(Note.class).count());
        em.close();
        assertFalse(db.isOpen(), "closing the manager did not close the connection it owns");
    }

    @Test
    @DisplayName("two entities on one table is refused, naming both")
    void refusesTwoEntitiesOnOneTable() throws Exception {
        // The registry is keyed by CLASS, so both of these register and nothing
        // notices. createTables then runs CREATE TABLE IF NOT EXISTS for the
        // first and skips the second, and the second dao selects columns that
        // were never created -- surfacing as a missing column on a query, far
        // from the two classes that explain it. foo.User and bar.User both
        // defaulting to "User" is the ordinary way to arrive here.
        //
        // Their names differ in CASE, which still counts: the ORM quotes every
        // identifier, so these are two tables on PostgreSQL and one on a MySQL
        // configured the usual way for macOS or Windows -- an entity pair that
        // works on one engine and not another, which is the whole failure this
        // layer exists to prevent.
        EntityManager.register(new TestEntities.SharedADefinition());
        EntityManager.register(new TestEntities.SharedBDefinition());
        try {
            DataSource pool = DataSource.open(":memory:");
            try {
                IOException err = assertThrows(IOException.class,
                        () -> EntityManager.open(pool));
                assertTrue(err.getMessage().contains("SharedA"), err.getMessage());
                assertTrue(err.getMessage().contains("SharedB"), err.getMessage());
                // And it says what to do about it.
                assertTrue(err.getMessage().contains("@Entity(table"), err.getMessage());
            } finally {
                pool.close();
            }
        } finally {
            EntityManager.forgetForTest(TestEntities.SharedA.class);
            EntityManager.forgetForTest(TestEntities.SharedB.class);
        }
    }

    @Test
    @DisplayName("two fields on one column is refused, naming both")
    void refusesTwoFieldsOnOneColumn() throws Exception {
        // Every statement would name the column twice: the CREATE TABLE is
        // refused outright, and against a schema the ORM did not create, a read
        // puts the same value into both fields and a write stores whichever the
        // generated order put last. The generator refuses this at build time --
        // see OrmAnnotationProcessorTest -- and a hand-written definition, which
        // is what this is, reaches the runtime check.
        EntityManager.register(new TestEntities.ClashingDefinition());
        try {
            DataSource pool = DataSource.open(":memory:");
            try {
                // An IOException rather than the IllegalStateException the table
                // itself throws: tablesFor COLLECTS every entity it cannot map,
                // so opening a server reports all of them at once instead of one
                // per restart.
                IOException err = assertThrows(IOException.class,
                        () -> EntityManager.open(pool));
                assertTrue(err.getMessage().contains("label"), err.getMessage());
                assertTrue(err.getMessage().contains("name"), err.getMessage());
                assertTrue(err.getMessage().contains("title"), err.getMessage());
            } finally {
                pool.close();
            }
        } finally {
            EntityManager.forgetForTest(TestEntities.Clashing.class);
        }
    }

    @Test
    @DisplayName("an entity with no dao says which of the two things went wrong")
    void reportsAnUnregisteredEntity() throws Exception {
        DataSource pool = DataSource.open(":memory:");
        try {
            EntityManager em = EntityManager.open(pool);
            IllegalStateException err = assertThrows(IllegalStateException.class,
                    () -> em.dao(String.class));
            assertTrue(err.getMessage().contains("@Entity"), err.getMessage());
            assertTrue(err.getMessage().contains("BackendDaoBootstrap"), err.getMessage());
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("an entity manager over one connection works the same way")
    void worksOverASingleConnection() throws Exception {
        Database db = Database.open(":memory:");
        try {
            EntityManager em = EntityManager.open(db);
            Dao<Note> notes = em.dao(Note.class);
            notes.createTable();
            notes.insert(note("only", 1, false, 1L));
            assertEquals(1, notes.count());
            assertNull(em.dataSource());
            assertEquals(db, em.database());
        } finally {
            db.close();
        }
    }

    private static Note note(String title, int views, boolean pinned, long created) {
        Note out = new Note();
        out.title = title;
        out.views = views;
        out.pinned = pinned;
        out.created = new Date(created);
        return out;
    }
}
