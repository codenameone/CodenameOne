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
        assertEquals("STRA\u00dfE", DString.toUpperCase("stra\u00dfe"), "Dart leaves the sharp s alone");
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

    @Test
    public void aListModifiedWhileIteratedFailsInsteadOfLooping() {
        DartList<Object> l = new DartList<Object>();
        l.add("a");
        l.add("b");
        assertThrows(java.util.ConcurrentModificationException.class, () -> {
            for (Object x : l) {
                l.add(x);
            }
        });
        DartLongList longs = DartLongList.ofLongs(1, 2);
        assertThrows(java.util.ConcurrentModificationException.class, () -> {
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
        assertThrows(java.util.ConcurrentModificationException.class,
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
}
