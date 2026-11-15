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

/// A custom oscillator waveform given by its Fourier coefficients, the Web
/// Audio `PeriodicWave`. Create one with
/// [BaseAudioContext#createPeriodicWave(float[], float[])] and play it with
/// [OscillatorNode#setPeriodicWave(PeriodicWave)].
///
/// The waveform is `sum(real[k] cos(k w t) + imag[k] sin(k w t))` for `k` from
/// 1. It is rendered from wavetables built once, each keeping only the
/// harmonics below the Nyquist frequency for its pitch range, so it does not
/// alias.
public final class PeriodicWave {
    static final int TABLE_SIZE = 2048;
    static final int MAX_HARMONICS = TABLE_SIZE / 2 - 1;
    /// Table `r` keeps harmonics up to `MAX_HARMONICS >> r`.
    static final int TABLES = 11;

    final float[][] tables = new float[TABLES][];

    PeriodicWave(float[] real, float[] imag, boolean disableNormalization) {
        if (real == null || imag == null || real.length != imag.length || real.length < 2) {
            throw new IllegalArgumentException("real and imag must have the same length, at least 2");
        }
        double[] a = new double[real.length];
        double[] b = new double[imag.length];
        for (int i = 0; i < a.length; i++) {
            a[i] = real[i];
            b[i] = imag[i];
        }
        build(a, b, disableNormalization);
    }

    private PeriodicWave(double[] a, double[] b) {
        build(a, b, false);
    }

    static PeriodicWave standard(OscillatorType type) {
        int n = MAX_HARMONICS + 1;
        double[] a = new double[n];
        double[] b = new double[n];
        for (int k = 1; k < n; k++) {
            switch (type) {
                case SINE:
                    b[k] = k == 1 ? 1 : 0;
                    break;
                case SQUARE:
                    b[k] = (k & 1) == 1 ? 4 / (k * Math.PI) : 0;
                    break;
                case SAWTOOTH:
                    b[k] = ((k & 1) == 1 ? 2 : -2) / (k * Math.PI);
                    break;
                case TRIANGLE:
                    if ((k & 1) == 1) {
                        double s = 8 / (Math.PI * Math.PI * k * k);
                        b[k] = (k & 3) == 1 ? s : -s;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Not a standard waveform: " + type);
            }
        }
        return new PeriodicWave(a, b);
    }

    private void build(double[] a, double[] b, boolean disableNormalization) {
        double max = 0;
        for (int r = 0; r < TABLES; r++) {
            int harmonics = Math.min(MAX_HARMONICS >> r, a.length - 1);
            double[] re = new double[TABLE_SIZE];
            double[] im = new double[TABLE_SIZE];
            for (int k = 1; k <= harmonics; k++) {
                re[k] = a[k];
                im[k] = -b[k];
            }
            FFT.transform(re, im, true);
            float[] t = new float[TABLE_SIZE + 1];
            for (int i = 0; i < TABLE_SIZE; i++) {
                t[i] = (float) re[i];
                if (r == 0 && Math.abs(re[i]) > max) {
                    max = Math.abs(re[i]);
                }
            }
            t[TABLE_SIZE] = t[0];
            tables[r] = t;
        }
        if (!disableNormalization && max > 0) {
            float scale = (float) (1 / max);
            for (int r = 0; r < TABLES; r++) {
                float[] t = tables[r];
                for (int i = 0; i < t.length; i++) {
                    t[i] *= scale;
                }
            }
        }
    }

    /// The table to use at `frequency`, or null when even the fundamental is
    /// above Nyquist.
    float[] tableFor(double frequency, double nyquist) {
        double f = Math.abs(frequency);
        if (f >= nyquist) {
            return null;
        }
        double allowed = f == 0 ? MAX_HARMONICS : nyquist / f;
        int r = 0;
        while (r < TABLES - 1 && (MAX_HARMONICS >> r) > allowed) {
            r++;
        }
        return tables[r];
    }
}
