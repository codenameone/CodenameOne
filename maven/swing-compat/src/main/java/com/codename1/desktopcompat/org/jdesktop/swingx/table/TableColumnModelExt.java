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

import com.codename1.desktopcompat.javax.swing.event.TableColumnModelListener;
import com.codename1.desktopcompat.javax.swing.table.TableColumn;
import com.codename1.desktopcompat.javax.swing.table.TableColumnModel;
import java.util.List;

/// A column model that keeps columns that are hidden. The methods of
/// `TableColumnModel` see the visible columns only.
public interface TableColumnModelExt extends TableColumnModel {

    /// The number of columns, hidden ones included when asked for.
    int getColumnCount(boolean includeHidden);

    /// The columns in their order, hidden ones included when asked for.
    List<TableColumn> getColumns(boolean includeHidden);

    /// The first column with the identifier, visible or hidden, when it
    /// is a [TableColumnExt]; else `null`.
    TableColumnExt getColumnExt(Object identifier);

    /// The visible column at the index when it is a [TableColumnExt];
    /// `null` for another kind of column or an index out of range.
    TableColumnExt getColumnExt(int columnIndex);

    /// Adds a listener; one that is a
    /// `TableColumnModelExtListener` also hears of column properties.
    @Override
    void addColumnModelListener(TableColumnModelListener x);
}
