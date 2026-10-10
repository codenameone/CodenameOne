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
    /// of its own that nothing refers to again. `volume` is 0..1 with the
    /// listener's already multiplied in; `pitch` is a speed, 1 unchanged.
    void play(int voice, AudioClip clip, float volume, float pitch, boolean loop);

    /// Stops a voice, if it is still playing.
    void stop(int voice);

    void pause(int voice, boolean paused);

    void volume(int voice, float volume);

    /// Moves a voice that is playing or paused to a position in its clip,
    /// in seconds from the start: what a script's `source.time = t` asks
    /// for. A voice that has ended is not there to move.
    void seek(int voice, float seconds);
}
