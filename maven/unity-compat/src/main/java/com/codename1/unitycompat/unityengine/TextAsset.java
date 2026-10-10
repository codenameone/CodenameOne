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
package com.codename1.unitycompat.unityengine;

/// `UnityEngine.TextAsset`: a file of the project read as text or as bytes
/// -- a level, a table, a document.
///
/// The scene compiler puts the contents into the generated code, so there
/// is nothing to open or to find when a script asks: the text is a string
/// constant. A file with the `.bytes` extension is kept byte for byte and
/// decoded as UTF-8 if its text is asked for; any other is kept as its
/// text, a byte order mark left out, and encoded as UTF-8 if its bytes are.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class TextAsset extends Object {
    private String text;
    private byte[] bytes;

    public TextAsset(String name, String text) {
        this.name = name;
        this.text = text;
    }

    /// A file kept as bytes, one per character of `packed`.
    public static TextAsset $bytes(String name, String packed) {
        TextAsset asset = new TextAsset(name, null);
        byte[] b = new byte[packed.length()];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) packed.charAt(i);
        }
        asset.bytes = b;
        return asset;
    }

    public String get_text() {
        if (text == null) {
            text = decode(bytes);
        }
        return text;
    }

    /// A copy: the caller owns what it is handed, as it does in Unity.
    public byte[] get_bytes() {
        if (bytes == null) {
            bytes = encode(text);
        }
        byte[] copy = new byte[bytes.length];
        System.arraycopy(bytes, 0, copy, 0, copy.length);
        return copy;
    }

    @Override
    public String toString() {
        return get_text();
    }

    // UTF-8 by hand: the same answer on every target, including for bytes
    // that are not valid UTF-8, each of which reads as U+FFFD.

    private static String decode(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length);
        int at = b.length >= 3 && (b[0] & 0xff) == 0xef && (b[1] & 0xff) == 0xbb && (b[2] & 0xff) == 0xbf ? 3 : 0;
        while (at < b.length) {
            int c = b[at++] & 0xff;
            int more;
            int code;
            if (c < 0x80) {
                sb.append((char) c);
                continue;
            } else if (c >= 0xc2 && c < 0xe0) {
                more = 1;
                code = c & 0x1f;
            } else if (c >= 0xe0 && c < 0xf0) {
                more = 2;
                code = c & 0x0f;
            } else if (c >= 0xf0 && c < 0xf5) {
                more = 3;
                code = c & 0x07;
            } else {
                sb.append((char) 0xfffd);
                continue;
            }
            int end = at + more;
            boolean valid = end <= b.length;
            for (int i = at; valid && i < end; i++) {
                int next = b[i] & 0xff;
                valid = (next & 0xc0) == 0x80;
                code = (code << 6) | (next & 0x3f);
            }
            if (!valid || (more == 2 && code < 0x800) || (more == 3 && (code < 0x10000 || code > 0x10ffff))
                    || (code >= 0xd800 && code < 0xe000)) {
                sb.append((char) 0xfffd);
                continue;
            }
            at = end;
            if (code >= 0x10000) {
                code -= 0x10000;
                sb.append((char) (0xd800 + (code >> 10)));
                sb.append((char) (0xdc00 + (code & 0x3ff)));
            } else {
                sb.append((char) code);
            }
        }
        return sb.toString();
    }

    private static byte[] encode(String s) {
        int n = s.length();
        byte[] out = new byte[n * 3];
        int size = 0;
        for (int i = 0; i < n; i++) {
            int c = s.charAt(i);
            if (c >= 0xd800 && c < 0xdc00 && i + 1 < n && s.charAt(i + 1) >= 0xdc00 && s.charAt(i + 1) < 0xe000) {
                c = 0x10000 + ((c - 0xd800) << 10) + (s.charAt(++i) - 0xdc00);
            } else if (c >= 0xd800 && c < 0xe000) {
                c = 0xfffd;
            }
            if (c < 0x80) {
                out[size++] = (byte) c;
            } else if (c < 0x800) {
                out[size++] = (byte) (0xc0 | (c >> 6));
                out[size++] = (byte) (0x80 | (c & 0x3f));
            } else if (c < 0x10000) {
                out[size++] = (byte) (0xe0 | (c >> 12));
                out[size++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                out[size++] = (byte) (0x80 | (c & 0x3f));
            } else {
                // Two characters made this one, so there was room for four.
                out[size++] = (byte) (0xf0 | (c >> 18));
                out[size++] = (byte) (0x80 | ((c >> 12) & 0x3f));
                out[size++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                out[size++] = (byte) (0x80 | (c & 0x3f));
            }
        }
        byte[] exact = new byte[size];
        System.arraycopy(out, 0, exact, 0, size);
        return exact;
    }
}
