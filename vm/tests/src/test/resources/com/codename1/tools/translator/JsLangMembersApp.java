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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/// The java.lang / java.util members JavaAPI took over from compat-jdk, in the
/// browser runtime. One bit per property; all of them is 0x3fffffff (30 bits).
/// Java 11 API only.
public class JsLangMembersApp {
    public static int result;

    static String msg(Throwable t) {
        return t.getClass().getName() + ":" + t.getMessage();
    }

    public static void main(String[] args) throws Exception {
        int mask = 0;
        String em = "" + (char) 0x2003;
        if ((em + " a b " + em).strip().equals("a b")) mask |= 1;
        if (" x ".isBlank() == false && "  ".isBlank() && "ab".repeat(3).equals("ababab")) mask |= 1 << 1;
        String smile = new String(new int[] {0x1F600}, 0, 1);
        if (smile.length() == 2 && smile.codePointAt(0) == 0x1F600 && ("a" + smile).codePointCount(0, 3) == 2) mask |= 1 << 2;
        if ("a\nb\r\nc".lines().count() == 3) mask |= 1 << 3;
        if (Integer.parseUnsignedInt("4294967295") == -1 && Integer.toUnsignedString(-1).equals("4294967295")) mask |= 1 << 4;
        if (Integer.decode("-0x1F") == -31 && Integer.decode("#ff") == 255 && Integer.decode("010") == 8) mask |= 1 << 5;
        try {
            Integer.parseInt("80000000", 16);
        } catch (NumberFormatException e) {
            if ("For input string: \"80000000\" under radix 16".equals(e.getMessage())) mask |= 1 << 6;
        }
        if (Long.parseUnsignedLong("18446744073709551615") == -1L && Long.toUnsignedString(-1L).equals("18446744073709551615")
                && Long.divideUnsigned(-1L, 10) == 1844674407370955161L) mask |= 1 << 7;
        if (Long.toHexString(-255L).equals("ffffffffffffff01") && Long.toBinaryString(5L).equals("101") && Long.bitCount(-1L) == 64) mask |= 1 << 8;
        if (Integer.reverse(0x12345678) == 0x1e6a2c48 && Integer.reverseBytes(0x12345678) == 0x78563412 && Integer.highestOneBit(100) == 64) mask |= 1 << 9;
        if (Double.toHexString(1.0).equals("0x1.0p0") && Double.toHexString(0.1).equals("0x1.999999999999ap-4")
                && Float.toHexString(Float.MIN_VALUE).equals("0x0.000002p-126")) mask |= 1 << 10;
        if (Short.decode("0x7fff") == 32767 && Byte.toUnsignedInt((byte) -1) == 255 && Short.reverseBytes((short) 0x1234) == 0x3412) mask |= 1 << 11;
        if (Boolean.logicalXor(true, false) && Boolean.compare(true, false) == 1 && Boolean.hashCode(true) == 1231) mask |= 1 << 12;
        if (Character.getNumericValue('a') == 10 && Character.digit((char) 0xFF21, 16) == 10 && Character.isDigit(0x0663)) mask |= 1 << 13;
        if (Character.toUpperCase(0xe9) == 0xc9 && Character.toUpperCase(0xff) == 0x178 && Character.isSpaceChar((char) 0xa0)) mask |= 1 << 14;
        if (Character.getType('A') == Character.UPPERCASE_LETTER && Character.getType(' ') == Character.SPACE_SEPARATOR
                && Character.getType((char) 0x01c5) == Character.TITLECASE_LETTER) mask |= 1 << 15;
        if (Character.toString(0x1F600).length() == 2 && Character.forDigit(11, 16) == 'b' && Character.isAlphabetic('a')) mask |= 1 << 16;
        if (Math.round(-2.5) == -2 && Math.round(2.5f) == 3 && Math.round(0.49999999999999994) == 0 && Math.round(1.5) == 2) mask |= 1 << 17;
        if (Math.floorDiv(-7, 2) == -4 && Math.floorMod(-7, 2) == 1 && Math.floorMod(7, -2) == -1) mask |= 1 << 18;
        try {
            Math.addExact(Integer.MAX_VALUE, 1);
        } catch (ArithmeticException e) {
            if ("integer overflow".equals(e.getMessage())) mask |= 1 << 19;
        }
        if (Double.doubleToLongBits(Math.nextUp(1.0)) == 0x3ff0000000000001L && Double.doubleToLongBits(Math.ulp(1.0)) == 0x3cb0000000000000L
                && Math.hypot(3, 4) == 5.0 && Math.getExponent(1024.0) == 10) mask |= 1 << 20;
        if (Math.multiplyHigh(Long.MAX_VALUE, Long.MAX_VALUE) == 4611686018427387903L && Math.multiplyFull(-2, 3) == -6L && StrictMath.max(1, 2) == 2) mask |= 1 << 21;
        if ("d".equals(Objects.requireNonNullElse(null, "d"))) mask |= 1 << 22;
        try {
            Objects.checkIndex(5, 3);
        } catch (IndexOutOfBoundsException e) {
            if ("Index 5 out of bounds for length 3".equals(e.getMessage())) mask |= 1 << 23;
        }
        StringBuilder sb = new StringBuilder("ab");
        sb.appendCodePoint(0x1F600);
        if (sb.length() == 4 && sb.codePointCount(0, 4) == 3 && sb.compareTo(new StringBuilder("ac")) < 0
                && CharSequence.compare("a", sb) < 0 && sb.substring(1, 2).equals("b")) mask |= 1 << 24;
        if (new Locale("IW", "il").toString().equals("he_IL") && new Locale("en", "US", "WIN").toLanguageTag().equals("en-US-x-lvariant-WIN")
                && Locale.forLanguageTag("en-US-x-lvariant-POSIX").getVariant().equals("POSIX")) mask |= 1 << 25;
        if (new Locale("fr", "CA").getDisplayName(Locale.ENGLISH).equals("French (Canada)") && Locale.ROOT.toLanguageTag().equals("und")
                && new Locale("en", "US").equals(Locale.US)) mask |= 1 << 26;
        if (Charset.forName("latin1").name().equals("ISO-8859-1") && Charset.forName("utf8") == StandardCharsets.UTF_8
                && !Charset.isSupported("x-nope") && Charset.forName("UTF-8").aliases().contains("UTF8")) mask |= 1 << 27;
        try {
            Charset.forName("x-nope");
        } catch (IllegalArgumentException e) {
            if ("java.nio.charset.UnsupportedCharsetException:x-nope".equals(msg(e))) mask |= 1 << 28;
        }
        byte[] utf8 = "h\u00e9".getBytes(StandardCharsets.UTF_8);
        if (utf8.length == 3 && new String(utf8, StandardCharsets.UTF_8).equals("h\u00e9")
                && new String(new byte[] {(byte) 0xe9}, StandardCharsets.ISO_8859_1).charAt(0) == 0xe9) mask |= 1 << 29;
        result = mask;
        System.out.println("MASK=" + Integer.toHexString(mask));
        System.exit(mask);
    }
}
