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
import com.codename1.flutter.Color;
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Row;
import com.codename1.flutter.widgets.SizedBox;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A chip that triggers an action when pressed — Flutter's {@code ActionChip}.
 * This milestone renders {@code avatar} + {@code label} in a {@link Row}; the
 * {@code onPressed} tap is deferred.
 */
public class ActionChip extends StatelessWidget {

    private Widget avatar;
    private Widget label;
    private Color backgroundColor;
    private TextStyle labelStyle;
    private Funcs.VoidFunc0 onPressed;

    public void avatar(Widget v) {
        this.avatar = v;
    }

    public void label(Widget v) {
        this.label = v;
    }

    public void labelStyle(TextStyle v) {
        this.labelStyle = v;
    }

    public void labelPadding(Object v) {
    }

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void pressElevation(Object v) {
    }

    public void tooltip(Object v) {
    }

    public void side(Object v) {
    }

    public void shape(Object v) {
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void padding(Object v) {
    }

    public void elevation(double v) {
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = new DartList<Widget>();
        if (avatar != null) {
            kids.add(avatar);
        }
        if (label != null) {
            kids.add(label);
        }
        if (kids.size() == 0) {
            return new SizedBox();
        }
        Row row = new Row();
        row.mainAxisSize(MainAxisSize.min);
        row.crossAxisAlignment(CrossAxisAlignment.center);
        row.children(kids);

        // An ActionChip is a chip. Built as a bare row it was an icon and some
        // text loose on the page, with none of the outline that IS a chip's
        // appearance -- Chip itself had already been given the capsule and this
        // one had not. Material's default is an outline over the surface rather
        // than a fill.
        double h = 32;
        com.codename1.flutter.BoxDecoration d =
                new com.codename1.flutter.BoxDecoration();
        d.borderRadius(com.codename1.flutter.BorderRadius.circular(h / 2));
        com.codename1.flutter.BorderSide side = new com.codename1.flutter.BorderSide();
        side.color(outlineColor(context));
        side.width(1);
        d.border(com.codename1.flutter.Border.all(
                side.color(), 1, null, null));

        com.codename1.flutter.widgets.Container box =
                new com.codename1.flutter.widgets.Container();
        box.decoration(d);
        box.padding(com.codename1.flutter.EdgeInsets.symmetric(12, 0));
        box.constraints(new com.codename1.flutter.rendering.BoxConstraints(
                0, Double.POSITIVE_INFINITY, h, h));
        box.child(row);
        return box;
    }

    /** The chip's outline: the scheme's outline, or a mid grey without a theme. */
    private com.codename1.flutter.Color outlineColor(BuildContext context) {
        try {
            ThemeData t = Theme.of(context);
            if (t != null && t.colorScheme() != null
                    && t.colorScheme().outline() != null) {
                return t.colorScheme().outline();
            }
        } catch (Throwable noTheme) {
            // an unthemed chip still needs an outline
        }
        return new com.codename1.flutter.Color(0xFF79747EL);
    }
}
