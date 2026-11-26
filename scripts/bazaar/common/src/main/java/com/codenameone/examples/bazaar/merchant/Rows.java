/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.merchant;

import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.table.TableModel;
import com.codename1.ui.util.EventDispatcher;
import java.util.ArrayList;
import java.util.List;

/// A `TableModel` over a list of records, for a `DataGrid`: each column reads
/// a value out of a record and, when it is editable, hands an edited value to
/// a setter that saves it. The grid sorts and filters by moving row numbers,
/// so the list itself is never reordered.
final class Rows<T> implements TableModel {
    interface Getter<T> {
        Object get(T row);
    }

    interface Setter<T> {
        void set(T row, Object value);
    }

    private static final class Column<T> {
        final String name;
        final Getter<T> getter;
        final Setter<T> setter;

        Column(String name, Getter<T> getter, Setter<T> setter) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
        }
    }

    private final List<Column<T>> columns = new ArrayList<Column<T>>();
    private final List<T> rows = new ArrayList<T>();
    private final EventDispatcher listeners = new EventDispatcher();

    Rows<T> col(String name, Getter<T> getter) {
        return col(name, getter, null);
    }

    Rows<T> col(String name, Getter<T> getter, Setter<T> setter) {
        columns.add(new Column<T>(name, getter, setter));
        return this;
    }

    void set(List<T> data) {
        rows.clear();
        if (data != null) {
            rows.addAll(data);
        }
        listeners.fireDataChangeEvent(-1, DataChangedListener.CHANGED);
    }

    /// Replaces one record, after the server has answered an edit.
    void replace(int row, T value) {
        if (row >= 0 && row < rows.size()) {
            rows.set(row, value);
            listeners.fireDataChangeEvent(row, DataChangedListener.CHANGED);
        }
    }

    T at(int row) {
        return row >= 0 && row < rows.size() ? rows.get(row) : null;
    }

    int indexOf(T value) {
        return rows.indexOf(value);
    }

    List<T> all() {
        return new ArrayList<T>(rows);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int i) {
        return columns.get(i).name;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return columns.get(column).setter != null;
    }

    @Override
    public Object getValueAt(int row, int column) {
        T r = at(row);
        return r == null ? null : columns.get(column).getter.get(r);
    }

    @Override
    public void setValueAt(int row, int column, Object o) {
        T r = at(row);
        Setter<T> setter = columns.get(column).setter;
        if (r != null && setter != null) {
            setter.set(r, o);
        }
    }

    @Override
    public void addDataChangeListener(DataChangedListener d) {
        listeners.addListener(d);
    }

    @Override
    public void removeDataChangeListener(DataChangedListener d) {
        listeners.removeListener(d);
    }
}
