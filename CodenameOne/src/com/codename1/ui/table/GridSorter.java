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
package com.codename1.ui.table;

import java.util.Comparator;

/// The sort a `DataGrid` orders its rows with, for any other list of rows.
///
/// It orders positions rather than data: each sort key is read once per row
/// into an array before sorting, so a comparison never goes back to the
/// model, and the sort is stable, so rows that compare equal keep the order
/// they came in. A million rows cost a million reads per key.
///
/// ```java
/// String[] names = {"dana", "Ann", "carl"};
/// int n = names.length;
/// Object[][] keys = new Object[1][n];
/// for (int i = 0; i < n; i++) {
///     keys[0][i] = names[i];
/// }
/// int[] order = GridSorter.order(n, keys, null, new boolean[]{true});
/// // order[k] is the position that sorts k-th
/// ```
public final class GridSorter {
    private GridSorter() {
    }

    /// Orders the positions `0..count-1` by their keys.
    ///
    /// #### Parameters
    ///
    /// - `count`: how many positions there are
    /// - `keys`: `keys[k][i]`, the value of sort key `k` at position `i`;
    ///   the first key decides, the next one breaks its ties, and so on
    /// - `comparators`: a comparator per key, or null; a null array or a
    ///   null entry compares values naturally: numbers by value, dates by
    ///   time, booleans false first, anything else by its text ignoring case
    /// - `ascending`: the direction of each key
    ///
    /// A null value sorts before any other, and after them all when its key
    /// descends. A comparator is never given a null.
    ///
    /// #### Returns
    ///
    /// the positions in sorted order
    public static int[] order(int count, Object[][] keys, Comparator[] comparators, boolean[] ascending) {
        int[] pos = new int[count];
        for (int i = 0; i < count; i++) {
            pos[i] = i;
        }
        if (count > 1 && keys.length > 0) {
            GridRows.sort(pos, 0, count, new KeyOrder(keys, comparators, ascending));
        }
        return pos;
    }

    /// Compares two values the way `order` does for an ascending key.
    public static int compare(Comparator comparator, Object a, Object b) {
        if (a == null) {
            return b == null ? 0 : -1;
        }
        if (b == null) {
            return 1;
        }
        return comparator == null ? GridRows.naturalCompare(a, b) : comparator.compare(a, b);
    }

    private static final class KeyOrder implements GridRows.Order {
        private final Object[][] keys;
        private final Comparator[] comparators;
        private final boolean[] ascending;

        KeyOrder(Object[][] keys, Comparator[] comparators, boolean[] ascending) {
            this.keys = keys;
            this.comparators = comparators;
            this.ascending = ascending;
        }

        @Override
        public int compare(int a, int b) {
            for (int k = 0; k < keys.length; k++) {
                Comparator c = comparators == null ? null : comparators[k];
                Object[] col = keys[k];
                int r = ascending[k] ? GridSorter.compare(c, col[a], col[b]) : GridSorter.compare(c, col[b], col[a]);
                if (r != 0) {
                    return r;
                }
            }
            return 0;
        }
    }
}
