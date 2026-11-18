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
package com.codenameone.examples.brickyard.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;

/// The Brickyard mark: three toy bricks stacked in the palette's red, blue
/// and yellow, drawn in isometric projection with studs on top, over the
/// two-tone wordmark. Painted rather than shipped as an image so it is sharp
/// at every density and follows the palette.
public class BrandMark extends Component {
    private static final int RED = 0xf0563c;
    private static final int BLUE = 0x3caaff;
    private static final int YELLOW = 0xffc43c;
    private static final int WHITE = 0xffffff;

    private final boolean wordmark;
    private final float markMm;
    private Font font;

    /// `markMm` is the width of the brick stack; `wordmark` adds the name under it.
    public BrandMark(float markMm, boolean wordmark) {
        this.markMm = markMm;
        this.wordmark = wordmark;
        setUIID("Container");
        setFocusable(false);
    }

    private Font font() {
        if (font == null) {
            int px = CN.convertToPixels(markMm * 0.42f);
            font = Font.isNativeFontSchemeSupported()
                    ? Font.createTrueTypeFont("native:MainBlack", "native:MainBlack").derive((float) px, Font.STYLE_BOLD)
                    : Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
        }
        return font;
    }

    @Override
    protected Dimension calcPreferredSize() {
        int w = CN.convertToPixels(markMm);
        int h = (int) (w * 0.95f);
        if (wordmark) {
            Font f = font();
            w = Math.max(w, f.stringWidth("BRICKYARD") + CN.convertToPixels(2));
            h += CN.convertToPixels(markMm * 0.12f) + f.getHeight();
        }
        return new Dimension(w, h);
    }

    @Override
    public void paint(Graphics g) {
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        int markW = CN.convertToPixels(markMm);
        int cx = getX() + getWidth() / 2;
        // Half the width of one brick's top face; a brick is about 0.36 of the mark tall.
        int hw = markW * 30 / 100;
        int th = markW * 18 / 100;
        int top = getY() + markW * 6 / 100;
        // Bottom row: red to the left, blue to the right; yellow sits across both.
        int baseY = top + markW * 48 / 100;
        brick(g, cx - hw / 2, baseY, hw, th, RED);
        brick(g, cx + hw / 2, baseY + hw / 4, hw, th, BLUE);
        brick(g, cx, baseY - th - hw / 8, hw, th, YELLOW);
        if (wordmark) {
            Font f = font();
            g.setFont(f);
            int y = getY() + (int) (markW * 0.95f) + CN.convertToPixels(markMm * 0.12f);
            int wBrick = f.stringWidth("BRICK");
            int x = cx - f.stringWidth("BRICKYARD") / 2;
            g.setColor(YELLOW);
            g.drawString("BRICK", x, y);
            g.setColor(WHITE);
            g.drawString("YARD", x + wBrick, y);
        }
        g.setAntiAliased(aa);
    }

    /// One isometric brick whose top face is centred on (`cx`, `cy`): a
    /// rhombus `2 * hw` wide, two shaded sides `th` tall and a 2x2 of studs.
    private static void brick(Graphics g, int cx, int cy, int hw, int th, int color) {
        int hh = hw / 2;
        // Left side, a little darker than the top.
        g.setColor(shade(color, 78));
        g.fillPolygon(new int[] {cx - hw, cx, cx, cx - hw},
                new int[] {cy, cy + hh, cy + hh + th, cy + th}, 4);
        // Right side, darker still.
        g.setColor(shade(color, 58));
        g.fillPolygon(new int[] {cx, cx + hw, cx + hw, cx},
                new int[] {cy + hh, cy, cy + th, cy + hh + th}, 4);
        g.setColor(color);
        g.fillPolygon(new int[] {cx, cx + hw, cx, cx - hw},
                new int[] {cy - hh, cy, cy + hh, cy}, 4);
        // Studs, back to front so the nearer ones overlap.
        int sw = hw * 46 / 100;
        int sh = sw / 2;
        int lift = Math.max(2, th / 4);
        int[][] at = {{0, -hh / 2}, {-hw / 2, 0}, {hw / 2, 0}, {0, hh / 2}};
        for (int[] p : at) {
            int sx = cx + p[0] - sw / 2;
            int sy = cy + p[1] - sh / 2;
            g.setColor(shade(color, 62));
            g.fillArc(sx, sy, sw, sh, 0, 360);
            g.fillRect(sx, sy - lift + sh / 2, sw, lift);
            g.setColor(lighten(color, 18));
            g.fillArc(sx, sy - lift, sw, sh, 0, 360);
        }
    }

    private static int shade(int rgb, int percent) {
        int r = (rgb >> 16 & 0xff) * percent / 100;
        int gr = (rgb >> 8 & 0xff) * percent / 100;
        int b = (rgb & 0xff) * percent / 100;
        return r << 16 | gr << 8 | b;
    }

    private static int lighten(int rgb, int percent) {
        int r = rgb >> 16 & 0xff;
        int gr = rgb >> 8 & 0xff;
        int b = rgb & 0xff;
        r += (255 - r) * percent / 100;
        gr += (255 - gr) * percent / 100;
        b += (255 - b) * percent / 100;
        return r << 16 | gr << 8 | b;
    }
}
