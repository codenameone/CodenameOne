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
package com.codename1.wordprocessing;

import com.codename1.junit.UITestBase;
import com.codename1.spreadsheet.Sheet;
import com.codename1.spreadsheet.Workbook;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tables calculated by the spreadsheet engine: Word formulas, recalculation, and a live binding
/// between a document table and a sheet.
class TableSheetsTest extends UITestBase {
    static List<Block> cell(String id, RichText t) {
        return new ArrayList<Block>(Arrays.asList((Block) new Paragraph(id, t)));
    }

    static List<Block> cell(String id, String text) {
        return cell(id, RichText.of(text));
    }

    /// Item / Amount, three amounts, and a total row with a Word SUM(ABOVE) field.
    static Table budget() {
        List<List<List<Block>>> rows = new ArrayList<List<List<Block>>>();
        rows.add(Arrays.asList(cell("h0", "Item"), cell("h1", "Amount")));
        rows.add(Arrays.asList(cell("a0", "Rent"), cell("a1", "1,200")));
        rows.add(Arrays.asList(cell("b0", "Food"), cell("b1", "300.5")));
        rows.add(Arrays.asList(cell("c0", "Fun"), cell("c1", "50%")));
        rows.add(Arrays.asList(cell("t0", "Total"),
                cell("t1", RichText.of(new Field(" = SUM(ABOVE) \\# \"0.00\"", "0", false),
                        TextStyle.DEFAULT.withBold(true)))));
        return new Table("t", rows, null, 1);
    }

    static Document doc(Block... b) {
        return new Document(new ArrayList<Block>(Arrays.asList(b)), null, null, 100);
    }

    static Field totalField(Document d) {
        Table t = (Table) d.get(d.indexOf("t"));
        Paragraph p = (Paragraph) t.cell(4, 1).get(0);
        return (Field) p.text.objectAt(0);
    }

    @Test
    void translatesWordPositionsAndSwitches() {
        Table t = budget();
        assertEquals("=SUM(B2:B4)", TableSheets.excelFormula(" = SUM(ABOVE) \\# \"0.00\"", t, 4, 1));
        assertEquals("=sum(A5:A5)", TableSheets.excelFormula("=sum(left)", t, 4, 1));
        assertEquals("=AVERAGE(B3:B5)", TableSheets.excelFormula("=AVERAGE(BELOW)", t, 1, 1));
        assertEquals("=B2*2+\"LEFT\"", TableSheets.excelFormula("B2*2+\"LEFT\"", t, 4, 1));
        assertEquals("=SUM(XFD1048576)", TableSheets.excelFormula("=SUM(LEFT)", t, 1, 0));
        assertEquals("AA10", TableSheets.address(9, 26));
    }

    @Test
    void recalculatesFieldsAndKeepsTheirFormatting() {
        Document d = TableSheets.recalculate(doc(budget()));
        Field f = totalField(d);
        assertEquals("1501", f.result);
        assertEquals(new Field(" = SUM(ABOVE) \\# \"0.00\"", "", false).instruction, f.instruction);
        Paragraph p = (Paragraph) ((Table) d.get(0)).cell(4, 1).get(0);
        assertTrue(p.text.styleAt(0).bold);
        assertSame(d, TableSheets.recalculate(d));
    }

    @Test
    void typedFormulaBecomesAField() {
        List<List<List<Block>>> rows = new ArrayList<List<List<Block>>>();
        rows.add(Arrays.asList(cell("x", "2"), cell("y", "=A1*21")));
        Document d = TableSheets.recalculate(doc(new Table("t", rows, null, 0)));
        Paragraph p = (Paragraph) ((Table) d.get(0)).cell(0, 1).get(0);
        Field f = (Field) p.text.objectAt(0);
        assertEquals("=A1*21", f.instruction);
        assertEquals("42", f.result);
    }

    @Test
    void modelRecalculatesAsYouType() {
        DocumentModel m = new DocumentModel(doc(budget()));
        m.insertText(new TextPosition("b1", 0), "1", null);
        // 1200 + 1300.5 + 0.5
        assertEquals("2501", totalField(m.getDocument()).result);
        m.setAutoRecalculate(false);
        m.insertText(new TextPosition("b1", 0), "9", null);
        assertEquals("2501", totalField(m.getDocument()).result);
    }

    @Test
    void sheetRoundTrip() {
        Workbook w = new Workbook();
        Sheet s = w.addSheet("S");
        s.setCellValue(0, 0, "Name");
        s.setCellValue(1, 0, Double.valueOf(4));
        s.setCellFormula(2, 0, "A2*10");
        s.setHeaderRow(true);
        DocumentModel m = new DocumentModel();
        Table t = TableSheets.fromSheet(s, m);
        assertEquals(3, t.rows());
        assertEquals(1, t.headerRows);
        Paragraph p = (Paragraph) t.cell(2, 0).get(0);
        assertEquals("40", ((Field) p.text.objectAt(0)).result);
        Sheet back = TableSheets.toSheet(t, new Workbook(), "B");
        assertTrue(back.isHeaderRow());
        assertEquals("=A2*10", back.getCellFormula(2, 0));
        assertEquals(Double.valueOf(4), back.getCellValue(1, 0));
    }

    @Test
    void bindingFollowsBothWays() {
        DocumentModel m = new DocumentModel(doc(budget()));
        assertEquals("t", m.tableOf("b1"));
        TableBinding b = new TableBinding(m, m.tableOf("b1"));
        Sheet s = b.getSheet();
        assertNotNull(b.createGrid());
        s.setCellValue(1, 1, Double.valueOf(1000));
        flushSerialCalls();
        Table t = (Table) m.getDocument().get(0);
        assertEquals("1000", ((Paragraph) t.cell(1, 1).get(0)).text.plainText());
        assertEquals("1301", totalField(m.getDocument()).result);
        assertTrue(m.canUndo());
        m.undo();
        assertEquals(Double.valueOf(1200), s.getCellValue(1, 1));
        b.dispose();
        s.setCellValue(1, 1, Double.valueOf(7));
        flushSerialCalls();
        assertEquals("1,200", ((Paragraph) ((Table) m.getDocument().get(0)).cell(1, 1).get(0))
                .text.plainText());
        assertFalse(TableSheets.hasFormulas(TableSheetsTest.plainTable()));
    }

    static Table plainTable() {
        List<List<List<Block>>> rows = new ArrayList<List<List<Block>>>();
        rows.add(Arrays.asList(cell("q", "a")));
        return new Table("p", rows, null, 0);
    }
}
