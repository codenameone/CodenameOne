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
package com.codename1.compat.jdk;

/// `java.util.Base64`: the basic, URL-safe and MIME encodings of RFC 4648
/// and RFC 2045, by the JDK's rules for padding and for what a decoder
/// refuses.
public final class Base64 {
    private static final String BASIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final String URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    private static final byte[] CRLF = {'\r', '\n'};

    private Base64() {
    }

    public static Encoder getEncoder() {
        return new Encoder(BASIC, 0, null, true);
    }

    public static Encoder getUrlEncoder() {
        return new Encoder(URL, 0, null, true);
    }

    public static Encoder getMimeEncoder() {
        return new Encoder(BASIC, 76, CRLF, true);
    }

    public static Encoder getMimeEncoder(int lineLength, byte[] lineSeparator) {
        if (lineSeparator == null) {
            throw new NullPointerException();
        }
        for (int i = 0; i < lineSeparator.length; i++) {
            int b = lineSeparator[i] & 0xFF;
            if (BASIC.indexOf((char) b) >= 0) {
                throw new IllegalArgumentException("Illegal base64 line separator character 0x"
                        + Integer.toString(b, 16));
            }
        }
        // Rounded down to a multiple of four, as whole groups make a line.
        int length = lineLength & ~3;
        if (length <= 0) {
            return new Encoder(BASIC, 0, null, true);
        }
        byte[] separator = new byte[lineSeparator.length];
        System.arraycopy(lineSeparator, 0, separator, 0, separator.length);
        return new Encoder(BASIC, length, separator, true);
    }

    public static Decoder getDecoder() {
        return new Decoder(BASIC, false);
    }

    public static Decoder getUrlDecoder() {
        return new Decoder(URL, false);
    }

    public static Decoder getMimeDecoder() {
        return new Decoder(BASIC, true);
    }

    /// `java.util.Base64.Encoder`.
    public static final class Encoder {
        private final String alphabet;
        private final int lineLength;
        private final byte[] separator;
        private final boolean padding;

        Encoder(String alphabet, int lineLength, byte[] separator, boolean padding) {
            this.alphabet = alphabet;
            this.lineLength = lineLength;
            this.separator = separator;
            this.padding = padding;
        }

        /// The same encoding without the `=` that fills the last group.
        public Encoder withoutPadding() {
            return padding ? new Encoder(alphabet, lineLength, separator, false) : this;
        }

        private int length(int n) {
            int length = padding ? 4 * ((n + 2) / 3) : 4 * (n / 3) + (n % 3 == 0 ? 0 : n % 3 + 1);
            if (lineLength > 0 && length > 0) {
                length += (length - 1) / lineLength * separator.length;
            }
            return length;
        }

        public byte[] encode(byte[] src) {
            byte[] out = new byte[length(src.length)];
            write(src, out);
            return out;
        }

        /// Encodes into `dst`, which must be large enough, and answers how
        /// many bytes were written.
        public int encode(byte[] src, byte[] dst) {
            if (dst.length < length(src.length)) {
                throw new IllegalArgumentException("Output byte array is too small for encoding all input bytes");
            }
            return write(src, dst);
        }

        public String encodeToString(byte[] src) {
            byte[] out = encode(src);
            char[] chars = new char[out.length];
            for (int i = 0; i < out.length; i++) {
                chars[i] = (char) (out[i] & 0xFF);
            }
            return new String(chars);
        }

        private int write(byte[] src, byte[] dst) {
            int at = 0;
            int onLine = 0;
            int n = src.length;
            for (int i = 0; i < n; i += 3) {
                if (lineLength > 0 && onLine == lineLength) {
                    for (int s = 0; s < separator.length; s++) {
                        dst[at++] = separator[s];
                    }
                    onLine = 0;
                }
                int left = n - i;
                int bits = (src[i] & 0xFF) << 16;
                if (left > 1) {
                    bits |= (src[i + 1] & 0xFF) << 8;
                }
                if (left > 2) {
                    bits |= src[i + 2] & 0xFF;
                }
                dst[at++] = (byte) alphabet.charAt((bits >> 18) & 0x3F);
                dst[at++] = (byte) alphabet.charAt((bits >> 12) & 0x3F);
                if (left > 1) {
                    dst[at++] = (byte) alphabet.charAt((bits >> 6) & 0x3F);
                } else if (padding) {
                    dst[at++] = '=';
                }
                if (left > 2) {
                    dst[at++] = (byte) alphabet.charAt(bits & 0x3F);
                } else if (padding) {
                    dst[at++] = '=';
                }
                onLine += 4;
            }
            return at;
        }
    }

    /// `java.util.Base64.Decoder`.
    public static final class Decoder {
        private final int[] values = new int[256];
        private final boolean mime;

        Decoder(String alphabet, boolean mime) {
            this.mime = mime;
            for (int i = 0; i < 256; i++) {
                values[i] = -1;
            }
            for (int i = 0; i < alphabet.length(); i++) {
                values[alphabet.charAt(i)] = i;
            }
            values['='] = -2;
        }

        public byte[] decode(String src) {
            byte[] bytes = new byte[src.length()];
            for (int i = 0; i < bytes.length; i++) {
                char c = src.charAt(i);
                bytes[i] = (byte) (c < 256 ? c : '?');
            }
            return decode(bytes);
        }

        public byte[] decode(byte[] src) {
            byte[] out = new byte[src.length / 4 * 3 + 3];
            int n = read(src, out);
            byte[] exact = new byte[n];
            System.arraycopy(out, 0, exact, 0, n);
            return exact;
        }

        /// Decodes into `dst`, which must be large enough, and answers how
        /// many bytes were written.
        public int decode(byte[] src, byte[] dst) {
            byte[] out = decode(src);
            if (dst.length < out.length) {
                throw new IllegalArgumentException("Output byte array is too small for decoding all input bytes");
            }
            System.arraycopy(out, 0, dst, 0, out.length);
            return out.length;
        }

        private int read(byte[] src, byte[] dst) {
            int at = 0;
            int bits = 0;
            int shift = 18;
            int sp = 0;
            int end = src.length;
            while (sp < end) {
                int b = values[src[sp++] & 0xFF];
                if (b < 0) {
                    if (b == -2) {
                        // xx== or xxx=, and nothing else, ends the input.
                        if (shift == 6 && (sp == end || src[sp++] != '=') || shift == 18) {
                            throw new IllegalArgumentException("Input byte array has wrong 4-byte ending unit");
                        }
                        break;
                    }
                    if (mime) {
                        continue;
                    }
                    throw new IllegalArgumentException("Illegal base64 character "
                            + Integer.toString(src[sp - 1], 16));
                }
                bits |= b << shift;
                shift -= 6;
                if (shift < 0) {
                    dst[at++] = (byte) (bits >> 16);
                    dst[at++] = (byte) (bits >> 8);
                    dst[at++] = (byte) bits;
                    shift = 18;
                    bits = 0;
                }
            }
            if (shift == 6) {
                dst[at++] = (byte) (bits >> 16);
            } else if (shift == 0) {
                dst[at++] = (byte) (bits >> 16);
                dst[at++] = (byte) (bits >> 8);
            } else if (shift == 12) {
                throw new IllegalArgumentException("Last unit does not have enough valid bits");
            }
            while (sp < end) {
                if (mime && values[src[sp++] & 0xFF] < 0) {
                    continue;
                }
                throw new IllegalArgumentException("Input byte array has incorrect ending byte at " + sp);
            }
            return at;
        }
    }
}
