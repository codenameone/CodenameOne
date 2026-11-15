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
package com.codename1.unitycompat.unityengine.ui;

import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.media.audio.AudioBufferSourceNode;
import com.codename1.media.audio.AudioContext;
import com.codename1.media.audio.AudioData;
import com.codename1.media.audio.GainNode;
import com.codename1.media.audio.StereoPannerNode;
import com.codename1.ui.Display;
import com.codename1.unitycompat.unityengine.AudioClip;
import com.codename1.unitycompat.unityengine.AudioOutput;
import com.codename1.unitycompat.unityengine.UnityRuntime;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/// Makes the sounds the runtime asks for on Codename One's audio graph,
/// where the device has one (`AudioContext.isSupported()`).
///
/// Every voice is a buffer source through its own gain and stereo panner
/// into one gain for the listener's volume, so a voice is heard at its
/// pitch, loops without a gap, moves within its clip at once and is panned
/// where [AudioOutput#place] puts it. A clip the graph cannot decode -- it
/// reads WAV -- is handed to a [UnityGameAudio] instead, and is heard as
/// that class plays it: at its recorded speed, with the attenuation and
/// without the pan.
///
/// The context is opened at the first clip the graph plays, so a game with
/// no sound keeps no audio output running.
///
/// `AudioListener.pause` suspends the whole context, which stops every
/// voice where it is, and resuming it lets each go on from its place.
public final class UnityGraphAudio implements AudioOutput {
    private static final Object UNDECODABLE = new Object();

    private AudioContext context;
    private GainNode master;
    private float listener = 1f;
    /// The device refused a context once; every clip then goes to the fallback.
    private boolean refused;
    private final UnityGameAudio fallback = new UnityGameAudio();
    /// A voice number to its [Voice], for those the graph plays.
    private final HashMap voices = new HashMap();
    /// A clip's resource to its decoded [AudioData], or [#UNDECODABLE].
    private final HashMap decoded = new HashMap();
    private boolean held;

    private static final class Voice {
        AudioData data;
        AudioBufferSourceNode source;
        GainNode gain;
        StereoPannerNode panner;
        float volume;
        float spatial = 1f;
        float pitch = 1f;
        boolean loop;
        boolean paused;
        /// Where in the clip the voice was, in seconds, at `startedAt`.
        double offset;
        double startedAt;
    }

    private UnityGraphAudio() {
    }

    /// The context, opened on first use; null if the device refused it.
    private AudioContext context() {
        if (context == null) {
            AudioContext c = refused ? null : AudioContext.create();
            if (c == null) {
                refused = true;
                return null;
            }
            context = c;
            master = c.createGain();
            master.getGain().setValue(listener);
            master.connect(c.getDestination());
            if (held) {
                c.suspend();
            }
        }
        return context;
    }

    /// An output on a new real-time context, or null where the device has
    /// no real-time audio; the caller then uses a [UnityGameAudio].
    ///
    /// #### Returns
    ///
    /// the output, or null
    public static UnityGraphAudio create() {
        return AudioContext.isSupported() ? new UnityGraphAudio() : null;
    }

    private AudioData decode(AudioClip clip) {
        Object known = decoded.get(clip.resource);
        if (known instanceof AudioData) {
            return (AudioData) known;
        }
        if (known != null) {
            return null;
        }
        AudioContext c = context();
        if (c == null) {
            return null;
        }
        AudioData data = null;
        InputStream in = Display.getInstance().getResourceAsStream(getClass(), "/" + clip.resource); // NOPMD CloseResource - closed in finally
        if (in != null) {
            try {
                data = c.decodeAudioData(Util.readInputStream(in));
            } catch (IOException e) {
                Log.e(e);
            } finally {
                Util.cleanup(in);
            }
        }
        decoded.put(clip.resource, data == null ? UNDECODABLE : data);
        return data;
    }

    private Voice voice(int voice) {
        Object v = voices.get(Integer.valueOf(voice));
        return v instanceof Voice ? (Voice) v : null;
    }

    @Override
    public void play(int voice, AudioClip clip, float volume, float pitch, boolean loop) {
        stop(voice);
        if (clip == null || clip.resource == null) {
            return;
        }
        AudioData data = decode(clip);
        if (data == null) {
            fallback.play(voice, clip, volume, pitch, loop);
            return;
        }
        Voice v = new Voice();
        v.data = data;
        v.volume = volume;
        v.pitch = pitch;
        v.loop = loop;
        v.gain = context.createGain();
        v.gain.getGain().setValue(volume);
        v.panner = context.createStereoPanner();
        v.gain.connect(v.panner).connect(master);
        voices.put(Integer.valueOf(voice), v);
        startSource(voice, v);
    }

    private void startSource(final int voice, final Voice v) {
        final AudioBufferSourceNode s = context.createBufferSource();
        s.setBuffer(v.data);
        s.setLoop(v.loop);
        s.getPlaybackRate().setValue(v.pitch);
        s.connect(v.gain);
        s.setOnEnded(new Runnable() {
            @Override
            public void run() {
                // The context reports the end on the EDT, and a port may run
                // the frame elsewhere: the voice table is the frame's, so the
                // release is handed to it.
                UnityRuntime.callInFrame(new Runnable() {
                    @Override
                    public void run() {
                        // Only the source still current ends the voice: one
                        // stopped for a seek or a pause has been replaced.
                        if (v.source == s) { // NOPMD CompareObjectsWithEquals
                            release(voice, v);
                        }
                    }
                });
            }
        });
        v.source = s;
        v.startedAt = context.getCurrentTime();
        double length = v.data.getDuration();
        double at = v.offset;
        if (v.loop && length > 0) {
            at = at % length;
        }
        s.start(0, at);
    }

    /// Where the voice is in its clip now, in seconds.
    private double position(Voice v) {
        if (v.source == null) {
            return v.offset;
        }
        double t = v.offset + (context.getCurrentTime() - v.startedAt) * v.pitch;
        double length = v.data.getDuration();
        if (v.loop && length > 0) {
            t = t % length;
        }
        return t;
    }

    private void stopSource(Voice v) {
        AudioBufferSourceNode s = v.source;
        if (s != null) {
            v.offset = position(v);
            v.source = null;
            s.stop();
            s.disconnect();
        }
    }

    private void release(int voice, Voice v) {
        stopSource(v);
        v.gain.disconnect();
        v.panner.disconnect();
        voices.remove(Integer.valueOf(voice));
    }

    @Override
    public void stop(int voice) {
        Voice v = voice(voice);
        if (v != null) {
            release(voice, v);
        } else {
            fallback.stop(voice);
        }
    }

    @Override
    public void pause(int voice, boolean paused) {
        Voice v = voice(voice);
        if (v == null) {
            fallback.pause(voice, paused);
            return;
        }
        if (v.paused == paused) {
            return;
        }
        v.paused = paused;
        if (paused) {
            stopSource(v);
        } else {
            startSource(voice, v);
        }
    }

    @Override
    public void volume(int voice, float volume) {
        Voice v = voice(voice);
        if (v == null) {
            fallback.volume(voice, volume);
            return;
        }
        v.volume = volume;
        v.gain.getGain().setValue(volume * v.spatial);
    }

    @Override
    public void place(int voice, float gain, float pan) {
        Voice v = voice(voice);
        if (v == null) {
            fallback.place(voice, gain, pan);
            return;
        }
        v.spatial = gain;
        v.gain.getGain().setValue(v.volume * gain);
        v.panner.getPan().setValue(pan);
    }

    @Override
    public void listenerVolume(float volume) {
        listener = volume;
        if (master != null) {
            master.getGain().setValue(volume);
        }
        fallback.listenerVolume(volume);
    }

    /// The source goes on from where it is at the new speed.
    @Override
    public void pitch(int voice, float pitch) {
        Voice v = voice(voice);
        if (v == null) {
            fallback.pitch(voice, pitch);
            return;
        }
        if (v.source != null) {
            v.offset = position(v);
            v.startedAt = context.getCurrentTime();
            v.source.getPlaybackRate().setValue(pitch);
        }
        v.pitch = pitch;
    }

    @Override
    public void seek(int voice, float seconds) {
        Voice v = voice(voice);
        if (v == null) {
            fallback.seek(voice, seconds);
            return;
        }
        boolean running = v.source != null;
        stopSource(v);
        v.offset = seconds;
        if (running) {
            startSource(voice, v);
        }
    }

    @Override
    public void loop(int voice, boolean loop) {
        Voice v = voice(voice);
        if (v == null) {
            fallback.loop(voice, loop);
            return;
        }
        if (v.source != null) {
            v.offset = position(v);
            v.startedAt = context.getCurrentTime();
            v.source.setLoop(loop);
        }
        v.loop = loop;
    }

    @Override
    public void hold(boolean held) {
        if (this.held == held) {
            return;
        }
        this.held = held;
        if (context != null) {
            if (held) {
                context.suspend();
            } else {
                context.resume();
            }
        }
        fallback.hold(held);
    }

    /// Silences everything, for a view that is leaving the screen.
    public void stopAll() {
        ArrayList keys = new ArrayList(voices.keySet());
        for (int i = 0; i < keys.size(); i++) { // NOPMD ForLoopCanBeForeach
            stop(((Integer) keys.get(i)).intValue());
        }
        fallback.stopAll();
    }

    /// Silences everything and closes the context, releasing its device
    /// stream and render thread, for a view that is taken away.
    public void close() {
        stopAll();
        if (context != null) {
            context.close();
            context = null;
        }
    }
}
