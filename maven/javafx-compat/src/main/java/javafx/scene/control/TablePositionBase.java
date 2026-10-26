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

/// A cell of a table named by its row and its column, rather than by the
/// node that happens to show it.
public abstract class TablePositionBase<TC extends TableColumnBase> {

    private final int row;
    private final TC tableColumn;

    /// Creates the position of a row in a column.
    protected TablePositionBase(int row, TC tableColumn) {
        this.row = row;
        this.tableColumn = tableColumn;
    }

    /// Returns the row.
    public int getRow() {
        return row;
    }

    /// Returns the index of the column among the columns of the table, -1
    /// when it is in none.
    public abstract int getColumn();

    /// Returns the column.
    public TC getTableColumn() {
        return tableColumn;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TablePositionBase)) {
            return false;
        }
        TablePositionBase<?> other = (TablePositionBase<?>) obj;
        return row == other.row && tableColumn == other.tableColumn;
    }

    @Override
    public int hashCode() {
        return 79 * (79 * 5 + row) + (tableColumn == null ? 0 : tableColumn.hashCode());
    }
}
