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

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.Signals;

/**
 * The Codename One half of the Go comparison.
 *
 * Two routes, deliberately the two TechEmpower framework-benchmark shapes, so the
 * numbers here can be read against published ones as well as against the Go
 * server beside them:
 *
 *   /plaintext   text/plain, a fixed 13-byte body
 *   /json        application/json, a small object serialised per request
 *
 * The JSON is built per request rather than served from a constant, because
 * serialisation is part of what is being compared; the plaintext route is the one
 * that measures the HTTP path with nothing else in it.
 *
 * bench-server.go is line-for-line the same two handlers on net/http. Anything
 * this file does that that one does not -- or the other way round -- is a
 * difference in the measurement, not in the runtimes, so keep them matched.
 */
public class Bench {
    private static final byte[] PLAINTEXT = bytes("Hello, World!");
    private static final int RESPONSE_MODE = envInt("BENCH_REUSE_RESPONSE", 1);

    /**
     * 0 = build a LinkedHashMap per request (what a hand-written handler does),
     * 1 = reuse one map (isolates construction from serialising),
     * 2 = write the fields directly (what the annotation processor now emits).
     */
    private static final int JSON_MODE = envInt("BENCH_JSON_MODE", 0);

    /** Reused; the object is immutable and the writer holds no state. */
    private static final com.codename1.backend.Json.Writable MESSAGE_WRITABLE =
            new com.codename1.backend.Json.Writable() {
        public void writeTo(com.codename1.backend.ByteSink out) {
            out.put('{');
            out.putAscii("\"message\":");
            com.codename1.backend.Json.writeString("Hello, World!", out);
            out.put('}');
        }
    };

    private static final Map HOISTED = new LinkedHashMap();
    static {
        HOISTED.put("message", "Hello, World!");
    }

    public static void main(String[] args) throws Exception {
        Signals.installShutdownHandler();
        int port = envInt("PORT", 8080);
        int workers = envInt("WORKERS", 16);
        int backlog = envInt("BACKLOG", 1024);

        // DIAGNOSTIC (BENCH_IDLE_THREADS=N): park N Java threads that do nothing.
        //
        // The collector stops and scans threads ONE AT A TIME, and it rebuilds its
        // virtual-thread snapshot inside that per-thread loop -- so the more Java
        // threads exist, the longer a snapshot stays live while OTHER threads are
        // still running and still free virtual threads. Idle threads therefore
        // widen a race they take no part in. This reproduces that without the
        // worker pool, which used to supply the threads and no longer exists in
        // virtual-thread mode.
        int idle = envInt("BENCH_IDLE_THREADS", 0);
        for(int iter = 0 ; iter < idle ; iter++) {
            Thread parked = new Thread(new Runnable() {
                public void run() {
                    while(true) {
                        try {
                            Thread.sleep(3600000);
                        } catch (InterruptedException err) {
                            return;
                        }
                    }
                }
            });
            parked.setDaemon(true);
            parked.start();
        }

        final HttpServer server = HttpServer.start(null, port, backlog, workers,
                new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                String target = request.getTarget();
                // startsWith, so BENCH_VARY_TARGETS can drive a DISTINCT target per
                // request (/plaintext?u=N) and still be served 200 + keep-alive.
                // Matching exactly sends those to the 404 path, which replies
                // Connection: close -- and a red-team run of the target cache then
                // measures connection teardown rather than the cache: 292 requests
                // in 5.1s with 758,381 write errors. Prefix matching keeps the
                // adversarial case on the same code path as the normal one.
                if(target.startsWith("/plaintext")) {
                    // BENCH_REUSE_RESPONSE=1: hand back one shared Response
                    // instead of building one per request.
                    //
                    // Not a shippable handler -- it measures a CEILING. Response
                    // is the only per-request allocation left on this route (88
                    // bytes), and the server only ever READS it, so sharing one
                    // is safe here and answers what pooling would be worth before
                    // any public API is changed to allow it.
                    // No per-request Response: the connection's own is re-pointed.
                    // BENCH_REUSE_RESPONSE=0 restores the allocating path, which is
                    // what the comparison measures against.
                    // 0 = allocate per request
                    // 1 = pooled, re-pointed via respond() (nine field writes)
                    // 2 = pooled but PRE-SET, returned untouched
                    //
                    // Mode 2 exists to separate two things mode 1 conflates: the
                    // saved allocation, and the cost of writing the fields into a
                    // connection-cold object instead of a bump-allocated one that
                    // is still warm in cache. Only valid because this route always
                    // answers with the same status, type and body.
                    if(RESPONSE_MODE == 2) {
                        HttpServer.Response r = request.presetResponse();
                        if(r != null) {
                            return r;
                        }
                    }
                    if(RESPONSE_MODE == 1) {
                        return request.respond(200, "text/plain", PLAINTEXT);
                    }
                    return new HttpServer.Response(200, "text/plain", PLAINTEXT);
                }
                if("/json".equals(target)) {
                    // DIAGNOSTIC SPLIT (BENCH_JSON_HOIST=1): reuse one map instead
                    // of building it per request.
                    //
                    // Not a shippable handler -- a real one has different values
                    // each time -- but it separates the two costs this route pays.
                    // Go encodes a STRUCT with a cached per-type encoder; we build
                    // a LinkedHashMap, hash a key, insert, then walk it. Those are
                    // not the same work, so before concluding "our JSON serialiser
                    // is slow" it is worth knowing how much of the gap is the
                    // container rather than the serialising.
                    //
                    // Recovers most of the gap -> the fix is a struct-shaped API in
                    // plain Java. Recovers little -> the cost really is in the byte
                    // writer, and porting that to C is justified.
                    //
                    // ANSWERED, and it is the container. Two pinned cores, 64
                    // connections, interleaved with rotating arm order, n=2:
                    //
                    //     generated DTO (2)   595158 rps   2.619 us/req
                    //     fasthttp            585906 rps   2.525 us/req
                    //     hoisted map (1)     547423 rps   2.849 us/req
                    //     map per request (0) 338167 rps   4.512 us/req
                    //
                    // Building the map costs 1.9 us of the 2.0 us that separated
                    // this route from Go -- hoisting it alone recovers most of that,
                    // and the struct-shaped writer recovers the rest and passes
                    // fasthttp. So the byte writer does NOT need porting to C: at
                    // mode 2 it is already serialising this object for less cpu than
                    // Go spends on the equivalent, and what looked like a serialiser
                    // gap was a LinkedHashMap allocated, hashed, inserted into and
                    // walked once per request.
                    //
                    // Mode 0 stays the default because it is the honest cost of a
                    // handler that hands back a Map, which is what an unannotated
                    // one does. An annotated DTO gets mode 2's shape from the
                    // processor without the author writing any of it.
                    if(JSON_MODE == 1) {
                        return HttpServer.Response.jsonValue(200, HOISTED);
                    }
                    if(JSON_MODE == 3) {
                        // Mode 2's writer on the connection's pooled Response, so the
                        // route allocates nothing at all. Mode 2 stays as it was so the
                        // cost of the Response itself remains measurable against it.
                        return request.respondJson(200, MESSAGE_WRITABLE);
                    }
                    if(JSON_MODE == 2) {
                        // What the annotation processor now emits for a DTO: no
                        // map, no key hashing, no walk, no instanceof per value --
                        // the field name is a literal and the value takes the
                        // writer its static type selects. Hand-written here only
                        // because this benchmark handler is not annotated; the
                        // generated PetJson.toJson has exactly this shape.
                        return HttpServer.Response.jsonValue(200, MESSAGE_WRITABLE);
                    }
                    // Serialised per request, because the Go side encodes a struct
                    // per request. Returning a constant string here would compare
                    // our memcpy against their reflection.
                    Map out = new LinkedHashMap();
                    out.put("message", "Hello, World!");
                    // jsonValue, not json(Json.write(...)): the map is serialised
                    // straight into the connection's write buffer. The Go side
                    // encodes a struct per request, so this stays a real
                    // serialisation rather than a hoisted constant.
                    return HttpServer.Response.jsonValue(200, out);
                }
                return HttpServer.Response.text(404, "not found");
            }
        }, null);

        System.out.println("bench listening on port " + server.getPort()
                + " with " + workers + " workers");
        Signals.onShutdown(new Runnable() {
            public void run() {
                server.stop(2000);
                System.exit(0);
            }
        });
        server.awaitTermination();
    }

    private static byte[] bytes(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (Exception err) {
            return new byte[0];
        }
    }

    private static int envInt(String name, int fallback) {
        String value = System.getenv(name);
        if(value == null || value.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException err) {
            return fallback;
        }
    }
}
