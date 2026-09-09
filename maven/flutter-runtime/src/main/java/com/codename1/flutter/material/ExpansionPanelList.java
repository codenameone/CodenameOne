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
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A material list of expandable {@link ExpansionPanel}s — Flutter's
 * {@code ExpansionPanelList}. This milestone renders each panel's header
 * followed by its body; header taps that fire {@code expansionCallback} and the
 * expand/collapse animation are deferred (bodies render expanded).
 */
public class ExpansionPanelList extends StatelessWidget {

    private DartList<ExpansionPanel> children;
    private Funcs.VoidFunc2<Long, Boolean> expansionCallback;

    public void children(DartList<ExpansionPanel> v) {
        this.children = v;
    }

    public void expansionCallback(Funcs.VoidFunc2<Long, Boolean> v) {
        this.expansionCallback = v;
    }

    public void animationDuration(Object v) {
    }

    public void expandedHeaderPadding(Object v) {
    }

    public void elevation(double v) {
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = new DartList<Widget>();
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                ExpansionPanel p = children.get(i);
                if (p.getHeaderBuilder() != null) {
                    Widget header = p.getHeaderBuilder().call(context, p.isExpanded());
                    if (header != null) {
                        kids.add(header);
                    }
                }
                if (p.getBody() != null) {
                    kids.add(p.getBody());
                }
            }
        }
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(kids);
        return col;
    }
}
