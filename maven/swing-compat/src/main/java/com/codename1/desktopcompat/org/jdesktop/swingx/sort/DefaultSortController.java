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

import com.codename1.desktopcompat.javax.swing.DefaultRowSorter;
import com.codename1.desktopcompat.javax.swing.RowSorter;
import com.codename1.desktopcompat.javax.swing.SortOrder;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.StringValue;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.StringValues;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A row sorter that is a [SortController].
///
/// A column without a comparator of its own is compared by
/// [#COMPARABLE_COMPARATOR].
public abstract class DefaultSortController<M> extends DefaultRowSorter<M, Integer> implements SortController<M> {

    /// Compares two values of the same comparable class by their natural
    /// order, two numbers by their value, and anything else by their
    /// strings. It never fails on values of different classes.
    @SuppressWarnings("rawtypes")
    public static final Comparator COMPARABLE_COMPARATOR = new ComparableComparator();

    private static final SortOrder[] DEFAULT_CYCLE = {SortOrder.ASCENDING, SortOrder.DESCENDING};

    private static final StringValueProvider TO_STRING_PROVIDER = new StringValueProvider() {
        @Override
        public StringValue getStringValue(int row, int column) {
            return StringValues.TO_STRING;
        }
    };

    private final Map<Integer, Comparator<?>> cn1Comparators = new HashMap<Integer, Comparator<?>>();
    private boolean sortable = true;
    private SortOrder[] sortCycle = getDefaultSortOrderCycle();
    private StringValueProvider stringValueProvider = TO_STRING_PROVIDER;

    public DefaultSortController() {
        super();
    }

    @Override
    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    @Override
    public boolean isSortable() {
        return sortable;
    }

    @Override
    public void setSortable(int column, boolean sortable) {
        super.setSortable(column, sortable);
    }

    @Override
    public boolean isSortable(int column) {
        return sortable && super.isSortable(column);
    }

    @Override
    public void setComparator(int column, Comparator<?> comparator) {
        super.setComparator(column, comparator);
        if (comparator == null) {
            cn1Comparators.remove(Integer.valueOf(column));
        } else {
            cn1Comparators.put(Integer.valueOf(column), comparator);
        }
    }

    /// The comparator set for a column, or `null`.
    Comparator<?> cn1Comparator(int column) {
        return cn1Comparators.get(Integer.valueOf(column));
    }

    @Override
    public Comparator<?> getComparator(int column) {
        Comparator<?> set = cn1Comparator(column);
        return set != null ? set : COMPARABLE_COMPARATOR;
    }

    @Override
    protected boolean useToString(int column) {
        return false;
    }

    private List<RowSorter.SortKey> cn1KeysWithout(int column) {
        List<RowSorter.SortKey> keys = new ArrayList<RowSorter.SortKey>();
        List<? extends RowSorter.SortKey> now = getSortKeys();
        for (int i = 0; i < now.size(); i++) {
            if (now.get(i).getColumn() != column) {
                keys.add(now.get(i));
            }
        }
        return keys;
    }

    private void cn1SetFirst(int column, SortOrder order) {
        List<RowSorter.SortKey> keys = cn1KeysWithout(column);
        if (SortUtils.isSorted(order)) {
            keys.add(0, new RowSorter.SortKey(column, order));
        }
        int max = Math.max(1, getMaxSortKeys());
        while (keys.size() > max) {
            keys.remove(keys.size() - 1);
        }
        setSortKeys(keys);
    }

    /// Moves a sortable column to the next order of the cycle and makes
    /// it the first column sorted by. A column that is not the first one
    /// sorted by starts the cycle over.
    @Override
    public void toggleSortOrder(int column) {
        if (!isSortable(column) || sortCycle.length == 0) {
            return;
        }
        SortOrder next = sortCycle[0];
        List<? extends RowSorter.SortKey> keys = getSortKeys();
        if (!keys.isEmpty() && keys.get(0).getColumn() == column) {
            SortOrder current = keys.get(0).getSortOrder();
            for (int i = 0; i < sortCycle.length; i++) {
                if (sortCycle[i] == current) {
                    next = sortCycle[(i + 1) % sortCycle.length];
                    break;
                }
            }
        }
        cn1SetFirst(column, next);
    }

    @Override
    public void setSortOrder(int column, SortOrder sortOrder) {
        if (!isSortable(column)) {
            return;
        }
        cn1SetFirst(column, sortOrder);
    }

    @Override
    public SortOrder getSortOrder(int column) {
        RowSorter.SortKey key = SortUtils.getFirstSortKeyForColumn(getSortKeys(), column);
        return key != null ? key.getSortOrder() : SortOrder.UNSORTED;
    }

    @Override
    public void resetSortOrders() {
        if (!isSortable()) {
            return;
        }
        List<RowSorter.SortKey> keys = new ArrayList<RowSorter.SortKey>();
        List<? extends RowSorter.SortKey> now = getSortKeys();
        for (int i = 0; i < now.size(); i++) {
            if (!isSortable(now.get(i).getColumn())) {
                keys.add(now.get(i));
            }
        }
        setSortKeys(keys);
    }

    @Override
    public SortOrder[] getSortOrderCycle() {
        SortOrder[] copy = new SortOrder[sortCycle.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = sortCycle[i];
        }
        return copy;
    }

    @Override
    public void setSortOrderCycle(SortOrder... cycle) {
        SortOrder[] copy = new SortOrder[cycle == null ? 0 : cycle.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = cycle[i];
        }
        sortCycle = copy;
    }

    /// Sets where the strings of the cells come from; `null` goes back
    /// to the values' `toString`.
    @Override
    public void setStringValueProvider(StringValueProvider registry) {
        stringValueProvider = registry != null ? registry : TO_STRING_PROVIDER;
    }

    @Override
    public StringValueProvider getStringValueProvider() {
        return stringValueProvider;
    }

    /// Ascending, then descending.
    public static SortOrder[] getDefaultSortOrderCycle() {
        return new SortOrder[]{DEFAULT_CYCLE[0], DEFAULT_CYCLE[1]};
    }

    /// The comparators and sortable flags of the columns are forgotten
    /// with the columns.
    @Override
    public void modelStructureChanged() {
        if (cn1Comparators != null) {
            cn1Comparators.clear();
        }
        super.modelStructureChanged();
    }

    @SuppressWarnings("rawtypes")
    private static final class ComparableComparator implements Comparator {

        @Override
        @SuppressWarnings("unchecked")
        public int compare(Object a, Object b) {
            if (a == b) {
                return 0;
            }
            if (a == null) {
                return -1;
            }
            if (b == null) {
                return 1;
            }
            if (a instanceof Comparable && a.getClass() == b.getClass()) {
                return ((Comparable) a).compareTo(b);
            }
            if (a instanceof Number && b instanceof Number) {
                double x = ((Number) a).doubleValue();
                double y = ((Number) b).doubleValue();
                return x < y ? -1 : x > y ? 1 : 0;
            }
            return a.toString().compareTo(b.toString());
        }
    }
}
