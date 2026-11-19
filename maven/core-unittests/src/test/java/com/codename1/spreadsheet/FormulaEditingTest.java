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
import com.codename1.ui.CodeEditor;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.table.DataGrid;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A formula typed into a cell the way a user types it: half finished most of the time,
/// completed from the list, its arguments hinted and its references picked from the grid.
class FormulaEditingTest extends UITestBase {
    private Workbook book;
    private Sheet sheet;
    private DataGrid grid;

    @Test
    void lexerColorsEachDistinctReference() {
        String f = "=SUM(A1:B2, 'My Data'!C3, $D$4) + a1:b2 + A1 + B:B + 3:4";
        List<FormulaLexer.Token> tokens = FormulaLexer.lex(f);
        StringBuilder sb = new StringBuilder();
        for (FormulaLexer.Token t : tokens) {
            if (t.kind == FormulaLexer.REFERENCE) {
                sb.append(f, t.start, t.end).append('=').append(t.color).append(' ');
            }
        }
        assertEquals("A1:B2=0 'My Data'!C3=1 $D$4=2 a1:b2=0 A1=3 B:B=4 3:4=5 ", sb.toString());
        FormulaLexer.Token sheetRef = null;
        for (FormulaLexer.Token t : tokens) {
            if (t.kind == FormulaLexer.REFERENCE && t.sheet != null) {
                sheetRef = t;
            }
        }
        assertNotNull(sheetRef);
        assertEquals("My Data", sheetRef.sheet);
        assertEquals(2, sheetRef.r1);
        assertEquals(2, sheetRef.c1);
    }

    @Test
    void lexerTellsNamesFunctionsAndNumbersFromCells() {
        String f = "=LOG10(Rate) + 1.5E3 + TRUE + \"A1\" + #N/A + XFD1048576 + XFE1";
        StringBuilder sb = new StringBuilder();
        for (FormulaLexer.Token t : FormulaLexer.lex(f)) {
            if (t.kind != FormulaLexer.SPACE && t.kind != FormulaLexer.OPERATOR) {
                sb.append(t.kind).append(':').append(f, t.start, t.end).append(' ');
            }
        }
        assertEquals("6:LOG10 9:( 7:Rate 10:) 3:1.5E3 5:TRUE 2:\"A1\" 4:#N/A 8:XFD1048576 7:XFE1 ", sb.toString());
        assertTrue(FormulaLexer.lex("SUM(A1)").isEmpty(), "a value, not a formula");
    }

    @Test
    void theCallAroundTheCaret() {
        String f = "=IF(A1>0, SUM(1, (2+3";
        Object[] call = FormulaLexer.call(FormulaLexer.lex(f), f, f.length());
        assertEquals("SUM", call[0]);
        assertEquals(Integer.valueOf(1), call[1]);
        String g = "=IF(A1>0, SUM(1, 2), {1,2,";
        call = FormulaLexer.call(FormulaLexer.lex(g), g, g.length());
        assertEquals("IF", call[0]);
        assertEquals(Integer.valueOf(2), call[1], "commas in an array constant separate values, not arguments");
        assertNull(FormulaLexer.call(FormulaLexer.lex("=1+2"), "=1+2", 4));
    }

    @Test
    void helpKnowsEveryFunctionTheEvaluatorHas() {
        String[] names = FormulaHelp.names();
        assertTrue(names.length > 400);
        assertEquals("number1, [number2], ...", FormulaHelp.of("SUM")[0]);
        String[] args = FormulaHelp.arguments(FormulaHelp.of("SUMIFS")[0]);
        assertEquals("sum_range", args[0]);
        assertEquals("[criteria_range2, criteria2]", args[3]);
        for (String name : names) {
            assertNotNull(FormulaParser.canonicalFunctionName(name), name);
        }
    }

    private void open() {
        book = new Workbook();
        sheet = book.addSheet("Sales");
        sheet.addRow("Region", "Q1", "Q2", "Total");
        sheet.addRow("North", Double.valueOf(10), Double.valueOf(20), null);
        sheet.addRow("South", Double.valueOf(30), Double.valueOf(40), null);
        sheet.addRow("East", Double.valueOf(50), Double.valueOf(60), null);
        sheet.setCellFormula(1, 3, "=B2+C2");
        sheet.setHeaderRow(true);
        book.setDefinedName("Rate", "Sales!$B$2");
        grid = new DataGrid(sheet);
        grid.setColumns(sheet.createColumns());
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, grid);
        form.revalidate();
        grid.refreshLayout();
    }

    private FormulaEditing.CellEditor edit(int row, int column) {
        assertTrue(grid.editCellAt(row, column));
        FormulaEditing.CellEditor ed = find(Display.getInstance().getCurrent().getFormLayeredPane(DataGrid.class, true));
        assertNotNull(ed);
        // the editor's backend comes up on the next serial call, as on a device
        flushSerialCalls();
        assertTrue(ed.isEditorReady());
        return ed;
    }

    private static FormulaEditing.CellEditor find(Container c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component k = c.getComponentAt(i);
            if (k instanceof FormulaEditing.CellEditor) {
                return (FormulaEditing.CellEditor) k;
            }
            if (k instanceof Container) {
                FormulaEditing.CellEditor t = find((Container) k);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    private static void type(FormulaEditing.CellEditor ed, String text) {
        ed.getEditing().setText(text);
    }

    @FormTest
    void aFormulaIsTypedCompletedPickedAndEntered() {
        open();
        FormulaEditing.CellEditor ed = edit(0, 3);
        assertEquals("=B2+C2", ed.currentText());
        assertSame(ed.getEditing(), grid.getCellPicker(), "the grid's taps go to the formula");
        assertSame(ed.getEditing(), grid.getOverlayPainter(), "the references are outlined");
        FormulaEditing editing = ed.getEditing();

        type(ed, "=SU");
        List<String> items = editing.shownItems();
        assertTrue(items.contains("SUM") && items.contains("SUMIFS") && items.contains("SUBSTITUTE"), items.toString());
        assertTrue(items.indexOf("SUBSTITUTE") < items.indexOf("SUM"), "alphabetical");
        editing.press(CodeEditor.KEY_DOWN);
        editing.press(CodeEditor.KEY_UP);
        int sum = items.indexOf("SUM");
        for (int i = 0; i < sum; i++) {
            editing.press(CodeEditor.KEY_DOWN);
        }
        editing.press(CodeEditor.KEY_ENTER);
        assertEquals("=SUM(", ed.currentText());
        assertTrue(grid.isEditing(), "choosing a function does not enter the formula");
        assertEquals("SUM", editing.hintName());
        assertEquals(0, editing.hintArgument());

        assertTrue(editing.isPicking(grid));
        editing.cellPicked(grid, 0, 1, false);
        assertEquals("=SUM(B2", ed.currentText());
        editing.cellPicked(grid, 2, 1, false);
        assertEquals("=SUM(B4", ed.currentText(), "a second tap replaces the reference it put in");
        editing.cellPicked(grid, 1, 2, true);
        assertEquals("=SUM(B3:C4", ed.currentText(), "shift extends it into a range");

        type(ed, "=SUM(B3:C4, ");
        assertEquals(1, editing.hintArgument(), "past number2 the repeating argument stays bold");
        type(ed, "=SUM(B3:C4, 1, 2, ");
        assertEquals(1, editing.hintArgument());
        type(ed, "=SUM(B3:C4) + Ra");
        assertTrue(editing.shownItems().contains("Rate"), "defined names complete too");
        assertTrue(editing.shownItems().contains("RATE"));
        type(ed, "=SUM(B3:C4)");
        assertFalse(editing.isPicking(grid), "after a closing parenthesis a tap is a tap");
        editing.press(CodeEditor.KEY_ENTER);
        assertFalse(grid.isEditing());
        assertEquals("=SUM(B3:C4)", sheet.getCellFormula(1, 3));
        assertEquals(Double.valueOf(180), sheet.getValueAt(0, 3), "B3:C4 is South and East");
        assertNull(grid.getCellPicker(), "the grid's taps are its own again");
        assertNull(grid.getOverlayPainter());
    }

    @FormTest
    void anUnfinishedFormulaIsNotEntered() {
        open();
        FormulaEditing.CellEditor ed = edit(0, 3);
        FormulaEditing editing = ed.getEditing();
        type(ed, "=SUM(B2,");
        editing.press(CodeEditor.KEY_ENTER);
        assertTrue(grid.isEditing(), "the formula cannot be read, so it stays in the editor");
        assertNotNull(editing.error());
        assertEquals("=B2+C2", sheet.getCellFormula(1, 3));
        editing.press(CodeEditor.KEY_ESCAPE);
        assertFalse(grid.isEditing(), "escape gives up the edit");
        assertEquals("=B2+C2", sheet.getCellFormula(1, 3));
    }

    @FormTest
    void aValueIsEditedInTheSameEditor() {
        open();
        FormulaEditing.CellEditor ed = edit(0, 1);
        assertEquals("10", ed.currentText());
        assertTrue(ed.isSingleLine());
        type(ed, "12.5");
        assertTrue(ed.getEditing().shownItems().isEmpty());
        assertFalse(ed.getEditing().isPicking(grid), "a value takes no reference");
        ed.getEditing().press(CodeEditor.KEY_ENTER);
        assertEquals(Double.valueOf(12.5), sheet.getValueAt(0, 1));
        assertEquals(Double.valueOf(32.5), sheet.getValueAt(0, 3));
    }

    private void tap(int row, int column) {
        int[] b = new int[4];
        grid.getCellBounds(row, column, b);
        int x = grid.getAbsoluteX() + b[0] + b[2] / 2;
        int y = grid.getAbsoluteY() + b[1] + b[3] / 2;
        grid.pointerPressed(x, y);
        grid.pointerReleased(x, y);
        // two taps in a row on one cell would read as a double tap
        grid.pointerPressed(-1, -1);
        grid.pointerReleased(-1, -1);
    }

    @FormTest
    void theFormulaBarFollowsTheSelectionAndWritesTheCell() {
        open();
        FormulaBar bar = new FormulaBar(sheet, grid);
        Form form = Display.getInstance().getCurrent();
        form.add(BorderLayout.NORTH, bar);
        form.revalidate();
        flushSerialCalls();
        tap(0, 3);
        assertEquals("D2", bar.getAddress());
        assertEquals("=B2+C2", bar.getText());
        assertNull(grid.getCellPicker(), "showing a cell takes nothing from the grid");
        assertNull(grid.getOverlayPainter());

        bar.setText("=SUM(");
        assertSame(bar.editing(), grid.getCellPicker(), "typing in the bar takes the grid's taps");
        assertEquals("SUM", bar.editing().hintName());
        tap(0, 1);
        assertEquals("=SUM(B2", bar.getText(), "a tap on a cell is a reference, through the grid's picker");
        assertEquals("D2", bar.getAddress(), "and the selection stays");
        bar.editing().cellPicked(grid, 2, 2, true);
        assertEquals("=SUM(B2:C4", bar.getText());
        assertFalse(bar.enter(), "unfinished");
        bar.setText("=SUM(B2:C4)");
        assertTrue(bar.enter());
        assertEquals("=SUM(B2:C4)", sheet.getCellFormula(1, 3));
        assertNull(grid.getCellPicker());
        assertEquals("=SUM(B2:C4)", bar.getText());

        tap(1, 1);
        assertEquals("B3", bar.getAddress());
        assertEquals("30", bar.getText());
        sheet.setValueAt(1, 1, Double.valueOf(31));
        assertEquals("31", bar.getText(), "a change to the sheet shows in the bar");
        bar.setText("32");
        tap(2, 1);
        assertEquals(Double.valueOf(32), sheet.getValueAt(1, 1), "moving on enters what was typed");
        assertEquals("B4", bar.getAddress());
    }
}
