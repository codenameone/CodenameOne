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
import com.codename1.desktopcompat.javax.swing.JTable;
import com.codename1.desktopcompat.javax.swing.table.JTableHeader;
import com.codename1.desktopcompat.javax.swing.table.TableCellRenderer;
import com.codename1.desktopcompat.javax.swing.table.TableColumn;
import com.codename1.desktopcompat.javax.swing.table.TableModel;
import com.codename1.desktopcompat.org.jdesktop.swingx.JXTable;
import java.util.List;

/// Makes the columns of a `JXTable`, gives them their header from the
/// model, sizes them from a prototype value and packs them to their
/// content.
///
/// A table uses the shared instance unless it is given its own; replace
/// the shared instance to change how every table makes its columns.
public class ColumnFactory {

    private static ColumnFactory columnFactory;

    private int packMargin = 4;

    public ColumnFactory() {
    }

    public static ColumnFactory getInstance() {
        if (columnFactory == null) {
            columnFactory = new ColumnFactory();
        }
        return columnFactory;
    }

    public static void setInstance(ColumnFactory factory) {
        columnFactory = factory;
    }

    /// Makes the column of a model column; `null` when
    /// [#createTableColumn(int)] makes none.
    public TableColumnExt createAndConfigureTableColumn(TableModel model, int modelIndex) {
        TableColumnExt column = createTableColumn(modelIndex);
        if (column != null) {
            configureTableColumn(model, column);
        }
        return column;
    }

    public TableColumnExt createTableColumn(int modelIndex) {
        return new TableColumnExt(modelIndex);
    }

    /// Gives the column the model's name of it as its header.
    public void configureTableColumn(TableModel model, TableColumnExt columnExt) {
        int index = columnExt.getModelIndex();
        if (index < 0 || index >= model.getColumnCount()) {
            throw new IllegalStateException("column must have valid modelIndex");
        }
        columnExt.setHeaderValue(model.getColumnName(index));
    }

    /// Gives a column with a prototype value the preferred width that
    /// value, or the header if it is wider, needs. A column without one
    /// is left alone.
    public void configureColumnWidths(JXTable table, TableColumnExt columnExt) {
        int prototype = calcPrototypeWidth(table, columnExt);
        if (prototype < 0) {
            return;
        }
        int width = Math.max(prototype, calcHeaderWidth(table, columnExt));
        columnExt.setPreferredWidth(width + 2 * getDefaultPackMargin());
    }

    /// The preferred widths of the columns the table wants to show
    /// added up: its visible column count, or all of them when that is
    /// negative.
    public int getPreferredScrollableViewportWidth(JXTable table) {
        int count = table.getColumnCount();
        int wanted = table.getVisibleColumnCount();
        if (wanted >= 0 && wanted < count) {
            count = wanted;
        }
        int w = 0;
        for (int i = 0; i < count; i++) {
            w += table.getColumn(i).getPreferredWidth();
        }
        if (wanted > table.getColumnCount()) {
            w += (wanted - table.getColumnCount()) * 75;
        }
        return w;
    }

    /// The width the column's header wants, 0 without a header renderer.
    protected int calcHeaderWidth(JXTable table, TableColumnExt columnExt) {
        TableCellRenderer renderer = getHeaderRenderer(table, columnExt);
        if (renderer == null) {
            return 0;
        }
        Component c = renderer.getTableCellRendererComponent(table, columnExt.getHeaderValue(), false, false, -1,
                -1);
        return c != null ? c.getPreferredSize().width : 0;
    }

    /// The width the column's prototype value wants, -1 without one.
    protected int calcPrototypeWidth(JXTable table, TableColumnExt columnExt) {
        Object prototype = columnExt.getPrototypeValue();
        if (prototype == null) {
            return -1;
        }
        TableCellRenderer renderer = getCellRenderer(table, columnExt);
        if (renderer == null) {
            return -1;
        }
        Component c = renderer.getTableCellRendererComponent(table, prototype, false, false, -1, -1);
        return c != null ? c.getPreferredSize().width : -1;
    }

    /// The renderer of the column's cells: the table's choice for a
    /// visible column, else the column's own or the default one of the
    /// model column's class.
    protected TableCellRenderer getCellRenderer(JXTable table, TableColumnExt columnExt) {
        int view = table.convertColumnIndexToView(columnExt.getModelIndex());
        if (view >= 0) {
            return table.getCellRenderer(0, view);
        }
        TableCellRenderer renderer = columnExt.getCellRenderer();
        if (renderer == null) {
            int model = columnExt.getModelIndex();
            Class<?> c = model >= 0 && model < table.getModel().getColumnCount()
                    ? table.getModel().getColumnClass(model) : Object.class;
            renderer = table.getDefaultRenderer(c);
        }
        return renderer;
    }

    protected TableCellRenderer getHeaderRenderer(JXTable table, TableColumnExt columnExt) {
        TableCellRenderer renderer = columnExt.getHeaderRenderer();
        if (renderer == null) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                renderer = header.getDefaultRenderer();
            }
        }
        return renderer;
    }

    /// Sets the preferred width of a visible column to what its header
    /// and all of its cells need, plus `margin` on both sides, and at
    /// most `max`. A negative margin is the default one; a `max` of -1
    /// is no limit.
    public void packColumn(JXTable table, TableColumnExt columnExt, int margin, int max) {
        if (!columnExt.isVisible()) {
            throw new IllegalStateException("column must be visible to pack");
        }
        int column = cn1ViewIndex(table, columnExt);
        if (column < 0) {
            return;
        }
        int width = calcHeaderWidth(table, columnExt);
        TableCellRenderer renderer = getCellRenderer(table, columnExt);
        if (renderer != null) {
            int rows = getRowCount(table);
            for (int r = 0; r < rows; r++) {
                Component c = renderer.getTableCellRendererComponent(table, table.getValueAt(r, column), false,
                        false, r, column);
                if (c != null) {
                    width = Math.max(width, c.getPreferredSize().width);
                }
            }
        }
        if (margin < 0) {
            margin = getDefaultPackMargin();
        }
        width += 2 * margin;
        if (max != -1 && width > max) {
            width = max;
        }
        columnExt.setPreferredWidth(width);
    }

    private static int cn1ViewIndex(JTable table, TableColumn column) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (table.getColumnModel().getColumn(i) == column) {
                return i;
            }
        }
        return -1;
    }

    /// The number of rows measured when packing.
    protected int getRowCount(JXTable table) {
        return table.getRowCount();
    }

    public int getDefaultPackMargin() {
        return packMargin;
    }

    public void setDefaultPackMargin(int margin) {
        this.packMargin = margin;
    }

    static List<TableColumn> cn1Columns(JXTable table) {
        return table.getColumns(true);
    }
}
