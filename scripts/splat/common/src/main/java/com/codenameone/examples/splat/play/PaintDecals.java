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

import java.util.Random;

/// The shapes paint leaves behind, drawn once at start-up into textures: a
/// splat is an irregular blob with lobes thrown out from where the ball burst,
/// a scatter of droplets around it and, on a wall, runs of paint dripping down.
///
/// Each texture is white with the shape in its alpha, so one set serves both
/// teams: the material's colour tints it.
final class PaintDecals {
    static final int SIZE = 128;
    /// How many different shapes there are of each kind, so no two splats in
    /// a row look alike.
    static final int VARIANTS = 4;

    private PaintDecals() {
    }

    /// One splat as ARGB pixels. `drips` adds runs toward the bottom of the
    /// texture, which a decal on a wall points down.
    static int[] splat(long seed, boolean drips) {
        Random r = new Random(seed);
        float[] a = new float[SIZE * SIZE];
        float c = SIZE / 2f;
        // the body: a radius that wobbles with the angle, plus a few long lobes
        int lobes = 5 + r.nextInt(4);
        float[] lobeAt = new float[lobes];
        float[] lobeLen = new float[lobes];
        for (int i = 0; i < lobes; i++) {
            lobeAt[i] = (float) (r.nextDouble() * Math.PI * 2);
            lobeLen[i] = 0.15f + (float) r.nextDouble() * 0.35f;
        }
        float p1 = (float) (r.nextDouble() * 6.28);
        float p2 = (float) (r.nextDouble() * 6.28);
        float base = SIZE * 0.24f;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float dx = x - c;
                float dy = y - c;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float ang = atan2(dy, dx);
                float rad = base * (1 + 0.12f * (float) Math.sin(ang * 5 + p1) + 0.07f * (float) Math.sin(ang * 11 + p2));
                for (int i = 0; i < lobes; i++) {
                    float diff = angleDiff(ang, lobeAt[i]);
                    // a narrow lobe that tapers to a round end
                    float w = 0.22f;
                    if (diff < w) {
                        float k = 1 - diff / w;
                        rad = Math.max(rad, base * (1 + lobeLen[i] * 1.6f * k * k));
                    }
                }
                a[y * SIZE + x] = edge(rad - d);
            }
        }
        // droplets flung outward, bigger the closer in
        int drops = 10 + r.nextInt(10);
        for (int i = 0; i < drops; i++) {
            float ang = (float) (r.nextDouble() * Math.PI * 2);
            float dist = base * (1.3f + (float) r.nextDouble() * 1.2f);
            float size = SIZE * (0.012f + (float) r.nextDouble() * 0.03f) * (2.6f * base / (dist + base));
            disc(a, c + (float) Math.cos(ang) * dist, c + (float) Math.sin(ang) * dist, size);
        }
        if (drips) {
            int runs = 2 + r.nextInt(3);
            for (int i = 0; i < runs; i++) {
                float x = c + (float) (r.nextDouble() - 0.5) * base * 1.4f;
                float len = SIZE * (0.15f + (float) r.nextDouble() * 0.3f);
                float w = SIZE * (0.012f + (float) r.nextDouble() * 0.012f);
                float start = c + base * 0.5f;
                for (float y = start; y < Math.min(SIZE - 4, start + len); y += 0.5f) {
                    disc(a, x, y, w);
                }
                // a bead where the run stops
                disc(a, x, Math.min(SIZE - 6, start + len), w * 1.6f);
            }
        }
        int[] argb = new int[SIZE * SIZE];
        for (int i = 0; i < argb.length; i++) {
            int al = (int) (Math.max(0, Math.min(1, a[i])) * 255);
            // a touch darker at the rim, where paint pools as it dries
            int shade = 255 - (int) (40 * Math.max(0, Math.min(1, 1 - a[i]) * Math.min(1, a[i] * 2)));
            argb[i] = (al << 24) | (shade << 16) | (shade << 8) | shade;
        }
        return argb;
    }

    private static float edge(float inside) {
        return Math.max(0, Math.min(1, inside * 0.7f + 0.5f));
    }

    private static void disc(float[] a, float cx, float cy, float rad) {
        int x0 = Math.max(0, (int) (cx - rad - 2));
        int x1 = Math.min(SIZE - 1, (int) (cx + rad + 2));
        int y0 = Math.max(0, (int) (cy - rad - 2));
        int y1 = Math.min(SIZE - 1, (int) (cy + rad + 2));
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                float dx = x - cx;
                float dy = y - cy;
                float v = edge(rad - (float) Math.sqrt(dx * dx + dy * dy));
                int i = y * SIZE + x;
                if (v > a[i]) {
                    a[i] = v;
                }
            }
        }
    }

    private static float angleDiff(float a, float b) {
        float d = Math.abs(a - b) % (float) (Math.PI * 2);
        return d > Math.PI ? (float) (Math.PI * 2) - d : d;
    }

    /// atan2 without java.lang.Math.atan2, which a device's class library lacks.
    private static float atan2(float y, float x) {
        return (float) com.codename1.util.MathUtil.atan2(y, x);
    }
}
