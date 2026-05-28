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

import java.io.IOException;
import java.util.List;

/// Runtime contract a build-time-generated `@Entity` data access object
/// implements. Application code rarely references `Dao` by name -- the typed
/// dao is reached through `EntityManager.dao(EntityClass.class)`.
///
/// All methods throw `IOException` for the same reasons `com.codename1.db`
/// does: the underlying SQLite driver propagates IO failures, locking
/// failures, and constraint violations through `IOException`.
public interface Dao<T> {

    /// The entity class this dao handles.
    Class<T> type();

    /// The SQL table name. Defaults to the simple class name; an explicit
    /// `@Entity(table="...")` wins.
    String tableName();

    /// Connects this dao to a `Database` instance. Called by
    /// `EntityManager.dao(Class)`; the same dao is reused for the lifetime of
    /// the entity manager.
    void attach(Database db);

    /// `CREATE TABLE IF NOT EXISTS ...`. Idempotent.
    void createTable() throws IOException;

    /// `DROP TABLE IF EXISTS ...`.
    void dropTable() throws IOException;

    /// `INSERT INTO ...`. When the entity has an auto-increment `@Id`, the
    /// generated id is written back into the instance via
    /// `SELECT last_insert_rowid()`.
    void insert(T entity) throws IOException;

    /// `UPDATE ... WHERE id = ?`.
    void update(T entity) throws IOException;

    /// `DELETE ... WHERE id = ?`.
    void delete(T entity) throws IOException;

    /// `SELECT ... WHERE id = ?`. Returns `null` when no row matches.
    T findById(Object id) throws IOException;

    /// `SELECT * FROM ...`. Returns every row mapped to an instance of the
    /// entity class.
    List<T> findAll() throws IOException;

    /// Free-form WHERE clause; `where` is appended verbatim after `WHERE`
    /// and `params` fills the `?` placeholders.
    ///
    /// ```java
    /// users.find("age > ? AND city = ?", 18, "Berlin");
    /// ```
    List<T> find(String where, Object... params) throws IOException;
}
