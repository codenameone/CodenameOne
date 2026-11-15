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

/// Multiplies its input by [#getGain()], the Web Audio `GainNode`. The output
/// has the input's channel count.
public final class GainNode extends AudioNode {
    private final AudioParam gain;

    GainNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
        gain = new AudioParam(context, "gain", 1f, -Float.MAX_VALUE, Float.MAX_VALUE,
                AutomationRate.A_RATE, false);
    }

    /// The gain, 1 by default; an audio rate parameter.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getGain() {
        return gain;
    }

    @Override
    void process(long q) {
        AudioBus in = inBuses[0];
        AudioBus out = outBuses[0];
        float[] g = gain.compute(q);
        out.setChannels(in.channels);
        boolean constant = gain.isConstant();
        float g0 = g[0];
        for (int c = 0; c < in.channels; c++) {
            float[] s = in.data[c];
            float[] d = out.data[c];
            if (constant) {
                for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
                    d[i] = s[i] * g0;
                }
            } else {
                for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
                    d[i] = s[i] * g[i];
                }
            }
        }
    }
}
