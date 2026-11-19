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

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Files written to break whoever opens them, and the rest of what a hostile review of this
/// package found. A file an application was handed is not a file it wrote: the only thing
/// reading one may do, beside reading it, is throw `IOException`. Running out of memory or
/// of stack is not an exception an application can catch on every device.
class SpreadsheetHostileFileTest extends UITestBase {
    private static final String MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String ODS_NS = " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\""
            + " xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\""
            + " xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\"";
    private StringBuilder wrong;
    private Workbook book;
    private Sheet sheet;
    private long limit;
    private long seed = 20261118L;

    @BeforeEach
    void start() {
        wrong = new StringBuilder();
        book = new Workbook();
        sheet = book.addSheet("Data");
        limit = Workbook.getCellLimit();
    }

    @AfterEach
    void report() {
        Workbook.setCellLimit(limit);
        assertEquals("", wrong.toString());
    }

    private void same(Object expected, Object actual, String what) {
        if (expected instanceof Double && actual instanceof Double
                ? Math.abs(((Double) expected).doubleValue() - ((Double) actual).doubleValue()) > 1e-9
                : expected == null ? actual != null : !expected.equals(actual)) {
            wrong.append('\n').append(what).append(" gave ").append(actual).append(", not ").append(expected);
        }
    }

    private Object eval(String formula) {
        sheet.setCellFormula("Z100", formula);
        sheet.setCellFormat(99, 25, "General");
        return sheet.getCellValue("Z100");
    }

    private void val(Object expected, String formula) {
        same(expected, eval(formula), formula);
    }

    private int random(int bound) {
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        return (int) ((seed >>> 33) % bound);
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

    private static byte[] xlsx(String sheetData, String styles) throws IOException {
        return zip("[Content_Types].xml", "<Types/>",
                "xl/workbook.xml", "<workbook xmlns=\"" + MAIN + "\" xmlns:r=\"" + REL + "\">"
                        + "<sheets><sheet name=\"S\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>",
                "xl/_rels/workbook.xml.rels", "<Relationships>"
                        + "<Relationship Id=\"rId1\" Type=\"" + REL + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                        + "<Relationship Id=\"rId2\" Type=\"" + REL + "/styles\" Target=\"styles.xml\"/></Relationships>",
                "xl/styles.xml", styles == null ? "<styleSheet xmlns=\"" + MAIN + "\"/>" : styles,
                "xl/worksheets/sheet1.xml", "<worksheet xmlns=\"" + MAIN + "\"><sheetData>" + sheetData
                        + "</sheetData></worksheet>");
    }

    private static byte[] unzip(byte[] zip, String name) throws IOException {
        java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(new ByteArrayInputStream(zip));
        for (java.util.zip.ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
            if (name.equals(e.getName())) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] b = new byte[8192];
                for (int n = in.read(b); n > 0; n = in.read(b)) {
                    out.write(b, 0, n);
                }
                return out.toByteArray();
            }
        }
        return new byte[0];
    }

    private static byte[] ods(String rows) throws IOException {
        return zip("mimetype", "application/vnd.oasis.opendocument.spreadsheet", "content.xml",
                "<office:document-content" + ODS_NS + "><office:body><office:spreadsheet><table:table table:name=\"S\">"
                        + rows + "</table:table></office:spreadsheet></office:body></office:document-content>");
    }

    /// Reads a file that must either be read or be refused with an `IOException`.
    private Workbook survive(String what, byte[] file, Workbook.Format format) {
        try {
            Workbook read = Workbook.read(new ByteArrayInputStream(file), format);
            // what was read has to be usable too: shown, and written again
            for (int i = 0; i < read.getSheetCount(); i++) {
                Sheet s = read.getSheet(i);
                for (int r = 0; r < Math.min(s.getRowCount(), 50); r++) {
                    for (int c = 0; c < Math.min(s.getColumnCount(), 50); c++) {
                        s.getValueAt(r, c);
                        s.getCellText(r, c);
                    }
                }
            }
            read.write(new ByteArrayOutputStream(), Workbook.Format.XLSX);
            read.write(new ByteArrayOutputStream(), Workbook.Format.ODS);
            read.write(new ByteArrayOutputStream(), Workbook.Format.CSV);
            return read;
        } catch (IOException refused) {
            return null;
        } catch (Throwable t) { //NOPMD AvoidCatchingThrowable - anything but an IOException is the failure looked for
            wrong.append('\n').append(what).append(" threw ").append(t);
            return null;
        }
    }

    private void refused(String what, byte[] file, Workbook.Format format) {
        if (survive(what, file, format) != null) {
            wrong.append('\n').append(what).append(" was read");
        }
    }

    @Test
    void cellsOutsideTheSheet() throws IOException {
        refused("a row past the last", xlsx("<row r=\"2000000\"><c r=\"A2000000\"><v>1</v></c></row>", null),
                Workbook.Format.XLSX);
        refused("a column past the last", xlsx("<row r=\"1\"><c r=\"ZZZZ1\"><v>1</v></c></row>", null),
                Workbook.Format.XLSX);
        survive("an address with no column", xlsx("<row r=\"1\"><c r=\"17\"><v>1</v></c></row>", null),
                Workbook.Format.XLSX);
        survive("an address that is not one", xlsx("<row r=\"x\"><c r=\"!!\"><v>1</v></c><c r=\"\"><v>2</v></c></row>", null),
                Workbook.Format.XLSX);
        StringBuilder wide = new StringBuilder();
        for (int i = 0; i < 20000; i++) {
            wide.append(i).append(',');
        }
        refused("a line of 20,000 fields", wide.toString().getBytes("UTF-8"), Workbook.Format.CSV);
    }

    /// A few hundred bytes that ask for gigabytes.
    @Test
    void filesThatUnpackToMoreThanTheyAre() throws IOException {
        Workbook.setCellLimit(200000);
        StringBuilder rows = new StringBuilder();
        for (int r = 1; r <= 100; r++) {
            rows.append("<row r=\"").append(r).append("\"><c r=\"XFD").append(r).append("\"><v>1</v></c></row>");
        }
        refused("the last column of a hundred rows", xlsx(rows.toString(), null), Workbook.Format.XLSX);
        String cell = "<table:table-cell office:value-type=\"float\" office:value=\"1\"";
        refused("a cell repeated across a row repeated down the sheet", ods("<table:table-row"
                + " table:number-rows-repeated=\"1000000\">" + cell + " table:number-columns-repeated=\"16384\"/>"
                + "</table:table-row>"), Workbook.Format.ODS);
        refused("a cell repeated two thousand million times", ods("<table:table-row>" + cell
                + " table:number-columns-repeated=\"2000000000\"/>" + cell + "/></table:table-row>"), Workbook.Format.ODS);
        refused("rows repeated two thousand million times and then one more", ods("<table:table-row"
                + " table:number-rows-repeated=\"2000000000\"><table:table-cell/></table:table-row><table:table-row"
                + " table:number-rows-repeated=\"2000000000\"><table:table-cell/></table:table-row><table:table-row>"
                + cell + "/></table:table-row>"), Workbook.Format.ODS);
        Workbook spaces = survive("a run of two thousand million spaces", ods("<table:table-row><table:table-cell"
                + " office:value-type=\"string\"><text:p>a<text:s text:c=\"2000000000\"/>b</text:p></table:table-cell>"
                + "</table:table-row>"), Workbook.Format.ODS);
        if (spaces != null) {
            same(Boolean.TRUE, Boolean.valueOf(((String) spaces.getSheet(0).getCellValue("A1")).length() <= XmlScanner.MAX_TEXT + 2),
                    "the length of the text is bounded");
        }
        // empty rows and columns cost nothing, however many: this is how every sheet ends
        Workbook empty = survive("a million empty rows", ods("<table:table-row table:number-rows-repeated=\"1048576\">"
                + "<table:table-cell table:number-columns-repeated=\"16384\"/></table:table-row>"), Workbook.Format.ODS);
        same(Boolean.TRUE, Boolean.valueOf(empty != null), "an empty sheet of the full size is read");
        Workbook.setCellLimit(limit);
        Workbook far = survive("a value after 20,000 empty columns", ods("<table:table-row><table:table-cell"
                + " table:number-columns-repeated=\"16383\"/>" + cell + "/></table:table-row>"), Workbook.Format.ODS);
        if (far != null) {
            same(Double.valueOf(1), far.getSheet(0).getCellValue(0, 16383), "the value in the last column");
        }
    }

    @Test
    void partsThatStopInTheMiddle() throws IOException {
        refused("a sheet that ends inside a row", zip("[Content_Types].xml", "<Types/>", "xl/workbook.xml",
                "<workbook xmlns=\"" + MAIN + "\" xmlns:r=\"" + REL + "\"><sheets><sheet name=\"S\" sheetId=\"1\" r:id=\"rId1\"/>"
                + "</sheets></workbook>", "xl/_rels/workbook.xml.rels", "<Relationships><Relationship Id=\"rId1\" Target=\""
                + "worksheets/sheet1.xml\"/></Relationships>", "xl/worksheets/sheet1.xml",
                "<worksheet><sheetData><row r=\"1\"><c r=\"A1\"><v>1</v></c>"), Workbook.Format.XLSX);
        refused("an archive with no workbook", zip("readme.txt", "nothing"), Workbook.Format.XLSX);
        refused("an archive with no content", zip("readme.txt", "nothing"), Workbook.Format.ODS);
        refused("no archive at all", "not a zip".getBytes("UTF-8"), Workbook.Format.XLSX);
        refused("nothing", new byte[0], Workbook.Format.ODS);
        Workbook amp = survive("an ampersand nobody escaped", xlsx("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>"
                + "Fish & Chips</t></is></c><c r=\"B1\" t=\"inlineStr\"><is><t>R&D</t></is></c><c r=\"C1\"><v>7</v></c>"
                + "<c r=\"D1\" t=\"inlineStr\"><is><t>a&#0;b&#xD800;c&#55;</t></is></c></row>", null), Workbook.Format.XLSX);
        if (amp != null) {
            Sheet s = amp.getSheet(0);
            same("Fish & Chips", s.getCellValue("A1"), "text with a bare ampersand");
            same("R&D", s.getCellValue("B1"), "a bare ampersand before a tag");
            same(Double.valueOf(7), s.getCellValue("C1"), "the cell after it");
            same("abc7", s.getCellValue("D1"), "characters XML has none of");
        }
    }

    /// Format codes are text from the file that is run as a small program for every cell.
    @Test
    void formatCodesNobodyWouldWrite() throws IOException {
        String[] codes = {"[", "]", "[Red", "0_", "\\", "*", "_", "0*", "\"", "0\"", ";", ";;;", ";;;;;;", "@@", "0.0.0", "E+",
            "0E+", "E+0", "0E", "/", "0/", "/0", "# /", "?/?/?", "%%%", ",,,", "0,,,,,,,,,,,,", "[>", "[<=]", "[=x]0", "[$", "[$-",
            "[h", "[hh]:", "AM/PM", "A/P", "yyyyy", "mmmmmm", "ddddd", "h:m:s.000000", "0." + repeat("0", 400),
            repeat("#", 600), repeat("0", 1100), "General;General;General;General", "0;;", ";0", "[<0]0;[<0]0;[<0]0",
            ".", "-", "+", "(", ")", " ", "\u00e9", "0\\", "0_)", "*-0", "_(* #,##0.00_)"};
        Object[] values = {Double.valueOf(0), Double.valueOf(-1234.5678), Double.valueOf(45000.75), Double.valueOf(1e300),
            Double.valueOf(1e-300), Double.valueOf(-0.0), "text", Boolean.TRUE, CellError.NA, null};
        Sheet s = book.addSheet("Formats");
        int row = 0;
        for (int round = 0; round < 3000; round++) {
            String code;
            if (round < codes.length) {
                code = codes[round];
            } else {
                // what a format is made of, in no order a format has
                String alphabet = "0#?.,%E+-/;[]\"\\_*@ymdhsAMP: $<>=GeneralRed1";
                StringBuilder made = new StringBuilder();
                for (int i = random(14) + 1; i > 0; i--) {
                    made.append(alphabet.charAt(random(alphabet.length())));
                }
                code = made.toString();
            }
            for (Object value : values) {
                try {
                    CellFormat.format(code, value, round % 2 == 0);
                } catch (Throwable t) { //NOPMD AvoidCatchingThrowable - no format may throw
                    wrong.append("\nthe format ").append(code).append(" of ").append(value).append(" threw ").append(t);
                }
            }
            if (round % 7 == 0 || round < codes.length) {
                s.setCellValue(row, 0, Double.valueOf(1234.5));
                try {
                    s.setCellFormat(row, 0, code);
                } catch (Throwable t) { //NOPMD AvoidCatchingThrowable - no format may throw
                    wrong.append("\nsetting the format ").append(code).append(" threw ").append(t);
                }
                row++;
            }
        }
        for (Workbook.Format format : new Workbook.Format[] {Workbook.Format.XLSX, Workbook.Format.ODS, Workbook.Format.CSV}) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                book.write(out, format);
                Workbook back = Workbook.read(new ByteArrayInputStream(out.toByteArray()), format);
                same(Double.valueOf(1234.5), format == Workbook.Format.CSV ? Double.valueOf(1234.5)
                        : back.getSheet("Formats").getCellValue(0, 0), "the value under a format through " + format);
            } catch (Throwable t) { //NOPMD AvoidCatchingThrowable - no format may throw
                wrong.append("\nwriting the formats as ").append(format).append(" threw ").append(t);
            }
        }
        // and a format a file brings
        survive("a file with a format that opens a bracket", xlsx("<row r=\"1\"><c r=\"A1\" s=\"1\"><v>5</v></c>"
                + "<c r=\"B1\" s=\"2\"><v>5</v></c><c r=\"C1\" s=\"99\"><v>5</v></c></row>", "<styleSheet xmlns=\"" + MAIN + "\"><numFmts>"
                + "<numFmt numFmtId=\"164\" formatCode=\"[\"/><numFmt numFmtId=\"165\" formatCode=\"0_\"/></numFmts><cellXfs>"
                + "<xf numFmtId=\"0\"/><xf numFmtId=\"164\"/><xf numFmtId=\"165\"/></cellXfs></styleSheet>"), Workbook.Format.XLSX);
    }

    /// A formula nested deeper than the stack is tall.
    @Test
    void formulasNobodyWouldWrite() throws IOException {
        String[] deep = {repeat("(", 100000) + "1" + repeat(")", 100000), repeat("-", 100000) + "1",
            repeat("SUM(", 50000) + "1" + repeat(")", 50000), repeat("{", 5000), repeat("1+", 100000) + "1",
            repeat("A1:", 50000) + "A1", repeat("@", 100000) + "A1", "1" + repeat("%", 100000), repeat("IF(1,", 3000) + "1"
            + repeat(")", 3000)};
        for (String formula : deep) {
            try {
                sheet.setCellFormula(0, 0, formula);
                sheet.getCellValue(0, 0);
            } catch (IllegalArgumentException refusedAsItShouldBe) {
                continue;
            } catch (Throwable t) { //NOPMD AvoidCatchingThrowable - a stack that overflows is the failure looked for
                wrong.append("\na formula of ").append(formula.length()).append(" characters starting ")
                        .append(formula.substring(0, 6)).append(" threw ").append(t);
            }
        }
        // sixty levels are what Excel allows, and are read
        val(Double.valueOf(1), repeat("SUM(", 60) + "1" + repeat(")", 60));
        val(Double.valueOf(1), repeat("(", 60) + "1" + repeat(")", 60));
        Workbook file = survive("a file with such a formula", xlsx("<row r=\"1\"><c r=\"A1\"><f>" + deep[0] + "</f><v>4</v></c>"
                + "<c r=\"B1\"><f>" + deep[1] + "</f><v>5</v></c><c r=\"C1\"><f>A1+B1</f><v>9</v></c></row>", null),
                Workbook.Format.XLSX);
        if (file != null) {
            same(Double.valueOf(4), file.getSheet(0).getCellValue("A1"), "a formula that is not read keeps the value of the file");
        }
        String alphabet = "A1B2$:,;()+-*/^&=<>%!'\" {}#.SUMIFxREF?@[]\\e0";
        for (int round = 0; round < 20000; round++) {
            StringBuilder made = new StringBuilder();
            for (int i = random(16) + 1; i > 0; i--) {
                made.append(alphabet.charAt(random(alphabet.length())));
            }
            try {
                sheet.setCellFormula(1, 1, made.toString());
                sheet.getCellValue(1, 1);
                sheet.getCellText(1, 1);
                String kept = sheet.getCellFormula(1, 1);
                sheet.insertRows(0, 1);
                sheet.removeRows(0, 1);
                if (round % 50 == 0) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    book.write(out, round % 100 == 0 ? Workbook.Format.XLSX : Workbook.Format.ODS);
                    Workbook.read(new ByteArrayInputStream(out.toByteArray()),
                            round % 100 == 0 ? Workbook.Format.XLSX : Workbook.Format.ODS).recalculate();
                }
                same(Boolean.TRUE, Boolean.valueOf(kept != null), "a formula that was accepted is kept");
            } catch (IllegalArgumentException refusedAsItShouldBe) {
                continue;
            } catch (Throwable t) { //NOPMD AvoidCatchingThrowable - only a refusal is allowed
                wrong.append("\nthe formula ").append(made).append(" threw ").append(t);
                if (wrong.length() > 4000) {
                    break;
                }
            }
        }
    }

    /// A column that adds to the row above it, the way every ledger is written, read from
    /// the bottom: each cell needs the one above, as deep as the sheet is long.
    @Test
    void aLongChainOfFormulas() {
        int rows = 60000;
        book.silent = true;
        sheet.setCellValue(0, 0, Double.valueOf(1));
        for (int r = 1; r < rows; r++) {
            sheet.setCellFormula(r, 0, "A" + r + "+1");
        }
        book.silent = false;
        try {
            same(Double.valueOf(rows), sheet.getCellValue(rows - 1, 0), "the last cell of the chain");
            sheet.setCellValue(0, 0, Double.valueOf(2));
            same(Double.valueOf(rows + 1), sheet.getCellValue(rows - 1, 0), "the last cell after the first changed");
        } catch (Throwable t) { //NOPMD AvoidCatchingThrowable - a stack that overflows is the failure looked for
            wrong.append("\na chain of ").append(rows).append(" formulas threw ").append(t);
        }
    }

    @Test
    void mutatedParts() throws IOException {
        String sheetData = "<row r=\"1\" spans=\"1:3\"><c r=\"A1\" s=\"1\"><v>1.5</v></c><c r=\"B1\" t=\"s\"><v>0</v></c>"
                + "<c r=\"C1\"><f t=\"shared\" ref=\"C1:C2\" si=\"0\">A1*2</f><v>3</v></c></row>"
                + "<row r=\"2\"><c r=\"A2\" t=\"b\"><v>1</v></c><c r=\"C2\"><f t=\"shared\" si=\"0\"/><v>2</v></c></row>";
        String odsRows = "<table:table-row table:number-rows-repeated=\"2\"><table:table-cell office:value-type=\"float\""
                + " office:value=\"1\" table:number-columns-repeated=\"3\"/><table:table-cell table:formula=\"of:=[.A1]*2\""
                + " office:value-type=\"float\" office:value=\"2\"/><table:table-cell office:value-type=\"string\"><text:p>a"
                + "<text:s text:c=\"3\"/>b</text:p></table:table-cell></table:table-row>";
        String junk = "<>/\"'=&;0123456789 -ar";
        for (int round = 0; round < 1500; round++) {
            boolean x = round % 2 == 0;
            StringBuilder part = new StringBuilder(x ? sheetData : odsRows);
            for (int edits = random(3) + 1; edits > 0 && part.length() > 0; edits--) {
                int at = random(part.length());
                switch (random(4)) {
                    case 0:
                        part.setCharAt(at, junk.charAt(random(junk.length())));
                        break;
                    case 1:
                        part.deleteCharAt(at);
                        break;
                    case 2:
                        part.insert(at, junk.charAt(random(junk.length())));
                        break;
                    default:
                        part.setLength(at);
                        break;
                }
            }
            survive("the part " + part, x ? xlsx(part.toString(), null) : ods(part.toString()),
                    x ? Workbook.Format.XLSX : Workbook.Format.ODS);
            if (wrong.length() > 4000) {
                break;
            }
        }
    }

    /// What is written has to be a file other programs take.
    @Test
    void whatIsWritten() throws IOException {
        sheet.setCellValue(0, 0, "=1+1");
        sheet.setCellValue(0, 1, Double.valueOf(-5));
        sheet.setCellValue(0, 2, "-1,234.50");
        sheet.setCellValue(0, 3, "+972 54 000 0000");
        sheet.setCellValue(0, 4, "-");
        sheet.setCellValue(0, 5, "-2+cmd|' /C calc'!A0");
        sheet.setCellValue(0, 6, "@SUM(1)");
        sheet.setCellValue(0, 7, Double.valueOf(999999999999999d));
        sheet.setCellFormat(0, 7, "0");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        book.write(out, Workbook.Format.CSV);
        same("'=1+1,-5,\"-1,234.50\",+972 54 000 0000,-,\"'-2+cmd|' /C calc'!A0\",'@SUM(1),999999999999999",
                new String(out.toByteArray(), "UTF-8").trim(), "a sheet as CSV");

        out = new ByteArrayOutputStream();
        book.write(out, Workbook.Format.ODS);
        byte[] file = out.toByteArray();
        // the type has to stand at byte 38, straight after the name: no extra field
        same(Integer.valueOf(0), Integer.valueOf(file[28] | file[29]), "the length of the extra field of the first entry");
        same("mimetypeapplication/vnd.oasis.opendocument.spreadsheet", new String(file, 30, 54, "UTF-8"),
                "the start of an ods file");
        same(Workbook.Format.ODS, Workbook.Format.detect("x.bin", file), "an ods file by its bytes");
        byte[] plainZip = zip("content.xml", "<x/>");
        same(Workbook.Format.ODS, Workbook.Format.detect("book.ods", plainZip), "an archive that says nothing, named .ods");
        same(Workbook.Format.XLSX, Workbook.Format.detect("book.ods", xlsx("", null)), "an Excel workbook named .ods");
        same(Workbook.Format.XLSX, Workbook.Format.detect("book.xlsx", plainZip), "an archive that says nothing, named .xlsx");
    }

    /// A text file is read where its lines are: an empty line is an empty row.
    @Test
    void aTextFileKeepsItsRows() throws IOException {
        Workbook csv = Workbook.read(new ByteArrayInputStream("a,b\n\n1,-007\n,\n\"=1+1\",0123456789012345678\n"
                .getBytes("UTF-8")), Workbook.Format.CSV);
        Sheet s = csv.getSheet(0);
        same("a", s.getCellValue("A1"), "the first line");
        same(null, s.getCellValue("A2"), "the empty line");
        same(Double.valueOf(1), s.getCellValue("A3"), "the line after the empty one");
        same("-007", s.getCellValue("B3"), "digits a number would not keep, with a sign");
        same("=1+1", s.getCellValue("A5"), "a formula in a text file is text");
        same(null, s.getCellFormula(4, 0), "and no formula");
        same("0123456789012345678", s.getCellValue("B5"), "digits a number would not keep");
        StringBuilder tall = new StringBuilder();
        for (int i = 0; i < 1048580; i++) {
            tall.append("1\n");
        }
        refused("more lines than a sheet has rows", tall.toString().getBytes("UTF-8"), Workbook.Format.CSV);
    }

    @Test
    void whatTheModelRefuses() {
        String[] names = {"a/b", "a\\b", "a?b", "a*b", "a[b", "a]b", "a:b", "'a", "a'", " ", ""};
        for (String name : names) {
            try {
                book.addSheet(name);
                wrong.append("\na sheet named ").append(name).append(" was accepted");
            } catch (IllegalArgumentException expected) {
                continue;
            }
        }
        same("It's", book.addSheet("It's").getName(), "an apostrophe inside a name");
        String[] defined = {"N2", "TRUE", "1st", "a b", "a+b", "", "R1C1x!"};
        for (String name : defined) {
            try {
                book.setDefinedName(name, "1");
                wrong.append("\nthe name ").append(name).append(" was accepted");
            } catch (IllegalArgumentException expected) {
                continue;
            }
        }
        book.setDefinedName("tax_rate.2026", "0.17");
        val(Double.valueOf(0.17), "tax_rate.2026");
        try {
            sheet.addMergedRegion(0, 0, 2000000, 0);
            wrong.append("\na merge past the sheet was accepted");
        } catch (IllegalArgumentException expected) {
            same(Integer.valueOf(0), Integer.valueOf(sheet.getMergedRegionCount()), "merges");
        }
        sheet.addMergedRegion(3, 2, 1, 0);
        int[] merged = sheet.getMergedRegion(0);
        same("1,0,3,2", merged[0] + "," + merged[1] + "," + merged[2] + "," + merged[3], "a merge given backwards");
        sheet.setFrozenPanes(-3, 99999);
        same(Integer.valueOf(0), Integer.valueOf(sheet.getFrozenRows()), "frozen rows");
        same(Boolean.TRUE, Boolean.valueOf(sheet.getFrozenColumns() <= 16383), "frozen columns stay on the sheet");
        // names that define each other have no value
        book.setDefinedName("first", "second+1");
        book.setDefinedName("second", "first+1");
        same(Boolean.TRUE, Boolean.valueOf(eval("first") instanceof CellError), "names that define each other are an error");
    }

    /// What a review of the arithmetic found.
    @Test
    void whatASpreadsheetAnswers() {
        Sheet other = book.addSheet("Other");
        other.setCellValue("A1", Double.valueOf(3));
        sheet.setCellFormula("A1", "Other!A1*2");
        sheet.setCellFormula("A2", "SHEETS()");
        same(Double.valueOf(6), sheet.getCellValue("A1"), "a cell of another sheet");
        same(Double.valueOf(2), sheet.getCellValue("A2"), "the sheets");
        book.removeSheet(1);
        same(CellError.REF, sheet.getCellValue("A1"), "a cell of a sheet that was removed, read before");
        same(Double.valueOf(1), sheet.getCellValue("A2"), "the sheets after one was removed");

        sheet.setCellFormula("B1", "YEAR(45000)");
        sheet.setCellValue("B2", Double.valueOf(45000));
        sheet.setCellFormat(1, 1, "yyyy");
        same(Double.valueOf(2023), sheet.getCellValue("B1"), "the year of a day");
        book.setDate1904(true);
        same(Double.valueOf(2027), sheet.getCellValue("B1"), "the year of a day counted from 1904");
        same("2027", sheet.getCellText(1, 1), "the year shown counted from 1904");
        book.setDate1904(false);

        val("999999999999999", "TEXT(999999999999999,\"0\")");
        val("10,000,000,000,000,000", "TEXT(1E+16,\"#,##0\")");
        val("1,000,000,000,000,000", "FIXED(1E+15,0)");
        val("12345678912345.00", "TEXT(12345678912345,\"0.00\")");
        val("123456789012346000", "TEXT(123456789012345678,\"0\")");
        val("0.10", "TEXT(0.1,\"0.00\")");
        val("1234.57", "TEXT(1234.5678,\"0.00\")");
        val("5", "TEXT(5,\"@\")");
        val("x", "TEXT(\"x\",\"@\")");
        sheet.setCellValue("C1", Double.valueOf(5));
        sheet.setCellFormat(0, 2, "@");
        same("5", sheet.getCellText(0, 2), "a number under the text format");

        sheet.setCellValue("D1", Double.valueOf(10));
        sheet.setCellValue("D2", Double.valueOf(20));
        sheet.setCellValue("E1", Double.valueOf(1));
        sheet.setCellValue("E2", Double.valueOf(2));
        val(Double.valueOf(2), "VLOOKUP(20,IF({1,0},D1:D2,E1:E2),2,FALSE)");
        val(Double.valueOf(2), "ROWS(IF({1,0},D1:D2,E1:E2))");
        val(Double.valueOf(33), "SUM(IF({1,0},D1:D2,E1:E2))");
        val(Double.valueOf(66), "SUM({1,2}+{10;20})");

        val(Boolean.TRUE, "0.1+0.2=0.3");
        val(Boolean.TRUE, "1.1*3=3.3");
        val(Boolean.FALSE, "0.1+0.2>0.3");
        val(Boolean.FALSE, "1=1.000000001");
        val(Boolean.TRUE, "1<1.000000001");
        val(Boolean.TRUE, "0=0");
        val(Boolean.FALSE, "0=1E-300");

        val(Double.valueOf(43832), "DATEVALUE(\"2-Jan-2020\")");
        val(Double.valueOf(43832), "DATEVALUE(\"2 January 2020\")");
        val(Double.valueOf(43832), "DATEVALUE(\"Jan 2, 2020\")");
        val(Double.valueOf(43832.75), "\"2-jan-2020 18:00\"+0");
        val(CellError.VALUE, "DATEVALUE(\"1/2/2020\")");
        val(CellError.VALUE, "DATEVALUE(\"30-Feb-2020\")");
        val(CellError.VALUE, "DATEVALUE(\"2-Janx-2020\")");
        sheet.setCellValue("F1", Double.valueOf(43832));
        sheet.setCellValue("F2", Double.valueOf(43000));
        val(Double.valueOf(1), "COUNTIF(F1:F2,\">=1-Jan-2020\")");
        sheet.setValueAt(5, 0, "12-Mar-2026");
        same(Boolean.TRUE, Boolean.valueOf(sheet.getValueAt(5, 0) instanceof Date), "a date typed with the name of its month");
        sheet.setValueAt(5, 1, "1.2E-05");
        same(Double.valueOf(0.000012), sheet.getValueAt(5, 1), "a number typed with an exponent");
        sheet.setValueAt(5, 2, "1E400");
        same("1E400", sheet.getValueAt(5, 2), "a number no cell holds stays the text it was");
        sheet.setValueAt(5, 3, "-12:30:00");
        sheet.setCellValue(5, 4, Float.valueOf(1.1f));
        same(Double.valueOf(1.1), sheet.getCellValue(5, 4), "a float");

        val(CellError.VALUE, "SUMIFS(D1:D2,E1:E3,\">0\")");
        val(Double.valueOf(30), "SUMIFS(D1:D2,E1:E2,\">0\")");
        val(Double.valueOf(0), "RATE(10,-10,100)");
        val(Double.valueOf(0), "RATE(10,-10,100,0,0,0.1)");
        val(Double.valueOf(5), "VALUE(\"$5\")");
        val(Double.valueOf(-5), "VALUE(\"(5)\")");
        val(Double.valueOf(-1234.5), "VALUE(\"($1,234.50)\")");
        val(CellError.VALUE, "VALUE(\"()\")");
        val(Double.valueOf(-2), "(-8)^(1/3)");
        val(CellError.NUM, "(-8)^(1/2)");
        val(CellError.NUM, "(-8)^0.3");
        val(Double.valueOf(0), "DAY(0)");
        val("1900-01-00", "TEXT(0,\"yyyy-mm-dd\")");
        val("12:00", "TEXT(0.5,\"hh:mm\")");
        val(CellError.VALUE, "TEXT(-1,\"dd/mm/yyyy\")");
        val("-1", "TEXT(-1,\"0\")");
        sheet.setCellFormula("G1", "1/0");
        sheet.setCellFormula("G2", "NA()");
        val(Double.valueOf(1), "COUNTIF(G1:G2,\"#DIV/0!\")");
        val(Double.valueOf(1), "COUNTIF(G1:G2,\"#N/A\")");
        val(Double.valueOf(1), "COUNTIF(G1:G2,\"<>#N/A\")");

        sheet.setCellFormula("H1", "1+1");
        sheet.setCellFormula(0, 7, null);
        same(null, sheet.getCellValue("H1"), "a cell whose formula was taken away");
        same(null, sheet.getCellFormula(0, 7), "its formula");

        sheet.setCellFormula("I1", "DATE(2026,3,4)");
        same("2026-03-04", sheet.getCellText(0, 8), "a date a formula made");
        same(Boolean.TRUE, Boolean.valueOf(sheet.getCellValue("I1") instanceof Date), "and its value");
        sheet.setCellFormula("I2", "TIME(6,30,0)");
        same("06:30:00", sheet.getCellText(1, 8), "a time a formula made");
        sheet.setCellFormula("I3", "DATE(2026,3,4)+1");
        same("46086", sheet.getCellText(2, 8), "arithmetic on a date is a number");
    }

    /// What the second review of files found: memory nothing counted, chains of operators
    /// as deep as they are long, formulas that never return, formats that throw.
    @Test
    void memoryNothingCounted() throws IOException {
        // one cell at the bottom of each of sixty sheets: the rows above it take room
        StringBuilder tables = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            tables.append("<table:table table:name=\"S").append(i).append("\"><table:table-row")
                    .append(" table:number-rows-repeated=\"1048575\"><table:table-cell/></table:table-row><table:table-row>")
                    .append("<table:table-cell office:value-type=\"float\" office:value=\"1\"/></table:table-row></table:table>");
        }
        refused("a cell at the bottom of sixty sheets", zip("mimetype", "application/vnd.oasis.opendocument.spreadsheet",
                "content.xml", "<office:document-content" + ODS_NS + "><office:body><office:spreadsheet>" + tables
                        + "</office:spreadsheet></office:body></office:document-content>"), Workbook.Format.ODS);
        // a column width for every column, three thousand times over
        refused("three thousand widths of every column", xlsx("</sheetData><cols>"
                + repeat("<col min=\"1\" max=\"16384\" width=\"10\" customWidth=\"1\"/>", 3000) + "</cols><sheetData>", null),
                Workbook.Format.XLSX);
        // but "every column from here on" is how Excel ends a file, and it is kept whole
        Workbook rest = survive("one width for the rest of the columns", xlsx("</sheetData><cols>"
                + "<col min=\"3\" max=\"16384\" width=\"12\" customWidth=\"1\"/></cols><sheetData>", null), Workbook.Format.XLSX);
        if (rest != null) {
            same(Double.valueOf(12), Double.valueOf(rest.getSheet(0).getColumnWidth(16383)), "the width of the last column");
        }
        // a number style of three hundred million digits
        survive("a style of three hundred million digits", zip("mimetype", "application/vnd.oasis.opendocument.spreadsheet",
                "content.xml", "<office:document-content" + ODS_NS
                        + " xmlns:number=\"urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0\""
                        + " xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\"><office:automatic-styles>"
                        + "<number:number-style style:name=\"N1\"><number:number number:min-integer-digits=\"300000000\"/>"
                        + "</number:number-style><style:style style:name=\"ce1\" style:family=\"table-cell\""
                        + " style:data-style-name=\"N1\"/></office:automatic-styles><office:body><office:spreadsheet>"
                        + "<table:table table:name=\"S\"><table:table-row><table:table-cell table:style-name=\"ce1\""
                        + " office:value-type=\"float\" office:value=\"1\"/></table:table-row></table:table>"
                        + "</office:spreadsheet></office:body></office:document-content>"), Workbook.Format.ODS);
        // spaces in two hundred cells, each as many as a text may hold
        String spaces = "<table:table-cell office:value-type=\"string\"><text:p><text:s text:c=\"2000000000\"/></text:p>"
                + "</table:table-cell>";
        refused("two hundred cells of four million spaces", ods("<table:table-row>" + repeat(spaces, 200)
                + "</table:table-row>"), Workbook.Format.ODS);
        // a table of strings that the cells never use
        Workbook.setCellLimit(200000);
        refused("half a million shared strings", zip("[Content_Types].xml", "<Types/>",
                "xl/workbook.xml", "<workbook xmlns=\"" + MAIN + "\" xmlns:r=\"" + REL + "\">"
                        + "<sheets><sheet name=\"S\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>",
                "xl/_rels/workbook.xml.rels", "<Relationships>"
                        + "<Relationship Id=\"rId1\" Type=\"" + REL + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                        + "<Relationship Id=\"rId2\" Type=\"" + REL + "/sharedStrings\" Target=\"sharedStrings.xml\"/>"
                        + "</Relationships>",
                "xl/sharedStrings.xml", "<sst xmlns=\"" + MAIN + "\">" + repeat("<si><t>a</t></si>", 500000) + "</sst>",
                "xl/worksheets/sheet1.xml", "<worksheet xmlns=\"" + MAIN + "\"><sheetData/></worksheet>"), Workbook.Format.XLSX);
        // one formula row repeated down the whole sheet: a tree for every row
        Workbook.setCellLimit(limit);
        refused("a formula repeated a million times", ods("<table:table-row table:number-rows-repeated=\"1048576\">"
                + "<table:table-cell table:formula=\"of:=[.B1]+1\" office:value-type=\"float\" office:value=\"1\"/>"
                + "<table:table-cell office:value-type=\"float\" office:value=\"1\"/></table:table-row>"), Workbook.Format.ODS);
        // a cell's own row is where it is; a row past the last is a damaged file
        Workbook own = survive("a cell whose row differs from its row element's",
                xlsx("<row r=\"2\"><c r=\"B9\"><v>7</v></c></row>", null), Workbook.Format.XLSX);
        if (own != null) {
            same(Double.valueOf(7), own.getSheet(0).getCellValue("B9"), "the cell is at its own reference");
        }
        refused("a cell in row 1048577", xlsx("<row r=\"1\"><c r=\"A1048577\"><v>1</v></c></row>", null), Workbook.Format.XLSX);
        refused("a cell in row 0", xlsx("<row r=\"1\"><c r=\"A0\"><v>1</v></c></row>", null), Workbook.Format.XLSX);
        // numbers are written in decimal, and nothing else reads as one
        Workbook hex = survive("a value in hexadecimal", xlsx("<row r=\"1\"><c r=\"A1\"><v>0x1p3</v></c>"
                + "<c r=\"B1\"><v>1d</v></c><c r=\"C1\"><v>-1.5E+3</v></c></row>", null), Workbook.Format.XLSX);
        if (hex != null) {
            same("0x1p3", hex.getSheet(0).getCellValue("A1"), "0x1p3 is not 8");
            same("1d", hex.getSheet(0).getCellValue("B1"), "1d is not 1");
            same(Double.valueOf(-1500), hex.getSheet(0).getCellValue("C1"), "scientific notation is a number");
        }
    }

    /// A workbook whose sheets have these names, each holding 5 in A1.
    private static byte[] namedSheets(String[] names) throws IOException {
        StringBuilder sheets = new StringBuilder();
        StringBuilder rels = new StringBuilder();
        String[] parts = new String[6 + 2 * names.length];
        for (int i = 0; i < names.length; i++) {
            sheets.append("<sheet name=\"").append(names[i]).append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>");
            rels.append("<Relationship Id=\"rId").append(i + 1).append("\" Type=\"").append(REL)
                    .append("/worksheet\" Target=\"worksheets/sheet").append(i + 1).append(".xml\"/>");
            parts[6 + 2 * i] = "xl/worksheets/sheet" + (i + 1) + ".xml";
            parts[7 + 2 * i] = "<worksheet xmlns=\"" + MAIN + "\"><sheetData><row r=\"1\"><c r=\"A1\"><v>5</v></c>"
                    + "</row></sheetData></worksheet>";
        }
        parts[0] = "[Content_Types].xml";
        parts[1] = "<Types/>";
        parts[2] = "xl/workbook.xml";
        parts[3] = "<workbook xmlns=\"" + MAIN + "\" xmlns:r=\"" + REL + "\"><sheets>" + sheets + "</sheets></workbook>";
        parts[4] = "xl/_rels/workbook.xml.rels";
        parts[5] = "<Relationships>" + rels + "</Relationships>";
        return zip(parts);
    }

    @Test
    void sheetNamesFromAFile() throws IOException {
        String[] names = {"", "   ", "a/b", "[x]", "a:b", "'q'", "Same", "same", "Same"};
        Workbook w = survive("sheets no formula could name", namedSheets(names), Workbook.Format.XLSX);
        if (w == null) {
            wrong.append("\nthe workbook of odd names was refused");
            return;
        }
        String[] expected = {"Sheet1", "Sheet2", "a_b", "_x_", "a_b (2)", "q", "Same", "same (2)", "Same (3)"};
        for (int i = 0; i < expected.length; i++) {
            same(expected[i], w.getSheet(i).getName(), "sheet " + i);
        }
        // and each can be named by a formula
        Sheet s = w.addSheet("Reader");
        s.setCellFormula("A1", "='a_b (2)'!A1+q!A1");
        same(Double.valueOf(10), s.getCellValue("A1"), "a formula reads the renamed sheets");
        // thousands of sheets that share one name are read in a moment
        String[] many = new String[3000];
        for (int i = 0; i < many.length; i++) {
            many[i] = "S";
        }
        Workbook big = Workbook.read(new ByteArrayInputStream(namedSheets(many)), Workbook.Format.XLSX);
        same("S (3000)", big.getSheet(2999).getName(), "the last of them");
        // a sheet whose name reads as a reference in R1C1 notation is quoted in a formula
        Workbook r1 = new Workbook();
        r1.addSheet("R1C1").setCellValue("A1", Double.valueOf(3));
        Sheet user = r1.addSheet("User");
        user.setCellFormula("A1", "='R1C1'!A1*2");
        same("='R1C1'!A1*2", user.getCellFormula(0, 0), "R1C1 is quoted");
        user.setCellFormula("A2", "='RC'!A1+'C3'!A1");
        same("='RC'!A1+'C3'!A1", user.getCellFormula(1, 0), "RC and C3 are quoted");
    }

    @Test
    void formulasThatNeverReturn() {
        // a sum of a thousand terms is an ordinary formula, and a chain of operators is as
        // long as the formula allows, on a small stack
        final StringBuilder chain = new StringBuilder("=A1");
        while (chain.length() < 16000) {
            chain.append("+A1");
        }
        final String[] failed = {null};
        Thread small = new Thread(null, new Runnable() {
            public void run() {
                try {
                    Workbook w = new Workbook();
                    // a chain this long is past Excel's formula limit; it is written to test the writers
                    w.setExcelLimitsEnforced(false);
                    Sheet s = w.addSheet("S");
                    s.setCellValue("A1", Double.valueOf(1));
                    s.setCellFormula("B1", chain.toString());
                    Object v = s.getCellValue("B1");
                    if (!Double.valueOf(chain.length() / 3).equals(v)) {
                        failed[0] = "the chain added up to " + v;
                    }
                    s.insertRows(0, 1);
                    s.setCellFormula("C1", "=(A2,A2,A2,A2,A2,A2) " + repeat("A2:A3 ", 2000) + "A2");
                    s.getCellValue("C1");
                    for (Workbook.Format f : new Workbook.Format[] {Workbook.Format.XLSX, Workbook.Format.ODS,
                            Workbook.Format.CSV}) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        w.write(out, f);
                        if (f != Workbook.Format.CSV) {
                            Workbook back = Workbook.read(new ByteArrayInputStream(out.toByteArray()), f);
                            back.getSheet(0).getCellValue("B2");
                        }
                    }
                } catch (Throwable t) { //NOPMD AvoidCatchingThrowable - a stack overflow is the failure looked for
                    failed[0] = t.toString();
                }
            }
        }, "small stack", 256 * 1024);
        small.start();
        try {
            small.join(60000);
        } catch (InterruptedException e) {
            failed[0] = e.toString();
        }
        if (small.isAlive()) {
            failed[0] = "the formulas never returned";
        }
        same(null, failed[0], "a long chain of operators on a small stack");
        val(CellError.VALUE, "LEN(SUBSTITUTE(REPT(\"a\",32767),\"a\",REPT(\"b\",32767)))");
        val(CellError.VALUE, "CONCAT(SEQUENCE(1000,1000),SEQUENCE(1000,1000))");
        val(CellError.VALUE, "ROWS(REPT(SEQUENCE(1000,1000),SEQUENCE(1000,1000)))");
        val(CellError.VALUE, "LEN(REPT(\"a\",20000)&REPT(\"a\",20000))");
        val(CellError.NUM, "COMBIN(1E15,5E14)");
        val(CellError.NUM, "COMBINA(1E15,5E14)");
        val(CellError.NUM, "PERMUT(1E9,5E8)");
        val(CellError.NUM, "NETWORKDAYS(1E300,1E300,1E300)");
        val(CellError.NUM, "WORKDAY(1E300,5)");
        eval("DDB(1,1E15,1E15,1E15)");
        val(CellError.NA, "MODE(SEQUENCE(1000,1000))");
        val(Double.valueOf(1000000), "ROWS(MODE.MULT(SEQUENCE(1000,1000)*0+{1}))*0+1000000");
        val(Double.valueOf(999000), "INDEX(FREQUENCY(SEQUENCE(1000,1000),SEQUENCE(1000)),1001)");
        // the answers small cases give did not move
        val(Double.valueOf(2), "MODE(1,2,2,3,3)");
        val(Double.valueOf(3), "MODE(3,1,1,3)");
        val(Double.valueOf(0.25), "DDB(1,0,4,2)");
        val(Double.valueOf(252), "COMBIN(10,5)");
        val(Double.valueOf(720), "PERMUT(10,3)");
        // forty cells that each double the one before
        Workbook w = new Workbook();
        Sheet s = w.addSheet("S");
        s.setCellValue("A1", "ab");
        for (int r = 2; r <= 40; r++) {
            s.setCellFormula("A" + r, "=A" + (r - 1) + "&A" + (r - 1));
        }
        w.recalculate();
        same(CellError.VALUE, s.getCellValue("A40"), "text that doubles forty times");
        // a format code in scientific notation on a number too small for a power of ten
        same("4.00E-320", CellFormat.format("0.00E+00", Double.valueOf(4e-320), false), "a subnormal number");
        same("4.94E-324", CellFormat.format("0.00E+00", Double.valueOf(Double.MIN_VALUE), false), "the smallest number");
        same("40.0E-321", CellFormat.format("##0.0E+0", Double.valueOf(4e-320), false), "in engineering notation");
        // nothing smaller than the smallest normal number is shown with digits it does not have
        Workbook tiny = new Workbook();
        Sheet ts = tiny.addSheet("S");
        ts.setCellValue("A1", Double.valueOf(1e-320));
        same("1E-320", ts.getCellText(0, 0), "a subnormal number in General");
        same("4.9E-324", Values.general(Double.MIN_VALUE), "the smallest number in General");
        same("1.5E-301", Values.general(1.5e-301), "a number just past the scaling");
        same("1E-300", Values.general(9.999999999999999E-301), "a mantissa that rounds up to ten");
        // the codes of built-in formats 5 to 8 are written in full: those ids are currency in en-US
        Workbook money = new Workbook();
        Sheet ms = money.addSheet("S");
        ms.setCellValue("A1", Double.valueOf(-1234.5));
        ms.setCellFormat(0, 0, "#,##0;[Red]-#,##0");
        try {
            ByteArrayOutputStream x = new ByteArrayOutputStream();
            money.write(x, Workbook.Format.XLSX);
            same("#,##0;[Red]-#,##0", Workbook.read(new ByteArrayInputStream(x.toByteArray()), Workbook.Format.XLSX)
                    .getSheet(0).getCellFormat(0, 0), "a plain format is not written as a built-in id");
            java.util.zip.ZipInputStream z = new java.util.zip.ZipInputStream(new ByteArrayInputStream(x.toByteArray()));
            for (java.util.zip.ZipEntry en = z.getNextEntry(); en != null; en = z.getNextEntry()) {
                if ("xl/styles.xml".equals(en.getName())) {
                    ByteArrayOutputStream st = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    for (int n = z.read(buf); n > 0; n = z.read(buf)) {
                        st.write(buf, 0, n);
                    }
                    same(Boolean.TRUE, Boolean.valueOf(new String(st.toByteArray(), "UTF-8").contains("formatCode=\"#,##0;[Red]-#,##0\"")),
                            "the code is in the styles");
                }
            }
        } catch (IOException e) {
            wrong.append('\n').append(e);
        }
        same("9999-12-31", CellFormat.format("yyyy-mm-dd", Double.valueOf(2958465), false), "the last day");
        same("########", CellFormat.format("yyyy-mm-dd", Double.valueOf(2958466), false), "the day after the last");
        // a date the format cannot show keeps its number in a text file
        ts.setCellValue("B1", Double.valueOf(-5));
        ts.setCellFormat(0, 1, "yyyy-mm-dd");
        try {
            ByteArrayOutputStream csv = new ByteArrayOutputStream();
            tiny.write(csv, Workbook.Format.CSV);
            same("1E-320,-5", new String(csv.toByteArray(), "UTF-8").trim(), "a date before the first day in CSV");
            // an xlsx whose sheets are all missing is no workbook
            refused("a workbook whose sheet parts are missing", zip("[Content_Types].xml", "<Types/>",
                    "xl/workbook.xml", "<workbook xmlns=\"" + MAIN + "\" xmlns:r=\"" + REL + "\"><sheets><sheet name=\"S\""
                            + " sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>",
                    "xl/_rels/workbook.xml.rels", "<Relationships><Relationship Id=\"rId1\" Type=\"" + REL
                            + "/worksheet\" Target=\"../../etc/passwd\"/></Relationships>"), Workbook.Format.XLSX);
        } catch (IOException e) {
            wrong.append('\n').append(e);
        }
        // TEXTAFTER ignoring case does not cut the text where a folded copy would
        val("B", "TEXTAFTER(\"\u0130a-B\",\"-\",1,1)");
        val("x", "TEXTAFTER(\"aBcx\",\"bc\",1,1)");
        val("aB", "TEXTBEFORE(\"aBcx\",\"CX\",-1,1)");
    }

    @Test
    void nestingPastExcelsLimit() throws IOException {
        // Excel nests 64 deep; deeper is refused as a file error, never a stack overflow
        StringBuilder deep = new StringBuilder("<row r=\"1\"><c r=\"A1\"><f>");
        for (int i = 0; i < 2000; i++) {
            deep.append("IF(1,");
        }
        deep.append('1');
        for (int i = 0; i < 2000; i++) {
            deep.append(')');
        }
        deep.append("</f></c></row>");
        survive("formulas nested 2000 deep", xlsx(deep.toString(), null), Workbook.Format.XLSX);
        Workbook w = new Workbook();
        Sheet sheet = w.addSheet("S");
        StringBuilder f = new StringBuilder("=");
        for (int i = 0; i < 60; i++) {
            f.append("IF(1,");
        }
        f.append('7');
        for (int i = 0; i < 60; i++) {
            f.append(')');
        }
        sheet.setCellFormula("A1", f.toString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        w.write(out, Workbook.Format.XLSX);
        same(Double.valueOf(7), Workbook.read(new ByteArrayInputStream(out.toByteArray()), Workbook.Format.XLSX)
                .getSheet(0).getCellValue("A1"), "60 deep is read back");
    }

    @Test
    void roundTwentyTwoLosses() throws IOException {
        // a duplicated sheet name that uses another duplicated sheet name
        Workbook w = Workbook.read(new ByteArrayInputStream(zip("[Content_Types].xml", "<Types/>",
                "xl/workbook.xml", "<workbook xmlns=\"" + MAIN + "\" xmlns:r=\"" + REL + "\"><sheets>"
                        + "<sheet name=\"A\" sheetId=\"1\" r:id=\"rId1\"/><sheet name=\"B\" sheetId=\"2\" r:id=\"rId2\"/></sheets>"
                        + "<definedNames><definedName name=\"X\" localSheetId=\"0\">A!$A$1</definedName>"
                        + "<definedName name=\"X\" localSheetId=\"1\">B!$A$1</definedName>"
                        + "<definedName name=\"Y\" localSheetId=\"0\">X*2</definedName>"
                        + "<definedName name=\"Y\" localSheetId=\"1\">X*3</definedName></definedNames></workbook>",
                "xl/_rels/workbook.xml.rels", "<Relationships><Relationship Id=\"rId1\" Type=\"" + REL
                        + "/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"" + REL
                        + "/worksheet\" Target=\"worksheets/sheet2.xml\"/></Relationships>",
                "xl/worksheets/sheet1.xml", "<worksheet xmlns=\"" + MAIN + "\"><sheetData><row r=\"1\"><c r=\"A1\"><v>100</v></c><c r=\"B1\"><f>SUM(X,Y)</f></c></row></sheetData></worksheet>",
                "xl/worksheets/sheet2.xml", "<worksheet xmlns=\"" + MAIN + "\"><sheetData><row r=\"1\"><c r=\"A1\"><v>2</v></c><c r=\"B1\"><f>SUM(X,Y)</f></c></row></sheetData></worksheet>")), Workbook.Format.XLSX);
        w.getSheet(0).setCellValue("A1", Double.valueOf(100));
        same(Double.valueOf(300), w.getSheet(0).getCellValue("B1"), "X and Y of the first sheet");
        same(Double.valueOf(8), w.getSheet(1).getCellValue("B1"), "X and Y of the second sheet");
        // a relative reference in a name moves to the cell that uses it, wrapping at the edge
        Sheet s = new Workbook().addSheet("A");
        s.getWorkbook().setDefinedName("Rel", "=A!$A1");
        s.getWorkbook().setDefinedName("Above", "=A!$A1048576");
        for (int r = 0; r < 3; r++) {
            s.setCellValue(r, 0, Double.valueOf(r + 1));
            s.setCellFormula(r, 1, "=Rel");
        }
        same(Double.valueOf(2), s.getCellValue("B2"), "R from row 2");
        s.setCellFormula("C3", "=Above");
        same(Double.valueOf(2), s.getCellValue("C3"), "the row above");
        s.setCellValue("A2", Double.valueOf(20));
        same(Double.valueOf(20), s.getCellValue("B2"), "recalculated through the moved reference");
        // an intersection with a row range on its right
        s.setCellFormula("D1", "=SUM(A1:A3 2:2)");
        same(Double.valueOf(20), s.getCellValue("D1"), "row range on the right of an intersection");
        s.setCellValue("E1", new java.math.BigDecimal("1e400"));
        same(CellError.NUM, s.getCellValue("E1"), "a number past a double");
        // inserting rows moves the cell that uses a relative name, never the name itself
        Sheet t = new Workbook().addSheet("T");
        t.getWorkbook().setDefinedName("Up", "=T!$A1048576");
        t.getWorkbook().setDefinedName("Below", "=T!$A2");
        t.getWorkbook().setDefinedName("Fixed", "=T!$A$2");
        for (int r = 0; r < 6; r++) {
            t.setCellValue(r, 0, Double.valueOf(r + 1));
        }
        t.setCellFormula("B4", "=Up");
        t.setCellFormula("C1", "=Below+Fixed");
        t.insertRows(0, 1);
        t.insertRows(5, 1);
        same(Double.valueOf(3), t.getCellValue("B5"), "the row above, after inserting rows");
        same(Double.valueOf(4), t.getCellValue("C2"), "the row below and a fixed cell, after inserting rows");
        same("T!$A2", t.getWorkbook().getDefinedName("Below"), "a relative name's text");
        t.insertColumns(0, 1);
        same(Double.valueOf(3), t.getCellValue("C5"), "the row above, after inserting a column");
        // written to OpenDocument from a base cell that needs no wrapping, and read back the same
        ByteArrayOutputStream ods = new ByteArrayOutputStream();
        t.getWorkbook().write(ods, Workbook.Format.ODS);
        String content = new String(unzip(ods.toByteArray(), "content.xml"), "UTF-8");
        if (content.indexOf("1048576") >= 0) {
            wrong.append("\na wrapped reference reached content.xml");
        }
        Sheet back = Workbook.read(new ByteArrayInputStream(ods.toByteArray()), Workbook.Format.ODS).getSheet(0);
        back.setCellValue("B4", Double.valueOf(30));
        same(Double.valueOf(30), back.getCellValue("C5"), "the row above, through OpenDocument");
        // a mixed name whose fixed end is removed stands for nothing
        t.getWorkbook().setDefinedName("Mixed", "=T!$A1:$A$5");
        t.removeRows(0, 5);
        same("#REF!", t.getWorkbook().getDefinedName("Mixed"), "a mixed name with its fixed end removed");
        try {
            t.getWorkbook().setDefinedName("NoSheet", "$A$1:$A$5");
            wrong.append("\na name without a sheet was accepted");
        } catch (IllegalArgumentException expected) {
            // a name belongs to no sheet
        }
        // OpenDocument: a relative name is relative to its base cell, here B2, so from B1 it is A1
        Workbook o = Workbook.read(new ByteArrayInputStream(ods("<table:table-row><table:table-cell office:value-type=\"float\""
                + " office:value=\"5\"/><table:table-cell table:formula=\"of:=N\"/></table:table-row><table:table-row>"
                + "<table:table-cell office:value-type=\"float\" office:value=\"6\"/></table:table-row></table:table>"
                + "<table:named-expressions><table:named-range table:name=\"N\" table:cell-range-address=\"$S.$A2\""
                + " table:base-cell-address=\"$S.$B$2\"/></table:named-expressions><table:table table:name=\"T\">")),
                Workbook.Format.ODS);
        o.getSheet(0).setCellValue("A2", Double.valueOf(7));
        same(Double.valueOf(5), o.getSheet(0).getCellValue("B1"), "an OpenDocument name relative to its base cell");
    }

    @Test
    void roundTwentyOneLosses() throws IOException {
        // two sheets each define Q: each sheet's formulas keep their own meaning
        Workbook w = Workbook.read(new ByteArrayInputStream(zip("[Content_Types].xml", "<Types/>",
                "xl/workbook.xml", "<workbook xmlns=\"" + MAIN + "\" xmlns:r=\"" + REL + "\"><sheets>"
                        + "<sheet name=\"S1\" sheetId=\"1\" r:id=\"rId1\"/><sheet name=\"S2\" sheetId=\"2\" r:id=\"rId2\"/></sheets>"
                        + "<definedNames><definedName name=\"Q\" localSheetId=\"0\">S1!$A$1</definedName>"
                        + "<definedName name=\"Q\" localSheetId=\"1\">S2!$A$1</definedName></definedNames></workbook>",
                "xl/_rels/workbook.xml.rels", "<Relationships><Relationship Id=\"rId1\" Type=\"" + REL
                        + "/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"" + REL
                        + "/worksheet\" Target=\"worksheets/sheet2.xml\"/></Relationships>",
                "xl/worksheets/sheet1.xml", "<worksheet xmlns=\"" + MAIN + "\"><sheetData><row r=\"1\"><c r=\"A1\"><v>1</v></c><c r=\"B1\"><f>Q*10</f></c></row></sheetData></worksheet>",
                "xl/worksheets/sheet2.xml", "<worksheet xmlns=\"" + MAIN + "\"><sheetData><row r=\"1\"><c r=\"A1\"><v>2</v></c><c r=\"B1\"><f>Q*10</f></c></row></sheetData></worksheet>")), Workbook.Format.XLSX);
        w.getSheet(0).setCellValue("A1", Double.valueOf(3));
        w.getSheet(1).setCellValue("A1", Double.valueOf(4));
        same(Double.valueOf(30), w.getSheet(0).getCellValue("B1"), "the first sheet's Q");
        same(Double.valueOf(40), w.getSheet(1).getCellValue("B1"), "the second sheet's Q");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        w.write(out, Workbook.Format.XLSX);
        Workbook back = Workbook.read(new ByteArrayInputStream(out.toByteArray()), Workbook.Format.XLSX);
        same(Double.valueOf(40), back.getSheet(1).getCellValue("B1"), "the second sheet's Q, saved");
        Sheet s = back.getSheet(0);
        s.setCellValue("C1", Float.valueOf(Float.NaN));
        same(CellError.NUM, s.getCellValue("C1"), "a float NaN");
        for (int r = 0; r < 5; r++) {
            s.setCellValue(r, 4, Double.valueOf(r * 10));
            s.setCellValue(r, 5, "v" + r);
        }
        s.setCellFormula("G1", "=XLOOKUP(40,E1:E5,F1:F4)");
        same(CellError.VALUE, s.getCellValue("G1"), "results shorter than the keys");
        s.setCellFormula("G1", "=XLOOKUP(40,E1:E5,F1:F5)");
        same("v4", s.getCellValue("G1"), "results as long as the keys");
    }

    @Test
    void roundTwentyLosses() throws IOException {
        // a name only a sheet defines is kept; one the workbook also defines takes the workbook's
        Workbook w = Workbook.read(new ByteArrayInputStream(zip("[Content_Types].xml", "<Types/>",
                "xl/workbook.xml", "<workbook xmlns=\"" + MAIN + "\" xmlns:r=\"" + REL + "\"><sheets><sheet name=\"S\""
                        + " sheetId=\"1\" r:id=\"rId1\"/></sheets><definedNames>"
                        + "<definedName name=\"Loc\" localSheetId=\"0\">S!$B$1</definedName>"
                        + "<definedName name=\"Both\" localSheetId=\"0\">S!$A$1</definedName>"
                        + "<definedName name=\"Both\">S!$B$1</definedName></definedNames></workbook>",
                "xl/_rels/workbook.xml.rels", "<Relationships><Relationship Id=\"rId1\" Type=\"" + REL
                        + "/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>",
                "xl/worksheets/sheet1.xml", "<worksheet xmlns=\"" + MAIN + "\"><sheetData><row r=\"1\"><c r=\"A1\"><v>1</v></c>"
                        + "<c r=\"B1\"><v>5</v></c></row></sheetData></worksheet>")), Workbook.Format.XLSX);
        Sheet s = w.getSheet(0);
        s.setCellFormula("C1", "=Loc+Both");
        same(Double.valueOf(10), s.getCellValue("C1"), "a sheet's own name, and the workbook's over a sheet's");
        // NaN and infinity are #NUM! the moment they are stored, as every file writes them
        s.setCellValue("D1", Double.valueOf(Double.NaN));
        same(CellError.NUM, s.getCellValue("D1"), "NaN");
        s.setCellFormula("E1", "=ISERROR(D1)");
        same(Boolean.TRUE, s.getCellValue("E1"), "NaN is an error to formulas too");
        // match and search modes Excel does not know
        String[] modes = {"XMATCH(2,{1,2,3},3)", "XMATCH(2,{1,2,3},0,0)", "XMATCH(2,{1,2,3},0,7)", "XLOOKUP(2,{1,2,3},{4,5,6},,9)"};
        for (String f : modes) {
            s.setCellFormula("F1", "=" + f);
            same(CellError.VALUE, s.getCellValue("F1"), f);
        }
        s.setCellFormula("F1", "=XMATCH(2,{1,2,3},0,-1)");
        same(Double.valueOf(2), s.getCellValue("F1"), "a search from the end");
    }

    @Test
    void argumentsCountedAsExcelCountsThem() throws IOException {
        Sheet s = new Workbook().addSheet("S");
        String[] refused = {"=ABS(1,2)", "=ROUND(1)", "=NOT()", "=SUM()", "=NORM.S.DIST(1)", "=FLOOR(2.5)"};
        for (String f : refused) {
            try {
                s.setCellFormula("A1", f);
                wrong.append('\n').append(f).append(" was accepted");
            } catch (IllegalArgumentException expected) {
                // Excel does not let it be typed either
            }
        }
        s.setCellFormula("A1", "=ROUND(2.567,1)+ABS(-1)");
        same(Double.valueOf(3.6), s.getCellValue("A1"), "counts that fit");
        // read from a file, the formula is unreadable and keeps the value the file stored
        Workbook w = Workbook.read(new ByteArrayInputStream(xlsx("<row r=\"1\"><c r=\"A1\"><f>ABS(1,2)</f><v>7</v></c></row>",
                null)), Workbook.Format.XLSX);
        same(Double.valueOf(7), w.getSheet(0).getCellValue(0, 0), "the stored value of a formula with too many arguments");
        // OpenDocument allows ROUND with one argument; Excel does not, so it stays a value
        Workbook o = Workbook.read(new ByteArrayInputStream(ods("<table:table-row><table:table-cell table:formula=\"of:=ROUND(2.5)\""
                + " office:value-type=\"float\" office:value=\"3\"/></table:table-row>")), Workbook.Format.ODS);
        same(Double.valueOf(3), o.getSheet(0).getCellValue(0, 0), "an OpenDocument call Excel would refuse");
        ByteArrayOutputStream x = new ByteArrayOutputStream();
        o.write(x, Workbook.Format.XLSX);
        same(null, Workbook.read(new ByteArrayInputStream(x.toByteArray()), Workbook.Format.XLSX).getSheet(0).getFormulaAt(0, 0),
                "written to xlsx as its value");
    }

    @Test
    void settingsWithAStrayEndTag() throws IOException {
        // an end tag before any sheet's settings once read the split modes of no sheet
        Workbook w = Workbook.read(new ByteArrayInputStream(zip("mimetype", "application/vnd.oasis.opendocument.spreadsheet",
                "content.xml", "<office:document-content" + ODS_NS + "><office:body><office:spreadsheet>"
                        + "<table:table table:name=\"S\"><table:table-row><table:table-cell office:value-type=\"float\""
                        + " office:value=\"1\"/></table:table-row></table:table></office:spreadsheet></office:body>"
                        + "</office:document-content>",
                "settings.xml", "</a></b><office:document-settings/>")), Workbook.Format.ODS);
        same(Double.valueOf(1), w.getSheet(0).getCellValue(0, 0), "the cell past broken settings");
    }

    @Test
    void fractionsLineUpOnQuestionMarks() {
        // what LibreOffice answers for TEXT with these formats; Excel pads the same way
        same("-2  1/3 ", CellFormat.format("# ??/??", Double.valueOf(-2.3333333333), false), "# ??/??");
        same(" 1/2 ", CellFormat.format("??/??", Double.valueOf(0.5), false), "??/??");
        same("3  1/10", CellFormat.format("0 ??/??", Double.valueOf(3.1), false), "0 ??/??");
        same("1/4", CellFormat.format("#/#", Double.valueOf(0.25), false), "#/#");
        same("2   ", CellFormat.format("# ?/8", Double.valueOf(2), false), "no fraction, fixed denominator");
        same("0      ", CellFormat.format("# ??/??", Double.valueOf(0), false), "no fraction");
        same("2 0/1", CellFormat.format("0 0/0", Double.valueOf(2), false), "a zero placeholder shows the fraction");
        same("-02 1/2", CellFormat.format("00 ?/?", Double.valueOf(-2.5), false), "a padded whole part");
        same("00 1/2", CellFormat.format("00 0/0", Double.valueOf(0.5), false), "a zero whole part kept by its zeros");
        same("   1/2", CellFormat.format("?? ?/?", Double.valueOf(0.5), false), "a blank whole part");
        same("1,234    ", CellFormat.format("#,##0 ?/?", Double.valueOf(1234), false), "a grouped whole part");
        same("0,005 1/2", CellFormat.format("0,000 ?/?", Double.valueOf(5.5), false), "zeros grouped with the digits");
        same("1,000,000,000,000,000 1/2", CellFormat.format("#,##0 ?/?", Double.valueOf(1e15 + 0.5), false),
                "a large whole part in full");
    }

    @Test
    void insertingAndRemovingPastTheData() {
        Workbook w = new Workbook();
        Sheet s = w.addSheet("S");
        s.setCellValue("B1", Double.valueOf(1));
        s.setCellFormula("A1", "=H1");
        s.setCellFormula("A2", "=E1");
        // the cells end at B, but a reference past them still follows the removal
        s.removeColumns(3, 1);
        same("=G1", s.getFormulaAt(0, 0), "a reference past the data moves left");
        s.removeColumns(2, 10);
        same(CellError.REF, s.getCellValue(1, 0), "a reference into removed empty columns");
        // inserting a million rows is one move, and pushing data off the grid is refused
        long start = System.currentTimeMillis();
        s.insertRows(0, 1000000);
        same(Boolean.TRUE, Boolean.valueOf(System.currentTimeMillis() - start < 20000), "a large insert finishes");
        try {
            s.insertRows(0, 100000);
            wrong.append("\nan insert pushing data past the last row was accepted");
        } catch (IllegalArgumentException expected) {
            // refused
        }
        // counts too large to add to a position reach the edge of the grid and no further
        Sheet o = w.addSheet("O");
        o.setCellValue("A1", Double.valueOf(1));
        o.setCellFormula("B1", "=D10");
        o.addMergedRegion(2, 2, 3, 3);
        o.removeRows(1, Integer.MAX_VALUE);
        same(CellError.REF, o.getCellValue(0, 1), "a reference into rows removed to the end");
        same(Integer.valueOf(0), Integer.valueOf(o.getMergedRegionCount()), "a merge in rows removed to the end");
        o.setCellFormula("B1", "=D10");
        o.addMergedRegion(2, 2, 3, 3);
        o.insertRows(1, Integer.MAX_VALUE);
        same(CellError.REF, o.getCellValue(0, 1), "a reference pushed off the grid");
        same(Integer.valueOf(0), Integer.valueOf(o.getMergedRegionCount()), "a merge pushed off the grid");
        // widths and outline levels pushed off the grid leave it too, or the file is invalid
        Sheet edge = w.addSheet("E");
        edge.setColumnWidth(16000, 20);
        edge.setRowOutlineLevel(1048000, 1);
        edge.insertColumns(10, 1000);
        edge.insertRows(10, 1000);
        same(Integer.valueOf(0), Integer.valueOf(edge.getRowOutlineLevel(1048575)), "no outline level past the grid");
        try {
            ByteArrayOutputStream x = new ByteArrayOutputStream();
            w.write(x, Workbook.Format.XLSX);
            StringBuilder all = new StringBuilder();
            java.util.zip.ZipInputStream z = new java.util.zip.ZipInputStream(new ByteArrayInputStream(x.toByteArray()));
            byte[] buf = new byte[4096];
            for (java.util.zip.ZipEntry en = z.getNextEntry(); en != null; en = z.getNextEntry()) {
                for (int n = z.read(buf); n > 0; n = z.read(buf)) {
                    all.append(new String(buf, 0, n, "ISO-8859-1"));
                }
            }
            same(Boolean.FALSE, Boolean.valueOf(all.indexOf("min=\"17001\"") >= 0), "no column past the grid in the file");
        } catch (IOException e) {
            wrong.append('\n').append(e);
        }
        o.setCellFormula("B1", "=Z1");
        o.removeColumns(2, Integer.MAX_VALUE);
        same(CellError.REF, o.getCellValue(0, 1), "a reference into columns removed to the end");
        try {
            s.insertColumns(0, 16384);
            wrong.append("\nan insert pushing data past the last column was accepted");
        } catch (IllegalArgumentException expected) {
            // refused
        }
    }

    @Test
    void countsOverEveryArea() {
        Workbook w = new Workbook();
        Sheet s = w.addSheet("S");
        w.addSheet("M").setCellValue("A1", Double.valueOf(5));
        w.addSheet("Q").setCellValue("A1", Double.valueOf(7));
        String[] formulas = {"COUNT(S:Q!A1)", "COUNT(M:Q!A1)", "COUNTA(S:Q!A1)", "COUNT((M!A1,Q!A1))",
            "AGGREGATE(2,0,M:Q!A1)", "SUBTOTAL(2,M:Q!A1)", "COUNTBLANK(S!B1:B2)", "SUM(S:Q!A1)"};
        Object[] expected = {Double.valueOf(2), Double.valueOf(2), Double.valueOf(2), Double.valueOf(2),
            Double.valueOf(2), Double.valueOf(2), Double.valueOf(2), Double.valueOf(12)};
        for (int i = 0; i < formulas.length; i++) {
            s.setCellFormula(i, 3, "=" + formulas[i]);
            same(expected[i], s.getCellValue(i, 3), formulas[i]);
        }
        // a function that wants rows and columns refuses several areas, as Excel does
        s.setCellFormula("E1", "=SUMPRODUCT(M:Q!A1)");
        same(CellError.VALUE, s.getCellValue("E1"), "SUMPRODUCT over sheets");
        s.setCellFormula("E2", "=COUNTIF(M:Q!A1,\">0\")");
        same(CellError.VALUE, s.getCellValue("E2"), "COUNTIF over sheets");
        // the IS functions answer for a union or a span of sheets, never return an error
        s.setCellFormula("E3", "=ISERROR(M:Q!A1)");
        same(Boolean.TRUE, s.getCellValue("E3"), "ISERROR over sheets");
        s.setCellFormula("E4", "=ISNUMBER((M!A1,Q!A1))");
        same(Boolean.FALSE, s.getCellValue("E4"), "ISNUMBER of a union");
        // functions that take several areas walk all of them
        s.setCellFormula("E5", "=OR((M!A1,Q!A1))");
        same(Boolean.TRUE, s.getCellValue("E5"), "OR of a union");
        s.setCellFormula("E6", "=AND(M:Q!A1)");
        same(Boolean.TRUE, s.getCellValue("E6"), "AND over sheets");
        s.setCellFormula("E7", "=CONCAT((M!A1,Q!A1))");
        same("57", s.getCellValue("E7"), "CONCAT of a union");
        s.setCellFormula("E8", "=TEXTJOIN(\",\",TRUE,M:Q!A1)");
        same("5,7", s.getCellValue("E8"), "TEXTJOIN over sheets");
    }

    @Test
    void roundEightLosses() throws Exception {
        // names nested in names are held to one depth, on a small stack too. 1 MB, not
        // less: the coverage agent the build runs tests under makes every frame larger, and
        // a plain 512 KB thread calculates both cases below.
        final StringBuilder failure = new StringBuilder();
        Thread t = new Thread(null, new Runnable() {
            public void run() {
                try {
                    Workbook w = new Workbook();
                    Sheet s = w.addSheet("S");
                    for (int i = 1; i <= 32; i++) {
                        StringBuilder f = new StringBuilder("=");
                        for (int k = 0; k < 60; k++) {
                            f.append("ABS(");
                        }
                        f.append(i == 1 ? "1" : "NM_" + (i - 1) + "+1");
                        for (int k = 0; k < 60; k++) {
                            f.append(')');
                        }
                        w.setDefinedName("NM_" + i, f.toString());
                    }
                    s.setCellFormula("A1", "=NM_32");
                    w.recalculate();
                    if (!CellError.NUM.equals(s.getCellValue("A1"))) {
                        failure.append("names nested past the limit gave ").append(s.getCellValue("A1"));
                    }
                } catch (Throwable err) {
                    failure.append(err);
                }
            }
        }, "small", 1024 * 1024);
        t.start();
        t.join(60000);
        same(Boolean.FALSE, Boolean.valueOf(t.isAlive()), "the nested names finished");
        same("", failure.toString(), "nested names");
        // one name at the limit, used by a formula at the limit, still calculates
        final StringBuilder single = new StringBuilder();
        Thread one = new Thread(null, new Runnable() {
            public void run() {
                try {
                    Workbook w = new Workbook();
                    Sheet s = w.addSheet("S");
                    w.setDefinedName("Rate", "=" + repeat("ABS(", 63) + repeat("(", 61) + "-2" + repeat(")", 61)
                            + repeat(")", 63));
                    s.setCellFormula("A1", "=" + repeat("ABS(", 64) + "Rate" + repeat(")", 64));
                    w.recalculate();
                    if (!Double.valueOf(2).equals(s.getCellValue("A1"))) {
                        single.append("a name in a formula gave ").append(s.getCellValue("A1"));
                    }
                } catch (Throwable err) {
                    single.append(err);
                }
            }
        }, "small-one", 1024 * 1024);
        one.start();
        one.join(60000);
        same("", single.toString(), "one deep name");
        // 64 functions nested is Excel's limit, and parentheses are not functions
        Sheet s = new Workbook().addSheet("S");
        s.setCellFormula("A1", "=" + repeat("ABS(", 64) + "-1" + repeat(")", 64));
        same(Double.valueOf(1), s.getCellValue("A1"), "64 nested functions");
        s.setCellFormula("A2", "=" + repeat("ABS((", 40) + "-1" + repeat("))", 40));
        same(Double.valueOf(1), s.getCellValue("A2"), "40 functions with their own parentheses");
        try {
            s.setCellFormula("A3", "=" + repeat("ABS(", 65) + "1" + repeat(")", 65));
            wrong.append("\n65 nested functions were taken");
        } catch (IllegalArgumentException expected) {
            // Excel refuses it too
        }
        // every section of a format survives OpenDocument: empty ones, dates, text
        String[] codes = {"##0.0E+0", "0.00E+00", ";;;", ";0", "General;General;;", "General;-General", "General;;", "0;;;", "0;;;@", "0;-0;", "0;-0;;@", "yyyy-mm-dd;-0;0;@", "0;-0;0;\"x\"",
            "[>100]0;[<0]0;0;@"};
        Object[] values = {Double.valueOf(5), Double.valueOf(-5), Double.valueOf(0), "tx", Double.valueOf(500),
            Double.valueOf(1234.75), Double.valueOf(-12.5)};
        Workbook formats = new Workbook();
        Sheet f = formats.addSheet("S");
        for (int r = 0; r < codes.length; r++) {
            for (int c = 0; c < values.length; c++) {
                f.setCellValue(r, c, values[c]);
                f.setCellFormat(r, c, codes[r]);
            }
        }
        ByteArrayOutputStream ods = new ByteArrayOutputStream();
        formats.write(ods, Workbook.Format.ODS);
        Sheet back = Workbook.read(new ByteArrayInputStream(ods.toByteArray()), Workbook.Format.ODS).getSheet(0);
        for (int r = 0; r < codes.length; r++) {
            for (int c = 0; c < values.length; c++) {
                same(f.getCellText(r, c), back.getCellText(r, c), codes[r] + " showing " + values[c]);
            }
        }
        // text that starts with an apostrophe keeps it through CSV
        Workbook csv = new Workbook();
        Sheet cs = csv.addSheet("S");
        String[] texts = {"'=x", "'+1+2", "'@b", "'plain", "=y", "''=z", "a\tb", "a;b", "note: a|b", "c|d",
            "\t=1"};
        for (int i = 0; i < texts.length; i++) {
            cs.setCellValue(i, 0, texts[i]);
        }
        ByteArrayOutputStream c = new ByteArrayOutputStream();
        csv.write(c, Workbook.Format.CSV);
        Sheet csvBack = Workbook.read(new ByteArrayInputStream(c.toByteArray()), Workbook.Format.CSV).getSheet(0);
        for (int i = 0; i < texts.length; i++) {
            same(texts[i], csvBack.getCellValue(i, 0), "text after CSV");
        }
        // a date past year 9999 keeps its number in OpenDocument and CSV
        Workbook late = new Workbook();
        Sheet ls = late.addSheet("S");
        ls.setCellValue(0, 0, Double.valueOf(3000000));
        ls.setCellFormat(0, 0, "yyyy-mm-dd");
        ByteArrayOutputStream lo = new ByteArrayOutputStream();
        late.write(lo, Workbook.Format.ODS);
        Sheet lateBack = Workbook.read(new ByteArrayInputStream(lo.toByteArray()), Workbook.Format.ODS).getSheet(0);
        same(ls.getCellValue(0, 0), lateBack.getCellValue(0, 0), "a late date after OpenDocument");
        ByteArrayOutputStream lc = new ByteArrayOutputStream();
        late.write(lc, Workbook.Format.CSV);
        same("3000000", new String(lc.toByteArray(), "UTF-8").trim(), "a late date in CSV");
    }

    @Test
    void roundTripsThatLostSomething() throws IOException {
        // an OpenDocument name nothing can read stays out of Excel's file
        byte[] file = zip("mimetype", "application/vnd.oasis.opendocument.spreadsheet", "content.xml",
                "<office:document-content" + ODS_NS + "><office:body><office:spreadsheet><table:table table:name=\"S\">"
                        + "<table:table-row><table:table-cell office:value-type=\"float\" office:value=\"1\"/>"
                        + "</table:table-row></table:table><table:named-expressions><table:named-expression"
                        + " table:name=\"N\" table:expression=\"of:=[$S.$A$1]+((\"/></table:named-expressions>"
                        + "</office:spreadsheet></office:body></office:document-content>");
        Workbook named = survive("an unreadable named expression", file, Workbook.Format.ODS);
        if (named != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            named.write(out, Workbook.Format.XLSX);
            String[] names = Workbook.read(new ByteArrayInputStream(out.toByteArray()), Workbook.Format.XLSX)
                    .getDefinedNames();
            same(Integer.valueOf(0), Integer.valueOf(names.length), "OpenDocument syntax written as an Excel name");
        }
        // a fourth section, for text, survives OpenDocument
        Workbook formats = new Workbook();
        Sheet f = formats.addSheet("S");
        f.setValueAt(0, 0, "hi");
        f.setCellFormat(0, 0, "0;-0;0;\"Note: \"@");
        same("Note: hi", f.getCellText(0, 0), "the text section");
        ByteArrayOutputStream ods = new ByteArrayOutputStream();
        formats.write(ods, Workbook.Format.ODS);
        Sheet back = Workbook.read(new ByteArrayInputStream(ods.toByteArray()), Workbook.Format.ODS).getSheet(0);
        same("Note: hi", back.getCellText(0, 0), "the text section after OpenDocument");
        // the apostrophe CSV puts before text that would run comes off again
        Workbook csv = new Workbook();
        csv.addSheet("S").setCellValue(0, 0, "=A1");
        ByteArrayOutputStream c = new ByteArrayOutputStream();
        csv.write(c, Workbook.Format.CSV);
        same("=A1", Workbook.read(new ByteArrayInputStream(c.toByteArray()), Workbook.Format.CSV).getSheet(0)
                .getCellValue("A1"), "text with an equals sign after CSV");
        // "=" counts what is empty, "<>" what is not, ="" included; dates end where Excel's do
        Workbook book = new Workbook();
        Sheet s = book.addSheet("S");
        s.setCellValue("A1", Double.valueOf(1));
        s.setCellValue("A3", "x");
        s.setCellFormula("A4", "=\"\"");
        s.setCellValue("A5", Double.valueOf(5));
        s.setCellFormula("B1", "=COUNTIF(A1:A5,\"=\")");
        s.setCellFormula("B2", "=COUNTIF(A1:A5,\"<>\")");
        s.setCellFormula("B3", "=COUNTIF(A1:A5,\"\")");
        s.setCellFormula("B4", "=EDATE(1,1000000)");
        s.setCellFormula("B5", "=EOMONTH(1,1000000)");
        same(Double.valueOf(1), s.getCellValue("B1"), "COUNTIF =");
        same(Double.valueOf(4), s.getCellValue("B2"), "COUNTIF <>");
        same(Double.valueOf(2), s.getCellValue("B3"), "COUNTIF of the empty text");
        same(CellError.NUM, s.getCellValue("B4"), "EDATE past the last day");
        same(CellError.NUM, s.getCellValue("B5"), "EOMONTH past the last day");
    }

    @Test
    void anOpenDocumentFormulaNothingCanRead() throws IOException {
        Workbook w = survive("an unreadable formula with a text value", ods("<table:table-row><table:table-cell"
                + " table:formula=\"of:=[.A2]+((\" office:value-type=\"string\"><text:p>hi</text:p></table:table-cell>"
                + "</table:table-row>"), Workbook.Format.ODS);
        if (w == null) {
            wrong.append("\nthe file was refused");
            return;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        w.write(out, Workbook.Format.XLSX);
        same("hi", Workbook.read(new ByteArrayInputStream(out.toByteArray()), Workbook.Format.XLSX).getSheet(0)
                .getCellValue("A1"), "its value is kept in Excel's file");
        // and a long value of such a cell is held to Excel's text limit
        Workbook longer = survive("an unreadable formula with a long text value", ods("<table:table-row><table:table-cell"
                + " table:formula=\"of:=((\" office:value-type=\"string\"><text:p>" + repeat("a", 32768)
                + "</text:p></table:table-cell></table:table-row>"), Workbook.Format.ODS);
        if (longer != null) {
            try {
                longer.write(new ByteArrayOutputStream(), Workbook.Format.XLSX);
                wrong.append("\na long value past Excel's limit was written");
            } catch (IOException expected) {
                same(Boolean.TRUE, Boolean.valueOf(expected.getMessage().endsWith("S!A1")), "the cell is named");
            }
        }
    }

    @Test
    void excelLimitsFailFast() throws IOException {
        String[] what = {"name", "text", "formula"};
        for (String limit : what) {
            Workbook w = new Workbook();
            Sheet s = w.addSheet("name".equals(limit) ? repeat("n", 32) : "S");
            if ("text".equals(limit)) {
                s.setCellValue("B3", repeat("t", 32768));
            } else if ("formula".equals(limit)) {
                s.setCellFormula("B3", "=" + repeat("1+", 4096) + "1");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                w.write(out, Workbook.Format.XLSX);
                wrong.append("\nthe ").append(limit).append(" over Excel's limit was written");
            } catch (IOException expected) {
                if (!"name".equals(limit)) {
                    same(Boolean.TRUE, Boolean.valueOf(expected.getMessage().endsWith("S!B3")), "the cell is named");
                }
            }
            w.setExcelLimitsEnforced(false);
            w.write(out, Workbook.Format.XLSX);
            Workbook back = Workbook.read(new ByteArrayInputStream(out.toByteArray()), Workbook.Format.XLSX);
            same(s.getName(), back.getSheet(0).getName(), "with the limits off, the " + limit + " is written as it is");
            // OpenDocument has none of these limits
            w.setExcelLimitsEnforced(true);
            w.write(new ByteArrayOutputStream(), Workbook.Format.ODS);
        }
        // what a defined name refers to is a formula too
        Workbook named = new Workbook();
        named.addSheet("S");
        named.setDefinedName("big", "=" + repeat("1+", 4600) + "1");
        try {
            named.write(new ByteArrayOutputStream(), Workbook.Format.XLSX);
            wrong.append("\na defined name over Excel's limit was written");
        } catch (IOException expected) {
            same(Boolean.TRUE, Boolean.valueOf(expected.getMessage().endsWith("big")), "the name is named");
        }
        // and a formula is measured as the file holds it, with the prefixes of newer functions
        Workbook prefixed = new Workbook();
        prefixed.addSheet("S").setCellFormula("A1", "=" + repeat("IFS(1,1)+", 900) + "1");
        try {
            prefixed.write(new ByteArrayOutputStream(), Workbook.Format.XLSX);
            wrong.append("\na formula over the limit once prefixed was written");
        } catch (IOException expected) {
            same(Boolean.TRUE, Boolean.valueOf(expected.getMessage().endsWith("S!A1")), "the prefixed formula is named");
        }
        // at the limits exactly, everything is written
        Workbook w = new Workbook();
        Sheet s = w.addSheet(repeat("n", 31));
        s.setCellValue("A1", repeat("t", 32767));
        w.write(new ByteArrayOutputStream(), Workbook.Format.XLSX);
    }

    @Test
    void whatOpenDocumentKeeps() throws IOException {
        String[] codes = {"#,##0.00_);(#,##0.00)", "#,##0;-#,##0;\"zero\"", "[>100]\"big\";0", "000-00-0000"};
        double[] values = {-1234.5, 0, 0, 123456789};
        String[] shown = {"(1,234.50)", "zero", "0", "123-45-6789"};
        Workbook w = new Workbook();
        Sheet s = w.addSheet("S");
        for (int i = 0; i < codes.length; i++) {
            s.setCellValue(i, 0, Double.valueOf(values[i]));
            s.setCellFormat(i, 0, codes[i]);
        }
        s.setRowOutlineLevel(40, 2);
        s.setColumnWidth(300, 5);
        for (Workbook.Format f : new Workbook.Format[] {Workbook.Format.ODS, Workbook.Format.XLSX}) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            w.write(out, f);
            Sheet back = Workbook.read(new ByteArrayInputStream(out.toByteArray()), f).getSheet(0);
            for (int i = 0; i < codes.length; i++) {
                same(shown[i], s.getCellText(i, 0), codes[i]);
                same(shown[i], back.getCellText(i, 0), codes[i] + " through " + f);
            }
            same(Integer.valueOf(2), Integer.valueOf(back.getRowOutlineLevel(40)), "an empty grouped row through " + f);
            same(Double.valueOf(5), Double.valueOf(Math.round(back.getColumnWidth(300) * 100) / 100.0),
                    "the width of an empty column through " + f);
        }
    }

    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder(s.length() * times);
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
