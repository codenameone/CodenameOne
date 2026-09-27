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
}
