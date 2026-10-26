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
package com.codename1.tools.translator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The operational contract on the PACKAGED runtime: what a load balancer, a
 * Prometheus scrape, an OTLP collector and the sample dashboard see of a
 * translated server. The backend module's MetricSchemaTest holds the JVM arm to
 * the same contract; this is the native one, built locally through build.sh --
 * no GraalVM, no native-image.
 */
class BackendOperationsTest {
    private static final String TOKEN = "s3cret";
    /** The schema entries a native server cannot measure. */
    private static final String[] JVM_ONLY = {"jvm_thread_count", "jvm_class_count", "jvm_cpu_count",
        "cn1_jvm_gc_collections_total", "cn1_jvm_gc_time_total", "process_runtime_memory_limit"};

    @Test
    @DisplayName("a translated server answers probes, guards its endpoints, serves the dashboard's metrics and flushes its exporter on stop")
    void operationsOnThePackagedRuntime() throws Exception {
        if (CompilerHelper.isWindows()) {
            BackendTestSupport.skipOrFail("the server-side backend is POSIX-only for now");
        }
        Path jdk8 = BackendTestSupport.findJdk8();
        BackendTestSupport.require(jdk8 != null, "no JDK 8 available to compile the backend");
        Path work = Files.createTempDirectory("backend-ops");
        Path binary = work.resolve("opsserver");
        String failure = BackendTestSupport.build("OpsServer", "demo/opscheck", binary, jdk8, true);
        if (failure != null) {
            BackendTestSupport.skipOrFail(failure);
        }

        final AtomicInteger exports = new AtomicInteger();
        HttpServer collector = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        collector.createContext("/v1/metrics", (HttpExchange exchange) -> {
            readAll(exchange.getRequestBody());
            exports.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        collector.start();
        int port = BackendTestSupport.freePort();
        Map<String, String> env = new HashMap<String, String>();
        env.put("PORT", String.valueOf(port));
        env.put("CN1_DATASOURCE_URL", work.resolve("ops.db").toString());
        env.put("CN1_MANAGEMENT_ENABLED", "true");
        env.put("CN1_MANAGEMENT_TOKEN", TOKEN);
        env.put("OTEL_EXPORTER_OTLP_ENDPOINT", "http://127.0.0.1:" + collector.getAddress().getPort());
        // Long enough that the shutdown flush is the export the test waits for.
        env.put("OTEL_METRIC_EXPORT_INTERVAL", "600000");
        Path log = work.resolve("server.log");
        Process server = BackendTestSupport.start(binary, env, log);
        try {
            assertTrue(BackendTestSupport.waitForPort(port, 60000),
                    "the server never listened:\n" + read(log));
            String base = "http://127.0.0.1:" + port;
            for (int i = 0; i < 5; i++) {
                assertEquals(200, get(base + "/hello", null).status);
            }
            assertEquals(503, get(base + "/fail", null).status);

            // The probes need no token; everything else needs the right one.
            assertEquals(200, get(base + "/manage/health/liveness", null).status);
            assertEquals(200, get(base + "/manage/health/readiness", null).status);
            for (String path : new String[] {"/manage/schema", "/manage/metrics", "/manage/prometheus"}) {
                assertEquals(401, get(base + path, null).status, path + " without a token");
                assertEquals(401, get(base + path, "wrong").status, path + " with a wrong token");
                assertEquals(200, get(base + path, TOKEN).status, path + " with the token");
            }

            String schema = get(base + "/manage/schema", TOKEN).body;
            assertTrue(schema.contains("\"runtime\":\"native\""), schema);
            assertTrue(schema.contains("\"name\":\"jvm.thread.count\"") && schema.contains("\"available\":false"),
                    "the JVM-only figures are not marked unavailable: " + schema);

            String scrape = get(base + "/manage/prometheus", TOKEN).body;
            for (String name : JVM_ONLY) {
                assertFalse(scrape.contains("# TYPE " + name + " "),
                        name + " is scraped on a runtime that cannot measure it");
            }
            // Every series the sample dashboard queries is served here, typed.
            Set<String> missing = new TreeSet<String>();
            for (String name : dashboardMetrics()) {
                String series = name.replaceAll("_(bucket|sum|count)$", "");
            if (!scrape.contains("# TYPE " + name + " ")
                    && !scrape.contains("# TYPE " + series + " histogram")) {
                    missing.add(name);
                }
            }
            assertTrue(missing.isEmpty(), "the dashboard reads " + missing
                    + ", which the packaged server does not serve:\n" + scrape);
            assertTrue(scrape.contains("http_server_request_duration_count{"), scrape);
            assertTrue(scrape.contains("db_client_connection_max "), scrape);

            // SIGTERM: the server drains and the exporter sends what it holds.
            assertEquals(0, exports.get(), "exported before the interval");
            server.destroy();
            assertTrue(server.waitFor(30, TimeUnit.SECONDS), "the server did not stop:\n" + read(log));
            assertTrue(exports.get() >= 1, "the exporter did not flush on stop:\n" + read(log));
        } finally {
            BackendTestSupport.stop(server);
            collector.stop(0);
        }
    }

    /** The metric names the sample dashboard's queries name. */
    private static Set<String> dashboardMetrics() throws IOException {
        String json = new String(Files.readAllBytes(BackendTestSupport.backendDir()
                .resolve("dashboards/backend-overview.json")), StandardCharsets.UTF_8);
        Set<String> names = new TreeSet<String>();
        Matcher m = Pattern.compile("([a-zA-Z_:][a-zA-Z0-9_:]*)\\{").matcher(json);
        while (m.find()) {
            String name = m.group(1);
            if (name.contains("_")) {
                names.add(name);
            }
        }
        assertTrue(names.size() >= 10, "read too few names from the dashboard: " + names);
        return names;
    }

    private static final class Answer {
        int status;
        String body;
    }

    private static Answer get(String url, String token) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        if (token != null) {
            c.setRequestProperty("Authorization", "Bearer " + token);
        }
        Answer a = new Answer();
        a.status = c.getResponseCode();
        InputStream in = a.status < 400 ? c.getInputStream() : c.getErrorStream();
        a.body = in == null ? "" : new String(readAll(in), StandardCharsets.UTF_8);
        c.disconnect();
        return a;
    }

    private static String read(Path log) {
        try {
            return new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
        } catch (IOException err) {
            return "(no log: " + err + ")";
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        for (int n; (n = in.read(buf)) > 0; ) {
            out.write(buf, 0, n);
        }
        in.close();
        return out.toByteArray();
    }
}
