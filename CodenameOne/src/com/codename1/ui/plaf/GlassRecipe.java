/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.ui.plaf;

/// A typed, named recipe for the "Liquid Glass" backdrop materials (iOS 26).
///
/// A glass surface is described by its material INTENT -- which named recipe
/// it uses -- rather than by loose per-parameter theme constants. Each recipe
/// bundles the bounded, measured parameters of one native material (the colour
/// transform the real UIVisualEffectView / UIGlassEffect applies to the blurred
/// backdrop, plus the edge optics), so similar glass surfaces cannot silently
/// diverge and a theme cannot tune itself into an incoherent material. The
/// available recipes:
///
/// - `blur`   -- {@link Kind#PLAIN_BLUR}: backdrop blur only, no
/// material colour transform (a plain CSS backdrop-filter).
/// - `chrome` -- {@link Kind#LIQUID_CHROME}: the rectangular chrome
/// bars anchored at a screen edge (navigation / title bars). Very
/// transparent; the backdrop reads through at near-full saturation.
/// - `pill`   -- {@link Kind#LIQUID_PILL}: the floating pill chrome
/// (the iOS 26 tab bar). Frostier than the edge bars -- in light mode it
/// washes strongly toward white while boosting saturation.
/// - `panel`  -- {@link Kind#LIQUID_PANEL}: a bare glass panel or
/// button capsule (UIGlassEffect). The strongest material: heavy wash,
/// plus edge refraction and a specular rim so the glass reads as a layer
/// on top of the content rather than a flat hole.
/// - `chrome27`, `pill27`, `panel27` -- the same three materials as
/// iOS 27 retuned them. Light is measured as unchanged in chrome and pill;
/// dark moved in all three. See {@link #liquidPanel27(boolean)} for how the
/// constants were measured and why dark is approximate.
///
/// A theme assigns a recipe per UIID with the theme constant
/// `<UIID>GlassRecipe` (for example `ToolbarGlassRecipe: chrome`),
/// with `glassRecipeDefault` as the theme-wide default (`panel`
/// when unset). The recipe is resolved at paint time by
/// `Component.internalPaintImpl` and its parameters are passed to the
/// port through `Graphics.glassRegion`; ports never read material
/// constants themselves.
///
/// The travelling tab-selection lens is the remaining glass surface; it is
/// an optics-over-content effect bound to the morph motion, so its typed
/// parameters live with the motion model (`TabSelectionMorph`) rather
/// than here.
public final class GlassRecipe {

    /// The material kind a {@link GlassRecipe} renders.
    public enum Kind {
        /// Backdrop blur only -- no material colour transform.
        PLAIN_BLUR,
        /// Rectangular chrome bar anchored at a screen edge (nav/title bar).
        LIQUID_CHROME,
        /// Floating pill chrome (the iOS 26 tab bar).
        LIQUID_PILL,
        /// Bare glass panel / button capsule (UIGlassEffect).
        LIQUID_PANEL
    }

    private final Kind kind;
    private final float saturation;
    private final float scale;
    private final float offset;
    private final float refraction;
    private final float specular;

    private GlassRecipe(Kind kind, float saturation, float scale, float offset,
            float refraction, float specular) {
        this.kind = kind;
        this.saturation = saturation;
        this.scale = scale;
        this.offset = offset;
        this.refraction = refraction;
        this.specular = specular;
    }

    /// Plain backdrop blur with no material transform.
    ///
    /// @return the plain-blur recipe
    public static GlassRecipe plainBlur() {
        return new GlassRecipe(Kind.PLAIN_BLUR, 1f, 1f, 0f, 0f, 0f);
    }

    /// The rectangular edge-anchored chrome bar material (navigation/title
    /// bars). Measured against the iOS 26 UINavigationBar glass: the backdrop
    /// passes through at near-full strength with only a light wash.
    ///
    /// @param dark true for the dark appearance
    /// @return the chrome-bar recipe
    public static GlassRecipe liquidChrome(boolean dark) {
        return dark
                ? new GlassRecipe(Kind.LIQUID_CHROME, 1.6f, 1.0f, 12f, 0f, 0f)
                : new GlassRecipe(Kind.LIQUID_CHROME, 1.1f, 0.85f, 20f, 0f, 0f);
    }

    /// The floating pill chrome material (the iOS 26 tab bar). Frostier than
    /// the edge bars: light mode washes strongly toward white while boosting
    /// saturation, and a faint specular rim keeps the pill edge legible.
    ///
    /// @param dark true for the dark appearance
    /// @return the floating-pill recipe
    public static GlassRecipe liquidPill(boolean dark) {
        return dark
                ? new GlassRecipe(Kind.LIQUID_PILL, 2.5f, 0.3f, 13f, 0f, 0.2f)
                : new GlassRecipe(Kind.LIQUID_PILL, 1.8f, 1.0f, 108f, 0f, 0.2f);
    }

    /// The bare glass panel / button capsule material (UIGlassEffect). The
    /// strongest material: a heavy wash plus edge refraction (lensing) and a
    /// specular rim, so a free-standing glass element reads as a layer on top
    /// of the content.
    ///
    /// @param dark true for the dark appearance
    /// @return the glass-panel recipe
    public static GlassRecipe liquidPanel(boolean dark) {
        return dark
                ? new GlassRecipe(Kind.LIQUID_PANEL, 2.5f, 0.238f, 28.4f, 0.4f, 0.5f)
                : new GlassRecipe(Kind.LIQUID_PANEL, 1.95f, 0.303f, 174.3f, 0.4f, 0.5f);
    }

    /// The iOS 27 chrome-bar material.
    ///
    /// LIGHT IS DELIBERATELY IDENTICAL to {@link #liquidChrome(boolean)}. Fitting
    /// the transform against the iOS 27 capture returned sat 1.09, scale 0.860,
    /// offset 18.9 where iOS 26 is 1.10 / 0.850 / 20.0 -- inside 1.3% on every
    /// parameter, at an rms of 1.27/255. That is a measurement saying "unchanged",
    /// so the iOS 26 numbers are reused verbatim rather than replaced by a
    /// near-identical duplicate that would read as a real difference.
    ///
    /// DARK is the best affine fit (rms 14.6/255) and is KNOWN NOT TO BE EXACT.
    /// See {@link #liquidPanel27(boolean)} for why dark cannot be fitted properly.
    ///
    /// @param dark true for the dark appearance
    /// @return the iOS 27 chrome-bar recipe
    public static GlassRecipe liquidChrome27(boolean dark) {
        return dark
                ? new GlassRecipe(Kind.LIQUID_CHROME, 1.98f, 0.764f, 51.3f, 0f, 0f)
                : new GlassRecipe(Kind.LIQUID_CHROME, 1.1f, 0.85f, 20f, 0f, 0f);
    }

    /// The iOS 27 floating-pill material.
    ///
    /// LIGHT is again measured as unchanged -- the fit returned 1.86 / 0.984 /
    /// 111.4 against iOS 26's 1.80 / 1.000 / 108.0 -- so the iOS 26 values are
    /// reused. DARK is the best affine fit and, like chrome, is approximate.
    ///
    /// @param dark true for the dark appearance
    /// @return the iOS 27 floating-pill recipe
    public static GlassRecipe liquidPill27(boolean dark) {
        return dark
                ? new GlassRecipe(Kind.LIQUID_PILL, 3.04f, 0.244f, 45.5f, 0f, 0.2f)
                : new GlassRecipe(Kind.LIQUID_PILL, 1.8f, 1.0f, 108f, 0f, 0.2f);
    }

    /// The iOS 27 glass-panel material, and the recipe that carries the method
    /// these three were measured with.
    ///
    /// HOW THE NUMBERS WERE OBTAINED. The material is the documented affine
    /// transform `c' = clamp((lum + (c - lum) * sat) * scale + offset)`. The iOS
    /// 26 constants are known, so every interior pixel of a committed
    /// goldens/ios-26-metal tile can be inverted back to the backdrop that
    /// produced it, and the matching goldens/ios-27-metal pixel then fitted
    /// against that same backdrop. Nothing about the backdrop has to be assumed,
    /// and because the transform is linear it commutes with the Gaussian blur, so
    /// the blur does not bias the fit. Clipping does NOT commute, so a pixel is
    /// used only when nothing within the blur's reach clipped -- without that
    /// erosion the photo-backdrop tile fitted at rms 14 against rms 1 for the flat
    /// ones, and it dragged every parameter with it.
    ///
    /// The method self-checks: run against the iOS 26 set it recovers the iOS 26
    /// chrome constants it was never told, to within 1.3%.
    ///
    /// WHY DARK IS APPROXIMATE, IN ALL THREE RECIPES. After the best affine fit,
    /// the light residual is flat across the whole backdrop-luma range (within
    /// +/-0.9/255). The dark residual is not: on the chrome bar it runs +8.9 at
    /// low luma, -9.3 through the middle and +10.3 at high luma -- a systematic
    /// curve, not noise. iOS 27's dark glass therefore applies a NON-LINEAR
    /// luminance response, and no choice of sat/scale/offset can express it,
    /// because this transform is affine by construction. Matching dark properly
    /// needs a curve term in the material model and in every port's shader, which
    /// is a larger change than new constants; these values are the closest an
    /// affine material gets until then.
    ///
    /// @param dark true for the dark appearance
    /// @return the iOS 27 glass-panel recipe
    public static GlassRecipe liquidPanel27(boolean dark) {
        return dark
                ? new GlassRecipe(Kind.LIQUID_PANEL, 2.84f, 0.378f, 79.8f, 0.4f, 0.5f)
                : new GlassRecipe(Kind.LIQUID_PANEL, 2.08f, 0.457f, 137.4f, 0.4f, 0.5f);
    }

    /// Looks up a recipe by its theme name: `blur`, `chrome`,
    /// `pill`, `panel`, or their iOS 27 variants `chrome27`,
    /// `pill27` and `panel27`. Unknown names fall back to the panel
    /// recipe -- the safest default for a free-standing glass surface.
    ///
    /// @param name the recipe name from the theme
    /// @param dark true for the dark appearance
    /// @return the named recipe, never null
    public static GlassRecipe named(String name, boolean dark) {
        String n = name == null ? "" : name.trim();
        if ("blur".equals(n)) {
            return plainBlur();
        }
        if ("chrome".equals(n)) {
            return liquidChrome(dark);
        }
        if ("pill".equals(n)) {
            return liquidPill(dark);
        }
        if ("chrome27".equals(n)) {
            return liquidChrome27(dark);
        }
        if ("pill27".equals(n)) {
            return liquidPill27(dark);
        }
        if ("panel27".equals(n)) {
            return liquidPanel27(dark);
        }
        return liquidPanel(dark);
    }

    /// Resolves the recipe for a UIID from the theme: the per-UIID
    /// `<UIID>GlassRecipe` constant wins, then the theme-wide
    /// `glassRecipeDefault`, then the panel recipe.
    ///
    /// @param manager the UI manager holding the theme
    /// @param uiid    the component's UIID
    /// @param dark    true for the dark appearance
    /// @return the resolved recipe, never null
    public static GlassRecipe resolve(UIManager manager, String uiid, boolean dark) {
        String name = manager.getThemeConstant(uiid + "GlassRecipe", null);
        if (name == null) {
            name = manager.getThemeConstant("glassRecipeDefault", "panel");
        }
        return named(name, dark);
    }

    /// The material kind this recipe renders.
    ///
    /// @return the kind, never null
    public Kind getKind() {
        return kind;
    }

    /// The saturation boost applied to the blurred backdrop.
    ///
    /// @return the saturation multiplier
    public float getSaturation() {
        return saturation;
    }

    /// The colour scale multiplier of the material's affine colour transform.
    ///
    /// @return the scale factor
    public float getScale() {
        return scale;
    }

    /// The colour offset (wash floor) of the material's affine colour transform,
    /// in 0..255 channel units.
    ///
    /// @return the offset
    public float getOffset() {
        return offset;
    }

    /// The edge refraction (lensing) strength -- bends the backdrop toward the
    /// edges so the glass reads as a layer on top rather than a flat hole.
    ///
    /// @return the refraction strength, 0 = none
    public float getRefraction() {
        return refraction;
    }

    /// The brightness of the specular edge rim (the bright glint).
    ///
    /// @return the specular strength, 0 = none
    public float getSpecular() {
        return specular;
    }
}
