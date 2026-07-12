/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 *
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

    /// Looks up a recipe by its theme name (`blur`, `chrome`,
    /// `pill` or `panel`). Unknown names fall back to the panel
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
