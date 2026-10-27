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
package com.codename1.backend.test;

import com.codename1.backend.HttpServer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// An OTLP/HTTP receiver for tests: what a test points the exporters of a
/// [BackendTest.Feature#TELEMETRY] application at, to see that they send and
/// that they flush when the application stops.
///
/// It accepts every POST, answers 200 with an empty body, and keeps each
/// request's path and body. It decodes nothing -- the exporters' encoding has its
/// own tests -- so a test asserts on what arrived where, and how much.
///
/// ```java
/// static final OtlpTestCollector COLLECTOR = OtlpTestCollector.start();
///
/// @BackendTest(features = BackendTest.Feature.TELEMETRY,
///         properties = {"cn1.otel.endpoint=http://127.0.0.1:4318",
///                       "cn1.otel.metrics.intervalMillis=200"})
/// ```
///
/// A fixed endpoint in `properties` needs a fixed port: [#start(int)] takes one.
/// Runs on the JVM and compiled alike; it is an ordinary [HttpServer].
public final class OtlpTestCollector {
    private final HttpServer server;
    private final List<String> paths = new ArrayList<String>();
    private final List<byte[]> bodies = new ArrayList<byte[]>();

    private OtlpTestCollector(int port) throws IOException {
        server = HttpServer.start("127.0.0.1", port, 64, 2, new HttpServer.Handler() {
            @Override
            public HttpServer.Response handle(HttpServer.Request request) {
                record(request.getTarget(), request.getBodyBytes());
                return HttpServer.Response.text(200, "");
            }
        });
    }

    /// A collector on a free loopback port.
    public static OtlpTestCollector start() throws IOException {
        return new OtlpTestCollector(0);
    }

    /// A collector on `port`, for an endpoint a test names in its properties.
    public static OtlpTestCollector start(int port) throws IOException {
        return new OtlpTestCollector(port);
    }

    /// The port it listens on.
    public int getPort() {
        return server.getPort();
    }

    /// The base URL for `cn1.otel.endpoint`, `http://127.0.0.1:<port>`.
    public String getEndpoint() {
        return "http://127.0.0.1:" + getPort();
    }

    private synchronized void record(String path, byte[] body) {
        int query = path.indexOf('?');
        paths.add(query < 0 ? path : path.substring(0, query));
        bodies.add(body == null ? new byte[0] : body);
        notifyAll();
    }

    /// How many requests arrived at `path`, `/v1/traces` or `/v1/metrics`.
    public synchronized int count(String path) {
        int n = 0;
        for (String p : paths) {
            if (p.equals(path)) {
                n++;
            }
        }
        return n;
    }

    /// The bodies that arrived at `path`, oldest first.
    public synchronized List<byte[]> bodies(String path) {
        List<byte[]> out = new ArrayList<byte[]>();
        for (int i = 0; i < paths.size(); i++) {
            if (paths.get(i).equals(path)) {
                out.add(bodies.get(i));
            }
        }
        return out;
    }

    /// Waits until `count` requests have arrived at `path`; false when
    /// `timeoutMillis` passed first.
    public synchronized boolean await(String path, int count, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (count(path) < count) {
            long left = deadline - System.currentTimeMillis();
            if (left <= 0) {
                return false;
            }
            wait(left);
        }
        return true;
    }

    /// Forgets what arrived so far.
    public synchronized void clear() {
        paths.clear();
        bodies.clear();
    }

    /// Stops listening.
    public void stop() {
        server.stop();
    }
}
