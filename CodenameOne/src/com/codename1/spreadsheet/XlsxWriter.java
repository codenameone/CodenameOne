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

import com.codename1.io.zip.ZipArchiveOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;

/// Writes a workbook as an Excel file.
///
/// The parts and the order of the elements inside them follow the schema of the format, to
/// which Excel holds a file strictly: an element out of order is a file it offers to repair.
final class XlsxWriter {
    private static final String HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    private static final String MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String PACKAGE_REL = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final int FIRST_CUSTOM_FORMAT = 164;

    private static final int MAX_SHEET_NAME = 31;
    private static final int MAX_FORMULA = 8192;
    private static final int MAX_TEXT = 32767;
    private final Workbook book;
    private final ArrayList<String> strings = new ArrayList<String>();
    private final HashMap<String, Integer> stringIndex = new HashMap<String, Integer>();
    private int stringUses;
    private boolean formulas;

    XlsxWriter(Workbook book) {
        this.book = book;
    }

    /// Excel's limits, checked before anything is written. Excel repairs a file past one by
    /// dropping or cutting what is over it, so the caller hears of it here instead; shortening
    /// a name behind the caller's back would also leave every formula naming it pointing at
    /// nothing. `Workbook.setExcelLimitsEnforced(false)` writes the file as it is.
    private void checkLimits() throws IOException {
        for (String name : book.getDefinedNames()) {
            String text = storedName(name);
            if (text != null && text.length() + 1 > MAX_FORMULA) {
                throw new IOException("Excel takes formulas of up to " + MAX_FORMULA + " characters: the name " + name);
            }
        }
        for (int i = 0; i < book.getSheetCount(); i++) {
            Sheet sheet = book.getSheet(i);
            String name = sheet.getName();
            if (name.length() > MAX_SHEET_NAME) {
                throw new IOException("Excel takes sheet names of up to " + MAX_SHEET_NAME + " characters: " + name);
            }
            int rows = sheet.usedRows();
            for (int r = 0; r < rows; r++) {
                Cell[] cells = sheet.rowCells(r);
                if (cells == null) {
                    continue;
                }
                for (int c = 0; c < cells.length; c++) {
                    Cell cell = cells[c];
                    if (cell == null) {
                        continue;
                    }
                    // measured as written, prefixes included: the stricter of the two lengths
                    String formula = storedFormula(cell);
                    if (formula != null && formula.length() + 1 > MAX_FORMULA) {
                        throw new IOException("Excel takes formulas of up to " + MAX_FORMULA + " characters: "
                                + name + "!" + ref(r, c));
                    }
                    if (formula == null && cell.value instanceof String
                            && ((String) cell.value).length() > MAX_TEXT) {
                        throw new IOException("Excel takes text of up to " + MAX_TEXT + " characters in a cell: "
                                + name + "!" + ref(r, c));
                    }
                }
            }
        }
    }

    /// A formula as the file holds it, with the prefixes newer functions take; null for none.
    private static String storedFormula(Cell cell) {
        if (cell.formula != null) {
            return FormulaParser.print(cell.formula, false);
        }
        return cell.formulaText != null && !cell.odsSyntax ? cell.formulaText : null;
    }

    /// What a defined name refers to, as the file holds it.
    private String storedName(String name) {
        Node node = book.definedNameNode(name);
        return node == null ? book.getDefinedName(name) : FormulaParser.print(node, false);
    }

    void write(OutputStream out) throws IOException {
        if (book.isExcelLimitsEnforced()) {
            checkLimits();
        }
        collectStrings();
        ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out);
        int sheets = Math.max(1, book.getSheetCount());

        XmlOut x = part(zip, "[Content_Types].xml");
        x.raw(HEAD).raw("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .raw("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                .raw("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .raw("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        for (int i = 1; i <= sheets; i++) {
            x.raw("<Override PartName=\"/xl/worksheets/sheet").raw(i).raw(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        x.raw("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
                .raw("<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>")
                .raw("</Types>");
        end(zip, x);

        x = part(zip, "_rels/.rels");
        x.raw(HEAD).raw("<Relationships xmlns=\"").raw(PACKAGE_REL).raw("\">")
                .raw("<Relationship Id=\"rId1\" Type=\"").raw(REL).raw("/officeDocument\" Target=\"xl/workbook.xml\"/>")
                .raw("</Relationships>");
        end(zip, x);

        x = part(zip, "xl/workbook.xml");
        writeWorkbook(x, sheets);
        end(zip, x);

        x = part(zip, "xl/_rels/workbook.xml.rels");
        x.raw(HEAD).raw("<Relationships xmlns=\"").raw(PACKAGE_REL).raw("\">");
        for (int i = 1; i <= sheets; i++) {
            x.raw("<Relationship Id=\"rId").raw(i).raw("\" Type=\"").raw(REL).raw("/worksheet\" Target=\"worksheets/sheet")
                    .raw(i).raw(".xml\"/>");
        }
        x.raw("<Relationship Id=\"rId").raw(sheets + 1).raw("\" Type=\"").raw(REL).raw("/styles\" Target=\"styles.xml\"/>")
                .raw("<Relationship Id=\"rId").raw(sheets + 2).raw("\" Type=\"").raw(REL)
                .raw("/sharedStrings\" Target=\"sharedStrings.xml\"/>").raw("</Relationships>");
        end(zip, x);

        for (int i = 0; i < sheets; i++) {
            x = part(zip, "xl/worksheets/sheet" + (i + 1) + ".xml");
            writeSheet(x, i < book.getSheetCount() ? book.getSheet(i) : null);
            end(zip, x);
        }

        x = part(zip, "xl/styles.xml");
        writeStyles(x);
        end(zip, x);

        x = part(zip, "xl/sharedStrings.xml");
        x.raw(HEAD).raw("<sst xmlns=\"").raw(MAIN).raw("\"").attr("count", stringUses).attr("uniqueCount", strings.size())
                .raw(">");
        for (String s : strings) {
            x.raw(needsPreserve(s) ? "<si><t xml:space=\"preserve\">" : "<si><t>").text(s, true).raw("</t></si>");
        }
        x.raw("</sst>");
        end(zip, x);
        zip.finish();
    }

    private static XmlOut part(ZipArchiveOutputStream zip, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        return new XmlOut(zip);
    }

    private static void end(ZipArchiveOutputStream zip, XmlOut x) throws IOException {
        x.flush();
        zip.closeEntry();
    }

    private static boolean needsPreserve(String s) {
        int n = s.length();
        if (n == 0) {
            return false;
        }
        char first = s.charAt(0);
        char last = s.charAt(n - 1);
        return first <= ' ' || last <= ' ' || s.indexOf('\n') >= 0;
    }

    private void collectStrings() {
        for (int s = 0; s < book.getSheetCount(); s++) {
            Sheet sheet = book.getSheet(s);
            int rows = sheet.usedRows();
            for (int r = 0; r < rows; r++) {
                Cell[] cells = sheet.rowCells(r);
                if (cells == null) {
                    continue;
                }
                for (Cell cell : cells) {
                    if (cell == null) {
                        continue;
                    }
                    // the same test as writeCell's: an OpenDocument formula that could not be
                    // read is written as its value, and a text value needs its shared string
                    if (storedFormula(cell) != null) {
                        formulas = true;
                    } else if (cell.value instanceof String) {
                        stringUses++;
                        String text = (String) cell.value;
                        if (!stringIndex.containsKey(text)) {
                            stringIndex.put(text, Integer.valueOf(strings.size()));
                            strings.add(text);
                        }
                    }
                }
            }
        }
    }

    private void writeWorkbook(XmlOut x, int sheets) throws IOException {
        x.raw(HEAD).raw("<workbook xmlns=\"").raw(MAIN).raw("\" xmlns:r=\"").raw(REL).raw("\">");
        x.raw(book.isDate1904() ? "<workbookPr date1904=\"1\"/>" : "<workbookPr/>");
        x.raw("<bookViews><workbookView/></bookViews><sheets>");
        for (int i = 0; i < sheets; i++) {
            String name = i < book.getSheetCount() ? book.getSheet(i).getName() : "Sheet1";
            x.raw("<sheet").attr("name", name).attr("sheetId", i + 1).raw(" r:id=\"rId").raw(i + 1).raw("\"/>");
        }
        x.raw("</sheets>");
        String[] names = book.getDefinedNames();
        if (names.length > 0) {
            x.raw("<definedNames>");
            for (String nameItem : names) {
                String text = storedName(nameItem);
                x.raw("<definedName").attr("name", nameItem).raw(">").text(text, false).raw("</definedName>");
            }
            x.raw("</definedNames>");
        }
        if (formulas) {
            // the values written beside the formulas are this implementation's; a program
            // that calculates is asked to calculate
            x.raw("<calcPr calcId=\"0\" fullCalcOnLoad=\"1\"/>");
        }
        x.raw("</workbook>");
    }

    /// Cell styles: one per number format, and the same again in bold for header rows.
    private void writeStyles(XmlOut x) throws IOException {
        int count = book.formatCount();
        int[] ids = new int[count];
        int custom = 0;
        for (int i = 0; i < count; i++) {
            int builtin = CellFormat.builtinId(book.format(i).code());
            ids[i] = builtin >= 0 ? builtin : FIRST_CUSTOM_FORMAT + custom++;
        }
        x.raw(HEAD).raw("<styleSheet xmlns=\"").raw(MAIN).raw("\">");
        if (custom > 0) {
            x.raw("<numFmts").attr("count", custom).raw(">");
            for (int i = 0; i < count; i++) {
                if (ids[i] >= FIRST_CUSTOM_FORMAT) {
                    x.raw("<numFmt").attr("numFmtId", ids[i]).attr("formatCode", book.format(i).code()).raw("/>");
                }
            }
            x.raw("</numFmts>");
        }
        x.raw("<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/></font>")
                .raw("<font><b/><sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/></font></fonts>")
                .raw("<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>")
                .raw("<fill><patternFill patternType=\"gray125\"/></fill></fills>")
                .raw("<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>")
                .raw("<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>");
        x.raw("<cellXfs").attr("count", count * 2).raw(">");
        for (int font = 0; font < 2; font++) {
            for (int i = 0; i < count; i++) {
                x.raw("<xf").attr("numFmtId", ids[i]).attr("fontId", font).raw(" fillId=\"0\" borderId=\"0\" xfId=\"0\"");
                if (ids[i] != 0) {
                    x.raw(" applyNumberFormat=\"1\"");
                }
                if (font != 0) {
                    x.raw(" applyFont=\"1\"");
                }
                x.raw("/>");
            }
        }
        x.raw("</cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>")
                .raw("</styleSheet>");
    }

    private static String ref(int row, int col) {
        StringBuilder sb = new StringBuilder(8);
        FormulaParser.appendColumn(sb, col);
        sb.append(row + 1);
        return sb.toString();
    }

    private void writeSheet(XmlOut x, Sheet sheet) throws IOException {
        x.raw(HEAD).raw("<worksheet xmlns=\"").raw(MAIN).raw("\">");
        if (sheet == null) {
            x.raw("<sheetData/></worksheet>");
            return;
        }
        int rows = Math.max(sheet.usedRows(), sheet.outlineExtent());
        int columns = Math.max(sheet.usedColumns(), sheet.widthExtent());
        boolean outline = false;
        for (int r = 0; r < rows && !outline; r++) {
            outline = sheet.getRowOutlineLevel(r) > 0;
        }
        if (outline) {
            // a tree's children are under their parent, so a group is summed up above it
            x.raw("<sheetPr><outlinePr summaryBelow=\"0\"/></sheetPr>");
        }
        if (rows > 0 && columns > 0) {
            x.raw("<dimension").attr("ref", "A1:" + ref(rows - 1, columns - 1)).raw("/>");
        }
        x.raw("<sheetViews><sheetView workbookViewId=\"0\">");
        int fr = sheet.getFrozenRows();
        int fc = sheet.getFrozenColumns();
        if (fr > 0 || fc > 0) {
            x.raw("<pane");
            if (fc > 0) {
                x.attr("xSplit", fc);
            }
            if (fr > 0) {
                x.attr("ySplit", fr);
            }
            x.attr("topLeftCell", ref(fr, fc))
                    .attr("activePane", fr > 0 && fc > 0 ? "bottomRight" : fr > 0 ? "bottomLeft" : "topRight")
                    .raw(" state=\"frozen\"/>");
        }
        x.raw("</sheetView></sheetViews><sheetFormatPr defaultRowHeight=\"15\"/>");
        boolean cols = false;
        for (int c = 0; c < columns; c++) {
            double w = sheet.getColumnWidth(c);
            if (w > 0) {
                if (!cols) {
                    x.raw("<cols>");
                    cols = true;
                }
                x.raw("<col").attr("min", c + 1).attr("max", c + 1).attr("width", FormulaParser.numberText(w))
                        .raw(" customWidth=\"1\"/>");
            }
        }
        if (cols) {
            x.raw("</cols>");
        }
        x.raw("<sheetData>");
        int bold = book.formatCount();
        boolean header = sheet.isHeaderRow();
        for (int r = 0; r < rows; r++) {
            Cell[] cells = sheet.rowCells(r);
            int level = sheet.getRowOutlineLevel(r);
            boolean any = false;
            if (cells != null) {
                for (int c = 0; c < cells.length && !any; c++) {
                    any = cells[c] != null && (cells[c].value != null || cells[c].formulaText != null
                            || cells[c].format != 0);
                }
            }
            if (!any && level == 0) {
                continue;
            }
            x.raw("<row").attr("r", r + 1);
            if (level > 0) {
                x.attr("outlineLevel", level);
            }
            x.raw(">");
            if (any && cells != null) {
                for (int c = 0; c < cells.length; c++) {
                    if (cells[c] != null) {
                        writeCell(x, cells[c], r, c, header && r == 0 ? bold : 0);
                    }
                }
            }
            x.raw("</row>");
        }
        x.raw("</sheetData>");
        int merges = sheet.getMergedRegionCount();
        if (merges > 0) {
            x.raw("<mergeCells").attr("count", merges).raw(">");
            for (int i = 0; i < merges; i++) {
                int[] m = sheet.getMergedRegion(i);
                x.raw("<mergeCell").attr("ref", ref(m[0], m[1]) + ":" + ref(m[2], m[3])).raw("/>");
            }
            x.raw("</mergeCells>");
        }
        x.raw("</worksheet>");
    }

    private void writeCell(XmlOut x, Cell cell, int row, int col, int styleBase) throws IOException {
        Object v = cell.value;
        String formula = storedFormula(cell);
        int style = styleBase + cell.format;
        if (v == null && formula == null && style == 0) {
            return;
        }
        if (v instanceof Double) {
            double d = ((Double) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                v = CellError.NUM;
            }
        }
        x.raw("<c").attr("r", ref(row, col));
        if (style != 0) {
            x.attr("s", style);
        }
        if (v instanceof String) {
            x.raw(formula != null ? " t=\"str\"" : " t=\"s\"");
        } else if (v instanceof Boolean) {
            x.raw(" t=\"b\"");
        } else if (v instanceof CellError) {
            x.raw(" t=\"e\"");
        }
        x.raw(">");
        if (formula != null) {
            if (cell.arrayFormula) {
                x.raw("<f t=\"array\"").attr("ref", ref(row, col)).raw(">");
            } else {
                x.raw("<f>");
            }
            x.text(formula, false).raw("</f>");
        }
        if (v instanceof String) {
            if (formula != null) {
                x.raw("<v>").text((String) v, true).raw("</v>");
            } else {
                x.raw("<v>").raw(stringIndex.get(v).intValue()).raw("</v>");
            }
        } else if (v instanceof Boolean) {
            x.raw(((Boolean) v).booleanValue() ? "<v>1</v>" : "<v>0</v>");
        } else if (v instanceof CellError) {
            // the circular reference marker is not an error a file can hold
            x.raw("<v>").text(v == CellError.CIRCULAR ? "#REF!" : v.toString(), false).raw("</v>"); //NOPMD CompareObjectsWithEquals - an error value is a constant
        } else if (v instanceof Double) {
            x.raw("<v>").raw(FormulaParser.numberText(((Double) v).doubleValue())).raw("</v>");
        }
        x.raw("</c>");
    }
}
