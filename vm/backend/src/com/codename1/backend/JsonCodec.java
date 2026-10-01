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
package com.codename1.backend;

import java.util.Date;

/// What the JSON codecs the build generates call: typed reads that refuse a
/// value with a message naming where it was, and the date form.
///
/// A controller that returns or accepts one of the application's own classes --
/// an entity, a DTO -- gets a codec written for that class at build time, the way
/// Jackson would map it at run time: its fields by name, `@JsonProperty` renaming
/// one and `@JsonIgnore` leaving one out. The codec calls these methods, so
/// nothing here is looked up by name or reflected on, and a server with no such
/// route never links this class.
///
/// A refused value is an `IllegalArgumentException` whose message says where in
/// the body it was and what was expected -- `$.items[2].due: expected a number or
/// an ISO-8601 date, got true` -- which the generated router answers with 400.
public final class JsonCodec {
    /// How deep a codec follows one object into another before it refuses: past
    /// any real document, and short of the stack a cycle between two objects
    /// would otherwise exhaust.
    public static final int MAX_DEPTH = 64;

    private JsonCodec() {
    }

    /// Where a value sits in a body. Built one level per nested object or array
    /// the codec enters, and turned into text only for a message.
    public static final class Path {
        /// The document itself, `$`.
        public static final Path ROOT = new Path(null, null, -1);

        private final Path parent;
        private final String name;
        private final int index;

        private Path(Path parent, String name, int index) {
            this.parent = parent;
            this.name = name;
            this.index = index;
        }

        /// The member `name` of this object.
        public Path child(String name) {
            return new Path(this, name, -1);
        }

        /// The element `index` of this array.
        public Path child(int index) {
            return new Path(this, null, index);
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder();
            append(out);
            return out.toString();
        }

        private void append(StringBuilder out) {
            if (parent == null) {
                out.append('$');
                return;
            }
            parent.append(out);
            segment(out, name, index);
        }
    }

    /// The path of member `name` or element `index` of `at`, or `at` itself when
    /// neither is given -- where a codec is when it starts reading an object or
    /// an array.
    public static Path enter(Path at, String name, int index) {
        if (name != null) {
            return at.child(name);
        }
        return index >= 0 ? at.child(index) : at;
    }

    private static void segment(StringBuilder out, String name, int index) {
        if (name != null) {
            out.append('.').append(name);
        } else if (index >= 0) {
            out.append('[').append(index).append(']');
        }
    }

    /// The refusal of `got` at member `name` or element `index` of `at`; pass
    /// null and -1 for `at` itself.
    public static IllegalArgumentException mismatch(Path at, String name, int index,
                                                    String expected, Object got) {
        StringBuilder out = new StringBuilder();
        at.append(out);
        segment(out, name, index);
        out.append(": expected ").append(expected).append(", got ").append(describe(got));
        return new IllegalArgumentException(out.toString());
    }

    /// Refuses an object nested past [#MAX_DEPTH], which in a response means a
    /// cycle: two objects that reach each other through their fields.
    public static IllegalStateException tooDeep(String type) {
        return new IllegalStateException("A " + type + " nests more than " + MAX_DEPTH
                + " objects deep while being written as JSON, which is a cycle between "
                + "objects that refer to each other. Mark the field that points back "
                + "with @JsonIgnore.");
    }

    private static String describe(Object got) {
        if (got == null) {
            return "null";
        }
        if (got instanceof String) {
            return "a string";
        }
        if (got instanceof Boolean) {
            return String.valueOf(got);
        }
        if (got instanceof Number) {
            return "the number " + got;
        }
        if (got instanceof java.util.Map) {
            return "an object";
        }
        if (got instanceof java.util.List) {
            return "an array";
        }
        return "a value";
    }

    /// A whole number between `min` and `max`. A number with a fraction is
    /// refused rather than cut short.
    public static long readLong(Object json, Path at, String name, int index,
                                long min, long max) {
        if (json instanceof Long) {
            long v = ((Long) json).longValue();
            if (v >= min && v <= max) {
                return v;
            }
        } else if (json instanceof Double) {
            double d = ((Double) json).doubleValue();
            // Bounded by 2^63 BEFORE the conversion, and compared as a long after
            // it. As a double, Long.MAX_VALUE rounds up to 2^63, so a direct
            // "d <= max" passed 9223372036854775808.0 and the cast clamped it.
            if (d == Math.floor(d) && d >= -9.223372036854775808E18
                    && d < 9.223372036854775808E18) {
                long v = (long) d;
                if (v >= min && v <= max) {
                    return v;
                }
            }
        }
        throw mismatch(at, name, index, "a whole number from " + min + " to " + max, json);
    }

    /// Any number.
    public static double readDouble(Object json, Path at, String name, int index) {
        if (json instanceof Number) {
            return ((Number) json).doubleValue();
        }
        throw mismatch(at, name, index, "a number", json);
    }

    /// A number a float can hold. One past Float.MAX_VALUE would become
    /// infinity and one below the smallest float would become zero -- a
    /// different number from the one sent -- so both are refused, as an MCP
    /// tool's float argument is.
    public static float readFloat(Object json, Path at, String name, int index) {
        double d = readDouble(json, at, name, index);
        float f = (float) d;
        if (Double.isNaN(d) || Double.isInfinite(d) || Math.abs(d) > Float.MAX_VALUE
                || (f == 0 && d != 0)) {
            throw mismatch(at, name, index, "a number within the range of a float", json);
        }
        return f;
    }

    /// `true` or `false`.
    public static boolean readBoolean(Object json, Path at, String name, int index) {
        if (json instanceof Boolean) {
            return ((Boolean) json).booleanValue();
        }
        throw mismatch(at, name, index, "true or false", json);
    }

    /// A string of one character.
    public static char readChar(Object json, Path at, String name, int index) {
        if (json instanceof String && ((String) json).length() == 1) {
            return ((String) json).charAt(0);
        }
        throw mismatch(at, name, index, "a one-character string", json);
    }

    /// A string, or null.
    public static String readString(Object json, Path at, String name, int index) {
        if (json == null || json instanceof String) {
            return (String) json;
        }
        throw mismatch(at, name, index, "a string", json);
    }

    /// Base64 text, or null.
    public static byte[] readBytes(Object json, Path at, String name, int index) {
        if (json == null) {
            return null;
        }
        if (json instanceof String) {
            byte[] out = Base64.decode((String) json);
            if (out != null) {
                return out;
            }
        }
        throw mismatch(at, name, index, "base64 text", json);
    }

    /// Milliseconds since the epoch -- the form [#writeDate] writes and the
    /// app's mapper reads -- or an ISO-8601 date, with or without a time and an
    /// offset; one without an offset is UTC. Null stays null.
    public static Date readDate(Object json, Path at, String name, int index) {
        if (json == null) {
            return null;
        }
        if (json instanceof Long) {
            return new Date(((Long) json).longValue());
        }
        if (json instanceof String) {
            long millis = parseIso8601((String) json);
            if (millis != Long.MIN_VALUE) {
                return new Date(millis);
            }
        }
        throw mismatch(at, name, index, "a number or an ISO-8601 date", json);
    }

    /// A date as milliseconds since the epoch, as Jackson writes one by default
    /// and as the app's generated mapper reads it back.
    public static void writeDate(Date value, ByteSink out) {
        if (value == null) {
            out.putAscii("null");
        } else {
            out.putNumber(value.getTime());
        }
    }

    /// Milliseconds since the epoch, or Long.MIN_VALUE when `s` isn't one of
    /// `yyyy-MM-dd`, `yyyy-MM-ddTHH:mm`, `yyyy-MM-ddTHH:mm:ss` and the last with
    /// a fraction, each optionally followed by `Z` or an offset such as
    /// `+02:00` or `+0200`.
    static long parseIso8601(String s) {
        int n = s.length();
        if (n < 10 || s.charAt(4) != '-' || s.charAt(7) != '-') {
            return Long.MIN_VALUE;
        }
        int year = digits(s, 0, 4);
        int month = digits(s, 5, 2);
        int day = digits(s, 8, 2);
        if (year < 0 || month < 1 || month > 12 || day < 1 || day > daysIn(year, month)) {
            return Long.MIN_VALUE;
        }
        int hour = 0;
        int minute = 0;
        int second = 0;
        int millis = 0;
        int pos = 10;
        if (pos < n && (s.charAt(pos) == 'T' || s.charAt(pos) == 't' || s.charAt(pos) == ' ')) {
            if (pos + 6 > n || s.charAt(pos + 3) != ':') {
                return Long.MIN_VALUE;
            }
            hour = digits(s, pos + 1, 2);
            minute = digits(s, pos + 4, 2);
            pos += 6;
            if (pos < n && s.charAt(pos) == ':') {
                second = digits(s, pos + 1, 2);
                pos += 3;
                if (pos < n && s.charAt(pos) == '.') {
                    pos++;
                    int start = pos;
                    int scale = 100;
                    while (pos < n && s.charAt(pos) >= '0' && s.charAt(pos) <= '9') {
                        millis += (s.charAt(pos) - '0') * scale;
                        scale /= 10;
                        pos++;
                    }
                    if (pos == start) {
                        return Long.MIN_VALUE;
                    }
                }
            }
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0
                    || second > 59) {
                return Long.MIN_VALUE;
            }
        }
        int offsetMinutes = 0;
        if (pos < n) {
            char c = s.charAt(pos);
            if ((c == 'Z' || c == 'z') && pos + 1 == n) {
                offsetMinutes = 0;
            } else if (c == '+' || c == '-') {
                int oh = digits(s, pos + 1, 2);
                int om;
                if (pos + 6 == n && s.charAt(pos + 3) == ':') {
                    om = digits(s, pos + 4, 2);
                } else if (pos + 5 == n) {
                    om = digits(s, pos + 3, 2);
                } else if (pos + 3 == n) {
                    om = 0;
                } else {
                    return Long.MIN_VALUE;
                }
                if (oh < 0 || oh > 23 || om < 0 || om > 59) {
                    return Long.MIN_VALUE;
                }
                offsetMinutes = (c == '+' ? 1 : -1) * (oh * 60 + om);
            } else {
                return Long.MIN_VALUE;
            }
        }
        long days = daysFromCivil(year, month, day);
        return ((days * 24 + hour) * 60 + minute - offsetMinutes) * 60000L
                + second * 1000L + millis;
    }

    /// The decimal value of `count` digits at `from`, or -1.
    private static int digits(String s, int from, int count) {
        if (from < 0 || from + count > s.length()) {
            return -1;
        }
        int v = 0;
        for (int iter = from ; iter < from + count ; iter++) {
            char c = s.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }

    private static int daysIn(int year, int month) {
        if (month == 2) {
            boolean leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
            return leap ? 29 : 28;
        }
        return month == 4 || month == 6 || month == 9 || month == 11 ? 30 : 31;
    }

    /// Days from 1970-01-01 to the given proleptic Gregorian date; Howard
    /// Hinnant's algorithm, which needs no calendar class.
    private static long daysFromCivil(int year, int month, int day) {
        int y = month <= 2 ? year - 1 : year;
        int era = (y >= 0 ? y : y - 399) / 400;
        int yoe = y - era * 400;
        int doy = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
        int doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097L + doe - 719468;
    }
}
