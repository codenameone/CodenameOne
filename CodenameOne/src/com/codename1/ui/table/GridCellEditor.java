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

import com.codename1.ui.Component;

/// Edits the cells of a `DataGrid` column.
///
/// An editor lives only while a cell is edited. The grid asks for a
/// component, lays it over the cell, starts editing it and, when the edit
/// is done, reads the value back and throws the component away; from then on
/// the cell is painted again. Nothing stays in the component hierarchy.
///
/// An editor calls `DataGrid#commitEdit()` when the user is done, and
/// `DataGrid#cancelEdit()` to drop the edit. A boolean cell needs no editor:
/// a tap toggles it.
public interface GridCellEditor {

    /// A component that edits the cell, holding its current value.
    Component createEditor(DataGrid grid, GridCell cell);

    /// The value the editor holds now.
    ///
    /// #### Parameters
    ///
    /// - `editor`: what `createEditor` returned
    /// - `cell`: the cell being edited; `getValue()` is the value before
    Object getEditorValue(Component editor, GridCell cell);
}
