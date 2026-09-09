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

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * One row of a {@link DataTable} — Flutter's {@code DataRow}. Holds the row's
 * {@link DataCell}s.
 */
public class DataRow {

    private DartList<DataCell> cells;
    private Boolean selected;
    private Long index;
    private Funcs.VoidFunc1<Boolean> onSelectChanged;

    public DataRow() {
    }

    /** Dart's {@code DataRow.byIndex} named constructor. */
    public static DataRow byIndex(long index,
                                  Boolean selected,
                                  Funcs.VoidFunc1<Boolean> onSelectChanged,
                                  Object onLongPress,
                                  Object color,
                                  DartList<DataCell> cells) {
        DataRow r = new DataRow();
        r.index = index;
        r.selected = selected;
        r.onSelectChanged = onSelectChanged;
        r.cells = cells;
        return r;
    }

    public void cells(DartList<DataCell> v) {
        this.cells = v;
    }

    public void selected(boolean v) {
        this.selected = v;
    }

    public void onSelectChanged(Funcs.VoidFunc1<Boolean> v) {
        this.onSelectChanged = v;
    }

    public void onLongPress(Object v) {
    }

    public void color(Object v) {
    }

    public DartList<DataCell> getCells() {
        return cells;
    }
}
