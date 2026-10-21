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
public final class ByteCodec implements dart.async.StreamTransformer<Object, Object> {

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
        byte[] b = bytesOf(input);
        if (kind == UTF8 && !Boolean.TRUE.equals(allowMalformed)) {
            checkUtf8(b, b.length);
        }
        return text(b, true);
    }

    /**
     * Dart's decoder throws FormatException on malformed UTF-8 unless allowMalformed;
     * Java's String constructor would silently substitute U+FFFD instead.
     */
    static void checkUtf8(byte[] b, int end) {
        int i = 0;
        while (i < end) {
            int v = b[i] & 0xff;
            int n;
            int min;
            if (v < 0x80) {
                i++;
                continue;
            } else if (v >= 0xc2 && v <= 0xdf) {
                n = 1;
                min = 0x80;
            } else if (v >= 0xe0 && v <= 0xef) {
                n = 2;
                min = 0x800;
            } else if (v >= 0xf0 && v <= 0xf4) {
                n = 3;
                min = 0x10000;
            } else {
                throw new FormatException("Unexpected extension byte (at offset " + i + ")");
            }
            if (i + n >= end) {
                throw new FormatException("Unfinished UTF-8 octet sequence (at offset " + i + ")");
            }
            int cp = v & (0x3f >> n);
            for (int k = 1; k <= n; k++) {
                int c = b[i + k] & 0xff;
                if ((c & 0xc0) != 0x80) {
                    throw new FormatException("Missing extension byte (at offset " + (i + k) + ")");
                }
                cp = (cp << 6) | (c & 0x3f);
            }
            if (cp < min || cp > 0x10ffff || (cp >= 0xd800 && cp <= 0xdfff)) {
                throw new FormatException("Invalid UTF-8 encoding (at offset " + i + ")");
            }
            i += n + 1;
        }
    }

    /**
     * {@code stream.transform(utf8.decoder)}: chunk by chunk. A UTF-8 sequence split
     * across two chunks is held back until the rest of it arrives.
     */
    @Override
    public dart.async.Stream<Object> bind(final dart.async.Stream<Object> source) {
        return dart.async.AsyncStar.asyncStar(new dart.runtime.Funcs.VoidFunc1<dart.async.AsyncStar.Sink<Object>>() {
            @Override
            public void call(dart.async.AsyncStar.Sink<Object> sink) {
                dart.async.StreamIterator<Object> it = new dart.async.StreamIterator<Object>(source);
                byte[] carry = new byte[0];
                // Only the BOM at the very start of the stream is dropped; one opening a
                // later chunk is a real U+FEFF in the middle of the text.
                boolean atStart = true;
                // Base64 works in whole groups -- three bytes encode to four characters --
                // so a chunk's incomplete group is carried into the next one and finished
                // only when the stream closes; per-chunk padding would corrupt the output.
                boolean b64 = kind == BASE64 || kind == BASE64_URL;
                StringBuilder textCarry = new StringBuilder();
                try {
                    while (it.next()) {
                        Object chunk = it.current();
                        if (b64 && chunk instanceof String) {
                            textCarry.append((String) chunk);
                            // Padding only ends the input, so text holding '=' waits for the end.
                            int cut = textCarry.indexOf("=") >= 0 ? 0 : textCarry.length() - textCarry.length() % 4;
                            if (cut > 0) {
                                String head = textCarry.substring(0, cut);
                                textCarry.delete(0, cut);
                                sink.yield$(convert(head));
                            }
                            continue;
                        }
                        if (b64) {
                            byte[] in = bytesOf(chunk);
                            byte[] all = new byte[carry.length + in.length];
                            System.arraycopy(carry, 0, all, 0, carry.length);
                            System.arraycopy(in, 0, all, carry.length, in.length);
                            int cut = all.length - all.length % 3;
                            carry = new byte[all.length - cut];
                            System.arraycopy(all, cut, carry, 0, carry.length);
                            if (cut > 0) {
                                byte[] head = new byte[cut];
                                System.arraycopy(all, 0, head, 0, cut);
                                sink.yield$(base64(head, kind == BASE64_URL));
                            }
                            continue;
                        }
                        if (kind != UTF8 || chunk instanceof String) {
                            sink.yield$(convert(chunk));
                            continue;
                        }
                        byte[] in = bytesOf(chunk);
                        byte[] all = new byte[carry.length + in.length];
                        System.arraycopy(carry, 0, all, 0, carry.length);
                        System.arraycopy(in, 0, all, carry.length, in.length);
                        int cut = completeUtf8Prefix(all);
                        byte[] head = new byte[cut];
                        System.arraycopy(all, 0, head, 0, cut);
                        carry = new byte[all.length - cut];
                        System.arraycopy(all, cut, carry, 0, carry.length);
                        if (cut > 0) {
                            // Strict as decode() is: Java's decoder would substitute U+FFFD.
                            checkUtf8(head, head.length);
                            sink.yield$(text(head, atStart));
                            atStart = false;
                        }
                    }
                    if (b64) {
                        if (textCarry.length() > 0) {
                            sink.yield$(convert(textCarry.toString()));
                        }
                        if (carry.length > 0) {
                            sink.yield$(base64(carry, kind == BASE64_URL));
                        }
                        return;
                    }
                    if (carry.length > 0) {
                        // An incomplete sequence left at the end of the stream is malformed.
                        checkUtf8(carry, carry.length);
                        sink.yield$(text(carry, atStart));
                    }
                } finally {
                    it.cancel();
                }
            }
        });
    }

    /** The length of the longest prefix of {@code b} that ends on a whole UTF-8 sequence. */
    static int completeUtf8Prefix(byte[] b) {
        int n = b.length;
        int i = n - 1;
        int back = 0;
        while (i >= 0 && back < 3 && (b[i] & 0xC0) == 0x80) {
            i--;
            back++;
        }
        if (i < 0) {
            return n;
        }
        int lead = b[i] & 0xFF;
        int need = lead >= 0xF0 ? 4 : lead >= 0xE0 ? 3 : lead >= 0xC0 ? 2 : 1;
        return n - i >= need ? n : i;
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

    private String text(byte[] b, boolean atStart) {
        try {
            if (kind == UTF8) {
                int off = atStart && b.length >= 3 && (b[0] & 0xff) == 0xef && (b[1] & 0xff) == 0xbb
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

    public static byte[] bytesOf(Object input) {
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
                // a List<int> element: a double (65.5 from JSON) is a TypeError in Dart,
                // never truncated into a valid byte
                if (!(o instanceof Long || o instanceof Integer || o instanceof Short || o instanceof Byte)) {
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
        checkBase64(s);
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

    /**
     * Dart's decoder rejects what this one used to truncate silently: a length that is
     * not a multiple of four, padding that does not complete the last quartet,
     * anything after the padding, and set bits left over before it. Messages and
     * positions follow the Dart VM's.
     */
    private static void checkBase64(String s) {
        int len = s.length();
        int pad = s.indexOf('=');
        int data = pad < 0 ? len : pad;
        if (pad >= 0) {
            int need = data % 4 == 2 ? 2 : data % 4 == 3 ? 1 : 0;
            int have = 0;
            while (pad + have < len && s.charAt(pad + have) == '=') {
                have++;
            }
            if (need == 0 || pad + have < len || have > need) {
                throw new FormatException("Invalid padding character (at character " + (pad + Math.min(have, need) + 1)
                        + ")");
            }
            if (have < need) {
                throw new FormatException("Missing padding character (at character " + (len + 1) + ")");
            }
        } else if (len % 4 != 0) {
            // Deliberately strict, and checked against the Dart VM: base64.decode and
            // base64Url.decode both reject unpadded "QQ" and "QUI" with exactly this
            // message. Dart code decoding unpadded input (a JWT segment) calls
            // base64.normalize first, which pads it.
            throw new FormatException("Invalid length, must be multiple of four (at character " + (len + 1) + ")");
        }
        if (data % 4 != 0) {
            // The bits of the last character that do not reach a whole byte must be zero.
            char last = s.charAt(data - 1);
            int v = last >= 'A' && last <= 'Z' ? last - 'A' : last >= 'a' && last <= 'z' ? last - 'a' + 26
                    : last >= '0' && last <= '9' ? last - '0' + 52 : last == '+' || last == '-' ? 62 : 63;
            int unused = data % 4 == 2 ? 0x0f : 0x03;
            if ((v & unused) != 0) {
                throw new FormatException("Invalid encoding before padding (at character " + (data + 1) + ")");
            }
        }
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
