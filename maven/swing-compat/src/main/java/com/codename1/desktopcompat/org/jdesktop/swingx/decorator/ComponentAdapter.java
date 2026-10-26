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
package com.codename1.desktopcompat.org.jdesktop.swingx.decorator;

import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.JComponent;

/// The cell a renderer component is being prepared for, in terms that are
/// the same for a table, a list and a tree.
///
/// [#row] and [#column] are the cell in view coordinates. The methods that
/// take a row and a column take model coordinates unless they say
/// otherwise.
public abstract class ComponentAdapter {

    public static final Object DEFAULT_COLUMN_IDENTIFIER = "Column0";

    /// The view row of the cell.
    public int row = 0;

    /// The view column of the cell.
    public int column = 0;

    protected final JComponent target;

    public ComponentAdapter(JComponent component) {
        target = component;
    }

    public JComponent getComponent() {
        return target;
    }

    /// The name of the column at a model index.
    public String getColumnName(int columnIndex) {
        return null;
    }

    /// The identifier of the column at a model index.
    public Object getColumnIdentifierAt(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= getColumnCount()) {
            throw new ArrayIndexOutOfBoundsException("invalid column index: " + columnIndex);
        }
        return DEFAULT_COLUMN_IDENTIFIER;
    }

    /// The model index of the column with the identifier, or -1.
    public int getColumnIndex(Object identifier) {
        for (int i = 0; i < getColumnCount(); i++) {
            if (identifier != null && identifier.equals(getColumnIdentifierAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /// Whether the model column can be shown, which a hidden one cannot.
    public boolean isTestable(int column) {
        return column >= 0 && column < getColumnCount();
    }

    public Class<?> getColumnClass(int column) {
        return Object.class;
    }

    /// The class of the cell's column.
    public Class<?> getColumnClass() {
        return getColumnClass(convertColumnIndexToModel(column));
    }

    public int getColumnCount() {
        return 1;
    }

    public int getRowCount() {
        return 0;
    }

    /// The value at a model row and model column.
    public abstract Object getValueAt(int row, int column);

    /// Whether the cell at a model row and model column can be edited.
    public abstract boolean isCellEditable(int row, int column);

    /// The text of the cell.
    public String getString() {
        return getString(convertColumnIndexToModel(column));
    }

    /// The text of the cell in the same row at a model column.
    public String getString(int modelColumnIndex) {
        return getFilteredStringAt(row, modelColumnIndex);
    }

    /// The text at a view row and a model column.
    public String getFilteredStringAt(int row, int column) {
        return getStringAt(convertRowIndexToModel(row), column);
    }

    /// The text at a model row and model column: the value's `toString`.
    public String getStringAt(int row, int column) {
        Object value = getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    /// The value of the cell.
    public Object getValue() {
        return getValue(convertColumnIndexToModel(column));
    }

    /// The value in the cell's row at a model column.
    public Object getValue(int modelColumnIndex) {
        return getFilteredValueAt(row, modelColumnIndex);
    }

    /// The value at a view row and a model column.
    public Object getFilteredValueAt(int row, int column) {
        return getValueAt(convertRowIndexToModel(row), column);
    }

    /// The bounds of the cell in its component.
    public Rectangle getCellBounds() {
        return target.getBounds();
    }

    public abstract boolean hasFocus();

    public abstract boolean isSelected();

    public abstract boolean isEditable();

    public boolean isExpanded() {
        return false;
    }

    public boolean isLeaf() {
        return false;
    }

    /// Whether the cell is the one that shows the tree structure.
    public boolean isHierarchical() {
        return false;
    }

    /// The depth of the cell's node, 0 where there is no hierarchy.
    public int getDepth() {
        return 0;
    }

    public int convertColumnIndexToView(int columnModelIndex) {
        return columnModelIndex;
    }

    public int convertColumnIndexToModel(int columnViewIndex) {
        return columnViewIndex;
    }

    public int convertRowIndexToView(int rowModelIndex) {
        return rowModelIndex;
    }

    public int convertRowIndexToModel(int rowViewIndex) {
        return rowViewIndex;
    }
}
