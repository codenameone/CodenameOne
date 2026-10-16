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

import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * Scroll boundary and content for {@link MasonryGridView}.
 *
 * <p>Built as a row of columns: item <i>i</i> goes to column <i>i % n</i>, with
 * the requested gaps between columns and between items. That is not Flutter's
 * shortest-column rule, so at more than one column the vertical offsets can
 * differ where items have very unequal heights; at one column — the mobile
 * layout, and the only one the gallery uses on a phone — it is exactly right.
 *
 * <p>It previously returned no content at all, so the scrollable rendered
 * EMPTY. Nothing reported it: Crane's destination list, the whole point of the
 * screen, was simply absent, and the sweep stayed quiet because an empty
 * scrollable throws nothing.</p>
 */
public class MasonryGridViewRenderElement extends ScrollRenderElement {

    public MasonryGridViewRenderElement(MasonryGridView widget) {
        super(widget);
    }

    private MasonryGridView grid() {
        return (MasonryGridView) widget();
    }

    @Override
    protected Widget buildContent() {
        MasonryGridView g = grid();
        if (g.getItemBuilder() == null || g.getItemCount() == null) {
            return null;
        }
        int count = (int) Math.max(0, g.getItemCount().longValue());
        int columns = (int) Math.max(1, g.getCrossAxisCount());
        if (count == 0) {
            return null;
        }

        DartList<DartList<Widget>> byColumn = new DartList<DartList<Widget>>();
        for (int c = 0; c < columns; c++) {
            byColumn.add(new DartList<Widget>());
        }
        for (int i = 0; i < count; i++) {
            Widget item = g.getItemBuilder().call(this, Long.valueOf(i));
            if (item == null) {
                continue;
            }
            DartList<Widget> column = byColumn.get(i % columns);
            if (!column.isEmpty() && g.getMainAxisSpacing() > 0) {
                column.add(gap(0, g.getMainAxisSpacing()));
            }
            column.add(item);
        }

        if (columns == 1) {
            return column(byColumn.get(0));
        }

        DartList<Widget> row = new DartList<Widget>();
        for (int c = 0; c < columns; c++) {
            if (c > 0 && g.getCrossAxisSpacing() > 0) {
                row.add(gap(g.getCrossAxisSpacing(), 0));
            }
            Expanded e = new Expanded();
            e.child(column(byColumn.get(c)));
            row.add(e);
        }
        Row r = new Row();
        r.children(row);
        r.crossAxisAlignment(CrossAxisAlignment.start);
        r.mainAxisSize(MainAxisSize.max);
        return r;
    }

    private static Widget column(DartList<Widget> children) {
        Column c = new Column();
        c.children(children);
        c.crossAxisAlignment(CrossAxisAlignment.stretch);
        c.mainAxisSize(MainAxisSize.min);
        return c;
    }

    private static Widget gap(double width, double height) {
        SizedBox b = new SizedBox();
        if (width > 0) {
            b.width(width);
        }
        if (height > 0) {
            b.height(height);
        }
        return b;
    }
}
