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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.MainAxisAlignment;

import dart.core.DartList;

/**
 * Lays its {@code children} out horizontally, falling back to a vertical column
 * when they do not fit — Flutter's {@code OverflowBar} (the modern
 * {@code ButtonBar}). This pass always renders the horizontal {@link Row} form;
 * the overflow-to-column behavior is deferred.
 */
public class OverflowBar extends StatelessWidget {

    private double spacing;
    private Object alignment;
    private double overflowSpacing;
    private Object overflowAlignment;
    private Object overflowDirection;
    private Object textDirection;
    private DartList<Widget> children;

    public void spacing(double v) {
        this.spacing = v;
    }

    public void alignment(Object v) {
        this.alignment = v;
    }

    public void overflowSpacing(double v) {
        this.overflowSpacing = v;
    }

    public void overflowAlignment(Object v) {
        this.overflowAlignment = v;
    }

    public void overflowDirection(Object v) {
        this.overflowDirection = v;
    }

    public void textDirection(Object v) {
        this.textDirection = v;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public double getSpacing() {
        return spacing;
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    @Override
    public Widget build(BuildContext context) {
        Row row = new Row();
        // Both of these were dropped, and both are visible. Shrine's login states an END
        // alignment and 8 logical pixels of spacing: its CANCEL and NEXT sat hard against
        // the left edge with no gap, where the reference has them apart and to the right.
        if (alignment instanceof MainAxisAlignment) {
            row.mainAxisAlignment((MainAxisAlignment) alignment);
        }
        row.children(spacing > 0 ? spaced(children) : children);
        return row;
    }

    /// The children with a gap of {@code spacing} between each pair.
    private DartList<Widget> spaced(DartList<Widget> kids) {
        if (kids == null || kids.size() < 2) {
            return kids;
        }
        DartList<Widget> out = new DartList<Widget>();
        for (int i = 0; i < kids.size(); i++) {
            if (i > 0) {
                SizedBox gap = new SizedBox();
                gap.width(spacing);
                out.add(gap);
            }
            out.add(kids.get(i));
        }
        return out;
    }
}
