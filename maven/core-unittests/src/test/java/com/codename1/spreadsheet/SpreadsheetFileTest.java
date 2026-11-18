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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Reads workbooks LibreOffice calculated and saved (see
/// `scripts/spreadsheet-interop/make-fixtures.py`), and holds the evaluator to the values
/// LibreOffice stored beside each formula.
class SpreadsheetFileTest {
    private static Workbook fixture(String name, Workbook.Format format) throws IOException {
        InputStream in = SpreadsheetFileTest.class.getResourceAsStream("/spreadsheet-fixtures/" + name);
        assertNotNull(in, name);
        try {
            return Workbook.read(in, format);
        } finally {
            in.close();
        }
    }

    private static boolean same(Object a, Object b) {
        if (a instanceof Date) {
            a = Double.valueOf(((Date) a).getTime() / 86400000d);
        }
        if (b instanceof Date) {
            b = Double.valueOf(((Date) b).getTime() / 86400000d);
        }
        if (a instanceof Double && b instanceof Double) {
            double x = ((Double) a).doubleValue();
            double y = ((Double) b).doubleValue();
            return Math.abs(x - y) <= 1e-9 * Math.max(1, Math.max(Math.abs(x), Math.abs(y)));
        }
        return a == null ? b == null : a.equals(b);
    }

    private static String show(Object o) {
        return o == null ? "null" : o + " (" + o.getClass().getSimpleName() + ")";
    }

    /// Every formula of every sheet, calculated again and compared with what the file had.
    private static void assertRecalculates(Workbook book, String file) {
        List<Object[]> cached = new ArrayList<Object[]>();
        for (int s = 0; s < book.getSheetCount(); s++) {
            Sheet sheet = book.getSheet(s);
            for (int r = 0; r < sheet.getRowCount(); r++) {
                for (int c = 0; c < sheet.getColumnCount(); c++) {
                    if (sheet.getCellFormula(r, c) != null) {
                        // a formula that was not understood keeps what the file stored, and
                        // would agree with it without having calculated anything
                        assertTrue(sheet.cell(r, c).formula != null, file + ": " + sheet.getName() + " row " + (r + 1)
                                + " was not read: " + sheet.getCellFormula(r, c));
                        cached.add(new Object[] {sheet, Integer.valueOf(r), Integer.valueOf(c), sheet.getCellValue(r, c)});
                    }
                }
            }
        }
        assertTrue(cached.size() > 390, file + " has " + cached.size() + " formulas");
        book.recalculate();
        StringBuilder wrong = new StringBuilder();
        int count = 0;
        for (Object[] e : cached) {
            Sheet sheet = (Sheet) e[0];
            int r = ((Integer) e[1]).intValue();
            int c = ((Integer) e[2]).intValue();
            Object now = sheet.getCellValue(r, c);
            if (!same(e[3], now)) {
                count++;
                wrong.append('\n').append(sheet.getName()).append(" row ").append(r + 1).append(": =")
                        .append(sheet.getCellFormula(r, c)).append(" file ").append(show(e[3])).append(" ours ")
                        .append(show(now));
            }
        }
        assertEquals("", wrong.toString(), file + ": " + count + " of " + cached.size() + " formulas differ");
    }

    @Test
    void xlsxFormulasMatchTheValuesLibreOfficeStored() throws IOException {
        assertRecalculates(fixture("calc.xlsx", Workbook.Format.XLSX), "calc.xlsx");
    }

    @Test
    void odsFormulasMatchTheValuesLibreOfficeStored() throws IOException {
        assertRecalculates(fixture("calc.ods", Workbook.Format.ODS), "calc.ods");
    }

    private static void assertData(Workbook book, String what) {
        assertEquals(4, book.getSheetCount(), what);
        Sheet data = book.getSheet("Data");
        assertNotNull(data, what);
        assertEquals("Region", data.getCellValue("A1"), what);
        assertEquals("North", data.getCellValue("A2"), what);
        assertEquals(Double.valueOf(12), data.getCellValue("C2"), what);
        assertEquals(Double.valueOf(19.99), data.getCellValue("D3"), what);
        assertEquals(Boolean.TRUE, data.getCellValue("F2"), what);
        assertEquals(Boolean.FALSE, data.getCellValue("F3"), what);
        assertTrue(data.getValueAt(1, 4) instanceof Date, what + ": " + show(data.getValueAt(1, 4)));
        assertEquals("2024-01-15", data.getCellText(1, 4), what);
        assertEquals("19.99", data.getCellText(2, 3), what);
        assertEquals("82.0%", data.getCellText(1, 6), what);
        assertEquals(Double.valueOf(221), data.getCellValue("C15"), what);
        assertEquals("=SUM(C2:C13)", data.getCellFormula(14, 2), what);
        // no frozen panes here: a headless LibreOffice saves no view, so the fixtures have none.
        // SpreadsheetModelTest holds them through our own writer.
        assertEquals(1, data.getMergedRegionCount(), what);
        int[] merged = data.getMergedRegion(0);
        assertEquals(14, merged[0], what);
        assertEquals(0, merged[1], what);
        assertEquals(14, merged[2], what);
        assertEquals(1, merged[3], what);
        assertTrue(data.getColumnWidth(0) > 12 && data.getColumnWidth(0) < 16, what + " width " + data.getColumnWidth(0));
        assertEquals("SUM(Data!$C$2:$C$13)", book.getDefinedName("TotalUnits"), what);
        Sheet plan = book.getSheet("Year's Plan");
        assertNotNull(plan, what);
        assertEquals(Double.valueOf(60), plan.getCellValue("A2"), what);
    }

    @Test
    void xlsxDataIsTyped() throws IOException {
        assertData(fixture("calc.xlsx", Workbook.Format.XLSX), "xlsx");
    }

    @Test
    void odsDataIsTyped() throws IOException {
        assertData(fixture("calc.ods", Workbook.Format.ODS), "ods");
    }

    private static Workbook copy(Workbook book, Workbook.Format format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        book.write(out, format);
        return Workbook.read(new ByteArrayInputStream(out.toByteArray()), format);
    }

    private static void assertSameCells(Workbook a, Workbook b, String what) {
        assertEquals(a.getSheetCount(), b.getSheetCount(), what);
        StringBuilder wrong = new StringBuilder();
        for (int s = 0; s < a.getSheetCount(); s++) {
            Sheet x = a.getSheet(s);
            Sheet y = b.getSheet(s);
            assertEquals(x.getName(), y.getName(), what);
            assertEquals(x.getRowCount(), y.getRowCount(), what + " rows of " + x.getName());
            assertEquals(x.getColumnCount(), y.getColumnCount(), what + " columns of " + x.getName());
            for (int r = 0; r < x.getRowCount(); r++) {
                for (int c = 0; c < x.getColumnCount(); c++) {
                    String fx = x.getCellFormula(r, c);
                    String fy = y.getCellFormula(r, c);
                    if (!same(x.getCellValue(r, c), y.getCellValue(r, c)) || !(fx == null ? fy == null : fx.equals(fy))
                            || !x.getCellText(r, c).equals(y.getCellText(r, c))) {
                        wrong.append('\n').append(x.getName()).append(' ').append(r + 1).append(',').append(c + 1)
                                .append(": ").append(fx).append(" = ").append(show(x.getCellValue(r, c))).append(" '")
                                .append(x.getCellText(r, c)).append("' became ").append(fy).append(" = ")
                                .append(show(y.getCellValue(r, c))).append(" '").append(y.getCellText(r, c)).append('\'');
                    }
                }
            }
        }
        assertEquals("", wrong.toString(), what);
    }

    @Test
    void xlsxSurvivesWritingAndReading() throws IOException {
        Workbook book = fixture("calc.xlsx", Workbook.Format.XLSX);
        Workbook again = copy(book, Workbook.Format.XLSX);
        assertSameCells(book, again, "xlsx to xlsx");
        assertData(again, "xlsx to xlsx");
        assertRecalculates(again, "xlsx written by us");
    }

    @Test
    void odsSurvivesWritingAndReading() throws IOException {
        Workbook book = fixture("calc.ods", Workbook.Format.ODS);
        Workbook again = copy(book, Workbook.Format.ODS);
        assertSameCells(book, again, "ods to ods");
        assertData(again, "ods to ods");
        assertRecalculates(again, "ods written by us");
    }

    @Test
    void oneFormatConvertsToTheOther() throws IOException {
        Workbook book = fixture("calc.xlsx", Workbook.Format.XLSX);
        Workbook ods = copy(book, Workbook.Format.ODS);
        assertSameCells(book, ods, "xlsx to ods");
        assertData(ods, "xlsx to ods");
        Workbook back = copy(fixture("calc.ods", Workbook.Format.ODS), Workbook.Format.XLSX);
        assertData(back, "ods to xlsx");
    }

    @Test
    void theFormatIsRecognisedFromTheFirstBytes() throws IOException {
        Workbook book = fixture("calc.xlsx", Workbook.Format.XLSX);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        book.write(out, Workbook.Format.XLSX);
        assertEquals(Workbook.Format.XLSX, Workbook.Format.detect("unnamed", out.toByteArray()));
        out = new ByteArrayOutputStream();
        book.write(out, Workbook.Format.ODS);
        assertEquals(Workbook.Format.ODS, Workbook.Format.detect(null, out.toByteArray()));
        assertEquals(Workbook.Format.TSV, Workbook.Format.detect("a.tsv", "a\tb\n".getBytes("UTF-8")));
        assertEquals(Workbook.Format.CSV, Workbook.Format.detect("a.csv", "a,b\n".getBytes("UTF-8")));
    }
}
