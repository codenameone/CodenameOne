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
/// [#$seed] before the scene is built; the seed is otherwise fixed.
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
        float span = maxInclusive - minInclusive;
        float part = span * get_value();
        return minInclusive + part;
    }

    /// From `minInclusive` up to but not including `maxExclusive`; when the
    /// two are equal, that value.
    public static int Range(int minInclusive, int maxExclusive) {
        int lo = minInclusive < maxExclusive ? minInclusive : maxExclusive;
        int hi = minInclusive < maxExclusive ? maxExclusive : minInclusive;
        long span = (long) hi - (long) lo;
        if (span <= 0L) {
            return lo;
        }
        long r = next() & 0xffffffffL;
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
