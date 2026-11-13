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

/// Convolves its input with an impulse response, for reverb and cabinet or
/// room simulation: the Web Audio `ConvolverNode`.
///
/// The response is a 1, 2 or 4 channel [AudioData] at the context's sample
/// rate. One channel is applied to each input channel; two are a left and a
/// right response; four are true stereo (left to left, left to right, right to
/// left, right to right). The output is mono only for a mono response on a
/// mono input, otherwise stereo.
///
/// ```java
/// OfflineAudioContext audio = new OfflineAudioContext(2, 44100, 44100);
/// OscillatorNode dry = audio.createOscillator();
/// GainNode wetGain = audio.createGain();
/// AudioData impulse = audio.createBuffer(2, 22050, 44100); // decoded or synthesised response
/// ConvolverNode hall = audio.createConvolver();
/// hall.setBuffer(impulse);
/// dry.connect(hall).connect(wetGain).connect(audio.getDestination());
/// ```
///
/// Convolution is uniformly partitioned in the frequency domain, one 128 frame
/// block at a time, so there is no added latency. Work grows linearly with the
/// response's length; a few seconds of stereo reverb is practical, a whole
/// cathedral on a low-end phone may not be.
public final class ConvolverNode extends AudioNode {
    private static final int B = BaseAudioContext.RENDER_QUANTUM;
    private static final int N = 2 * B;
    private static final int BINS = B + 1;

    private AudioData buffer;
    private boolean normalize = true;

    private Kernel rKernel;
    private final double[] re = new double[N]; // NOPMD SingularField - preallocated FFT scratch
    private final double[] im = new double[N]; // NOPMD SingularField - preallocated FFT scratch
    private final float[][] prevBlock = new float[2][B];

    /// The response, transformed once on the thread that sets it.
    static final class Kernel {
        final int partitions;
        final int channels;
        /// [irChannel][partition * BINS * 2 + bin * 2 + (0 re, 1 im)]
        final float[][] spectra;
        /// Input spectra, a ring of `partitions` per input channel.
        final float[][] history;
        final double[][] acc = new double[2][BINS * 2];
        int head;

        Kernel(AudioData ir, double scale) {
            channels = ir.getNumberOfChannels();
            partitions = Math.max(1, (ir.getLength() + B - 1) / B);
            spectra = new float[channels][partitions * BINS * 2];
            history = new float[2][partitions * BINS * 2];
            double[] r = new double[N];
            double[] i = new double[N];
            for (int c = 0; c < channels; c++) {
                float[] h = ir.getChannelData(c);
                for (int p = 0; p < partitions; p++) {
                    for (int k = 0; k < N; k++) {
                        int s = p * B + k;
                        r[k] = k < B && s < h.length ? h[s] * scale : 0;
                        i[k] = 0;
                    }
                    FFT.transform(r, i, false);
                    int o = p * BINS * 2;
                    for (int k = 0; k < BINS; k++) {
                        spectra[c][o + 2 * k] = (float) r[k];
                        spectra[c][o + 2 * k + 1] = (float) i[k];
                    }
                }
            }
        }
    }

    ConvolverNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.CLAMPED_MAX, ChannelInterpretation.SPEAKERS);
    }

    @Override
    void checkChannelCount(int count) {
        if (count < 1 || count > 2) {
            throw new IllegalArgumentException("A convolver takes 1 or 2 channels: " + count);
        }
    }

    @Override
    void checkChannelCountMode(ChannelCountMode mode) {
        if (mode == ChannelCountMode.MAX) {
            throw new IllegalArgumentException("A convolver cannot use ChannelCountMode.MAX");
        }
    }

    /// The impulse response, or null.
    ///
    /// #### Returns
    ///
    /// the response
    public AudioData getBuffer() {
        return buffer;
    }

    /// Sets the impulse response. It is read once, when set: later changes to
    /// its samples do not reach the node. Null silences the node.
    ///
    /// #### Parameters
    ///
    /// - `buffer`: 1, 2 or 4 channels at the context's sample rate, or null
    public void setBuffer(AudioData buffer) {
        Kernel k = null;
        if (buffer != null) {
            int ch = buffer.getNumberOfChannels();
            if (ch != 1 && ch != 2 && ch != 4) {
                throw new IllegalArgumentException("A response needs 1, 2 or 4 channels: " + ch);
            }
            if (buffer.getSampleRate() != context.getSampleRate()) {
                throw new IllegalArgumentException("The response's sample rate " + buffer.getSampleRate()
                        + " is not the context's " + context.getSampleRate());
            }
            k = new Kernel(buffer, normalize ? normalizationScale(buffer) : 1);
        }
        this.buffer = buffer;
        final Kernel kernel = k;
        context.post(new Runnable() {
            @Override
            public void run() {
                rKernel = kernel;
            }
        });
    }

    /// Whether a response set from now on is scaled to a standard loudness;
    /// true by default.
    ///
    /// #### Returns
    ///
    /// true to normalize
    public boolean isNormalize() {
        return normalize;
    }

    /// Sets whether a response set from now on is scaled to a standard loudness.
    /// As in the specification it applies at the next [#setBuffer(AudioData)].
    ///
    /// #### Parameters
    ///
    /// - `normalize`: true to normalize
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

    /// The specification's normalization scale for a response.
    static double normalizationScale(AudioData b) {
        double power = 0;
        int ch = b.getNumberOfChannels();
        int len = b.getLength();
        for (int c = 0; c < ch; c++) {
            float[] d = b.getChannelData(c);
            for (int i = 0; i < len; i++) {
                power += d[i] * d[i];
            }
        }
        power = Math.sqrt(power / ((double) ch * len));
        double minPower = 0.000125;
        if (Double.isNaN(power) || Double.isInfinite(power) || power < minPower) {
            power = minPower;
        }
        double scale = 1 / power * 0.00125 * (44100 / b.getSampleRate());
        if (ch == 4) {
            scale *= 0.5;
        }
        return scale;
    }

    @Override
    void process(long q) {
        Kernel k = rKernel;
        AudioBus in = inBuses[0];
        AudioBus out = outBuses[0];
        if (k == null) {
            out.setChannels(1);
            return;
        }
        boolean monoIn = in.channels == 1;
        int outCh = monoIn && k.channels == 1 ? 1 : 2;
        out.setChannels(outCh);
        int slot = k.head * BINS * 2;
        int inputs = monoIn ? 1 : 2;
        for (int c = 0; c < inputs; c++) {
            float[] cur = in.data[c];
            float[] prev = prevBlock[c];
            for (int i = 0; i < B; i++) {
                re[i] = prev[i];
                re[B + i] = cur[i];
            }
            for (int i = 0; i < N; i++) {
                im[i] = 0;
            }
            System.arraycopy(cur, 0, prev, 0, B);
            FFT.transform(re, im, false);
            float[] h = k.history[c];
            for (int b = 0; b < BINS; b++) {
                h[slot + 2 * b] = (float) re[b];
                h[slot + 2 * b + 1] = (float) im[b];
            }
        }
        if (monoIn) {
            System.arraycopy(k.history[0], slot, k.history[1], slot, BINS * 2);
            System.arraycopy(prevBlock[0], 0, prevBlock[1], 0, B);
        }
        for (int o = 0; o < outCh; o++) {
            double[] acc = k.acc[o];
            for (int i = 0; i < acc.length; i++) {
                acc[i] = 0;
            }
            if (k.channels == 4) {
                accumulate(k, 0, o, acc);
                accumulate(k, 1, 2 + o, acc);
            } else {
                accumulate(k, o, k.channels == 1 ? 0 : o, acc);
            }
            for (int b = 0; b < BINS; b++) {
                re[b] = acc[2 * b];
                im[b] = acc[2 * b + 1];
            }
            for (int b = BINS; b < N; b++) {
                re[b] = re[N - b];
                im[b] = -im[N - b];
            }
            FFT.transform(re, im, true);
            float[] d = out.data[o];
            for (int i = 0; i < B; i++) {
                d[i] = (float) (re[B + i] / N);
            }
        }
        k.head = (k.head + 1) % k.partitions;
    }

    /// Adds input channel `in`'s history times response channel `ir`.
    private static void accumulate(Kernel k, int in, int ir, double[] acc) {
        float[] x = k.history[in];
        float[] h = k.spectra[ir];
        int parts = k.partitions;
        for (int p = 0; p < parts; p++) {
            int idx = k.head - p;
            if (idx < 0) {
                idx += parts;
            }
            int xs = idx * BINS * 2;
            int hs = p * BINS * 2;
            for (int b = 0; b < 2 * BINS; b += 2) {
                double xr = x[xs + b];
                double xi = x[xs + b + 1];
                double hr = h[hs + b];
                double hi = h[hs + b + 1];
                acc[b] += xr * hr - xi * hi;
                acc[b + 1] += xr * hi + xi * hr;
            }
        }
    }
}
