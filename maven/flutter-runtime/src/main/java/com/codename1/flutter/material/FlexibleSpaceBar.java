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
import com.codename1.flutter.widgets.Kit;
import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * Flutter's {@code FlexibleSpaceBar}: {@code background} filling the app bar's flexible
 * space with {@code title} over its bottom-start corner (centred when asked).
 */
public class FlexibleSpaceBar extends StatelessWidget {
    private Widget title;
    private Widget background;
    private Boolean centerTitle;
    private Object titlePadding;
    public void title(Widget v) { this.title = v; }
    public void background(Widget v) { this.background = v; }
    public void centerTitle(Boolean v) { this.centerTitle = v; }
    public void titlePadding(Object v) { this.titlePadding = v; }
    public void collapseMode(Object v) { }
    public void stretchModes(Object v) { }
    public void expandedTitleScale(Object v) { }
    @Override
    public Widget build(BuildContext context) {
        com.codename1.flutter.widgets.Stack s = new com.codename1.flutter.widgets.Stack();
        s.fit(com.codename1.flutter.StackFit.expand);
        DartList<Widget> kids = DartList.of();
        if (background != null) {
            kids.add(background);
        }
        if (title != null) {
            com.codename1.flutter.widgets.Align a = new com.codename1.flutter.widgets.Align();
            a.alignment(Boolean.TRUE.equals(centerTitle) ? com.codename1.flutter.Alignment.bottomCenter
                    : com.codename1.flutter.Alignment.bottomLeft);
            com.codename1.flutter.widgets.Padding p = new com.codename1.flutter.widgets.Padding();
            p.padding(titlePadding instanceof com.codename1.flutter.EdgeInsetsGeometry
                    ? (com.codename1.flutter.EdgeInsetsGeometry) titlePadding
                    : com.codename1.flutter.EdgeInsets.fromLTRB(16, 0, 16, 16));
            com.codename1.flutter.TextStyle ts = new com.codename1.flutter.TextStyle();
            ts.fontSize(20);
            ts.color(Theme.of(context).colorScheme().onSurface());
            com.codename1.flutter.widgets.DefaultTextStyle d = new com.codename1.flutter.widgets.DefaultTextStyle();
            d.style(ts);
            d.child(title);
            p.child(d);
            a.child(p);
            kids.add(a);
        }
        s.children(kids);
        return s;
    }
}
