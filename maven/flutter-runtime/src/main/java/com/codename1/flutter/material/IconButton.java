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

import com.codename1.flutter.Color;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * The material icon button: a bare tappable icon, backed by a CN1 Button
 * (UIID "FlutterIconButton"). The {@code icon} widget is consumed as
 * configuration (an {@link com.codename1.flutter.widgets.Icon Icon}'s glyph
 * becomes the material icon).
 */
public class IconButton extends Widget {

    private Funcs.VoidFunc0 onPressed;
    private Widget icon;
    private Double iconSize;
    private Color color;
    private String tooltip;
    private EdgeInsets padding;
    private Color hoverColor;

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void tooltip(String v) {
        this.tooltip = v;
    }

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void hoverColor(Color v) {
        this.hoverColor = v;
    }

    public void splashRadius(double v) {
    }

    public void alignment(Object v) {
    }

    public void visualDensity(Object v) {
    }

    public String getTooltip() {
        return tooltip;
    }

    public void icon(Widget v) {
        this.icon = v;
    }

    public void iconSize(double v) {
        this.iconSize = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public Funcs.VoidFunc0 getOnPressed() {
        return onPressed;
    }

    public Widget getIcon() {
        return icon;
    }

    public Double getIconSize() {
        return iconSize;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public Element createElement() {
        return new ButtonRenderElement(this);
    }

    /** Flutter's {@code IconButton.filled}: the icon on a primary-coloured circle. */
    public static IconButton filled(com.codename1.flutter.Key key, Funcs.VoidFunc0 onPressed, Widget icon,
            Double iconSize, Color color) {
        return variant("filled", key, onPressed, icon, iconSize, color);
    }

    /** Flutter's {@code IconButton.filledTonal}: on a secondary-container circle. */
    public static IconButton filledTonal(com.codename1.flutter.Key key, Funcs.VoidFunc0 onPressed, Widget icon,
            Double iconSize, Color color) {
        return variant("tonal", key, onPressed, icon, iconSize, color);
    }

    /** Flutter's {@code IconButton.outlined}: inside an outline-coloured ring. */
    public static IconButton outlined(com.codename1.flutter.Key key, Funcs.VoidFunc0 onPressed, Widget icon,
            Double iconSize, Color color) {
        return variant("outlined", key, onPressed, icon, iconSize, color);
    }

    private String variant;

    /** "filled", "tonal", "outlined", or null for the standard icon button. */
    public String getVariant() {
        return variant;
    }

    private static IconButton variant(String kind, com.codename1.flutter.Key key, Funcs.VoidFunc0 onPressed,
            Widget icon, Double iconSize, Color color) {
        IconButton b = new IconButton();
        b.key(key);
        b.onPressed(onPressed);
        b.icon(icon);
        if (iconSize != null) b.iconSize(iconSize.doubleValue());
        if (color != null) b.color(color);
        b.variant = kind;
        return b;
    }
}
