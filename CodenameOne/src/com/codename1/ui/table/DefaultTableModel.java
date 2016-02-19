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

/**
 * <p>A default implementation of the table model based on a two dimensional array.
 * </p>
 *
 * <script src="https://gist.github.com/codenameone/6b106772ad1d58c50270.js"></script>
 * 
 * <img src="https://www.codenameone.com/img/developer-guide/components-table-pinstripe.png" alt="Table with customize cells using the pinstripe effect" />
 * <img src="https://www.codenameone.com/img/developer-guide/components-table-pinstripe-edit.png" alt="Picker table cell during edit" />
 * 
 * @author Shai Almog
 */
public class DefaultTableModel implements TableModel {
    ArrayList<Object[]> data = new ArrayList<Object[]>();
    String[] columnNames;
    private EventDispatcher dispatcher = new EventDispatcher();
    boolean editable;

    /**
     * Constructs a new table with a 2 dimensional array for row/column data
     *
     * @param columnNames the names of the columns
     * @param data the data within the table
     */
    public DefaultTableModel(String[] columnNames, Object[][] data) {
        this(columnNames, data, false);
    }

    /**
     * Constructs a new table with a 2 dimensional array for row/column data
     *
     * @param columnNames the names of the columns
     * @param data the data within the table
     * @param editable indicates whether table cells are editable or not by default
     * @see #isCellEditable(int, int) 
     */
    public DefaultTableModel(String[] columnNames, Object[][] data, boolean editable) {
        for(Object[] o : data) {
            this.data.add(o);
        }
        this.columnNames = columnNames;
        this.editable = editable;
    }

    DefaultTableModel(String[] columnNames, ArrayList<Object[]> data, boolean editable) {
        this.data = data;
        this.columnNames = columnNames;
        this.editable = editable;
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return data.size();
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
     * {@inheritDoc}
     */
    public String getColumnName(int i) {
        return columnNames[i];
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCellEditable(int row, int column) {
        return editable;
    }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(int row, int column) {
        try {
            return data.get(row)[column];
        } catch(ArrayIndexOutOfBoundsException err) {
            // not the best situation but quite useful for the resource editor
            //err.printStackTrace();
            return "";
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setValueAt(int row, int column, Object o) {
        data.get(row)[column] = o;
        dispatcher.fireDataChangeEvent(column, row);
    }

    /**
     * {@inheritDoc}
     */
    public void addDataChangeListener(DataChangedListener d) {
        dispatcher.addListener(d);
    }

    /**
     * {@inheritDoc}
     */
    public void removeDataChangeListener(DataChangedListener d) {
        dispatcher.removeListener(d);
    }

    /**
     * Adds the given row to the table data
     * 
     * @param row array or row items, notice that row.length should match the column count exactly!
     */
    public void addRow(Object... row) {
       data.add(row);
       for(int col = 0 ; col < row.length ; col++) {
           dispatcher.fireDataChangeEvent(col, data.size() - 1);
       }
    }

    /**
     * Inserts the given row to the table data at the given offset
     * 
     * @param offset position within the table that is 0 or larger yet smaller than the row count
     * @param row array or row items, notice that row.length should match the column count exactly!
     */
    public void insertRow(int offset, Object... row) {
       data.add(offset, row);
       for(int col = 0 ; col < row.length ; col++) {
           dispatcher.fireDataChangeEvent(col, data.size() - 1);
           dispatcher.fireDataChangeEvent(col, offset);
       }
    }

    /**
     * Removes the given row offset from the table
     * 
     * @param offset position within the table that is 0 or larger yet smaller than the row count
     */
    public void removeRow(int offset) {
       data.remove(offset);
       for(int col = 0 ; col < getColumnCount() ; col++) {
           dispatcher.fireDataChangeEvent(col, offset);
       }
    }
}
