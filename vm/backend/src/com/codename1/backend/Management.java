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
package com.codename1.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.metrics.Metrics;

/// The server's management endpoints: health for the load balancer, metrics for
/// a scraper, and the jobs and managed beans for an operator.
///
/// ```java
///   GET  /manage/health          public; 503 while draining
///   GET  /manage/metrics         every metric, as JSON
///   GET  /manage/prometheus      every metric, in the Prometheus text format
///   GET  /manage/jobs            the scheduled jobs and their last runs
///   GET  /manage/managed         the managed beans, with their attributes' values
///   POST /manage/managed/{bean}/{operation}   calls an operation; body: arguments
/// ```
///
/// Off by default outside a development profile; `cn1.management.enabled`
/// turns it on or off explicitly, and `cn1.management.path` moves it.
/// Everything but health needs `Authorization: Bearer `.
/// Without a token, a development profile serves the read-only views to anyone
/// who can reach the port -- a laptop -- and no profile serves an operation,
/// since an operation changes the running server.
public final class Management implements HttpServer.Handler {
    public static final String ENABLED = "cn1.management.enabled";
    public static final String PATH = "cn1.management.path";
    public static final String TOKEN = "cn1.management.token";

    private final String path;
    private final byte[] token;
    private final boolean development;
    /// Volatile: attached after the listener's workers are running, and a plain
    /// write need never reach them -- health would read STARTING for good.
    private volatile Backend backend; //NOPMD AvoidUsingVolatile - attached after the workers start

    private Management(String path, String token, boolean development) {
        this.path = path;
        this.token = token == null || token.length() == 0 ? null : utf8(token);
        this.development = development;
    }

    /// The endpoints this configuration asks for, or null when they are off.
    ///
    /// #### Throws
    ///
    /// - `IOException`: when they are on outside development with no token,
    /// which would publish the server's internals to anyone
    public static Management fromConfig(Config config) throws IOException {
        boolean development = config.isDevelopmentProfile();
        if (!config.getBoolean(ENABLED, development)) {
            return null;
        }
        String token = config.getHeaderSecret(TOKEN);
        if (!development && (token == null || token.length() == 0)) {
            throw new IOException(ENABLED + " is on outside a development profile and "
                    + TOKEN + " is not set. The metrics and managed beans would be readable "
                    + "by anyone who can reach the port; set a token, or leave the "
                    + "endpoints off.");
        }
        String path = config.getRoutePath(PATH, "/manage");
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (!path.startsWith("/")) {
            throw new IOException(PATH + " must start with /");
        }
        return new Management(path, token, development);
    }

    /// Set once the server is running.
    void attach(Backend running) {
        this.backend = running;
    }

    /// The managed beans of the server this endpoint belongs to.
    private List beans() {
        return backend == null ? new ArrayList() : backend.getManagedBeans();
    }

    /// Each of `all` -- a server's managed beans, from
    /// [Backend#getManagedBeans] -- with its attributes' current values and
    /// its operations.
    public static List describeBeans(List all) {
        List out = new ArrayList();
        for (Object element : all) {
            ManagedBean bean = (ManagedBean) element;
            Map m = new LinkedHashMap();
            m.put("objectName", bean.getObjectName());
            if (bean.getDescription().length() > 0) {
                m.put("description", bean.getDescription());
            }
            Map attributes = new LinkedHashMap();
            String[] names = bean.attributeNames();
            for (int a = 0 ; a < names.length ; a++) {
                Object value;
                try {
                    value = bean.readAttribute(a);
                } catch (Exception err) {
                    value = "<" + err + ">";
                }
                attributes.put(names[a], value);
            }
            m.put("attributes", attributes);
            List operations = new ArrayList();
            String[] ops = bean.operationNames();
            String[] descriptions = bean.operationDescriptions();
            String[][] params = bean.operationParameters();
            for (int o = 0 ; o < ops.length ; o++) {
                Map op = new LinkedHashMap();
                op.put("name", ops[o]);
                if (descriptions[o].length() > 0) {
                    op.put("description", descriptions[o]);
                }
                List p = new ArrayList();
                for (int q = 0 ; q < params[o].length ; q++) {
                    p.add(params[o][q]);
                }
                op.put("parameters", p);
                operations.add(op);
            }
            m.put("operations", operations);
            out.add(m);
        }
        return out;
    }

    /// Calls an operation of a managed bean by name.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when there is no such bean or operation
    private static ManagedBean find(List all, String objectName) {
        for (Object element : all) {
            ManagedBean bean = (ManagedBean) element;
            if (bean.getObjectName().equals(objectName)) {
                return bean;
            }
        }
        return null;
    }

    private static int operationIndex(ManagedBean bean, String operation) {
        String[] ops = bean.operationNames();
        for (int o = 0 ; o < ops.length ; o++) {
            if (ops[o].equals(operation)) {
                return o;
            }
        }
        return -1;
    }

    public static Object invoke(List all, String objectName, String operation,
                                Map arguments) throws Exception {
        for (Object element : all) {
            ManagedBean bean = (ManagedBean) element;
            if (!bean.getObjectName().equals(objectName)) {
                continue;
            }
            String[] ops = bean.operationNames();
            for (int o = 0 ; o < ops.length ; o++) {
                if (ops[o].equals(operation)) {
                    return bean.invoke(o, arguments == null ? new LinkedHashMap() : arguments);
                }
            }
            throw new IllegalArgumentException(objectName + " has no operation " + operation);
        }
        throw new IllegalArgumentException("No managed bean " + objectName);
    }

    @Override
    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
        // The CANONICAL path, as the routers compare it; see McpServer.handle.
        String canonical = request.getTarget() == null ? null : request.pathFrom(0);
        if (canonical == null || !canonical.startsWith(path)) {
            return null;
        }
        String rest = canonical.substring(path.length());
        if (rest.length() > 0 && rest.charAt(0) != '/') {
            return null;
        }
        String method = request.getMethod();
        if ("/health".equals(rest) && ("GET".equals(method) || "HEAD".equals(method))) {
            return health(request);
        }
        if (rest.length() == 0) {
            return null;
        }
        boolean operation = "POST".equals(method) && rest.startsWith("/managed/");
        if (!authorized(request, operation)) {
            return request.respondJson(token == null ? 403 : 401, error(token == null
                    ? "Set " + TOKEN + " to use this endpoint"
                    : "A bearer token is required"));
        }
        if ("GET".equals(method) || "HEAD".equals(method)) {
            if ("/metrics".equals(rest)) {
                return request.respondJson(200, Metrics.snapshot());
            }
            if ("/prometheus".equals(rest)) {
                return request.respond(200, "text/plain; version=0.0.4; charset=utf-8",
                        utf8(Metrics.prometheus()));
            }
            if ("/jobs".equals(rest)) {
                Scheduler scheduler = backend == null || backend.getApplication() == null
                        ? null : backend.getApplication().getScheduler();
                return request.respondJson(200, scheduler == null ? new ArrayList()
                        : scheduler.describe());
            }
            if ("/managed".equals(rest)) {
                return request.respondJson(200, describeBeans(beans()));
            }
            return null;
        }
        if (operation) {
            String[] parts = split(rest.substring("/managed/".length()));
            if (parts == null) {
                return request.respondJson(404, error("Expected /managed/{bean}/{operation}"));
            }
            // Bean and operation names are Java identifiers, so there is nothing
            // to percent-decode: an escaped one matches nothing. Looked up FIRST
            // and on its own, so 404 means "no such operation" and nothing else --
            // an operation that rejects its arguments is the caller's mistake to
            // fix, a 400, not an endpoint that does not exist.
            ManagedBean bean = find(beans(), parts[0]);
            int op = bean == null ? -1 : operationIndex(bean, parts[1]);
            if (bean == null) {
                return request.respondJson(404, error("No managed bean " + parts[0]));
            }
            if (op < 0) {
                return request.respondJson(404, error(parts[0] + " has no operation "
                        + parts[1]));
            }
            Map arguments = new LinkedHashMap();
            String body = request.getBody();
            if (body != null && body.trim().length() > 0) {
                try {
                    arguments = Json.parseObject(body);
                } catch (Exception err) {
                    return request.respondJson(400, error("The body must be a JSON object "
                            + "of the operation's arguments: " + err.getMessage()));
                }
            }
            try {
                Map result = new LinkedHashMap();
                result.put("result", bean.invoke(op, arguments));
                return request.respondJson(200, result);
            } catch (IllegalArgumentException err) {
                return request.respondJson(400, error(err.getMessage()));
            }
        }
        return null;
    }

    private HttpServer.Response health(HttpServer.Request request) {
        Map out = new LinkedHashMap();
        boolean up = true;
        if (backend != null) {
            Map server = backend.getServer().getMetrics();
            boolean serving = "ok".equals(server.get("status"));
            // STARTING until the application's start-up hook has returned: the
            // listener accepts before it runs, and a load balancer that read UP
            // then would send traffic to a server that may still fail to start.
            up = serving && backend.isReady();
            out.put("status", !serving ? "DRAINING" : up ? "UP" : "STARTING");
            out.put("uptimeSeconds", server.get("uptimeSeconds"));
            if (backend.getDataSource() != null) {
                Map db = new LinkedHashMap();
                db.put("open", Integer.valueOf(backend.getDataSource().getOpenCount()));
                db.put("idle", Integer.valueOf(backend.getDataSource().getIdleCount()));
                out.put("database", db);
            }
        } else {
            // Not attached yet: the server is still being put together.
            up = false;
            out.put("status", "STARTING");
        }
        return request.respondJson(up ? 200 : 503, out);
    }

    private boolean authorized(HttpServer.Request request, boolean operation) {
        if (token == null) {
            return development && !operation;
        }
        String header = request.getHeader("authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return false;
        }
        return Crypto.equalsConstantTime(token, utf8(header.substring(7).trim()));
    }

    private static String[] split(String rest) {
        int slash = rest.indexOf('/');
        if (slash <= 0 || slash == rest.length() - 1 || rest.indexOf('/', slash + 1) >= 0) {
            return null;
        }
        return new String[] {rest.substring(0, slash), rest.substring(slash + 1)};
    }

    private static Map error(String message) {
        Map m = new LinkedHashMap();
        m.put("error", message);
        return m;
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }
}
