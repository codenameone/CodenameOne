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

import com.codename1.ui.Component;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.DefaultGridCellEditor;
import com.codename1.ui.table.DefaultGridCellRenderer;
import com.codename1.ui.table.GridCell;
import com.codename1.ui.table.GridColumn;

/// How a grid shows and edits the cells of a sheet. A column of a grid has one formatter,
/// and a column of a sheet has a format for each cell: the total under a column of counts
/// is money. These read the cell itself.
final class SheetCells {
    private static final String SHOWN = "cn1.spreadsheet.shown";

    private SheetCells() {
    }

    /// The sheet row a cell of the grid shows, or -1 for a group row, a tree, or a grid
    /// that shows some other model with these columns.
    static int sheetRow(Sheet sheet, GridCell cell) {
        return cell.isGroupRow() ? -1 : sheetRow(sheet, cell.getGrid(), cell.getRow());
    }

    static int sheetRow(Sheet sheet, DataGrid grid, int viewRow) {
        if (grid == null || grid.getTableModel() != sheet) { //NOPMD CompareObjectsWithEquals - the same model, not an equal one
            return -1;
        }
        int row = grid.getModelRow(viewRow);
        return row < 0 ? -1 : row + sheet.headerRows();
    }

    /// The text a grid paints for a row it shows, or null where the column's formatter
    /// decides.
    static String text(Sheet sheet, DataGrid grid, int viewRow, GridColumn column) {
        int row = sheetRow(sheet, grid, viewRow);
        return row < 0 ? null : sheet.getCellText(row, column.getModelColumn());
    }

    /// Paints each cell in its own number format.
    static final class Renderer extends DefaultGridCellRenderer {
        private final Sheet sheet;

        Renderer(Sheet sheet) {
            this.sheet = sheet;
        }

        @Override
        protected String text(GridCell cell) {
            String text = cell.isGroupRow() ? null : SheetCells.text(sheet, cell.getGrid(), cell.getRow(), cell.getColumn());
            return text == null ? super.text(cell) : text;
        }
    }

    /// Edits what a spreadsheet edits: the formula of a cell that has one, and otherwise the
    /// whole value and not its formatted text, which may have fewer digits. A field left as
    /// it was opened writes nothing.
    static final class Editor extends DefaultGridCellEditor {
        private final Sheet sheet;

        Editor(Sheet sheet) {
            this.sheet = sheet;
        }

        @Override
        public Component createEditor(DataGrid grid, GridCell cell) {
            int row = sheetRow(sheet, cell);
            if (row < 0) {
                return super.createEditor(grid, cell);
            }
            int column = cell.getColumn().getModelColumn();
            GridColumn.Kind kind = cell.getColumn().resolveKind(cell.getValue());
            boolean picks = kind == GridColumn.Kind.DATE || kind == GridColumn.Kind.BOOLEAN
                    || kind == GridColumn.Kind.CHOICE;
            if (picks && sheet.getCellFormula(row, column) == null) {
                return super.createEditor(grid, cell);
            }
            String text = sheet.editText(row, column);
            FormulaEditing.CellEditor ed = new FormulaEditing.CellEditor();
            ed.setText(text);
            ed.setCursorPosition(text.length());
            ed.putClientProperty(SHOWN, text);
            ed.setEditing(new FormulaEditing(sheet, grid, ed, text, new Commit(grid, true), new Commit(grid, false)));
            return ed;
        }

        @Override
        public Object getEditorValue(Component editor, GridCell cell) {
            Object shown = editor.getClientProperty(SHOWN);
            if (!(shown instanceof String) || !(editor instanceof FormulaEditing.CellEditor)) {
                return super.getEditorValue(editor, cell);
            }
            String text = ((FormulaEditing.CellEditor) editor).currentText();
            // the value the cell has is the answer that writes nothing
            return shown.equals(text) ? cell.getValue() : text;
        }
    }

    private static final class Commit implements Runnable {
        private final DataGrid grid;
        private final boolean commit;

        Commit(DataGrid grid, boolean commit) {
            this.grid = grid;
            this.commit = commit;
        }

        @Override
        public void run() {
            if (commit) {
                grid.commitEdit();
            } else {
                grid.cancelEdit();
            }
        }
    }
}
