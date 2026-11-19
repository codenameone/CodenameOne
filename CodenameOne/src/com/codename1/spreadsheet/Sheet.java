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

import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.table.AbstractTableModel;
import com.codename1.ui.table.GridColumn;
import com.codename1.ui.table.GridFormatter;
import com.codename1.ui.util.EventDispatcher;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/// One sheet of a [Workbook]: a grid of cells with no fixed size, each empty or holding a
/// number, text, a logical value, an error or a formula.
///
/// #### Two ways to address a cell
///
/// The methods named `...Cell...` address the sheet the way a formula does: row 0, column 0
/// is `A1`, whatever the sheet is used for. The [com.codename1.ui.table.TableModel] methods
/// address the *data*: when [#setHeaderRow(boolean)] is on, the first row of the sheet is
/// the column names and model row 0 is the row under it.
///
/// #### Values
///
/// A cell's value is a `Double`, a `String`, a `Boolean`, a [CellError], a `java.util.Date`
/// for a number whose format shows a date, or null for an empty cell. The value of a formula
/// is what it calculates to; [#getCellFormula(int, int)] answers the formula itself.
///
/// A `String` passed to [#setValueAt(int, int, Object)] is read as if it had been typed into
/// a spreadsheet: `=A1*2` is a formula, `12.5` a number, `45%` a number shown as a
/// percentage, `2024-03-01` a date, `TRUE` a logical value, and `'12` the text 12.
/// [#setCellValue(int, int, Object)] stores what it is given, so a `String` stays text.
public final class Sheet extends AbstractTableModel {
    /// The width in millimetres of one character of the width unit spreadsheets use for
    /// columns: a digit of the default font at 96 dots per inch.
    static final float MM_PER_CHARACTER = 1.85f;


    private final Workbook workbook;
    private String name;
    private final ArrayList<Cell[]> rows = new ArrayList<Cell[]>();
    private final EventDispatcher dispatcher = new EventDispatcher();
    private final ArrayList<int[]> merges = new ArrayList<int[]>();
    private final HashMap<Integer, Double> columnWidths = new HashMap<Integer, Double>();
    private final HashMap<Integer, Integer> outlineLevels = new HashMap<Integer, Integer>();
    private int usedColumns;
    private boolean headerRow;
    private boolean editable = true;
    private int frozenRows;
    private int frozenColumns;

    Sheet(Workbook workbook, String name) {
        this.workbook = workbook;
        this.name = name;
    }

    /// The name of the sheet, which formulas on other sheets refer to it by.
    ///
    /// #### Returns
    ///
    /// the name
    public String getName() {
        return name;
    }

    /// The workbook the sheet is part of.
    ///
    /// #### Returns
    ///
    /// the workbook
    public Workbook getWorkbook() {
        return workbook;
    }

    // ------------------------------------------------------------------ storage

    void rename(String name) {
        this.name = name;
    }

    Cell cell(int row, int col) {
        if (row < 0 || row >= rows.size() || col < 0) {
            return null;
        }
        Cell[] cells = rows.get(row);
        return cells == null || col >= cells.length ? null : cells[col];
    }

    Cell[] rowCells(int row) {
        return row < 0 || row >= rows.size() ? null : rows.get(row);
    }

    int usedRows() {
        return rows.size();
    }

    int usedColumns() {
        return usedColumns;
    }

    Cell ensure(int row, int col) {
        if (row < 0 || col < 0 || row > Node.MAX_ROW || col > Node.MAX_COL) {
            throw new IllegalArgumentException("No such cell: row " + row + ", column " + col);
        }
        if (rows.size() <= row) {
            // The rows above a cell far down take room too, though they hold nothing: a
            // reference each, an eighth of what a cell takes.
            workbook.allocated((row + 1 - rows.size() + 7) / 8);
        }
        while (rows.size() <= row) {
            rows.add(null);
        }
        Cell[] cells = rows.get(row);
        if (cells == null || cells.length <= col) {
            int size = cells == null ? Math.max(col + 1, Math.min(usedColumns, 64)) : Math.max(col + 1, cells.length * 2);
            workbook.allocated(size - (cells == null ? 0 : cells.length));
            Cell[] grown = new Cell[size];
            if (cells != null) {
                System.arraycopy(cells, 0, grown, 0, cells.length);
            }
            cells = grown;
            rows.set(row, cells);
        }
        Cell c = cells[col];
        if (c == null) {
            c = new Cell(this, row, col);
            cells[col] = c;
        }
        if (col >= usedColumns) {
            usedColumns = col + 1;
        }
        return c;
    }

    /// Puts a value into a cell without telling anyone: what the file readers and the
    /// copies from a grid use. A date becomes a day number with a date format.
    void store(int row, int col, Object value) {
        if (value == null) {
            Cell old = cell(row, col);
            if (old != null) {
                old.value = null;
                clearFormula(old);
            }
            return;
        }
        Cell c = ensure(row, col);
        clearFormula(c);
        if (value instanceof Date) {
            double serial = workbook.toSerial((Date) value);
            c.value = Double.valueOf(serial);
            if (!workbook.format(c.format).isDate()) {
                c.format = workbook.formatId(serial == Math.floor(serial) ? "yyyy-mm-dd" : "yyyy-mm-dd hh:mm:ss");
            }
        } else if (value instanceof Double) {
            double d = ((Double) value).doubleValue();
            // a spreadsheet has no NaN or infinity: every file writes them as #NUM!, so the
            // cell holds that error from the start rather than a number that acts as one
            c.value = Double.isNaN(d) || Double.isInfinite(d) ? CellError.NUM : value;
        } else if (value instanceof Float) {
            // 1.1f widened is 1.100000023841858: the number meant is the one it prints as
            double d = Double.parseDouble(value.toString());
            c.value = Double.isNaN(d) || Double.isInfinite(d) ? CellError.NUM : Double.valueOf(d);
        } else if (value instanceof Number) {
            c.value = Double.valueOf(((Number) value).doubleValue());
        } else if (value instanceof Boolean || value instanceof String || value instanceof CellError) {
            c.value = value;
        } else {
            c.value = value.toString();
        }
    }

    private static void clearFormula(Cell c) {
        c.formula = null;
        c.formulaText = null;
        c.refs = null;
        c.odsSyntax = false;
        c.alwaysDirty = false;
        c.arrayFormula = false;
        c.keepsCached = false;
        c.circular = false;
        c.state = Cell.CLEAN;
        if (c.autoFormat) {
            // the date format =TODAY() brought would turn the 5 typed over it into a date
            c.autoFormat = false;
            c.format = 0;
        }
    }

    /// What a formula read from a file is counted as against the cell limit. A formula cell
    /// holds its text and a tree of it, measured at about eight times what a number takes,
    /// and more for a long formula.
    static long formulaCost(int length) {
        return 8 + length / 8;
    }

    /// Puts a formula read from a file into a cell, with the value the file stored for it.
    /// A formula that cannot be read keeps its text and that value, and is written back as
    /// it came.
    void loadFormula(int row, int col, String text, boolean ods, Object cached, boolean hasCached,
            boolean array) {
        Cell c = ensure(row, col);
        workbook.allocated(formulaCost(text.length()));
        storeRaw(row, col, cached);
        c.arrayFormula = array;
        try {
            c.formula = FormulaParser.parse(text, ods);
            c.formulaText = FormulaParser.display(c.formula);
            c.state = hasCached ? Cell.CLEAN : Cell.DIRTY;
            c.keepsCached = hasCached && Workbook.callsUnknown(c.formula, 0);
            workbook.formulaSet(c);
        } catch (FormulaSyntaxException e) {
            c.formula = null;
            c.formulaText = text;
            c.odsSyntax = ods;
            c.state = Cell.CLEAN;
        }
    }

    /// Puts a formula a file reader has already parsed -- one cell of a shared formula --
    /// into a cell.
    void loadFormula(int row, int col, Node node, Object cached, boolean hasCached, boolean array) {
        Cell c = ensure(row, col);
        storeRaw(row, col, cached);
        c.arrayFormula = array;
        c.formula = node;
        c.formulaText = FormulaParser.display(node);
        workbook.allocated(formulaCost(c.formulaText.length()));
        c.state = hasCached ? Cell.CLEAN : Cell.DIRTY;
        c.keepsCached = hasCached && Workbook.callsUnknown(node, 0);
        workbook.formulaSet(c);
    }

    /// Puts a value into a cell exactly as it is given, whatever its class: a file reader
    /// leaves a marker here that it replaces when it has read the rest of the file.
    void storeRaw(int row, int col, Object value) {
        Cell c = ensure(row, col);
        clearFormula(c);
        c.value = value;
    }

    // ------------------------------------------------------------------ cells

    /// The value of a cell.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row of the sheet, 0 for the first
    ///
    /// - `column`: the column of the sheet, 0 for `A`
    ///
    /// #### Returns
    ///
    /// a `Double`, `String`, `Boolean`, [CellError] or `Date`, or null for an empty cell
    public Object getCellValue(int row, int column) {
        Cell c = cell(row, column);
        if (c == null) {
            return null;
        }
        Object v = workbook.valueOf(c);
        if (v instanceof Double && c.format != 0) {
            CellFormat f = workbook.format(c.format);
            if (f.isDate() && !f.isTimeOnly()) {
                double d = ((Double) v).doubleValue();
                if (d >= 0) {
                    return DateUtil.toDate(d, workbook.isDate1904());
                }
            }
        }
        return v;
    }

    /// The value of a cell named the way a formula names it.
    ///
    /// #### Parameters
    ///
    /// - `address`: an address such as `B7`
    ///
    /// #### Returns
    ///
    /// the value, see [#getCellValue(int, int)]
    public Object getCellValue(String address) {
        int[] rc = address(address);
        return getCellValue(rc[0], rc[1]);
    }

    /// Stores a value in a cell, replacing a formula it held, and recalculates what depends
    /// on it.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row of the sheet
    ///
    /// - `column`: the column of the sheet
    ///
    /// - `value`: a `Number`, `String`, `Boolean`, `Date` or [CellError]; null empties the
    ///   cell. A `String` is stored as text even when it looks like a number or a formula.
    public void setCellValue(int row, int column, Object value) {
        store(row, column, value);
        workbook.cellChanged(this, row, column);
    }

    /// Stores a value in a cell named the way a formula names it.
    ///
    /// #### Parameters
    ///
    /// - `address`: an address such as `B7`
    ///
    /// - `value`: the value, see [#setCellValue(int, int, Object)]
    public void setCellValue(String address, Object value) {
        int[] rc = address(address);
        setCellValue(rc[0], rc[1], value);
    }

    /// The formula of a cell.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row of the sheet
    ///
    /// - `column`: the column of the sheet
    ///
    /// #### Returns
    ///
    /// the formula with its leading `=`, or null when the cell holds no formula
    public String getCellFormula(int row, int column) {
        Cell c = cell(row, column);
        return c == null || c.formulaText == null ? null : "=" + c.formulaText;
    }

    /// Puts a formula into a cell and recalculates what depends on it.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row of the sheet
    ///
    /// - `column`: the column of the sheet
    ///
    /// - `formula`: a formula in Excel's syntax, with or without the leading `=`
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the formula cannot be read
    public void setCellFormula(int row, int column, String formula) {
        if (formula == null) {
            setCellValue(row, column, null);
            return;
        }
        String text = formula.startsWith("=") ? formula.substring(1) : formula;
        Node node;
        try {
            node = FormulaParser.parse(text, false);
        } catch (FormulaSyntaxException e) {
            throw new IllegalArgumentException(e.getMessage()); //NOPMD PreserveStackTrace - the device library has no constructor that takes a cause
        }
        putFormula(row, column, text, node);
    }

    /// The format a formula typed into a cell with none gives it, as a spreadsheet does for
    /// the functions that make a date: `=TODAY()` shows a date and not the number of a day.
    private void formatOfTyped(Cell c, Node node) {
        if (node.kind != Node.FUNC || !(node.value instanceof String) || !workbook.format(c.format).isGeneral()) {
            return;
        }
        String name = (String) node.value;
        if ("DATE".equalsIgnoreCase(name) || "TODAY".equalsIgnoreCase(name)) {
            c.format = workbook.formatId("yyyy-mm-dd");
            c.autoFormat = true;
        } else if ("NOW".equalsIgnoreCase(name)) {
            c.format = workbook.formatId("yyyy-mm-dd hh:mm:ss");
            c.autoFormat = true;
        } else if ("TIME".equalsIgnoreCase(name)) {
            c.format = workbook.formatId("hh:mm:ss");
            c.autoFormat = true;
        }
    }

    /// Puts a formula into a cell named the way a formula names it.
    ///
    /// #### Parameters
    ///
    /// - `address`: an address such as `B7`
    ///
    /// - `formula`: the formula, see [#setCellFormula(int, int, String)]
    public void setCellFormula(String address, String formula) {
        int[] rc = address(address);
        setCellFormula(rc[0], rc[1], formula);
    }

    private void putFormula(int row, int column, String text, Node node) {
        Cell c = ensure(row, column);
        clearFormula(c);
        c.formula = node;
        c.formulaText = text;
        c.state = Cell.DIRTY;
        formatOfTyped(c, node);
        workbook.formulaSet(c);
        workbook.cellChanged(this, row, column);
    }

    /// The text a cell shows: its value in its number format.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row of the sheet
    ///
    /// - `column`: the column of the sheet
    ///
    /// #### Returns
    ///
    /// the text, empty for an empty cell
    public String getCellText(int row, int column) {
        Cell c = cell(row, column);
        if (c == null) {
            return "";
        }
        return workbook.format(c.format).format(workbook.valueOf(c), workbook.isDate1904());
    }

    /// The number format of a cell.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row of the sheet
    ///
    /// - `column`: the column of the sheet
    ///
    /// #### Returns
    ///
    /// a format code such as `#,##0.00`, `General` when the cell has none
    public String getCellFormat(int row, int column) {
        Cell c = cell(row, column);
        return workbook.format(c == null ? 0 : c.format).code();
    }

    /// Sets the number format of a cell: how its value is shown, and for a number whether
    /// it is a date.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row of the sheet
    ///
    /// - `column`: the column of the sheet
    ///
    /// - `format`: a format code in Excel's syntax, such as `0.0%` or `d mmm yyyy`; null for
    ///   none
    public void setCellFormat(int row, int column, String format) {
        Cell c = ensure(row, column);
        c.format = workbook.formatId(format);
        c.autoFormat = false;
        fire(row, column);
    }

    void storeFormat(int row, int col, int format) {
        ensure(row, col).format = format;
    }

    private static int[] address(String a) {
        int n = a.length();
        int i = 0;
        while (i < n && FormulaParser.isLetter(a.charAt(i))) {
            i++;
        }
        int row = -1;
        if (i > 0 && i < n && i <= 3) {
            try {
                row = Integer.parseInt(a.substring(i)) - 1;
            } catch (NumberFormatException e) {
                row = -1;
            }
        }
        if (row < 0) {
            throw new IllegalArgumentException("Not a cell address: " + a);
        }
        return new int[] {row, FormulaParser.column(a, 0, i)};
    }

    /// Reads text as if it had been typed into the cell.
    private void input(int row, int column, String text) {
        if (text.length() == 0) {
            setCellValue(row, column, null);
            return;
        }
        if (text.charAt(0) == '\'') {
            setCellValue(row, column, text.substring(1));
            return;
        }
        Cell typedInto = cell(row, column);
        if (typedInto != null && !typedInto.autoFormat && workbook.format(typedInto.format).isText()) {
            // a cell formatted as text keeps what is typed: the zeros of 00123, and a formula
            // as its text
            setCellValue(row, column, text);
            return;
        }
        if (text.charAt(0) == '=' && text.length() > 1) {
            try {
                String body = text.substring(1);
                putFormula(row, column, body, FormulaParser.parse(body, false));
            } catch (FormulaSyntaxException e) {
                // a spreadsheet refuses the entry; a model has nobody to refuse it to
                setCellValue(row, column, text);
            }
            return;
        }
        String trimmed = text.trim();
        if ("TRUE".equalsIgnoreCase(trimmed)) {
            setCellValue(row, column, Boolean.TRUE);
            return;
        }
        if ("FALSE".equalsIgnoreCase(trimmed)) {
            setCellValue(row, column, Boolean.FALSE);
            return;
        }
        CellError error = CellError.valueOf(trimmed);
        if (error != null) {
            setCellValue(row, column, error);
            return;
        }
        Double number = Values.parseNumber(trimmed);
        // 1E400 reads as a number and is none a cell can hold
        if (number == null || Double.isInfinite(number.doubleValue()) || Double.isNaN(number.doubleValue())) {
            setCellValue(row, column, text);
            return;
        }
        double d = number.doubleValue();
        String format = null;
        // 1.2E-05 has a dash where a year would end, and is no date
        boolean date = Values.namedDate(trimmed, new int[1]) >= 0 || trimmed.indexOf('-', 1) == 4
                && trimmed.indexOf('e') < 0 && trimmed.indexOf('E') < 0;
        boolean time = trimmed.indexOf(':') >= 0;
        if (trimmed.endsWith("%")) {
            format = trimmed.indexOf('.') < 0 ? "0%" : "0.00%";
        } else if (date || time) {
            format = date ? time ? "yyyy-mm-dd hh:mm:ss" : "yyyy-mm-dd" : "hh:mm:ss";
            if (date && workbook.isDate1904()) {
                d -= 1462;
            }
        }
        store(row, column, Double.valueOf(d));
        Cell c = cell(row, column);
        if (format != null && workbook.format(c.format).isGeneral()) {
            c.format = workbook.formatId(format);
        }
        workbook.cellChanged(this, row, column);
    }

    // ------------------------------------------------------------------ the table model

    /// Whether the first row of the sheet holds the names of the columns. When it does, the
    /// table model's rows start under it.
    ///
    /// #### Returns
    ///
    /// true when the first row is a header
    public boolean isHeaderRow() {
        return headerRow;
    }

    /// Sets whether the first row of the sheet holds the names of the columns.
    ///
    /// #### Parameters
    ///
    /// - `headerRow`: true to take the column names from the first row
    public void setHeaderRow(boolean headerRow) {
        if (this.headerRow != headerRow) {
            this.headerRow = headerRow;
            fireAll();
        }
    }

    /// Sets whether the table model reports its cells as editable.
    ///
    /// #### Parameters
    ///
    /// - `editable`: false for a grid that only shows the sheet
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    private int offset() {
        return headerRow ? 1 : 0;
    }

    int headerRows() {
        return offset();
    }

    /// What an editor opens with for a cell: the formula, or the value written so that
    /// typing it back gives the same value.
    String editText(int row, int column) {
        Cell c = cell(row, column);
        if (c == null) {
            return "";
        }
        if (c.isFormula()) {
            return getCellFormula(row, column);
        }
        Object v = c.value;
        if (v == null) {
            return "";
        }
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue() ? "TRUE" : "FALSE";
        }
        if (!(v instanceof Double)) {
            return v.toString();
        }
        double d = ((Double) v).doubleValue();
        CellFormat format = workbook.format(c.format);
        if (format.isDate()) {
            boolean day = d >= 1;
            boolean time = d - Math.floor(d) > 0;
            String code = day ? time ? "yyyy-mm-dd hh:mm:ss" : "yyyy-mm-dd" : "hh:mm:ss";
            return new CellFormat(code).format(v, workbook.isDate1904());
        }
        if (format.isPercent()) {
            return Values.general(d * 100) + "%";
        }
        return Values.general(d);
    }

    @Override
    public int getRowCount() {
        return Math.max(0, rows.size() - offset());
    }

    @Override
    public int getColumnCount() {
        return usedColumns;
    }

    /// The name of a column: the text of the header row's cell when the sheet has a header
    /// row and the cell is not empty, the letters of the column otherwise.
    @Override
    public String getColumnName(int i) {
        if (headerRow) {
            String text = getCellText(0, i);
            if (text.length() > 0) {
                return text;
            }
        }
        StringBuilder sb = new StringBuilder();
        FormulaParser.appendColumn(sb, i);
        return sb.toString();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return editable;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return getCellValue(row + offset(), column);
    }

    /// Stores a value. A `String` is read as if it had been typed into a spreadsheet, see
    /// the class description; anything else is stored as it is.
    @Override
    public void setValueAt(int row, int column, Object o) {
        if (o instanceof String) {
            input(row + offset(), column, (String) o);
        } else {
            setCellValue(row + offset(), column, o);
        }
    }

    /// The formula of a cell of the table model.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row of the model
    ///
    /// - `column`: the column
    ///
    /// #### Returns
    ///
    /// the formula with its leading `=`, or null
    public String getFormulaAt(int row, int column) {
        return getCellFormula(row + offset(), column);
    }

    /// Adds a listener that is told when a cell changes: the cell that was edited, and every
    /// formula that reads it, directly or through other formulas.
    ///
    /// A formula is calculated when its value is asked for, not when what it reads changes,
    /// so the event says that the cell is out of date. A cell that is out of date already,
    /// because nobody read it since the last event, is not announced a second time.
    ///
    /// #### Parameters
    ///
    /// - `d`: the listener
    @Override
    public void addDataChangeListener(DataChangedListener d) {
        dispatcher.addListener(d);
    }

    @Override
    public void removeDataChangeListener(DataChangedListener d) {
        dispatcher.removeListener(d);
    }

    /// Tells the listeners about a cell of the sheet.
    void fire(int row, int col) {
        if (!dispatcher.hasListeners()) {
            return;
        }
        int modelRow = row - offset();
        if (modelRow < 0) {
            // a column name changed
            dispatcher.fireDataChangeEvent(Integer.MIN_VALUE, Integer.MIN_VALUE);
        } else {
            dispatcher.fireDataChangeEvent(col, modelRow);
        }
    }

    void fireAll() {
        if (dispatcher.hasListeners()) {
            dispatcher.fireDataChangeEvent(Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
    }

    /// Adds a row under the last one.
    ///
    /// #### Parameters
    ///
    /// - `values`: the values of the row's cells from the first column on; each is stored
    ///   as [#setCellValue(int, int, Object)] stores it
    public void addRow(Object... values) {
        int row = rows.size();
        rows.add(null);
        for (int c = 0; c < values.length; c++) {
            store(row, c, values[c]);
        }
        for (int c = 0; c < values.length; c++) {
            workbook.cellChanged(this, row, c);
        }
        if (values.length == 0) {
            fireAll();
        }
    }

    // ------------------------------------------------------------------ rows and columns

    /// Inserts empty rows. Formulas that refer to cells under them are rewritten to follow
    /// the cells, as a spreadsheet does.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row of the sheet the first new row becomes
    ///
    /// - `count`: how many rows to insert
    public void insertRows(int row, int count) {
        if (count <= 0 || row < 0 || row > Node.MAX_ROW) {
            return;
        }
        // no more than reach the edge of the grid, so that no position overflows
        count = Math.min(count, Node.MAX_ROW + 1 - row);
        if (row < rows.size()) {
            if ((long) rows.size() - 1 + count > Node.MAX_ROW) {
                // the last row of data would leave the grid, where no reference can reach it
                throw new IllegalArgumentException("Inserting " + count + " rows pushes row " + rows.size()
                        + " past the last row of a sheet, " + (Node.MAX_ROW + 1));
            }
            // one move of the rows under the insertion, however many rows go in
            ArrayList<Cell[]> empty = new ArrayList<Cell[]>(count);
            for (int i = 0; i < count; i++) {
                empty.add(null);
            }
            rows.addAll(row, empty);
        }
        renumber();
        shiftKeys(outlineLevels, row, count, Node.MAX_ROW);
        for (int i = merges.size() - 1; i >= 0; i--) {
            int[] m = merges.get(i);
            if (m[0] >= row) {
                m[0] += count;
            }
            if (m[2] >= row) {
                m[2] += count;
            }
            if (m[2] > Node.MAX_ROW) {
                merges.remove(i);
            }
        }
        workbook.restructured(this, true, row, count);
    }

    /// Removes rows. A formula that referred to a removed cell evaluates to `#REF!`; a range
    /// that included removed rows shrinks.
    ///
    /// #### Parameters
    ///
    /// - `row`: the first row of the sheet to remove
    ///
    /// - `count`: how many rows to remove
    public void removeRows(int row, int count) {
        if (count <= 0 || row < 0 || row > Node.MAX_ROW) {
            return;
        }
        // no more than reach the edge of the grid, so that no position overflows
        count = Math.min(count, Node.MAX_ROW + 1 - row);
        for (int i = 0; i < count && row < rows.size(); i++) {
            orphan(rows.remove(row));
        }
        renumber();
        shiftKeys(outlineLevels, row, -count, Node.MAX_ROW);
        for (int i = merges.size() - 1; i >= 0; i--) {
            int[] m = merges.get(i);
            if (m[0] >= row && m[2] < row + count) {
                merges.remove(i);
                continue;
            }
            m[0] = m[0] >= row + count ? m[0] - count : Math.min(m[0], row);
            m[2] = m[2] >= row + count ? m[2] - count : Math.min(m[2], row - 1);
        }
        workbook.restructured(this, true, row, -count);
    }

    /// Inserts empty columns. Formulas follow the cells that move.
    ///
    /// #### Parameters
    ///
    /// - `column`: the column the first new column becomes
    ///
    /// - `count`: how many columns to insert
    public void insertColumns(int column, int count) {
        if (count <= 0 || column < 0 || column > Node.MAX_COL) {
            return;
        }
        // no more than reach the edge of the grid, so that no position overflows
        count = Math.min(count, Node.MAX_COL + 1 - column);
        if (column < usedColumns && (long) usedColumns - 1 + count > Node.MAX_COL) {
            throw new IllegalArgumentException("Inserting " + count + " columns pushes column " + usedColumns
                    + " past the last column of a sheet, " + (Node.MAX_COL + 1));
        }
        for (int r = 0; r < rows.size(); r++) {
            Cell[] cells = rows.get(r);
            if (cells == null || cells.length <= column) {
                continue;
            }
            Cell[] grown = new Cell[cells.length + count];
            System.arraycopy(cells, 0, grown, 0, column);
            System.arraycopy(cells, column, grown, column + count, cells.length - column);
            rows.set(r, grown);
        }
        if (column < usedColumns) {
            usedColumns += count;
        }
        renumber();
        shiftKeys(columnWidths, column, count, Node.MAX_COL);
        for (int i = merges.size() - 1; i >= 0; i--) {
            int[] m = merges.get(i);
            if (m[1] >= column) {
                m[1] += count;
            }
            if (m[3] >= column) {
                m[3] += count;
            }
            if (m[3] > Node.MAX_COL) {
                merges.remove(i);
            }
        }
        workbook.restructured(this, false, column, count);
    }

    /// Removes columns. A formula that referred to a removed cell evaluates to `#REF!`.
    ///
    /// #### Parameters
    ///
    /// - `column`: the first column to remove
    ///
    /// - `count`: how many columns to remove
    public void removeColumns(int column, int count) {
        if (count <= 0 || column < 0 || column > Node.MAX_COL) {
            return;
        }
        // no more than reach the edge of the grid, so that no position overflows
        count = Math.min(count, Node.MAX_COL + 1 - column);
        // the cells go only as far as the data, but references move by the whole count
        int n = Math.max(0, Math.min(count, usedColumns - column));
        for (int r = 0; r < rows.size(); r++) {
            Cell[] cells = rows.get(r);
            if (cells == null || cells.length <= column) {
                continue;
            }
            int end = Math.min(cells.length, column + n);
            Cell[] removed = new Cell[end - column];
            System.arraycopy(cells, column, removed, 0, removed.length);
            orphan(removed);
            Cell[] shrunk = new Cell[cells.length - removed.length];
            System.arraycopy(cells, 0, shrunk, 0, column);
            System.arraycopy(cells, end, shrunk, column, cells.length - end);
            rows.set(r, shrunk);
        }
        usedColumns -= n;
        renumber();
        shiftKeys(columnWidths, column, -count, Node.MAX_COL);
        for (int i = merges.size() - 1; i >= 0; i--) {
            int[] m = merges.get(i);
            if (m[1] >= column && m[3] < column + count) {
                merges.remove(i);
                continue;
            }
            m[1] = m[1] >= column + count ? m[1] - count : Math.min(m[1], column);
            m[3] = m[3] >= column + count ? m[3] - count : Math.min(m[3], column - 1);
        }
        workbook.restructured(this, false, column, -count);
    }

    private static void orphan(Cell[] cells) {
        if (cells != null) {
            for (int i = 0; i < cells.length; i++) {
                if (cells[i] != null) {
                    cells[i].sheet = null;
                }
            }
        }
    }

    private void renumber() {
        for (int r = 0; r < rows.size(); r++) {
            Cell[] cells = rows.get(r);
            if (cells == null) {
                continue;
            }
            for (int c = 0; c < cells.length; c++) {
                if (cells[c] != null) {
                    cells[c].row = r;
                    cells[c].col = c;
                }
            }
        }
    }

    /// Moves the entries of a map keyed by row or column to where the rows or columns went.
    private static <T> void shiftKeys(HashMap<Integer, T> map, int at, int delta, int max) {
        if (map.isEmpty()) {
            return;
        }
        HashMap<Integer, T> moved = new HashMap<Integer, T>();
        for (java.util.Map.Entry<Integer, T> e : map.entrySet()) {
            int k = e.getKey().intValue();
            if (k < at) {
                moved.put(e.getKey(), e.getValue());
            } else if ((delta > 0 || k >= at - delta) && (long) k + delta <= max) {
                // a key pushed past the edge of the grid leaves it, as a merge does
                moved.put(Integer.valueOf(k + delta), e.getValue());
            }
        }
        map.clear();
        map.putAll(moved);
    }

    // ------------------------------------------------------------------ presentation

    /// Describes the columns of the sheet for a [com.codename1.ui.table.DataGrid]: the kind
    /// of each -- number, date, logical, text -- from the values it holds, a formatter that
    /// shows values in the number format most of the column's cells have, and the width the
    /// file gave the column.
    ///
    /// The columns describe the sheet as it is now. Call this again after loading other data
    /// into it.
    ///
    /// #### Returns
    ///
    /// one column per column of the sheet
    public GridColumn[] createColumns() {
        GridColumn[] out = new GridColumn[usedColumns];
        int first = offset();
        int last = Math.min(rows.size(), first + 500);
        for (int col = 0; col < usedColumns; col++) {
            int numbers = 0;
            int dates = 0;
            int logicals = 0;
            int texts = 0;
            HashMap<Integer, int[]> formatCounts = new HashMap<Integer, int[]>();
            for (int r = first; r < last; r++) {
                Cell c = cell(r, col);
                if (c == null) {
                    continue;
                }
                Object v = workbook.valueOf(c);
                if (v instanceof Double) {
                    if (workbook.format(c.format).isDate()) {
                        dates++;
                    } else {
                        numbers++;
                    }
                    Integer key = Integer.valueOf(c.format);
                    int[] n = formatCounts.get(key);
                    if (n == null) {
                        n = new int[1];
                        formatCounts.put(key, n);
                    }
                    n[0]++;
                } else if (v instanceof Boolean) {
                    logicals++;
                } else if (v instanceof String) {
                    texts++;
                }
            }
            int format = 0;
            int best = 0;
            for (java.util.Map.Entry<Integer, int[]> e : formatCounts.entrySet()) {
                int n = e.getValue()[0];
                if (n > best || n == best && e.getKey().intValue() < format) {
                    best = n;
                    format = e.getKey().intValue();
                }
            }
            GridColumn column = new GridColumn(col, getColumnName(col));
            int total = numbers + dates + logicals + texts;
            CellFormat cellFormat = workbook.format(format);
            if (total == 0) {
                column.setKind(GridColumn.Kind.AUTO);
            } else if (dates == total && !cellFormat.isTimeOnly()) {
                column.setKind(GridColumn.Kind.DATE);
            } else if (numbers + dates == total) {
                column.setKind(GridColumn.Kind.NUMBER);
            } else if (logicals == total) {
                column.setKind(GridColumn.Kind.BOOLEAN);
            } else if (texts == total) {
                column.setKind(GridColumn.Kind.TEXT);
            } else {
                column.setKind(GridColumn.Kind.AUTO);
            }
            if (logicals != total || total == 0) {
                column.setFormatter(new Formatter(workbook, cellFormat));
            }
            // the formatter is the column's: what the grid sorts, filters and groups by. The
            // cells are painted and edited one by one, each in its own format
            column.setRenderer(new SheetCells.Renderer(this));
            column.setEditor(new SheetCells.Editor(this));
            Double width = columnWidths.get(Integer.valueOf(col));
            if (width != null && width.doubleValue() > 0) {
                column.setWidthMM((float) (width.doubleValue() * MM_PER_CHARACTER));
            }
            out[col] = column;
        }
        return out;
    }

    /// Shows the values of a column in its number format. What is typed goes to the sheet as
    /// text, which reads it the way a spreadsheet reads what is typed into a cell.
    private static final class Formatter implements GridFormatter {
        private final Workbook workbook;
        private final CellFormat format;

        Formatter(Workbook workbook, CellFormat format) {
            this.workbook = workbook;
            this.format = format;
        }

        @Override
        public String format(Object value) {
            Object v = value;
            if (v instanceof Date) {
                v = Double.valueOf(workbook.toSerial((Date) v));
            }
            return format.format(v, workbook.isDate1904());
        }

        @Override
        public Object parse(String text, Object previous) {
            return text;
        }
    }

    /// The width of a column.
    ///
    /// #### Parameters
    ///
    /// - `column`: the column
    ///
    /// #### Returns
    ///
    /// the width in characters of the default font, the unit spreadsheet files use; 0 when
    /// the column has the default width
    public double getColumnWidth(int column) {
        Double w = columnWidths.get(Integer.valueOf(column));
        return w == null ? 0 : w.doubleValue();
    }

    /// Sets the width of a column.
    ///
    /// #### Parameters
    ///
    /// - `column`: the column
    ///
    /// - `characters`: the width in characters of the default font; 0 for the default width
    public void setColumnWidth(int column, double characters) {
        if (characters > 0) {
            columnWidths.put(Integer.valueOf(column), Double.valueOf(characters));
        } else {
            columnWidths.remove(Integer.valueOf(column));
        }
    }

    /// The outline level of a row: how deep it is nested in the groups a spreadsheet can
    /// collapse.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row of the sheet
    ///
    /// #### Returns
    ///
    /// the level, 0 for a row in no group
    public int getRowOutlineLevel(int row) {
        Integer level = outlineLevels.get(Integer.valueOf(row));
        return level == null ? 0 : level.intValue();
    }

    /// Sets the outline level of a row.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row of the sheet
    ///
    /// - `level`: the level, 0 to 7
    public void setRowOutlineLevel(int row, int level) {
        if (level > 0) {
            outlineLevels.put(Integer.valueOf(row), Integer.valueOf(Math.min(level, 7)));
        } else {
            outlineLevels.remove(Integer.valueOf(row));
        }
    }

    /// One past the last row with an outline level, which may hold nothing: a writer
    /// writes rows that far so that a group of empty rows is kept.
    int outlineExtent() {
        int last = 0;
        for (Integer row : outlineLevels.keySet()) {
            last = Math.max(last, row.intValue() + 1);
        }
        return last;
    }

    /// One past the last column with a width of its own.
    int widthExtent() {
        int last = 0;
        for (Integer col : columnWidths.keySet()) {
            last = Math.max(last, col.intValue() + 1);
        }
        return last;
    }

    /// The number of rows at the top that stay in place while the rest scrolls.
    ///
    /// #### Returns
    ///
    /// the count of frozen rows
    public int getFrozenRows() {
        return frozenRows;
    }

    /// The number of columns at the left that stay in place while the rest scrolls.
    ///
    /// #### Returns
    ///
    /// the count of frozen columns
    public int getFrozenColumns() {
        return frozenColumns;
    }

    /// Sets the rows and columns that stay in place while the rest of the sheet scrolls.
    ///
    /// #### Parameters
    ///
    /// - `rows`: the count of rows at the top
    ///
    /// - `columns`: the count of columns at the left
    public void setFrozenPanes(int rows, int columns) {
        frozenRows = Math.max(0, Math.min(rows, Node.MAX_ROW));
        frozenColumns = Math.max(0, Math.min(columns, Node.MAX_COL));
    }

    /// Merges a rectangle of cells into one, as the file formats record it. The value shown
    /// is the one of the top left cell.
    ///
    /// #### Parameters
    ///
    /// - `firstRow`: the top row
    ///
    /// - `firstColumn`: the left column
    ///
    /// - `lastRow`: the bottom row
    ///
    /// - `lastColumn`: the right column
    public void addMergedRegion(int firstRow, int firstColumn, int lastRow, int lastColumn) {
        if (!loadMergedRegion(firstRow, firstColumn, lastRow, lastColumn)) {
            throw new IllegalArgumentException("Not a rectangle of the sheet, or over a merge it has: rows " + firstRow + " to " + lastRow
                    + ", columns " + firstColumn + " to " + lastColumn);
        }
    }

    /// Records a merge a file names. Corners given the wrong way around are put right; a
    /// rectangle that leaves the sheet is not recorded, and false is the answer.
    boolean loadMergedRegion(int firstRow, int firstColumn, int lastRow, int lastColumn) {
        int r1 = Math.min(firstRow, lastRow);
        int r2 = Math.max(firstRow, lastRow);
        int c1 = Math.min(firstColumn, lastColumn);
        int c2 = Math.max(firstColumn, lastColumn);
        if (r1 < 0 || c1 < 0 || r2 > Node.MAX_ROW || c2 > Node.MAX_COL) {
            return false;
        }
        // a cell is in one merge; a file with two over one cell is repaired on opening. A
        // file with merges by the hundred thousand is not compared pair by pair.
        int compared = Math.min(merges.size(), 4096);
        for (int i = 0; i < compared; i++) {
            int[] m = merges.get(i);
            if (r1 <= m[2] && r2 >= m[0] && c1 <= m[3] && c2 >= m[1]) {
                return false;
            }
        }
        merges.add(new int[] {r1, c1, r2, c2});
        return true;
    }

    /// The number of merged regions.
    ///
    /// #### Returns
    ///
    /// the count
    public int getMergedRegionCount() {
        return merges.size();
    }

    /// A merged region.
    ///
    /// #### Parameters
    ///
    /// - `index`: which one, from 0
    ///
    /// #### Returns
    ///
    /// a new array of the top row, left column, bottom row and right column
    public int[] getMergedRegion(int index) {
        int[] m = merges.get(index);
        return new int[] {m[0], m[1], m[2], m[3]};
    }
}
