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

import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.DefaultListSelectionModel;
import com.codename1.desktopcompat.javax.swing.ListSelectionModel;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.EventListenerList;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionEvent;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionListener;
import com.codename1.desktopcompat.javax.swing.event.TableColumnModelEvent;
import com.codename1.desktopcompat.javax.swing.event.TableColumnModelListener;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Vector;

/// The usual column model: a vector of columns and a list selection model.
public class DefaultTableColumnModel
        implements TableColumnModel, PropertyChangeListener, ListSelectionListener {

    protected Vector<TableColumn> tableColumns = new Vector<TableColumn>();
    protected ListSelectionModel selectionModel;
    protected int columnMargin = 1;
    protected EventListenerList listenerList = new EventListenerList();
    protected transient ChangeEvent changeEvent;
    protected boolean columnSelectionAllowed;
    protected int totalColumnWidth = -1;

    public DefaultTableColumnModel() {
        setSelectionModel(createSelectionModel());
    }

    @Override
    public void addColumn(TableColumn aColumn) {
        if (aColumn == null) {
            throw new IllegalArgumentException("Object is null");
        }
        tableColumns.addElement(aColumn);
        aColumn.addPropertyChangeListener(this);
        totalColumnWidth = -1;
        fireColumnAdded(new TableColumnModelEvent(this, 0, getColumnCount() - 1));
    }

    @Override
    public void removeColumn(TableColumn column) {
        int index = tableColumns.indexOf(column);
        if (index < 0) {
            return;
        }
        if (selectionModel != null) {
            selectionModel.removeIndexInterval(index, index);
        }
        column.removePropertyChangeListener(this);
        tableColumns.removeElementAt(index);
        totalColumnWidth = -1;
        fireColumnRemoved(new TableColumnModelEvent(this, index, 0));
    }

    @Override
    public void moveColumn(int columnIndex, int newIndex) {
        int n = getColumnCount();
        if (columnIndex < 0 || columnIndex >= n || newIndex < 0 || newIndex >= n) {
            throw new IllegalArgumentException("moveColumn() - Index out of range");
        }
        if (columnIndex != newIndex) {
            TableColumn c = tableColumns.elementAt(columnIndex);
            tableColumns.removeElementAt(columnIndex);
            boolean selected = selectionModel.isSelectedIndex(columnIndex);
            selectionModel.removeIndexInterval(columnIndex, columnIndex);
            tableColumns.insertElementAt(c, newIndex);
            selectionModel.insertIndexInterval(newIndex, 1, true);
            if (selected) {
                selectionModel.addSelectionInterval(newIndex, newIndex);
            } else {
                selectionModel.removeSelectionInterval(newIndex, newIndex);
            }
        }
        fireColumnMoved(new TableColumnModelEvent(this, columnIndex, newIndex));
    }

    @Override
    public void setColumnMargin(int newMargin) {
        if (newMargin != columnMargin) {
            columnMargin = newMargin;
            fireColumnMarginChanged();
        }
    }

    @Override
    public int getColumnCount() {
        return tableColumns.size();
    }

    @Override
    public Enumeration<TableColumn> getColumns() {
        return tableColumns.elements();
    }

    @Override
    public int getColumnIndex(Object identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("Identifier is null");
        }
        for (int i = 0; i < tableColumns.size(); i++) {
            if (identifier.equals(tableColumns.elementAt(i).getIdentifier())) {
                return i;
            }
        }
        throw new IllegalArgumentException("Identifier not found");
    }

    @Override
    public TableColumn getColumn(int columnIndex) {
        return tableColumns.elementAt(columnIndex);
    }

    @Override
    public int getColumnMargin() {
        return columnMargin;
    }

    @Override
    public int getColumnIndexAtX(int x) {
        if (x < 0) {
            return -1;
        }
        int right = 0;
        for (int i = 0; i < tableColumns.size(); i++) {
            right += tableColumns.elementAt(i).getWidth();
            if (x < right) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getTotalColumnWidth() {
        if (totalColumnWidth < 0) {
            recalcWidthCache();
        }
        return totalColumnWidth;
    }

    @Override
    public void setSelectionModel(ListSelectionModel newModel) {
        if (newModel == null) {
            throw new IllegalArgumentException("Cannot set a null SelectionModel");
        }
        if (newModel != selectionModel) {
            if (selectionModel != null) {
                selectionModel.removeListSelectionListener(this);
            }
            selectionModel = newModel;
            newModel.addListSelectionListener(this);
        }
    }

    @Override
    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    @Override
    public void setColumnSelectionAllowed(boolean flag) {
        columnSelectionAllowed = flag;
    }

    @Override
    public boolean getColumnSelectionAllowed() {
        return columnSelectionAllowed;
    }

    @Override
    public int[] getSelectedColumns() {
        int n = getSelectedColumnCount();
        int[] out = new int[n];
        if (n > 0) {
            int at = 0;
            int max = selectionModel.getMaxSelectionIndex();
            for (int i = selectionModel.getMinSelectionIndex(); i <= max; i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    out[at++] = i;
                }
            }
        }
        return out;
    }

    @Override
    public int getSelectedColumnCount() {
        if (selectionModel == null) {
            return 0;
        }
        int min = selectionModel.getMinSelectionIndex();
        int max = selectionModel.getMaxSelectionIndex();
        int count = 0;
        if (min >= 0) {
            for (int i = min; i <= max; i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public void addColumnModelListener(TableColumnModelListener x) {
        listenerList.add(TableColumnModelListener.class, x);
    }

    @Override
    public void removeColumnModelListener(TableColumnModelListener x) {
        listenerList.remove(TableColumnModelListener.class, x);
    }

    public TableColumnModelListener[] getColumnModelListeners() {
        return listenerList.getListeners(TableColumnModelListener.class);
    }

    protected void fireColumnAdded(TableColumnModelEvent e) {
        TableColumnModelListener[] ls = getColumnModelListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].columnAdded(e);
        }
    }

    protected void fireColumnRemoved(TableColumnModelEvent e) {
        TableColumnModelListener[] ls = getColumnModelListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].columnRemoved(e);
        }
    }

    protected void fireColumnMoved(TableColumnModelEvent e) {
        TableColumnModelListener[] ls = getColumnModelListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].columnMoved(e);
        }
    }

    protected void fireColumnSelectionChanged(ListSelectionEvent e) {
        TableColumnModelListener[] ls = getColumnModelListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].columnSelectionChanged(e);
        }
    }

    protected void fireColumnMarginChanged() {
        TableColumnModelListener[] ls = getColumnModelListeners();
        if (ls.length == 0) {
            return;
        }
        if (changeEvent == null) {
            changeEvent = new ChangeEvent(this);
        }
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].columnMarginChanged(changeEvent);
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /// A column's width changed: the total is stale and the table must lay
    /// its cells out again.
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        if ("width".equals(name) || "preferredWidth".equals(name)) {
            totalColumnWidth = -1;
            fireColumnMarginChanged();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        fireColumnSelectionChanged(e);
    }

    protected ListSelectionModel createSelectionModel() {
        return new DefaultListSelectionModel();
    }

    protected void recalcWidthCache() {
        int total = 0;
        for (int i = 0; i < tableColumns.size(); i++) {
            total += tableColumns.elementAt(i).getWidth();
        }
        totalColumnWidth = total;
    }
}
