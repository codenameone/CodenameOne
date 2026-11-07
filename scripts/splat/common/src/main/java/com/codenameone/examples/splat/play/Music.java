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
package com.codenameone.examples.splat.play;

import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.ui.CN;
import java.io.ByteArrayInputStream;

/// The menu theme and the sound of the field, both rendered by [Synth] and
/// looped through a [Media] player: a long clip is a media player's job on
/// every platform, where a sound pool is for short effects.
///
/// The theme is a march that takes itself far too seriously, which is the
/// game's whole joke. On the field there is no music, only wind, birds and the
/// pop of markers on the next field over.
///
/// Both are built on a background thread the first time they are wanted, so a
/// slow phone starts no later for them.
public final class Music {
    private static final int MENU = 1;
    private static final int FIELD = 2;
    private static final int MENU_VOLUME = 45;
    private static final int FIELD_VOLUME = 70;

    private static byte[] menu;
    private static byte[] field;
    private static boolean building;
    private static boolean muted;
    private static int wanted;
    private static int playing;
    private static Media media;

    private Music() {
    }

    public static void setMuted(boolean m) {
        muted = m;
        if (m) {
            halt();
        } else {
            apply();
        }
    }

    public static boolean isMuted() {
        return muted;
    }

    /// The theme, for the screens around a match.
    public static void menu() {
        want(MENU);
    }

    /// The field's ambience, for a match.
    public static void field() {
        want(FIELD);
    }

    /// Silence, for a stinger that should be heard alone.
    public static void stop() {
        want(0);
    }

    private static void want(int what) {
        wanted = what;
        apply();
    }

    private static void apply() {
        if (muted || wanted == playing && media != null) {
            return;
        }
        halt();
        if (wanted == 0) {
            return;
        }
        if (menu == null || field == null) {
            build();
            return;
        }
        final int what = wanted;
        final byte[] wav = what == MENU ? menu : field;
        try {
            final Media[] self = new Media[1];
            self[0] = MediaManager.createMedia(new ByteArrayInputStream(wav), "audio/wav", () -> CN.callSerially(() -> {
                // At the end, go round again: the clips are written to loop.
                // halt() releases the finished player before the next starts.
                if (media == self[0] && playing == what) {
                    playing = 0;
                    apply();
                }
            }));
            media = self[0];
            playing = what;
            media.setVolume(what == MENU ? MENU_VOLUME : FIELD_VOLUME);
            media.play();
        } catch (Throwable unavailable) {
            // No media player (a CI runner without audio): stay quiet.
            media = null;
            playing = 0;
        }
    }

    private static void halt() {
        Media m = media;
        media = null;
        playing = 0;
        if (m != null) {
            try {
                m.cleanup();
            } catch (Throwable ignored) {
                // already gone
            }
        }
    }

    private static void build() {
        if (building) {
            return;
        }
        building = true;
        new Thread(() -> {
            byte[] m = null;
            byte[] f = null;
            try {
                m = theme().wav();
                f = ambience().wav();
            } catch (Throwable outOfMemory) {
                // Leave both null: the next screen that wants music tries again.
            }
            final byte[] menuWav = m;
            final byte[] fieldWav = f;
            CN.callSerially(() -> {
                building = false;
                if (menuWav != null && fieldWav != null) {
                    menu = menuWav;
                    field = fieldWav;
                    apply();
                }
            });
        }, "splat-music").start();
    }

    // ------------------------------------------------------------ the theme

    private static final double BEAT = 60.0 / 104;
    private static final int BARS = 8;
    private static final double TAIL = 0.6;

    /// The bass root of each bar, in semitones from A 440.
    private static final int[] ROOTS = {-31, -31, -35, -33, -31, -31, -35, -36};

    /// Each bar's chord, as three notes in semitones from A 440.
    private static final int[][] CHORDS = {
        {-7, -4, 0}, {-7, -4, 0}, {-11, -7, -4}, {-9, -5, -2},
        {-7, -4, 0}, {-7, -4, 0}, {-11, -7, -4}, {-12, -8, -5},
    };

    /// The tune of the second half: start beat, length in beats, note.
    private static final double[][] TUNE = {
        {16, 1, 0}, {17, 1, 5}, {18, 0.5, 7}, {18.5, 0.5, 8}, {19, 1, 7},
        {20, 1.5, 5}, {21.5, 0.5, 3}, {22, 2, 5},
        {24, 1, 1}, {25, 1, 5}, {26, 0.5, 8}, {26.5, 0.5, 10}, {27, 1, 8},
        {28, 1, 7}, {29, 1, 4}, {30, 2, 7},
    };

    /// "Operation Weekend": eight bars of a very serious march.
    static Synth theme() {
        Synth s = new Synth(BARS * 4 * BEAT + TAIL, 104);
        for (int bar = 0; bar < BARS; bar++) {
            double b0 = bar * 4 * BEAT;
            // the drums: kick on one and three, snare on two and four, hats in eighths
            for (int beat = 0; beat < 4; beat++) {
                double t = b0 + beat * BEAT;
                if (beat % 2 == 0) {
                    s.tone(t, 0.14, 110, 45, Synth.SINE, 0.5, 0.002, 0.045);
                } else {
                    snare(s, t, 0.2);
                }
                s.noise(t, 0.03, 0.05, 11000, 11000, 6000, 0, 0.01);
                s.noise(t + BEAT / 2, 0.03, 0.035, 11000, 11000, 6000, 0, 0.01);
            }
            if (bar % 4 == 3) {
                // the march roll into the next phrase
                for (int i = 0; i < 8; i++) {
                    snare(s, b0 + 3 * BEAT + i * BEAT / 8, 0.05 + i * 0.012);
                }
            }
            // the bass, in eighths between the root and its octave
            double root = Synth.note(ROOTS[bar]);
            for (int i = 0; i < 8; i++) {
                double f = i % 2 == 0 ? root : root * 2;
                s.tone(b0 + i * BEAT / 2, BEAT / 2, f, f, Synth.TRIANGLE, 0.22, 0.004, 0.14);
                s.tone(b0 + i * BEAT / 2, BEAT / 2, f, f, Synth.SQUARE, 0.05, 0.004, 0.1, 0, 0, 900);
            }
            // a brass stab on the downbeat, and again on three
            for (int n : CHORDS[bar]) {
                brass(s, b0, 0.42, Synth.note(n), 0.055);
                brass(s, b0 + 2 * BEAT, 0.25, Synth.note(n), 0.04);
            }
            if (bar < 4) {
                // the first half: plucked chord tones, a step up each eighth
                for (int i = 0; i < 8; i++) {
                    double f = Synth.note(CHORDS[bar][i % 3] + 12);
                    s.tone(b0 + i * BEAT / 2, 0.16, f, f, Synth.TRIANGLE, 0.07, 0.002, 0.05);
                }
            }
        }
        // the second half: the tune
        for (double[] n : TUNE) {
            double f = Synth.note((int) n[2] + 12);
            double len = n[1] * BEAT;
            s.tone(n[0] * BEAT, len * 0.95, f, f, Synth.SQUARE, 0.07, 0.01, len, 5, len > BEAT ? 0.006 : 0, 2200);
            s.tone(n[0] * BEAT, len * 0.95, f / 2, f / 2, Synth.SAW, 0.05, 0.01, len, 5, 0, 1500);
        }
        return s.loop(TAIL);
    }

    private static void snare(Synth s, double t, double amp) {
        s.noise(t, 0.16, amp, 7000, 4000, 1200, 0.001, 0.05);
        s.tone(t, 0.08, 200, 160, Synth.TRIANGLE, amp * 0.8, 0.001, 0.03);
    }

    private static void brass(Synth s, double at, double len, double f, double amp) {
        for (int d = -1; d <= 1; d++) {
            double g = f * (1 + d * 0.005);
            s.tone(at, len, g, g, Synth.SAW, amp, 0.015, len * 0.6, 5, 0.003, 2400);
        }
    }

    // ------------------------------------------------------------ the field

    private static final double FIELD_SECONDS = 14;

    /// Wind over an empty lot, birds, and somebody else's match next door.
    static Synth ambience() {
        Synth s = new Synth(FIELD_SECONDS + 1.5, 7);
        // wind: long gusts that overlap, each a slow sweep of filtered noise
        for (double t = -1; t < FIELD_SECONDS + 1.5; t += 1.8) {
            double from = 220 + s.random() * 380;
            double to = 220 + s.random() * 380;
            s.noise(Math.max(0, t), 3.4, 0.2 + s.random() * 0.1, from, to, 60, 1.2, 1.4);
        }
        // birds: a few chirps in twos and threes
        for (int i = 0; i < 7; i++) {
            double at = s.random() * FIELD_SECONDS;
            double base = 2400 + s.random() * 1200;
            int chirps = 2 + (int) (s.random() * 2);
            for (int c = 0; c < chirps; c++) {
                s.tone(at + c * 0.11, 0.06, base, base * 1.35, Synth.SINE, 0.08, 0.005, 0.02);
            }
        }
        // the next field over: distant pops, a whistle once
        for (int i = 0; i < 12; i++) {
            double at = s.random() * FIELD_SECONDS;
            s.tone(at, 0.08, 120, 60, Synth.SINE, 0.09, 0.002, 0.025);
            s.noise(at, 0.1, 0.03, 900, 400, 150, 0.001, 0.03);
        }
        double w = 3 + s.random() * 8;
        s.tone(w, 0.4, 2850, 2800, Synth.SINE, 0.025, 0.02, 0, 31, 0.035, 0);
        return s.loop(1.5);
    }
}
