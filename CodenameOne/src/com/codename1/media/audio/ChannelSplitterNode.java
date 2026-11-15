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

/// Splits the channels of its input into mono outputs, the Web Audio
/// `ChannelSplitterNode`. Output `k` carries input channel `k`, or silence when
/// the input has fewer channels.
public final class ChannelSplitterNode extends AudioNode {
    ChannelSplitterNode(BaseAudioContext context, int outputs) {
        super(context, 1, checkCount(outputs), outputs, ChannelCountMode.EXPLICIT,
                ChannelInterpretation.DISCRETE);
    }

    static int checkCount(int n) {
        if (n < 1 || n > AudioBus.MAX_CHANNELS) {
            throw new IllegalArgumentException("Count must be between 1 and " + AudioBus.MAX_CHANNELS + ": " + n);
        }
        return n;
    }

    @Override
    void checkChannelCount(int count) {
        if (count != getNumberOfOutputs()) {
            throw new IllegalArgumentException("A splitter's channel count is its number of outputs");
        }
    }

    @Override
    void checkChannelCountMode(ChannelCountMode mode) {
        if (mode != ChannelCountMode.EXPLICIT) {
            throw new IllegalArgumentException("A splitter's channel count mode is EXPLICIT");
        }
    }

    @Override
    void checkChannelInterpretation(ChannelInterpretation interpretation) {
        if (interpretation != ChannelInterpretation.DISCRETE) {
            throw new IllegalArgumentException("A splitter's channel interpretation is DISCRETE");
        }
    }

    @Override
    void process(long q) {
        AudioBus in = inBuses[0];
        for (int k = 0; k < outBuses.length; k++) {
            AudioBus out = outBuses[k];
            out.setChannels(1);
            if (k < in.channels) {
                System.arraycopy(in.data[k], 0, out.data[0], 0, BaseAudioContext.RENDER_QUANTUM);
            }
        }
    }
}
