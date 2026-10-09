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
package com.codename1.compat.jdk;

/// `java.util.HexFormat`: bytes to hexadecimal text and back, with an
/// optional delimiter between bytes and a prefix and suffix around each.
public final class HexFormat {
    private static final String LOWER = "0123456789abcdef";
    private static final String UPPER = "0123456789ABCDEF";
    private static final HexFormat PLAIN = new HexFormat("", "", "", false);

    private final String delimiter;
    private final String prefix;
    private final String suffix;
    private final boolean upper;

    private HexFormat(String delimiter, String prefix, String suffix, boolean upper) {
        if (delimiter == null || prefix == null || suffix == null) {
            throw new NullPointerException();
        }
        this.delimiter = delimiter;
        this.prefix = prefix;
        this.suffix = suffix;
        this.upper = upper;
    }

    /// Lowercase digits and nothing between or around the bytes.
    public static HexFormat of() {
        return PLAIN;
    }

    public static HexFormat ofDelimiter(String delimiter) {
        return new HexFormat(delimiter, "", "", false);
    }

    public HexFormat withDelimiter(String delimiter) {
        return new HexFormat(delimiter, prefix, suffix, upper);
    }

    public HexFormat withPrefix(String prefix) {
        return new HexFormat(delimiter, prefix, suffix, upper);
    }

    public HexFormat withSuffix(String suffix) {
        return new HexFormat(delimiter, prefix, suffix, upper);
    }

    public HexFormat withUpperCase() {
        return new HexFormat(delimiter, prefix, suffix, true);
    }

    public HexFormat withLowerCase() {
        return new HexFormat(delimiter, prefix, suffix, false);
    }

    public String delimiter() {
        return delimiter;
    }

    public String prefix() {
        return prefix;
    }

    public String suffix() {
        return suffix;
    }

    public boolean isUpperCase() {
        return upper;
    }

    public String formatHex(byte[] bytes) {
        return formatHex(bytes, 0, bytes.length);
    }

    public String formatHex(byte[] bytes, int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > bytes.length || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + toIndex
                    + ") out of bounds for length " + bytes.length);
        }
        String digits = upper ? UPPER : LOWER;
        StringBuilder out = new StringBuilder();
        for (int i = fromIndex; i < toIndex; i++) {
            if (i > fromIndex) {
                out.append(delimiter);
            }
            out.append(prefix).append(digits.charAt((bytes[i] >> 4) & 0xF)).append(digits.charAt(bytes[i] & 0xF))
                    .append(suffix);
        }
        return out.toString();
    }

    public byte[] parseHex(CharSequence string) {
        return parseHex(string, 0, string.length());
    }

    public byte[] parseHex(CharSequence string, int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > string.length() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + toIndex
                    + ") out of bounds for length " + string.length());
        }
        int length = toIndex - fromIndex;
        if (length == 0) {
            return new byte[0];
        }
        int each = prefix.length() + 2 + suffix.length();
        int stride = each + delimiter.length();
        if ((length - each) % stride != 0) {
            throw new IllegalArgumentException("extra or missing delimiters "
                    + "or values consisting of prefix, two hexadecimal digits, and suffix in the string");
        }
        byte[] out = new byte[(length - each) / stride + 1];
        int at = fromIndex;
        for (int i = 0; i < out.length; i++) {
            if (i > 0) {
                at = expect(string, at, delimiter, "delimiter");
            }
            at = expect(string, at, prefix, "prefix");
            out[i] = (byte) (fromHexDigit(string.charAt(at)) * 16 + fromHexDigit(string.charAt(at + 1)));
            at = expect(string, at + 2, suffix, "suffix");
        }
        return out;
    }

    private static int expect(CharSequence string, int at, String literal, String what) {
        for (int i = 0; i < literal.length(); i++) {
            if (string.charAt(at + i) != literal.charAt(i)) {
                throw new IllegalArgumentException("found: \"" + string.subSequence(at, at + literal.length())
                        + "\", expected: \"" + literal + "\", index: " + at + " ch: "
                        + Integer.toHexString(string.charAt(at)));
            }
        }
        return at + literal.length();
    }

    public char toLowHexDigit(int value) {
        return (upper ? UPPER : LOWER).charAt(value & 0xF);
    }

    public char toHighHexDigit(int value) {
        return (upper ? UPPER : LOWER).charAt((value >> 4) & 0xF);
    }

    public String toHexDigits(byte value) {
        return toHexDigits(value, 2);
    }

    public String toHexDigits(char value) {
        return toHexDigits(value, 4);
    }

    public String toHexDigits(short value) {
        return toHexDigits(value, 4);
    }

    public String toHexDigits(int value) {
        return toHexDigits(value, 8);
    }

    public String toHexDigits(long value) {
        return toHexDigits(value, 16);
    }

    public String toHexDigits(long value, int digits) {
        if (digits < 0 || digits > 16) {
            throw new IllegalArgumentException("number of digits: " + digits);
        }
        char[] out = new char[digits];
        long v = value;
        for (int i = digits - 1; i >= 0; i--) {
            out[i] = toLowHexDigit((int) v);
            v >>>= 4;
        }
        return new String(out);
    }

    public static boolean isHexDigit(int ch) {
        return ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F';
    }

    public static int fromHexDigit(int ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }
        if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        }
        throw new NumberFormatException("not a hexadecimal digit: \"" + (char) ch + "\" = " + ch);
    }

    public static int fromHexDigits(CharSequence string) {
        int length = string.length();
        if (length > 8) {
            throw new IllegalArgumentException("string length greater than 8: " + length);
        }
        int value = 0;
        for (int i = 0; i < length; i++) {
            value = (value << 4) + fromHexDigit(string.charAt(i));
        }
        return value;
    }

    public static long fromHexDigitsToLong(CharSequence string) {
        int length = string.length();
        if (length > 16) {
            throw new IllegalArgumentException("string length greater than 16: " + length);
        }
        long value = 0;
        for (int i = 0; i < length; i++) {
            value = (value << 4) + fromHexDigit(string.charAt(i));
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HexFormat)) {
            return false;
        }
        HexFormat other = (HexFormat) o;
        return upper == other.upper && delimiter.equals(other.delimiter) && prefix.equals(other.prefix)
                && suffix.equals(other.suffix);
    }

    @Override
    public int hashCode() {
        return (delimiter.hashCode() * 31 + prefix.hashCode()) * 31 + suffix.hashCode() + (upper ? 1 : 0);
    }

    @Override
    public String toString() {
        return "uppercase: " + upper + ", delimiter: \"" + delimiter + "\", prefix: \"" + prefix
                + "\", suffix: \"" + suffix + "\"";
    }
}
