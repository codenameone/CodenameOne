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
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/// Paints a CSS `border-image`: the source image cut into nine slices and
/// laid over the border area.
///
/// The image is cut by the four slice offsets. The border image area is the
/// border box, and the thickness of each edge of the target grid is that
/// side's border width (`border-image-width: 1`, the CSS initial value).
/// The four corners are scaled to fit their cell. The edges and, with
/// `fill`, the middle are stretched, repeated or rounded:
///
/// - `STRETCH` scales the slice to its cell.
/// - `REPEAT` scales the slice to the thickness of its cell, keeping its
///   aspect ratio, and tiles it along the cell, centred, cutting off the
///   tiles at both ends.
/// - `ROUND` does the same and then resizes the tile along the cell so a
///   whole number fits.
///
/// `REPEAT` and `ROUND` are best effort: the middle slice takes its tile
/// width from the top edge (or the bottom one, or its own) and its tile
/// height from the left edge (or the right one, or its own), which is the
/// CSS rule, but the CSS `space` keyword and per-axis repeat modes are not
/// modelled.
///
/// Slice offsets are rounded to whole image px. As CSS requires, when the
/// left and right slices together cover the image width there is nothing
/// left for the top and bottom edges or the middle, and likewise
/// vertically. The border radius does not clip a border image.
public final class BorderImagePainter {
    private BorderImagePainter() {
    }

    /// Paints the border image.
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics of the target image
    ///
    /// - `bi`: the border image
    ///
    /// - `box`: the border box in image coordinates
    ///
    /// - `widths`: the border widths for top, right, bottom, left
    public static void paint(Graphics2D g, BorderImage bi, Rectangle2D box, double[] widths) {
        BufferedImage img = bi.image;
        int iw = img.getWidth();
        int ih = img.getHeight();
        int st = clampSlice(bi.sliceTop, ih);
        int sb = clampSlice(bi.sliceBottom, ih);
        int sl = clampSlice(bi.sliceLeft, iw);
        int sr = clampSlice(bi.sliceRight, iw);
        int midW = iw - sl - sr;
        int midH = ih - st - sb;

        double t = widths[RoundedBox.TOP];
        double r = widths[RoundedBox.RIGHT];
        double b = widths[RoundedBox.BOTTOM];
        double l = widths[RoundedBox.LEFT];
        double x0 = box.getX();
        double y0 = box.getY();
        double x3 = box.getMaxX();
        double y3 = box.getMaxY();
        double x1 = x0 + l;
        double x2 = x3 - r;
        double y1 = y0 + t;
        double y2 = y3 - b;

        Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Shape oldClip = g.getClip();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        try {
            BorderImage.Mode mode = bi.repeat == null ? BorderImage.Mode.STRETCH : bi.repeat;
            // Corners.
            piece(g, img, 0, 0, sl, st, x0, y0, l, t);
            piece(g, img, iw - sr, 0, sr, st, x2, y0, r, t);
            piece(g, img, iw - sr, ih - sb, sr, sb, x2, y2, r, b);
            piece(g, img, 0, ih - sb, sl, sb, x0, y2, l, b);
            // Edges: the tile's free dimension follows from the thickness.
            if (midW > 0) {
                tiled(g, img, sl, 0, midW, st, x1, y0, x2 - x1, t,
                        st > 0 ? midW * t / st : 0, t, mode);
                tiled(g, img, sl, ih - sb, midW, sb, x1, y2, x2 - x1, b,
                        sb > 0 ? midW * b / sb : 0, b, mode);
            }
            if (midH > 0) {
                tiled(g, img, 0, st, sl, midH, x0, y1, l, y2 - y1,
                        l, sl > 0 ? midH * l / sl : 0, mode);
                tiled(g, img, iw - sr, st, sr, midH, x2, y1, r, y2 - y1,
                        r, sr > 0 ? midH * r / sr : 0, mode);
            }
            if (bi.fill && midW > 0 && midH > 0) {
                double sx = st > 0 && t > 0 ? t / st : (sb > 0 && b > 0 ? b / sb : 1);
                double sy = sl > 0 && l > 0 ? l / sl : (sr > 0 && r > 0 ? r / sr : 1);
                tiled(g, img, sl, st, midW, midH, x1, y1, x2 - x1, y2 - y1, midW * sx, midH * sy, mode);
            }
        } finally {
            g.setClip(oldClip);
            if (oldHint != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
            }
        }
    }

    private static int clampSlice(double v, int max) {
        if (!(v > 0)) {
            return 0;
        }
        long i = Math.round(v);
        return i > max ? max : (int) i;
    }

    /// Draws one source rectangle scaled into one target rectangle.
    private static void piece(Graphics2D g, BufferedImage img, int sx, int sy, int sw, int sh,
            double dx, double dy, double dw, double dh) {
        if (sw <= 0 || sh <= 0 || !(dw > 0) || !(dh > 0)) {
            return;
        }
        if (sx < 0 || sy < 0 || sx + sw > img.getWidth() || sy + sh > img.getHeight()) {
            return;
        }
        BufferedImage sub = img.getSubimage(sx, sy, sw, sh);
        if (isWhole(dx) && isWhole(dy) && isWhole(dw) && isWhole(dh)) {
            int ix = (int) Math.round(dx);
            int iy = (int) Math.round(dy);
            g.drawImage(sub, ix, iy, (int) Math.round(dw), (int) Math.round(dh), null);
            return;
        }
        AffineTransform at = new AffineTransform();
        at.translate(dx, dy);
        at.scale(dw / sw, dh / sh);
        g.drawImage(sub, at, null);
    }

    private static boolean isWhole(double v) {
        return Math.abs(v - Math.rint(v)) < 1e-9;
    }

    /// Covers a target rectangle with a slice: stretched, or as centred
    /// tiles of `tileW` by `tileH`.
    private static void tiled(Graphics2D g, BufferedImage img, int sx, int sy, int sw, int sh,
            double dx, double dy, double dw, double dh, double tileW, double tileH, BorderImage.Mode mode) {
        if (sw <= 0 || sh <= 0 || !(dw > 0) || !(dh > 0)) {
            return;
        }
        if (mode == BorderImage.Mode.STRETCH || !(tileW > 0) || !(tileH > 0)) {
            piece(g, img, sx, sy, sw, sh, dx, dy, dw, dh);
            return;
        }
        if (mode == BorderImage.Mode.ROUND) {
            tileW = dw / Math.max(1, Math.round(dw / tileW));
            tileH = dh / Math.max(1, Math.round(dh / tileH));
        }
        double startX = dx;
        double startY = dy;
        if (mode == BorderImage.Mode.REPEAT) {
            // Tiles are centred: one tile sits in the middle of the cell and
            // the row is cut off at both ends. ROUND tiles fit exactly and
            // start on the cell edge.
            startX = dx + (dw - tileW) / 2;
            startX -= Math.ceil((startX - dx) / tileW) * tileW;
            startY = dy + (dh - tileH) / 2;
            startY -= Math.ceil((startY - dy) / tileH) * tileH;
        }
        long nx = (long) Math.ceil((dx + dw - startX) / tileW - 1e-9);
        long ny = (long) Math.ceil((dy + dh - startY) / tileH - 1e-9);
        if (nx * ny > 100000) {
            piece(g, img, sx, sy, sw, sh, dx, dy, dw, dh);
            return;
        }
        Shape old = g.getClip();
        g.clip(new Rectangle2D.Double(dx, dy, dw, dh));
        try {
            for (long j = 0; j < ny; j++) {
                for (long i = 0; i < nx; i++) {
                    piece(g, img, sx, sy, sw, sh, startX + i * tileW, startY + j * tileH, tileW, tileH);
                }
            }
        } finally {
            g.setClip(old);
        }
    }
}
