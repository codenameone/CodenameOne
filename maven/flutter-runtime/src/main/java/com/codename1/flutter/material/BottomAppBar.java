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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Clip;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Container;

/**
 * A material bottom app bar: a bar docked to the bottom of a {@link Scaffold},
 * typically hosting a row of actions and (with a {@code shape}) a notch for a
 * docked FloatingActionButton. The notch geometry is retained as configuration
 * but not yet cut.
 *
 * <p>It used to build a bare container with neither the theme's colour nor the
 * Material height, so a bar that names no colour of its own — which is the
 * usual case, since the theme supplies it — came out invisible.</p>
 */
public class BottomAppBar extends StatelessWidget {

    private Color color;
    private Double elevation;
    private Object shape;
    private Double notchMargin;
    private Clip clipBehavior;
    private Widget child;

    public void color(Color v) {
        this.color = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void notchMargin(double v) {
        this.notchMargin = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Color getColor() {
        return color;
    }

    public Double getElevation() {
        return elevation;
    }

    public Object getShape() {
        return shape;
    }

    public Widget getChild() {
        return child;
    }

    /** Material 3's bottom app bar height in logical pixels -- 80, not M2's 56. */
    public static final double HEIGHT_LP = 80;

    @Override
    public Widget build(BuildContext context) {
        Container c = new Container();
        Color fill = color != null ? color : themedColor(context);
        if (fill != null) {
            c.color(fill);
        }
        // Exactly the Material height, with no safe-area padding of its own: the
        // reference draws this bar at 80 logical pixels even on a screen that
        // HAS a bottom inset, so the inset is not the bar's to carry. Where the
        // reply study looks 114 tall it is 80 of bar over 34 of the scaffold's
        // own dark background.
        c.height(HEIGHT_LP);
        c.child(child);
        return c;
    }

    /** {@code BottomAppBarTheme.color}, then the surface the bar sits on. */
    private static Color themedColor(BuildContext context) {
        try {
            ThemeData theme = Theme.of(context);
            BottomAppBarThemeData bar = theme.bottomAppBarTheme();
            if (bar != null && bar.color() != null) {
                return bar.color();
            }
            return theme.colorScheme().surface();
        } catch (Throwable t) {
            return null;
        }
    }
}
