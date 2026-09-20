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

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Label;

/**
 * Leaf render box for {@link Divider}: the element occupies the full
 * {@code height} extent while the owned strip component (UIID
 * "FlutterDivider") is shrunk to the {@code thickness} line centered inside
 * it, painted via its background color.
 */
public class DividerRenderElement extends RenderElement {

    /** Flutter's default divider extent in logical pixels. */
    public static final double DEFAULT_HEIGHT_LP = 16;
    /** Default painted line thickness in logical pixels. */
    public static final double DEFAULT_THICKNESS_LP = 1;
    /** Material light-theme divider color (black at ~12% on white). */
    private static final int DEFAULT_COLOR = 0xE0E0E0;

    public DividerRenderElement(Divider widget) {
        super(widget);
    }

    private Divider divider() {
        return (Divider) widget();
    }

    /// The ambient divider theme, or null.
    ///
    /// Every dimension below reads widget, then theme, then Flutter's default -- the
    /// precedence Divider.createBorderSide uses. Reading only the widget meant a study that
    /// states its rules once in its ThemeData got none of them.
    private DividerThemeData theme() {
        try {
            ThemeData t = Theme.of(this);
            return t == null ? null : t.dividerTheme();
        } catch (Throwable err) {
            return null;
        }
    }

    private double heightLp() {
        if (divider().getHeight() != null) {
            return divider().getHeight();
        }
        DividerThemeData dt = theme();
        if (dt != null && dt.space() != null) {
            return dt.space();
        }
        return DEFAULT_HEIGHT_LP;
    }

    private double thicknessLp() {
        if (divider().getThickness() != null) {
            return divider().getThickness();
        }
        DividerThemeData dt = theme();
        if (dt != null && dt.thickness() != null) {
            return dt.thickness();
        }
        return DEFAULT_THICKNESS_LP;
    }

    private double indentLp() {
        if (divider().getIndent() != null) {
            return divider().getIndent();
        }
        DividerThemeData dt = theme();
        return dt != null && dt.indent() != null ? dt.indent() : 0;
    }

    private double endIndentLp() {
        if (divider().getEndIndent() != null) {
            return divider().getEndIndent();
        }
        DividerThemeData dt = theme();
        return dt != null && dt.endIndent() != null ? dt.endIndent() : 0;
    }

    /// Widget colour, then the divider theme's, then the scheme's outlineVariant -- and
    /// black when there is no theme at all, which is the colour a BorderSide defaults to.
    private int colorRgb() {
        if (divider().getColor() != null) {
            return divider().getColor().rgb();
        }
        DividerThemeData dt = theme();
        if (dt != null && dt.color() != null) {
            return dt.color().rgb();
        }
        try {
            ColorScheme cs = Theme.of(this).colorScheme();
            if (cs != null && cs.outlineVariant() != null) {
                return cs.outlineVariant().rgb();
            }
        } catch (Throwable err) {
            // no ambient theme: fall through
        }
        return DEFAULT_COLOR;
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        Label strip = new Label("", "FlutterDivider");
        strip.getAllStyles().setPadding(0, 0, 0, 0);
        strip.getAllStyles().setMargin(0, 0, 0, 0);
        applyStyle(strip);
        return strip;
    }

    @Override
    protected void updateComponent(Component c) {
        applyStyle(c);
    }

    private void applyStyle(Component strip) {
        strip.getAllStyles().setBgColor(colorRgb());
        strip.getAllStyles().setBgTransparency(255);
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double w = constraints.hasBoundedWidth() ? constraints.maxWidth() : 0;
        return constraints.constrain(new Size(w, Dp.px(heightLp())));
    }

    @Override
    public void position(int x, int y) {
        super.position(x, y);
        Component strip = component();
        if (strip != null) {
            int t = Math.max(1, (int) Math.round(Dp.px(thicknessLp())));
            t = (int) Math.min(t, Math.round(size().height()));
            strip.setY(y + (int) Math.round((size().height() - t) / 2));
            strip.setHeight(t);
            // The rule is inset from both ends; the BOX is not. Flutter puts the indent on
            // the line's own margin, so a divider still occupies the full width it was
            // given and only the painted part is short.
            int start = (int) Math.round(Dp.px(indentLp()));
            int end = (int) Math.round(Dp.px(endIndentLp()));
            int w = (int) Math.round(size().width()) - start - end;
            if (w > 0) {
                strip.setX(x + start);
                strip.setWidth(w);
            }
        }
    }
}
