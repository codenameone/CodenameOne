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
/// The Codename One backend runtime: the server side of an application, written
/// in the same Java as the app and compiled to a native server binary.
///
/// This package is server code. None of it is available in the app on the
/// device; the classes an app and its server share are the ORM session API and
/// the entity annotations, which live in the client packages and are documented
/// in both references.
///
/// [Backend] is the entry point: it reads the [Config], opens the database the
/// deployment names, and runs an [HttpServer]. Around it are the pieces a server
/// needs without a framework behind it -- [Database] and [DataSource] over SQLite,
/// PostgreSQL and MySQL, [Json], [Jwt], [WebSocket] endpoints, [StaticFiles],
/// distributed tracing through [Tracing], and [LambdaRuntime] for AWS Lambda.
///
/// Controllers are ordinary classes annotated from
/// `com.codename1.backend.annotations`; the build generates the router, so
/// nothing is registered or scanned at start-up.
package com.codename1.backend;
