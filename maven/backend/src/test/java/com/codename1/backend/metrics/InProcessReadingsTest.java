/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InProcessReadingsTest {
    @Test
    void aHistogramReadsItsCountSumAndMean() {
        Histogram h = Metrics.histogram("test.inprocess.latency", "", "ms", null,
                new String[] {"route"});
        h.record(10);
        h.record(20, "/a", null, null);
        h.record(30, "/b", null, null);
        assertEquals(3, h.getCount());
        assertEquals(60.0, h.getSum(), 0);
        assertEquals(20.0, h.getMean(), 0);
    }

    @Test
    void aTaggedGaugeExportsItsTags() {
        Map<String, Object> tags = new LinkedHashMap<String, Object>();
        tags.put("pool", "main");
        tags.put("region", "eu");
        Gauge g = Metrics.gauge("test.inprocess.size", "", "1", tags, new Gauge.Source() {
            @Override
            public double read() {
                return 4;
            }
        });
        List points = g.points();
        assertEquals(1, points.size());
        Map point = (Map) points.get(0);
        assertEquals(tags, point.get("attributes"));
        String exposition = Metrics.prometheus();
        assertTrue(exposition.contains("pool=\"main\",region=\"eu\""), exposition);
    }
}
