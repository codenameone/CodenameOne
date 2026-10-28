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

import com.codename1.backend.ByteSink;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.Signals;

/**
 * The Codename One half of the Go and Rust comparisons.
 *
 * Two routes, deliberately the two TechEmpower framework-benchmark shapes, so the
 * numbers here can be read against published ones as well as against the servers
 * beside them:
 *
 *   /plaintext   text/plain, a fixed 13-byte body
 *   /json        application/json, a small object serialised per request
 *
 * Both are written the way an application's are, with no switches: /json builds
 * its object per request and answers through the shape the annotation processor
 * emits for a controller returning a DTO -- the build-time codec writing the
 * fields straight into the connection's buffer, on the connection's own Response.
 * bench-server.go and the headless comparison's servers do the same work.
 */
public class Bench {
    private static final byte[] PLAINTEXT = bytes("Hello, World!");

    /** The DTO a controller would return. */
    static final class Message {
        final String message;

        Message(String message) {
            this.message = message;
        }
    }

    /// The encoder the build generates for a route returning Message.
    private static final Json.Encoder MESSAGE_ENCODER = new Json.Encoder() {
        public void write(Object value, ByteSink out) {
            final Message result = (Message) value;
            out.put('{');
            out.putAscii("\"message\":");
            Json.writeString(result.message, out);
            out.put('}');
        }
    };

    public static void main(String[] args) throws Exception {
        Signals.installShutdownHandler();
        int port = envInt("PORT", 8080);
        int workers = envInt("WORKERS", 16);
        int backlog = envInt("BACKLOG", 1024);
        final HttpServer server = HttpServer.start(null, port, backlog, workers,
                new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                String target = request.getTarget();
                if(target.startsWith("/plaintext")) {
                    return request.respond(200, "text/plain", PLAINTEXT);
                }
                if("/json".equals(target)) {
                    return request.respondJson(200, new Message("Hello, World!"), MESSAGE_ENCODER);
                }
                return HttpServer.Response.text(404, "not found");
            }
        }, null);

        System.out.println("bench listening on port " + server.getPort()
                + " with " + workers + " workers");
        Signals.onShutdown(new Runnable() {
            public void run() {
                server.stop(2000);
                // Signals ends the process; see PetServer for why not here.
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
