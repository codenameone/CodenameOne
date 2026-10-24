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
package com.codename1.desktopcompat.org.jdesktop.swingx.sort;

import com.codename1.desktopcompat.javax.swing.table.TableModel;
import java.util.Comparator;

/// The sort controller of a table.
///
/// A column is compared by the comparator set for it; else, when the
/// model declares its class comparable, by the values; else by the
/// strings the table shows for them.
public class TableSortController<M extends TableModel> extends DefaultSortController<M> {

    private M tableModel;

    public TableSortController() {
        this(null);
    }

    public TableSortController(M model) {
        super();
        setModel(model);
    }

    public void setModel(M model) {
        tableModel = model;
        setModelWrapper(new Wrapper());
    }

    private boolean cn1Comparable(int column) {
        Class<?> c = tableModel == null ? null : tableModel.getColumnClass(column);
        return c != null && Comparable.class.isAssignableFrom(c);
    }

    @Override
    public Comparator<?> getComparator(int column) {
        Comparator<?> set = cn1Comparator(column);
        if (set != null) {
            return set;
        }
        return COMPARABLE_COMPARATOR;
    }

    @Override
    protected boolean useToString(int column) {
        return cn1Comparator(column) == null && !cn1Comparable(column);
    }

    private final class Wrapper extends ModelWrapper<M, Integer> {

        @Override
        public M getModel() {
            return tableModel;
        }

        @Override
        public int getColumnCount() {
            return tableModel == null ? 0 : tableModel.getColumnCount();
        }

        @Override
        public int getRowCount() {
            return tableModel == null ? 0 : tableModel.getRowCount();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return tableModel.getValueAt(row, column);
        }

        @Override
        public String getStringValueAt(int row, int column) {
            String s = getStringValueProvider().getStringValue(row, column).getString(getValueAt(row, column));
            return s == null ? "" : s;
        }

        @Override
        public Integer getIdentifier(int row) {
            return Integer.valueOf(row);
        }
    }
}
