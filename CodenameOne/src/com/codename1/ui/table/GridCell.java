/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.ui.table;

import com.codename1.ui.plaf.Style;

/// One cell of a `DataGrid` as a renderer or an editor sees it.
///
/// The grid hands the same object out for every cell it paints, so a cell is
/// read where it is given and never kept.
public final class GridCell {
    DataGrid grid;
    int row;
    GridColumn column;
    Object value;
    boolean rowSelected;
    boolean lead;
    boolean group;
    boolean editable;
    int depth;
    Style style;

    GridCell() {
    }

    /// The grid the cell is in.
    public DataGrid getGrid() {
        return grid;
    }

    /// The view row: an index into what the grid shows, not into the model.
    public int getRow() {
        return row;
    }

    /// The column the cell is in.
    public GridColumn getColumn() {
        return column;
    }

    /// The value the cell shows; for a group row, the group's key in the
    /// grouped column, an aggregate in a column with one, and null elsewhere.
    public Object getValue() {
        return value;
    }

    /// Whether the cell's row is selected.
    public boolean isRowSelected() {
        return rowSelected;
    }

    /// Whether this is the cell the keyboard is on.
    public boolean isLead() {
        return lead;
    }

    /// Whether the cell is on a group row.
    public boolean isGroupRow() {
        return group;
    }

    /// Whether the cell may be edited.
    public boolean isEditable() {
        return editable;
    }

    /// How deep the row is in a tree, or 1 for a row under a group.
    public int getDepth() {
        return depth;
    }

    /// The style the row is painted with: its colours, font and padding.
    public Style getStyle() {
        return style;
    }
}
