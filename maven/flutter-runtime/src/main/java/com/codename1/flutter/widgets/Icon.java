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
package com.codename1.flutter.widgets;

import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.IconData;
import com.codename1.flutter.Widget;

/**
 * A material icon glyph, backed by a CN1 Label with a FontImage
 * (UIID "FlutterIcon"). Default size 24 logical pixels.
 */
public class Icon extends Widget {

    private IconData icon;
    private Double size;
    private Color color;
    private String semanticLabel;
    private Double fill;
    private Double weight;
    private Double grade;
    private Double opticalSize;
    private Object shadows;
    private com.codename1.flutter.TextDirection textDirection;
    private Boolean applyTextScaling;
    private com.codename1.flutter.BlendMode blendMode;
    private com.codename1.flutter.FontWeight fontWeight;

    public Icon(IconData icon) {
        this.icon = icon;
    }

    /**
     * For a program subclass (`class MyIcon extends Icon { MyIcon(super.icon) }`), whose
     * super-parameters arrive through the setters rather than this class's constructor.
     */
    protected Icon() {
    }

    /** The glyph, assigned by a subclass's {@code super.icon} parameter. */
    public void icon(IconData v) {
        this.icon = v;
    }

    public void semanticLabel(String v) {
        this.semanticLabel = v;
    }

    public String getSemanticLabel() {
        return semanticLabel;
    }

    public void size(double v) {
        this.size = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public IconData getIcon() {
        return icon;
    }

    public Double getSize() {
        return size;
    }

    public Color getColor() {
        return color;
    }

    public void fill(double v) {
        this.fill = v;
    }

    public Double fill() {
        return fill;
    }

    public void weight(double v) {
        this.weight = v;
    }

    public Double weight() {
        return weight;
    }

    public void grade(double v) {
        this.grade = v;
    }

    public Double grade() {
        return grade;
    }

    public void opticalSize(double v) {
        this.opticalSize = v;
    }

    public Double opticalSize() {
        return opticalSize;
    }

    public void shadows(Object v) {
        this.shadows = v;
    }

    public Object shadows() {
        return shadows;
    }

    public void textDirection(com.codename1.flutter.TextDirection v) {
        this.textDirection = v;
    }

    public com.codename1.flutter.TextDirection textDirection() {
        return textDirection;
    }

    public void applyTextScaling(boolean v) {
        this.applyTextScaling = v;
    }

    public Boolean applyTextScaling() {
        return applyTextScaling;
    }

    public void blendMode(com.codename1.flutter.BlendMode v) {
        this.blendMode = v;
    }

    public com.codename1.flutter.BlendMode blendMode() {
        return blendMode;
    }

    public void fontWeight(com.codename1.flutter.FontWeight v) {
        this.fontWeight = v;
    }

    public com.codename1.flutter.FontWeight fontWeight() {
        return fontWeight;
    }

    /** Dart's {@code icon} getter (the glyph, or null for an empty square). */
    public IconData icon() {
        return icon;
    }

    /** Dart's {@code size} getter: the explicit size, or null to follow IconTheme. */
    public Double size() {
        return size;
    }

    /** Dart's {@code color} getter. */
    public Color color() {
        return color;
    }

    /** Dart's {@code semanticLabel} getter. */
    public String semanticLabel() {
        return semanticLabel;
    }

    /**
     * Flutter's {@code Icon} is a {@code StatelessWidget}, so a subclass may override
     * {@code build} to draw itself some other way (an icon font package's own icon class
     * does exactly that). The exact class keeps its direct render element; a subclass is
     * hosted by a composed element that calls {@link #build}, whose default is the glyph.
     */
    @Override
    public Element createElement() {
        if (getClass() == Icon.class) {
            return new IconRenderElement(this);
        }
        return new SubclassElement(this);
    }

    /**
     * The default build: this icon's glyph, drawn by {@link IconRenderElement}. A
     * subclass that overrides {@code build} may still call {@code super.build(context)}.
     */
    public Widget build(com.codename1.flutter.BuildContext context) {
        Icon glyph = new Icon(icon);
        glyph.size = size;
        glyph.color = color;
        glyph.semanticLabel = semanticLabel;
        glyph.fill = fill;
        glyph.weight = weight;
        glyph.grade = grade;
        glyph.opticalSize = opticalSize;
        glyph.shadows = shadows;
        glyph.textDirection = textDirection;
        glyph.applyTextScaling = applyTextScaling;
        glyph.blendMode = blendMode;
        glyph.fontWeight = fontWeight;
        return glyph;
    }

    private static final class SubclassElement extends com.codename1.flutter.ComposedElement {
        SubclassElement(Icon widget) {
            super(widget);
        }

        @Override
        protected Widget build() {
            return ((Icon) widget()).build(this);
        }
    }
}
