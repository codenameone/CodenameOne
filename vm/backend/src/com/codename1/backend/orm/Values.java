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
package com.codename1.backend.orm;

import java.io.IOException;
import java.util.Date;

/**
 * Turns what an engine sent into what a field holds.
 *
 * <p>{@link com.codename1.backend.Database} promises that rows come back as Long,
 * Double, String, byte[] or null whichever engine answered, and it keeps that
 * promise -- but "a Long" is not the same claim as "the type this field is". The
 * same column is a Long from SQLite and a String from PostgreSQL when it is
 * declared NUMERIC, because arbitrary precision does not fit in a double and the
 * exact text is the only lossless answer; MySQL does the same for DECIMAL. A
 * column somebody else's migration declared BOOLEAN comes back as a Long here and
 * could be "t" from a text-format driver elsewhere.
 *
 * <p>So the conversions are written once, tolerantly, in one place instead of
 * being generated into every entity -- and none of them is a cast whose failure
 * is caught. ParparVM's CHECKCAST is unchecked, so a cast that fails hands the
 * wrong object to the next instruction rather than throwing something a handler
 * could see.
 */
public final class Values {
    private Values() {
    }

    /** The value as text, or null. */
    public static String asString(Object value) {
        if(value == null) {
            return null;
        }
        if(value instanceof String) {
            return (String)value;
        }
        if(value instanceof byte[]) {
            // A BLOB column read into a String field. Decoded as UTF-8 rather
            // than through String.valueOf, which would answer "[B@1f3a".
            try {
                byte[] bytes = (byte[])value;
                return new String(bytes, 0, bytes.length, "UTF-8");
            } catch (IOException err) {
                return null;
            }
        }
        return String.valueOf(value);
    }

    /** The value as a 64-bit integer, or {@code fallback} when it is null. */
    public static long asLong(Object value, long fallback) throws IOException {
        Long out = asLongObject(value);
        return out == null ? fallback : out.longValue();
    }

    /** The value as a 64-bit integer, or null. */
    public static Long asLongObject(Object value) throws IOException {
        if(value == null) {
            return null;
        }
        if(value instanceof Double || value instanceof Float) {
            // SQLITE HAS NO COLUMN TYPES, only affinities: an INTEGER column
            // holds 12.5 if something put one there -- a migration, raw SQL,
            // another client -- and the driver hands it back as a Double.
            // longValue() then truncated it into the entity, while the exact-text
            // branch below refuses the identical "12.5". The same value cannot be
            // corrupted one way and refused the other because of how the engine
            // chose to encode it.
            double exact = ((Number)value).doubleValue();
            if(exact != Math.floor(exact) || Double.isInfinite(exact) || Double.isNaN(exact)) {
                throw new IOException("A column holding " + describe(value)
                        + " has a fractional part and the field it is read into is an "
                        + "integer; the value would have to be rounded to fit");
            }
            // 9.223372036854776E18 IS 2^63, and Long.MAX_VALUE is 2^63 - 1,
            // which no double can represent. So the boundary value compares
            // equal to the literal rather than greater, and the cast below then
            // clamps it to Long.MAX_VALUE: a different number, silently. The
            // upper bound is exclusive for that reason; the lower one is not,
            // because -2^63 IS Long.MIN_VALUE exactly.
            if(exact < -9.223372036854776E18 || exact >= 9.223372036854776E18) {
                throw new IOException("A column holding " + describe(value)
                        + " is outside the range of the integer field it is read into");
            }
            return Long.valueOf((long)exact);
        }
        if(value instanceof Number) {
            return Long.valueOf(((Number)value).longValue());
        }
        if(value instanceof Boolean) {
            return Long.valueOf(((Boolean)value).booleanValue() ? 1L : 0L);
        }
        if(value instanceof String) {
            String text = ((String)value).trim();
            // The spellings a server sends for a boolean when it does not send a
            // number: PostgreSQL's text format is t and f.
            if("t".equals(text) || "true".equalsIgnoreCase(text)) {
                return Long.valueOf(1L);
            }
            if("f".equals(text) || "false".equalsIgnoreCase(text)) {
                return Long.valueOf(0L);
            }
            try {
                return Long.valueOf(Long.parseLong(text));
            } catch (NumberFormatException notAnInteger) {
                // A NUMERIC or DECIMAL column arrives as EXACT TEXT -- that is
                // the whole reason those two engines send it as text rather than
                // as a number -- so "12.00" is an ordinary integer that
                // Long.parseLong refuses for its fraction alone.
                //
                // Parsed exactly rather than through Double.parseDouble, which
                // was the previous fallback and threw away the precision the
                // engine took care to preserve: 9007199254740993.00 fits in a
                // long and came back as ...992, and anything past the double
                // range clamped to Long.MAX_VALUE without a word.
                return Long.valueOf(integralText(text, value));
            }
        }
        throw notANumber(value, "an integer");
    }

    /** The value as a 32-bit integer, or {@code fallback} when it is null. */
    public static int asInt(Object value, int fallback) throws IOException {
        Long out = asLongObject(value);
        return out == null ? fallback : (int)narrowed(out.longValue(), value,
                -2147483648L, 2147483647L, "int");
    }

    /** The value as a boxed 32-bit integer, or null. */
    public static Integer asIntObject(Object value) throws IOException {
        Long out = asLongObject(value);
        return out == null ? null : Integer.valueOf((int)narrowed(out.longValue(), value,
                -2147483648L, 2147483647L, "int"));
    }

    /** The value as a 16-bit integer, or {@code fallback} when it is null. */
    public static short asShort(Object value, short fallback) throws IOException {
        Long out = asLongObject(value);
        return out == null ? fallback : (short)narrowed(out.longValue(), value,
                -32768L, 32767L, "short");
    }

    /** The value as a boxed 16-bit integer, or null. */
    public static Short asShortObject(Object value) throws IOException {
        Long out = asLongObject(value);
        return out == null ? null : Short.valueOf((short)narrowed(out.longValue(), value,
                -32768L, 32767L, "short"));
    }

    /** The value as a byte, or {@code fallback} when it is null. */
    public static byte asByte(Object value, byte fallback) throws IOException {
        Long out = asLongObject(value);
        return out == null ? fallback : (byte)narrowed(out.longValue(), value,
                -128L, 127L, "byte");
    }

    /** The value as a boxed byte, or null. */
    public static Byte asByteObject(Object value) throws IOException {
        Long out = asLongObject(value);
        return out == null ? null : Byte.valueOf((byte)narrowed(out.longValue(), value,
                -128L, 127L, "byte"));
    }

    /**
     * {@code number} when the field it is going into can hold it, or a refusal.
     *
     * <p>A cast would WRAP: a column holding 2147483648 read into an int field
     * became -2147483648, which is a different row's key, a different count, a
     * different answer -- and nothing said so. The columns these fields map to
     * are ordinary integer columns on every engine, and SQLite's are not even
     * range-checked, so a value too large for the field is something a migration
     * or another client can put there any day.
     */
    private static long narrowed(long number, Object value, long min, long max, String field)
            throws IOException {
        if(number < min || number > max) {
            throw new IOException("A column holding " + describe(value)
                    + " is outside the range of the " + field + " field it is read into");
        }
        return number;
    }

    /** The value as a double, or {@code fallback} when it is null. */
    public static double asDouble(Object value, double fallback) throws IOException {
        Double out = asDoubleObject(value);
        return out == null ? fallback : out.doubleValue();
    }

    /** The value as a boxed double, or null. */
    public static Double asDoubleObject(Object value) throws IOException {
        if(value == null) {
            return null;
        }
        if(value instanceof Number) {
            return Double.valueOf(((Number)value).doubleValue());
        }
        if(value instanceof String) {
            try {
                return Double.valueOf(Double.parseDouble(((String)value).trim()));
            } catch (NumberFormatException err) {
                throw notANumber(value, "a number");
            }
        }
        throw notANumber(value, "a number");
    }

    /** The value as a float, or {@code fallback} when it is null. */
    public static float asFloat(Object value, float fallback) throws IOException {
        Double out = asDoubleObject(value);
        return out == null ? fallback : narrowedToFloat(out.doubleValue(), value);
    }

    /** The value as a boxed float, or null. */
    public static Float asFloatObject(Object value) throws IOException {
        Double out = asDoubleObject(value);
        return out == null ? null : Float.valueOf(narrowedToFloat(out.doubleValue(), value));
    }

    /**
     * {@code number} as a float, when a float can hold it.
     *
     * <p>A float field's column is DOUBLE on MySQL and DOUBLE PRECISION on
     * PostgreSQL, both of which hold numbers a float cannot: 1e100 narrowed to
     * INFINITY, so the entity held something the row does not and writing it
     * back would store that or fail. The integral converters were given this
     * check a round ago and this one was missed.
     *
     * <p>A value that is ALREADY infinite or NaN passes through: PostgreSQL can
     * store either, and reproducing what the row holds is right. What is refused
     * is a finite number becoming an infinite one.
     */
    private static float narrowedToFloat(double number, Object value) throws IOException {
        if(!Double.isNaN(number) && !Double.isInfinite(number)
                && (number > 3.4028234663852886E38 || number < -3.4028234663852886E38)) {
            throw new IOException("A column holding " + describe(value)
                    + " is outside the range of the float field it is read into");
        }
        return (float)number;
    }

    /** The value as a flag: anything non-zero, or the text of one, is true. */
    public static boolean asBoolean(Object value, boolean fallback) throws IOException {
        Boolean out = asBooleanObject(value);
        return out == null ? fallback : out.booleanValue();
    }

    /** The value as a boxed flag, or null. */
    public static Boolean asBooleanObject(Object value) throws IOException {
        if(value == null) {
            return null;
        }
        if(value instanceof Boolean) {
            return (Boolean)value;
        }
        Long number = asLongObject(value);
        return number == null ? null : Boolean.valueOf(number.longValue() != 0);
    }

    /**
     * The first character of the value, or {@code fallback} when it is null or
     * empty.
     *
     * <p>TEXT ONLY. An entity's char field is stored in a text column, so a
     * number or a blob arriving here means the table and the entity disagree --
     * and taking the first character of however that value happens to print is
     * not a reading of it: a byte[] gave whatever its first byte decoded to, a
     * Long gave the first DIGIT, and NaN gave 'N'. Every other converter in this
     * class refuses an encoding it cannot mean; this one used to accept them all.
     */
    public static char asChar(Object value, char fallback) throws IOException {
        if(value == null) {
            return fallback;
        }
        if(!(value instanceof String)) {
            throw new IOException("A column holding " + describe(value)
                    + " cannot be read as a character; the field expects text");
        }
        String text = (String)value;
        return text.length() == 0 ? fallback : text.charAt(0);
    }

    /**
     * The value as a moment in time, or null.
     *
     * <p>An entity stores a Date as epoch MILLISECONDS in an integer column, on
     * every engine, which is what makes it the same value everywhere: a native
     * timestamp comes back as text whose format follows the server's DateStyle
     * and session time zone. A number is therefore read as milliseconds; text
     * that is a number is read the same way, and anything else is refused rather
     * than guessed at.
     */
    public static Date asDate(Object value) throws IOException {
        if(value instanceof Boolean) {
            // A flag is not a moment. asLongObject reads one as 0 or 1, which
            // would make "true" the first millisecond of 1970 rather than an
            // error, and every other converter refuses the encodings it cannot
            // mean.
            throw new IOException("A column holding " + describe(value)
                    + " cannot be read as a date");
        }
        Long millis = asLongObject(value);
        return millis == null ? null : new Date(millis.longValue());
    }

    /** The value as bytes, or null. Text is encoded as UTF-8. */
    public static byte[] asBytes(Object value) throws IOException {
        if(value == null) {
            return null;
        }
        if(value instanceof byte[]) {
            return (byte[])value;
        }
        if(value instanceof String) {
            return ((String)value).getBytes("UTF-8");
        }
        throw new IOException("A column holding " + describe(value)
                + " cannot be read as bytes");
    }

    /**
     * {@code text} as a long, where it is an integer written with a fraction of
     * zeros -- "12", "12.", "12.000".
     *
     * <p>A fraction that is NOT zero is refused rather than truncated. The field
     * is an integer and the column is not, which is a disagreement between the
     * entity and the table; rounding it silently is how the wrong number ends up
     * stored back. Out of range is refused for the same reason: a clamp to
     * Long.MAX_VALUE is a value nobody wrote.
     */
    private static long integralText(String text, Object value) throws IOException {
        int dot = text.indexOf('.');
        if(dot < 0) {
            throw notANumber(value, "an integer");
        }
        for(int iter = dot + 1 ; iter < text.length() ; iter++) {
            if(text.charAt(iter) != '0') {
                throw new IOException("A column holding " + describe(value)
                        + " has a fractional part and the field it is read into is an "
                        + "integer; the value would have to be rounded to fit");
            }
        }
        String whole = text.substring(0, dot);
        if(whole.length() == 0 || "+".equals(whole) || "-".equals(whole)) {
            whole = whole + "0";
        }
        try {
            return Long.parseLong(whole);
        } catch (NumberFormatException err) {
            throw new IOException("A column holding " + describe(value)
                    + " is outside the range of the integer field it is read into");
        }
    }

    private static IOException notANumber(Object value, String wanted) {
        return new IOException("A column holding " + describe(value)
                + " cannot be read as " + wanted);
    }

    /** What a value is, for a message, without pasting a password into a log. */
    private static String describe(Object value) {
        if(value == null) {
            return "null";
        }
        if(value instanceof String) {
            String text = (String)value;
            // Truncated: the value may be a row somebody stored, and an error
            // message is a log line.
            return "the text '" + (text.length() > 32 ? text.substring(0, 32) + "..." : text)
                    + "'";
        }
        if(value instanceof byte[]) {
            return ((byte[])value).length + " bytes";
        }
        return "a " + value.getClass().getName();
    }
}
