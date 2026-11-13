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

import com.codename1.io.Log;
import com.codename1.ui.Display;
import java.util.concurrent.atomic.AtomicReference;

/// Renders an audio graph to the device's speaker in real time, the Web Audio
/// `AudioContext`.
///
/// Not every platform has a real-time audio output; check [#isSupported()] or
/// test [#create()] for null, and fall back to `Media` or `SoundPool` playback.
/// [OfflineAudioContext] works everywhere.
///
/// ```java
/// AudioContext ctx = AudioContext.create();
/// if (ctx != null) {
///     OscillatorNode osc = ctx.createOscillator();
///     GainNode amp = ctx.createGain();
///     osc.connect(amp).connect(ctx.getDestination());
///     double now = ctx.getCurrentTime();
///     amp.getGain().setValueAtTime(0, now).linearRampToValueAtTime(0.5f, now + 0.02)
///             .setTargetAtTime(0, now + 0.1, 0.2);
///     osc.start(now);
///     osc.stop(now + 1.5);
/// }
/// ```
///
/// The graph renders on the platform's audio thread. Callbacks such as
/// [AudioScheduledSourceNode#setOnEnded(Runnable)] are delivered on the EDT.
public final class AudioContext extends BaseAudioContext {
    private final AudioOutputPeer peer;
    private AudioContextState state = AudioContextState.RUNNING;

    // ---- render side ----
    private AudioBus rBus;
    private int rPos = RENDER_QUANTUM;
    private boolean rFailed;

    private AudioContext(AudioOutputPeer peer) {
        super(peer.getSampleRate(), peer.getChannels(), peer.getChannels());
        this.peer = peer;
    }

    /// Whether this platform can play an audio graph in real time.
    ///
    /// #### Returns
    ///
    /// true when [#create()] can succeed
    public static boolean isSupported() {
        return Display.getInstance().isAudioOutputSupported();
    }

    /// Opens the default output at the device's preferred sample rate.
    ///
    /// #### Returns
    ///
    /// a running context, or null when the platform has no real-time output
    public static AudioContext create() {
        return create(0);
    }

    /// Opens the default output.
    ///
    /// #### Parameters
    ///
    /// - `sampleRate`: the rate wanted, or 0 for the device's own; the device
    ///   may run at another, which [#getSampleRate()] reports
    ///
    /// #### Returns
    ///
    /// a running context, or null when the platform has no real-time output
    public static AudioContext create(int sampleRate) {
        if (!isSupported()) {
            return null;
        }
        Forwarder f = new Forwarder();
        AudioOutputPeer peer = Display.getInstance().openAudioOutput(sampleRate, 2, f);
        if (peer == null) {
            return null;
        }
        AudioContext ctx = new AudioContext(peer);
        f.target.set(ctx);
        peer.start();
        return ctx;
    }

    @Override
    public AudioContextState getState() {
        return state;
    }

    /// The processing latency the graph adds: one render quantum.
    ///
    /// #### Returns
    ///
    /// seconds
    public double getBaseLatency() {
        return RENDER_QUANTUM / (double) getSampleRate();
    }

    /// The latency the platform reports between rendering and hearing a sample.
    ///
    /// #### Returns
    ///
    /// seconds, or 0 when unknown
    public double getOutputLatency() {
        return peer.getOutputLatency();
    }

    /// Pauses rendering; the clock stops.
    public void suspend() {
        if (state == AudioContextState.RUNNING) {
            peer.stop();
            state = AudioContextState.SUSPENDED;
        }
    }

    /// Resumes rendering after [#suspend()].
    public void resume() {
        if (state == AudioContextState.CLOSED) {
            throw new IllegalStateException("The context is closed");
        }
        if (state == AudioContextState.SUSPENDED) {
            peer.start();
            state = AudioContextState.RUNNING;
        }
    }

    /// Releases the output for good.
    public void close() {
        if (state != AudioContextState.CLOSED) {
            peer.close();
            state = AudioContextState.CLOSED;
        }
    }

    @Override
    boolean dispatchesEndedInline() {
        return false;
    }

    void fill(float[] out, int frames, int channels) {
        if (rFailed) {
            silence(out, frames * channels);
            return;
        }
        try {
            int o = 0;
            int left = frames;
            while (left > 0) {
                if (rPos >= RENDER_QUANTUM) {
                    rBus = renderQuantum();
                    rPos = 0;
                }
                AudioBus bus = rBus;
                int n = Math.min(left, RENDER_QUANTUM - rPos);
                for (int i = 0; i < n; i++) {
                    for (int c = 0; c < channels; c++) {
                        out[o++] = c < bus.channels ? bus.data[c][rPos + i] : 0f;
                    }
                }
                rPos += n;
                left -= n;
            }
        } catch (RuntimeException e) {
            rFailed = true;
            Log.e(e);
            silence(out, frames * channels);
        }
    }

    private static void silence(float[] out, int n) {
        for (int i = 0; i < n; i++) {
            out[i] = 0f;
        }
    }

    /// Hands the port a callback before the context it renders exists.
    private static final class Forwarder implements AudioRenderCallback {
        final AtomicReference<AudioContext> target = new AtomicReference<AudioContext>();

        @Override
        public void render(float[] interleaved, int frames, int channels) {
            AudioContext ctx = target.get();
            if (ctx == null) {
                silence(interleaved, frames * channels);
            } else {
                ctx.fill(interleaved, frames, channels);
            }
        }
    }
}
