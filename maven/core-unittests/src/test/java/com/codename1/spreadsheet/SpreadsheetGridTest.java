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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.GridColumn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A sheet edited through a grid, the way a user does it: a field opened over a cell and
/// closed again, with a header row over the data so that the two row numberings differ.
class SpreadsheetGridTest extends UITestBase {
    private Workbook book;
    private Sheet sheet;
    private DataGrid grid;
    private GridColumn[] columns;

    private void open() {
        book = new Workbook();
        sheet = book.addSheet("Orders");
        sheet.addRow("Item", "Price", "Units", "Total", "Share", "Code");
        sheet.addRow("Desk", Double.valueOf(1234.5), Double.valueOf(3), null, Double.valueOf(0.0525), "00123");
        sheet.addRow("Lamp", Double.valueOf(1.23456), Double.valueOf(2), null, Double.valueOf(0.5), "00124");
        sheet.addRow("Refund", Double.valueOf(-5), Double.valueOf(1), null, Double.valueOf(0.25), "00125");
        for (int r = 1; r <= 3; r++) {
            sheet.setCellFormula(r, 3, "=B" + (r + 1) + "*C" + (r + 1));
            sheet.setCellFormat(r, 3, "#,##0.00");
            sheet.setCellFormat(r, 4, "0.0%");
            sheet.setCellFormat(r, 5, "@");
        }
        sheet.setCellFormat(1, 1, "$#,##0.00");
        sheet.setCellFormat(2, 1, "0.00");
        sheet.setCellFormat(3, 1, "#,##0.00;(#,##0.00)");
        sheet.setHeaderRow(true);
        grid = new DataGrid(sheet);
        columns = sheet.createColumns();
        grid.setColumns(columns);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, grid);
        form.revalidate();
        grid.refreshLayout();
    }

    private TextField editor() {
        Form f = Display.getInstance().getCurrent();
        return find(f.getFormLayeredPane(DataGrid.class, true));
    }

    private static TextField find(Container c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component k = c.getComponentAt(i);
            if (k instanceof TextField) {
                return (TextField) k;
            }
            if (k instanceof Container) {
                TextField t = find((Container) k);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    private String openAndClose(int row, int column) {
        assertTrue(grid.editCellAt(row, column), "row " + row + " column " + column + " is editable");
        TextField field = editor();
        assertNotNull(field, "an editor for row " + row + " column " + column);
        String shown = field.getText();
        grid.commitEdit();
        return shown;
    }

    private void type(int row, int column, String text) {
        assertTrue(grid.editCellAt(row, column));
        editor().setText(text);
        grid.commitEdit();
    }

    @FormTest
    void aFieldOpenedAndClosedChangesNothing() {
        open();
        final List<String> events = new ArrayList<String>();
        sheet.addDataChangeListener(new DataChangedListener() {
            public void dataChanged(int type, int index) {
                events.add(type + "," + index);
            }
        });
        // a formula opens as the formula, and is one afterwards
        assertEquals("=B2*C2", openAndClose(0, 3));
        assertEquals("=B2*C2", sheet.getFormulaAt(0, 3));
        assertEquals(Double.valueOf(3703.5), sheet.getValueAt(0, 3));
        // a number opens whole, whatever its format shows of it
        assertEquals("1234.5", openAndClose(0, 1));
        assertEquals(Double.valueOf(1234.5), sheet.getValueAt(0, 1));
        assertEquals("1.23456", openAndClose(1, 1));
        assertEquals(Double.valueOf(1.23456), sheet.getValueAt(1, 1));
        assertEquals("-5", openAndClose(2, 1));
        assertEquals(Double.valueOf(-5), sheet.getValueAt(2, 1));
        assertEquals("5.25%", openAndClose(0, 4));
        assertEquals(Double.valueOf(0.0525), sheet.getValueAt(0, 4));
        assertEquals("00123", openAndClose(0, 5));
        assertEquals("00123", sheet.getValueAt(0, 5));
        assertEquals("Desk", openAndClose(0, 0));
        assertEquals("[]", events.toString(), "nothing was written, so nothing is announced");
    }

    @FormTest
    void whatIsTypedIsReadAsASpreadsheetReadsIt() {
        open();
        final List<String> events = new ArrayList<String>();
        sheet.addDataChangeListener(new DataChangedListener() {
            public void dataChanged(int type, int index) {
                events.add(type + "," + index);
            }
        });
        // read the total first: a formula nobody read since it went stale is not announced
        assertEquals(Double.valueOf(3703.5), sheet.getValueAt(0, 3));
        type(0, 1, "2000");
        assertEquals(Double.valueOf(2000), sheet.getValueAt(0, 1));
        assertEquals("$2,000.00", sheet.getCellText(1, 1), "the format of the cell stays");
        assertEquals(Double.valueOf(6000), sheet.getValueAt(0, 3));
        // the rows of the events are the rows of the model, under the header: the price and
        // the total of the first row, each once
        Collections.sort(events);
        assertEquals("[0,1, 0,3]", events.toString());

        type(1, 3, "=B3*C3*2");
        assertEquals("=B3*C3*2", sheet.getFormulaAt(1, 3));
        assertEquals("=B3*C3*2", openAndClose(1, 3));
        type(2, 3, "7");
        assertNull(sheet.getFormulaAt(2, 3), "a number typed over a formula replaces it");
        assertEquals(Double.valueOf(7), sheet.getValueAt(2, 3));
        type(0, 4, "50%");
        assertEquals(Double.valueOf(0.5), sheet.getValueAt(0, 4));
        // a cell formatted as text keeps the zeros of what is typed into it
        type(0, 5, "00999");
        assertEquals("00999", sheet.getValueAt(0, 5));
    }

    @FormTest
    void eachCellIsPaintedInItsOwnFormat() {
        open();
        // one column, three formats: the column's formatter knows one of them
        assertEquals("$1,234.50", SheetCells.text(sheet, grid, 0, columns[1]));
        assertEquals("1.23", SheetCells.text(sheet, grid, 1, columns[1]));
        assertEquals("(5.00)", SheetCells.text(sheet, grid, 2, columns[1]));
        assertEquals("3,703.50", SheetCells.text(sheet, grid, 0, columns[3]));
        // sorted, a row of the view is another row of the sheet
        grid.sortBy(columns[1], true);
        assertEquals("(5.00)", SheetCells.text(sheet, grid, 0, columns[1]));
        assertEquals("$1,234.50", SheetCells.text(sheet, grid, 2, columns[1]));
        // a date formula typed into a column of numbers shows as a date
        grid.sortBy(columns[1], true);
        sheet.setValueAt(0, 2, "=DATE(2024,3,15)");
        assertEquals("2024-03-15", SheetCells.text(sheet, grid, 2, columns[2]));
        assertNull(SheetCells.text(sheet, new DataGrid(new Workbook().addSheet("Other")), 0, columns[1]),
                "the columns of one sheet on a grid of another fall back to their formatter");
    }
}
