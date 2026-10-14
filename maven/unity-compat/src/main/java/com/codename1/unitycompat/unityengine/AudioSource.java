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
/// the order it happened, and so is every move of the voice within its
/// clip, as `seek clip milliseconds`, every change of `loop` that reaches a
/// playing voice, as `looping clip on` or `off`, and the listener's pause,
/// as `listener paused` and `listener resumed`.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class AudioSource extends Behaviour {
    private static AudioOutput output;
    private static int nextVoice = 1;
    private static final ArrayList log = new ArrayList();
    private static boolean logging;
    static float listenerVolume = 1f;
    /// `AudioListener.pause`, and the two numbers that make it stand still
    /// for every source at once: when it began, and how long the pauses
    /// before it lasted in all. See [#now()].
    private static boolean listenerPaused;
    private static float listenerPausedAt;
    private static float listenerPausedFor;

    AudioClip clip;
    float volume = 1f;
    float pitch = 1f;
    boolean loop;
    boolean mute;
    boolean playOnAwake = true;
    private int voice;
    private boolean playing;
    private boolean paused;
    /// Where in the clip the voice was, in seconds, at `startedAt`.
    private float startedFrom;
    /// When the voice was at `startedFrom`, on the unscaled clock: the
    /// moment it started, or the last time it was moved or changed speed.
    private float startedAt;
    private float pausedAt;
    /// Where the next `Play` starts the clip, in seconds: what `time` was
    /// set to while nothing played. Zero once a `Play` has used it or a
    /// `Stop` has dropped it.
    private float pendingFrom;
    private boolean autoPlayed;

    /// Installs what makes the sound, or null for silence.
    public static void $output(AudioOutput o) {
        output = o;
        // An output that arrives in the middle of a pause -- a view shown
        // again behind a pause menu -- must not sound what starts on it.
        if (o != null && listenerPaused) {
            o.hold(true);
        }
    }

    /// The clock every source counts on: the game's unscaled one, less the
    /// time `AudioListener.pause` was set. While the listener is paused it
    /// does not move, so no clip runs out, `time` stands still and a `Play`
    /// issued meanwhile has played nothing when the pause ends; afterwards
    /// every voice is where it was. It is the account a source's own
    /// `Pause` keeps in `pausedAt`, kept once for all of them, and since
    /// `pausedAt` is read off this clock the two pauses add up by
    /// themselves.
    private static float now() {
        return (listenerPaused ? listenerPausedAt : Time.unscaledTime) - listenerPausedFor;
    }

    static boolean listenerPaused() {
        return listenerPaused;
    }

    /// `AudioListener.pause`. Unity pauses everything that is heard, the
    /// one-shots too, which no source refers to once they have started; so
    /// the sound is held by the output as a whole, which knows its voices,
    /// and not voice by voice from here. The sources themselves need no
    /// visit: they read [#now()].
    static void listenerPause(boolean value) {
        if (value == listenerPaused) {
            return;
        }
        if (value) {
            listenerPausedAt = Time.unscaledTime;
        } else {
            listenerPausedFor += Time.unscaledTime - listenerPausedAt;
        }
        listenerPaused = value;
        if (logging) {
            log.add(value ? "listener paused" : "listener resumed");
        }
        if (output != null) {
            output.hold(value);
        }
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
        // The clock starts again with the game's, and an output the
        // project before left held is released for the next one.
        if (listenerPaused && output != null) {
            output.hold(false);
        }
        listenerPaused = false;
        listenerPausedAt = 0f;
        listenerPausedFor = 0f;
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

    /// A change of speed counts from now: what has played so far played at
    /// the old one. On a voice that is sounding it goes to the output as
    /// well, as the volume does; it used to change this account alone, and
    /// an output able to play faster was never asked to.
    ///
    /// What a script reads is Unity's answer whatever the output can do:
    /// `time` and `isPlaying` follow the pitch on every target and with no
    /// sound at all, which is what lets one trace hold all of them, and a
    /// game that waits for a clip waits as long as it would in Unity. The
    /// price is paid where the device cannot change a sound's speed --
    /// [com.codename1.unitycompat.unityengine.ui.UnityGameAudio] says why
    /// Codename One's media cannot -- and the sound is then heard at the
    /// speed it was recorded at: above a pitch of one it is still sounding
    /// when `isPlaying` turns false, below one it has ended while `time`
    /// goes on. Counting at the speed heard instead would have a game run
    /// differently where there is sound and where there is none. The
    /// scene compiler says so for a source whose scene gives it a pitch.
    public void set_pitch(float value) {
        if (playing && clip != null) {
            startedFrom = position();
            startedAt = paused ? pausedAt : now();
        }
        pitch = value;
        if (playing && output != null) {
            output.pitch(voice, value);
        }
    }

    public boolean get_loop() {
        return loop;
    }

    /// On a playing voice the change has to reach two accounts, or they
    /// part: turned on, the device let the clip end while `isPlaying` said
    /// true for ever; turned off, the device went on repeating a clip the
    /// script had been told was over.
    ///
    /// The account kept here is closed under the old rule first and
    /// reopened at the place in the clip the voice has reached. Without
    /// that, a clip on its third round that stops looping would be three
    /// lengths past its end and over at once, where Unity plays the round
    /// it is in to its end. The output is told to change the voice it
    /// has, not to start another, so nothing is heard to restart.
    public void set_loop(boolean value) {
        if (value == loop) {
            return;
        }
        if (playing && clip != null) {
            expire();
            if (playing) {
                startedFrom = get_time();
                startedAt = paused ? pausedAt : now();
                loop = value;
                if (logging) {
                    log.add("looping " + clip.name + (value ? " on" : " off"));
                }
                if (output != null) {
                    output.loop(voice, value);
                }
                return;
            }
        }
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
        expire();
        return playing;
    }

    /// Ends a voice that does not loop and has reached the end of its
    /// clip, as of now or of the moment it was paused.
    private void expire() {
        if (!playing || loop) {
            return;
        }
        // What is left of the clip from where the voice last started, was
        // moved to or changed speed, at the speed it has now.
        float speed = pitch < 0f ? -pitch : pitch;
        if (clip == null || !((paused ? pausedAt : now()) - startedAt // NOPMD LogicInversion
                < (clip.get_length() - startedFrom) / (speed > 0f ? speed : 1f))) {
            playing = false;
        }
    }

    /// Where the voice is in its clip, in seconds, before it is wrapped
    /// for a loop or held to the clip: the place it was last known to be,
    /// and the time since then at the speed the pitch sets.
    private float position() {
        return startedFrom + ((paused ? pausedAt : now()) - startedAt) * pitch;
    }

    public float get_time() {
        if (!playing || clip == null) {
            return 0f;
        }
        float at = position();
        float length = clip.get_length();
        if (loop && length > 0f) {
            at = at - length * (float) Math.floor(at / length);
        }
        return at < 0f ? 0f : at > length ? length : at;
    }

    /// Moves the voice to a place in its clip, in seconds. Two things
    /// move together: the account `time` and `isPlaying` are worked out
    /// from, which restarts from the new place so that the pitch applies to
    /// the time after it and not to the place itself, and the sound, which
    /// the output is asked to seek. Before this the device kept playing
    /// from where it was while the script was told otherwise.
    ///
    /// On a source that is not playing there is no voice to move, and the
    /// place is kept for the next `Play` to start from: "Setting this
    /// property while the audio source is not playing will update the
    /// playback position, but it will only take effect once playback
    /// begins", as Unity's page for `time` has it, and until then `time`
    /// reads zero as that page says it does. It is held to the clip when it
    /// is used and not here, since a script may set the clip afterwards. A
    /// voice that has run out counts as not playing, which [#expire] finds
    /// out first.
    public void set_time(float value) {
        expire();
        if (!playing || clip == null) {
            pendingFrom = value > 0f ? value : 0f; // a NaN is not greater than zero either
            return;
        }
        float length = clip.get_length();
        float at = value < 0f || value != value ? 0f : value > length ? length : value; // NOPMD ComparisonWithNaN
        startedFrom = at;
        startedAt = paused ? pausedAt : now();
        if (logging) {
            log.add("seek " + clip.name + " " + (int) (at * 1000f + 0.5f));
        }
        if (output != null) {
            output.seek(voice, at);
        }
    }

    private static int newVoice() {
        return nextVoice++;
    }

    public void Play() {
        if (clip == null || !live()) {
            return;
        }
        // The voice before this one is stopped whether or not the account
        // has it over: at a pitch above one the account ends the clip
        // before an output that cannot play faster has, and a second
        // `Play` then sounded over the first, on a source that has one
        // voice. Stopping a voice that has ended is nothing.
        if (voice != 0 && output != null) {
            output.stop(voice);
        }
        // From the place `time` was set to while the source was silent, if
        // it was; a `Play` over a voice that is sounding has no such place
        // and restarts the clip. The account and the sound start together,
        // so `time` read on the next line answers that place.
        float length = clip.get_length();
        float from = pendingFrom > length ? length : pendingFrom;
        pendingFrom = 0f;
        voice = newVoice();
        playing = true;
        paused = false;
        startedFrom = from;
        startedAt = now();
        record(loop ? "loop" : "play", clip);
        if (output != null) {
            output.play(voice, clip, level(1f), pitch, loop);
        }
        if (from > 0f) {
            if (logging) {
                log.add("seek " + clip.name + " " + (int) (from * 1000f + 0.5f));
            }
            if (output != null) {
                output.seek(voice, from);
            }
        }
    }

    /// The delay is not kept: the sound starts now. It is a deviation a
    /// fraction of a second long, and said here so that it is known. A
    /// place set with `time` beforehand is where it starts, as for `Play`.
    public void PlayDelayed(float delay) {
        Play();
    }

    /// "The Audio clip plays from the beginning the next time you play it",
    /// so a place set with `time` and not yet used is dropped as well.
    public void Stop() {
        // As in `Play`: a voice the account has over may still be heard.
        if (voice != 0 && output != null) {
            output.stop(voice);
        }
        playing = false;
        paused = false;
        pendingFrom = 0f;
    }

    public void Pause() {
        if (playing && !paused) {
            paused = true;
            pausedAt = now();
            if (output != null) {
                output.pause(voice, true);
            }
        }
    }

    public void UnPause() {
        if (playing && paused) {
            paused = false;
            startedAt += now() - pausedAt;
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
