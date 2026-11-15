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

/// Plays [AudioData], the Web Audio `AudioBufferSourceNode`. Its output has the
/// data's channel count. Data at another sample rate is resampled with linear
/// interpolation.
///
/// ```java
/// OfflineAudioContext audio = new OfflineAudioContext(2, 44100, 44100);
/// AudioData clip = audio.createBuffer(1, 44100, 44100); // or decodeAudioData
/// AudioBufferSourceNode src = audio.createBufferSource();
/// src.setBuffer(clip);
/// src.getPlaybackRate().setValue(1.5f);
/// src.connect(audio.getDestination());
/// src.start(audio.getCurrentTime() + 0.1);
/// ```
public final class AudioBufferSourceNode extends AudioScheduledSourceNode {
    private final AudioParam playbackRate;
    private final AudioParam detune;
    private AudioData buffer;
    private boolean loop;
    private double loopStart;
    private double loopEnd;

    // ---- render side ----
    private AudioData rBuffer;
    private boolean rLoop;
    private double rLoopStart;
    private double rLoopEnd;
    private double rOffset;
    private double rDuration = Double.POSITIVE_INFINITY;
    private boolean rPlaying;
    private double rPosition;
    private double rPlayed;

    AudioBufferSourceNode(BaseAudioContext context) {
        super(context, 1);
        playbackRate = new AudioParam(context, "playbackRate", 1f, -Float.MAX_VALUE, Float.MAX_VALUE,
                AutomationRate.K_RATE, true);
        detune = new AudioParam(context, "detune", 0f, -Float.MAX_VALUE, Float.MAX_VALUE,
                AutomationRate.K_RATE, true);
    }

    /// The speed factor, 1 by default; negative plays backwards. A control rate
    /// parameter.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getPlaybackRate() {
        return playbackRate;
    }

    /// A detune in cents, combined with the playback rate; a control rate
    /// parameter.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getDetune() {
        return detune;
    }

    /// The audio played.
    ///
    /// #### Returns
    ///
    /// the data, or null
    public AudioData getBuffer() {
        return buffer;
    }

    /// Sets the audio to play. It can be set once; a source without data plays
    /// one channel of silence.
    ///
    /// #### Parameters
    ///
    /// - `buffer`: the data
    public void setBuffer(final AudioData buffer) {
        if (this.buffer != null && buffer != null) {
            throw new IllegalStateException("The buffer of a source can be set only once");
        }
        this.buffer = buffer;
        context.post(new Runnable() {
            @Override
            public void run() {
                rBuffer = buffer;
            }
        });
    }

    /// Whether playback loops between [#getLoopStart()] and [#getLoopEnd()].
    ///
    /// #### Returns
    ///
    /// true when looping
    public boolean isLoop() {
        return loop;
    }

    /// Turns looping on or off.
    ///
    /// #### Parameters
    ///
    /// - `loop`: true to loop
    public void setLoop(final boolean loop) {
        this.loop = loop;
        context.post(new Runnable() {
            @Override
            public void run() {
                rLoop = loop;
            }
        });
    }

    /// The start of the loop in seconds of the data.
    ///
    /// #### Returns
    ///
    /// seconds
    public double getLoopStart() {
        return loopStart;
    }

    /// Sets the start of the loop.
    ///
    /// #### Parameters
    ///
    /// - `loopStart`: seconds of the data
    public void setLoopStart(final double loopStart) {
        this.loopStart = loopStart;
        context.post(new Runnable() {
            @Override
            public void run() {
                rLoopStart = loopStart;
            }
        });
    }

    /// The end of the loop in seconds of the data; 0 (the default), or any value
    /// not after the start, means the end of the data.
    ///
    /// #### Returns
    ///
    /// seconds
    public double getLoopEnd() {
        return loopEnd;
    }

    /// Sets the end of the loop.
    ///
    /// #### Parameters
    ///
    /// - `loopEnd`: seconds of the data
    public void setLoopEnd(final double loopEnd) {
        this.loopEnd = loopEnd;
        context.post(new Runnable() {
            @Override
            public void run() {
                rLoopEnd = loopEnd;
            }
        });
    }

    /// Starts playing at `when` from `offset` seconds into the data.
    ///
    /// #### Parameters
    ///
    /// - `when`: context time in seconds
    ///
    /// - `offset`: where in the data to start
    public void start(double when, double offset) {
        start(when, offset, Double.POSITIVE_INFINITY);
    }

    /// Starts playing at `when` from `offset` seconds into the data, for
    /// `duration` seconds of the data (loops included).
    ///
    /// #### Parameters
    ///
    /// - `when`: context time in seconds
    ///
    /// - `offset`: where in the data to start, 0 or more
    ///
    /// - `duration`: how much of the data to play, more than 0
    public void start(double when, final double offset, final double duration) {
        if (!BaseAudioContext.nonNegativeFinite(offset)) {
            throw new IllegalArgumentException("offset must be 0 or more: " + offset);
        }
        if (Double.isNaN(duration) || duration <= 0) {
            throw new IllegalArgumentException("duration must be more than 0: " + duration);
        }
        startAt(when, new Runnable() {
            @Override
            public void run() {
                rOffset = offset;
                rDuration = duration;
            }
        });
    }

    @Override
    void render(long q, int from, int to) {
        AudioData data = rBuffer;
        if (data == null) {
            return;
        }
        float[][] src = data.channels();
        int channels = src.length;
        int len = data.getLength();
        AudioBus out = outBuses[0];
        out.setChannels(channels);
        double bufferRate = data.getSampleRate();
        double rate = playbackRate.compute(q)[0];
        float cents = detune.compute(q)[0];
        if (cents != 0f) {
            rate *= MathUtil.pow(2, cents / 1200.0);
        }
        double step = rate * bufferRate / context.getSampleRate();
        double loopA = 0;
        double loopB = len;
        if (rLoop) {
            double a = rLoopStart * bufferRate;
            double b = rLoopEnd * bufferRate;
            if (rLoopEnd > 0 && b > a && a >= 0) {
                loopA = Math.max(0, a);
                loopB = Math.min(len, b);
            }
        }
        double limit = rDuration * bufferRate;
        if (!rPlaying) {
            rPlaying = true;
            rPosition = Math.min(BaseAudioContext.snapFrames(rOffset * bufferRate), len);
            if (step < 0 && rOffset == 0) {
                rPosition = rLoop ? loopB : len;
            }
            rPlayed = 0;
        }
        double pos = rPosition;
        for (int i = from; i < to; i++) {
            if (rLoop) {
                double span = loopB - loopA;
                if (span > 0) {
                    while (pos >= loopB) {
                        pos -= span;
                    }
                    while (pos < loopA && step < 0) {
                        pos += span;
                    }
                }
            } else if (pos >= len || pos < 0 || (step < 0 && pos <= 0)) {
                end();
                break;
            }
            if (rPlayed >= limit) {
                end();
                break;
            }
            int i0 = (int) Math.floor(pos);
            double frac = pos - i0;
            int i1 = i0 + 1;
            if (i1 >= len) {
                i1 = rLoop ? (int) loopA : -1;
            }
            for (int c = 0; c < channels; c++) {
                float[] s = src[c];
                float a = i0 >= 0 && i0 < len ? s[i0] : 0f;
                if (frac == 0) {
                    out.data[c][i] = a;
                } else {
                    float b = i1 >= 0 ? s[i1] : 0f;
                    out.data[c][i] = (float) (a + (b - a) * frac);
                }
            }
            pos += step;
            rPlayed += Math.abs(step);
        }
        rPosition = pos;
    }
}
