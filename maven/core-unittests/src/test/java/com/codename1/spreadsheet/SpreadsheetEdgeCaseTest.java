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
import com.codename1.ui.table.DefaultTableModel;
import com.codename1.ui.table.GridColumn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What the files of a spreadsheet application never exercise: parts written by hand the way
/// other producers write them, the corners of the formula grammar and of the number
/// formats, and every way a sheet can be restructured. The fixtures LibreOffice saved are
/// regular; these are not.
class SpreadsheetEdgeCaseTest extends UITestBase {
    private static final String MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private Workbook book;
    private Sheet sheet;
    private StringBuilder wrong;

    @BeforeEach
    void grid() {
        book = new Workbook();
        sheet = book.addSheet("Data");
        int n = 1;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                sheet.setCellValue(r, c, Double.valueOf(n++));
            }
        }
        wrong = new StringBuilder();
    }

    @AfterEach
    void report() {
        assertEquals("", wrong.toString());
    }

    private void same(Object expected, Object actual, String what) {
        if (expected instanceof Double && actual instanceof Double
                ? Math.abs(((Double) expected).doubleValue() - ((Double) actual).doubleValue()) > 1e-9
                : expected == null ? actual != null : !expected.equals(actual)) {
            wrong.append('\n').append(what).append(" gave ").append(actual).append(", not ").append(expected);
        }
    }

    private void val(Object expected, String formula) {
        sheet.setCellFormula("Z100", formula);
        same(expected, sheet.getCellValue("Z100"), formula);
    }

    private void num(double expected, String formula) {
        val(Double.valueOf(expected), formula);
    }

    private void text(String expected, Object value, String format) {
        if (value instanceof String && ((String) value).startsWith("=")) {
            sheet.setCellFormula("Y50", (String) value);
        } else {
            sheet.setCellValue("Y50", value);
        }
        sheet.setCellFormat(49, 24, format);
        same(expected, sheet.getCellText(49, 24), value + " as " + format);
    }

    private static byte[] zip(String... namesAndParts) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bytes);
        for (int i = 0; i < namesAndParts.length; i += 2) {
            zip.putNextEntry(new ZipEntry(namesAndParts[i]));
            zip.write(namesAndParts[i + 1].getBytes("UTF-8"));
            zip.closeEntry();
        }
        zip.close();
        return bytes.toByteArray();
    }

    private static byte[] xlsx(String workbookExtras, String sheetXml, String strings) throws IOException {
        return zip("[Content_Types].xml", "<Types/>",
                "xl/worksheets/sheet1.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!-- written by hand -->\n"
                        + "<worksheet xmlns=\"" + MAIN + "\">" + sheetXml + "</worksheet>",
                "xl/_rels/workbook.xml.rels", "<Relationships>"
                        + "<Relationship Id=\"rId1\" Type=\"" + REL + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                        + "<Relationship Id=\"rId2\" Type=\"" + REL + "/sharedStrings\" Target=\"sharedStrings.xml\"/>"
                        + "</Relationships>",
                "xl/workbook.xml", "<?xml version=\"1.0\"?><!DOCTYPE workbook [<!ENTITY who \"nobody\">]>"
                        + "<x:workbook xmlns:x=\"" + MAIN + "\" xmlns:r=\"" + REL + "\">" + workbookExtras
                        + "<x:sheets><x:sheet name=\"Hand &amp; made\" sheetId=\"1\" r:id=\"rId1\"/></x:sheets></x:workbook>",
                "xl/sharedStrings.xml", "<sst xmlns=\"" + MAIN + "\">" + strings + "</sst>");
    }

    private static Workbook open(byte[] file, Workbook.Format format) throws IOException {
        return Workbook.read(new ByteArrayInputStream(file), format);
    }

    private static Workbook copy(Workbook source, Workbook.Format format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        source.write(out, format);
        return open(out.toByteArray(), format);
    }

    /// Excel writes a formula that fills a column once and has the other cells point at it.
    /// LibreOffice never does, so no fixture has one.
    @Test
    void anXlsxWrittenByHand() throws IOException {
        Workbook hand = open(xlsx("<x:definedNames><x:definedName name=\"Top\">'Hand &amp; made'!$A$1</x:definedName>"
                + "<x:definedName name=\"_xlnm.Print_Area\">'Hand &amp; made'!$A$1:$B$2</x:definedName></x:definedNames>",
                "<cols><col min=\"1\" max=\"2\" width=\"12.5\" customWidth=\"1\"/></cols><sheetData>"
                + "<row r=\"1\"><c r=\"A1\"><v>1</v></c><c r=\"B1\"><f t=\"shared\" ref=\"B1:B3\" si=\"0\">A1*$A$1+1</f><v>2</v></c>"
                + "<c r=\"C1\" t=\"s\"><v>0</v></c><c r=\"D1\" t=\"inlineStr\"><is><t>in</t><r><t>line</t></r></is></c>"
                + "<c r=\"E1\" t=\"b\"><v>1</v></c><c r=\"F1\" t=\"e\"><v>#DIV/0!</v></c>"
                + "<c r=\"G1\" t=\"str\"><f>\"a\"&amp;\"b\"</f><v>ab</v></c><c r=\"H1\" t=\"s\"><v>99</v></c>"
                + "<c r=\"I1\" t=\"s\"><v>many</v></c><c r=\"J1\"><v></v></c><c r=\"K1\" t=\"b\"><v>0</v></c></row>"
                + "<row r=\"2\"><c r=\"A2\"><v>2</v></c><c r=\"B2\"><f t=\"shared\" si=\"0\"/><v>3</v></c>"
                + "<c r=\"C2\" t=\"s\"><v>1</v></c><c r=\"D2\" t=\"d\"><v>2026-03-04T09:05:00</v></c>"
                + "<c r=\"E2\"><f t=\"shared\" si=\"7\"/><v>5</v></c></row>"
                + "<row><c><v>3</v></c><c><f t=\"shared\" si=\"0\"/></c><c t=\"s\"><v>2</v></c><c><v>7</v></c></row>"
                + "</sheetData><mergeCells count=\"1\"><mergeCell ref=\"A5:B6\"/></mergeCells>",
                "<si><t>plain</t></si><si><r><t>rich </t></r><r><rPr><b/></rPr><t xml:space=\"preserve\">text</t></r></si>"
                + "<si><t><![CDATA[a<b]]]]><![CDATA[>]]>&#65;&#x1F600;&lt;&bogus;</t></si>"), Workbook.Format.XLSX);
        Sheet s = hand.getSheet("Hand & made");
        same(Double.valueOf(1), s.getCellValue("A1"), "A1");
        same("=A1*$A$1+1", s.getCellFormula(0, 1), "the shared formula where it is written");
        same("=A2*$A$1+1", s.getCellFormula(1, 1), "the shared formula a row down");
        same("=A3*$A$1+1", s.getCellFormula(2, 1), "the shared formula two rows down");
        same(Double.valueOf(4), s.getCellValue("B3"), "a shared formula with no stored value");
        same("plain", s.getCellValue("C1"), "C1");
        same("rich text", s.getCellValue("C2"), "runs of rich text");
        same("a<b]]>A\uD83D\uDE00<&bogus;", s.getCellValue("C3"), "character data, references and an unknown entity");
        same("inline", s.getCellValue("D1"), "an inline string");
        same(Boolean.TRUE, s.getCellValue("E1"), "E1");
        same(Boolean.FALSE, s.getCellValue("K1"), "K1");
        same(CellError.DIV0, s.getCellValue("F1"), "F1");
        same("ab", s.getCellValue("G1"), "G1");
        same("", s.getCellValue("H1"), "a string index past the table");
        same(null, s.getCellValue("I1"), "a string index that is no number");
        same(null, s.getCellValue("J1"), "an empty value");
        same(Double.valueOf(5), s.getCellValue("E2"), "a shared formula that was never written keeps its value");
        same(null, s.getCellFormula(1, 4), "and has no formula");
        same(Double.valueOf(7), s.getCellValue("D3"), "a cell with no address follows the one before");
        assertTrue(s.getCellValue("D2") instanceof Date, "an ISO date cell");
        same(Double.valueOf(12.5), Double.valueOf(s.getColumnWidth(1)), "column width");
        same(Integer.valueOf(1), Integer.valueOf(s.getMergedRegionCount()), "merges");
        assertEquals("[4, 0, 5, 1]", java.util.Arrays.toString(s.getMergedRegion(0)));
        same("'Hand & made'!$A$1", hand.getDefinedName("top"), "a defined name");
        same(null, hand.getDefinedName("_xlnm.Print_Area"), "a built-in name is not a defined name");
        // and the shared formula is alive
        s.setCellValue("A1", Double.valueOf(10));
        same(Double.valueOf(31), s.getCellValue("B3"), "B3 after an edit");
    }

    @Test
    void anXlsxThatIsNotOne() {
        assertThrows(IOException.class, () -> open(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, Workbook.Format.XLSX));
        assertThrows(IOException.class, () -> open(new byte[0], Workbook.Format.ODS));
        assertThrows(IOException.class, () -> open(zip("readme.txt", "nothing"), Workbook.Format.XLSX));
        assertThrows(IOException.class, () -> open(zip("readme.txt", "nothing"), Workbook.Format.ODS));
        assertThrows(IOException.class, () -> open(zip("xl/workbook.xml", "<workbook><sheets><sheet name=\"A\""),
                Workbook.Format.XLSX));
        assertThrows(IOException.class, () -> open(zip("content.xml", "<office:document-content><office:body>"),
                Workbook.Format.ODS));
    }

    @Test
    void anOdsWrittenByHand() throws IOException {
        String ns = " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\""
                + " xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\""
                + " xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\""
                + " xmlns:of=\"urn:oasis:names:tc:opendocument:xmlns:of:1.2\"";
        Workbook hand = open(zip("mimetype", "application/vnd.oasis.opendocument.spreadsheet", "content.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><office:document-content" + ns + "><office:body><office:spreadsheet>"
                + "<table:table table:name=\"Hand\"><table:table-row>"
                + "<table:table-cell office:value-type=\"float\" office:value=\"1\"><text:p>1</text:p></table:table-cell>"
                + "<table:table-cell table:number-columns-repeated=\"3\" table:formula=\"of:=[.$A1]*2\""
                + " office:value-type=\"float\" office:value=\"2\"/>"
                + "<table:table-cell office:value-type=\"string\"><text:p>a<text:s text:c=\"2\"/>b<text:tab/>c"
                + "<text:line-break/>d<text:span>e</text:span></text:p><text:p>second</text:p></table:table-cell>"
                + "<table:table-cell office:value-type=\"boolean\" office:boolean-value=\"true\"/>"
                + "<table:table-cell office:value-type=\"percentage\" office:value=\"0.25\"/>"
                + "<table:table-cell office:value-type=\"currency\" office:value=\"9.5\" office:currency=\"EUR\"/>"
                + "<table:table-cell office:value-type=\"date\" office:date-value=\"2026-03-04T09:05:00\"/>"
                + "<table:table-cell office:value-type=\"time\" office:time-value=\"PT36H30M15S\"/>"
                + "<table:table-cell office:value-type=\"date\" office:date-value=\"2026-03-04\"/>"
                + "<table:table-cell office:value-type=\"float\" office:value=\"oops\"/>"
                + "</table:table-row>"
                + "<table:table-row table:number-rows-repeated=\"2\">"
                + "<table:table-cell office:value-type=\"float\" office:value=\"5\"/>"
                + "<table:table-cell table:formula=\"of:=[.A2]+1\"/></table:table-row>"
                + "<table:table-row table:number-rows-repeated=\"1048000\">"
                + "<table:table-cell table:number-columns-repeated=\"16384\"/></table:table-row>"
                + "</table:table></office:spreadsheet></office:body></office:document-content>"), Workbook.Format.ODS);
        Sheet s = hand.getSheet("Hand");
        same(Integer.valueOf(3), Integer.valueOf(s.getRowCount()), "rows: the empty million is not kept");
        same("=$A1*2", s.getCellFormula(0, 1), "B1");
        same("=$A1*2", s.getCellFormula(0, 3), "the third repeat of a formula");
        same(Double.valueOf(2), s.getCellValue("D1"), "D1");
        same("a  b\tc\nde\nsecond", s.getCellValue("E1"), "spaces, a tab, a break and two paragraphs");
        same(Boolean.TRUE, s.getCellValue("F1"), "F1");
        same(Double.valueOf(0.25), s.getCellValue("G1"), "G1");
        same(Double.valueOf(9.5), s.getCellValue("H1"), "H1");
        assertTrue(s.getCellValue("I1") instanceof Date, "a date and time");
        same(Double.valueOf(36.5 / 24 + 15 / 86400d), s.getCellValue("J1"), "a duration");
        assertTrue(s.getCellValue("K1") instanceof Date, "a date");
        same(Double.valueOf(6), s.getCellValue("B2"), "B2");
        same("=A3+1", s.getCellFormula(2, 1), "a formula in a repeated row moves with the row");
        same(Double.valueOf(6), s.getCellValue("B3"), "B3");
    }

    @Test
    void theCornersOfTheGrammar() {
        num(14, "=SUM((A1:A2,C1:C2))");
        num(7, "=SUM(A1:B2 B1:C2)");
        val(CellError.NULL, "=SUM(A1:A2 C1:C2)");
        num(12, "=SUM(A:A)");
        num(6, "=SUM(1:1)");
        num(27, "=SUM($A:$B)");
        num(24, "=SUM($2:$3)-SUM(C2:C3)");
        num(45, "=SUM(Data!A1:C3)");
        num(45, "=SUM('Data'!$A$1:$C$3)");
        num(12, "=SUM(A1:INDEX(A1:A3,3))");
        val(CellError.NA, "=#N/A");
        val(CellError.DIV0, "=#DIV/0!");
        val(CellError.NUM, "=#NUM!+1");
        val(CellError.VALUE, "=-#VALUE!");
        val(CellError.REF, "=#REF!&\"x\"");
        val(CellError.NAME, "=SUM(#NAME?)");
        val(CellError.NULL, "=IF(TRUE,#NULL!)");
        val(CellError.NAME, "=NOSUCHFUNCTION(1)");
        val(CellError.NAME, "=nosuchname");
        num(1, "=IFERROR(#REF!,1)");
        val(Boolean.FALSE, "=FALSE");
        val(Boolean.TRUE, "=true");
        val(Boolean.TRUE, "=TRUE()");
        num(1000, "=1E+3");
        num(0.15, "=1.5e-1");
        num(0.5, "=.5");
        val("a\"b", "=\"a\"\"b\"");
        num(10, "=SUM({1,2;3,4})");
        val("x", "=INDEX({TRUE,\"x\",#N/A},1,2)");
        val(Boolean.TRUE, "=INDEX({TRUE,\"x\",#N/A},1,1)");
        val(CellError.NA, "=INDEX({TRUE,\"x\",#N/A},1,3)");
        num(-3, "=SUM({-1,-2})");
        num(0.5, "=50%");
        num(0.005, "=50%%");
        num(0.5, "=2^-1");
        num(4, "=-2^2");
        num(5, "=+5");
        num(-5, "=--+-5");
        num(64, "=2^3^2/1");
        num(64, "=(2^3)^2");
        num(7, "=1+2*3");
        val(Boolean.TRUE, "=1<=2");
        val(Boolean.FALSE, "=1<>1");
        val(Boolean.TRUE, "=\"a\"<\"B\"");
        val(Boolean.TRUE, "=2>=2");
        val(Boolean.TRUE, "=\"1\"<>1");
        val(Boolean.TRUE, "=TRUE>\"z\"");
        val(Boolean.TRUE, "=\"a\">99");
        val("12", "=1&2");
        num(3, "= 1 + 2 ");
        num(3, "=SUM( 1 , 2 )");
        Sheet one = book.addSheet("My 1");
        Sheet two = book.addSheet("My 2");
        Sheet three = book.addSheet("It's 3");
        one.setCellValue("A1", Double.valueOf(1));
        two.setCellValue("A1", Double.valueOf(2));
        three.setCellValue("A1", Double.valueOf(3));
        num(6, "=SUM('My 1:It''s 3'!A1)");
        num(3, "=SUM('My 1:My 2'!A1:B2)");
        num(3, "='It''s 3'!A1");
        val(CellError.REF, "=Nowhere!A1");
        for (String bad : new String[] {"=1+", "=(1", "=SUM(1,", "=\"abc", "={1,2", "='Data", "=#BAD!", "=1 2", "=)",
            "=A1:", "=SUM(1;2)", "={1,{2}}", "=1e", "=Data!", "=", "=*2"}) {
            try {
                sheet.setCellFormula("Z99", bad);
                wrong.append('\n').append(bad).append(" was accepted as ").append(sheet.getCellFormula(98, 25));
            } catch (IllegalArgumentException expected) {
                // what a formula nobody can read answers
            }
        }
        // typed into a grid, the same text stays what was typed
        sheet.setValueAt(0, 5, "=1+");
        same("=1+", sheet.getValueAt(0, 5), "an unreadable formula typed into a cell");
        same(null, sheet.getFormulaAt(0, 5), "and it is no formula");
        sheet.setCellFormula("F2", "=sum( a1 , $b$1 )+'Data'!C1");
        same("=sum( a1 , $b$1 )+'Data'!C1", sheet.getCellFormula(1, 5), "a formula is kept the way it was typed");
        sheet.setCellFormula("F3", "=IF(A1>0,\"y\"&\"\"\"\",{1,2;3,4})+-1%");
        same("=IF(A1>0,\"y\"&\"\"\"\",{1,2;3,4})+-1%", sheet.getCellFormula(2, 5), "strings, arrays and percent");
        sheet.setCellFormula("F4", "=SUM('My 1:It''s 3'!A1,(A1:A2,B1) A1:A1,TRUE,#N/A)");
        same("=SUM('My 1:It''s 3'!A1,(A1:A2,B1) A1:A1,TRUE,#N/A)", sheet.getCellFormula(3, 5), "sheets, unions and errors");
    }

    @Test
    void numberFormats() {
        text("50", Double.valueOf(50), "[<=100]0;[>100]0.0");
        text("150.0", Double.valueOf(150), "[<=100]0;[>100]0.0");
        text("one", Double.valueOf(1), "[=1]\"one\";[<>2]\"other\";0");
        text("other", Double.valueOf(3), "[=1]\"one\";[<>2]\"other\";0");
        text("2", Double.valueOf(2), "[=1]\"one\";[<>2]\"other\";0");
        text("low", Double.valueOf(-3), "[<0]\"low\";[>=10]\"high\";\"mid\"");
        text("high", Double.valueOf(10), "[<0]\"low\";[>=10]\"high\";\"mid\"");
        text("mid", Double.valueOf(5), "[<0]\"low\";[>=10]\"high\";\"mid\"");
        text("2 1/2", Double.valueOf(2.5), "# ?/?");
        text("2 3/8", Double.valueOf(2.375), "# ?/8");
        text("5/2", Double.valueOf(2.5), "?/?");
        text("-2 1/4", Double.valueOf(-2.25), "# ?/?");
        text("3", Double.valueOf(3), "#");
        text("1.23E+04", Double.valueOf(12345), "0.00E+00");
        text("1.20E-04", Double.valueOf(0.00012), "0.00E+00");
        text("-1.2E+3", Double.valueOf(-1234), "0.0E+0");
        text("12:00:00.500", Double.valueOf(0.5 + 0.5 / 86400), "hh:mm:ss.000");
        text("6:00 PM", Double.valueOf(0.75), "h:mm AM/PM");
        text("12:30 AM", Double.valueOf(0.5 / 24), "h:mm AM/PM");
        text("6:00 p", Double.valueOf(0.75), "h:mm a/p");
        text("M", "=DATE(2026,3,4)", "mmmmm");
        text("March", "=DATE(2026,3,4)", "mmmm");
        text("Mar", "=DATE(2026,3,4)", "mmm");
        text("Wed", "=DATE(2026,3,4)", "ddd");
        text("Wednesday 4", "=DATE(2026,3,4)", "dddd d");
        text("26-3-04", "=DATE(2026,3,4)", "yy-m-dd");
        text("2026-03-04 09:05", "=DATE(2026,3,4)+TIME(9,5,0)", "yyyy-mm-dd hh:mm");
        text("09:05:07", "=TIME(9,5,7)", "hh:mm:ss");
        text("30:00", Double.valueOf(1 / 48d), "mm:ss");
        text("36:00", Double.valueOf(1.5), "[h]:mm");
        text("90:00", Double.valueOf(1.5 / 24), "[mm]:ss");
        text("90", Double.valueOf(90 / 86400d), "[s]");
        text("12", Double.valueOf(12345678), "0,,");
        text("1,235", Double.valueOf(1234567), "#,##0,");
        text("EUR 1,234", Double.valueOf(1234), "[$EUR-407] #,##0");
        text(" 5 ", Double.valueOf(5), "_(0_)");
        text("<x>", "x", "0;-0;0;\"<\"@\">\"");
        text("x", "x", "0.00");
        text("12.5%", Double.valueOf(0.125), "0.0%");
        text("-1.50", Double.valueOf(-1.5), "0.00");
        text("(5)", Double.valueOf(-5), "0;(0)");
        text("zero", Double.valueOf(0), "0;-0;\"zero\"");
        text("5 units", Double.valueOf(5), "0 \"units\"");
        text("5x", Double.valueOf(5), "0\\x");
        text("007", Double.valueOf(7), "000");
        text("1,234,567.9", Double.valueOf(1234567.89), "#,##0.0");
        text(".5", Double.valueOf(0.5), "#.#");
        text("1.5", Double.valueOf(1.5), "0.0##");
        text("1.235", Double.valueOf(1.23456), "0.0##");
        text("TRUE", Boolean.TRUE, "0.00");
        text("#DIV/0!", "=1/0", "0.00");
        text("1E+20", Double.valueOf(1e20), "General");
        text("0.000000123", Double.valueOf(1.23e-7), "General");
        text("1.23E-12", Double.valueOf(1.23e-12), "General");
        text("123456789012", Double.valueOf(123456789012d), "General");
        text("0.3", "=0.1+0.2", "General");
        text("-0.5", Double.valueOf(-0.5), "General");
        text("1234.5678", Double.valueOf(1234.5678), "General");
        text("1.23456789012346E+15", Double.valueOf(1234567890123456d), "General");
        text("0.0000123456", Double.valueOf(0.0000123456), "General");
        text("", null, "0.00");
        same("0.00", sheet.getCellFormat(49, 24), "the format of a cell");
        same("General", sheet.getCellFormat(200, 200), "the format of a cell that is not there");
    }

    @Test
    void formatsSurviveBothFiles() throws IOException {
        String[] codes = {"0.00E+00", "# ?/?", "?/?", "# ?/8", "0.0,,", "[h]:mm:ss", "h:mm AM/PM", "#,##0.00;[Red]-#,##0.00", "0.0%",
            "yyyy-mm-dd hh:mm:ss", "@", "#,##0,", "dd/mm/yyyy", "\"EUR\" 0.00", "mmmm d, yyyy"};
        double[] values = {12345.678, 2.5, 2.5, 2.375, 12345678, 1.52100694, 0.75, -1234.5, 0.125, 46085.378472, 5, 1234567, 46085, 9.5, 46085};
        Sheet s = book.addSheet("Formats");
        for (int i = 0; i < codes.length; i++) {
            s.setCellValue(i, 0, Double.valueOf(values[i]));
            s.setCellFormat(i, 0, codes[i]);
        }
        s.setCellValue(codes.length, 0, "text & <markup> \"quoted\" 'single'\ttab\nline\u0001");
        for (Workbook.Format format : new Workbook.Format[] {Workbook.Format.XLSX, Workbook.Format.ODS}) {
            Sheet back = copy(book, format).getSheet("Formats");
            for (int i = 0; i < codes.length; i++) {
                same(s.getCellText(i, 0), back.getCellText(i, 0), codes[i] + " through " + format);
            }
            // an xlsx spells a character no XML file can hold as an escape; an ods has no way to
            same("text & <markup> \"quoted\" 'single'\ttab\nline" + (format == Workbook.Format.XLSX ? "\u0001" : ""),
                    back.getCellValue(codes.length, 0), "text through " + format);
        }
    }

    @Test
    void restructuringMovesEverythingThatHasAPlace() {
        Sheet other = book.addSheet("Other");
        other.setCellFormula("A1", "=SUM(Data!A1:A3)+Data!$B$2");
        sheet.setCellFormula("E1", "=SUM(A1:A3)+$B$2+B3");
        book.setDefinedName("Column", "Data!$A$1:$A$3");
        sheet.setCellFormula("E2", "=SUM(Column)");
        sheet.addMergedRegion(0, 0, 0, 1);
        sheet.addMergedRegion(2, 0, 3, 2);
        sheet.setColumnWidth(1, 20);
        sheet.setRowOutlineLevel(2, 3);
        sheet.insertRows(1, 2);
        same("=SUM(A1:A5)+$B$4+B5", sheet.getCellFormula(0, 4), "a formula after two rows went in");
        same("=SUM(Data!A1:A5)+Data!$B$4", other.getCellFormula(0, 0), "a formula of another sheet");
        same("Data!$A$1:$A$5", book.getDefinedName("Column"), "a defined name");
        same(Double.valueOf(12), sheet.getCellValue("E4"), "the formula that moved down still adds the column");
        same(Double.valueOf(12 + 5 + 8), sheet.getCellValue("E1"), "E1");
        assertEquals("[0, 0, 0, 1]", java.util.Arrays.toString(sheet.getMergedRegion(0)));
        assertEquals("[4, 0, 5, 2]", java.util.Arrays.toString(sheet.getMergedRegion(1)));
        same(Integer.valueOf(3), Integer.valueOf(sheet.getRowOutlineLevel(4)), "the outline level moved with its row");
        same(Integer.valueOf(0), Integer.valueOf(sheet.getRowOutlineLevel(2)), "and left the row it was on");
        sheet.removeRows(1, 2);
        same("=SUM(A1:A3)+$B$2+B3", sheet.getCellFormula(0, 4), "and after they came out again");
        same("Data!$A$1:$A$3", book.getDefinedName("Column"), "the name again");
        assertEquals("[2, 0, 3, 2]", java.util.Arrays.toString(sheet.getMergedRegion(1)));
        same(Integer.valueOf(3), Integer.valueOf(sheet.getRowOutlineLevel(2)), "the outline level came back");

        sheet.insertColumns(1, 1);
        same("=SUM(A1:A3)+$C$2+C3", sheet.getCellFormula(0, 5), "a formula after a column went in");
        same(Double.valueOf(20), Double.valueOf(sheet.getColumnWidth(2)), "the width moved with its column");
        assertEquals("[0, 0, 0, 2]", java.util.Arrays.toString(sheet.getMergedRegion(0)));
        assertEquals("[2, 0, 3, 3]", java.util.Arrays.toString(sheet.getMergedRegion(1)));
        sheet.removeColumns(1, 1);
        same("=SUM(A1:A3)+$B$2+B3", sheet.getCellFormula(0, 4), "and after it came out");
        same(Double.valueOf(20), Double.valueOf(sheet.getColumnWidth(1)), "the width came back");

        // rows that held everything a reference named leave nothing to refer to
        sheet.removeRows(0, 3);
        same(CellError.REF, other.getCellValue("A1"), "a reference to rows that are gone");
        same("=SUM(#REF!)+#REF!", other.getCellFormula(0, 0), "and what the formula says now");
        same(Integer.valueOf(1), Integer.valueOf(sheet.getMergedRegionCount()), "a merge wholly inside the rows went with them");
        sheet.removeColumns(0, 3);
        same(Integer.valueOf(0), Integer.valueOf(sheet.getMergedRegionCount()), "and the other with its columns");
    }

    @Test
    void columnsAreReadFromWhatTheyHold() {
        Sheet s = book.addSheet("Kinds");
        s.addRow("When", "Done", "Mixed", "Empty", "Clock", "Count");
        for (int r = 1; r < 4; r++) {
            s.setCellValue(r, 0, new Date(1700000000000L + r * 86400000L));
            s.setCellValue(r, 1, r % 2 == 0 ? Boolean.TRUE : Boolean.FALSE);
            s.setCellValue(r, 2, r == 1 ? (Object) "text" : (Object) Double.valueOf(r));
            s.setCellValue(r, 4, Double.valueOf(r / 24d));
            s.setCellFormat(r, 4, "h:mm");
            s.setCellValue(r, 5, Integer.valueOf(r));
        }
        s.setHeaderRow(true);
        s.setColumnWidth(0, 14);
        GridColumn[] columns = s.createColumns();
        assertEquals(6, columns.length);
        same(GridColumn.Kind.DATE, columns[0].getKind(), "a column of dates");
        same(GridColumn.Kind.BOOLEAN, columns[1].getKind(), "a column of truth values");
        same(GridColumn.Kind.AUTO, columns[2].getKind(), "a column of text and numbers");
        same(GridColumn.Kind.AUTO, columns[3].getKind(), "a column of nothing");
        same(GridColumn.Kind.NUMBER, columns[4].getKind(), "a column of times of day");
        same(GridColumn.Kind.NUMBER, columns[5].getKind(), "a column of integers");
        same("When", columns[0].getTitle(), "the header row names the column");
        assertTrue(columns[0].getWidthMM() > 0, "a width of the file is a width of the column");
        same(Double.valueOf(2), s.getValueAt(1, 5), "an Integer is stored as the number it is");
        same(Integer.valueOf(3), Integer.valueOf(s.getRowCount()), "the header is not a row");
        s.setHeaderRow(false);
        same("A", s.getColumnName(0), "without a header row a column has its letter");
        same("F", s.getColumnName(5), "F");
        same(Integer.valueOf(4), Integer.valueOf(s.getRowCount()), "and the first row is a row");
    }

    @Test
    void theRestOfTheModel() throws IOException {
        same(book, sheet.getWorkbook(), "a sheet knows its workbook");
        assertThrows(IllegalArgumentException.class, () -> book.addSheet(""));
        assertThrows(IllegalArgumentException.class, () -> book.addSheet(null));
        assertThrows(IllegalArgumentException.class, () -> book.addSheet("data"));
        assertThrows(IllegalArgumentException.class, () -> sheet.getCellValue("1A"));
        assertThrows(IllegalArgumentException.class, () -> sheet.getCellValue("A0"));
        assertThrows(IllegalArgumentException.class, () -> sheet.getCellValue("A99999999999"));
        assertThrows(IllegalArgumentException.class, () -> sheet.getCellValue(""));
        assertThrows(IllegalArgumentException.class, () -> sheet.setCellValue(-1, 0, "x"));
        assertThrows(IllegalArgumentException.class, () -> book.setDefinedName("Bad", "=1+"));
        same(Double.valueOf(1), sheet.getCellValue("a1"), "an address in lower case");

        book.setDefinedName("Rate", "0.2");
        book.setDefinedName("First", "Data!$A$1");
        sheet.setCellFormula("E1", "=First*Rate");
        same(Double.valueOf(0.2), sheet.getCellValue("E1"), "names in a formula");
        book.setDefinedName("rate", "0.5");
        same(Double.valueOf(0.5), sheet.getCellValue("E1"), "a name defined again, in another case");
        assertEquals(2, book.getDefinedNames().length);
        book.setDefinedName("Rate", null);
        same(CellError.NAME, sheet.getCellValue("E1"), "a name that was removed");
        assertEquals(1, book.getDefinedNames().length);
        assertNull(book.getDefinedName("Rate"));

        Sheet gone = book.addSheet("Gone");
        gone.setCellValue("A1", Double.valueOf(5));
        sheet.setCellFormula("E2", "=Gone!A1+1");
        same(Double.valueOf(6), sheet.getCellValue("E2"), "a cell of another sheet");
        book.removeSheet(1);
        assertEquals(1, book.getSheetCount());
        same(CellError.REF, sheet.getCellValue("E2"), "a cell of a sheet that was removed");

        // values that are not a spreadsheet's own
        sheet.setCellValue("G1", Long.valueOf(7));
        sheet.setCellValue("G2", new StringBuilder("built"));
        sheet.setCellValue("G3", Float.valueOf(0.5f));
        same(Double.valueOf(7), sheet.getCellValue("G1"), "a Long");
        same("built", sheet.getCellValue("G2"), "anything else is its text");
        same(Double.valueOf(0.5), sheet.getCellValue("G3"), "a Float");
        sheet.setValueAt(0, 7, "  12.5 ");
        sheet.setValueAt(1, 7, "true");
        sheet.setValueAt(2, 7, "'=not a formula");
        sheet.setValueAt(3, 7, "");
        sheet.setValueAt(4, 7, "1e3");
        sheet.setValueAt(5, 7, "12%");
        same(Double.valueOf(12.5), sheet.getValueAt(0, 7), "a number typed into a cell");
        same(Boolean.TRUE, sheet.getValueAt(1, 7), "a truth value typed into a cell");
        same(null, sheet.getValueAt(3, 7), "nothing typed into a cell");
        same(Double.valueOf(1000), sheet.getValueAt(4, 7), "a number with an exponent");
        same(Double.valueOf(0.12), sheet.getValueAt(5, 7), "a percentage");
        assertTrue(sheet.isCellEditable(0, 0));
        sheet.setEditable(false);
        assertTrue(!sheet.isCellEditable(0, 0));

        DefaultTableModel model = new DefaultTableModel(new String[] {"Name", "Qty"},
                new Object[][] {{"bolt", Double.valueOf(4)}, {"nut", Double.valueOf(6)}});
        Workbook from = Workbook.fromModel(model);
        Sheet first = from.getSheet(0);
        same("Name", first.getColumnName(0), "the names of a model are the header row");
        same(Integer.valueOf(2), Integer.valueOf(first.getRowCount()), "rows of a model");
        same(Double.valueOf(6), first.getValueAt(1, 1), "a value of a model");
        same(Integer.valueOf(1), Integer.valueOf(first.getFrozenRows()), "the header is frozen");
        ByteArrayOutputStream csv = new ByteArrayOutputStream();
        from.write(csv, Workbook.Format.CSV);
        assertEquals("Name,Qty\r\nbolt,4\r\nnut,6\r\n", new String(csv.toByteArray(), "UTF-8"));
        ByteArrayOutputStream tsv = new ByteArrayOutputStream();
        from.write(tsv, Workbook.Format.TSV);
        assertEquals("Name\tQty\r\nbolt\t4\r\nnut\t6\r\n", new String(tsv.toByteArray(), "UTF-8"));
    }

    private static Workbook guide(String name, Workbook.Format format) throws IOException {
        File file = new File("../../docs/demos/common/src/main/resources/" + name);
        assertTrue(file.isFile(), file + " is the workbook the developer guide opens");
        InputStream in = new FileInputStream(file);
        try {
            return Workbook.read(in, format);
        } finally {
            in.close();
        }
    }

    private static String row(Sheet s, int row) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < s.getColumnCount(); c++) {
            sb.append(c == 0 ? "" : "|").append(s.getCellText(row, c));
        }
        return sb.toString();
    }

    /// The developer guide prints these workbooks before and after an edit. What its
    /// figures show is held here, so that a figure cannot go on showing a number the code no
    /// longer calculates.
    @Test
    void theWorkbooksOfTheDeveloperGuide() throws IOException {
        Workbook sales = guide("regional-sales.xlsx", Workbook.Format.XLSX);
        Sheet summary = sales.getSheet("Summary");
        assertEquals("North|3|329|34,114.92|28.2%|1,364.60|57%|Behind", row(summary, 1));
        assertEquals("West|2|146|17,617.10|14.6%|792.77|70%|Behind", row(summary, 4));
        assertEquals("All regions|10|823|120,826.52|100.0%|4,972.13|76%|South leads", row(summary, 5));
        // every value in the file is LibreOffice's; calculating them all again changes none
        sales.recalculate();
        assertEquals("All regions|10|823|120,826.52|100.0%|4,972.13|76%|South leads", row(summary, 5));
        sales.getSheet("Orders").setCellValue("E6", Double.valueOf(40));
        assertEquals("West|2|180|60,117.10|36.8%|2,705.27|240%|Ahead", row(summary, 4));
        assertEquals("North|3|329|34,114.92|20.9%|1,364.60|57%|Behind", row(summary, 1));
        assertEquals("All regions|10|857|163,326.52|100.0%|6,884.63|102%|West leads", row(summary, 5));
        assertEquals("1045|23 Feb 2026|West|Compressor|40|1,250.00|0.0%|50,000.00", row(sales.getSheet("Orders"), 5));

        Workbook loan = guide("loan-schedule.ods", Workbook.Format.ODS);
        Sheet schedule = loan.getSheet("Schedule");
        assertEquals("1|1 Feb 2026|1,556.65|103.50|1,453.15|16,546.85|8.1%", row(schedule, 1));
        assertEquals("12|1 Jan 2027|1,556.65|8.90|1,547.75|0.00|100.0%", row(schedule, 12));
        assertEquals("Terms!$B$2", loan.getDefinedName("Rate"));
        loan.recalculate();
        assertEquals("12|1 Jan 2027|1,556.65|8.90|1,547.75|0.00|100.0%", row(schedule, 12));
        loan.getSheet("Terms").setCellValue("B2", Double.valueOf(0.089));
        assertEquals("1|1 Feb 2026|1,573.29|133.50|1,439.79|16,560.21|8.0%", row(schedule, 1));
        assertEquals("12|1 Jan 2027|1,573.29|11.58|1,561.71|0.00|100.0%", row(schedule, 12));
    }

    @Test
    void aCircleIsACircleWhicheverCellIsReadFirst() {
        // COUNT does not mind an error, so without care this is 1 beside an error when A1 is
        // read first and 2 beside 1 when B1 is
        for (int first = 0; first < 2; first++) {
            Workbook w = new Workbook();
            Sheet s = w.addSheet("S");
            s.setCellFormula("A1", "=COUNT(B1)+1");
            s.setCellFormula("B1", "=A1+1");
            s.setCellFormula("C1", "=IFERROR(B1,0)");
            Object one = s.getCellValue(first == 0 ? "A1" : "B1");
            Object two = s.getCellValue(first == 0 ? "B1" : "A1");
            assertEquals(CellError.CIRCULAR, one, "read first, order " + first);
            assertEquals(CellError.CIRCULAR, two, "read second, order " + first);
            // a cell outside the circle may do with the error what it likes
            assertEquals(Double.valueOf(0), s.getCellValue("C1"));
            // and the circle is over when it is broken
            s.setCellValue("B1", Double.valueOf(5));
            assertEquals(Double.valueOf(2), s.getCellValue("A1"));
        }
        Workbook w = new Workbook();
        Sheet s = w.addSheet("S");
        for (int r = 0; r < 4; r++) {
            s.setCellValue(r, 0, Double.valueOf(r + 1));
        }
        String[] own = {"=COUNT(A1:A5)", "=COUNTA(A:A)", "=SUMIF(A1:A5,\">2\")", "=IFERROR(A5+1,0)", "=A5"};
        for (String formula : own) {
            s.setCellFormula("A5", formula);
            assertEquals(CellError.CIRCULAR, s.getCellValue("A5"), formula);
        }
        // three cells round, entered at each of them
        for (int start = 0; start < 3; start++) {
            Workbook ring = new Workbook();
            Sheet r = ring.addSheet("S");
            r.setCellFormula("A1", "=N(B1)+1");
            r.setCellFormula("B1", "=IFERROR(C1,7)");
            r.setCellFormula("C1", "=ISERROR(A1)+0");
            r.getCellValue(0, start);
            assertEquals(CellError.CIRCULAR, r.getCellValue("A1"), "from " + start);
            assertEquals(CellError.CIRCULAR, r.getCellValue("B1"), "from " + start);
            assertEquals(CellError.CIRCULAR, r.getCellValue("C1"), "from " + start);
        }
    }

    @Test
    void whatTheSecondReviewFound() throws IOException {
        // one cell on several sheets, through both files
        Workbook w = new Workbook();
        String[] months = {"Jan", "Feb", "Mar"};
        for (int i = 0; i < 3; i++) {
            w.addSheet(months[i]).setCellValue("B2", Double.valueOf(1000 * (i + 1)));
        }
        Sheet sum = w.addSheet("Year");
        sum.setCellFormula("A1", "=SUM(Jan:Mar!B2)");
        sum.setCellFormula("A2", "=SUM(Jan:Mar!B2:B3)");
        for (Workbook.Format format : new Workbook.Format[] {Workbook.Format.ODS, Workbook.Format.XLSX}) {
            Workbook back = copy(w, format);
            Sheet year = back.getSheet("Year");
            assertEquals("=SUM(Jan:Mar!B2)", year.getCellFormula(0, 0), format.toString());
            assertEquals("=SUM(Jan:Mar!B2:B3)", year.getCellFormula(1, 0), format.toString());
            back.getSheet("Feb").setCellValue("B2", Double.valueOf(1));
            assertEquals(Double.valueOf(4001), year.getCellValue("A1"), format.toString());
        }

        // MATCH in a sorted list is the last of the equal entries, as the lookups have it,
        // and a number larger than any is the place of the last number
        double[] list = {3, 1, 4, 1, 5, 2};
        for (int i = 0; i < list.length; i++) {
            sheet.setCellValue(i, 0, Double.valueOf(list[i]));
            sheet.setCellValue(i, 1, String.valueOf((char) ('a' + i)));
        }
        num(6, "=MATCH(1E+99,A1:A6)");
        val("f", "=INDEX(B1:B6,MATCH(1E+99,A1:A6))");
        val("f", "=VLOOKUP(1E+99,A1:B6,2)");
        num(5, "=MATCH(5,{1,3,5,5,5,7},1)");
        num(3, "=MATCH(5,{1,3,5,5,5,7},0)");
        num(2, "=MATCH(4,{1,3,5,7},1)");
        num(2, "=MATCH(4,{7,5,3,1},-1)");
        num(3, "=MATCH(5,{9,5,5,1},-1)");
        val(CellError.NA, "=MATCH(0,{1,3,5,7},1)");

        // fifteen digits, counted from the first that is not a zero
        val("0.00012345678901234500", "=TEXT(0.000123456789012345,\"0.00000000000000000000\")");
        val("0.30000000000000000000", "=TEXT(0.1+0.2,\"0.00000000000000000000\")");
        val("0.05000000000000000000", "=TEXT(0.05,\"0.00000000000000000000\")");
        val("0.0000000000000001", "=TEXT(0.0000000000000001,\"0.0000000000000000\")");

        // unequal is the opposite of equal, for text that reads as the number too
        sheet.setCellValue("D1", "10");
        sheet.setCellValue("D2", Double.valueOf(10));
        sheet.setCellValue("D3", "ten");
        num(2, "=COUNTIF(D1:D3,\"=10\")");
        num(1, "=COUNTIF(D1:D3,\"<>10\")");

        // a range has one sheet
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                sheet.setCellFormula("E1", "=SUM(A1:'Other sheet'!B2)");
            }
        });
        Sheet other = book.addSheet("Other sheet");
        other.setCellValue("A1", Double.valueOf(2));
        other.setCellValue("B2", Double.valueOf(3));
        sheet.setCellFormula("E1", "=SUM('Other sheet'!A1:'Other sheet'!B2)");
        assertEquals(Double.valueOf(5), sheet.getCellValue("E1"));
        // a typed formula keeps the text it was typed as
        assertEquals("=SUM('Other sheet'!A1:'Other sheet'!B2)", sheet.getCellFormula(0, 4));

        // a cell is in one merge
        sheet.addMergedRegion(10, 0, 11, 1);
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                sheet.addMergedRegion(11, 1, 12, 2);
            }
        });
        sheet.addMergedRegion(12, 0, 12, 1);
        assertEquals(2, sheet.getMergedRegionCount());
    }

    @Test
    void theFormatOfATypedDateGoesWithIt() {
        Object[] next = {"=1+1", Double.valueOf(5), "7", "text"};
        Object[] expected = {Double.valueOf(2), Double.valueOf(5), Double.valueOf(7), "text"};
        String[] typed = {"=TODAY()", "=NOW()", "=DATE(2024,3,15)", "=TIME(1,2,3)"};
        for (String formula : typed) {
            for (int i = 0; i < next.length; i++) {
                Sheet s = new Workbook().addSheet("S");
                s.setValueAt(0, 0, formula);
                if (!formula.startsWith("=TIME")) {
                    assertTrue(s.getCellValue(0, 0) instanceof Date, formula);
                }
                if (next[i] instanceof String) {
                    s.setValueAt(0, 0, next[i]);
                } else {
                    s.setCellValue(0, 0, next[i]);
                }
                assertEquals(expected[i], s.getCellValue(0, 0), formula + " then " + next[i]);
                assertEquals("General", s.getCellFormat(0, 0), formula + " then " + next[i]);
            }
        }
        // cleared and written again, and a format somebody chose
        Sheet s = new Workbook().addSheet("S");
        s.setValueAt(0, 0, "=TODAY()");
        s.setCellValue(0, 0, null);
        s.setCellValue(0, 0, Double.valueOf(7));
        assertEquals(Double.valueOf(7), s.getCellValue(0, 0));
        s.setValueAt(0, 1, "=TODAY()");
        s.setCellFormat(0, 1, "dd/mm/yyyy");
        s.setCellValue(0, 1, Double.valueOf(45000));
        assertEquals("dd/mm/yyyy", s.getCellFormat(0, 1));
        assertTrue(s.getCellValue(0, 1) instanceof Date);
    }

    @Test
    void aWorkbookThatCountsFrom1904() throws IOException {
        book.setDate1904(true);
        num(2024, "=YEAR(\"2024-03-15\")");
        num(15, "=DAY(\"15-Mar-2024\")");
        val(Boolean.TRUE, "=DATEVALUE(\"2024-03-15\")=DATE(2024,3,15)");
        num(0.5, "=TIMEVALUE(\"12:00\")");
        num(12, "=HOUR(\"2024-03-15 12:30\")");
        num(1, "=DAYS(\"2024-03-16\",\"2024-03-15\")");
        // a date a file writes as text
        Workbook w = open(xlsx("<x:workbookPr date1904=\"1\"/>", "<sheetData><row r=\"1\">"
                + "<c r=\"A1\" t=\"d\"><v>2024-03-15</v></c></row></sheetData>", ""), Workbook.Format.XLSX);
        Sheet s = w.getSheet(0);
        s.setCellFormula("B1", "=YEAR(A1)");
        assertEquals(Double.valueOf(2024), s.getCellValue("B1"));
        assertEquals("2024-03-15", s.getCellText(0, 0));
    }

    @FormTest
    void aFunctionThisPackageLacksKeepsItsStoredValue() throws IOException {
        Workbook w = open(xlsx("", "<sheetData><row r=\"1\"><c r=\"A1\"><v>1</v></c>"
                + "<c r=\"B1\"><f>IMSUM(A1)</f><v>42</v></c><c r=\"C1\"><f>B1+1</f><v>43</v></c></row></sheetData>",
                ""), Workbook.Format.XLSX);
        Sheet s = w.getSheet(0);
        final int[] told = {0};
        s.addDataChangeListener(new com.codename1.ui.events.DataChangedListener() {
            public void dataChanged(int type, int index) {
                told[0]++;
            }
        });
        // nothing the formula reads changes in any of these
        s.insertRows(9, 1);
        w.setDefinedName("Rate", "=0.1");
        assertTrue(told[0] > 0, "a name that formulas may read is announced");
        w.recalculate();
        w.addSheet("More");
        w.removeSheet(w.getSheetCount() - 1);
        assertEquals(Double.valueOf(42), s.getCellValue("B1"));
        assertEquals(Double.valueOf(43), s.getCellValue("C1"));
        assertEquals("=IMSUM(A1)", s.getCellFormula(0, 1));
        // and now something does
        s.setCellValue("A1", Double.valueOf(2));
        assertEquals(CellError.NAME, s.getCellValue("B1"));
        // typed, it has no stored value to keep
        s.setCellFormula("D1", "=IMSUM(A1)");
        assertEquals(CellError.NAME, s.getCellValue("D1"));
    }

    @Test
    void aTextFileReadAndWrittenStaysAsLongAsItWas() throws IOException {
        String text = "a,b\r\nc,d\r\n";
        for (int round = 0; round < 3; round++) {
            String[][] rows = new com.codename1.io.CSVParser().parse(new java.io.StringReader(text));
            java.io.StringWriter out = new java.io.StringWriter();
            new com.codename1.io.CSVWriter().write(out, rows);
            text = out.toString();
        }
        assertEquals("a,b\r\nc,d\r\n", text);
        // an empty row in the middle is a row
        java.io.StringWriter out = new java.io.StringWriter();
        new com.codename1.io.CSVWriter().write(out, new String[][] {{"a"}, {}, {"b"}, {}});
        assertEquals("a\r\n\r\nb\r\n", out.toString());
    }
}
