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

import UnityEngine.Vector2;

/// `UnityEngine.Random`: the game's shared source of random numbers.
///
/// The sequence is this library's own -- a xorshift generator on four
/// integers -- and not Unity's, which no document specifies. It is the same
/// on every target, though: the generator is integer arithmetic, a float is
/// made from 24 of its bits by one exact division, and nothing here calls a
/// trigonometric function. A host that wants a repeatable run seeds it with
/// [#$seed] before the scene is built.
///
/// An application is seeded from the clock at every launch, as Unity seeds
/// from the operating system's entropy: `UnityApplication` calls `InitState`
/// before the first scene unless the `unity.seed` display property names a
/// seed. The fixed seed below is only what a host that skips
/// `UnityApplication` gets -- the trace harnesses, which compare runs.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Random {
    private static int sx;
    private static int sy;
    private static int sz;
    private static int sw;

    static {
        $seed(0);
    }

    private Random() {
    }

    /// Starts the sequence over from a seed.
    public static void $seed(int seed) {
        // The usual way to fill a xorshift state from one integer; a state
        // of all zeros would stay there, and this cannot produce one.
        sx = seed;
        sy = sx * 1812433253 + 1;
        sz = sy * 1812433253 + 1;
        sw = sz * 1812433253 + 1;
        if ((sx | sy | sz | sw) == 0) {
            sw = 1;
        }
    }

    public static void InitState(int seed) {
        $seed(seed);
    }

    private static int next() {
        int t = sx ^ (sx << 11);
        sx = sy;
        sy = sz;
        sz = sw;
        sw = sw ^ (sw >>> 19) ^ t ^ (t >>> 8);
        return sw;
    }

    /// 0 to 1, both possible.
    public static float get_value() {
        return (next() & 0xffffff) / 16777215f;
    }

    /// Between the two, both possible. The order they are given in does not
    /// matter.
    public static float Range(float minInclusive, float maxInclusive) {
        float v = get_value();
        float span = maxInclusive - minInclusive;
        if (span - span != 0f) {
            // Two finite ends further apart than a float reaches: weigh
            // each end instead, which never forms the overflowing span.
            return minInclusive * (1f - v) + maxInclusive * v;
        }
        float part = span * v;
        return minInclusive + part;
    }

    /// From `minInclusive` up to but not including `maxExclusive`; when the
    /// two are equal, that value.
    ///
    /// Given the wrong way round, each end keeps what its place in the call
    /// made it, as Unity documents: "the input parameters are swapped but
    /// retain their inclusivity or exclusivity based on their original
    /// positions, which means the method becomes Random.Range(minExclusive,
    /// maxInclusive)". `Range(10, 0)` is therefore 1 to 10 and never 0, where
    /// sorting the two first gave 0 to 9: a script that counts down from a
    /// length with it then read one before the start of its array.
    public static int Range(int minInclusive, int maxExclusive) {
        // In `long`, since the two may be the whole of `int` apart.
        long lo = minInclusive;
        long span = (long) maxExclusive - lo;
        if (span < 0L) {
            lo = (long) maxExclusive + 1L;
            span = -span;
        }
        if (span == 0L) {
            return minInclusive;
        }
        // Every value equally likely: `r % span` alone favours the low
        // values whenever span does not divide 2^32, so a sample at or
        // past the last whole multiple of span is drawn again. For the
        // small spans scripts use a redraw has probability span / 2^32,
        // which leaves seeded sequences as they were. All in `long`, read
        // as unsigned; span is at least 1 here and below 2^32.
        long limit = (0x100000000L / span) * span;
        long r = next() & 0xffffffffL;
        while (r >= limit) {
            r = next() & 0xffffffffL;
        }
        return (int) (lo + r % span);
    }

    /// A point inside the circle of radius one, every point as likely as
    /// any other: points of the enclosing square are drawn until one falls
    /// inside, which takes four tries in three on average.
    public static Vector2 get_insideUnitCircle(Vector2 ret) {
        for (;;) {
            float px = get_value() * 2f - 1f;
            float py = get_value() * 2f - 1f;
            float xx = px * px;
            float yy = py * py;
            if (xx + yy <= 1f) {
                ret.x = px;
                ret.y = py;
                return ret;
            }
        }
    }
}
