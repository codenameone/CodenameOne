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
/// `@Entity` classes get a generated dao at build time. Each dao's static
/// initializer self-registers with this registry. The registry stays empty
/// until something triggers each generated class's `<clinit>`:
///
/// - **iOS / Android** -- the build server probes the project zip for
///   `cn1app.DaoBootstrap` and splices a `new cn1app.DaoBootstrap();` into
///   the per-build application stub before `Display.init`.
/// - **JavaSE simulator + desktop** -- `JavaSEPort#postInit` calls
///   `Class.forName("cn1app.DaoBootstrap")` so the registry is populated
///   on the same boundary.
/// - **Unit tests / manual init** -- application code can call
///   `EntityManager.registerDao(...)` directly.
///
/// ```java
/// EntityManager em = EntityManager.open("MyApp.db");
/// Dao<User> users = em.dao(User.class);
/// users.createTable();
/// users.insert(new User("alice"));
/// for (User u : users.findAll()) { ... }
/// em.close();
/// ```
///
/// The registry is keyed on `getClass().getName()` so it survives ParparVM
/// rename and R8 obfuscation: both the registration site and the lookup
/// site see the same renamed name within a single execution.
public final class EntityManager {

    private static final Map<String, Dao<?>> BY_NAME = new HashMap<String, Dao<?>>();

    private final Database db;
    private boolean closed;

    private EntityManager(Database db) {
        this.db = db;
    }

    /// Opens (or creates) the SQLite file `databaseName` via
    /// `Display.openOrCreate`. Throws `IOException` when the platform
    /// refuses to provide a SQLite database.
    public static EntityManager open(String databaseName) throws IOException {
        Database db = Display.getInstance().openOrCreate(databaseName);
        if (db == null) {
            throw new IOException("Platform does not support SQLite: "
                    + Display.getInstance().getPlatformName());
        }
        return open(db);
    }

    /// Wraps an existing `Database` (e.g. one opened with a custom path)
    /// in an `EntityManager`. The caller retains ownership; `close()`
    /// closes the database.
    public static EntityManager open(Database db) {
        if (db == null) {
            throw new IllegalArgumentException("database is null");
        }
        return new EntityManager(db);
    }

    /// Installs `dao` under `dao.type().getName()`. The generated
    /// per-class dao's static initializer calls this; hand-written daos
    /// for classes outside the build's annotation scan call it
    /// explicitly.
    public static <T> void registerDao(Dao<T> dao) {
        if (dao == null) {
            throw new IllegalArgumentException("dao is null");
        }
        BY_NAME.put(dao.type().getName(), dao);
    }

    /// Returns the dao for `entityClass`, freshly attached to this
    /// entity manager's `Database`. Throws `IllegalStateException` when
    /// no dao was generated for the class -- typically because `@Entity`
    /// is missing or the process-annotations Mojo did not run.
    @SuppressWarnings("unchecked")
    public <T> Dao<T> dao(Class<T> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("entityClass is null");
        }
        Dao<T> d = (Dao<T>) BY_NAME.get(entityClass.getName());
        if (d == null) {
            throw new IllegalStateException("No dao registered for "
                    + entityClass.getName() + ". Add @Entity and ensure the "
                    + "cn1:process-annotations Mojo ran during build, then re-run -- "
                    + "the generated DaoBootstrap populates this registry at startup.");
        }
        d.attach(db);
        return d;
    }

    /// The underlying `Database`. Use it for raw SQL when the dao surface
    /// isn't enough.
    public Database database() {
        return db;
    }

    /// Begin a transaction. Equivalent to `database().beginTransaction()`.
    public void beginTransaction() throws IOException {
        db.beginTransaction();
    }

    /// Commit the current transaction.
    public void commitTransaction() throws IOException {
        db.commitTransaction();
    }

    /// Roll back the current transaction.
    public void rollbackTransaction() throws IOException {
        db.rollbackTransaction();
    }

    /// Close the underlying database. Idempotent.
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        db.close();
    }
}
