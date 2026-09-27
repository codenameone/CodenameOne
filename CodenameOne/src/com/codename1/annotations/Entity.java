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
package com.codename1.annotations;

import com.codename1.annotations.db.Index;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a POJO or `PropertyBusinessObject` as a persistent entity, in the
/// app and on the Codename One backend alike.
///
/// At build time the Codename One Maven plugin generates a reflection-free
/// `Dao` next to the class with `createTable`, `insert`, `update`, `delete`,
/// `findById`, `findAll`, and `find(where, params)` methods. Application code
/// reaches the generated dao through `com.codename1.orm.EntityManager`:
///
/// ```java
/// @Entity(table="users")
/// public class User {
///     @Id(autoIncrement=true) public long id;
///     @Column(name="full_name") public String name;
///     public int age;
/// }
///
/// EntityManager em = EntityManager.open("MyDB");
/// Dao<User> users = em.dao(User.class);
/// users.createTable();
/// users.insert(new User());
/// ```
///
/// The dao uses the same prepared-statement protocol as the existing
/// `com.codename1.db.Database`; no `Class.forName` lookups, so the binding
/// survives ParparVM rename / R8 obfuscation in shipped builds.
///
/// An entity is the one kind of class both halves of an application own: the
/// app stores rows of it in its local SQLite file and the server stores rows of
/// it in PostgreSQL, MySQL or SQLite. The backend runtime compiles this very
/// annotation (it is marked `SharedWithBackend`), so the entity class can be a
/// single file in a module both sides depend on. What differs is what the build
/// GENERATES from it: against the core the processor emits a dao over
/// `com.codename1.db.Database`, against the backend one over
/// `com.codename1.backend.Database`, whose SQL is built for whichever engine the
/// connection turns out to be. On the server the class needs public fields and a
/// public no-arg constructor, because the generated dao reads and writes them
/// directly.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@com.codename1.impl.SharedWithBackend
public @interface Entity {
    /// SQL table name. Defaults to the simple class name when blank.
    String table() default "";
    /// Additional indexes created with the entity table.
    /// @return index declarations, empty by default
    Index[] indexes() default {};
}
