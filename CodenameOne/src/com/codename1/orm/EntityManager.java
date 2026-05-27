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

import com.codename1.db.Database;
import com.codename1.ui.Display;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// Public entry point for the build-time SQLite ORM.
///
/// `@Entity` classes are picked up by the Maven plugin's annotation processor
/// at build time. The generated `Dao` is wired into an internal registry via
/// `com.codename1.orm.generated.DaosIndex` whose static initializer fires the
/// first time `EntityManager.open` is called.
///
/// Typical use:
///
/// ```java
/// EntityManager em = EntityManager.open("MyApp.db");
/// Dao<User> users = em.dao(User.class);
/// users.createTable();
/// users.insert(new User("alice"));
/// for (User u : users.findAll()) { ... }
/// em.close();
/// ```
public final class EntityManager {

    private static final Map<Class<?>, Dao<?>> BY_TYPE = new HashMap<Class<?>, Dao<?>>();
    private static boolean indexLoaded = false;

    private final Database db;
    private boolean closed;

    private EntityManager(Database db) {
        this.db = db;
    }

    /// Opens (or creates) the SQLite file `databaseName` via
    /// `Display.openOrCreate`. Throws `IOException` when the platform refuses
    /// to provide a SQLite database.
    public static EntityManager open(String databaseName) throws IOException {
        Database db = Display.getInstance().openOrCreate(databaseName);
        if (db == null) {
            throw new IOException("Platform does not support SQLite: "
                    + Display.getInstance().getPlatformName());
        }
        return open(db);
    }

    /// Wraps an existing `Database` (e.g. one already opened with a custom
    /// path) in an `EntityManager`. The caller retains ownership; `close()`
    /// will still close the database.
    public static EntityManager open(Database db) {
        if (db == null) {
            throw new IllegalArgumentException("database is null");
        }
        return new EntityManager(db);
    }

    /// Registers a hand-written dao. The build-time-generated `DaosIndex`
    /// uses the same call; explicit registration is only needed for entity
    /// classes that live in a dependency JAR the annotation Mojo cannot scan.
    public static <T> void registerDao(Dao<T> dao) {
        if (dao == null) {
            throw new IllegalArgumentException("dao is null");
        }
        BY_TYPE.put(dao.type(), dao);
    }

    /// Returns the dao for `entityClass`, freshly attached to this entity
    /// manager's `Database`. Throws `IllegalStateException` when no dao was
    /// generated for the class -- typically because `@Entity` is missing or
    /// the process-annotations Mojo did not run.
    @SuppressWarnings("unchecked")
    public <T> Dao<T> dao(Class<T> entityClass) {
        ensureIndexLoaded();
        Dao<T> d = (Dao<T>) BY_TYPE.get(entityClass);
        if (d == null) {
            throw new IllegalStateException("No dao registered for "
                    + entityClass.getName() + ". Add @Entity and ensure the "
                    + "cn1:process-annotations Mojo ran during build.");
        }
        d.attach(db);
        return d;
    }

    /// The underlying `Database`. Use it for raw SQL when the dao surface is
    /// not enough.
    public Database database() {
        return db;
    }

    /// Begin a transaction on the underlying database. Equivalent to
    /// `database().beginTransaction()`.
    public void beginTransaction() throws IOException {
        db.beginTransaction();
    }

    /// Commit a transaction on the underlying database.
    public void commitTransaction() throws IOException {
        db.commitTransaction();
    }

    /// Roll back a transaction on the underlying database.
    public void rollbackTransaction() throws IOException {
        db.rollbackTransaction();
    }

    /// Closes the underlying database. Idempotent.
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        db.close();
    }

    private static synchronized void ensureIndexLoaded() {
        if (indexLoaded) return;
        indexLoaded = true;
        try {
            new com.codename1.orm.generated.DaosIndex();
        } catch (NoClassDefFoundError ignore) {
            // No @Entity types in this project.
        } catch (RuntimeException ignore) {
            // Already loaded.
        }
    }
}
