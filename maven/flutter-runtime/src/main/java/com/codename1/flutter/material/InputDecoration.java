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
package com.codename1.flutter.material;

/**
 * Decoration configuration for a {@link TextField}. M3 renders both
 * {@code labelText} and {@code hintText} through the CN1 hint mechanism
 * (labelText wins when both are set) — a floating label is a later
 * milestone.
 */
public class InputDecoration {

    private String labelText;
    private String hintText;
    private String helperText;
    private String errorText;
    private String prefixText;
    private String suffixText;
    private com.codename1.flutter.Widget icon;
    private com.codename1.flutter.Widget prefixIcon;
    private com.codename1.flutter.Widget suffixIcon;
    private Boolean filled;
    private com.codename1.flutter.Color fillColor;
    private com.codename1.flutter.InputBorder border;
    private com.codename1.flutter.TextStyle labelStyle;
    private com.codename1.flutter.TextStyle hintStyle;
    private com.codename1.flutter.EdgeInsetsGeometry contentPadding;
    private FloatingLabelBehavior floatingLabelBehavior;

    public void labelText(String v) {
        this.labelText = v;
    }

    public void hintText(String v) {
        this.hintText = v;
    }

    public void helperText(String v) {
        this.helperText = v;
    }

    public void errorText(String v) {
        this.errorText = v;
    }

    public void prefixText(String v) {
        this.prefixText = v;
    }

    public void suffixText(String v) {
        this.suffixText = v;
    }

    public void icon(com.codename1.flutter.Widget v) {
        this.icon = v;
    }

    public void prefixIcon(com.codename1.flutter.Widget v) {
        this.prefixIcon = v;
    }

    public void suffixIcon(com.codename1.flutter.Widget v) {
        this.suffixIcon = v;
    }

    public void filled(boolean v) {
        this.filled = v;
    }

    public void fillColor(com.codename1.flutter.Color v) {
        this.fillColor = v;
    }

    public void border(com.codename1.flutter.InputBorder v) {
        this.border = v;
    }

    public void labelStyle(com.codename1.flutter.TextStyle v) {
        this.labelStyle = v;
    }

    public void hintStyle(com.codename1.flutter.TextStyle v) {
        this.hintStyle = v;
    }

    public void contentPadding(com.codename1.flutter.EdgeInsetsGeometry v) {
        this.contentPadding = v;
    }

    public void floatingLabelBehavior(FloatingLabelBehavior v) {
        this.floatingLabelBehavior = v;
    }

    public String getLabelText() {
        return labelText;
    }

    /** Whether the field paints a solid fill behind its content. */
    public boolean isFilled() {
        return filled != null && filled.booleanValue();
    }

    /** The fill colour, or null to take the theme's. */
    public com.codename1.flutter.Color getFillColor() {
        return fillColor;
    }

    /** The glyph shown before the content, or null. */
    /**
     * {@code icon} -- the glyph OUTSIDE the decorated box, to its left.
     *
     * <p>Distinct from {@code prefixIcon}, which sits inside the fill and the
     * border. The text-field demo uses this one for the person, phone and
     * envelope beside its fields.</p>
     */
    public com.codename1.flutter.Widget getIcon() {
        return icon;
    }

    /** {@code hintStyle} -- the type the placeholder is set in. */
    public com.codename1.flutter.TextStyle getHintStyle() {
        return hintStyle;
    }

    /** The hint drawn under the field, or null. */
    public String getHelperText() {
        return helperText;
    }

    public com.codename1.flutter.Widget getSuffixIcon() {
        return suffixIcon;
    }

    public com.codename1.flutter.Widget getPrefixIcon() {
        return prefixIcon;
    }

    /** The requested border, or null for the theme's. */
    public com.codename1.flutter.InputBorder getBorder() {
        return border;
    }

    /** The requested content padding, or null. */
    public com.codename1.flutter.EdgeInsetsGeometry getContentPadding() {
        return contentPadding;
    }

    public String getHintText() {
        return hintText;
    }

    /**
     * {@code InputDecoration.collapsed}: a minimal decoration with no label,
     * border or padding — only a hint. Styling parameters are accepted for API
     * shape and ignored at this milestone.
     */
    public static InputDecoration collapsed(String hintText, Object hintStyle, Object border,
            Boolean filled, com.codename1.flutter.Color fillColor) {
        InputDecoration d = new InputDecoration();
        d.hintText(hintText);
        // No border and no padding are what COLLAPSED means -- they are the whole point
        // of the factory, not defaults it happens to inherit. Keeping only the hint left
        // the result indistinguishable from a plain InputDecoration, so every collapsed
        // field still drew the full outlined box: Reply's compose screen boxed its
        // subject and its message body, where the reference has neither.
        d.border(com.codename1.flutter.InputBorder.none);
        d.contentPadding(com.codename1.flutter.EdgeInsets.all(0));
        if (border instanceof com.codename1.flutter.InputBorder) {
            d.border((com.codename1.flutter.InputBorder) border);
        }
        if (hintStyle instanceof com.codename1.flutter.TextStyle) {
            d.hintStyle((com.codename1.flutter.TextStyle) hintStyle);
        }
        if (filled != null) {
            d.filled(filled.booleanValue());
        }
        if (fillColor != null) {
            d.fillColor(fillColor);
        }
        return d;
    }
}
