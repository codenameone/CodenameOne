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

/// Everything [CssBoxRasterizer] needs to paint one CSS box. Immutable;
/// built with [#builder()].
///
/// Colours are non-premultiplied ARGB ints, lengths are resolved CSS px.
/// The builder does not validate; [CssBoxRasterizer#rasterize(BoxStyle)]
/// does, and names the offending field.
public final class BoxStyle {
    private final double borderBoxWidth;
    private final double borderBoxHeight;
    private final int padTop;
    private final int padRight;
    private final int padBottom;
    private final int padLeft;
    private final int backgroundColor;
    private final GradientSpec gradient;
    private final BackgroundImage backgroundImage;
    private final BorderSide top;
    private final BorderSide right;
    private final BorderSide bottom;
    private final BorderSide left;
    private final double[] radii;
    private final Shadow shadow;
    private final BorderImage borderImage;

    private BoxStyle(Builder b) {
        borderBoxWidth = b.borderBoxWidth;
        borderBoxHeight = b.borderBoxHeight;
        padTop = b.padTop;
        padRight = b.padRight;
        padBottom = b.padBottom;
        padLeft = b.padLeft;
        backgroundColor = b.backgroundColor;
        gradient = b.gradient == null ? null : new GradientSpec(b.gradient);
        backgroundImage = b.backgroundImage;
        top = b.top;
        right = b.right;
        bottom = b.bottom;
        left = b.left;
        radii = b.radii == null ? null : b.radii.clone();
        shadow = b.shadow;
        borderImage = b.borderImage;
    }

    /// Starts a style: no padding, a transparent background, no borders,
    /// square corners. Only the border box size has to be set.
    public static Builder builder() {
        return new Builder();
    }

    /// The width of the CSS border box in px; may be fractional.
    public double getBorderBoxWidth() {
        return borderBoxWidth;
    }

    /// The height of the CSS border box in px; may be fractional.
    public double getBorderBoxHeight() {
        return borderBoxHeight;
    }

    /// Transparent rows captured above the border box.
    public int getPadTop() {
        return padTop;
    }

    public int getPadRight() {
        return padRight;
    }

    public int getPadBottom() {
        return padBottom;
    }

    /// Transparent columns captured left of the border box.
    public int getPadLeft() {
        return padLeft;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    /// The gradient painted over the background colour, or `null`. A copy.
    public GradientSpec getGradient() {
        return gradient == null ? null : new GradientSpec(gradient);
    }

    /// The image painted over the background colour and gradient, or `null`.
    public BackgroundImage getBackgroundImage() {
        return backgroundImage;
    }

    public BorderSide getTop() {
        return top;
    }

    public BorderSide getRight() {
        return right;
    }

    public BorderSide getBottom() {
        return bottom;
    }

    public BorderSide getLeft() {
        return left;
    }

    /// The eight corner radii in px: top-left x and y, top-right x and y,
    /// bottom-right x and y, bottom-left x and y. A copy.
    public double[] getRadii() {
        return radii == null ? null : radii.clone();
    }

    public Shadow getShadow() {
        return shadow;
    }

    public BorderImage getBorderImage() {
        return borderImage;
    }

    /// Fluent builder for [BoxStyle]. The setters carry the field names.
    public static final class Builder {
        private double borderBoxWidth;
        private double borderBoxHeight;
        private int padTop;
        private int padRight;
        private int padBottom;
        private int padLeft;
        private int backgroundColor;
        private GradientSpec gradient;
        private BackgroundImage backgroundImage;
        private BorderSide top = BorderSide.NONE;
        private BorderSide right = BorderSide.NONE;
        private BorderSide bottom = BorderSide.NONE;
        private BorderSide left = BorderSide.NONE;
        private double[] radii = new double[8];
        private Shadow shadow;
        private BorderImage borderImage;

        private Builder() {
        }

        public Builder borderBoxWidth(double v) {
            borderBoxWidth = v;
            return this;
        }

        public Builder borderBoxHeight(double v) {
            borderBoxHeight = v;
            return this;
        }

        /// Sets both border box dimensions.
        public Builder size(double width, double height) {
            borderBoxWidth = width;
            borderBoxHeight = height;
            return this;
        }

        public Builder padTop(int v) {
            padTop = v;
            return this;
        }

        public Builder padRight(int v) {
            padRight = v;
            return this;
        }

        public Builder padBottom(int v) {
            padBottom = v;
            return this;
        }

        public Builder padLeft(int v) {
            padLeft = v;
            return this;
        }

        /// Sets the same padding on all four sides.
        public Builder pad(int all) {
            padTop = all;
            padRight = all;
            padBottom = all;
            padLeft = all;
            return this;
        }

        public Builder backgroundColor(int argb) {
            backgroundColor = argb;
            return this;
        }

        public Builder gradient(GradientSpec v) {
            gradient = v;
            return this;
        }

        public Builder backgroundImage(BackgroundImage v) {
            backgroundImage = v;
            return this;
        }

        public Builder top(BorderSide v) {
            top = v;
            return this;
        }

        public Builder right(BorderSide v) {
            right = v;
            return this;
        }

        public Builder bottom(BorderSide v) {
            bottom = v;
            return this;
        }

        public Builder left(BorderSide v) {
            left = v;
            return this;
        }

        /// Sets the same border on all four sides.
        public Builder border(BorderSide all) {
            top = all;
            right = all;
            bottom = all;
            left = all;
            return this;
        }

        /// Sets the corner radii. Eight values are taken as they are
        /// (top-left x and y, top-right x and y, bottom-right x and y,
        /// bottom-left x and y). As a convenience four values are circular
        /// radii for the top-left, top-right, bottom-right and bottom-left
        /// corners, and one value is a circular radius for every corner.
        /// Any other count is stored unchanged and rejected by the
        /// rasterizer.
        public Builder radii(double... v) {
            if (v == null) {
                radii = null;
            } else if (v.length == 1) {
                radii = new double[] {v[0], v[0], v[0], v[0], v[0], v[0], v[0], v[0]};
            } else if (v.length == 4) {
                radii = new double[] {v[0], v[0], v[1], v[1], v[2], v[2], v[3], v[3]};
            } else {
                radii = v.clone();
            }
            return this;
        }

        public Builder shadow(Shadow v) {
            shadow = v;
            return this;
        }

        public Builder borderImage(BorderImage v) {
            borderImage = v;
            return this;
        }

        public BoxStyle build() {
            return new BoxStyle(this);
        }
    }
}
