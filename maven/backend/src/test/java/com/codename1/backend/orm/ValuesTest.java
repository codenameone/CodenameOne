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

import com.codename1.impl.orm.Values;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every encoding an engine can send, against every field type it can be read
 * into, as one table.
 *
 * <p>This exists because the alternative kept failing. The conversions were
 * corrected one representation at a time -- exact text made strict while the
 * Number beside it stayed lenient, every integral narrowing bounded while the
 * float next to it wrapped, ten converters refusing a byte[] while asChar took
 * its first byte -- and each half that was missed came back as its own review
 * finding. The shape of the mistake was always the same: a rule applied to one
 * cell of this table and not to its neighbour.
 *
 * <p>So the table is the test. A cell is a rendered value or `!`, which means
 * the conversion is refused, and a change that makes any cell disagree fails
 * here rather than in somebody's database. Adding a converter or an encoding
 * means adding a column or a row and deciding every cell in it, which is the
 * point.
 *
 * <p>Why these encodings: SQLite has affinities rather than types and hands back
 * whatever was stored, so a Double arrives for an integer column; PostgreSQL and
 * MySQL send NUMERIC and DECIMAL as exact text because a double cannot hold
 * them; PostgreSQL sends its booleans as t and f; a blob is a byte[] everywhere.
 */
class ValuesTest {

    /** What one converter does with one encoding. */
    private interface Conversion {
        Object apply(Object value) throws Exception;
    }

    private static final String[] COLUMNS = {
        "asString", "asLong", "asInt", "asShort", "asByte",
        "asDouble", "asFloat", "asBoolean", "asChar", "asDate", "asBytes", "asCharObject",
    };

    private static final Conversion[] CONVERSIONS = {
        new Conversion() { public Object apply(Object v) throws Exception {
            return Values.asString(v); } },
        new Conversion() { public Object apply(Object v) throws Exception {
            return Long.valueOf(Values.asLong(v, -1)); } },
        new Conversion() { public Object apply(Object v) throws Exception {
            return Integer.valueOf(Values.asInt(v, -1)); } },
        new Conversion() { public Object apply(Object v) throws Exception {
            return Short.valueOf(Values.asShort(v, (short)-1)); } },
        new Conversion() { public Object apply(Object v) throws Exception {
            return Byte.valueOf(Values.asByte(v, (byte)-1)); } },
        new Conversion() { public Object apply(Object v) throws Exception {
            return Double.valueOf(Values.asDouble(v, -1)); } },
        new Conversion() { public Object apply(Object v) throws Exception {
            return Float.valueOf(Values.asFloat(v, -1)); } },
        new Conversion() { public Object apply(Object v) throws Exception {
            return Boolean.valueOf(Values.asBoolean(v, false)); } },
        new Conversion() { public Object apply(Object v) throws Exception {
            return Character.valueOf(Values.asChar(v, '?')); } },
        new Conversion() { public Object apply(Object v) throws Exception {
            Date d = Values.asDate(v); return d == null ? null : Long.valueOf(d.getTime()); } },
        new Conversion() { public Object apply(Object v) throws Exception {
            byte[] b = Values.asBytes(v); return b == null ? null : Integer.valueOf(b.length); } },
        new Conversion() { public Object apply(Object v) throws Exception {
            return Values.asCharObject(v); } },
    };

    @Test
    @DisplayName("every encoding against every field type")
    void theWholeTable() {
        expect(null, "null,-1,-1,-1,-1,-1.0,-1.0,false,?,null,null,null");
        expect(Long.valueOf(42), "42,42,42,42,42,42.0,42.0,true,!,42,!,!");
        expect(Integer.valueOf(42), "42,42,42,42,42,42.0,42.0,true,!,42,!,!");
        // A key at the edge of long: narrowing to int, short or byte refuses
        // rather than wrapping to a different number.
        expect(Long.valueOf(Long.MAX_VALUE),
                "9223372036854775807,9223372036854775807,!,!,!,9.223372036854776E18,"
                        + "9.223372E18,true,!,9223372036854775807,!,!");
        expect(Long.valueOf(2147483648L),
                "2147483648,2147483648,!,!,!,2.147483648E9,2.14748365E9,true,!,2147483648,!,!");
        expect(Long.valueOf(32768L), "32768,32768,32768,!,!,32768.0,32768.0,true,!,32768,!,!");
        expect(Long.valueOf(128L), "128,128,128,128,!,128.0,128.0,true,!,128,!,!");
        // SQLite hands back what it was given: a REAL in an integer column.
        expect(Double.valueOf(12.0), "12.0,12,12,12,12,12.0,12.0,true,!,12,!,!");
        expect(Double.valueOf(12.5), "12.5,!,!,!,!,12.5,12.5,!,!,!,!,!");
        // 2^63 as a double is NOT Long.MAX_VALUE, and a cast clamps it to one,
        // so the integral conversions refuse it. -2^63 IS Long.MIN_VALUE.
        expect(Double.valueOf(9.223372036854776E18),
                "9.223372036854776E18,!,!,!,!,9.223372036854776E18,9.223372E18,!,!,!,!,!");
        expect(Double.valueOf(-9.223372036854776E18),
                "-9.223372036854776E18,-9223372036854775808,!,!,!,-9.223372036854776E18,"
                        + "-9.223372E18,true,!,-9223372036854775808,!,!");
        // Larger than a float holds: narrowing would make it infinite.
        expect(Double.valueOf(1e100), "1.0E100,!,!,!,!,1.0E100,!,!,!,!,!,!");
        // And below what a float holds at the other end: 1e-100 is nonzero and
        // narrows to 0.0f, so the field would carry no value where the row has
        // one. A double reads it exactly, which is why only the float cell moves.
        expect(Double.valueOf(1e-100), "1.0E-100,!,!,!,!,1.0E-100,!,!,!,!,!,!");
        expect(Double.valueOf(Double.NaN), "NaN,!,!,!,!,NaN,NaN,!,!,!,!,!");
        // Already infinite: PostgreSQL stores that, and reproducing it is right.
        expect(Double.valueOf(Double.POSITIVE_INFINITY),
                "Infinity,!,!,!,!,Infinity,Infinity,!,!,!,!,!");
        expect(Float.valueOf(1.5f), "1.5,!,!,!,!,1.5,1.5,!,!,!,!,!");
        expect(Boolean.TRUE, "true,1,1,1,1,!,!,true,!,!,!,!");
        expect(Boolean.FALSE, "false,0,0,0,0,!,!,false,!,!,!,!");
        expect("42", "42,42,42,42,42,42.0,42.0,true,4,42,2,4");
        expect("12.5", "12.5,!,!,!,!,12.5,12.5,!,1,!,4,1");
        // NUMERIC and DECIMAL arrive as exact text; a zero fraction is an integer.
        expect("12.00", "12.00,12,12,12,12,12.0,12.0,true,1,12,5,1");
        // A finite NUMERIC too large for a double parses to Infinity, and too
        // small parses to zero. Reading either into the field would hand the
        // entity a value the row does not hold, so both are refused the way the
        // integral conversions refuse a value that does not survive narrowing.
        expect("1e400", "1e400,!,!,!,!,!,!,!,1,!,5,1");
        expect("1e-400", "1e-400,!,!,!,!,!,!,!,1,!,6,1");
        // Text a double DOES hold and a float does not, so the two disagree.
        expect("1e-100", "1e-100,!,!,!,!,1.0E-100,!,!,1,!,6,1");
        // An infinity the database really stores is spelled, so it still passes.
        expect("Infinity", "Infinity,!,!,!,!,Infinity,Infinity,!,I,!,8,I");

        // Past what a double represents, and it must not go through one.
        expect("9007199254740993.00",
                "9007199254740993.00,9007199254740993,!,!,!,9.007199254740992E15,"
                        + "9.0071993E15,true,9,9007199254740993,19,9");
        expect("92233720368547758080",
                "92233720368547758080,!,!,!,!,9.223372036854776E19,9.223372E19,!,9,!,20,9");
        // PostgreSQL's text booleans.
        expect("t", "t,1,1,1,1,!,!,true,t,1,1,t");
        expect("f", "f,0,0,0,0,!,!,false,f,0,1,f");
        expect("true", "true,1,1,1,1,!,!,true,t,1,4,t");
        expect("false", "false,0,0,0,0,!,!,false,f,0,5,f");
        expect("", ",!,!,!,!,!,!,!,?,!,0,null");
        expect("abc", "abc,!,!,!,!,!,!,!,a,!,3,a");
        // A blob is bytes and text, and nothing else. Its text is three control
        // characters, which is why every cell here is rendered rather than
        // printed.
        expect(new byte[] {1, 2, 3}, "\\x01\\x02\\x03,!,!,!,!,!,!,!,!,!,3,!");
    }

    @Test
    @DisplayName("a primitive field refuses SQL NULL instead of taking a default")
    void nullIntoAPrimitive() throws Exception {
        // The conversions take a fallback, so without this an int field read a
        // null column as 0 -- the same bits as a row that really holds 0, told
        // apart by nobody. The tables this ORM creates declare such a column NOT
        // NULL; this answers for the ones it did not create.
        assertThrows(IOException.class, () -> Values.required(null, "views"));
        // The message names the field, because the row does not.
        try {
            Values.required(null, "views");
            throw new IllegalStateException("should have refused");
        } catch (IOException err) {
            assertTrue(err.getMessage().indexOf("views") >= 0, err.getMessage());
        }
        // Anything that is not null passes straight through, zero included: a
        // column that HOLDS zero is a value, not an absence.
        assertEquals(Long.valueOf(0), Values.required(Long.valueOf(0), "views"));
        assertEquals("", Values.required("", "title"));
    }

    @Test
    @DisplayName("a char is stored as its code unit, not as one-character text")
    void aCharIsANumber() throws Exception {
        // The mapping the generated server-side dao uses. Text looks like the
        // obvious column for a char and is the wrong one: an unassigned char
        // field holds '\0', and PostgreSQL refuses a NUL inside a text value,
        // so an entity nobody touched could not be inserted there at all.
        assertEquals('A', Values.asCodeUnit(Long.valueOf('A'), 'z'));
        assertEquals(Character.valueOf('A'), Values.asCodeUnitObject(Long.valueOf('A')));
        // The two values text cannot carry, which is the reason for the mapping.
        assertEquals('\0', Values.asCodeUnit(Long.valueOf(0), 'z'));
        assertEquals('\ud800', Values.asCodeUnit(Long.valueOf(0xd800), 'z'));
        // The top of the range and one past it. Narrowing would answer a
        // different character rather than say the column does not fit.
        assertEquals('\uffff', Values.asCodeUnit(Long.valueOf(65535), 'z'));
        assertThrows(IOException.class, () -> Values.asCodeUnit(Long.valueOf(65536), 'z'));
        assertThrows(IOException.class, () -> Values.asCodeUnit(Long.valueOf(-1), 'z'));
        // Null takes the fallback, and its boxed twin stays null.
        assertEquals('z', Values.asCodeUnit(null, 'z'));
        assertNull(Values.asCodeUnitObject(null));
        // Text is still read, for a column mapped onto an existing CHAR(1): a
        // number and a string cannot be mistaken for one another.
        assertEquals('A', Values.asCodeUnit("A", 'z'));
        assertEquals(Character.valueOf('A'), Values.asCodeUnitObject("A"));
        // And a blob is neither.
        assertThrows(IOException.class, () -> Values.asCodeUnit(new byte[] {65}, 'z'));
    }

    private static void expect(Object encoding, String row) {
        String[] cells = row.split(",", -1);
        assertEquals(COLUMNS.length, cells.length,
                "the row for " + describe(encoding) + " has " + cells.length
                        + " cells and there are " + COLUMNS.length + " conversions");
        for (int iter = 0; iter < CONVERSIONS.length; iter++) {
            String actual;
            try {
                actual = render(CONVERSIONS[iter].apply(encoding));
            } catch (Exception refused) {
                actual = "!";
            }
            assertEquals(cells[iter], actual, describe(encoding) + " -> " + COLUMNS[iter]);
        }
    }

    /**
     * A result as text, with anything unprintable escaped.
     *
     * <p>asString of a blob is the bytes decoded, which for a test fixture is
     * control characters -- and a control character written into a source file
     * makes it binary to grep and to git. See scripts/check-control-characters.py.
     */
    private static String render(Object value) {
        String text = String.valueOf(value);
        StringBuilder out = new StringBuilder(text.length());
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c < 0x20 || c > 0x7e) {
                out.append("\\x").append(c < 0x10 ? "0" : "").append(Integer.toHexString(c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof byte[]) {
            return "byte[" + ((byte[])value).length + "]";
        }
        return value.getClass().getSimpleName() + "(" + value + ")";
    }
}
