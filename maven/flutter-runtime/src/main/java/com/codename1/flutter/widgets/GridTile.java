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

/**
 * A single tile of a Material grid — Flutter's {@code GridTile}. An optional
 * {@code header}/{@code footer} band (typically a {@link GridTileBar}) overlays the main
 * {@code child}, pinned to the top and bottom edges.
 *
 * <p>Both bands were dropped before, so every tile in the grid demo lost its caption.
 */
public class GridTile extends StatelessWidget {

    private Widget header;
    private Widget footer;
    private Widget child;

    public void header(Widget v) {
        this.header = v;
    }

    public void footer(Widget v) {
        this.footer = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getHeader() {
        return header;
    }

    public Widget getFooter() {
        return footer;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        if (header == null && footer == null) {
            return child;
        }
        dart.core.DartList<Widget> layers = new dart.core.DartList<Widget>();
        if (child != null) {
            layers.add(Positioned.fill(null, null, null, null, null, child));
        }
        if (header != null) {
            Positioned p = new Positioned();
            p.top(0);
            p.left(0);
            p.right(0);
            p.child(header);
            layers.add(p);
        }
        if (footer != null) {
            Positioned p = new Positioned();
            p.bottom(0);
            p.left(0);
            p.right(0);
            p.child(footer);
            layers.add(p);
        }
        Stack stack = new Stack();
        stack.children(layers);
        return stack;
    }
}
