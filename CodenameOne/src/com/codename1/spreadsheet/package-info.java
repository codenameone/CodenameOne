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

/// Spreadsheet files and formulas.
///
/// A `Workbook` is read from, and written to, an Excel `.xlsx` file, an OpenDocument `.ods`
/// file or separated text (CSV and TSV). Each of its `Sheet`s is a
/// `com.codename1.ui.table.TableModel`, so a sheet is shown and edited by handing it to a
/// `com.codename1.ui.table.DataGrid`, and `Workbook.fromGrid` turns what a grid shows back
/// into a file.
///
/// ```java
/// Workbook book = Workbook.read(input, Workbook.Format.XLSX);
/// Sheet sheet = book.getSheet(0);
/// sheet.setHeaderRow(true);
/// DataGrid grid = new DataGrid(sheet);
/// grid.setColumns(sheet.createColumns());
/// ```
///
/// Cells keep their types: a number is a `Double`, a date a `java.util.Date`, a logical
/// value a `Boolean`, and an error such as `#DIV/0!` a `CellError`. Number formats are
/// Excel's format codes, and `Sheet.getCellText` answers what a spreadsheet would show.
///
/// Formulas are live. They are written the way Excel writes them, whichever format a file
/// is in, and a change to a cell recalculates every formula that reads it and tells the
/// sheet's listeners which cells changed, so a grid repaints them.
/// `Workbook.getSupportedFunctions` lists the functions a formula can call. A formula this
/// package cannot evaluate is never lost: its text is kept and written back, and its cell
/// shows the value the file stored for it.
///
/// Nothing in `com.codename1.ui.table` refers to this package, so an application that does
/// not use it does not carry it.
package com.codename1.spreadsheet;
