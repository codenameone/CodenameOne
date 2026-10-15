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
package com.codename1.impl.charset;

import java.io.UnsupportedEncodingException;

/**
 * The one list of charsets this class library converts, and the conversions
 * that are done in Java rather than by a runtime native.
 *
 * Every API that takes a charset name -- {@code String.getBytes(String)},
 * {@code new String(byte[], String)}, {@code InputStreamReader},
 * {@code OutputStreamWriter}, {@code Charset.forName}, {@code URLEncoder} and
 * {@code URLDecoder} -- resolves it here, so a name is either supported by all
 * of them or refused by all of them with the JDK's
 * {@link UnsupportedEncodingException}.
 *
 * Support is decided by name and never by asking a native to convert
 * something. The natives cannot answer that question: the C conversion wrote
 * UTF-8 for a name it did not know on every target, and read it as UTF-8 on
 * Apple targets and as ISO-8859-1 elsewhere; it handled UTF-16 and ISO-8859-2
 * only where NSString exists, and the JavaScript port's encoder ignored the
 * name altogether and always wrote UTF-8. So only UTF-8 is handed to a native,
 * whose UTF-8 path is correct on every target, and the other charsets are
 * converted by this class, which gives the same bytes on all of them.
 *
 * The names accepted are each charset's canonical name and the aliases the JDK
 * reports for it, compared without case as the JDK compares them. The
 * comparison is {@code equalsIgnoreCase}, which does not depend on the
 * device's locale.
 */
public final class CharsetSupport {
    /** UTF-8. The only charset whose conversion is left to the runtime's natives. */
    public static final int UTF_8 = 0;
    /** ISO-8859-1. */
    public static final int ISO_8859_1 = 1;
    /** US-ASCII. */
    public static final int US_ASCII = 2;
    /** UTF-16: big endian, written with a byte order mark, read honouring one. */
    public static final int UTF_16 = 3;
    /** UTF-16BE: big endian, no byte order mark. */
    public static final int UTF_16BE = 4;
    /** UTF-16LE: little endian, no byte order mark. */
    public static final int UTF_16LE = 5;
    /** ISO-8859-2. */
    public static final int ISO_8859_2 = 6;

    /**
     * Indexed by the constants above; the first name of each row is the
     * canonical one. The aliases are the ones {@code Charset.aliases()} reports
     * on JDK 8 through 25, less {@code default}, which later JDKs dropped from
     * US-ASCII.
     */
    private static final String[][] NAMES = {
        {"UTF-8", "UTF8", "unicode-1-1-utf-8"},
        {"ISO-8859-1", "819", "8859_1", "IBM-819", "IBM819", "ISO8859-1", "ISO8859_1",
            "ISO_8859-1", "ISO_8859-1:1987", "ISO_8859_1", "cp819", "csISOLatin1",
            "iso-ir-100", "l1", "latin1"},
        {"US-ASCII", "646", "ANSI_X3.4-1968", "ANSI_X3.4-1986", "ASCII", "IBM367",
            "ISO646-US", "ISO_646.irv:1991", "ascii7", "cp367", "csASCII", "iso-ir-6",
            "iso_646.irv:1983", "us"},
        {"UTF-16", "UTF_16", "UnicodeBig", "unicode", "utf16"},
        {"UTF-16BE", "ISO-10646-UCS-2", "UTF_16BE", "UnicodeBigUnmarked", "X-UTF-16BE"},
        {"UTF-16LE", "UTF_16LE", "UnicodeLittleUnmarked", "X-UTF-16LE"},
        {"ISO-8859-2", "8859_2", "912", "ISO8859-2", "ISO_8859-2", "ISO_8859-2:1987",
            "cp912", "csISOLatin2", "ibm-912", "ibm912", "iso-ir-101", "iso8859_2", "l2",
            "latin2"}
    };

    /**
     * ISO-8859-2 bytes 0xA0..0xFF; below 0xA0 it agrees with ISO-8859-1.
     * Generated from the JDK's decoder.
     */
    private static final char[] LATIN2_HIGH = {
        0x00A0, 0x0104, 0x02D8, 0x0141, 0x00A4, 0x013D, 0x015A, 0x00A7,
        0x00A8, 0x0160, 0x015E, 0x0164, 0x0179, 0x00AD, 0x017D, 0x017B,
        0x00B0, 0x0105, 0x02DB, 0x0142, 0x00B4, 0x013E, 0x015B, 0x02C7,
        0x00B8, 0x0161, 0x015F, 0x0165, 0x017A, 0x02DD, 0x017E, 0x017C,
        0x0154, 0x00C1, 0x00C2, 0x0102, 0x00C4, 0x0139, 0x0106, 0x00C7,
        0x010C, 0x00C9, 0x0118, 0x00CB, 0x011A, 0x00CD, 0x00CE, 0x010E,
        0x0110, 0x0143, 0x0147, 0x00D3, 0x00D4, 0x0150, 0x00D6, 0x00D7,
        0x0158, 0x016E, 0x00DA, 0x0170, 0x00DC, 0x00DD, 0x0162, 0x00DF,
        0x0155, 0x00E1, 0x00E2, 0x0103, 0x00E4, 0x013A, 0x0107, 0x00E7,
        0x010D, 0x00E9, 0x0119, 0x00EB, 0x011B, 0x00ED, 0x00EE, 0x010F,
        0x0111, 0x0144, 0x0148, 0x00F3, 0x00F4, 0x0151, 0x00F6, 0x00F7,
        0x0159, 0x016F, 0x00FA, 0x0171, 0x00FC, 0x00FD, 0x0163, 0x02D9
    };

    private static final char REPLACEMENT = (char) 0xFFFD;

    private CharsetSupport() {
    }

    /**
     * The charset {@code name} names, as one of this class's constants.
     *
     * @throws NullPointerException when {@code name} is null, as the JDK throws
     * @throws UnsupportedEncodingException when it names no charset here,
     *     including the empty name
     */
    public static int lookup(String name) throws UnsupportedEncodingException {
        if (name == null) {
            throw new NullPointerException("charsetName");
        }
        // The spelling nearly every caller uses, answered without the table.
        if ("UTF-8".equals(name)) {
            return UTF_8;
        }
        for (int c = 0; c < NAMES.length; c++) {
            String[] names = NAMES[c];
            for (int i = 0; i < names.length; i++) {
                if (names[i].equalsIgnoreCase(name)) {
                    return c;
                }
            }
        }
        throw new UnsupportedEncodingException(name);
    }

    /** The canonical name of {@code charset}, one of this class's constants. */
    public static String name(int charset) {
        return NAMES[charset][0];
    }

    /**
     * Encodes {@code chars[0..len)} in {@code charset}, as the JDK's
     * {@code String.getBytes} does: a character the charset cannot hold is
     * written as its replacement, {@code ?} for the single byte charsets and
     * U+FFFD for UTF-16, once for a surrogate pair. UTF-8 is not converted
     * here; the caller hands it to the runtime.
     */
    public static byte[] encode(int charset, char[] chars, int len) {
        if (charset == UTF_16 || charset == UTF_16BE || charset == UTF_16LE) {
            return encodeUtf16(charset, chars, len);
        }
        // Below this every charset here writes the character as its own byte.
        int direct = charset == US_ASCII ? 0x80 : charset == ISO_8859_2 ? 0xA0 : 0x100;
        byte[] out = new byte[len];
        int count = 0;
        for (int i = 0; i < len; i++) {
            char c = chars[i];
            if (c < direct) {
                out[count++] = (byte) c;
                continue;
            }
            if (charset == ISO_8859_2) {
                int b = latin2Byte(c);
                if (b >= 0) {
                    out[count++] = (byte) b;
                    continue;
                }
            }
            if (isHigh(c) && i + 1 < len && isLow(chars[i + 1])) {
                i++;
            }
            out[count++] = (byte) '?';
        }
        if (count == len) {
            return out;
        }
        byte[] result = new byte[count];
        System.arraycopy(out, 0, result, 0, count);
        return result;
    }

    /**
     * Decodes {@code len} bytes of {@code bytes} from {@code off} in
     * {@code charset}, as the JDK's {@code new String(byte[], String)} does: a
     * byte or sequence the charset cannot read becomes U+FFFD. UTF-8 is not
     * converted here; the caller hands it to the runtime.
     */
    public static char[] decode(int charset, byte[] bytes, int off, int len) {
        if (off < 0 || len < 0 || off > bytes.length - len) {
            throw new StringIndexOutOfBoundsException("offset " + off + ", count " + len
                    + ", length " + bytes.length);
        }
        if (charset == UTF_16 || charset == UTF_16BE || charset == UTF_16LE) {
            return decodeUtf16(charset, bytes, off, len);
        }
        char[] out = new char[len];
        for (int i = 0; i < len; i++) {
            int b = bytes[off + i] & 0xff;
            if (charset == US_ASCII && b > 0x7F) {
                out[i] = REPLACEMENT;
            } else if (charset == ISO_8859_2 && b >= 0xA0) {
                out[i] = LATIN2_HIGH[b - 0xA0];
            } else {
                out[i] = (char) b;
            }
        }
        return out;
    }

    private static int latin2Byte(char c) {
        for (int i = 0; i < LATIN2_HIGH.length; i++) {
            if (LATIN2_HIGH[i] == c) {
                return 0xA0 + i;
            }
        }
        return -1;
    }

    private static boolean isHigh(char c) {
        return c >= 0xD800 && c <= 0xDBFF;
    }

    private static boolean isLow(char c) {
        return c >= 0xDC00 && c <= 0xDFFF;
    }

    /**
     * UTF-16 is written big endian behind a byte order mark, UTF-16BE and
     * UTF-16LE without one. An unpaired surrogate is written as U+FFFD, the
     * replacement the JDK's UTF-16 encoders use. The JDK writes nothing at all,
     * not even the mark, for an empty string.
     */
    private static byte[] encodeUtf16(int charset, char[] chars, int len) {
        if (len == 0) {
            return new byte[0];
        }
        boolean little = charset == UTF_16LE;
        int at = charset == UTF_16 ? 2 : 0;
        byte[] out = new byte[at + len * 2];
        if (at == 2) {
            out[0] = (byte) 0xFE;
            out[1] = (byte) 0xFF;
        }
        for (int i = 0; i < len; i++) {
            char c = chars[i];
            if (isHigh(c) && i + 1 < len && isLow(chars[i + 1])) {
                at = putUnit(out, at, c, little);
                at = putUnit(out, at, chars[++i], little);
                continue;
            }
            if (isHigh(c) || isLow(c)) {
                c = REPLACEMENT;
            }
            at = putUnit(out, at, c, little);
        }
        return out;
    }

    private static int putUnit(byte[] out, int at, char c, boolean little) {
        if (little) {
            out[at] = (byte) c;
            out[at + 1] = (byte) (c >> 8);
        } else {
            out[at] = (byte) (c >> 8);
            out[at + 1] = (byte) c;
        }
        return at + 2;
    }

    /**
     * UTF-16 honours a leading byte order mark and drops it, and reads big
     * endian without one; UTF-16BE and UTF-16LE keep a leading U+FEFF as a
     * character. As in the JDK's decoder, an unpaired surrogate and a trailing
     * odd byte each become U+FFFD. U+FFFE is read as a character, as JDK 17
     * and later read it; JDK 8 replaced it.
     */
    private static char[] decodeUtf16(int charset, byte[] bytes, int off, int len) {
        boolean little = charset == UTF_16LE;
        int i = off;
        int end = off + len;
        if (charset == UTF_16 && len >= 2) {
            int b0 = bytes[off] & 0xff;
            int b1 = bytes[off + 1] & 0xff;
            if (b0 == 0xFE && b1 == 0xFF) {
                i += 2;
            } else if (b0 == 0xFF && b1 == 0xFE) {
                little = true;
                i += 2;
            }
        }
        char[] out = new char[(end - i + 1) / 2];
        int count = 0;
        while (i < end) {
            if (end - i < 2) {
                out[count++] = REPLACEMENT;
                break;
            }
            char c = unit(bytes, i, little);
            i += 2;
            if (isHigh(c)) {
                if (end - i < 2) {
                    // A high surrogate cut off by the end of the input is one
                    // malformed sequence together with any odd byte after it.
                    out[count++] = REPLACEMENT;
                    break;
                }
                char next = unit(bytes, i, little);
                i += 2;
                if (isLow(next)) {
                    out[count++] = c;
                    out[count++] = next;
                } else {
                    // The JDK reports a high surrogate and the unit after it
                    // as one four byte malformed sequence.
                    out[count++] = REPLACEMENT;
                }
            } else if (isLow(c)) {
                out[count++] = REPLACEMENT;
            } else {
                out[count++] = c;
            }
        }
        if (count == out.length) {
            return out;
        }
        char[] result = new char[count];
        System.arraycopy(out, 0, result, 0, count);
        return result;
    }

    private static char unit(byte[] bytes, int at, boolean little) {
        int a = bytes[at] & 0xff;
        int b = bytes[at + 1] & 0xff;
        return little ? (char) (b << 8 | a) : (char) (a << 8 | b);
    }
}
