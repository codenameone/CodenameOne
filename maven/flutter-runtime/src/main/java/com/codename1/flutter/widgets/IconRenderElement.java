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

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.Style;

/**
 * Leaf render box for {@link Icon}: a CN1 Label carrying a material
 * FontImage sized in millimeters equivalent to the requested logical pixels.
 */
public class IconRenderElement extends RenderElement {

    /** Flutter's default icon size in logical pixels. */
    public static final double DEFAULT_SIZE_LP = 24;

    /// Glyph rasterisation cost, and how much of it is REPEATED work: a
    /// material icon is drawn into an image per element, so the same glyph at
    /// the same size and colour is rasterised once per place it appears.
    private static long glyphMs;
    private static int glyphCount;
    private static final java.util.Set<String> GLYPHS = new java.util.HashSet<String>();

    /** Icon rasterisations, distinct glyphs among them, and the cost. */
    public static String glyphCost() {
        return glyphCount + " icon(s) (" + GLYPHS.size() + " distinct) in " + glyphMs + "ms";
    }

    public IconRenderElement(Icon widget) {
        super(widget);
    }

    private Icon icon() {
        return (Icon) widget();
    }

    /**
     * The ambient icon theme, or an empty one.
     *
     * <p>Resolved per paint rather than cached: the theme an icon sits under can
     * change when an ancestor rebuilds, and an icon that sampled its colour once
     * would keep the first one forever.</p>
     */
    private com.codename1.flutter.material.IconThemeData ambient() {
        try {
            return com.codename1.flutter.material.IconTheme.of(this);
        } catch (Throwable t) {
            return new com.codename1.flutter.material.IconThemeData();
        }
    }

    private double sizeLp() {
        return sizeLp(icon().getSize() != null ? null : ambient());
    }

    private double sizeLp(com.codename1.flutter.material.IconThemeData themed) {
        if (icon().getSize() != null) {
            return icon().getSize();
        }
        Double size = themed == null ? null : themed.size();
        return size != null ? size.doubleValue() : DEFAULT_SIZE_LP;
    }

    /** {@code Icon.color}, else the ambient {@code IconTheme}'s, else the default ink. */
    private com.codename1.flutter.Color effectiveColor(
            com.codename1.flutter.material.IconThemeData themed) {
        if (icon().getColor() != null) {
            return icon().getColor();
        }
        return themed == null ? null : themed.color();
    }

    @Override
    protected Component createComponent() {
        if (!com.codename1.ui.Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        Label l = new Label("", "FlutterIcon");
        l.getAllStyles().setPadding(0, 0, 0, 0);
        l.getAllStyles().setMargin(0, 0, 0, 0);
        applyIcon(l);
        return l;
    }

    @Override
    protected void updateComponent(Component c) {
        applyIcon((Label) c);
    }

    private void applyIcon(Label l) {
        if (icon().getIcon() == null) {
            l.setIcon(null);
            return;
        }
        // One lookup for both the colour and the size: each is a walk to the
        // root of the element tree, and this runs for every icon on screen.
        com.codename1.flutter.material.IconThemeData themed =
                (icon().getColor() == null || icon().getSize() == null) ? ambient() : null;
        Style s = new Style(l.getUnselectedStyle());
        com.codename1.flutter.Color fg = effectiveColor(themed);
        if (fg != null) {
            s.setFgColor(fg.rgb());
            l.getAllStyles().setFgColor(fg.rgb());
        }
        s.setBgTransparency(0);
        try {
            long g0 = System.currentTimeMillis();
            l.setIcon(FontImage.createMaterial(icon().getIcon().codePoint(), s,
                    Dp.mm(sizeLp(themed))));
            glyphMs += System.currentTimeMillis() - g0;
            glyphCount++;
            GLYPHS.add(icon().getIcon().codePoint() + "/" + (int) sizeLp(themed)
                    + "/" + (fg == null ? -1 : fg.rgb()));
        } catch (Exception err) {
            // headless or missing icon font: layout still reserves the box
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double px = Dp.px(sizeLp());
        return constraints.constrain(new Size(px, px));
    }
}
