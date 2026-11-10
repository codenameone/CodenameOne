/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.cli;

import java.util.ArrayList;
import java.util.List;

/// Rows and columns rendered as text, aligned by the columns each cell takes
/// on a terminal, so styled cells, wide characters and emoji line up:
///
/// ```java
/// Table table = new Table("Name", "Status", "Size")
///         .align(2, Table.Align.RIGHT);
/// table.addRow("api", Console.style("running", Console.Style.GREEN), "12 MB");
/// table.addRow("worker", Console.style("stopped", Console.Style.RED), "3 MB");
/// Console.print(table.render());
/// ```
///
/// A table wider than [#maxWidth] (the terminal by default) shrinks its widest
/// columns and wraps their cells. A cell may hold line breaks.
public final class Table {
    /// How a column's cells line up.
    public enum Align {
        /// Against the left edge.
        LEFT,
        /// In the middle.
        CENTER,
        /// Against the right edge, for numbers.
        RIGHT
    }

    /// The lines drawn around and between cells.
    public enum Border {
        /// Single box-drawing lines.
        SINGLE("\u250c\u2500\u252c\u2510\u2502\u251c\u253c\u2524\u2514\u2534\u2518"),
        /// Single lines with rounded corners.
        ROUNDED("\u256d\u2500\u252c\u256e\u2502\u251c\u253c\u2524\u2570\u2534\u256f"),
        /// Double box-drawing lines.
        DOUBLE("\u2554\u2550\u2566\u2557\u2551\u2560\u256c\u2563\u255a\u2569\u255d"),
        /// Plus signs, dashes and bars, for any terminal.
        ASCII("+-++|++++++"),
        /// A rule under the header and no other lines, like `ps` or `kubectl`.
        MINIMAL(null),
        /// No lines at all; columns separated by spaces.
        NONE(null);

        // top-left, horizontal, top-join, top-right, vertical, mid-left,
        // cross, mid-right, bottom-left, bottom-join, bottom-right
        private final String chars;

        Border(String chars) {
            this.chars = chars;
        }

        char c(int i) {
            return chars.charAt(i);
        }

        boolean boxed() {
            return chars != null;
        }
    }

    private final String[] headers;
    private final List<String[]> rows = new ArrayList<String[]>();
    private Align[] aligns;
    private Border border;
    private int maxWidth = -1;
    private boolean headerStyle = true;

    /// A table with these column headers; none for a table without a header
    /// row, whose column count then comes from its widest row.
    public Table(String... headers) {
        this.headers = headers == null ? new String[0] : headers.clone();
        this.aligns = new Align[this.headers.length];
    }

    /// Aligns column `column` (from 0).
    public Table align(int column, Align align) {
        ensureColumns(column + 1);
        aligns[column] = align;
        return this;
    }

    /// The border style; [Border#ROUNDED] by default, [Border#ASCII] when
    /// [Console#unicode] says box drawing is unsafe.
    public Table border(Border border) {
        this.border = border;
        return this;
    }

    /// The widest the table may be, in columns; the terminal width by default.
    public Table maxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    /// Whether the header row is bold (when color is on); true by default.
    public Table headerStyle(boolean bold) {
        this.headerStyle = bold;
        return this;
    }

    /// Appends a row. Each value is shown as `String.valueOf` gives it; a
    /// row shorter than the table is padded with empty cells.
    public Table addRow(Object... cells) {
        String[] row = new String[cells == null ? 0 : cells.length];
        for (int i = 0; i < row.length; i++) {
            row[i] = cells[i] == null ? "" : String.valueOf(cells[i]);
        }
        ensureColumns(row.length);
        rows.add(row);
        return this;
    }

    /// The number of rows added so far, header excluded.
    public int rowCount() {
        return rows.size();
    }

    private void ensureColumns(int n) {
        if (aligns.length < n) {
            Align[] a = new Align[n];
            System.arraycopy(aligns, 0, a, 0, aligns.length);
            aligns = a;
        }
    }

    /// The table as text, every line ended by a line break.
    public String render() {
        Border b = border != null ? border : Console.unicode() ? Border.ROUNDED : Border.ASCII;
        int cols = aligns.length;
        if (cols == 0) {
            return "";
        }
        boolean hasHeader = headers.length > 0;
        int[] widths = new int[cols];
        if (hasHeader) {
            measure(headers, widths);
        }
        for (String[] row : rows) {
            measure(row, widths);
        }
        fit(widths, b, maxWidth > 0 ? maxWidth : Console.width());

        StringBuilder sb = new StringBuilder();
        if (b.boxed()) {
            rule(sb, widths, b, 0, 1, 2, 3);
        }
        if (hasHeader) {
            String[] styled = new String[cols];
            for (int i = 0; i < cols; i++) {
                String h = i < headers.length ? headers[i] : "";
                styled[i] = headerStyle ? Console.style(h, Console.Style.BOLD) : h;
            }
            row(sb, styled, widths, b);
            if (b.boxed()) {
                rule(sb, widths, b, 5, 1, 6, 7);
            } else if (b == Border.MINIMAL) {
                char line = Console.unicode() ? '\u2500' : '-';
                for (int i = 0; i < cols; i++) {
                    if (i > 0) {
                        sb.append("  ");
                    }
                    repeat(sb, line, widths[i]);
                }
                sb.append('\n');
            }
        }
        for (String[] r : rows) {
            row(sb, r, widths, b);
        }
        if (b.boxed()) {
            rule(sb, widths, b, 8, 1, 9, 10);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return render();
    }

    private static void measure(String[] cells, int[] widths) {
        for (int i = 0; i < cells.length; i++) {
            for (String line : TextWidth.splitLines(cells[i])) {
                widths[i] = Math.max(widths[i], TextWidth.width(line));
            }
        }
    }

    /// Shrinks the widest columns, one column at a time, until the table fits
    /// in `max` or every column is down to a few characters.
    private static void fit(int[] widths, Border b, int max) {
        int cols = widths.length;
        int chrome = b.boxed() ? 3 * cols + 1 : 2 * (cols - 1);
        while (true) {
            int total = chrome;
            int widest = 0;
            for (int i = 0; i < cols; i++) {
                total += widths[i];
                if (widths[i] > widths[widest]) {
                    widest = i;
                }
            }
            if (total <= max || widths[widest] <= 4) {
                return;
            }
            widths[widest]--;
        }
    }

    private void row(StringBuilder sb, String[] cells, int[] widths, Border b) {
        int cols = widths.length;
        List<List<String>> wrapped = new ArrayList<List<String>>(cols);
        int height = 1;
        for (int i = 0; i < cols; i++) {
            String cell = i < cells.length ? cells[i] : "";
            List<String> lines = TextWidth.wrap(cell, widths[i]);
            wrapped.add(lines);
            height = Math.max(height, lines.size());
        }
        for (int line = 0; line < height; line++) {
            StringBuilder out = new StringBuilder();
            if (b.boxed()) {
                out.append(b.c(4)).append(' ');
            }
            for (int i = 0; i < cols; i++) {
                if (i > 0) {
                    if (b.boxed()) {
                        out.append(' ').append(b.c(4)).append(' ');
                    } else {
                        out.append("  ");
                    }
                }
                List<String> lines = wrapped.get(i);
                String text = line < lines.size() ? lines.get(line) : "";
                Align a = aligns[i] == null ? Align.LEFT : aligns[i];
                out.append(TextWidth.pad(text, widths[i], a));
            }
            if (b.boxed()) {
                out.append(' ').append(b.c(4));
            } else {
                // Unboxed tables carry no trailing padding.
                int end = out.length();
                while (end > 0 && out.charAt(end - 1) == ' ') {
                    end--;
                }
                out.setLength(end);
            }
            sb.append(out).append('\n');
        }
    }

    private static void rule(StringBuilder sb, int[] widths, Border b, int left, int fill,
            int join, int right) {
        sb.append(b.c(left));
        for (int i = 0; i < widths.length; i++) {
            if (i > 0) {
                sb.append(b.c(join));
            }
            repeat(sb, b.c(fill), widths[i] + 2);
        }
        sb.append(b.c(right)).append('\n');
    }

    static void repeat(StringBuilder sb, char c, int n) {
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
    }
}
