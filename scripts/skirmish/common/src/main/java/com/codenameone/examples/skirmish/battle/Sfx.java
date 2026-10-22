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
package com.codenameone.examples.skirmish.battle;

import com.codename1.gaming.SoundEffect;
import com.codename1.gaming.SoundPool;
import java.io.ByteArrayInputStream;

/// The battle's sound effects, synthesized at start-up into short WAV clips
/// and played through a [SoundPool], which mixes overlapping voices with low
/// latency where the platform has a native mixer.
///
/// Swap [#tone] for `pool.load("/hit.wav")` to use recorded sounds.
public final class Sfx {
    private static SoundPool pool;
    private static SoundEffect deploy;
    private static SoundEffect hit;
    private static SoundEffect boom;
    private static boolean muted;
    private static long lastHit;

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
    /// Only a native mixer is used. Without one, a pool falls back to a media
    /// player per voice -- in the simulator a browser engine per sound -- which
    /// a battle firing a hit every 60 ms would drown in, so the game stays
    /// silent instead (a simulator or CI runner with no audio device).
    public static void load() {
        if (pool != null) {
            return;
        }
        try {
            SoundPool created = SoundPool.create(6);
            if (!created.isNativeAccelerated()) {
                return;
            }
            pool = created;
            deploy = pool.load(new ByteArrayInputStream(tone(660, 990, 90, 0.35)), "audio/wav");
            hit = pool.load(new ByteArrayInputStream(tone(220, 140, 45, 0.25)), "audio/wav");
            boom = pool.load(new ByteArrayInputStream(tone(110, 40, 380, 0.6)), "audio/wav");
        } catch (Throwable unavailable) {
            pool = null;
        }
    }

    public static void deploy() {
        play(deploy);
    }

    /// Hits are many; one every 60 ms is enough to hear the fight.
    public static void hit() {
        long now = System.currentTimeMillis();
        if (now - lastHit > 60) {
            lastHit = now;
            play(hit);
        }
    }

    public static void boom() {
        play(boom);
    }

    private static void play(SoundEffect e) {
        if (!muted && pool != null && e != null) {
            try {
                pool.play(e);
            } catch (Throwable ignored) {
                // Sound is decoration; a failure must never stop a battle.
            }
        }
    }

    /// A mono 16-bit 22 kHz WAV of a square-ish tone sliding from `fromHz`
    /// to `toHz` over `millis`, fading out.
    static byte[] tone(int fromHz, int toHz, int millis, double volume) {
        int rate = 22050;
        int n = rate * millis / 1000;
        byte[] out = new byte[44 + n * 2];
        header(out, n * 2, rate);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double hz = fromHz + (toHz - fromHz) * t;
            phase += hz / rate;
            double wave = (phase - Math.floor(phase)) < 0.5 ? 1 : -1;
            double sample = wave * volume * (1 - t) * 32767 * 0.5;
            int s = (int) sample;
            out[44 + i * 2] = (byte) (s & 0xff);
            out[45 + i * 2] = (byte) ((s >> 8) & 0xff);
        }
        return out;
    }

    private static void header(byte[] b, int dataLen, int rate) {
        put(b, 0, "RIFF");
        le32(b, 4, 36 + dataLen);
        put(b, 8, "WAVE");
        put(b, 12, "fmt ");
        le32(b, 16, 16);
        le16(b, 20, 1);
        le16(b, 22, 1);
        le32(b, 24, rate);
        le32(b, 28, rate * 2);
        le16(b, 32, 2);
        le16(b, 34, 16);
        put(b, 36, "data");
        le32(b, 40, dataLen);
    }

    private static void put(byte[] b, int at, String s) {
        for (int i = 0; i < s.length(); i++) {
            b[at + i] = (byte) s.charAt(i);
        }
    }

    private static void le32(byte[] b, int at, int v) {
        le16(b, at, v & 0xffff);
        le16(b, at + 2, (v >> 16) & 0xffff);
    }

    private static void le16(byte[] b, int at, int v) {
        b[at] = (byte) (v & 0xff);
        b[at + 1] = (byte) ((v >> 8) & 0xff);
    }
}
