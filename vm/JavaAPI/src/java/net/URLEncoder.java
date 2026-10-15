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
package java.net;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Encodes text in the application/x-www-form-urlencoded format, with the JDK's
 * rules: the letters, the digits and the four characters {@code . - * _} are
 * kept, a space becomes {@code +}, and every other character is converted to
 * bytes in the named charset and each byte written as {@code %XY} in upper case
 * hexadecimal.
 *
 * Pure Java on purpose. A server written against the JDK calls this class, and
 * the compiled backend has to answer the same bytes a JVM would, so nothing
 * here may depend on a native.
 */
public class URLEncoder {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private URLEncoder() {
    }

    /**
     * Encodes {@code s} as UTF-8, which is the platform charset on every
     * runtime this library runs on.
     *
     * @deprecated name the charset with {@link #encode(String, String)}, as the
     *     JDK asks.
     */
    public static String encode(String s) {
        try {
            return encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Encodes {@code s} with the bytes of the charset named {@code enc}.
     *
     * @throws UnsupportedEncodingException when the charset is not supported
     */
    public static String encode(String s, String enc) throws UnsupportedEncodingException {
        if (enc == null) {
            throw new NullPointerException("charsetName");
        }
        // Named and checked before anything is converted, as the JDK does: a
        // charset this runtime cannot write is refused for any input, and not
        // only for the input that happens to need a byte converted.
        return encodeWith(s, charset(enc));
    }

    /**
     * The canonical name of the charset {@code enc} names, or an
     * {@link UnsupportedEncodingException} when it names none this class
     * converts.
     *
     * Support is decided here, by name, and not by asking the runtime to
     * convert something. {@code String.getBytes(String)} cannot answer that
     * question: the native conversion behind it writes UTF-8 for any name it
     * does not know rather than refusing it, so a misspelt charset would encode
     * and decode without complaint. The names accepted are those of the three
     * charsets {@link Charset#forName(String)} and {@code StandardCharsets}
     * provide, with the JDK's common aliases for them, and all three are
     * converted in Java by this class and {@link URLDecoder}, so the same bytes
     * come out on every runtime. Names are compared without case, as the JDK
     * compares them, and with {@code equalsIgnoreCase}, which does not depend
     * on the device's locale.
     */
    static String charset(String enc) throws UnsupportedEncodingException {
        if (enc == null) {
            throw new NullPointerException("charsetName");
        }
        if (is(enc, UTF8_NAMES)) {
            return "UTF-8";
        }
        if (is(enc, LATIN1_NAMES)) {
            return "ISO-8859-1";
        }
        if (is(enc, ASCII_NAMES)) {
            return "US-ASCII";
        }
        throw new UnsupportedEncodingException(enc);
    }

    private static final String[] UTF8_NAMES = {"UTF-8", "UTF8"};
    private static final String[] LATIN1_NAMES = {"ISO-8859-1", "ISO8859-1", "ISO8859_1",
        "ISO_8859_1", "ISO_8859-1", "8859_1", "LATIN1", "L1"};
    private static final String[] ASCII_NAMES = {"US-ASCII", "ASCII", "US", "ISO646-US",
        "ASCII7", "646"};

    private static boolean is(String enc, String[] names) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equalsIgnoreCase(enc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Encodes {@code s} with the bytes of {@code charset}.
     */
    public static String encode(String s, Charset charset) {
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        try {
            return encodeWith(s, charset(charset.name()));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    private static String encodeWith(String s, String enc) {
        int length = s.length();
        StringBuilder out = null;
        int i = 0;
        while (i < length) {
            char c = s.charAt(i);
            if (keep(c)) {
                if (out != null) {
                    out.append(c);
                }
                i++;
                continue;
            }
            if (out == null) {
                out = new StringBuilder(length + 16);
                out.append(s, 0, i);
            }
            if (c == ' ') {
                out.append('+');
                i++;
                continue;
            }
            // The whole run of characters that need encoding is converted at
            // once, as the JDK does, so a surrogate pair reaches the charset
            // together and becomes the four bytes of one code point.
            int start = i;
            while (i < length && !keep(s.charAt(i)) && s.charAt(i) != ' ') {
                i++;
            }
            byte[] bytes = bytes(s.substring(start, i), enc);
            for (int b = 0; b < bytes.length; b++) {
                int v = bytes[b] & 0xff;
                out.append('%').append(HEX[v >> 4]).append(HEX[v & 0xf]);
            }
        }
        return out == null ? s : out.toString();
    }

    private static boolean keep(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                || c == '.' || c == '-' || c == '*' || c == '_';
    }

    /**
     * Converted here rather than by the runtime's charset support, so that a
     * character the charset cannot hold is written the way the JDK writes it
     * -- as the replacement byte {@code ?}, once for a surrogate pair --
     * whatever the native conversion would do. {@code enc} is a canonical name
     * from {@link #charset(String)}.
     */
    private static byte[] bytes(String run, String enc) {
        if (!"UTF-8".equals(enc)) {
            int max = "US-ASCII".equals(enc) ? 0x80 : 0x100;
            byte[] narrow = new byte[run.length()];
            int count = 0;
            for (int i = 0; i < run.length(); i++) {
                int c = run.charAt(i);
                if (c >= 0xD800 && c <= 0xDBFF && i + 1 < run.length()
                        && run.charAt(i + 1) >= 0xDC00 && run.charAt(i + 1) <= 0xDFFF) {
                    i++;
                }
                narrow[count++] = (byte) (c < max ? c : '?');
            }
            byte[] result = new byte[count];
            System.arraycopy(narrow, 0, result, 0, count);
            return result;
        }
        // At most three bytes per char: a pair is two chars and four bytes.
        byte[] out = new byte[run.length() * 3];
        int at = 0;
        int n = run.length();
        for (int i = 0; i < n; i++) {
            int c = run.charAt(i);
            if (c >= 0xD800 && c <= 0xDBFF && i + 1 < n
                    && run.charAt(i + 1) >= 0xDC00 && run.charAt(i + 1) <= 0xDFFF) {
                int cp = 0x10000 + ((c - 0xD800) << 10) + (run.charAt(i + 1) - 0xDC00);
                i++;
                out[at++] = (byte) (0xF0 | (cp >> 18));
                out[at++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
                out[at++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
                out[at++] = (byte) (0x80 | (cp & 0x3F));
            } else if (c >= 0xD800 && c <= 0xDFFF) {
                out[at++] = (byte) ('?');
            } else if (c < 0x80) {
                out[at++] = (byte) (c);
            } else if (c < 0x800) {
                out[at++] = (byte) (0xC0 | (c >> 6));
                out[at++] = (byte) (0x80 | (c & 0x3F));
            } else {
                out[at++] = (byte) (0xE0 | (c >> 12));
                out[at++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                out[at++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        byte[] result = new byte[at];
        System.arraycopy(out, 0, result, 0, at);
        return result;
    }
}
