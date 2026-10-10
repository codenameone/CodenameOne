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
package javafx.scene.control;

import javafx.beans.NamedArg;

/// A cell of a [TableView], by row and column.
///
/// Unlike in JavaFX the position does not remember the item of its row:
/// it is read from the table when asked for.
public class TablePosition<S, T> extends TablePositionBase<TableColumn<S, T>> {

    private final TableView<S> tableView;

    /// Creates the position of a row in a column of a table.
    public TablePosition(@NamedArg("tableView") TableView<S> tableView, @NamedArg("row") int row,
            @NamedArg("tableColumn") TableColumn<S, T> tableColumn) {
        super(row, tableColumn);
        this.tableView = tableView;
    }

    @Override
    public int getColumn() {
        TableColumn<S, T> column = getTableColumn();
        return tableView == null || column == null ? -1 : tableView.getColumns().indexOf(column);
    }

    /// Returns the table.
    public final TableView<S> getTableView() {
        return tableView;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TablePosition && super.equals(obj) && tableView == ((TablePosition<?, ?>) obj).tableView;
    }

    @Override
    public int hashCode() {
        return 79 * super.hashCode() + (tableView == null ? 0 : tableView.hashCode());
    }

    @Override
    public String toString() {
        return "TablePosition [ row: " + getRow() + ", column: " + getColumn() + " ]";
    }
}
