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

import java.util.concurrent.atomic.AtomicInteger;

/// Lowers the volume of loud passages so the mix stays clear of clipping: the
/// Web Audio `DynamicsCompressorNode`.
///
/// Every parameter is k-rate. The detector follows the loudest channel, the
/// gain curve has a soft knee [#getKnee()] decibels wide centred on
/// [#getThreshold()], and gain changes are smoothed by [#getAttack()] and
/// [#getRelease()]. As in browsers the signal is delayed by a 6 ms look-ahead
/// and a make-up gain raises the result so that a full-scale signal comes out
/// near full scale. The specification leaves the exact curve to the
/// implementation, so values match browsers in shape, not to the sample.
///
/// ```java
/// OfflineAudioContext audio = new OfflineAudioContext(2, 44100, 44100);
/// GainNode mix = audio.createGain();
/// DynamicsCompressorNode comp = audio.createDynamicsCompressor();
/// comp.getThreshold().setValue(-18);
/// comp.getRatio().setValue(4);
/// mix.connect(comp).connect(audio.getDestination());
/// ```
public final class DynamicsCompressorNode extends AudioNode {
    private static final double LOOKAHEAD_SECONDS = 0.006;
    private static final double MAKEUP_EXPONENT = 0.6;

    private final AudioParam threshold;
    private final AudioParam knee;
    private final AudioParam ratio;
    private final AudioParam attack;
    private final AudioParam release;
    /// The current gain reduction in decibels, as float bits.
    private final AtomicInteger reduction = new AtomicInteger(Float.floatToIntBits(0f));

    private final float[][] delay = new float[2][];
    private final int lookahead;
    private int delayPos;
    private double envelopeDb;

    DynamicsCompressorNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.CLAMPED_MAX, ChannelInterpretation.SPEAKERS);
        threshold = new AudioParam(context, "threshold", -24, -100, 0, AutomationRate.K_RATE, true);
        knee = new AudioParam(context, "knee", 30, 0, 40, AutomationRate.K_RATE, true);
        ratio = new AudioParam(context, "ratio", 12, 1, 20, AutomationRate.K_RATE, true);
        attack = new AudioParam(context, "attack", 0.003f, 0, 1, AutomationRate.K_RATE, true);
        release = new AudioParam(context, "release", 0.25f, 0, 1, AutomationRate.K_RATE, true);
        lookahead = Math.max(1, (int) Math.round(LOOKAHEAD_SECONDS * context.getSampleRate()));
        for (int c = 0; c < 2; c++) {
            delay[c] = new float[lookahead];
        }
    }

    @Override
    void checkChannelCount(int count) {
        if (count < 1 || count > 2) {
            throw new IllegalArgumentException("A compressor takes 1 or 2 channels: " + count);
        }
    }

    @Override
    void checkChannelCountMode(ChannelCountMode mode) {
        if (mode == ChannelCountMode.MAX) {
            throw new IllegalArgumentException("A compressor cannot use ChannelCountMode.MAX");
        }
    }

    /// The level, in dB, above which the signal is compressed; -24 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getThreshold() {
        return threshold;
    }

    /// The width of the soft knee in dB, 30 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getKnee() {
        return knee;
    }

    /// How many dB of input above the threshold make one dB of output, 12 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getRatio() {
        return ratio;
    }

    /// How fast the gain falls when the signal gets louder: the time constant in
    /// seconds, 0.003 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getAttack() {
        return attack;
    }

    /// How fast the gain recovers when the signal gets quieter: the time constant
    /// in seconds, 0.25 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getRelease() {
        return release;
    }

    /// The gain reduction currently applied, in dB: 0 or negative.
    ///
    /// #### Returns
    ///
    /// decibels
    public float getReduction() {
        return Float.intBitsToFloat(reduction.get());
    }

    /// The static curve in dB: output level for input level `x`.
    static double curve(double x, double t, double w, double r) {
        double over = x - t;
        if (2 * over < -w) {
            return x;
        }
        if (w > 0 && 2 * Math.abs(over) <= w) {
            double a = over + w / 2;
            return x + (1 / r - 1) * a * a / (2 * w);
        }
        return t + over / r;
    }

    private static double toDb(double v) {
        return v <= 1e-10 ? -200 : 20 * MathUtil.log10(v);
    }

    private static double fromDb(double db) {
        return MathUtil.pow(10, db / 20);
    }

    @Override
    void process(long q) {
        double t = threshold.compute(q)[0];
        double w = knee.compute(q)[0];
        double r = ratio.compute(q)[0];
        double a = attack.compute(q)[0];
        double rel = release.compute(q)[0];
        double sr = context.getSampleRate();
        // time to move 10 dB -> per-sample coefficient of a one-pole smoother
        double attackCoef = a <= 0 ? 0 : MathUtil.exp(-1 / (a * sr));
        double releaseCoef = rel <= 0 ? 0 : MathUtil.exp(-1 / (rel * sr));
        double makeup = MathUtil.pow(1 / fromDb(curve(0, t, w, r)), MAKEUP_EXPONENT);
        AudioBus in = inBuses[0];
        AudioBus out = outBuses[0];
        int ch = Math.min(2, in.channels);
        out.setChannels(ch);
        double env = envelopeDb;
        int pos = delayPos;
        for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
            double peak = 0;
            for (int c = 0; c < ch; c++) {
                double v = Math.abs(in.data[c][i]);
                if (v > peak) {
                    peak = v;
                }
            }
            double xDb = toDb(peak);
            double target = curve(xDb, t, w, r) - xDb;
            double coef = target < env ? attackCoef : releaseCoef;
            env = target + coef * (env - target);
            double g = fromDb(env) * makeup;
            for (int c = 0; c < ch; c++) {
                float[] line = delay[c];
                float delayed = line[pos];
                line[pos] = in.data[c][i];
                out.data[c][i] = (float) (delayed * g);
            }
            pos++;
            if (pos == lookahead) {
                pos = 0;
            }
        }
        envelopeDb = env;
        delayPos = pos;
        reduction.set(Float.floatToIntBits((float) Math.min(0, env)));
    }
}
