/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.android;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import com.codename1.impl.PushAudioOutput;
import com.codename1.media.audio.AudioRenderCallback;

/// Real-time output for `com.codename1.media.audio.AudioContext`: a streaming
/// 16-bit `AudioTrack`. The base class's render thread writes to it, and the
/// blocking `AudioTrack.write` paces that thread, so `offer` always takes every
/// frame. 16-bit PCM is used rather than float because float streaming needs
/// API 21 and the default minimum is lower; the graph renders in float and
/// only the last step is quantized.
final class AndroidAudioOutput extends PushAudioOutput {
    private final AudioTrack track;
    private final short[] pcm16;
    private final int bufferFrames;

    private AndroidAudioOutput(int rate, int channels, AudioRenderCallback callback, AudioTrack track,
                               int bufferFrames) {
        super(rate, channels, callback);
        this.track = track;
        this.bufferFrames = bufferFrames;
        pcm16 = new short[FRAMES * channels];
    }

    static AndroidAudioOutput open(int requestedRate, int channels, AudioRenderCallback callback) {
        int ch = channels == 1 ? 1 : 2;
        int rate = requestedRate > 0 ? requestedRate
                : AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
        int mask = ch == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        int min = AudioTrack.getMinBufferSize(rate, mask, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) {
            return null;
        }
        int bytes = Math.max(min, FRAMES * ch * 2 * 2);
        AudioTrack t;
        try {
            t = new AudioTrack(AudioManager.STREAM_MUSIC, rate, mask, AudioFormat.ENCODING_PCM_16BIT,
                    bytes, AudioTrack.MODE_STREAM);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (t.getState() != AudioTrack.STATE_INITIALIZED) {
            t.release();
            return null;
        }
        AndroidAudioOutput out = new AndroidAudioOutput(rate, ch, callback, t, bytes / (ch * 2));
        out.startThread();
        return out;
    }

    @Override
    protected int offer(float[] pcm, int offset, int frames) {
        int ch = getChannels();
        int n = frames * ch;
        int base = offset * ch;
        for (int i = 0; i < n; i++) {
            float v = pcm[base + i];
            if (v > 1f) {
                v = 1f;
            } else if (v < -1f) {
                v = -1f;
            }
            pcm16[i] = (short) (v * 32767f);
        }
        int written = track.write(pcm16, 0, n);
        return written <= 0 ? 0 : written / ch;
    }

    @Override
    protected void nativeStart() {
        track.play();
    }

    @Override
    protected void nativeStop() {
        track.pause();
    }

    @Override
    protected void nativeClose() {
        track.stop();
        track.release();
    }

    @Override
    public double getOutputLatency() {
        return bufferFrames / (double) getSampleRate();
    }
}
