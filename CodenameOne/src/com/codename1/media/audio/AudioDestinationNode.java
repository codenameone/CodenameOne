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

/// The end of every audio graph: the speaker for an [AudioContext], the
/// rendered [AudioData] for an [OfflineAudioContext].
public final class AudioDestinationNode extends AudioNode {
    private final int maxChannelCount;

    AudioDestinationNode(BaseAudioContext context, int channels, int maxChannelCount) {
        super(context, 1, 0, channels, ChannelCountMode.EXPLICIT, ChannelInterpretation.SPEAKERS);
        this.maxChannelCount = maxChannelCount;
    }

    /// The largest channel count the destination accepts.
    ///
    /// #### Returns
    ///
    /// the channel count of the device, or of the offline result
    public int getMaxChannelCount() {
        return maxChannelCount;
    }

    @Override
    void checkChannelCount(int count) {
        super.checkChannelCount(count);
        if (count > maxChannelCount) {
            throw new IllegalArgumentException("channelCount above maxChannelCount " + maxChannelCount);
        }
    }

    @Override
    void process(long q) {
        AudioBus in = inBuses[0];
        AudioBus out = outBuses[0];
        out.setChannels(in.channels);
        for (int c = 0; c < in.channels; c++) {
            System.arraycopy(in.data[c], 0, out.data[c], 0, BaseAudioContext.RENDER_QUANTUM);
        }
    }
}
