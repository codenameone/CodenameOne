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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// An immutable table: a rectangular grid of cells, each holding its own blocks (paragraphs,
/// pictures and nested tables), with optional merged ranges.
///
/// The grid is always rectangular. A merged range keeps its covered cells in the grid; they are
/// simply not shown, so inserting or deleting a row never has to reshape the grid.
public final class Table implements Block {
    /// A rectangular range of cells shown as one, anchored at its top-left cell.
    public static final class Merge {
        /// The anchor row.
        public final int row;

        /// The anchor column.
        public final int column;

        /// The number of rows spanned, at least one.
        public final int rows;

        /// The number of columns spanned, at least one.
        public final int columns;

        /// Creates a merge.
        ///
        /// #### Parameters
        ///
        /// - `row`: the anchor row
        ///
        /// - `column`: the anchor column
        ///
        /// - `rows`: rows spanned
        ///
        /// - `columns`: columns spanned
        public Merge(int row, int column, int rows, int columns) {
            if (row < 0 || column < 0 || rows < 1 || columns < 1 || rows * columns < 2) {
                throw new IllegalArgumentException("Invalid merge");
            }
            this.row = row;
            this.column = column;
            this.rows = rows;
            this.columns = columns;
        }

        /// Whether the range contains a cell.
        ///
        /// #### Parameters
        ///
        /// - `r`: the row
        ///
        /// - `c`: the column
        ///
        /// #### Returns
        ///
        /// true when inside
        public boolean contains(int r, int c) {
            return r >= row && r < row + rows && c >= column && c < column + columns;
        }

        /// Two merges are equal when they cover the same range.
        ///
        /// #### Parameters
        ///
        /// - `o`: the other object
        ///
        /// #### Returns
        ///
        /// true when equal
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Merge)) {
                return false;
            }
            Merge m = (Merge) o;
            return m.row == row && m.column == column && m.rows == rows && m.columns == columns;
        }

        /// A hash consistent with `equals`.
        ///
        /// #### Returns
        ///
        /// the hash
        @Override
        public int hashCode() {
            return ((row * 31 + column) * 31 + rows) * 31 + columns;
        }
    }

    /// The block id.
    public final String id;

    /// The number of leading rows repeated at the top of every page the table spans.
    public final int headerRows;

    /// The merged ranges, never overlapping.
    public final List<Merge> merges;

    private final List<List<List<Block>>> cells;
    private final int columnCount;
    private final List<Float> widths;

    /// Creates a table.
    ///
    /// #### Parameters
    ///
    /// - `id`: the block id
    ///
    /// - `cells`: the rows; every row must have the same number of cells and every cell is a list
    /// of blocks. The lists are copied.
    ///
    /// - `merges`: merged ranges, or null for none
    ///
    /// - `headerRows`: the number of header rows
    public Table(String id, List<List<List<Block>>> cells, List<Merge> merges, int headerRows) {
        this(id, cells, merges, headerRows, null);
    }

    private Table(String id, List<List<List<Block>>> cells, List<Merge> merges, int headerRows,
            List<Float> widths) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("Table id is required");
        }
        if (cells == null || cells.isEmpty() || cells.get(0).isEmpty()) {
            throw new IllegalArgumentException("A table needs at least one cell");
        }
        this.id = id;
        this.columnCount = cells.get(0).size();
        List<List<List<Block>>> rows = new ArrayList<List<List<Block>>>(cells.size());
        for (int r = 0; r < cells.size(); r++) {
            List<List<Block>> row = cells.get(r);
            if (row.size() != columnCount) {
                throw new IllegalArgumentException("Row " + r + " has " + row.size()
                        + " cells, expected " + columnCount);
            }
            List<List<Block>> copy = new ArrayList<List<Block>>(row.size());
            for (List<Block> cell : row) {
                copy.add(Collections.unmodifiableList(cell == null ? new ArrayList<Block>()
                        : new ArrayList<Block>(cell)));
            }
            rows.add(Collections.unmodifiableList(copy));
        }
        this.cells = Collections.unmodifiableList(rows);
        List<Merge> m = new ArrayList<Merge>();
        if (merges != null) {
            for (Merge merge : merges) {
                if (merge.row + merge.rows > rows.size()
                        || merge.column + merge.columns > columnCount) {
                    throw new IllegalArgumentException("Merge outside the table");
                }
                for (Merge other : m) {
                    if (overlaps(other, merge)) {
                        throw new IllegalArgumentException("Overlapping merges");
                    }
                }
                m.add(merge);
            }
        }
        this.merges = Collections.unmodifiableList(m);
        this.headerRows = Math.max(0, Math.min(rows.size(), headerRows));
        List<Float> w = new ArrayList<Float>();
        if (widths != null && widths.size() == columnCount) {
            for (Float f : widths) {
                if (f == null || !(f.floatValue() > 0) || Float.isInfinite(f.floatValue())) {
                    throw new IllegalArgumentException("Column widths must be positive");
                }
                w.add(f);
            }
        }
        this.widths = Collections.unmodifiableList(w);
    }

    /// The preferred column widths in points, one per column, or an empty list when the
    /// columns share the width equally. A table wider or narrower than the space it is laid
    /// out in keeps the proportions.
    ///
    /// #### Returns
    ///
    /// the widths, unmodifiable
    public List<Float> columnWidths() {
        return widths;
    }

    /// A copy with preferred column widths.
    ///
    /// #### Parameters
    ///
    /// - `points`: one positive width per column, in points; null or empty for equal columns
    ///
    /// #### Returns
    ///
    /// the copy
    public Table withColumnWidths(float... points) {
        if (points == null || points.length == 0) {
            return new Table(id, cells, merges, headerRows, null);
        }
        if (points.length != columnCount) {
            throw new IllegalArgumentException("Expected " + columnCount + " widths, got "
                    + points.length);
        }
        List<Float> w = new ArrayList<Float>(points.length);
        for (float f : points) {
            w.add(Float.valueOf(f));
        }
        return new Table(id, cells, merges, headerRows, w);
    }

    private static boolean overlaps(Merge a, Merge b) {
        return a.row < b.row + b.rows && b.row < a.row + a.rows
                && a.column < b.column + b.columns && b.column < a.column + a.columns;
    }

    /// The block id.
    ///
    /// #### Returns
    ///
    /// the id
    @Override
    public String id() {
        return id;
    }

    /// The number of rows.
    ///
    /// #### Returns
    ///
    /// the row count
    public int rows() {
        return cells.size();
    }

    /// The number of columns.
    ///
    /// #### Returns
    ///
    /// the column count
    public int columns() {
        return columnCount;
    }

    /// The blocks of one cell.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row
    ///
    /// - `column`: the column
    ///
    /// #### Returns
    ///
    /// an unmodifiable list
    public List<Block> cell(int row, int column) {
        return cells.get(row).get(column);
    }

    /// The text of one cell without formatting, paragraphs joined by newlines.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row
    ///
    /// - `column`: the column
    ///
    /// #### Returns
    ///
    /// the characters
    public String cellText(int row, int column) {
        StringBuilder b = new StringBuilder();
        List<Block> blocks = cell(row, column);
        for (int i = 0; i < blocks.size(); i++) {
            if (i > 0) {
                b.append('\n');
            }
            Document.appendPlainText(b, blocks.get(i));
        }
        return b.toString();
    }

    /// The merge anchored at a cell.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row
    ///
    /// - `column`: the column
    ///
    /// #### Returns
    ///
    /// the merge, or null when none starts there
    public Merge span(int row, int column) {
        for (Merge m : merges) {
            if (m.row == row && m.column == column) {
                return m;
            }
        }
        return null;
    }

    /// Whether a cell is hidden inside a merge anchored elsewhere.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row
    ///
    /// - `column`: the column
    ///
    /// #### Returns
    ///
    /// true when covered
    public boolean covered(int row, int column) {
        for (Merge m : merges) {
            if (m.contains(row, column) && (m.row != row || m.column != column)) {
                return true;
            }
        }
        return false;
    }

    private List<List<List<Block>>> mutableCells() {
        List<List<List<Block>>> out = new ArrayList<List<List<Block>>>(cells.size());
        for (List<List<Block>> row : cells) {
            out.add(new ArrayList<List<Block>>(row));
        }
        return out;
    }

    /// A copy with one cell's blocks replaced.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row
    ///
    /// - `column`: the column
    ///
    /// - `blocks`: the new contents
    ///
    /// #### Returns
    ///
    /// the copy
    public Table withCell(int row, int column, List<Block> blocks) {
        List<List<List<Block>>> c = mutableCells();
        c.get(row).set(column, blocks);
        return new Table(id, c, merges, headerRows, widths);
    }

    /// A copy with different merges.
    ///
    /// #### Parameters
    ///
    /// - `value`: the merges
    ///
    /// #### Returns
    ///
    /// the copy
    public Table withMerges(List<Merge> value) {
        return new Table(id, cells, value, headerRows, widths);
    }

    /// A copy with a different number of header rows.
    ///
    /// #### Parameters
    ///
    /// - `value`: the header row count
    ///
    /// #### Returns
    ///
    /// the copy
    public Table withHeaderRows(int value) {
        return new Table(id, cells, merges, value, widths);
    }

    /// A copy with a row of empty cells inserted. Merges below move down; a merge the new row
    /// passes through grows by one.
    ///
    /// #### Parameters
    ///
    /// - `at`: the new row's index, 0 to `rows()`
    ///
    /// #### Returns
    ///
    /// the copy
    public Table insertRow(int at) {
        List<List<List<Block>>> c = mutableCells();
        List<List<Block>> row = new ArrayList<List<Block>>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            row.add(new ArrayList<Block>());
        }
        c.add(at, row);
        List<Merge> m = new ArrayList<Merge>();
        for (Merge x : merges) {
            if (x.row >= at) {
                m.add(new Merge(x.row + 1, x.column, x.rows, x.columns));
            } else if (x.row + x.rows > at) {
                m.add(new Merge(x.row, x.column, x.rows + 1, x.columns));
            } else {
                m.add(x);
            }
        }
        return new Table(id, c, m, at < headerRows ? headerRows + 1 : headerRows, widths);
    }

    /// A copy without one row. Merges shrink or disappear with it.
    ///
    /// #### Parameters
    ///
    /// - `at`: the row to delete
    ///
    /// #### Returns
    ///
    /// the copy
    public Table deleteRow(int at) {
        if (cells.size() == 1) {
            throw new IllegalStateException("A table keeps at least one row");
        }
        List<List<List<Block>>> c = mutableCells();
        c.remove(at);
        List<Merge> m = new ArrayList<Merge>();
        for (Merge x : merges) {
            int row = x.row;
            int rows = x.rows;
            if (at < x.row) {
                row--;
            } else if (at < x.row + x.rows) {
                rows--;
            }
            if (rows * x.columns >= 2) {
                m.add(new Merge(row, x.column, rows, x.columns));
            }
        }
        return new Table(id, c, m, at < headerRows ? headerRows - 1 : headerRows, widths);
    }

    /// A copy with a column of empty cells inserted.
    ///
    /// #### Parameters
    ///
    /// - `at`: the new column's index, 0 to `columns()`
    ///
    /// #### Returns
    ///
    /// the copy
    public Table insertColumn(int at) {
        List<List<List<Block>>> c = mutableCells();
        for (List<List<Block>> row : c) {
            row.add(at, new ArrayList<Block>());
        }
        List<Merge> m = new ArrayList<Merge>();
        for (Merge x : merges) {
            if (x.column >= at) {
                m.add(new Merge(x.row, x.column + 1, x.rows, x.columns));
            } else if (x.column + x.columns > at) {
                m.add(new Merge(x.row, x.column, x.rows, x.columns + 1));
            } else {
                m.add(x);
            }
        }
        List<Float> w = null;
        if (!widths.isEmpty()) {
            // the new column takes the width of the one it pushes right, as Word inserts left
            w = new ArrayList<Float>(widths);
            w.add(at, widths.get(Math.min(at, widths.size() - 1)));
        }
        return new Table(id, c, m, headerRows, w);
    }

    /// A copy without one column.
    ///
    /// #### Parameters
    ///
    /// - `at`: the column to delete
    ///
    /// #### Returns
    ///
    /// the copy
    public Table deleteColumn(int at) {
        if (columnCount == 1) {
            throw new IllegalStateException("A table keeps at least one column");
        }
        List<List<List<Block>>> c = mutableCells();
        for (List<List<Block>> row : c) {
            row.remove(at);
        }
        List<Merge> m = new ArrayList<Merge>();
        for (Merge x : merges) {
            int column = x.column;
            int columns = x.columns;
            if (at < x.column) {
                column--;
            } else if (at < x.column + x.columns) {
                columns--;
            }
            if (columns * x.rows >= 2) {
                m.add(new Merge(x.row, column, x.rows, columns));
            }
        }
        List<Float> w = null;
        if (!widths.isEmpty()) {
            w = new ArrayList<Float>(widths);
            w.remove(at);
        }
        return new Table(id, c, m, headerRows, w);
    }

    /// Creates a table whose cells each hold one paragraph of plain text. Paragraph ids are
    /// derived from the table id.
    ///
    /// #### Parameters
    ///
    /// - `id`: the table id
    ///
    /// - `values`: the cell texts, row by row; short rows are padded
    ///
    /// #### Returns
    ///
    /// the table
    public static Table ofText(String id, String[][] values) {
        int cols = 1;
        for (String[] row : values) {
            cols = Math.max(cols, row.length);
        }
        List<List<List<Block>>> rows = new ArrayList<List<List<Block>>>();
        for (int r = 0; r < Math.max(1, values.length); r++) {
            List<List<Block>> row = new ArrayList<List<Block>>();
            for (int c = 0; c < cols; c++) {
                String v = r < values.length && c < values[r].length ? values[r][c] : "";
                List<Block> cell = new ArrayList<Block>();
                cell.add(new Paragraph(id + "." + r + "." + c, RichText.of(v)));
                row.add(cell);
            }
            rows.add(row);
        }
        return new Table(id, rows, null, 0);
    }

    /// Two tables are equal when their id, cells, merges, header rows and column widths are.
    ///
    /// #### Parameters
    ///
    /// - `o`: the other object
    ///
    /// #### Returns
    ///
    /// true when equal
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Table)) {
            return false;
        }
        Table t = (Table) o;
        return id.equals(t.id) && headerRows == t.headerRows && merges.equals(t.merges)
                && cells.equals(t.cells) && widths.equals(t.widths);
    }

    /// A hash consistent with `equals`.
    ///
    /// #### Returns
    ///
    /// the hash
    @Override
    public int hashCode() {
        return id.hashCode() * 31 + cells.hashCode();
    }
}
