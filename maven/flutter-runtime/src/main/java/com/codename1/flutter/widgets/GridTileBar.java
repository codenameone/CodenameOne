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
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * The header/footer band shown inside a {@link GridTile} — Flutter's
 * {@code GridTileBar}: an optional {@code leading} widget, a {@code title} and
 * {@code subtitle}, and a {@code trailing} widget over a translucent
 * {@code backgroundColor}. This pass lays the pieces out as a horizontal
 * {@link Row}; precise Material spacing is deferred.
 */
public class GridTileBar extends StatelessWidget {

    private Color backgroundColor;
    private Widget leading;
    private Widget title;
    private Widget subtitle;
    private Widget trailing;

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void leading(Widget v) {
        this.leading = v;
    }

    public void title(Widget v) {
        this.title = v;
    }

    public void subtitle(Widget v) {
        this.subtitle = v;
    }

    public void trailing(Widget v) {
        this.trailing = v;
    }

    public Widget getTitle() {
        return title;
    }

    @Override
    public Widget build(BuildContext context) {
        Column texts = new Column();
        dart.core.DartList<Widget> lines = new dart.core.DartList<Widget>();
        if (title != null) {
            lines.add(title);
        }
        if (subtitle != null) {
            lines.add(subtitle);
        }
        texts.children(lines);

        Row row = new Row();
        dart.core.DartList<Widget> kids = new dart.core.DartList<Widget>();
        if (leading != null) {
            kids.add(leading);
        }
        kids.add(texts);
        if (trailing != null) {
            kids.add(trailing);
        }
        row.children(kids);
        return row;
    }
}
