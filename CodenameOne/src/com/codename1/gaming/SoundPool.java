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
package com.codename1.gaming;

import com.codename1.media.MediaManager;
import com.codename1.media.SoundPoolPeer;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import java.io.IOException;
import java.io.InputStream;

/// Plays many short, overlapping sound effects with low latency.
///
/// A `SoundPool` is built for game audio: gunshots, coins, footsteps -- sounds that
/// must trigger instantly and play several at once. Load each clip once with
/// `#load(String)` and trigger it repeatedly with `#play(SoundEffect)`; the pool
/// mixes up to `#getMaxStreams()` voices simultaneously and drops the request
/// (returning -1) rather than blocking when that limit is reached.
///
/// ```java
/// SoundPool sfx = SoundPool.create(8);
/// SoundEffect coin = sfx.load("/coin.wav");
/// // ... in the game loop:
/// coin.play();
/// ```
///
/// On platforms with a purpose built low latency audio engine (Android, iOS, the
/// desktop simulator and the browser) the pool uses it directly, supporting per
/// play volume, stereo pan and pitch/rate. Where no native backend exists it falls
/// back to a `com.codename1.media.MediaManager` based pool that still works
/// everywhere but has higher latency and ignores pan and rate --
/// `#isNativeAccelerated()` reports which path is in use.
public class SoundPool {
    private final SoundPoolPeer peer;
    private final boolean nativeAccelerated;
    private final int maxStreams;

    private SoundPool(SoundPoolPeer peer, boolean nativeAccelerated, int maxStreams) {
        this.peer = peer;
        this.nativeAccelerated = nativeAccelerated;
        this.maxStreams = maxStreams;
    }

    /// Creates a sound pool that mixes up to `maxStreams` voices at once.
    public static SoundPool create(int maxStreams) {
        if (maxStreams < 1) {
            maxStreams = 1;
        }
        SoundPoolPeer nativePeer = Display.getInstance().createSoundPool(maxStreams);
        boolean nativeAccel = nativePeer != null;
        SoundPoolPeer peer = nativeAccel ? nativePeer : MediaManager.createFallbackSoundPoolPeer(maxStreams);
        return new SoundPool(peer, nativeAccel, maxStreams);
    }

    /// True if a native low latency audio backend is in use; false if the cross
    /// platform `MediaManager` fallback is in use.
    public boolean isNativeAccelerated() {
        return nativeAccelerated;
    }

    /// The maximum number of voices that can play simultaneously.
    public int getMaxStreams() {
        return maxStreams;
    }

    /// Loads a sound effect from a uri (for example a `/sound.wav` resource path).
    public SoundEffect load(String uri) throws IOException {
        return new SoundEffect(this, peer.loadSound(uri));
    }

    /// Loads a sound effect from a stream of the given mime type. The stream is
    /// fully read and closed.
    public SoundEffect load(InputStream data, String mimeType) throws IOException {
        return new SoundEffect(this, peer.loadSound(data, mimeType));
    }

    /// Loads a sound effect from a uri on a background thread, completing the
    /// returned resource on success or error.
    public AsyncResource<SoundEffect> loadAsync(final String uri) {
        final AsyncResource<SoundEffect> result = new AsyncResource<SoundEffect>();
        new Thread(new Runnable() {
            public void run() {
                try {
                    result.complete(load(uri));
                } catch (Throwable t) {
                    result.error(t);
                }
            }
        }, "SoundPool.loadAsync").start();
        return result;
    }

    /// Plays the effect once at full volume, centered, normal rate. Returns a voice
    /// id, or -1 if no voice was available.
    public int play(SoundEffect effect) {
        return play(effect, 1f, 0f, 1f, 0);
    }

    /// Plays the effect with explicit parameters.
    ///
    /// #### Parameters
    ///
    /// - `effect`: the loaded sound to play
    ///
    /// - `volume`: 0.0 (silent) to 1.0 (full)
    ///
    /// - `pan`: -1.0 (full left) to 1.0 (full right), 0.0 centered (ignored by the
    /// fallback)
    ///
    /// - `rate`: playback rate / pitch, 1.0 normal, typically 0.5 to 2.0 (ignored
    /// by the fallback)
    ///
    /// - `loop`: 0 plays once, -1 loops forever, n repeats n extra times
    ///
    /// #### Returns
    ///
    /// a voice id usable with `#stop(int)` etc., or -1 if the pool is exhausted
    public int play(SoundEffect effect, float volume, float pan, float rate, int loop) {
        return peer.play(effect.getNativeSound(), volume, pan, rate, loop);
    }

    /// Sets the volume (0.0 to 1.0) of a playing voice.
    public void setVolume(int voiceId, float volume) {
        peer.setVolume(voiceId, volume);
    }

    /// Sets the playback rate / pitch of a playing voice (native backends only).
    public void setRate(int voiceId, float rate) {
        peer.setRate(voiceId, rate);
    }

    /// Sets the stereo pan (-1.0 to 1.0) of a playing voice (native backends only).
    public void setPan(int voiceId, float pan) {
        peer.setPan(voiceId, pan);
    }

    /// Pauses a playing voice.
    public void pause(int voiceId) {
        peer.pauseVoice(voiceId);
    }

    /// Resumes a paused voice.
    public void resume(int voiceId) {
        peer.resumeVoice(voiceId);
    }

    /// Stops a voice.
    public void stop(int voiceId) {
        peer.stopVoice(voiceId);
    }

    /// Stops every playing voice.
    public void stopAll() {
        peer.stopAll();
    }

    /// Pauses all playback, for example when the app is backgrounded.
    public void autoPause() {
        peer.autoPause();
    }

    /// Resumes playback paused by `#autoPause()`.
    public void autoResume() {
        peer.autoResume();
    }

    void unload(SoundEffect effect) {
        peer.unloadSound(effect.getNativeSound());
    }

    /// Releases the pool and all loaded effects. The pool must not be used
    /// afterwards.
    public void release() {
        peer.release();
    }
}
