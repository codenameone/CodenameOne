/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package dart.convert;

import dart.core.DartList;
import dart.core.FormatException;
import dart.typed_data.Uint8List;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * The byte codecs of {@code dart:convert}: {@code utf8}, {@code ascii}, {@code latin1},
 * {@code base64} and {@code base64Url}. A byte input may be a {@link Uint8List} or any
 * {@code List<int>}; byte outputs are {@link Uint8List}, as in Dart.
 */
public final class ByteCodec {

    static final int UTF8 = 0;
    static final int ASCII = 1;
    static final int LATIN1 = 2;
    static final int BASE64 = 3;
    static final int BASE64_URL = 4;

    private final int kind;

    ByteCodec(int kind) {
        this.kind = kind;
    }

    public String name() {
        switch (kind) {
            case UTF8:
                return "utf-8";
            case ASCII:
                return "us-ascii";
            case LATIN1:
                return "iso-8859-1";
            default:
                return "base64";
        }
    }

    /** Text codecs: String to bytes. Base64: bytes to String. */
    public Object encode(Object input) {
        if (kind == BASE64 || kind == BASE64_URL) {
            return base64(bytesOf(input), kind == BASE64_URL);
        }
        String s = (String) input;
        return Uint8List.fromBytes(textBytes(s));
    }

    /** Text codecs: bytes to String. Base64: String to bytes. */
    public Object decode(Object input) {
        return decode(input, null);
    }

    public Object decode(Object input, Boolean allowMalformed) {
        if (kind == BASE64 || kind == BASE64_URL) {
            return Uint8List.fromBytes(unbase64((String) input));
        }
        return text(bytesOf(input));
    }

    public ByteCodec encoder() {
        return this;
    }

    public ByteCodec decoder() {
        return this;
    }

    /** {@code utf8.encoder.convert(s)} / {@code utf8.decoder.convert(b)}, by input type. */
    public Object convert(Object input) {
        boolean textIn = input instanceof String;
        if (kind == BASE64 || kind == BASE64_URL) {
            return textIn ? decode(input) : encode(input);
        }
        return textIn ? encode(input) : decode(input);
    }

    private byte[] textBytes(String s) {
        try {
            switch (kind) {
                case UTF8:
                    return s.getBytes("UTF-8");
                default:
                    byte[] b = new byte[s.length()];
                    int limit = kind == ASCII ? 0x7f : 0xff;
                    for (int i = 0; i < b.length; i++) {
                        char c = s.charAt(i);
                        if (c > limit) {
                            throw new dart.core.ArgumentError("Contains invalid characters.");
                        }
                        b[i] = (byte) c;
                    }
                    return b;
            }
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String text(byte[] b) {
        try {
            if (kind == UTF8) {
                int off = b.length >= 3 && (b[0] & 0xff) == 0xef && (b[1] & 0xff) == 0xbb
                        && (b[2] & 0xff) == 0xbf ? 3 : 0;
                return new String(b, off, b.length - off, "UTF-8");
            }
            char[] c = new char[b.length];
            for (int i = 0; i < b.length; i++) {
                int v = b[i] & 0xff;
                if (kind == ASCII && v > 0x7f) {
                    throw new FormatException("Invalid value in input: " + v);
                }
                c[i] = (char) v;
            }
            return new String(c);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    static byte[] bytesOf(Object input) {
        if (input instanceof Uint8List) {
            return ((Uint8List) input).toBytes();
        }
        if (input instanceof byte[]) {
            return (byte[]) input;
        }
        if (input instanceof List) {
            List<?> l = (List<?>) input;
            byte[] b = new byte[l.size()];
            for (int i = 0; i < b.length; i++) {
                Object o = l.get(i);
                if (!(o instanceof Number)) {
                    throw new dart.core.TypeError("Expected a List<int>");
                }
                b[i] = (byte) ((Number) o).intValue();
            }
            return b;
        }
        throw new dart.core.TypeError("Expected a List<int>");
    }

    private static final String STD = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final String URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    static String base64(byte[] b, boolean url) {
        String alpha = url ? URL : STD;
        StringBuilder sb = new StringBuilder((b.length + 2) / 3 * 4);
        for (int i = 0; i < b.length; i += 3) {
            int n = (b[i] & 0xff) << 16;
            if (i + 1 < b.length) {
                n |= (b[i + 1] & 0xff) << 8;
            }
            if (i + 2 < b.length) {
                n |= b[i + 2] & 0xff;
            }
            sb.append(alpha.charAt((n >> 18) & 63)).append(alpha.charAt((n >> 12) & 63));
            sb.append(i + 1 < b.length ? alpha.charAt((n >> 6) & 63) : '=');
            sb.append(i + 2 < b.length ? alpha.charAt(n & 63) : '=');
        }
        return sb.toString();
    }

    /** Accepts both alphabets, and missing padding, as Dart's decoder does. */
    static byte[] unbase64(String s) {
        byte[] out = new byte[s.length() * 3 / 4 + 3];
        int n = 0;
        int acc = 0;
        int bits = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int v;
            if (c >= 'A' && c <= 'Z') {
                v = c - 'A';
            } else if (c >= 'a' && c <= 'z') {
                v = c - 'a' + 26;
            } else if (c >= '0' && c <= '9') {
                v = c - '0' + 52;
            } else if (c == '+' || c == '-') {
                v = 62;
            } else if (c == '/' || c == '_') {
                v = 63;
            } else if (c == '=') {
                break;
            } else {
                throw new FormatException("Invalid character (at character " + (i + 1) + ")");
            }
            acc = (acc << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out[n++] = (byte) (acc >> bits);
            }
        }
        byte[] r = new byte[n];
        System.arraycopy(out, 0, r, 0, n);
        return r;
    }

    /** {@code LineSplitter.split(s)} and {@code const LineSplitter().convert(s)}. */
    public static DartList<String> splitLines(String s) {
        DartList<String> out = new DartList<String>();
        int start = 0;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r') {
                out.add(s.substring(start, i));
                if (c == '\r' && i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                }
                start = i + 1;
            }
            i++;
        }
        if (start < s.length()) {
            out.add(s.substring(start));
        }
        return out;
    }
}
