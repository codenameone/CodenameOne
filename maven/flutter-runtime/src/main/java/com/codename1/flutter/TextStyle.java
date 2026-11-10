/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

/**
 * Text styling configuration. Like the widgets, named Dart parameters become
 * void setter methods; unset properties stay null and inherit the CN1
 * default style. {@link #copyWith} and {@link #apply} return derived copies
 * (the P2 cascade fixers) so member access on a text style stays statically
 * typed rather than collapsing to dynamic.
 */
public class TextStyle {

    private Double fontSize;
    private FontWeight fontWeight;
    private Color color;
    private String fontFamily;
    private Boolean inherit;
    private FontStyle fontStyle;

    /** Upright or italic glyphs. */
    public void fontStyle(FontStyle v) {
        this.fontStyle = v;
    }

    public FontStyle fontStyle() {
        return fontStyle;
    }
    private String fontPackage;
    private Paint foreground;

    /**
     * Whether null properties are taken from the ambient style. Flutter's default is
     * true; a style with false (an icon's glyph style) stands on its own.
     */
    public void inherit(boolean v) {
        this.inherit = v;
    }

    public boolean inherit() {
        return inherit == null || inherit.booleanValue();
    }

    /** Dart's {@code package}: the package whose bundled font {@code fontFamily} names. */
    public void package_(String v) {
        this.fontPackage = v;
    }

    public String package_() {
        return fontPackage;
    }

    /**
     * The paint the glyphs are drawn with. Flutter forbids both it and {@code color};
     * when only the paint is given, its color is the text color the renderers read.
     */
    public void foreground(Paint v) {
        this.foreground = v;
    }

    public Paint foreground() {
        return foreground;
    }
    private Double letterSpacing;
    private Double height;
    private TextDecoration decoration;
    private Color decorationColor;
    private TextDecorationStyle decorationStyle;
    private Double decorationThickness;
    private Object shadows;
    private Color backgroundColor;

    public void decoration(TextDecoration v) {
        this.decoration = v;
    }

    public void decorationColor(Color v) {
        this.decorationColor = v;
    }

    public void decorationStyle(TextDecorationStyle v) {
        this.decorationStyle = v;
    }

    public void decorationThickness(double v) {
        this.decorationThickness = v;
    }

    public void shadows(Object v) {
        this.shadows = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public TextDecoration decoration() {
        return decoration;
    }

    public Color decorationColor() {
        return decorationColor;
    }

    public TextDecorationStyle decorationStyle() {
        return decorationStyle;
    }

    public Double decorationThickness() {
        return decorationThickness;
    }

    public Object shadows() {
        return shadows;
    }

    public Color backgroundColor() {
        return backgroundColor;
    }

    /** This style with {@code other}'s decoration properties laid over it, when it sets them. */
    public TextStyle withDecorationFrom(TextStyle other) {
        if (other == null || (other.decoration == null && other.decorationColor == null
                && other.decorationStyle == null && other.decorationThickness == null
                && other.shadows == null && other.backgroundColor == null)) {
            return this;
        }
        TextStyle t = shallowClone();
        t.overlayDecoration(other);
        return t;
    }

    private void overlayDecoration(TextStyle o) {
        if (o.decoration != null) decoration = o.decoration;
        if (o.decorationColor != null) decorationColor = o.decorationColor;
        if (o.decorationStyle != null) decorationStyle = o.decorationStyle;
        if (o.decorationThickness != null) decorationThickness = o.decorationThickness;
        if (o.shadows != null) shadows = o.shadows;
        if (o.backgroundColor != null) backgroundColor = o.backgroundColor;
    }

    private Object fontVariations;
    private Object fontFamilyFallback;
    private TextLeadingDistribution leadingDistribution;

    public void fontVariations(Object v) {
        this.fontVariations = v;
    }

    @SuppressWarnings("unchecked")
    public dart.core.DartList<FontVariation> fontVariations() {
        return fontVariations instanceof dart.core.DartList ? (dart.core.DartList<FontVariation>) fontVariations : null;
    }

    public void fontFamilyFallback(Object v) {
        this.fontFamilyFallback = v;
    }

    @SuppressWarnings("unchecked")
    public dart.core.DartList<String> fontFamilyFallback() {
        return fontFamilyFallback instanceof dart.core.DartList ? (dart.core.DartList<String>) fontFamilyFallback : null;
    }

    public void leadingDistribution(TextLeadingDistribution v) {
        this.leadingDistribution = v;
    }

    public TextLeadingDistribution leadingDistribution() {
        return leadingDistribution;
    }

    public void fontSize(double v) {
        this.fontSize = v;
    }

    public void fontWeight(FontWeight v) {
        this.fontWeight = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void fontFamily(String v) {
        this.fontFamily = v;
    }

    public void letterSpacing(double v) {
        this.letterSpacing = v;
    }

    public Double getLetterSpacing() {
        return letterSpacing;
    }

    public void height(double v) {
        this.height = v;
    }

    // ------------------------------------------------------------------
    // Dart getters -> no-arg methods
    // ------------------------------------------------------------------

    public Color color() {
        return color;
    }

    public Double fontSize() {
        return fontSize;
    }

    public FontWeight fontWeight() {
        return fontWeight;
    }

    /**
     * Flutter's getter: with a {@code package} set, the family is
     * {@code packages/<package>/<fontFamily>}, which is also what font lookup
     * resolves against the package's bundled assets.
     */
    public String fontFamily() {
        if (fontFamily != null && fontPackage != null && !fontFamily.startsWith("packages/")) {
            return "packages/" + fontPackage + "/" + fontFamily;
        }
        return fontFamily;
    }

    public Double letterSpacing() {
        return letterSpacing;
    }

    public Double height() {
        return height;
    }

    // ------------------------------------------------------------------
    // Legacy accessors used by the render elements
    // ------------------------------------------------------------------

    /**
     * Font size in logical pixels, or null when inherited.
     */
    public Double getFontSize() {
        return fontSize;
    }

    public FontWeight getFontWeight() {
        return fontWeight;
    }

    public Color getColor() {
        if (color == null && foreground != null) {
            return foreground.color();
        }
        return color;
    }

    public String getFontFamily() {
        return fontFamily();
    }

    private TextStyle shallowClone() {
        TextStyle t = new TextStyle();
        t.fontSize = fontSize;
        t.fontWeight = fontWeight;
        t.color = color;
        t.fontFamily = fontFamily;
        t.inherit = inherit;
        t.fontStyle = fontStyle;
        t.fontPackage = fontPackage;
        t.foreground = foreground;
        t.letterSpacing = letterSpacing;
        t.height = height;
        t.decoration = decoration;
        t.decorationColor = decorationColor;
        t.decorationStyle = decorationStyle;
        t.decorationThickness = decorationThickness;
        t.shadows = shadows;
        t.backgroundColor = backgroundColor;
        return t;
    }

    /**
     * Returns a copy with the supplied (non-null) values overridden. Parameter
     * order matches the Dart stub. Properties this pass does not model
     * (fontStyle, wordSpacing, background, foreground, decoration) are accepted
     * for API shape and ignored.
     */
    /**
     * This style with {@code other}'s set properties layered on top — Flutter's
     * {@code TextStyle.merge}. A null {@code other} returns this style.
     */
    public TextStyle merge(TextStyle other) {
        if (other == null) {
            return this;
        }
        TextStyle t = shallowClone();
        if (other.color != null) {
            t.color = other.color;
        }
        if (other.fontSize != null) {
            t.fontSize = other.fontSize;
        }
        if (other.fontWeight != null) {
            t.fontWeight = other.fontWeight;
        }
        if (other.fontFamily != null) {
            t.fontFamily = other.fontFamily;
            t.fontPackage = other.fontPackage;
        }
        if (other.letterSpacing != null) {
            t.letterSpacing = other.letterSpacing;
        }
        if (other.height != null) {
            t.height = other.height;
        }
        t.overlayDecoration(other);
        return t;
    }

    public TextStyle copyWith(Boolean inherit, Color color, Color backgroundColor, String fontFamily,
                              Double fontSize, FontWeight fontWeight, Object fontStyle, Double letterSpacing,
                              Double wordSpacing, Double height, Object background, Object foreground,
                              Object decoration) {
        return copyWith(inherit, color, backgroundColor, fontFamily, fontSize, fontWeight, fontStyle,
                letterSpacing, wordSpacing, height, background, foreground, decoration,
                null, null, null, null, null, null);
    }

    /** The full Flutter {@code copyWith}: shadows, font features/variations and decoration details too. */
    public TextStyle copyWith(Boolean inherit, Color color, Color backgroundColor, String fontFamily,
                              Double fontSize, FontWeight fontWeight, Object fontStyle, Double letterSpacing,
                              Double wordSpacing, Double height, Object background, Object foreground,
                              Object decoration, Object shadows, Object fontFeatures, Object fontVariations,
                              Color decorationColor, Object decorationStyle, Double decorationThickness) {
        TextStyle t = shallowClone();
        if (shadows != null) t.shadows(shadows);
        if (fontFeatures != null) t.fontFeatures(fontFeatures);
        if (fontVariations != null) t.fontVariations(fontVariations);
        if (decorationColor != null) t.decorationColor(decorationColor);
        if (decorationStyle instanceof TextDecorationStyle) t.decorationStyle((TextDecorationStyle) decorationStyle);
        if (decorationThickness != null) t.decorationThickness(decorationThickness);
        if (color != null) t.color = color;
        if (fontFamily != null) { t.fontFamily = fontFamily; t.fontPackage = null; }
        if (fontSize != null) t.fontSize = fontSize;
        if (fontWeight != null) t.fontWeight = fontWeight;
        if (letterSpacing != null) t.letterSpacing = letterSpacing;
        if (height != null) t.height = height;
        if (backgroundColor != null) t.backgroundColor = backgroundColor;
        if (decoration instanceof TextDecoration) t.decoration = (TextDecoration) decoration;
        return t;
    }

    /**
     * Returns a copy with a foreground {@code color} override and the font size
     * scaled by {@code fontSizeFactor} then offset by {@code fontSizeDelta}
     * (both default to no-op when null). Mirrors Flutter's {@code TextStyle.apply}.
     */
    public TextStyle apply(Color color, Color backgroundColor, String fontFamily,
                           Double fontSizeFactor, Double fontSizeDelta, Object decoration) {
        TextStyle t = shallowClone();
        if (color != null) t.color = color;
        if (fontFamily != null) { t.fontFamily = fontFamily; t.fontPackage = null; }
        if (t.fontSize != null) {
            double factor = fontSizeFactor != null ? fontSizeFactor : 1.0;
            double delta = fontSizeDelta != null ? fontSizeDelta : 0.0;
            t.fontSize = t.fontSize * factor + delta;
        }
        if (backgroundColor != null) t.backgroundColor = backgroundColor;
        if (decoration instanceof TextDecoration) t.decoration = (TextDecoration) decoration;
        return t;
    }

    private Object fontFeatures;

    /// TextStyle.fontFeatures, recorded and not applied. Codename One's Font draws a
    /// face's default glyphs and exposes no OpenType feature selection (no GSUB/GPOS
    /// access), so FontFeature.tabularFigures() cannot switch a proportional face to
    /// its tabular digits; a face whose digits are tabular by default (most UI fonts'
    /// are) already lines numbers up. Use a monospaced family where alignment matters.
    public void fontFeatures(Object v) {
        this.fontFeatures = v;
    }

    public Object fontFeatures() {
        return fontFeatures;
    }
}
