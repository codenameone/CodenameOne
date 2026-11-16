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

import com.codename1.ui.Graphics;

/// Paints the cells of a `DataGrid` column.
///
/// A renderer draws straight into the grid's graphics. There is no
/// component per cell, which is what lets a grid show a million rows: only
/// the cells on screen are painted, and each costs what its drawing costs.
/// The row's background is already painted when `paint` is called.
///
/// ```java
/// DataGrid grid = new DataGrid(new DefaultTableModel(new String[]{"Item", "Balance"},
///         new Object[][]{{"rent", -1200}, {"salary", 4000}}, false));
/// grid.getColumn(1).setRenderer(new DefaultGridCellRenderer() {
///     protected int textColor(GridCell cell, int defaultColor) {
///         Object v = cell.getValue();
///         return v instanceof Number && ((Number) v).doubleValue() < 0 ? 0xc62828 : defaultColor;
///     }
/// });
/// ```
public interface GridCellRenderer {

    /// Paints a cell into the rectangle `x`, `y`, `width`, `height`. The
    /// graphics is clipped to it.
    void paint(Graphics g, GridCell cell, int x, int y, int width, int height);

    /// The width, in pixels, the cell's content needs, for a column that
    /// sizes itself to what it shows.
    int getPreferredWidth(GridCell cell);
}
