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
        NotchedShape notch = shape instanceof NotchedShape ? (NotchedShape) shape : null;
        c.height(HEIGHT_LP);
        c.alignment(com.codename1.flutter.Alignment.topCenter);
        c.child(child);
        // The bar CARRIES the display's bottom inset; it is not something the scaffold
        // paints behind it. Flutter's BottomAppBar wraps its child in a SafeArea, so the
        // bar measures its Material height plus the inset and everything applied to the
        // bar applies to both.
        //
        // That distinction is invisible at rest and decides what happens when the bar
        // animates. Reply folds its bar away as you read down the list, and a scaffold
        // that adds the inset separately keeps painting a band of bar colour across the
        // bottom of the screen after the bar itself has collapsed to nothing -- the bar
        // does not go away, it shrinks to a stripe. Owning the inset means the fold takes
        // it along and the bar vanishes, which is what the reference does.
        Widget body = withBottomInset(context, c);
        if (fill != null && notch != null) {
            // Painted through the shape rather than coloured: a Container fills its box,
            // and the whole point of a notched shape is that the box is not what should
            // be filled.
            NotchedSurface surface = new NotchedSurface();
            surface.shape(notch);
            surface.color(fill);
            surface.notchMargin(notchMargin != null ? notchMargin.doubleValue() : 4.0);
            surface.child(body);
            return surface;
        }
        if (fill == null) {
            return body;
        }
        Container filled = new Container();
        filled.color(fill);
        filled.child(body);
        return filled;
    }

    /// The bar's content plus the display's bottom inset below it, as Flutter's
    /// {@code SafeArea} inside BottomAppBar does.
    private static Widget withBottomInset(BuildContext context, Widget content) {
        double bottom = 0;
        try {
            com.codename1.flutter.EdgeInsets p =
                    com.codename1.flutter.MediaQuery.paddingOf(context);
            if (p != null) {
                bottom = p.bottom();
            }
        } catch (Throwable t) {
            // no MediaQuery in reach: the bar is simply its Material height
        }
        if (bottom <= 0) {
            return content;
        }
        com.codename1.flutter.widgets.Padding pad = new com.codename1.flutter.widgets.Padding();
        pad.padding(com.codename1.flutter.EdgeInsets.only(0, 0, 0, bottom));
        pad.child(content);
        return pad;
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
