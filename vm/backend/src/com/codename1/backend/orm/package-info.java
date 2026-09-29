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
/// The build-time ORM on the server: entity classes in, typed data access objects
/// out.
///
/// [EntityManager] hands out a [Dao] per entity, and a [Query] names rows in the
/// entity's own terms. The entity classes use the same annotations as the client
/// ORM, so one class can be stored in the app's SQLite file and in the server's
/// database. The build generates the data access code, so nothing reflects at
/// run time.
///
/// For a managed persistence context with change tracking and relationships, see
/// the shared `com.codename1.orm.session` package.
///
/// Server code: this package is not available in the app.
package com.codename1.backend.orm;
