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

import java.io.UnsupportedEncodingException;

/// UTF-8 coding with Python's strict error behaviour.
final class Utf8 {
    private Utf8() {
    }

    static byte[] encode(String s) {
        // The platform's encoder: on ParparVM a vectorized native that
        // writes a compact (Latin-1) string with one copy, and on a JDK an
        // intrinsic. A lone surrogate -- which only an escape, chr() or a
        // surrogateescape decode can put into a str -- comes out as U+FFFD
        // where CPython's strict handler raises: finding one first would be
        // a scan of every string in Java, which is what this replaces.
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return encodeChars(s);
        }
    }

    /// The Java encoder, for a platform without a UTF-8 charset.
    static byte[] encodeChars(String s) {
        int n = s.length();
        char[] cs = new char[n];
        s.getChars(0, n, cs, 0);
        int i = 0;
        for (; i < n; i++) {
            if (cs[i] >= 0x80) {
                break;
            }
        }
        if (i == n) {
            byte[] out = new byte[n];
            for (int k = 0; k < n; k++) {
                out[k] = (byte) cs[k];
            }
            return out;
        }
        byte[] buf = new byte[i + (n - i) * 3];
        for (int k = 0; k < i; k++) {
            buf[k] = (byte) cs[k];
        }
        int o = i;
        for (; i < n; i++) {
            int c = cs[i];
            if (c < 0x80) {
                buf[o] = (byte) c;
                o++;
            } else if (c < 0x800) {
                buf[o] = (byte) (0xc0 | c >> 6);
                buf[o + 1] = (byte) (0x80 | c & 0x3f);
                o += 2;
            } else if ((c & 0xfc00) == 0xd800 && i + 1 < n && (cs[i + 1] & 0xfc00) == 0xdc00) {
                c = ((c & 0x3ff) << 10 | cs[i + 1] & 0x3ff) + 0x10000;
                i++;
                buf[o] = (byte) (0xf0 | c >> 18);
                buf[o + 1] = (byte) (0x80 | c >> 12 & 0x3f);
                buf[o + 2] = (byte) (0x80 | c >> 6 & 0x3f);
                buf[o + 3] = (byte) (0x80 | c & 0x3f);
                o += 4;
            } else {
                buf[o] = (byte) (0xe0 | c >> 12);
                buf[o + 1] = (byte) (0x80 | c >> 6 & 0x3f);
                buf[o + 2] = (byte) (0x80 | c & 0x3f);
                o += 3;
            }
        }
        byte[] out = new byte[o];
        System.arraycopy(buf, 0, out, 0, o);
        return out;
    }

    static String decode(byte[] b) {
        return decode(b, "strict");
    }

    /// Decodes UTF-8 under a Python error handler: `strict` raises,
    /// `replace` substitutes U+FFFD for each bad byte, `ignore` drops it.
    static String decode(byte[] b, String errors) {
        // The platform's decoder (a vectorized native on ParparVM) replaces
        // each malformed sequence with U+FFFD. A result without one proves
        // the input valid, and every handler agrees on valid input; the Java
        // decoder below runs only for the rest, raising or replacing exactly
        // as Python does.
        String fast;
        try {
            fast = new String(b, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            fast = null;
        }
        if (fast != null && fast.indexOf(0xFFFD) < 0) {
            return fast;
        }
        return decodeChecked(b, errors);
    }

    private static String decodeChecked(byte[] b, String errors) {
        boolean strict = !"replace".equals(errors) && !"ignore".equals(errors);
        boolean replace = "replace".equals(errors);
        int i = 0;
        for (; i < b.length; i++) {
            if (b[i] < 0) {
                break;
            }
        }
        if (i == b.length) {
            // All ASCII: the bytes are the characters, one copy into Latin-1 storage.
            return Codecs.latin1(b, 0, b.length);
        }
        // Chars straight into an array no longer than the bytes: a
        // StringBuilder paid a capacity check and a call per character.
        char[] sb = new char[b.length];
        int o = 0;
        int pairs = 0;
        for (; o < i; o++) {
            sb[o] = (char) b[o];
        }
        int n = b.length;
        while (i < n) {
            int c = b[i] & 0xff;
            int need;
            int cp;
            if (c < 0x80) {
                sb[o] = (char) c;
                o++;
                i++;
                continue;
            }
            // The well-formed two and three byte forms, tested in line: no
            // lead here has a narrowed second byte (E0 and ED do, and go the
            // long way through `continues`).
            if (c >= 0xc2 && c < 0xe0 && i + 1 < n && (b[i + 1] & 0xc0) == 0x80) {
                sb[o] = (char) ((c & 0x1f) << 6 | b[i + 1] & 0x3f);
                o++;
                i += 2;
                continue;
            }
            if (c > 0xe0 && c < 0xf0 && c != 0xed && i + 2 < n && (b[i + 1] & 0xc0) == 0x80
                    && (b[i + 2] & 0xc0) == 0x80) {
                sb[o] = (char) ((c & 0x0f) << 12 | (b[i + 1] & 0x3f) << 6 | b[i + 2] & 0x3f);
                o++;
                i += 3;
                continue;
            }
            if (c >= 0xc2 && c < 0xe0) {
                need = 1;
                cp = c & 0x1f;
            } else if (c >= 0xe0 && c < 0xf0) {
                need = 2;
                cp = c & 0x0f;
            } else if (c >= 0xf0 && c < 0xf5) {
                need = 3;
                cp = c & 0x07;
            } else {
                if (strict) {
                    throw Py.decodeError("utf-8", b, i, i + 1, "invalid start byte");
                }
                if (replace) {
                    sb[o++] = '\ufffd';
                }
                i++;
                continue;
            }
            boolean bad = false;
            if (i + need >= b.length) {
                if (strict) {
                    for (int k = 1; i + k < b.length; k++) {
                        if (!continues(c, k, b[i + k] & 0xff)) {
                            throw Py.decodeError("utf-8", b, i, i + k, "invalid continuation byte");
                        }
                    }
                    throw Py.decodeError("utf-8", b, i, b.length, "unexpected end of data");
                }
                bad = true;
            } else {
                for (int k = 1; k <= need; k++) {
                    int cc = b[i + k] & 0xff;
                    if (!continues(c, k, cc)) {
                        if (strict) {
                            throw Py.decodeError("utf-8", b, i, i + k, "invalid continuation byte");
                        }
                        bad = true;
                        break;
                    }
                    cp = cp << 6 | cc & 0x3f;
                }
            }
            if (bad) {
                if (replace) {
                    sb[o++] = '\ufffd';
                }
                i++;
                continue;
            }
            if (cp >= 0x10000) {
                sb[o] = Character.highSurrogate(cp);
                sb[o + 1] = Character.lowSurrogate(cp);
                o += 2;
                pairs++;
            } else {
                sb[o] = (char) cp;
                o++;
            }
            i += need + 1;
        }
        String r = new String(sb, 0, o);
        PyStr.knownSurrogates(r, pairs);
        return r;
    }

    /// Whether `cc` may be the `k`th byte of a sequence led by `lead`. The
    /// second byte's range is narrowed after E0, ED, F0 and F4, so an
    /// overlong form, an encoded surrogate or a code point past U+10FFFF is
    /// rejected there, at the byte CPython rejects it at.
    private static boolean continues(int lead, int k, int cc) {
        int lo = 0x80;
        int hi = 0xbf;
        if (k == 1) {
            if (lead == 0xe0) {
                lo = 0xa0;
            } else if (lead == 0xed) {
                hi = 0x9f;
            } else if (lead == 0xf0) {
                lo = 0x90;
            } else if (lead == 0xf4) {
                hi = 0x8f;
            }
        }
        return cc >= lo && cc <= hi;
    }
}
