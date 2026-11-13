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

import com.codename1.util.MathUtil;

import java.util.concurrent.atomic.AtomicLong;

/// Exposes the recent signal and its spectrum for visualisation, passing its
/// input through unchanged: the Web Audio `AnalyserNode`.
///
/// ```java
/// AnalyserNode an = ctx.createAnalyser();
/// an.setFftSize(1024);
/// player.connect(an).connect(ctx.getDestination());
/// byte[] bars = new byte[an.getFrequencyBinCount()];
/// // in an animation callback
/// an.getByteFrequencyData(bars);
/// ```
///
/// The input is down-mixed to mono for analysis. The frequency data is a
/// Blackman windowed FFT of the newest [#getFftSize()] samples, smoothed over
/// time and converted to decibels, as the specification defines it.
///
/// The getters may be called from any thread. With a real-time
/// [AudioContext] the render thread keeps writing while they copy, so a copy
/// is the latest audio at the moment it was taken; it is meant for drawing,
/// not for sample-exact measurement, which belongs to an [OfflineAudioContext].
public final class AnalyserNode extends AudioNode {
    private static final int RING = 32768;
    private static final double BLACKMAN_ALPHA = 0.16;

    private final float[] ring = new float[RING];
    /// Frames written to `ring`, published after each quantum.
    private final AtomicLong written = new AtomicLong();

    private int fftSize = 2048;
    private double minDecibels = -100;
    private double maxDecibels = -30;
    private double smoothing = 0.8;

    // ---- reader side, used by whichever thread calls the getters ----
    private double[] re;
    private double[] im;
    private float[] smoothed;
    private float[] decibels;
    private long analysedAt = -1;

    AnalyserNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
    }

    /// The analysis window, 2048 by default.
    ///
    /// #### Returns
    ///
    /// a power of two from 32 to 32768
    public int getFftSize() {
        return fftSize;
    }

    /// Sets the analysis window.
    ///
    /// #### Parameters
    ///
    /// - `size`: a power of two from 32 to 32768
    public void setFftSize(int size) {
        if (size < 32 || size > RING || (size & (size - 1)) != 0) {
            throw new IllegalArgumentException("fftSize must be a power of two from 32 to 32768: " + size);
        }
        resetAnalysis(size);
    }

    private void resetAnalysis(int size) {
        fftSize = size;
        re = null;
        smoothed = null;
        analysedAt = -1;
    }

    /// Half the FFT size: the number of values the frequency getters fill.
    ///
    /// #### Returns
    ///
    /// the bin count
    public int getFrequencyBinCount() {
        return fftSize / 2;
    }

    /// The decibel value that maps to byte 0, -100 by default.
    ///
    /// #### Returns
    ///
    /// decibels
    public double getMinDecibels() {
        return minDecibels;
    }

    /// Sets the decibel value that maps to byte 0.
    ///
    /// #### Parameters
    ///
    /// - `db`: less than [#getMaxDecibels()]
    public void setMinDecibels(double db) {
        if (!(db < maxDecibels)) { // NOPMD LogicInversion - also rejects NaN
            throw new IllegalArgumentException("minDecibels must be below maxDecibels: " + db);
        }
        minDecibels = db;
    }

    /// The decibel value that maps to byte 255, -30 by default.
    ///
    /// #### Returns
    ///
    /// decibels
    public double getMaxDecibels() {
        return maxDecibels;
    }

    /// Sets the decibel value that maps to byte 255.
    ///
    /// #### Parameters
    ///
    /// - `db`: more than [#getMinDecibels()]
    public void setMaxDecibels(double db) {
        if (!(db > minDecibels)) { // NOPMD LogicInversion - also rejects NaN
            throw new IllegalArgumentException("maxDecibels must be above minDecibels: " + db);
        }
        maxDecibels = db;
    }

    /// How much of the previous spectrum each new one keeps, 0.8 by default.
    ///
    /// #### Returns
    ///
    /// 0 to 1
    public double getSmoothingTimeConstant() {
        return smoothing;
    }

    /// Sets how much of the previous spectrum each new one keeps.
    ///
    /// #### Parameters
    ///
    /// - `value`: 0 (no smoothing) to 1
    public void setSmoothingTimeConstant(double value) {
        if (Double.isNaN(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException("smoothingTimeConstant must be between 0 and 1: " + value);
        }
        smoothing = value;
    }

    /// Copies the newest samples, as -1 to 1.
    ///
    /// #### Parameters
    ///
    /// - `array`: filled with up to [#getFftSize()] values
    public void getFloatTimeDomainData(float[] array) {
        int n = Math.min(array.length, fftSize);
        long end = written.get();
        long start = end - fftSize;
        for (int i = 0; i < n; i++) {
            long p = start + i;
            array[i] = p < 0 ? 0f : ring[(int) (p & (RING - 1))];
        }
    }

    /// Copies the newest samples as bytes, 128 being silence.
    ///
    /// #### Parameters
    ///
    /// - `array`: filled with up to [#getFftSize()] values
    public void getByteTimeDomainData(byte[] array) {
        int n = Math.min(array.length, fftSize);
        long end = written.get();
        long start = end - fftSize;
        for (int i = 0; i < n; i++) {
            long p = start + i;
            float v = p < 0 ? 0f : ring[(int) (p & (RING - 1))];
            array[i] = (byte) clampByte(Math.floor(128 * (1 + v)));
        }
    }

    /// Copies the spectrum in decibels, bin `k` being `k * sampleRate / fftSize` Hz.
    ///
    /// #### Parameters
    ///
    /// - `array`: filled with up to [#getFrequencyBinCount()] values
    public void getFloatFrequencyData(float[] array) {
        analyse();
        int n = Math.min(array.length, fftSize / 2);
        System.arraycopy(decibels, 0, array, 0, n);
    }

    /// Copies the spectrum scaled from [#getMinDecibels()] (0) to
    /// [#getMaxDecibels()] (255).
    ///
    /// #### Parameters
    ///
    /// - `array`: filled with up to [#getFrequencyBinCount()] values
    public void getByteFrequencyData(byte[] array) {
        analyse();
        int n = Math.min(array.length, fftSize / 2);
        double scale = 255 / (maxDecibels - minDecibels);
        for (int i = 0; i < n; i++) {
            array[i] = (byte) clampByte(Math.floor(scale * (decibels[i] - minDecibels)));
        }
    }

    private static int clampByte(double v) {
        return v < 0 ? 0 : v > 255 ? 255 : (int) v;
    }

    private void analyse() {
        int n = fftSize;
        if (re == null || re.length != n) {
            re = new double[n];
            im = new double[n];
            smoothed = new float[n / 2];
            decibels = new float[n / 2];
            analysedAt = -1;
        }
        long end = written.get();
        if (end == analysedAt) {
            return;
        }
        analysedAt = end;
        long start = end - n;
        for (int i = 0; i < n; i++) {
            long p = start + i;
            double x = p < 0 ? 0 : ring[(int) (p & (RING - 1))];
            double w = (1 - BLACKMAN_ALPHA) / 2 - 0.5 * Math.cos(2 * Math.PI * i / n)
                    + BLACKMAN_ALPHA / 2 * Math.cos(4 * Math.PI * i / n);
            re[i] = x * w;
            im[i] = 0;
        }
        FFT.transform(re, im, false);
        double tau = smoothing;
        for (int k = 0; k < n / 2; k++) {
            double mag = Math.sqrt(re[k] * re[k] + im[k] * im[k]) / n;
            double v = tau * smoothed[k] + (1 - tau) * mag;
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                v = 0;
            }
            smoothed[k] = (float) v;
            decibels[k] = v == 0 ? Float.NEGATIVE_INFINITY : (float) (20 * MathUtil.log10(v));
        }
    }

    @Override
    void process(long q) {
        AudioBus in = inBuses[0];
        AudioBus out = outBuses[0];
        out.setChannels(in.channels);
        int n = BaseAudioContext.RENDER_QUANTUM;
        long w = written.get();
        int base = (int) (w & (RING - 1));
        float scale = 1f / in.channels;
        for (int i = 0; i < n; i++) {
            float sum = 0;
            for (int c = 0; c < in.channels; c++) {
                sum += in.data[c][i];
            }
            ring[base + i] = in.channels == 1 ? sum : sum * scale;
        }
        for (int c = 0; c < in.channels; c++) {
            System.arraycopy(in.data[c], 0, out.data[c], 0, n);
        }
        written.set(w + n);
    }
}
