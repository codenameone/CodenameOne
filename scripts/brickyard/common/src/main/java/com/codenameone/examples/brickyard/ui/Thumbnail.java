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
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;

/// A stand-in thumbnail: a little skyline of blocks whose colours and heights
/// come from the experience id, so each experience keeps the same picture.
public class Thumbnail extends Component {
    private static final int[] COLORS = {0xf0563c, 0xffc43c, 0x3caaff, 0x5ac86e, 0x8a5cf0, 0xff8fc8};
    private final int seed;
    private final float widthMm;

    public Thumbnail(String id, float widthMm) {
        this.seed = id == null ? 0 : id.hashCode();
        this.widthMm = widthMm;
    }

    @Override
    public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(0x222a45);
        g.fillRect(getX(), getY(), w, h);
        int columns = 6;
        int cw = Math.max(1, w / columns);
        int s = seed;
        for (int i = 0; i < columns; i++) {
            s = s * 1103515245 + 12345;
            int rows = 1 + ((s >>> 16) & 3);
            int bh = Math.max(1, h / 6);
            for (int r = 0; r < rows; r++) {
                g.setColor(COLORS[(((s >>> 8) + r) & 0x7fffffff) % COLORS.length]);
                g.fillRect(getX() + i * cw + 1, getY() + h - (r + 1) * bh, cw - 2, bh - 1);
            }
        }
    }

    @Override
    protected Dimension calcPreferredSize() {
        int w = CN.convertToPixels(widthMm);
        return new Dimension(w, w * 9 / 16);
    }
}
