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

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codenameone.examples.skirmish.rules.Arena;

/// The textures the battle paints in code: the field with its river, lanes
/// and bridges, the meadow around it, the selection ring and a white pixel
/// for health bars. The units, towers and scenery are 3D models (see
/// [Models]); the card and hero pictures outside the arena are renders of the
/// same models (see `ui/CardArt`).
// tag::skirmish-art[]
public final class Art {
    /// Ground texture pixels per arena tile.
    public static final int TILE_PX = 32;

    private static Image pixel;

    private Art() {
    }

    /// A white pixel, tinted and stretched for bars and overlays.
    public static synchronized Image pixel() {
        if (pixel == null) {
            pixel = Image.createImage(4, 4, 0xffffffff);
        }
        return pixel;
    }

    /// The field's ground texture, `TILE_PX` pixels per tile, laid out in rules
    /// coordinates (row 0 is player 1's back line). Checkered turf, worn lanes,
    /// sandy banks and the river; the bridges are 3D models on top of it.
    public static Image field(Arena a) {
        int t = TILE_PX;
        int cols = a.width() / Arena.TILE;
        int rows = a.height() / Arena.TILE;
        int w = cols * t;
        int h = rows * t;
        boolean snow = "frost".equals(a.theme());
        boolean sand = "desert".equals(a.theme());
        int turf0 = snow ? 0xdfe8ee : sand ? 0xd9b878 : 0x5f9e47;
        int turf1 = snow ? 0xd2dde5 : sand ? 0xcfac6c : 0x578f41;
        Image img = Image.createImage(w, h, 0xff000000 | turf0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        for (int ty = 0; ty < rows; ty++) {
            for (int tx = 0; tx < cols; tx++) {
                g.setColor((tx + ty) % 2 == 0 ? turf0 : turf1);
                g.fillRect(tx * t, ty * t, t, t);
            }
        }
        // Worn lanes, and the cross paths behind the towers.
        g.setColor(snow ? 0xb9c6cf : 0xc9b27a);
        g.setAlpha(150);
        int lanes = a.laneCount();
        for (int lane = 0; lane < lanes; lane++) {
            int lx = a.laneX(lane) * t / Arena.TILE;
            g.fillRoundRect(lx - t * 3 / 4, t * 5, t * 3 / 2, h - t * 10, t, t);
        }
        int left = a.laneX(0) * t / Arena.TILE;
        int span = (a.laneX(lanes - 1) - a.laneX(0)) * t / Arena.TILE;
        if (span > 0) {
            g.fillRoundRect(left, t * 5 / 2, span, t, t, t);
            g.fillRoundRect(left, h - t * 7 / 2, span, t, t, t);
        }
        g.setAlpha(255);
        // Banks, then the water with a lighter centre.
        int r1 = a.riverTop() * t / Arena.TILE;
        int r2 = a.riverBottom() * t / Arena.TILE;
        g.setColor(0xd8c48e);
        g.fillRect(0, r1 - t / 4, w, r2 - r1 + t / 2);
        int deep = snow ? 0x7fb6d8 : 0x2f7fc8;
        int light = snow ? 0xb8dcf0 : 0x46a6e8;
        g.fillLinearGradient(deep, light, 0, r1, w, (r2 - r1) / 2, false);
        g.fillLinearGradient(light, deep, 0, r1 + (r2 - r1) / 2, w, (r2 - r1) / 2, false);
        g.setColor(0xffffff);
        g.setAlpha(60);
        for (int i = 0; i < cols; i++) {
            int wx = (i * 37) % w;
            g.fillRoundRect(wx, r1 + ((i * 13) % Math.max(1, r2 - r1 - 6)), t, 3, 3, 3);
        }
        g.setAlpha(255);
        // Plank bridges over the river, one per lane.
        for (int lane = 0; lane < lanes; lane++) {
            int bx = (a.laneX(lane) - a.bridgeHalfWidth()) * t / Arena.TILE;
            int bw = 2 * a.bridgeHalfWidth() * t / Arena.TILE;
            g.setColor(0x5a3a1c);
            g.fillRect(bx - 3, r1 - t / 3, bw + 6, r2 - r1 + t * 2 / 3);
            for (int py = r1 - t / 3; py < r2 + t / 3; py += 8) {
                g.setColor((py / 8) % 2 == 0 ? 0xa8743e : 0x9a6835);
                g.fillRect(bx, py, bw, 7);
            }
        }
        // A low stone border around the play area.
        g.setColor(0x8f8a7c);
        g.drawRect(0, 0, w - 1, h - 1);
        g.drawRect(1, 1, w - 3, h - 3);
        return img;
    }

    /// The ground beyond the field.
    public static Image meadow() {
        int s = 128;
        Image img = Image.createImage(s, s, 0xff4f8f3e);
        Graphics g = img.getGraphics();
        for (int i = 0; i < 260; i++) {
            int x = (i * 53) % s;
            int y = (i * 97 + i / 3) % s;
            g.setColor(i % 3 == 0 ? 0x5a9c47 : 0x467f36);
            g.fillRect(x, y, 2 + i % 3, 2);
        }
        return img;
    }

    /// A soft-edged ring for the circle under a unit's feet, white so a
    /// material color tints it.
    public static int[] ring(int size) {
        int[] argb = new int[size * size];
        float r = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f - r) / r;
                float dy = (y + 0.5f - r) / r;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a;
                if (d > 1f) {
                    a = 0f;
                } else if (d > 0.78f) {
                    a = 1f - Math.abs(d - 0.89f) / 0.11f;
                } else {
                    a = 0.18f;
                }
                int c = (int) (Math.max(0f, a) * 255f);
                argb[y * size + x] = (c << 24) | 0xffffff;
            }
        }
        return argb;
    }
}

// end::skirmish-art[]
