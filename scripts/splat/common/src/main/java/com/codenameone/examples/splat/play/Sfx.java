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

import com.codename1.gaming.SoundEffect;
import com.codename1.gaming.SoundPool;
import java.io.ByteArrayInputStream;

/// The match's sound effects, synthesized at start-up by [Synth] and played
/// through a [SoundPool], which mixes overlapping voices with low latency where
/// the platform has a native mixer.
///
/// Sounds from elsewhere on the field take a volume and a pan from where they
/// happened (`ArenaView.place`), so a firefight to your left is heard on the
/// left and a far one barely at all.
public final class Sfx {
    private static SoundPool pool;
    private static SoundEffect fire;
    private static SoundEffect[] splats;
    private static SoundEffect hit;
    private static SoundEffect painted;
    private static SoundEffect whistle;
    private static SoundEffect whistleTwice;
    private static SoundEffect grenade;
    private static SoundEffect toss;
    private static SoundEffect reload;
    private static SoundEffect[] steps;
    private static SoundEffect land;
    private static SoundEffect flagTaken;
    private static SoundEffect flagLost;
    private static SoundEffect capture;
    private static SoundEffect point;
    private static SoundEffect beep;
    private static SoundEffect horn;
    private static SoundEffect wipe;
    private static SoundEffect buzzer;
    private static SoundEffect boing;
    private static SoundEffect win;
    private static SoundEffect lose;
    private static SoundEffect click;
    private static boolean muted;
    private static long lastFire;
    private static int step;

    private Sfx() {
    }

    public static void setMuted(boolean m) {
        muted = m;
        if (m && pool != null) {
            pool.stopAll();
        }
    }

    public static boolean isMuted() {
        return muted;
    }

    /// Builds and loads every clip; quietly does nothing where audio is
    /// unavailable.
    ///
    /// Only a native mixer is used. Without one, a pool falls back to a media
    /// player per voice, which a marker firing ten times a second would drown
    /// in, so the game stays silent instead (a CI runner with no audio device).
    public static void load() {
        if (pool != null) {
            return;
        }
        try {
            SoundPool created = SoundPool.create(16);
            if (!created.isNativeAccelerated()) {
                created.release();
                return;
            }
            pool = created;
            fire = clip(makeFire());
            splats = new SoundEffect[] {clip(makeSplat(1, 1)), clip(makeSplat(2, 1)), clip(makeSplat(3, 1))};
            hit = clip(makeHit());
            painted = clip(makePainted());
            whistle = clip(makeWhistle(false));
            whistleTwice = clip(makeWhistle(true));
            grenade = clip(makeGrenade());
            toss = clip(makeToss());
            reload = clip(makeReload());
            steps = new SoundEffect[] {clip(makeStep(1)), clip(makeStep(2)), clip(makeStep(3)), clip(makeStep(4))};
            land = clip(makeLand());
            flagTaken = clip(makeArpeggio(new int[] {15, 19, 22}, 0.08, 0.3, Synth.SQUARE));
            flagLost = clip(makeArpeggio(new int[] {10, 6, 3}, 0.1, 0.4, Synth.SQUARE));
            capture = clip(makeCapture());
            point = clip(makePoint());
            beep = clip(makeBeep());
            horn = clip(makeHorn());
            wipe = clip(makeWipe());
            buzzer = clip(makeBuzzer());
            boing = clip(makeBoing());
            win = clip(makeWin());
            lose = clip(makeLose());
            click = clip(makeClick());
        } catch (Throwable unavailable) {
            pool = null;
        }
    }

    private static SoundEffect clip(Synth s) throws java.io.IOException {
        return pool.load(new ByteArrayInputStream(s.wav()), "audio/wav");
    }

    // ------------------------------------------------------------ playing

    /// Your own marker. A ramping marker fires every third tick; one shot
    /// sound per 60 ms is plenty.
    public static void fire() {
        long now = System.currentTimeMillis();
        if (now - lastFire > 60) {
            lastFire = now;
            play(fire, 0.9f, 0, 0.94f + (float) Math.random() * 0.12f);
        }
    }

    /// Somebody else's marker.
    public static void fireAt(float volume, float pan) {
        play(fire, volume * 0.6f, pan, 0.85f + (float) Math.random() * 0.1f);
    }

    public static void splat(float volume, float pan) {
        play(splats[(int) (Math.random() * splats.length)], volume, pan, 0.9f + (float) Math.random() * 0.2f);
    }

    /// The tick that says your ball landed on somebody.
    public static void hit() {
        play(hit, 0.9f, 0, 1);
    }

    /// You were the somebody.
    public static void painted() {
        play(painted, 1, 0, 1);
    }

    /// The referee. Every PAINTED, and every wipe the referee catches.
    public static void whistle(float volume, float pan) {
        play(whistle, volume, pan, 0.97f + (float) Math.random() * 0.06f);
    }

    /// The two blasts that start and end a match.
    public static void whistleTwice() {
        play(whistleTwice, 1, 0, 1);
    }

    public static void grenade(float volume, float pan) {
        play(grenade, volume, pan, 1);
    }

    public static void toss() {
        play(toss, 0.8f, 0, 1);
    }

    public static void reload() {
        play(reload, 0.8f, 0, 1);
    }

    /// One footstep; the four takes go round so a run never sounds like a loop.
    public static void step(float volume) {
        step = (step + 1 + (int) (Math.random() * 2)) % steps.length;
        play(steps[step], volume, 0, 0.92f + (float) Math.random() * 0.16f);
    }

    public static void land() {
        play(land, 0.8f, 0, 1);
    }

    /// A flag was picked up: bright if it is theirs, a warning if it is ours.
    public static void flag(boolean good) {
        play(good ? flagTaken : flagLost, 0.8f, 0, 1);
    }

    public static void capture(boolean ours) {
        play(ours ? capture : flagLost, 0.9f, 0, ours ? 1 : 0.8f);
    }

    public static void point(boolean ours) {
        play(point, 0.6f, 0, ours ? 1 : 0.8f);
    }

    public static void beep() {
        play(beep, 0.7f, 0, 1);
    }

    /// The air horn at the start, the way every weekend league opens.
    public static void horn() {
        play(horn, 0.9f, 0, 1);
    }

    public static void wipe() {
        play(wipe, 0.8f, 0, 1);
    }

    public static void buzzer() {
        play(buzzer, 0.8f, 0, 1);
    }

    /// Hit by your own team.
    public static void boing() {
        play(boing, 0.8f, 0, 1);
    }

    public static void win() {
        play(win, 0.9f, 0, 1);
    }

    public static void lose() {
        play(lose, 0.9f, 0, 1);
    }

    public static void click() {
        play(click, 0.6f, 0, 1);
    }

    private static void play(SoundEffect e, float volume, float pan, float rate) {
        if (!muted && pool != null && e != null && volume > 0.02f) {
            try {
                pool.play(e, Math.min(1, volume), Math.max(-1, Math.min(1, pan)), rate, 0);
            } catch (Throwable ignored) {
                // Sound is decoration; a failure must never stop a match.
            }
        }
    }

    // ------------------------------------------------------------ the sounds

    /// A paintball marker: a soft low thump of air and the hiss of the barrel.
    private static Synth makeFire() {
        Synth s = new Synth(0.16, 1);
        s.tone(0, 0.09, 150, 55, Synth.SINE, 0.55, 0.002, 0.03);
        s.noise(0, 0.14, 0.4, 3200, 700, 300, 0.001, 0.035);
        s.tone(0, 0.01, 1800, 1500, Synth.SQUARE, 0.08, 0, 0.004);
        return s;
    }

    /// A ball bursting: a wet slap, a thud and a drip.
    private static Synth makeSplat(long seed, double size) {
        Synth s = new Synth(0.3, seed * 31);
        s.noise(0, 0.24, 0.6 * size, 2600, 350, 0, 0.001, 0.05);
        s.tone(0, 0.1, 95, 50, Synth.SINE, 0.4 * size, 0.001, 0.03);
        double drip = 0.05 + s.random() * 0.04;
        s.tone(drip, 0.05, 950 + s.random() * 300, 500, Synth.SINE, 0.12, 0.002, 0.018);
        return s;
    }

    /// The hit marker: one dry, bright tick.
    private static Synth makeHit() {
        Synth s = new Synth(0.07, 2);
        s.tone(0, 0.05, 2500, 2300, Synth.SINE, 0.32, 0.001, 0.012);
        s.noise(0, 0.02, 0.15, 9000, 9000, 3000, 0, 0.006);
        return s;
    }

    /// Hit: a big splat right on you, and the sinking feeling that comes with it.
    private static Synth makePainted() {
        Synth s = new Synth(0.5, 7);
        s.noise(0, 0.32, 0.75, 3000, 300, 0, 0.001, 0.07);
        s.tone(0, 0.3, 75, 38, Synth.SINE, 0.6, 0.002, 0.09);
        s.tone(0.12, 0.3, 330, 165, Synth.TRIANGLE, 0.12, 0.01, 0.12);
        return s;
    }

    /// A referee's pea whistle: the pea rattles the pitch thirty times a second.
    private static Synth makeWhistle(boolean twice) {
        Synth s = new Synth(twice ? 0.85 : 0.5, 3);
        if (twice) {
            blast(s, 0, 0.17);
            blast(s, 0.27, 0.55);
        } else {
            blast(s, 0, 0.48);
        }
        return s;
    }

    private static void blast(Synth s, double at, double len) {
        s.tone(at, len, 2850, 2800, Synth.SINE, 0.22, 0.015, 0, 31, 0.035, 0);
        s.tone(at, len, 5700, 5600, Synth.SINE, 0.03, 0.015, 0, 31, 0.035, 0);
        s.noise(at, len, 0.07, 7000, 7000, 2000, 0.01, 0);
    }

    /// A paint grenade: a pop, a boom and paint landing everywhere.
    private static Synth makeGrenade() {
        Synth s = new Synth(0.9, 4);
        s.tone(0, 0.5, 85, 32, Synth.SINE, 0.75, 0.002, 0.13);
        s.noise(0, 0.7, 0.7, 2000, 180, 0, 0.002, 0.14);
        for (int i = 0; i < 6; i++) {
            double at = 0.08 + s.random() * 0.35;
            s.noise(at, 0.12, 0.25, 2400, 500, 0, 0.001, 0.03);
        }
        return s;
    }

    /// The whoosh of a throw.
    private static Synth makeToss() {
        Synth s = new Synth(0.32, 5);
        s.noise(0, 0.3, 0.35, 400, 2600, 250, 0.18, 0.06);
        return s;
    }

    /// Balls rattling down from the hopper, and the bolt closing.
    private static Synth makeReload() {
        Synth s = new Synth(0.6, 6);
        for (int i = 0; i < 9; i++) {
            double at = 0.02 + s.random() * 0.42;
            s.noise(at, 0.014, 0.3, 6500, 6500, 1400, 0, 0.004);
            s.tone(at, 0.02, 700 + s.random() * 400, 500, Synth.SINE, 0.08, 0, 0.006);
        }
        s.tone(0.5, 0.05, 520, 380, Synth.SQUARE, 0.14, 0, 0.012, 0, 0, 2500);
        s.noise(0.5, 0.04, 0.25, 5000, 2000, 600, 0, 0.01);
        return s;
    }

    /// A step on gravel and asphalt.
    private static Synth makeStep(long seed) {
        Synth s = new Synth(0.11, 40 + seed);
        s.noise(0, 0.08, 0.45, 1000 + s.random() * 400, 450, 120, 0.004, 0.022);
        s.tone(0, 0.05, 125, 80, Synth.SINE, 0.12, 0.002, 0.015);
        return s;
    }

    private static Synth makeLand() {
        Synth s = new Synth(0.18, 8);
        s.tone(0, 0.14, 105, 48, Synth.SINE, 0.45, 0.002, 0.04);
        s.noise(0, 0.12, 0.3, 700, 300, 80, 0.002, 0.03);
        return s;
    }

    /// Notes one after another, semitones above A 440.
    private static Synth makeArpeggio(int[] semis, double each, double last, int wave) {
        Synth s = new Synth(each * (semis.length - 1) + last + 0.05, 9);
        for (int i = 0; i < semis.length; i++) {
            double f = Synth.note(semis[i]);
            double len = i == semis.length - 1 ? last : each + 0.02;
            s.tone(i * each, len, f, f, wave, 0.2, 0.005, len * 0.5, 0, 0, 3500);
        }
        return s;
    }

    /// A capture: a little brass fanfare and a snare.
    private static Synth makeCapture() {
        Synth s = new Synth(1.0, 10);
        int[] notes = {3, 7, 10, 15};
        for (int i = 0; i < notes.length; i++) {
            double f = Synth.note(notes[i]);
            double len = i == notes.length - 1 ? 0.6 : 0.13;
            for (int d = -1; d <= 1; d++) {
                s.tone(i * 0.11, len, f * (1 + d * 0.004), f * (1 + d * 0.004), Synth.SAW, 0.09, 0.012, len * 0.7,
                        5, i == notes.length - 1 ? 0.006 : 0, 2600);
            }
            s.noise(i * 0.11, 0.08, 0.15, 8000, 3000, 1200, 0, 0.025);
        }
        return s;
    }

    /// A snack bar point: two soft bell notes.
    private static Synth makePoint() {
        Synth s = new Synth(0.5, 11);
        s.tone(0, 0.45, 1320, 1320, Synth.SINE, 0.18, 0.002, 0.12);
        s.tone(0.09, 0.4, 1760, 1760, Synth.SINE, 0.15, 0.002, 0.12);
        return s;
    }

    private static Synth makeBeep() {
        Synth s = new Synth(0.16, 12);
        s.tone(0, 0.14, 880, 880, Synth.SQUARE, 0.18, 0.003, 0, 0, 0, 3000);
        return s;
    }

    /// An air horn: three detuned reeds, a dip in pitch as the can opens.
    private static Synth makeHorn() {
        Synth s = new Synth(1.0, 13);
        double[] f = {349, 440, 523};
        for (int i = 0; i < f.length; i++) {
            s.tone(0, 0.08, f[i] * 0.93, f[i], Synth.SAW, 0.12, 0.01, 0, 0, 0, 3200);
            s.tone(0.08, 0.85, f[i], f[i] * 0.99, Synth.SAW, 0.12, 0, 0, 6, 0.004, 3200);
        }
        return s;
    }

    /// Rubbing the paint off a sleeve, quickly, while nobody looks.
    private static Synth makeWipe() {
        Synth s = new Synth(0.55, 14);
        for (int i = 0; i < 5; i++) {
            s.noise(i * 0.1, 0.08, 0.5, 1400, 800, 300, 0.03, 0.03);
        }
        return s;
    }

    /// Caught. The buzzer of a game show nobody wanted to be on.
    private static Synth makeBuzzer() {
        Synth s = new Synth(0.5, 15);
        s.tone(0, 0.45, 110, 105, Synth.SQUARE, 0.13, 0.005, 0, 0, 0, 1600);
        s.tone(0, 0.45, 117, 112, Synth.SQUARE, 0.13, 0.005, 0, 0, 0, 1600);
        return s;
    }

    private static Synth makeBoing() {
        Synth s = new Synth(0.4, 16);
        s.tone(0, 0.36, 180, 420, Synth.TRIANGLE, 0.3, 0.005, 0.15, 11, 0.06, 0);
        return s;
    }

    /// Victory: a brass run up to a held chord, a roll and a cymbal.
    private static Synth makeWin() {
        Synth s = new Synth(2.6, 17);
        int[] run = {-7, -3, 0, 5};
        for (int i = 0; i < run.length; i++) {
            brass(s, i * 0.14, 0.16, Synth.note(run[i]), 0.1);
        }
        int[] chord = {5, 9, 12, 17};
        for (int n : chord) {
            brass(s, 0.56, 1.8, Synth.note(n), 0.07);
        }
        for (int i = 0; i < 10; i++) {
            s.noise(0.06 * i, 0.05, 0.08 + i * 0.012, 7000, 3000, 1500, 0, 0.02);
        }
        s.noise(0.56, 2.0, 0.22, 11000, 7000, 4000, 0.002, 0.5);
        s.tone(0.56, 0.4, 80, 45, Synth.SINE, 0.4, 0.002, 0.1);
        return s;
    }

    /// Defeat: four notes on a sad trombone, the last one wobbling.
    private static Synth makeLose() {
        Synth s = new Synth(2.4, 18);
        int[] notes = {-11, -12, -13, -14};
        for (int i = 0; i < notes.length; i++) {
            double f = Synth.note(notes[i]);
            boolean last = i == notes.length - 1;
            double len = last ? 1.3 : 0.36;
            s.tone(i * 0.42, len, f * 1.02, f, Synth.SAW, 0.16, 0.06, last ? 0.9 : 0.4, last ? 5.5 : 0,
                    last ? 0.025 : 0, 1100);
            s.tone(i * 0.42, len, f * 1.02, f, Synth.TRIANGLE, 0.14, 0.06, last ? 0.9 : 0.4, last ? 5.5 : 0,
                    last ? 0.025 : 0, 0);
        }
        return s;
    }

    private static void brass(Synth s, double at, double len, double f, double amp) {
        for (int d = -1; d <= 1; d++) {
            s.tone(at, len, f * (1 + d * 0.005), f * (1 + d * 0.005), Synth.SAW, amp, 0.02, len * 0.8, 5, 0.004,
                    2400);
        }
    }

    private static Synth makeClick() {
        Synth s = new Synth(0.04, 19);
        s.tone(0, 0.025, 1600, 1300, Synth.SINE, 0.25, 0.001, 0.006);
        s.noise(0, 0.012, 0.12, 9000, 9000, 2500, 0, 0.004);
        return s;
    }

    /// The menu music and the field's ambience come from [Music]; the stingers
    /// are effects, so they stop with the rest when sound is turned off.
    static boolean ready() {
        return pool != null && !muted;
    }
}
