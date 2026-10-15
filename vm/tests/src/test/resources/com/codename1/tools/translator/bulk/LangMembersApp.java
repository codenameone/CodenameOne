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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/// The java.lang / java.util members JavaAPI gained from compat-jdk, run on
/// HotSpot (the JDK's own classes) and on ParparVM; the RESULT lines must
/// match. Every case answers a string, or the class and message of what it
/// threw, so a difference names the member. Java 17 API only.
public class LangMembersApp {

    static final int CASES = 140;

    public static void fixtureMain(String[] args) throws Exception {
        StringBuilder all = new StringBuilder();
        for (int i = 0; i < CASES; i++) {
            String v;
            try {
                v = run(i);
            } catch (Throwable t) {
                v = "!" + t.getClass().getName() + ":" + t.getMessage();
            }
            if (v == null) {
                continue;
            }
            System.out.println("case " + i + " = " + v);
            all.append(i).append('=').append(v).append('|');
        }
        System.out.println("RESULT=" + all);
    }

    static String bits(double d) {
        return Long.toHexString(Double.doubleToLongBits(d));
    }

    static String bits(float f) {
        return Integer.toHexString(Float.floatToIntBits(f));
    }

    // Results that are not bit-exact by design: compared to 1e-12 relative.
    static String approx(double d) {
        return String.valueOf(Math.round(d * 1e12));
    }

    static String cps(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(Integer.toHexString(s.charAt(i))).append(',');
        }
        return sb.toString();
    }

    static String run(int i) throws Exception {
        String em = "" + (char) 0x2003;
        String smile = new String(new int[] {0x1F600}, 0, 1);
        switch (i) {
            // ---- String
            case 0: return "[" + (em + " a b " + em).strip() + "]";
            case 1: return "[" + ("  a b  ").stripLeading() + "]";
            case 2: return "[" + ("  a b  " + em).stripTrailing() + "]";
            case 3: return "" + "   ".isBlank() + ("  " + em).isBlank() + " x ".isBlank();
            case 4: return "ab".repeat(3) + "/" + "ab".repeat(0).length();
            case 5: return "ab".repeat(-1);
            case 6: return cps("a\nb".indent(2));
            case 7: return cps("   a\n  b\r\n".indent(-1));
            case 8: return cps("    a\n      b\n    ".stripIndent());
            case 9: return cps("a\\tb\\n\\101\\s\\\\".translateEscapes());
            case 10: return "a\\qb".translateEscapes();
            case 11: return "" + smile.length() + smile.codePointAt(0) + "/" + ("a" + smile).codePointCount(0, 3);
            case 12: return "" + ("a" + smile + "b").offsetByCodePoints(0, 2) + "/" + (smile + "x").codePointBefore(2);
            case 13: return "" + "a\nb\r\nc\rd".lines().count();
            case 14: return "%d-%s".formatted(3, "x");
            case 15: return "abc".transform(new Function<String, String>() {
                public String apply(String s) {
                    return s + s.length();
                }
            });
            case 16: return String.join("-", "a", "b", "c") + "/" + String.join(",", java.util.Arrays.asList("x", "y"));
            case 17: return cps(new String(new int[] {0x41, 0x1F600, 0x42}, 1, 2));
            case 18: return new String(new int[] {-1}, 0, 1);
            case 19: return cps(new String("h\u00e9llo".getBytes(StandardCharsets.UTF_8), 1, 2, StandardCharsets.UTF_8));
            // ---- Integer
            case 20: return Integer.parseUnsignedInt("4294967295") + "/" + Integer.toUnsignedString(-1) + "/" + Integer.toUnsignedString(-1, 16);
            case 21: return Integer.divideUnsigned(-1, 3) + "/" + Integer.remainderUnsigned(-1, 3) + "/" + Integer.compareUnsigned(-1, 1) + "/" + Integer.toUnsignedLong(-2);
            case 22: return Integer.decode("-0x1F") + "/" + Integer.decode("#ff") + "/" + Integer.decode("010") + "/" + Integer.decode("-2147483648");
            case 23: return "" + Integer.decode("+-1");
            case 24: return "" + Integer.decode("");
            case 25: return "" + Integer.decode("0x80000000");
            case 26: return Integer.toHexString(Integer.reverse(0x12345678)) + "/" + Integer.toHexString(Integer.reverseBytes(0x12345678));
            case 27: return Integer.bitCount(0xF0F0) + "/" + Integer.highestOneBit(100) + "/" + Integer.lowestOneBit(100) + "/" + Integer.numberOfTrailingZeros(64) + "/" + Integer.rotateRight(1, 1);
            case 28: return "" + Integer.parseInt("x123y", 1, 4, 10) + "/" + Integer.parseUnsignedInt("ffffffff", 16);
            case 29: return "" + Integer.parseUnsignedInt("4294967296");
            case 30: return "" + Integer.divideUnsigned(5, 0);
            case 31: return Integer.hashCode(-7) + "/" + Integer.BYTES + "/" + new Integer("12");
            // ---- Long
            case 32: return Long.parseUnsignedLong("18446744073709551615") + "/" + Long.toUnsignedString(-1L) + "/" + Long.toUnsignedString(-1L, 36);
            case 33: return Long.divideUnsigned(-1L, 10) + "/" + Long.remainderUnsigned(-1L, 10) + "/" + Long.compareUnsigned(-1L, 1L);
            case 34: return Long.toHexString(-255L) + "/" + Long.toBinaryString(5L) + "/" + Long.toOctalString(-1L);
            case 35: return Long.decode("-0x8000000000000000") + "/" + Long.decode("0777") + "/" + Long.valueOf("-42") + "/" + Long.valueOf("zz", 36);
            case 36: return "" + Long.parseUnsignedLong("18446744073709551616");
            case 37: return Long.bitCount(-1L) + "/" + Long.numberOfLeadingZeros(1L) + "/" + Long.numberOfTrailingZeros(0L) + "/" + Long.signum(-9L);
            case 38: return Long.toHexString(Long.reverse(1L)) + "/" + Long.toHexString(Long.reverseBytes(0x0102030405060708L)) + "/" + Long.rotateLeft(1L, 65);
            case 39: return Long.highestOneBit(-1L) + "/" + Long.lowestOneBit(12L) + "/" + Long.hashCode(-1L) + "/" + Long.parseLong("x-77", 1, 4, 10);
            // ---- Short / Byte
            case 40: return Short.decode("0x7fff") + "/" + Short.toUnsignedInt((short) -1) + "/" + Integer.toHexString(Short.reverseBytes((short) 0x1234));
            case 41: return "" + Short.decode("0x8000");
            case 42: return Byte.decode("-0x80") + "/" + Byte.toUnsignedInt((byte) -1) + "/" + Byte.compare((byte) 1, (byte) -1) + "/" + Byte.compareUnsigned((byte) 1, (byte) -1);
            case 43: return "" + Byte.valueOf("128");
            case 44: return Short.valueOf("-7", 8) + "/" + Short.compareUnsigned((short) -1, (short) 1) + "/" + Short.hashCode((short) -3) + "/" + Byte.hashCode((byte) -3);
            // ---- Double / Float
            case 45: return Double.toHexString(1.0) + "/" + Double.toHexString(-0.0) + "/" + Double.toHexString(Double.MIN_VALUE) + "/" + Double.toHexString(0.1);
            case 46: return Double.toHexString(Double.MAX_VALUE) + "/" + Double.toHexString(Double.NaN) + "/" + Double.toHexString(Double.NEGATIVE_INFINITY) + "/" + Double.toHexString(Double.MIN_NORMAL);
            case 47: return Float.toHexString(1.5f) + "/" + Float.toHexString(Float.MIN_VALUE) + "/" + Float.toHexString(-3.25e-40f) + "/" + Float.toHexString(Float.MAX_VALUE);
            case 48: return "" + Double.isFinite(1e308) + Double.isFinite(Double.POSITIVE_INFINITY) + Float.isFinite(Float.NaN) + "/" + Double.hashCode(1.5) + "/" + Float.hashCode(1.5f);
            case 49: return bits(Double.MIN_NORMAL) + "/" + bits(Float.MIN_NORMAL) + "/" + Float.MAX_EXPONENT + Float.MIN_EXPONENT + Double.MAX_EXPONENT;
            // ---- Boolean
            case 50: return "" + Boolean.logicalXor(true, false) + Boolean.logicalAnd(true, false) + Boolean.logicalOr(false, false) + Boolean.compare(true, false) + Boolean.hashCode(true) + Boolean.hashCode(false) + Boolean.toString(true) + new Boolean("TRUE");
            // ---- Character
            case 51: return Character.getNumericValue('a') + "/" + Character.getNumericValue((char) 0x0663) + "/" + Character.getNumericValue('5') + "/" + Character.getNumericValue('%');
            case 52: return Character.digit((char) 0xFF21, 16) + "/" + Character.digit(0x0663, 10) + "/" + Character.digit('z', 36) + "/" + Character.digit('9', 8);
            case 53: return "" + Character.isDigit(0x0663) + Character.isDigit(0xFF10) + Character.isDigit('a');
            case 54: return Integer.toHexString(Character.toUpperCase(0xe9)) + "/" + Integer.toHexString(Character.toUpperCase(0xff)) + "/" + Integer.toHexString(Character.toUpperCase(0xb5)) + "/" + Integer.toHexString(Character.toUpperCase(0xf7));
            case 55: return "" + Character.isSpaceChar((char) 0xa0) + Character.isSpaceChar(0x2028) + Character.isSpaceChar('\t') + Character.isISOControl('\t') + Character.isISOControl(0x85) + Character.isISOControl('a');
            case 56: return "" + Character.forDigit(11, 16) + Character.forDigit(5, 10) + (int) Character.forDigit(10, 10) + (int) Character.forDigit(1, 99);
            case 57: return "" + Character.isAlphabetic('a') + Character.isAlphabetic('1') + Character.isIdeographic(0x4e00) + Character.isIdeographic('a');
            case 58: return Character.getType('A') + "/" + Character.getType('a') + "/" + Character.getType(' ') + "/" + Character.getType((char) 0x01c5) + "/" + Character.getType('1') + "/" + Character.getType(0x4e00);
            case 59: return "" + Character.isTitleCase((char) 0x01c5) + Character.isTitleCase('A') + Character.isMirrored('(') + Character.isMirrored('a');
            case 60: return "" + Character.isUnicodeIdentifierStart('a') + Character.isUnicodeIdentifierStart('1') + Character.isUnicodeIdentifierPart('1') + Character.isUnicodeIdentifierPart(' ');
            case 61: return Character.toString(0x1F600).length() + "/" + Character.toString('x') + "/" + Character.compare('a', 'b') + "/" + Character.hashCode('a') + "/" + Character.BYTES;
            case 62: return "" + Character.isDefined('a') + Character.isLetter(0x4e00) + Character.isLowerCase(0xe9) + Character.isUpperCase(0xc9) + Character.isBmpCodePoint(0x10000);
            case 63: return Character.toString(0x110000);
            // ---- Math
            case 64: return Math.floorDiv(-7, 2) + "/" + Math.floorMod(-7, 2) + "/" + Math.floorMod(7, -2) + "/" + Math.floorDiv(Integer.MIN_VALUE, -1) + "/" + Math.floorMod(-7L, 3) + "/" + Math.floorDiv(-7L, 2L);
            case 65: return "" + Math.floorDiv(1, 0);
            case 66: return "" + Math.addExact(Integer.MAX_VALUE, 1);
            case 67: return "" + Math.multiplyExact(Long.MAX_VALUE, 2L);
            case 68: return "" + Math.toIntExact(1L << 40);
            case 69: return "" + Math.negateExact(Integer.MIN_VALUE);
            case 70: return "" + Math.absExact(Integer.MIN_VALUE);
            case 71: return Math.multiplyHigh(Long.MAX_VALUE, Long.MAX_VALUE) + "/" + Math.multiplyFull(Integer.MAX_VALUE, Integer.MAX_VALUE) + "/" + Math.multiplyHigh(-3L, 5L);
            case 72: return Math.subtractExact(5L, 7L) + "/" + Math.incrementExact(4) + "/" + Math.decrementExact(-4L) + "/" + Math.multiplyExact(1L << 30, 2);
            case 73: return bits(Math.signum(-2.5)) + "/" + bits(Math.signum(0.0f)) + "/" + bits(Math.copySign(1.0, -0.0)) + "/" + bits(Math.copySign(2f, Float.NaN));
            case 74: return Math.getExponent(1024.0) + "/" + Math.getExponent(Double.MIN_VALUE) + "/" + Math.getExponent(0.5f) + "/" + Math.getExponent(Float.NaN);
            case 75: return bits(Math.ulp(1.0)) + "/" + bits(Math.ulp(Double.MAX_VALUE)) + "/" + bits(Math.ulp(0.0f)) + "/" + bits(Math.ulp(Double.POSITIVE_INFINITY));
            case 76: return bits(Math.nextUp(1.0)) + "/" + bits(Math.nextUp(-0.0)) + "/" + bits(Math.nextDown(0.0)) + "/" + bits(Math.nextDown(1.0f)) + "/" + bits(Math.nextUp(Float.MAX_VALUE));
            case 77: return bits(Math.nextAfter(1.0, 0)) + "/" + bits(Math.nextAfter(0.0, -1)) + "/" + bits(Math.nextAfter(1f, 2.0)) + "/" + bits(Math.nextAfter(-0.0, 0.0));
            case 78: return bits(Math.scalb(1.0, 1030)) + "/" + bits(Math.scalb(3.0, -1075)) + "/" + bits(Math.scalb(1.5f, -130)) + "/" + bits(Math.scalb(1.0, -1074));
            case 79: return bits(Math.rint(2.5)) + "/" + bits(Math.rint(-3.5)) + "/" + bits(Math.rint(-0.2)) + "/" + bits(Math.rint(1e300));
            case 80: return Math.round(0.49999999999999994) + "/" + Math.round(-2.5) + "/" + Math.round(2.5f) + "/" + Math.round(Double.NaN) + "/" + Math.round(1e20) + "/" + Math.round(-0.5f);
            case 81: return bits(Math.hypot(3, 4)) + "/" + bits(Math.hypot(Double.POSITIVE_INFINITY, Double.NaN)) + "/" + approx(Math.cbrt(27)) + "/" + approx(Math.cbrt(-2));
            case 82: return approx(Math.sinh(1)) + "/" + approx(Math.cosh(1)) + "/" + approx(Math.tanh(0.5)) + "/" + bits(Math.tanh(40)) + "/" + bits(Math.sinh(-0.0));
            case 83: return StrictMath.floorMod(-1, 5) + "/" + StrictMath.max(1, 2) + "/" + bits(StrictMath.abs(-0.0)) + "/" + StrictMath.round(1.5);
            case 84: return "" + Math.floorMod(Long.MIN_VALUE, -1L) + "/" + Math.floorDiv(Long.MIN_VALUE, -1L);
            // ---- Objects
            case 85: return Objects.requireNonNullElse(null, "d") + "/" + Objects.requireNonNullElse("o", "d");
            case 86: return "" + Objects.requireNonNullElse(null, null);
            case 87: return Objects.requireNonNullElseGet(null, new Supplier<String>() {
                public String get() {
                    return "got";
                }
            });
            case 88: return Objects.requireNonNullElseGet(null, new Supplier<String>() {
                public String get() {
                    return null;
                }
            });
            case 89: return "" + Objects.checkIndex(5, 3);
            case 90: return "" + Objects.checkFromToIndex(2, 1, 5);
            case 91: return "" + Objects.checkFromIndexSize(2, 4, 5);
            case 92: return "" + Objects.checkIndex(5L, 3L);
            case 93: return Objects.requireNonNull(null, new Supplier<String>() {
                public String get() {
                    return "made";
                }
            });
            case 94: return Objects.checkIndex(2, 3) + "/" + Objects.checkFromToIndex(1, 5, 5) + "/" + Objects.checkFromIndexSize(0, 0, 0);
            // ---- CharSequence / StringBuilder / StringBuffer
            case 95: {
                StringBuilder sb = new StringBuilder("ab");
                sb.appendCodePoint(0x1F600).appendCodePoint('c');
                return sb.length() + "/" + sb.codePointCount(0, sb.length()) + "/" + sb.codePointAt(2) + "/" + sb.codePointBefore(4) + "/" + sb.offsetByCodePoints(0, 3);
            }
            case 96: {
                StringBuilder sb = new StringBuilder("hello");
                return sb.compareTo(new StringBuilder("help")) + "/" + CharSequence.compare("a", sb) + "/" + sb.isEmpty() + new StringBuilder().isEmpty();
            }
            case 97: {
                StringBuilder sb = new StringBuilder("hello lo");
                return sb.indexOf("lo") + "/" + sb.indexOf("lo", 4) + "/" + sb.lastIndexOf("lo") + "/" + sb.lastIndexOf("lo", 5) + "/" + sb.substring(6) + "/" + sb.substring(1, 3);
            }
            case 98: return new StringBuilder("hello").replace(1, 3, "XY").replace(4, 99, "!").toString();
            case 99: {
                // The message changed between JDK 17 and 21; the class did not.
                try {
                    return new StringBuilder("hello").replace(6, 7, "x").toString();
                } catch (StringIndexOutOfBoundsException e) {
                    return "SIOOBE";
                }
            }
            case 100: return new StringBuilder().appendCodePoint(-1).toString();
            case 101: {
                StringBuffer sb = new StringBuffer((CharSequence) new StringBuilder("buf"));
                sb.appendCodePoint(0x1F600).append(new StringBuffer("!"));
                return sb.length() + "/" + sb.compareTo(new StringBuffer("bug")) + "/" + sb.indexOf("f") + "/" + sb.substring(2, 3) + "/" + cps(sb.replace(0, 1, "B").toString());
            }
            // ---- Locale
            case 102: return new Locale("IW", "il").toString() + "/" + new Locale("JI").getLanguage() + "/" + new Locale("in", "id").toLanguageTag();
            case 103: return new Locale("en", "US", "WIN").toString() + "/" + new Locale("en", "US", "WIN").toLanguageTag() + "/" + new Locale("", "", "POSIX").toString();
            case 104: return Locale.forLanguageTag("en-US-x-lvariant-POSIX").getVariant() + "/" + Locale.forLanguageTag("de-CH-1996").getVariant() + "/" + Locale.forLanguageTag("de-CH-1996").toString() + "/" + Locale.forLanguageTag("EN-gb").toString();
            case 105: return Locale.ROOT.toLanguageTag() + "/" + Locale.CHINA + "/" + Locale.CANADA_FRENCH + "/" + Locale.JAPANESE.toLanguageTag() + "/" + new Locale("", "US").toString() + "/" + new Locale("", "US").toLanguageTag();
            case 106: return new Locale("fr").getDisplayLanguage(Locale.ENGLISH) + "/" + new Locale("", "DE").getDisplayCountry(Locale.ENGLISH) + "/" + new Locale("fr", "CA").getDisplayName(Locale.ENGLISH) + "/" + new Locale("xx").getDisplayLanguage(Locale.ENGLISH);
            case 107: return "" + new Locale("en", "US").equals(Locale.US) + new Locale("en", "US", "x").equals(Locale.US) + Locale.US.getVariant().length() + Locale.US.getScript().length() + Locale.US.hasExtensions();
            case 108: return "" + new Locale(null);
            case 109: return Locale.forLanguageTag("").toString().length() + "/" + Locale.forLanguageTag("und").toString().length() + "/" + Locale.forLanguageTag("pt-BR").getCountry();
            // ---- Charsets
            case 110: return Charset.forName("latin1").name() + "/" + Charset.forName("utf8").name() + "/" + Charset.forName("ascii").name() + "/" + Charset.forName("Iso-8859-1").name();
            case 111: return "" + Charset.isSupported("x-nope") + Charset.isSupported("UTF-8") + Charset.forName("UTF-8").aliases().contains("UTF8") + Charset.forName("UTF-8").equals(StandardCharsets.UTF_8);
            case 112: return "" + Charset.forName("bad name");
            case 113: return "" + Charset.forName("x-nope");
            case 114: return "" + Charset.forName(null);
            case 115: return "" + Charset.isSupported("");
            case 116: return StandardCharsets.UTF_8.compareTo(Charset.forName("utf-8")) + "/" + StandardCharsets.US_ASCII + "/" + StandardCharsets.ISO_8859_1.displayName() + "/" + StandardCharsets.UTF_8.isRegistered() + StandardCharsets.UTF_8.canEncode();
            case 117: return cps(new String(new byte[] {(byte) 0xe9, 0x41}, StandardCharsets.ISO_8859_1)) + "/" + "\u00e9".getBytes(StandardCharsets.UTF_8).length + "/" + "\u00e9".getBytes(StandardCharsets.ISO_8859_1).length;
            case 118: return new InputStreamReader(new ByteArrayInputStream(new byte[0]), StandardCharsets.UTF_8).getEncoding() + "/" + new OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.ISO_8859_1).getEncoding() + "/" + new OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.US_ASCII).getEncoding();
            case 119: {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                OutputStreamWriter w = new OutputStreamWriter(bout, StandardCharsets.UTF_8);
                w.write("h\u00e9");
                w.flush();
                return bout.size() + "/" + cps(bout.toString(StandardCharsets.UTF_8)) + "/" + cps(bout.toString(StandardCharsets.ISO_8859_1));
            }
            case 120: {
                InputStreamReader r = new InputStreamReader(new ByteArrayInputStream(new byte[] {(byte) 0xc3, (byte) 0xa9, 0x41}), StandardCharsets.UTF_8);
                return r.read() + "/" + r.read() + "/" + r.read();
            }
            default: return null;
        }
    }
}
