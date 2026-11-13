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

/// One render quantum of planar audio, reused every quantum. Render thread only.
final class AudioBus {
    static final int MAX_CHANNELS = 32;
    private static final float SQRT_HALF = 0.70710678f;

    final float[][] data = new float[MAX_CHANNELS][];
    int channels;

    AudioBus(int channels) {
        setChannels(channels);
    }

    /// Sets the channel count and zeroes those channels.
    void setChannels(int n) {
        for (int c = 0; c < n; c++) {
            float[] d = data[c];
            if (d == null) {
                data[c] = new float[BaseAudioContext.RENDER_QUANTUM];
            } else {
                for (int i = 0; i < d.length; i++) {
                    d[i] = 0f;
                }
            }
        }
        channels = n;
    }

    /// Adds `src` into `dst`, up-mixing or down-mixing to `dst.channels`.
    static void sumInto(AudioBus src, AudioBus dst, ChannelInterpretation interp) {
        int sc = src.channels;
        int dc = dst.channels;
        if (sc == dc) {
            for (int c = 0; c < dc; c++) {
                add(src.data[c], dst.data[c], 1f);
            }
            return;
        }
        if (interp == ChannelInterpretation.SPEAKERS && speakers(sc, src, dc, dst)) {
            return;
        }
        int n = Math.min(sc, dc);
        for (int c = 0; c < n; c++) {
            add(src.data[c], dst.data[c], 1f);
        }
    }

    private static boolean speakers(int sc, AudioBus src, int dc, AudioBus dst) {
        float[][] s = src.data;
        float[][] d = dst.data;
        switch (sc) {
            case 1:
                if (dc == 2 || dc == 4) {
                    add(s[0], d[0], 1f);
                    add(s[0], d[1], 1f);
                    return true;
                }
                if (dc == 6) {
                    add(s[0], d[2], 1f);
                    return true;
                }
                return false;
            case 2:
                if (dc == 1) {
                    add(s[0], d[0], 0.5f);
                    add(s[1], d[0], 0.5f);
                    return true;
                }
                if (dc == 4 || dc == 6) {
                    add(s[0], d[0], 1f);
                    add(s[1], d[1], 1f);
                    return true;
                }
                return false;
            case 4:
                if (dc == 1) {
                    for (int c = 0; c < 4; c++) {
                        add(s[c], d[0], 0.25f);
                    }
                    return true;
                }
                if (dc == 2) {
                    add(s[0], d[0], 0.5f);
                    add(s[2], d[0], 0.5f);
                    add(s[1], d[1], 0.5f);
                    add(s[3], d[1], 0.5f);
                    return true;
                }
                if (dc == 6) {
                    add(s[0], d[0], 1f);
                    add(s[1], d[1], 1f);
                    add(s[2], d[4], 1f);
                    add(s[3], d[5], 1f);
                    return true;
                }
                return false;
            case 6:
                if (dc == 1) {
                    add(s[0], d[0], SQRT_HALF);
                    add(s[1], d[0], SQRT_HALF);
                    add(s[2], d[0], 1f);
                    add(s[4], d[0], 0.5f);
                    add(s[5], d[0], 0.5f);
                    return true;
                }
                if (dc == 2) {
                    add(s[0], d[0], 1f);
                    add(s[2], d[0], SQRT_HALF);
                    add(s[4], d[0], SQRT_HALF);
                    add(s[1], d[1], 1f);
                    add(s[2], d[1], SQRT_HALF);
                    add(s[5], d[1], SQRT_HALF);
                    return true;
                }
                if (dc == 4) {
                    add(s[0], d[0], 1f);
                    add(s[2], d[0], SQRT_HALF);
                    add(s[1], d[1], 1f);
                    add(s[2], d[1], SQRT_HALF);
                    add(s[4], d[2], 1f);
                    add(s[5], d[3], 1f);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private static void add(float[] s, float[] d, float g) {
        for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
            d[i] += s[i] * g;
        }
    }
}
