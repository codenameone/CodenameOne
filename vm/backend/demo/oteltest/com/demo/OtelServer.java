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

import java.util.Map;

import com.codename1.backend.Backend;
import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Tracing;
import com.codename1.backend.Web;
import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.orm.EntityManager;
import com.codename1.backend.otel.OtlpTracer;

/**
 * A traced server, for BackendOtelTest to drive on the PACKAGED runtime.
 *
 * <p>Built the way the generated entry point builds one -- the builder with a
 * tracer -- and configured entirely from the environment, the way a deployment
 * configures it: OTEL_EXPORTER_OTLP_ENDPOINT names the test's collector.
 *
 * <p>GET /work does the three things a span is made for in one request: a
 * statement, and an outbound call that follows a redirect to a server the TEST
 * runs (CN1_OTEL_DOWNSTREAM), which records the trace context that arrived. Not
 * this server calling itself: on the packaged runtime an outbound call blocks the
 * host thread it runs on, and the request it makes can be queued behind it on
 * that same host.
 *
 * <p>/ws is a websocket whose onOpen runs a statement, so the test can see that a
 * handshake is a span and that onOpen's work is its child.
 */
public class OtelServer {
    public static void main(String[] args) throws Exception {
        final String downstream = System.getenv("CN1_OTEL_DOWNSTREAM");
        final Database db = Database.open(":memory:");
        db.execute("CREATE TABLE pets (id INTEGER PRIMARY KEY, name TEXT)", null);
        db.execute("INSERT INTO pets (id, name) VALUES (7, 'Rex')", null);
        Backend.builder()
                .tracing(new OtlpTracer("oteltest"))
                .webSockets(new Backend.WebSocketEndpoints() {
                    public void register(HttpServer.WebSocketRegistry registry,
                                         DataSource dataSource, EntityManager entities) {
                        registry.route("/ws", new WebSocket() {
                            public void onOpen(WebSocketSession session) throws Exception {
                                db.queryOne("SELECT name FROM pets WHERE id = ?",
                                        new Object[] {Long.valueOf(7)});
                            }

                            public void onText(WebSocketSession session, String message) {
                            }

                            public void onBinary(WebSocketSession session, byte[] message,
                                                 int offset, int length) {
                            }
                        });
                    }
                })
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request)
                            throws Exception {
                        String path = request.getTarget();
                        int query = path.indexOf('?');
                        if(query >= 0) {
                            path = path.substring(0, query);
                        }
                        if("/boom".equals(path)) {
                            throw new IllegalStateException("boom");
                        }
                        if(!"/work".equals(path)) {
                            return null;
                        }
                        Tracing.route("/work");
                        Map row = db.queryOne("SELECT name FROM pets WHERE id = ?",
                                new Object[] {Long.valueOf(7)});
                        // A GET with no headers of its own FOLLOWS a redirect, and
                        // the trace context this call carries must not change that.
                        Web.Result hop = Web.get(downstream + "/hop");
                        return HttpServer.Response.text(200, row.get("name") + " "
                                + hop.getStatus() + " " + hop.getBodyAsString());
                    }
                })
                .run();
    }
}
