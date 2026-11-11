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

import com.codename1.util.MathUtil;
import java.util.Random;

/// A tiny synthesizer: every sound and every bar of music in the game is
/// rendered by it at start-up, into mono 16-bit WAV, so the sample ships no
/// audio files and nothing in it needs a licence.
///
/// A clip is a buffer of the given length; [#tone] and [#noise] add a voice to
/// it, each with its own envelope (a linear attack, an exponential decay and a
/// short release so nothing clicks). [#wav] encodes the result.
///
/// To use recorded sounds instead, load files into the `SoundPool` in [Sfx]
/// and drop the calls that build them here.
final class Synth {
    /// Low enough to keep the clips small, high enough for a whistle.
    static final int RATE = 22050;

    static final int SINE = 0;
    static final int SQUARE = 1;
    static final int SAW = 2;
    static final int TRIANGLE = 3;

    private final float[] buf;
    private final Random rnd;

    Synth(double seconds, long seed) {
        buf = new float[Math.max(1, (int) (seconds * RATE))];
        rnd = new Random(seed);
    }

    double random() {
        return rnd.nextDouble();
    }

    double seconds() {
        return (double) buf.length / RATE;
    }

    /// Adds a pitched voice.
    ///
    /// - `start`, `dur`: where and how long, in seconds
    /// - `f0`, `f1`: the pitch at the start and at the end, in Hz (a slide)
    /// - `wave`: `SINE`, `SQUARE`, `SAW` or `TRIANGLE`
    /// - `amp`: the peak level, 0..1
    /// - `attack`: seconds to full level
    /// - `decay`: the time constant of the fall after it; 0 holds the level
    /// - `vibHz`, `vibDepth`: a vibrato, as a rate and a fraction of the pitch
    /// - `lowpass`: a cutoff in Hz that softens a bright wave; 0 for none
    void tone(double start, double dur, double f0, double f1, int wave, double amp, double attack,
            double decay, double vibHz, double vibDepth, double lowpass) {
        int from = (int) (start * RATE);
        int n = (int) (dur * RATE);
        double phase = rnd.nextDouble();
        double lp = 0;
        double a = lowpass > 0 ? alpha(lowpass) : 1;
        for (int i = 0; i < n && from + i < buf.length; i++) {
            if (from + i < 0) {
                continue;
            }
            double t = (double) i / RATE;
            double hz = f0 + (f1 - f0) * i / n;
            if (vibDepth > 0) {
                hz *= 1 + vibDepth * Math.sin(2 * Math.PI * vibHz * t);
            }
            phase += hz / RATE;
            double p = phase - Math.floor(phase);
            double s;
            switch (wave) {
                case SQUARE:
                    s = p < 0.5 ? 1 : -1;
                    break;
                case SAW:
                    s = 2 * p - 1;
                    break;
                case TRIANGLE:
                    s = p < 0.5 ? 4 * p - 1 : 3 - 4 * p;
                    break;
                default:
                    s = Math.sin(2 * Math.PI * p);
                    break;
            }
            lp += a * (s - lp);
            buf[from + i] += (float) (lp * amp * envelope(i, n, attack, decay));
        }
    }

    /// The common case of [#tone]: no vibrato and no filter.
    void tone(double start, double dur, double f0, double f1, int wave, double amp, double attack, double decay) {
        tone(start, dur, f0, f1, wave, amp, attack, decay, 0, 0, 0);
    }

    /// Adds filtered white noise: the breath of a whistle, the burst of a
    /// paintball, wind. `lowFrom`/`lowTo` sweep the low-pass cutoff over the
    /// voice; `high` is a fixed high-pass cutoff (0 for none).
    void noise(double start, double dur, double amp, double lowFrom, double lowTo, double high, double attack,
            double decay) {
        int from = (int) (start * RATE);
        int n = (int) (dur * RATE);
        double lp = 0;
        double hpIn = 0;
        double hpOut = 0;
        double ha = high > 0 ? MathUtil.exp(-2 * Math.PI * high / RATE) : 0;
        for (int i = 0; i < n && from + i < buf.length; i++) {
            if (from + i < 0) {
                continue;
            }
            double cutoff = lowFrom + (lowTo - lowFrom) * i / n;
            double w = rnd.nextDouble() * 2 - 1;
            lp += alpha(cutoff) * (w - lp);
            double s = lp;
            if (high > 0) {
                hpOut = ha * (hpOut + s - hpIn);
                hpIn = s;
                s = hpOut;
            }
            buf[from + i] += (float) (s * amp * envelope(i, n, attack, decay));
        }
    }

    /// The level at sample `i` of `n`.
    private static double envelope(int i, int n, double attack, double decay) {
        double t = (double) i / RATE;
        double e;
        if (attack > 0 && t < attack) {
            e = t / attack;
        } else {
            e = decay > 0 ? MathUtil.exp(-(t - attack) / decay) : 1;
        }
        // 4 ms of release, so a voice cut short never clicks
        int tail = Math.min(n, RATE / 250);
        if (i > n - tail) {
            e *= (double) (n - i) / tail;
        }
        return e;
    }

    private static double alpha(double cutoff) {
        double c = Math.max(20, Math.min(RATE / 2.2, cutoff));
        return 1 - MathUtil.exp(-2 * Math.PI * c / RATE);
    }

    /// Fades the last `seconds` into the start, so the clip loops without a
    /// seam. The buffer is shortened by that much.
    Synth loop(double seconds) {
        int x = (int) (seconds * RATE);
        int len = buf.length - x;
        for (int i = 0; i < x; i++) {
            double k = (double) i / x;
            buf[i] = (float) (buf[i] * k + buf[len + i] * (1 - k));
        }
        Synth out = new Synth((double) len / RATE, 0);
        System.arraycopy(buf, 0, out.buf, 0, len);
        return out;
    }

    /// A mono 16-bit WAV of the buffer, clipped softly at full scale.
    byte[] wav() {
        int n = buf.length;
        byte[] out = new byte[44 + n * 2];
        put(out, 0, "RIFF");
        le32(out, 4, 36 + n * 2);
        put(out, 8, "WAVE");
        put(out, 12, "fmt ");
        le32(out, 16, 16);
        le16(out, 20, 1);
        le16(out, 22, 1);
        le32(out, 24, RATE);
        le32(out, 28, RATE * 2);
        le16(out, 32, 2);
        le16(out, 34, 16);
        put(out, 36, "data");
        le32(out, 40, n * 2);
        for (int i = 0; i < n; i++) {
            double v = buf[i];
            // a gentle knee instead of a hard clip when voices pile up
            v = v > 0.8 ? 0.8 + (v - 0.8) / (1 + (v - 0.8) * 5) : v < -0.8 ? -0.8 + (v + 0.8) / (1 - (v + 0.8) * 5) : v;
            int s = (int) (Math.max(-1, Math.min(1, v)) * 32767);
            out[44 + i * 2] = (byte) (s & 0xff);
            out[45 + i * 2] = (byte) ((s >> 8) & 0xff);
        }
        return out;
    }

    private static void put(byte[] b, int at, String s) {
        for (int i = 0; i < 4; i++) {
            b[at + i] = (byte) s.charAt(i);
        }
    }

    private static void le32(byte[] b, int at, int v) {
        b[at] = (byte) (v & 0xff);
        b[at + 1] = (byte) ((v >> 8) & 0xff);
        b[at + 2] = (byte) ((v >> 16) & 0xff);
        b[at + 3] = (byte) ((v >> 24) & 0xff);
    }

    private static void le16(byte[] b, int at, int v) {
        b[at] = (byte) (v & 0xff);
        b[at + 1] = (byte) ((v >> 8) & 0xff);
    }

    /// The frequency of a note, `semis` semitones above A 440.
    static double note(int semis) {
        return 440 * MathUtil.pow(2, semis / 12.0);
    }
}
