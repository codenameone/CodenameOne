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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.javax.swing.table.TableCellEditor;
import com.codename1.desktopcompat.javax.swing.table.TableCellRenderer;
import com.codename1.desktopcompat.javax.swing.table.TableColumn;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.ComponentAdapter;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.CompoundHighlighter;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.Highlighter;
import java.util.Comparator;
import java.util.Hashtable;

/// A table column that can be hidden, has switches for sorting and
/// editing, a comparator, a prototype value to size it by, client
/// properties and highlighters that apply to its cells only.
///
/// Every property tells the column's property change listeners. The
/// tool tip text is recorded and not shown.
public class TableColumnExt extends TableColumn {

    protected boolean visible = true;
    protected boolean hideable = true;
    protected Object prototypeValue;
    protected Comparator<?> comparator;
    protected boolean sortable = true;
    protected boolean editable = true;
    protected Hashtable<Object, Object> clientProperties;
    protected CompoundHighlighter compoundHighlighter;

    private String toolTipText;
    private ChangeListener highlighterChangeListener;

    public TableColumnExt() {
        this(0);
    }

    public TableColumnExt(int modelIndex) {
        this(modelIndex, 75);
    }

    public TableColumnExt(int modelIndex, int width) {
        this(modelIndex, width, null, null);
    }

    public TableColumnExt(int modelIndex, int width, TableCellRenderer cellRenderer, TableCellEditor cellEditor) {
        super(modelIndex, width, cellRenderer, cellEditor);
    }

    /// Makes a column with the properties of another.
    public TableColumnExt(TableColumnExt columnExt) {
        this(columnExt.getModelIndex(), columnExt.getWidth(), columnExt.getCellRenderer(),
                columnExt.getCellEditor());
        copyFrom(columnExt);
    }

    // ------------------------------------------------------------ highlighters

    public void setHighlighters(Highlighter... highlighters) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().setHighlighters(highlighters);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    public Highlighter[] getHighlighters() {
        return getCompoundHighlighter().getHighlighters();
    }

    public void addHighlighter(Highlighter highlighter) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().addHighlighter(highlighter);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    public void removeHighlighter(Highlighter highlighter) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().removeHighlighter(highlighter);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    protected CompoundHighlighter getCompoundHighlighter() {
        if (compoundHighlighter == null) {
            compoundHighlighter = new CompoundHighlighter();
            compoundHighlighter.addChangeListener(getHighlighterChangeListener());
        }
        return compoundHighlighter;
    }

    protected ChangeListener getHighlighterChangeListener() {
        if (highlighterChangeListener == null) {
            highlighterChangeListener = createHighlighterChangeListener();
        }
        return highlighterChangeListener;
    }

    /// A listener that reports a highlighter's change as the property
    /// `highlighterStateChanged`.
    protected ChangeListener createHighlighterChangeListener() {
        return new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                firePropertyChange("highlighterStateChanged", Boolean.FALSE, Boolean.TRUE);
            }
        };
    }

    /// Applies the column's highlighters to a cell's component.
    public Component cn1Highlight(Component stamp, ComponentAdapter adapter) {
        if (compoundHighlighter == null) {
            return stamp;
        }
        return compoundHighlighter.highlight(stamp, adapter);
    }

    // ------------------------------------------------------------ properties

    /// A column whose minimum and maximum widths are the same cannot be
    /// resized whatever the flag says.
    @Override
    public boolean getResizable() {
        return super.getResizable() && getMinWidth() < getMaxWidth();
    }

    public void setEditable(boolean editable) {
        boolean old = this.editable;
        this.editable = editable;
        firePropertyChange("editable", Boolean.valueOf(old), Boolean.valueOf(editable));
    }

    public boolean isEditable() {
        return editable;
    }

    /// Sets a value typical of the column's widest cell, used to size
    /// the column.
    public void setPrototypeValue(Object value) {
        Object old = prototypeValue;
        prototypeValue = value;
        firePropertyChange("prototypeValue", old, value);
    }

    public Object getPrototypeValue() {
        return prototypeValue;
    }

    public void setComparator(Comparator<?> comparator) {
        Comparator<?> old = this.comparator;
        this.comparator = comparator;
        firePropertyChange("comparator", old, comparator);
    }

    public Comparator<?> getComparator() {
        return comparator;
    }

    public void setSortable(boolean sortable) {
        boolean old = this.sortable;
        this.sortable = sortable;
        firePropertyChange("sortable", Boolean.valueOf(old), Boolean.valueOf(sortable));
    }

    public boolean isSortable() {
        return sortable;
    }

    public void setToolTipText(String toolTipText) {
        String old = this.toolTipText;
        this.toolTipText = toolTipText;
        firePropertyChange("toolTipText", old, toolTipText);
    }

    public String getToolTipText() {
        return toolTipText;
    }

    /// Sets the header value.
    public void setTitle(String title) {
        setHeaderValue(title);
    }

    /// The header value's string, or `null`.
    public String getTitle() {
        Object header = getHeaderValue();
        return header != null ? header.toString() : null;
    }

    /// Shows or hides the column. A column model that is a
    /// [TableColumnModelExt] takes a hidden column out of the columns
    /// its table sees and keeps it for later.
    public void setVisible(boolean visible) {
        boolean old = this.visible;
        this.visible = visible;
        firePropertyChange("visible", Boolean.valueOf(old), Boolean.valueOf(visible));
    }

    public boolean isVisible() {
        return visible;
    }

    /// Says whether the user may hide the column. The layer has no
    /// control that hides columns, so this is recorded only.
    public void setHideable(boolean hideable) {
        boolean old = this.hideable;
        this.hideable = hideable;
        firePropertyChange("hideable", Boolean.valueOf(old), Boolean.valueOf(hideable));
    }

    public boolean isHideable() {
        return hideable;
    }

    /// Sets a client property; `null` removes it.
    public void putClientProperty(Object key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("null key");
        }
        if (value == null && getClientProperty(key) == null) {
            return;
        }
        Object old = getClientProperty(key);
        if (value == null) {
            clientProperties.remove(key);
        } else {
            if (clientProperties == null) {
                clientProperties = new Hashtable<Object, Object>();
            }
            clientProperties.put(key, value);
        }
        firePropertyChange(key.toString(), old, value);
    }

    public Object getClientProperty(Object key) {
        return key == null || clientProperties == null ? null : clientProperties.get(key);
    }

    /// Takes over the properties of another column, client properties
    /// included; highlighters are not copied.
    protected void copyFrom(TableColumnExt original) {
        setEditable(original.isEditable());
        setHeaderValue(original.getHeaderValue());
        setToolTipText(original.getToolTipText());
        setIdentifier(original.getIdentifier());
        setMaxWidth(original.getMaxWidth());
        setMinWidth(original.getMinWidth());
        setPreferredWidth(original.getPreferredWidth());
        setPrototypeValue(original.getPrototypeValue());
        setResizable(original.isResizable);
        setVisible(original.isVisible());
        setSortable(original.isSortable());
        setComparator(original.getComparator());
        setHideable(original.isHideable());
        setHeaderRenderer(original.getHeaderRenderer());
        copyClientPropertiesFrom(original);
    }

    protected void copyClientPropertiesFrom(TableColumnExt original) {
        if (original.clientProperties == null) {
            return;
        }
        Object[] keys = original.clientProperties.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            putClientProperty(keys[i], original.getClientProperty(keys[i]));
        }
    }

    /// Tells the property change listeners, unless both values are the
    /// same non-null value.
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (oldValue != null && oldValue.equals(newValue)) {
            return;
        }
        if (oldValue == null && newValue == null) {
            return;
        }
        PropertyChangeListener[] listeners = getPropertyChangeListeners();
        if (listeners.length == 0) {
            return;
        }
        PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].propertyChange(event);
        }
    }
}
