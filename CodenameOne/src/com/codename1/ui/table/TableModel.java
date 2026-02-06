/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.table;

import com.codename1.ui.events.DataChangedListener;

/// The table and table model class are complimentry classes allowing the quick construction
/// of tabular data controls. The table model represents the data source according to which
/// the table is constructed.
///
/// @author Shai Almog
public interface TableModel {
    /// Returns the number of rows in the table
    ///
    /// #### Returns
    ///
    /// the number of rows in the table
    int getRowCount();

    /// Returns the number of columns in the table
    ///
    /// #### Returns
    ///
    /// the number of columns in the table
    int getColumnCount();

    /// Returns the name of the column at the given offset
    ///
    /// #### Parameters
    ///
    /// - `i`: the offset for the column name
    ///
    /// #### Returns
    ///
    /// name to display at the top of the table
    String getColumnName(int i);

    /// Returns true if the cell at the given location is an editable cell
    ///
    /// #### Parameters
    ///
    /// - `row`: the cell row
    ///
    /// - `column`: the cell column
    ///
    /// #### Returns
    ///
    /// true if the cell at the given location is an editable cell
    boolean isCellEditable(int row, int column);

    /// Returns the value of the cell at the given location
    ///
    /// #### Parameters
    ///
    /// - `row`: the cell row
    ///
    /// - `column`: the cell column
    ///
    /// #### Returns
    ///
    /// the value of the cell at the given location
    Object getValueAt(int row, int column);

    /// Sets the value of the cell at the given location
    ///
    /// #### Parameters
    ///
    /// - `row`: the cell row
    ///
    /// - `column`: the cell column
    ///
    /// - `o`: the value of the cell at the given location
    void setValueAt(int row, int column, Object o);

    /// Adds a listener to the data changed event
    ///
    /// #### Parameters
    ///
    /// - `d`: the new listener
    void addDataChangeListener(DataChangedListener d);

    /// Removes a listener to the data changed event
    ///
    /// #### Parameters
    ///
    /// - `d`: the listener to remove
    void removeDataChangeListener(DataChangedListener d);

}
