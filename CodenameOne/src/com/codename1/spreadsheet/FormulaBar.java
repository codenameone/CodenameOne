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
package com.codename1.spreadsheet;

import com.codename1.ui.CodeEditor;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.GridColumn;

/// The bar a spreadsheet application has over its cells: the address of the selected cell,
/// and its formula, or its value, in an editor of one line. What is typed there is edited as
/// in a cell -- references colored and outlined in the grid, functions completed, arguments
/// hinted, a tap on a cell putting its address in -- and Enter writes it to the cell.
///
/// ```java
/// Workbook book = new Workbook();
/// // com.codename1.ui has a Sheet too, hence the full name
/// com.codename1.spreadsheet.Sheet sheet = book.addSheet("Prices");
/// sheet.setCellValue("A1", Double.valueOf(3.5));
/// sheet.setCellValue("B1", "=A1*2");
/// DataGrid grid = new DataGrid(sheet);
/// grid.setColumns(sheet.createColumns());
/// Form form = new Form("Prices", new BorderLayout());
/// form.add(BorderLayout.NORTH, new FormulaBar(sheet, grid));
/// form.add(BorderLayout.CENTER, grid);
/// form.show();
/// ```
///
/// The bar follows the grid's selection and the sheet's changes. Escape puts back what the
/// cell holds; a formula that cannot be read is not written, and the bar says why. The name
/// box is a `Label` with the UIID `FormulaBarName`, the `fx` before the editor one with
/// `FormulaBarFx`, and the bar itself has the UIID `FormulaBar`.
public class FormulaBar extends Container {
    private final Sheet sheet;
    private final DataGrid grid;
    private final Label name = new Label("", "FormulaBarName");
    private final Editor editor = new Editor();
    private final FormulaEditing editing;
    private final Listener listener = new Listener();
    private String shown = "";
    private int row = -1;
    private int column = -1;

    /// Creates a bar for a sheet shown in a grid.
    ///
    /// #### Parameters
    ///
    /// - `sheet`: the sheet, which is the grid's model
    ///
    /// - `grid`: the grid that shows it
    public FormulaBar(Sheet sheet, DataGrid grid) {
        super(new BorderLayout(), "FormulaBar");
        this.sheet = sheet;
        this.grid = grid;
        editing = new FormulaEditing(sheet, grid, editor, "", new Enter(this, true), new Enter(this, false));
        editing.setOnlyWhileTyped(true);
        editor.bar = this;
        Container west = new Container(new BorderLayout());
        west.add(BorderLayout.CENTER, name);
        west.add(BorderLayout.EAST, new Label("fx", "FormulaBarFx"));
        add(BorderLayout.WEST, west);
        add(BorderLayout.CENTER, editor);
    }

    /// The address of the cell the bar shows, as `B4`, or an empty string when no cell is
    /// selected.
    ///
    /// #### Returns
    ///
    /// the address
    public String getAddress() {
        return name.getText();
    }

    /// The text in the bar now: what was typed, or what the selected cell holds.
    ///
    /// #### Returns
    ///
    /// the text
    public String getText() {
        return editing.text();
    }

    /// Puts text into the bar as if it had been typed, so that it is edited as typing it
    /// would be: Enter, or `#enter()`, writes it to the cell.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text
    public void setText(String text) {
        editing.setText(text);
        if (!editing.isActive()) {
            editing.textChangedByUser();
        }
    }

    /// Writes what is in the bar to the selected cell, as Enter does. Text that is what the
    /// cell holds writes nothing.
    ///
    /// #### Returns
    ///
    /// false when the text is a formula that cannot be read, which is then not written
    public boolean enter() {
        editing.press(CodeEditor.KEY_ENTER);
        return editing.error() == null;
    }

    /// The editing behind the bar, for the tests in this package.
    FormulaEditing editing() {
        return editing;
    }

    void write() {
        String text = editing.text();
        if (row >= 0 && column >= 0 && !text.equals(shown)) {
            int modelRow = row - sheet.headerRows();
            sheet.setValueAt(modelRow, column, text);
        }
        load();
    }

    /// Shows the selected cell again.
    void load() {
        int viewRow = grid.getSelectedRow();
        int visible = grid.getSelectedColumn();
        GridColumn[] columns = grid.getVisibleColumns();
        row = viewRow < 0 ? -1 : SheetCells.sheetRow(sheet, grid, viewRow);
        column = visible >= 0 && visible < columns.length ? columns[visible].getModelColumn() : -1;
        if (row < 0 || column < 0) {
            row = -1;
            column = -1;
            name.setText("");
            shown = "";
        } else {
            StringBuilder sb = new StringBuilder();
            FormulaParser.appendColumn(sb, column);
            name.setText(sb.append(row + 1).toString());
            shown = sheet.editText(row, column);
        }
        editing.deactivate();
        editing.setText(shown);
        revalidateLater();
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        grid.addSelectionListener(listener);
        sheet.addDataChangeListener(listener);
        load();
    }

    @Override
    protected void deinitialize() {
        grid.removeSelectionListener(listener);
        sheet.removeDataChangeListener(listener);
        super.deinitialize();
    }

    private final class Listener implements SelectionListener, DataChangedListener {
        @Override
        public void selectionChanged(int oldSelected, int newSelected) {
            if (editing.isActive()) {
                // moving to another cell enters what was typed into the one it was typed for
                write();
                return;
            }
            load();
        }

        @Override
        public void dataChanged(int type, int index) {
            if (!editing.isActive()) {
                load();
            }
        }
    }

    private static final class Enter implements Runnable {
        private final FormulaBar bar;
        private final boolean write;

        Enter(FormulaBar bar, boolean write) {
            this.bar = bar;
            this.write = write;
        }

        @Override
        public void run() {
            if (write) {
                bar.write();
            } else {
                bar.load();
            }
        }
    }

    private static final class Editor extends CodeEditor {
        private FormulaBar bar;

        @Override
        protected void initComponent() {
            super.initComponent();
            bar.editing.attach();
        }

        @Override
        protected void deinitialize() {
            bar.editing.detach();
            super.deinitialize();
        }
    }
}
