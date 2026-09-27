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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A distribution of values -- durations, sizes -- kept as counts per bucket.
/// Exported as a cumulative explicit-bucket histogram.
///
/// The buckets are fixed when the histogram is created; the default
/// boundaries are OpenTelemetry's, which suit milliseconds.
///
/// A histogram created with label keys keeps a series per combination of
/// label values -- the server's request histogram has route, method and status.
/// The series are found by a map lookup on the first label, which is a constant
/// such as a route template, so nothing new is hashed; and they are capped, so a
/// label with unbounded values cannot grow the histogram without limit.
public final class Histogram extends Instrument {
    /// OpenTelemetry's default explicit bucket boundaries. Private, and handed
    /// out as copies: a histogram's boundaries must never change once values are
    /// counted against them, or the exported counts describe other buckets.
    private static final double[] DEFAULT_BOUNDS = {0, 5, 10, 25, 50, 75, 100, 250, 500,
            750, 1000, 2500, 5000, 7500, 10000};

    /// A copy of OpenTelemetry's default explicit bucket boundaries.
    public static double[] defaultBounds() {
        return (double[]) DEFAULT_BOUNDS.clone();
    }

    /// Series beyond this many share one, marked as overflow.
    static final int MAX_SERIES = 2000;

    private final double[] bounds;
    private final String[] labels;
    private final Series plain;
    /// first label value -> List of Series.
    private final Map byFirst = new HashMap();
    /// The byFirst key of a series whose first label value is null.
    private static final Object NO_VALUE = new Object();
    private int seriesCount;
    private Series overflow;

    Histogram(String name, String description, String unit, double[] bounds,
              String[] labels) {
        super(name, description, unit, HISTOGRAM);
        // Copied, so the caller changing its array later cannot move the
        // boundaries or rename the labels under counts already recorded.
        this.bounds = bounds == null ? DEFAULT_BOUNDS : (double[]) bounds.clone();
        for (int iter = 0 ; iter < this.bounds.length ; iter++) {
            double b = this.bounds[iter];
            if (Double.isNaN(b) || Double.isInfinite(b)
                    || (iter > 0 && b <= this.bounds[iter - 1])) {
                throw new IllegalArgumentException("Histogram " + name + ": bucket "
                        + "boundaries must be finite and strictly ascending");
            }
        }
        this.labels = labels == null ? new String[0] : (String[]) labels.clone();
        java.util.Set exported = new java.util.HashSet();
        for (int iter = 0 ; iter < this.labels.length ; iter++) {
            // Null is an unused slot of the three; an empty name is a mistake.
            if (this.labels[iter] == null) {
                continue;
            }
            if (this.labels[iter].length() == 0) {
                throw new IllegalArgumentException("Histogram " + name + ": a label key "
                        + "is empty");
            }
            // As Prometheus will see them: folded to its alphabet, and beside the
            // "le" every bucket sample carries. Two that fold alike, or one that
            // IS le, repeat a label name in a sample, and the scrape is rejected.
            String folded = Metrics.promLabel(this.labels[iter]);
            if ("le".equals(folded) || !exported.add(folded)) {
                throw new IllegalArgumentException("Histogram " + name + ": label key "
                        + this.labels[iter] + " is exported to Prometheus as " + folded
                        + ", which " + ("le".equals(folded) ? "the bucket boundary uses"
                        : "another key of this histogram already is"));
            }
        }
        this.plain = new Series(null, this.bounds.length + 1);
    }

    /// Whether `other` has the same boundaries and label keys: two
    /// registrations of one name must agree on both, or the values the second
    /// records are read against the first's buckets and labels.
    boolean sameShape(Histogram other) {
        return java.util.Arrays.equals(bounds, other.bounds)
                && java.util.Arrays.equals(labels, other.labels);
    }

    /// The label keys this histogram was created with.
    public String[] getLabelKeys() {
        return (String[]) labels.clone();
    }

    /// Records one value in the series without labels.
    public void record(double value) {
        plain.record(value, bounds);
    }

    /// Records one value in the series for these label values, given in the
    /// order of the keys the histogram was created with.
    public void record(double value, Object first, Object second, Object third) {
        Series s;
        synchronized (this) {
            s = find(first, second, third);
        }
        s.record(value, bounds);
    }

    private Series find(Object first, Object second, Object third) {
        // Null has a key of its own: exported, null omits the attribute and ""
        // sends an empty one, so they are two series and must stay two.
        Object key = first == null ? NO_VALUE : first;
        List list = (List) byFirst.get(key);
        if (list != null) {
            for (Object element : list) {
                Series s = (Series) element;
                if (same(s.values[1], second) && same(s.values[2], third)) {
                    return s;
                }
            }
        }
        if (seriesCount >= MAX_SERIES) {
            if (overflow == null) {
                overflow = new Series(null, bounds.length + 1);
            }
            return overflow;
        }
        Series created = new Series(new Object[] {first, second, third}, bounds.length + 1);
        if (list == null) {
            list = new ArrayList(4);
            byFirst.put(key, list);
        }
        list.add(created);
        seriesCount++;
        return created;
    }

    private static boolean same(Object a, Object b) {
        return a == b || (a != null && a.equals(b)); //NOPMD CompareObjectsWithEquals - identity first, then equals
    }

    @Override
    public List points() {
        List out = new ArrayList();
        if (plain.count() > 0 || byFirst.isEmpty()) {
            out.add(plain.point(bounds, null, false));
        }
        synchronized (this) {
            java.util.Iterator lists = byFirst.values().iterator();
            while (lists.hasNext()) {
                List list = (List) lists.next();
                for (Object element : list) {
                    out.add(((Series) element).point(bounds, labels, false));
                }
            }
            if (overflow != null) {
                out.add(overflow.point(bounds, null, true));
            }
        }
        return out;
    }

    /// One set of bucket counts.
    static final class Series {
        final Object[] values;
        private final long[] buckets;
        private long count;
        private double sum;
        private double min = Double.NaN;
        private double max = Double.NaN;

        Series(Object[] values, int bucketCount) {
            this.values = values == null ? new Object[3] : values;
            this.buckets = new long[bucketCount];
        }

        synchronized void record(double value, double[] bounds) {
            if (Double.isNaN(value)) {
                return;
            }
            int bucket = bounds.length;
            for (int iter = 0 ; iter < bounds.length ; iter++) {
                if (value <= bounds[iter]) {
                    bucket = iter;
                    break;
                }
            }
            buckets[bucket]++;
            count++;
            sum += value;
            if (count == 1 || value < min) {
                min = value;
            }
            if (count == 1 || value > max) {
                max = value;
            }
        }

        synchronized long count() {
            return count;
        }

        synchronized Map point(double[] bounds, String[] keys, boolean isOverflow) {
            Map p = new LinkedHashMap();
            Map attributes = new LinkedHashMap();
            if (isOverflow) {
                attributes.put("otel.metric.overflow", Boolean.TRUE);
            } else if (keys != null) {
                for (int iter = 0 ; iter < keys.length && iter < values.length ; iter++) {
                    Object v = values[iter];
                    if (v != null) {
                        attributes.put(keys[iter], v instanceof Integer
                                ? Long.valueOf(((Integer) v).longValue()) : v);
                    }
                }
            }
            p.put("attributes", attributes);
            p.put("count", Long.valueOf(count));
            p.put("sum", Double.valueOf(sum));
            if (count > 0) {
                p.put("min", Double.valueOf(min));
                p.put("max", Double.valueOf(max));
            }
            List b = new ArrayList(bounds.length);
            for (double element : bounds) {
                b.add(Double.valueOf(element));
            }
            p.put("bounds", b);
            List c = new ArrayList(buckets.length);
            for (long element : buckets) {
                c.add(Long.valueOf(element));
            }
            p.put("buckets", c);
            return p;
        }
    }
}
