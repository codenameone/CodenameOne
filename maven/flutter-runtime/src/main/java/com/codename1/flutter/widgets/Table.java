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
import com.codename1.flutter.widgets.Kit;
import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * Flutter's {@code Table}: rows of cells in aligned columns. Each column is a
 * {@link FixedColumnWidth} or a {@link FlexColumnWidth} share ({@code columnWidths},
 * else {@code defaultColumnWidth}, else flex 1); every row is as tall as its tallest
 * cell; a {@link TableBorder} outlines each cell.
 */
public class Table extends StatelessWidget {
    private DartList<?> children = DartList.of();
    private java.util.Map<?, ?> columnWidths;
    private TableColumnWidth defaultColumnWidth;
    private TableBorder border;
    private Object defaultVerticalAlignment;
    public void children(DartList<?> v) { this.children = v == null ? DartList.of() : v; }
    public void columnWidths(java.util.Map<?, ?> v) { this.columnWidths = v; }
    public void defaultColumnWidth(TableColumnWidth v) { this.defaultColumnWidth = v; }
    public void border(TableBorder v) { this.border = v; }
    public void defaultVerticalAlignment(Object v) { this.defaultVerticalAlignment = v; }
    public void textDirection(Object v) { }
    public void textBaseline(Object v) { }

    private TableColumnWidth widthOf(int col) {
        Object w = columnWidths == null ? null : columnWidths.get(Long.valueOf(col));
        if (w == null && columnWidths != null) {
            w = columnWidths.get(Integer.valueOf(col));
        }
        if (w instanceof TableColumnWidth) {
            return (TableColumnWidth) w;
        }
        return defaultColumnWidth != null ? defaultColumnWidth : new FlexColumnWidth(1.0);
    }

    private static com.codename1.flutter.CrossAxisAlignment cross(Object align) {
        String a = align == null ? "top" : align.toString();
        if (a.endsWith("middle")) {
            return com.codename1.flutter.CrossAxisAlignment.center;
        }
        if (a.endsWith("bottom")) {
            return com.codename1.flutter.CrossAxisAlignment.end;
        }
        if (a.endsWith("fill")) {
            return com.codename1.flutter.CrossAxisAlignment.stretch;
        }
        return com.codename1.flutter.CrossAxisAlignment.start;
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> rows = DartList.of();
        for (Object o : children) {
            if (!(o instanceof TableRow)) {
                continue;
            }
            TableRow tr = (TableRow) o;
            DartList<Widget> cells = DartList.of();
            int c = 0;
            for (Widget cell : tr.getChildren()) {
                Object va = cell instanceof TableCell && ((TableCell) cell).getVerticalAlignment() != null
                        ? ((TableCell) cell).getVerticalAlignment() : defaultVerticalAlignment;
                Column holder = new Column();
                holder.crossAxisAlignment(com.codename1.flutter.CrossAxisAlignment.stretch);
                String a = va == null ? "top" : va.toString();
                holder.mainAxisAlignment(a.endsWith("middle") ? com.codename1.flutter.MainAxisAlignment.center
                        : a.endsWith("bottom") ? com.codename1.flutter.MainAxisAlignment.end
                        : com.codename1.flutter.MainAxisAlignment.start);
                holder.children(Kit.list(cell));
                Widget w = border == null ? holder : Kit.box(null, 0, border.getColor(), border.getWidth(), holder);
                TableColumnWidth cw = widthOf(c++);
                cells.add(cw.fixedWidth() >= 0 ? Kit.sized(Double.valueOf(cw.fixedWidth()), null, w)
                        : Kit.expanded(Math.max(1, Math.round(cw.flexFactor() * 1000)), w));
            }
            Row r = new Row();
            r.crossAxisAlignment(com.codename1.flutter.CrossAxisAlignment.stretch);
            r.children(cells);
            IntrinsicHeight ih = new IntrinsicHeight();
            ih.child(r);
            Widget row = ih;
            if (tr.getDecoration() != null) {
                DecoratedBox db = new DecoratedBox();
                db.decoration(tr.getDecoration());
                db.child(ih);
                row = db;
            }
            rows.add(row);
        }
        return Kit.col(com.codename1.flutter.CrossAxisAlignment.stretch, rows.toArray(new Widget[0]));
    }
}
