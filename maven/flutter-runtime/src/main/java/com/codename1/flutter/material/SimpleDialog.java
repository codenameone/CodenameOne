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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;

/**
 * A Material dialog presenting an optional {@code title} above a vertical list
 * of option {@code children} (typically {@link SimpleDialogOption}s) — Flutter's
 * {@code SimpleDialog}. This pass composes the title and options into a
 * {@link Column}; dialog chrome (shape, elevation, inset padding) is recorded
 * for API shape and rendered by the enclosing dialog host.
 */
public class SimpleDialog extends StatelessWidget {

    private Widget title;
    private EdgeInsets titlePadding;
    private TextStyle titleTextStyle;
    private DartList<Widget> children;
    private EdgeInsets contentPadding;
    private Color backgroundColor;
    private Double elevation;
    private Color shadowColor;
    private Color surfaceTintColor;
    private String semanticLabel;
    private EdgeInsets insetPadding;
    private Clip clipBehavior = Clip.none;
    private Object shape;
    private Object alignment;

    public void title(Widget v) {
        this.title = v;
    }

    public void titlePadding(EdgeInsets v) {
        this.titlePadding = v;
    }

    public void titleTextStyle(TextStyle v) {
        this.titleTextStyle = v;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public void contentPadding(EdgeInsets v) {
        this.contentPadding = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void shadowColor(Color v) {
        this.shadowColor = v;
    }

    public void surfaceTintColor(Color v) {
        this.surfaceTintColor = v;
    }

    public void semanticLabel(String v) {
        this.semanticLabel = v;
    }

    public void insetPadding(EdgeInsets v) {
        this.insetPadding = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v == null ? Clip.none : v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void alignment(Object v) {
        this.alignment = v;
    }

    public Widget getTitle() {
        return title;
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = new DartList<Widget>();
        if (title != null) {
            kids.add(title);
        }
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                kids.add(children.get(i));
            }
        }
        Column col = new Column();
        col.children(kids);
        return col;
    }
}
