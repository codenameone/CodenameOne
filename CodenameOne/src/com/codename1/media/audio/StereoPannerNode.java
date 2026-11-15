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

/// Places mono or stereo input in the stereo field with an equal-power law,
/// the Web Audio `StereoPannerNode`. The output is always stereo.
public final class StereoPannerNode extends AudioNode {
    private static final double HALF_PI = Math.PI / 2;
    private final AudioParam pan;

    StereoPannerNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.CLAMPED_MAX, ChannelInterpretation.SPEAKERS);
        pan = new AudioParam(context, "pan", 0f, -1f, 1f, AutomationRate.A_RATE, false);
    }

    /// The position, -1 (left) to 1 (right), 0 by default; an audio rate parameter.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getPan() {
        return pan;
    }

    @Override
    void checkChannelCount(int count) {
        if (count < 1 || count > 2) {
            throw new IllegalArgumentException("A stereo panner takes 1 or 2 channels: " + count);
        }
    }

    @Override
    void checkChannelCountMode(ChannelCountMode mode) {
        if (mode == ChannelCountMode.MAX) {
            throw new IllegalArgumentException("A stereo panner cannot use ChannelCountMode.MAX");
        }
    }

    @Override
    void process(long q) {
        AudioBus in = inBuses[0];
        AudioBus out = outBuses[0];
        float[] p = pan.compute(q);
        out.setChannels(2);
        float[] l = out.data[0];
        float[] r = out.data[1];
        boolean mono = in.channels == 1;
        float[] inL = in.data[0];
        float[] inR = mono ? inL : in.data[1];
        boolean constant = pan.isConstant();
        double gl = 0;
        double gr = 0;
        for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
            double x = p[i];
            if (i == 0 || !constant) {
                double pos = mono ? (x + 1) / 2 : (x <= 0 ? x + 1 : x);
                gl = Math.cos(pos * HALF_PI);
                gr = Math.sin(pos * HALF_PI);
            }
            if (mono) {
                l[i] = (float) (inL[i] * gl);
                r[i] = (float) (inL[i] * gr);
            } else if (x <= 0) {
                l[i] = (float) (inL[i] + inR[i] * gl);
                r[i] = (float) (inR[i] * gr);
            } else {
                l[i] = (float) (inL[i] * gl);
                r[i] = (float) (inR[i] + inL[i] * gr);
            }
        }
    }
}
