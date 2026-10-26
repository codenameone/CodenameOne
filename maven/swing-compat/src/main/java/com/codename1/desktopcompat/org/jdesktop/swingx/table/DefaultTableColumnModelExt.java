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
package com.codename1.desktopcompat.org.jdesktop.swingx.table;

import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.event.EventListenerList;
import com.codename1.desktopcompat.javax.swing.event.TableColumnModelListener;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableColumnModel;
import com.codename1.desktopcompat.javax.swing.table.TableColumn;
import com.codename1.desktopcompat.org.jdesktop.swingx.event.TableColumnModelExtListener;
import java.util.ArrayList;
import java.util.List;

/// The column model of a `JXTable`: hiding a [TableColumnExt] takes it
/// out of the visible columns, as a removal the listeners see, and
/// showing it again puts it back where it was among the others.
///
/// While a listener is told of such a removal or addition,
/// [#isRemovedToInvisibleEvent(int)] or
/// [#isAddedFromInvisibleEvent(int)] answers `true`.
public class DefaultTableColumnModelExt extends DefaultTableColumnModel implements TableColumnModelExt {

    /// Every column, hidden ones included, in order.
    private final List<TableColumn> cn1All = new ArrayList<TableColumn>();
    private boolean cn1Hiding;
    private boolean cn1Showing;

    private final PropertyChangeListener cn1Watch = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            Object source = evt.getSource();
            if ("visible".equals(evt.getPropertyName()) && source instanceof TableColumnExt) {
                TableColumnExt column = (TableColumnExt) source;
                if (column.isVisible()) {
                    moveToVisible(column);
                } else {
                    moveToInvisible(column);
                }
            }
            fireColumnPropertyChange(evt);
        }
    };

    public DefaultTableColumnModelExt() {
        super();
    }

    @Override
    public List<TableColumn> getColumns(boolean includeHidden) {
        if (includeHidden) {
            return new ArrayList<TableColumn>(cn1All);
        }
        return new ArrayList<TableColumn>(tableColumns);
    }

    @Override
    public int getColumnCount(boolean includeHidden) {
        return includeHidden ? cn1All.size() : getColumnCount();
    }

    @Override
    public TableColumnExt getColumnExt(Object identifier) {
        if (identifier == null) {
            return null;
        }
        for (int i = 0; i < cn1All.size(); i++) {
            TableColumn c = cn1All.get(i);
            if (c instanceof TableColumnExt && identifier.equals(c.getIdentifier())) {
                return (TableColumnExt) c;
            }
        }
        return null;
    }

    @Override
    public TableColumnExt getColumnExt(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= getColumnCount()) {
            return null;
        }
        TableColumn c = getColumn(columnIndex);
        return c instanceof TableColumnExt ? (TableColumnExt) c : null;
    }

    /// Whether a removal the listeners are being told of right now is a
    /// column being hidden.
    public boolean isRemovedToInvisibleEvent(int oldIndex) {
        return cn1Hiding;
    }

    /// Whether an addition the listeners are being told of right now is
    /// a hidden column being shown.
    public boolean isAddedFromInvisibleEvent(int newIndex) {
        return cn1Showing;
    }

    private static boolean cn1Hidden(TableColumn c) {
        return c instanceof TableColumnExt && !((TableColumnExt) c).isVisible();
    }

    @Override
    public void removeColumn(TableColumn column) {
        if (!cn1All.remove(column)) {
            return;
        }
        column.removePropertyChangeListener(cn1Watch);
        super.removeColumn(column);
    }

    /// Adds a column at the end. One that is hidden already is kept
    /// without becoming visible.
    @Override
    public void addColumn(TableColumn aColumn) {
        if (aColumn == null) {
            throw new IllegalArgumentException("Object is null");
        }
        cn1All.add(aColumn);
        aColumn.addPropertyChangeListener(cn1Watch);
        if (!cn1Hidden(aColumn)) {
            super.addColumn(aColumn);
        }
    }

    /// Moves a visible column among the visible ones; the hidden ones
    /// keep their places between them.
    @Override
    public void moveColumn(int columnIndex, int newIndex) {
        super.moveColumn(columnIndex, newIndex);
        int next = 0;
        for (int i = 0; i < cn1All.size(); i++) {
            if (!cn1Hidden(cn1All.get(i)) && next < tableColumns.size()) {
                cn1All.set(i, tableColumns.elementAt(next++));
            }
        }
    }

    protected void moveToInvisible(TableColumnExt col) {
        if (!tableColumns.contains(col)) {
            return;
        }
        cn1Hiding = true;
        try {
            super.removeColumn(col);
        } finally {
            cn1Hiding = false;
        }
    }

    protected void moveToVisible(TableColumnExt col) {
        if (tableColumns.contains(col) || !cn1All.contains(col)) {
            return;
        }
        cn1Showing = true;
        try {
            super.addColumn(col);
        } finally {
            cn1Showing = false;
        }
        int target = 0;
        for (int i = 0; i < cn1All.size(); i++) {
            TableColumn c = cn1All.get(i);
            if (c == col) {
                break;
            }
            if (!cn1Hidden(c)) {
                target++;
            }
        }
        int last = getColumnCount() - 1;
        if (target < last) {
            super.moveColumn(last, target);
        }
    }

    protected EventListenerList getEventListenerList() {
        return listenerList;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
    }

    /// Tells the listeners that are [TableColumnModelExtListener]s of a
    /// column's property.
    protected void fireColumnPropertyChange(PropertyChangeEvent evt) {
        TableColumnModelListener[] listeners = getColumnModelListeners();
        for (int i = listeners.length - 1; i >= 0; i--) {
            if (listeners[i] instanceof TableColumnModelExtListener) {
                ((TableColumnModelExtListener) listeners[i]).columnPropertyChange(evt);
            }
        }
    }

    @Override
    public void addColumnModelListener(TableColumnModelListener x) {
        super.addColumnModelListener(x);
    }

    @Override
    public void removeColumnModelListener(TableColumnModelListener x) {
        super.removeColumnModelListener(x);
    }

    public TableColumnModelExtListener[] getTableColumnModelExtListeners() {
        TableColumnModelListener[] listeners = getColumnModelListeners();
        List<TableColumnModelExtListener> out = new ArrayList<TableColumnModelExtListener>();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] instanceof TableColumnModelExtListener) {
                out.add((TableColumnModelExtListener) listeners[i]);
            }
        }
        return out.toArray(new TableColumnModelExtListener[out.size()]);
    }
}
