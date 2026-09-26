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
import com.codename1.flutter.EdgeInsets;
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
 * A compact material chip carrying a label and optional avatar/delete icon —
 * Flutter's {@code Chip}. This milestone renders the {@code avatar} and
 * {@code label} in a horizontal {@link Row}; the rounded background, delete
 * affordance and material styling are deferred.
 */
public class Chip extends StatelessWidget {

    private Widget avatar;
    private Widget label;
    private Widget deleteIcon;
    private Color backgroundColor;
    private Color deleteIconColor;
    private TextStyle labelStyle;
    private Funcs.VoidFunc0 onDeleted;

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

    public void deleteIcon(Widget v) {
        this.deleteIcon = v;
    }

    public void onDeleted(Funcs.VoidFunc0 v) {
        this.onDeleted = v;
    }

    public void deleteIconColor(Color v) {
        this.deleteIconColor = v;
    }

    public void deleteButtonTooltipMessage(String v) {
    }

    public void side(Object v) {
    }

    public void shape(Object v) {
    }

    public void clipBehavior(Clip v) {
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void padding(Object v) {
    }

    public void visualDensity(Object v) {
    }

    public void materialTapTargetSize(Object v) {
    }

    public void elevation(double v) {
    }

    public void shadowColor(Color v) {
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
        if (deleteIcon != null) {
            kids.add(deleteIcon);
        }
        if (kids.size() == 0) {
            return new SizedBox();
        }
        Row row = new Row();
        row.mainAxisSize(MainAxisSize.min);
        row.crossAxisAlignment(CrossAxisAlignment.center);
        row.children(kids);

        // A chip is a CAPSULE, not a row. Built as a bare row it was an avatar and some
        // text loose on the page -- the compose page's recipient had no pill behind it at
        // all, and neither did anything else that uses one.
        com.codename1.flutter.BoxDecoration d =
                new com.codename1.flutter.BoxDecoration();
        d.color(resolveBackground(context));
        d.borderRadius(com.codename1.flutter.BorderRadius.circular(HEIGHT_LP / 2));

        com.codename1.flutter.widgets.Container box =
                new com.codename1.flutter.widgets.Container();
        box.decoration(d);
        box.padding(labelPadding(context));
        // Height only: a chip is exactly 32 logical pixels tall and as wide as it needs.
        box.constraints(new com.codename1.flutter.rendering.BoxConstraints(
                0, Double.POSITIVE_INFINITY, HEIGHT_LP, HEIGHT_LP));
        box.child(row);
        return box;
    }

    /** Material's chip height in logical pixels. */
    private static final double HEIGHT_LP = 32;
    /** Material's default label padding, horizontal only. */
    private static final double LABEL_HPAD_LP = 8;

    private EdgeInsets labelPadding(BuildContext context) {
        ChipThemeData t = chipTheme(context);
        if (t != null && t.labelPadding() != null) {
            return t.labelPadding();
        }
        return EdgeInsets.symmetric(LABEL_HPAD_LP, 0);
    }

    /// The chip's own colour, then the theme's, then Material's surface.
    private Color resolveBackground(BuildContext context) {
        if (backgroundColor != null) {
            return backgroundColor;
        }
        ChipThemeData t = chipTheme(context);
        if (t != null && t.backgroundColor() != null) {
            return t.backgroundColor();
        }
        try {
            return Theme.of(context).colorScheme().surfaceVariant();
        } catch (Throwable err) {
            return null;
        }
    }

    private ChipThemeData chipTheme(BuildContext context) {
        try {
            ThemeData t = Theme.of(context);
            return t == null ? null : t.chipTheme();
        } catch (Throwable err) {
            return null;
        }
    }
}
