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
package com.codename1.impl.backend;

import com.codename1.backend.HttpServer;

/// The security layer of one server, as the request path sees it: a type that
/// names none of the security classes.
///
/// A server whose build found no `SecurityFilterChain` bean has none of these and
/// pays one null check per request. One that did gets the implementation the
/// generated entry point linked, and every request the server does not answer on
/// its own behalf passes through it on the way to the application's routers.
public interface RequestSecurity {
    /// The rest of the request path: the application's routers and static files.
    interface Next {
        /// The answer of the first router that takes `request`, or null when none does.
        HttpServer.Response route(HttpServer.Request request) throws Exception;
    }

    /// Answers `request`, calling `next` when the chain it belongs to lets it
    /// through. A request no chain claims goes straight to `next`. An
    /// authentication or authorization failure thrown anywhere under a chain --
    /// a filter, a controller, a service -- is answered here rather than thrown.
    HttpServer.Response serve(HttpServer.Request request, Next next) throws Exception;

    /// Adds the headers the chain of `request` writes to `response`, which is the
    /// caller's own copy: the response a handler returns may be one shared by
    /// every request. Called for the server's own 404 and 500 as well, after
    /// [#clear].
    void decorate(HttpServer.Request request, HttpServer.Response response);

    /// Decides a WebSocket handshake: 0 to let it reach its endpoint, otherwise
    /// the status to refuse it with.
    int upgrade(HttpServer.Request request) throws Exception;

    /// Forgets what the calling thread holds about the request it served. A
    /// virtual thread serves a whole keep-alive connection, so whatever stays
    /// here is what the next request on it would be answered with.
    void clear();
}
