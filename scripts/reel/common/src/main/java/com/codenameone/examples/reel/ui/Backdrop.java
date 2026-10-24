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
package com.codenameone.examples.reel.ui;

import com.codename1.io.Util;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.UIManager;

/// A picture that covers the whole width of the screen, edge to edge, with
/// what is laid over it -- a title, its facts, its buttons -- kept legible by a
/// shade that deepens towards the bottom and melts into the page under it.
///
/// The picture is cropped, never letterboxed: it is scaled until it covers the
/// box and centred. The box is as tall as the picture's own proportion makes
/// it at the screen's width, but no more than a share of the screen's height,
/// so that on a desktop window the rows under it still show.
public final class Backdrop extends Container {
    /// Height over width of the art, which is 16:9 video.
    private static final float RATIO = 9f / 16f;

    private final float ratio;
    private final float most;
    private Image image;
    private Image scaled;
    private Image fade;
    private int fadeColor;
    private Image side;
    private int sideColor;

    /// @param path the art, as the server names it; null shows the shade alone
    /// @param tallness how much taller than 16:9 the box is (1 for 16:9 itself)
    /// @param mostOfScreen the largest share of the screen's height it takes
    public Backdrop(String path, float tallness, float mostOfScreen) {
        super(new BorderLayout());
        setUIID("RlBackdrop");
        this.ratio = RATIO * tallness;
        this.most = mostOfScreen;
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
        int w = CN.getDisplayWidth();
        int h = Math.min((int) (w * ratio), (int) (CN.getDisplayHeight() * most));
        return new Dimension(w, Math.max(h, super.calcPreferredSize().getHeight()));
    }

    @Override
    public void paintBackground(Graphics g) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int ground = UIManager.getInstance().getComponentStyle("SvForm").getBgColor();
        g.setColor(ground);
        g.fillRect(x, y, w, h);
        if (image != null && w > 0 && h > 0) {
            if (scaled == null || scaled.getWidth() < w || scaled.getHeight() < h) {
                float s = Math.max(w / (float) image.getWidth(), h / (float) image.getHeight());
                scaled = image.scaled(Math.max(w, (int) (image.getWidth() * s)),
                        Math.max(h, (int) (image.getHeight() * s)));
            }
            g.drawImage(scaled, x + (w - scaled.getWidth()) / 2, y + (h - scaled.getHeight()) / 3);
        }
        // The shade, so the picture runs into the rows below rather than
        // ending at a line, and white text stays readable over a bright sky:
        // light at the top, deepening through the middle where the title sits,
        // the page's own colour at the bottom. A gradient one pixel wide,
        // stretched, so every row has its own alpha on every port.
        if (h > 0 && w > 0) {
            if (fade == null || fade.getHeight() != h || fadeColor != ground) {
                fade = gradient(ground, h, true);
                fadeColor = ground;
            }
            g.drawImage(fade, x, y, w, h);
            if (w > CN.convertToPixels(Layouts.WIDE_MM)) {
                // On a wide screen the text sits on the left over the picture:
                // shade that side as well.
                if (side == null || sideColor != ground) {
                    side = gradient(ground, 256, false);
                    sideColor = ground;
                }
                g.drawImage(side, x, y, w * 60 / 100, h);
            }
        }
    }

    /// A strip of `color` whose alpha follows the shade: `vertical` is the
    /// bottom fade (one pixel wide, `length` tall), otherwise the side fade
    /// (`length` wide, one pixel tall), dark on the left.
    private static Image gradient(int color, int length, boolean vertical) {
        int[] argb = new int[length];
        int rgb = color & 0xffffff;
        for (int iter = 0; iter < length; iter++) {
            float t = iter / (float) Math.max(1, length - 1);
            float a;
            if (vertical) {
                // A veil over the whole picture, a little more under the
                // toolbar, then an ease into solid from a third of the way down.
                float top = t < 0.18f ? 0.32f * (1f - t / 0.18f) : 0f;
                float u = t < 0.33f ? 0f : (t - 0.33f) / 0.67f;
                float ease = u * u * (3f - 2f * u);
                a = Math.min(1f, 0.18f + top + 0.82f * ease);
            } else {
                float u = 1f - t;
                a = 0.78f * u * u * (3f - 2f * u);
            }
            argb[iter] = ((int) (a * 255) << 24) | rgb;
        }
        return vertical ? Image.createImage(argb, 1, length) : Image.createImage(argb, length, 1);
    }
}
