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

package com.codename1.ui.util;

import java.io.DataInputStream;
import java.io.IOException;

/// The strings of one theme load, each stored once.
///
/// A theme repeats a few values thousands of times -- `0`, `ffffff`,
/// `native:MainRegular` -- and reading each with `readUTF`, `toHexString` or
/// `"" + int` made a byte array, a char array and a String per entry, almost all
/// of them garbage on arrival. The pages that garbage filled stay resident after
/// it is collected, so deduplicating the result afterwards saves nothing. This
/// decodes into reusable buffers and creates a String only for a value it has not
/// seen in this load; a repeated value allocates nothing.
final class ThemeStrings {
    private byte[] bytes = new byte[64];
    private char[] chars = new char[64];
    private String[] table;
    private int size;

    /// `entries` is the entry count the theme declares. Every key is a distinct
    /// string and the values add a few hundred more, so a table sized for that up
    /// front never has to grow: growing a 5,000 entry theme from a small table
    /// discarded four tables on the way.
    ThemeStrings(int entries) {
        int want = Math.max(entries, 0) * 2 + 1024;
        int len = 1024;
        while (len < want && len < (1 << 20)) {
            len <<= 1;
        }
        table = new String[len];
    }

    /// Reads a string written by `DataOutputStream.writeUTF` (modified UTF-8).
    String readUTF(DataInputStream in) throws IOException {
        int len = in.readUnsignedShort();
        if (bytes.length < len) {
            bytes = new byte[len];
        }
        in.readFully(bytes, 0, len);
        if (chars.length < len) {
            chars = new char[len];
        }
        byte[] b = bytes;
        char[] c = chars;
        int n = 0;
        int i = 0;
        while (i < len) {
            int x = b[i] & 0xff;
            if (x < 0x80) {
                c[n++] = (char) x;
                i++;
            } else if ((x & 0xe0) == 0xc0) {
                if (i + 1 >= len || (b[i + 1] & 0xc0) != 0x80) {
                    throw new IOException("Malformed UTF-8 in theme");
                }
                c[n++] = (char) (((x & 0x1f) << 6) | (b[i + 1] & 0x3f));
                i += 2;
            } else if ((x & 0xf0) == 0xe0) {
                if (i + 2 >= len || (b[i + 1] & 0xc0) != 0x80 || (b[i + 2] & 0xc0) != 0x80) {
                    throw new IOException("Malformed UTF-8 in theme");
                }
                c[n++] = (char) (((x & 0x0f) << 12) | ((b[i + 1] & 0x3f) << 6) | (b[i + 2] & 0x3f));
                i += 3;
            } else {
                throw new IOException("Malformed UTF-8 in theme");
            }
        }
        return intern(n);
    }

    /// `Integer.toHexString(v)`.
    String hex(int v) {
        char[] c = chars;
        int n = 0;
        int shift = 28;
        while (shift > 0 && ((v >>> shift) & 0xf) == 0) {
            shift -= 4;
        }
        for (; shift >= 0; shift -= 4) {
            int d = (v >>> shift) & 0xf;
            c[n++] = (char) (d < 10 ? '0' + d : 'a' + d - 10);
        }
        return intern(n);
    }

    /// `"" + v` for a value from 0 to 255, which is all a theme stores this way.
    String decimal(int v) {
        char[] c = chars;
        int n = 0;
        if (v >= 100) {
            c[n++] = (char) ('0' + v / 100);
        }
        if (v >= 10) {
            c[n++] = (char) ('0' + v / 10 % 10);
        }
        c[n++] = (char) ('0' + v % 10);
        return intern(n);
    }

    /// `"" + a + "," + b + "," + c + "," + d`, the form padding and margin are
    /// stored in. Each `+` on a float made a String of its own.
    String floats(float a, float b, float c, float d) {
        ensureChars(64);
        int n = appendFloat(a, 0);
        chars[n++] = ',';
        n = appendFloat(b, n);
        chars[n++] = ',';
        n = appendFloat(c, n);
        chars[n++] = ',';
        n = appendFloat(d, n);
        return intern(n);
    }

    /// `"" + a + "," + b + "," + c + "," + d` for the integer form older themes use.
    String ints(int a, int b, int c, int d) {
        ensureChars(64);
        int n = appendInt(a, 0);
        chars[n++] = ',';
        n = appendInt(b, n);
        chars[n++] = ',';
        n = appendInt(c, n);
        chars[n++] = ',';
        n = appendInt(d, n);
        return intern(n);
    }

    /// Appends `Float.toString(f)`. A theme's values have at most one decimal
    /// (2.0, 0.5, 48.9); those are written digit by digit, which is what
    /// `Float.toString` prints for them, and anything else is handed to it.
    /// The digits are the shortest form that reads back as `f`; a VM whose
    /// `Float.toString` prints more digits for such a value produced a
    /// longer string before, never a different float once the theme parses it.
    private int appendFloat(float f, int n) {
        float tenths = f * 10f;
        int t = (int) tenths;
        if (t == tenths && t > -10000000 && t < 10000000 && (float) (t / 10.0) == f && !(t == 0 && 1 / f < 0)) {
            if (t < 0) {
                chars[n++] = '-';
                t = -t;
            }
            n = appendInt(t / 10, n);
            chars[n++] = '.';
            chars[n++] = (char) ('0' + t % 10);
            return n;
        }
        String s = Float.toString(f);
        int len = s.length();
        ensureChars(n + len + 48);
        s.getChars(0, len, chars, n);
        return n + len;
    }

    private int appendInt(int v, int n) {
        if (v < 0) {
            String s = String.valueOf(v);
            int len = s.length();
            s.getChars(0, len, chars, n);
            return n + len;
        }
        int start = n;
        do {
            chars[n++] = (char) ('0' + v % 10);
            v /= 10;
        } while (v != 0);
        for (int i = start, j = n - 1; i < j; i++, j--) {
            char x = chars[i];
            chars[i] = chars[j];
            chars[j] = x;
        }
        return n;
    }

    private void ensureChars(int n) {
        if (chars.length < n) {
            char[] c = new char[n];
            System.arraycopy(chars, 0, c, 0, chars.length);
            chars = c;
        }
    }

    /// The shared copy of a string built elsewhere.
    String share(String s) {
        int n = s.length();
        if (chars.length < n) {
            chars = new char[n];
        }
        s.getChars(0, n, chars, 0);
        return intern(n);
    }

    private String intern(int n) {
        char[] c = chars;
        int h = 0;
        for (int i = 0; i < n; i++) {
            h = 31 * h + c[i];
        }
        String[] t = table;
        int mask = t.length - 1;
        int i = (h ^ (h >>> 16)) & mask;
        while (true) {
            String s = t[i];
            if (s == null) {
                break;
            }
            if (s.length() == n && same(s, c, n)) {
                return s;
            }
            i = (i + 1) & mask;
        }
        String s = new String(c, 0, n);
        t[i] = s;
        size++;
        if (size * 2 > t.length) {
            grow();
        }
        return s;
    }

    private static boolean same(String s, char[] c, int n) {
        for (int i = 0; i < n; i++) {
            if (s.charAt(i) != c[i]) {
                return false;
            }
        }
        return true;
    }

    private void grow() {
        String[] old = table;
        String[] t = new String[old.length * 2];
        int mask = t.length - 1;
        for (String s : old) {
            if (s != null) {
                int h = s.hashCode();
                int i = (h ^ (h >>> 16)) & mask;
                while (t[i] != null) {
                    i = (i + 1) & mask;
                }
                t[i] = s;
            }
        }
        table = t;
    }
}
