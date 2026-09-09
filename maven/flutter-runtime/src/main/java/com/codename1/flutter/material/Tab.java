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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.SizedBox;
import com.codename1.flutter.widgets.Text;

import dart.core.DartList;

/**
 * A single tab label for a {@link TabBar} — Flutter's {@code Tab}. Composes its
 * {@code text} (and/or {@code icon}) as the visible content; an explicit
 * {@code child} overrides both.
 */
public class Tab extends StatelessWidget {

    private String text;
    private Widget icon;
    private Object iconMargin;
    private Double height;
    private Widget child;

    public void text(String v) {
        this.text = v;
    }

    public void icon(Widget v) {
        this.icon = v;
    }

    public void iconMargin(Object v) {
        this.iconMargin = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget build(BuildContext context) {
        if (child != null) {
            return child;
        }
        Widget label = text != null ? new Text(text) : null;
        if (icon != null && label != null) {
            DartList<Widget> kids = new DartList<Widget>();
            kids.add(icon);
            kids.add(label);
            Column col = new Column();
            col.children(kids);
            return col;
        }
        if (icon != null) {
            return icon;
        }
        if (label != null) {
            return label;
        }
        return new SizedBox();
    }
}
