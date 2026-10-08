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
package javafx.scene.control;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/// The row selection the list view and the table view share: a sorted
/// list of selected indices over the items of the view, kept right while
/// the items change. The model that owns it publishes the result through
/// its own `selectedIndex` and `selectedItem`.
final class RowSelection<T> {

    /// The model a selection reports to.
    interface Owner<T> {
        /// The items of the view; may be `null`.
        ObservableList<T> rows();

        /// Whether several rows may be selected.
        boolean multiple();

        /// Publishes the row selected last, -1 and `null` for none.
        void lead(int index, T item);
    }

    private final Owner<T> owner;
    private final ObservableList<Integer> indices = FXCollections.observableArrayList();
    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final ObservableList<Integer> indicesView = FXCollections.unmodifiableObservableList(indices);
    private final ObservableList<T> itemsView = FXCollections.unmodifiableObservableList(items);
    private int lead = -1;

    RowSelection(Owner<T> owner) {
        this.owner = owner;
    }

    ObservableList<Integer> indices() {
        return indicesView;
    }

    ObservableList<T> items() {
        return itemsView;
    }

    private int count() {
        ObservableList<T> rows = owner.rows();
        return rows == null ? 0 : rows.size();
    }

    private T row(int index) {
        ObservableList<T> rows = owner.rows();
        return rows == null || index < 0 || index >= rows.size() ? null : rows.get(index);
    }

    /// Replaces the selection with a set of indices; the last valid one
    /// given becomes the lead unless `newLead` names another.
    private void commit(List<Integer> wanted, int newLead) {
        ArrayList<Integer> sorted = new ArrayList<Integer>();
        int n = count();
        for (int i = 0; i < wanted.size(); i++) {
            int v = wanted.get(i).intValue();
            if (v < 0 || v >= n) {
                continue;
            }
            int at = 0;
            boolean present = false;
            while (at < sorted.size()) {
                int other = sorted.get(at).intValue();
                if (other == v) {
                    present = true;
                    break;
                }
                if (other > v) {
                    break;
                }
                at++;
            }
            if (!present) {
                sorted.add(at, Integer.valueOf(v));
            }
        }
        int l = newLead;
        if (!sorted.contains(Integer.valueOf(l))) {
            l = sorted.isEmpty() ? -1 : sorted.get(sorted.size() - 1).intValue();
        }
        if (!owner.multiple() && sorted.size() > 1) {
            sorted.clear();
            sorted.add(Integer.valueOf(l));
        }
        ArrayList<T> values = new ArrayList<T>();
        for (int i = 0; i < sorted.size(); i++) {
            values.add(row(sorted.get(i).intValue()));
        }
        lead = l;
        if (!sorted.equals(indices)) {
            indices.setAll(sorted);
        }
        if (!sameItems(values)) {
            items.setAll(values);
        }
        owner.lead(l, row(l));
    }

    private boolean sameItems(List<T> values) {
        if (values.size() != items.size()) {
            return false;
        }
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) != items.get(i)) {
                return false;
            }
        }
        return true;
    }

    void clearAndSelect(int index) {
        ArrayList<Integer> one = new ArrayList<Integer>();
        one.add(Integer.valueOf(index));
        commit(one, index);
    }

    void select(int index) {
        if (index == -1) {
            clear();
            return;
        }
        if (index < 0 || index >= count()) {
            return;
        }
        ArrayList<Integer> next = new ArrayList<Integer>();
        if (owner.multiple()) {
            next.addAll(indices);
        }
        next.add(Integer.valueOf(index));
        commit(next, index);
    }

    void select(T item) {
        ObservableList<T> rows = owner.rows();
        if (item == null || rows == null) {
            clear();
            return;
        }
        int at = rows.indexOf(item);
        if (at >= 0) {
            select(at);
        }
    }

    void selectIndices(int index, int[] more) {
        ArrayList<Integer> next = new ArrayList<Integer>();
        next.addAll(indices);
        int n = count();
        int last = index >= 0 && index < n ? index : -1;
        next.add(Integer.valueOf(index));
        if (more != null) {
            for (int i = 0; i < more.length; i++) {
                next.add(Integer.valueOf(more[i]));
                if (more[i] >= 0 && more[i] < n) {
                    last = more[i];
                }
            }
        }
        if (last < 0) {
            return;
        }
        if (!owner.multiple()) {
            clearAndSelect(last);
            return;
        }
        commit(next, last);
    }

    void selectAll() {
        if (!owner.multiple()) {
            return;
        }
        ArrayList<Integer> all = new ArrayList<Integer>();
        int n = count();
        for (int i = 0; i < n; i++) {
            all.add(Integer.valueOf(i));
        }
        commit(all, n - 1);
    }

    void clear(int index) {
        if (!indices.contains(Integer.valueOf(index))) {
            return;
        }
        ArrayList<Integer> next = new ArrayList<Integer>(indices);
        next.remove(Integer.valueOf(index));
        commit(next, lead == index ? -1 : lead);
    }

    void clear() {
        commit(new ArrayList<Integer>(), -1);
    }

    boolean isSelected(int index) {
        return indices.contains(Integer.valueOf(index));
    }

    boolean isEmpty() {
        return indices.isEmpty();
    }

    int lead() {
        return lead;
    }

    int rowCount() {
        return count();
    }

    /// Keeps the selection on the same items while the items change.
    void rowsChanged(ListChangeListener.Change<? extends T> change) {
        ArrayList<Integer> next = new ArrayList<Integer>(indices);
        int l = lead;
        while (change.next()) {
            if (change.wasPermutated()) {
                for (int i = 0; i < next.size(); i++) {
                    int v = next.get(i).intValue();
                    if (v >= change.getFrom() && v < change.getTo()) {
                        next.set(i, Integer.valueOf(change.getPermutation(v)));
                    }
                }
                if (l >= change.getFrom() && l < change.getTo()) {
                    l = change.getPermutation(l);
                }
                continue;
            }
            int from = change.getFrom();
            int removed = change.getRemovedSize();
            int added = change.getAddedSize();
            ArrayList<Integer> shifted = new ArrayList<Integer>();
            for (int i = 0; i < next.size(); i++) {
                int v = next.get(i).intValue();
                if (v < from) {
                    shifted.add(Integer.valueOf(v));
                } else if (v >= from + removed) {
                    shifted.add(Integer.valueOf(v - removed + added));
                }
            }
            next = shifted;
            if (l >= from + removed) {
                l = l - removed + added;
            } else if (l >= from) {
                l = -1;
            }
        }
        // The items behind unchanged indices may be new objects.
        items.clear();
        commit(next, l);
    }

    /// The whole list of items was replaced: nothing stays selected.
    void rowsReplaced() {
        indices.clear();
        items.clear();
        lead = -1;
        owner.lead(-1, null);
    }
}
