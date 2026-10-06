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
package com.codename1.backend.security;

import com.codename1.backend.Backend;
import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.BackendApplication;
import com.codename1.impl.backend.Scheduler;
import com.codename1.impl.backend.WiringEnvironment;
import com.codename1.impl.backend.security.SecuritySupport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/// A running backend whose only beans are security chains, started the way the
/// generated wiring starts one: the chains are built from an HttpSecurity each
/// and registered with their order, and the layer is linked through the
/// runtime's internal access.
final class SecuredServer implements AutoCloseable {
    /// One `SecurityFilterChain` bean method.
    interface Chain {
        SecurityFilterChain build(HttpSecurity http) throws Exception;
    }

    /// One answer, read out of the response while it is still valid.
    static final class Reply {
        final int status;
        final String body;
        final List<String[]> headers;

        Reply(int status, String body, List<String[]> headers) {
            this.status = status;
            this.body = body;
            this.headers = headers;
        }

        /// The first value of a header, in any case, or null.
        String header(String name) {
            for (String[] h : headers) {
                if (h[0].equalsIgnoreCase(name)) {
                    return h[1];
                }
            }
            return null;
        }

        List<String> headers(String name) {
            List<String> out = new ArrayList<String>();
            for (String[] h : headers) {
                if (h[0].equalsIgnoreCase(name)) {
                    out.add(h[1]);
                }
            }
            return out;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder().append(status);
            for (String[] h : headers) {
                sb.append(" [").append(h[0]).append(": ").append(h[1]).append(']');
            }
            return sb.append(' ').append(body).toString();
        }
    }

    final Backend backend;
    /// The cookies a browser would hold: name to value, updated by every reply.
    final Map<String, String> cookies = new LinkedHashMap<String, String>();
    /// What the handler was asked for, in order.
    final List<String> reached = new ArrayList<String>();

    private SecuredServer(Backend backend) {
        this.backend = backend;
    }

    static Properties settings() {
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, "0");
        return settings;
    }

    static SecuredServer start(HttpServer.Handler handler, Chain... chains) throws Exception {
        return start(settings(), "test", new Object[0], handler, chains);
    }

    static SecuredServer start(Properties settings, String profile, Object[] beans,
                               HttpServer.Handler handler, Chain... chains) throws Exception {
        return start(settings, profile, beans, handler, null, chains);
    }

    /// @param beans what the application's other beans would be: a user store,
    /// a password encoder
    /// @param socket an endpoint served at the exact route `/ws` and, through a
    /// fallback router, at `/ws-routed`; null for none
    static SecuredServer start(Properties settings, String profile, final Object[] beans,
                               final HttpServer.Handler handler,
                               final com.codename1.backend.WebSocket socket,
                               final Chain... chains) throws Exception {
        final SecuredServer[] self = new SecuredServer[1];
        final List<String> reached = new ArrayList<String>();
        BackendApplication application = new Application() {
            @Override
            public void registerWebSockets(HttpServer.WebSocketRegistry registry) {
                if (socket != null) {
                    registry.route("/ws", socket);
                    registry.fallback(new HttpServer.WebSocketHandler() {
                        @Override
                        public com.codename1.backend.WebSocket open(HttpServer.Request request) {
                            return "/ws-routed".equals(request.pathFrom(0)) ? socket : null;
                        }
                    });
                }
            }

            @Override
            public HttpServer.Handler[] create(WiringEnvironment environment) throws Exception {
                for (int iter = 0 ; iter < chains.length ; iter++) {
                    // Position is the order, as @Order(1), @Order(2) would be.
                    environment.registerSecurityFilterChain(chains[iter].build(
                            SecuritySupport.http(environment.getConfig(), beans)), iter + 1);
                }
                return new HttpServer.Handler[] {new HttpServer.Handler() {
                    @Override
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        synchronized (reached) {
                            reached.add(request.getMethod() + " " + request.pathFrom(0));
                        }
                        return handler.handle(request);
                    }
                }};
            }
        };
        Backend.Builder builder = Backend.builder(Config.of(settings, profile)).quiet()
                .host("127.0.0.1");
        BackendAccess.get().application(builder, application);
        BackendAccess.get().security(builder);
        self[0] = new SecuredServer(builder.start());
        self[0].reachedSource = reached;
        return self[0];
    }

    private List<String> reachedSource;

    /// What reached the application's handler so far.
    List<String> reached() {
        synchronized (reachedSource) {
            return new ArrayList<String>(reachedSource);
        }
    }

    int port() {
        return backend.getServer().getPort();
    }

    Reply get(String target, String... headers) throws Exception {
        return call("GET", target, null, null, headers);
    }

    /// A form post, as a browser sends one.
    Reply post(String target, String form, String... headers) throws Exception {
        return call("POST", target, form, "application/x-www-form-urlencoded", headers);
    }

    /// One request through the server's whole request path, in process, carrying
    /// the cookies earlier replies set and keeping the ones this reply sets.
    Reply call(String method, String target, String body, String contentType, String... headers)
            throws Exception {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("Host", "localhost");
        if (!cookies.isEmpty()) {
            StringBuilder jar = new StringBuilder();
            for (Map.Entry<String, String> e : cookies.entrySet()) {
                jar.append(jar.length() == 0 ? "" : "; ").append(e.getKey()).append('=')
                        .append(e.getValue());
            }
            map.put("Cookie", jar.toString());
        }
        byte[] bytes = body == null ? null : body.getBytes("UTF-8");
        if (bytes != null) {
            map.put("Content-Type", contentType);
            map.put("Content-Length", String.valueOf(bytes.length));
        }
        for (int iter = 0 ; iter + 1 < headers.length ; iter += 2) {
            map.put(headers[iter], headers[iter + 1]);
        }
        BackendAccess access = BackendAccess.get();
        HttpServer.Response response = access.dispatch(backend, method, target, map, bytes);
        List<String[]> out = new ArrayList<String[]>();
        for (Object header : access.headers(response)) {
            out.add((String[]) header);
        }
        Reply reply = new Reply(access.status(response),
                new String(access.body(response), "UTF-8"), out);
        for (String cookie : reply.headers("Set-Cookie")) {
            int eq = cookie.indexOf('=');
            int semi = cookie.indexOf(';');
            String name = cookie.substring(0, eq);
            String value = cookie.substring(eq + 1, semi < 0 ? cookie.length() : semi);
            if (cookie.contains("Max-Age=0")) {
                cookies.remove(name);
            } else {
                cookies.put(name, value);
            }
        }
        return reply;
    }

    /// Sends each of `requests` in turn on ONE connection and returns the status
    /// line and body of each answer, as `status / body`: what a keep-alive
    /// client sees.
    List<String> onOneConnection(String... requests) throws IOException {
        List<String> out = new ArrayList<String>();
        try (Socket socket = new Socket("127.0.0.1", port())) {
            socket.setSoTimeout(10000);
            OutputStream to = socket.getOutputStream();
            InputStream from = socket.getInputStream();
            for (String request : requests) {
                to.write(request.getBytes("ISO-8859-1"));
                to.flush();
                String head = readHead(from);
                String status = head.substring(0, head.indexOf('\r'));
                int length = 0;
                for (String line : head.split("\r\n")) {
                    if (line.regionMatches(true, 0, "content-length:", 0, 15)) {
                        length = Integer.parseInt(line.substring(15).trim());
                    }
                }
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                for (int read = 0 ; read < length ; read++) {
                    int b = from.read();
                    if (b < 0) {
                        throw new IOException("the body ended early");
                    }
                    body.write(b);
                }
                out.add(status + " / " + new String(body.toByteArray(), "UTF-8"));
            }
        }
        return out;
    }

    private static String readHead(InputStream from) throws IOException {
        ByteArrayOutputStream head = new ByteArrayOutputStream();
        int matched = 0;
        while (matched < 4) {
            int b = from.read();
            if (b < 0) {
                throw new IOException("the connection closed before a response: " + head);
            }
            head.write(b);
            matched = (b == '\r' && (matched == 0 || matched == 2))
                    || (b == '\n' && (matched == 1 || matched == 3)) ? matched + 1
                    : b == '\r' ? 1 : 0;
        }
        return new String(head.toByteArray(), "ISO-8859-1");
    }

    /// A GET on its own connection, as a keep-alive request.
    static String request(String target, String... headerLines) {
        StringBuilder sb = new StringBuilder("GET ").append(target)
                .append(" HTTP/1.1\r\nHost: 127.0.0.1\r\n");
        for (String line : headerLines) {
            sb.append(line).append("\r\n");
        }
        return sb.append("\r\n").toString();
    }

    @Override
    public void close() {
        backend.stop();
    }

    /// An application with nothing but what a test overrides.
    abstract static class Application implements BackendApplication {
        @Override
        public void registerWebSockets(HttpServer.WebSocketRegistry registry) throws Exception {
        }

        @Override
        public void started(Backend backend) {
        }

        @Override
        public void stopping() {
        }

        @Override
        public void stopped() {
        }

        @Override
        public boolean tracksCurrentRequest() {
            return false;
        }

        @Override
        public void requestEnded(Object[] beans) {
        }

        @Override
        public void sessionEnded(Object[] beans) {
        }

        @Override
        public Scheduler getScheduler() {
            return null;
        }

        @Override
        public List describeBeans() {
            return new ArrayList();
        }

        @Override
        public List describeRoutes() {
            return new ArrayList();
        }
    }
}
