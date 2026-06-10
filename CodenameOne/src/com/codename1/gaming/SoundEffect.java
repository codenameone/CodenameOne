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

/// A short, reusable sound clip loaded into a `SoundPool`.
///
/// Obtain one with `SoundPool#load(String)` or `SoundPool#load(java.io.InputStream, String)`,
/// then play it any number of times -- overlapping plays mix together up to the
/// pool's voice limit. A sound effect belongs to the pool that created it and must
/// not be used with a different pool.
public final class SoundEffect {
    private final SoundPool pool;
    private final Object nativeSound;
    private boolean loaded = true;

    SoundEffect(SoundPool pool, Object nativeSound) {
        this.pool = pool;
        this.nativeSound = nativeSound;
    }

    Object getNativeSound() {
        return nativeSound;
    }

    /// The pool that owns this effect.
    public SoundPool getPool() {
        return pool;
    }

    /// True until `#unload()` is called.
    public boolean isLoaded() {
        return loaded;
    }

    /// Plays the effect once at full volume, returning a voice id or -1 if the pool
    /// is exhausted. Equivalent to `pool.play(this)`.
    public int play() {
        return pool.play(this);
    }

    /// Plays the effect with explicit parameters. See
    /// `SoundPool#play(SoundEffect, float, float, float, int)`.
    public int play(float volume, float pan, float rate, int loop) {
        return pool.play(this, volume, pan, rate, loop);
    }

    /// Releases this effect's buffers from the pool. The effect must not be played
    /// afterwards.
    public void unload() {
        if (loaded) {
            loaded = false;
            pool.unload(this);
        }
    }
}
