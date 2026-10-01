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
package com.codename1.backend.otel;

import com.codename1.backend.metrics.Histogram;
import com.codename1.backend.metrics.Metrics;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.Metric;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The metrics export decoded by the classes opentelemetry-proto generates from
 * the specification's own .proto files, so the wire types are judged by the
 * schema rather than by a decoder written beside the encoder.
 */
class OtlpMetricsProtoTest {

    @Test
    @DisplayName("histogram bucket counts decode to one count per bucket, matching the bounds")
    void histogramBuckets() throws Exception {
        Histogram h = Metrics.histogram("test.proto.histogram", "Proto check", "ms",
                new double[] {1, 10, 100}, null);
        h.record(0.5);
        h.record(5);
        h.record(5);
        h.record(50);
        h.record(500);
        h.record(700);
        List instruments = Collections.singletonList(h);
        Map request = OtlpMetricExporter.request(new LinkedHashMap(), instruments,
                System.currentTimeMillis());
        ExportMetricsServiceRequest decoded = ExportMetricsServiceRequest.parseFrom(
                OtlpSchema.metricsProtobuf(request));
        Metric metric = decoded.getResourceMetrics(0).getScopeMetrics(0).getMetrics(0);
        assertEquals("test.proto.histogram", metric.getName());
        HistogramDataPoint point = metric.getHistogram().getDataPoints(0);
        assertEquals(Arrays.asList(1.0, 10.0, 100.0), point.getExplicitBoundsList());
        // A varint/fixed64 mismatch decodes as roughly eight counts per bucket.
        assertEquals(Arrays.asList(1L, 2L, 1L, 2L), point.getBucketCountsList());
        assertEquals(6L, point.getCount());
        assertEquals(1260.5, point.getSum(), 1e-9);
        assertNotNull(point.getAttributesList());
    }

    @Test
    @DisplayName("an infinite reading is exported as OTLP/JSON's string form, and as a double")
    void infiniteValuesSurviveBothEncodings() throws Exception {
        Histogram h = Metrics.histogram("test.proto.infinite", "", "ms",
                new double[] {1, 10}, null);
        h.record(Double.POSITIVE_INFINITY);
        Map request = OtlpMetricExporter.request(new LinkedHashMap(),
                Collections.singletonList(h), System.currentTimeMillis());
        String json = new String(OtlpSchema.json(request), "UTF-8");
        assertTrue(json.contains("\"sum\":\"Infinity\""), json);
        assertFalse(json.contains("null"), "a non-finite value was written as null: " + json);
        ExportMetricsServiceRequest decoded = ExportMetricsServiceRequest.parseFrom(
                OtlpSchema.metricsProtobuf(request));
        assertEquals(Double.POSITIVE_INFINITY, decoded.getResourceMetrics(0).getScopeMetrics(0)
                .getMetrics(0).getHistogram().getDataPoints(0).getSum(), 0.0);
    }

    @Test
    @DisplayName("a gauge whose callback throws an Error costs its value, not the export")
    void aGaugeErrorIsContained() throws Exception {
        com.codename1.backend.metrics.Gauge broken = Metrics.gauge("test.proto.broken", "", "",
                new com.codename1.backend.metrics.Gauge.Source() {
                    public double read() {
                        throw new AssertionError("application bug");
                    }
                });
        assertTrue(Double.isNaN(broken.read()));
        Map request = OtlpMetricExporter.request(new LinkedHashMap(),
                Collections.singletonList(broken), System.currentTimeMillis());
        OtlpSchema.metricsProtobuf(request);
        assertTrue(Metrics.prometheus().length() > 0);
    }

    @Test
    @DisplayName("a counter past 2^53 is exported exactly, through as_int")
    void countersAreIntegral() throws Exception {
        com.codename1.backend.metrics.Counter big = Metrics.counter("test.proto.big", "", "");
        big.add(9007199254740993L);
        Map request = OtlpMetricExporter.request(new LinkedHashMap(),
                Collections.singletonList(big), System.currentTimeMillis());
        ExportMetricsServiceRequest decoded = ExportMetricsServiceRequest.parseFrom(
                OtlpSchema.metricsProtobuf(request));
        io.opentelemetry.proto.metrics.v1.NumberDataPoint point = decoded.getResourceMetrics(0)
                .getScopeMetrics(0).getMetrics(0).getSum().getDataPoints(0);
        assertEquals(9007199254740993L, point.getAsInt(),
                "a counter was rounded through a double");
    }

    @Test
    @DisplayName("an up-down counter below zero encodes as a signed as_int")
    void negativeUpDownCounter() throws Exception {
        com.codename1.backend.metrics.Counter c = Metrics.upDownCounter("test.proto.negative",
                "", "");
        c.add(-5);
        Map request = OtlpMetricExporter.request(new LinkedHashMap(),
                Collections.singletonList(c), System.currentTimeMillis());
        ExportMetricsServiceRequest decoded = ExportMetricsServiceRequest.parseFrom(
                OtlpSchema.metricsProtobuf(request));
        assertEquals(-5L, decoded.getResourceMetrics(0).getScopeMetrics(0).getMetrics(0)
                .getSum().getDataPoints(0).getAsInt());
    }

    @Test
    @DisplayName("data points a collector rejects in a partial success are reported, not healthy")
    void partialSuccessIsReported() throws Exception {
        final byte[][] answer = {new byte[0]};
        final String[] type = {"application/x-protobuf"};
        com.sun.net.httpserver.HttpServer collector = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        collector.createContext("/v1/metrics", exchange -> {
            java.io.InputStream in = exchange.getRequestBody();
            while(in.read() >= 0) {
                // drained
            }
            exchange.getResponseHeaders().add("Content-Type", type[0]);
            exchange.sendResponseHeaders(200, answer[0].length);
            exchange.getResponseBody().write(answer[0]);
            exchange.close();
        });
        collector.start();
        try {
            java.util.Properties p = new java.util.Properties();
            p.setProperty(OtlpMetricExporter.ENDPOINT, "http://127.0.0.1:"
                    + collector.getAddress().getPort() + "/v1/metrics");
            p.setProperty(OtlpMetricExporter.INTERVAL, "3600000");
            OtlpMetricExporter exporter = new OtlpMetricExporter("partial");
            exporter.open(com.codename1.backend.Config.of(p, "test"));
            try {
                answer[0] = io.opentelemetry.proto.collector.metrics.v1
                        .ExportMetricsServiceResponse.newBuilder()
                        .setPartialSuccess(io.opentelemetry.proto.collector.metrics.v1
                                .ExportMetricsPartialSuccess.newBuilder()
                                .setRejectedDataPoints(3).setErrorMessage("too old").build())
                        .build().toByteArray();
                type[0] = "application/x-protobuf";
                assertFalse(exporter.export(), "a partial success counted as healthy");
                assertEquals(Long.valueOf(3), exporter.status().get("dataPointsRejected"));
                assertTrue(String.valueOf(exporter.status().get("lastError"))
                        .contains("too old"), String.valueOf(exporter.status()));
                answer[0] = "{\"partialSuccess\":{\"rejectedDataPoints\":\"2\"}}"
                        .getBytes("UTF-8");
                type[0] = "application/json";
                assertFalse(exporter.export());
                assertEquals(Long.valueOf(5), exporter.status().get("dataPointsRejected"));
                answer[0] = new byte[0];
                assertTrue(exporter.export(), "an empty 200 is a full success");
            } finally {
                exporter.shutdown(0);
            }
        } finally {
            collector.stop(0);
        }
    }

    @Test
    @DisplayName("a second metrics exporter with another identity is refused")
    void oneMetricsIdentityPerProcess() throws Exception {
        java.util.Properties a = new java.util.Properties();
        a.setProperty(OtlpMetricExporter.ENDPOINT, "http://127.0.0.1:9/v1/metrics");
        a.setProperty(OtlpTracer.SERVICE_NAME, "service-a");
        java.util.Properties b = new java.util.Properties(a);
        b.setProperty(OtlpTracer.SERVICE_NAME, "service-b");
        OtlpMetricExporter first = new OtlpMetricExporter("x");
        OtlpMetricExporter second = new OtlpMetricExporter("x");
        OtlpMetricExporter same = new OtlpMetricExporter("x");
        first.open(com.codename1.backend.Config.of(a, "test"));
        try {
            org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
                    () -> second.open(com.codename1.backend.Config.of(b, "test")));
            same.open(com.codename1.backend.Config.of(a, "test"));
            same.shutdown(0);
        } finally {
            first.shutdown(0);
        }
    }

    @Test
    @DisplayName("a second metrics exporter with other credentials or protocol is refused")
    void oneMetricsTransportPerProcess() throws Exception {
        java.util.Properties a = new java.util.Properties();
        a.setProperty(OtlpMetricExporter.ENDPOINT, "http://127.0.0.1:9/v1/metrics");
        a.setProperty(OtlpTracer.SERVICE_NAME, "shared");
        a.setProperty(OtlpMetricExporter.HEADERS, "x-api-key=tenant-a,x-team=one");
        java.util.Properties otherKey = new java.util.Properties(a);
        otherKey.setProperty(OtlpMetricExporter.HEADERS, "x-api-key=tenant-b,x-team=one");
        java.util.Properties json = new java.util.Properties(a);
        json.setProperty(OtlpMetricExporter.PROTOCOL, "http/json");
        java.util.Properties reordered = new java.util.Properties(a);
        reordered.setProperty(OtlpMetricExporter.HEADERS, "x-team=one,x-api-key=tenant-a");
        OtlpMetricExporter first = new OtlpMetricExporter("x");
        // Shut down even when the refusal fails, so a wrongly opened exporter
        // cannot collide with the next test's.
        final OtlpMetricExporter keyed = new OtlpMetricExporter("x");
        final OtlpMetricExporter asJson = new OtlpMetricExporter("x");
        first.open(com.codename1.backend.Config.of(a, "test"));
        try {
            java.io.IOException refused = org.junit.jupiter.api.Assertions.assertThrows(
                    java.io.IOException.class,
                    () -> keyed.open(com.codename1.backend.Config.of(otherKey, "test")));
            assertTrue(refused.getMessage().contains("different"), refused.getMessage());
            assertFalse(refused.getMessage().contains("tenant"),
                    "the refusal printed a header value: " + refused.getMessage());
            org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
                    () -> asJson.open(com.codename1.backend.Config.of(json, "test")));
            OtlpMetricExporter same = new OtlpMetricExporter("x");
            same.open(com.codename1.backend.Config.of(reordered, "test"));
            same.shutdown(0);
        } finally {
            keyed.shutdown(0);
            asJson.shutdown(0);
            first.shutdown(0);
        }
    }

    @Test
    @DisplayName("an exporter opened again after a shutdown exports periodically again")
    void reopenedExporterKeepsExporting() throws Exception {
        java.net.ServerSocket probe = new java.net.ServerSocket(0);
        int closed = probe.getLocalPort();
        probe.close();
        java.util.Properties p = new java.util.Properties();
        // Nothing listens there, so every export counts as a failure -- which is
        // what shows the thread is running.
        p.setProperty(OtlpMetricExporter.ENDPOINT, "http://127.0.0.1:" + closed + "/v1/metrics");
        p.setProperty(OtlpMetricExporter.INTERVAL, "20");
        com.codename1.backend.Config config = com.codename1.backend.Config.of(p, "test");
        OtlpMetricExporter exporter = new OtlpMetricExporter("reopen");
        exporter.open(config);
        exporter.shutdown(0);
        exporter.open(config);
        try {
            long deadline = System.currentTimeMillis() + 5000;
            long failures = 0;
            while(System.currentTimeMillis() < deadline && failures < 3) {
                Thread.sleep(20);
                failures = ((Number)exporter.status().get("failures")).longValue();
            }
            assertTrue(failures >= 3, "the reopened exporter stopped after " + failures
                    + " export(s)");
        } finally {
            exporter.shutdown(0);
        }
    }

    @Test
    @DisplayName("two exporters of one identity: one exports, the other takes over when it stops")
    void oneExporterPerIdentityExports() throws Exception {
        java.net.ServerSocket probe = new java.net.ServerSocket(0);
        int closed = probe.getLocalPort();
        probe.close();
        java.util.Properties p = new java.util.Properties();
        p.setProperty(OtlpMetricExporter.ENDPOINT, "http://127.0.0.1:" + closed + "/v1/metrics");
        p.setProperty(OtlpMetricExporter.INTERVAL, "20");
        com.codename1.backend.Config config = com.codename1.backend.Config.of(p, "test");
        OtlpMetricExporter first = new OtlpMetricExporter("dup");
        OtlpMetricExporter second = new OtlpMetricExporter("dup");
        first.open(config);
        second.open(config);
        try {
            org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
                    () -> first.open(config), "the same exporter opened twice");
            long deadline = System.currentTimeMillis() + 5000;
            while(System.currentTimeMillis() < deadline && failures(first) < 3) {
                Thread.sleep(20);
            }
            assertTrue(failures(first) >= 3);
            assertEquals(0L, failures(second), "both exporters sent the process's metrics");
            first.shutdown(0);
            deadline = System.currentTimeMillis() + 5000;
            while(System.currentTimeMillis() < deadline && failures(second) < 3) {
                Thread.sleep(20);
            }
            assertTrue(failures(second) >= 3, "nobody exported after the first stopped");
        } finally {
            first.shutdown(0);
            second.shutdown(0);
        }
    }

    private static long failures(OtlpMetricExporter e) {
        return ((Number) e.status().get("failures")).longValue();
    }

    @Test
    @DisplayName("an exporter that shut down is not started by a late hand-over")
    void aStoppedSuccessorIsNotStarted() throws Exception {
        java.net.ServerSocket probe = new java.net.ServerSocket(0);
        int closed = probe.getLocalPort();
        probe.close();
        java.util.Properties p = new java.util.Properties();
        p.setProperty(OtlpMetricExporter.ENDPOINT, "http://127.0.0.1:" + closed + "/v1/metrics");
        p.setProperty(OtlpMetricExporter.INTERVAL, "20");
        com.codename1.backend.Config config = com.codename1.backend.Config.of(p, "test");
        OtlpMetricExporter leader = new OtlpMetricExporter("race");
        OtlpMetricExporter successor = new OtlpMetricExporter("race");
        leader.open(config);
        successor.open(config);
        successor.shutdown(0);       // stops first, while the leader is deciding
        successor.takeOver();        // the leader's hand-over, arriving late
        leader.shutdown(0);
        Thread.sleep(200);
        assertEquals(0L, failures(successor), "a shut-down exporter was started and exports");
    }

    @Test
    @DisplayName("a successor starts exporting only once the old exporter's thread has exited")
    void aSuccessorWaitsForTheOldExport() throws Exception {
        final java.net.ServerSocket slow = new java.net.ServerSocket(0);
        final java.util.List held = java.util.Collections.synchronizedList(
                new java.util.ArrayList());
        Thread acceptor = new Thread(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        held.add(slow.accept());           // accepted, never answered
                    }
                } catch (java.io.IOException closed) {
                    // released
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
        java.util.Properties p = new java.util.Properties();
        p.setProperty(OtlpMetricExporter.ENDPOINT, "http://127.0.0.1:" + slow.getLocalPort()
                + "/v1/metrics");
        p.setProperty(OtlpMetricExporter.INTERVAL, "20");
        com.codename1.backend.Config config = com.codename1.backend.Config.of(p, "test");
        OtlpMetricExporter first = new OtlpMetricExporter("handoff");
        OtlpMetricExporter second = new OtlpMetricExporter("handoff");
        first.open(config);
        second.open(config);
        try {
            long deadline = System.currentTimeMillis() + 5000;
            while(held.isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);                // the first export is now stuck in flight
            }
            assertEquals(1, held.size());
            first.shutdown(0);                   // hands over without waiting
            Thread.sleep(300);
            assertEquals(1, held.size(),
                    "the successor exported while the old export was still in flight");
        } finally {
            slow.close();
            synchronized (held) {
                for(Object s : held) {
                    ((java.net.Socket) s).close();
                }
            }
            second.shutdown(0);
        }
    }

    @Test
    @DisplayName("a reopened exporter waits for its old thread's export before starting")
    void aReopenWaitsForTheOldExport() throws Exception {
        final java.net.ServerSocket slow = new java.net.ServerSocket(0);
        final java.util.List held = java.util.Collections.synchronizedList(
                new java.util.ArrayList());
        Thread acceptor = new Thread(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        held.add(slow.accept());           // accepted, never answered
                    }
                } catch (java.io.IOException closed) {
                    // released
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
        java.util.Properties p = new java.util.Properties();
        p.setProperty(OtlpMetricExporter.ENDPOINT, "http://127.0.0.1:" + slow.getLocalPort()
                + "/v1/metrics");
        p.setProperty(OtlpMetricExporter.INTERVAL, "20");
        final com.codename1.backend.Config config = com.codename1.backend.Config.of(p, "test");
        final OtlpMetricExporter exporter = new OtlpMetricExporter("reopen-wait");
        exporter.open(config);
        final Throwable[] reopenFailed = new Throwable[1];
        Thread reopen = new Thread(new Runnable() {
            public void run() {
                try {
                    exporter.open(config);
                } catch (Throwable err) {
                    reopenFailed[0] = err;
                }
            }
        });
        try {
            long deadline = System.currentTimeMillis() + 5000;
            while(held.isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);                // the export is now stuck in flight
            }
            assertEquals(1, held.size());
            exporter.shutdown(0);                // returns while it is still stuck
            reopen.start();
            Thread.sleep(300);
            assertEquals(1, held.size(),
                    "the reopened exporter exported beside its old thread's export");
            assertTrue(reopen.isAlive(), "the reopen did not wait for the old thread");
        } finally {
            synchronized (held) {
                for(Object s : held) {
                    ((java.net.Socket) s).close();  // the old export fails and its thread ends
                }
            }
            reopen.join(10000);
            slow.close();
            exporter.shutdown(0);
        }
        assertTrue(reopenFailed[0] == null, String.valueOf(reopenFailed[0]));
    }
}
