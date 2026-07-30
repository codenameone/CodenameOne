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

import com.codename1.health.BloodPressureSample;
import com.codename1.health.CategorySample;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthQuantity;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthSource;
import com.codename1.health.HealthUnit;
import com.codename1.health.QuantitySample;
import com.codename1.health.RecordingMethod;
import com.codename1.health.SeriesSample;
import com.codename1.health.SessionSample;
import com.codename1.health.SleepSample;
import com.codename1.health.SleepStage;
import com.codename1.health.SleepStageInterval;
import com.codename1.health.WorkoutActivityType;
import com.codename1.health.WorkoutSample;
import com.codename1.health.nutrition.Nutrient;
import com.codename1.health.nutrition.NutritionSample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Serializes the local store's samples so they survive a restart.
///
/// #### Why not the wire format
///
/// [HealthWire] exists to cross a native boundary and carries only what
/// HealthKit and Health Connect accept through the sample write path:
/// quantities and series, with series flattened to their measurements. A
/// local store holds more than that -- workouts, sleep sessions with their
/// stages, nutrition, categories, blood pressure -- and persisting through
/// the wire format would have quietly dropped every one of those and
/// dissolved each series record into loose points. Silently losing the
/// shapes that only work locally is worse than not persisting at all,
/// because the app is told the write succeeded.
///
/// #### Format
///
/// One line per record, tab-separated, with a version marker on the first
/// line so a later format can be told apart from this one rather than
/// misread as it. Every string leaf is escaped, because titles, notes and
/// metadata are free text and a tab in a note would otherwise shift every
/// field after it.
///
/// A line this build cannot read -- an unknown shape, a data type dropped
/// from a later release -- is skipped rather than aborting the restore, so
/// one bad record costs one record.
final class LocalHealthCodec {

    /// Bumped only when a line stops meaning what it used to. A reader
    /// that does not recognise the marker restores nothing rather than
    /// guessing.
    static final String VERSION = "cn1health/1";

    private static final char FIELD = '\t';
    private static final char ITEM = ',';
    private static final char PART = ':';

    private LocalHealthCodec() {
    }

    // ------------------------------------------------------------------
    // encoding
    // ------------------------------------------------------------------

    static String encode(List<HealthSample> samples) {
        StringBuilder sb = new StringBuilder();
        sb.append(VERSION).append('\n');
        for (HealthSample s : samples) {
            String line = encodeOne(s);
            if (line != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static String encodeOne(HealthSample s) {
        String shape = shapeOf(s);
        if (shape == null || s.getType() == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(shape).append(FIELD);
        sb.append(esc(s.getType().getId())).append(FIELD);
        sb.append(s.getStartMillis()).append(FIELD);
        sb.append(s.getEndMillis()).append(FIELD);
        sb.append(esc(s.getId())).append(FIELD);
        HealthSource src = s.getSource();
        sb.append(esc(src == null ? null : src.getBundleId())).append(FIELD);
        sb.append(esc(src == null ? null : src.getName())).append(FIELD);
        sb.append(esc(src == null ? null : src.getDeviceName())).append(FIELD);
        sb.append(esc(src == null ? null : src.getDeviceModel())).append(FIELD);
        sb.append(esc(src == null ? null : src.getDeviceManufacturer()))
                .append(FIELD);
        sb.append(s.getRecordingMethod() == null
                ? "" : s.getRecordingMethod().name()).append(FIELD);
        appendMetadata(sb, s);
        sb.append(FIELD);
        appendPayload(sb, s, shape);
        return sb.toString();
    }

    private static void appendMetadata(StringBuilder sb, HealthSample s) {
        boolean first = true;
        for (Map.Entry<String, String> e : s.getMetadata().entrySet()) {
            if (!first) {
                sb.append(ITEM);
            }
            first = false;
            sb.append(esc(e.getKey())).append(PART).append(esc(e.getValue()));
        }
    }

    private static void appendPayload(StringBuilder sb, HealthSample s,
            String shape) {
        if ("Q".equals(shape)) {
            HealthQuantity q = ((QuantitySample) s).getQuantity();
            sb.append(esc(q.getUnit().getSymbol())).append(FIELD)
                    .append(q.getRawValue());
            return;
        }
        if ("C".equals(shape)) {
            sb.append(((CategorySample) s).getValue());
            return;
        }
        if ("S".equals(shape)) {
            SeriesSample series = (SeriesSample) s;
            HealthUnit unit = series.getUnit();
            sb.append(esc(unit.getSymbol())).append(FIELD);
            for (int i = 0; i < series.size(); i++) {
                if (i > 0) {
                    sb.append(ITEM);
                }
                sb.append(series.getSampleStartMillis(i)).append(PART)
                        .append(series.getSampleEndMillis(i)).append(PART)
                        .append(series.getSampleValue(i, unit));
            }
            return;
        }
        if ("SL".equals(shape)) {
            SleepSample sleep = (SleepSample) s;
            appendSessionText(sb, sleep);
            List<SleepStageInterval> stages = sleep.getStages();
            for (int i = 0; i < stages.size(); i++) {
                if (i > 0) {
                    sb.append(ITEM);
                }
                SleepStageInterval iv = stages.get(i);
                sb.append(iv.getStage().name()).append(PART)
                        .append(iv.getStartMillis()).append(PART)
                        .append(iv.getEndMillis());
            }
            return;
        }
        if ("W".equals(shape)) {
            WorkoutSample w = (WorkoutSample) s;
            appendSessionText(sb, w);
            sb.append(w.getActivityType() == null
                    ? "" : w.getActivityType().name()).append(FIELD);
            sb.append(w.getPlatformCode()).append(FIELD);
            // The raw field, not the getter: the getter substitutes the
            // wall duration when nothing was reported, and persisting that
            // would turn "not measured" into a measurement on the way back.
            sb.append(w.getActiveDurationMillis() == w.getDurationMillis()
                    ? -1 : w.getActiveDurationMillis()).append(FIELD);
            appendQuantity(sb, w.getTotalEnergy());
            sb.append(FIELD);
            appendQuantity(sb, w.getTotalDistance());
            return;
        }
        if ("N".equals(shape)) {
            NutritionSample n = (NutritionSample) s;
            appendSessionText(sb, n);
            sb.append(n.getMealType()).append(FIELD);
            List<Nutrient> nutrients = n.getNutrients();
            for (int i = 0; i < nutrients.size(); i++) {
                if (i > 0) {
                    sb.append(ITEM);
                }
                Nutrient nut = nutrients.get(i);
                HealthQuantity q = n.getNutrient(nut);
                sb.append(esc(nut.getId())).append(PART)
                        .append(esc(q.getUnit().getSymbol())).append(PART)
                        .append(q.getRawValue());
            }
            // Appended after the nutrients rather than beside the meal
            // type, so a store written before this field existed still
            // decodes -- it simply has no food name, which is what it had.
            sb.append(FIELD).append(esc(n.getFoodName()));
            return;
        }
        if ("BP".equals(shape)) {
            BloodPressureSample bp = (BloodPressureSample) s;
            appendQuantity(sb, bp.getSystolic());
            sb.append(FIELD);
            appendQuantity(sb, bp.getDiastolic());
            sb.append(FIELD);
            appendQuantity(sb, bp.getPulse());
            sb.append(FIELD).append(bp.getBodyPosition())
                    .append(FIELD).append(bp.getMeasurementLocation());
        }
    }

    private static void appendSessionText(StringBuilder sb,
            SessionSample s) {
        sb.append(esc(s.getTitle())).append(FIELD)
                .append(esc(s.getNotes())).append(FIELD);
    }

    private static void appendQuantity(StringBuilder sb, HealthQuantity q) {
        if (q == null) {
            sb.append("").append(FIELD).append("");
            return;
        }
        sb.append(esc(q.getUnit().getSymbol())).append(FIELD)
                .append(q.getRawValue());
    }

    private static String shapeOf(HealthSample s) {
        if (s instanceof BloodPressureSample) {
            return "BP";
        }
        if (s instanceof NutritionSample) {
            return "N";
        }
        if (s instanceof WorkoutSample) {
            return "W";
        }
        if (s instanceof SleepSample) {
            return "SL";
        }
        if (s instanceof SeriesSample) {
            return "S";
        }
        if (s instanceof CategorySample) {
            return "C";
        }
        if (s instanceof QuantitySample) {
            return "Q";
        }
        return null;
    }

    // ------------------------------------------------------------------
    // decoding
    // ------------------------------------------------------------------

    static List<HealthSample> decode(String blob) {
        List<HealthSample> out = new ArrayList<HealthSample>();
        if (blob == null || blob.length() == 0) {
            return out;
        }
        List<String> lines = split(blob, '\n');
        if (lines.isEmpty() || !VERSION.equals(lines.get(0))) {
            return out;
        }
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.length() == 0) {
                continue;
            }
            HealthSample s = decodeOne(line);
            if (s != null) {
                out.add(s);
            }
        }
        return out;
    }

    private static HealthSample decodeOne(String line) {
        try {
            List<String> f = split(line, FIELD);
            if (f.size() < 12) {
                return null;
            }
            String shape = f.get(0);
            HealthDataType type = HealthDataType.forId(unesc(f.get(1)));
            if (type == null) {
                return null;
            }
            long start = Long.parseLong(f.get(2));
            long end = Long.parseLong(f.get(3));
            HealthSample s = decodeShape(shape, type, start, end, f);
            if (s == null) {
                return null;
            }
            s.setId(unesc(f.get(4)));
            String bundle = unesc(f.get(5));
            if (bundle != null) {
                s.setSource(new HealthSource(bundle, unesc(f.get(6)),
                        unesc(f.get(7)), unesc(f.get(8)), unesc(f.get(9))));
            }
            if (f.get(10).length() > 0) {
                s.setRecordingMethod(RecordingMethod.valueOf(f.get(10)));
            }
            for (Map.Entry<String, String> e
                    : parsePairs(f.get(11)).entrySet()) {
                s.putMetadata(e.getKey(), e.getValue());
            }
            return s;
        } catch (RuntimeException ex) {
            // One unreadable record costs one record. Restoring is a
            // best-effort recovery of the app's own data, and refusing the
            // whole file over a single bad line would turn a small loss
            // into a total one.
            return null;
        }
    }

    private static HealthSample decodeShape(String shape,
            HealthDataType type, long start, long end, List<String> f) {
        if ("Q".equals(shape)) {
            return QuantitySample.create(type, quantity(f, 12), start, end);
        }
        if ("C".equals(shape)) {
            return CategorySample.create(type, Integer.parseInt(f.get(12)),
                    start, end);
        }
        if ("S".equals(shape)) {
            return decodeSeries(type, start, end, f);
        }
        if ("SL".equals(shape)) {
            SleepSample sleep = SleepSample.create(start, end);
            applySessionText(sleep, f);
            for (String item : split(f.get(14), ITEM)) {
                List<String> p = split(item, PART);
                if (p.size() == 3) {
                    sleep.addStage(new SleepStageInterval(
                            SleepStage.valueOf(p.get(0)),
                            Long.parseLong(p.get(1)),
                            Long.parseLong(p.get(2))));
                }
            }
            return sleep;
        }
        if ("W".equals(shape)) {
            WorkoutSample w = WorkoutSample.create(
                    WorkoutActivityType.valueOf(f.get(14)), start, end);
            applySessionText(w, f);
            w.setPlatformCode(Integer.parseInt(f.get(15)));
            w.setActiveDurationMillis(Long.parseLong(f.get(16)));
            w.setTotalEnergy(quantity(f, 17));
            w.setTotalDistance(quantity(f, 19));
            return w;
        }
        if ("N".equals(shape)) {
            NutritionSample n = NutritionSample.create(start, end);
            applySessionText(n, f);
            n.setMealType(Integer.parseInt(f.get(14)));
            for (String item : split(f.get(15), ITEM)) {
                List<String> p = split(item, PART);
                if (p.size() == 3) {
                    Nutrient nut = Nutrient.forId(unesc(p.get(0)));
                    HealthUnit unit = HealthUnit.forSymbol(unesc(p.get(1)));
                    if (nut != null && unit != null) {
                        n.setNutrient(nut, Double.parseDouble(p.get(2)),
                                unit);
                    }
                }
            }
            if (f.size() > 16) {
                n.setFoodName(unesc(f.get(16)));
            }
            return n;
        }
        if ("BP".equals(shape)) {
            BloodPressureSample bp = BloodPressureSample.create(
                    quantity(f, 12), quantity(f, 14), start);
            bp.setPulse(quantity(f, 16));
            bp.setBodyPosition(Integer.parseInt(f.get(18)));
            bp.setMeasurementLocation(Integer.parseInt(f.get(19)));
            return bp;
        }
        return null;
    }

    private static HealthSample decodeSeries(HealthDataType type, long start,
            long end, List<String> f) {
        HealthUnit unit = HealthUnit.forSymbol(unesc(f.get(12)));
        if (unit == null) {
            return null;
        }
        List<String> points = split(f.get(13), ITEM);
        List<long[]> spans = new ArrayList<long[]>();
        List<Double> values = new ArrayList<Double>();
        for (String point : points) {
            List<String> p = split(point, PART);
            if (p.size() != 3) {
                continue;
            }
            spans.add(new long[] {
                    Long.parseLong(p.get(0)), Long.parseLong(p.get(1)),
            });
            values.add(Double.valueOf(Double.parseDouble(p.get(2))));
        }
        if (spans.isEmpty()) {
            return null;
        }
        long[] starts = new long[spans.size()];
        long[] ends = new long[spans.size()];
        double[] vals = new double[spans.size()];
        for (int i = 0; i < spans.size(); i++) {
            starts[i] = spans.get(i)[0];
            ends[i] = spans.get(i)[1];
            vals[i] = values.get(i).doubleValue();
        }
        return SeriesSample.create(type, start, end, starts, ends, vals,
                unit);
    }

    private static void applySessionText(SessionSample s, List<String> f) {
        s.setTitle(unesc(f.get(12)));
        s.setNotes(unesc(f.get(13)));
    }

    /// A quantity written as a unit field followed by a value field, or
    /// two empty fields when it was never measured.
    private static HealthQuantity quantity(List<String> f, int at) {
        if (at + 1 >= f.size() || f.get(at).length() == 0) {
            return null;
        }
        HealthUnit unit = HealthUnit.forSymbol(unesc(f.get(at)));
        if (unit == null) {
            return null;
        }
        return new HealthQuantity(Double.parseDouble(f.get(at + 1)), unit);
    }

    private static Map<String, String> parsePairs(String field) {
        Map<String, String> out = new HashMap<String, String>();
        for (String item : split(field, ITEM)) {
            List<String> p = split(item, PART);
            if (p.size() == 2) {
                out.put(unesc(p.get(0)), unesc(p.get(1)));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // escaping
    // ------------------------------------------------------------------

    /// Escapes every separator this format uses, plus the escape character
    /// itself.
    ///
    /// An empty string is written as the sentinel `\e` rather than as
    /// nothing, because null and empty used to encode identically and read
    /// back as null. The public setters distinguish them -- `putMetadata`
    /// treats a null value as a removal and an empty value as a stored
    /// presence marker -- so a restart quietly deleted keys an app had set
    /// to "", and emptied titles, notes, food names and source fields came
    /// back as null.
    ///
    /// The sentinel is on empty rather than on null so that data already
    /// persisted keeps its meaning: an old record wrote nothing for null,
    /// and nothing still reads as null.
    ///
    /// `\e` is unambiguous because a real backslash is doubled on the way
    /// in, so no escaped value can produce it.
    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        if (s.length() == 0) {
            return "\\e";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\t': sb.append("\\t"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case ',':  sb.append("\\c"); break;
                case ':':  sb.append("\\o"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String unesc(String s) {
        if (s == null || s.length() == 0) {
            return null;
        }
        if ("\\e".equals(s)) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 >= s.length()) {
                sb.append(c);
                continue;
            }
            char n = s.charAt(++i);
            switch (n) {
                case 't': sb.append('\t'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 'c': sb.append(','); break;
                case 'o': sb.append(':'); break;
                default:  sb.append(n);
            }
        }
        return sb.toString();
    }

    /// Splits on a separator without treating an escaped one as a break,
    /// and keeps trailing empty fields -- which `String.split` drops and
    /// a record whose last field is empty depends on.
    private static List<String> split(String s, char sep) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                cur.append(c).append(s.charAt(++i));
                continue;
            }
            if (c == sep) {
                out.add(cur.toString());
                cur.setLength(0);
                continue;
            }
            cur.append(c);
        }
        out.add(cur.toString());
        return out;
    }
}
