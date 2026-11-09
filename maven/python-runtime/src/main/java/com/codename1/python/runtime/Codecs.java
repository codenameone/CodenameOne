/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.runtime;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/// `str.encode` / `bytes.decode` for the encodings Codename One supports on
/// every platform: UTF-8, ASCII and Latin-1, plus the two that file formats
/// mandate rather than a locale picks -- UTF-16, which PDF text strings and
/// OLE compound files (`.msg`, `.xls`) store, and code page 437, the ZIP
/// specification's encoding for entry names without the UTF-8 flag. Any
/// other name is a LookupError rather than a silent fallback, so a program
/// that needs another charset fails where it asks for it. This is not a
/// step toward the JDK's charset list: a locale charset (Shift-JIS, cp1252)
/// stays unsupported.
final class Codecs {
    /// Code page 437's characters for bytes 0x80-0xFF; the low half is ASCII.
    private static final String CP437_HIGH =
            "\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7" +
            "\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5" +
            "\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9" +
            "\u00ff\u00d6\u00dc\u00a2\u00a3\u00a5\u20a7\u0192" +
            "\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba" +
            "\u00bf\u2310\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb" +
            "\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556" +
            "\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510" +
            "\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f" +
            "\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567" +
            "\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b" +
            "\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580" +
            "\u03b1\u00df\u0393\u03c0\u03a3\u03c3\u00b5\u03c4" +
            "\u03a6\u0398\u03a9\u03b4\u221e\u03c6\u03b5\u2229" +
            "\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248" +
            "\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u00a0";

    private Codecs() {
    }

    /// `b[off..off+len)` as Latin-1 characters: one copy straight into a compact
    /// (one byte per character) string, on the device and on the JVM alike.
    /// The Charset overload: the name overload looks the charset up on every
    /// call and declares a checked exception, whose handler costs a setjmp on
    /// ParparVM.
    static String latin1(byte[] b, int off, int len) {
        return new String(b, off, len, StandardCharsets.ISO_8859_1);
    }

    /// A normalised codec name: `utf-8`, `utf-8-sig`, `ascii`, `latin-1`, `utf-16`,
    /// `utf-16-le`, `utf-16-be` or `cp437`.
    static String normalize(String encoding) {
        // The spellings programs actually pass, answered without folding a copy.
        if ("utf-8".equals(encoding) || "utf8".equals(encoding) || "UTF-8".equals(encoding)) {
            return "utf-8";
        }
        if ("latin-1".equals(encoding) || "latin1".equals(encoding) || "iso-8859-1".equals(encoding)) {
            return "latin-1";
        }
        if ("ascii".equals(encoding)) {
            return "ascii";
        }
        String e = TypeMethods.asciiLower(encoding).replace('_', '-');
        if ("utf-8".equals(e) || "utf8".equals(e) || "u8".equals(e) || "utf".equals(e)) {
            return "utf-8";
        }
        if ("utf-8-sig".equals(e) || "utf8-sig".equals(e)) {
            return "utf-8-sig";
        }
        if ("ascii".equals(e) || "us-ascii".equals(e) || "646".equals(e)) {
            return "ascii";
        }
        if ("latin-1".equals(e) || "latin1".equals(e) || "iso-8859-1".equals(e) || "iso8859-1".equals(e)
                || "l1".equals(e) || "8859".equals(e) || "cp819".equals(e)) {
            return "latin-1";
        }
        if ("utf-16".equals(e) || "utf16".equals(e) || "u16".equals(e)) {
            return "utf-16";
        }
        if ("utf-16-le".equals(e) || "utf-16le".equals(e)) {
            return "utf-16-le";
        }
        if ("utf-16-be".equals(e) || "utf-16be".equals(e)) {
            return "utf-16-be";
        }
        if ("cp437".equals(e) || "437".equals(e) || "ibm437".equals(e)) {
            return "cp437";
        }
        throw Py.error(Py.LOOKUP_ERROR, "unknown encoding: " + encoding);
    }

    static byte[] encode(String s, String encoding, String errors) {
        String enc = normalize(encoding);
        if ("utf-8".equals(enc)) {
            return Utf8.encode(s);
        }
        if ("utf-8-sig".equals(enc)) {
            byte[] body = Utf8.encode(s);
            byte[] out = new byte[body.length + 3];
            out[0] = (byte) 0xef;
            out[1] = (byte) 0xbb;
            out[2] = (byte) 0xbf;
            System.arraycopy(body, 0, out, 3, body.length);
            return out;
        }
        if (enc.startsWith("utf-16")) {
            return encodeUtf16(s, enc);
        }
        if ("cp437".equals(enc)) {
            return encodeCp437(s, errors);
        }
        int limit = "ascii".equals(enc) ? 0x80 : 0x100;
        ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());
        int i = 0;
        while (i < s.length()) {
            int cp = Character.codePointAt(s, i);
            int pos = i;
            i += Character.charCount(cp);
            if (cp < limit) {
                out.write(cp);
            } else if ("ignore".equals(errors)) {
                continue;
            } else if ("replace".equals(errors)) {
                out.write('?');
            } else if ("xmlcharrefreplace".equals(errors)) {
                writeAscii(out, "&#" + cp + ";");
            } else if ("backslashreplace".equals(errors)) {
                String hex = Integer.toHexString(cp);
                String pad = cp < 0x100 ? "\\x00" : cp < 0x10000 ? "\\u0000" : "\\U00000000";
                writeAscii(out, pad.substring(0, pad.length() - hex.length()) + hex);
            } else {
                int at = PyStr.cpIndex(s, pos);
                throw Py.encodeError(limit == 0x80 ? "ascii" : "latin-1", s, at, at + 1,
                        "ordinal not in range(" + limit + ")");
            }
        }
        return out.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream out, String s) {
        for (int k = 0; k < s.length(); k++) {
            out.write(s.charAt(k));
        }
    }

    static String decode(byte[] b, String encoding, String errors) {
        String enc = normalize(encoding);
        if ("utf-8".equals(enc)) {
            return Utf8.decode(b, errors);
        }
        if ("utf-8-sig".equals(enc)) {
            if (b.length >= 3 && (b[0] & 0xff) == 0xef && (b[1] & 0xff) == 0xbb && (b[2] & 0xff) == 0xbf) {
                byte[] rest = new byte[b.length - 3];
                System.arraycopy(b, 3, rest, 0, rest.length);
                return Utf8.decode(rest, errors);
            }
            return Utf8.decode(b, errors);
        }
        if (enc.startsWith("utf-16")) {
            return decodeUtf16(b, enc, errors);
        }
        if ("cp437".equals(enc)) {
            StringBuilder sb = new StringBuilder(b.length);
            for (byte x : b) {
                int c = x & 0xff;
                sb.append(c < 0x80 ? (char) c : CP437_HIGH.charAt(c - 0x80));
            }
            return sb.toString();
        }
        boolean ascii = "ascii".equals(enc);
        // Latin-1, and ASCII until a byte above 0x7f: one char per byte into
        // an array. pdfminer decodes every content stream token this way, and
        // a StringBuilder appending char by char was a twentieth of the
        // conversion's time.
        int at = b.length;
        if (ascii) {
            at = 0;
            while (at < b.length && b[at] >= 0) {
                at++;
            }
        }
        if (at == b.length) {
            return latin1(b, 0, b.length);
        }
        StringBuilder sb = new StringBuilder(b.length);
        sb.append(latin1(b, 0, at));
        for (int i = at; i < b.length; i++) {
            int c = b[i] & 0xff;
            if (ascii && c >= 0x80) {
                if ("ignore".equals(errors)) {
                    continue;
                }
                if ("replace".equals(errors)) {
                    sb.append('\uFFFD');
                    continue;
                }
                throw Py.decodeError("ascii", b, i, i + 1, "ordinal not in range(128)");
            }
            sb.append((char) c);
        }
        return sb.toString();
    }

    /// `utf-16` writes a byte order mark and little-endian units, as CPython
    /// does on every platform Codename One runs on; the `-le`/`-be` forms
    /// write no mark. A Java string is already UTF-16, so a lone surrogate
    /// is written as it is, as `surrogatepass` would.
    private static byte[] encodeUtf16(String s, String enc) {
        boolean big = "utf-16-be".equals(enc);
        boolean bom = "utf-16".equals(enc);
        byte[] out = new byte[s.length() * 2 + (bom ? 2 : 0)];
        int o = 0;
        if (bom) {
            out[o++] = (byte) 0xff;
            out[o++] = (byte) 0xfe;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (big) {
                out[o++] = (byte) (c >> 8);
                out[o++] = (byte) c;
            } else {
                out[o++] = (byte) c;
                out[o++] = (byte) (c >> 8);
            }
        }
        return out;
    }

    /// `utf-16` reads a byte order mark when there is one and takes
    /// little-endian otherwise; the `-le`/`-be` forms keep a mark as U+FEFF.
    private static String decodeUtf16(byte[] b, String enc, String errors) {
        boolean big = "utf-16-be".equals(enc);
        int i = 0;
        if ("utf-16".equals(enc) && b.length >= 2) {
            if ((b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xfe) {
                i = 2;
            } else if ((b[0] & 0xff) == 0xfe && (b[1] & 0xff) == 0xff) {
                big = true;
                i = 2;
            }
        }
        boolean strict = !"replace".equals(errors) && !"ignore".equals(errors);
        String name = "utf-16".equals(enc) ? "utf-16-" + (big ? "be" : "le") : enc;
        StringBuilder sb = new StringBuilder(b.length / 2);
        while (i + 1 < b.length) {
            char c = unit(b, i, big);
            if (Character.isHighSurrogate(c)) {
                if (i + 3 < b.length && Character.isLowSurrogate(unit(b, i + 2, big))) {
                    sb.append(c).append(unit(b, i + 2, big));
                    i += 4;
                    continue;
                }
                if (i + 3 >= b.length && strict) {
                    throw Py.decodeError(name, b, i, b.length, "unexpected end of data");
                }
                if (strict) {
                    throw Py.decodeError(name, b, i, i + 2, "illegal UTF-16 surrogate");
                }
            } else if (Character.isLowSurrogate(c)) {
                if (strict) {
                    throw Py.decodeError(name, b, i, i + 2, "illegal encoding");
                }
            } else {
                sb.append(c);
                i += 2;
                continue;
            }
            if ("replace".equals(errors)) {
                sb.append('\ufffd');
            }
            i += 2;
        }
        if (i < b.length) {
            if (strict) {
                throw Py.decodeError(name, b, i, b.length, "truncated data");
            }
            if ("replace".equals(errors)) {
                sb.append('\ufffd');
            }
        }
        return sb.toString();
    }

    private static char unit(byte[] b, int i, boolean big) {
        int hi = b[big ? i : i + 1] & 0xff;
        int lo = b[big ? i + 1 : i] & 0xff;
        return (char) (hi << 8 | lo);
    }

    private static byte[] encodeCp437(String s, String errors) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int at = c < 0x80 ? c : CP437_HIGH.indexOf(c);
            if (at >= 0) {
                out.write(c < 0x80 ? c : at + 0x80);
            } else if ("replace".equals(errors)) {
                out.write('?');
            } else if (!"ignore".equals(errors)) {
                throw Py.encodeError("charmap", s, PyStr.cpIndex(s, i), PyStr.cpIndex(s, i) + 1,
                        "character maps to <undefined>");
            }
        }
        return out.toByteArray();
    }

    /// `repr`'s escape of a code point: backslash-x for Latin-1, backslash-u
    /// below 0x10000, backslash-U above.
    static String escape(int cp) {
        if (cp < 0x100) {
            String h = Integer.toHexString(cp);
            return "\\x" + (h.length() < 2 ? "0" + h : h);
        }
        if (cp < 0x10000) {
            return "\\u" + hex4(cp);
        }
        String h = Integer.toHexString(cp);
        return "\\U" + "00000000".substring(h.length()) + h;
    }

    private static String hex4(int cp) {
        String h = Integer.toHexString(cp);
        StringBuilder sb = new StringBuilder();
        for (int k = h.length(); k < 4; k++) {
            sb.append('0');
        }
        return sb.append(h).toString();
    }
}
