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

/// Takes the taps on the cells of a grid while an editor wants them, the way a spreadsheet
/// puts a reference into the formula being typed when a cell is tapped. Set with
/// `DataGrid#setCellPicker(GridCellPicker)`.
public interface GridCellPicker {
    /// Whether a tap on a cell now goes to `#cellPicked(DataGrid, int, int, boolean)` and not
    /// to the grid. While it is false the grid does what it always does: it ends an edit in
    /// progress and selects the cell.
    ///
    /// #### Parameters
    ///
    /// - `grid`: the grid
    ///
    /// #### Returns
    ///
    /// true to take the tap
    boolean isPicking(DataGrid grid);

    /// A cell was tapped while `#isPicking(DataGrid)` was true. The grid keeps its selection
    /// and any edit in progress.
    ///
    /// #### Parameters
    ///
    /// - `grid`: the grid
    ///
    /// - `row`: the row, as the grid shows it
    ///
    /// - `column`: the column, an index among the visible columns
    ///
    /// - `extend`: true when Shift was held, which a spreadsheet takes as widening what was
    ///   picked last into a range
    void cellPicked(DataGrid grid, int row, int column, boolean extend);
}
