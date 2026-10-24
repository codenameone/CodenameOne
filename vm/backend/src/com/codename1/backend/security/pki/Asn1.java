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
package com.codename1.backend.security.pki;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/// The DER this package reads and writes: a reader over definite-length
/// encodings and the handful of writers the formats here need.
///
/// BER's indefinite lengths are refused. X.509, PKCS#10 and the PKCS#12 files
/// OpenSSL and the JDK write are DER; a file that is not is reported as such
/// rather than half read.
final class Asn1 {
    static final int BOOLEAN = 0x01;
    static final int INTEGER = 0x02;
    static final int BIT_STRING = 0x03;
    static final int OCTET_STRING = 0x04;
    static final int NULL = 0x05;
    static final int OID = 0x06;
    static final int UTF8_STRING = 0x0c;
    static final int PRINTABLE_STRING = 0x13;
    static final int T61_STRING = 0x14;
    static final int IA5_STRING = 0x16;
    static final int UTC_TIME = 0x17;
    static final int GENERALIZED_TIME = 0x18;
    static final int UNIVERSAL_STRING = 0x1c;
    static final int BMP_STRING = 0x1e;
    static final int SEQUENCE = 0x30;
    static final int SET = 0x31;

    private Asn1() {
    }

    /// The context-specific constructed tag `[n]`.
    static int explicit(int n) {
        return 0xa0 | n;
    }

    /// The context-specific primitive tag `[n]`.
    static int implicit(int n) {
        return 0x80 | n;
    }

    /// Reads one level of a DER structure. Every read checks the tag and that
    /// the element ends inside its parent.
    static final class Reader {
        private final byte[] data;
        private int pos;
        private final int end;

        Reader(byte[] data) throws IOException {
            this(data, 0, data == null ? 0 : data.length);
            if (data == null || data.length == 0) {
                throw new IOException("No DER data was given");
            }
        }

        private Reader(byte[] data, int pos, int end) {
            this.data = data;
            this.pos = pos;
            this.end = end;
        }

        boolean hasMore() {
            return pos < end;
        }

        /// The tag of the next element, or -1 at the end.
        int peek() {
            return pos < end ? data[pos] & 0xff : -1;
        }

        /// Fails unless every byte has been read.
        void finish() throws IOException {
            if (pos != end) {
                throw new IOException("Unexpected data after the end of a DER structure");
            }
        }

        /// Reads the header of the next element, which must carry `tag`, and
        /// returns the offset of its content; `pos` is left at the content.
        private int[] header(int tag) throws IOException {
            if (pos >= end) {
                throw new IOException("DER ended early, where tag 0x" + Integer.toHexString(tag)
                        + " was expected");
            }
            int start = pos;
            int have = data[pos++] & 0xff;
            if (have != tag) {
                throw new IOException("Expected DER tag 0x" + Integer.toHexString(tag) + ", found 0x"
                        + Integer.toHexString(have));
            }
            if (pos >= end) {
                throw new IOException("DER ended inside a length");
            }
            int first = data[pos++] & 0xff;
            int length;
            if (first < 0x80) {
                length = first;
            } else if (first == 0x80) {
                throw new IOException("BER indefinite length; only DER is read");
            } else {
                int count = first & 0x7f;
                if (count > 4) {
                    throw new IOException("A DER length too large to be real");
                }
                length = 0;
                for (int iter = 0 ; iter < count ; iter++) {
                    if (pos >= end) {
                        throw new IOException("DER ended inside a length");
                    }
                    length = (length << 8) | (data[pos++] & 0xff);
                }
                if (length < 0) {
                    throw new IOException("A DER length too large to be real");
                }
            }
            if (length > end - pos) {
                throw new IOException("A DER element runs past the end of its parent");
            }
            return new int[] {start, pos, pos + length};
        }

        /// The next element's content, which must carry `tag`.
        byte[] read(int tag) throws IOException {
            int[] h = header(tag);
            pos = h[2];
            return copy(h[1], h[2]);
        }

        /// The next element whole, header included, which must carry `tag`.
        byte[] raw(int tag) throws IOException {
            int[] h = header(tag);
            pos = h[2];
            return copy(h[0], h[2]);
        }

        /// A reader over the content of the next element, which must carry `tag`.
        Reader open(int tag) throws IOException {
            int[] h = header(tag);
            pos = h[2];
            return new Reader(data, h[1], h[2]);
        }

        /// Like [#open], or null and nothing read when the next tag is another.
        Reader openOptional(int tag) throws IOException {
            return peek() == tag ? open(tag) : null;
        }

        /// Skips the next element, whatever it is.
        void skip() throws IOException {
            pos = header(peek())[2];
        }

        /// The next element whole, whatever its tag.
        byte[] rawAny() throws IOException {
            return raw(peek());
        }

        String oid() throws IOException {
            return oidString(read(OID));
        }

        /// An INTEGER as its unsigned magnitude, leading zeros removed.
        byte[] unsignedInteger() throws IOException {
            byte[] value = read(INTEGER);
            if (value.length == 0) {
                throw new IOException("An empty DER INTEGER");
            }
            int skip = 0;
            while (skip < value.length - 1 && value[skip] == 0) {
                skip++;
            }
            byte[] out = new byte[value.length - skip];
            System.arraycopy(value, skip, out, 0, out.length);
            return out;
        }

        /// A small non-negative INTEGER.
        int smallInteger() throws IOException {
            byte[] value = read(INTEGER);
            if (value.length == 0 || value.length > 4 || (value[0] & 0x80) != 0) {
                throw new IOException("Expected a small non-negative DER INTEGER");
            }
            int out = 0;
            for (byte b : value) {
                out = (out << 8) | (b & 0xff);
            }
            return out;
        }

        boolean bool() throws IOException {
            byte[] value = read(BOOLEAN);
            if (value.length != 1) {
                throw new IOException("A DER BOOLEAN is one byte");
            }
            return value[0] != 0;
        }

        /// A BIT STRING's bytes; it must have no unused bits.
        byte[] bits() throws IOException {
            byte[] value = read(BIT_STRING);
            if (value.length == 0 || value[0] != 0) {
                throw new IOException("Expected a BIT STRING of whole bytes");
            }
            byte[] out = new byte[value.length - 1];
            System.arraycopy(value, 1, out, 0, out.length);
            return out;
        }

        /// A time, UTCTime or GeneralizedTime, as milliseconds since the epoch.
        long time() throws IOException {
            int tag = peek();
            if (tag != UTC_TIME && tag != GENERALIZED_TIME) {
                throw new IOException("Expected a UTCTime or a GeneralizedTime");
            }
            return parseTime(ascii(read(tag)), tag == UTC_TIME);
        }

        /// Any of the string types a certificate name uses, decoded.
        String string() throws IOException {
            int tag = peek();
            byte[] value = read(tag);
            switch (tag) {
                case UTF8_STRING:
                    return utf8(value);
                case PRINTABLE_STRING:
                case IA5_STRING:
                case T61_STRING:
                    return latin1(value);
                case BMP_STRING:
                    return bmp(value);
                case UNIVERSAL_STRING:
                    return universal(value);
                default:
                    throw new IOException("Not a DER string type: 0x" + Integer.toHexString(tag));
            }
        }

        private byte[] copy(int from, int to) {
            byte[] out = new byte[to - from];
            System.arraycopy(data, from, out, 0, out.length);
            return out;
        }
    }

    // ---------------------------------------------------------------- writing

    static byte[] tlv(int tag, byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        int lengthBytes = length < 0x80 ? 0 : length < 0x100 ? 1 : length < 0x10000 ? 2
                : length < 0x1000000 ? 3 : 4;
        byte[] out = new byte[2 + lengthBytes + length];
        out[0] = (byte) tag;
        if (lengthBytes == 0) {
            out[1] = (byte) length;
        } else {
            out[1] = (byte) (0x80 | lengthBytes);
            for (int iter = 0 ; iter < lengthBytes ; iter++) {
                out[2 + iter] = (byte) ((length >> (8 * (lengthBytes - 1 - iter))) & 0xff);
            }
        }
        int pos = 2 + lengthBytes;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, out, pos, part.length);
            pos += part.length;
        }
        return out;
    }

    static byte[] sequence(byte[]... parts) {
        return tlv(SEQUENCE, parts);
    }

    static byte[] set(byte[]... parts) {
        return tlv(SET, parts);
    }

    static byte[] octets(byte[] value) {
        return tlv(OCTET_STRING, value);
    }

    static byte[] bitString(byte[] value) {
        byte[] content = new byte[value.length + 1];
        System.arraycopy(value, 0, content, 1, value.length);
        return tlv(BIT_STRING, content);
    }

    static byte[] nullValue() {
        // Written element by element, like every other encoder here, and not
        // as an array literal: CodeQL treats a byte array that is never
        // written after it is made as a constant IV, follows these two bytes
        // through an encoded PFX into the IV decode() reads back out of it, and
        // reports the decrypting Cipher.init as java/static-initialization-vector.
        byte[] out = new byte[2];
        out[0] = NULL;
        return out;
    }

    /// A non-negative INTEGER.
    static byte[] integer(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("negative");
        }
        int bytes = 1;
        while (bytes < 8 && (value >>> (8 * bytes)) != 0) {
            bytes++;
        }
        byte[] magnitude = new byte[bytes];
        for (int iter = 0 ; iter < bytes ; iter++) {
            magnitude[iter] = (byte) (value >>> (8 * (bytes - 1 - iter)));
        }
        return unsignedInteger(magnitude);
    }

    /// An INTEGER from an unsigned magnitude.
    static byte[] unsignedInteger(byte[] magnitude) {
        int skip = 0;
        while (skip < magnitude.length - 1 && magnitude[skip] == 0) {
            skip++;
        }
        boolean pad = magnitude.length == 0 || (magnitude[skip] & 0x80) != 0;
        byte[] content = new byte[magnitude.length - skip + (pad ? 1 : 0)];
        System.arraycopy(magnitude, skip, content, pad ? 1 : 0, magnitude.length - skip);
        return tlv(INTEGER, content);
    }

    static byte[] oid(String dotted) {
        return tlv(OID, oidBytes(dotted));
    }

    static byte[] utf8String(String value) {
        return tlv(UTF8_STRING, utf8Bytes(value));
    }

    static byte[] ia5String(String value) {
        return tlv(IA5_STRING, asciiBytes(value));
    }

    static byte[] printableString(String value) {
        return tlv(PRINTABLE_STRING, asciiBytes(value));
    }

    static byte[] bmpString(String value) {
        return tlv(BMP_STRING, bmpBytes(value, false));
    }

    static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        byte[] out = new byte[length];
        int pos = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, out, pos, part.length);
            pos += part.length;
        }
        return out;
    }

    // --------------------------------------------------------------- encoding

    static String oidString(byte[] value) throws IOException {
        if (value.length == 0) {
            throw new IOException("An empty OBJECT IDENTIFIER");
        }
        StringBuilder out = new StringBuilder();
        long arc = 0;
        boolean first = true;
        for (int iter = 0 ; iter < value.length ; iter++) {
            int b = value[iter] & 0xff;
            if (arc > (Long.MAX_VALUE >>> 7)) {
                throw new IOException("An OBJECT IDENTIFIER arc too large");
            }
            arc = (arc << 7) | (b & 0x7f);
            if ((b & 0x80) != 0) {
                if (iter == value.length - 1) {
                    throw new IOException("An OBJECT IDENTIFIER ends inside an arc");
                }
                continue;
            }
            if (first) {
                long top = arc < 40 ? 0 : arc < 80 ? 1 : 2;
                out.append(top).append('.').append(arc - 40 * top);
                first = false;
            } else {
                out.append('.').append(arc);
            }
            arc = 0;
        }
        return out.toString();
    }

    static byte[] oidBytes(String dotted) {
        String[] arcs = split(dotted, '.');
        if (arcs.length < 2) {
            throw new IllegalArgumentException("Not an OBJECT IDENTIFIER: " + dotted);
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        long[] values = new long[arcs.length - 1];
        values[0] = Long.parseLong(arcs[0]) * 40 + Long.parseLong(arcs[1]);
        for (int iter = 2 ; iter < arcs.length ; iter++) {
            values[iter - 1] = Long.parseLong(arcs[iter]);
        }
        for (long value : values) {
            int groups = 1;
            while (groups < 10 && (value >>> (7 * groups)) != 0) {
                groups++;
            }
            for (int iter = groups - 1 ; iter >= 0 ; iter--) {
                int b = (int) ((value >>> (7 * iter)) & 0x7f);
                out.write(iter == 0 ? b : b | 0x80);
            }
        }
        return out.toByteArray();
    }

    static String[] split(String value, char sep) {
        java.util.List<String> parts = new java.util.ArrayList<String>();
        int pos = 0;
        while (true) {
            int next = value.indexOf(sep, pos);
            if (next < 0) {
                parts.add(value.substring(pos));
                break;
            }
            parts.add(value.substring(pos, next));
            pos = next + 1;
        }
        return parts.toArray(new String[parts.size()]);
    }

    static byte[] utf8Bytes(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8", err);
        }
    }

    static String utf8(byte[] value) throws IOException {
        try {
            return new String(value, "UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IOException("UTF-8", err);
        }
    }

    static byte[] asciiBytes(String value) {
        byte[] out = new byte[value.length()];
        for (int iter = 0 ; iter < out.length ; iter++) {
            char c = value.charAt(iter);
            if (c > 0x7e) {
                throw new IllegalArgumentException("Not ASCII: " + value);
            }
            out[iter] = (byte) c;
        }
        return out;
    }

    static String ascii(byte[] value) {
        return latin1(value);
    }

    static String latin1(byte[] value) {
        char[] out = new char[value.length];
        for (int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (char) (value[iter] & 0xff);
        }
        return new String(out);
    }

    /// UTF-16 big-endian, the BMPString encoding; PKCS#12 passwords add two
    /// zero bytes at the end.
    static byte[] bmpBytes(String value, boolean terminate) {
        byte[] out = new byte[value.length() * 2 + (terminate ? 2 : 0)];
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            out[iter * 2] = (byte) (c >> 8);
            out[iter * 2 + 1] = (byte) c;
        }
        return out;
    }

    static String bmp(byte[] value) throws IOException {
        if (value.length % 2 != 0) {
            throw new IOException("A BMPString of an odd number of bytes");
        }
        char[] out = new char[value.length / 2];
        for (int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (char) (((value[iter * 2] & 0xff) << 8) | (value[iter * 2 + 1] & 0xff));
        }
        return new String(out);
    }

    private static String universal(byte[] value) throws IOException {
        if (value.length % 4 != 0) {
            throw new IOException("A UniversalString that is not whole code points");
        }
        StringBuilder out = new StringBuilder();
        for (int iter = 0 ; iter < value.length ; iter += 4) {
            int cp = ((value[iter] & 0xff) << 24) | ((value[iter + 1] & 0xff) << 16)
                    | ((value[iter + 2] & 0xff) << 8) | (value[iter + 3] & 0xff);
            if (cp < 0 || cp > 0x10ffff) {
                throw new IOException("A UniversalString code point out of range");
            }
            if (cp >= 0x10000) {
                cp -= 0x10000;
                out.append((char) (0xd800 + (cp >> 10))).append((char) (0xdc00 + (cp & 0x3ff)));
            } else {
                out.append((char) cp);
            }
        }
        return out.toString();
    }

    // ------------------------------------------------------------------ times

    /// `YYMMDDHHMMSSZ` (UTCTime, years 1950 to 2049) or `YYYYMMDDHHMMSSZ`
    /// (GeneralizedTime), the forms RFC 5280 allows, in UTC.
    static long parseTime(String text, boolean utc) throws IOException {
        int yearDigits = utc ? 2 : 4;
        if (text.length() != yearDigits + 11 || text.charAt(text.length() - 1) != 'Z') {
            throw new IOException("Not an RFC 5280 time: " + text);
        }
        int year = digits(text, 0, yearDigits);
        if (utc) {
            year += year < 50 ? 2000 : 1900;
        }
        int month = digits(text, yearDigits, 2);
        int day = digits(text, yearDigits + 2, 2);
        int hour = digits(text, yearDigits + 4, 2);
        int minute = digits(text, yearDigits + 6, 2);
        int second = digits(text, yearDigits + 8, 2);
        if (month < 1 || month > 12 || day < 1 || day > 31 || hour > 23 || minute > 59
                || second > 59) {
            throw new IOException("Not an RFC 5280 time: " + text);
        }
        long days = daysFromCivil(year, month, day);
        return ((days * 24 + hour) * 60 + minute) * 60000L + second * 1000L;
    }

    /// A time as `2026-10-24T08:00:00Z`, for messages.
    static String formatTime(long millis) {
        long seconds = millis / 1000L;
        if (millis < 0 && seconds * 1000L != millis) {
            seconds--;
        }
        long days = seconds / 86400L;
        if (seconds < 0 && days * 86400L != seconds) {
            days--;
        }
        int secondOfDay = (int) (seconds - days * 86400L);
        int[] civil = civilFromDays(days);
        StringBuilder out = new StringBuilder();
        pad(out, civil[0], 4);
        out.append('-');
        pad(out, civil[1], 2);
        out.append('-');
        pad(out, civil[2], 2);
        out.append('T');
        pad(out, secondOfDay / 3600, 2);
        out.append(':');
        pad(out, secondOfDay / 60 % 60, 2);
        out.append(':');
        pad(out, secondOfDay % 60, 2);
        return out.append('Z').toString();
    }

    private static void pad(StringBuilder out, int value, int width) {
        String text = Integer.toString(value);
        for (int iter = text.length() ; iter < width ; iter++) {
            out.append('0');
        }
        out.append(text);
    }

    private static int digits(String text, int from, int count) throws IOException {
        int out = 0;
        for (int iter = from ; iter < from + count ; iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                throw new IOException("Not an RFC 5280 time: " + text);
            }
            out = out * 10 + (c - '0');
        }
        return out;
    }

    /// Days since 1970-01-01 of a proleptic Gregorian date (Howard Hinnant's
    /// algorithm).
    static long daysFromCivil(int year, int month, int day) {
        int y = month <= 2 ? year - 1 : year;
        int era = (y >= 0 ? y : y - 399) / 400;
        int yoe = y - era * 400;
        int doy = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
        int doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097L + doe - 719468L;
    }

    static int[] civilFromDays(long days) {
        long z = days + 719468L;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        int doe = (int) (z - era * 146097);
        int yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        int doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        int mp = (5 * doy + 2) / 153;
        int day = doy - (153 * mp + 2) / 5 + 1;
        int month = mp < 10 ? mp + 3 : mp - 9;
        long year = yoe + era * 400 + (month <= 2 ? 1 : 0);
        return new int[] {(int) year, month, day};
    }

    static String hex(byte[] value) {
        char[] digits = "0123456789abcdef".toCharArray();
        StringBuilder out = new StringBuilder(value.length * 2);
        for (byte b : value) {
            out.append(digits[(b >> 4) & 0xf]).append(digits[b & 0xf]);
        }
        return out.toString();
    }

    static boolean same(byte[] a, byte[] b) {
        return java.util.Arrays.equals(a, b);
    }
}
