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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/// Paints one CSS box into an image with plain Java2D. It needs no display,
/// no native toolkit and no browser, and runs with `java.awt.headless=true`.
///
/// The layers are painted bottom to top:
///
/// 1. the outer shadow, which is not drawn under the border box;
/// 2. the background colour, then the gradient and the background image,
///    each clipped to the rounded border box (`background-clip:
///    border-box`). A style that sets both a gradient and an image gets
///    both, the image on top unless [BoxStyle#isGradientOverImage()];
/// 3. the inset shadow, clipped to the rounded padding box;
/// 4. the borders, or the border image when there is one. As in CSS, a
///    border image replaces the border styles instead of being drawn over
///    them.
///
/// The box keeps its fractional size and position; only the image size is
/// truncated to whole pixels. Instances hold no state and may be shared
/// between threads.
public final class CssBoxRasterizer {
    /// The most pixels an output image may have. A larger request is a
    /// broken length, not a box.
    private static final long MAX_PIXELS = 64L * 1024 * 1024;

    public CssBoxRasterizer() {
    }

    /// Paints a box.
    ///
    /// #### Parameters
    ///
    /// - `style`: the box to paint
    ///
    /// #### Returns
    ///
    /// a `TYPE_INT_ARGB` image of
    /// `(int) (borderBoxWidth + padLeft + padRight)` by
    /// `(int) (borderBoxHeight + padTop + padBottom)` pixels, each at least
    /// 1, with the top left of the border box at `(padLeft, padTop)`. Never
    /// `null`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: naming the offending field, when the
    ///   style is `null` or holds a value that cannot be painted
    public BufferedImage rasterize(BoxStyle style) {
        validate(style);
        double boxW = style.getBorderBoxWidth();
        double boxH = style.getBorderBoxHeight();
        int padLeft = style.getPadLeft();
        int padTop = style.getPadTop();
        double fullW = boxW + padLeft + style.getPadRight();
        double fullH = boxH + padTop + style.getPadBottom();
        if (fullW * fullH > MAX_PIXELS) {
            throw new IllegalArgumentException("borderBoxWidth x borderBoxHeight plus padding is "
                    + fullW + " x " + fullH + " px, more than this rasterizer paints");
        }
        int w = Math.max(1, (int) fullW);
        int h = Math.max(1, (int) fullH);

        BorderSide[] sides = {style.getTop(), style.getRight(), style.getBottom(), style.getLeft()};
        double[] widths = BorderPainter.effectiveWidths(sides, boxW, boxH);
        RoundedBox borderBox = new RoundedBox(padLeft, padTop, boxW, boxH, style.getRadii());
        RoundedBox paddingBox = borderBox.inset(widths[RoundedBox.TOP], widths[RoundedBox.RIGHT],
                widths[RoundedBox.BOTTOM], widths[RoundedBox.LEFT]);

        int[] px = new int[w * h];
        float[] borderCoverage = Pixels.coverage(borderBox.toPath(), w, h, 0, 0);
        Shadow shadow = style.getShadow();

        if (shadow != null && !shadow.inset) {
            BoxShadowPainter.paintOuter(px, w, h, borderBox, borderCoverage, shadow);
        }

        Pixels.fill(px, style.getBackgroundColor(), borderCoverage);

        // The two background layers go down bottom first. The image is the
        // upper one unless the style says the gradient was written first.
        if (style.isGradientOverImage()) {
            paintBackgroundImage(px, w, h, style, paddingBox, borderCoverage);
            paintGradient(px, w, h, style, paddingBox, borderCoverage);
        } else {
            paintGradient(px, w, h, style, paddingBox, borderCoverage);
            paintBackgroundImage(px, w, h, style, paddingBox, borderCoverage);
        }

        if (shadow != null && shadow.inset) {
            BoxShadowPainter.paintInset(px, w, h, paddingBox, shadow);
        }

        BorderImage borderImage = style.getBorderImage();
        if (borderImage == null) {
            BorderPainter.paint(px, w, h, borderBox, sides, widths);
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, px, 0, w);
        if (borderImage != null) {
            Graphics2D g = out.createGraphics();
            try {
                Pixels.hints(g);
                BorderImagePainter.paint(g, borderImage, new Rectangle2D.Double(padLeft, padTop, boxW, boxH), widths);
            } finally {
                g.dispose();
            }
        }
        return out;
    }

    private static void paintGradient(int[] px, int w, int h, BoxStyle style, RoundedBox paddingBox,
            float[] borderCoverage) {
        GradientSpec gradient = style.getGradient();
        if (gradient == null) {
            return;
        }
        // A gradient is sized and positioned in the padding box, the
        // initial background-origin, and shows through to the border
        // edge, the initial background-clip. A box that is all border has
        // no padding box to measure in and falls back to the border box.
        GradientPainter painter = paddingBox.isEmpty()
                ? new GradientPainter(gradient, style.getPadLeft(), style.getPadTop(),
                        style.getBorderBoxWidth(), style.getBorderBoxHeight())
                : new GradientPainter(gradient, paddingBox.getX(), paddingBox.getY(),
                        paddingBox.getWidth(), paddingBox.getHeight());
        Pixels.layer(px, painter.paint(w, h), borderCoverage);
    }

    private static void paintBackgroundImage(int[] px, int w, int h, BoxStyle style, RoundedBox paddingBox,
            float[] borderCoverage) {
        BackgroundImage bg = style.getBackgroundImage();
        if (bg == null || paddingBox.isEmpty()) {
            return;
        }
        int[] layer = BackgroundImagePainter.paint(bg, w, h,
                new Rectangle2D.Double(paddingBox.getX(), paddingBox.getY(),
                        paddingBox.getWidth(), paddingBox.getHeight()),
                new Rectangle2D.Double(style.getPadLeft(), style.getPadTop(),
                        style.getBorderBoxWidth(), style.getBorderBoxHeight()));
        if (layer != null) {
            Pixels.layer(px, layer, borderCoverage);
        }
    }

    private static void validate(BoxStyle s) {
        if (s == null) {
            throw new IllegalArgumentException("style must not be null");
        }
        positive(s.getBorderBoxWidth(), "borderBoxWidth");
        positive(s.getBorderBoxHeight(), "borderBoxHeight");
        pad(s.getPadTop(), "padTop");
        pad(s.getPadRight(), "padRight");
        pad(s.getPadBottom(), "padBottom");
        pad(s.getPadLeft(), "padLeft");
        side(s.getTop(), "top");
        side(s.getRight(), "right");
        side(s.getBottom(), "bottom");
        side(s.getLeft(), "left");
        double[] radii = s.getRadii();
        if (radii == null || radii.length != 8) {
            throw new IllegalArgumentException("radii must hold 8 values, got "
                    + (radii == null ? "null" : String.valueOf(radii.length)));
        }
        for (int i = 0; i < 8; i++) {
            if (Double.isNaN(radii[i]) || Double.isInfinite(radii[i]) || radii[i] < 0) {
                throw new IllegalArgumentException("radii[" + i + "] must be a length of 0 or more, got " + radii[i]);
            }
        }
        Shadow sh = s.getShadow();
        if (sh != null) {
            finite(sh.offsetX, "shadow.offsetX");
            finite(sh.offsetY, "shadow.offsetY");
            finite(sh.spread, "shadow.spread");
            if (Double.isNaN(sh.blur) || Double.isInfinite(sh.blur) || sh.blur < 0) {
                throw new IllegalArgumentException("shadow.blur must be 0 or more, got " + sh.blur);
            }
        }
        BackgroundImage bg = s.getBackgroundImage();
        if (bg != null) {
            if (bg.image == null) {
                throw new IllegalArgumentException("backgroundImage.image must not be null");
            }
            if (bg.repeat == null) {
                throw new IllegalArgumentException("backgroundImage.repeat must not be null");
            }
            if (bg.size == null) {
                throw new IllegalArgumentException("backgroundImage.size must not be null");
            }
            finite(bg.posX, "backgroundImage.posX");
            finite(bg.posY, "backgroundImage.posY");
            if (bg.size == BackgroundImage.Size.EXPLICIT) {
                finite(bg.sizeW, "backgroundImage.sizeW");
                finite(bg.sizeH, "backgroundImage.sizeH");
            }
        }
        BorderImage bi = s.getBorderImage();
        if (bi != null) {
            if (bi.image == null) {
                throw new IllegalArgumentException("borderImage.image must not be null");
            }
            slice(bi.sliceTop, "borderImage.sliceTop");
            slice(bi.sliceRight, "borderImage.sliceRight");
            slice(bi.sliceBottom, "borderImage.sliceBottom");
            slice(bi.sliceLeft, "borderImage.sliceLeft");
        }
        // The gradient is validated by GradientPainter, which names its
        // fields with a "gradient." prefix.
    }

    private static void positive(double v, String field) {
        if (Double.isNaN(v) || Double.isInfinite(v) || !(v > 0)) {
            throw new IllegalArgumentException(field + " must be greater than 0, got " + v);
        }
    }

    private static void finite(double v, String field) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw new IllegalArgumentException(field + " is not a number: " + v);
        }
    }

    private static void slice(double v, String field) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 0) {
            throw new IllegalArgumentException(field + " must be 0 or more, got " + v);
        }
    }

    private static void pad(int v, String field) {
        if (v < 0) {
            throw new IllegalArgumentException(field + " must not be negative, got " + v);
        }
    }

    private static void side(BorderSide side, String field) {
        if (side == null) {
            throw new IllegalArgumentException(field + " must not be null; use BorderSide.NONE for no border");
        }
        if (side.style == null) {
            throw new IllegalArgumentException(field + ".style must not be null");
        }
        if (Double.isNaN(side.width) || Double.isInfinite(side.width) || side.width < 0) {
            throw new IllegalArgumentException(field + ".width must be 0 or more, got " + side.width);
        }
    }
}
