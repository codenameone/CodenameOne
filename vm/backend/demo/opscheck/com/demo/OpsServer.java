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

import com.codename1.backend.Backend;
import com.codename1.backend.HttpServer;
import com.codename1.backend.otel.OtlpMetricExporter;
import com.codename1.impl.backend.BackendAccess;

/// A server with management and the OTLP metric exporter on, for
/// BackendOperationsTest to hold the PACKAGED runtime to the operational
/// contract: the probes, the endpoint authentication, the metric schema, the
/// Prometheus scrape the sample dashboard reads, and an exporter that flushes
/// when the process is told to stop.
///
/// Configured from the environment, as a deployment is: PORT,
/// CN1_DATASOURCE_URL, CN1_MANAGEMENT_ENABLED, CN1_MANAGEMENT_TOKEN and the
/// OTEL_* variables.
public class OpsServer {
    public static void main(String[] args) throws Exception {
        Backend.Builder builder = Backend.builder()
                .metrics(new OtlpMetricExporter("opscheck"))
                .requiresDataSource();
        // As the generated entry point links it: not a Builder method an
        // application calls.
        BackendAccess.get().management(builder);
        builder.handler(new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) {
                String path = request.getTarget();
                int query = path.indexOf('?');
                if (query >= 0) {
                    path = path.substring(0, query);
                }
                if ("/hello".equals(path)) {
                    return HttpServer.Response.text(200, "hi");
                }
                if ("/fail".equals(path)) {
                    return HttpServer.Response.text(503, "no");
                }
                return null;
            }
        }).run();
    }
}
