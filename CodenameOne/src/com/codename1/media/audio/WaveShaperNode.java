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

/// Applies a transfer curve to every sample, for distortion and saturation:
/// the Web Audio `WaveShaperNode`.
///
/// An input of -1 maps to the first curve entry and +1 to the last, linearly
/// interpolated between and held past both ends. With no curve the node
/// passes its input through.
///
/// ```java
/// float[] curve = new float[1024];
/// for (int i = 0; i < curve.length; i++) {
///     double x = i * 2.0 / (curve.length - 1) - 1;
///     curve[i] = (float) (x * 3 / (1 + Math.abs(x * 3)));   // soft clip
/// }
/// WaveShaperNode drive = ctx.createWaveShaper();
/// drive.setCurve(curve);
/// drive.setOversample(OverSampleType.FOUR_X);
/// ```
public final class WaveShaperNode extends AudioNode {
    /// Taps of the interpolation and decimation filter per oversampling factor.
    private static final int TAPS_PER_PHASE = 16;
    private static final float[] KERNEL2 = kernel(2);
    private static final float[] KERNEL4 = kernel(4);

    private float[] curve;
    private OverSampleType oversample = OverSampleType.NONE;

    private float[] rCurve;
    private int rFactor = 1;
    private final float[][] upHistory = new float[AudioBus.MAX_CHANNELS][];
    private final float[][] downHistory = new float[AudioBus.MAX_CHANNELS][];
    private final float[] wide = new float[BaseAudioContext.RENDER_QUANTUM * 4];

    WaveShaperNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
    }

    /// The curve, or null when the node passes its input through.
    ///
    /// #### Returns
    ///
    /// a copy of the curve or null
    public float[] getCurve() {
        return curve == null ? null : copy(curve);
    }

    /// Sets the transfer curve. It is copied, so later changes to the array do
    /// not reach the node.
    ///
    /// #### Parameters
    ///
    /// - `curve`: at least 2 values, or null to pass the input through
    public void setCurve(float[] curve) {
        if (curve != null && curve.length < 2) {
            throw new IllegalArgumentException("A curve needs at least 2 values: " + curve.length);
        }
        final float[] c = curve == null ? null : copy(curve);
        this.curve = c;
        context.post(new Runnable() {
            @Override
            public void run() {
                rCurve = c;
            }
        });
    }

    /// The oversampling applied with the curve.
    ///
    /// #### Returns
    ///
    /// the oversampling
    public OverSampleType getOversample() {
        return oversample;
    }

    /// Sets the oversampling applied with the curve.
    ///
    /// #### Parameters
    ///
    /// - `type`: the oversampling
    public void setOversample(OverSampleType type) {
        if (type == null) {
            throw new IllegalArgumentException("type is null");
        }
        oversample = type;
        final int f = type == OverSampleType.FOUR_X ? 4 : type == OverSampleType.TWO_X ? 2 : 1;
        context.post(new Runnable() {
            @Override
            public void run() {
                rFactor = f;
            }
        });
    }

    private static float[] copy(float[] a) {
        float[] r = new float[a.length];
        System.arraycopy(a, 0, r, 0, a.length);
        return r;
    }

    /// The curve's value for `x`, exactly as the specification interpolates it.
    static float shape(float[] curve, float x) {
        int n = curve.length;
        double v = (n - 1) / 2.0 * (x + 1);
        if (v <= 0 || Float.isNaN(x)) {
            return curve[0];
        }
        if (v >= n - 1) {
            return curve[n - 1];
        }
        int k = (int) v;
        double f = v - k;
        return (float) ((1 - f) * curve[k] + f * curve[k + 1]);
    }

    /// A Blackman windowed sinc low-pass at the original Nyquist, `factor`
    /// phases of [#TAPS_PER_PHASE] taps.
    private static float[] kernel(int factor) {
        int n = TAPS_PER_PHASE * factor;
        float[] k = new float[n];
        double mid = (n - 1) / 2.0;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            double t = (i - mid) / factor;
            double sinc = t == 0 ? 1 : Math.sin(Math.PI * t) / (Math.PI * t);
            double w = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1)) + 0.08 * Math.cos(4 * Math.PI * i / (n - 1));
            k[i] = (float) (sinc * w);
            sum += k[i];
        }
        for (int i = 0; i < n; i++) {
            k[i] = (float) (k[i] / sum);
        }
        return k;
    }

    @Override
    void process(long q) {
        AudioBus in = inBuses[0];
        AudioBus out = outBuses[0];
        out.setChannels(in.channels);
        float[] c = rCurve;
        int n = BaseAudioContext.RENDER_QUANTUM;
        for (int ch = 0; ch < in.channels; ch++) {
            float[] s = in.data[ch];
            float[] d = out.data[ch];
            if (c == null) {
                System.arraycopy(s, 0, d, 0, n);
            } else if (rFactor == 1) {
                for (int i = 0; i < n; i++) {
                    d[i] = shape(c, s[i]);
                }
            } else {
                oversampled(ch, c, s, d);
            }
        }
    }

    private void oversampled(int ch, float[] c, float[] s, float[] d) {
        int f = rFactor;
        float[] k = f == 4 ? KERNEL4 : KERNEL2;
        int taps = k.length;
        int n = BaseAudioContext.RENDER_QUANTUM;
        int m = n * f;
        float[] up = upHistory[ch];
        if (up == null || up.length != taps + m) {
            // grown once per channel and factor, never in the steady state
            up = new float[taps + m];
            upHistory[ch] = up;
            downHistory[ch] = new float[taps + m];
        }
        float[] down = downHistory[ch];
        System.arraycopy(up, m, up, 0, taps);
        for (int i = 0; i < m; i++) {
            up[taps + i] = i % f == 0 ? s[i / f] * f : 0f;
        }
        float[] wv = wide;
        for (int i = 0; i < m; i++) {
            double acc = 0;
            int base = taps + i;
            for (int j = 0; j < taps; j++) {
                acc += k[j] * up[base - j];
            }
            wv[i] = shape(c, (float) acc);
        }
        System.arraycopy(down, m, down, 0, taps);
        System.arraycopy(wv, 0, down, taps, m);
        for (int i = 0; i < n; i++) {
            double acc = 0;
            int base = taps + i * f;
            for (int j = 0; j < taps; j++) {
                acc += k[j] * down[base - j];
            }
            d[i] = (float) acc;
        }
    }
}
