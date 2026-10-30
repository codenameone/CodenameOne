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
package com.codename1.backend;

import com.codename1.backend.metrics.Instrument;
import com.codename1.backend.metrics.MetricSchema;
import com.codename1.backend.metrics.Metrics;
import com.codename1.impl.backend.Management;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The published metric schema held to what a running server serves: every
/// instrument it lists is recorded with the kind, unit and Prometheus name it
/// declares, a figure the server cannot measure is absent rather than zero, and
/// the sample dashboard reads nothing a runtime may lack.
class MetricSchemaTest {
    private static final HttpServer.Handler NONE = new HttpServer.Handler() {
        public HttpServer.Response handle(HttpServer.Request request) {
            return "/hello".equals(request.getTarget()) ? HttpServer.Response.text(200, "hi") : null;
        }
    };

    @Test
    @DisplayName("the schema names each instrument once, with a unit and a Prometheus name of its own")
    void schemaIsWellFormed() {
        Set<String> names = new HashSet<String>();
        Set<String> prometheus = new HashSet<String>();
        for (Object element : MetricSchema.entries()) {
            MetricSchema.Entry e = (MetricSchema.Entry) element;
            assertTrue(names.add(e.getName()), "listed twice: " + e.getName());
            assertTrue(prometheus.add(e.getPrometheusName()), "two entries scrape as "
                    + e.getPrometheusName());
            assertFalse(e.getUnit().isEmpty(), e.getName() + " has no unit");
            assertTrue(MetricSchema.CUMULATIVE.equals(e.getTemporality())
                    || MetricSchema.INSTANT.equals(e.getTemporality()), e.getName());
            assertTrue(MetricSchema.ALWAYS.equals(e.getAvailability())
                    || MetricSchema.DATABASE.equals(e.getAvailability())
                    || MetricSchema.JVM.equals(e.getAvailability()), e.getName());
            assertTrue(e == MetricSchema.get(e.getName()));
        }
        for (Object element : MetricSchema.unmeasured()) {
            String[] u = (String[]) element;
            assertFalse(u[1].isEmpty(), u[0] + " is listed without a reason");
        }
    }

    @Test
    @DisplayName("a server with a database records every instrument of the schema as declared")
    void everyInstrumentIsServedAsDeclared() throws Exception {
        File db = File.createTempFile("schema", ".db");
        db.deleteOnExit();
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(Config.DATASOURCE_URL, db.getAbsolutePath());
        Backend backend = Backend.builder(Config.of(settings, "dev")).quiet().management()
                .requiresDataSource().handler(NONE).start();
        try {
            assertEquals(200, get(port, "/hello", null).status);
            Map schema = Json.parseObject(get(port, "/manage/schema", null).body);
            assertEquals("jvm", schema.get("runtime"));
            String scrape = get(port, "/manage/prometheus", null).body;
            for (Object element : (List) schema.get("metrics")) {
                Map m = (Map) element;
                String name = (String) m.get("name");
                MetricSchema.Entry entry = MetricSchema.get(name);
                assertNotNull(entry, name);
                assertEquals(Boolean.TRUE, m.get("available"), name + ": " + m.get("reason"));
                Instrument recorded = Metrics.get(name);
                assertEquals(entry.getKind(), recorded.getKind(), name + " kind");
                assertEquals(entry.getUnit(), recorded.getUnit(), name + " unit");
                String type = entry.getKind() == Instrument.COUNTER ? "counter"
                        : entry.getKind() == Instrument.HISTOGRAM ? "histogram" : "gauge";
                assertTrue(scrape.contains("# TYPE " + entry.getPrometheusName() + " " + type + "\n"),
                        name + " is not scraped as " + entry.getPrometheusName() + " " + type);
            }
            // The figures themselves, not only their presence.
            assertTrue(value("db.client.connection.max") >= 1);
            assertEquals(0.0, value("db.client.connection.pending_requests"), 0.0);
            assertTrue(value("process.runtime.memory.committed")
                    >= value("process.runtime.memory.used"));
            assertTrue(value("jvm.thread.count") > 0);
            assertTrue(scrape.contains("http_server_request_duration_count{"), scrape);
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a server without a database reports no pool figures, and says why")
    void absentFiguresAreNotZero() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "dev")).quiet().management()
                .handler(NONE).start();
        try {
            Map schema = Json.parseObject(get(port, "/manage/schema", null).body);
            String scrape = get(port, "/manage/prometheus", null).body;
            Map view = Json.parseObject(get(port, "/manage/metrics", null).body);
            int pool = 0;
            for (Object element : (List) schema.get("metrics")) {
                Map m = (Map) element;
                if (!MetricSchema.DATABASE.equals(m.get("availability"))) {
                    continue;
                }
                pool++;
                assertEquals(Boolean.FALSE, m.get("available"), (String) m.get("name"));
                assertEquals("This server has no database pool.", m.get("reason"));
                String prom = (String) m.get("prometheus");
                assertFalse(scrape.contains(" " + prom + " "),
                        prom + " is named in the scrape of a server with no pool");
                assertFalse(view.containsKey(m.get("name")),
                        m.get("name") + " is listed by the JSON view of a server with no pool");
            }
            assertEquals(5, pool);
            assertFalse(((List) schema.get("unmeasured")).isEmpty());
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("the pool counts the borrowers it keeps waiting and the borrows that time out")
    void poolSaturationIsMeasured() throws Exception {
        File file = File.createTempFile("pool", ".db");
        file.deleteOnExit();
        final DataSource pool = DataSource.open(file.getAbsolutePath(), 1, 1000, 400);
        try {
            Database held = pool.borrow();
            final AtomicReference<Throwable> outcome = new AtomicReference<Throwable>();
            Thread waiter = new Thread(new Runnable() {
                public void run() {
                    try {
                        pool.release(pool.borrow());
                    } catch (Throwable err) {
                        outcome.set(err);
                    }
                }
            });
            waiter.start();
            long deadline = System.currentTimeMillis() + 5000;
            while (pool.getPendingCount() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(5);
            }
            assertEquals(1, pool.getPendingCount());
            waiter.join(5000);
            assertTrue(outcome.get() instanceof IOException, String.valueOf(outcome.get()));
            assertEquals(0, pool.getPendingCount());
            assertEquals(1L, pool.getTimeoutCount());
            pool.release(held);
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("readiness follows health, liveness stays up, and only health is public")
    void probesAndTheirAuthentication() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(Management.ENABLED, "true");
        settings.setProperty(Management.TOKEN, "s3cret");
        Backend backend = Backend.builder(Config.of(settings, "prod")).quiet().management()
                .handler(NONE).start();
        try {
            Reply ready = get(port, "/manage/health/readiness", null);
            assertEquals(200, ready.status);
            assertEquals("UP", Json.parseObject(ready.body).get("status"));
            Reply live = get(port, "/manage/health/liveness", null);
            assertEquals(200, live.status);
            assertEquals("UP", Json.parseObject(live.body).get("status"));
            assertEquals(200, get(port, "/manage/health", null).status);
            for (String path : new String[] {"/manage/schema", "/manage/metrics", "/manage/prometheus"}) {
                assertEquals(401, get(port, path, null).status, path + " without the token");
                assertEquals(401, get(port, path, "wrong").status, path + " with a wrong token");
                assertEquals(200, get(port, path, "s3cret").status, path + " with the token");
            }
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("the sample dashboard reads only instruments every runtime reports")
    void theDashboardIsPortable() throws Exception {
        File dashboard = new File("../../vm/backend/dashboards/backend-overview.json");
        assertTrue(dashboard.isFile(), dashboard.getAbsolutePath());
        String json = new String(Files.readAllBytes(dashboard.toPath()), StandardCharsets.UTF_8);
        Map parsed = Json.parseObject(json);
        Set<String> portable = new HashSet<String>();
        for (Object element : MetricSchema.entries()) {
            MetricSchema.Entry e = (MetricSchema.Entry) element;
            if (!MetricSchema.JVM.equals(e.getAvailability())) {
                portable.add(e.getPrometheusName());
            }
        }
        // A metric selector is a name followed by its label matchers.
        Pattern selector = Pattern.compile("([a-zA-Z_:][a-zA-Z0-9_:]*)\\{");
        Set<String> used = new HashSet<String>();
        for (Object panel : (List) parsed.get("panels")) {
            Object targets = ((Map) panel).get("targets");
            if (!(targets instanceof List)) {
                continue;
            }
            for (Object target : (List) targets) {
                Matcher m = selector.matcher((String) ((Map) target).get("expr"));
                while (m.find()) {
                    String name = m.group(1).replaceAll("_(bucket|sum|count)$", "");
                    if (!portable.contains(name)) {
                        name = m.group(1);
                    }
                    assertTrue(portable.contains(name), "the dashboard reads " + m.group(1)
                            + ", which is not a metric every runtime reports");
                    used.add(name);
                }
            }
        }
        assertTrue(used.size() >= 15, "the dashboard reads only " + used);
    }

    private static double value(String name) {
        List points = Metrics.get(name).points();
        assertFalse(points.isEmpty(), name + " has no point");
        return ((Number) ((Map) points.get(0)).get("value")).doubleValue();
    }

    private static final class Reply {
        int status;
        String body;
    }

    private static Reply get(int port, String path, String token) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path)
                .openConnection();
        if (token != null) {
            c.setRequestProperty("Authorization", "Bearer " + token);
        }
        Reply r = new Reply();
        r.status = c.getResponseCode();
        InputStream in = r.status >= 400 ? c.getErrorStream() : c.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (in != null) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            in.close();
        }
        r.body = new String(out.toByteArray(), StandardCharsets.UTF_8);
        return r;
    }

    private static int freePort() throws IOException {
        ServerSocket s = new ServerSocket(0);
        try {
            return s.getLocalPort();
        } finally {
            s.close();
        }
    }
}
