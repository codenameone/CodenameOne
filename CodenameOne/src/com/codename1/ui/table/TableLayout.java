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

import com.codename1.ui.layouts.*;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import java.util.Vector;

/**
 * <p>TableLayout is a very elaborate <b>constraint based</b> layout manager that can arrange elements 
 * in rows/columns while defining constraints to control complex behavior such as spanning, alignment/weight 
 * etc.<br>
 * Notice that the table layout is in the <code>com.codename1.ui.table</code> package and not in the 
 * layouts package. <br>
 * This is due to the fact that <code>TableLayout</code> was originally designed for the 
 * {@link Table} class.</p>
 *
 * <p>Despite being constraint based the table layout isn't strict about constraints and will implicitly add a 
 * constraint when one is missing. However, unlike grid layout table layout won't implicitly add a row if the 
 * row/column count is incorrect<br>
 * E.g this creates a 2x2 table but adds 5 elements. The 5th element won't show:</p>
 * <script src="https://gist.github.com/codenameone/a25944769128d5330cd4.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/table-layout-2x2.png" alt="TableLayout 2x2 missing component" />
 *
 * <p>Table layout supports the ability to grow the last column which can be enabled using the 
 * <code>setGrowHorizontally</code> method. You can also use a shortened terse syntax to construct a table 
 * layout however since the table layout is a constraint based layout you won't be able to utilize its full power 
 * with this syntax.</>
 * 
 * <p>The default usage of the encloseIn below uses the <code>setGrowHorizontally</code> flag.</p>
 *<script src="https://gist.github.com/codenameone/2b4d9a13f409e297fb2e.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/table-layout-enclose.png" alt="TableLayout that grows the last column" />
 *
 * <h4>The Full Potential</h4>
 *<p>To truly appreciate the {@code TableLayout} we need to use the constraint syntax which allows 
 * us to span, align and set width/height for the rows &amp; columns.<br>
 * Table layout works with a {@link Constraint} instance that can communicate our intentions into the 
 * layout manager. Such constraints can include more than one attribute e.g. span and height.</p>
 *
 * <p>Notice that table layout constraints can't be reused for more than one component.<br>
 * The constraint class supports the following attributes:</p>
 *
 * <table summary="">
 *   <tr>
 *        <td> column       </td><td> The column for the table cell. This defaults to -1 which will just place the component in the next available cell</td>
 *   </tr>
 *   <tr>
 *        <td> row             </td><td> Similar to column, defaults to -1 as well</td>
 *   </tr>
 *   <tr>
 *       <td> width           </td><td> The column width in percentages, -1 will use the preferred size. -2 for width will take up the rest of the available space</td>
 *   </tr>
 *   <tr>
 *       <td> height          </td><td> Similar to width but doesn't support the -2 value</td>
 *   </tr>
 *   <tr>
 *       <td> spanHorizontal  </td><td> The cells that should be occupied horizontally defaults to 1 and can't exceed the column count - current offset.</td>
 *   </tr>
 *   <tr>
 *       <td> spanVertical    </td><td> Similar to spanHorizontal with the same limitations</td>
 *   </tr>
 *   <tr>
 *       <td> horizontalAlign </td><td> The horizontal alignment of the content within the cell, defaults to the special case -1 value to take up all the cell space can be either `-1`, `Component.LEFT`, `Component.RIGHT` or `Component.CENTER`</td>
 *   </tr>
 *   <tr>
 *       <td> verticalAlign   </td><td> Similar to horizontalAlign can be one of `-1`, `Component.TOP`, `Component.BOTTOM` or `Component.CENTER`</td>
 *   </tr>
 * </table>
 * 
 * <p> Notice that you only need to set `width`/`height` to one cell in a column/row.<br>
 * The table layout constraint sample tries to demonstrate some of the unique things you can do with constraints.<br>
 *
 * We go into further details on this in the <a href="https://www.codenameone.com/manual/basics.html#_table_layout">developer guide</a> 
 * so check that out.</p>
 * <script src="https://gist.github.com/codenameone/573f73164df4af00b7b1.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/table-layout-constraints.png" alt="TableLayout with complex constraints" />
 *
 * @author Shai Almog
 */
public class TableLayout extends Layout {

    /**
     * Represents the layout constraint for an entry within the table indicating
     * the desired position/behavior of the component.
     */
    public static class Constraint {
        private Component parent;
        private int row = -1;
        private int column = -1;
        private int width = defaultColumnWidth;
        private int height = defaultRowHeight;
        private int spanHorizontal = 1;
        private int spanVertical = 1;
        private int align = -1;
        private int valign = -1;
        int actualRow = -1;
        int actualColumn = -1;

        /**
         * {@inheritDoc}
         */
        public String toString() {
            return "row: " + row + " column: " + column + " width: " + width + " height: " + height + " hspan: " + 
                    spanHorizontal + " vspan: " + spanVertical + " align " + align + " valign " + valign;
        }
        
        /**
         * Sets the cells to span vertically, this number must never be smaller than 1
         *
         * @param span a number larger than 1
         */
        public void setVerticalSpan(int span) {
            if(span < 1) {
                throw new IllegalArgumentException("Illegal span");
            }
            spanVertical = span;
        }

        /**
         * Sets the cells to span vertically, this number must never be smaller than 1
         *
         * @param span a number larger than 1
         * @return this
         */
        public Constraint verticalSpan(int span) {
            setVerticalSpan(span);
            return this;
        }

        /**
         * Sets the cells to span horizontally, this number must never be smaller than 1
         *
         * @param span a number larger than 1
         */
        public void setHorizontalSpan(int span) {
            if(span < 1) {
                throw new IllegalArgumentException("Illegal span");
            }
            spanHorizontal = span;
        }

        /**
         * Sets the cells to span horizontally, this number must never be smaller than 1
         *
         * @param span a number larger than 1
         */
        public Constraint horizontalSpan(int span) {
            setHorizontalSpan(span);
            return this;
        }

        /**
         * Sets the column width based on percentage of the parent
         *
         * @param width negative number indicates ignoring this member
         */
        public void setWidthPercentage(int width) {
            this.width = width;
        }

        /**
         * Sets the column width based on percentage of the parent
         *
         * @param width negative number indicates ignoring this member
         */
        public Constraint widthPercentage(int width) {
            this.width = width;
            return this;
        }

        /**
         * Sets the row height based on percentage of the parent
         *
         * @param height negative number indicates ignoring this member
         */
        public void setHeightPercentage(int height) {
            this.height = height;
        }

        /**
         * Sets the row height based on percentage of the parent
         *
         * @param height negative number indicates ignoring this member
         */
        public Constraint heightPercentage(int height) {
            this.height = height;
            return this;
        }

        /**
         * Sets the horizontal alignment of the table cell
         *
         * @param align Component.LEFT/RIGHT/CENTER
         */
        public void setHorizontalAlign(int align) {
            this.align = align;
        }

        /**
         * Sets the horizontal alignment of the table cell
         *
         * @param align Component.LEFT/RIGHT/CENTER
         */
        public Constraint horizontalAlign(int align) {
            this.align = align;
            return this;
        }

        /**
         * Sets the vertical alignment of the table cell
         *
         * @param valign Component.TOP/BOTTOM/CENTER
         */
        public void setVerticalAlign(int valign) {
            this.valign = valign;
        }

        /**
         * Sets the vertical alignment of the table cell
         *
         * @param valign Component.TOP/BOTTOM/CENTER
         */
        public Constraint verticalAlign(int valign) {
            this.valign = valign;
            return this;
        }

        /**
         * @return the row
         */
        public int getRow() {
            return row;
        }

        /**
         * @return the column
         */
        public int getColumn() {
            return column;
        }

        /**
         * @return the width
         */
        public int getWidthPercentage() {
            return width;
        }

        /**
         * @return the height
         */
        public int getHeightPercentage() {
            return height;
        }

        /**
         * @return the spanHorizontal
         */
        public int getHorizontalSpan() {
            return spanHorizontal;
        }

        /**
         * @return the spanVertical
         */
        public int getVerticalSpan() {
            return spanVertical;
        }

        /**
         * @return the align
         */
        public int getHorizontalAlign() {
            return align;
        }

        /**
         * @return the valign
         */
        public int getVerticalAlign() {
            return valign;
        }
    }

    private int currentRow;
    private int currentColumn;

    private static int minimumSizePerColumn = 10;
    private Constraint[] tablePositions;

    private int[] columnSizes;
    private int[] columnPositions;
    private int[] rowPositions;
    private boolean[] modifableColumnSize;

    /**
     * Special case marker SPAN constraint reserving place for other elements
     */
    private static final Constraint H_SPAN_CONSTRAINT = new Constraint();
    private static final Constraint V_SPAN_CONSTRAINT = new Constraint();
    private static final Constraint VH_SPAN_CONSTRAINT = new Constraint();

    private static int defaultColumnWidth = -1;
    private static int defaultRowHeight = -1;
    private boolean horizontalSpanningExists;
    private boolean verticalSpanningExists;

    private int rows, columns;
    private boolean growHorizontally;
    
    /**
     * A table must declare the amount of rows and columns in advance
     *
     * @param rows rows of the table
     * @param columns columns of the table
     */
    public TableLayout(int rows, int columns) {
    	this.rows = rows;
    	this.columns = columns;
        tablePositions = new Constraint[rows * columns];
    }

    /**
     * Get the number of rows
     * @return number of rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * Get the number of columns
     * @return number of columns
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Returns the component at the given row/column
     * 
     * @param row the row of the component
     * @param column the column of the component
     * @return the component instance
     */
    public Component getComponentAt(int row, int column) {
        Constraint  c =tablePositions[row * columns + column];
        return c != null ? c.parent : null;
    }

    /**
     * {@inheritDoc}
     */
    public void layoutContainer(Container parent) {
        try {
            verticalSpanningExists = false;
            horizontalSpanningExists = false;

            // column and row size in pixels
            Style s = parent.getStyle();
            int top = s.getPadding(false, Component.TOP);
            int left = s.getPadding(parent.isRTL(), Component.LEFT);
            int bottom = s.getPadding(false, Component.BOTTOM);
            int right = s.getPadding(parent.isRTL(), Component.RIGHT);

            boolean rtl = parent.isRTL();

            columnSizes = new int[columns];
            if(modifableColumnSize == null || columns != modifableColumnSize.length) {
                modifableColumnSize = new boolean[columns];
            }
            columnPositions = new int[columns];
            int[] rowSizes = new int[rows];
            rowPositions = new int[rows];

            int pWidth = parent.getLayoutWidth() - parent.getSideGap() - left - right; 
            int pHeight = parent.getLayoutHeight() - parent.getBottomGap() - top - bottom; 

            int currentX = left;
            int availableReminder = pWidth;
            int cslen = columnSizes.length;
            for(int iter = 0 ; iter < cslen ; iter++) {
                columnSizes[iter] = getColumnWidthPixels(iter, pWidth, availableReminder);
                availableReminder -= columnSizes[iter];
            }

            // try to recalculate the columns for none horizontally scrollable tables
            // so they are distributed sensibly if no room is available
            if(!parent.isScrollableX()) {
                int totalWidth = 0;
                int totalModifyablePixels = 0;

                // check how many columns we can modify (the user hasn't requested a specific size for those)
                for(int iter = 0 ; iter < modifableColumnSize.length ; iter++) {
                    if(modifableColumnSize[iter]) {
                        totalModifyablePixels += columnSizes[iter];
                    }
                    totalWidth += columnSizes[iter];
                }
                if(pWidth < totalWidth) {
                    int totalPixelsToRemove = totalWidth - pWidth;

                    int totalPixelsNecessary = totalModifyablePixels - totalPixelsToRemove;

                    // Go over the modifyable columns and remove the right pixels according to the ratio
                    for(int iter = 0 ; iter < modifableColumnSize.length ; iter++) {
                        if(modifableColumnSize[iter]) {
                            columnSizes[iter] = (int)(((float)columnSizes[iter]) / ((float)totalModifyablePixels) * totalPixelsNecessary);
                        }
                    }
                }
            }

            for(int iter = 0 ; iter < columnSizes.length ; iter++) {
                if(rtl) {
                    currentX += columnSizes[iter];
                    columnPositions[iter] = pWidth - currentX;
                } else {
                    columnPositions[iter] = currentX;
                    currentX += columnSizes[iter];
                }
            }

            int currentY = top;
            int rlen = rowSizes.length;
            for(int iter = 0 ; iter < rlen ; iter++) {
                if(parent.isScrollableY()) {
                    rowSizes[iter] = getRowHeightPixels(iter, pHeight, -1);
                } else {
                    rowSizes[iter] = getRowHeightPixels(iter, pHeight, pHeight - currentY + top);
                }
                rowPositions[iter] = currentY;
                currentY += rowSizes[iter];
            }

            int clen = columnSizes.length;
            for(int r = 0 ; r < rlen ; r++) {
                for(int c = 0 ; c < clen ; c++) {
                    Constraint con = tablePositions[r * columns + c];
                    int conX, conY, conW, conH;
                    if(con != null && con != H_SPAN_CONSTRAINT && con != V_SPAN_CONSTRAINT && con != VH_SPAN_CONSTRAINT) {
                        Style componentStyle = con.parent.getStyle();
                        int leftMargin = componentStyle.getMargin(parent.isRTL(), Component.LEFT);
                        int topMargin = componentStyle.getMargin(false, Component.TOP);
    //                    conX = left + leftMargin + columnPositions[c]; // bugfix table with padding not drawn correctly
    //                    conY = top + topMargin + rowPositions[r]; // bugfix table with padding not drawn correctly
                        conX = leftMargin + columnPositions[c];
                        conY = topMargin + rowPositions[r];
                        if(con.spanHorizontal > 1) {
                            horizontalSpanningExists = true;
                            int w = columnSizes[c];
                            for(int sh = 1 ; sh < con.spanHorizontal ; sh++) {
                                w += columnSizes[Math.min(c + sh, columnSizes.length - 1)];
                            }

                            // for RTL we need to move the component to the side so spanning will work
                            if(rtl) {
                                conX = left + leftMargin + columnPositions[c + con.spanHorizontal - 1];
                            }
                            conW = w - leftMargin - componentStyle.getMargin(parent.isRTL(), Component.RIGHT);
                        } else {
                            conW = columnSizes[c] - leftMargin - componentStyle.getMargin(parent.isRTL(), Component.RIGHT);
                        }
                        if(con.spanVertical > 1) {
                            verticalSpanningExists = true;
                            int h = rowSizes[r];
                            for(int sv = 1 ; sv < con.spanVertical ; sv++) {
                                h += rowSizes[Math.min(r + sv, rowSizes.length - 1)];
                            }
                            conH = h - topMargin - componentStyle.getMargin(false, Component.BOTTOM);
                        } else {
                            conH = rowSizes[r] - topMargin - componentStyle.getMargin(false, Component.BOTTOM);
                        }
                        placeComponent(rtl, con, conX, conY, conW, conH);
                    }
                }
            }
        } catch(ArrayIndexOutOfBoundsException err) {
            err.printStackTrace();
        }
    }

    /**
     * Returns the position of the given table row. A valid value is only returned after the
     * layout occurred.
     *
     * @param row the row in the table
     * @return the Y position in pixels or -1 if layout hasn't occured/row is too large etc.
     */
    public int getRowPosition(int row) {
        if(rowPositions != null && rowPositions.length > row) {
            return rowPositions[row];
        }
        return -1;
    }

    /**
     * Returns the position of the given table column. A valid value is only returned after the 
     * layout occurred.
     * 
     * @param col the column in the table
     * @return the X position in pixels or -1 if layout hasn't occured/column is too large etc.
     */
    public int getColumnPosition(int col) {
        if(columnPositions != null && columnPositions.length > col) {
            return columnPositions[col];
        }
        return -1;
    }

    /**
     * Places the component/constraint in the proper alignment within the cell whose bounds are given
     */
    private void placeComponent(boolean rtl, Constraint con, int x, int y, int width, int height) {
        con.parent.setX(x);
        con.parent.setY(y);
        con.parent.setWidth(width);
        con.parent.setHeight(height);
        Dimension pref = con.parent.getPreferredSize();
        int pWidth = pref.getWidth();
        int pHeight = pref.getHeight();
        if(pWidth < width) {
            int d = (width - pWidth);
            int a = con.align;
            if(rtl) {
                switch(a) {
                    case Component.LEFT:
                        a = Component.RIGHT;
                        break;
                    case Component.RIGHT:
                        a = Component.LEFT;
                        break;
                }
            }
            switch(a) {
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
            }
        }
        if(pHeight < height) {
            int d = (height - pHeight);
            switch(con.valign) {
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
            }
        }
    }

    private int getColumnWidthPixels(int column, int percentageOf, int available) {
        int current = 0;
        if(modifableColumnSize == null) {
            modifableColumnSize = new boolean[columns];
        }
        
        int availableSpaceColumn = -1;

        for(int iter = 0 ; iter < rows ; iter++) {
            Constraint c = tablePositions[iter * columns + column];

            if(c == null || c == H_SPAN_CONSTRAINT || c == V_SPAN_CONSTRAINT || c == VH_SPAN_CONSTRAINT || c.spanHorizontal > 1) {
                continue;
            }

            // width in percentage of the parent container
            if(c.width > 0 && available > -1) {
                current = Math.max(current, c.width * percentageOf / 100);
                modifableColumnSize[column] = false;
            } else {
                // special case, width -2 gives the column the rest of the available space
                if(c.width == -2 || (growHorizontally && column == columns - 1)) {
                    if(available < 0) {
                        return Display.getInstance().getDisplayWidth();
                    }
                    return available;
                }
                Style s = c.parent.getStyle();
                current = Math.max(current, c.parent.getPreferredW()  + s.getMargin(false, Component.LEFT) + s.getMargin(false, Component.RIGHT));
                modifableColumnSize[column] = true;
            }
            if(available > -1) {
                current = Math.min(available, current);
            }
        }
        if(availableSpaceColumn > -1) {
            modifableColumnSize[availableSpaceColumn] = false;
            return percentageOf - current;
        }
        return current;
    }

    private int getRowHeightPixels(int row, int percentageOf, int available) {
        int current = 0;
        for(int iter = 0 ; iter < columns ; iter++) {
            Constraint c = tablePositions[row * columns + iter];

            if(c == null || c == H_SPAN_CONSTRAINT || c == V_SPAN_CONSTRAINT || c == VH_SPAN_CONSTRAINT) {
                continue;
            }

            // height in percentage of the parent container
            if(c.height > 0) {
                current = Math.max(current, c.height * percentageOf / 100);
            } else {
                Style s = c.parent.getStyle();
                current = Math.max(current, c.parent.getPreferredH() + s.getMargin(false, Component.TOP) + s.getMargin(false, Component.BOTTOM));
            }
            if(available > -1) {
                current = Math.min(available, current);
            }
        }
        return current;
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getPreferredSize(Container parent) {
        Style s = parent.getStyle();
        int w = s.getPadding(false, Component.LEFT) + s.getPadding(false, Component.RIGHT);
        int h = s.getPadding(false, Component.TOP) + s.getPadding(false, Component.BOTTOM);

        int maxW = Display.getInstance().getDisplayWidth() * 2;
        int maxH = Display.getInstance().getDisplayHeight() * 2;
        for(int iter = 0 ; iter < columns ; iter++) {
            w += getColumnWidthPixels(iter, maxW, -1);
        }

        for(int iter = 0 ; iter < rows ; iter++) {
            h += getRowHeightPixels(iter, maxH, -1);
        }

        return new Dimension(w, h);
    }

    /**
     * Returns the row where the next operation of add will appear
     *
     * @return the row where the next operation of add will appear
     */
    public int getNextRow() {
        return currentRow;
    }

    /**
     * Returns the column where the next operation of add will appear
     *
     * @return the column where the next operation of add will appear
     */
    public int getNextColumn() {
        return currentColumn;
    }

    private void shiftCell(int row, int column) {
        Constraint currentConstraint = tablePositions[row * columns + column];
        for(int iter = column + 1 ; iter < columns ; iter++) {
            if(tablePositions[row * columns + iter] != null) {
                Constraint tmp = tablePositions[row * columns + iter];
                tablePositions[row * columns + iter] = currentConstraint;
                currentConstraint = tmp;
            } else {
                tablePositions[row * columns + iter] = currentConstraint;
                return;
            }
        }
        for(int rowIter = row + 1 ; rowIter < getRows() ; rowIter++) {
            for(int colIter = 0 ; colIter < getColumns() ; colIter++) {
                if(tablePositions[rowIter * columns + colIter] != null) {
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
        Constraint[] newArr = new Constraint[(rows + 1) * columns];
        System.arraycopy(tablePositions, 0, newArr, 0, tablePositions.length);
        tablePositions = newArr;
    }

    /**
     * {@inheritDoc}
     */
    public void addLayoutComponent(Object value, Component comp, Container c) {
        Constraint con = null;
        if(!(value instanceof Constraint)) {
            con = createConstraint();
        } else {
            con = (Constraint)value;
            if(con.parent != null) {
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
        if(con.actualRow < 0) {
            con.actualRow = currentRow;
        }
        if(con.actualColumn < 0) {
            con.actualColumn = currentColumn;
        }
        con.parent = comp;
        if(con.actualRow >= rows) {
            // increase the table row count implicitly
            addRow();
        }
        if(tablePositions[con.actualRow * columns + con.actualColumn] != null) {
            if(tablePositions[con.actualRow * columns + con.actualColumn].row != -1 || tablePositions[con.actualRow * columns + con.actualColumn].column != -1) {
                throw new IllegalArgumentException("Row: " + con.row + " and column: " + con.column + " already occupied");
            }

            // try to reflow the table from this row/column onwards
            shiftCell(con.actualRow, con.actualColumn);
            tablePositions[con.actualRow * columns + con.actualColumn] = con;
        }
        tablePositions[con.actualRow * columns + con.actualColumn] = con;
        if(con.spanHorizontal > 1 || con.spanVertical > 1) {
            for(int sh = 0 ; sh < con.spanHorizontal ; sh++) {
                for(int sv = 0 ; sv < con.spanVertical ; sv++) {
                    if((sh > 0 || sv > 0) && rows > con.actualRow + sv &&
                            columns > con.actualColumn + sh) {
                        if(tablePositions[(con.actualRow + sv) * columns + con.actualColumn + sh] == null) {
                            if(con.spanHorizontal > 1) {
                                if(con.spanVertical > 1) {
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
        if(currentRow >= rows) {
            return;
        }
        while(tablePositions[currentRow * columns + currentColumn] != null) {
            currentColumn++;
            if(currentColumn >= columns) {
                currentColumn = 0;
                currentRow++;
                if(currentRow >= rows) {
                    return;
                }
            }
        }
    }

    /**
     * Returns the spanning for the table cell at the given coordinate
     * 
     * @param row row in the table
     * @param column column within the table
     * @return the amount of spanning 1 for no spanning
     */
    public int getCellHorizontalSpan(int row, int column) {
        return tablePositions[row * columns + column].spanHorizontal;
    }

    /**
     * Returns the spanning for the table cell at the given coordinate
     *
     * @param row row in the table
     * @param column column within the table
     * @return the amount of spanning 1 for no spanning
     */
    public int getCellVerticalSpan(int row, int column) {
        return tablePositions[row * columns + column].spanVertical;
    }

    /**
     * Returns true if the cell at the given position is spanned through vertically
     * 
     * @param row cell row
     * @param column cell column
     * @return true if the cell is a part of a span for another cell
     */
    public boolean isCellSpannedThroughVertically(int row, int column) {
        return tablePositions[row * columns + column] == V_SPAN_CONSTRAINT || tablePositions[row * columns + column] == VH_SPAN_CONSTRAINT;
    }

    /**
     * Returns true if the cell at the given position is spanned through horizontally
     *
     * @param row cell row
     * @param column cell column
     * @return true if the cell is a part of a span for another cell
     */
    public boolean isCellSpannedThroughHorizontally(int row, int column) {
        return tablePositions[row * columns + column] == H_SPAN_CONSTRAINT || tablePositions[row * columns + column] == VH_SPAN_CONSTRAINT;
    }

    /**
     * Indicates whether there is spanning within this layout
     *
     * @return true if the layout makes use of spanning
     */
    public boolean hasVerticalSpanning() {
        return verticalSpanningExists;
    }

    /**
     * Indicates whether there is spanning within this layout
     * 
     * @return true if the layout makes use of spanning
     */
    public boolean hasHorizontalSpanning() {
        return horizontalSpanningExists;
    }

    /**
     * {@inheritDoc}
     */
    public void removeLayoutComponent(Component comp) {
        // reflow the table
        Vector comps = new Vector();
        for(int r = 0 ; r < rows ; r++) {
            for(int c = 0 ; c < columns ; c++) {
                if(tablePositions[r * columns + c] != null) {
                    if(tablePositions[r * columns + c].parent != comp) {
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
        for(int iter = 0 ; iter < count ; iter++) {
            Constraint con = (Constraint)comps.elementAt(iter);
            if(con == H_SPAN_CONSTRAINT || con == V_SPAN_CONSTRAINT || con == VH_SPAN_CONSTRAINT) {
                continue;
            }
            Component c = con.parent;
            con.parent = null;
            addLayoutComponent(con, c, c.getParent());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getComponentConstraint(Component comp) {
        for(int r = 0 ; r < rows ; r++) {
            for(int c = 0 ; c < columns ; c++) {
                if(tablePositions[r * columns + c] != null && tablePositions[r * columns + c].parent == comp) {
                    return tablePositions[r * columns + c];
                }
            }
        }
        return null;
    }

    /**
     * Creates a new Constraint instance to add to the layout
     *
     * @return the default constraint
     */
    public Constraint createConstraint() {
        return new Constraint();
    }

    /**
     * Creates a new Constraint instance to add to the layout
     *
     * @param row the row for the table starting with 0
     * @param column the column for the table starting with 0
     * @return the new constraint
     */
    public Constraint createConstraint(int row, int column) {
        Constraint c = createConstraint();
        c.row = row;
        c.column = column;
        return c;
    }

    /**
     * Sets the minimum size for a column in the table, this is applicable for tables that are
     * not scrollable on the X axis. This will force the earlier columns to leave room for
     * the latter columns.
     *
     * @param minimumSize the minimum width of the column
     */
    public static void setMinimumSizePerColumn(int minimumSize) {
        minimumSizePerColumn = minimumSize;
    }

    /**
     * Indicates the minimum size for a column in the table, this is applicable for tables that are
     * not scrollable on the X axis. This will force the earlier columns to leave room for
     * the latter columns.
     *
     * @return  the minimum width of the column
     */
    public static int getMinimumSizePerColumn() {
        return minimumSizePerColumn;
    }

    /**
     * Indicates the default (in percentage) for the column width, -1 indicates
     * automatic sizing
     *
     * @param w width in percentage
     */
    public static void setDefaultColumnWidth(int w) {
        defaultColumnWidth = w;
    }


    /**
     * Indicates the default (in percentage) for the column width, -1 indicates
     * automatic sizing
     *
     * @return width in percentage
     */
    public static int getDefaultColumnWidth() {
        return defaultColumnWidth;
    }


    /**
     * Indicates the default (in percentage) for the row height, -1 indicates
     * automatic sizing
     *
     * @param h height in percentage
     */
    public static void setDefaultRowHeight(int h) {
        defaultRowHeight = h;
    }

    /**
     * Indicates the default (in percentage) for the row height, -1 indicates
     * automatic sizing
     *
     * @return height in percentage
     */
    public static int getDefaultRowHeight() {
        return defaultRowHeight;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "TableLayout";
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        return super.equals(o) && ((TableLayout)o).getRows() == getRows() && ((TableLayout)o).getColumns() == getColumns();
    }
        
    /**
     * {@inheritDoc}
     */
    public boolean isConstraintTracking() {
        return true;
    }    

    /**
     * Indicates whether the table layout should grow horizontally to take up available space by stretching the last column
     * @return the growHorizontally
     */
    public boolean isGrowHorizontally() {
        return growHorizontally;
    }

    /**
     * Indicates whether the table layout should grow horizontally to take up available space by stretching the last column
     * @param growHorizontally the growHorizontally to set
     */
    public void setGrowHorizontally(boolean growHorizontally) {
        this.growHorizontally = growHorizontally;
    }

    /**
     * Creates a table layout container that grows the last column horizontally, the number of rows is automatically
     * calculated based on the number of columns
     * @param columns the number of columns
     * @param cmps components to add
     * @return a newly created table layout container with the components in it
     */
    public static Container encloseIn(int columns, Component... cmps) {
        return encloseIn(columns, true, cmps);
    }


    /**
     * Creates a table layout container, the number of rows is automatically calculated based on the number 
     * of columns
     * @param columns the number of columns
     * @param growHorizontally true to grow the last column to fit available width
     * @param cmps components to add
     * @return a newly created table layout container with the components in it
     */
    public static Container encloseIn(int columns, boolean growHorizontally, Component... cmps) {
        int rows = cmps.length;
        if(rows % columns > 0) {
            rows = rows / columns + 1;
        } else {
            rows = rows / columns;
        }
        TableLayout tl = new TableLayout(rows, columns);
        tl.setGrowHorizontally(growHorizontally);
        return Container.encloseIn(tl, cmps);
    }
}
