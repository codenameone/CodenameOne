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
package com.codenameone.examples.dash.run;

/// The look a level's `theme` names: the sky, the light, the track and the
/// city along it. A level author picks one in the editor; a new theme is a
/// new entry in [#of].
public final class Theme {
    public final String id;
    /// The sky straight up, and at the horizon.
    public final int skyTop;
    public final int skyHorizon;
    /// The sun or moon sitting on the horizon ahead.
    public final int sun;
    public final int ambient;
    public final int light;
    public final int ground;
    /// The running surface, and the light lines painted on it.
    public final int asphalt;
    public final int line;
    /// The glowing rails along both edges, and the gantries over the track.
    public final int rail;
    public final int tower;
    public final int window;

    private Theme(String id, int skyTop, int skyHorizon, int sun, int ambient, int light, int ground,
            int asphalt, int line, int rail, int tower, int window) {
        this.id = id;
        this.skyTop = skyTop;
        this.skyHorizon = skyHorizon;
        this.sun = sun;
        this.ambient = ambient;
        this.light = light;
        this.ground = ground;
        this.asphalt = asphalt;
        this.line = line;
        this.rail = rail;
        this.tower = tower;
        this.window = window;
    }

    /// The themes an editor offers, in order.
    public static final String[] IDS = {"daybreak", "neon", "ember"};

    public static Theme of(String id) {
        if ("neon".equals(id)) {
            return new Theme("neon", 0xff07051a, 0xff3a1258, 0xffff3d9a, 0xff4a4070, 0xffb8c8ff, 0xff0a0816,
                    0xff15132a, 0xff35e8ff, 0xffff3d9a, 0xff12102a, 0xff35e8ff);
        }
        if ("ember".equals(id)) {
            return new Theme("ember", 0xff2a1a4a, 0xffff8a4a, 0xffffd27a, 0xff7a5a6a, 0xffffd0a0, 0xff2a1a24,
                    0xff2c2430, 0xffffc35a, 0xffff6a3c, 0xff3a2438, 0xffffc35a);
        }
        return new Theme("daybreak", 0xff3c8ae6, 0xffc8ecff, 0xfffff6d8, 0xff8a96aa, 0xfffff4e0, 0xff4f8a5e,
                0xff3a4152, 0xfff4f7ff, 0xffff5a3c, 0xffdfe8f4, 0xff7ab8ff);
    }

    /// A 128x128 tile of the track: dark glossy lanes with a sheen down
    /// their middle, a glowing dashed line between each pair, a bright edge
    /// on both sides, panel seams across, and a chevron per tile pointing
    /// down the track, so speed reads even on a straight.
    static int[] laneTexture(Theme t, int lanes) {
        int n = 128;
        int[] px = new int[n * n];
        int edge = 5;
        float laneW = (n - edge * 2) / (float) Math.max(1, lanes);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int grain = ((x * 73856093) ^ (y * 19349663)) & 7;
                float lx = (x - edge) / laneW;
                int laneIdx = (int) Math.floor(lx);
                float in = lx - laneIdx;
                // Sheen: a little brighter in the middle of each lane.
                float off = Math.abs(in - 0.5f) * 2f;
                int c = shade(t.asphalt, 0.9f + grain * 0.01f + (1f - off * off) * 0.14f);
                // Panel seams: a dark groove with a lit lip behind it.
                int sy = y % 64;
                if (sy == 0) {
                    c = shade(c, 0.55f);
                } else if (sy == 1) {
                    c = mix(c, t.line, 0.12f);
                }
                // Chevron down the lane centre.
                float dx = Math.abs(in - 0.5f) * laneW;
                if (dx < laneW * 0.22f) {
                    float d = Math.abs(((y + dx * 1.2f) % 64f) - 10f);
                    if (d < 3f) {
                        c = mix(c, t.line, 0.28f * (1f - d / 3f));
                    }
                }
                // Lane dividers, dashed, with a soft glow either side.
                if (lanes > 1 && x >= edge && x < n - edge) {
                    float dist = Math.min(in, 1f - in) * laneW;
                    boolean between = !(laneIdx == 0 && in < 0.5f) && !(laneIdx == lanes - 1 && in > 0.5f);
                    if (between && (y / 16) % 2 == 0) {
                        if (dist < 1.2f) {
                            c = mix(t.line, 0xffffffff, 0.35f);
                        } else if (dist < 5f) {
                            c = mix(c, t.line, 0.45f * (1f - (dist - 1.2f) / 3.8f));
                        }
                    }
                }
                // The edges: a hot line fading inwards.
                int e = Math.min(x, n - 1 - x);
                if (e < 2) {
                    c = mix(t.line, 0xffffffff, 0.4f);
                } else if (e < edge + 4) {
                    c = mix(c, t.line, 0.6f * (1f - (e - 2) / (float) (edge + 2)));
                }
                px[y * n + x] = c;
            }
        }
        return px;
    }

    /// A tall strip of sky, `skyTop` overhead fading to `skyHorizon`, with
    /// the horizon band brightest just above the ground.
    static int[] skyTexture(Theme t, int h) {
        int[] px = new int[4 * h];
        for (int y = 0; y < h; y++) {
            float k = y / (float) (h - 1);
            // Rows run top to bottom; the lower half of the dome is under
            // the ground, so the horizon is the middle row.
            float up = Math.max(0f, 1f - k * 2f);
            int c = mix(t.skyHorizon, t.skyTop, (float) Math.sqrt(up));
            for (int x = 0; x < 4; x++) {
                px[y * 4 + x] = c;
            }
        }
        return px;
    }

    /// A tower face: the theme's wall with rows of windows, some lit, the
    /// whole of it pulled `haze` of the way to the horizon colour so far
    /// towers fade into the sky.
    static int[] towerTexture(Theme t, float haze) {
        int n = 64;
        int[] px = new int[n * n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                // A soft vertical gradient: lighter up top, where the sky is.
                int c = shade(t.tower, 0.8f + 0.35f * (1f - y / (float) n));
                boolean win = x % 6 > 1 && y % 6 > 2;
                if (win) {
                    int cell = (x / 6) * 7 + (y / 6) * 13;
                    int r = (int) ((cell * 2654435761L) >>> 7 & 7);
                    if (r < 2) {
                        c = mix(t.window, 0xffffffff, 0.35f);
                    } else if (r < 3) {
                        c = mix(t.window, t.tower, 0.5f);
                    } else {
                        c = shade(t.tower, 0.6f);
                    }
                }
                // A vertical accent stripe on the corner.
                if (x < 2) {
                    c = mix(c, t.rail, 0.5f);
                }
                px[y * n + x] = mix(c, t.skyHorizon, haze);
            }
        }
        return px;
    }

    /// A vertical ramp of the horizon colour, opaque at the bottom and clear
    /// at the top: the haze that sits over the far city.
    static int[] hazeTexture(Theme t) {
        int h = 64;
        int[] px = new int[4 * h];
        for (int y = 0; y < h; y++) {
            float k = y / (float) (h - 1);
            float a = k * k * (3f - 2f * k);
            int c = (t.skyHorizon & 0x00ffffff) | ((int) (a * 235f) << 24);
            for (int x = 0; x < 4; x++) {
                px[y * 4 + x] = c;
            }
        }
        return px;
    }

    /// A holographic barrier panel: bright diagonal bars over a translucent
    /// field, with scan lines and a lit border.
    static int[] holoTexture(int a, int b) {
        int n = 64;
        int[] px = new int[n * n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                boolean bar = ((x + y) / 10) % 2 == 0;
                int c = bar ? a : b;
                if (y % 4 == 0) {
                    c = shade(c, 0.7f);
                }
                if (x < 3 || x >= n - 3 || y < 3 || y >= n - 3) {
                    c = mix(a, 0xffffffff, 0.5f);
                }
                px[y * n + x] = c;
            }
        }
        return px;
    }

    /// A cargo pod's side: a dark band of lit windows between panel lines.
    static int[] podTexture(int body, int light) {
        int n = 64;
        int[] px = new int[n * n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int c = shade(body, 0.85f + 0.3f * (1f - y / (float) n));
                if (y > 14 && y < 26) {
                    c = x % 12 < 9 ? mix(light, 0xffffffff, 0.25f) : shade(body, 0.4f);
                } else if (y == 40 || y == 41) {
                    c = light;
                } else if (x % 32 == 0) {
                    c = shade(c, 0.6f);
                }
                px[y * n + x] = c;
            }
        }
        return px;
    }

    /// Diagonal hazard stripes, for the barriers a runner slides under.
    static int[] hazardTexture(int a, int b) {
        int n = 32;
        int[] px = new int[n * n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                px[y * n + x] = ((x + y) / 8) % 2 == 0 ? a : b;
            }
        }
        return px;
    }

    static int shade(int argb, float k) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xff) * k));
        int g = Math.min(255, (int) (((argb >> 8) & 0xff) * k));
        int b = Math.min(255, (int) ((argb & 0xff) * k));
        return (argb & 0xff000000) | (r << 16) | (g << 8) | b;
    }

    static int mix(int a, int b, float k) {
        return Outfit.mix(a, b, k);
    }
}
