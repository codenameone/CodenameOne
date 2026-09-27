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
package com.codename1.backend.metrics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// One named measurement: a counter, a gauge or a histogram.
///
/// Created once through [Metrics] and kept -- the build's generated code
/// holds each in a static field -- so recording a value never looks anything up.
public abstract class Instrument {
    public static final int COUNTER = 0;
    public static final int UP_DOWN_COUNTER = 1;
    public static final int GAUGE = 2;
    public static final int HISTOGRAM = 3;

    private final String name;
    private final String description;
    private final String unit;
    private final int kind;

    Instrument(String name, String description, String unit, int kind) {
        this.name = name;
        this.description = description == null ? "" : description;
        this.unit = unit == null ? "" : unit;
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUnit() {
        return unit;
    }

    /// [#COUNTER], [#UP_DOWN_COUNTER], [#GAUGE] or [#HISTOGRAM].
    public int getKind() {
        return kind;
    }

    /// The current points, each a map with `attributes` (a map, possibly
    /// empty) and either `value` or, for a histogram, `count`,
    /// `sum`, `min`, `max`, `bounds` and `buckets`.
    public abstract List points();

    /// A point with an integral value, kept as a Long: a counter past 2^53 is not
    /// exactly representable as a double, and the exporter sends a Long as
    /// OTLP's integer field rather than rounding it.
    static Map point(Map attributes, long value) {
        Map p = new LinkedHashMap();
        p.put("attributes", attributes == null ? new LinkedHashMap() : attributes);
        p.put("value", Long.valueOf(value));
        return p;
    }

    static Map point(Map attributes, double value) {
        Map p = new LinkedHashMap();
        p.put("attributes", attributes == null ? new LinkedHashMap() : attributes);
        p.put("value", Double.valueOf(value));
        return p;
    }

    static List single(Map point) {
        List out = new ArrayList(1);
        out.add(point);
        return out;
    }
}
