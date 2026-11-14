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

import com.codename1.ui.CN;

/// A source node that is started and stopped at scheduled times, the Web Audio
/// `AudioScheduledSourceNode`. A source can be started only once; create a new
/// one to play again.
public abstract class AudioScheduledSourceNode extends AudioNode {
    private boolean started;
    private Runnable onEnded;
    final Runnable endedTask = new Runnable() {
        @Override
        public void run() {
            Runnable r = onEnded;
            if (r != null) {
                r.run();
            }
        }
    };

    // ---- render side ----
    double rStart = Double.NaN;
    double rStop = Double.POSITIVE_INFINITY;
    boolean rEnded;
    /// First and one past the last active frame of the current quantum.
    int rFrom;
    int rTo;

    AudioScheduledSourceNode(BaseAudioContext context, int outputs) {
        super(context, 0, outputs, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
    }

    /// Starts playing now.
    public void start() {
        start(0);
    }

    /// Starts playing at `when`; a time already past starts at once.
    ///
    /// #### Parameters
    ///
    /// - `when`: context time in seconds
    public void start(double when) {
        startAt(when, null);
    }

    final void startAt(double when, final Runnable extra) {
        checkWhen(when);
        if (started) {
            throw new IllegalStateException("A source node can only be started once");
        }
        started = true;
        final double w = when;
        final AudioScheduledSourceNode self = this;
        context.post(new Runnable() {
            @Override
            public void run() {
                if (extra != null) {
                    extra.run();
                }
                rStart = w;
                context.rActivate(self);
            }
        });
    }

    /// Stops playing now.
    public void stop() {
        stop(0);
    }

    /// Stops playing at `when`. Calling it again replaces the earlier stop time.
    ///
    /// #### Parameters
    ///
    /// - `when`: context time in seconds
    public void stop(double when) {
        checkWhen(when);
        if (!started) {
            throw new IllegalStateException("stop() called before start()");
        }
        final double w = when;
        context.post(new Runnable() {
            @Override
            public void run() {
                rStop = w;
            }
        });
    }

    /// Sets what runs when the source stops playing, either because it reached
    /// its stop time or because it ran out of audio. It runs on the EDT for an
    /// [AudioContext], and on the rendering thread, between render quanta, for
    /// an [OfflineAudioContext].
    ///
    /// #### Parameters
    ///
    /// - `onEnded`: the callback, or null
    public void setOnEnded(Runnable onEnded) {
        this.onEnded = onEnded;
    }

    private static void checkWhen(double when) {
        if (!BaseAudioContext.nonNegativeFinite(when)) {
            throw new IllegalArgumentException("Time must be 0 or more and finite: " + when);
        }
    }

    /// Computes `rFrom`/`rTo` for quantum `q`. Returns false when nothing of this
    /// quantum plays, after ending the node if its stop time has passed.
    final boolean window(long q) {
        if (rEnded || Double.isNaN(rStart)) {
            return false;
        }
        float sr = context.getSampleRate();
        long frame0 = q * BaseAudioContext.RENDER_QUANTUM;
        long startFrame = (long) Math.ceil(BaseAudioContext.snapFrames(rStart * sr));
        long stopFrame = Double.isInfinite(rStop) ? Long.MAX_VALUE
                : (long) Math.ceil(BaseAudioContext.snapFrames(rStop * sr));
        long from = Math.max(frame0, startFrame);
        long to = Math.min(frame0 + BaseAudioContext.RENDER_QUANTUM, stopFrame);
        if (to <= from) {
            if (stopFrame <= frame0 + BaseAudioContext.RENDER_QUANTUM) {
                end();
            }
            return false;
        }
        rFrom = (int) (from - frame0);
        rTo = (int) (to - frame0);
        return true;
    }

    /// Whether the stop time falls inside or before the end of quantum `q`.
    final boolean stopsIn(long q) {
        if (Double.isInfinite(rStop)) {
            return false;
        }
        long stopFrame = (long) Math.ceil(BaseAudioContext.snapFrames(rStop * context.getSampleRate()));
        return stopFrame <= (q + 1) * BaseAudioContext.RENDER_QUANTUM;
    }

    /// Marks the node finished; outputs are silent from now on.
    final void end() {
        if (!rEnded) {
            rEnded = true;
            context.rEnded(this);
        }
    }

    final void dispatchEnded(boolean onThisThread) {
        if (onEnded == null) {
            return;
        }
        if (onThisThread) {
            endedTask.run();
        } else {
            CN.callSerially(endedTask);
        }
    }

    @Override
    final void process(long q) {
        for (int i = 0; i < outBuses.length; i++) {
            outBuses[i].setChannels(1);
        }
        if (!window(q)) {
            return;
        }
        render(q, rFrom, rTo);
        if (stopsIn(q)) {
            end();
        }
    }

    /// Renders active frames `from` to `to` of quantum `q`. The outputs were set
    /// to one silent channel; a subclass changes the count before writing.
    abstract void render(long q, int from, int to);
}
