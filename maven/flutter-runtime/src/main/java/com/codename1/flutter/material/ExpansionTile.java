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
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A single-line {@link ListTile} that expands to reveal children — Flutter's
 * {@code ExpansionTile}. This milestone renders the {@code title} followed by
 * the {@code children}; the expand/collapse toggle that fires
 * {@code onExpansionChanged} is deferred (children render expanded).
 */
public class ExpansionTile extends StatelessWidget {

    private Widget title;
    private Widget subtitle;
    private Widget leading;
    private Widget trailing;
    private DartList<Widget> children;
    private boolean initiallyExpanded;
    private Funcs.VoidFunc1<Boolean> onExpansionChanged;

    public void title(Widget v) {
        this.title = v;
    }

    public void subtitle(Widget v) {
        this.subtitle = v;
    }

    public void leading(Widget v) {
        this.leading = v;
    }

    public void trailing(Widget v) {
        this.trailing = v;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public void initiallyExpanded(boolean v) {
        this.initiallyExpanded = v;
    }

    public void onExpansionChanged(Funcs.VoidFunc1<Boolean> v) {
        this.onExpansionChanged = v;
    }

    public void childrenPadding(Object v) {
    }

    public void backgroundColor(Color v) {
    }

    public void collapsedBackgroundColor(Color v) {
    }

    public void textColor(Color v) {
    }

    public void iconColor(Color v) {
    }

    public void tilePadding(Object v) {
    }

    public void expandedAlignment(Object v) {
    }

    public void expandedCrossAxisAlignment(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = new DartList<Widget>();
        ListTile header = new ListTile();
        if (leading != null) {
            header.leading(leading);
        }
        if (title != null) {
            header.title(title);
        }
        if (subtitle != null) {
            header.subtitle(subtitle);
        }
        if (trailing != null) {
            header.trailing(trailing);
        }
        kids.add(header);
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                kids.add(children.get(i));
            }
        }
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(kids);
        return col;
    }
}
