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

import com.codename1.desktopcompat.javax.swing.DefaultRowSorter;
import com.codename1.desktopcompat.rt.Comparators;
import java.util.Comparator;

/// Sorts and filters the rows of a table model.
///
/// A column is compared by the comparator set for it; else, when the
/// model declares its class to be `String` or a `Comparable`, by the
/// values themselves; else by the strings the values (or the string
/// converter) give. Values are compared safely whatever the model
/// declares: see [DefaultRowSorter].
public class TableRowSorter<M extends TableModel> extends DefaultRowSorter<M, Integer> {

    private M tableModel;
    private TableStringConverter stringConverter;

    public TableRowSorter() {
        this(null);
    }

    public TableRowSorter(M model) {
        setModel(model);
    }

    public void setModel(M model) {
        tableModel = model;
        setModelWrapper(new Wrapper());
    }

    public void setStringConverter(TableStringConverter stringConverter) {
        this.stringConverter = stringConverter;
    }

    public TableStringConverter getStringConverter() {
        return stringConverter;
    }

    private Class<?> columnClass(int column) {
        Class<?> c = tableModel == null ? null : tableModel.getColumnClass(column);
        return c == null ? Object.class : c;
    }

    /// The comparator set for the column, or `null`.
    private Comparator<?> explicit(int column) {
        Comparator<?> c = super.getComparator(column);
        return c == Comparators.STRINGS ? null : c;
    }

    @Override
    public Comparator<?> getComparator(int column) {
        Comparator<?> set = explicit(column);
        if (set != null) {
            return set;
        }
        Class<?> c = columnClass(column);
        if (c != String.class && Comparable.class.isAssignableFrom(c)) {
            return Comparators.VALUES;
        }
        return Comparators.STRINGS;
    }

    @Override
    protected boolean useToString(int column) {
        if (explicit(column) != null) {
            return false;
        }
        Class<?> c = columnClass(column);
        return c != String.class && !Comparable.class.isAssignableFrom(c);
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
            if (stringConverter != null) {
                String s = stringConverter.toString(tableModel, row, column);
                return s == null ? "" : s;
            }
            Object o = getValueAt(row, column);
            if (o == null) {
                return "";
            }
            return o.toString();
        }

        @Override
        public Integer getIdentifier(int row) {
            return Integer.valueOf(row);
        }
    }
}
