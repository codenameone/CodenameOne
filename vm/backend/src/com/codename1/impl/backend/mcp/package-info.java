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
/// A Model Context Protocol server inside the backend, so an agent can call a
/// running server's tools over HTTP.
///
/// [McpServer] answers JSON-RPC at `cn1.mcp.path` (`/mcp` by default). An
/// application publishes its own tools by annotating bean methods with
/// `McpTool` from `com.codename1.backend.annotations`; the build generates their
/// schemas and a dispatcher, so nothing is looked up by reflection. [DevTools]
/// adds the development tools -- routes, beans, configuration, requests, SQL,
/// jobs and metrics -- and is installed only on a development profile.
///
/// Server code: this package is not available in the app.
package com.codename1.impl.backend.mcp;
