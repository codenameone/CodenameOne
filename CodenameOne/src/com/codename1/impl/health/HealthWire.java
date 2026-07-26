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
package com.codename1.impl.health;

import com.codename1.health.AggregateMetric;
import com.codename1.health.AggregateQuery;
import com.codename1.health.AggregateResult;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthDeleteRequest;
import com.codename1.health.HealthQuantity;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthSource;
import com.codename1.health.HealthTimeRange;
import com.codename1.health.HealthUnit;
import com.codename1.health.HealthWriteResult;
import com.codename1.health.QuantitySample;
import com.codename1.health.SampleQuery;
import com.codename1.health.SamplePage;

import java.util.ArrayList;
import java.util.List;

/// The wire format shared by the native health bridges.
///
/// #### Why line-delimited text rather than JSON
///
/// A year of continuous heart rate is on the order of half a million
/// samples. Parsing that as JSON on ParparVM allocates an object graph
/// several times the size of the data and will exhaust the heap on a
/// phone. Samples therefore cross the boundary as tab-separated lines,
/// which stream and parse with a bounded working set. JSON is used only
/// for cold, irregular payloads -- a query descriptor, per-sample metadata
/// -- where the volume is a handful of fields rather than a year of them.
///
/// Both the Android bridge and the iOS native layer speak this format, so
/// the encoding and decoding live here rather than being written twice.
///
/// #### Sample line
///
/// ```
/// id \t typeId \t startMillis \t endMillis \t value \t unitSymbol
///    \t sourceBundleId \t sourceName \t deviceName
/// ```
///
/// Fields after the unit are optional and may be empty. Unknown type ids
/// and unit symbols cause the line to be skipped rather than failing the
/// whole page, so a platform that gains a new record type does not break
/// an older app.
public final class HealthWire {

    private static final char FIELD = '\t';
    private static final char LINE = '\n';

    private HealthWire() {
    }

    /// Whether Health Connect has a record type for this data type.
    ///
    /// Kept alongside the wire format because the two travel together: a
    /// type with no Health Connect equivalent can never appear in a
    /// payload, and claiming to support it would produce queries the
    /// bridge cannot answer.
    public static boolean isAndroidSupported(HealthDataType type) {
        // Every type in HealthDataType has a Health Connect record, by
        // construction: the builder's permission table is derived from the
        // same list, and a type with no Health Connect equivalent would
        // have no permission to declare and so could never be requested.
        // Keep this in step if a platform-specific type is ever added.
        return type != null;
    }

    // ------------------------------------------------------------------
    // samples
    // ------------------------------------------------------------------

    /// Encodes samples as tab-separated lines for a write.
    public static String encodeSamples(List<HealthSample> samples) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < samples.size(); i++) {
            HealthSample s = samples.get(i);
            if (!(s instanceof QuantitySample)) {
                // Sessions and categories are encoded by the port-specific
                // bridges, which know their platform's record shapes.
                continue;
            }
            QuantitySample q = (QuantitySample) s;
            HealthUnit unit = q.getQuantity().getUnit();
            sb.append(nullToEmpty(q.getId())).append(FIELD)
              .append(q.getType().getId()).append(FIELD)
              .append(q.getStartMillis()).append(FIELD)
              .append(q.getEndMillis()).append(FIELD)
              .append(q.getQuantity().getRawValue()).append(FIELD)
              .append(unit.getSymbol()).append(FIELD)
              .append(q.getRecordingMethod().name()).append(LINE);
        }
        return sb.toString();
    }

    /// Decodes a page of tab-separated sample lines.
    ///
    /// Malformed and unrecognised lines are skipped rather than failing
    /// the page: one unparseable record out of fifty thousand should cost
    /// that record, not the whole read.
    public static SamplePage decodeSamplePage(String payload) {
        List<HealthSample> out = new ArrayList<HealthSample>();
        if (payload == null || payload.length() == 0) {
            return new SamplePage(out, null, false);
        }
        int start = 0;
        while (start < payload.length()) {
            int end = payload.indexOf(LINE, start);
            if (end < 0) {
                end = payload.length();
            }
            HealthSample s = decodeSampleLine(
                    payload.substring(start, end));
            if (s != null) {
                out.add(s);
            }
            start = end + 1;
        }
        return new SamplePage(out, null, false);
    }

    private static HealthSample decodeSampleLine(String line) {
        if (line == null || line.trim().length() == 0) {
            return null;
        }
        String[] f = split(line);
        if (f.length < 6) {
            return null;
        }
        HealthDataType type = HealthDataType.forId(f[1]);
        HealthUnit unit = HealthUnit.forSymbol(f[5]);
        if (type == null || unit == null) {
            // A record type or unit this build does not know. Skipping is
            // the right call: an older app reading a newer store should
            // lose the unfamiliar rows, not the familiar ones.
            return null;
        }
        try {
            long startMillis = Long.parseLong(f[2]);
            long endMillis = Long.parseLong(f[3]);
            double value = Double.parseDouble(f[4]);
            QuantitySample s = startMillis == endMillis
                    ? QuantitySample.create(type,
                            new HealthQuantity(value, unit), startMillis)
                    : QuantitySample.create(type,
                            new HealthQuantity(value, unit), startMillis,
                            endMillis);
            if (f[0].length() > 0) {
                s.setId(f[0]);
            }
            if (f.length > 6 && f[6].length() > 0) {
                s.setSource(new HealthSource(f[6],
                        f.length > 7 ? emptyToNull(f[7]) : null,
                        f.length > 8 ? emptyToNull(f[8]) : null, null,
                        null));
            }
            return s;
        } catch (NumberFormatException ex) {
            return null;
        } catch (IllegalArgumentException ex) {
            // An interval-only type sent as an instant, for example.
            return null;
        }
    }

    // ------------------------------------------------------------------
    // requests
    // ------------------------------------------------------------------

    /// Encodes a read as a small JSON object. Cold and bounded, so JSON is
    /// the right trade here.
    public static String encodeSampleQuery(SampleQuery query) {
        HealthTimeRange range = query.getTimeRange()
                .resolve(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder("{\"types\":[");
        List<HealthDataType> types = query.getTypes();
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(types.get(i).getId()).append('"');
        }
        sb.append("],\"start\":").append(range.getStartMillis())
          .append(",\"end\":").append(range.getEndMillis())
          .append(",\"limit\":").append(query.getLimit())
          .append(",\"descending\":").append(query.isSortDescending());
        List<String> sources = query.getSources();
        if (!sources.isEmpty()) {
            sb.append(",\"sources\":[");
            for (int i = 0; i < sources.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('"').append(escape(sources.get(i))).append('"');
            }
            sb.append(']');
        }
        return sb.append('}').toString();
    }

    /// Encodes an aggregate request, including the already-computed bucket
    /// boundaries so the bridge never has to reason about calendars.
    public static String encodeAggregateQuery(AggregateQuery query,
            long[] boundaries) {
        StringBuilder sb = new StringBuilder("{\"types\":[");
        List<HealthDataType> types = query.getTypes();
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(types.get(i).getId()).append('"');
        }
        sb.append("],\"metrics\":[");
        List<AggregateMetric> metrics = query.getMetrics();
        for (int i = 0; i < metrics.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(metrics.get(i).name()).append('"');
        }
        sb.append("],\"buckets\":[");
        for (int i = 0; i < boundaries.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(boundaries[i]);
        }
        return sb.append("]}").toString();
    }

    /// Encodes a delete request.
    public static String encodeDeleteRequest(HealthDeleteRequest request) {
        StringBuilder sb = new StringBuilder("{");
        if (request.isById()) {
            sb.append("\"ids\":[");
            List<String> ids = request.getSampleIds();
            for (int i = 0; i < ids.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('"').append(escape(ids.get(i))).append('"');
            }
            sb.append(']');
        } else {
            HealthTimeRange range = request.getTimeRange()
                    .resolve(System.currentTimeMillis());
            sb.append("\"types\":[");
            List<HealthDataType> types = request.getTypes();
            for (int i = 0; i < types.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('"').append(types.get(i).getId()).append('"');
            }
            sb.append("],\"start\":").append(range.getStartMillis())
              .append(",\"end\":").append(range.getEndMillis());
        }
        return sb.append('}').toString();
    }

    // ------------------------------------------------------------------
    // results
    // ------------------------------------------------------------------

    /// Decodes the ids assigned by a write, one per line.
    public static HealthWriteResult decodeWriteResult(String payload) {
        HealthWriteResult result = new HealthWriteResult();
        if (payload == null) {
            return result;
        }
        int start = 0;
        while (start < payload.length()) {
            int end = payload.indexOf(LINE, start);
            if (end < 0) {
                end = payload.length();
            }
            String id = payload.substring(start, end).trim();
            if (id.length() > 0) {
                result.addSampleId(id);
            }
            start = end + 1;
        }
        return result;
    }

    /// Decodes aggregate results, one line per bucket:
    /// `bucketIndex \t typeId \t metric \t value \t unitSymbol`.
    ///
    /// Buckets the platform reported nothing for are left empty rather
    /// than being filled with zeros -- the distinction the whole aggregate
    /// contract rests on.
    public static List<AggregateResult> decodeAggregates(String payload,
            AggregateQuery query, long[] boundaries) {
        List<AggregateResult> out = new ArrayList<AggregateResult>();
        for (int i = 0; i + 1 < boundaries.length; i++) {
            out.add(new AggregateResult(boundaries[i], boundaries[i + 1]));
        }
        if (payload == null || out.isEmpty()) {
            return out;
        }
        int start = 0;
        while (start < payload.length()) {
            int end = payload.indexOf(LINE, start);
            if (end < 0) {
                end = payload.length();
            }
            applyAggregateLine(out, payload.substring(start, end));
            start = end + 1;
        }
        return out;
    }

    private static void applyAggregateLine(List<AggregateResult> buckets,
            String line) {
        if (line == null || line.trim().length() == 0) {
            return;
        }
        String[] f = split(line);
        if (f.length < 5) {
            return;
        }
        try {
            int index = Integer.parseInt(f[0]);
            if (index < 0 || index >= buckets.size()) {
                return;
            }
            HealthDataType type = HealthDataType.forId(f[1]);
            HealthUnit unit = HealthUnit.forSymbol(f[4]);
            if (type == null || unit == null) {
                return;
            }
            AggregateMetric metric = AggregateMetric.valueOf(f[2]);
            buckets.get(index).put(type, metric,
                    new HealthQuantity(Double.parseDouble(f[3]), unit));
        } catch (RuntimeException ex) {
            // Unknown metric name, unparseable number: skip this line.
        }
    }

    // ------------------------------------------------------------------

    private static String[] split(String line) {
        List<String> parts = new ArrayList<String>();
        int start = 0;
        while (true) {
            int idx = line.indexOf(FIELD, start);
            if (idx < 0) {
                parts.add(line.substring(start));
                break;
            }
            parts.add(line.substring(start, idx));
            start = idx + 1;
        }
        return parts.toArray(new String[parts.size()]);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        return s == null || s.length() == 0 ? null : s;
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
