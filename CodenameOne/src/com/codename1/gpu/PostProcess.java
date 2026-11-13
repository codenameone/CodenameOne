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

import com.codename1.gpu.shader.ShaderMaterial;

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
    /// The texture name a custom pass reads the frame from. `addPass`
    /// declares it on the pass; read it with
    /// `com.codename1.gpu.shader.ShaderNode.sceneColor`.
    public static final String SCENE_COLOR = "cn1SceneColor";

    private ShaderMaterial[] passes = new ShaderMaterial[0];
    private float bloomThreshold = 1f;
    private float bloomIntensity;
    private float contrast = 1f;
    private float saturation = 1f;
    private int shadowTint = 0xff000000;
    private int highlightTint = 0xffffffff;
    private float vignette;
    private float grain;
    private float aberration;
    private boolean fxaa;
    private float godRays;
    private float aoStrength;
    private float aoRadius = 12f;
    private float focusDistance = 10f;
    private float dofBlur;
    private float outline;
    private int outlineColor = 0xff000000;
    private Texture lut;
    private float lutStrength;

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

    /// Splits the colour channels toward the edges of the frame, the fringe
    /// of a cheap lens. Subtle values sell an action camera; strong ones a
    /// hit or a glitch.
    ///
    /// #### Parameters
    ///
    /// - `amount`: 0 for none; 0.004 to 0.01 is subtle
    ///
    /// #### Returns
    ///
    /// this post process for chaining
    public PostProcess setChromaticAberration(float amount) {
        this.aberration = Math.max(0f, Math.min(0.05f, amount));
        return this;
    }

    /// Returns the chromatic aberration amount.
    public float getChromaticAberration() {
        return aberration;
    }

    /// Smooths jagged edges with FXAA, a single full screen pass that finds
    /// edges by contrast and blends along them. Cheaper than multisampling
    /// and it also smooths edges inside textures and shaders.
    ///
    /// #### Parameters
    ///
    /// - `on`: true to smooth
    ///
    /// #### Returns
    ///
    /// this post process for chaining
    public PostProcess setFxaa(boolean on) {
        this.fxaa = on;
        return this;
    }

    /// Returns true when FXAA is on.
    public boolean isFxaa() {
        return fxaa;
    }

    /// Light shafts from the sun: bright parts of the frame (sky, a sunlit
    /// wall) are smeared radially from where the sun sits on screen, so they
    /// stream past whatever stands in front of it. The sun's position comes
    /// from the scene's light and camera each frame; the shafts fade out as
    /// the sun leaves the screen or drops behind the camera.
    ///
    /// #### Parameters
    ///
    /// - `strength`: 0 for none; 0.3 to 0.8 for visible shafts
    ///
    /// #### Returns
    ///
    /// this post process for chaining
    public PostProcess setGodRays(float strength) {
        this.godRays = Math.max(0f, Math.min(2f, strength));
        return this;
    }

    /// Returns the god ray strength.
    public float getGodRays() {
        return godRays;
    }

    /// Darkens creases and contact points from the depth of the frame
    /// (screen space ambient occlusion), so objects sit on the ground instead
    /// of floating over it.
    ///
    /// #### Parameters
    ///
    /// - `strength`: 0 for none; 0.5 to 1 for visible contact shadows
    ///
    /// - `radiusPixels`: how far around each pixel to look, in pixels
    ///
    /// #### Returns
    ///
    /// this post process for chaining
    public PostProcess setAmbientOcclusion(float strength, float radiusPixels) {
        this.aoStrength = Math.max(0f, Math.min(1f, strength));
        this.aoRadius = Math.max(1f, Math.min(64f, radiusPixels));
        return this;
    }

    /// Returns the ambient occlusion strength.
    public float getAmbientOcclusion() {
        return aoStrength;
    }

    /// Returns the ambient occlusion radius in pixels.
    public float getAmbientOcclusionRadius() {
        return aoRadius;
    }

    /// Blurs what is nearer or farther than the focus distance, as a camera
    /// lens with a wide aperture does.
    ///
    /// #### Parameters
    ///
    /// - `focusDistance`: the distance from the camera that stays sharp, in
    ///   world units
    ///
    /// - `blurPixels`: the widest blur, in pixels; 0 turns depth of field off
    ///
    /// #### Returns
    ///
    /// this post process for chaining
    public PostProcess setDepthOfField(float focusDistance, float blurPixels) {
        this.focusDistance = Math.max(0.001f, focusDistance);
        this.dofBlur = Math.max(0f, Math.min(32f, blurPixels));
        return this;
    }

    /// Returns the focus distance.
    public float getFocusDistance() {
        return focusDistance;
    }

    /// Returns the widest depth of field blur in pixels.
    public float getDepthOfFieldBlur() {
        return dofBlur;
    }

    /// Draws a line where the depth of the frame jumps, around silhouettes
    /// and creases, for a toon or ink look.
    ///
    /// #### Parameters
    ///
    /// - `strength`: 0 for none, 1 for solid lines
    ///
    /// - `argb`: the line colour
    ///
    /// #### Returns
    ///
    /// this post process for chaining
    public PostProcess setOutline(float strength, int argb) {
        this.outline = Math.max(0f, Math.min(1f, strength));
        this.outlineColor = argb;
        return this;
    }

    /// Returns the outline strength.
    public float getOutline() {
        return outline;
    }

    /// Returns the outline colour.
    public int getOutlineColor() {
        return outlineColor;
    }

    /// Maps every colour through a lookup table after the grade, the way a
    /// film look is applied in a colour grading tool. The table is a 256 by
    /// 16 strip of 16 squares: square `b` holds blue `b / 15`, and inside it
    /// the column is red and the row (from the top) green, both 0 to 15.
    /// `ColorLuts` builds such strips.
    ///
    /// #### Parameters
    ///
    /// - `table`: the lookup strip, or null for none
    ///
    /// - `strength`: 0 keeps the graded colour, 1 uses the table's
    ///
    /// #### Returns
    ///
    /// this post process for chaining
    public PostProcess setColorLut(Texture table, float strength) {
        this.lut = table;
        this.lutStrength = Math.max(0f, Math.min(1f, strength));
        return this;
    }

    /// Returns the lookup table, or null.
    public Texture getColorLut() {
        return lut;
    }

    /// Returns the lookup table strength, 0 when there is no table.
    public float getColorLutStrength() {
        return lut == null ? 0f : lutStrength;
    }

    /// True when an effect reads the depth of the frame: ambient occlusion,
    /// depth of field or outline.
    public boolean isDepthEffects() {
        return aoStrength > 0f || dofBlur > 0f || outline > 0f;
    }

    /// Adds a full screen effect of your own, written with the same
    /// `com.codename1.gpu.shader.ShaderNode` graphs as a material. The pass's
    /// `ShaderMaterial.Hook.BASE_COLOR` graph is evaluated for every pixel of
    /// the frame: `ShaderNode.screenUv()` is the pixel's place on screen (0,0
    /// top left) and `ShaderNode.sceneColor(uv)` reads the frame as it stands
    /// so far. Its own uniforms, textures and `ShaderNode.time()` work as on
    /// any material.
    ///
    /// ```java
    /// ShaderNode uv = ShaderNode.screenUv();
    /// ShaderNode wobble = ShaderNode.sin(uv.y().mul(ShaderNode.f(40f)).add(ShaderNode.time()))
    ///         .mul(ShaderNode.f(0.004f));
    /// post.addPass(new ShaderMaterial().set(ShaderMaterial.Hook.BASE_COLOR,
    ///         ShaderNode.sceneColor(uv.add(ShaderNode.vec(wobble, ShaderNode.f(0f))))));
    /// ```
    ///
    /// Passes run in the order they were added, each reading the one before,
    /// and the built-in effects run on the result. The depth based effects
    /// (`isDepthEffects`) read the depth of the scene, which a pass does not
    /// have, so they are left out of a frame with passes. A device that
    /// cannot draw offscreen draws the scene without the passes.
    ///
    /// #### Parameters
    ///
    /// - `pass`: a shader material with a `BASE_COLOR` graph
    ///
    /// #### Returns
    ///
    /// this post process for chaining
    public PostProcess addPass(ShaderMaterial pass) {
        if (pass == null || pass.get(ShaderMaterial.Hook.BASE_COLOR) == null) {
            throw new IllegalArgumentException("A pass needs a BASE_COLOR graph");
        }
        if (pass.getTextureSlot(SCENE_COLOR) < 0) {
            pass.declareTexture(SCENE_COLOR, null);
        }
        ShaderMaterial[] next = new ShaderMaterial[passes.length + 1];
        System.arraycopy(passes, 0, next, 0, passes.length);
        next[passes.length] = pass;
        passes = next;
        return this;
    }

    /// Removes every pass `addPass` added.
    ///
    /// #### Returns
    ///
    /// this post process for chaining
    public PostProcess clearPasses() {
        passes = new ShaderMaterial[0];
        return this;
    }

    /// Returns how many custom passes there are.
    public int getPassCount() {
        return passes.length;
    }

    /// Returns a custom pass.
    ///
    /// #### Parameters
    ///
    /// - `index`: 0 for the first pass added
    ///
    /// #### Returns
    ///
    /// the pass
    public ShaderMaterial getPass(int index) {
        return passes[index];
    }

    /// Copies every built-in effect of `from` into this one except the depth
    /// based effects, and none of its passes.
    void copyWithoutDepth(PostProcess from) {
        bloomThreshold = from.bloomThreshold;
        bloomIntensity = from.bloomIntensity;
        contrast = from.contrast;
        saturation = from.saturation;
        shadowTint = from.shadowTint;
        highlightTint = from.highlightTint;
        vignette = from.vignette;
        grain = from.grain;
        aberration = from.aberration;
        fxaa = from.fxaa;
        godRays = from.godRays;
        aoStrength = 0f;
        aoRadius = from.aoRadius;
        focusDistance = from.focusDistance;
        dofBlur = 0f;
        outline = 0f;
        outlineColor = from.outlineColor;
        lut = from.lut;
        lutStrength = from.lutStrength;
    }
}
