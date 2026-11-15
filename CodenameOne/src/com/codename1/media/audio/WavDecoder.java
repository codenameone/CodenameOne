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
package com.codename1.media.audio;

/// Decodes RIFF WAVE files: integer PCM of 8, 16, 24 or 32 bits and IEEE
/// float of 32 or 64 bits, plain or `WAVE_FORMAT_EXTENSIBLE`.
final class WavDecoder {
    private static final int PCM = 1;
    private static final int FLOAT = 3;
    private static final int EXTENSIBLE = 0xFFFE;

    private WavDecoder() {
    }

    private static int le16(byte[] b, int o) {
        return (b[o] & 0xff) | ((b[o + 1] & 0xff) << 8);
    }

    private static int le32(byte[] b, int o) {
        return (b[o] & 0xff) | ((b[o + 1] & 0xff) << 8) | ((b[o + 2] & 0xff) << 16) | ((b[o + 3] & 0xff) << 24);
    }

    private static boolean tag(byte[] b, int o, String t) {
        for (int i = 0; i < 4; i++) {
            if (b[o + i] != t.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /// The decoded audio, or null when the bytes are not a WAVE file this
    /// decoder reads.
    static AudioData decode(byte[] b) {
        if (b == null || b.length < 12 || !tag(b, 0, "RIFF") || !tag(b, 8, "WAVE")) {
            return null;
        }
        int format = -1;
        int channels = 0;
        int rate = 0;
        int bits = 0;
        int blockAlign = 0;
        int pos = 12;
        while (pos + 8 <= b.length) {
            int size = le32(b, pos + 4);
            int body = pos + 8;
            if (size < 0) {
                return null;
            }
            if (tag(b, pos, "fmt ")) {
                if (size < 16 || body + 16 > b.length) {
                    return null;
                }
                format = le16(b, body);
                channels = le16(b, body + 2);
                rate = le32(b, body + 4);
                blockAlign = le16(b, body + 12);
                bits = le16(b, body + 14);
                if (format == EXTENSIBLE) {
                    if (size < 26 || body + 26 > b.length) {
                        return null;
                    }
                    format = le16(b, body + 24);
                }
            } else if (tag(b, pos, "data")) {
                int len = Math.min(size, b.length - body);
                return samples(b, body, len, format, channels, rate, bits, blockAlign);
            }
            long next = (long) body + size + (size & 1);
            if (next > b.length) {
                return null;
            }
            pos = (int) next;
        }
        return null;
    }

    private static AudioData samples(byte[] b, int off, int len, int format, int channels, int rate,
            int bits, int blockAlign) {
        if (channels < 1 || channels > AudioBus.MAX_CHANNELS || rate < 3000 || rate > 768000) {
            return null;
        }
        int bytes = bits / 8;
        boolean ok = format == PCM && (bits == 8 || bits == 16 || bits == 24 || bits == 32)
                || format == FLOAT && (bits == 32 || bits == 64);
        if (!ok || blockAlign != bytes * channels) {
            return null;
        }
        int frames = len / blockAlign;
        AudioData data = new AudioData(channels, Math.max(frames, 1), rate);
        float[][] out = new float[channels][];
        for (int c = 0; c < channels; c++) {
            out[c] = data.getChannelData(c);
        }
        int p = off;
        for (int f = 0; f < frames; f++) {
            for (int c = 0; c < channels; c++) {
                out[c][f] = sample(b, p, format, bits);
                p += bytes;
            }
        }
        return data;
    }

    private static float sample(byte[] b, int p, int format, int bits) {
        if (format == FLOAT) {
            if (bits == 32) {
                return Float.intBitsToFloat(le32(b, p));
            }
            long lo = le32(b, p) & 0xffffffffL;
            long hi = le32(b, p + 4) & 0xffffffffL;
            return (float) Double.longBitsToDouble(lo | (hi << 32));
        }
        switch (bits) {
            case 8:
                return ((b[p] & 0xff) - 128) / 128f;
            case 16:
                return ((short) le16(b, p)) / 32768f;
            case 24:
                return (((b[p] & 0xff) << 8 | (b[p + 1] & 0xff) << 16 | b[p + 2] << 24) >> 8) / 8388608f;
            default:
                return (float) (le32(b, p) / 2147483648.0);
        }
    }
}
