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

import com.codename1.desktopcompat.javax.swing.ListSelectionModel;
import com.codename1.desktopcompat.javax.swing.event.TableColumnModelListener;
import java.util.Enumeration;

/// The columns of a table in the order they are shown, the gap between
/// them, and which of them are selected.
public interface TableColumnModel {

    void addColumn(TableColumn aColumn);

    void removeColumn(TableColumn column);

    void moveColumn(int columnIndex, int newIndex);

    void setColumnMargin(int newMargin);

    int getColumnCount();

    Enumeration<TableColumn> getColumns();

    int getColumnIndex(Object columnIdentifier);

    TableColumn getColumn(int columnIndex);

    int getColumnMargin();

    int getColumnIndexAtX(int xPosition);

    int getTotalColumnWidth();

    void setColumnSelectionAllowed(boolean flag);

    boolean getColumnSelectionAllowed();

    int[] getSelectedColumns();

    int getSelectedColumnCount();

    void setSelectionModel(ListSelectionModel newModel);

    ListSelectionModel getSelectionModel();

    void addColumnModelListener(TableColumnModelListener x);

    void removeColumnModelListener(TableColumnModelListener x);
}
