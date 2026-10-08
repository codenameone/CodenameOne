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

import com.codename1.desktopcompat.javax.swing.RowFilter;
import com.codename1.desktopcompat.javax.swing.SortOrder;
import java.util.Comparator;

/// What a SwingX component asks of its row sorter beyond `RowSorter`.
/// Columns are model columns.
public interface SortController<M> {

    /// Switches sorting by the user on or off as a whole; a column is
    /// sortable when this is on and the column itself is.
    void setSortable(boolean sortable);

    boolean isSortable();

    void setSortable(int column, boolean sortable);

    boolean isSortable(int column);

    void setComparator(int column, Comparator<?> comparator);

    Comparator<?> getComparator(int column);

    /// Sets the orders [#toggleSortOrder(int)] steps a column through.
    void setSortOrderCycle(SortOrder... cycle);

    SortOrder[] getSortOrderCycle();

    void setSortsOnUpdates(boolean sortsOnUpdates);

    boolean getSortsOnUpdates();

    void setStringValueProvider(StringValueProvider registry);

    StringValueProvider getStringValueProvider();

    void toggleSortOrder(int column);

    void setSortOrder(int column, SortOrder sortOrder);

    SortOrder getSortOrder(int column);

    void resetSortOrders();

    void setRowFilter(RowFilter<? super M, ? super Integer> filter);

    RowFilter<? super M, ? super Integer> getRowFilter();
}
