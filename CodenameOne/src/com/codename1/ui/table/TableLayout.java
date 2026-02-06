/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.table;

import com.codename1.io.Log;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

import java.util.Vector;

/// TableLayout is a very elaborate **constraint based** layout manager that can arrange elements
/// in rows/columns while defining constraints to control complex behavior such as spanning, alignment/weight
/// etc.
///
/// Notice that the table layout is in the `com.codename1.ui.table` package and not in the
/// layouts package.
///
/// This is due to the fact that `TableLayout` was originally designed for the
/// `Table` class.
///
/// Despite being constraint based the table layout isn't strict about constraints and will implicitly add a
/// constraint when one is missing. However, unlike grid layout table layout won't implicitly add a row if the
/// row/column count is incorrect
///
/// E.g this creates a 2x2 table but adds 5 elements. The 5th element won't show:
///
/// ```java
/// Form hi = new Form("Table Layout 2x2", new TableLayout(2, 2));
/// hi.add(new Label("First")).
///     add(new Label("Second")).
///     add(new Label("Third")).
///     add(new Label("Fourth")).
///     add(new Label("Fifth"));
/// hi.show();
/// ```
///
/// Table layout supports the ability to grow the last column which can be enabled using the
/// `setGrowHorizontally` method. You can also use a shortened terse syntax to construct a table
/// layout however since the table layout is a constraint based layout you won't be able to utilize its full power
/// with this syntax.
///
/// The default usage of the encloseIn below uses the `setGrowHorizontally` flag.
///
/// ```java
/// Container tl = TableLayout.encloseIn(2, new Label("First"),
///                 new Label("Second"),
///                 new Label("Third"),
///                 new Label("Fourth"),
///                 new Label("Fifth")));
/// ```
///
/// The Full Potential
///
/// To truly appreciate the `TableLayout` we need to use the constraint syntax which allows
/// us to span, align and set width/height for the rows & columns.
///
/// Table layout works with a `Constraint` instance that can communicate our intentions into the
/// layout manager. Such constraints can include more than one attribute e.g. span and height.
///
/// Notice that table layout constraints can't be reused for more than one component.
///
/// The constraint class supports the following attributes:
///
///
///         column        The column for the table cell. This defaults to -1 which will just place the component in the next available cell
///
///
///         row              Similar to column, defaults to -1 as well
///
///
///        width            The column width in percentages, -1 will use the preferred size. -2 for width will take up the rest of the available space
///
///
///        height           The row height in percentages, -1 will use the preferred size. -2 for height will take up the rest of the available space
///
///
///        spanHorizontal   The cells that should be occupied horizontally defaults to 1 and can't exceed the column count - current offset.
///
///
///        spanVertical     Similar to spanHorizontal with the same limitations
///
///
///        horizontalAlign  The horizontal alignment of the content within the cell, defaults to the special case -1 value to take up all the cell space can be either `-1`, `Component.LEFT`, `Component.RIGHT` or `Component.CENTER`
///
///
///        verticalAlign    Similar to horizontalAlign can be one of `-1`, `Component.TOP`, `Component.BOTTOM` or `Component.CENTER`
///
///
///  Notice that you only need to set `width`/`height` to one cell in a column/row.
///
/// The table layout constraint sample tries to demonstrate some of the unique things you can do with constraints.
///
/// We go into further details on this in the [developer guide](https://www.codenameone.com/manual/basics.html#_table_layout)
/// so check that out.
///
/// ```java
/// TableLayout tl = new TableLayout(2, 3);
/// Form hi = new Form("Table Layout Cons", tl);
/// hi.setScrollable(false);
/// hi.add(tl.createConstraint().
///             widthPercentage(20),
///                 new Label("AAA")).
///
///         add(tl.createConstraint().
///             horizontalSpan(2).
///             heightPercentage(80).
///             verticalAlign(Component.CENTER).
///             horizontalAlign(Component.CENTER),
///                 new Label("Span H")).
///
///         add(new Label("BBB")).
///
///         add(tl.createConstraint().
///             widthPercentage(60).
///             heightPercentage(20),
///                 new Label("CCC")).
///
///         add(tl.createConstraint().
///             widthPercentage(20),
///                 new Label("DDD"));
/// ```
///
/// @author Shai Almog
public class TableLayout extends Layout {

    /// Special case marker SPAN constraint reserving place for other elements
    private static final Constraint H_SPAN_CONSTRAINT = new Constraint();
    private static final Constraint V_SPAN_CONSTRAINT = new Constraint();
    private static final Constraint VH_SPAN_CONSTRAINT = new Constraint();
    private static int minimumSizePerColumn = 10;
    private static int defaultColumnWidth = -1;
    private static int defaultRowHeight = -1;
    private final int columns;
    private int currentRow;
    private int currentColumn;
    private Constraint[] tablePositions;
    private int[] columnPositions;
    private int[] rowPositions;
    private boolean horizontalSpanningExists;
    private boolean verticalSpanningExists;
    private int rows;
    private boolean growHorizontally;
    private boolean truncateHorizontally; //whether we should truncate or shrink the table if the prefered width of all elements exceed the available width. default = false = shrink
    private boolean truncateVertically;

    /// A table must declare the amount of rows and columns in advance
    ///
    /// #### Parameters
    ///
    /// - `rows`: rows of the table
    ///
    /// - `columns`: columns of the table
    public TableLayout(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        tablePositions = new Constraint[rows * columns];
    }

    /// Indicates the minimum size for a column in the table, this is applicable for tables that are
    /// not scrollable on the X axis. This will force the earlier columns to leave room for
    /// the latter columns.
    ///
    /// #### Returns
    ///
    /// the minimum width of the column
    public static int getMinimumSizePerColumn() {
        return minimumSizePerColumn;
    }

    /// Sets the minimum size for a column in the table, this is applicable for tables that are
    /// not scrollable on the X axis. This will force the earlier columns to leave room for
    /// the latter columns.
    ///
    /// #### Parameters
    ///
    /// - `minimumSize`: the minimum width of the column
    public static void setMinimumSizePerColumn(int minimumSize) {
        minimumSizePerColumn = minimumSize;
    }

    /// Indicates the default (in percentage) for the column width, -1 indicates
    /// automatic sizing
    ///
    /// #### Returns
    ///
    /// width in percentage
    public static int getDefaultColumnWidth() {
        return defaultColumnWidth;
    }

    /// Indicates the default (in percentage) for the column width, -1 indicates
    /// automatic sizing
    ///
    /// #### Parameters
    ///
    /// - `w`: width in percentage
    public static void setDefaultColumnWidth(int w) {
        defaultColumnWidth = w;
    }

    /// Indicates the default (in percentage) for the row height, -1 indicates
    /// automatic sizing
    ///
    /// #### Returns
    ///
    /// height in percentage
    public static int getDefaultRowHeight() {
        return defaultRowHeight;
    }

    /// Indicates the default (in percentage) for the row height, -1 indicates
    /// automatic sizing
    ///
    /// #### Parameters
    ///
    /// - `h`: height in percentage
    public static void setDefaultRowHeight(int h) {
        defaultRowHeight = h;
    }

    /// Creates a table layout container that grows the last column horizontally, the number of rows is automatically
    /// calculated based on the number of columns. See usage:
    ///
    /// ```java
    /// Container tl = TableLayout.encloseIn(2, new Label("First"),
    ///                 new Label("Second"),
    ///                 new Label("Third"),
    ///                 new Label("Fourth"),
    ///                 new Label("Fifth")));
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `columns`: the number of columns
    ///
    /// - `cmps`: components to add
    ///
    /// #### Returns
    ///
    /// a newly created table layout container with the components in it
    public static Container encloseIn(int columns, Component... cmps) {
        return encloseIn(columns, true, cmps);
    }

    /// Creates a table layout container, the number of rows is automatically calculated based on the number
    /// of columns. See usage:
    ///
    /// ```java
    /// Container tl = TableLayout.encloseIn(2, new Label("First"),
    ///                 new Label("Second"),
    ///                 new Label("Third"),
    ///                 new Label("Fourth"),
    ///                 new Label("Fifth")));
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `columns`: the number of columns
    ///
    /// - `growHorizontally`: true to grow the last column to fit available width
    ///
    /// - `cmps`: components to add
    ///
    /// #### Returns
    ///
    /// a newly created table layout container with the components in it
    public static Container encloseIn(int columns, boolean growHorizontally, Component... cmps) {
        int rows = cmps.length;
        if (rows % columns > 0) {
            rows = rows / columns + 1;
        } else {
            rows = rows / columns;
        }
        TableLayout tl = new TableLayout(rows, columns);
        tl.setGrowHorizontally(growHorizontally);
        return Container.encloseIn(tl, cmps);
    }

    /// Get the number of rows
    ///
    /// #### Returns
    ///
    /// number of rows
    public int getRows() {
        return rows;
    }

    /// Get the number of columns
    ///
    /// #### Returns
    ///
    /// number of columns
    public int getColumns() {
        return columns;
    }

    /// Returns the component at the given row/column
    ///
    /// #### Parameters
    ///
    /// - `row`: the row of the component
    ///
    /// - `column`: the column of the component
    ///
    /// #### Returns
    ///
    /// the component instance
    public Component getComponentAt(int row, int column) {
        int pos = row * columns + column;
        if (pos > -1 && pos < tablePositions.length) {
            Constraint c = tablePositions[pos];
            return c != null ? c.parent : null;
        }
        return null;
    }

    /// {@inheritDoc}
    @Override
    public void layoutContainer(Container parent) {
        try {
            verticalSpanningExists = false;
            horizontalSpanningExists = false;

            // column and row size in pixels
            Style s = parent.getStyle();
            int top = s.getPaddingTop();
            int left = s.getPaddingLeft(parent.isRTL());
            int bottom = s.getPaddingBottom();
            int right = s.getPaddingRight(parent.isRTL());

            boolean rtl = parent.isRTL();


            //compute columns width and X position
            int[] columnSizes = new int[columns];
            boolean[] modifableColumnSize = new boolean[columns];
            boolean[] growingColumnSize = new boolean[columns];
            columnPositions = new int[columns];

            int pWidth = parent.getLayoutWidth() - parent.getSideGap() - left - right;
            int cslen = columnSizes.length;
            int availableReminder = pWidth;
            int growingWidth = 0;
            boolean hasGrowingCols = false;
            int totalWidth = 0;
            int totalModifyablePixels = 0;

            for (int iter = 0; iter < cslen; iter++) {
                int[] psize = getColumnWidthPixels(iter, pWidth);
                columnSizes[iter] = psize[0];
                availableReminder -= columnSizes[iter];
                totalWidth += columnSizes[iter];
                if (psize[1] < 0) {
                    modifableColumnSize[iter] = true;
                    totalModifyablePixels += columnSizes[iter];
                }
                if (psize[1] < -1) {
                    growingColumnSize[iter] = true;
                    hasGrowingCols = true;
                    growingWidth += columnSizes[iter];
                }
            }

            //If there is some space left and some "auto growing" columns, attribute them the availableReminder space
            if (hasGrowingCols && availableReminder > 0) {
                for (int iter = 0; iter < cslen; iter++) {
                    if (growingColumnSize[iter]) {
                        int sp = (int) (((float) columnSizes[iter]) / ((float) growingWidth) * availableReminder);
                        columnSizes[iter] += sp;
                    }
                }
            }

            // For horizontally scrollable tables, if not enough room is available
            // to correctly display all the components given their preferred width, truncate or shrink the table
            if (!parent.isScrollableX() && pWidth < totalWidth) {
                if (truncateHorizontally) {
                    //TODO: see if this is actually necessary to recompute the column size for truncated columns as the drawer should already automatically clip components with pixels out of the drawing boundaries
                    availableReminder = pWidth;
                    for (int iter = 0; iter < cslen; iter++) {
                        columnSizes[iter] = Math.min(columnSizes[iter], Math.max(0, availableReminder));
                        availableReminder -= columnSizes[iter];
                    }
                } else { // try to recalculate the columns width so they are distributed sensibly
                    int totalPixelsToRemove = totalWidth - pWidth;
                    int totalPixelsNecessary = totalModifyablePixels - totalPixelsToRemove;
                    // Go over the modifyable columns and remove the right pixels according to the ratio
                    for (int iter = 0; iter < cslen; iter++) {
                        if (modifableColumnSize[iter]) {
                            columnSizes[iter] = (int) (((float) columnSizes[iter]) / ((float) totalModifyablePixels) * totalPixelsNecessary);
                        }
                    }
                }
            }

            //Compute X position
            int currentX = left;
            for (int iter = 0; iter < cslen; iter++) {
                if (rtl) {
                    currentX += columnSizes[iter];
                    columnPositions[iter] = pWidth - currentX;
                } else {
                    columnPositions[iter] = currentX;
                    currentX += columnSizes[iter];
                }
            }


            //Compute rows height and Y position
            int[] rowSizes = new int[rows];
            boolean[] modifableRowSize = new boolean[rows];
            boolean[] growingRowSize = new boolean[rows];
            rowPositions = new int[rows];

            int pHeight = parent.getLayoutHeight() - parent.getBottomGap() - top - bottom;
            int rlen = rowSizes.length;
            availableReminder = pHeight;
            int growingHeight = 0;
            boolean hasGrowingRows = false;
            int totalHeight = 0;
            totalModifyablePixels = 0;

            for (int iter = 0; iter < rlen; iter++) {
                int[] psize = getRowHeightPixels(iter, pHeight);
                rowSizes[iter] = psize[0];
                availableReminder -= rowSizes[iter];
                totalHeight += rowSizes[iter];
                if (psize[0] < 0) {
                    modifableRowSize[iter] = true;
                    totalModifyablePixels += rowSizes[iter];
                }
                if (psize[0] < -1) {
                    growingRowSize[iter] = true;
                    hasGrowingRows = true;
                    growingHeight += rowSizes[iter];
                }
            }

            //If there is some space left and some "auto growing" rows, attribute them the availableReminder space
            if (hasGrowingRows && availableReminder > 0) {
                for (int iter = 0; iter < rlen; iter++) {
                    if (growingRowSize[iter]) {
                        int sp = (int) (((float) rowSizes[iter]) / ((float) growingHeight) * availableReminder);
                        rowSizes[iter] += sp;
                    }
                }
            }

            // For vertically scrollable tables, if not enough room is available
            // to correctly display all the components given their preferred height, truncate or shrink the table
            if (!parent.isScrollableY() && pHeight < totalHeight) {
                if (truncateVertically) {
                    //TODO: see if this is actually necessary to recompute the row size for truncated rows as the drawer should already automatically clip components with pixels out of the drawing boundaries
                    availableReminder = pHeight;
                    for (int iter = 0; iter < rlen; iter++) {
                        rowSizes[iter] = Math.min(rowSizes[iter], Math.max(0, availableReminder));
                        availableReminder -= rowSizes[iter];
                    }
                } else { // try to recalculate the rows height so they are distributed sensibly
                    int totalPixelsToRemove = totalHeight - pHeight;
                    int totalPixelsNecessary = totalModifyablePixels - totalPixelsToRemove;
                    // Go over the modifyable rows and remove the bottom pixels according to the ratio
                    for (int iter = 0; iter < rlen; iter++) {
                        if (modifableRowSize[iter]) {
                            rowSizes[iter] = (int) (((float) rowSizes[iter]) / ((float) totalModifyablePixels) * totalPixelsNecessary);
                        }
                    }
                }
            }

            //Compute Y position
            int currentY = top;
            for (int iter = 0; iter < rlen; iter++) {
                rowPositions[iter] = currentY;
                currentY += rowSizes[iter];
            }


            //Place each cell component
            int clen = columnSizes.length;
            for (int r = 0; r < rlen; r++) {
                for (int c = 0; c < clen; c++) {
                    Constraint con = tablePositions[r * columns + c];
                    int conX;
                    int conY;
                    int conW;
                    int conH;
                    if (con != null && con != H_SPAN_CONSTRAINT && con != V_SPAN_CONSTRAINT && con != VH_SPAN_CONSTRAINT) {
                        Style componentStyle = con.parent.getStyle();
                        int leftMargin = componentStyle.getMarginLeft(parent.isRTL());
                        int topMargin = componentStyle.getMarginTop();
                        //                    conX = left + leftMargin + columnPositions[c]; // bugfix table with padding not drawn correctly
                        //                    conY = top + topMargin + rowPositions[r]; // bugfix table with padding not drawn correctly
                        conX = leftMargin + columnPositions[c];
                        conY = topMargin + rowPositions[r];
                        if (con.spanHorizontal > 1) {
                            horizontalSpanningExists = true;
                            int w = columnSizes[c];
                            for (int sh = 1; sh < con.spanHorizontal; sh++) {
                                w += columnSizes[Math.min(c + sh, columnSizes.length - 1)];
                            }

                            // for RTL we need to move the component to the side so spanning will work
                            if (rtl) {
                                int spanEndPos = c + con.spanHorizontal - 1;

                                if (spanEndPos < 0) {
                                    spanEndPos = 0;
                                } else if (spanEndPos > clen - 1) {
                                    spanEndPos = clen - 1;

                                }
                                conX = left + leftMargin + columnPositions[spanEndPos];

                            }
                            conW = w - leftMargin - componentStyle.getMarginRight(parent.isRTL());
                        } else {
                            conW = columnSizes[c] - leftMargin - componentStyle.getMarginRight(parent.isRTL());
                        }
                        if (con.spanVertical > 1) {
                            verticalSpanningExists = true;
                            int h = rowSizes[r];
                            for (int sv = 1; sv < con.spanVertical; sv++) {
                                h += rowSizes[Math.min(r + sv, rowSizes.length - 1)];
                            }
                            conH = h - topMargin - componentStyle.getMarginBottom();
                        } else {
                            conH = rowSizes[r] - topMargin - componentStyle.getMarginBottom();
                        }
                        placeComponent(rtl, con, conX, conY, conW, conH);
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException err) {
            Log.e(err);
        }
    }

    /// Returns the position of the given table row. A valid value is only returned after the
    /// layout occurred.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row in the table
    ///
    /// #### Returns
    ///
    /// the Y position in pixels or -1 if layout hasn't occured/row is too large etc.
    public int getRowPosition(int row) {
        if (rowPositions != null && rowPositions.length > row) {
            return rowPositions[row];
        }
        return -1;
    }

    /// Returns the position of the given table column. A valid value is only returned after the
    /// layout occurred.
    ///
    /// #### Parameters
    ///
    /// - `col`: the column in the table
    ///
    /// #### Returns
    ///
    /// the X position in pixels or -1 if layout hasn't occured/column is too large etc.
    public int getColumnPosition(int col) {
        if (columnPositions != null && columnPositions.length > col) {
            return columnPositions[col];
        }
        return -1;
    }

    /// Places the component/constraint in the proper alignment within the cell whose bounds are given
    private void placeComponent(boolean rtl, Constraint con, int x, int y, int width, int height) {
        con.parent.setX(x);
        con.parent.setY(y);
        con.parent.setWidth(width);
        con.parent.setHeight(height);
        Dimension pref = con.parent.getPreferredSize();
        int pWidth = pref.getWidth();
        int pHeight = pref.getHeight();
        if (pWidth < width) {
            int d = (width - pWidth);
            int a = con.align;
            if (rtl) {
                switch (a) {
                    case Component.LEFT:
                        a = Component.RIGHT;
                        break;
                    case Component.RIGHT:
                        a = Component.LEFT;
                        break;
                    default:
                        break;
                }
            }
            switch (a) {
                case Component.LEFT:
                    con.parent.setX(x);
                    con.parent.setWidth(width - d);
                    break;
                case Component.RIGHT:
                    con.parent.setX(x + d);
                    con.parent.setWidth(width - d);
                    break;
                case Component.CENTER:
                    con.parent.setX(x + d / 2);
                    con.parent.setWidth(width - d);
                    break;
                default:
                    break;
            }
        }
        if (pHeight < height) {
            int d = (height - pHeight);
            switch (con.valign) {
                case Component.TOP:
                    con.parent.setY(y);
                    con.parent.setHeight(height - d);
                    break;
                case Component.BOTTOM:
                    con.parent.setY(y + d);
                    con.parent.setHeight(height - d);
                    break;
                case Component.CENTER:
                    con.parent.setY(y + d / 2);
                    con.parent.setHeight(height - d);
                    break;
                default:
                    break;
            }
        }
    }

    /// #### Parameters
    ///
    /// - `column:`: the column index
    ///
    /// - `percentageOf:`: the table width to take into account to compute percentages constraints. if <0 these constraints are ignored and the max prefered width of the components of this column is returned
    ///
    /// #### Returns
    ///
    /// a size 2 int array with: the prefered width of the column , in pixels, as first element of the array and a constraint code for this column as second element. 0=column width is fixed, -1=column width is modifiable, -2=column width can automatically grow to take all the available space
    private int[] getColumnWidthPixels(int column, int percentageOf) {
        int current = 0;
        boolean foundExplicitWidth = false;
        boolean growable = false;
        for (int iter = 0; iter < rows; iter++) {
            Constraint c = tablePositions[iter * columns + column];

            //ignore "virtual" cells (i.e. cells that are part of a merge)
            if (c == null || c == H_SPAN_CONSTRAINT || c == V_SPAN_CONSTRAINT || c == VH_SPAN_CONSTRAINT || c.spanHorizontal > 1) {
                continue;
            }

            // width in percentage of the parent container
            if (c.width > 0 && percentageOf > -1) {
                current = Math.max(current, c.width * percentageOf / 100);
                foundExplicitWidth = true;
            } else if (!foundExplicitWidth) {
                // special case, width -2 gives the column the rest of the available space (and growHorizontally=true is the same as setting -2 in the width constraint of a cell from the last column. Kept here for historical reasons)
                if (c.width == -2 || (growHorizontally && column == columns - 1)) {
                    growable = true;
                }
                Style s = c.parent.getStyle();
                current = Math.max(current, c.parent.getPreferredW() + s.getMarginLeftNoRTL() + s.getMarginRightNoRTL());
            }
        }

        return new int[]{current, (foundExplicitWidth ? 0 : (growable ? -2 : -1))};
    }

    private int[] getRowHeightPixels(int row, int percentageOf) {
        int current = 0;
        boolean foundExplicitHeight = false;
        boolean growable = false;
        for (int iter = 0; iter < columns; iter++) {
            Constraint c = tablePositions[row * columns + iter];

            if (c == null || c == H_SPAN_CONSTRAINT || c == V_SPAN_CONSTRAINT || c == VH_SPAN_CONSTRAINT || c.spanVertical > 1) {
                continue;
            }

            // height in percentage of the parent container
            if (c.height > 0 && percentageOf > -1) {
                current = Math.max(current, c.height * percentageOf / 100);
                foundExplicitHeight = true;
            } else if (!foundExplicitHeight) {
                // special case, height -2 gives the row the possibility to take the rest of the available space -> tag these rows
                if (c.height == -2) {
                    growable = true;
                }
                Style s = c.parent.getStyle();
                current = Math.max(current, c.parent.getPreferredH() + s.getMarginTop() + s.getMarginBottom());
            }
        }

        return new int[]{current, (foundExplicitHeight ? 0 : (growable ? -2 : -1))};
    }

    /// {@inheritDoc}
    @Override
    public Dimension getPreferredSize(Container parent) {
        Style s = parent.getStyle();
        int w = s.getPaddingLeftNoRTL() + s.getPaddingRightNoRTL();
        int h = s.getPaddingTop() + s.getPaddingBottom();

        for (int iter = 0; iter < columns; iter++) {
            w += getColumnWidthPixels(iter, -1)[0];
        }

        for (int iter = 0; iter < rows; iter++) {
            h += getRowHeightPixels(iter, -1)[0];
        }

        return new Dimension(w, h);
    }

    /// Returns the row where the next operation of add will appear
    ///
    /// #### Returns
    ///
    /// the row where the next operation of add will appear
    public int getNextRow() {
        return currentRow;
    }

    /// Returns the column where the next operation of add will appear
    ///
    /// #### Returns
    ///
    /// the column where the next operation of add will appear
    public int getNextColumn() {
        return currentColumn;
    }

    private void shiftCell(int row, int column) {
        Constraint currentConstraint = tablePositions[row * columns + column];
        for (int iter = column + 1; iter < columns; iter++) {
            if (tablePositions[row * columns + iter] != null) {
                Constraint tmp = tablePositions[row * columns + iter];
                tablePositions[row * columns + iter] = currentConstraint;
                currentConstraint = tmp;
            } else {
                tablePositions[row * columns + iter] = currentConstraint;
                return;
            }
        }
        for (int rowIter = row + 1; rowIter < getRows(); rowIter++) {
            for (int colIter = 0; colIter < getColumns(); colIter++) {
                if (tablePositions[rowIter * columns + colIter] != null) {
                    Constraint tmp = tablePositions[rowIter * columns + colIter];
                    tablePositions[rowIter * columns + colIter] = currentConstraint;
                    currentConstraint = tmp;
                } else {
                    tablePositions[rowIter * columns + colIter] = currentConstraint;
                    return;
                }
            }
        }

        // if we reached this point there aren't enough rows
        addRow();
    }

    private void addRow() {
        rows++;
        Constraint[] newArr = new Constraint[rows * columns];
        System.arraycopy(tablePositions, 0, newArr, 0, tablePositions.length);
        tablePositions = newArr;
    }

    /// {@inheritDoc}
    @Override
    public void addLayoutComponent(Object value, Component comp, Container c) {
        Constraint con = null;
        if (!(value instanceof Constraint)) {
            con = createConstraint();
        } else {
            con = (Constraint) value;
            if (con.parent != null) {
                Constraint con2 = createConstraint();
                con2.align = con.align;
                con2.column = con.column;
                con2.height = con.height;
                con2.parent = c;
                con2.row = con.row;
                con2.spanHorizontal = con.spanHorizontal;
                con2.spanVertical = con.spanVertical;
                con2.valign = con.valign;
                con2.width = con.width;
                con = con2;
            }
        }
        con.actualRow = con.row;
        con.actualColumn = con.column;
        if (con.actualRow < 0) {
            con.actualRow = currentRow;
        }
        if (con.actualColumn < 0) {
            con.actualColumn = currentColumn;
        }
        con.parent = comp;
        if (con.actualRow >= rows) {
            // increase the table row count implicitly
            addRow();
        }
        if (tablePositions[con.actualRow * columns + con.actualColumn] != null) {
            if (tablePositions[con.actualRow * columns + con.actualColumn].row != -1 || tablePositions[con.actualRow * columns + con.actualColumn].column != -1) {
                throw new IllegalArgumentException("Row: " + con.row + " and column: " + con.column + " already occupied");
            }

            // try to reflow the table from this row/column onwards
            shiftCell(con.actualRow, con.actualColumn);
            tablePositions[con.actualRow * columns + con.actualColumn] = con;
        }
        tablePositions[con.actualRow * columns + con.actualColumn] = con;
        if (con.spanHorizontal > 1 || con.spanVertical > 1) {
            for (int sh = 0; sh < con.spanHorizontal; sh++) {
                for (int sv = 0; sv < con.spanVertical; sv++) {
                    if ((sh > 0 || sv > 0) && rows > con.actualRow + sv &&
                            columns > con.actualColumn + sh) {
                        if (tablePositions[(con.actualRow + sv) * columns + con.actualColumn + sh] == null) {
                            if (con.spanHorizontal > 1) {
                                if (con.spanVertical > 1) {
                                    tablePositions[(con.actualRow + sv) * columns + con.actualColumn + sh] = VH_SPAN_CONSTRAINT;
                                } else {
                                    tablePositions[(con.actualRow + sv) * columns + con.actualColumn + sh] = V_SPAN_CONSTRAINT;
                                }
                            } else {
                                tablePositions[(con.actualRow + sv) * columns + con.actualColumn + sh] = H_SPAN_CONSTRAINT;
                            }
                        }
                    }
                }
            }
        }

        updateRowColumn();
    }

    private void updateRowColumn() {
        if (currentRow >= rows) {
            return;
        }
        while (tablePositions[currentRow * columns + currentColumn] != null) {
            currentColumn++;
            if (currentColumn >= columns) {
                currentColumn = 0;
                currentRow++;
                if (currentRow >= rows) {
                    return;
                }
            }
        }
    }

    /// Returns the spanning for the table cell at the given coordinate
    ///
    /// #### Parameters
    ///
    /// - `row`: row in the table
    ///
    /// - `column`: column within the table
    ///
    /// #### Returns
    ///
    /// the amount of spanning 1 for no spanning
    public int getCellHorizontalSpan(int row, int column) {
        return tablePositions[row * columns + column].spanHorizontal;
    }

    /// Returns the spanning for the table cell at the given coordinate
    ///
    /// #### Parameters
    ///
    /// - `row`: row in the table
    ///
    /// - `column`: column within the table
    ///
    /// #### Returns
    ///
    /// the amount of spanning 1 for no spanning
    public int getCellVerticalSpan(int row, int column) {
        return tablePositions[row * columns + column].spanVertical;
    }

    /// Returns true if the cell at the given position is spanned through vertically
    ///
    /// #### Parameters
    ///
    /// - `row`: cell row
    ///
    /// - `column`: cell column
    ///
    /// #### Returns
    ///
    /// true if the cell is a part of a span for another cell
    public boolean isCellSpannedThroughVertically(int row, int column) {
        return tablePositions[row * columns + column] == V_SPAN_CONSTRAINT || tablePositions[row * columns + column] == VH_SPAN_CONSTRAINT;
    }

    /// Returns true if the cell at the given position is spanned through horizontally
    ///
    /// #### Parameters
    ///
    /// - `row`: cell row
    ///
    /// - `column`: cell column
    ///
    /// #### Returns
    ///
    /// true if the cell is a part of a span for another cell
    public boolean isCellSpannedThroughHorizontally(int row, int column) {
        return tablePositions[row * columns + column] == H_SPAN_CONSTRAINT || tablePositions[row * columns + column] == VH_SPAN_CONSTRAINT;
    }

    /// Indicates whether there is spanning within this layout
    ///
    /// #### Returns
    ///
    /// true if the layout makes use of spanning
    public boolean hasVerticalSpanning() {
        return verticalSpanningExists;
    }

    /// Indicates whether there is spanning within this layout
    ///
    /// #### Returns
    ///
    /// true if the layout makes use of spanning
    public boolean hasHorizontalSpanning() {
        return horizontalSpanningExists;
    }

    /// {@inheritDoc}
    @Override
    public void removeLayoutComponent(Component comp) {
        // reflow the table
        Vector comps = new Vector();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (tablePositions[r * columns + c] != null) {
                    if (tablePositions[r * columns + c].parent != comp) {
                        comps.addElement(tablePositions[r * columns + c]);
                    } else {
                        tablePositions[r * columns + c].parent = null;
                    }
                }
                tablePositions[r * columns + c] = null;
            }
        }
        currentRow = 0;
        currentColumn = 0;
        int count = comps.size();
        for (int iter = 0; iter < count; iter++) {
            Constraint con = (Constraint) comps.elementAt(iter);
            if (con == H_SPAN_CONSTRAINT || con == V_SPAN_CONSTRAINT || con == VH_SPAN_CONSTRAINT) {
                continue;
            }
            Component c = con.parent;
            con.parent = null;
            addLayoutComponent(con, c, c.getParent());
        }
    }

    /// {@inheritDoc}
    @Override
    public Object getComponentConstraint(Component comp) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (tablePositions[r * columns + c] != null && tablePositions[r * columns + c].parent == comp) {
                    return tablePositions[r * columns + c];
                }
            }
        }
        return null;
    }

    /// Creates a new Constraint instance to add to the layout
    ///
    /// #### Returns
    ///
    /// the default constraint
    public Constraint createConstraint() {
        return new Constraint();
    }

    /// Creates a new Constraint instance to add to the layout, same as
    /// `createConstraint` only shorter syntax
    ///
    /// #### Returns
    ///
    /// the default constraint
    public Constraint cc() {
        return new Constraint();
    }

    /// Creates a new Constraint instance to add to the layout, same as
    /// `createConstraint` only shorter syntax
    ///
    /// #### Parameters
    ///
    /// - `row`: the row for the table starting with 0
    ///
    /// - `column`: the column for the table starting with 0
    ///
    /// #### Returns
    ///
    /// the new constraint
    public Constraint cc(int row, int column) {
        return createConstraint(row, column);
    }

    /// Creates a new Constraint instance to add to the layout
    ///
    /// #### Parameters
    ///
    /// - `row`: the row for the table starting with 0
    ///
    /// - `column`: the column for the table starting with 0
    ///
    /// #### Returns
    ///
    /// the new constraint
    public Constraint createConstraint(int row, int column) {
        Constraint c = createConstraint();
        c.row = row;
        c.column = column;
        return c;
    }

    /// {@inheritDoc}
    @Override
    public String toString() {
        return "TableLayout";
    }

    /// {@inheritDoc}
    @Override
    public boolean equals(Object o) {
        return super.equals(o) && ((TableLayout) o).getRows() == getRows() && ((TableLayout) o).getColumns() == getColumns();
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ getRows() ^ getColumns();
    }

    /// {@inheritDoc}
    @Override
    public boolean isConstraintTracking() {
        return true;
    }

    /// Indicates whether the table layout should grow horizontally to take up available space by stretching the last column
    ///
    /// #### Returns
    ///
    /// the growHorizontally
    public boolean isGrowHorizontally() {
        return growHorizontally;
    }

    /// Indicates whether the table layout should grow horizontally to take up available space by stretching the last column
    ///
    /// #### Parameters
    ///
    /// - `growHorizontally`: the growHorizontally to set
    public void setGrowHorizontally(boolean growHorizontally) {
        this.growHorizontally = growHorizontally;
    }

    /// Indicates whether the table should be truncated if it do not have enough available horizontal space to display all its content. If not, will shrink
    ///
    /// #### Returns
    ///
    /// the truncateHorizontally
    public boolean isTruncateHorizontally() {
        return truncateHorizontally;
    }

    /// Indicates whether the table should be truncated if it do not have enough available horizontal space to display all its content. If not, will shrink
    ///
    /// #### Parameters
    ///
    /// - `truncateHorizontally`: the truncateHorizontally to set
    public void setTruncateHorizontally(boolean truncateHorizontally) {
        this.truncateHorizontally = truncateHorizontally;
    }

    /// Indicates whether the table should be truncated if it do not have enough available vertical space to display all its content. If not, will shrink
    ///
    /// #### Returns
    ///
    /// the truncateVertically
    public boolean isTruncateVertically() {
        return truncateVertically;
    }

    /// Indicates whether the table should be truncated if it do not have enough available vertical space to display all its content. If not, will shrink
    ///
    /// #### Parameters
    ///
    /// - `truncateVertically`: the truncateVertically to set
    public void setTruncateVertically(boolean truncateVertically) {
        this.truncateVertically = truncateVertically;
    }

    @Override
    public boolean overridesTabIndices(Container parent) {
        return true;
    }

    @Override
    protected Component[] getChildrenInTraversalOrder(Container parent) {
        int len = tablePositions.length;
        Component[] out = new Component[len];
        for (int i = 0; i < len; i++) {
            Constraint con = tablePositions[i];
            if (con != null) {
                out[i] = tablePositions[i].parent;
            }
        }
        return out;
    }

    /// Represents the layout constraint for an entry within the table indicating
    /// the desired position/behavior of the component.
    public static class Constraint {
        int actualRow = -1;
        int actualColumn = -1;
        private Component parent;
        private int row = -1;
        private int column = -1;
        private int width = defaultColumnWidth;
        private int height = defaultRowHeight;
        private int spanHorizontal = 1;
        private int spanVertical = 1;
        private int align = -1;
        private int valign = -1;

        /// {@inheritDoc}
        @Override
        public String toString() {
            return "row: " + row + " column: " + column + " width: " + width + " height: " + height + " hspan: " +
                    spanHorizontal + " vspan: " + spanVertical + " align " + align + " valign " + valign;
        }

        /// Sets the cells to span vertically, this number must never be smaller than 1
        ///
        /// #### Parameters
        ///
        /// - `span`: a number larger than 1
        ///
        /// #### Returns
        ///
        /// this
        public Constraint verticalSpan(int span) {
            setVerticalSpan(span);
            return this;
        }

        /// Sets the cells to span vertically, this number must never be smaller than 1
        ///
        /// #### Parameters
        ///
        /// - `span`: a number larger than 1
        ///
        /// #### Returns
        ///
        /// this
        public Constraint vs(int span) {
            return verticalSpan(span);
        }

        /// Sets the cells to span horizontally, this number must never be smaller than 1
        ///
        /// #### Parameters
        ///
        /// - `span`: a number larger than 1
        public Constraint horizontalSpan(int span) {
            setHorizontalSpan(span);
            return this;
        }

        /// Sets the cells to span horizontally, this number must never be smaller than 1
        ///
        /// #### Parameters
        ///
        /// - `span`: a number larger than 1
        public Constraint hs(int span) {
            return horizontalSpan(span);
        }

        /// Sets the column width based on percentage of the parent
        ///
        /// #### Parameters
        ///
        /// - `width`: negative number indicates ignoring this member
        public Constraint widthPercentage(int width) {
            this.width = width;
            return this;
        }

        /// Sets the column width based on percentage of the parent
        ///
        /// #### Parameters
        ///
        /// - `width`: negative number indicates ignoring this member
        public Constraint wp(int width) {
            return widthPercentage(width);
        }

        /// Sets the row height based on percentage of the parent
        ///
        /// #### Parameters
        ///
        /// - `height`: negative number indicates ignoring this member
        public Constraint heightPercentage(int height) {
            this.height = height;
            return this;
        }

        /// Sets the row height based on percentage of the parent
        ///
        /// #### Parameters
        ///
        /// - `height`: negative number indicates ignoring this member
        public Constraint hp(int height) {
            return heightPercentage(height);
        }

        /// Sets the horizontal alignment of the table cell
        ///
        /// #### Parameters
        ///
        /// - `align`: Component.LEFT/RIGHT/CENTER
        public Constraint horizontalAlign(int align) {
            this.align = align;
            return this;
        }

        /// Sets the horizontal alignment of the table cell
        ///
        /// #### Parameters
        ///
        /// - `align`: Component.LEFT/RIGHT/CENTER
        public Constraint ha(int align) {
            return horizontalAlign(align);
        }

        /// Sets the vertical alignment of the table cell
        ///
        /// #### Parameters
        ///
        /// - `valign`: Component.TOP/BOTTOM/CENTER
        public Constraint verticalAlign(int valign) {
            this.valign = valign;
            return this;
        }

        /// Sets the vertical alignment of the table cell
        ///
        /// #### Parameters
        ///
        /// - `valign`: Component.TOP/BOTTOM/CENTER
        public Constraint va(int valign) {
            return verticalAlign(valign);
        }

        /// #### Returns
        ///
        /// the row
        public int getRow() {
            return row;
        }

        /// #### Returns
        ///
        /// the column
        public int getColumn() {
            return column;
        }

        /// #### Returns
        ///
        /// the width
        public int getWidthPercentage() {
            return width;
        }

        /// Sets the column width based on percentage of the parent
        ///
        /// #### Parameters
        ///
        /// - `width`: negative number indicates ignoring this member
        public void setWidthPercentage(int width) {
            this.width = width;
        }

        /// #### Returns
        ///
        /// the height
        public int getHeightPercentage() {
            return height;
        }

        /// Sets the row height based on percentage of the parent
        ///
        /// #### Parameters
        ///
        /// - `height`: negative number indicates ignoring this member
        public void setHeightPercentage(int height) {
            this.height = height;
        }

        /// #### Returns
        ///
        /// the spanHorizontal
        public int getHorizontalSpan() {
            return spanHorizontal;
        }

        /// Sets the cells to span horizontally, this number must never be smaller than 1
        ///
        /// #### Parameters
        ///
        /// - `span`: a number larger than 1
        public void setHorizontalSpan(int span) {
            if (span < 1) {
                throw new IllegalArgumentException("Illegal span");
            }
            spanHorizontal = span;
        }

        /// #### Returns
        ///
        /// the spanVertical
        public int getVerticalSpan() {
            return spanVertical;
        }

        /// Sets the cells to span vertically, this number must never be smaller than 1
        ///
        /// #### Parameters
        ///
        /// - `span`: a number larger than 1
        public void setVerticalSpan(int span) {
            if (span < 1) {
                throw new IllegalArgumentException("Illegal span");
            }
            spanVertical = span;
        }

        /// #### Returns
        ///
        /// the align
        public int getHorizontalAlign() {
            return align;
        }

        /// Sets the horizontal alignment of the table cell
        ///
        /// #### Parameters
        ///
        /// - `align`: Component.LEFT/RIGHT/CENTER
        public void setHorizontalAlign(int align) {
            this.align = align;
        }

        /// #### Returns
        ///
        /// the valign
        public int getVerticalAlign() {
            return valign;
        }

        /// Sets the vertical alignment of the table cell
        ///
        /// #### Parameters
        ///
        /// - `valign`: Component.TOP/BOTTOM/CENTER
        public void setVerticalAlign(int valign) {
            this.valign = valign;
        }
    }

}
