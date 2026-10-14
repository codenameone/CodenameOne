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
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.ui.Display;
import com.codename1.unitycompat.unityengine.AudioClip;
import com.codename1.unitycompat.unityengine.AudioOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/// Makes the sounds the runtime asks for, with Codename One's media API.
///
/// It is the only class that knows there is a device to play on, which is
/// why it sits beside [UnityGameView] and not in the runtime: the runtime
/// keeps its own account of what is playing, so a build with no media --
/// the headless one, and ParparVM's when it is run for a trace -- leaves
/// this class out and behaves the same.
///
/// A clip is a resource of the application, opened by name every time it
/// is played. Each play is a `Media` of its own, released when it ends, so
/// a sound can overlap itself the way a one-shot does in Unity. Two things
/// the media API has no word for are left out: pitch, so a clip plays at
/// its recorded speed whatever the source asks, and seamless looping -- a
/// looped clip is started again when it ends, with whatever gap the
/// platform leaves.
///
/// Pitch is left out for want of anything to ask: a `Media` has a volume
/// and a place in time and no rate. The one playback rate Codename One has
/// is a voice's in `com.codename1.gaming.SoundPool`, and a pool cannot
/// stand in here: it has no way to move a voice within its clip, which
/// `AudioSource.time` needs, nor to change whether a playing voice loops,
/// and only some of its backends say when a voice has ended. The source
/// keeps Unity's account of the time regardless, so only the ear is told
/// something else.
public final class UnityGameAudio implements AudioOutput {
    /// A voice to its `Media`, for those still playing.
    private final HashMap playing = new HashMap();
    private final HashMap looping = new HashMap();
    /// A voice to its own volume now, a `Float`: what it was started with,
    /// then each `volume` since -- `AudioSource.volume` and `mute` both
    /// arrive that way. A looped voice is started again with this, and
    /// never with the volume of its first play: a source turned down or
    /// muted after `Play` would otherwise come back at its old volume at
    /// the next loop, audible while muted. The listener's volume is not in
    /// it; the `Media` is set to the product of the two.
    private final HashMap volumes = new HashMap();
    /// `AudioListener.volume`, which multiplies every voice's.
    private float listener = 1f;
    /// The voices paused by themselves, which a release leaves paused.
    private final HashMap paused = new HashMap();
    /// `AudioListener.pause`: nothing sounds until it is released.
    private boolean held;

    private static String mime(String resource) {
        // By hand and case insensitively: a file name is not prose, and
        // folding it by the device's locale would misread an `I`.
        int n = resource.length();
        if (n > 4 && resource.regionMatches(true, n - 4, ".wav", 0, 4)) {
            return "audio/wav";
        }
        if (n > 4 && resource.regionMatches(true, n - 4, ".ogg", 0, 4)) {
            return "audio/ogg";
        }
        return "audio/mpeg";
    }

    @Override
    public void play(final int voice, final AudioClip clip, final float volume, float pitch, final boolean loop) {
        stop(voice);
        if (clip == null || clip.resource == null) {
            return;
        }
        final Integer key = Integer.valueOf(voice);
        InputStream in = Display.getInstance().getResourceAsStream(getClass(), "/" + clip.resource); // NOPMD CloseResource
        if (in == null) {
            return;
        }
        try {
            Media media = MediaManager.createMedia(in, mime(clip.resource), new Runnable() {
                @Override
                public void run() {
                    ended(key, clip);
                }
            });
            if (media == null) {
                return;
            }
            playing.put(key, media);
            volumes.put(key, Float.valueOf(volume));
            if (loop) {
                looping.put(key, Boolean.TRUE);
            }
            media.setVolume(percent(volume * listener));
            // Started while everything is held, it is ready and waits for
            // the release.
            if (!held) {
                media.play();
            }
        } catch (IOException e) {
            Log.e(e);
        }
    }

    /// A clip that ran to its end: release it, or start it again at the
    /// volume the voice has now. Nothing else of the voice carries over:
    /// a pitch is never applied (see the head of this class), and a loop
    /// starts again at the start of the clip wherever a seek had put it.
    private void ended(Integer key, AudioClip clip) {
        Object level = volumes.get(key);
        paused.remove(key);
        Object media = playing.remove(key);
        if (media instanceof Media) {
            ((Media) media).cleanup();
        }
        if (looping.containsKey(key) && level instanceof Float) {
            play(key.intValue(), clip, ((Float) level).floatValue(), 1f, true);
        }
    }

    private static int percent(float volume) {
        int v = (int) (volume * 100f + 0.5f);
        return v < 0 ? 0 : v > 100 ? 100 : v;
    }

    @Override
    public void stop(int voice) {
        Integer key = Integer.valueOf(voice);
        looping.remove(key);
        paused.remove(key);
        volumes.remove(key);
        Object media = playing.remove(key);
        if (media instanceof Media) {
            ((Media) media).cleanup();
        }
    }

    @Override
    public void pause(int voice, boolean paused) {
        Integer key = Integer.valueOf(voice);
        Object media = playing.get(key);
        if (media instanceof Media) {
            if (paused) {
                this.paused.put(key, Boolean.TRUE);
            } else {
                this.paused.remove(key);
            }
            // Held, the voice is silent already and stays so; what was
            // asked is remembered for the release.
            if (held) {
                return;
            }
            if (paused) {
                ((Media) media).pause();
            } else {
                ((Media) media).play();
            }
        }
    }

    /// Looping is done here, by starting the clip again when it ends, so
    /// changing it on a live voice is changing what its end will do: the
    /// sound itself is not touched. A voice that has already ended on the
    /// device is not brought back.
    @Override
    public void loop(int voice, boolean loop) {
        Integer key = Integer.valueOf(voice);
        if (!playing.containsKey(key)) {
            return;
        }
        if (loop) {
            looping.put(key, Boolean.TRUE);
        } else {
            looping.remove(key);
        }
    }

    @Override
    public void hold(boolean held) {
        if (this.held == held) {
            return;
        }
        this.held = held;
        ArrayList voices = new ArrayList(playing.keySet());
        for (int i = 0; i < voices.size(); i++) { // NOPMD ForLoopCanBeForeach
            Object key = voices.get(i);
            Object media = playing.get(key);
            if (!(media instanceof Media) || paused.containsKey(key)) {
                continue;
            }
            if (held) {
                ((Media) media).pause();
            } else {
                ((Media) media).play();
            }
        }
    }

    @Override
    public void volume(int voice, float volume) {
        Integer key = Integer.valueOf(voice);
        Object media = playing.get(key);
        if (media instanceof Media) {
            volumes.put(key, Float.valueOf(volume));
            ((Media) media).setVolume(percent(volume * listener));
        }
    }

    /// Every voice that is playing, at once, at its own volume times the
    /// new one; those started later take it in [#play].
    @Override
    public void listenerVolume(float volume) {
        listener = volume;
        ArrayList voices = new ArrayList(playing.keySet());
        for (int i = 0; i < voices.size(); i++) { // NOPMD ForLoopCanBeForeach
            Object key = voices.get(i);
            Object media = playing.get(key);
            Object level = volumes.get(key);
            if (media instanceof Media && level instanceof Float) {
                ((Media) media).setVolume(percent(((Float) level).floatValue() * listener));
            }
        }
    }

    /// Nothing to do: see the head of this class. The voice goes on at the
    /// speed it was recorded at.
    @Override
    public void pitch(int voice, float pitch) {
        // `Media` has no playback rate.
    }

    @Override
    public void seek(int voice, float seconds) {
        Object media = playing.get(Integer.valueOf(voice));
        if (media instanceof Media) {
            ((Media) media).setTime((int) (seconds * 1000f + 0.5f));
        }
    }

    /// Silences everything, for a view that is leaving the screen.
    public void stopAll() {
        ArrayList voices = new ArrayList(playing.keySet());
        for (int i = 0; i < voices.size(); i++) { // NOPMD ForLoopCanBeForeach
            stop(((Integer) voices.get(i)).intValue());
        }
    }
}
