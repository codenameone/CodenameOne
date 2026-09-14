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
            assertEquals(2, notes.query().in("title", new Object[] {"low", "mid"}).count());
            // An empty set matches nothing. A filter that silently disappeared
            // would return every row in the table.
            assertEquals(0, notes.query().in("title", new Object[0]).count());
            assertEquals(3, notes.query().isNull("body").count());

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
