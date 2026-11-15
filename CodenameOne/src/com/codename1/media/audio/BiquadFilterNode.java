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

/// A second order filter, the Web Audio `BiquadFilterNode`: low and high pass,
/// band pass, shelves, peaking, notch and all pass. Coefficients follow the
/// specification's formulas and are recomputed per sample only while a
/// parameter changes. The output has the input's channel count.
public final class BiquadFilterNode extends AudioNode {
    private final AudioParam frequency;
    private final AudioParam detune;
    private final AudioParam q;
    private final AudioParam gain;
    private BiquadFilterType type = BiquadFilterType.LOWPASS;

    // ---- render side ----
    private BiquadFilterType rType = BiquadFilterType.LOWPASS;
    private final double[] x1 = new double[AudioBus.MAX_CHANNELS];
    private final double[] x2 = new double[AudioBus.MAX_CHANNELS];
    private final double[] y1 = new double[AudioBus.MAX_CHANNELS];
    private final double[] y2 = new double[AudioBus.MAX_CHANNELS];
    private final double[] coef = new double[5];

    BiquadFilterNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
        float nyquist = context.getSampleRate() / 2;
        frequency = new AudioParam(context, "frequency", 350f, 0f, nyquist, AutomationRate.A_RATE, false);
        detune = new AudioParam(context, "detune", 0f, -153600f, 153600f, AutomationRate.A_RATE, false);
        q = new AudioParam(context, "Q", 1f, -Float.MAX_VALUE, Float.MAX_VALUE, AutomationRate.A_RATE, false);
        gain = new AudioParam(context, "gain", 0f, -Float.MAX_VALUE, 1541f, AutomationRate.A_RATE, false);
    }

    /// The cutoff or center frequency in Hz, 350 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getFrequency() {
        return frequency;
    }

    /// A detune of the frequency in cents, 0 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getDetune() {
        return detune;
    }

    /// The quality factor, 1 by default: resonance in dB for low and high pass,
    /// width for band pass, peaking, notch and all pass. Unused by the shelves.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getQ() {
        return q;
    }

    /// The boost in dB for the shelves and peaking, 0 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getGain() {
        return gain;
    }

    /// The filter type.
    ///
    /// #### Returns
    ///
    /// the type
    public BiquadFilterType getType() {
        return type;
    }

    /// Sets the filter type.
    ///
    /// #### Parameters
    ///
    /// - `type`: the type
    public void setType(final BiquadFilterType type) {
        if (type == null) {
            throw new IllegalArgumentException("type is null");
        }
        this.type = type;
        context.post(new Runnable() {
            @Override
            public void run() {
                rType = type;
            }
        });
    }

    /// Computes the response of the filter, with the parameters' current values,
    /// at each of `frequencyHz`.
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
        double[] c = new double[5];
        coefficients(type, frequency.getValue(), detune.getValue(), q.getValue(), gain.getValue(),
                context.getSampleRate(), c);
        response(new double[] {c[0], c[1], c[2]}, new double[] {1, c[3], c[4]}, context.getSampleRate(),
                frequencyHz, magResponse, phaseResponse);
    }

    static void response(double[] b, double[] a, float sampleRate, float[] frequencyHz,
                         float[] magResponse, float[] phaseResponse) {
        double nyquist = sampleRate / 2.0;
        for (int i = 0; i < frequencyHz.length; i++) {
            double f = frequencyHz[i];
            if (f < 0 || f > nyquist || Double.isNaN(f)) {
                magResponse[i] = Float.NaN;
                phaseResponse[i] = Float.NaN;
                continue;
            }
            double w = Math.PI * f / nyquist;
            double nr = 0;
            double ni = 0;
            for (int k = 0; k < b.length; k++) {
                nr += b[k] * Math.cos(-k * w);
                ni += b[k] * Math.sin(-k * w);
            }
            double dr = 0;
            double di = 0;
            for (int k = 0; k < a.length; k++) {
                dr += a[k] * Math.cos(-k * w);
                di += a[k] * Math.sin(-k * w);
            }
            double den = dr * dr + di * di;
            double hr = (nr * dr + ni * di) / den;
            double hi = (ni * dr - nr * di) / den;
            magResponse[i] = (float) Math.sqrt(hr * hr + hi * hi);
            phaseResponse[i] = (float) MathUtil.atan2(hi, hr);
        }
    }

    /// Fills `c` with b0, b1, b2, a1, a2, normalized by a0.
    static void coefficients(BiquadFilterType type, double freq, double detuneCents, double qv,
                             double gainDb, double sampleRate, double[] c) {
        double f0 = detuneCents == 0 ? freq : freq * MathUtil.pow(2, detuneCents / 1200.0);
        double nyquist = sampleRate / 2;
        double norm = f0 / nyquist;
        double a = MathUtil.pow(10, gainDb / 40);
        if (!(norm > 0 && norm < 1)) { // NOPMD LogicInversion - a NaN frequency takes this path
            double pass;
            boolean low = !(norm >= 1); // NOPMD LogicInversion - NaN reads as the low edge
            switch (type) {
                case LOWPASS:
                    pass = low ? 0 : 1;
                    break;
                case HIGHPASS:
                    pass = low ? 1 : 0;
                    break;
                case BANDPASS:
                    pass = 0;
                    break;
                case LOWSHELF:
                    pass = low ? 1 : a * a;
                    break;
                case HIGHSHELF:
                    pass = low ? a * a : 1;
                    break;
                default:
                    pass = 1;
                    break;
            }
            c[0] = pass;
            c[1] = 0;
            c[2] = 0;
            c[3] = 0;
            c[4] = 0;
            return;
        }
        boolean qType = type == BiquadFilterType.BANDPASS || type == BiquadFilterType.NOTCH
                || type == BiquadFilterType.ALLPASS || type == BiquadFilterType.PEAKING;
        if (qType && !(qv > 0)) { // NOPMD LogicInversion - NaN takes this path too
            // The specification's Q <= 0 cases: each formula's limit as alpha
            // grows without bound, a constant gain -- bandpass passes (1),
            // notch silences (0), allpass inverts (-1), peaking is A^2.
            double gain;
            if (type == BiquadFilterType.BANDPASS) {
                gain = 1;
            } else if (type == BiquadFilterType.NOTCH) {
                gain = 0;
            } else if (type == BiquadFilterType.ALLPASS) {
                gain = -1;
            } else {
                gain = a * a;
            }
            c[0] = gain;
            c[1] = 0;
            c[2] = 0;
            c[3] = 0;
            c[4] = 0;
            return;
        }
        double w0 = Math.PI * norm;
        double cos = Math.cos(w0);
        double sin = Math.sin(w0);
        double alphaQ = sin / (2 * qv);
        double alphaQdB = sin / (2 * MathUtil.pow(10, qv / 20));
        double alphaS = sin / 2 * Math.sqrt(2);
        double sqrtA = Math.sqrt(a);
        double b0;
        double b1;
        double b2;
        double a0;
        double a1;
        double a2;
        switch (type) {
            case LOWPASS:
                b0 = (1 - cos) / 2;
                b1 = 1 - cos;
                b2 = (1 - cos) / 2;
                a0 = 1 + alphaQdB;
                a1 = -2 * cos;
                a2 = 1 - alphaQdB;
                break;
            case HIGHPASS:
                b0 = (1 + cos) / 2;
                b1 = -(1 + cos);
                b2 = (1 + cos) / 2;
                a0 = 1 + alphaQdB;
                a1 = -2 * cos;
                a2 = 1 - alphaQdB;
                break;
            case BANDPASS:
                b0 = alphaQ;
                b1 = 0;
                b2 = -alphaQ;
                a0 = 1 + alphaQ;
                a1 = -2 * cos;
                a2 = 1 - alphaQ;
                break;
            case NOTCH:
                b0 = 1;
                b1 = -2 * cos;
                b2 = 1;
                a0 = 1 + alphaQ;
                a1 = -2 * cos;
                a2 = 1 - alphaQ;
                break;
            case ALLPASS:
                b0 = 1 - alphaQ;
                b1 = -2 * cos;
                b2 = 1 + alphaQ;
                a0 = 1 + alphaQ;
                a1 = -2 * cos;
                a2 = 1 - alphaQ;
                break;
            case PEAKING:
                b0 = 1 + alphaQ * a;
                b1 = -2 * cos;
                b2 = 1 - alphaQ * a;
                a0 = 1 + alphaQ / a;
                a1 = -2 * cos;
                a2 = 1 - alphaQ / a;
                break;
            case LOWSHELF:
                b0 = a * ((a + 1) - (a - 1) * cos + 2 * alphaS * sqrtA);
                b1 = 2 * a * ((a - 1) - (a + 1) * cos);
                b2 = a * ((a + 1) - (a - 1) * cos - 2 * alphaS * sqrtA);
                a0 = (a + 1) + (a - 1) * cos + 2 * alphaS * sqrtA;
                a1 = -2 * ((a - 1) + (a + 1) * cos);
                a2 = (a + 1) + (a - 1) * cos - 2 * alphaS * sqrtA;
                break;
            default:
                b0 = a * ((a + 1) + (a - 1) * cos + 2 * alphaS * sqrtA);
                b1 = -2 * a * ((a - 1) + (a + 1) * cos);
                b2 = a * ((a + 1) + (a - 1) * cos - 2 * alphaS * sqrtA);
                a0 = (a + 1) - (a - 1) * cos + 2 * alphaS * sqrtA;
                a1 = 2 * ((a - 1) - (a + 1) * cos);
                a2 = (a + 1) - (a - 1) * cos - 2 * alphaS * sqrtA;
                break;
        }
        c[0] = b0 / a0;
        c[1] = b1 / a0;
        c[2] = b2 / a0;
        c[3] = a1 / a0;
        c[4] = a2 / a0;
    }

    @Override
    void process(long qn) {
        AudioBus in = inBuses[0];
        AudioBus out = outBuses[0];
        float[] f = frequency.compute(qn);
        float[] dt = detune.compute(qn);
        float[] qq = q.compute(qn);
        float[] g = gain.compute(qn);
        boolean constant = frequency.isConstant() && detune.isConstant() && q.isConstant() && gain.isConstant();
        out.setChannels(in.channels);
        float sr = context.getSampleRate();
        double[] c = coef;
        if (constant) {
            coefficients(rType, f[0], dt[0], qq[0], g[0], sr, c);
        }
        for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
            if (!constant) {
                coefficients(rType, f[i], dt[i], qq[i], g[i], sr, c);
            }
            for (int ch = 0; ch < in.channels; ch++) {
                double x = in.data[ch][i];
                double y = c[0] * x + c[1] * x1[ch] + c[2] * x2[ch] - c[3] * y1[ch] - c[4] * y2[ch];
                x2[ch] = x1[ch];
                x1[ch] = x;
                y2[ch] = y1[ch];
                if (!(Math.abs(y) >= 1e-30) || Double.isInfinite(y)) { // NOPMD LogicInversion - NaN resets the state
                    y = 0;
                }
                y1[ch] = y;
                out.data[ch][i] = (float) y;
            }
        }
    }
}
