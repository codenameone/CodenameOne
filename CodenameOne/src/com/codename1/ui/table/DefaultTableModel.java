/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.table;

import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.util.EventDispatcher;

import java.util.ArrayList;
import java.util.Collections;

/// A default implementation of the table model based on a two dimensional array.
///
/// ```java
/// Form hi = new Form("Table", new BorderLayout());
/// TableModel model = new DefaultTableModel(new String[] {"Col 1", "Col 2", "Col 3"}, new Object[][] {
///     {"Row 1", "Row A", "Row X"},
///     {"Row 2", "Row B can now stretch", null},
///     {"Row 3", "Row C", "Row Z"},
///     {"Row 4", "Row D", "Row K"},
///     }) {
///         public boolean isCellEditable(int row, int col) {
///             return col != 0;
///         }
///     };
/// Table table = new Table(model) {
/// @Override
///     protected Component createCell(Object value, int row, int column, boolean editable) { // (1)
///         Component cell;
///         if(row == 1 && column == 1) { // (2)
///             Picker p = new Picker();
///             p.setType(Display.PICKER_TYPE_STRINGS);
///             p.setStrings("Row B can now stretch", "This is a good value", "So Is This", "Better than text field");
///             p.setSelectedString((String)value); // (3)
///             p.setUIID("TableCell");
///             p.addActionListener((e) -> getModel().setValueAt(row, column, p.getSelectedString())); // (4)
///             cell = p;
///         } else {
///             cell = super.createCell(value, row, column, editable);
///         }
///         if(row > -1 && row % 2 == 0) { // (5)
///             // pinstripe effect
///             cell.getAllStyles().setBgColor(0xeeeeee);
///             cell.getAllStyles().setBgTransparency(255);
///         }
///         return cell;
///     }
/// @Override
///     protected TableLayout.Constraint createCellConstraint(Object value, int row, int column) {
///         TableLayout.Constraint con =  super.createCellConstraint(value, row, column);
///         if(row == 1 && column == 1) {
///             con.setHorizontalSpan(2);
///         }
///         con.setWidthPercentage(33);
///         return con;
///     }
/// };
/// hi.add(BorderLayout.CENTER, table);
/// hi.show();
/// ```
/// @author Shai Almog
public class DefaultTableModel extends AbstractTableModel {
    private final EventDispatcher dispatcher = new EventDispatcher();
    ArrayList<Object[]> data = new ArrayList<Object[]>();
    String[] columnNames;
    boolean editable;

    /// Constructs a new table with a 2 dimensional array for row/column data
    ///
    /// #### Parameters
    ///
    /// - `columnNames`: the names of the columns
    ///
    /// - `data`: the data within the table
    public DefaultTableModel(String[] columnNames, Object[][] data) {
        this(columnNames, data, false);
    }

    /// Constructs a new table with a 2 dimensional array for row/column data
    ///
    /// #### Parameters
    ///
    /// - `columnNames`: the names of the columns
    ///
    /// - `data`: the data within the table
    ///
    /// - `editable`: indicates whether table cells are editable or not by default
    ///
    /// #### See also
    ///
    /// - #isCellEditable(int, int)
    public DefaultTableModel(String[] columnNames, Object[][] data, boolean editable) {
        Collections.addAll(this.data, data);
        this.columnNames = columnNames;
        this.editable = editable;
    }

    DefaultTableModel(String[] columnNames, ArrayList<Object[]> data, boolean editable) {
        this.data = data;
        this.columnNames = columnNames;
        this.editable = editable;
    }

    /// {@inheritDoc}
    @Override
    public int getRowCount() {
        return data.size();
    }

    /// {@inheritDoc}
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    /// {@inheritDoc}
    @Override
    public String getColumnName(int i) {
        return columnNames[i];
    }

    /// {@inheritDoc}
    @Override
    public boolean isCellEditable(int row, int column) {
        return editable;
    }

    /// {@inheritDoc}
    @Override
    public Object getValueAt(int row, int column) {
        try {
            return data.get(row)[column];
        } catch (ArrayIndexOutOfBoundsException err) {
            // not the best situation but quite useful for the resource editor
            //err.printStackTrace();
            return "";
        }
    }

    /// {@inheritDoc}
    @Override
    public void setValueAt(int row, int column, Object o) {
        data.get(row)[column] = o;
        dispatcher.fireDataChangeEvent(column, row);
    }

    /// {@inheritDoc}
    @Override
    public void addDataChangeListener(DataChangedListener d) {
        dispatcher.addListener(d);
    }

    /// {@inheritDoc}
    @Override
    public void removeDataChangeListener(DataChangedListener d) {
        dispatcher.removeListener(d);
    }

    /// Adds the given row to the table data
    ///
    /// #### Parameters
    ///
    /// - `row`: array or row items, notice that row.length should match the column count exactly!
    public void addRow(Object... row) {
        data.add(row);
        for (int col = 0; col < row.length; col++) {
            dispatcher.fireDataChangeEvent(col, data.size() - 1);
        }
    }

    /// Inserts the given row to the table data at the given offset
    ///
    /// #### Parameters
    ///
    /// - `offset`: position within the table that is 0 or larger yet smaller than the row count
    ///
    /// - `row`: array or row items, notice that row.length should match the column count exactly!
    public void insertRow(int offset, Object... row) {
        data.add(offset, row);
        for (int col = 0; col < row.length; col++) {
            dispatcher.fireDataChangeEvent(col, data.size() - 1);
            dispatcher.fireDataChangeEvent(col, offset);
        }
    }

    /// Removes the given row offset from the table
    ///
    /// #### Parameters
    ///
    /// - `offset`: position within the table that is 0 or larger yet smaller than the row count
    public void removeRow(int offset) {
        data.remove(offset);
        dispatcher.fireDataChangeEvent(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }
}
