/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.cli;

/// Turns raw terminal bytes into [Key]s: UTF-8 characters, control keys and
/// the CSI (`ESC [`) and SS3 (`ESC O`) sequences xterm, the Linux console,
/// macOS Terminal and Windows Terminal send for arrows, Home, End, paging and
/// function keys.
final class KeyReader {
    /// How long to wait for the rest of an escape sequence before deciding
    /// the user pressed Escape on its own.
    static final int SEQUENCE_TIMEOUT_MS = 50;

    private final TerminalBackend term;

    KeyReader(TerminalBackend term) {
        this.term = term;
    }

    Key read() {
        int b = next(-1);
        return decode(b, false);
    }

    /// Bytes read ahead that belong to the next key: a byte that ended a
    /// malformed UTF-8 sequence, the second of two ESCs. At most two.
    private final int[] pending = new int[2];
    private int pendingCount;

    private void unread(int b) {
        if (b >= 0 && pendingCount < pending.length) {
            pending[pendingCount++] = b;
        }
    }

    private int next(int timeoutMs) {
        if (pendingCount > 0) {
            int b = pending[0];
            pendingCount--;
            for (int i = 0; i < pendingCount; i++) {
                pending[i] = pending[i + 1];
            }
            return b;
        }
        return term.readByte(timeoutMs);
    }

    private Key decode(int b, boolean alt) {
        if (b < 0) {
            return Key.of(Key.Type.EOF);
        }
        switch (b) {
            case 27:
                return escape(alt);
            case 13:
            case 10:
                return new Key(Key.Type.ENTER, 0, alt);
            case 9:
                return new Key(Key.Type.TAB, 0, alt);
            case 127:
            case 8:
                return new Key(Key.Type.BACKSPACE, 0, alt);
            case 3:
                return new Key(Key.Type.CTRL_C, 0, alt);
            case 4:
                return new Key(Key.Type.CTRL_D, 0, alt);
            default:
                break;
        }
        if (b == 0) {
            // Ctrl-Space (and Ctrl-@) send NUL.
            return new Key(Key.Type.CTRL, ' ', alt);
        }
        if (b >= 28 && b < 32) {
            return new Key(Key.Type.CTRL, "\\]^_".charAt(b - 28), alt);
        }
        if (b < 32) {
            return new Key(Key.Type.CTRL, 'a' + b - 1, alt);
        }
        if (b < 0x80) {
            return new Key(Key.Type.CHAR, b, alt);
        }
        return utf8(b, alt);
    }

    private Key escape(boolean alt) {
        int next = next(SEQUENCE_TIMEOUT_MS);
        if (next == -2 || next == -1) {
            return new Key(Key.Type.ESCAPE, 0, alt);
        }
        if (next == '[') {
            return csi(alt);
        }
        if (next == 'O') {
            return ss3(alt);
        }
        if (next == 27) {
            // ESC ESC [ A is Alt with an arrow on some terminals; anything
            // else after two ESCs is two presses, and the second is kept.
            int third = next(SEQUENCE_TIMEOUT_MS);
            if (third == '[') {
                return csi(true);
            }
            if (third == 'O') {
                return ss3(true);
            }
            unread(27);
            unread(third);
            return new Key(Key.Type.ESCAPE, 0, alt);
        }
        return decode(next, true);
    }

    private Key csi(boolean alt) {
        StringBuilder params = new StringBuilder();
        int b;
        while (true) {
            b = next(SEQUENCE_TIMEOUT_MS);
            if (b < 0) {
                // A sequence cut off for longer than a terminal ever pauses
                // inside one: what arrived is dropped rather than typed.
                return new Key(Key.Type.ESCAPE, 0, alt);
            }
            if (b >= 0x40 && b <= 0x7e) {
                break;
            }
            params.append((char) b);
        }
        int first = firstNumber(params);
        boolean modAlt = alt || modifier(params) == 3;
        switch (b) {
            case 'A':
                return new Key(Key.Type.UP, 0, modAlt);
            case 'B':
                return new Key(Key.Type.DOWN, 0, modAlt);
            case 'C':
                return new Key(Key.Type.RIGHT, 0, modAlt);
            case 'D':
                return new Key(Key.Type.LEFT, 0, modAlt);
            case 'H':
                return new Key(Key.Type.HOME, 0, modAlt);
            case 'F':
                return new Key(Key.Type.END, 0, modAlt);
            case 'Z':
                return new Key(Key.Type.BACKTAB, 0, modAlt);
            case 'P':
                return new Key(Key.Type.FUNCTION, 1, modAlt);
            case 'Q':
                return new Key(Key.Type.FUNCTION, 2, modAlt);
            case 'R':
                return new Key(Key.Type.FUNCTION, 3, modAlt);
            case 'S':
                return new Key(Key.Type.FUNCTION, 4, modAlt);
            case '~':
                return tilde(first, modAlt);
            default:
                return new Key(Key.Type.UNKNOWN, 0, modAlt);
        }
    }

    private static Key tilde(int n, boolean alt) {
        switch (n) {
            case 1:
            case 7:
                return new Key(Key.Type.HOME, 0, alt);
            case 2:
                return new Key(Key.Type.INSERT, 0, alt);
            case 3:
                return new Key(Key.Type.DELETE, 0, alt);
            case 4:
            case 8:
                return new Key(Key.Type.END, 0, alt);
            case 5:
                return new Key(Key.Type.PAGE_UP, 0, alt);
            case 6:
                return new Key(Key.Type.PAGE_DOWN, 0, alt);
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                return new Key(Key.Type.FUNCTION, n - 10, alt);
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
                return new Key(Key.Type.FUNCTION, n - 11, alt);
            case 23:
            case 24:
                return new Key(Key.Type.FUNCTION, n - 12, alt);
            default:
                return new Key(Key.Type.UNKNOWN, 0, alt);
        }
    }

    private Key ss3(boolean alt) {
        int b = next(SEQUENCE_TIMEOUT_MS);
        switch (b) {
            case 'A':
                return new Key(Key.Type.UP, 0, alt);
            case 'B':
                return new Key(Key.Type.DOWN, 0, alt);
            case 'C':
                return new Key(Key.Type.RIGHT, 0, alt);
            case 'D':
                return new Key(Key.Type.LEFT, 0, alt);
            case 'H':
                return new Key(Key.Type.HOME, 0, alt);
            case 'F':
                return new Key(Key.Type.END, 0, alt);
            case 'M':
                return new Key(Key.Type.ENTER, 0, alt);
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
                return new Key(Key.Type.FUNCTION, b - 'P' + 1, alt);
            default:
                return b < 0 ? new Key(Key.Type.ESCAPE, 0, alt) : new Key(Key.Type.UNKNOWN, 0, alt);
        }
    }

    private static int firstNumber(CharSequence params) {
        int v = 0;
        boolean any = false;
        for (int i = 0; i < params.length(); i++) {
            char c = params.charAt(i);
            if (c < '0' || c > '9') {
                break;
            }
            v = v * 10 + (c - '0');
            any = true;
        }
        return any ? v : 1;
    }

    /// The xterm modifier parameter after `;` (2 Shift, 3 Alt, 5 Ctrl), or 1.
    private static int modifier(CharSequence params) {
        int semi = -1;
        for (int i = 0; i < params.length(); i++) {
            if (params.charAt(i) == ';') {
                semi = i;
                break;
            }
        }
        if (semi < 0) {
            return 1;
        }
        return firstNumber(params.subSequence(semi + 1, params.length()));
    }

    private Key utf8(int lead, boolean alt) {
        int extra;
        int cp;
        int min;
        if ((lead & 0xe0) == 0xc0) {
            extra = 1;
            cp = lead & 0x1f;
            min = 0x80;
        } else if ((lead & 0xf0) == 0xe0) {
            extra = 2;
            cp = lead & 0x0f;
            min = 0x800;
        } else if ((lead & 0xf8) == 0xf0) {
            extra = 3;
            cp = lead & 0x07;
            min = 0x10000;
        } else {
            return new Key(Key.Type.UNKNOWN, 0, alt);
        }
        for (int i = 0; i < extra; i++) {
            int b = next(SEQUENCE_TIMEOUT_MS);
            if (b < 0 || (b & 0xc0) != 0x80) {
                // Not a continuation: it starts the next key (an ESC of an
                // arrow, say), so it is kept rather than swallowed.
                unread(b);
                return new Key(Key.Type.UNKNOWN, 0, alt);
            }
            cp = (cp << 6) | (b & 0x3f);
        }
        // Overlong forms, surrogates and values past U+10FFFF are not
        // characters; Character.toChars would throw on the last two.
        if (cp < min || cp > 0x10ffff || cp >= 0xd800 && cp <= 0xdfff) {
            return new Key(Key.Type.UNKNOWN, 0, alt);
        }
        return new Key(Key.Type.CHAR, cp, alt);
    }
}
