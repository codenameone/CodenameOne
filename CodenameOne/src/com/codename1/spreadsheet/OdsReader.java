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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// Reads an OpenDocument spreadsheet: a zip archive whose `content.xml` holds the sheets.
///
/// The format describes a number format as elements -- a year, a separator, a month -- and
/// not as a code; they are turned into the code Excel would write for the same thing, which
/// is what the rest of this package speaks. Formulas are in OpenFormula syntax and are
/// parsed into the same trees Excel's are.
final class OdsReader {
    private final Workbook book;
    private final long[] budget = {XmlScanner.MAX_DOCUMENT};
    private final HashMap<String, String> dataStyles = new HashMap<String, String>();
    /// The sections of a style that maps values to other styles: condition and style name,
    /// in order.
    private final java.util.HashSet<String> textStyles = new java.util.HashSet<String>();
    private final HashMap<String, ArrayList<String[]>> styleMaps = new HashMap<String, ArrayList<String[]>>();
    private final HashMap<String, String> cellStyles = new HashMap<String, String>();
    private final HashMap<String, Double> columnStyleWidths = new HashMap<String, Double>();
    private final HashMap<String, int[]> panes = new HashMap<String, int[]>();
    private final ArrayList<String[]> names = new ArrayList<String[]>();
    private final StringBuilder text = new StringBuilder(); //NOPMD AvoidStringBufferField - one buffer for every token of one file

    OdsReader(Workbook book) {
        this.book = book;
    }

    void read(InputStream in) throws IOException {
        ZipInputStream zip = new ZipInputStream(in);
        boolean content = false;
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            String path = entry.getName();
            if ("content.xml".equals(path)) {
                content = true;
                readContent(new XmlScanner(zip, budget));
            } else if ("styles.xml".equals(path)) {
                // number formats a document shares between its cells are here, and this part
                // may come before the cells or after them
                readContent(new XmlScanner(zip, budget));
            } else if ("settings.xml".equals(path)) {
                readSettings(new XmlScanner(zip, budget));
            }
            zip.closeEntry();
        }
        if (!content) {
            throw new IOException("Not an OpenDocument spreadsheet: the archive has no content.xml");
        }
        if (book.getSheetCount() == 0) {
            // a text document or a drawing has a content.xml too
            throw new IOException("Not an OpenDocument spreadsheet: it has no tables");
        }
        resolveFormats();
        for (int i = 0; i < book.getSheetCount(); i++) {
            Sheet sheet = book.getSheet(i);
            int[] pane = panes.get(sheet.getName());
            if (pane != null) {
                sheet.setFrozenPanes(pane[0], pane[1]);
            }
        }
        for (String[] d : names) {
            try {
                Node node = FormulaParser.parse(d[1], true);
                book.defineName(d[0], FormulaParser.display(node), node);
            } catch (FormulaSyntaxException e) {
                book.defineName(d[0], d[1], null);
                book.odsNameText.add(FormulaParser.asciiUpper(d[0]));
            }
        }
    }

    // ------------------------------------------------------------------ settings

    /// The frozen panes are not in the document but in the settings of the view of it.
    private void readSettings(XmlScanner x) throws IOException {
        String table = null;
        int depth = 0;
        int tableDepth = -1;
        int[] pane = null;
        int[] modes = null;
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START) {
                depth++;
                String name = x.attr("config:name");
                if ("config-item-map-entry".equals(x.name) && name != null && tableDepth < 0) {
                    table = name;
                    tableDepth = depth;
                    pane = new int[2];
                    modes = new int[2];
                } else if ("config-item".equals(x.name) && table != null && name != null) {
                    String value = x.elementText().trim();
                    depth--;
                    int v;
                    try {
                        v = Integer.parseInt(value);
                    } catch (NumberFormatException err) {
                        continue;
                    }
                    if ("HorizontalSplitMode".equals(name)) {
                        modes[1] = v;
                    } else if ("VerticalSplitMode".equals(name)) {
                        modes[0] = v;
                    } else if ("HorizontalSplitPosition".equals(name)) {
                        pane[1] = v;
                    } else if ("VerticalSplitPosition".equals(name)) {
                        pane[0] = v;
                    }
                }
            } else if (e == XmlScanner.END) {
                if (depth == tableDepth) {
                    // mode 2 is a frozen split; 1 is a split that scrolls, at a pixel position
                    if (modes[0] != 2) {
                        pane[0] = 0;
                    }
                    if (modes[1] != 2) {
                        pane[1] = 0;
                    }
                    if (pane[0] > 0 || pane[1] > 0) {
                        panes.put(table, pane);
                    }
                    table = null;
                    tableDepth = -1;
                }
                depth--;
            }
        }
    }

    // ------------------------------------------------------------------ content

    private Sheet sheet;
    private String sheetName;
    private int row;
    private int col;
    private int rowRepeat;
    private int groupDepth;
    private boolean rowHasContent;
    private String currentColumnStyle;
    /// For each run of columns: first column, last column, default cell style.
    private final ArrayList<Object[]> columnDefaults = new ArrayList<Object[]>();
    private int declaredColumn;

    private void readContent(XmlScanner x) throws IOException {
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            String tag = x.name;
            if (e == XmlScanner.END) {
                if ("table-row".equals(tag) && sheet != null) {
                    endRow();
                } else if ("table-row-group".equals(tag)) {
                    groupDepth--;
                } else if ("table".equals(tag) && sheet != null) {
                    book.attach(sheet, sheetName);
                    sheet = null;
                }
                continue;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            if ("table-cell".equals(tag) || "covered-table-cell".equals(tag)) {
                if (sheet != null) {
                    readCell(x, "covered-table-cell".equals(tag));
                }
            } else if ("table-row".equals(tag)) {
                rowRepeat = Math.max(1, x.intAttr("table:number-rows-repeated", 1));
                col = 0;
                rowHasContent = false;
            } else if ("table".equals(tag)) {
                sheet = book.detachedSheet();
                sheetName = x.attr("table:name");
                if (sheetName == null) {
                    sheetName = "Sheet" + (book.getSheetCount() + 1);
                }
                row = 0;
                groupDepth = 0;
                declaredColumn = 0;
                columnDefaults.clear();
            } else if ("table-column".equals(tag)) {
                if (sheet != null) {
                    int n = Math.max(1, x.intAttr("table:number-columns-repeated", 1));
                    Double width = columnStyleWidths.get(x.attr("table:style-name"));
                    if (width != null) {
                        for (int c = declaredColumn; c < declaredColumn + n && c < declaredColumn + 256; c++) {
                            sheet.setColumnWidth(c, width.doubleValue());
                        }
                    }
                    String cellStyle = x.attr("table:default-cell-style-name");
                    if (cellStyle != null) {
                        Integer first = Integer.valueOf(declaredColumn);
                        Integer last = Integer.valueOf(declaredColumn + n - 1);
                        columnDefaults.add(new Object[] {first, last, cellStyle});
                    }
                    declaredColumn += n;
                }
            } else if ("table-row-group".equals(tag)) {
                groupDepth++;
            } else if ("style".equals(tag)) {
                String family = x.attr("style:family");
                String name = x.attr("style:name");
                currentColumnStyle = null;
                if ("table-cell".equals(family) && name != null) {
                    String data = x.attr("style:data-style-name");
                    if (data != null) {
                        cellStyles.put(name, data);
                    }
                } else if ("table-column".equals(family)) {
                    currentColumnStyle = name;
                }
            } else if ("table-column-properties".equals(tag)) {
                double mm = length(x.attr("style:column-width"));
                if (currentColumnStyle != null && mm > 0) {
                    columnStyleWidths.put(currentColumnStyle, Double.valueOf(mm / OdsWriter.MM_PER_CHARACTER));
                }
            } else if ("number-style".equals(tag) || "currency-style".equals(tag) || "percentage-style".equals(tag)
                    || "date-style".equals(tag) || "time-style".equals(tag) || "text-style".equals(tag)
                    || "boolean-style".equals(tag)) {
                String name = x.attr("style:name");
                boolean elapsed = "false".equals(x.attr("number:truncate-on-overflow"));
                ArrayList<String[]> maps = new ArrayList<String[]>();
                String code = readDataStyle(x, tag, elapsed, maps);
                if (name != null && "text-style".equals(tag)) {
                    textStyles.add(name);
                }
                // an empty style is kept too: a section of a format that shows nothing
                if (name != null) {
                    dataStyles.put(name, code);
                    if (!maps.isEmpty()) {
                        styleMaps.put(name, maps);
                    }
                }
            } else if ("null-date".equals(tag)) {
                // table:case-sensitive is not read: comparisons are Excel's, without case,
                // which is also what LibreOffice writes unless a user changed it.
                String date = x.attr("table:date-value");
                book.setDate1904(date != null && date.startsWith("1904"));
            } else if ("named-range".equals(tag)) {
                String name = x.attr("table:name");
                String address = x.attr("table:cell-range-address");
                if (name != null && address != null) {
                    names.add(new String[] {name, "[" + address + "]"});
                }
            } else if ("named-expression".equals(tag)) {
                String name = x.attr("table:name");
                String expression = x.attr("table:expression");
                if (name != null && expression != null) {
                    names.add(new String[] {name, stripFormulaPrefix(expression)});
                }
            } else if ("shapes".equals(tag) || "forms".equals(tag) || "content-validations".equals(tag)
                    || "tracked-changes".equals(tag) || "database-ranges".equals(tag)
                    || "data-pilot-tables".equals(tag) || "annotation".equals(tag)) {
                x.skipElement();
            }
        }
    }

    private static String stripFormulaPrefix(String formula) {
        String f = formula;
        if (f.startsWith("of:")) {
            f = f.substring(3);
        }
        return f.startsWith("=") ? f.substring(1) : f;
    }

    /// A length such as `2.258cm` in millimetres, 0 when it cannot be read.
    private static double length(String s) {
        if (s == null || s.length() < 3) {
            return 0;
        }
        int n = s.length();
        int unit = n;
        while (unit > 0 && FormulaParser.isLetter(s.charAt(unit - 1))) {
            unit--;
        }
        double v;
        try {
            v = Double.parseDouble(s.substring(0, unit));
        } catch (NumberFormatException e) {
            return 0;
        }
        String u = s.substring(unit);
        if ("cm".equals(u)) {
            return v * 10;
        }
        if ("mm".equals(u)) {
            return v;
        }
        if ("in".equals(u)) {
            return v * 25.4;
        }
        if ("pt".equals(u)) {
            return v * 25.4 / 72;
        }
        return 0;
    }

    private void endRow() {
        if (rowHasContent && rowRepeat > 1) {
            Cell[] cells = sheet.rowCells(row);
            int copies = Math.min(rowRepeat - 1, Node.MAX_ROW - row);
            for (int i = 1; i <= copies && cells != null; i++) {
                for (int c = 0; c < cells.length; c++) {
                    Cell from = cells[c];
                    if (from == null) {
                        continue;
                    }
                    if (from.formula != null) {
                        // the value beside the formula is the first row's; it is the next row's only
                        // until that row is calculated, which a formula with no value is at once
                        sheet.loadFormula(row + i, c, FormulaParser.shift(from.formula, i, 0), from.value,
                                from.value != null, false);
                    } else if (from.value != null) {
                        sheet.storeRaw(row + i, c, from.value);
                    } else {
                        continue;
                    }
                    sheet.storeFormat(row + i, c, from.format);
                }
            }
        }
        if (groupDepth > 0) {
            // a group of empty rows is still a group; what a repeat asks for is counted
            int grouped = Math.min(rowRepeat, 4096);
            book.allocated(grouped);
            for (int i = 0; i < grouped; i++) {
                sheet.setRowOutlineLevel(row + i, groupDepth);
            }
        }
        row = (int) Math.min((long) row + rowRepeat, (long) Node.MAX_ROW + 1);
    }

    /// What a cell said about its format, which cannot be resolved until every part that may
    /// define a style has been read: the name of its style and the format its type implies.
    private final ArrayList<String[]> pendingFormats = new ArrayList<String[]>();
    private final HashMap<String, Integer> pendingIndex = new HashMap<String, Integer>();

    /// A number standing for a style and a fallback until `resolveFormats`; 0 for neither.
    private int pendingFormat(String style, int column, String fallback) {
        String name = style;
        if (name == null) {
            for (Object[] d : columnDefaults) {
                if (column >= ((Integer) d[0]).intValue() && column <= ((Integer) d[1]).intValue()) {
                    name = (String) d[2];
                    break;
                }
            }
        }
        if (name == null && fallback == null) {
            return 0;
        }
        String key = name + "\n" + fallback;
        Integer index = pendingIndex.get(key);
        if (index == null) {
            pendingFormats.add(new String[] {name, fallback});
            index = Integer.valueOf(pendingFormats.size());
            pendingIndex.put(key, index);
        }
        return index.intValue();
    }

    /// The format code of a data style, with the sections its maps name before its own: a
    /// map to a style for `value()>=0` is the first section of a code of two.
    private String sectioned(String name) {
        String own = dataStyles.get(name);
        ArrayList<String[]> maps = styleMaps.get(name);
        if (own == null || maps == null) {
            return own;
        }
        ArrayList<String> conditions = new ArrayList<String>();
        ArrayList<String> codes = new ArrayList<String>();
        // a text style sends numbers to three number styles, the sections before its own
        int most = textStyles.contains(name) ? 3 : 2;
        for (int i = 0; i < maps.size() && codes.size() < most; i++) {
            String[] m = maps.get(i);
            String code = dataStyles.get(m[1]);
            String condition = m[0];
            if (code == null || condition == null || !condition.startsWith("value()")) {
                continue;
            }
            conditions.add(condition.substring(7).trim());
            codes.add(code);
        }
        if (codes.isEmpty()) {
            return own;
        }
        StringBuilder sb = new StringBuilder();
        boolean implicit = codes.size() == 1 ? ">=0".equals(conditions.get(0))
                : ">0".equals(conditions.get(0)) && "<0".equals(conditions.get(1))
                        && (codes.size() == 2 || "=0".equals(conditions.get(2)));
        for (int i = 0; i < codes.size(); i++) {
            // a third section is what is left, which Excel's code spells with no condition
            if (!implicit && i < 2) {
                String c = conditions.get(i);
                sb.append('[').append(c.startsWith("!=") ? "<>" + c.substring(2) : c).append(']');
            }
            sb.append(codes.get(i)).append(';');
        }
        return sb.append(own).toString();
    }

    private void resolveFormats() {
        int[] ids = new int[pendingFormats.size() + 1];
        for (int i = 1; i < ids.length; i++) {
            String[] pending = pendingFormats.get(i - 1);
            String data = pending[0] == null ? null : cellStyles.get(pending[0]);
            String code = data == null ? null : sectioned(data);
            int id = code == null ? 0 : book.formatId(code);
            String fallback = pending[1];
            if (fallback != null) {
                // The type of a cell says it is a date or a percentage even when its style says
                // nothing, or names a style this reader could not turn into a format.
                CellFormat f = book.format(id);
                boolean wantsDate = fallback.charAt(0) != '0';
                if (wantsDate ? !f.isDate() : f.isGeneral()) {
                    id = book.formatId(fallback);
                }
            }
            ids[i] = id;
        }
        for (int s = 0; s < book.getSheetCount(); s++) {
            Sheet sheet = book.getSheet(s);
            for (int r = sheet.usedRows() - 1; r >= 0; r--) {
                Cell[] cells = sheet.rowCells(r);
                for (int c = 0; cells != null && c < cells.length; c++) {
                    if (cells[c] != null && cells[c].format > 0 && cells[c].format < ids.length) {
                        cells[c].format = ids[cells[c].format];
                    }
                }
            }
        }
    }

    private void readCell(XmlScanner x, boolean covered) throws IOException {
        int repeat = Math.max(1, x.intAttr("table:number-columns-repeated", 1));
        String type = x.attr("office:value-type");
        String number = x.attr("office:value");
        String date = x.attr("office:date-value");
        String time = x.attr("office:time-value");
        String bool = x.attr("office:boolean-value");
        String string = x.attr("office:string-value");
        String formula = x.attr("table:formula");
        String style = x.attr("table:style-name");
        boolean error = "error".equals(x.attr("calcext:value-type"));
        int spanColumns = x.intAttr("table:number-columns-spanned", 1);
        int spanRows = x.intAttr("table:number-rows-spanned", 1);
        boolean array = x.attr("table:number-matrix-columns-spanned") != null;
        String end = covered ? "covered-table-cell" : "table-cell";

        text.setLength(0);
        int paragraphs = 0;
        boolean inParagraph = false;
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.END) {
                if (end.equals(x.name)) {
                    break;
                }
                if ("p".equals(x.name)) {
                    inParagraph = false;
                }
            } else if (e == XmlScanner.TEXT) {
                if (inParagraph) {
                    text.append(x.text);
                }
            } else if ("annotation".equals(x.name)) {
                x.skipElement();
            } else if ("p".equals(x.name)) {
                if (paragraphs > 0) {
                    text.append('\n');
                }
                paragraphs++;
                inParagraph = true;
            } else if (inParagraph) {
                if ("s".equals(x.name)) {
                    int n = Math.min(x.intAttr("text:c", 1), XmlScanner.MAX_TEXT - text.length());
                    if (n > 0) {
                        // Spaces the file asks for take memory it never sent: two bytes each,
                        // and a cell is counted as about seventy.
                        book.allocated(n / 36);
                    }
                    for (int i = 0; i < n && text.length() < XmlScanner.MAX_TEXT; i++) {
                        text.append(' ');
                    }
                } else if ("tab".equals(x.name)) {
                    text.append('\t');
                } else if ("line-break".equals(x.name)) {
                    text.append('\n');
                }
            }
        }
        if (covered) {
            col += repeat;
            return;
        }

        Object value = null;
        String fallbackFormat = null;
        if (error) {
            CellError err = CellError.valueOf(text.toString().trim());
            value = err == null ? CellError.VALUE : err;
        } else if ("float".equals(type) || "currency".equals(type) || "percentage".equals(type)) {
            value = parseDouble(number);
            // LibreOffice has no logical cells: a truth value is the number 1 or 0 shown as a
            // word. What it shows is the only thing that tells it from a number.
            if (value instanceof Double && style == null) {
                double d = ((Double) value).doubleValue();
                String shown = text.toString();
                if (d == 1 && "TRUE".equals(shown)) {
                    value = Boolean.TRUE;
                } else if (d == 0 && "FALSE".equals(shown)) {
                    value = Boolean.FALSE;
                }
            }
            if ("percentage".equals(type)) {
                fallbackFormat = "0.00%";
            }
        } else if ("date".equals(type)) {
            Double d = date == null ? null : Values.parseDateTime(date);
            if (d != null) {
                double serial = d.doubleValue();
                if (book.isDate1904()) {
                    serial -= 1462;
                }
                value = Double.valueOf(serial);
                fallbackFormat = date.indexOf('T') > 0 ? "yyyy-mm-dd hh:mm:ss" : "yyyy-mm-dd";
            } else {
                // a date before 1900 has no day number here; what the cell shows is kept
                value = text.toString();
            }
        } else if ("time".equals(type)) {
            value = duration(time);
            fallbackFormat = "[h]:mm:ss";
        } else if ("boolean".equals(type)) {
            value = "true".equals(bool) ? Boolean.TRUE : Boolean.FALSE;
        } else if ("string".equals(type)) {
            value = string != null ? string : text.toString();
        }

        Node node = null;
        String formulaText = null;
        if (formula != null && formula.length() > 0) {
            formulaText = stripFormulaPrefix(formula);
        }
        if (spanColumns > 1 || spanRows > 1) {
            sheet.loadMergedRegion(row, col, row + spanRows - 1, col + spanColumns - 1);
        }
        if (value == null && formulaText == null) {
            col += repeat;
            return;
        }
        int format = pendingFormat(style, col, value instanceof Double ? fallbackFormat : null);
        rowHasContent = true;
        if (col > Node.MAX_COL) {
            throw new IllegalArgumentException("a cell with a value stands past the last column of a sheet");
        }
        int count = Math.min(repeat, Node.MAX_COL + 1 - col);
        for (int i = 0; i < count; i++) {
            int c = col + i;
            if (formulaText != null) {
                if (i == 0) {
                    sheet.loadFormula(row, c, formulaText, true, value, value != null, array);
                    node = sheet.cell(row, c).formula;
                } else if (node != null) {
                    sheet.loadFormula(row, c, FormulaParser.shift(node, 0, i), value, value != null, false);
                } else {
                    sheet.loadFormula(row, c, formulaText, true, value, value != null, false);
                }
            } else {
                sheet.storeRaw(row, c, value);
            }
            if (format != 0) {
                sheet.storeFormat(row, c, format);
            }
        }
        col += repeat;
    }

    private static Object parseDouble(String s) {
        if (s == null || !Values.isDecimal(s.trim())) {
            return null;
        }
        try {
            return Double.valueOf(Double.parseDouble(s.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /// A duration such as `PT13H45M10.5S` as a fraction of a day.
    static Object duration(String s) {
        if (s == null || !s.startsWith("PT")) {
            return null;
        }
        double seconds = 0;
        int start = 2;
        for (int i = 2; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == 'H' || c == 'M' || c == 'S') {
                double v;
                try {
                    v = Double.parseDouble(s.substring(start, i));
                } catch (NumberFormatException e) {
                    return null;
                }
                seconds += c == 'H' ? v * 3600 : c == 'M' ? v * 60 : v;
                start = i + 1;
            }
        }
        return Double.valueOf(seconds / 86400);
    }

    // ------------------------------------------------------------------ number formats

    private static void repeat(StringBuilder sb, char c, int count) {
        // a style is a few placeholders; a count from the file past the longest format code
        // is not written out
        int n = Math.min(count, CellFormat.MAX_CODE);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
    }

    /// Writes text of a format so that a format code shows it as it is.
    private static void literal(StringBuilder sb, String s, String plain) {
        boolean quoted = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean bare = plain.indexOf(c) >= 0;
            if (bare == quoted) {
                sb.append('"');
                quoted = !quoted;
            }
            // a quote cannot be written inside quotes; the nearest thing is two apostrophes
            sb.append(c == '"' ? "''" : String.valueOf(c));
        }
        if (quoted) {
            sb.append('"');
        }
    }

    /// The format code that says what a data style says.
    private static String readDataStyle(XmlScanner x, String styleTag, boolean elapsed, ArrayList<String[]> maps)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean date = "date-style".equals(styleTag) || "time-style".equals(styleTag);
        boolean percent = "percentage-style".equals(styleTag);
        boolean elapsedPending = elapsed;
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.END && styleTag.equals(x.name)) {
                break;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String tag = x.name;
            boolean longStyle = "long".equals(x.attr("number:style"));
            if ("map".equals(tag)) {
                maps.add(new String[] {x.attr("style:condition"), x.attr("style:apply-style-name")});
            } else if ("text".equals(tag)) {
                String s = x.elementText();
                literal(sb, s, date ? " -/:.," : percent ? " $-+()%" : " $-+()");
            } else if ("currency-symbol".equals(tag)) {
                literal(sb, x.elementText(), "$");
            } else if ("number".equals(tag) && x.attr("number:decimal-places") == null
                    && !x.boolAttr("number:grouping")
                    && x.attr("number:display-factor") == null && x.attr("number:min-decimal-places") == null) {
                // a number with nothing said about its decimals is General: LibreOffice writes
                // General so (min-integer-digits="1" and no decimal-places), as CellFormat does,
                // and writes decimal-places for every fixed format, 0 included
                sb.append("General");
            } else if ("number".equals(tag)) {
                int decimals = x.intAttr("number:decimal-places", 0);
                int minDecimals = Math.min(decimals, x.intAttr("number:min-decimal-places", decimals));
                int minInteger = x.intAttr("number:min-integer-digits", 1);
                boolean grouping = x.boolAttr("number:grouping");
                double factor = x.doubleAttr("number:display-factor", 1);
                StringBuilder whole = new StringBuilder();
                if (grouping) {
                    whole.append(minInteger == 0 ? "#,###" : "#,##");
                    repeat(whole, '0', Math.max(1, minInteger));
                } else if (minInteger == 0) {
                    whole.append('#');
                } else {
                    repeat(whole, '0', minInteger);
                }
                // text inside the digits, 000-00-0000, placed by the count of digits after it
                for (int n = x.next(); n != XmlScanner.EOF; n = x.next()) {
                    if (n == XmlScanner.END && "number".equals(x.name)) {
                        break;
                    }
                    if (n == XmlScanner.START && "embedded-text".equals(x.name)) {
                        int position = x.intAttr("number:position", 0);
                        StringBuilder text = new StringBuilder();
                        literal(text, x.elementText(), " -/()");
                        int at = whole.length();
                        for (int digits = 0; at > 0 && digits < position; at--) {
                            char ch = whole.charAt(at - 1);
                            digits += ch == '0' || ch == '#' ? 1 : 0;
                        }
                        whole.insert(at, text);
                    }
                }
                sb.append(whole);
                if (decimals > 0) {
                    sb.append('.');
                    repeat(sb, '0', minDecimals);
                    repeat(sb, '#', decimals - minDecimals);
                }
                // shown in thousands or in millions: a comma after the digits for each three
                // places the number is divided by
                for (int k = 0; k < 5 && factor >= 1000; k++) {
                    sb.append(',');
                    factor /= 1000;
                }
            } else if ("scientific-number".equals(tag)) {
                int decimals = x.intAttr("number:decimal-places", 0);
                int minInteger = Math.max(1, x.intAttr("number:min-integer-digits", 1));
                // an interval of 3 is engineering notation, ##0.0E+0: one place for each step
                repeat(sb, '#', Math.max(0, x.intAttr("number:exponent-interval", 1) - minInteger));
                repeat(sb, '0', minInteger);
                if (decimals > 0) {
                    sb.append('.');
                    repeat(sb, '0', decimals);
                }
                sb.append("E+");
                repeat(sb, '0', Math.max(1, x.intAttr("number:min-exponent-digits", 2)));
            } else if ("fraction".equals(tag)) {
                // the attribute is there, even at zero, when a whole number is written before
                // the fraction; a format that is a fraction alone leaves it out
                if (x.attr("number:min-integer-digits") != null) {
                    sb.append("# ");
                }
                repeat(sb, '?', Math.max(1, x.intAttr("number:min-numerator-digits", 1)));
                sb.append('/');
                String fixed = x.attr("number:denominator-value");
                if (fixed != null) {
                    sb.append(fixed);
                } else {
                    repeat(sb, '?', Math.max(1, x.intAttr("number:min-denominator-digits", 1)));
                }
            } else if ("year".equals(tag)) {
                sb.append(longStyle ? "yyyy" : "yy");
            } else if ("month".equals(tag)) {
                if (x.boolAttr("number:textual")) {
                    sb.append(longStyle ? "mmmm" : "mmm");
                } else {
                    sb.append(longStyle ? "mm" : "m");
                }
            } else if ("day".equals(tag)) {
                sb.append(longStyle ? "dd" : "d");
            } else if ("day-of-week".equals(tag)) {
                sb.append(longStyle ? "dddd" : "ddd");
            } else if ("hours".equals(tag)) {
                if (elapsedPending) {
                    sb.append(longStyle ? "[hh]" : "[h]");
                    elapsedPending = false;
                } else {
                    sb.append(longStyle ? "hh" : "h");
                }
            } else if ("minutes".equals(tag)) {
                sb.append(longStyle ? "mm" : "m");
            } else if ("seconds".equals(tag)) {
                sb.append(longStyle ? "ss" : "s");
                int decimals = x.intAttr("number:decimal-places", 0);
                if (decimals > 0) {
                    sb.append('.');
                    repeat(sb, '0', decimals);
                }
            } else if ("am-pm".equals(tag)) {
                sb.append("AM/PM");
            } else if ("text-content".equals(tag)) {
                sb.append('@');
            }
        }
        return sb.toString();
    }
}
