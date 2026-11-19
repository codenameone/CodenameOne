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

/// Reads an Excel workbook: a zip archive of XML parts.
///
/// The archive is read front to back, once, and nothing is kept of a part but what it says.
/// That decides the shape of this class: Excel writes the sheets before the table of strings
/// their cells point into and before the styles that say which numbers are dates, so a cell
/// is stored with the indices it was written with and given its text and its format when the
/// archive has ended. A part is recognised by its root element and not by its path, which
/// the format leaves to the program that wrote the file.
final class XlsxReader {
    private final Workbook book;
    private final long[] budget = {XmlScanner.MAX_DOCUMENT};
    private final ArrayList<String> strings = new ArrayList<String>();
    /// The number format id of every cell style, in the order cells refer to them.
    private final ArrayList<Integer> styleFormats = new ArrayList<Integer>();
    private final HashMap<Integer, String> formatCodes = new HashMap<Integer, String>();
    private final HashMap<String, Sheet> parts = new HashMap<String, Sheet>();
    private final HashMap<String, HashMap<String, String>> relations = new HashMap<String, HashMap<String, String>>();
    private final ArrayList<String> sheetNames = new ArrayList<String>();
    private final ArrayList<String> sheetRelations = new ArrayList<String>();
    private final ArrayList<String[]> definedNames = new ArrayList<String[]>();
    private final ArrayList<String[]> localNames = new ArrayList<String[]>();
    private String workbookPath;
    private final java.util.HashSet<Cell> isoDates = new java.util.HashSet<Cell>();

    XlsxReader(Workbook book) {
        this.book = book;
    }

    void read(InputStream in) throws IOException {
        ZipInputStream zip = new ZipInputStream(in);
        boolean any = false;
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            String path = entry.getName();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (entry.isDirectory()) {
                continue;
            }
            any = true;
            if (path.endsWith(".rels")) {
                readRelations(path, new XmlScanner(zip, budget));
            } else if (path.endsWith(".xml") && path.indexOf('/') >= 0) {
                readPart(path, new XmlScanner(zip, budget));
            }
            zip.closeEntry();
        }
        if (!any || workbookPath == null) {
            throw new IOException("Not an Excel workbook: the archive has no workbook part");
        }
        finish();
        if (book.getSheetCount() == 0) {
            // every sheet the workbook lists is missing or is not a worksheet
            throw new IOException("Not an Excel workbook: none of its sheets could be found");
        }
    }

    private void readPart(String path, XmlScanner x) throws IOException {
        int e = x.next();
        while (e == XmlScanner.TEXT) {
            e = x.next();
        }
        if (e != XmlScanner.START) {
            return;
        }
        if ("worksheet".equals(x.name)) {
            Sheet sheet = book.detachedSheet();
            parts.put(path, sheet);
            readSheet(sheet, x);
        } else if ("sst".equals(x.name)) {
            readStrings(x);
        } else if ("styleSheet".equals(x.name)) {
            readStyles(x);
        } else if ("workbook".equals(x.name)) {
            workbookPath = path;
            readWorkbook(x);
        }
    }

    private void readRelations(String path, XmlScanner x) throws IOException {
        HashMap<String, String> map = new HashMap<String, String>();
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START && "Relationship".equals(x.name)) {
                String id = x.attr("Id");
                String target = x.attr("Target");
                if (id != null && target != null && !"External".equals(x.attr("TargetMode"))) {
                    map.put(id, target);
                }
            }
        }
        relations.put(path, map);
    }

    private void readWorkbook(XmlScanner x) throws IOException {
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e != XmlScanner.START) {
                continue;
            }
            if ("workbookPr".equals(x.name)) {
                book.setDate1904(x.boolAttr("date1904"));
            } else if ("sheet".equals(x.name)) {
                String name = x.attr("name");
                String id = x.attr("r:id");
                if (id == null) {
                    id = x.attr("id");
                }
                sheetNames.add(name == null ? "Sheet" + (sheetNames.size() + 1) : name);
                sheetRelations.add(id);
            } else if ("definedName".equals(x.name)) {
                String name = x.attr("name");
                // there is one table of names here: a name only one sheet defines becomes a
                // workbook name, and one defined more than once (by two sheets, or by a sheet
                // and the workbook) is written into its own sheet's formulas as what it refers
                // to, so no formula silently takes another sheet's meaning
                boolean local = x.attr("localSheetId") != null;
                String text = x.elementText();
                if (name != null && !name.startsWith("_xlnm.")) {
                    if (local) {
                        localNames.add(new String[] {name, text, x.attr("localSheetId")});
                    } else {
                        definedNames.add(new String[] {name, text});
                    }
                }
            } else if ("externalReferences".equals(x.name) || "extLst".equals(x.name)) {
                x.skipElement();
            }
        }
    }

    private void readStrings(XmlScanner x) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START && "si".equals(x.name)) {
                String text = readRichText(x, sb, "si");
                // a table of strings takes memory as cells do: a few bytes of a compressed
                // file can list millions of them
                book.allocated(1 + text.length() / 16);
                strings.add(text);
            }
        }
    }

    /// The text of a string item: its `t` elements joined, without the phonetic guides East
    /// Asian text carries beside it.
    private static String readRichText(XmlScanner x, StringBuilder sb, String end) throws IOException {
        sb.setLength(0);
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.END && end.equals(x.name)) {
                break;
            }
            if (e == XmlScanner.START) {
                if ("rPh".equals(x.name) || "phoneticPr".equals(x.name)) {
                    x.skipElement();
                } else if ("t".equals(x.name)) {
                    sb.append(x.elementText());
                }
            }
        }
        return unescape(sb.toString());
    }

    /// Excel writes a character XML cannot hold as `_xHHHH_`.
    static String unescape(String s) {
        int at = s.indexOf("_x");
        if (at < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int n = s.length();
        for (int i = 0; i < n; i++) {
            if (s.charAt(i) == '_' && XmlOut.isEscape(s, i)) {
                sb.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                i += 6;
            } else {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    private void readStyles(XmlScanner x) throws IOException {
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e != XmlScanner.START) {
                continue;
            }
            if ("numFmt".equals(x.name)) {
                String code = x.attr("formatCode");
                int id = x.intAttr("numFmtId", -1);
                if (code != null && id >= 0) {
                    formatCodes.put(Integer.valueOf(id), code);
                }
            } else if ("cellStyleXfs".equals(x.name) || "dxfs".equals(x.name) || "cellStyles".equals(x.name)
                    || "extLst".equals(x.name)) {
                // these have xf and numFmt elements of their own that are not cell styles
                x.skipElement();
            } else if ("cellXfs".equals(x.name)) {
                for (e = x.next(); e != XmlScanner.EOF; e = x.next()) {
                    if (e == XmlScanner.END && "cellXfs".equals(x.name)) {
                        break;
                    }
                    if (e == XmlScanner.START && "xf".equals(x.name)) {
                        styleFormats.add(Integer.valueOf(x.intAttr("numFmtId", 0)));
                        x.skipElement();
                    }
                }
            }
        }
    }

    private static final int T_NUMBER = 0;
    private static final int T_SHARED = 1;
    private static final int T_STRING = 2;
    private static final int T_BOOLEAN = 3;
    private static final int T_ERROR = 4;
    private static final int T_INLINE = 5;
    private static final int T_DATE = 6;

    private void readSheet(Sheet sheet, XmlScanner x) throws IOException {
        HashMap<Integer, Node> sharedFormulas = new HashMap<Integer, Node>();
        HashMap<Integer, int[]> sharedOrigins = new HashMap<Integer, int[]>();
        StringBuilder sb = new StringBuilder();
        int row = -1;
        int col = -1;
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e != XmlScanner.START) {
                continue;
            }
            String tag = x.name;
            if ("row".equals(tag)) {
                row = x.intAttr("r", row + 2) - 1;
                col = -1;
                int level = x.intAttr("outlineLevel", 0);
                if (level > 0) {
                    sheet.setRowOutlineLevel(row, level);
                }
            } else if ("c".equals(tag)) {
                String ref = x.attr("r");
                if (ref == null) {
                    col++;
                } else {
                    int letters = 0;
                    while (letters < ref.length() && FormulaParser.isLetter(ref.charAt(letters))) {
                        letters++;
                    }
                    col = FormulaParser.column(ref, 0, letters);
                    if (letters < ref.length()) {
                        // the cell's own row wins over the row element's, as it does in Excel;
                        // a row past the last one is a damaged file, not row 1
                        long r = 0;
                        for (int i = letters; i < ref.length(); i++) {
                            char d = ref.charAt(i);
                            if (d < '0' || d > '9' || r > Node.MAX_ROW + 1) {
                                throw new IllegalArgumentException("No such cell: " + ref);
                            }
                            r = r * 10 + (d - '0');
                        }
                        if (r < 1 || r > Node.MAX_ROW + 1) {
                            throw new IllegalArgumentException("No such cell: " + ref);
                        }
                        row = (int) r - 1;
                    }
                    if (row < 0) {
                        row = 0;
                    }
                }
                readCell(sheet, x, row, col, sharedFormulas, sharedOrigins, sb);
            } else if ("col".equals(tag)) {
                // what Excel writes for "every column from here on" runs to the last column;
                // anything past it is not a column, and each column set is counted
                int min = Math.max(0, x.intAttr("min", 1) - 1);
                int max = Math.min(x.intAttr("max", min + 1) - 1, Node.MAX_COL);
                if (min <= max) {
                    book.allocated(max - min + 1);
                }
                String width = x.attr("width");
                if (width != null && (x.boolAttr("customWidth") || x.attr("customWidth") == null)) {
                    try {
                        double w = Double.parseDouble(width);
                        for (int c = min; c <= max; c++) {
                            sheet.setColumnWidth(c, w);
                        }
                    } catch (NumberFormatException err) {
                        // no width then
                    }
                }
            } else if ("mergeCell".equals(tag)) {
                int[] r = range(x.attr("ref"));
                if (r != null) {
                    sheet.loadMergedRegion(r[0], r[1], r[2], r[3]);
                }
            } else if ("pane".equals(tag)) {
                String state = x.attr("state");
                if ("frozen".equals(state) || "frozenSplit".equals(state)) {
                    sheet.setFrozenPanes(doubleAttr(x, "ySplit"), doubleAttr(x, "xSplit"));
                }
            } else if ("extLst".equals(tag) || "conditionalFormatting".equals(tag) || "dataValidations".equals(tag)) {
                x.skipElement();
            }
        }
    }

    private static int doubleAttr(XmlScanner x, String name) {
        String v = x.attr(name);
        if (v == null) {
            return 0;
        }
        try {
            return (int) Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /// `B2:D7` as first row, first column, last row, last column; null for anything else.
    static int[] range(String ref) {
        if (ref == null) {
            return null;
        }
        int colon = ref.indexOf(':');
        int[] a = cellRef(colon < 0 ? ref : ref.substring(0, colon));
        int[] b = colon < 0 ? a : cellRef(ref.substring(colon + 1));
        if (a == null || b == null) {
            return null;
        }
        return new int[] {a[0], a[1], b[0], b[1]};
    }

    private static int[] cellRef(String ref) {
        int n = ref.length();
        int letters = 0;
        while (letters < n && FormulaParser.isLetter(ref.charAt(letters))) {
            letters++;
        }
        if (letters == 0 || letters == n || letters > 3) {
            return null;
        }
        try {
            return new int[] {Integer.parseInt(ref.substring(letters)) - 1, FormulaParser.column(ref, 0, letters)};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void readCell(Sheet sheet, XmlScanner x, int row, int col, HashMap<Integer, Node> sharedFormulas,
            HashMap<Integer, int[]> sharedOrigins, StringBuilder sb) throws IOException {
        int style = x.intAttr("s", 0);
        String t = x.attr("t");
        int type = T_NUMBER;
        if (t != null) {
            if ("s".equals(t)) {
                type = T_SHARED;
            } else if ("str".equals(t)) {
                type = T_STRING;
            } else if ("b".equals(t)) {
                type = T_BOOLEAN;
            } else if ("e".equals(t)) {
                type = T_ERROR;
            } else if ("inlineStr".equals(t)) {
                type = T_INLINE;
            } else if ("d".equals(t)) {
                type = T_DATE;
            }
        }
        String raw = null;
        String formula = null;
        Node sharedNode = null;
        boolean array = false;
        boolean hasFormula = false;
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.END && "c".equals(x.name)) {
                break;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            if ("v".equals(x.name)) {
                raw = x.elementText();
            } else if ("is".equals(x.name)) {
                raw = readRichText(x, sb, "is");
            } else if ("f".equals(x.name)) {
                String kind = x.attr("t");
                int si = x.intAttr("si", -1);
                String text = x.elementText();
                if ("dataTable".equals(kind)) {
                    continue;
                }
                hasFormula = true;
                array = "array".equals(kind);
                if ("shared".equals(kind) && si >= 0) {
                    Integer key = Integer.valueOf(si);
                    if (text.length() > 0) {
                        formula = text;
                        try {
                            sharedFormulas.put(key, FormulaParser.parse(text, false));
                            sharedOrigins.put(key, new int[] {row, col});
                        } catch (FormulaSyntaxException err) {
                            // the cells that share it keep the values the file has for them
                            sharedFormulas.remove(key);
                        }
                    } else {
                        Node master = sharedFormulas.get(key);
                        int[] origin = sharedOrigins.get(key);
                        if (master != null && origin != null) {
                            sharedNode = FormulaParser.shift(master, row - origin[0], col - origin[1]);
                        } else {
                            hasFormula = false;
                        }
                    }
                } else if (text.length() > 0) {
                    formula = text;
                } else {
                    hasFormula = false;
                }
            } else if ("extLst".equals(x.name)) {
                x.skipElement();
            }
        }
        Object value = null;
        boolean isoDate = false;
        boolean hasValue = raw != null && (raw.length() > 0 || type == T_STRING || type == T_INLINE);
        if (hasValue) {
            switch (type) {
                case T_SHARED:
                    try {
                        // resolved when the table of strings has been read
                        value = Integer.valueOf(Integer.parseInt(raw.trim()));
                    } catch (NumberFormatException err) {
                        value = null;
                    }
                    break;
                case T_STRING:
                    value = unescape(raw);
                    break;
                case T_INLINE:
                    value = raw;
                    break;
                case T_BOOLEAN:
                    value = "1".equals(raw.trim()) || "true".equalsIgnoreCase(raw.trim()) ? Boolean.TRUE : Boolean.FALSE;
                    break;
                case T_ERROR: {
                    CellError err = CellError.valueOf(raw.trim());
                    value = err == null ? CellError.VALUE : err;
                    break;
                }
                case T_DATE: {
                    Double d = Values.parseDateTime(raw.trim());
                    value = d == null ? (Object) raw : d;
                    isoDate = d != null;
                    break;
                }
                default:
                    String number = raw.trim();
                    try {
                        value = Values.isDecimal(number) ? (Object) Double.valueOf(Double.parseDouble(number)) : raw;
                    } catch (NumberFormatException err) {
                        value = raw;
                    }
                    break;
            }
        }
        if (hasFormula) {
            if (sharedNode != null) {
                sheet.loadFormula(row, col, sharedNode, value, hasValue, false);
            } else {
                sheet.loadFormula(row, col, formula, false, value, hasValue, array);
            }
        } else if (value != null) {
            sheet.storeRaw(row, col, value);
        } else if (style == 0) {
            return;
        }
        if (isoDate) {
            isoDates.add(sheet.rowCells(row)[col]);
        }
        if (style != 0) {
            sheet.storeFormat(row, col, style);
        }
    }

    /// Gives every cell the text and the format its indices stand for, and the sheets their
    /// names and their order.
    private void finish() {
        int[] formats = new int[styleFormats.size()];
        for (int i = 0; i < formats.length; i++) {
            int id = styleFormats.get(i).intValue();
            String code = formatCodes.get(Integer.valueOf(id));
            if (code == null) {
                code = CellFormat.builtin(id);
            }
            if (code == null) {
                // the ids between the built-in ones belong to the locale of whoever wrote
                // the file: 27 to 36 and 50 to 58 are dates in the East Asian locales
                code = id >= 27 && id <= 36 || id >= 50 && id <= 58 ? "m/d/yyyy" : CellFormat.GENERAL;
            }
            formats[i] = book.formatId(code);
        }
        int slash = workbookPath.lastIndexOf('/');
        String dir = slash < 0 ? "" : workbookPath.substring(0, slash + 1);
        HashMap<String, String> rels = relations.get(dir + "_rels/" + workbookPath.substring(slash + 1) + ".rels");
        HashMap<String, Integer> definitions = new HashMap<String, Integer>();
        for (String[] d : definedNames) {
            count(definitions, d[0]);
        }
        for (String[] d : localNames) {
            count(definitions, d[0]);
        }
        for (int i = 0; i < sheetNames.size(); i++) {
            String target = rels == null ? null : rels.get(sheetRelations.get(i));
            if (target == null) {
                continue;
            }
            String path = target.startsWith("/") ? target.substring(1) : normalize(dir + target);
            Sheet sheet = parts.get(path);
            if (sheet == null) {
                // a chart sheet, a macro sheet
                continue;
            }
            parts.remove(path);
            HashMap<String, String> inline = new HashMap<String, String>();
            for (String[] d : localNames) {
                if (String.valueOf(i).equals(d[2]) && definitions.get(FormulaParser.asciiUpper(d[0])).intValue() > 1) {
                    inline.put(FormulaParser.asciiUpper(d[0]), d[1]);
                }
            }
            if (!inline.isEmpty()) {
                inlineNames(sheet, inline);
            }
            resolve(sheet, formats);
            book.attach(sheet, sheetNames.get(i));
        }
        for (String[] d : localNames) {
            if (definitions.get(FormulaParser.asciiUpper(d[0])).intValue() == 1) {
                definedNames.add(d);
            }
        }
        for (String[] d : definedNames) {
            Node node = null;
            String text = d[1];
            try {
                node = FormulaParser.parse(d[1], false);
                // a name belongs to no sheet here, so a reference that left its sheet out
                // gets one: a local name's own sheet, otherwise the first, and then follows
                // that sheet's edits
                String home = sheetFor(d.length > 2 ? d[2] : null);
                if (home != null && FormulaParser.hasSheetlessRef(node)) {
                    FormulaParser.qualify(node, home);
                    text = FormulaParser.print(node, false);
                }
            } catch (FormulaSyntaxException e) {
                // kept as text and written back; a formula that uses it gives #NAME?
                node = null;
            }
            book.defineName(d[0], text, node);
        }
    }

    private String sheetFor(String localSheetId) {
        int i = 0;
        if (localSheetId != null) {
            try {
                i = Integer.parseInt(localSheetId.trim());
            } catch (NumberFormatException e) {
                i = 0;
            }
        }
        if (i >= 0 && i < sheetNames.size()) {
            Sheet s = book.getSheet(sheetNames.get(i));
            if (s != null) {
                return s.getName();
            }
        }
        return book.getSheetCount() > 0 ? book.getSheet(0).getName() : null;
    }

    private static void count(HashMap<String, Integer> counts, String name) {
        String key = FormulaParser.asciiUpper(name);
        Integer n = counts.get(key);
        counts.put(key, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
    }

    /// Replaces each use of a sheet's own names in its formulas with what the name refers
    /// to, in parentheses. Walks with a stack: a formula can be a long chain.
    private static void inlineNames(Sheet sheet, HashMap<String, String> names) {
        ArrayList<Node> stack = new ArrayList<Node>();
        for (int r = 0; r < sheet.usedRows(); r++) {
            Cell[] row = sheet.rowCells(r);
            if (row == null) {
                continue;
            }
            for (Cell c : row) {
                if (c == null || c.formula == null) {
                    continue;
                }
                Node root = new Node(Node.PAREN, null, c.formula);
                boolean changed = false;
                // a name may use another, and one may use itself: stop after as many as a
                // name chain is followed when calculating
                int budget = 32;
                stack.add(root);
                while (!stack.isEmpty()) {
                    Node n = stack.remove(stack.size() - 1);
                    if (n.args == null) {
                        continue;
                    }
                    for (int j = 0; j < n.args.length; j++) {
                        Node a = n.args[j];
                        if (a == null) {
                            continue;
                        }
                        String text = a.kind == Node.NAME ? names.get(FormulaParser.asciiUpper(a.name())) : null;
                        Node definition = text == null || budget <= 0 ? null : parseOrNull(text);
                        if (definition != null) {
                            budget--;
                            Node inlined = new Node(Node.PAREN, null, FormulaParser.fromCell(definition, c.row, c.col));
                            n.args[j] = inlined;
                            changed = true;
                            stack.add(inlined);
                        } else {
                            stack.add(a);
                        }
                    }
                }
                if (changed) {
                    c.formula = root.args[0];
                    c.formulaText = FormulaParser.display(c.formula);
                }
            }
        }
    }

    private static Node parseOrNull(String text) {
        try {
            return FormulaParser.parse(text, false);
        } catch (FormulaSyntaxException e) {
            return null;
        }
    }

    private static String normalize(String path) {
        int at = path.indexOf("/../");
        while (at > 0) {
            int before = path.lastIndexOf('/', at - 1);
            path = path.substring(0, before + 1) + path.substring(at + 4);
            at = path.indexOf("/../");
        }
        return path;
    }

    private void resolve(Sheet sheet, int[] formats) {
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
                if (cell.value instanceof Integer) {
                    int index = ((Integer) cell.value).intValue();
                    cell.value = index >= 0 && index < strings.size() ? strings.get(index) : "";
                }
                cell.format = cell.format > 0 && cell.format < formats.length ? formats[cell.format] : 0;
                // A cell written as an ISO date says it is a date by its type alone and need
                // not have a date format; without one it would read as a bare day number.
                if (cell.value instanceof Double && isoDates.contains(cell) && book.isDate1904()
                        && ((Double) cell.value).doubleValue() >= 1462) {
                    // the text of the date was numbered from 1900
                    cell.value = Double.valueOf(((Double) cell.value).doubleValue() - 1462);
                }
                if (cell.value instanceof Double && isoDates.contains(cell) && !book.format(cell.format).isDate()) {
                    double day = ((Double) cell.value).doubleValue();
                    cell.format = book.formatId(day - Math.floor(day) > 0 ? "yyyy-mm-dd hh:mm:ss" : "yyyy-mm-dd");
                }
            }
        }
    }
}
