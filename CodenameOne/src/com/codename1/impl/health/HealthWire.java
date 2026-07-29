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
import com.codename1.health.BloodPressureSample;
import com.codename1.health.CategorySample;
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
import com.codename1.health.RecordingMethod;
import com.codename1.health.SamplePage;
import com.codename1.health.SeriesSample;
import com.codename1.health.SessionSample;
import com.codename1.health.SleepSample;
import com.codename1.health.WorkoutSample;
import com.codename1.health.nutrition.Nutrient;
import com.codename1.health.nutrition.NutritionSample;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    /// Types the Health Connect bridge can actually read.
    ///
    /// Not every portable type: having a Health Connect *permission* is not
    /// the same as having a record shape the bridge can turn into a sample.
    /// Reporting the wider set made the store advertise types that passed
    /// validation and then failed at read time with an invalid-argument
    /// error, which is a worse answer than "not supported here".
    ///
    /// Kept in step with `CN1HealthConnectBridge.recordClassFor` by
    /// `HealthBridgeTokenTableTest`, which parses the Kotlin and fails the
    /// build when the two drift.
    private static final String ANDROID_READABLE =
            ",steps,distance_walking_running,"
            + "flights_climbed,elevation_gained,"
            + "active_energy,wheelchair_pushes,hydration,heart_rate,"
            + "resting_heart_rate,oxygen_saturation,respiratory_rate,"
            + "body_temperature,basal_body_temperature,vo2_max,"
            + "blood_glucose,body_mass,lean_body_mass,bone_mass,"
            + "body_fat_percentage,height,power,speed,cycling_cadence,"
            + "running_cadence,";

    /// Types the bridge can write. Narrower than the readable set: the
    /// series-shaped types have no single-value write form.
    private static final String ANDROID_WRITABLE =
            ",steps,distance_walking_running,"
            + "flights_climbed,elevation_gained,"
            + "active_energy,wheelchair_pushes,hydration,body_mass,"
            + "lean_body_mass,bone_mass,body_fat_percentage,height,"
            + "resting_heart_rate,oxygen_saturation,respiratory_rate,"
            + "body_temperature,basal_body_temperature,vo2_max,"
            + "blood_glucose,heart_rate,";

    public static boolean isAndroidSupported(HealthDataType type) {
        return type != null
                && ANDROID_READABLE.indexOf("," + type.getId() + ",") >= 0;
    }

    /// Whether the Health Connect bridge can write `type`.
    public static boolean isAndroidWritable(HealthDataType type) {
        return type != null
                && ANDROID_WRITABLE.indexOf("," + type.getId() + ",") >= 0;
    }

    /// Whether the Health Connect bridge can delete records of `type`.
    ///
    /// The same set it can read. Deletion goes by record class plus
    /// identifier or range, so it needs a mapped record class and nothing
    /// more -- unlike a write, which also needs a single-value form the
    /// series-shaped types do not have.
    public static boolean isAndroidDeletable(HealthDataType type) {
        return isAndroidSupported(type);
    }

    // ------------------------------------------------------------------
    // samples
    // ------------------------------------------------------------------

    /// A copy of `sample` carrying its shape-specific state, or **null**
    /// when this build has no copy for that shape.
    ///
    /// Shared because two callers need the same answer and a second
    /// implementation of it would drift: the local store copies so a
    /// later edit cannot rewrite what it holds, and the shared write path
    /// copies so a chunk dispatched after the caller has moved on still
    /// carries what was submitted.
    ///
    /// Every shape, not only [QuantitySample]. The common setters --
    /// identifier, source, recording method, metadata -- exist on all of
    /// them, and a series, a workout or a nutrition entry has mutable
    /// state of its own on top.
    public static HealthSample copyOf(HealthSample sample) {
        if (sample == null) {
            return null;
        }
        HealthSample copy = copyShape(sample);
        if (copy == null) {
            return null;
        }
        carryCommon(sample, copy);
        return copy;
    }

    /// Copies the state every shape carries from `from` onto `to`.
    private static void carryCommon(HealthSample from, HealthSample to) {
        to.setId(from.getId());
        to.setSource(from.getSource());
        to.setRecordingMethod(from.getRecordingMethod());
        // Title and notes live on SessionSample, above the shapes that
        // carry them, so every per-shape branch missed them and a titled
        // workout or sleep session lost both the moment it was written.
        if (from instanceof SessionSample && to instanceof SessionSample) {
            ((SessionSample) to).setTitle(((SessionSample) from).getTitle());
            ((SessionSample) to).setNotes(((SessionSample) from).getNotes());
        }
        for (Map.Entry<String, String> e : from.getMetadata().entrySet()) {
            to.putMetadata(e.getKey(), e.getValue());
        }
    }

    /// A series carrying `starts`, `ends` and `values`, with the
    /// identifier and provenance of `source`.
    ///
    /// For a reader that has to stop inside a record because the caller's
    /// limit falls there. The piece keeps the original's identifier --
    /// the measurements came from one record and there is nothing else to
    /// call it -- and its stated span is the extent of what it actually
    /// carries, so it never claims time it no longer covers.
    public static SeriesSample seriesOfPoints(SeriesSample source,
            long[] starts, long[] ends, double[] values) {
        SeriesSample piece = SeriesSample.create(source.getType(),
                starts[0], ends[ends.length - 1], starts, ends, values,
                source.getUnit());
        carryCommon(source, piece);
        return piece;
    }

    /// The shape-specific half of [#copyOf(HealthSample)].
    ///
    /// A copy of `s` carrying its shape-specific state, or null when the
    /// shape is unknown to this build.
    ///
    /// Every shape, not only [QuantitySample]. The common setters --
    /// identifier, source, metadata -- exist on all of them, and a series,
    /// a workout or a nutrition entry has mutable state of its own on top.
    /// Returning the caller's object left the store aliased to it, so
    /// editing a sample after writing it silently changed what was stored,
    /// and editing one that came back from a read did the same. Losing the
    /// identifier that way breaks deletion by id.
    private static HealthSample copyShape(HealthSample s) {
        if (s instanceof QuantitySample) {
            QuantitySample q = (QuantitySample) s;
            return q.isInstantaneous()
                    ? QuantitySample.create(q.getType(), q.getQuantity(),
                            q.getStartMillis())
                    : QuantitySample.create(q.getType(), q.getQuantity(),
                            q.getStartMillis(), q.getEndMillis());
        }
        if (s instanceof SeriesSample) {
            SeriesSample series = (SeriesSample) s;
            int n = series.size();
            long[] starts = new long[n];
            long[] ends = new long[n];
            double[] values = new double[n];
            for (int i = 0; i < n; i++) {
                starts[i] = series.getSampleStartMillis(i);
                ends[i] = series.getSampleEndMillis(i);
                values[i] = series.getSampleValue(i, series.getUnit());
            }
            return SeriesSample.create(series.getType(),
                    series.getStartMillis(), series.getEndMillis(), starts,
                    ends, values, series.getUnit());
        }
        if (s instanceof BloodPressureSample) {
            BloodPressureSample bp = (BloodPressureSample) s;
            BloodPressureSample copy = BloodPressureSample.create(
                    bp.getSystolic(), bp.getDiastolic(), bp.getStartMillis());
            copy.setPulse(bp.getPulse());
            copy.setBodyPosition(bp.getBodyPosition());
            copy.setMeasurementLocation(bp.getMeasurementLocation());
            return copy;
        }
        if (s instanceof CategorySample) {
            CategorySample c = (CategorySample) s;
            return c.isInstantaneous()
                    ? CategorySample.create(c.getType(), c.getValue(),
                            c.getStartMillis())
                    : CategorySample.create(c.getType(), c.getValue(),
                            c.getStartMillis(), c.getEndMillis());
        }
        if (s instanceof SleepSample) {
            SleepSample sleep = (SleepSample) s;
            return SleepSample.create(sleep.getStartMillis(),
                    sleep.getEndMillis(), sleep.getStages());
        }
        if (s instanceof WorkoutSample) {
            WorkoutSample w = (WorkoutSample) s;
            WorkoutSample copy = WorkoutSample.create(w.getActivityType(),
                    w.getStartMillis(), w.getEndMillis());
            copy.setPlatformCode(w.getPlatformCode());
            copy.setTotalEnergy(w.getTotalEnergy());
            copy.setTotalDistance(w.getTotalDistance());
            copy.setActiveDurationMillis(w.getActiveDurationMillis());
            return copy;
        }
        if (s instanceof NutritionSample) {
            NutritionSample n = (NutritionSample) s;
            NutritionSample copy = n.isInstantaneous()
                    ? NutritionSample.create(n.getStartMillis())
                    : NutritionSample.create(n.getStartMillis(),
                            n.getEndMillis());
            for (Nutrient nutrient : n.getNutrients()) {
                HealthQuantity amount = n.getNutrient(nutrient);
                if (amount != null) {
                    copy.setNutrient(nutrient, amount.getRawValue(),
                            amount.getUnit());
                }
            }
            copy.setMealType(n.getMealType());
            copy.setFoodName(n.getFoodName());
            return copy;
        }
        return null;
    }

    /// Encodes samples as tab-separated lines for a write.
    ///
    /// A [SeriesSample] is written out point by point. Neither platform
    /// accepts a series through this payload -- HealthKit has no series
    /// save at all here, and the Health Connect bridge inserts discrete
    /// records -- so the choice was between persisting the measurements
    /// and persisting nothing. Skipping the sample outright is what it did
    /// before, and because both ports hand this payload straight to their
    /// bridge, an all-series write produced an empty batch that completed
    /// successfully with no identifiers: the caller was told the write
    /// worked while none of the data reached the store. The points survive;
    /// the record identity does not, which is why the returned result
    /// carries one identifier per point rather than one per series.
    ///
    /// Shapes this cannot carry at all are reported by
    /// [#unsupportedForWrite(List)] so a port can refuse the write rather
    /// than report a success it did not perform.
    public static String encodeSamples(List<HealthSample> samples) {
        StringBuilder sb = new StringBuilder();
        for (HealthSample s : samples) {
            if (s instanceof SeriesSample) {
                SeriesSample series = (SeriesSample) s;
                for (int i = 0; i < series.size(); i++) {
                    appendSample(sb, series.toQuantitySample(i));
                }
                continue;
            }
            if (!(s instanceof QuantitySample)) {
                continue;
            }
            appendSample(sb, (QuantitySample) s);
        }
        return sb.toString();
    }

    private static void appendSample(StringBuilder sb, QuantitySample q) {
        HealthUnit unit = q.getQuantity().getUnit();
        sb.append(nullToEmpty(q.getId())).append(FIELD)
          .append(q.getType().getId()).append(FIELD)
          .append(q.getStartMillis()).append(FIELD)
          .append(q.getEndMillis()).append(FIELD)
          .append(q.getQuantity().getRawValue()).append(FIELD)
          .append(unit.getSymbol()).append(FIELD)
          .append(q.getRecordingMethod().name()).append(LINE);
    }

    /// The first sample [#encodeSamples(List)] would drop, or null.
    ///
    /// Sessions and categories have no line in this format. They were
    /// documented as being encoded by the port-specific bridges, which is
    /// not what either port does -- both pass this payload through
    /// unchanged -- so a sleep or workout sample handed to a mobile write
    /// vanished and the write still resolved successfully. A port calls
    /// this first and fails the write instead.
    public static HealthSample unsupportedForWrite(
            List<HealthSample> samples) {
        for (HealthSample s : samples) {
            if (!(s instanceof QuantitySample)
                    && !(s instanceof SeriesSample)) {
                return s;
            }
        }
        return null;
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
        String nextToken = null;
        boolean truncated = false;
        int start = 0;
        while (start < payload.length()) {
            int end = payload.indexOf(LINE, start);
            if (end < 0) {
                end = payload.length();
            }
            String line = payload.substring(start, end);
            start = end + 1;
            if (line.length() > 0 && line.charAt(0) == PAGE_MARKER) {
                // Trailer: the platform's continuation state. Without it
                // every page claimed to be the last complete one, so the
                // documented paging loop could never fetch the rest and a
                // long history was silently cut off at the first limit.
                String[] f = split(line.substring(1));
                if (f.length > 0 && f[0].length() > 0) {
                    nextToken = f[0];
                }
                truncated = f.length > 1 && "1".equals(f[1]);
                continue;
            }
            HealthSample s = line.length() > 0
                    && line.charAt(0) == SERIES_MARKER
                    ? decodeSeriesLine(line.substring(1))
                    : decodeSampleLine(line);
            if (s != null) {
                out.add(s);
            }
        }
        return new SamplePage(out, nextToken, truncated);
    }

    /// First character of a line carrying a whole series record.
    ///
    /// A bridge emits one only when the query turned flattening off. The
    /// default stays one line per measurement, which is both the common
    /// case and the cheaper one to parse.
    public static final char SERIES_MARKER = '~';

    /// Separates the measurements of a series.
    private static final char POINT = ',';

    /// Separates a measurement's start, end and value.
    private static final char POINT_FIELD = ':';

    /// Appends a series as a single line: the shared fields, then the
    /// measurements packed into the last one.
    ///
    /// The values ride in one field rather than in extra columns because a
    /// series can hold tens of thousands of points and the decoder walks
    /// them without splitting the line into that many strings.
    public static void appendSeries(StringBuilder sb, SeriesSample series,
            String sourceBundleId, String sourceName, String deviceName) {
        sb.append(SERIES_MARKER)
          .append(nullToEmpty(series.getId())).append(FIELD)
          .append(series.getType().getId()).append(FIELD)
          .append(series.getStartMillis()).append(FIELD)
          .append(series.getEndMillis()).append(FIELD)
          .append(series.size()).append(FIELD)
          .append(series.getUnit().getSymbol()).append(FIELD)
          .append(nullToEmpty(sourceBundleId)).append(FIELD)
          .append(nullToEmpty(sourceName)).append(FIELD)
          .append(nullToEmpty(deviceName)).append(FIELD)
          .append(series.getRecordingMethod().name()).append(FIELD);
        for (int i = 0; i < series.size(); i++) {
            if (i > 0) {
                sb.append(POINT);
            }
            sb.append(series.getSampleStartMillis(i)).append(POINT_FIELD)
              .append(series.getSampleEndMillis(i)).append(POINT_FIELD)
              .append(series.getSampleValue(i, series.getUnit()));
        }
        sb.append(LINE);
    }

    private static HealthSample decodeSeriesLine(String line) {
        String[] f = split(line);
        if (f.length < 11) {
            return null;
        }
        HealthDataType type = HealthDataType.forId(f[1]);
        HealthUnit unit = HealthUnit.forSymbol(f[5]);
        if (type == null || unit == null) {
            return null;
        }
        try {
            int count = Integer.parseInt(f[4]);
            long[] starts = new long[count];
            long[] ends = new long[count];
            double[] values = new double[count];
            if (!parsePoints(f[10], starts, ends, values)) {
                return null;
            }
            SeriesSample series = SeriesSample.create(type,
                    Long.parseLong(f[2]), Long.parseLong(f[3]), starts, ends,
                    values, unit);
            if (f[0].length() > 0) {
                series.setId(f[0]);
            }
            if (f[6].length() > 0) {
                series.setSource(new HealthSource(f[6], emptyToNull(f[7]),
                        emptyToNull(f[8]), null, null));
            }
            RecordingMethod m = recordingMethod(f[9]);
            if (m != null) {
                series.setRecordingMethod(m);
            }
            return series;
        } catch (NumberFormatException ex) {
            return null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /// Fills the arrays from the packed measurement field.
    ///
    /// Returns false when the field holds a different number of points
    /// than the count column claimed, which is the one inconsistency that
    /// would otherwise produce a series whose tail is silently zeroes.
    private static boolean parsePoints(String packed, long[] starts,
            long[] ends, double[] values) {
        int index = 0;
        int at = 0;
        while (at < packed.length() && index < starts.length) {
            int next = packed.indexOf(POINT, at);
            if (next < 0) {
                next = packed.length();
            }
            int a = packed.indexOf(POINT_FIELD, at);
            if (a < 0 || a > next) {
                return false;
            }
            int b = packed.indexOf(POINT_FIELD, a + 1);
            if (b < 0 || b > next) {
                return false;
            }
            starts[index] = Long.parseLong(packed.substring(at, a));
            ends[index] = Long.parseLong(packed.substring(a + 1, b));
            values[index] = Double.parseDouble(packed.substring(b + 1, next));
            index++;
            at = next + 1;
        }
        return index == starts.length && at >= packed.length();
    }

    /// First character of the optional trailer line carrying paging state.
    ///
    /// A marker rather than a fixed position because ports stream sample
    /// lines as they read them and only learn the continuation state at
    /// the end.
    public static final char PAGE_MARKER = '#';

    /// Decodes the change page produced by the Health Connect bridge.
    ///
    /// The first line carries the next token, whether the previous token
    /// had expired and whether more pages remain. Every line after it is a
    /// change: `+` followed by an ordinary sample line, or `-` followed by
    /// the identifier of a deleted record.
    ///
    /// A change line this build cannot decode is skipped rather than
    /// failing the page, matching [#decodeSamplePage(String)]. The page is
    /// never silently emptied, though: an unreadable header yields a null
    /// return so the caller can leave its cursor where it was rather than
    /// advancing past changes it never saw.
    public static HealthChangePage decodeChangePage(String payload) {
        if (payload == null || payload.length() == 0) {
            return null;
        }
        int nl = payload.indexOf(LINE);
        if (nl < 0) {
            return null;
        }
        String[] head = split(payload.substring(0, nl));
        if (head.length < 3) {
            return null;
        }
        HealthChangePage page = new HealthChangePage(head[0],
                "1".equals(head[1]), "1".equals(head[2]));
        int start = nl + 1;
        while (start < payload.length()) {
            int end = payload.indexOf(LINE, start);
            if (end < 0) {
                end = payload.length();
            }
            String line = payload.substring(start, end);
            start = end + 1;
            if (line.length() < 2) {
                continue;
            }
            char op = line.charAt(0);
            String body = line.substring(2);
            if (op == '-') {
                if (body.trim().length() > 0) {
                    page.deletedIds.add(body.trim());
                }
            } else if (op == '+') {
                HealthSample s = decodeSampleLine(body);
                if (s != null) {
                    page.added.add(s);
                }
            }
        }
        return page;
    }

    private static RecordingMethod recordingMethod(String token) {
        if ("MANUAL_ENTRY".equals(token)) {
            return RecordingMethod.MANUAL_ENTRY;
        }
        if ("AUTOMATIC".equals(token)) {
            return RecordingMethod.AUTOMATIC;
        }
        if ("ACTIVE".equals(token)) {
            return RecordingMethod.ACTIVELY_RECORDED;
        }
        return null;
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
            if (f.length > 9) {
                // Field 9, its own column. Overloading field 7 -- the
                // source display name -- made getSource().getName() report
                // "AUTOMATIC" as the app that wrote the sample.
                RecordingMethod m = recordingMethod(f[9]);
                if (m != null) {
                    s.setRecordingMethod(m);
                }
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
          .append(",\"descending\":").append(query.isSortDescending())
          // The bridge decides record shape, so it needs this. Leaving it
          // out meant a query that asked to keep its series whole got one
          // scalar line per measurement anyway, and the option the API
          // documents was honoured nowhere.
          .append(",\"flatten\":").append(query.isFlattenSeries());
        if (query.getPageToken() != null) {
            sb.append(",\"pageToken\":\"")
              .append(escape(query.getPageToken())).append('"');
        }
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
        // The type list is emitted for both shapes. Health Connect deletes by
        // record class plus id and cannot resolve a bare id, so the id form
        // needs the type just as much as the range form does.
        StringBuilder sb = new StringBuilder("{\"types\":[");
        List<HealthDataType> types = request.getTypes();
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(types.get(i).getId()).append('"');
        }
        sb.append(']');
        if (request.isById()) {
            sb.append(",\"ids\":[");
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
            sb.append(",\"start\":").append(range.getStartMillis())
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
