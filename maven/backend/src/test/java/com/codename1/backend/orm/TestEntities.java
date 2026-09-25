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
import java.util.Date;

import com.codename1.backend.sql.Dialect;

/**
 * A test entity and the definition the build would generate for it.
 *
 * <p>Written by hand HERE, and generated for real in the maven plugin's
 * OrmAnnotationProcessorTest and exercised end to end by vm/backend's ormcheck
 * demo: this module has no annotation processor, and what these tests are about
 * is the runtime that turns a definition into statements rather than the
 * generator that writes one. The shape is exactly what the generator emits, so a
 * change on either side that makes them disagree fails one of the two.
 */
final class TestEntities {
    private TestEntities() {
    }

    /** The entity, as a developer writes it: public fields, no-arg constructor. */
    public static final class Note {
        public long id;
        public String title;
        public String body;
        public int views;
        public boolean pinned;
        public double score;
        public Date created;
        public byte[] payload;
        public Long optional;

        public Note() {
        }
    }

    /**
     * An entity whose ONLY persisted field is a key the database generates.
     *
     * <p>Legal, and the case the obvious insert construction cannot express:
     * with no columns to name it builds "INSERT INTO t () VALUES ()", which
     * SQLite and PostgreSQL both refuse.
     */
    public static final class Ticket {
        public long id;

        public Ticket() {
        }
    }

    /** What `@Entity` on Ticket generates. */
    static final class TicketDefinition extends EntityDefinition {
        private static final ColumnDefinition[] COLUMNS = {
            new ColumnDefinition("id", "id", Dialect.BIGINT, false, null, true, true),
        };

        public Class type() {
            return Ticket.class;
        }

        public String table() {
            return "tickets";
        }

        public ColumnDefinition[] columns() {
            return COLUMNS;
        }

        public Object newInstance() {
            return new Ticket();
        }

        public Object get(Object entity, int index) {
            return index == 0 ? Long.valueOf(((Ticket)entity).id) : null;
        }

        public void set(Object entity, int index, Object value) throws java.io.IOException {
            if(index == 0) {
                ((Ticket)entity).id = Values.asLong(value, 0L);
            }
        }
    }

    /** The entity the clashing definition below describes. Its own class, so
     * removing that definition cannot disturb another entity's registration. */
    public static final class Clashing {
        public long id;
        public String name;
        public String title;

        public Clashing() {
        }
    }

    /** A definition that maps two fields to one column, which is a mistake. */
    static final class ClashingDefinition extends EntityDefinition {
        private static final ColumnDefinition[] COLUMNS = {
            new ColumnDefinition("id", "id", Dialect.BIGINT, false, null, true, true),
            new ColumnDefinition("name", "label", Dialect.TEXT, true, null, false, false),
            new ColumnDefinition("title", "label", Dialect.TEXT, true, null, false, false),
        };

        public Class type() {
            return Clashing.class;
        }

        public String table() {
            return "clashing";
        }

        public ColumnDefinition[] columns() {
            return COLUMNS;
        }

        public Object newInstance() {
            return new Clashing();
        }

        public Object get(Object entity, int index) {
            return null;
        }

        public void set(Object entity, int index, Object value) {
        }
    }

    /** What `@Entity` on Note generates. */
    static final class NoteDefinition extends EntityDefinition {
        private static final ColumnDefinition[] COLUMNS = {
            new ColumnDefinition("id", "id", Dialect.BIGINT, false, null, true, true),
            new ColumnDefinition("title", "title", Dialect.TEXT, false, null, false, false),
            new ColumnDefinition("body", "body", Dialect.TEXT, true, null, false, false),
            new ColumnDefinition("views", "views", Dialect.INTEGER, true, null, false, false),
            new ColumnDefinition("pinned", "pinned", Dialect.BOOLEAN, true, null, false, false),
            new ColumnDefinition("score", "score", Dialect.REAL, true, null, false, false),
            // A MIXED-CASE column name, deliberately: PostgreSQL folds an
            // unquoted one to lower case, so this is the column that proves the
            // generated SQL quotes what it names.
            new ColumnDefinition("created", "createdAt", Dialect.TIMESTAMP, true, null, false, false),
            new ColumnDefinition("payload", "payload", Dialect.BLOB, true, null, false, false),
            new ColumnDefinition("optional", "optional", Dialect.BIGINT, true, null, false, false),
        };

        public Class type() {
            return Note.class;
        }

        public String table() {
            return "notes";
        }

        public ColumnDefinition[] columns() {
            return COLUMNS;
        }

        public Object newInstance() {
            return new Note();
        }

        public Object get(Object entity, int index) {
            Note e = (Note)entity;
            switch(index) {
                case 0: return Long.valueOf(e.id);
                case 1: return e.title;
                case 2: return e.body;
                case 3: return Long.valueOf(e.views);
                case 4: return Long.valueOf(e.pinned ? 1L : 0L);
                case 5: return Double.valueOf(e.score);
                case 6: return e.created == null ? null : Long.valueOf(e.created.getTime());
                case 7: return e.payload;
                case 8: return e.optional == null ? null : Long.valueOf(e.optional.longValue());
                default: return null;
            }
        }

        public void set(Object entity, int index, Object value) throws java.io.IOException {
            Note e = (Note)entity;
            switch(index) {
                case 0: e.id = Values.asLong(value, 0L); return;
                case 1: e.title = Values.asString(value); return;
                case 2: e.body = Values.asString(value); return;
                case 3: e.views = Values.asInt(value, 0); return;
                case 4: e.pinned = Values.asBoolean(value, false); return;
                case 5: e.score = Values.asDouble(value, 0); return;
                case 6: e.created = Values.asDate(value); return;
                case 7: e.payload = Values.asBytes(value); return;
                case 8: e.optional = Values.asLongObject(value); return;
                default: return;
            }
        }
    }

    /** Two entities, two classes, ONE table -- which is the mistake. */
    public static final class SharedA {
        public long id;
        public SharedA() {
        }
    }

    public static final class SharedB {
        public long id;
        public SharedB() {
        }
    }

    /** Spelled in different CASES on purpose: the engines disagree about whether
     * an unquoted name folds, and the ORM quotes every identifier, so "Shared"
     * and "shared" are two tables on PostgreSQL and one on a MySQL configured
     * the usual way for macOS or Windows. An entity pair that works on one
     * engine and not another is what this refusal exists to stop. */
    static final class SharedADefinition extends EntityDefinition {
        private static final ColumnDefinition[] COLUMNS = {
            new ColumnDefinition("id", "id", Dialect.BIGINT, false, null, true, true),
        };

        public Class type() {
            return SharedA.class;
        }

        public String table() {
            return "Shared";
        }

        public ColumnDefinition[] columns() {
            return COLUMNS;
        }

        public Object newInstance() {
            return new SharedA();
        }

        public Object get(Object entity, int index) {
            return null;
        }

        public void set(Object entity, int index, Object value) {
        }
    }

    static final class SharedBDefinition extends EntityDefinition {
        private static final ColumnDefinition[] COLUMNS = {
            new ColumnDefinition("id", "id", Dialect.BIGINT, false, null, true, true),
        };

        public Class type() {
            return SharedB.class;
        }

        public String table() {
            return "shared";
        }

        public ColumnDefinition[] columns() {
            return COLUMNS;
        }

        public Object newInstance() {
            return new SharedB();
        }

        public Object get(Object entity, int index) {
            return null;
        }

        public void set(Object entity, int index, Object value) {
        }
    }
}
