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
package com.codename1.media;

import java.io.IOException;
import java.io.InputStream;

/// Low level service provider interface backing `com.codename1.gaming.SoundPool`.
///
/// A peer owns a fixed number of simultaneous playback "voices" and a set of loaded
/// sounds. Each platform port provides its own peer over the platform's purpose
/// built low latency audio API (Android `SoundPool`, iOS `AVAudioEngine`, the
/// desktop `javax.sound.sampled` mixer, WebAudio in the browser). When a port does
/// not provide one, `com.codename1.gaming.SoundPool` falls back to
/// `MediaSoundPoolPeer`, which is implemented purely on top of the existing
/// `MediaManager`.
///
/// A loaded sound is represented by an opaque `Object` handle returned from
/// `#loadSound(String)` / `#loadSound(InputStream, String)`. A playing voice is
/// represented by an `int` id returned from
/// `#play(Object, float, float, float, int)`; `-1` means no voice was available.
/// Per voice operations are no-ops if the voice has already finished and been
/// recycled.
///
/// Callbacks from the underlying audio engine may arrive off the Codename One EDT;
/// implementations must keep their own bookkeeping thread safe.
public interface SoundPoolPeer {
    /// Loads a short sound from a stream, decoding/buffering it up front so that
    /// playback latency is paid here rather than at `#play`. The stream is fully
    /// consumed and closed.
    Object loadSound(InputStream data, String mimeType) throws IOException;

    /// Loads a short sound from a uri (for example a `jar://` resource path).
    Object loadSound(String uri) throws IOException;

    /// Plays a loaded sound, returning a voice id or `-1` if the pool is exhausted.
    ///
    /// #### Parameters
    ///
    /// - `sound`: a handle returned from one of the load methods
    ///
    /// - `volume`: 0.0 (silent) to 1.0 (full)
    ///
    /// - `pan`: -1.0 (full left) to 1.0 (full right), 0.0 centered
    ///
    /// - `rate`: playback rate / pitch, 1.0 is normal (typically 0.5 to 2.0)
    ///
    /// - `loop`: 0 plays once, -1 loops forever, n repeats n extra times
    int play(Object sound, float volume, float pan, float rate, int loop);

    void setVolume(int voiceId, float volume);

    void setRate(int voiceId, float rate);

    void setPan(int voiceId, float pan);

    void pauseVoice(int voiceId);

    void resumeVoice(int voiceId);

    void stopVoice(int voiceId);

    /// Stops every currently playing voice.
    void stopAll();

    /// Pauses all active playback (for example when the app is sent to the
    /// background).
    void autoPause();

    /// Resumes playback paused by `#autoPause()`.
    void autoResume();

    /// Releases a single loaded sound and its buffers.
    void unloadSound(Object sound);

    /// Releases the whole pool and all loaded sounds.
    void release();
}
