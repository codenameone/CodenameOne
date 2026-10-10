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
package com.codename1.designer.css.raster;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/// Paints a CSS background image: `background-size`, `background-position`
/// and `background-repeat`.
///
/// The image is sized and positioned against the positioning area, which
/// the rasterizer makes the padding box (`background-origin: padding-box`,
/// the CSS initial value), and tiled over the painting area, the border box
/// (`background-clip: border-box`). The rounded clip is applied by the
/// caller through a coverage mask; this class only clips to the painting
/// rectangle.
///
/// Scaling is bicubic. An image reduced to less than half its size is first
/// halved repeatedly with bilinear filtering, because a single bicubic step
/// samples too few source pixels and aliases.
public final class BackgroundImagePainter {
    /// Above this many tiles the image is tiled with a `TexturePaint`
    /// instead of being drawn tile by tile.
    private static final long MAX_DRAWN_TILES = 4096;

    private BackgroundImagePainter() {
    }

    /// The size of one tile for a background image in a positioning area.
    ///
    /// #### Returns
    ///
    /// `{width, height}` in px
    public static double[] tileSize(BackgroundImage bg, double areaW, double areaH) {
        return tileSize(bg.image.getWidth(), bg.image.getHeight(), bg, areaW, areaH);
    }

    /// The size of one tile of an image whose own size is `iw` by `ih`,
    /// which need not be the size of the pixels `bg` holds.
    public static double[] tileSize(double iw, double ih, BackgroundImage bg, double areaW, double areaH) {
        switch (bg.size) {
            case COVER: {
                double s = Math.max(areaW / iw, areaH / ih);
                return new double[] {iw * s, ih * s};
            }
            case CONTAIN: {
                double s = Math.min(areaW / iw, areaH / ih);
                return new double[] {iw * s, ih * s};
            }
            case EXPLICIT: {
                boolean autoW = bg.sizeW < 0;
                boolean autoH = bg.sizeH < 0;
                if (autoW && autoH) {
                    return new double[] {iw, ih};
                }
                if (autoW) {
                    return new double[] {iw * bg.sizeH / ih, bg.sizeH};
                }
                if (autoH) {
                    return new double[] {bg.sizeW, ih * bg.sizeW / iw};
                }
                return new double[] {bg.sizeW, bg.sizeH};
            }
            default:
                return new double[] {iw, ih};
        }
    }

    /// Paints the image into a transparent layer the size of the output.
    ///
    /// #### Parameters
    ///
    /// - `bg`: the background image
    ///
    /// - `w`: layer width
    ///
    /// - `h`: layer height
    ///
    /// - `area`: the positioning area (padding box) in layer coordinates
    ///
    /// - `clip`: the painting area (border box bounds) in layer coordinates
    ///
    /// #### Returns
    ///
    /// `w * h` non-premultiplied ARGB values, or `null` when nothing is painted
    public static int[] paint(BackgroundImage bg, int w, int h, Rectangle2D area, Rectangle2D clip) {
        double[] ts = tileSize(bg, area.getWidth(), area.getHeight());
        double tw = ts[0];
        double th = ts[1];
        if (!(tw > 0) || !(th > 0) || Double.isInfinite(tw) || Double.isInfinite(th)) {
            return null;
        }
        double px = area.getX() + (bg.posXPercent ? (area.getWidth() - tw) * bg.posX / 100 : bg.posX);
        double py = area.getY() + (bg.posYPercent ? (area.getHeight() - th) * bg.posY / 100 : bg.posY);
        boolean repeatX = bg.repeat == BackgroundImage.Repeat.REPEAT || bg.repeat == BackgroundImage.Repeat.REPEAT_X;
        boolean repeatY = bg.repeat == BackgroundImage.Repeat.REPEAT || bg.repeat == BackgroundImage.Repeat.REPEAT_Y;

        double x0 = px;
        long nx = 1;
        if (repeatX) {
            x0 = px - Math.ceil((px - clip.getMinX()) / tw) * tw;
            nx = (long) Math.ceil((clip.getMaxX() - x0) / tw);
        }
        double y0 = py;
        long ny = 1;
        if (repeatY) {
            y0 = py - Math.ceil((py - clip.getMinY()) / th) * th;
            ny = (long) Math.ceil((clip.getMaxY() - y0) / th);
        }
        if (nx <= 0 || ny <= 0) {
            return null;
        }

        BufferedImage src = reduce(bg.image, tw, th);
        BufferedImage layer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = layer.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.clip(clip);
            // Divided, not multiplied: two counts large enough wrap around
            // to a small product, and the loops below would then never end.
            if (nx > MAX_DRAWN_TILES / ny) {
                g.setPaint(new TexturePaint(src, new Rectangle2D.Double(x0, y0, tw, th)));
                g.fill(new Rectangle2D.Double(x0, y0, nx * tw, ny * th));
            } else {
                double sx = tw / src.getWidth();
                double sy = th / src.getHeight();
                for (long j = 0; j < ny; j++) {
                    for (long i = 0; i < nx; i++) {
                        AffineTransform at = new AffineTransform();
                        at.translate(x0 + i * tw, y0 + j * th);
                        at.scale(sx, sy);
                        g.drawImage(src, at, null);
                    }
                }
            }
        } finally {
            g.dispose();
        }
        return layer.getRGB(0, 0, w, h, null, 0, w);
    }

    /// Halves an image until it is less than twice the target size.
    private static BufferedImage reduce(BufferedImage img, double tw, double th) {
        BufferedImage cur = img;
        while (cur.getWidth() >= tw * 2 && cur.getHeight() >= th * 2
                && cur.getWidth() >= 2 && cur.getHeight() >= 2) {
            int nw = cur.getWidth() / 2;
            int nh = cur.getHeight() / 2;
            BufferedImage half = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = half.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(cur, 0, 0, nw, nh, null);
            } finally {
                g.dispose();
            }
            cur = half;
        }
        return cur;
    }
}
