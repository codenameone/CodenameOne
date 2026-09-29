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
/// Annotations that turn an ordinary class into an HTTP or websocket endpoint.
///
/// Mark a class [RestController] and its methods with [GetMapping],
/// [PostMapping] and the other verb mappings; bind the request to parameters with
/// [PathVariable], [RequestParam], [RequestHeader] and [RequestBody]. The build
/// reads these annotations and generates the router, so routing is decided at
/// build time rather than by reflection when the server starts. The names follow
/// Spring's, so the shape reads without learning it first.
///
/// Server code: these annotations are read by the backend build and mean nothing
/// in an app.
package com.codename1.backend.annotations;
