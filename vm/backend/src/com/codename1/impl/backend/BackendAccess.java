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
import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;
import com.codename1.backend.Span;
import com.codename1.backend.TaskExecutor;
import com.codename1.backend.Tracer;
import com.codename1.backend.Tracing;
import com.codename1.backend.orm.EntityManager;
import com.codename1.impl.backend.mcp.McpServer;
import com.codename1.impl.backend.mcp.McpTool;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/// The runtime's internals, for the code the build generates and for the runtime's
/// own classes in other packages.
///
/// `com.codename1.backend` is what an application is written against, and its
/// javadoc is the backend's public API. The generated wiring, the woven aspects and
/// the runtime's MCP, management and scheduling classes need more than that -- to
/// begin a transaction around a method, to hand a request its scoped beans, to
/// install an application into the builder -- and making those members public
/// would publish them as API. They are package-private instead, and this is the
/// one door to them: an abstract class whose only implementation lives in
/// `com.codename1.backend` and installs itself when [Backend] is initialized.
///
/// Nothing here is a contract with applications. It changes whenever the build's
/// output changes, which is why it lives under `com.codename1.impl`.
public abstract class BackendAccess {
    /// [#executor]'s thread kind: whatever the platform runs best.
    public static final int AUTO = 0;
    /// [#executor]'s thread kind: virtual threads.
    public static final int VIRTUAL = 1;
    /// [#executor]'s thread kind: platform threads.
    public static final int PLATFORM = 2;
    /// The executor `@Async` work goes to when it names none.
    public static final String DEFAULT_EXECUTOR = "default";
    /// The executor scheduled jobs go to when they name none.
    public static final String SCHEDULING_EXECUTOR = "scheduling";

    /// `@Transactional`'s propagation values, in the order of the annotation's enum.
    public static final int REQUIRED = 0;
    /// See [#REQUIRED].
    public static final int SUPPORTS = 1;
    /// See [#REQUIRED].
    public static final int MANDATORY = 2;
    /// See [#REQUIRED].
    public static final int REQUIRES_NEW = 3;
    /// See [#REQUIRED].
    public static final int NOT_SUPPORTED = 4;
    /// See [#REQUIRED].
    public static final int NEVER = 5;
    /// See [#REQUIRED].
    public static final int NESTED = 6;

    private static BackendAccess instance;

    /// Called once, by the implementation, from [Backend]'s static initializer.
    public static synchronized void install(BackendAccess access) {
        if (instance != null) {
            throw new IllegalStateException("BackendAccess is already installed");
        }
        instance = access;
    }

    /// The implementation, initializing [Backend] first if nothing has yet.
    public static BackendAccess get() {
        BackendAccess a = instance;
        if (a == null) {
            // A static call is what makes a class initialize, on the JVM and under
            // ParparVM alike; a class literal does not. currentRequest() has no
            // other effect.
            Backend.currentRequest();
            a = instance;
            if (a == null) {
                throw new IllegalStateException("The backend runtime did not install its access");
            }
        }
        return a;
    }

    // ---------------------------------------------------------- the builder

    /// Installs the generated wiring of an application.
    public abstract void application(Backend.Builder builder, BackendApplication application);

    /// Serves the MCP endpoint, with `devTools` on a development profile when given.
    public abstract void mcp(Backend.Builder builder, McpServer.Extension devTools);

    /// Serves the management endpoints when the configuration turns them on.
    public abstract void management(Backend.Builder builder);

    /// The module's compiled-in settings: the bottom layer of the configuration.
    public abstract void compiledSettings(Backend.Builder builder, String[] keysAndValues);

    /// A configuration whose `values` win over every layer, the environment
    /// included: the settings a test names for itself.
    public abstract Config testConfig(java.util.Properties values, String profile);

    /// Adds a tool to the MCP endpoint.
    public abstract void mcpTool(Backend.Builder builder, McpTool tool);

    /// The name the server reports to MCP clients.
    public abstract void serviceName(Backend.Builder builder, String name);

    // ------------------------------------------------------------ a server

    /// The server's generated wiring, or null.
    public abstract BackendApplication applicationOf(Backend backend);

    /// A copy of the server's managed beans.
    public abstract List managedBeans(Backend backend);

    /// Whether the server records request and job metrics.
    public abstract boolean isMeasured(Backend backend);

    /// Turns on the server's recent-request log, keeping `capacity` entries.
    public abstract void enableRequestLog(Backend backend, int capacity);

    /// The server's recent requests, newest first; see [#enableRequestLog].
    public abstract List recentRequests(Backend backend, int limit, boolean failuresOnly);

    /// Answers a request the way the listener would -- the body decoded and
    /// refused by the same rules, then sessions, scoped beans, tracing and metrics
    /// -- on the calling thread, without a socket. `headers` maps a name to its
    /// value, in any case; `body` may be null.
    public abstract HttpServer.Response dispatch(Backend backend, String method, String target,
                                                 java.util.Map headers, byte[] body)
            throws Exception;

    // ------------------------------------------------- requests and sessions

    /// The response's status, headers and body, with deferred JSON serialized.
    public abstract int status(HttpServer.Response response);

    /// The response's headers, as name and value pairs in the order they were set.
    public abstract List headers(HttpServer.Response response);

    /// The response's body, serialized; never null.
    public abstract byte[] body(HttpServer.Response response) throws IOException;

    /// Releases what `response` holds without reading it -- the file a static file
    /// response keeps open -- for a caller that sends no body, as a HEAD answer.
    public abstract void discard(HttpServer.Response response);

    /// The response's content type, or null.
    public abstract String contentType(HttpServer.Response response);

    /// The request's request-scoped beans, grown to `count` slots.
    public abstract Object[] requestBeans(HttpServer.Request request, int count);

    /// The lock guarding a session's session-scoped beans.
    public abstract Object sessionBeanLock(HttpSession session);

    /// The session's session-scoped beans, grown to `count` slots.
    public abstract Object[] sessionBeans(HttpSession session, int count);

    /// Sleeps until `deadlineMillis`, yielding a virtual thread rather than its host.
    public abstract void napUntil(long deadlineMillis);

    // --------------------------------------------------------------- tracing

    /// The span the calling thread is in, for work it hands to another thread.
    public abstract Span captureParent();

    /// The tracer the calling thread's server owns.
    public abstract Tracer captureOwner();

    /// The tracer that records nothing.
    public abstract Tracer untraced();

    /// Runs `work` in a span named `name`, under `parent` and recorded by `own`.
    public abstract Object inBackground(String name, Span parent, Tracer own, Tracing.Work work)
            throws Exception;

    // ---------------------------------------------------------- transactions

    /// Begins a woven `@Transactional` method; the result is handed back to
    /// [#commit] or [#afterThrow].
    public abstract Object begin(int propagation, boolean readOnly, int timeoutSeconds);

    /// Ends a transaction [#begin] returned, normally.
    public abstract void commit(Object transaction);

    /// Ends a transaction [#begin] returned, after its method threw.
    public abstract void afterThrow(Object transaction, boolean rollback);

    /// The connection the current transaction holds on `pool`, borrowing one if
    /// the transaction has not touched it yet; null outside a transaction.
    public abstract Database joined(DataSource pool) throws IOException;

    /// Whether `db` is the connection a transaction holds on `pool`.
    public abstract boolean isJoined(DataSource pool, Database db);

    /// Whether a transaction is active on `pool`.
    public abstract boolean isActiveOn(DataSource pool);

    /// The ORM session bound to the current transaction.
    public abstract com.codename1.orm.session.Session session(EntityManager entities);

    /// Marks the current transaction rollback-only after a failure.
    public abstract void markRollbackOnly();

    /// Marks the transaction on `pool` rollback-only after a failure.
    public abstract void markRollbackOnly(DataSource pool);

    // -------------------------------------------------------------- executors

    /// The named executor of the calling thread's server, created on first use.
    public abstract TaskExecutor executor(String name, int kind);

    /// Every executor of the given servers.
    public abstract List executorsOf(Collection servers);
}
