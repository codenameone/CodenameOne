/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.transpiler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// The server the `http_*` conformance programs talk to, for both arms: the
/// test starts it in process, and the expected output is made by running
/// the real `requests` and `httpx` against `main(port)`.
///
/// Routes: `/get` and `/anything` echo the request as JSON (method, query,
/// body, content type and every `X-` header, keys sorted); `/status/<n>`
/// answers `n`; `/redirect` sends 302 to `/get?redirected=1`;
/// `/headers-out` sets response headers; `/basic-auth` wants user:pass;
/// `/bad-json` answers a body that is not JSON.
public final class HttpTestServer {
    private final HttpServer server;

    public HttpTestServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", HttpTestServer::handle);
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void stop() {
        server.stop(0);
    }

    public static void main(String[] args) throws Exception {
        HttpTestServer s = new HttpTestServer(args.length > 0 ? Integer.parseInt(args[0]) : 0);
        System.out.println(s.port());
        System.out.flush();
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void handle(HttpExchange x) throws IOException {
        String path = x.getRequestURI().getPath();
        byte[] body = readAll(x.getRequestBody());
        try {
            if (path.equals("/get") || path.equals("/anything")) {
                send(x, 200, "application/json", echo(x, body));
            } else if (path.startsWith("/status/")) {
                int code = Integer.parseInt(path.substring("/status/".length()));
                send(x, code, "text/plain", "status " + code);
            } else if (path.equals("/redirect")) {
                x.getResponseHeaders().add("Location", "/get?redirected=1");
                send(x, 302, "text/plain", "");
            } else if (path.equals("/headers-out")) {
                x.getResponseHeaders().add("X-One", "1");
                x.getResponseHeaders().add("X-Two", "two");
                send(x, 200, "text/plain; charset=utf-8", "headers");
            } else if (path.equals("/basic-auth")) {
                String auth = x.getRequestHeaders().getFirst("Authorization");
                if ("Basic dXNlcjpwYXNz".equals(auth)) {
                    send(x, 200, "application/json", "{\"authenticated\": true}");
                } else {
                    send(x, 401, "application/json", "{\"authenticated\": false}");
                }
            } else if (path.equals("/bad-json")) {
                send(x, 200, "application/json", "{not json");
            } else {
                send(x, 404, "text/plain", "no route " + path);
            }
        } finally {
            x.close();
        }
    }

    private static String echo(HttpExchange x, byte[] body) throws IOException {
        Map<String, String> fields = new TreeMap<String, String>();
        fields.put("method", quote(x.getRequestMethod()));
        Map<String, String> args = new TreeMap<String, String>();
        String q = x.getRequestURI().getRawQuery();
        if (q != null && !q.isEmpty()) {
            for (String kv : q.split("&")) {
                int eq = kv.indexOf('=');
                String k = URLDecoder.decode(eq < 0 ? kv : kv.substring(0, eq), "UTF-8");
                String v = eq < 0 ? "" : URLDecoder.decode(kv.substring(eq + 1), "UTF-8");
                args.merge(k, v, (a, b) -> a + "," + b);
            }
        }
        fields.put("args", object(args));
        Map<String, String> headers = new TreeMap<String, String>();
        for (Map.Entry<String, List<String>> e : x.getRequestHeaders().entrySet()) {
            String k = e.getKey().toLowerCase(java.util.Locale.ROOT);
            if (k.startsWith("x-")) {
                headers.put(k, String.join(",", e.getValue()));
            }
        }
        fields.put("headers", object(headers));
        String ct = x.getRequestHeaders().getFirst("Content-Type");
        fields.put("content_type", ct == null ? "null" : quote(ct));
        fields.put("body", quote(new String(body, StandardCharsets.UTF_8)));
        return object0(fields);
    }

    private static String object(Map<String, String> m) {
        Map<String, String> q = new TreeMap<String, String>();
        for (Map.Entry<String, String> e : m.entrySet()) {
            q.put(e.getKey(), quote(e.getValue()));
        }
        return object0(q);
    }

    private static String object0(Map<String, String> m) {
        List<String> parts = new ArrayList<String>();
        for (Map.Entry<String, String> e : m.entrySet()) {
            parts.add(quote(e.getKey()) + ": " + e.getValue());
        }
        return "{" + String.join(", ", parts) + "}";
    }

    private static String quote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') {
                b.append('\\').append(c);
            } else if (c < 0x20 || c > 0x7e) {
                b.append(String.format("\\u%04x", (int) c));
            } else {
                b.append(c);
            }
        }
        return b.append('"').toString();
    }

    private static void send(HttpExchange x, int code, String type, String text) throws IOException {
        byte[] b = text.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().add("Content-Type", type);
        x.sendResponseHeaders(code, b.length == 0 ? -1 : b.length);
        if (b.length > 0) {
            try (OutputStream o = x.getResponseBody()) {
                o.write(b);
            }
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        for (int n = in.read(buf); n > 0; n = in.read(buf)) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
