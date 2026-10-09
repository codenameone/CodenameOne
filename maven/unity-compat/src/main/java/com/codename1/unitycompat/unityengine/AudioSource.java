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
package com.codename1.unitycompat.unityengine;

import UnityEngine.Vector3;
import java.util.ArrayList;

/// `UnityEngine.AudioSource`: plays an [AudioClip].
///
/// A source has one voice of its own, which `Play` restarts and `Stop`
/// silences, and any number of one-shots that play to their end. Whether
/// the voice is still playing is worked out from the clip's length and the
/// game's clock, not asked of the device, so that a script that waits for
/// a sound to finish behaves the same on every target, with or without
/// sound. Everything is 2D: there is no distance attenuation or panning.
///
/// Every start is also recorded in a log a test or a trace can read, in
/// the order it happened.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class AudioSource extends Behaviour {
    private static AudioOutput output;
    private static int nextVoice = 1;
    private static final ArrayList log = new ArrayList();
    private static boolean logging;
    static float listenerVolume = 1f;

    AudioClip clip;
    float volume = 1f;
    float pitch = 1f;
    boolean loop;
    boolean mute;
    boolean playOnAwake = true;
    private int voice;
    private boolean playing;
    private boolean paused;
    /// When the voice started, on the unscaled clock.
    private float startedAt;
    private float pausedAt;
    private boolean autoPlayed;

    /// Installs what makes the sound, or null for silence.
    public static void $output(AudioOutput o) {
        output = o;
    }

    /// Starts or stops the recording of plays, and forgets those recorded.
    public static void $record(boolean on) {
        logging = on;
        log.clear();
    }

    /// The plays recorded since the last call, each as `kind clip`, and
    /// forgets them.
    public static String[] $drainLog() {
        String[] out = new String[log.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (String) log.get(i);
        }
        log.clear();
        return out;
    }

    static void resetAll() {
        log.clear();
        listenerVolume = 1f;
    }

    /// What a scene file sets.
    public void $setup(AudioClip clip, float volume, float pitch, boolean loop, boolean mute, boolean playOnAwake) {
        this.clip = clip;
        this.volume = volume;
        this.pitch = pitch;
        this.loop = loop;
        this.mute = mute;
        this.playOnAwake = playOnAwake;
    }

    @Override
    public Component $new() {
        return new AudioSource();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        AudioSource s = (AudioSource) source;
        $setup(s.clip, s.volume, s.pitch, s.loop, s.mute, s.playOnAwake);
    }

    private float level(float scale) {
        return mute ? 0f : volume * scale * listenerVolume;
    }

    private static void record(String kind, AudioClip c) {
        if (logging) {
            log.add(kind + " " + c.name);
        }
    }

    /// Play On Awake: the source starts when it first becomes live.
    @Override
    void registered() {
        autoPlay();
    }

    @Override
    void activeChanged(boolean nowLive) {
        if (nowLive) {
            autoPlay();
        } else {
            Stop();
        }
    }

    private void autoPlay() {
        if (!autoPlayed && playOnAwake && clip != null && live()) {
            autoPlayed = true;
            Play();
        }
    }

    @Override
    void unregistered() {
        Stop();
    }

    public AudioClip get_clip() {
        return clip;
    }

    public void set_clip(AudioClip value) {
        clip = value;
    }

    public float get_volume() {
        return volume;
    }

    public void set_volume(float value) {
        volume = value < 0f ? 0f : value > 1f ? 1f : value;
        if (playing && output != null) {
            output.volume(voice, level(1f));
        }
    }

    public float get_pitch() {
        return pitch;
    }

    public void set_pitch(float value) {
        pitch = value;
    }

    public boolean get_loop() {
        return loop;
    }

    public void set_loop(boolean value) {
        loop = value;
    }

    public boolean get_mute() {
        return mute;
    }

    public void set_mute(boolean value) {
        mute = value;
        if (playing && output != null) {
            output.volume(voice, level(1f));
        }
    }

    public boolean get_playOnAwake() {
        return playOnAwake;
    }

    public void set_playOnAwake(boolean value) {
        playOnAwake = value;
    }

    /// True while the source's own voice has not reached the end of its
    /// clip. A clip whose length is unknown counts as finished at once
    /// unless it loops.
    public boolean get_isPlaying() {
        if (!playing || paused) {
            return false;
        }
        if (loop) {
            return true;
        }
        float speed = pitch < 0f ? -pitch : pitch;
        if (clip == null || !(Time.unscaledTime - startedAt < clip.get_length() / (speed > 0f ? speed : 1f))) { // NOPMD LogicInversion
            playing = false;
        }
        return playing;
    }

    public float get_time() {
        if (!playing || clip == null) {
            return 0f;
        }
        float at = ((paused ? pausedAt : Time.unscaledTime) - startedAt) * pitch;
        float length = clip.get_length();
        if (loop && length > 0f) {
            at = at - length * (float) Math.floor(at / length);
        }
        return at < 0f ? 0f : at > length ? length : at;
    }

    public void set_time(float value) {
        startedAt = Time.unscaledTime - value;
    }

    private static int newVoice() {
        return nextVoice++;
    }

    public void Play() {
        if (clip == null || !live()) {
            return;
        }
        if (playing && output != null) {
            output.stop(voice);
        }
        voice = newVoice();
        playing = true;
        paused = false;
        startedAt = Time.unscaledTime;
        record(loop ? "loop" : "play", clip);
        if (output != null) {
            output.play(voice, clip, level(1f), pitch, loop);
        }
    }

    /// The delay is not kept: the sound starts now. It is a deviation a
    /// fraction of a second long, and said here so that it is known.
    public void PlayDelayed(float delay) {
        Play();
    }

    public void Stop() {
        if (playing && output != null) {
            output.stop(voice);
        }
        playing = false;
        paused = false;
    }

    public void Pause() {
        if (playing && !paused) {
            paused = true;
            pausedAt = Time.unscaledTime;
            if (output != null) {
                output.pause(voice, true);
            }
        }
    }

    public void UnPause() {
        if (playing && paused) {
            paused = false;
            startedAt += Time.unscaledTime - pausedAt;
            if (output != null) {
                output.pause(voice, false);
            }
        }
    }

    public void PlayOneShot(AudioClip c) {
        PlayOneShot(c, 1f);
    }

    public void PlayOneShot(AudioClip c, float volumeScale) {
        if (c == null || !live()) {
            return;
        }
        record("shot", c);
        if (output != null) {
            output.play(newVoice(), c, level(volumeScale), pitch, false);
        }
    }

    public static void PlayClipAtPoint(AudioClip c, Vector3 position) {
        PlayClipAtPoint(c, position, 1f);
    }

    public static void PlayClipAtPoint(AudioClip c, Vector3 position, float volume) {
        if (c == null) {
            return;
        }
        record("shot", c);
        if (output != null) {
            output.play(newVoice(), c, volume * listenerVolume, 1f, false);
        }
    }
}
