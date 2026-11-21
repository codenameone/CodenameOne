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

import com.codename1.spreadsheet.Sheet;
import com.codename1.spreadsheet.Workbook;

import java.util.ArrayList;
import java.util.List;

/// Moves tables between documents and spreadsheets, so a table's numbers and formulas are
/// calculated by the same engine as a `Workbook`'s.
///
/// A cell is a formula when its text starts with `=`, or when it holds a field whose instruction
/// does -- the form Word writes for a table formula, such as `= SUM(ABOVE)`. Word's position
/// words become ranges: `ABOVE` is the cells over the formula in its column, below the header
/// rows; `LEFT` the cells before it in its row; `BELOW` and `RIGHT` likewise. A number format
/// switch such as `\# "0.00"` is dropped. Every other cell holds its text, or a number when its
/// text is one.
///
/// ```java
/// Document d = TableSheets.recalculate(doc);   // every formula field shows its value
/// Sheet s = TableSheets.toSheet(table, new Workbook(), "Table 1");
/// ```
public final class TableSheets {
    private TableSheets() {
    }

    /// Whether a table holds any formula.
    ///
    /// #### Parameters
    ///
    /// - `t`: the table
    ///
    /// #### Returns
    ///
    /// true when a cell is a formula
    public static boolean hasFormulas(Table t) {
        for (int r = 0; r < t.rows(); r++) {
            for (int c = 0; c < t.columns(); c++) {
                if (formulaOf(t, r, c) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Copies a table into a new sheet of a workbook.
    ///
    /// #### Parameters
    ///
    /// - `t`: the table
    ///
    /// - `book`: the workbook
    ///
    /// - `name`: the new sheet's name
    ///
    /// #### Returns
    ///
    /// the sheet; when the table has one header row, the sheet's first row is its header
    public static Sheet toSheet(Table t, Workbook book, String name) {
        Sheet s = book.addSheet(name);
        fill(s, t);
        s.setHeaderRow(t.headerRows == 1);
        return s;
    }

    /// Writes a table's cells into an existing sheet, replacing what it held.
    static void fill(Sheet s, Table t) {
        if (s.getRowCount() > 0 || s.getColumnCount() > 0) {
            boolean header = s.isHeaderRow();
            s.setHeaderRow(false);
            for (int r = 0; r < Math.max(s.getRowCount(), t.rows()); r++) {
                for (int c = 0; c < Math.max(s.getColumnCount(), t.columns()); c++) {
                    if (r >= t.rows() || c >= t.columns()) {
                        s.setCellValue(r, c, null);
                    }
                }
            }
            s.setHeaderRow(header);
        }
        for (int r = 0; r < t.rows(); r++) {
            for (int c = 0; c < t.columns(); c++) {
                String f = formulaOf(t, r, c);
                if (f != null) {
                    try {
                        s.setCellFormula(r, c, excelFormula(f, t, r, c));
                    } catch (IllegalArgumentException e) {
                        s.setCellValue(r, c, plain(t.cell(r, c)));
                    }
                } else {
                    s.setCellValue(r, c, value(plain(t.cell(r, c))));
                }
            }
        }
        if (s.getMergedRegionCount() > 0) {
            // a sheet filled again keeps the regions it has
            return;
        }
        for (Table.Merge m : t.merges) {
            if (m.rows > 1 || m.columns > 1) {
                s.addMergedRegion(m.row, m.column, m.row + m.rows - 1, m.column + m.columns - 1);
            }
        }
    }

    /// A table made from a sheet: one paragraph per cell, formulas as fields showing their
    /// values, merged regions as merges.
    ///
    /// #### Parameters
    ///
    /// - `s`: the sheet
    ///
    /// - `ids`: the model whose document the table is for; its ids are reserved for the table
    /// and its paragraphs
    ///
    /// #### Returns
    ///
    /// the table, not yet in the document
    public static Table fromSheet(Sheet s, DocumentModel ids) {
        boolean header = s.isHeaderRow();
        s.setHeaderRow(false);
        try {
            int rows = Math.max(1, s.getRowCount());
            int cols = Math.max(1, s.getColumnCount());
            String id = ids.newId('t');
            List<List<List<Block>>> cells = new ArrayList<List<List<Block>>>();
            for (int r = 0; r < rows; r++) {
                List<List<Block>> row = new ArrayList<List<Block>>();
                for (int c = 0; c < cols; c++) {
                    List<Block> cell = new ArrayList<Block>();
                    cell.add(new Paragraph(ids.newId('p'), cellText(s, r, c, TextStyle.DEFAULT)));
                    row.add(cell);
                }
                cells.add(row);
            }
            List<Table.Merge> merges = new ArrayList<Table.Merge>();
            for (int i = 0; i < s.getMergedRegionCount(); i++) {
                int[] m = s.getMergedRegion(i);
                merges.add(new Table.Merge(m[0], m[1], m[2] - m[0] + 1, m[3] - m[1] + 1));
            }
            return new Table(id, cells, merges, header ? 1 : 0);
        } finally {
            if (header) {
                s.setHeaderRow(true);
            }
        }
    }

    /// The content a sheet's cell gives a table cell's paragraph.
    static RichText cellText(Sheet s, int r, int c, TextStyle style) {
        String f = s.getCellFormula(r, c);
        String text = s.getCellText(r, c);
        if (f != null) {
            return RichText.of(new Field(f.startsWith("=") ? f : "=" + f, text, false), style);
        }
        return RichText.of(text, style);
    }

    /// Writes a sheet's cells back into a table of the same shape, keeping each cell's
    /// paragraphs and formatting and changing only the text of what changed.
    ///
    /// #### Parameters
    ///
    /// - `t`: the table
    ///
    /// - `s`: the sheet, as `toSheet` made it and then edited
    ///
    /// #### Returns
    ///
    /// the updated table, or `t` itself when nothing changed
    public static Table update(Table t, Sheet s) {
        boolean header = s.isHeaderRow();
        // setHeaderRow tells the sheet's listeners, and a TableBinding answers by calling this
        // again: only touch it when it has to change
        if (header) {
            s.setHeaderRow(false);
        }
        try {
            Table out = t;
            for (int r = 0; r < t.rows(); r++) {
                for (int c = 0; c < t.columns(); c++) {
                    List<Block> cell = out.cell(r, c);
                    List<Block> updated = updateCell(cell, s, r, c);
                    if (updated != cell) { //NOPMD CompareObjectsWithEquals - updateCell returns the same list when nothing changed
                        out = out.withCell(r, c, updated);
                    }
                }
            }
            return out;
        } finally {
            if (header) {
                s.setHeaderRow(true);
            }
        }
    }

    private static List<Block> updateCell(List<Block> cell, Sheet s, int r, int c) {
        Paragraph first = null;
        int index = -1;
        for (int i = 0; i < cell.size(); i++) {
            if (cell.get(i) instanceof Paragraph) {
                first = (Paragraph) cell.get(i);
                index = i;
                break;
            }
        }
        String formula = s.getCellFormula(r, c);
        String text = s.getCellText(r, c);
        if (first == null) {
            // a cell of pictures or nested tables has no text to change
            return cell;
        }
        Field field = fieldOf(first.text);
        RichText next;
        if (field != null && formula != null) {
            if (text.equals(field.result)) {
                return cell;
            }
            int at = first.text.plainText().indexOf(RichText.OBJECT);
            next = first.text.withObject(at, field.withResult(text));
        } else {
            if (formula == null && sameValue(plain(cell), text, s.getCellValue(r, c))) {
                return cell;
            }
            TextStyle style = first.text.isEmpty() ? TextStyle.DEFAULT : first.text.styleAt(0);
            next = cellText(s, r, c, style);
        }
        List<Block> out = new ArrayList<Block>(cell);
        out.set(index, first.withText(next));
        return out;
    }

    /// Calculates every table formula in a document and shows each value in its field.
    ///
    /// #### Parameters
    ///
    /// - `d`: the document
    ///
    /// #### Returns
    ///
    /// the document with current values, or `d` itself when none changed
    public static Document recalculate(Document d) {
        List<Block> out = null;
        for (int i = 0; i < d.size(); i++) {
            Block b = d.get(i);
            if (b instanceof Table && hasFormulas((Table) b)) {
                Table t = (Table) b;
                Sheet s = toSheet(t, new Workbook(), "T");
                s.getWorkbook().recalculate();
                Table u = update(t, s);
                if (u != t) { //NOPMD CompareObjectsWithEquals - update returns the same table when nothing changed
                    if (out == null) {
                        out = new ArrayList<Block>(d.blocks);
                    }
                    out.set(i, u);
                }
            }
        }
        return out == null ? d : d.withBlocks(out);
    }

    // ------------------------------------------------------------------ formulas

    /// The formula of a cell, with its leading `=`, or null.
    static String formulaOf(Table t, int r, int c) {
        List<Block> cell = t.cell(r, c);
        for (Block b : cell) {
            if (b instanceof Paragraph) {
                Paragraph p = (Paragraph) b;
                Field f = fieldOf(p.text);
                if (f != null && f.instruction.trim().startsWith("=")) {
                    return f.instruction.trim();
                }
                String s = p.text.plainText().trim();
                if (s.length() > 1 && s.charAt(0) == '=') {
                    return s;
                }
                return null;
            }
        }
        return null;
    }

    private static Field fieldOf(RichText t) {
        for (int i = 0; i < t.length(); i++) {
            Inline o = t.objectAt(i);
            if (o instanceof Field) {
                return (Field) o;
            }
        }
        return null;
    }

    /// A Word table formula in the spreadsheet engine's syntax, for the cell at `row`,
    /// `column`.
    ///
    /// #### Parameters
    ///
    /// - `formula`: the formula, with or without `=`
    ///
    /// - `t`: the table it is in
    ///
    /// - `row`: its row
    ///
    /// - `column`: its column
    ///
    /// #### Returns
    ///
    /// the formula with a leading `=`
    public static String excelFormula(String formula, Table t, int row, int column) {
        String f = formula.trim();
        if (f.startsWith("=")) {
            f = f.substring(1).trim();
        }
        int sw = f.indexOf('\\');
        if (sw >= 0) {
            f = f.substring(0, sw).trim();
        }
        StringBuilder out = new StringBuilder("=");
        int i = 0;
        boolean quoted = false;
        while (i < f.length()) {
            char ch = f.charAt(i);
            if (ch == '"') {
                quoted = !quoted;
            }
            if (!quoted && Character.isLetter(ch) && (i == 0 || !isWordChar(f.charAt(i - 1)))) {
                int j = i;
                while (j < f.length() && isWordChar(f.charAt(j))) {
                    j++;
                }
                String word = f.substring(i, j);
                String range = j < f.length() && f.charAt(j) == '(' ? null
                        : positionRange(word, t, row, column);
                out.append(range != null ? range : word);
                i = j;
                continue;
            }
            out.append(ch);
            i++;
        }
        return out.toString();
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.';
    }

    private static String positionRange(String word, Table t, int row, int column) {
        int r0;
        int c0;
        int r1;
        int c1;
        if ("ABOVE".equalsIgnoreCase(word)) {
            r0 = t.headerRows;
            r1 = row - 1;
            c0 = column;
            c1 = column;
        } else if ("BELOW".equalsIgnoreCase(word)) {
            r0 = row + 1;
            r1 = t.rows() - 1;
            c0 = column;
            c1 = column;
        } else if ("LEFT".equalsIgnoreCase(word)) {
            r0 = row;
            r1 = row;
            c0 = 0;
            c1 = column - 1;
        } else if ("RIGHT".equalsIgnoreCase(word)) {
            r0 = row;
            r1 = row;
            c0 = column + 1;
            c1 = t.columns() - 1;
        } else {
            return null;
        }
        if (r1 < r0 || c1 < c0) {
            // nothing there: a reference to an empty cell far away sums to nothing
            return "XFD1048576";
        }
        return address(r0, c0) + ":" + address(r1, c1);
    }

    /// The spreadsheet address of a cell, such as `B3` for row 2, column 1.
    ///
    /// #### Parameters
    ///
    /// - `row`: from zero
    ///
    /// - `column`: from zero
    ///
    /// #### Returns
    ///
    /// the address
    public static String address(int row, int column) {
        StringBuilder letters = new StringBuilder();
        int c = column + 1;
        while (c > 0) {
            int m = (c - 1) % 26;
            letters.insert(0, (char) ('A' + m));
            c = (c - 1) / 26;
        }
        return letters.toString() + (row + 1);
    }

    // ------------------------------------------------------------------ values

    /// The plain text of a cell's blocks, paragraphs joined by spaces.
    static String plain(List<Block> cell) {
        StringBuilder b = new StringBuilder();
        for (Block x : cell) {
            if (x instanceof Paragraph) {
                if (b.length() > 0) {
                    b.append(' ');
                }
                b.append(((Paragraph) x).text.plainText());
            }
        }
        return b.toString().trim();
    }

    /// A number when the text is one (thousands separators and a trailing percent allowed),
    /// the text otherwise, null when empty.
    /// Whether a cell's text already says what the sheet holds: "1,200" and a sheet showing
    /// 1200 are the same number, and rewriting the cell would lose the author's formatting
    /// (and, for a binding, echo every edit back as a second change).
    private static boolean sameValue(String cellText, String sheetText, Object sheetValue) {
        if (cellText.equals(sheetText)) {
            return true;
        }
        Object v = value(cellText);
        return v instanceof Double && sheetValue instanceof Number
                && ((Double) v).doubleValue() == ((Number) sheetValue).doubleValue();
    }

    static Object value(String text) {
        if (text.length() == 0) {
            return null;
        }
        String s = text;
        boolean percent = s.endsWith("%");
        if (percent) {
            s = s.substring(0, s.length() - 1).trim();
        }
        StringBuilder digits = new StringBuilder();
        boolean seenDigit = false;
        boolean seenDot = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                seenDigit = true;
                digits.append(c);
            } else if (c == '.' && !seenDot) {
                seenDot = true;
                digits.append(c);
            } else if (c == ',' && seenDigit && !seenDot) {
                continue;
            } else if ((c == '-' || c == '+') && i == 0) {
                digits.append(c);
            } else {
                return text;
            }
        }
        if (!seenDigit) {
            return text;
        }
        try {
            double d = Double.parseDouble(digits.toString());
            return Double.valueOf(percent ? d / 100 : d);
        } catch (NumberFormatException e) {
            return text;
        }
    }
}
