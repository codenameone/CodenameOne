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
package com.codename1.gpu;

/// Full screen effects applied to a rendered frame: bloom, a colour grade,
/// vignette and film grain. It is what turns a correctly lit scene into one
/// that looks shot through a lens.
///
/// The scene is drawn into an offscreen image, the effects are applied, and
/// the result is drawn to the screen. Touch controls are drawn afterwards and
/// are never graded. A device that cannot render offscreen
/// (`GpuCapabilities.isPostProcessSupported` is false) draws the scene
/// directly and ignores this.
///
/// ```java
/// GameView view = new GameView() {
///     protected void update(double deltaSeconds) {
///     }
/// };
/// view.setPostProcess(new PostProcess()
///         .setBloom(0.75f, 0.6f)
///         .setGrade(1.08f, 0.9f, 0xff0e1a24, 0xfffff1dc)
///         .setVignette(0.35f)
///         .setGrain(0.04f));
/// ```
///
/// All effects start off, so `new PostProcess()` changes nothing but the cost
/// of the extra pass.
public final class PostProcess {
    private float bloomThreshold = 1f;
    private float bloomIntensity;
    private float contrast = 1f;
    private float saturation = 1f;
    private int shadowTint = 0xff000000;
    private int highlightTint = 0xffffffff;
    private float vignette;
    private float grain;

    /// Makes bright parts of the frame glow into their surroundings.
    ///
    /// #### Parameters
    ///
    /// - `threshold`: how bright a pixel must be to glow, from 0 to 1 of its
    ///   brightest channel; lower glows more of the frame
    ///
    /// - `intensity`: how strong the glow is; 0 turns bloom off
    ///
    /// #### Returns
    ///
    /// this post process for chaining
    public PostProcess setBloom(float threshold, float intensity) {
        this.bloomThreshold = Math.max(0f, Math.min(1f, threshold));
        this.bloomIntensity = Math.max(0f, intensity);
        return this;
    }

    /// Grades the colour of the frame.
    ///
    /// #### Parameters
    ///
    /// - `contrast`: 1 leaves it, above 1 deepens shadows and brightens highlights
    ///
    /// - `saturation`: 1 leaves it, 0 is greyscale
    ///
    /// - `shadowTint`: the colour the darkest tones are lifted toward, as ARGB;
    ///   black leaves them
    ///
    /// - `highlightTint`: the colour the brightest tones are multiplied by, as
    ///   ARGB; white leaves them
    ///
    /// #### Returns
    ///
    /// this post process for chaining
    public PostProcess setGrade(float contrast, float saturation, int shadowTint, int highlightTint) {
        this.contrast = Math.max(0f, contrast);
        this.saturation = Math.max(0f, saturation);
        this.shadowTint = shadowTint;
        this.highlightTint = highlightTint;
        return this;
    }

    /// Darkens the corners of the frame.
    ///
    /// #### Parameters
    ///
    /// - `amount`: 0 for none, 1 for black corners
    ///
    /// #### Returns
    ///
    /// this post process for chaining
    public PostProcess setVignette(float amount) {
        this.vignette = Math.max(0f, Math.min(1f, amount));
        return this;
    }

    /// Adds moving film grain.
    ///
    /// #### Parameters
    ///
    /// - `amount`: 0 for none; 0.03 to 0.06 reads as film
    ///
    /// #### Returns
    ///
    /// this post process for chaining
    public PostProcess setGrain(float amount) {
        this.grain = Math.max(0f, Math.min(1f, amount));
        return this;
    }

    /// Returns the bloom brightness threshold.
    public float getBloomThreshold() {
        return bloomThreshold;
    }

    /// Returns the bloom intensity, 0 when bloom is off.
    public float getBloomIntensity() {
        return bloomIntensity;
    }

    /// Returns true if bloom is on.
    public boolean isBloom() {
        return bloomIntensity > 0f;
    }

    /// Returns the contrast multiplier.
    public float getContrast() {
        return contrast;
    }

    /// Returns the saturation multiplier.
    public float getSaturation() {
        return saturation;
    }

    /// Returns the shadow tint as ARGB.
    public int getShadowTint() {
        return shadowTint;
    }

    /// Returns the highlight tint as ARGB.
    public int getHighlightTint() {
        return highlightTint;
    }

    /// Returns the vignette amount.
    public float getVignette() {
        return vignette;
    }

    /// Returns the grain amount.
    public float getGrain() {
        return grain;
    }
}
