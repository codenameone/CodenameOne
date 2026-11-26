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
package com.codenameone.examples.bazaar.pos;

import com.codename1.ui.CN;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.geom.GeneralPath;
import com.codenameone.examples.bazaar.ui.ProductArt;

/// A register tile's photo, clipped to the card's rounded top corners. A
/// rounded border does not clip what is painted over it, so without this the
/// square corners of the photo poke out of the card.
final class TileArt extends Label {
    private final GeneralPath path = new GeneralPath();

    /// Rounds the bottom corners too: a picture standing on its own (the
    /// variant picker's), not the top of a card.
    private boolean round;

    private TileArt() {
        super("", "PosTileArt");
    }

    /// The art of `id` rounded on all four corners, `side` pixels square.
    static Label rounded(String id, String title, String photoId, int side) {
        TileArt art = (TileArt) create(id, title, photoId, side, side);
        art.round = true;
        art.setUIID("PosVpPhoto");
        return art;
    }

    /// The art of `id` (its photo once it arrives), `width` by `height` pixels.
    static Label create(String id, String title, String photoId, int width, int height) {
        TileArt art = new TileArt();
        art.setIcon(ProductArt.art(id, title, "", width, height));
        if (photoId != null && photoId.length() > 0) {
            ProductArt.photo(art, photoId, width, height);
        }
        return art;
    }

    @Override
    public void paint(Graphics g) {
        if (!g.isShapeClipSupported()) {
            super.paint(g);
            return;
        }
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        // The card's radius (--radius), less the one-pixel border the photo
        // sits inside.
        float r = Math.max(0, CN.convertToPixels(2.4f) - 1);
        path.reset();
        path.moveTo(x, round ? y + h - r : y + h);
        path.lineTo(x, y + r);
        path.quadTo(x, y, x + r, y);
        path.lineTo(x + w - r, y);
        path.quadTo(x + w, y, x + w, y + r);
        if (round) {
            path.lineTo(x + w, y + h - r);
            path.quadTo(x + w, y + h, x + w - r, y + h);
            path.lineTo(x + r, y + h);
            path.quadTo(x, y + h, x, y + h - r);
        } else {
            path.lineTo(x + w, y + h);
        }
        path.closePath();
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        g.pushClip();
        g.setClip(path);
        g.clipRect(cx, cy, cw, ch);
        super.paint(g);
        g.popClip();
    }
}
