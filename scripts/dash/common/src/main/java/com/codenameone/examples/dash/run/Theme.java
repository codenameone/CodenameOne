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

/// The look a level's `theme` names: sky, light, ground, lane stripes and
/// the scenery along the track. A level author picks one in the editor; a
/// new theme is a new entry in [#of].
public final class Theme {
    public final String id;
    public final int sky;
    public final int ambient;
    public final int ground;
    public final int lane;
    public final int stripe;
    public final String[] props;

    private Theme(String id, int sky, int ambient, int ground, int lane, int stripe, String... props) {
        this.id = id;
        this.sky = sky;
        this.ambient = ambient;
        this.ground = ground;
        this.lane = lane;
        this.stripe = stripe;
        this.props = props;
    }

    /// The themes an editor offers, in order.
    public static final String[] IDS = {"meadow", "neon", "dusk"};

    public static Theme of(String id) {
        if ("neon".equals(id)) {
            return new Theme("neon", 0xff120a2a, 0xff5a4a8a, 0xff1a1236, 0xff2a2050, 0xff38f0ff, "rock", "rock", "hill");
        }
        if ("dusk".equals(id)) {
            return new Theme("dusk", 0xff3a2a48, 0xff8a7a8a, 0xff4a3a32, 0xff6a5a50, 0xffffc070, "trees", "rock", "tree");
        }
        return new Theme("meadow", 0xff8ec8f0, 0xff8a8f9a, 0xff5c9a48, 0xffb89a70, 0xfff4ecd8, "tree", "trees", "rock", "hill");
    }

    /// A 64x64 tile of the track: `lanes` lanes of the lane colour with
    /// stripes between them and a faint cross band, so motion reads.
    static int[] laneTexture(Theme t, int lanes) {
        int n = 64;
        int[] px = new int[n * n];
        int edge = 2;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int c = t.lane;
                int laneW = n / Math.max(1, lanes);
                boolean divider = lanes > 1 && x % laneW < 1 && x > edge && x < n - edge;
                if (x < edge || x >= n - edge || divider && (y / 8) % 2 == 0) {
                    c = t.stripe;
                } else if (y < 2) {
                    c = shade(t.lane, 0.85f);
                }
                px[y * n + x] = c;
            }
        }
        return px;
    }

    static int shade(int argb, float k) {
        int r = (int) (((argb >> 16) & 0xff) * k);
        int g = (int) (((argb >> 8) & 0xff) * k);
        int b = (int) ((argb & 0xff) * k);
        return (argb & 0xff000000) | (r << 16) | (g << 8) | b;
    }
}
