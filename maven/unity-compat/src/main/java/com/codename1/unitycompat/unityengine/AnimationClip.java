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

/// `UnityEngine.AnimationClip`: what a set of properties is at each moment
/// of a stretch of time.
///
/// The scene compiler reads a `.anim` file and writes the calls that build
/// one of these: a curve for each property the clip animates. A curve of
/// numbers is a list of keys, each a time, a value and the slope on either
/// side, joined by the cubic through them; a curve of sprites is a list of
/// times and the sprite shown from each one on.
///
/// What a curve drives is named by a path -- the object, as the names of
/// its ancestors down from the one holding the [Animator] -- and a kind,
/// one of the constants here.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class AnimationClip extends Object {
    /// The transform's local position.
    public static final int POSITION_X = 0;
    public static final int POSITION_Y = 1;
    public static final int POSITION_Z = 2;
    /// The transform's turn about the z axis, in degrees.
    public static final int ROTATION_Z = 3;
    /// The transform's local scale.
    public static final int SCALE_X = 4;
    public static final int SCALE_Y = 5;
    public static final int SCALE_Z = 6;
    /// The sprite renderer's colour.
    public static final int COLOR_R = 7;
    public static final int COLOR_G = 8;
    public static final int COLOR_B = 9;
    public static final int COLOR_A = 10;
    /// Whether the sprite renderer is enabled: above a half is yes.
    public static final int RENDERER_ENABLED = 11;
    /// Whether the object is active.
    public static final int ACTIVE = 12;
    public static final int FLIP_X = 13;
    public static final int FLIP_Y = 14;
    /// The sprite renderer's sprite.
    public static final int SPRITE = 15;

    final float length;
    final float frameRate;
    final boolean loop;
    int curveCount;
    String[] paths = new String[4];
    int[] kinds = new int[4];
    /// For a curve of numbers, four to a key: time, value, slope coming in,
    /// slope going out. For a curve of sprites, the times alone.
    float[][] keys = new float[4][];
    Sprite[][] sprites = new Sprite[4][];
    int eventCount;
    float[] eventTimes = new float[0];
    String[] eventNames = new String[0];

    public AnimationClip(String name, float length, float frameRate, boolean loop) {
        this.name = name;
        this.length = length;
        this.frameRate = frameRate;
        this.loop = loop;
    }

    private int add(String path, int kind) {
        if (curveCount == paths.length) {
            int n = curveCount * 2;
            String[] p = new String[n];
            int[] k = new int[n];
            float[][] f = new float[n][];
            Sprite[][] s = new Sprite[n][];
            System.arraycopy(paths, 0, p, 0, curveCount);
            System.arraycopy(kinds, 0, k, 0, curveCount);
            System.arraycopy(keys, 0, f, 0, curveCount);
            System.arraycopy(sprites, 0, s, 0, curveCount);
            paths = p;
            kinds = k;
            keys = f;
            sprites = s;
        }
        paths[curveCount] = path;
        kinds[curveCount] = kind;
        return curveCount++;
    }

    /// A curve of numbers: `values` holds four to a key.
    public void $curve(String path, int kind, float[] values) {
        // The index first: adding may replace the arrays.
        int at = add(path, kind);
        keys[at] = values;
    }

    /// A curve of sprites: the sprite shown from each time on.
    public void $sprites(String path, float[] times, Sprite[] shown) {
        int at = add(path, SPRITE);
        keys[at] = times;
        sprites[at] = shown;
    }

    /// A method of the scripts beside the animator, called when the clip
    /// passes a time.
    public void $event(float time, String function) {
        float[] t = new float[eventCount + 1];
        String[] f = new String[eventCount + 1];
        System.arraycopy(eventTimes, 0, t, 0, eventCount);
        System.arraycopy(eventNames, 0, f, 0, eventCount);
        t[eventCount] = time;
        f[eventCount] = function;
        eventTimes = t;
        eventNames = f;
        eventCount++;
    }

    public float get_length() {
        return length;
    }

    public float get_frameRate() {
        return frameRate;
    }

    public boolean get_isLooping() {
        return loop;
    }

    /// The value of a curve of numbers at a time.
    static float sample(float[] k, float time) {
        int n = k.length / 4;
        if (n == 0) {
            return 0f;
        }
        if (time <= k[0] || n == 1) {
            return k[1];
        }
        int last = (n - 1) * 4;
        if (time >= k[last]) {
            return k[last + 1];
        }
        int at = 0;
        while (at + 4 < last && k[at + 4] <= time) {
            at += 4;
        }
        float t0 = k[at];
        float v0 = k[at + 1];
        float out = k[at + 3];
        float t1 = k[at + 4];
        float v1 = k[at + 5];
        float in = k[at + 6];
        float span = t1 - t0;
        // A slope that is not a number is how a step is written: the value
        // holds until the next key.
        if (!(span > 0f) || out != out || in != in || out > 3.0e38f || out < -3.0e38f || in > 3.0e38f // NOPMD
                || in < -3.0e38f) {
            return v0;
        }
        float u = (time - t0) / span;
        float u2 = u * u;
        float u3 = u2 * u;
        // The cubic through both keys with the two slopes, each term stored
        // before it is added so that every target rounds alike.
        float m0 = out * span;
        float m1 = in * span;
        float a3 = 2f * u3;
        float b3 = 3f * u2;
        float h00 = a3 - b3 + 1f;
        float c2 = 2f * u2;
        float h10 = u3 - c2 + u;
        float h01 = b3 - a3;
        float h11 = u3 - u2;
        float p0 = h00 * v0;
        float p1 = h10 * m0;
        float p2 = h01 * v1;
        float p3 = h11 * m1;
        return p0 + p1 + p2 + p3;
    }

    /// The index of the sprite a curve of sprites shows at a time.
    static int frame(float[] times, float time) {
        int at = 0;
        int n = times.length;
        while (at + 1 < n && times[at + 1] <= time) {
            at++;
        }
        return at;
    }
}
