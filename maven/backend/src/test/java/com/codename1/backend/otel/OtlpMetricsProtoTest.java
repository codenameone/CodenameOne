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
}
