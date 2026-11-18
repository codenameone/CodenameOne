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
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.GridColumn;
import com.codename1.ui.table.TreeTableModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The workbook as a model: what our own writers keep, what an edit recalculates, and how a
/// sheet behaves under a grid.
class SpreadsheetModelTest extends UITestBase {
    private static Workbook copy(Workbook book, Workbook.Format format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        book.write(out, format);
        return Workbook.read(new ByteArrayInputStream(out.toByteArray()), format);
    }

    private static Workbook sample() {
        return SpreadsheetInteropExport.sample();
    }

    private static void assertSample(Workbook book, String what) {
        assertEquals(2, book.getSheetCount(), what);
        Sheet s = book.getSheet(0);
        assertEquals("Orders & <Returns>", s.getName(), what);
        assertEquals("a \"quoted\" <name> & more", s.getCellValue("A2"), what);
        assertEquals("  two  spaces\nand a line", s.getCellValue("A3"), what);
        assertEquals("00420", s.getCellValue("A4"), what);
        assertEquals(Double.valueOf(1e-7), s.getCellValue("B4"), what);
        assertEquals(Boolean.TRUE, s.getCellValue("D2"), what);
        assertEquals(Boolean.FALSE, s.getCellValue("D3"), what);
        assertEquals("=B2*C2", s.getCellFormula(1, 4), what);
        assertEquals(Double.valueOf(7.5), s.getCellValue("E2"), what);
        assertEquals(CellError.DIV0, s.getCellValue("E4"), what);
        assertEquals(Double.valueOf(7.5 - 4938 + 0.5), s.getCellValue("E5"), what);
        assertEquals("1,234.50", s.getCellText(2, 2), what);
        assertEquals("300.0%", s.getCellText(1, 1), what);
        assertEquals("2023-03-15 18:00", s.getCellText(5, 0), what);
        assertTrue(s.getCellValue("A6") instanceof Date, what);
        assertEquals(1, s.getFrozenRows(), what);
        assertEquals(2, s.getFrozenColumns(), what);
        assertEquals(30, s.getColumnWidth(0), 0.6, what);
        assertEquals(1, s.getRowOutlineLevel(2), what);
        assertEquals(2, s.getRowOutlineLevel(3), what);
        assertEquals(0, s.getRowOutlineLevel(1), what);
        assertEquals(1, s.getMergedRegionCount(), what);
        assertEquals(7, s.getMergedRegion(0)[2], what);
        assertEquals("0.5", book.getDefinedName("Tax"), what);
        assertEquals(Double.valueOf(2 * (7.5 - 4938 + 0.5)), book.getSheet("Other").getCellValue("A1"), what);
        // and it is alive: the formulas were read as formulas
        s.setCellValue("B2", Double.valueOf(10));
        assertEquals(Double.valueOf(25), s.getCellValue("E2"), what);
        assertEquals(Double.valueOf(2 * (25 - 4938 + 0.5)), book.getSheet("Other").getCellValue("A1"), what);
    }

    @FormTest
    void aWorkbookSurvivesXlsx() throws IOException {
        assertSample(sample(), "as built");
        assertSample(copy(sample(), Workbook.Format.XLSX), "xlsx");
    }

    @FormTest
    void aWorkbookSurvivesOds() throws IOException {
        assertSample(copy(sample(), Workbook.Format.ODS), "ods");
        assertSample(copy(copy(sample(), Workbook.Format.ODS), Workbook.Format.XLSX), "ods then xlsx");
    }

    private static Workbook csv(String text, Workbook.Format format) throws IOException {
        return Workbook.read(new ByteArrayInputStream(text.getBytes("UTF-8")), format);
    }

    @FormTest
    void separatedTextIsTyped() throws IOException {
        Workbook book = csv("name,zip,qty,price,when,ok\n"
                + "\"Smith, J\",00420,3,2.50,2024-03-15,true\n"
                + "Lee,10001,12,1e3,2024-12-01,FALSE\n", Workbook.Format.CSV);
        Sheet s = book.getSheet(0);
        assertEquals("Smith, J", s.getCellValue("A2"));
        // one cell of the column would lose its zeros as a number, so none becomes one
        assertEquals("00420", s.getCellValue("B2"));
        assertEquals("10001", s.getCellValue("B3"));
        assertEquals(Double.valueOf(3), s.getCellValue("C2"));
        assertEquals(Double.valueOf(1000), s.getCellValue("D3"));
        assertTrue(s.getCellValue("E2") instanceof Date, String.valueOf(s.getCellValue("E2")));
        assertEquals(Boolean.TRUE, s.getCellValue("F2"));
        assertEquals(Boolean.FALSE, s.getCellValue("F3"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        book.write(out, Workbook.Format.TSV);
        String tsv = new String(out.toByteArray(), "UTF-8");
        assertTrue(tsv.startsWith("name\tzip\tqty\tprice\twhen\tok"), tsv);
        assertTrue(tsv.indexOf("Smith, J\t00420\t3\t2.5\t2024-03-15\tTRUE") > 0, tsv);

        Workbook semi = csv("a;b\n1,5;x\n", Workbook.Format.CSV);
        assertEquals("x", semi.getSheet(0).getCellValue("B2"));
        Workbook tabs = csv("a\tb\n1\t=1+1\n", Workbook.Format.TSV);
        // text is never read as a formula: a file from somewhere else must not run one
        assertEquals("=1+1", tabs.getSheet(0).getCellValue("B2"));
        assertNull(tabs.getSheet(0).getCellFormula(1, 1));
    }

    @FormTest
    void rowsAndColumnsMoveTheFormulasThatReadThem() {
        Workbook book = new Workbook();
        Sheet s = book.addSheet("S");
        for (int i = 0; i < 4; i++) {
            s.setCellValue(i, 0, Double.valueOf(i + 1));
        }
        s.setCellFormula("C1", "=SUM(A1:A4)");
        s.setCellFormula("C2", "=A2*$A$4");
        s.insertRows(1, 2);
        assertEquals("=SUM(A1:A6)", s.getCellFormula(0, 2));
        assertEquals("=A4*$A$6", s.getCellFormula(3, 2));
        assertEquals(Double.valueOf(10), s.getCellValue("C1"));
        assertEquals(Double.valueOf(8), s.getCellValue("C4"));
        s.removeRows(1, 2);
        assertEquals("=SUM(A1:A4)", s.getCellFormula(0, 2));
        s.insertColumns(0, 1);
        assertEquals("=SUM(B1:B4)", s.getCellFormula(0, 3));
        assertEquals(Double.valueOf(10), s.getCellValue("D1"));
        s.removeColumns(0, 1);
        s.removeRows(3, 1);
        assertEquals("=SUM(A1:A3)", s.getCellFormula(0, 2));
        assertEquals(Double.valueOf(6), s.getCellValue("C1"));
        // the cell the second formula read is gone
        assertEquals(CellError.REF, s.getCellValue("C2"));
    }

    @FormTest
    void anEditTellsTheListenersOfEveryCellItChanged() {
        Workbook book = new Workbook();
        Sheet s = book.addSheet("S");
        s.setCellValue("A1", Double.valueOf(1));
        s.setCellFormula("B1", "=A1+1");
        s.setCellFormula("C1", "=B1*2");
        s.setCellFormula("D1", "=5");
        Sheet other = book.addSheet("T");
        other.setCellFormula("A1", "=S!C1");
        assertEquals(Double.valueOf(4), other.getCellValue("A1"));
        final List<String> seen = new ArrayList<String>();
        s.addDataChangeListener(new DataChangedListener() {
            public void dataChanged(int type, int index) {
                seen.add(type + "," + index);
            }
        });
        final List<String> seenOther = new ArrayList<String>();
        other.addDataChangeListener(new DataChangedListener() {
            public void dataChanged(int type, int index) {
                seenOther.add(type + "," + index);
            }
        });
        s.setValueAt(0, 0, "41");
        assertTrue(seen.contains("0,0") && seen.contains("0,1") && seen.contains("0,2"), seen.toString());
        assertFalse(seen.contains("0,3"), seen.toString());
        assertTrue(seenOther.contains("0,0"), seenOther.toString());
        assertEquals(Double.valueOf(84), s.getCellValue("C1"));
        assertEquals(Double.valueOf(84), other.getCellValue("A1"));
        s.setCellFormula("A1", "=C1");
        assertEquals(CellError.CIRCULAR, s.getCellValue("B1"));
        s.setValueAt(0, 0, "2");
        assertEquals(Double.valueOf(6), s.getCellValue("C1"));
    }

    @FormTest
    void whatIsTypedIsReadTheWayASpreadsheetReadsIt() {
        Workbook book = new Workbook();
        Sheet s = book.addSheet("S");
        s.setValueAt(0, 0, "12.5");
        s.setValueAt(1, 0, "=A1*2");
        s.setValueAt(2, 0, "'=A1*2");
        s.setValueAt(3, 0, "true");
        s.setValueAt(4, 0, "25%");
        s.setValueAt(5, 0, "2024-02-29");
        s.setValueAt(6, 0, "hello");
        s.setValueAt(7, 0, "#N/A");
        assertEquals(Double.valueOf(12.5), s.getValueAt(0, 0));
        assertEquals(Double.valueOf(25), s.getValueAt(1, 0));
        assertEquals("=A1*2", s.getFormulaAt(1, 0));
        assertEquals("=A1*2", s.getValueAt(2, 0));
        assertNull(s.getFormulaAt(2, 0));
        assertEquals(Boolean.TRUE, s.getValueAt(3, 0));
        assertEquals(Double.valueOf(0.25), s.getValueAt(4, 0));
        assertEquals("25%", s.getCellText(4, 0));
        assertTrue(s.getValueAt(5, 0) instanceof Date);
        assertEquals("2024-02-29", s.getCellText(5, 0));
        assertEquals("hello", s.getValueAt(6, 0));
        assertEquals(CellError.NA, s.getValueAt(7, 0));
    }

    @FormTest
    void aSheetIsTheModelOfAGrid() throws IOException {
        Workbook book = new Workbook();
        Sheet s = book.addSheet("Sales");
        s.addRow("Region", "Units", "Price", "Total");
        s.addRow("North", Double.valueOf(3), Double.valueOf(2));
        s.addRow("South", Double.valueOf(9), Double.valueOf(1.5));
        s.addRow("East", Double.valueOf(5), Double.valueOf(4));
        for (int r = 2; r <= 4; r++) {
            s.setCellFormula("D" + r, "=B" + r + "*C" + r);
            s.setCellFormat(r - 1, 3, "#,##0.00");
        }
        s.setHeaderRow(true);
        assertEquals(3, s.getRowCount());
        assertEquals("Total", s.getColumnName(3));

        DataGrid grid = new DataGrid(s);
        GridColumn[] columns = s.createColumns();
        assertEquals(4, columns.length);
        assertEquals("Units", columns[1].getTitle());
        assertEquals(GridColumn.Kind.NUMBER, columns[1].getKind());
        assertEquals(GridColumn.Kind.TEXT, columns[0].getKind());
        grid.setColumns(columns);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, grid);
        form.revalidate();
        grid.refreshLayout();

        grid.sortBy(columns[3], false);
        assertEquals("East", grid.getValueAt(0, columns[0]));
        assertEquals(Double.valueOf(20), grid.getValueAt(0, columns[3]));
        assertEquals("20.00", columns[3].getFormatter().format(grid.getValueAt(0, columns[3])));

        // An edit goes to the model's row, whatever the grid's order: the formula in D2 reads
        // B2, and sorting moved neither.
        s.setValueAt(0, 1, "100");
        assertEquals(Double.valueOf(200), s.getValueAt(0, 3));
        assertEquals("North", grid.getValueAt(0, columns[0]));
        assertEquals(Double.valueOf(200), grid.getValueAt(0, columns[3]));

        Workbook exported = copy(Workbook.fromGrid(grid), Workbook.Format.XLSX);
        Sheet e = exported.getSheet(0);
        assertEquals("Region", e.getCellValue("A1"));
        assertEquals("North", e.getCellValue("A2"));
        assertEquals("East", e.getCellValue("A3"));
        assertEquals("South", e.getCellValue("A4"));
        assertEquals(Double.valueOf(200), e.getCellValue("D2"));
        assertEquals(1, e.getFrozenRows());
    }

    @FormTest
    void textFromAFileNeverBecomesAFormula() throws IOException {
        byte[] csv = "code,sum\n00420,=1+1\n00007,=A2\n".getBytes("UTF-8");
        Sheet s = Workbook.read(new ByteArrayInputStream(csv), Workbook.Format.CSV).getSheet(0);
        assertEquals("=1+1", s.getCellValue("B2"));
        assertNull(s.getCellFormula(1, 1));
        // one cell of the column would lose its zeros as a number, so none is read as one
        assertEquals("00420", s.getCellValue("A2"));
        assertEquals("00007", s.getCellValue("A3"));
    }

    @FormTest
    void aFormulaThatCannotBeCalculatedKeepsWhatTheFileStored() throws IOException {
        Workbook book = new Workbook();
        Sheet s = book.addSheet("S");
        s.setCellValue("A1", Double.valueOf(2));
        s.loadFormula(0, 1, "IMSUM(A1,\"3+4i\")", false, "5+4i", true, false);
        s.loadFormula(1, 1, "A1 +* ((", false, Double.valueOf(7), true, false);
        assertEquals("5+4i", s.getCellValue("B1"));
        assertEquals(Double.valueOf(7), s.getCellValue("B2"));

        Sheet read = copy(book, Workbook.Format.XLSX).getSheet(0);
        assertEquals("=IMSUM(A1,\"3+4i\")", read.getCellFormula(0, 1));
        assertEquals("5+4i", read.getCellValue("B1"));

        // what the file stored is stale once a cell the formula reads has changed
        s.setCellValue("A1", Double.valueOf(3));
        assertEquals(CellError.NAME, s.getCellValue("B1"));
    }

    @FormTest
    void aTreeGridIsSavedAsAnOutline() throws IOException {
        DataGrid grid = new DataGrid(new Folders());
        grid.setFrozenColumns(1);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, grid);
        form.revalidate();
        grid.refreshLayout();
        grid.expandAll();

        Sheet e = copy(Workbook.fromGrid(grid), Workbook.Format.XLSX).getSheet(0);
        assertEquals("src", e.getCellValue("A2"));
        assertEquals("src/main", e.getCellValue("A3"));
        assertEquals("docs", e.getCellValue("A4"));
        assertEquals(Double.valueOf(8), e.getCellValue("B3"));
        assertEquals(0, e.getRowOutlineLevel(1));
        assertEquals(1, e.getRowOutlineLevel(2));
        assertEquals(0, e.getRowOutlineLevel(3));
        assertEquals(1, e.getFrozenRows());
        assertEquals(1, e.getFrozenColumns());
    }

    private static final class Folders implements TreeTableModel {
        public Vector getChildren(Object parent) {
            Vector<String> v = new Vector<String>();
            if (parent == null) {
                v.add("src");
                v.add("docs");
            } else if ("src".equals(parent)) {
                v.add("src/main");
            }
            return v;
        }

        public boolean isLeaf(Object node) {
            return !"src".equals(node);
        }

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int column) {
            return column == 0 ? "Path" : "Size";
        }

        public Object getValueAt(Object node, int column) {
            return column == 0 ? node : (Object) Double.valueOf(((String) node).length());
        }

        public boolean isCellEditable(Object node, int column) {
            return false;
        }

        public void setValueAt(Object node, int column, Object value) {
        }
    }

    @FormTest
    void everyListedFunctionIsOneTheEvaluatorKnows() {
        Workbook book = new Workbook();
        Sheet s = book.addSheet("S");
        s.setCellValue("A1", Double.valueOf(1));
        String[] names = Workbook.getSupportedFunctions();
        assertTrue(names.length > 380, "functions: " + names.length);
        StringBuilder unknown = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            if (i > 0) {
                assertTrue(names[i - 1].compareTo(names[i]) < 0, names[i - 1] + " before " + names[i]);
            }
            // No number of arguments may do worse than answer an error: a formula is text
            // from a file, and a function must not index past the arguments it was given.
            boolean known = false;
            String[] samples = {"A1", "\"x\"", "A1:A3", "{1,2;3,4}", "TRUE", "-1"};
            for (int args = 0; args < 7; args++) {
                for (int kind = 0; kind < samples.length; kind++) {
                    StringBuilder f = new StringBuilder("=").append(names[i]).append('(');
                    for (int a = 0; a < args; a++) {
                        f.append(a == 0 ? "" : ",").append(samples[(kind + a) % samples.length]);
                    }
                    s.setCellFormula("B1", f.append(')').toString());
                    assertNotNull(s.getCellFormula(0, 1));
                    try {
                        known |= s.getCellValue("B1") != CellError.NAME;
                    } catch (RuntimeException e) {
                        known = true;
                        unknown.append('\n').append(f).append(" threw ").append(e);
                    }
                    if (args == 0) {
                        break;
                    }
                }
            }
            if (!known) {
                unknown.append('\n').append(names[i]).append(" is not a function");
            }
        }
        assertEquals("", unknown.toString());
        s.setCellFormula("B1", "=NOSUCHFUNCTION(1)");
        assertEquals(CellError.NAME, s.getCellValue("B1"));
    }

    private static void shows(String expected, String code, Object value) {
        Workbook book = new Workbook();
        Sheet s = book.addSheet("S");
        s.setCellValue(0, 0, value);
        s.setCellFormat(0, 0, code);
        assertEquals(expected, s.getCellText(0, 0), code + " of " + value);
    }

    @FormTest
    void formatCodesShowWhatASpreadsheetShows() {
        shows("1234.5", "General", Double.valueOf(1234.5));
        shows("1,234.50", "#,##0.00", Double.valueOf(1234.5));
        shows("-1,234.50", "#,##0.00", Double.valueOf(-1234.5));
        shows("(1,235)", "#,##0;(#,##0)", Double.valueOf(-1234.5));
        shows("0012", "0000", Double.valueOf(12));
        shows("12.5%", "0.0%", Double.valueOf(0.125));
        shows("1.23E+06", "0.00E+00", Double.valueOf(1234567));
        shows("1.2 k", "0.0, \"k\"", Double.valueOf(1234));
        shows("$5.00", "\"$\"0.00", Double.valueOf(5));
        shows("1 1/2", "# ?/?", Double.valueOf(1.5));
        shows(".5", ".#", Double.valueOf(0.5));
        shows("zero", "0;-0;\"zero\"", Double.valueOf(0));
        shows("<hi>", "0;0;0;\"<\"@\">\"", "hi");
        shows("2024-02-29", "yyyy-mm-dd", Double.valueOf(45351));
        shows("29/2/24", "d/m/yy", Double.valueOf(45351));
        shows("Thu 29 Feb 2024", "ddd d mmm yyyy", Double.valueOf(45351));
        shows("Thursday, February 29", "dddd, mmmm d", Double.valueOf(45351));
        shows("18:30:00", "hh:mm:ss", Double.valueOf(0.7708333333333334));
        shows("6:30 PM", "h:mm AM/PM", Double.valueOf(0.7708333333333334));
        shows("36:00", "[h]:mm", Double.valueOf(1.5));
        shows("1900-02-29", "yyyy-mm-dd", Double.valueOf(60));
        shows("TRUE", "0.00", Boolean.TRUE);
        shows("big", "[>100]\"big\";0", Double.valueOf(500));
        shows("50", "[>100]\"big\";0", Double.valueOf(50));
    }
}
