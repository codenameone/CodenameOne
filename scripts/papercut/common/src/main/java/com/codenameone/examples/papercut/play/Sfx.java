/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.papercut.play;

import com.codename1.gaming.SoundEffect;
import com.codename1.gaming.SoundPool;
import java.io.InputStream;

/// The game's sounds: paper, plops and splashes, CC0 recordings converted to
/// short mono WAVs by `tools/make-art.py`, mixed by a [SoundPool].
public final class Sfx {
    public static final int SHOT = 0;
    public static final int POP = 1;
    public static final int POP2 = 2;
    public static final int SPLASH = 3;
    public static final int TEAR = 4;
    public static final int HIT = 5;
    public static final int BOOM = 6;
    public static final int BELL = 7;
    public static final int SPRING = 8;
    public static final int FLIP = 9;
    public static final int FLIP2 = 10;
    private static final String[] FILES = {"sfx_shot", "sfx_pop", "sfx_pop2", "sfx_splash", "sfx_tear",
        "sfx_hit", "sfx_boom", "sfx_bell", "sfx_spring", "sfx_flip", "sfx_flip2"};
    /// The shortest gap between two plays of one sound, in milliseconds: a
    /// pencil firing eleven times a second must not drown everything else.
    private static final int[] GAP = {70, 40, 40, 60, 80, 50, 120, 200, 120, 60, 60};

    private static SoundPool pool;
    private static final SoundEffect[] EFFECTS = new SoundEffect[FILES.length];
    private static final long[] LAST = new long[FILES.length];
    private static boolean muted;

    private Sfx() {
    }

    public static void setMuted(boolean m) {
        muted = m;
    }

    public static boolean isMuted() {
        return muted;
    }

    /// Loads the clips; quietly does nothing where audio is unavailable.
    ///
    /// Only a native mixer is used. Without one a pool falls back to a media
    /// player per voice, which a game firing ten strokes a second would drown
    /// in, so the game stays silent instead (a CI runner with no audio device).
    public static void load() {
        if (pool != null) {
            return;
        }
        try {
            SoundPool created = SoundPool.create(10);
            if (!created.isNativeAccelerated()) {
                return;
            }
            for (int i = 0; i < FILES.length; i++) {
                InputStream in = Sfx.class.getResourceAsStream("/" + FILES[i] + ".wav");
                if (in != null) {
                    EFFECTS[i] = created.load(in, "audio/wav");
                }
            }
            pool = created;
        } catch (Throwable unavailable) {
            pool = null;
        }
    }

    public static void play(int which) {
        play(which, 1f, 1f);
    }

    /// Plays a sound at `volume`, its pitch scaled by `rate`.
    public static void play(int which, float volume, float rate) {
        if (muted || pool == null || EFFECTS[which] == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - LAST[which] < GAP[which]) {
            return;
        }
        LAST[which] = now;
        try {
            pool.play(EFFECTS[which], volume, 0f, rate, 0);
        } catch (Throwable ignored) {
            // Sound is decoration; a failure must never stop a run.
        }
    }
}
