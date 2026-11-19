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

import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.GridColumn;
import com.codename1.ui.table.TableModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/// A spreadsheet: sheets of cells that hold numbers, text, dates, logical values and
/// formulas, read from and written to Excel (`.xlsx`), OpenDocument (`.ods`), CSV and TSV
/// files.
///
/// Every [Sheet] is a [TableModel], so a file is three lines away from a grid:
///
/// ```java
/// Workbook book = Workbook.read(input, Workbook.Format.XLSX);
/// Sheet sheet = book.getSheet(0);
/// sheet.setHeaderRow(true);
/// DataGrid grid = new DataGrid(sheet);
/// grid.setColumns(sheet.createColumns());
/// ```
///
/// and a grid one line from a file:
///
/// ```java
/// Workbook.fromGrid(grid).write(output, Workbook.Format.XLSX);
/// ```
///
/// #### Formulas
///
/// A cell whose content starts with `=` is a formula in Excel's syntax. Formulas are
/// calculated here, on the device: changing a cell recalculates every cell that depends on
/// it, and the sheet tells its listeners which cells that was. A formula read from a file
/// keeps the value the file stored for it until something it depends on changes.
///
/// A function this implementation does not have evaluates to `#NAME?`; the formula itself is
/// kept and written back as it was read. [#getSupportedFunctions()] lists the functions
/// there are.
///
/// #### Threads
///
/// A workbook is not thread safe. Use it from one thread at a time; one that a grid shows
/// belongs to the event dispatch thread.
public final class Workbook {
    /// A file format a workbook is read from or written to.
    public enum Format {
        /// Excel's format, a zip of XML parts.
        XLSX,
        /// The OpenDocument spreadsheet LibreOffice and others write.
        ODS,
        /// Text with a comma, or another separator that is detected, between values. One
        /// sheet, values only.
        CSV,
        /// Text with a tab between values.
        TSV;

        /// Tells the format of a file from its name and its first bytes. The content wins:
        /// a zip archive is a spreadsheet whatever the file is called.
        ///
        /// #### Parameters
        ///
        /// - `fileName`: the name of the file, may be null
        ///
        /// - `head`: the first bytes of the file, as many as are at hand; may be null
        ///
        /// #### Returns
        ///
        /// the format, CSV when nothing says otherwise
        public static Format detect(String fileName, byte[] head) {
            String name = fileName == null ? "" : fileName;
            boolean ods = endsWith(name, ".ods");
            if (head != null && head.length >= 4 && head[0] == 'P' && head[1] == 'K') {
                // an OpenDocument archive starts with an uncompressed entry that names its type
                String start = latin1(head);
                if (start.indexOf("opendocument.spreadsheet") >= 0) {
                    return ODS;
                }
                // An archive that does not say: the parts of an Excel workbook give it away,
                // and failing that the name of the file is the better witness.
                if (start.indexOf("[Content_Types]") >= 0 || start.indexOf("xl/") >= 0 || !ods) {
                    return XLSX;
                }
                return ODS;
            }
            if (ods) {
                return ODS;
            }
            if (endsWith(name, ".xlsx") || endsWith(name, ".xlsm")) {
                return XLSX;
            }
            if (endsWith(name, ".tsv") || endsWith(name, ".tab")) {
                return TSV;
            }
            return CSV;
        }

        private static boolean endsWith(String name, String suffix) {
            int n = name.length();
            int m = suffix.length();
            return n >= m && name.regionMatches(true, n - m, suffix, 0, m);
        }

        private static String latin1(byte[] head) {
            StringBuilder sb = new StringBuilder(head.length);
            int n = Math.min(head.length, 256);
            for (int i = 0; i < n; i++) {
                sb.append((char) (head[i] & 0xff));
            }
            return sb.toString();
        }
    }

    /// The most cells of one reference that are indexed one by one. A larger range is kept
    /// as a rectangle and tested when a cell changes.
    private static final int POINT_LIMIT = 64;

    private final ArrayList<Sheet> sheets = new ArrayList<Sheet>();
    private final ArrayList<String> names = new ArrayList<String>();
    private final HashMap<String, Node> nameNodes = new HashMap<String, Node>();
    private final HashMap<String, String> nameFormulas = new HashMap<String, String>();
    private final ArrayList<CellFormat> formats = new ArrayList<CellFormat>();
    private final HashMap<String, Integer> formatIndex = new HashMap<String, Integer>();
    private final Evaluator evaluator = new Evaluator(this);
    private boolean date1904;
    /// Set while a file is read: cells change and nobody is told.
    boolean silent;

    // Which formulas read which cells. Entries are only ever added: one left behind by a
    // formula that changed marks a cell stale that need not be, which costs a calculation
    // and never a wrong value.
    private HashMap<Long, ArrayList<Cell>> pointReaders;
    private ArrayList<Cell> rangeReaders;
    private ArrayList<Ref> rangeRefs;
    private final ArrayList<Cell> volatileCells = new ArrayList<Cell>();
    // the cells being calculated, outermost first
    private final ArrayList<Cell> visiting = new ArrayList<Cell>();
    /// The names of the sheets, folded, while a file is read; see attach.
    private HashMap<String, Integer> attachedNames;
    /// The longest sheet name read from a file. OpenDocument has no limit and Excel stops at
    /// 31; this is far past any real name and keeps a hostile one from reaching every
    /// formula that is printed.
    private static final int MAX_SHEET_NAME = 255;

    private static long cellLimit = 1000000L;
    private long allocatedCells;
    private boolean loading;
    private boolean excelLimitsEnforced = true;

    /// Creates a workbook with no sheets.
    public Workbook() {
        formats.add(new CellFormat(CellFormat.GENERAL));
        formatIndex.put(CellFormat.GENERAL, Integer.valueOf(0));
    }

    // ------------------------------------------------------------------ reading and writing

    /// Reads a workbook.
    ///
    /// #### Parameters
    ///
    /// - `in`: the file; read to its end and not closed
    ///
    /// - `format`: the format of the file, see [Format#detect(String, byte\[\])]
    ///
    /// #### Returns
    ///
    /// the workbook
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the stream fails or the file is not of that format
    public static Workbook read(InputStream in, Format format) throws IOException {
        Workbook book = new Workbook();
        book.loading = true;
        try {
            switch (format) {
                case XLSX:
                    new XlsxReader(book).read(in);
                    break;
                case ODS:
                    new OdsReader(book).read(in);
                    break;
                case TSV:
                    CsvFiles.read(book, in, '\t', false);
                    break;
                default:
                    CsvFiles.read(book, in, ',', true);
                    break;
            }
        } catch (IllegalArgumentException e) {
            // a cell outside what a sheet holds, or more cells than are read: the file is at
            // fault, and a file at fault is what IOException says
            throw new IOException("Cannot read the spreadsheet: " + e.getMessage()); //NOPMD PreserveStackTrace - the device library has no constructor that takes a cause
        } finally {
            book.loading = false;
            book.attachedNames = null;
        }
        book.rebuildReaders();
        return book;
    }

    /// Counts the places for cells a sheet has just made room for. A file is not trusted
    /// with memory: a few hundred bytes can name a cell in the last column of every row, or
    /// say that one cell repeats a million times.
    void allocated(long cells) {
        allocatedCells += cells;
        if (loading && allocatedCells > cellLimit) {
            throw new IllegalArgumentException("it has more cells than a workbook is read with, " + cellLimit
                    + "; see Workbook.setCellLimit");
        }
    }

    /// The most cells `read` fills before it gives up on a file with an `IOException`,
    /// one million unless it is changed. A million numbers take about 75 MB.
    ///
    /// A file is not trusted with memory: a few hundred bytes of a compressed file can name
    /// a cell in the last column of every row, or say that one row repeats a million times.
    /// What is counted is the room the rows take, so a single cell far to the right counts
    /// as every cell to its left, and the rows above the last one count too. A formula
    /// counts as eight cells and more for a long one, and the shared strings, the column
    /// widths and the spaces a file asks for are counted with the cells.
    ///
    /// #### Returns
    ///
    /// the limit
    public static long getCellLimit() {
        return cellLimit;
    }

    /// Changes the most cells `read` fills, for an application that reads larger files and
    /// runs where there is memory for them.
    ///
    /// #### Parameters
    ///
    /// - `limit`: the most cells
    public static void setCellLimit(long limit) {
        cellLimit = Math.max(1, limit);
    }

    /// Writes the workbook. Formulas are written with the values they have now, so that a
    /// program that does not calculate still shows them.
    ///
    /// CSV and TSV hold one sheet, the first, as the text each cell shows; formulas are
    /// written as their values.
    ///
    /// #### Parameters
    ///
    /// - `out`: where to write; flushed and not closed
    ///
    /// - `format`: the format to write
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the stream fails
    public void write(OutputStream out, Format format) throws IOException {
        recalculateStale();
        switch (format) {
            case XLSX:
                new XlsxWriter(this).write(out);
                break;
            case ODS:
                new OdsWriter(this).write(out);
                break;
            case TSV:
                CsvFiles.write(this, out, '\t');
                break;
            default:
                CsvFiles.write(this, out, ',');
                break;
        }
        out.flush();
    }

    /// Creates a workbook with one sheet that holds what a grid shows: its rows in the order
    /// they are sorted and filtered to, its visible columns in the order they are arranged
    /// in, and the column titles as a header row.
    ///
    /// Values keep their types -- a number is a number in the file, a date a date -- and are
    /// never the text the grid formats them to. The depth of a row of a tree grid becomes its
    /// outline level, and the grid's frozen columns stay frozen.
    ///
    /// #### Parameters
    ///
    /// - `grid`: the grid to copy
    ///
    /// #### Returns
    ///
    /// a new workbook that shares nothing with the grid
    public static Workbook fromGrid(DataGrid grid) {
        Workbook book = new Workbook();
        Sheet sheet = book.addSheet("Sheet1");
        GridColumn[] columns = grid.getVisibleColumns();
        for (int c = 0; c < columns.length; c++) {
            sheet.store(0, c, columns[c].getTitle());
            float mm = columns[c].getWidthMM();
            if (mm > 0) {
                sheet.setColumnWidth(c, mm / Sheet.MM_PER_CHARACTER);
            }
        }
        int rows = grid.getRowCount();
        boolean tree = grid.isTree();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns.length; c++) {
                sheet.store(r + 1, c, grid.getValueAt(r, columns[c]));
            }
            if (tree) {
                sheet.setRowOutlineLevel(r + 1, grid.getDepth(r));
            }
        }
        sheet.setHeaderRow(true);
        sheet.setFrozenPanes(1, grid.getFrozenColumns());
        return book;
    }

    /// Creates a workbook with one sheet that holds the rows and columns of a model, with
    /// the column names as a header row.
    ///
    /// #### Parameters
    ///
    /// - `model`: the model to copy
    ///
    /// #### Returns
    ///
    /// a new workbook that shares nothing with the model
    public static Workbook fromModel(TableModel model) {
        Workbook book = new Workbook();
        Sheet sheet = book.addSheet("Sheet1");
        int columns = model.getColumnCount();
        for (int c = 0; c < columns; c++) {
            sheet.store(0, c, model.getColumnName(c));
        }
        int rows = model.getRowCount();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                sheet.store(r + 1, c, model.getValueAt(r, c));
            }
        }
        sheet.setHeaderRow(true);
        sheet.setFrozenPanes(1, 0);
        return book;
    }

    /// The names of the functions formulas can use, in alphabetical order.
    ///
    /// #### Returns
    ///
    /// a new array
    public static String[] getSupportedFunctions() {
        return Functions.names();
    }

    // ------------------------------------------------------------------ sheets

    /// Adds an empty sheet after the others.
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the sheet, which formulas on other sheets use to refer to it.
    ///   Excel takes names of up to 31 characters, and writing a workbook with a longer one as
    ///   XLSX throws `IOException` unless [#setExcelLimitsEnforced(boolean)] turns that off
    ///
    /// #### Returns
    ///
    /// the new sheet
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the workbook has a sheet of that name
    public Sheet addSheet(String name) {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("A sheet needs a name");
        }
        // What no spreadsheet accepts in the name of a sheet, because a formula could not
        // refer to it. The 31 characters Excel stops at are not enforced: OpenDocument has no
        // such limit, and a name is not shortened behind the caller's back.
        for (int i = 0; i < name.length(); i++) {
            if ("[]*?:/\\".indexOf(name.charAt(i)) >= 0) {
                throw new IllegalArgumentException("A sheet name cannot contain " + name.charAt(i));
            }
        }
        if (name.charAt(0) == '\'' || name.charAt(name.length() - 1) == '\'') {
            throw new IllegalArgumentException("A sheet name cannot start or end with an apostrophe");
        }
        if (getSheet(name) != null) {
            throw new IllegalArgumentException("The workbook has a sheet named " + name);
        }
        Sheet s = new Sheet(this, name);
        sheets.add(s);
        structureChanged();
        return s;
    }

    /// Removes a sheet. Formulas that refer to it evaluate to `#REF!` from then on.
    ///
    /// #### Parameters
    ///
    /// - `index`: the position of the sheet
    public void removeSheet(int index) {
        sheets.remove(index);
        structureChanged();
        // what was read from the sheet is an error now, and nothing else would say so
        recalculate();
    }

    /// The number of sheets.
    ///
    /// #### Returns
    ///
    /// the count
    public int getSheetCount() {
        return sheets.size();
    }

    /// The sheet at a position.
    ///
    /// #### Parameters
    ///
    /// - `index`: the position, from 0
    ///
    /// #### Returns
    ///
    /// the sheet
    public Sheet getSheet(int index) {
        return sheets.get(index);
    }

    /// The sheet with a name, ignoring case.
    ///
    /// #### Parameters
    ///
    /// - `name`: the name
    ///
    /// #### Returns
    ///
    /// the sheet, or null when there is none of that name
    public Sheet getSheet(String name) {
        for (Sheet each : sheets) {
            if (each.getName().equalsIgnoreCase(name)) {
                return each;
            }
        }
        return null;
    }

    /// Adds a sheet a file reader filled before it knew the sheet's name.
    void attach(Sheet s, String name) {
        // A file names its sheets as it likes; what addSheet refuses is refused here too, by
        // replacing the character, so that every sheet can be named by a formula and is
        // written back as a name another spreadsheet accepts.
        StringBuilder sb = new StringBuilder(name == null ? "" : name.trim());
        for (int i = 0; i < sb.length(); i++) {
            if ("[]*?:/\\".indexOf(sb.charAt(i)) >= 0) {
                sb.setCharAt(i, '_');
            }
        }
        while (sb.length() > 0 && sb.charAt(0) == '\'') {
            sb.deleteCharAt(0);
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\'') {
            sb.deleteCharAt(sb.length() - 1);
        }
        if (sb.length() > MAX_SHEET_NAME) {
            sb.setLength(MAX_SHEET_NAME);
        }
        String base = sb.length() == 0 ? "Sheet" + (sheets.size() + 1) : sb.toString();
        // Names already taken are kept in a set while a file is read: a file of thousands
        // of sheets that share one name is otherwise a search through every sheet for every
        // suffix of every sheet.
        if (attachedNames == null) {
            attachedNames = new HashMap<String, Integer>();
            for (Sheet each : sheets) {
                attachedNames.put(FormulaParser.asciiUpper(each.getName()), Integer.valueOf(1));
            }
        }
        String unique = base;
        String key = FormulaParser.asciiUpper(base);
        Integer taken = attachedNames.get(key);
        if (taken != null) {
            int n = taken.intValue() + 1;
            attachedNames.put(key, Integer.valueOf(n));
            unique = base + " (" + n + ")";
            while (attachedNames.containsKey(FormulaParser.asciiUpper(unique))) {
                n++;
                unique = base + " (" + n + ")";
            }
            attachedNames.put(key, Integer.valueOf(n));
        }
        attachedNames.put(FormulaParser.asciiUpper(unique), Integer.valueOf(1));
        s.rename(unique);
        sheets.add(s);
    }

    Sheet detachedSheet() {
        return new Sheet(this, "");
    }

    int indexOf(Sheet s) {
        return sheets.indexOf(s);
    }

    /// Whether writing XLSX refuses what Excel cannot open as it is.
    ///
    /// #### Returns
    ///
    /// true, the default, when a write past Excel's limits throws
    public boolean isExcelLimitsEnforced() {
        return excelLimitsEnforced;
    }

    /// Sets whether writing XLSX refuses what Excel cannot open as it is: a sheet name over 31
    /// characters, a formula or what a defined name refers to over 8,192 (counted as the file holds
    /// it, with the prefixes newer functions take) or a text over 32,767. By default such a write throws
    /// `IOException` naming the sheet or the cell, because Excel opens such a file only by
    /// repairing it, and drops or cuts what is over the limit. With false the file is written
    /// as it is, which suits a file meant for a spreadsheet that has no such limits.
    ///
    /// #### Parameters
    ///
    /// - `enforced`: false to write past the limits
    public void setExcelLimitsEnforced(boolean enforced) {
        excelLimitsEnforced = enforced;
    }

    /// Whether day numbers count from 1904, as files from old Macintosh versions of Excel
    /// do, and not from 1900.
    ///
    /// #### Returns
    ///
    /// true for the 1904 date system
    public boolean isDate1904() {
        return date1904;
    }

    /// Sets the date system. Changing it moves every date in the workbook by four years.
    ///
    /// #### Parameters
    ///
    /// - `date1904`: true to count days from 1904
    public void setDate1904(boolean date1904) {
        if (this.date1904 == date1904) {
            return;
        }
        this.date1904 = date1904;
        if (!loading) {
            // every function of a date answers differently now
            recalculate();
        }
    }

    // ------------------------------------------------------------------ names

    /// Defines a name formulas can use in place of a reference or a constant, as in
    /// `=SUM(Sales)` or `=Price*TaxRate`.
    ///
    /// #### Parameters
    ///
    /// - `name`: the name; replaces a definition of the same name, ignoring case
    ///
    /// - `formula`: what the name stands for, such as `Sheet1!$A$1:$A$10` or `0.17`; null
    ///   removes the name
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the formula cannot be read
    public void setDefinedName(String name, String formula) {
        String key = FormulaParser.asciiUpper(name);
        if (formula == null) {
            if (nameNodes.remove(key) != null || nameFormulas.remove(key) != null) {
                for (int i = 0; i < names.size(); i++) {
                    if (names.get(i).equalsIgnoreCase(name)) {
                        names.remove(i);
                        break;
                    }
                }
            }
            nameFormulas.remove(key);
        } else {
            if (!FormulaParser.isName(name)) {
                throw new IllegalArgumentException("Not a name a formula can use: " + name);
            }
            String text = formula.startsWith("=") ? formula.substring(1) : formula;
            Node node;
            try {
                node = FormulaParser.parse(text, false);
            } catch (FormulaSyntaxException e) {
                throw new IllegalArgumentException("Cannot read the formula of " + name + ": " + e.getMessage()); //NOPMD PreserveStackTrace - the device library has no constructor that takes a cause
            }
            defineName(name, text, node);
        }
        structureChanged();
        markAllStale();
        if (!loading) {
            for (int s = 0; s < sheets.size(); s++) { //NOPMD ForLoopCanBeForeach - a listener may add a sheet while they are told
                sheets.get(s).fireAll();
            }
        }
    }

    void defineName(String name, String text, Node node) {
        String key = FormulaParser.asciiUpper(name);
        if (!nameFormulas.containsKey(key)) {
            names.add(name);
        }
        nameFormulas.put(key, text);
        if (node == null) {
            nameNodes.remove(key);
        } else {
            nameNodes.put(key, node);
        }
    }

    /// What a defined name stands for.
    ///
    /// #### Parameters
    ///
    /// - `name`: the name, ignoring case
    ///
    /// #### Returns
    ///
    /// the formula without a leading `=`, or null when the name is not defined
    public String getDefinedName(String name) {
        return nameFormulas.get(FormulaParser.asciiUpper(name));
    }

    /// The defined names, in the order they were defined.
    ///
    /// #### Returns
    ///
    /// a new array
    public String[] getDefinedNames() {
        String[] out = new String[names.size()];
        names.toArray(out);
        return out;
    }

    Node definedNameNode(String name) {
        return nameNodes.get(FormulaParser.asciiUpper(name));
    }

    // ------------------------------------------------------------------ formats

    /// The index of a format code in this workbook's table, added when it is new.
    int formatId(String code) {
        if (code == null || code.length() == 0) {
            return 0;
        }
        Integer id = formatIndex.get(code);
        if (id == null) {
            id = Integer.valueOf(formats.size());
            formats.add(new CellFormat(code));
            formatIndex.put(code, id);
        }
        return id.intValue();
    }

    CellFormat format(int id) {
        return formats.get(id < 0 || id >= formats.size() ? 0 : id);
    }

    int formatCount() {
        return formats.size();
    }

    // ------------------------------------------------------------------ calculation

    /// Calculates every formula again, whether or not anything it reads has changed. A
    /// formula that could not be read keeps the value its file stored.
    public void recalculate() {
        markAllStale();
        recalculateStale();
        for (int i = 0; i < sheets.size(); i++) { //NOPMD ForLoopCanBeForeach - a listener may add a sheet while they are told
            sheets.get(i).fireAll();
        }
    }

    /// Rows or columns of a sheet were inserted or removed: every formula and name that
    /// refers to the cells that moved is rewritten to follow them.
    void restructured(Sheet edited, boolean rows, int at, int delta) {
        for (Sheet sheet : sheets) {
            int count = sheet.usedRows();
            for (int r = 0; r < count; r++) {
                Cell[] cells = sheet.rowCells(r);
                if (cells == null) {
                    continue;
                }
                for (Cell cell : cells) {
                    if (cell == null || cell.formula == null) {
                        continue;
                    }
                    Node moved = FormulaParser.restructure(cell.formula, sheet == edited, edited.getName(), rows, at, //NOPMD CompareObjectsWithEquals - a sheet is an identity
                            delta);
                    if (moved != cell.formula) { //NOPMD CompareObjectsWithEquals - the same object is meant, not an equal one
                        cell.formula = moved;
                        cell.formulaText = FormulaParser.display(moved);
                    }
                }
            }
        }
        for (String nameItem : names) {
            String key = FormulaParser.asciiUpper(nameItem);
            Node node = nameNodes.get(key);
            if (node != null) {
                Node moved = FormulaParser.restructure(node, false, edited.getName(), rows, at, delta);
                if (moved != node) { //NOPMD CompareObjectsWithEquals - the same object is meant, not an equal one
                    nameNodes.put(key, moved);
                    nameFormulas.put(key, FormulaParser.display(moved));
                }
            }
        }
        structureChanged();
        markAllStale();
        for (int s = 0; s < sheets.size(); s++) { //NOPMD ForLoopCanBeForeach - a listener may add a sheet while they are told
            sheets.get(s).fireAll();
        }
    }

    /// Whether a formula calls a function this package does not have.
    static boolean callsUnknown(Node node, int depth) {
        Node n = node;
        if (n == null || depth > FormulaParser.MAX_DEPTH * 4) {
            return false;
        }
        // down the left of a chain of operators in a loop: see Node.spine
        while (n.kind == Node.BINARY) {
            if (callsUnknown(n.args[1], depth + 1)) {
                return true;
            }
            n = n.args[0];
        }
        if (n.kind == Node.FUNC && n.value instanceof String
                && Functions.info(FormulaParser.asciiUpper((String) n.value)) < 0) {
            return true;
        }
        if (n.args != null) {
            for (Node arg : n.args) {
                if (callsUnknown(arg, depth + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void markAllStale() {
        for (Sheet sheet : sheets) {
            int rows = sheet.usedRows();
            for (int r = 0; r < rows; r++) {
                Cell[] cells = sheet.rowCells(r);
                if (cells == null) {
                    continue;
                }
                for (int c = 0; c < cells.length; c++) {
                    // nothing a cell reads has changed here, so one that calls a function
                    // this package does not have keeps the value its file stored
                    if (cells[c] != null && cells[c].formula != null && !cells[c].keepsCached) {
                        cells[c].state = Cell.DIRTY;
                    }
                }
            }
        }
    }

    private void recalculateStale() {
        for (Sheet sheet : sheets) {
            int rows = sheet.usedRows();
            for (int r = 0; r < rows; r++) {
                Cell[] cells = sheet.rowCells(r);
                if (cells == null) {
                    continue;
                }
                for (Cell cellItem : cells) {
                    if (cellItem != null && cellItem.state != Cell.CLEAN) {
                        calculate(cellItem);
                    }
                }
            }
        }
    }

    /// The value of a cell, calculated first when something it reads has changed since it
    /// was. A cell that is being calculated right now is reading itself: that is a circular
    /// reference.
    Object valueOf(Cell c) {
        if (c == null) {
            return null;
        }
        if (c.state != Cell.CLEAN) {
            if (c.state == Cell.VISITING) {
                circle(c);
                return CellError.CIRCULAR;
            }
            calculate(c);
        }
        return c.value;
    }

    /// A cell that is being calculated was read again: it and every cell entered since are
    /// a circle, and each of them is the error whatever its formula makes of what it read.
    /// Without this `=COUNT(B1)+1` beside `=A1+1` is a number or an error depending on
    /// which of the two is looked at first, since COUNT does not mind an error.
    private void circle(Cell read) {
        for (int i = visiting.size() - 1; i >= 0; i--) {
            Cell v = visiting.get(i);
            if (v == read) { //NOPMD CompareObjectsWithEquals - a cell is an identity
                v.circular = true;
                return;
            }
            if (v.circular && read.circular) {
                // marked by an earlier turn round the same circle, and so is all below it
                return;
            }
            v.circular = true;
        }
    }

    /// Calculates a cell and, first, every stale cell it reads. The cells wait on a list and
    /// not on the call stack: a column in which every row adds to the one above is ten
    /// thousand cells deep, and a device's stack is not.
    private void calculate(Cell root) {
        int entered = visiting.size();
        boolean finished = false;
        try {
            calculateFrom(root);
            finished = true;
        } finally {
            if (!finished) {
                // Something escaped a formula (out of memory, a stack). The cells half way
                // through are left to be calculated again rather than reading as a circle
                // forever after.
                for (int i = visiting.size() - 1; i >= entered; i--) {
                    Cell v = visiting.remove(i);
                    v.circular = false;
                    v.state = Cell.DIRTY;
                }
            }
        }
    }

    private void calculateFrom(Cell root) {
        ArrayList<Cell> stack = new ArrayList<Cell>();
        stack.add(root);
        while (!stack.isEmpty()) {
            int top = stack.size() - 1;
            Cell c = stack.get(top);
            if (c.state == Cell.CLEAN) {
                stack.remove(top);
                continue;
            }
            if (c.state == Cell.DIRTY) {
                c.state = Cell.VISITING;
                visiting.add(c);
                if (c.formula != null) {
                    pushStalePrecedents(c, stack);
                }
                if (stack.size() - 1 != top) {
                    continue;
                }
            }
            // everything this cell reads directly is up to date
            stack.remove(top);
            Object v;
            if (c.formula == null) {
                v = c.value;
            } else {
                v = evaluator.evaluate(c.formula, c.sheet, c.row, c.col, c.arrayFormula);
            }
            int last = visiting.size() - 1;
            if (last >= 0 && visiting.get(last) == c) { //NOPMD CompareObjectsWithEquals - a cell is an identity
                visiting.remove(last);
            } else {
                visiting.remove(c);
            }
            if (c.circular) {
                c.circular = false;
                v = CellError.CIRCULAR;
            }
            c.value = v;
            c.state = Cell.CLEAN;
        }
    }

    private void pushStalePrecedents(Cell c, ArrayList<Cell> stack) {
        Ref[] refs = references(c);
        for (Ref r : refs) {
            int first = indexOf(r.sheet);
            int last = r.lastSheet == null ? first : indexOf(r.lastSheet);
            if (first < 0 || last < 0) {
                continue;
            }
            for (int s = first; s <= last; s++) {
                Sheet sheet = sheets.get(s);
                int maxRow = Math.min(r.r2, sheet.usedRows() - 1);
                for (int row = r.r1; row <= maxRow; row++) {
                    Cell[] cells = sheet.rowCells(row);
                    if (cells == null) {
                        continue;
                    }
                    int maxCol = Math.min(r.c2, cells.length - 1);
                    for (int col = r.c1; col <= maxCol; col++) {
                        Cell p = cells[col];
                        if (p != null && p.state == Cell.DIRTY) {
                            stack.add(p);
                        }
                    }
                }
            }
        }
    }

    /// The references a formula names, directly or through defined names. What a formula
    /// reaches by calculating an address -- INDIRECT, OFFSET -- is not among them; such a
    /// formula is calculated again whenever anything changes.
    private Ref[] references(Cell c) {
        if (c.refs == null) {
            ArrayList<Ref> out = new ArrayList<Ref>();
            collect(c.formula, c.sheet, out, 0);
            Ref[] refs = new Ref[out.size()];
            out.toArray(refs);
            c.refs = refs;
        }
        return c.refs;
    }

    private void collect(Node n, Sheet home, ArrayList<Ref> out, int depth) {
        if (n == null) {
            return;
        }
        if (n.kind == Node.REF) {
            Sheet s = n.sheet == null ? home : getSheet(n.sheet);
            Sheet last = n.sheet2 == null ? null : getSheet(n.sheet2);
            if (s == null || n.sheet2 != null && last == null) {
                return;
            }
            if (last != null && indexOf(last) < indexOf(s)) {
                Sheet swap = s;
                s = last;
                last = swap;
            }
            out.add(new Ref(s, last, n.r1, n.c1, n.r2, n.c2));
            return;
        }
        if (n.kind == Node.NAME) {
            if (depth < 16) {
                collect(definedNameNode(n.name()), home, out, depth + 1);
            }
            return;
        }
        if (n.kind == Node.BINARY) {
            // a chain of operators is walked and not recursed into: see Node.spine
            Node[] spine = Node.spine(n);
            collect(spine[0].args[0], home, out, depth);
            for (Node link : spine) {
                collect(link.args[1], home, out, depth);
            }
            return;
        }
        if (n.args != null) {
            for (int i = 0; i < n.args.length; i++) {
                collect(n.args[i], home, out, depth);
            }
        }
    }

    /// Whether a formula has to be calculated again after any change at all: it uses a
    /// function whose result moves by itself, or one that reads cells it does not name.
    boolean isVolatile(Node node, int depth) {
        Node n = node;
        if (n == null) {
            return false;
        }
        // down the left of a chain of operators in a loop: see Node.spine
        while (n.kind == Node.BINARY) {
            if (isVolatile(n.args[1], depth)) {
                return true;
            }
            n = n.args[0];
        }
        if (n.kind == Node.FUNC) {
            int info = Functions.info(n.name());
            if (info >= 0 && (info & Functions.VOLATILE) != 0) {
                return true;
            }
        }
        if (n.kind == Node.NAME && depth < 16 && isVolatile(definedNameNode(n.name()), depth + 1)) {
            return true;
        }
        if (n.args != null) {
            for (int i = 0; i < n.args.length; i++) {
                if (isVolatile(n.args[i], depth)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Long key(int sheet, int row, int col) {
        return Long.valueOf(((long) sheet << 34) | ((long) row << 14) | col);
    }

    /// Sheets were added, removed or reshaped: what is known about who reads what is void.
    void structureChanged() {
        pointReaders = null;
        rangeReaders = null;
        rangeRefs = null;
        for (Sheet sheet : sheets) {
            int rows = sheet.usedRows();
            for (int r = 0; r < rows; r++) {
                Cell[] cells = sheet.rowCells(r);
                if (cells == null) {
                    continue;
                }
                for (int c = 0; c < cells.length; c++) {
                    if (cells[c] != null) {
                        cells[c].refs = null;
                    }
                }
            }
        }
    }

    private void rebuildReaders() {
        pointReaders = new HashMap<Long, ArrayList<Cell>>();
        rangeReaders = new ArrayList<Cell>();
        rangeRefs = new ArrayList<Ref>();
        volatileCells.clear();
        for (Sheet sheet : sheets) {
            int rows = sheet.usedRows();
            for (int r = 0; r < rows; r++) {
                Cell[] cells = sheet.rowCells(r);
                if (cells == null) {
                    continue;
                }
                for (Cell cell : cells) {
                    if (cell != null && cell.formula != null) {
                        register(cell);
                    }
                }
            }
        }
    }

    private void register(Cell cell) {
        if (cell.alwaysDirty) {
            volatileCells.add(cell);
        }
        Ref[] refs = references(cell);
        for (Ref r : refs) {
            long count = (long) r.rows() * r.cols();
            if (r.lastSheet != null || count > POINT_LIMIT) {
                rangeReaders.add(cell);
                rangeRefs.add(r);
                continue;
            }
            int sheet = indexOf(r.sheet);
            for (int row = r.r1; row <= r.r2; row++) {
                for (int col = r.c1; col <= r.c2; col++) {
                    Long k = key(sheet, row, col);
                    ArrayList<Cell> list = pointReaders.get(k);
                    if (list == null) {
                        list = new ArrayList<Cell>(2);
                        pointReaders.put(k, list);
                    }
                    list.add(cell);
                }
            }
        }
    }

    /// A formula was put into a cell.
    void formulaSet(Cell cell) {
        cell.refs = null;
        cell.alwaysDirty = isVolatile(cell.formula, 0);
        if (pointReaders != null) {
            register(cell);
        }
    }

    /// The content of a cell changed. Marks every formula that reads it, and every formula
    /// that reads one of those, as stale, and tells the sheets' listeners. Nothing is
    /// calculated here: a stale cell is calculated when its value is asked for, which for a
    /// cell on screen is the repaint the event leads to.
    void cellChanged(Sheet sheet, int row, int col) {
        if (silent) {
            return;
        }
        if (pointReaders == null) {
            rebuildReaders();
        }
        ArrayList<Cell> queue = new ArrayList<Cell>();
        markReaders(sheet, row, col, queue);
        for (Cell v : volatileCells) {
            if (v.state == Cell.CLEAN && v.formula != null && v.alwaysDirty) {
                v.state = Cell.DIRTY;
                queue.add(v);
            }
        }
        for (int i = 0; i < queue.size(); i++) { //NOPMD ForLoopCanBeForeach - the list grows while it is walked
            Cell c = queue.get(i);
            markReaders(c.sheet, c.row, c.col, queue);
        }
        sheet.fire(row, col);
        for (int i = 0; i < queue.size(); i++) { //NOPMD ForLoopCanBeForeach - a listener may edit a cell, which walks these lists again
            Cell c = queue.get(i);
            if (c.sheet != sheet || c.row != row || c.col != col) { //NOPMD CompareObjectsWithEquals - a sheet is an identity
                c.sheet.fire(c.row, c.col);
            }
        }
    }

    private void markReaders(Sheet sheet, int row, int col, ArrayList<Cell> queue) {
        int index = indexOf(sheet);
        ArrayList<Cell> list = pointReaders.get(key(index, row, col));
        if (list != null) {
            for (Cell reader : list) {
                mark(reader, queue);
            }
        }
        // Tested one by one: a sheet with thousands of large ranges pays for each of them on
        // every edit. The ranges of one sheet are few in the files this is meant for.
        for (int i = 0; i < rangeRefs.size(); i++) {
            Cell reader = rangeReaders.get(i);
            if (reader.state != Cell.CLEAN) {
                continue;
            }
            Ref r = rangeRefs.get(i);
            if (row < r.r1 || row > r.r2 || col < r.c1 || col > r.c2) {
                continue;
            }
            if (r.lastSheet == null ? r.sheet != sheet : index < indexOf(r.sheet) || index > indexOf(r.lastSheet)) { //NOPMD CompareObjectsWithEquals - a sheet is an identity
                continue;
            }
            mark(reader, queue);
        }
    }

    private static void mark(Cell reader, ArrayList<Cell> queue) {
        // a cell that was a formula when it was registered may hold a plain value by now
        if (reader.state == Cell.CLEAN && reader.formula != null && reader.sheet != null) {
            reader.state = Cell.DIRTY;
            queue.add(reader);
        }
    }

    /// The day number a workbook of this date system stores for a moment.
    double toSerial(Date date) {
        return DateUtil.toSerial(date, date1904);
    }
}
