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
package com.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import com.codename1.backend.Db;
import com.codename1.backend.DbPool;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.Signals;
import com.codename1.backend.StaticFiles;
import com.codename1.backend.Tls;

/**
 * The same contract, the same service, a different front end.
 *
 * Nothing about GreeterApi, GreeterApiDispatcher, PetJson or GreeterService knows
 * whether it is behind a Lambda or a socket -- which is the point of generating the
 * dispatcher from the contract rather than writing a router per deployment. Greeter
 * (the Lambda) and this file are the only two things that differ, and both are
 * transport glue.
 */
public class PetServer {
    public static void main(String[] args) throws Exception {
        Signals.installShutdownHandler();
        int port = envInt("CN1_PORT", 8080);
        int workers = envInt("CN1_WORKERS", 16);
        String dbPath = System.getenv("CN1_DB_PATH");

        final DbPool pool;
        final GreeterService service;
        if(dbPath == null || ":memory:".equals(dbPath)) {
            // An in-memory database cannot be pooled: each connection would get its
            // own. One shared connection is correct here -- and Db serializes a
            // whole transaction, which is the part SQLite does NOT do for you: it
            // serializes each call, so without that a second request could execute
            // between another's BEGIN and COMMIT.
            pool = null;
            service = new GreeterService(Db.open(":memory:"));
        } else {
            // The POOL, not one connection out of it. Borrowing one here and sharing
            // it left the rest of the pool idle and let concurrent requests interleave
            // on the same connection -- a plain insert could land inside another
            // request's transaction and be rolled back with it.
            pool = DbPool.open(dbPath, Math.max(2, workers / 4), 5000);
            service = new GreeterService(pool);
        }
        final GreeterApiDispatcher dispatcher = new GreeterApiDispatcher(service);

        // Static files are served from CN1_STATIC_ROOT when it is set. They are
        // tried only AFTER the API, so a file can never shadow a route.
        String staticRoot = System.getenv("CN1_STATIC_ROOT");
        final StaticFiles files = staticRoot == null ? null
                : new StaticFiles(staticRoot, "/static", "index.html", "public, max-age=3600");
        if(files != null) {
            System.out.println("serving " + staticRoot + " at /static"
                    + (StaticFiles.isZeroCopy() ? " (sendfile)" : " (read/write)"));
        }

        // TLS is terminated here when a certificate is configured. Plaintext is the
        // right default behind a load balancer that already terminated it.
        String certPath = System.getenv("CN1_TLS_CERT");
        String keyPath = System.getenv("CN1_TLS_KEY");
        // HTTP/2 is advertised through ALPN; there is no other way to reach it over
        // TLS. Set CN1_HTTP2=0 to offer only http/1.1.
        boolean offerHttp2 = !"0".equals(System.getenv("CN1_HTTP2"));
        Tls tls = certPath == null || keyPath == null ? null
                : Tls.create(certPath, keyPath, offerHttp2);

        // The handler needs the server to report its own metrics, and the server
        // needs the handler to be constructed: one holder breaks the cycle.
        final HttpServer[] serverRef = new HttpServer[1];
        final HttpServer server = HttpServer.start(null, port, 512, workers, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                String method = request.getMethod();
                String target = request.getTarget();
                if("/healthz".equals(stripQuery(target))) {
                    return HttpServer.Response.json(200, Json.write(serverRef[0].getMetrics()));
                }
                // The DEFERRED json form: respondJson hands the value over
                // unserialised so the HTTP/1 writer can render it straight into
                // the connection buffer, which leaves response.body empty. Any
                // code that measures that array instead of rendering the value
                // reports zero for a representation that is not, and only a
                // handler shaped like this one can show it.
                if("/deferred".equals(stripQuery(target))) {
                    Map value = new LinkedHashMap();
                    value.put("name", "deferred");
                    value.put("digits", "1234567890");
                    // ?big=N pads the answer past the size at which the server
                    // stops copying a body into the head buffer and writes it
                    // separately. The padding is a run of one character so a
                    // truncated or doubled write is visible as a length.
                    String big = request.queryParam("big");
                    if(big != null) {
                        int n = 0;
                        try {
                            n = Integer.parseInt(big);
                        } catch (NumberFormatException ignored) {
                            n = 0;
                        }
                        if(n > 0 && n <= 4 * 1024 * 1024) {
                            StringBuilder pad = new StringBuilder();
                            for(int iter = 0 ; iter < n ; iter++) {
                                pad.append('x');
                            }
                            value.put("pad", pad.toString());
                        }
                    }
                    return request.respondJson(200, value);
                }
                // Deliberately a body on a status that cannot carry one. A handler
                // is allowed to build this -- the Response constructor takes any
                // status and any bytes -- and suppressing it is the server's job,
                // because writing it would leave the client reading those bytes as
                // the start of the next reply on a keep-alive connection.
                if("/nocontent".equals(stripQuery(target))) {
                    return new HttpServer.Response(204, "text/plain",
                            "junk".getBytes("UTF-8"));
                }
                // Also deliberately malformed, and for the same reason: a handler
                // can put anything in extraHeaders, and what it must never do is
                // reach the wire. A name with a space in it is not a field name,
                // and a name with a LEADING space is obsolete line folding, which
                // appends both to whatever header came before -- so a header the
                // handler could not have meant would silently rewrite one the
                // server owns.
                // 205 with bytes, for the same reason /nocontent exists: a
                // handler may build it, and RFC 9110 ends a Reset Content
                // response at the header section, so writing them would leave a
                // keep-alive client reading them as the next reply.
                // Echoes the VERB back, so a client can prove which one arrived
                // rather than which one it believes it sent. HttpServer routes
                // seven methods; this answers for any of them.
                if("/echo".equals(stripQuery(target))) {
                    String echoed = request.getBody();
                    return new HttpServer.Response(200, "text/plain",
                            ("method=" + method + " len="
                                    + (echoed == null ? 0 : echoed.length())).getBytes("UTF-8"));
                }
                // A body whose size the caller picks, so a test can ask for more
                // than one serialisation buffer's worth and check that every byte
                // still arrives. Deliberately NOT file-backed: the point is the
                // in-memory DATA-frame path, which is where the output buffer sits.
                if("/bulk".equals(stripQuery(target))) {
                    String sizeText = request.queryParam("size");
                    int size = sizeText == null ? 1024 : Integer.parseInt(sizeText);
                    byte[] payload = new byte[size];
                    for(int iter = 0 ; iter < size ; iter++) {
                        payload[iter] = (byte)('a' + (iter % 26));
                    }
                    return new HttpServer.Response(200, "text/plain", payload);
                }
                if("/reset".equals(stripQuery(target))) {
                    return new HttpServer.Response(205, "text/plain",
                            "junk".getBytes("UTF-8"));
                }
                if("/rawheader".equals(stripQuery(target))) {
                    Map extra = new LinkedHashMap();
                    extra.put("X-Good", "ok");
                    extra.put("X Bad", "space-in-name");
                    extra.put(" X-Fold", "obsolete-folding");
                    extra.put("X:Colon", "colon-in-name");
                    // A value taken STRAIGHT FROM THE REQUEST. The four above are
                    // malformed names the handler wrote itself; this is the shape
                    // the value rule actually exists for -- a handler reflecting a
                    // parameter into a header -- and it is the only one a client
                    // controls. ?v=%C4%8A decodes to U+010A, which is not '\n' to
                    // a char test and is byte 0x0A once narrowed for the wire.
                    String reflected = request.queryParam("v");
                    if(reflected != null) {
                        extra.put("X-Reflected", reflected);
                    }
                    return new HttpServer.Response(200, "text/plain",
                            "raw".getBytes("UTF-8"), extra);
                }
                // A non-ASCII parameter NAME, spelled as an escape so this source
                // stays ASCII. Every client percent-encodes such a name as its
                // UTF-8 octets, so the server has to compare it that way round.
                if("/accent".equals(stripQuery(target))) {
                    String value = request.queryParam("caf\u00e9");
                    return new HttpServer.Response(200, "text/plain",
                            ("caf\u00e9=" + value).getBytes("UTF-8"));
                }
                if(!dispatcher.hasRoute(method, target)) {
                    if(files != null) {
                        HttpServer.Response served = files.handle(request);
                        if(served != null) {
                            return served;
                        }
                    }
                    return HttpServer.Response.json(404,
                            "{\"error\":\"no route for " + method + " " + target + "\"}");
                }
                Object body = decodeBody(request.getBody());
                Object result;
                try {
                    result = dispatcher.dispatch(method, target, request.getHeaders(), body);
                } catch (SecurityException err) {
                    // Authentication or authorisation failed. A 500 here would be
                    // both wrong and unactionable for the client.
                    return HttpServer.Response.json(401, errorJson(err.getMessage()));
                } catch (IllegalArgumentException err) {
                    // The handler rejected the input; that is a 400, not a 500.
                    return HttpServer.Response.json(400, errorJson(err.getMessage()));
                }
                if(result == null) {
                    return HttpServer.Response.json(404, "{\"error\":\"not found\"}");
                }
                return HttpServer.Response.json(200, Json.write(result));
            }
        }, tls);
        serverRef[0] = server;
        System.out.println("listening on port " + server.getPort()
                + " with " + workers + " workers"
                + (tls == null ? " (plaintext)" : " (TLS)"));
        Signals.onShutdown(new Runnable() {
            public void run() {
                server.stop(10000);
                if(pool != null) {
                    pool.close();
                }
                System.out.println("stopped");
                // Signals ends the process once this returns. Exiting from here
                // would deadlock under the JavaSE implementation, where the same
                // body runs from a JVM shutdown hook.
            }
        });
        // Hold main here. The reactor and workers are detached threads, so a main
        // that returns ends the process with status 0 and no message.
        server.awaitTermination();
    }

    private static String stripQuery(String target) {
        int q = target == null ? -1 : target.indexOf('?');
        return q < 0 ? target : target.substring(0, q);
    }

    private static Object decodeBody(String raw) {
        if(raw == null || raw.length() == 0) {
            return null;
        }
        try {
            return Json.parse(raw);
        } catch (Exception err) {
            // Not JSON: hand it through as text so a @Body String still works.
            return raw;
        }
    }

    private static String errorJson(String message) {
        Map out = new LinkedHashMap();
        out.put("error", message == null ? "bad request" : message);
        return Json.write(out);
    }

    private static int envInt(String name, int fallback) {
        String v = System.getenv(name);
        if(v == null || v.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException err) {
            return fallback;
        }
    }
}
