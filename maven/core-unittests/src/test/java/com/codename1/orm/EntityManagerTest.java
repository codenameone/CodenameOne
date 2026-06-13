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
package com.codename1.orm;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for {@link EntityManager}: the open factories and their validation,
 * the dao registry lookup (including the unregistered-class failure), the
 * transaction delegation to the wrapped {@link Database}, and the idempotent
 * close. Uses a hand-written recording {@code Database} and {@code Dao} (no
 * Mockito); the dao is keyed on a dedicated entity class so it cannot collide
 * with anything else in the process-global registry.
 */
class EntityManagerTest extends UITestBase {

    /** Dedicated entity type so the static dao registry stays collision-free. */
    private static final class SampleEntity {
    }

    /** Records the transaction / close calls EntityManager makes. */
    private static final class RecordingDatabase extends Database {
        int begin;
        int commit;
        int rollback;
        int close;

        public void beginTransaction() throws IOException {
            begin++;
        }

        public void commitTransaction() throws IOException {
            commit++;
        }

        public void rollbackTransaction() throws IOException {
            rollback++;
        }

        public void close() throws IOException {
            close++;
        }

        public void execute(String sql) throws IOException {
        }

        public void execute(String sql, String[] params) throws IOException {
        }

        public Cursor executeQuery(String sql, String[] params) throws IOException {
            return null;
        }

        public Cursor executeQuery(String sql) throws IOException {
            return null;
        }
    }

    /** Hand-written dao that records the database it was attached to. */
    private static final class RecordingDao implements Dao<SampleEntity> {
        Database attached;

        public Class<SampleEntity> type() {
            return SampleEntity.class;
        }

        public String tableName() {
            return "SampleEntity";
        }

        public void attach(Database db) {
            this.attached = db;
        }

        public void createTable() throws IOException {
        }

        public void dropTable() throws IOException {
        }

        public void insert(SampleEntity entity) throws IOException {
        }

        public void update(SampleEntity entity) throws IOException {
        }

        public void delete(SampleEntity entity) throws IOException {
        }

        public SampleEntity findById(Object id) throws IOException {
            return null;
        }

        public List<SampleEntity> findAll() throws IOException {
            return Collections.emptyList();
        }

        public List<SampleEntity> find(String where, Object... params) throws IOException {
            return Collections.emptyList();
        }
    }

    @Test
    void openNullDatabaseThrows() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                EntityManager.open((Database) null);
            }
        });
    }

    @Test
    void openWrapsTheGivenDatabase() {
        RecordingDatabase db = new RecordingDatabase();
        EntityManager em = EntityManager.open(db);
        assertSame(db, em.database());
    }

    @Test
    void openByNameUsesThePlatformDatabase() throws Exception {
        EntityManager em = EntityManager.open("EntityManagerTest.db");
        assertNotNull(em.database());
        em.close();
    }

    @Test
    void registerDaoNullThrows() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                EntityManager.registerDao(null);
            }
        });
    }

    @Test
    void daoNullClassThrows() {
        final EntityManager em = EntityManager.open(new RecordingDatabase());
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                em.dao(null);
            }
        });
    }

    @Test
    void daoForUnregisteredClassThrows() {
        final EntityManager em = EntityManager.open(new RecordingDatabase());
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                em.dao(UnregisteredEntity.class);
            }
        });
    }

    @Test
    void registeredDaoIsReturnedAndAttachedToTheDatabase() {
        RecordingDao dao = new RecordingDao();
        EntityManager.registerDao(dao);
        RecordingDatabase db = new RecordingDatabase();
        EntityManager em = EntityManager.open(db);

        Dao<SampleEntity> looked = em.dao(SampleEntity.class);
        assertSame(dao, looked);
        assertSame(db, dao.attached);
    }

    @Test
    void transactionCallsDelegateToTheDatabase() throws Exception {
        RecordingDatabase db = new RecordingDatabase();
        EntityManager em = EntityManager.open(db);
        em.beginTransaction();
        em.commitTransaction();
        em.rollbackTransaction();
        assertEquals(1, db.begin);
        assertEquals(1, db.commit);
        assertEquals(1, db.rollback);
    }

    @Test
    void closeIsIdempotentAndClosesTheDatabaseOnce() throws Exception {
        RecordingDatabase db = new RecordingDatabase();
        EntityManager em = EntityManager.open(db);
        em.close();
        em.close();
        assertEquals(1, db.close);
    }

    /** A class with no registered dao, used to drive the failure path. */
    private static final class UnregisteredEntity {
    }
}
