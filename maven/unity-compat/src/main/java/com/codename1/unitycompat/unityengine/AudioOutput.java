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

/// Where sound goes. The runtime decides *what* plays and when; what makes
/// a noise is whoever hosts it, behind this interface, so that the runtime
/// itself names no media API and runs where there is none.
///
/// With no output installed nothing is heard, and [AudioSource] keeps its
/// own account of what would be playing so that scripts see the same
/// answers either way.
public interface AudioOutput {
    /// Starts a clip. `voice` identifies the playback: a source's own
    /// voice is replaced by its next `Play`, and a one-shot gets a voice
    /// of its own, which its source changes only the volume of. `volume` is 0..1, the
    /// source's own -- its volume, a one-shot's scale, zero when muted --
    /// and the output multiplies the listener's in (see
    /// [#listenerVolume(float)]); `pitch` is a speed, 1 unchanged.
    void play(int voice, AudioClip clip, float volume, float pitch, boolean loop);

    /// Stops a voice, if it is still playing.
    void stop(int voice);

    void pause(int voice, boolean paused);

    /// A voice's own volume changed, as in [#play]: the listener's is
    /// still to be multiplied in.
    void volume(int voice, float volume);

    /// `AudioListener.volume`: every voice sounds at its own volume times
    /// this, from now on -- those already playing, the one-shots among
    /// them, at once, and every one started later. Unity's listener volume
    /// is "the game sound volume", and a slider in a settings menu that
    /// moved only the next sound would not be one. It is kept here and not
    /// multiplied in by the sources because a one-shot has no source that
    /// knows it is still playing; the output does.
    void listenerVolume(float volume);

    /// Changes the speed of a voice that is playing or paused, from where
    /// it is: `source.pitch = value` on a source that is heard. A pitch
    /// set beforehand arrives with [#play]. An output that cannot change
    /// the speed of a sound leaves the voice as it is; the source goes on
    /// counting at the pitch either way, as Unity would.
    void pitch(int voice, float pitch);

    /// Moves a voice that is playing or paused to a position in its clip,
    /// in seconds from the start: what a script's `source.time = t` asks
    /// for. A voice that has ended is not there to move.
    void seek(int voice, float seconds);

    /// Makes a voice that is playing or paused repeat its clip from now
    /// on, or play the round it is in to the end and stop. The voice goes
    /// on from where it is: this is `source.loop = value` on a source
    /// that is heard, and a restart would be heard too.
    void loop(int voice, boolean loop);

    /// `AudioListener.pause`. While held nothing sounds: every voice
    /// stops where it is, one that is started or unpaused meanwhile
    /// waits, and the release lets each go on from its place -- except
    /// those paused by themselves with [#pause(int, boolean)], which stay
    /// paused until that is undone.
    void hold(boolean held);
}
