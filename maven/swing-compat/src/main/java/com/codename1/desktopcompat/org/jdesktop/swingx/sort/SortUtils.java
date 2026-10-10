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

import com.codename1.desktopcompat.javax.swing.RowSorter;
import com.codename1.desktopcompat.javax.swing.SortOrder;
import java.util.List;

/// Helpers for sort keys and sort orders.
public class SortUtils {

    SortUtils() {
    }

    /// The first key that sorts at all, or `null`.
    public static RowSorter.SortKey getFirstSortingKey(List<? extends RowSorter.SortKey> keys) {
        if (keys == null) {
            return null;
        }
        for (int i = 0; i < keys.size(); i++) {
            RowSorter.SortKey key = keys.get(i);
            if (isSorted(key.getSortOrder())) {
                return key;
            }
        }
        return null;
    }

    /// The first key of a model column, or `null`.
    public static RowSorter.SortKey getFirstSortKeyForColumn(List<? extends RowSorter.SortKey> keys,
            int modelColumn) {
        if (keys == null) {
            return null;
        }
        for (int i = 0; i < keys.size(); i++) {
            RowSorter.SortKey key = keys.get(i);
            if (key.getColumn() == modelColumn) {
                return key;
            }
        }
        return null;
    }

    /// Removes the first key of a model column from the list and answers
    /// it, or `null`.
    public static RowSorter.SortKey removeFirstSortKeyForColumn(List<? extends RowSorter.SortKey> keys,
            int modelColumn) {
        if (keys == null) {
            return null;
        }
        for (int i = 0; i < keys.size(); i++) {
            RowSorter.SortKey key = keys.get(i);
            if (key.getColumn() == modelColumn) {
                keys.remove(i);
                return key;
            }
        }
        return null;
    }

    public static boolean isSorted(SortOrder sortOrder) {
        return sortOrder != null && sortOrder != SortOrder.UNSORTED;
    }

    public static boolean isAscending(SortOrder sortOrder) {
        return sortOrder == SortOrder.ASCENDING;
    }

    /// Whether the order sorts in the given direction.
    public static boolean isSorted(SortOrder sortOrder, boolean ascending) {
        return isSorted(sortOrder) && ascending == isAscending(sortOrder);
    }
}
