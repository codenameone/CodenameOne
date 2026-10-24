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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.SwingConstants;
import com.codename1.desktopcompat.javax.swing.event.SwingPropertyChangeSupport;

/// One column of a table as it is shown: the model column it takes its
/// values from, its width and the limits of it, its header value and the
/// renderers and editor that replace the table's defaults.
///
/// Widths are logical pixels.
public class TableColumn {

    public static final String COLUMN_WIDTH_PROPERTY = "columWidth";
    public static final String HEADER_VALUE_PROPERTY = "headerValue";
    public static final String HEADER_RENDERER_PROPERTY = "headerRenderer";
    public static final String CELL_RENDERER_PROPERTY = "cellRenderer";

    protected int modelIndex;
    protected Object identifier;
    protected int width;
    protected int minWidth;
    protected int maxWidth;
    protected TableCellRenderer headerRenderer;
    protected Object headerValue;
    protected TableCellRenderer cellRenderer;
    protected TableCellEditor cellEditor;
    protected boolean isResizable;

    private int preferredWidth;
    private SwingPropertyChangeSupport changeSupport;

    public TableColumn() {
        this(0);
    }

    public TableColumn(int modelIndex) {
        this(modelIndex, 75, null, null);
    }

    public TableColumn(int modelIndex, int width) {
        this(modelIndex, width, null, null);
    }

    public TableColumn(int modelIndex, int width, TableCellRenderer cellRenderer, TableCellEditor cellEditor) {
        this.modelIndex = modelIndex;
        this.width = Math.max(width, 0);
        this.preferredWidth = this.width;
        this.cellRenderer = cellRenderer;
        this.cellEditor = cellEditor;
        this.minWidth = Math.min(15, this.width);
        this.maxWidth = Integer.MAX_VALUE;
        this.isResizable = true;
    }

    private void fire(String name, Object old, Object now) {
        if (changeSupport != null) {
            changeSupport.firePropertyChange(name, old, now);
        }
    }

    private void fire(String name, int old, int now) {
        if (changeSupport != null && old != now) {
            changeSupport.firePropertyChange(name, Integer.valueOf(old), Integer.valueOf(now));
        }
    }

    public void setModelIndex(int modelIndex) {
        int old = this.modelIndex;
        this.modelIndex = modelIndex;
        fire("modelIndex", old, modelIndex);
    }

    public int getModelIndex() {
        return modelIndex;
    }

    public void setIdentifier(Object identifier) {
        Object old = this.identifier;
        this.identifier = identifier;
        fire("identifier", old, identifier);
    }

    /// The identifier, or the header value when none was set.
    public Object getIdentifier() {
        return identifier != null ? identifier : getHeaderValue();
    }

    public void setHeaderValue(Object headerValue) {
        Object old = this.headerValue;
        this.headerValue = headerValue;
        fire(HEADER_VALUE_PROPERTY, old, headerValue);
    }

    public Object getHeaderValue() {
        return headerValue;
    }

    public void setHeaderRenderer(TableCellRenderer headerRenderer) {
        Object old = this.headerRenderer;
        this.headerRenderer = headerRenderer;
        fire(HEADER_RENDERER_PROPERTY, old, headerRenderer);
    }

    public TableCellRenderer getHeaderRenderer() {
        return headerRenderer;
    }

    public void setCellRenderer(TableCellRenderer cellRenderer) {
        Object old = this.cellRenderer;
        this.cellRenderer = cellRenderer;
        fire(CELL_RENDERER_PROPERTY, old, cellRenderer);
    }

    public TableCellRenderer getCellRenderer() {
        return cellRenderer;
    }

    public void setCellEditor(TableCellEditor cellEditor) {
        Object old = this.cellEditor;
        this.cellEditor = cellEditor;
        fire("cellEditor", old, cellEditor);
    }

    public TableCellEditor getCellEditor() {
        return cellEditor;
    }

    private int clamp(int w) {
        return Math.min(Math.max(w, minWidth), maxWidth);
    }

    /// Sets the width the column has now; the table's layout overwrites
    /// it. Use [#setPreferredWidth(int)] to ask for a width.
    public void setWidth(int width) {
        int old = this.width;
        this.width = clamp(width);
        fire("width", old, this.width);
    }

    public int getWidth() {
        return width;
    }

    public void setPreferredWidth(int preferredWidth) {
        int old = this.preferredWidth;
        this.preferredWidth = clamp(preferredWidth);
        fire("preferredWidth", old, this.preferredWidth);
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setMinWidth(int minWidth) {
        int old = this.minWidth;
        this.minWidth = Math.max(Math.min(minWidth, maxWidth), 0);
        if (width < this.minWidth) {
            setWidth(this.minWidth);
        }
        if (preferredWidth < this.minWidth) {
            setPreferredWidth(this.minWidth);
        }
        fire("minWidth", old, this.minWidth);
    }

    public int getMinWidth() {
        return minWidth;
    }

    public void setMaxWidth(int maxWidth) {
        int old = this.maxWidth;
        this.maxWidth = Math.max(minWidth, maxWidth);
        if (width > this.maxWidth) {
            setWidth(this.maxWidth);
        }
        if (preferredWidth > this.maxWidth) {
            setPreferredWidth(this.maxWidth);
        }
        fire("maxWidth", old, this.maxWidth);
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setResizable(boolean isResizable) {
        boolean old = this.isResizable;
        this.isResizable = isResizable;
        fire("isResizable", Boolean.valueOf(old), Boolean.valueOf(isResizable));
    }

    public boolean getResizable() {
        return isResizable;
    }

    /// Fits the column to its header cell, if it has a header renderer of
    /// its own; does nothing otherwise.
    public void sizeWidthToFit() {
        if (headerRenderer == null) {
            return;
        }
        Component c = headerRenderer.getTableCellRendererComponent(null, getHeaderValue(), false, false, 0, 0);
        if (c == null) {
            return;
        }
        Dimension min = c.getMinimumSize();
        Dimension max = c.getMaximumSize();
        Dimension pref = c.getPreferredSize();
        setMinWidth(min.width);
        setMaxWidth(max.width);
        setPreferredWidth(pref.width);
        setWidth(getPreferredWidth());
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new SwingPropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return changeSupport.getPropertyChangeListeners();
    }

    /// A renderer that centers the header value. A column has no header
    /// renderer until one is set; the table header's default is used.
    protected TableCellRenderer createDefaultHeaderRenderer() {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.CENTER);
        return r;
    }
}
