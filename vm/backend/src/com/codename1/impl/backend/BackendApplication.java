/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.backend.Backend;
import com.codename1.backend.HttpServer;
import java.util.List;

/// The build-generated wiring of an application: every bean, constructed and
/// injected by straight-line code the build wrote, and the lifecycle calls
/// around them.
///
/// Nothing here is looked up or reflected. The build resolves which
/// constructor each bean gets, which bean each injection point receives and
/// in what order they are built, and writes that down as `new` and
/// setter calls; this interface is only where the server calls into it.
public interface BackendApplication {
    /// Constructs the beans and returns the routers, once the database, if
    /// any, is open.
    HttpServer.Handler[] create(WiringEnvironment environment) throws Exception;

    /// Registers the websocket endpoints, which are beans too.
    void registerWebSockets(HttpServer.WebSocketRegistry registry) throws Exception;

    /// The server is accepting: scheduled jobs and exporters start here.
    void started(Backend backend) throws Exception;

    /// The server is about to drain: no new scheduled run starts after this.
    void stopping();

    /// The server has drained: the beans' destroy methods run here.
    void stopped();

    /// Whether a generated class needs `Backend.currentRequest()`: a
    /// request- or session-scoped bean reached from a singleton. False keeps
    /// the per-request thread-local write out of servers that have none.
    boolean tracksCurrentRequest();

    /// A request has been answered; `beans` are its
    /// request-scoped beans, whose destroy methods run here.
    void requestEnded(Object[] beans);

    /// A session has ended -- invalidated, expired, or the server stopped;
    /// `beans` are its session-scoped beans, whose destroy
    /// methods run here.
    void sessionEnded(Object[] beans);

    /// The scheduler running this application's `@Scheduled` jobs, or null.
    Scheduler getScheduler();

    /// Every bean the build wired: name, type, scope and what it was given.
    /// For the management endpoint and the development MCP server.
    List describeBeans();

    /// Every route the build generated: method, path and handler.
    List describeRoutes();
}
