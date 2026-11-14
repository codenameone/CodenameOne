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

/// Renders an audio graph to memory as fast as the device can, the Web Audio
/// `OfflineAudioContext`. It needs no audio hardware, so it works on every
/// platform: use it to bounce a mix, pre-render effects, or test a graph.
///
/// ```java
/// OfflineAudioContext audio = new OfflineAudioContext(2, 44100, 44100);
/// OscillatorNode osc = audio.createOscillator();
/// osc.connect(audio.getDestination());
/// osc.start();
/// AudioData oneSecond = audio.startRendering();
/// ```
public final class OfflineAudioContext extends BaseAudioContext {
    private final int length;
    private AudioContextState state = AudioContextState.SUSPENDED;

    /// Creates an offline context.
    ///
    /// #### Parameters
    ///
    /// - `numberOfChannels`: channels of the result, 1 to 32
    ///
    /// - `length`: frames to render, at least 1
    ///
    /// - `sampleRate`: frames per second, 3000 to 768000
    public OfflineAudioContext(int numberOfChannels, int length, float sampleRate) {
        super(sampleRate, checkChannels(numberOfChannels), numberOfChannels);
        if (length < 1) {
            throw new IllegalArgumentException("length must be at least 1: " + length);
        }
        this.length = length;
    }

    private static int checkChannels(int n) {
        if (n < 1 || n > AudioBus.MAX_CHANNELS) {
            throw new IllegalArgumentException("numberOfChannels must be between 1 and "
                    + AudioBus.MAX_CHANNELS + ": " + n);
        }
        return n;
    }

    /// The number of frames [#startRendering()] produces.
    ///
    /// #### Returns
    ///
    /// frames
    public int getLength() {
        return length;
    }

    @Override
    public AudioContextState getState() {
        return state;
    }

    /// Renders the whole graph on the calling thread and returns the result.
    /// `onended` callbacks run on this thread between render quanta. Do not
    /// call it on the EDT for long renders.
    ///
    /// #### Returns
    ///
    /// `getLength()` frames with the destination's channel count
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: if rendering was already started
    public AudioData startRendering() {
        if (state != AudioContextState.SUSPENDED) {
            throw new IllegalStateException("An offline context renders only once");
        }
        state = AudioContextState.RUNNING;
        int channels = getDestination().getChannelCount();
        AudioData result = new AudioData(channels, length, getSampleRate());
        float[][] dst = result.channels();
        int done = 0;
        while (done < length) {
            AudioBus bus = renderQuantum();
            int n = Math.min(RENDER_QUANTUM, length - done);
            for (int c = 0; c < channels; c++) {
                if (c < bus.channels) {
                    System.arraycopy(bus.data[c], 0, dst[c], done, n);
                }
            }
            done += n;
        }
        state = AudioContextState.CLOSED;
        return result;
    }

    @Override
    boolean dispatchesEndedInline() {
        return true;
    }
}
