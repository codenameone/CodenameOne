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
        same("'=1+1,-5,\"-1,234.50\",+972 54 000 0000,-,'-2+cmd|' /C calc'!A0,'@SUM(1),999999999999999",
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

    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder(s.length() * times);
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
