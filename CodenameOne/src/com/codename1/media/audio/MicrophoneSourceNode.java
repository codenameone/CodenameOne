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

import com.codename1.media.AudioBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/// Live captured audio as a graph source, the Codename One counterpart of the
/// Web Audio `MediaStreamAudioSourceNode` for a microphone. Created by
/// [BaseAudioContext#createMicrophoneSource(AudioBuffer)] over the
/// [AudioBuffer] a recorder fills when it is built with
/// `MediaRecorderBuilder.redirectToAudioBuffer(true)`.
///
/// The node outputs one or two channels, as the capture delivers them (a
/// capture with more keeps the first two), resampled to the context's rate.
/// It keeps its delay short: when the graph falls behind it drops the oldest
/// captured audio rather than fall further behind, and when capture stalls it
/// outputs silence. Call [#close()] to detach it from the buffer.
public final class MicrophoneSourceNode extends AudioNode {
    private static final int CAPACITY = 16384;
    private static final int MASK = CAPACITY - 1;

    private final AudioBuffer source;
    private final float[] ring = new float[CAPACITY * 2];
    private final AtomicLong written = new AtomicLong();
    private final AtomicLong read = new AtomicLong();
    private final AtomicInteger sourceRate = new AtomicInteger();
    private final AtomicInteger sourceChannels = new AtomicInteger(1);
    private final AtomicInteger lastBlock = new AtomicInteger();
    private float[] scratch = new float[0];
    private final AudioBuffer.AudioBufferCallback callback = new AudioBuffer.AudioBufferCallback() {
        @Override
        public void frameReceived(AudioBuffer buffer) {
            receive(buffer);
        }
    };

    // ---- render side ----
    private double rFrac;

    MicrophoneSourceNode(BaseAudioContext context, AudioBuffer source) {
        super(context, 0, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
        if (source == null) {
            throw new IllegalArgumentException("source");
        }
        this.source = source;
        source.addCallback(callback);
    }

    /// The buffer this node reads.
    ///
    /// #### Returns
    ///
    /// the capture buffer
    public AudioBuffer getSource() {
        return source;
    }

    /// Stops listening to the capture buffer; the node outputs silence after
    /// what it already received. Recording itself is the recorder's to stop.
    public void close() {
        source.removeCallback(callback);
    }

    /// Capture thread: appends a block to the ring, dropping what does not fit.
    void receive(AudioBuffer buffer) {
        int n = buffer.getSize();
        int ch = buffer.getNumChannels();
        if (n <= 0 || ch <= 0) {
            return;
        }
        if (scratch.length < n) {
            scratch = new float[n];
        }
        buffer.copyTo(scratch);
        int frames = n / ch;
        long w = written.get();
        long free = CAPACITY - (w - read.get());
        if (frames > free) {
            frames = (int) free;
        }
        for (int f = 0; f < frames; f++) {
            int at = (int) ((w + f) & MASK) * 2;
            float l = scratch[f * ch];
            ring[at] = l;
            ring[at + 1] = ch > 1 ? scratch[f * ch + 1] : l;
        }
        sourceRate.set(buffer.getSampleRate());
        sourceChannels.set(ch > 1 ? 2 : 1);
        lastBlock.set(n / ch);
        written.set(w + frames);
    }

    @Override
    void process(long q) {
        AudioBus out = outBuses[0];
        int ch = sourceChannels.get();
        out.setChannels(ch);
        int rate = sourceRate.get();
        if (rate <= 0) {
            return;
        }
        long w = written.get();
        long r = read.get();
        int block = lastBlock.get();
        if (w - r > 2L * block + 512) {
            r = w - block - 256;
            rFrac = 0;
        }
        double step = rate / (double) context.getSampleRate();
        float[] left = out.data[0];
        float[] right = ch > 1 ? out.data[1] : null;
        double frac = rFrac;
        for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
            int advance = (int) frac;
            r += advance;
            frac -= advance;
            if (r + 1 >= w) {
                frac = 0;
                break;
            }
            int a = (int) (r & MASK) * 2;
            int b = (int) ((r + 1) & MASK) * 2;
            float t = (float) frac;
            left[i] = ring[a] + (ring[b] - ring[a]) * t;
            if (right != null) {
                right[i] = ring[a + 1] + (ring[b + 1] - ring[a + 1]) * t;
            }
            frac += step;
        }
        rFrac = frac;
        read.set(r);
    }
}
