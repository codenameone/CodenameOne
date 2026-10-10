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
package com.codename1.desktopcompat.javax.swing.table;

import com.codename1.desktopcompat.javax.swing.event.TableModelEvent;
import java.util.Vector;

/// A table model that keeps its values in a vector of row vectors, and
/// its column names in another. Every cell is editable.
@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultTableModel extends AbstractTableModel {

    protected Vector dataVector;
    protected Vector columnIdentifiers;

    public DefaultTableModel() {
        this(0, 0);
    }

    public DefaultTableModel(int rowCount, int columnCount) {
        this(sized(columnCount), rowCount);
    }

    public DefaultTableModel(Vector columnNames, int rowCount) {
        setDataVector(sized(rowCount), columnNames);
    }

    public DefaultTableModel(Object[] columnNames, int rowCount) {
        this(convertToVector(columnNames), rowCount);
    }

    public DefaultTableModel(Vector data, Vector columnNames) {
        setDataVector(data, columnNames);
    }

    public DefaultTableModel(Object[][] data, Object[] columnNames) {
        setDataVector(data, columnNames);
    }

    private static Vector sized(int size) {
        Vector v = new Vector(size);
        v.setSize(size);
        return v;
    }

    private static Vector orEmpty(Vector v) {
        return v != null ? v : new Vector();
    }

    /// The vector of row vectors itself, not a copy.
    public Vector getDataVector() {
        return dataVector;
    }

    public void setDataVector(Vector dataVector, Vector columnIdentifiers) {
        this.dataVector = orEmpty(dataVector);
        this.columnIdentifiers = orEmpty(columnIdentifiers);
        justifyRows(0, getRowCount());
        fireTableStructureChanged();
    }

    public void setDataVector(Object[][] dataVector, Object[] columnIdentifiers) {
        setDataVector(convertToVector(dataVector), convertToVector(columnIdentifiers));
    }

    public void newDataAvailable(TableModelEvent event) {
        fireTableChanged(event);
    }

    /// Gives every row from `from` up to `to` a vector exactly as long as
    /// there are columns.
    private void justifyRows(int from, int to) {
        dataVector.setSize(getRowCount());
        for (int i = from; i < to; i++) {
            Object row = dataVector.elementAt(i);
            if (!(row instanceof Vector)) {
                row = new Vector();
                dataVector.setElementAt(row, i);
            }
            ((Vector) row).setSize(getColumnCount());
        }
    }

    public void newRowsAdded(TableModelEvent e) {
        justifyRows(e.getFirstRow(), e.getLastRow() + 1);
        fireTableChanged(e);
    }

    public void rowsRemoved(TableModelEvent event) {
        fireTableChanged(event);
    }

    public void setNumRows(int rowCount) {
        int old = getRowCount();
        if (old == rowCount) {
            return;
        }
        dataVector.setSize(rowCount);
        if (rowCount <= old) {
            fireTableRowsDeleted(rowCount, old - 1);
        } else {
            justifyRows(old, rowCount);
            fireTableRowsInserted(old, rowCount - 1);
        }
    }

    public void setRowCount(int rowCount) {
        setNumRows(rowCount);
    }

    public void addRow(Vector rowData) {
        insertRow(getRowCount(), rowData);
    }

    public void addRow(Object[] rowData) {
        addRow(convertToVector(rowData));
    }

    public void insertRow(int row, Vector rowData) {
        dataVector.insertElementAt(rowData, row);
        justifyRows(row, row + 1);
        fireTableRowsInserted(row, row);
    }

    public void insertRow(int row, Object[] rowData) {
        insertRow(row, convertToVector(rowData));
    }

    /// Moves the rows `start` to `end` so that the first of them ends up
    /// at `to`.
    public void moveRow(int start, int end, int to) {
        int count = end - start + 1;
        int size = getRowCount();
        if (start < 0 || end >= size || count <= 0 || to < 0 || to + count > size) {
            throw new ArrayIndexOutOfBoundsException("moveRow " + start + ".." + end + " to " + to);
        }
        if (to == start) {
            return;
        }
        Object[] moved = new Object[count];
        for (int i = 0; i < count; i++) {
            moved[i] = dataVector.elementAt(start);
            dataVector.removeElementAt(start);
        }
        for (int i = 0; i < count; i++) {
            dataVector.insertElementAt(moved[i], to + i);
        }
        fireTableRowsUpdated(Math.min(start, to), Math.max(end, to + count - 1));
    }

    public void removeRow(int row) {
        dataVector.removeElementAt(row);
        fireTableRowsDeleted(row, row);
    }

    public void setColumnIdentifiers(Vector columnIdentifiers) {
        setDataVector(dataVector, columnIdentifiers);
    }

    public void setColumnIdentifiers(Object[] newIdentifiers) {
        setColumnIdentifiers(convertToVector(newIdentifiers));
    }

    public void setColumnCount(int columnCount) {
        columnIdentifiers.setSize(columnCount);
        justifyRows(0, getRowCount());
        fireTableStructureChanged();
    }

    public void addColumn(Object columnName) {
        addColumn(columnName, (Vector) null);
    }

    public void addColumn(Object columnName, Vector columnData) {
        columnIdentifiers.addElement(columnName);
        if (columnData != null) {
            int n = columnData.size();
            if (n > getRowCount()) {
                dataVector.setSize(n);
            }
            justifyRows(0, getRowCount());
            int column = getColumnCount() - 1;
            for (int i = 0; i < n; i++) {
                ((Vector) dataVector.elementAt(i)).setElementAt(columnData.elementAt(i), column);
            }
        } else {
            justifyRows(0, getRowCount());
        }
        fireTableStructureChanged();
    }

    public void addColumn(Object columnName, Object[] columnData) {
        addColumn(columnName, convertToVector(columnData));
    }

    @Override
    public int getRowCount() {
        return dataVector.size();
    }

    @Override
    public int getColumnCount() {
        return columnIdentifiers.size();
    }

    @Override
    public String getColumnName(int column) {
        Object id = column >= 0 && column < columnIdentifiers.size() ? columnIdentifiers.elementAt(column) : null;
        return id == null ? super.getColumnName(column) : id.toString();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return true;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Object r = dataVector.elementAt(row);
        return r instanceof Vector ? ((Vector) r).elementAt(column) : null;
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
        Object r = dataVector.elementAt(row);
        if (r instanceof Vector) {
            ((Vector) r).setElementAt(aValue, column);
            fireTableCellUpdated(row, column);
        }
    }

    protected static Vector convertToVector(Object[] anArray) {
        if (anArray == null) {
            return null;
        }
        Vector v = new Vector(anArray.length);
        for (int i = 0; i < anArray.length; i++) {
            v.addElement(anArray[i]);
        }
        return v;
    }

    protected static Vector convertToVector(Object[][] anArray) {
        if (anArray == null) {
            return null;
        }
        Vector v = new Vector(anArray.length);
        for (int i = 0; i < anArray.length; i++) {
            v.addElement(convertToVector(anArray[i]));
        }
        return v;
    }
}
