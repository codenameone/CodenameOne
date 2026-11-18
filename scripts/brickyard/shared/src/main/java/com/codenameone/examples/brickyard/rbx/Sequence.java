/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codenameone.examples.brickyard.rbx;

import com.codenameone.examples.brickyard.luau.Interp;
import com.codenameone.examples.brickyard.luau.LuaTable;

/// `NumberSequence` and `ColorSequence`: keypoints over 0..1, sampled
/// linearly. A keypoint is `{time, value, envelope}`; colors use the value
/// slot for a [Color3].
public final class Sequence extends RbxType {
    public final boolean color;
    public final double[] times;
    public final Object[] values;
    public final double[] envelopes;

    public Sequence(boolean color, double[] times, Object[] values, double[] envelopes) {
        this.color = color;
        this.times = times;
        this.values = values;
        this.envelopes = envelopes;
    }

    public static Sequence number(double v) {
        return new Sequence(false, new double[] {0, 1}, new Object[] {Args.n(v), Args.n(v)}, new double[] {0, 0});
    }

    public static Sequence numbers(double a, double b) {
        return new Sequence(false, new double[] {0, 1}, new Object[] {Args.n(a), Args.n(b)}, new double[] {0, 0});
    }

    public static Sequence color(Color3 c) {
        return new Sequence(true, new double[] {0, 1}, new Object[] {c, c}, new double[] {0, 0});
    }

    public static Sequence colors(Color3 a, Color3 b) {
        return new Sequence(true, new double[] {0, 1}, new Object[] {a, b}, new double[] {0, 0});
    }

    @Override
    public String typeName() {
        return color ? "ColorSequence" : "NumberSequence";
    }

    /// The number at `t` (number sequences).
    public double sampleNumber(double t) {
        int i = segment(t);
        double a = ((Double) values[i]).doubleValue();
        if (i + 1 >= times.length) {
            return a;
        }
        double b = ((Double) values[i + 1]).doubleValue();
        double span = times[i + 1] - times[i];
        double f = span <= 0 ? 0 : (t - times[i]) / span;
        return a + (b - a) * f;
    }

    /// The color at `t` (color sequences).
    public Color3 sampleColor(double t) {
        int i = segment(t);
        Color3 a = (Color3) values[i];
        if (i + 1 >= times.length) {
            return a;
        }
        Color3 b = (Color3) values[i + 1];
        double span = times[i + 1] - times[i];
        double f = span <= 0 ? 0 : (t - times[i]) / span;
        return new Color3(a.r + (b.r - a.r) * f, a.g + (b.g - a.g) * f, a.b + (b.b - a.b) * f);
    }

    private int segment(double t) {
        for (int i = times.length - 2; i >= 0; i--) {
            if (t >= times[i]) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public Object get(Interp in, String k) {
        if ("Keypoints".equals(k)) {
            LuaTable t = new LuaTable();
            for (int i = 0; i < times.length; i++) {
                t.rawseti(i + 1, new Keypoint(color, times[i], values[i], envelopes[i]));
            }
            return t;
        }
        return super.get(in, k);
    }

    @Override
    public String str() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < times.length; i++) {
            b.append(Interp.numberToString(times[i])).append(' ');
            b.append(values[i] instanceof Color3 ? ((Color3) values[i]).str() : Interp.rawToString(values[i]));
            b.append(' ').append(Interp.numberToString(envelopes[i])).append(' ');
        }
        return b.toString();
    }

    /// One keypoint.
    public static final class Keypoint extends RbxType {
        final boolean color;
        public final double time;
        public final Object value;
        public final double envelope;

        public Keypoint(boolean color, double time, Object value, double envelope) {
            this.color = color;
            this.time = time;
            this.value = value;
            this.envelope = envelope;
        }

        @Override
        public String typeName() {
            return color ? "ColorSequenceKeypoint" : "NumberSequenceKeypoint";
        }

        @Override
        public Object get(Interp in, String k) {
            switch (k) {
                case "Time":
                    return Args.n(time);
                case "Value":
                    return value;
                case "Envelope":
                    return Args.n(envelope);
                default:
                    return super.get(in, k);
            }
        }
    }
}
