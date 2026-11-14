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

/// A general infinite impulse response filter with fixed coefficients, the Web
/// Audio `IIRFilterNode`. The output has the input's channel count.
public final class IIRFilterNode extends AudioNode {
    private static final int MAX_ORDER = 20;
    private final double[] b;
    private final double[] a;

    // ---- render side ----
    private final double[][] xs = new double[AudioBus.MAX_CHANNELS][];
    private final double[][] ys = new double[AudioBus.MAX_CHANNELS][];
    private int head;

    IIRFilterNode(BaseAudioContext context, double[] feedforward, double[] feedback) {
        super(context, 1, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
        if (feedforward == null || feedforward.length < 1 || feedforward.length > MAX_ORDER) {
            throw new IllegalArgumentException("feedforward needs 1 to 20 coefficients");
        }
        if (feedback == null || feedback.length < 1 || feedback.length > MAX_ORDER) {
            throw new IllegalArgumentException("feedback needs 1 to 20 coefficients");
        }
        if (feedback[0] == 0) {
            throw new IllegalArgumentException("feedback[0] must not be 0");
        }
        boolean any = false;
        for (double v : feedforward) {
            if (v != 0) {
                any = true;
            }
        }
        if (!any) {
            throw new IllegalArgumentException("feedforward must not be all 0");
        }
        double a0 = feedback[0];
        b = new double[feedforward.length];
        a = new double[feedback.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = feedforward[i] / a0;
        }
        for (int i = 0; i < a.length; i++) {
            a[i] = feedback[i] / a0;
        }
    }

    /// Computes the filter's response at each of `frequencyHz`.
    ///
    /// #### Parameters
    ///
    /// - `frequencyHz`: the frequencies to evaluate
    ///
    /// - `magResponse`: receives the linear magnitude at each frequency
    ///
    /// - `phaseResponse`: receives the phase in radians at each frequency
    public void getFrequencyResponse(float[] frequencyHz, float[] magResponse, float[] phaseResponse) {
        if (magResponse.length < frequencyHz.length || phaseResponse.length < frequencyHz.length) {
            throw new IllegalArgumentException("Response arrays are shorter than the frequency array");
        }
        BiquadFilterNode.response(b, a, context.getSampleRate(), frequencyHz, magResponse, phaseResponse);
    }

    @Override
    void process(long q) {
        AudioBus in = inBuses[0];
        AudioBus out = outBuses[0];
        out.setChannels(in.channels);
        int nb = b.length;
        int na = a.length;
        for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
            int h = head;
            for (int ch = 0; ch < in.channels; ch++) {
                double[] x = xs[ch];
                double[] y = ys[ch];
                if (x == null) {
                    x = new double[MAX_ORDER];
                    y = new double[MAX_ORDER];
                    xs[ch] = x;
                    ys[ch] = y;
                }
                double v = in.data[ch][i];
                x[h] = v;
                double acc = 0;
                for (int k = 0; k < nb; k++) {
                    acc += b[k] * x[(h - k + MAX_ORDER) % MAX_ORDER];
                }
                for (int k = 1; k < na; k++) {
                    acc -= a[k] * y[(h - k + MAX_ORDER) % MAX_ORDER];
                }
                if (Math.abs(acc) < 1e-30) {
                    acc = 0;
                }
                y[h] = acc;
                out.data[ch][i] = (float) acc;
            }
            head = (h + 1) % MAX_ORDER;
        }
    }
}
