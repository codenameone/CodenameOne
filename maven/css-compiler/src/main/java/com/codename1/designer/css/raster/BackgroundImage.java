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

import java.awt.image.BufferedImage;

/// A CSS background image layer with its `background-repeat`,
/// `background-position` and `background-size`. Immutable, apart from the
/// image it refers to, which is never modified by this package.
public final class BackgroundImage {
    /// The `background-repeat` modes.
    public enum Repeat { REPEAT, REPEAT_X, REPEAT_Y, NO_REPEAT }

    /// How the tiles of a repeating axis are fitted to the positioning
    /// area: as they come, spread out so that a whole number fits with gaps
    /// between them (`space`), or resized so that a whole number fits
    /// exactly (`round`).
    public enum Fit { NONE, SPACE, ROUND }

    /// The `background-size` modes.
    public enum Size { AUTO, COVER, CONTAIN, EXPLICIT }

    /// The image to paint.
    public final BufferedImage image;
    /// How the image tiles.
    public final Repeat repeat;
    /// Horizontal position. A percentage `p` places the image at
    /// `p%` of (positioning area width - image width); otherwise px from
    /// the left of the positioning area (the padding box).
    public final double posX;
    /// Vertical position, see [#posX].
    public final double posY;
    /// Whether [#posX] is a percentage.
    public final boolean posXPercent;
    /// Whether [#posY] is a percentage.
    public final boolean posYPercent;
    /// The sizing mode.
    public final Size size;
    /// The width for [Size#EXPLICIT]; a negative value means `auto`.
    public final double sizeW;
    /// The height for [Size#EXPLICIT]; a negative value means `auto`.
    public final double sizeH;
    /// How the tiles are fitted across, when the image repeats across.
    public final Fit fitX;
    /// How the tiles are fitted down, when the image repeats down.
    public final Fit fitY;

    /// Creates a layer with the CSS initial values: `repeat`, position
    /// `0% 0%`, size `auto`.
    public BackgroundImage(BufferedImage image) {
        this(image, Repeat.REPEAT, 0, true, 0, true, Size.AUTO, -1, -1);
    }

    public BackgroundImage(BufferedImage image, Repeat repeat, double posX, boolean posXPercent,
            double posY, boolean posYPercent, Size size, double sizeW, double sizeH) {
        this(image, repeat, posX, posXPercent, posY, posYPercent, size, sizeW, sizeH, Fit.NONE, Fit.NONE);
    }

    private BackgroundImage(BufferedImage image, Repeat repeat, double posX, boolean posXPercent,
            double posY, boolean posYPercent, Size size, double sizeW, double sizeH, Fit fitX, Fit fitY) {
        this.fitX = fitX == null ? Fit.NONE : fitX;
        this.fitY = fitY == null ? Fit.NONE : fitY;
        this.image = image;
        this.repeat = repeat;
        this.posX = posX;
        this.posXPercent = posXPercent;
        this.posY = posY;
        this.posYPercent = posYPercent;
        this.size = size;
        this.sizeW = sizeW;
        this.sizeH = sizeH;
    }

    /// Returns a copy with a different repeat mode.
    public BackgroundImage withRepeat(Repeat r) {
        return new BackgroundImage(image, r, posX, posXPercent, posY, posYPercent, size, sizeW, sizeH, fitX, fitY);
    }

    /// Returns a copy whose tiles are fitted differently.
    public BackgroundImage withFit(Fit x, Fit y) {
        return new BackgroundImage(image, repeat, posX, posXPercent, posY, posYPercent, size, sizeW, sizeH, x, y);
    }

    /// Returns a copy with a different position.
    public BackgroundImage withPosition(double x, boolean xPercent, double y, boolean yPercent) {
        return new BackgroundImage(image, repeat, x, xPercent, y, yPercent, size, sizeW, sizeH, fitX, fitY);
    }

    /// Returns a copy sized by keyword (`AUTO`, `COVER` or `CONTAIN`).
    public BackgroundImage withSize(Size s) {
        return new BackgroundImage(image, repeat, posX, posXPercent, posY, posYPercent, s, -1, -1, fitX, fitY);
    }

    /// Returns a copy with an explicit size; a negative value is `auto`
    /// for that axis.
    public BackgroundImage withSize(double w, double h) {
        return new BackgroundImage(image, repeat, posX, posXPercent, posY, posYPercent, Size.EXPLICIT, w, h, fitX, fitY);
    }

    public BufferedImage getImage() {
        return image;
    }

    public Repeat getRepeat() {
        return repeat;
    }

    public double getPosX() {
        return posX;
    }

    public double getPosY() {
        return posY;
    }

    public boolean isPosXPercent() {
        return posXPercent;
    }

    public boolean isPosYPercent() {
        return posYPercent;
    }

    public Size getSize() {
        return size;
    }

    public double getSizeW() {
        return sizeW;
    }

    public double getSizeH() {
        return sizeH;
    }
}
