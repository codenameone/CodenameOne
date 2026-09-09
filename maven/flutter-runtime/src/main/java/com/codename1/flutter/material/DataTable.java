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
import com.codename1.flutter.widgets.Expanded;
import com.codename1.flutter.widgets.Row;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A material data table — Flutter's {@code DataTable}. This milestone renders a
 * {@link Column} of {@link Row}s: a heading row of the {@code columns}' labels
 * followed by one row per {@link DataRow}, each cell wrapped in an
 * {@link Expanded} so the columns share the width. Sorting, selection checkboxes
 * and the material grid styling are deferred.
 */
public class DataTable extends StatelessWidget {

    private DartList<DataColumn> columns;
    private DartList<DataRow> rows;

    public void columns(DartList<DataColumn> v) {
        this.columns = v;
    }

    public void rows(DartList<DataRow> v) {
        this.rows = v;
    }

    public void sortColumnIndex(Long v) {
    }

    public void sortAscending(Boolean v) {
    }

    public void onSelectAll(Funcs.VoidFunc1<Boolean> v) {
    }

    public void dataRowHeight(Double v) {
    }

    public void headingRowHeight(Double v) {
    }

    public void horizontalMargin(Double v) {
    }

    public void columnSpacing(Double v) {
    }

    public void showCheckboxColumn(Boolean v) {
    }

    public void decoration(Object v) {
    }

    private static Row rowOf(DartList<Widget> cells) {
        DartList<Widget> flexed = new DartList<Widget>();
        for (int i = 0; i < cells.size(); i++) {
            Expanded e = new Expanded();
            e.child(cells.get(i));
            flexed.add(e);
        }
        Row r = new Row();
        r.children(flexed);
        return r;
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> body = new DartList<Widget>();
        if (columns != null) {
            DartList<Widget> labels = new DartList<Widget>();
            for (int i = 0; i < columns.size(); i++) {
                labels.add(columns.get(i).getLabel());
            }
            body.add(rowOf(labels));
        }
        if (rows != null) {
            for (int i = 0; i < rows.size(); i++) {
                DataRow dr = rows.get(i);
                DartList<Widget> cellWidgets = new DartList<Widget>();
                DartList<DataCell> cells = dr.getCells();
                if (cells != null) {
                    for (int j = 0; j < cells.size(); j++) {
                        cellWidgets.add(cells.get(j).getChild());
                    }
                }
                body.add(rowOf(cellWidgets));
            }
        }
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(body);
        return col;
    }
}
