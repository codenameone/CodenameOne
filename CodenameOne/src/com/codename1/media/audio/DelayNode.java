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

/// Delays its input by [#getDelayTime()], the Web Audio `DelayNode`. Fractional
/// delays are linearly interpolated.
///
/// A delay is the only node that may close a cycle, as in a feedback echo:
///
/// ```java
/// DelayNode echo = ctx.createDelay(1);
/// echo.getDelayTime().setValue(0.3f);
/// GainNode feedback = ctx.createGain();
/// feedback.getGain().setValue(0.4f);
/// source.connect(echo).connect(feedback).connect(echo);
/// echo.connect(ctx.getDestination());
/// ```
///
/// Inside a cycle the delay cannot be shorter than one render quantum (128
/// frames), as the specification requires.
public final class DelayNode extends AudioNode {
    private final AudioParam delayTime;
    private final double maxDelayTime;

    // ---- render side ----
    private final int ringSize;
    private final float[][] ring = new float[AudioBus.MAX_CHANNELS][];
    private int ringChannels = 1;
    private long writeFrame;
    private long cycleQuantum = -1;

    DelayNode(BaseAudioContext context, double maxDelayTime) {
        super(context, 1, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
        if (!(maxDelayTime > 0 && maxDelayTime < 180)) {
            throw new IllegalArgumentException("maxDelayTime must be more than 0 and less than 180: "
                    + maxDelayTime);
        }
        this.maxDelayTime = maxDelayTime;
        delayTime = new AudioParam(context, "delayTime", 0f, 0f, (float) maxDelayTime,
                AutomationRate.A_RATE, false);
        ringSize = (int) Math.ceil(maxDelayTime * context.getSampleRate()) + 2 * BaseAudioContext.RENDER_QUANTUM + 2;
        // Mono and stereo rings are made here, on the caller's thread, so the
        // renderer never allocates one for the usual layouts; a delay fed
        // more than two channels allocates the rest on first use.
        ring[0] = new float[ringSize];
        ring[1] = new float[ringSize];
    }

    /// The delay in seconds, 0 by default, up to the maximum; an audio rate
    /// parameter.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getDelayTime() {
        return delayTime;
    }

    /// The longest delay this node supports.
    ///
    /// #### Returns
    ///
    /// seconds
    public double getMaxDelayTime() {
        return maxDelayTime;
    }

    private float[] ringChannel(int c) {
        float[] r = ring[c];
        if (r == null) {
            r = new float[ringSize];
            ring[c] = r;
        }
        return r;
    }

    /// Reached through a cycle: answer from what is already in the ring, with
    /// the delay held to at least one quantum so no current input is needed.
    @Override
    AudioBus reentered(int output, long q) {
        AudioBus out = outBuses[0];
        if (cycleQuantum != q) {
            cycleQuantum = q;
            float[] d = delayTime.compute(q);
            out.setChannels(ringChannels);
            float sr = context.getSampleRate();
            for (int c = 0; c < ringChannels; c++) {
                float[] r = ringChannel(c);
                float[] o = out.data[c];
                for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
                    double frames = Math.max(BaseAudioContext.snapFrames(d[i] * (double) sr),
                            BaseAudioContext.RENDER_QUANTUM);
                    o[i] = read(r, writeFrame + i - frames);
                }
            }
        }
        return out;
    }

    @Override
    void process(long q) {
        AudioBus in = inBuses[0];
        boolean cycled = cycleQuantum == q;
        float[] d = delayTime.compute(q);
        if (in.channels > ringChannels) {
            ringChannels = in.channels;
        }
        float sr = context.getSampleRate();
        AudioBus out = outBuses[0];
        if (!cycled) {
            out.setChannels(ringChannels);
        }
        for (int c = 0; c < ringChannels; c++) {
            float[] r = ringChannel(c);
            float[] s = c < in.channels ? in.data[c] : null;
            float[] o = cycled ? null : out.data[c];
            for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
                long w = writeFrame + i;
                r[(int) (w % ringSize)] = s == null ? 0f : s[i];
                if (o != null) {
                    o[i] = read(r, w - BaseAudioContext.snapFrames(d[i] * (double) sr));
                }
            }
        }
        writeFrame += BaseAudioContext.RENDER_QUANTUM;
    }

    private float read(float[] r, double pos) {
        if (pos < 0) {
            return 0f;
        }
        long i0 = (long) Math.floor(pos);
        double frac = pos - i0;
        float a = r[(int) (i0 % ringSize)];
        if (frac == 0) {
            return a;
        }
        float b = r[(int) ((i0 + 1) % ringSize)];
        return (float) (a + (b - a) * frac);
    }
}
