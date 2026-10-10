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
package com.codename1.fxcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.Pair;
import javafx.util.StringConverter;
import javafx.util.converter.BooleanStringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.NumberStringConverter;

public class UtilTest {

    @Test
    public void durationFactoriesAndConversions() {
        assertEquals(1500.0, Duration.seconds(1.5).toMillis(), 0d);
        assertEquals(120000.0, Duration.minutes(2).toMillis(), 0d);
        assertEquals(7200000.0, Duration.hours(2).toMillis(), 0d);
        Duration d = Duration.millis(90000);
        assertEquals(90.0, d.toSeconds(), 0d);
        assertEquals(1.5, d.toMinutes(), 0d);
        assertEquals(0.025, d.toHours(), 1e-12);
        assertSame(Duration.ZERO, Duration.millis(0));
        assertSame(Duration.ONE, Duration.millis(1));
        assertSame(Duration.INDEFINITE, Duration.millis(Double.POSITIVE_INFINITY));
        assertSame(Duration.UNKNOWN, Duration.seconds(Double.NaN));
        assertEquals(250.0, new Duration(250).toMillis(), 0d);
    }

    @Test
    public void durationArithmetic() {
        Duration a = Duration.seconds(3);
        Duration b = Duration.millis(500);
        assertEquals(3500.0, a.add(b).toMillis(), 0d);
        assertEquals(2500.0, a.subtract(b).toMillis(), 0d);
        assertEquals(6000.0, a.multiply(2).toMillis(), 0d);
        assertEquals(1500.0, a.divide(2).toMillis(), 0d);
        assertEquals(-3000.0, a.negate().toMillis(), 0d);
        assertSame(Duration.ZERO, a.subtract(a));
        assertTrue(a.add(Duration.INDEFINITE).isIndefinite());
        assertTrue(a.add(Duration.UNKNOWN).isUnknown());
        assertTrue(Duration.INDEFINITE.subtract(Duration.INDEFINITE).isUnknown());
        assertFalse(a.isIndefinite());
        assertFalse(a.isUnknown());
        assertTrue(a.divide(0).isIndefinite());
    }

    @Test
    public void durationComparison() {
        Duration a = Duration.seconds(1);
        Duration b = Duration.seconds(2);
        assertTrue(a.lessThan(b));
        assertTrue(a.lessThanOrEqualTo(b));
        assertTrue(a.lessThanOrEqualTo(Duration.millis(1000)));
        assertTrue(b.greaterThan(a));
        assertTrue(b.greaterThanOrEqualTo(a));
        assertFalse(a.greaterThan(b));
        assertFalse(a.greaterThan(Duration.UNKNOWN));
        assertFalse(a.lessThan(Duration.UNKNOWN));
        assertTrue(a.lessThan(Duration.INDEFINITE));
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(Duration.millis(1000)));
        assertEquals(a, Duration.millis(1000));
        assertEquals(a.hashCode(), Duration.millis(1000).hashCode());
        assertNotEquals(a, b);
        assertNotEquals(a, "1000.0 ms");
        assertEquals(Duration.UNKNOWN, new Duration(Double.NaN));
    }

    @Test
    public void durationParsing() {
        assertEquals(500.0, Duration.valueOf("500ms").toMillis(), 0d);
        assertEquals(1500.0, Duration.valueOf("1.5s").toMillis(), 0d);
        assertEquals(120000.0, Duration.valueOf("2m").toMillis(), 0d);
        assertEquals(3600000.0, Duration.valueOf("1h").toMillis(), 0d);
        assertEquals(-250.0, Duration.valueOf("-250ms").toMillis(), 0d);
        assertEquals(500.0, Duration.valueOf(".5s").toMillis(), 0d);
        assertEquals("units fold as ASCII", 2000.0, Duration.valueOf("2S").toMillis(), 0d);
        assertEquals(3.0, Duration.valueOf("3MS").toMillis(), 0d);
        assertSame(Duration.ZERO, Duration.valueOf("0s"));
        String[] malformed = {"", "10", "ms", "10 ms", "10x", "1.2.3s", "s10", "10sec", "--1s"};
        for (String text : malformed) {
            try {
                Duration.valueOf(text);
                fail("expected IllegalArgumentException for \"" + text + "\"");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().length() > 0);
            }
        }
        try {
            Duration.valueOf(null);
            fail("expected NullPointerException");
        } catch (NullPointerException expected) {
            assertTrue(true);
        }
    }

    @Test
    public void durationText() {
        assertEquals("1500.0 ms", Duration.seconds(1.5).toString());
        assertEquals("0.0 ms", Duration.ZERO.toString());
        assertEquals("-2.5 ms", Duration.millis(-2.5).toString());
        assertEquals("INDEFINITE", Duration.INDEFINITE.toString());
        assertEquals("UNKNOWN", Duration.UNKNOWN.toString());
    }

    @Test
    public void pair() {
        Pair<String, Integer> pair = new Pair<String, Integer>("k", Integer.valueOf(1));
        assertEquals("k", pair.getKey());
        assertEquals(Integer.valueOf(1), pair.getValue());
        assertEquals("k=1", pair.toString());
        assertEquals(pair, new Pair<String, Integer>("k", Integer.valueOf(1)));
        assertEquals(pair.hashCode(), new Pair<String, Integer>("k", Integer.valueOf(1)).hashCode());
        assertNotEquals(pair, new Pair<String, Integer>("k", Integer.valueOf(2)));
        assertNotEquals(pair, new Pair<String, Integer>("j", Integer.valueOf(1)));
        assertNotEquals(pair, "k=1");
        Pair<String, Integer> nulls = new Pair<String, Integer>(null, null);
        assertEquals(nulls, new Pair<String, Integer>(null, null));
        assertEquals("null=null", nulls.toString());
        assertNotEquals(nulls, pair);
        assertNotEquals(pair, nulls);
    }

    @Test
    public void callback() {
        Callback<String, Integer> length = new Callback<String, Integer>() {
            @Override
            public Integer call(String param) {
                return Integer.valueOf(param.length());
            }
        };
        assertEquals(Integer.valueOf(3), length.call("abc"));
        Callback<Integer, Integer> twice = n -> Integer.valueOf(n.intValue() * 2);
        assertEquals(Integer.valueOf(8), twice.call(Integer.valueOf(4)));
    }

    @Test
    public void simpleConverters() {
        StringConverter<String> text = new DefaultStringConverter();
        assertEquals("a", text.toString("a"));
        assertEquals("", text.toString(null));
        assertEquals("a", text.fromString("a"));
        assertNull(text.fromString(null));

        StringConverter<Boolean> bool = new BooleanStringConverter();
        assertEquals("true", bool.toString(Boolean.TRUE));
        assertEquals("false", bool.toString(Boolean.FALSE));
        assertEquals("", bool.toString(null));
        assertEquals(Boolean.TRUE, bool.fromString(" TRUE "));
        assertEquals(Boolean.TRUE, bool.fromString("true"));
        assertEquals(Boolean.FALSE, bool.fromString("yes"));
        assertNull(bool.fromString("  "));
        assertNull(bool.fromString(null));

        StringConverter<Integer> integer = new IntegerStringConverter();
        assertEquals("42", integer.toString(Integer.valueOf(42)));
        assertEquals("-7", integer.toString(Integer.valueOf(-7)));
        assertEquals("", integer.toString(null));
        assertEquals(Integer.valueOf(42), integer.fromString(" 42 "));
        assertEquals(Integer.valueOf(-7), integer.fromString("-7"));
        assertNull(integer.fromString(""));
        assertNull(integer.fromString(null));
        try {
            integer.fromString("4.2");
            fail("expected NumberFormatException");
        } catch (NumberFormatException expected) {
            assertTrue(true);
        }

        StringConverter<Double> dbl = new DoubleStringConverter();
        assertEquals("1.5", dbl.toString(Double.valueOf(1.5)));
        assertEquals("", dbl.toString(null));
        assertEquals(Double.valueOf(1.5), dbl.fromString(" 1.5 "));
        assertEquals(Double.valueOf(3), dbl.fromString("3"));
        assertNull(dbl.fromString(" "));
        try {
            dbl.fromString("abc");
            fail("expected NumberFormatException");
        } catch (NumberFormatException expected) {
            assertTrue(true);
        }
    }

    @Test
    public void numberConverterFormats() {
        NumberStringConverter c = new NumberStringConverter();
        assertEquals("", c.toString(null));
        assertEquals("0", c.toString(Integer.valueOf(0)));
        assertEquals("42", c.toString(Integer.valueOf(42)));
        assertEquals("1,234", c.toString(Integer.valueOf(1234)));
        assertEquals("-1,234,567", c.toString(Long.valueOf(-1234567L)));
        assertEquals("999", c.toString(Long.valueOf(999)));
        assertEquals("1,000", c.toString(Long.valueOf(1000)));
        assertEquals("-9,223,372,036,854,775,808", c.toString(Long.valueOf(Long.MIN_VALUE)));
        assertEquals("1.5", c.toString(Double.valueOf(1.5)));
        assertEquals("1,234.568", c.toString(Double.valueOf(1234.5678)));
        assertEquals("0.125", c.toString(Double.valueOf(0.125)));
        assertEquals("half rounds to the even digit", "0.062", c.toString(Double.valueOf(0.0625)));
        assertEquals("0.188", c.toString(Double.valueOf(0.1875)));
        assertEquals("2", c.toString(Double.valueOf(1.9999)));
        assertEquals("3", c.toString(Double.valueOf(3.0)));
        assertEquals("-0.5", c.toString(Double.valueOf(-0.5)));
        assertEquals("0", c.toString(Double.valueOf(-0.0001)));
        assertEquals("2.5", c.toString(Float.valueOf(2.5f)));
        assertEquals("NaN", c.toString(Double.valueOf(Double.NaN)));
        assertEquals("10,000,000,000,000,000", c.toString(Double.valueOf(1e16)));
    }

    @Test
    public void numberConverterParses() {
        NumberStringConverter c = new NumberStringConverter();
        assertNull(c.fromString(null));
        assertNull(c.fromString("   "));
        assertEquals(Long.valueOf(42), c.fromString("42"));
        assertEquals(Long.valueOf(1234567), c.fromString(" 1,234,567 "));
        assertEquals(Long.valueOf(-5), c.fromString("-5"));
        assertEquals(Double.valueOf(1.5), c.fromString("1.5"));
        assertEquals(Double.valueOf(1234.25), c.fromString("1,234.25"));
        assertEquals("a whole number is a Long", Long.valueOf(3), c.fromString("3.0"));
        try {
            c.fromString("abc");
            fail("expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().indexOf("abc") >= 0);
        }
        assertEquals(Long.valueOf(1234), c.fromString(c.toString(Integer.valueOf(1234))));
    }
}
