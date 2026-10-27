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
package com.codenameone.examples.reel.player;

import com.codename1.io.Util;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import com.codenameone.examples.reel.ui.Art;

/// The title's art behind the player until the first frame arrives: what
/// the controls sit on while the stream opens, and what stays when it cannot.
/// Scaled to cover the screen, cropped rather than letterboxed, and dimmed so
/// the controls over it read.
final class Poster extends Component {
    private Image image;
    private Image scaled;

    Poster(String path) {
        setUIID("RlPlayerPoster");
        String url = Art.url(path);
        if (url != null) {
            Util.downloadImageToCache(url, img -> {
                image = img;
                scaled = null;
                repaint();
            }, (sender, err, code, message) -> { });
        }
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(1, 1);
    }

    @Override
    public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (image == null || w <= 0 || h <= 0) {
            return;
        }
        if (scaled == null || scaled.getWidth() != w || scaled.getHeight() != h) {
            scaled = image.fill(w, h);
        }
        g.drawImage(scaled, getX(), getY());
        int alpha = g.getAlpha();
        g.setColor(0x000000);
        g.setAlpha(110);
        g.fillRect(getX(), getY(), w, h);
        g.setAlpha(alpha);
    }
}
