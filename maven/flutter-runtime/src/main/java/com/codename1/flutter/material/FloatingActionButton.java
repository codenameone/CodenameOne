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
import com.codename1.flutter.Element;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A material floating action button, backed by the real CN1
 * {@code com.codename1.components.FloatingActionButton}. M1 consumes an
 * {@link com.codename1.flutter.widgets.Icon Icon} child as configuration
 * (its glyph becomes the FAB icon); other child widgets are not mounted.
 */
public class FloatingActionButton extends Widget {

    private Funcs.VoidFunc0 onPressed;
    private String tooltip;
    private Widget child;
    private Object heroTag;
    private com.codename1.flutter.Color backgroundColor;
    private com.codename1.flutter.Color foregroundColor;
    private Double elevation;
    private Widget icon;
    private boolean isExtended;

    public void heroTag(Object v) {
        this.heroTag = v;
    }

    public void backgroundColor(com.codename1.flutter.Color v) {
        this.backgroundColor = v;
    }

    public void foregroundColor(com.codename1.flutter.Color v) {
        this.foregroundColor = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void tooltip(String v) {
        this.tooltip = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Funcs.VoidFunc0 getOnPressed() {
        return onPressed;
    }

    public String getTooltip() {
        return tooltip;
    }

    public Widget getChild() {
        return child;
    }

    /** The leading glyph of an extended FAB, or null. */
    public Widget getIcon() {
        return icon;
    }

    /** Whether this is the pill-shaped {@code FloatingActionButton.extended} form. */
    public boolean isExtended() {
        return isExtended;
    }

    public com.codename1.flutter.Color getBackgroundColor() {
        return backgroundColor;
    }

    public com.codename1.flutter.Color getForegroundColor() {
        return foregroundColor;
    }

    /**
     * {@code FloatingActionButton.extended}: a pill-shaped FAB carrying a label
     * and, usually, a leading glyph.
     *
     * <p>The icon and the background colour used to be dropped and the result
     * rendered as an ordinary round FAB with the default plus sign — which is
     * what every gallery study showed instead of its "Back to gallery" pill.</p>
     */
    public static FloatingActionButton extended(Key key, Funcs.VoidFunc0 onPressed, Widget label,
            Widget icon, String tooltip, Object heroTag, Color backgroundColor) {
        FloatingActionButton f = new FloatingActionButton();
        f.key(key);
        f.onPressed(onPressed);
        f.tooltip(tooltip);
        f.child(label);
        f.icon = icon;
        f.isExtended = true;
        f.heroTag(heroTag);
        if (backgroundColor != null) {
            f.backgroundColor(backgroundColor);
        }
        return f;
    }

    @Override
    public Element createElement() {
        return new FabRenderElement(this);
    }
}
