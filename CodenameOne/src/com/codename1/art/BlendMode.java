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
package com.codename1.art;

/// How a layer's colors combine with what is already painted below it.
///
/// The first sixteen are the modes of the W3C Compositing and Blending specification, which
/// PSD, Sketch, Figma and SVG's `mix-blend-mode` all share. The rest are Photoshop's
/// additional modes and the additive modes of CSS, Sketch and Figma.
public enum BlendMode {
    /// The layer covers what is below it.
    NORMAL(false),
    /// Multiplies the two colors, so the result is always darker.
    MULTIPLY(false),
    /// Inverts, multiplies and inverts again, so the result is always lighter.
    SCREEN(false),
    /// Multiply or screen, depending on the color below.
    OVERLAY(false),
    /// Keeps the darker of the two colors.
    DARKEN(false),
    /// Keeps the lighter of the two colors.
    LIGHTEN(false),
    /// Brightens what is below to reflect the layer.
    COLOR_DODGE(false),
    /// Darkens what is below to reflect the layer.
    COLOR_BURN(false),
    /// Multiply or screen, depending on the layer's color.
    HARD_LIGHT(false),
    /// A softer form of hard light.
    SOFT_LIGHT(false),
    /// The absolute difference of the two colors.
    DIFFERENCE(false),
    /// Like difference, with lower contrast.
    EXCLUSION(false),
    /// The layer's hue, with the saturation and luminosity below.
    HUE(true),
    /// The layer's saturation, with the hue and luminosity below.
    SATURATION(true),
    /// The layer's hue and saturation, with the luminosity below.
    COLOR(true),
    /// The layer's luminosity, with the hue and saturation below.
    LUMINOSITY(true),
    /// Adds the two colors and subtracts white: darker than multiply.
    LINEAR_BURN(false),
    /// Adds the two colors: brighter than screen (Photoshop's "add").
    LINEAR_DODGE(false),
    /// Color burn or color dodge, depending on the layer's color.
    VIVID_LIGHT(false),
    /// Linear burn or linear dodge, depending on the layer's color.
    LINEAR_LIGHT(false),
    /// Darken or lighten, depending on the layer's color.
    PIN_LIGHT(false),
    /// Each channel goes to 0 or 1: the threshold of vivid light.
    HARD_MIX(false),
    /// Subtracts the layer from what is below.
    SUBTRACT(false),
    /// Divides what is below by the layer.
    DIVIDE(false),
    /// Keeps whichever whole color, the layer's or the one below, has the lower luminosity.
    DARKER_COLOR(true),
    /// Keeps whichever whole color has the higher luminosity.
    LIGHTER_COLOR(true),
    /// Shows each pixel of the layer fully or not at all, at random, in proportion to its
    /// opacity.
    DISSOLVE(false),
    /// Adds colors and opacities, clamped: the `plus-lighter` of CSS, Sketch and Figma.
    PLUS_LIGHTER(false),
    /// Subtracts the inverses of the colors from white, adding opacities: Sketch's
    /// `plus-darker`.
    PLUS_DARKER(false);

    private final boolean nonSeparable;

    BlendMode(boolean nonSeparable) {
        this.nonSeparable = nonSeparable;
    }

    /// Returns true when this mode looks at all three color channels together, rather than
    /// blending each channel by itself.
    public boolean isNonSeparable() {
        return nonSeparable;
    }
}
