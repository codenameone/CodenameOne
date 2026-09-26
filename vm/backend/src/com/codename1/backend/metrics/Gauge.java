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

import java.util.List;

/**
 * A value read when metrics are collected -- a queue's depth, a pool's size, a
 * {@code @ManagedAttribute}. Nothing is recorded in between, so a gauge costs
 * nothing until something asks.
 */
public final class Gauge extends Instrument {
    /** Where a gauge's value comes from. */
    public interface Source {
        double read();
    }

    /**
     * Where a gauge with several labelled values comes from -- one per executor,
     * say. Each point is a map with {@code attributes} and {@code value}; see
     * {@link #point}.
     */
    public interface MultiSource {
        List read();
    }

    private final Source source;
    private final MultiSource multi;

    Gauge(String name, String description, String unit, Source source) {
        super(name, description, unit, GAUGE);
        this.source = source;
        this.multi = null;
    }

    Gauge(String name, String description, String unit, MultiSource multi) {
        super(name, description, unit, GAUGE);
        this.source = null;
        this.multi = multi;
    }

    /** One labelled value, for a {@link MultiSource}. */
    public static java.util.Map point(String key, Object label, double value) {
        java.util.Map attributes = new java.util.LinkedHashMap();
        attributes.put(key, label);
        return Instrument.point(attributes, value);
    }

    /** The value now, or NaN when reading it failed. */
    public double read() {
        if(source == null) {
            return Double.NaN;
        }
        try {
            return source.read();
        } catch (RuntimeException err) {
            return Double.NaN;
        }
    }

    public List points() {
        if(multi != null) {
            try {
                List points = multi.read();
                return points == null ? new java.util.ArrayList() : points;
            } catch (RuntimeException err) {
                return new java.util.ArrayList();
            }
        }
        return single(point(null, read()));
    }
}
