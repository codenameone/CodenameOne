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
package com.codename1.backend.graphql;

import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.log.Logger;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Serves a [GraphQL] engine over HTTP, as the GraphQL over HTTP specification
/// describes, and its subscriptions over WebSocket with the
/// `graphql-transport-ws` protocol -- what Spring for GraphQL serves, under the
/// same settings:
///
/// - `spring.graphql.path` (`/graphql`): `POST` a JSON body with `query`,
///   `operationName` and `variables`, or a body of type `application/graphql`
///   that is the query itself. `GET` with the same as query parameters runs a
///   query, never a mutation.
/// - `spring.graphql.websocket.path`: where subscriptions are served. When it
///   is not set and the schema has a subscription type, they are served at
///   `spring.graphql.path`, which a WebSocket upgrade reaches without
///   colliding with a `POST`; set it to `none` to serve none.
/// - `spring.graphql.graphiql.enabled` (`false`) and
///   `spring.graphql.graphiql.path` (`/graphiql`): the GraphiQL page, which
///   loads its scripts from a CDN.
///
/// Every response to a document the server could read is `200`, its outcome
/// in the body, and is written as `application/graphql-response+json` unless
/// the client accepts only `application/json`. A request with no readable
/// document is `400`.
///
/// Resolvers get the `HttpServer.Request` as their context over HTTP, and the
/// `WebSocketSession` over WebSocket. The endpoint sits behind the security
/// filter chain like any route, so `SecurityFilterChain` rules apply to its
/// paths.
public final class GraphQLEndpoint implements HttpServer.Handler {
    public static final String PATH = "spring.graphql.path";
    public static final String WEBSOCKET_PATH = "spring.graphql.websocket.path";
    public static final String GRAPHIQL_ENABLED = "spring.graphql.graphiql.enabled";
    public static final String GRAPHIQL_PATH = "spring.graphql.graphiql.path";
    public static final String INTROSPECTION = "spring.graphql.schema.introspection.enabled";
    /// The deepest selection nesting a document may have; see
    /// [GraphQL.Builder#maxDepth(int)].
    public static final String MAX_DEPTH = "cn1.graphql.maxDepth";
    /// The most fields a document may select, aliases and fragment spreads
    /// counted where they expand; see [GraphQL.Builder#maxFields(int)].
    public static final String MAX_FIELDS = "cn1.graphql.maxFields";

    static final String PROTOCOL = "graphql-transport-ws";
    private static final String RESPONSE_TYPE = "application/graphql-response+json";
    private static final Logger LOG = Logger.getLogger(GraphQLEndpoint.class);

    private final GraphQL graphQL;
    private final String path;
    private final byte[] pathBytes;
    private final String webSocketPath;
    private final byte[] graphiqlPath;

    private GraphQLEndpoint(GraphQL graphQL, String path, String webSocketPath,
                            String graphiqlPath) {
        this.graphQL = graphQL;
        this.path = path;
        this.pathBytes = ascii(path);
        this.webSocketPath = webSocketPath;
        this.graphiqlPath = graphiqlPath == null ? null : ascii(graphiqlPath);
    }

    /// The endpoint for the engine `builder` builds, configured from `config`:
    /// the paths above, and introspection and the depth limit on the engine.
    public static GraphQLEndpoint fromConfig(Config config, GraphQL.Builder builder)
            throws IOException {
        builder.introspection(config.getBoolean(INTROSPECTION, true));
        builder.maxDepth(config.getInt(MAX_DEPTH, 15));
        builder.maxFields(config.getInt(MAX_FIELDS, 2000));
        GraphQL graphQL = builder.build();
        String path = config.getRoutePath(PATH, "/graphql");
        String ws = config.get(WEBSOCKET_PATH, "");
        if (ws.length() == 0) {
            ws = graphQL.getSchema().getRootTypeName("subscription") != null ? path : null;
        } else if ("none".equals(ws)) {
            ws = null;
        }
        String graphiql = config.getBoolean(GRAPHIQL_ENABLED, false)
                ? config.getRoutePath(GRAPHIQL_PATH, "/graphiql") : null;
        for (String p : new String[] {path, ws, graphiql}) {
            if (p != null && !p.startsWith("/")) {
                throw new IOException("A GraphQL path must start with /: " + p);
            }
        }
        return new GraphQLEndpoint(graphQL, path, ws, graphiql);
    }

    /// An endpoint serving `graphQL` at `path`, with subscriptions at
    /// `webSocketPath` (or none for null) and no GraphiQL page.
    public static GraphQLEndpoint of(GraphQL graphQL, String path, String webSocketPath) {
        return new GraphQLEndpoint(graphQL, path, webSocketPath, null);
    }

    /// The engine.
    public GraphQL getGraphQL() {
        return graphQL;
    }

    /// The HTTP path.
    public String getPath() {
        return path;
    }

    /// Where subscriptions are served, or null.
    public String getWebSocketPath() {
        return webSocketPath;
    }

    /// Registers the subscription endpoint, when there is one.
    public void registerWebSocket(HttpServer.WebSocketRegistry registry) {
        if (webSocketPath != null) {
            registry.route(webSocketPath, new Subscriptions());
        }
    }

    private static byte[] ascii(String s) {
        byte[] b = new byte[s.length()];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) s.charAt(i);
        }
        return b;
    }

    // --------------------------------------------------------------- HTTP

    @Override
    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
        if (graphiqlPath != null && request.pathIs(graphiqlPath)) {
            return new HttpServer.Response(200, "text/html;charset=utf-8", utf8(graphiqlPage()));
        }
        if (!request.pathIs(pathBytes)) {
            return null;
        }
        String method = request.getMethod();
        boolean get = "GET".equals(method);
        if (!get && !"POST".equals(method)) {
            return notAllowed();
        }
        String query;
        String operationName;
        Map variables = null;
        if (get) {
            query = request.queryParam("query");
            operationName = request.queryParam("operationName");
            String vars = request.queryParam("variables");
            if (vars != null && vars.length() > 0) {
                Object v = parseJson(vars);
                if (!(v instanceof Map)) {
                    return badRequest("variables must be a JSON object");
                }
                variables = (Map) v;
            }
        } else {
            String type = request.getHeader("content-type");
            String body = request.getBody();
            if (type != null && startsWithIgnoreCase(type, "application/graphql")
                    && !startsWithIgnoreCase(type, "application/graphql-response")) {
                query = body;
                operationName = request.queryParam("operationName");
            } else if (type == null || !startsWithIgnoreCase(type, "application/json")) {
                // A text/plain or form POST is a CORS "simple request": a hostile
                // page can send one with the victim's cookies and no preflight.
                // Only a JSON or GraphQL body is accepted, as Spring does.
                return json(415, "application/json",
                        error("Send the request as application/json."), null);
            } else {
                Object parsed = parseJson(body);
                if (!(parsed instanceof Map)) {
                    return badRequest("The body must be a JSON object with a query");
                }
                Map m = (Map) parsed;
                Object q = m.get("query");
                Object o = m.get("operationName");
                Object v = m.get("variables");
                if (!(q instanceof String) || (o != null && !(o instanceof String))
                        || (v != null && !(v instanceof Map))) {
                    return badRequest("query must be a string, operationName a string and "
                            + "variables an object");
                }
                query = (String) q;
                operationName = (String) o;
                variables = (Map) v;
            }
        }
        if (query == null || query.trim().length() == 0) {
            return badRequest("No query was given");
        }
        if (get && "mutation".equals(graphQL.operationType(query, operationName))) {
            // A GET must be safe: a link or a prefetch must never run a mutation.
            Map<String, String> allow = new HashMap<String, String>();
            allow.put("Allow", "POST");
            return respond(request, 405, error("Mutations are not allowed over GET."), allow);
        }
        return respond(request, 200, graphQL.execute(query, operationName, variables, request),
                null);
    }

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        return s.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static Object parseJson(String s) {
        try {
            return s == null || s.length() == 0 ? null : Json.parse(s);
        } catch (IOException e) {
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Map<String, Object> error(String message) {
        Map<String, Object> e = new LinkedHashMap<String, Object>();
        e.put("message", message);
        List<Object> errors = new ArrayList<Object>();
        errors.add(e);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("errors", errors);
        return out;
    }

    private HttpServer.Response badRequest(String message) {
        return json(400, "application/json", error(message), null);
    }

    private HttpServer.Response notAllowed() {
        Map<String, String> allow = new HashMap<String, String>();
        allow.put("Allow", "GET, POST");
        return json(405, "application/json", error("Use GET or POST."), allow);
    }

    private HttpServer.Response respond(HttpServer.Request request, int status, Map body,
                                        Map<String, String> headers) {
        String accept = request.getHeader("accept");
        boolean jsonOnly = accept != null && accept.indexOf("application/json") >= 0
                && accept.indexOf("application/graphql-response+json") < 0
                && accept.indexOf("*/*") < 0;
        return json(status, (jsonOnly ? "application/json" : RESPONSE_TYPE)
                + ";charset=utf-8", body, headers);
    }

    private static HttpServer.Response json(int status, String type, Map body,
                                            Map<String, String> headers) {
        byte[] bytes = utf8(Json.write(body));
        return headers == null ? new HttpServer.Response(status, type, bytes)
                : new HttpServer.Response(status, type, bytes, headers);
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8", e);
        }
    }

    private String graphiqlPage() {
        return "<!doctype html>\n<html><head><meta charset=\"utf-8\"><title>GraphiQL</title>\n"
                + "<style>body{margin:0;height:100vh}#graphiql{height:100vh}</style>\n"
                + "<link rel=\"stylesheet\" href=\"https://unpkg.com/graphiql@3.8.3/graphiql.min.css\">\n"
                + "<script src=\"https://unpkg.com/react@18.3.1/umd/react.production.min.js\"></script>\n"
                + "<script src=\"https://unpkg.com/react-dom@18.3.1/umd/react-dom.production.min.js\"></script>\n"
                + "<script src=\"https://unpkg.com/graphiql@3.8.3/graphiql.min.js\"></script>\n"
                + "</head><body><div id=\"graphiql\"></div><script>\n"
                + "var u=new URL(" + jsString(path) + ",location.href);\n"
                + "var w=" + (webSocketPath == null ? "undefined"
                : "new URL(" + jsString(webSocketPath) + ",location.href).href"
                + ".replace(/^http/,'ws')") + ";\n"
                + "ReactDOM.createRoot(document.getElementById('graphiql')).render("
                + "React.createElement(GraphiQL,{fetcher:GraphiQL.createFetcher("
                + "{url:u.href,subscriptionUrl:w})}));\n"
                + "</script></body></html>\n";
    }

    private static String jsString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20 || c == '"' || c == '\\' || c == '<' || c == '>' || c > 0x7e) {
                String hex = Integer.toHexString(c);
                sb.append("\\u");
                for (int p = hex.length(); p < 4; p++) {
                    sb.append('0');
                }
                sb.append(hex);
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    // ---------------------------------------------------------- WebSocket

    /// One connection's state: whether it was acknowledged, and its running
    /// subscriptions by id.
    /// Subscriptions one connection may hold open at once.
    static final int MAX_SUBSCRIPTIONS = 100;
    /// How long a socket may stay open without `connection_init`, as Spring's
    /// default `connectionInitTimeout`.
    static final long INIT_TIMEOUT_MS = 60000L;

    private static final class Connection {
        boolean initialized;
        final long opened = System.currentTimeMillis();
        final Map<String, Runnable> running = new HashMap<String, Runnable>();
    }

    /// The `graphql-transport-ws` protocol: `connection_init` and
    /// `connection_ack`, `ping` and `pong`, then `subscribe`, `next`, `error`
    /// and `complete` per operation id. Violations close the socket with the
    /// protocol's codes (4400 bad message, 4401 unauthorized, 4409 duplicate
    /// id, 4429 a second init).
    final class Subscriptions implements WebSocket {
        @Override
        public String[] getSubprotocols() {
            return new String[] {PROTOCOL};
        }

        @Override
        public void onOpen(WebSocketSession session) {
            session.setAttachment(new Connection());
            watchInit(session);
        }

        @Override
        public void onText(final WebSocketSession session, String message) throws Exception {
            final Connection c = (Connection) session.getAttachment();
            Object parsed = parseJson(message);
            if (!(parsed instanceof Map) || !(((Map) parsed).get("type") instanceof String)) {
                session.close(4400, "Invalid message");
                return;
            }
            Map m = (Map) parsed;
            String type = (String) m.get("type");
            if ("connection_init".equals(type)) {
                boolean again;
                synchronized (c) {
                    again = c.initialized;
                    c.initialized = true;
                }
                if (again) {
                    session.close(4429, "Too many initialisation requests");
                    return;
                }
                send(session, c, message("connection_ack", null, null));
                return;
            }
            if ("ping".equals(type)) {
                send(session, c, message("pong", null, null));
                return;
            }
            if ("pong".equals(type)) {
                return;
            }
            boolean initialized;
            synchronized (c) {
                initialized = c.initialized;
            }
            if (!initialized) {
                session.close(4401, "Unauthorized");
                return;
            }
            Object idValue = m.get("id");
            if (!(idValue instanceof String)) {
                session.close(4400, "Invalid message: no id");
                return;
            }
            final String id = (String) idValue;
            if ("complete".equals(type)) {
                Runnable stop;
                synchronized (c) {
                    stop = c.running.remove(id);
                }
                if (stop != null) {
                    stop.run();
                }
                return;
            }
            if (!"subscribe".equals(type)) {
                session.close(4400, "Invalid message type " + type);
                return;
            }
            Object payload = m.get("payload");
            Object q = payload instanceof Map ? ((Map) payload).get("query") : null;
            Object o = payload instanceof Map ? ((Map) payload).get("operationName") : null;
            Object v = payload instanceof Map ? ((Map) payload).get("variables") : null;
            if (!(q instanceof String) || (o != null && !(o instanceof String))
                    || (v != null && !(v instanceof Map))) {
                session.close(4400, "Invalid subscribe payload");
                return;
            }
            final Runnable placeholder = new Runnable() {
                @Override
                public void run() {
                }
            };
            synchronized (c) {
                if (c.running.containsKey(id)) {
                    session.close(4409, "Subscriber for " + id + " already exists");
                    return;
                }
                if (c.running.size() >= MAX_SUBSCRIPTIONS) {
                    session.close(4400, "Too many subscriptions");
                    return;
                }
                c.running.put(id, placeholder);
            }
            Runnable stop = graphQL.subscribe((String) q, (String) o, (Map) v, session,
                    new GraphQL.SubscriptionListener() {
                        @Override
                        public void next(Map<String, Object> result) {
                            sendIfRunning(session, c, id, message("next", id, result));
                        }

                        @Override
                        public void error(Map<String, Object> result) {
                            if (finish(c, id)) {
                                send(session, c, message("error", id, result.get("errors")));
                            }
                        }

                        @Override
                        public void complete() {
                            if (finish(c, id)) {
                                send(session, c, message("complete", id, null));
                            }
                        }
                    });
            boolean cancelled = false;
            synchronized (c) {
                if (c.running.get(id) == placeholder) { // NOPMD CompareObjectsWithEquals - this subscription, not an equal one
                    if (stop != null) {
                        c.running.put(id, stop);
                    }
                } else {
                    // Completed or cancelled while it was starting.
                    cancelled = c.running.get(id) == null;
                }
            }
            if (cancelled && stop != null) {
                stop.run();
            }
        }

        /// One lock for the check and the send, so a `complete` cannot slip
        /// between them and be followed by a `next` for the finished id.
        private void sendIfRunning(WebSocketSession session, Connection c, String id,
                                   String text) {
            synchronized (c) {
                if (c.running.containsKey(id)) {
                    send(session, c, text);
                }
            }
        }

        private boolean finish(Connection c, String id) {
            synchronized (c) {
                return c.running.remove(id) != null;
            }
        }

        @Override
        public void onBinary(WebSocketSession session, byte[] message, int offset, int length) {
            session.close(4400, "Binary messages are not part of the protocol");
        }

        @Override
        public void onClose(WebSocketSession session, int code, String reason) {
            Connection c = (Connection) session.getAttachment();
            if (c == null) {
                return;
            }
            List<Runnable> stops;
            synchronized (c) {
                stops = new ArrayList<Runnable>(c.running.values());
                c.running.clear();
            }
            for (Runnable r : stops) {
                try {
                    r.run();
                } catch (RuntimeException e) {
                    LOG.warn("Stopping a GraphQL subscription failed", e);
                }
            }
        }
    }

    private static String message(String type, String id, Object payload) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("type", type);
        if (id != null) {
            m.put("id", id);
        }
        if (payload != null) {
            m.put("payload", payload);
        }
        return Json.write(m);
    }

    /// Sends one message; frames from several subscriptions on one connection
    /// are serialized on the connection's state.
    private final List<WebSocketSession> awaitingInit = new ArrayList<WebSocketSession>();
    private Thread initWatcher;

    /// Closes with 4408 a socket that sends no `connection_init` in time. One
    /// thread per endpoint, started with the first socket and ended when none
    /// is waiting, rather than a timer per connection.
    private void watchInit(WebSocketSession session) {
        synchronized (awaitingInit) {
            awaitingInit.add(session);
            if (initWatcher != null) {
                return;
            }
            initWatcher = new Thread(new Runnable() {
                @Override
                public void run() {
                    reapUninitialized();
                }
            }, "graphql-ws-init");
            initWatcher.setDaemon(true);
            initWatcher.start();
        }
    }

    private void reapUninitialized() {
        while (true) {
            List<WebSocketSession> expired = new ArrayList<WebSocketSession>();
            synchronized (awaitingInit) {
                long now = System.currentTimeMillis();
                for (int i = awaitingInit.size() - 1; i >= 0; i--) {
                    WebSocketSession s = awaitingInit.get(i);
                    Object a = s.getAttachment();
                    boolean done = !s.isOpen() || !(a instanceof Connection);
                    if (!done) {
                        Connection c = (Connection) a;
                        synchronized (c) {
                            done = c.initialized;
                        }
                        if (!done && now - c.opened >= INIT_TIMEOUT_MS) {
                            expired.add(s);
                            done = true;
                        }
                    }
                    if (done) {
                        awaitingInit.remove(i);
                    }
                }
                if (awaitingInit.isEmpty()) {
                    initWatcher = null;
                }
            }
            for (WebSocketSession s : expired) {
                s.close(4408, "Connection initialisation timeout");
            }
            synchronized (awaitingInit) {
                if (initWatcher != Thread.currentThread()) { // NOPMD CompareObjectsWithEquals - is this thread still the watcher
                    return;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                synchronized (awaitingInit) {
                    if (initWatcher == Thread.currentThread()) { // NOPMD CompareObjectsWithEquals - is this thread still the watcher
                        initWatcher = null;
                    }
                }
                return;
            }
        }
    }

    private static void send(WebSocketSession session, Connection c, String text) {
        synchronized (c) {
            if (!session.isOpen()) {
                return;
            }
            try {
                session.sendText(text);
            } catch (IOException e) {
                session.abort();
            }
        }
    }
}
