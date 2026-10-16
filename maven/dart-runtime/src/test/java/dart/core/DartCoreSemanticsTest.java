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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    // --- DateTime rejects a huge component instead of wrapping it into a --------
    // --- plausible-looking epoch -------------------------------------------------

    @Test
    public void hugeYearThrowsInsteadOfWrappingIntoAPlausibleDate() {
        // Recorded regression: build()'s arithmetic used to be plain long
        // add/multiply, so this overflowed (most sharply inside
        // daysFromCivil's era * 146097) and silently wrapped into some
        // value millisToMicros' range check accepted as a normal date,
        // instead of failing the way Dart itself does.
        assertThrows(ArgumentError.class,
                () -> DateTime.utc(Long.MAX_VALUE, 1, 1, 0, 0, 0, 0, 0));
        assertThrows(ArgumentError.class,
                () -> DateTime.utc(Long.MIN_VALUE, 1, 1, 0, 0, 0, 0, 0));
        assertThrows(ArgumentError.class,
                () -> new DateTime(Long.MAX_VALUE, 1, 1, 0, 0, 0, 0, 0));
    }

    @Test
    public void hugeNonYearComponentsAlsoThrowInsteadOfWrapping() {
        assertThrows(ArgumentError.class,
                () -> DateTime.utc(2024, 1, Long.MAX_VALUE, 0, 0, 0, 0, 0),
                "a huge day must not silently overflow through the *DAY_MS multiply");
        assertThrows(ArgumentError.class,
                () -> DateTime.utc(2024, 1, 1, Long.MAX_VALUE, 0, 0, 0, 0),
                "a huge hour must not silently overflow");
    }

    @Test
    public void ordinaryDatesAreUnaffectedByTheOverflowCheck() {
        // The fix must not touch Dart's own out-of-range calendar-arithmetic
        // idiom (month 0, day 0, a day offset of thousands) or any ordinary
        // date.
        DateTime d = DateTime.utc(2024, 3, 1, 0, 0, 0, 0, 0);
        assertEquals(2024, d.year());
        assertEquals(3, d.month());
        assertEquals(1, d.day());

        // 1970-01-01 plus a 10,000-day offset through the `day` component
        // (a large but entirely sane calendar-arithmetic use) lands in 1997.
        DateTime offset = DateTime.utc(1970, 1, 1 + 10000, 0, 0, 0, 0, 0);
        assertEquals(1997, offset.year());
    }

    // --- DateTime's LOCAL constructor normalizes an overflowing component -----
    // --- instead of narrowing it to int ----------------------------------------

    @Test
    public void localConstructorNormalizesAnOverflowingHourInsteadOfWrapping() {
        // Regression: calendarBuild() used to narrow each raw long component to
        // int with a plain cast before handing it to Calendar.set(). 2_200_000_000
        // does not fit in an int, so the cast used to wrap it into a small, wrong
        // value (a different date entirely) instead of Dart's own normalization,
        // which carries the overflow into day/month/year -- landing here about
        // 91.7 million days after the epoch. Expected values are `dart run`'s own
        // output for the identical local DateTime() calls (Dart SDK 3.9.3); the
        // zone is pinned here so the comparison does not depend on the runner's.
        java.util.TimeZone saved = java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Jerusalem"));

            DateTime overflowHour = new DateTime(1970, 1, 1, 2_200_000_000L, 0, 0, 0, 0);
            assertEquals(252944, overflowHour.year());
            assertEquals(10, overflowHour.month());
            assertEquals(22, overflowHour.day());
            assertEquals(16, overflowHour.hour());
            assertEquals(7919999989200000L, overflowHour.millisecondsSinceEpoch());

            // The negative side lands in the deep past (year < 0), which takes
            // build()'s earlier, Calendar-free "ancient" branch rather than
            // calendarBuild -- covered here too since both branches used to
            // share the same (int) narrowing of the raw hour.
            DateTime negativeOverflowHour = new DateTime(1970, 1, 1, -2_200_000_000L, 0, 0, 0, 0);
            assertEquals(-7920000007200000L, negativeOverflowHour.millisecondsSinceEpoch());
        } finally {
            java.util.TimeZone.setDefault(saved);
        }
    }

    @Test
    public void localConstructorNormalizesMonthAndDayOverflowThroughTheCalendarBranch() {
        // Same narrowing bug, through month and day this time, each landing a
        // modern-enough date that build() takes the Calendar (DST-aware) branch
        // calendarBuild() rather than the ancient/raw-offset one above.
        java.util.TimeZone saved = java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Jerusalem"));

            // Month 25 of 2024 is January of 2026.
            DateTime monthOverflow = new DateTime(2024, 25, 1, 0, 0, 0, 0, 0);
            assertEquals(2026, monthOverflow.year());
            assertEquals(1, monthOverflow.month());
            assertEquals(1, monthOverflow.day());
            assertEquals(1767218400000L, monthOverflow.millisecondsSinceEpoch());

            // Day 0 of March is the last day of February.
            DateTime dayZero = new DateTime(2024, 3, 0, 0, 0, 0, 0, 0);
            assertEquals(2024, dayZero.year());
            assertEquals(2, dayZero.month());
            assertEquals(29, dayZero.day(), "2024 is a leap year");
            assertEquals(1709157600000L, dayZero.millisecondsSinceEpoch());

            // Day -5 of March is five days before the last day of February.
            DateTime negativeDay = new DateTime(2024, 3, -5, 0, 0, 0, 0, 0);
            assertEquals(2024, negativeDay.year());
            assertEquals(2, negativeDay.month());
            assertEquals(24, negativeDay.day());
            assertEquals(1708725600000L, negativeDay.millisecondsSinceEpoch());
        } finally {
            java.util.TimeZone.setDefault(saved);
        }
    }

    // --- Uri query components decode '+' as a space; path segments do not -----

    /// Dart normalizes a registered-name host: unreserved escapes decoded, other
    /// escapes' hex upper-cased, ASCII lower-cased. The authority was copied verbatim,
    /// so equivalent spellings had different hosts and compared unequal.
    @Test
    public void aUriHostIsNormalizedLikeDarts() {
        DartUri u = DartUri.parse("https://%65XAMPLE.com:8080/p");
        assertEquals("example.com", u.host());
        assertEquals("https://example.com:8080/p", u.toString());
        assertEquals(DartUri.parse("https://example.com:8080/p"), u);
        assertEquals("a%2Fb.com", DartUri.parse("http://A%2fB.com/").host(), "escape hex upper-case");
        assertEquals("http://user:PW@example.com/", DartUri.parse("http://user:PW@EXAMPLE.com/").toString(),
                "user info is not lower-cased");
        assertEquals("file:///a/b", DartUri.parse("file:///a/b").toString());
        assertEquals("http://[::1]:8080/", DartUri.parse("http://[::1]:8080/").toString());
    }

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

    @Test
    public void aBracketedHostMustBeAnIpLiteral() {
        // Expected values recorded from the Dart SDK (3.9).
        assertNull(DartUri.tryParse("http://[not-an-ip]/"));
        assertNull(DartUri.tryParse("http://[1::2::3]/"));
        assertNull(DartUri.tryParse("http://[1:2:3:4:5:6:7::]/"), "the wildcard counts as a part");
        assertNull(DartUri.tryParse("http://[12345::]/"));
        assertNull(DartUri.tryParse("http://[::256.1.1.1]/"));
        assertNull(DartUri.tryParse("http://[]/"));
        assertNull(DartUri.tryParse("http://[::1%25a%zz]/"));
        assertNull(DartUri.tryParse("http://[v1.]/"));
        assertThrows(FormatException.class, () -> DartUri.parse("http://[bad]/"));
        assertEquals("::ffff:192.168.1.1", DartUri.parse("http://[::ffff:192.168.1.1]/").host());
        assertEquals("fe80::1%25en0", DartUri.parse("http://[FE80::1%en0]:80/").host());
        assertEquals("::1%25Eth0", DartUri.parse("http://[::1%25Eth%30]/").host());
        assertEquals("[vA.x]", DartUri.parse("http://[vA.x]/").host());
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
    public void allMatchesSearchesOnlyAsFarAsTheIterationGoes() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            sb.append('a');
        }
        final int[] searches = new int[1];
        RegExp.searchHook = new Runnable() {
            @Override
            public void run() {
                searches[0]++;
            }
        };
        try {
            DartIterable<RegExpMatch> all = new RegExp("a").allMatches(sb.toString());
            assertEquals(0, searches[0], "creating the iterable searches nothing");
            int seen = 0;
            for (RegExpMatch m : all.take(1)) {
                seen++;
                assertEquals(0, m.start());
                assertEquals(1, m.end());
            }
            assertEquals(1, seen);
            assertEquals(1, searches[0], "take(1) is one search, not one per match");
            assertEquals(5, new RegExp("a").allMatches("aaaaa").length());
        } finally {
            RegExp.searchHook = null;
        }
    }

    @Test
    public void allMatchesIteratesAfreshAndKeepsItsOrder() {
        DartIterable<RegExpMatch> all = new RegExp("[0-9]+").allMatches("a1 b22 c333");
        StringBuilder first = new StringBuilder();
        for (RegExpMatch m : all) {
            first.append(m.group(0)).append(',');
        }
        StringBuilder second = new StringBuilder();
        for (RegExpMatch m : all) {
            second.append(m.group(0)).append(',');
        }
        assertEquals("1,22,333,", first.toString());
        assertEquals(first.toString(), second.toString());
        assertFalse(new RegExp("x").allMatches("abc").iterator().hasNext());
        assertFalse(new RegExp("x").allMatches(null).iterator().hasNext());
    }

    @Test
    public void allMatchesTakesAnOptionalStartAndKeepsAbsoluteOffsets() {
        StringBuilder found = new StringBuilder();
        for (RegExpMatch m : new RegExp("[0-9]+").allMatches("a1 b22 c333", 1)) {
            found.append(m.start()).append(':').append(m.group(0)).append(',');
        }
        // Starting the search AT offset 1 (inside "1") must not shift the reported
        // offsets: the match found is still "1" at absolute offset 1, exactly as with
        // no start argument, not renumbered as if the search began at a new offset 0.
        assertEquals("1:1,4:22,8:333,", found.toString());
        // start == input.length() is valid -- an empty remaining range to search --
        // recorded from the real Dart SDK, same as sublist/getRange's own end == length.
        assertEquals(0, new RegExp("a").allMatches("abc", 3).length());
        assertThrows(RangeError.class, () -> new RegExp("a").allMatches("abc", -1));
        assertThrows(RangeError.class, () -> new RegExp("a").allMatches("abc", 4));
    }

    @Test
    public void portDefaultsToTheSchemes() {
        assertEquals(443, DartUri.parse("https://example.com/path").port());
        assertEquals(80, DartUri.parse("http://example.com/").port());
        assertEquals(8443, DartUri.parse("https://example.com:8443/").port(), "explicit wins");
        assertEquals(0, DartUri.parse("ftp://example.com/").port());
    }

    @Test
    public void aDefaultPortIsDroppedAsDartDrops() {
        // Expected values are what Dart 3.9's Uri.parse prints.
        assertEquals("http://a.com/x", DartUri.parse("http://a.com:80/x").toString());
        assertEquals("https://a.com", DartUri.parse("https://a.com:443").toString());
        assertEquals("https://u@a.com/p?q#f", DartUri.parse("https://u@a.com:443/p?q#f").toString());
        assertEquals("http://a.com", DartUri.parse("HTTP://A.com:80").toString());
        assertEquals("http://a.com", DartUri.parse("http://a.com:080").toString());
        assertEquals("http://a.com:81", DartUri.parse("http://a.com:081").toString());
        assertEquals("http://a.com/p", DartUri.parse("http://a.com:/p").toString());
        assertEquals("http://[::1]/", DartUri.parse("http://[::1]:80/").toString());
        assertEquals("foo://a.com", DartUri.parse("foo://a.com:0").toString(), "0 is every other scheme's default");
        // Only http and https have a non-zero default; ws keeps its 80, and so does
        // a URI with no scheme at all.
        assertEquals("https://a.com:80", DartUri.parse("https://a.com:80").toString());
        assertEquals("ws://a.com:80/x", DartUri.parse("ws://a.com:80/x").toString());
        assertEquals("//a.com:80/x", DartUri.parse("//a.com:80/x").toString());
        // An empty path is not given a slash.
        assertEquals("http://a.com", DartUri.parse("http://a.com").toString());
        assertNotEquals(DartUri.parse("http://a.com/"), DartUri.parse("http://a.com"));
        DartUri explicit = DartUri.parse("http://a.com:80/x");
        DartUri implied = DartUri.parse("http://a.com/x");
        assertEquals(explicit, implied);
        assertEquals(explicit.hashCode(), implied.hashCode());
        assertEquals(80, DartUri.parse("http://a.com:").port(), "an empty port is no port");
        assertEquals(81, DartUri.parse("http://a.com:081").port());
    }

    @Test
    public void aLocalValueAndItsUtcFormAreNotEqual() {
        DateTime local = new DateTime(2024, 3, 5, 7, 8, 9, 10, 0);
        DateTime utc = local.toUtc();
        assertTrue(local.isAtSameMomentAs(utc), "the same instant");
        assertFalse(local.equals(utc), "but not equal: the time-zone mode differs");
        assertTrue(local.equals(utc.toLocal()));
    }

    // --- String's Pattern methods accept a RegExp -----------------------------

    @Test
    public void splitOnARegExpFollowsDart() {
        assertEquals(java.util.Arrays.asList("a", "b"), DString.split("a,b", new RegExp(",")));
        assertEquals(java.util.Arrays.asList("a", "b", "c"), DString.split("a1b22c", new RegExp("[0-9]+")));
        assertEquals(java.util.Arrays.asList("a", "b", "b", "a"), DString.split("abba", new RegExp("")),
                "empty matches split between characters, with no empty parts at the ends");
        assertEquals(0, DString.split("", new RegExp("")).size(), "an empty input the pattern matches has no parts");
        assertEquals(java.util.Arrays.asList(""), DString.split("", new RegExp(",")));
        assertEquals(java.util.Arrays.asList("a", ""), DString.split("a,", new RegExp(",")));
    }

    @Test
    public void theOtherPatternMethodsAcceptARegExp() {
        assertTrue(DString.contains("abc", new RegExp("b+")));
        assertFalse(DString.contains("abc", new RegExp("x")));
        assertEquals(1, DString.indexOf("xab", new RegExp("a")));
        assertEquals(1, DString.indexOf("aaa", new RegExp("aa"), 1),
                "searches from the start offset, so an overlapping match is found");
        assertEquals(3, DString.lastIndexOf("abcab", new RegExp("ab")));
        assertEquals(-1, DString.lastIndexOf("abc", new RegExp("x")));
        assertEquals("-a-b-c-", DString.replaceAll("abc", new RegExp(""), "-"));
        assertEquals("x-y-z", DString.replaceAll("x1y22z", new RegExp("[0-9]+"), "-"));
        assertEquals("a$1c", DString.replaceAll("abc", new RegExp("(b)"), "$1"), "the replacement is literal");
        assertEquals("a-b2", DString.replaceFirst("a1b2", new RegExp("[0-9]"), "-"));
    }

    // --- numbers as keys, sorting, text ----------------------------------------

    @Test
    public void aDoubleKeyFindsTheEqualIntKey() {
        DartMap<Object, String> m = new DartMap<Object, String>();
        m.put(Long.valueOf(1), "a");
        assertEquals("a", m.get(Double.valueOf(1.0)), "{1: 'a'}[1.0]");
        m.put(Double.valueOf(1.0), "b");
        assertEquals(1, m.size(), "m[1.0] = 'b' updates the entry for 1");
        assertTrue(m.keySet().iterator().next() instanceof Long, "and the key stays the int 1");
        assertEquals("b", m.get(Long.valueOf(1)));
        m.put(Double.valueOf(0.0), "zero");
        assertEquals("zero", m.get(Double.valueOf(-0.0)), "0.0 == -0.0");
        assertEquals("zero", m.remove(Long.valueOf(0)));
        assertFalse(m.containsKey(Double.valueOf(0.0)));
    }

    @Test
    public void aSetHoldsOneOfEachEqualNumber() {
        DartSet<Object> s = new DartSet<Object>();
        s.add(Long.valueOf(1));
        assertTrue(s.contains(Double.valueOf(1.0)));
        assertFalse(s.add(Double.valueOf(1.0)), "adding 1.0 to {1} changes nothing");
        assertEquals(1, s.size());
        assertTrue(s.remove(Double.valueOf(1.0)));
        assertTrue(s.isEmpty());
    }

    @Test
    public void mixedNumbersSortNumerically() {
        DartList<Object> l = new DartList<Object>();
        l.add(Long.valueOf(2));
        l.add(Double.valueOf(1.5));
        l.add(Long.valueOf(1));
        l.sortDefault();   // what the emitter calls for a bare sort()
        assertEquals(java.util.Arrays.<Object>asList(Long.valueOf(1), Double.valueOf(1.5), Long.valueOf(2)), l);
        assertTrue(DartComparable.compare(Long.valueOf(1), Double.valueOf(1.5)) < 0);
    }

    @Test
    public void equalIntMapsAreNotEqualMaps() {
        DartLongMap a = new DartLongMap();
        DartLongMap b = new DartLongMap();
        a.putLong(1, 2);
        b.putLong(1, 2);
        assertFalse(a.equals(b), "<int, int>{1: 2} == <int, int>{1: 2} is false in Dart");
        assertTrue(a.equals(a));
    }

    @Test
    public void writeCharCodeWritesASurrogatePairAboveTheBmp() {
        StringBuffer sb = new StringBuffer();
        sb.writeCharCode(0x1F600);
        assertEquals("\uD83D\uDE00", sb.toString());
        assertThrows(RangeError.class, () -> new StringBuffer().writeCharCode(0x110000));
    }

    @Test
    public void theHostAndSchemeAreCanonicallyLowerCase() {
        DartUri u = DartUri.parse("HTTPS://User@EXAMPLE.COM:8080/Path?Q=V#Frag");
        assertEquals("https", u.scheme());
        assertEquals("example.com", u.host());
        assertEquals("https://User@example.com:8080/Path?Q=V#Frag", u.toString(),
                "the path, query, fragment and user info keep their case");
        assertEquals("fe80::1", DartUri.parse("http://[FE80::1]:80/").host());
    }

    @Test
    public void schemeIsRestrictedToAsciiLetters() {
        // Dart's scheme grammar (RFC 3986) is ASCII letter, then ASCII
        // letters/digits/+/-/. only. Character.isLetter/isLetterOrDigit
        // accept any Unicode letter, so a non-ASCII letter used to be
        // accepted as (part of) a scheme; written with a backslash-u escape
        // so the source file itself stays pure ASCII.
        assertThrows(FormatException.class, () -> DartUri.parse("\u00e9:x"));
        assertEquals(null, DartUri.tryParse("\u00e9:x"));
        assertThrows(FormatException.class, () -> DartUri.parse("h\u00e9ttp://example.com/"));
        assertEquals("https", DartUri.parse("https://example.com/").scheme(),
                "a plain ASCII scheme must keep working");
    }

    @Test
    public void padLeftRepeatsTheWholePaddingOncePerMissingPosition() {
        // Recorded from the Dart SDK: 'x'.padLeft(4, 'ab') is 'abababx', NOT
        // 'ababx'. The padding is prepended once per missing position whatever its
        // length, which the Dart documentation spells out for multi-character
        // padding such as '&nbsp;'.
        assertEquals("abababx", DString.padLeft("x", 4, "ab"));
        assertEquals("xababab", DString.padRight("x", 4, "ab"));
    }

    @Test
    public void tryParseAnswersNullAndParseThrowsFormatException() {
        assertEquals(null, DString.tryParseInt("x1"));
        assertEquals(Long.valueOf(42), DString.tryParseInt(" 42 "));
        assertThrows(FormatException.class, () -> DString.parseInt("nope"));
    }

    // --- values recorded from the Dart SDK -------------------------------------

    @Test
    public void aDateTimeKeepsItsMicroseconds() {
        DateTime a = DateTime.utc(2024, 1, 15, 10, 30, 0, 0, 1);
        DateTime b = DateTime.utc(2024, 1, 15, 10, 30, 0, 0, 999);
        assertFalse(a.equals(b));
        assertEquals(998, b.difference(a).inMicroseconds());
        assertEquals(1, a.microsecond());
        assertEquals("2024-01-15 10:30:00.000001Z", a.toString());
        assertEquals("2024-01-15 10:30:00.000Z", DateTime.utc(2024, 1, 15, 10, 30, 0, 0, 0).toString());
        assertEquals(a.microsecondsSinceEpoch() + 5,
                a.add(Duration.ofMicroseconds(5)).microsecondsSinceEpoch(), "a sub-millisecond add counts");
        DateTime early = DateTime.fromMicrosecondsSinceEpoch(-1, true);
        assertEquals(-1, early.millisecondsSinceEpoch(), "milliseconds round toward negative infinity");
        assertEquals(999, early.microsecond());
    }

    /// Dart's DateTime is valid for +/-100,000,000 days from the epoch
    /// (+/-8,640,000,000,000,000 milliseconds). fromMillisecondsSinceEpoch used
    /// to multiply by 1000 to get microseconds BEFORE checking this -- it did
    /// not check it at all -- so Long.MAX_VALUE milliseconds overflowed the
    /// long multiply and wrapped around to a small, in-range microsecond value,
    /// landing near the epoch instead of failing the way Dart does.
    @Test
    public void dateTimeRejectsOutOfRangeMillisecondsAndMicroseconds() {
        long maxMillis = 8640000000000000L;
        // At the bound is still valid.
        assertEquals(maxMillis, DateTime.fromMillisecondsSinceEpoch(maxMillis, true).millisecondsSinceEpoch());
        assertEquals(-maxMillis, DateTime.fromMillisecondsSinceEpoch(-maxMillis, true).millisecondsSinceEpoch());
        assertThrows(ArgumentError.class, () -> DateTime.fromMillisecondsSinceEpoch(maxMillis + 1, true));
        assertThrows(ArgumentError.class, () -> DateTime.fromMillisecondsSinceEpoch(-maxMillis - 1, true));
        // The overflow case the review found: a huge millisecond value that
        // used to wrap around through the *1000 multiply into something small
        // and in-range instead of being rejected.
        assertThrows(ArgumentError.class, () -> DateTime.fromMillisecondsSinceEpoch(Long.MAX_VALUE, true));
        assertThrows(ArgumentError.class, () -> DateTime.fromMillisecondsSinceEpoch(Long.MIN_VALUE, true));
        // fromMicrosecondsSinceEpoch never multiplies, but still must not
        // silently accept a value outside Dart's range.
        long maxMicros = maxMillis * 1000L;
        assertEquals(maxMicros, DateTime.fromMicrosecondsSinceEpoch(maxMicros, true).microsecondsSinceEpoch());
        assertThrows(ArgumentError.class, () -> DateTime.fromMicrosecondsSinceEpoch(maxMicros + 1, true));
        assertThrows(ArgumentError.class, () -> DateTime.fromMicrosecondsSinceEpoch(-maxMicros - 1, true));
        // Arithmetic validates its result too, including a sum that overflows a long
        // into an apparently valid instant.
        DateTime top = DateTime.fromMicrosecondsSinceEpoch(maxMicros, true);
        DateTime bottom = DateTime.fromMicrosecondsSinceEpoch(-maxMicros, true);
        assertThrows(ArgumentError.class, () -> top.add(Duration.ofMicroseconds(1)));
        assertThrows(ArgumentError.class, () -> bottom.subtract(Duration.ofMicroseconds(1)));
        assertThrows(ArgumentError.class, () -> top.add(Duration.ofMicroseconds(Long.MAX_VALUE)));
        assertThrows(ArgumentError.class, () -> bottom.subtract(Duration.ofMicroseconds(Long.MAX_VALUE)));
        assertThrows(ArgumentError.class, () -> top.subtract(Duration.ofMicroseconds(Long.MIN_VALUE)));
        assertEquals(maxMicros - 1, top.subtract(Duration.ofMicroseconds(1)).microsecondsSinceEpoch());
    }

    @Test
    public void anIdentityMapKeepsEqualKeysApart() {
        DartMap<Object, String> m = DartMap.identity();
        String a = new String("k");
        String b = new String("k");
        m.put(a, "first");
        m.put(b, "second");
        assertEquals(2, m.size(), "equal but distinct keys are two entries");
        assertEquals("first", m.get(a));
        assertEquals("second", m.get(b));
        assertEquals(null, m.get("k"));
        assertEquals(java.util.Arrays.asList(a, b), new java.util.ArrayList<Object>(m.keySet()));
        m.remove(a);
        assertEquals(1, m.size());
    }

    @Test
    public void anIdentityMapKeysIterableAgreesWithContainsKey() {
        // keys() used to be the generic DartIterable wrapping keySet(), whose
        // contains() scans with Dart's == -- so an equal but distinct key read
        // as present through keys().contains() while containsKey() (identity)
        // said it was not. Both must agree, in both directions.
        DartMap<Object, String> m = DartMap.identity();
        String a = new String("k");
        String b = new String("k");
        m.put(a, "first");
        assertTrue(m.keys().contains(a));
        assertTrue(m.containsKey(a));
        assertFalse(m.keys().contains(b), "equal but distinct key must not read as a member");
        assertFalse(m.containsKey(b));
        assertFalse(m.keys().contains("k"));
        assertFalse(m.containsKey("k"));

        // values()/entries() checked for the same divergence: valuesIterable()
        // must agree with containsValue (values are never identity-compared,
        // but the delegation must not break the ordinary case), and entries()
        // -- MapEntry has no == override in Dart, so a freshly built MapEntry is
        // never `contains`-equal to another, identity map or not.
        assertTrue(m.valuesIterable().contains("first"));
        assertTrue(m.containsValue("first"));
        assertFalse(m.valuesIterable().contains("second"));
        assertFalse(m.entries().contains(new MapEntry<Object, String>(a, "first")));
    }

    @Test
    public void doublesPrintInDartNotation() {
        // Recorded from the Dart SDK.
        String[][] cases = {
            {"0.0001", "0.0001"}, {"0.000001", "0.000001"}, {"1e-7", "1e-7"},
            {"1e16", "10000000000000000.0"}, {"1e20", "100000000000000000000.0"},
            {"1e21", "1e+21"}, {"1.5e22", "1.5e+22"}, {"12345678.9", "12345678.9"},
            {"123.456", "123.456"}, {"-0.0005", "-0.0005"}, {"2.5e-10", "2.5e-10"},
            {"0.1", "0.1"}, {"100.0", "100.0"}, {"1e300", "1e+300"},
        };
        for (String[] c : cases) {
            assertEquals(c[1], dart.runtime.DartRuntime.doubleStr(Double.parseDouble(c[0])), c[0]);
        }
    }

    @Test
    public void intParseTakesARadixAndHex() {
        assertEquals(255, DString.parseInt("ff", 16));
        assertEquals(Long.valueOf(255), DString.tryParseInt("0xFF"));
        assertEquals(Long.valueOf(-16), DString.tryParseInt("-0x10"));
        assertEquals(Long.valueOf(1295), DString.tryParseInt("zz", 36));
        assertEquals(null, DString.tryParseInt("12", 2));
        assertEquals(null, DString.tryParseInt("--1"));
        assertThrows(RangeError.class, () -> DString.tryParseInt("1", 37));
    }

    @Test
    public void caseConversionIgnoresTheDefaultLocale() {
        java.util.Locale saved = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(new java.util.Locale("tr", "TR"));
            assertEquals("ISTANBUL", DString.toUpperCase("istanbul"), "no dotted capital I");
            assertEquals("title", DString.toLowerCase("TITLE"), "no dotless i");
        } finally {
            java.util.Locale.setDefault(saved);
        }
    }

    /// Dart's toUpperCase/toLowerCase use Unicode's FULL default case mapping,
    /// not a per-UTF-16-unit one: measured on the Dart 3.9 VM,
    /// 'stra\u00dfe'.toUpperCase() (sharp s, U+00DF) answers "STRASSE", not
    /// "STRA\u00dfE" -- a single character expands to two. A per-char
    /// Character.toUpperCase(char) loop cannot do this (nowhere to put the
    /// second output char) and would also split a supplementary-plane
    /// surrogate pair into two lone, unmapped halves.
    @Test
    public void caseConversionUsesUnicodesFullMapping() {
        assertEquals("STRASSE", DString.toUpperCase("stra\u00dfe"), "sharp s expands to SS");
        assertEquals("FI", DString.toUpperCase("\ufb01"), "the fi ligature expands to two letters");
        assertEquals("\ufb03", DString.toLowerCase("\ufb03"), "an already-lower ligature has no lower mapping of its own");
        // U+0130 (capital I with dot above) lowercases to "i" plus a combining
        // dot above, the one unconditional entry whose LOWER mapping (not just
        // upper) is multi-character.
        assertEquals("i\u0307", DString.toLowerCase("\u0130"));
        // A surrogate pair (U+10428 DESERET SMALL LETTER LONG I, upper is
        // U+10400) has to travel through this as one code point: mapping each
        // UTF-16 half on its own would answer two lone, unpaired surrogates.
        String deseretLower = new String(Character.toChars(0x10428));
        String deseretUpper = new String(Character.toChars(0x10400));
        String upperedResult = DString.toUpperCase(deseretLower);
        assertEquals(deseretUpper, upperedResult);
        assertEquals(deseretLower, DString.toLowerCase(deseretUpper));
        assertEquals(1, upperedResult.codePointCount(0, upperedResult.length()),
                "still one code point, not two mismatched surrogate halves");
    }

    /// Unicode's Final_Sigma: the one context-sensitive rule the default
    /// (locale-free) case-mapping algorithm still applies. A capital sigma
    /// lowercases to the final form (U+03C2) at the end of a word and to the
    /// medial form (U+03C3) everywhere else -- confirmed against the real
    /// JDK's String.toLowerCase(Locale.ROOT), which implements the same
    /// Unicode default algorithm.
    @Test
    public void toLowerCaseAppliesFinalSigmaAtWordEnd() {
        // Greek "AS": sigma is the last letter -> final form.
        assertEquals("\u03b1\u03c2", DString.toLowerCase("\u0391\u03a3"), "sigma at the end of a word");
        // Greek "ASA": sigma is followed by another letter -> medial form.
        assertEquals("\u03b1\u03c3\u03b1", DString.toLowerCase("\u0391\u03a3\u0391"),
                "sigma in the middle of a word");
    }

    @Test
    public void indexOfRangeChecksItsStart() {
        assertThrows(RangeError.class, () -> DString.indexOf("abc", "a", -1));
        assertThrows(RangeError.class, () -> DString.indexOf("abc", "a", 4));
        assertEquals(-1, DString.indexOf("abc", "c", 3), "the length itself is a valid start");
    }

    @Test
    public void equivalentUrisAreEqual() {
        DartUri a = DartUri.parse("https://EXAMPLE.com/a");
        DartUri b = DartUri.parse("https://example.com/a");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(DartUri.parse("https://example.com/A")), "the path is case sensitive");
    }

    // --- collections, round 4 ---------------------------------------------------

    @Test
    public void anIdentitySetKeepsEqualElementsApart() {
        DartSet<Object> s = DartSet.identity();
        String a = new String("v");
        String b = new String("v");
        assertTrue(s.add(a));
        assertTrue(s.add(b), "an equal but distinct element is new");
        assertFalse(s.add(a));
        assertEquals(2, s.size());
        assertFalse(s.contains("v"));
        assertTrue(s.remove(a));
        assertEquals(1, s.size());
    }

    @Test
    public void entriesIsALiveView() {
        DartMap<Object, Object> m = new DartMap<Object, Object>();
        m.put("a", 1L);
        DartIterable<MapEntry<Object, Object>> entries = m.entries();
        m.put("b", 2L);
        assertEquals(2, entries.length(), "a retained view sees the later entry");
    }

    @Test
    public void takeAndSkipRefuseANegativeCount() {
        DartList<Object> l = new DartList<Object>();
        l.add("x");
        assertThrows(RangeError.class, () -> l.take(-1));
        assertThrows(RangeError.class, () -> l.skip(-1));
        assertEquals(1, l.take(5).length());
    }

    /// skip and skipWhile are lazy in Dart: creating an iterator runs nothing, and the
    /// predicate (or an upstream map) first runs when the iterator is advanced.
    @Test
    public void skipAndSkipWhileRunNothingUntilTheIteratorAdvances() {
        final int[] calls = {0};
        DartList<Long> xs = DartList.of(1L, 2L, 5L, 1L);
        java.util.Iterator<Long> it = xs.asIterable().skipWhile(e -> {
            calls[0]++;
            return e < 3;
        }).iterator();
        assertEquals(0, calls[0], "creating the iterator must not run the predicate");
        assertTrue(it.hasNext());
        assertEquals(Long.valueOf(5), it.next());
        assertEquals(Long.valueOf(1), it.next());
        assertFalse(it.hasNext());
        assertEquals(3, calls[0]);

        final int[] mapped = {0};
        java.util.Iterator<Long> sk = xs.asIterable().map(e -> {
            mapped[0]++;
            return e;
        }).skip(2).iterator();
        assertEquals(0, mapped[0], "creating a skip iterator must not compute the skipped elements");
        assertEquals(Long.valueOf(5), sk.next());
    }

    /// The minimum duration has no positive counterpart, so negating first overflowed and
    /// printed negative components; Dart prints -2562047788:00:54.775808.
    @Test
    public void theMinimumDurationPrintsLikeDarts() {
        assertEquals("-2562047788:00:54.775808", Duration.ofMicroseconds(Long.MIN_VALUE).toString());
        assertEquals("-1:02:03.000004", Duration.ofMicroseconds(-(3723000004L)).toString());
        assertEquals("1:02:03.000004", Duration.ofMicroseconds(3723000004L).toString());
    }

    /// An explicit null generator means the same as none: the indices.
    @Test
    public void iterableGenerateWithANullGeneratorYieldsTheIndices() {
        assertEquals("[0, 1, 2]", DartIterable.<Long>generate(3, null).toList().toString());
    }

    /// Dart's iterators check on every moveNext. Java's check only in next(), so a body
    /// that cleared or shrank the collection after the last-but-one element ended the
    /// loop quietly: hasNext() answered false from the live size.
    @Test
    public void shrinkingACollectionWhileIteratingItThrowsFromHasNext() {
        final DartList<Object> l = new DartList<Object>();
        l.add("a");
        l.add("b");
        assertThrows(ConcurrentModificationError.class, () -> {
            for (Object x : l) {
                l.clear();
            }
        });
        // Replacing an element is not a length change, so Dart does not report it.
        DartList<Object> r = new DartList<Object>();
        r.add("a");
        r.add("b");
        int seen = 0;
        for (Object x : r) {
            r.set(0, "z");
            seen++;
        }
        assertEquals(2, seen);

        final DartSet<Object> set = new DartSet<Object>();
        set.add("a");
        set.add("b");
        assertThrows(ConcurrentModificationError.class, () -> {
            for (Object x : set) {
                set.clear();
            }
        });

        final DartMap<Object, Object> m = new DartMap<Object, Object>();
        m.put("a", 1);
        m.put("b", 2);
        assertThrows(ConcurrentModificationError.class, () -> {
            for (Object k : m.keys()) {
                m.clear();
            }
        });
        m.put("a", 1);
        m.put("b", 2);
        assertThrows(ConcurrentModificationError.class, () -> {
            for (Object v : m.valuesIterable()) {
                m.remove("b");
            }
        });
        m.put("b", 2);
        assertThrows(ConcurrentModificationError.class, () -> {
            for (MapEntry<Object, Object> e : m.entries()) {
                m.remove("a");
            }
        });

        // removeWhere removes through the JDK iterator, bypassing remove(): it must still
        // count as a change for a keys loop around it.
        final DartMap<Object, Object> rw = new DartMap<Object, Object>();
        rw.put("a", 1);
        assertThrows(ConcurrentModificationError.class, () -> {
            for (Object k : rw.keys()) {
                rw.removeWhere((key, value) -> Boolean.TRUE);
            }
        });

        // Set.identity() has its own storage; its iterator must be checked the same way.
        final DartIdentitySet<Object> ids = new DartIdentitySet<Object>();
        final Object only = new Object();
        ids.add(only);
        assertThrows(ConcurrentModificationError.class, () -> {
            for (Object x : ids) {
                ids.remove(only);
            }
        });

        final DartLongMap lm = new DartLongMap();
        lm.putLong(1, 10);
        lm.putLong(2, 20);
        assertThrows(ConcurrentModificationError.class, () -> {
            for (Long k : lm.keySet()) {
                lm.clear();
            }
        });
        lm.putLong(1, 10);
        lm.putLong(2, 20);
        assertThrows(ConcurrentModificationError.class, () -> {
            for (Long v : lm.values()) {
                lm.clear();
            }
        });
        lm.putLong(1, 10);
        lm.putLong(2, 20);
        assertThrows(ConcurrentModificationError.class, () -> {
            for (java.util.Map.Entry<Long, Long> e : lm.entrySet()) {
                lm.clear();
            }
        });
    }

    @Test
    public void aListModifiedWhileIteratedFailsInsteadOfLooping() {
        DartList<Object> l = new DartList<Object>();
        l.add("a");
        l.add("b");
        assertThrows(ConcurrentModificationError.class, () -> {
            for (Object x : l) {
                l.add(x);
            }
        });
        DartLongList longs = DartLongList.ofLongs(1, 2);
        assertThrows(ConcurrentModificationError.class, () -> {
            for (Long x : longs) {
                longs.addLong(x);
            }
        });
    }

    @Test
    public void aSpecialisedIntMapHasTheDartMapApi() {
        DartLongMap m = DartLongMap.ofLongs(1, 10, 2, 20);
        long sum = 0;
        for (MapEntry<Long, Long> e : m.entries()) {
            sum += e.key() + e.value();
        }
        assertEquals(33, sum);
        m.update(1L, v -> v + 1, null);
        assertEquals(Long.valueOf(11), m.idx(1));
        assertEquals(Long.valueOf(7), m.putIfAbsentDart(3L, () -> 7L));
        m.removeWhere((k, v) -> v > 15);
        assertEquals(2, m.length(), "20 removed; 11 and 7 remain");
        m.idxSet(4L, 4L);
        assertTrue(m.containsKeyLong(4));
    }

    @Test
    public void anIterableHasForEachDart() {
        DartList<Object> l = new DartList<Object>();
        l.add("a");
        l.add("b");
        final StringBuilder sb = new StringBuilder();
        l.asIterable().forEachDart(x -> sb.append(x));
        assertEquals("ab", sb.toString());
    }

    @Test
    public void trimAndStartsWithFollowDart() {
        assertEquals("padded", DString.trim("\u00a0 padded \u2003\u2028"));
        assertEquals("x", DString.trim("\ufeffx"));
        assertTrue(DString.startsWith("abc", "b", 1));
        assertThrows(RangeError.class, () -> DString.startsWith("abc", "a", 4));
    }

    @Test
    public void fromHonoursGrowableAndIndexOfTakesAStart() {
        DartList<Object> fixed = DartList.from(java.util.Arrays.<Object>asList("a", "b"), false);
        assertThrows(UnsupportedError.class, () -> fixed.add("c"));
        DartLongList longs = DartLongList.fromLongs(java.util.Arrays.asList(1L, 2L), false);
        assertThrows(UnsupportedError.class, () -> longs.addLong(3));
        DartList<Object> l = DartList.of((Object) 1L, 2L, 1L);
        assertEquals(2, l.indexOfDart(1L, 1));
    }

    // --- round 6 ----------------------------------------------------------------

    @Test
    public void negativeListLengthsAreRefused() {
        assertThrows(RangeError.class, () -> DartList.filled(-1, "x"));
        assertThrows(RangeError.class, () -> DartList.generate(-1, i -> "x"));
        assertThrows(RangeError.class, () -> DartLongList.filled(-1, 0L));
        assertThrows(RangeError.class, () -> DartDoubleList.generateDoubles(-1, i -> 0.0));
        assertEquals(0, DartList.filled(0, "x").size());
    }

    @Test
    public void byteDataChecksTheOffsetBeforeNarrowingIt() {
        dart.typed_data.ByteData b = new dart.typed_data.ByteData(4);
        b.setUint8(0, 7);
        assertThrows(RangeError.class, () -> b.getUint8(4294967296L), "2^32 must not wrap to byte 0");
        assertThrows(RangeError.class, () -> b.setUint8(-1, 1));
        assertThrows(RangeError.class, () -> b.getInt32(1), "four bytes from 1 run past the end");
        assertEquals(7, b.getUint8(0));
    }

    @Test
    public void malformedUrisAreRejectedAsDartRejectsThem() {
        // Each expectation recorded from the Dart SDK.
        assertEquals(null, DartUri.tryParse("http://[::1"));
        assertEquals(null, DartUri.tryParse("http://host:abc/"));
        assertEquals(null, DartUri.tryParse("::"));
        assertEquals(null, DartUri.tryParse("1http://x"));
        assertThrows(FormatException.class, () -> DartUri.parse("http://[::1"));
        // ...and what it accepts, it keeps accepting.
        assertTrue(DartUri.tryParse("http://host:99999/") != null);
        assertTrue(DartUri.tryParse("a%zzb") != null);
        assertTrue(DartUri.tryParse("mailto:a@b") != null);
        assertTrue(DartUri.tryParse("http://[::1]:80/x") != null);
        assertTrue(DartUri.tryParse("") != null);
    }

    // --- round 7 ----------------------------------------------------------------

    @Test
    public void intMapIterationFailsFastWhenTheMapGrows() {
        final DartLongMap m = DartLongMap.ofLongs(1, 1);
        assertThrows(ConcurrentModificationError.class,
                () -> m.forEachDart((k, v) -> m.putLong(m.length() + 1, v)),
                "a callback that adds keys must not be chased forever");
        DartLongMap updating = DartLongMap.ofLongs(1, 1, 2, 2);
        updating.forEachDart((k, v) -> updating.putLong(k, v + 10));
        assertEquals(Long.valueOf(11), updating.idx(1), "a value update is not a structural change");
    }

    @Test
    public void addAllMatchesKeysWithDartEquality() {
        DartMap<Object, String> m = new DartMap<Object, String>();
        m.put(Long.valueOf(1), "a");
        java.util.Map<Object, String> more = new java.util.HashMap<Object, String>();
        more.put(Double.valueOf(1.0), "b");
        m.addAll(more);
        assertEquals(1, m.size(), "{1: 'a'}.addAll({1.0: 'b'}) is {1: b}");
        assertEquals("b", m.get(Long.valueOf(1)));
    }

    @Test
    public void aStartBeyondIntDoesNotWrap() {
        DartList<Object> l = DartList.of((Object) 1L);
        assertEquals(-1, l.indexOfDart(1L, 4294967296L), "2^32 must not wrap to 0");
        assertEquals(-1, l.indexWhere(x -> true, 4294967296L));
    }

    @Test
    public void aNegativeStartFarBelowIntDoesNotWrapForLastIndex() {
        // Recorded from the real Dart SDK: [1].lastIndexOf(1, -4294967296) is -1, because
        // Dart's own backward loop starts at a negative index and never runs at all.
        // Narrowing -4294967296L (exactly -2^32) to int first wrapped it to 0, which read
        // as "search from the front" and found the element instead of answering -1.
        DartList<Object> l = DartList.of((Object) 1L);
        assertEquals(-1, l.lastIndexOfDart(1L, -4294967296L), "-2^32 must not wrap to 0");
        assertEquals(-1, l.lastIndexOfDart(1L, -1L));
        DartList<Object> three = DartList.of((Object) 1L, 2L, 1L);
        assertEquals(-1, three.lastIndexWhere(x -> x.equals(1L), -4294967296L));
        // The non-overflowing case keeps working: searching backward from index 1 finds
        // the earlier "1", not the one after it.
        assertEquals(0, three.lastIndexWhere(x -> x.equals(1L), 1L));
    }

    @Test
    public void dartsOwnAnswersForNegativeRepeatAndGenerate() {
        // Both recorded from the Dart SDK, against review findings that expected throws:
        // 'ab' * -1 is the empty string, and Iterable.generate(-1) is empty.
        assertEquals("", DString.repeat("ab", -1));
        assertEquals(0, DartIterable.generate(-1, i -> i).length());
    }

    // --- round 8 ----------------------------------------------------------------

    @Test
    public void aLengthNoArrayCanHoldIsRefusedNotWrapped() {
        // Dart runs out of memory for List.filled(2^32, ..); narrowed first, it was an
        // empty list.
        assertThrows(OutOfMemoryError.class, () -> DartLongList.filled(4294967296L, 0L));
        assertThrows(OutOfMemoryError.class, () -> DartList.filled(4294967296L, "x"));
    }

    @Test
    public void asMapIsALiveUnmodifiableView() {
        DartList<Object> l = DartList.of((Object) 1L, 2L);
        DartMap<Long, Object> m = l.asMap();
        l.set(0, 9L);
        assertEquals(9L, m.get(0L), "the view reads the list as it is now");
        assertEquals(2, m.size());
        assertThrows(UnsupportedError.class, () -> m.put(0L, 5L));
        assertEquals(java.util.Arrays.asList(0L, 1L), new java.util.ArrayList<Long>(m.keySet()));
    }

    @Test
    public void asMapRefusesEveryMutatorWithoutRunningItsCallback() {
        // Regression: DartListMapView only overrode put/putAll/remove/clear, so
        // the mutators DartUnmodifiableMap had to add overrides for too --
        // DartMap's own extensions (putIfAbsentDart, update) and java.util.Map's
        // putIfAbsent/compute*/merge/replace*/remove(k,v)/replaceAll, which
        // HashMap implements by manipulating its table directly -- still
        // mutated List.asMap() (and ran their callback) instead of throwing.
        DartList<Object> l = DartList.of((Object) 1L, 2L);
        DartMap<Long, Object> m = l.asMap();
        boolean[] ran = {false};

        assertThrows(UnsupportedError.class,
                () -> m.putIfAbsentDart(0L, () -> {
                    ran[0] = true;
                    return 9L;
                }),
                "putIfAbsentDart on an EXISTING key used to return the stored value "
                        + "instead of throwing");
        assertFalse(ran[0], "the ifAbsent callback must not run");

        assertThrows(UnsupportedError.class,
                () -> m.update(0L, v -> {
                    ran[0] = true;
                    return v;
                }, null),
                "update used to run its callback before the put() it fed into failed");
        assertFalse(ran[0], "the update callback must not run");

        assertThrows(UnsupportedError.class, () -> m.removeDart(0L));
        assertThrows(UnsupportedError.class, () -> m.addAll(new java.util.HashMap<>()),
                "addAll of an EMPTY map must still throw");
        assertThrows(UnsupportedError.class,
                () -> m.addEntries(java.util.Collections.emptyList()),
                "addEntries of an EMPTY iterable must still throw");

        assertThrows(UnsupportedError.class, () -> m.putIfAbsent(0L, 9L));
        assertThrows(UnsupportedError.class, () -> m.putIfAbsent(5L, 9L));
        assertThrows(UnsupportedError.class, () -> m.remove(0L, 1L));
        assertThrows(UnsupportedError.class, () -> m.replace(0L, 1L, 9L));
        assertThrows(UnsupportedError.class, () -> m.replace(0L, 9L));
        assertThrows(UnsupportedError.class, () -> m.computeIfAbsent(5L, k -> {
            ran[0] = true;
            return 9L;
        }));
        assertFalse(ran[0], "the computeIfAbsent callback must not run");
        assertThrows(UnsupportedError.class, () -> m.computeIfPresent(0L, (k, v) -> {
            ran[0] = true;
            return v;
        }));
        assertFalse(ran[0], "the computeIfPresent callback must not run");
        assertThrows(UnsupportedError.class, () -> m.compute(0L, (k, v) -> {
            ran[0] = true;
            return v;
        }));
        assertFalse(ran[0], "the compute callback must not run");
        assertThrows(UnsupportedError.class, () -> m.merge(0L, 9L, (v1, v2) -> {
            ran[0] = true;
            return v1;
        }));
        assertFalse(ran[0], "the merge callback must not run");
        assertThrows(UnsupportedError.class, () -> m.replaceAll((k, v) -> {
            ran[0] = true;
            return v;
        }));
        assertFalse(ran[0], "the replaceAll callback must not run");

        // Still readable, and nothing above touched it.
        assertEquals(1L, m.get(0L));
        assertEquals(2, m.size());
    }

    @Test
    public void asMapKeySetAndEntrySetIteratorsDetectConcurrentModification() {
        // Regression: asMap()'s keySet()/entrySet() iterators compared against the
        // LIVE list.size() on every step instead of the length snapshotted when the
        // iterator was created -- mirroring DartList's own ListIterator (reversed(),
        // getRange()), which already gets this right. A growth during iteration
        // silently started handing back keys/entries under indices that did not exist
        // yet, and a shrink truncated the iteration instead of reporting the change,
        // exactly the ConcurrentModificationError Dart itself throws here.
        DartList<Object> l = DartList.of((Object) 1L, 2L, 3L);
        DartMap<Long, Object> m = l.asMap();

        java.util.Iterator<Long> keys = m.keySet().iterator();
        assertEquals(0L, keys.next());
        l.add(4L);
        assertThrows(ConcurrentModificationError.class, keys::hasNext,
                "a grown list must be detected by keySet()'s iterator");

        java.util.Iterator<Long> keysAfterShrink = m.keySet().iterator();
        assertEquals(0L, keysAfterShrink.next());
        l.removeLast();
        assertThrows(ConcurrentModificationError.class, keysAfterShrink::next,
                "a shrunk list must be detected by keySet()'s iterator, not silently "
                        + "truncate the iteration");

        java.util.Iterator<java.util.Map.Entry<Long, Object>> entries = m.entrySet().iterator();
        assertEquals(0L, entries.next().getKey());
        l.add(5L);
        assertThrows(ConcurrentModificationError.class, entries::hasNext,
                "a grown list must be detected by entrySet()'s iterator too");
    }

    @Test
    public void doubleParseFollowsDartsGrammar() {
        // Each recorded from the Dart SDK.
        assertEquals(null, DString.tryParseDouble("1d"));
        assertEquals(null, DString.tryParseDouble("0x1.0p0"));
        assertEquals(null, DString.tryParseDouble("1e"));
        assertEquals(Double.valueOf(1500.0), DString.tryParseDouble(" 1.5e3 "));
        assertEquals(Double.valueOf(0.5), DString.tryParseDouble(".5"));
        assertEquals(Double.valueOf(5.0), DString.tryParseDouble("5."));
        assertEquals(Double.valueOf(1.0), DString.tryParseDouble("+1"));
        assertTrue(DString.tryParseDouble("NaN").isNaN());
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), DString.tryParseDouble("-Infinity"));
        assertThrows(FormatException.class, () -> DString.parseDouble("1f"));
    }

    @Test
    public void anIntAndADoubleCompareExactly() {
        // 2^53 + 1 widens onto 2^53; Dart's compareTo still orders them.
        assertEquals(1L, DartComparable.compare(Long.valueOf(9007199254740993L), Double.valueOf(9007199254740992.0)));
        assertEquals(-1L, DartComparable.compare(Double.valueOf(9007199254740992.0), Long.valueOf(9007199254740993L)));
        assertEquals(0L, DartComparable.compare(Long.valueOf(1), Double.valueOf(1.0)));
        assertEquals(1L, DartComparable.compare(Long.valueOf(0), Double.valueOf(-0.0)));
        assertEquals(-1L, DartComparable.compare(Long.valueOf(1), Double.valueOf(Double.NaN)));
        assertEquals(-1L, DartComparable.compare(Long.valueOf(1), Double.valueOf(1.5)));
        assertEquals(1L, DartComparable.compare(Long.valueOf(Long.MAX_VALUE), Double.valueOf(9.2e18)));
        assertEquals(-1L, DartComparable.compare(Long.valueOf(Long.MAX_VALUE), Double.valueOf(9.3e18)));
        assertEquals(1L, DartComparable.compare(Long.valueOf(-3), Double.valueOf(Double.NEGATIVE_INFINITY)));
    }

    @Test
    public void dotAllLetsTheDotMatchANewline() {
        RegExp all = new RegExp("a.b");
        all.dotAll(true);
        assertTrue(all.hasMatch("a\nb"));
        assertFalse(new RegExp("a.b").hasMatch("a\nb"));
    }

    @Test
    public void toRadixStringRefusesARadixOutsideTwoToThirtySix() {
        assertEquals("ff", dart.runtime.DartRuntime.toRadixString(255, 16));
        assertThrows(RangeError.class, () -> dart.runtime.DartRuntime.toRadixString(255, 1));
        assertThrows(RangeError.class, () -> dart.runtime.DartRuntime.toRadixString(255, 37));
        assertThrows(RangeError.class, () -> dart.runtime.DartRuntime.toRadixString(255, 4294967312L),
                "a radix that would wrap into range as a Java int");
    }

    @Test
    public void minMaxAndPowKeepIntsInts() {
        assertEquals(Long.valueOf(1), DartMath.minNum(Long.valueOf(1), Double.valueOf(2.5)));
        assertEquals(Double.valueOf(2.5), DartMath.maxNum(Long.valueOf(1), Double.valueOf(2.5)));
        assertEquals(Long.valueOf(8), DartMath.powNum(Long.valueOf(2), Long.valueOf(3)));
        assertEquals(Double.valueOf(0.5), DartMath.powNum(Long.valueOf(2), Long.valueOf(-1)));
        assertEquals(Double.valueOf(-0.0), DartMath.minNum(Double.valueOf(0.0), Double.valueOf(-0.0)));
        assertEquals(Double.valueOf(0.0), DartMath.maxNum(Double.valueOf(-0.0), Double.valueOf(0.0)));
        assertTrue(Double.isNaN(DartMath.minNum(Long.valueOf(1), Double.valueOf(Double.NaN)).doubleValue()));
    }

    @Test
    public void getRangeIsALiveViewThatNoticesALengthChange() {
        DartList<Long> list = new DartList<Long>();
        list.add(1L);
        list.add(2L);
        list.add(3L);
        DartIterable<Long> range = list.getRange(0, 2);
        list.set(0, 9L);
        assertEquals("[9, 2]", range.toList().toString());
        java.util.Iterator<Long> it = range.iterator();
        it.next();
        list.add(4L);
        assertThrows(ConcurrentModificationError.class, it::next);
        assertThrows(RangeError.class, () -> list.getRange(1, 9));
    }

    @Test
    public void mapsFindValuesAndIntKeysByDartEquality() {
        DartMap<String, Object> m = new DartMap<String, Object>();
        m.put("a", 1L);
        assertTrue(m.containsValue(1.0));
        assertFalse(m.containsValue(1.5));
        DartLongMap ints = DartLongMap.ofLongs(0, 1, 2, 3);
        assertTrue(ints.containsValue(1.0));
        assertEquals(Long.valueOf(3), ints.get(2.0));
        assertTrue(ints.containsKey(2.0));
        assertFalse(ints.containsKey(2.5));
        assertEquals(Long.valueOf(3), ints.remove(2.0));
        assertFalse(ints.containsKey(2L));
    }

    @Test
    public void uriParseNormalizesItsComponents() {
        assertEquals("https://x/a%20b", DartUri.parse("https://x/a b").toString());
        assertEquals("/a%20b", DartUri.parse("https://x/a b").path());
        assertEquals("https://x/a%2Fb~A%25zz%254", DartUri.parse("https://x/a%2fb%7e%41%zz%4").toString());
        assertEquals("https://x/p?q=a%20b#f%20g", DartUri.parse("https://x/p?q=a b#f g").toString());
        assertEquals("https://x/a/b?c%5Cd", DartUri.parse("https://x/a\\b?c\\d").toString());
        assertEquals("https://x/caf%C3%A9%F0%9F%98%80", DartUri.parse("https://x/caf\u00e9\ud83d\ude00").toString());
        assertEquals("https://u%20s@x/p", DartUri.parse("https://u s@x/p").toString());
        assertEquals("mailto:a%20b@x.com", DartUri.parse("mailto:a b@x.com").toString());
        assertEquals("https://x/!$&'()*+,;=:@-._~", DartUri.parse("https://x/!$&'()*+,;=:@-._~").toString());
        assertEquals("a b", DartUri.parse("https://x/a b").pathSegments().get(0));
    }

    /// Dart's Uri.decodeComponent/decodeQueryComponent are strict
    /// (allowMalformed: false): a percent escape that is not valid UTF-8
    /// throws FormatException. java.net.URLDecoder cannot do this -- it
    /// decodes through {@code new String(bytes, "UTF-8")}, whose Charset
    /// decoder REPLACEs a bad sequence with U+FFFD instead of raising
    /// anything a catch could see, so "/%FF" used to come back as a
    /// replacement character instead of failing.
    @Test
    public void uriComponentDecodingIsStrictAboutUtf8() {
        assertThrows(FormatException.class, () -> DartUri.parse("https://x/%FF").pathSegments().get(0),
                "a lone continuation byte is not valid UTF-8");
        assertThrows(FormatException.class, () -> DartUri.parse("https://x/%C0%80").pathSegments().get(0),
                "an overlong encoding of U+0000 is rejected, not accepted as NUL");
        assertThrows(FormatException.class, () -> DartUri.parse("https://x/%ED%A0%80").pathSegments().get(0),
                "a UTF-8 encoded surrogate code point is rejected");
        assertThrows(FormatException.class,
                () -> DartUri.parse("https://x/p?q=%FF").queryParameters().get("q"),
                "queryParameters decodes just as strictly as pathSegments");
        // Valid multi-byte UTF-8 still decodes correctly.
        assertEquals("caf\u00E9", DartUri.parse("https://x/caf%C3%A9").pathSegments().get(0));
        assertEquals("\uD83D\uDE00", DartUri.parse("https://x/%F0%9F%98%80").pathSegments().get(0),
                "a 4-byte sequence decodes back to its surrogate pair");
        // '+' is a literal plus in a path segment but a space in a query value.
        assertEquals("a+b", DartUri.parse("https://x/a+b").pathSegments().get(0));
        assertEquals("a b", DartUri.parse("https://x/p?q=a+b").queryParameters().get("q"));
    }

    @Test
    public void dateTimeIsProlepticGregorian() {
        DateTime d = DateTime.utc(1582, 10, 10, 0, 0, 0, 0, 0);
        assertEquals(1582L, d.year());
        assertEquals(10L, d.month());
        assertEquals(10L, d.day());
        assertEquals(-12219724800000L, d.millisecondsSinceEpoch());
        assertEquals(7L, d.weekday());
        assertEquals("-0001-12-31 23:59:59.999Z", DateTime.utc(-1, 12, 31, 23, 59, 59, 999, 0).toString());
        assertEquals("2024-03-01 00:00:00.000Z", DateTime.utc(2024, 2, 30, 0, 0, 0, 0, 0).toString());
        // A local value round-trips its fields on both sides of the old cutover.
        DateTime old = new DateTime(1582, 10, 10, 12, 30, 0, 0, 0);
        assertEquals("1582-10-10 12:30:00.000", old.toString());
        DateTime modern = new DateTime(2024, 7, 1, 12, 30, 0, 0, 0);
        assertEquals("2024-07-01 12:30:00.000", modern.toString());
    }

    @Test
    public void byteDataRefusesALengthItCannotAllocate() {
        assertThrows(OutOfMemoryError.class, () -> new dart.typed_data.ByteData(4294967296L));
        assertThrows(OutOfMemoryError.class, () -> new dart.typed_data.Uint8List(4294967296L));
        assertThrows(RangeError.class, () -> new dart.typed_data.Uint8List(-1));
        assertEquals(8L, new dart.typed_data.ByteData(8).lengthInBytes());
    }

    @Test
    public void typedListFromChecksEachElementsType() {
        java.util.List<Number> mixed = new java.util.ArrayList<Number>();
        mixed.add(Double.valueOf(1.9));
        assertThrows(TypeError.class, () -> DartLongList.fromLongs(mixed));
        java.util.List<Number> ints = new java.util.ArrayList<Number>();
        ints.add(Long.valueOf(1));
        assertThrows(TypeError.class, () -> DartDoubleList.fromDoubles(ints));
        assertEquals(1L, DartLongList.fromLongs(ints).getLong(0));
    }

    @Test
    public void forEachRefusesALengthChange() {
        final DartList<Long> list = new DartList<Long>();
        list.add(1L);
        list.add(2L);
        assertThrows(ConcurrentModificationError.class, () -> list.forEachDart(e -> list.add(3L)));
    }

    @Test
    public void queryParametersAreReadOnly() {
        DartMap<String, String> q = DartUri.parse("https://x/?a=1").queryParameters();
        assertEquals("1", q.get("a"));
        assertThrows(UnsupportedError.class, () -> q.put("b", "2"));
        assertThrows(UnsupportedError.class, () -> q.idxSet("b", "2"));
        assertThrows(UnsupportedError.class, () -> q.remove("a"));
        assertThrows(UnsupportedError.class, q::clear);
        assertThrows(UnsupportedOperationException.class, () -> q.keySet().clear());
        assertEquals(1, q.size());
    }

    @Test
    public void unmodifiableMapRefusesEveryMutatorWithoutRunningItsCallback() {
        // Regression: Map.unmodifiable's mutators -- including the ones with a
        // callback argument -- throw UnsupportedError up front on the Dart 3.9 VM,
        // even when the callback would never need to run (the key is already
        // present for putIfAbsent, the map merged in by addAll/addEntries is
        // empty). DartUnmodifiableMap only overrode put/putAll/remove/clear, so
        // two classes of mutator slipped through: DartMap's own extensions
        // (putIfAbsentDart, update) that check the map before ever calling the
        // overridden put, and java.util.HashMap's own putIfAbsent/compute*/merge/
        // replace*/replaceAll, which mutate its table directly rather than
        // calling put()/remove() -- confirmed by a probe subclass that overrides
        // put/remove to record every call: none of those nine HashMap methods
        // triggers either override.
        DartMap<String, String> q = DartUri.parse("https://x/?a=1").queryParameters();
        boolean[] ran = {false};

        assertThrows(UnsupportedError.class,
                () -> q.putIfAbsentDart("a", () -> {
                    ran[0] = true;
                    return "ignored";
                }),
                "putIfAbsentDart on an EXISTING key used to return the stored value "
                        + "instead of throwing");
        assertFalse(ran[0], "the ifAbsent callback must not run");

        assertThrows(UnsupportedError.class,
                () -> q.putIfAbsentDart("new-key", () -> {
                    ran[0] = true;
                    return "ignored";
                }));
        assertFalse(ran[0], "the ifAbsent callback must not run");

        assertThrows(UnsupportedError.class,
                () -> q.update("a", v -> {
                    ran[0] = true;
                    return v;
                }, null),
                "update used to run its callback before the put() it fed into failed");
        assertFalse(ran[0], "the update callback must not run");

        assertThrows(UnsupportedError.class, () -> q.removeDart("a"));
        assertThrows(UnsupportedError.class, () -> q.addAll(new java.util.HashMap<>()),
                "addAll of an EMPTY map must still throw");
        assertThrows(UnsupportedError.class,
                () -> q.addEntries(java.util.Collections.emptyList()),
                "addEntries of an EMPTY iterable must still throw");

        // java.util.Map's own mutators -- HashMap bypasses put()/remove() for all
        // of these, so each needs its own override rather than inheriting safety
        // from put()/remove() being guarded.
        assertThrows(UnsupportedError.class, () -> q.putIfAbsent("a", "x"));
        assertThrows(UnsupportedError.class, () -> q.putIfAbsent("new-key", "x"));
        assertThrows(UnsupportedError.class, () -> q.remove("a", "1"));
        assertThrows(UnsupportedError.class, () -> q.replace("a", "1", "2"));
        assertThrows(UnsupportedError.class, () -> q.replace("a", "2"));
        assertThrows(UnsupportedError.class, () -> q.computeIfAbsent("new-key", k -> {
            ran[0] = true;
            return "x";
        }));
        assertFalse(ran[0], "the computeIfAbsent callback must not run");
        assertThrows(UnsupportedError.class, () -> q.computeIfPresent("a", (k, v) -> {
            ran[0] = true;
            return v;
        }));
        assertFalse(ran[0], "the computeIfPresent callback must not run");
        assertThrows(UnsupportedError.class, () -> q.compute("a", (k, v) -> {
            ran[0] = true;
            return v;
        }));
        assertFalse(ran[0], "the compute callback must not run");
        assertThrows(UnsupportedError.class, () -> q.merge("a", "1", (v1, v2) -> {
            ran[0] = true;
            return v1;
        }));
        assertFalse(ran[0], "the merge callback must not run");
        assertThrows(UnsupportedError.class, () -> q.replaceAll((k, v) -> {
            ran[0] = true;
            return v;
        }));
        assertFalse(ran[0], "the replaceAll callback must not run");

        // Still readable, and nothing above touched it.
        assertEquals("1", q.get("a"));
        assertEquals(1, q.size());
    }

    @Test
    public void dateTimeRangesCompareByTheirEndpoints() {
        DateTime a = DateTime.utc(2024, 1, 1, 0, 0, 0, 0, 0);
        DateTime b = DateTime.utc(2024, 1, 9, 0, 0, 0, 0, 0);
        assertEquals(new DateTimeRange(a, b), new DateTimeRange(a, b));
        assertEquals(new DateTimeRange(a, b).hashCode(), new DateTimeRange(a, b).hashCode());
        assertFalse(new DateTimeRange(a, b).equals(new DateTimeRange(a, a)));
    }

    @Test
    public void containsHonoursItsStartIndex() {
        assertFalse(DString.contains("abc", "a", 1));
        assertTrue(DString.contains("abc", "c", 1));
        assertFalse(DString.contains("abc", new RegExp("a"), 1));
        assertThrows(RangeError.class, () -> DString.contains("abc", "a", 4));
    }

    @Test
    public void hashAllHashesTheElements() {
        DartList<Object> one = new DartList<Object>();
        one.add(1L);
        one.add("x");
        DartList<Object> two = new DartList<Object>();
        two.add(1L);
        two.add("x");
        assertEquals(dart.runtime.DartRuntime.hashAll(one), dart.runtime.DartRuntime.hashAll(two));
    }

    @Test
    public void aMalformedRegExpThrowsFromItsConstructor() {
        assertThrows(FormatException.class, () -> new RegExp("["));
        assertThrows(FormatException.class, () -> new RegExp("[", false, true, false, false));
        assertTrue(new RegExp("a+").hasMatch("caab"));
    }

    @Test
    public void iterableMixinThrowsDartErrors() {
        dart.collection.IterableMixin<Object> empty = new dart.collection.IterableMixin<Object>() {
            @Override
            public dart.collection.Iterator<Object> iterator() {
                return new dart.collection.Iterator<Object>() {
                    @Override
                    public boolean moveNext() {
                        return false;
                    }

                    @Override
                    public Object current() {
                        return null;
                    }
                };
            }
        };
        assertThrows(StateError.class, empty::first);
        assertThrows(StateError.class, empty::last);
        assertThrows(StateError.class, empty::single);
        assertThrows(RangeError.class, () -> empty.elementAt(0));
    }

    @Test
    public void aCancelledHeadlessTimerEndsItsThread() throws Exception {
        dart.async.Timer t = new dart.async.Timer(Duration.of(0, 1, 0, 0, 0, 0), null);
        t.cancel();
        long deadline = System.currentTimeMillis() + 2000;
        while (liveTimerThreads() > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(0, liveTimerThreads(), "cancel ends the hour-long sleep instead of waiting it out");
        assertFalse(t.isActive());
    }

    private static int liveTimerThreads() {
        int n = 0;
        for (Thread th : Thread.getAllStackTraces().keySet()) {
            if ("dart-timer".equals(th.getName()) && th.isAlive()) {
                n++;
            }
        }
        return n;
    }

    @Test
    public void aThrownValueIsCarriedAndCaughtAsItself() {
        Object token = new Object();
        RuntimeException carried = dart.runtime.DartRuntime.asError(token);
        assertTrue(carried instanceof DartThrown);
        assertTrue(dart.runtime.DartRuntime.caught(carried) == token, "the same object comes back");
        assertEquals("boom", dart.runtime.DartRuntime.asError("boom").toString());
        assertFalse(dart.runtime.DartRuntime.isDartError(carried), "a thrown string is not an Error");
        assertFalse(dart.runtime.DartRuntime.isDartException(carried), "nor an Exception");
        assertTrue(dart.runtime.DartRuntime.isDartException(new FormatException("f")));
        assertTrue(dart.runtime.DartRuntime.isDartError(new StateError("s")));
        assertThrows(StackOverflowError.class, () -> dart.runtime.DartRuntime.asError(new StackOverflowError()),
                "an Error is thrown as itself");
    }

    @Test
    public void unmodifiableListsRefuseEveryWrite() {
        DartList<Long> source = new DartList<Long>();
        source.add(1L);
        DartList<Long> ro = DartList.unmodifiable(source);
        assertThrows(UnsupportedError.class, () -> ro.set(0, 9L));
        assertThrows(UnsupportedError.class, () -> ro.add(2L));
        assertThrows(UnsupportedError.class, ro::clear);
        DartList<String> segments = DartUri.parse("https://x/a/b").pathSegments();
        assertThrows(UnsupportedError.class, () -> segments.add("c"));
        assertEquals("a", segments.get(0));
    }

    @Test
    public void anIntMapChangedDuringForEachThrowsDartsError() {
        final DartLongMap m = DartLongMap.ofLongs(1, 1);
        assertThrows(ConcurrentModificationError.class, () -> m.forEachDart((k, v) -> m.put(k + 10, v)));
    }

    @Test
    public void identicalTreatsNumbersAsValues() {
        assertTrue(dart.runtime.DartRuntime.identical(Long.valueOf(100000), Long.valueOf(100000)));
        assertTrue(dart.runtime.DartRuntime.identical(Double.valueOf(0.5), Double.valueOf(0.5)));
        assertFalse(dart.runtime.DartRuntime.identical(Double.valueOf(0.0), Double.valueOf(-0.0)));
        assertFalse(dart.runtime.DartRuntime.identical(new StringBuilder("a").toString(), "a"));
    }

    @Test
    public void hashesAgreeWithDartEqualityAcrossIntAndDouble() {
        assertEquals(dart.runtime.DartRuntime.hashOf(Long.valueOf(1)), dart.runtime.DartRuntime.hashOf(Double.valueOf(1.0)));
        assertEquals(dart.runtime.DartRuntime.hashOf(Double.valueOf(0.0)), dart.runtime.DartRuntime.hashOf(Double.valueOf(-0.0)));
        assertEquals(dart.runtime.DartRuntime.hash(Long.valueOf(2), "x"), dart.runtime.DartRuntime.hash(Double.valueOf(2.0), "x"));
    }

    @Test
    public void typedCopiesCheckTheirElements() {
        java.util.List<Object> mixed = new java.util.ArrayList<Object>();
        mixed.add(Long.valueOf(1));
        assertThrows(TypeError.class, () -> DartList.fromChecked(mixed, String.class, false, true));
        java.util.List<Object> strings = new java.util.ArrayList<Object>();
        strings.add("a");
        strings.add(null);
        assertThrows(TypeError.class, () -> DartList.fromChecked(strings, String.class, false, true),
                "null is not a String");
        assertEquals(2, DartList.fromChecked(strings, String.class, true, true).size());
        java.util.List<Object> notInts = new java.util.ArrayList<Object>();
        notInts.add("x");
        assertThrows(TypeError.class, () -> dart.typed_data.Uint8List.fromList(notInts));
    }

    @Test
    public void byteDataOffsetsNearTheLimitAreRangeErrors() {
        dart.typed_data.ByteData bd = new dart.typed_data.ByteData(8);
        assertThrows(RangeError.class, () -> bd.getInt32(Long.MAX_VALUE));
        assertThrows(RangeError.class, () -> bd.getInt32(5));
    }

    @Test
    public void intParsingTrimsDartWhitespace() {
        assertEquals(Long.valueOf(123), DString.tryParseInt("\u00A0123\u00A0"));
    }

    @Test
    @SuppressWarnings({"UnnecessaryBoxing", "removal"})
    public void identityCollectionsTreatEqualIntsAsOneKey() {
        Long a = new Long(1000);
        Long b = new Long(1000);
        assertFalse(a == b, "two separate boxes");
        DartIdentityMap<Object, String> m = new DartIdentityMap<Object, String>();
        m.put(a, "x");
        m.put(b, "y");
        assertEquals(1, m.size());
        assertEquals("y", m.get(a));
        DartIdentitySet<Object> set = new DartIdentitySet<Object>();
        set.add(a);
        set.add(b);
        assertEquals(1, set.size());
    }

    @Test
    public void aSameSizeSwapDuringForEachIsStillAModification() {
        final DartMap<Long, String> m = new DartMap<Long, String>();
        m.put(1L, "a");
        assertThrows(ConcurrentModificationError.class, () -> m.forEachDart((k, v) -> {
            m.remove(k);
            m.put(2L, "b");
        }));
        final DartSet<Long> s = new DartSet<Long>();
        s.add(1L);
        assertThrows(ConcurrentModificationError.class, () -> s.forEachDart(e -> {
            s.remove(e);
            s.add(2L);
        }));
        final DartMap<Long, String> updating = new DartMap<Long, String>();
        updating.put(1L, "a");
        updating.forEachDart((k, v) -> updating.put(k, "b"));
        assertEquals("b", updating.get(1L), "updating a value is not structural");
    }

    // --- int.parse: unsigned 0x literals and ASCII-only digits -----------------

    @Test
    public void anUnsignedHexLiteralParsesAsTwosComplementBits() {
        // Recorded from dart 3.9.3.
        assertEquals(Long.valueOf(-1L), DString.tryParseInt("0xffffffffffffffff"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), DString.tryParseInt("0x8000000000000000"));
        assertEquals(Long.valueOf(-2L), DString.tryParseInt("0x0000fffffffffffffffe"));
        assertEquals(Long.valueOf(-1L), DString.tryParseInt("+0XFFFFFFFFFFFFFFFF"));
        assertEquals(Long.valueOf(Long.MAX_VALUE), DString.tryParseInt("0x7fffffffffffffff"));
        assertNull(DString.tryParseInt("0x10000000000000000"));
        assertEquals(-1L, DString.parseInt("0xffffffffffffffff"));
    }

    @Test
    public void aNegativeHexLiteralKeepsTheSignedRange() {
        assertEquals(Long.valueOf(Long.MIN_VALUE), DString.tryParseInt("-0x8000000000000000"));
        assertEquals(Long.valueOf(-1L), DString.tryParseInt("-0x1"));
        assertNull(DString.tryParseInt("-0x8000000000000001"));
        assertNull(DString.tryParseInt("-0xffffffffffffffff"));
        assertThrows(FormatException.class, () -> DString.parseInt("-0xffffffffffffffff"));
        // An explicit radix takes no 0x and keeps the signed range.
        assertNull(DString.tryParseInt("8000000000000000", 16));
        assertEquals(Long.valueOf(Long.MIN_VALUE), DString.tryParseInt("-8000000000000000", 16));
    }

    @Test
    public void onlyAsciiDigitsParse() {
        assertNull(DString.tryParseInt("\u0663"));
        assertNull(DString.tryParseInt("1\u0663"));
        assertNull(DString.tryParseInt("0x\u0663"));
        assertNull(DString.tryParseInt("\u0663", 10));
        assertEquals(Long.valueOf(35L), DString.tryParseInt("Z", 36));
    }

    // --- List.reversed is Dart's ListIterator ------------------------------------

    @Test
    public void reversedThrowsWhenTheListChangesLengthMidLoop() {
        final DartList<Long> grow = new DartList<Long>();
        grow.add(1L);
        grow.add(2L);
        assertThrows(ConcurrentModificationError.class, () -> {
            for (Long v : grow.reversed()) {
                grow.add(v);
            }
        });
        final DartList<Long> shrink = new DartList<Long>();
        shrink.add(1L);
        shrink.add(2L);
        shrink.add(3L);
        // Shrinking used to read past the new end instead.
        assertThrows(ConcurrentModificationError.class, () -> {
            for (Long v : shrink.reversed()) {
                shrink.removeLast();
            }
        });
        DartList<Long> same = new DartList<Long>();
        same.add(1L);
        same.add(2L);
        StringBuilder seen = new StringBuilder();
        for (Long v : same.reversed()) {
            same.set(0, v);
            seen.append(v);
        }
        assertEquals("22", seen.toString(), "an element write is not a length change");
    }

    // --- Uri reads a port as int.parse does --------------------------------------

    @Test
    public void aSignedOrHexPortIsAcceptedAsDartAcceptsIt() {
        // Recorded from dart 3.9.3, which takes these rather than rejecting them.
        DartUri plus = DartUri.parse("http://a.com:+80/");
        assertEquals(80L, plus.port());
        assertEquals("http://a.com/", plus.toString());
        DartUri minus = DartUri.tryParse("http://a.com:-1/");
        assertEquals(-1L, minus.port());
        assertEquals("http://a.com:-1/", minus.toString());
        assertEquals("http://a.com:16/", DartUri.parse("http://a.com:0x10/").toString());
        assertEquals("foo://a.com:5/", DartUri.parse("foo://a.com:+5/").toString());
        assertEquals(80L, DartUri.parse("http://[::1]:+0x50/").port());
    }

    @Test
    public void aPortThatIsNotAnIntIsRejected() {
        assertNull(DartUri.tryParse("http://a.com:+/"));
        assertNull(DartUri.tryParse("http://a.com:-/"));
        assertNull(DartUri.tryParse("http://a.com:+-1/"));
        assertNull(DartUri.tryParse("http://a.com:\u0663/"));
        assertThrows(FormatException.class, () -> DartUri.parse("http://a.com:+/"));
    }

    @Test
    public void anOversizedPortParsesButReadingItThrows() {
        // dart 3.9.3: a decimal port past 2^63-1 in an otherwise plain URI parses, keeps
        // its text, and only Uri.port throws.
        String text = "http://a.com:99999999999999999999/";
        DartUri u = DartUri.tryParse(text);
        assertTrue(u != null);
        assertEquals(text, u.toString());
        assertEquals(DartUri.parse(text), u);
        assertEquals(DartUri.parse(text).hashCode(), u.hashCode());
        assertEquals("a.com", u.host());
        FormatException e = assertThrows(FormatException.class, u::port);
        assertEquals("FormatException: Positive input exceeds the limit of integer\n99999999999999999999",
                e.toString());
        assertTrue(DartUri.tryParse("foo://a.com:9223372036854775808") != null);
        assertTrue(DartUri.tryParse("http://a.com:99999999999999999999?q") != null);
        // A URI Dart has to normalise goes through int.parse instead, and fails.
        assertNull(DartUri.tryParse("http://A.com:99999999999999999999/"));
        assertNull(DartUri.tryParse("HTTP://a.com:99999999999999999999/"));
        assertNull(DartUri.tryParse("http://u@a.com:99999999999999999999/"));
        assertNull(DartUri.tryParse("http://[::1]:99999999999999999999/"));
        assertNull(DartUri.tryParse("http://a.com:99999999999999999999/%7e"));
        assertNull(DartUri.tryParse("http://a.com:99999999999999999999/x/../y"));
        assertNull(DartUri.tryParse("http://a.com:099999999999999999999/"));
        assertNull(DartUri.tryParse("http://a.com:-99999999999999999999/"));
        assertNull(DartUri.tryParse("http://a.com:0x10000000000000000/"));
        assertThrows(FormatException.class, () -> DartUri.parse("http://A.com:99999999999999999999/"));
    }

    // --- RegExpMatch checks a group index as the Dart int it is -----------------

    @Test
    public void aGroupIndexPastIntRangeIsARangeErrorNotGroupZero() {
        RegExpMatch m = new RegExp("(a)").firstMatch("xa");
        assertEquals("a", m.group(0));
        // (int) (1L << 32) is 0: narrowed before the check, this answered group 0.
        assertThrows(RangeError.class, () -> m.group(1L << 32));
        assertThrows(RangeError.class, () -> m.idx((1L << 32) + 1));
        assertThrows(RangeError.class, () -> m.group(-1L));
        assertThrows(RangeError.class, () -> m.group(2L));
    }

    // --- RegExpMatch's groupCount is the PATTERN's declared count ---------------

    @Test
    public void groupCountIsDeclaredNotJustWhatTheMatchReached() {
        // "(a)|(b)" matching "a" only ever opens group 1; group 2 never runs.
        // Dart (verified against `dart run` on the installed Flutter SDK):
        // groupCount answers 2, group(1) is "a", group(2) is null (in range,
        // just unmatched), and group(3) -- past the pattern's own two groups --
        // throws RangeError.
        RegExpMatch m = new RegExp("(a)|(b)").firstMatch("a");
        assertEquals(2, m.groupCount());
        assertEquals("a", m.group(1));
        assertNull(m.group(2));
        assertThrows(RangeError.class, () -> m.group(3));

        // Three alternatives, only the last one taken: groups 1 and 2 are both
        // in range and both unmatched.
        RegExpMatch m2 = new RegExp("(a)|(b)|(c)").firstMatch("c");
        assertEquals(3, m2.groupCount());
        assertNull(m2.group(1));
        assertNull(m2.group(2));
        assertEquals("c", m2.group(3));

        // A non-capturing group and a character-class paren are not groups.
        RegExpMatch m3 = new RegExp("(?:a)(b)[(]").firstMatch("ab(");
        assertEquals(1, m3.groupCount());
        assertEquals("b", m3.group(1));

        // An escaped paren is not a group either.
        RegExpMatch m4 = new RegExp("\\(a\\)(b)").firstMatch("(a)b");
        assertEquals(1, m4.groupCount());
        assertEquals("b", m4.group(1));
    }

    // --- The declared-group-count parser itself, incl. forms RE cannot run ------

    @Test
    public void countCapturingGroupsSkipsNonCapturingFormsAndCountsNamedGroups() {
        // com.codename1.util.regex.RE has no support for lookaround or named
        // groups at all -- a pattern using them never compiles, so these
        // forms can only be exercised against the parser directly, not
        // through a real RegExp match.
        assertEquals(0, RegExp.countCapturingGroups(""));
        assertEquals(0, RegExp.countCapturingGroups("abc"));
        assertEquals(2, RegExp.countCapturingGroups("(a)|(b)"));
        assertEquals(0, RegExp.countCapturingGroups("(?:a)"));
        assertEquals(1, RegExp.countCapturingGroups("(?:a)(b)"));
        assertEquals(0, RegExp.countCapturingGroups("(?=a)"));
        assertEquals(0, RegExp.countCapturingGroups("(?!a)"));
        assertEquals(0, RegExp.countCapturingGroups("(?<=a)"));
        assertEquals(0, RegExp.countCapturingGroups("(?<!a)"));
        assertEquals(1, RegExp.countCapturingGroups("(?=a)(b)(?!c)(?<=d)(?<!e)"));
        // A named group still captures -- its '<' is followed by a name, not '=' or '!'.
        assertEquals(1, RegExp.countCapturingGroups("(?<name>a)"));
        assertEquals(2, RegExp.countCapturingGroups("(?<name>a)(b)"));
        // Parens inside a character class are literal, not groups.
        assertEquals(0, RegExp.countCapturingGroups("[()]"));
        assertEquals(1, RegExp.countCapturingGroups("[()](a)"));
        // An escaped paren, in or out of a class, is not a group.
        assertEquals(0, RegExp.countCapturingGroups("\\(a\\)"));
        assertEquals(1, RegExp.countCapturingGroups("\\(a\\)(b)"));
        assertEquals(0, RegExp.countCapturingGroups("[\\]()]"));
    }

    // --- const collection literals refuse every write ---------------------------

    @Test
    public void aConstListRefusesWritesAndKeepsItsClass() {
        DartList<String> xs = DartList.constant(DartList.of("a", "b"));
        assertThrows(UnsupportedError.class, () -> xs.add("c"));
        assertThrows(UnsupportedError.class, () -> xs.set(0, "z"));
        assertThrows(UnsupportedError.class, () -> xs.removeAt(0));
        assertThrows(UnsupportedError.class, () -> xs.sortDefault());
        assertEquals("[a, b]", xs.toString());
        DartLongList ints = DartList.constant(DartLongList.ofLongs(1, 2));
        assertThrows(UnsupportedError.class, () -> ints.addLong(3));
        assertThrows(UnsupportedError.class, () -> ints.setLong(0, 9));
        assertEquals(2L, ints.getLong(1));
    }

    @Test
    public void aConstListRefusesEmptyBulkMutationsToo() {
        // The same hole as aConstSetRefusesEmptyBulkMutationsToo, in DartList: a
        // mutator whose own check sits only inside a per-element call (set() inside
        // sort()'s final loop, add() inside the inherited AbstractCollection.addAll)
        // never reaches it when the list is already empty / the argument is empty,
        // so these used to silently no-op on a const (unmodifiable) empty list
        // instead of throwing -- recorded from the real Dart SDK, where an
        // unmodifiable list's clear()/sort()/addAll() always throw regardless of
        // whether anything would actually change.
        DartList<String> xs = DartList.constant(DartList.<String>of());
        assertThrows(UnsupportedError.class, xs::clear);
        assertThrows(UnsupportedError.class, xs::sortDefault);
        assertThrows(UnsupportedError.class, () -> xs.addAll(java.util.Collections.<String>emptyList()));
        assertThrows(UnsupportedError.class, () -> xs.addAll(0, java.util.Collections.<String>emptyList()));
        assertEquals(0, xs.size(), "none of the refused calls may have mutated the list");
    }

    @Test
    public void aFixedLengthEmptyListDistinguishesLengthFromElementChecks() {
        // List.filled(0, v) (growable: false by default) is fixed-length but NOT
        // unmodifiable. Dart throws on it for a length-changing op (clear, addAll --
        // even with nothing to add) but NOT for a pure element-writing op (sort),
        // since a fixed length does not forbid overwriting the elements it has --
        // there just happen to be none. Recorded from the real Dart SDK.
        DartList<String> fixed = DartList.filled(0, "x");
        assertThrows(UnsupportedError.class, fixed::clear);
        assertThrows(UnsupportedError.class, () -> fixed.addAll(java.util.Collections.<String>emptyList()));
        assertThrows(UnsupportedError.class, () -> fixed.addAll(0, java.util.Collections.<String>emptyList()));
        fixed.sortDefault();
        assertEquals(0, fixed.size());
    }

    @Test
    public void aConstSetRefusesWrites() {
        DartSet<String> s = DartSet.constant(DartSet.of("a", "b"));
        assertThrows(UnsupportedError.class, () -> s.add("c"));
        assertThrows(UnsupportedError.class, () -> s.removeValue("a"));
        assertThrows(UnsupportedError.class, () -> s.clear());
        assertThrows(UnsupportedError.class, () -> s.removeWhere(e -> true));
        assertThrows(UnsupportedError.class, () -> s.retainWhere(e -> false));
        assertTrue(s.contains("a"));
        assertEquals(2, s.size());
        // An ordinary set is untouched by the flag.
        DartSet<String> plain = DartSet.of("a");
        plain.add("b");
        plain.removeWhere(e -> e.equals("a"));
        assertEquals("{b}", plain.toString());
    }

    @Test
    public void aConstSetRefusesEmptyBulkMutationsToo() {
        // A bulk mutator that loops over its argument and calls add()/remove() per
        // element never reaches either when the argument is empty, so checking
        // writability only inside that single-element call let addAll([]) etc. through
        // silently instead of throwing -- recorded from the real Dart SDK, where a
        // const set's addAll/removeAll/retainAll always throw regardless of whether
        // anything would actually change.
        DartSet<String> s = DartSet.constant(DartSet.of("a", "b"));
        assertThrows(UnsupportedError.class, () -> s.addAllIterable(java.util.Collections.<String>emptyList()));
        assertThrows(UnsupportedError.class, () -> s.addAll(java.util.Collections.<String>emptyList()));
        assertThrows(UnsupportedError.class, () -> s.removeAll((Iterable<?>) java.util.Collections.emptyList()));
        assertThrows(UnsupportedError.class, () -> s.removeAll((java.util.Collection<?>) java.util.Collections.emptyList()));
        assertThrows(UnsupportedError.class, () -> s.retainAll((Iterable<?>) s));
        assertThrows(UnsupportedError.class, () -> s.retainAll((java.util.Collection<?>) s));
        assertEquals(2, s.size(), "none of the refused calls may have mutated the set");
    }

    @Test
    public void aConstMapRefusesWrites() {
        DartMap<String, Long> m = DartMap.constant(DartMap.<String, Long>of("a", 1L));
        assertThrows(UnsupportedError.class, () -> m.idxSet("b", 2L));
        assertThrows(UnsupportedError.class, () -> m.removeDart("a"));
        assertThrows(UnsupportedError.class, () -> m.clear());
        assertEquals(Long.valueOf(1L), m.idx("a"));
        DartLongMap ints = DartLongMap.constant(DartLongMap.ofLongs(1, 2));
        assertThrows(UnsupportedError.class, () -> ints.putLong(3, 4));
        assertThrows(UnsupportedError.class, () -> ints.removeDart(1));
        assertThrows(UnsupportedError.class, () -> ints.removeDart(99));
        assertThrows(UnsupportedError.class, () -> ints.clear());
        assertEquals(2L, ints.getLongOr(1, 0));
    }
}
