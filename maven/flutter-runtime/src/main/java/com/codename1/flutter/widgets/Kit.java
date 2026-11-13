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
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.MainAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.Widget;
import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * Small builders the composed Material widgets (dropdowns, navigation bar, table, ...)
 * assemble their trees from, so each reads as the Flutter widget tree it stands for.
 */
public final class Kit {
    private Kit() {
    }

    public static Column col(CrossAxisAlignment cross, Widget... children) {
        Column c = new Column();
        c.mainAxisSize(MainAxisSize.min);
        if (cross != null) {
            c.crossAxisAlignment(cross);
        }
        c.children(list(children));
        return c;
    }

    public static Row row(MainAxisAlignment main, Widget... children) {
        Row r = new Row();
        r.mainAxisSize(MainAxisSize.min);
        if (main != null) {
            r.mainAxisAlignment(main);
            r.mainAxisSize(MainAxisSize.max);
        }
        r.crossAxisAlignment(CrossAxisAlignment.center);
        r.children(list(children));
        return r;
    }

    public static DartList<Widget> list(Widget... children) {
        DartList<Widget> l = DartList.of();
        for (Widget w : children) {
            if (w != null) {
                l.add(w);
            }
        }
        return l;
    }

    public static Widget text(String s, com.codename1.flutter.TextStyle style) {
        Text t = new Text(s == null ? "" : s);
        if (style != null) {
            t.style(style);
        }
        return t;
    }

    public static Widget pad(double h, double v, Widget child) {
        Padding p = new Padding();
        p.padding(EdgeInsets.symmetric(h, v));
        p.child(child);
        return p;
    }

    public static Widget expanded(long flex, Widget child) {
        Expanded e = new Expanded();
        e.flex(flex);
        e.child(child);
        return e;
    }

    public static Widget tap(Funcs.VoidFunc0 onTap, Widget child) {
        if (onTap == null) {
            return child;
        }
        GestureDetector g = new GestureDetector();
        g.onTap(onTap);
        g.child(child);
        return g;
    }

    /** A box with an optional fill, rounded corners and a border of the given colour. */
    public static Widget box(Color fill, double radius, Color border, double borderWidth, Widget child) {
        com.codename1.flutter.BoxDecoration d = new com.codename1.flutter.BoxDecoration();
        if (fill != null) {
            d.color(fill);
        }
        if (radius > 0) {
            d.borderRadius(com.codename1.flutter.BorderRadius.circular(radius));
        }
        if (border != null) {
            d.border(com.codename1.flutter.Border.all(border, borderWidth, null, null));
        }
        Container c = new Container();
        c.decoration(d);
        c.child(child);
        return c;
    }

    public static Widget sized(Double width, Double height, Widget child) {
        SizedBox s = new SizedBox();
        if (width != null) {
            s.width(width.doubleValue());
        }
        if (height != null) {
            s.height(height.doubleValue());
        }
        s.child(child);
        return s;
    }
}
