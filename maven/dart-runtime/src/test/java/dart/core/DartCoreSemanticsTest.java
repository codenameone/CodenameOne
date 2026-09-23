/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package dart.core;

import dart.math.DartMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// dart:core and dart:math behaviours that transpiled applications rely on and
/// that each once diverged from Dart.
public class DartCoreSemanticsTest {

    // --- DateTime normalizes out-of-range components --------------------------

    @Test
    public void dayZeroIsTheLastDayOfThePreviousMonth() {
        DateTime d = new DateTime(2024, 3, 0, 0, 0, 0, 0, 0);
        assertEquals(2, d.month());
        assertEquals(29, d.day(), "2024 is a leap year");
    }

    @Test
    public void monthZeroIsThePreviousDecember() {
        DateTime d = new DateTime(2024, 0, 15, 0, 0, 0, 0, 0);
        assertEquals(2023, d.year());
        assertEquals(12, d.month());
        assertEquals(15, d.day());
    }

    @Test
    public void lastDayOfMonthIdiom() {
        // DateTime(y, m + 1, 0) -- the usual way Dart code asks for a month's length.
        assertEquals(30, new DateTime(2023, 12, 0, 0, 0, 0, 0, 0).day());
        assertEquals(31, new DateTime(2024, 1, 0, 0, 0, 0, 0, 0).day());
    }

    // --- Uri query components decode '+' as a space; path segments do not -----

    @Test
    public void plusInAQueryIsASpace() {
        DartUri u = DartUri.parse("https://example.com/search?q=hello+world&x=a%2Bb");
        assertEquals("hello world", u.queryParameters().get("q"));
        assertEquals("a+b", u.queryParameters().get("x"), "an ENCODED plus stays a plus");
    }

    @Test
    public void plusInAPathSegmentIsLiteral() {
        DartUri u = DartUri.parse("https://example.com/c++/notes");
        assertEquals("c++", u.pathSegments().get(0));
    }

    // --- List.addAll of itself ------------------------------------------------

    @Test
    public void addAllOfItselfDuplicatesTheOriginal() {
        DartList<Object> l = new DartList<Object>();
        l.add("a");
        l.add("b");
        l.addAllIterable(l);
        assertEquals(4, l.size());
        assertEquals("a", l.get(2));
        assertEquals("b", l.get(3));
    }

    // --- Random.nextInt accepts Dart's whole range ----------------------------

    @Test
    public void nextIntAcceptsTheFullUnsigned32BitBound() {
        DartMath.DartRandom r = new DartMath.DartRandom(42);
        long max = 1L << 32;
        for (int i = 0; i < 2000; i++) {
            long v = r.nextInt(max);
            assertTrue(v >= 0 && v < max, "out of range: " + v);
        }
        long big = (1L << 31) + 7;
        for (int i = 0; i < 2000; i++) {
            long v = r.nextInt(big);
            assertTrue(v >= 0 && v < big, "out of range: " + v);
        }
    }

    @Test
    public void nextIntReachesAboveTheSignedIntRange() {
        DartMath.DartRandom r = new DartMath.DartRandom(7);
        boolean sawHigh = false;
        for (int i = 0; i < 200 && !sawHigh; i++) {
            sawHigh = r.nextInt(1L << 32) > Integer.MAX_VALUE;
        }
        assertTrue(sawHigh, "values above 2^31 must be produced, not only the low half");
    }

    @Test
    public void nextIntRejectsBoundsDartRejects() {
        DartMath.DartRandom r = new DartMath.DartRandom(1);
        assertThrows(RangeError.class, () -> r.nextInt(0));
        assertThrows(RangeError.class, () -> r.nextInt((1L << 32) + 1));
    }

    // --- DateTime.toString is Dart's format ---------------------------------

    @Test
    public void dateTimePrintsDartsFormat() {
        assertEquals("2024-03-05 07:08:09.010Z",
                DateTime.utc(2024, 3, 5, 7, 8, 9, 10, 0).toString());
        assertEquals("2024-03-05 07:08:09.010",
                new DateTime(2024, 3, 5, 7, 8, 9, 10, 0).toString(),
                "local time, no Z");
    }

    // --- Uri with an IPv6 literal ---------------------------------------------

    @Test
    public void bracketedIpv6AuthorityKeepsHostAndPort() {
        DartUri u = DartUri.parse("http://[::1]:8080/path");
        assertEquals("::1", u.host());
        assertEquals(8080, u.port());
        assertEquals("/path", u.path());
        DartUri noPort = DartUri.parse("http://[2001:db8::7]/x");
        assertEquals("2001:db8::7", noPort.host());
    }

    // --- RegExp.allMatches of an empty pattern --------------------------------

    @Test
    public void anEmptyMatchIsReportedOnce() {
        int count = 0;
        for (RegExpMatch m : new RegExp("$").allMatches("abc")) {
            count++;
            assertEquals(3, m.start());
        }
        assertEquals(1, count, "the end-of-input match must not be found twice");
    }

    @Test
    public void portDefaultsToTheSchemes() {
        assertEquals(443, DartUri.parse("https://example.com/path").port());
        assertEquals(80, DartUri.parse("http://example.com/").port());
        assertEquals(8443, DartUri.parse("https://example.com:8443/").port(), "explicit wins");
        assertEquals(0, DartUri.parse("ftp://example.com/").port());
    }

    @Test
    public void aLocalValueAndItsUtcFormAreNotEqual() {
        DateTime local = new DateTime(2024, 3, 5, 7, 8, 9, 10, 0);
        DateTime utc = local.toUtc();
        assertTrue(local.isAtSameMomentAs(utc), "the same instant");
        assertFalse(local.equals(utc), "but not equal: the time-zone mode differs");
        assertTrue(local.equals(utc.toLocal()));
    }
}
