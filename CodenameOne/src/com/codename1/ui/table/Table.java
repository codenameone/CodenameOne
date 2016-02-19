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

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;

/**
 * <p>The {@code Table} class represents a grid of data that can be used for rendering a grid
 * of components/labels. The table reflects and updates the underlying model data.
 * {@code Table} relies heavily on the {@link com.codename1.ui.table.TableLayout} class and 
 * {@link com.codename1.ui.table.TableModel} interface to present its UI. Unlike a 
 * {@link com.codename1.ui.List} a {@code Table} doesn't feature a separate renderer
 * and instead allows developers to derive the class.
 * </p>
 * 
 * <script src="https://gist.github.com/codenameone/6b106772ad1d58c50270.js"></script>
 * 
 * <img src="https://www.codenameone.com/img/developer-guide/components-table-pinstripe.png" alt="Table with customize cells using the pinstripe effect" />
 * <img src="https://www.codenameone.com/img/developer-guide/components-table-pinstripe-edit.png" alt="Picker table cell during edit" />
 *
 * @author Shai Almog
 */
public class Table extends Container {

    /**
     * Constant denoting that inner borders should not be drawn at all
     */
    public static final int INNER_BORDERS_NONE = 0;

    /**
     * Constant denoting that only inner borders rows should be drawn
     */
    public static final int INNER_BORDERS_ROWS = 1;

    /**
     * Constant denoting that only inner borders columns should be drawn
     */
    public static final int INNER_BORDERS_COLS = 2;

    /**
     * Constant denoting that inner borders should be drawn fully
     */
    public static final int INNER_BORDERS_ALL = 3;

    private TableModel model;
    private Listener listener = new Listener();
    private boolean drawBorder = true;
    private boolean collapseBorder = true;
    private boolean drawEmptyCellsBorder = true;
    private int horizontalBorderSpacing = 0;
    private int verticalBorderSpacing = 0;
    private boolean includeHeader = true;
    private int innerBorder = INNER_BORDERS_ALL;

    /**
     * Indicates the alignment of the title see label alignment for details
     * 
     * @see com.codename1.ui.Label#setAlignment(int) 
     */
    private int titleAlignment = Label.CENTER;

    /**
     * Indicates the alignment of the cells see label alignment for details
     * 
     * @see com.codename1.ui.Label#setAlignment(int)
     */
    private int cellAlignment = Label.LEFT;

    /**
     * This flag allows us to workaround issue 275 without incuring too many updateModel calls
     */
    private boolean potentiallyDirtyModel;

    /**
     * Constructor for usage by GUI builder and automated tools, normally one
     * should use the version that accepts the model
     */
    public Table() {
        this(new DefaultTableModel(new String[]{"Col1", "Col2"}, new String[][]{
            {"1", "2"},
            {"3", "4"}}));
    }


    /**
     * Create a table with a new model
     *
     * @param model the model underlying this table
     */
    public Table(TableModel model) {
        this.model = model;
        updateModel();
        setUIID("Table");
    }

    /**
     * Create a table with a new model
     *
     * @param model the model underlying this table
     * @param includeHeader Indicates whether the table should render a table header as the first row
     */
    public Table(TableModel model, boolean includeHeader) {
        setUIID("Table");
        this.includeHeader = includeHeader;
        this.model = model;
        updateModel();
    }

    /**
     * Returns the selected row in the table
     *
     * @return the offset of the selected row in the table if a selection exists
     */
    public int getSelectedRow() {
        Form f = getComponentForm();
        if(f != null) {
            Component c = f.getFocused();
            if(c != null) {
                return getCellRow(c);
            }
        }
        return -1;
    }

    /**
     * By default createCell/constraint won't be invoked for null values by overriding this method to return true
     * you can replace this behavior
     * @return false by default
     */
    protected boolean includeNullValues() {
        return false;
    }
    
    /**
     * Returns the selected column in the table
     *
     * @return the offset of the selected column in the table if a selection exists
     */
    public int getSelectedColumn() {
        Form f = getComponentForm();
        if(f != null) {
            Component c = f.getFocused();
            if(c != null) {
                return getCellColumn(c);
            }
        }
        return -1;
    }

    private void updateModel() {
        int selectionRow = -1, selectionColumn = -1;
        Form f = getComponentForm();
        if(f != null) {
            Component c = f.getFocused();
            if(c != null) {
                selectionRow = getCellRow(c);
                selectionColumn = getCellColumn(c);
            }
        }
        removeAll();
        int columnCount = model.getColumnCount();

        // another row for the table header
        if(includeHeader) {
            setLayout(new TableLayout(model.getRowCount() + 1, columnCount));
            for(int iter = 0 ; iter < columnCount ; iter++) {
                String name = model.getColumnName(iter);
                Component header = createCellImpl(name, -1, iter, false);
                TableLayout.Constraint con = createCellConstraint(name, -1, iter);
                addComponent(con, header);
            }
        } else {
            setLayout(new TableLayout(model.getRowCount(), columnCount));
        }

        for(int r = 0 ; r < model.getRowCount() ; r++) {
            for(int c = 0 ; c < columnCount ; c++) {
                Object value = model.getValueAt(r, c);

                // null should be returned for spanned over values
                if(value != null || includeNullValues()) {
                    boolean e = model.isCellEditable(r, c);
                    Component cell = createCellImpl(value, r, c, e);
                    if(cell != null) {
                        TableLayout.Constraint con = createCellConstraint(value, r, c);

                        // returns the current row we iterate about
                        int currentRow = ((TableLayout)getLayout()).getNextRow();
                        
                        if(r > model.getRowCount()) {
                            return;
                        }
                        addComponent(con, cell);
                        if(r == selectionRow && c == selectionColumn) {
                            cell.requestFocus();
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void paintGlass(Graphics g) {
        if ((drawBorder) && (innerBorder!=INNER_BORDERS_NONE)) {
            int xPos = getAbsoluteX();
            int yPos = getAbsoluteY();
            g.translate(xPos, yPos);
            int rows = model.getRowCount();
            int cols = model.getColumnCount();
            if(includeHeader) {
                rows++;
            }
            g.setColor(getStyle().getFgColor());
            TableLayout t = (TableLayout)getLayout();
            int actualWidth = Math.max(getWidth(), getScrollDimension().getWidth());
            int actualHeight = Math.max(getHeight(), getScrollDimension().getHeight());

            if ((collapseBorder) || (innerBorder!=INNER_BORDERS_ALL) || // inner borders cols/rows are supported only in collapsed mode
                (t.hasHorizontalSpanning()) || (t.hasVerticalSpanning())) { // TODO - We currently don't support separate borders for tables with spanned cells
                if ((innerBorder==INNER_BORDERS_ALL) || (innerBorder==INNER_BORDERS_ROWS)) {
                    if(t.hasVerticalSpanning()) {
                        // iterate over the components and draw a line on the side of all
                        // the components other than the ones that are at the last column.
                        for(int cellRow = 0 ; cellRow < rows - 1; cellRow++) {
                            for(int cellColumn = 0 ; cellColumn < cols ; cellColumn++) {
                                // if this isn't the last row
                                if(cellRow + t.getCellVerticalSpan(cellRow, cellColumn) - 1 != rows - 1) {
                                    // if this is a spanned through cell we don't want to draw a line here
                                    if(t.isCellSpannedThroughHorizontally(cellRow, cellColumn)) {
                                        continue;
                                    }

                                    int x = t.getColumnPosition(cellColumn);
                                    int y = t.getRowPosition(cellRow);
                                    int rowHeight = t.getRowPosition(cellRow + t.getCellVerticalSpan(cellRow, cellColumn)) - y;
                                    int columnWidth;
                                    if(cellColumn < getModel().getColumnCount() - 1) {
                                        columnWidth = t.getColumnPosition(cellColumn + 1) - x;
                                    } else {
                                        columnWidth = getWidth() - y;
                                    }

                                    if ((innerBorder!=INNER_BORDERS_ROWS) || (shouldDrawInnerBorderAfterRow(cellRow))) {
                                        g.drawLine(x, y + rowHeight, x + columnWidth, y + rowHeight);
                                    }
                                }
                            }
                        }
                    } else {
                        // this is much faster since we don't need to check spanning
                        for(int row = 1 ; row < rows; row++) {
                            int y = t.getRowPosition(row);
                            if ((innerBorder!=INNER_BORDERS_ROWS) || (shouldDrawInnerBorderAfterRow(row-1))) {
                                g.drawLine(0, y, actualWidth, y);
                            }
                            //g.drawLine(0+2, y+2, actualWidth-2, y+2);

                        }
                    }
                }

                if ((innerBorder==INNER_BORDERS_ALL) || (innerBorder==INNER_BORDERS_COLS)) {
                    if(t.hasHorizontalSpanning()) {
                        // iterate over the components and draw a line on the side of all
                        // the components other than the ones that are at the last column.
                        for(int cellRow = 0 ; cellRow < rows ; cellRow++) {
                            for(int cellColumn = 0 ; cellColumn < cols - 1 ; cellColumn++) {
                                // if this isn't the last column
                                if(cellColumn + t.getCellHorizontalSpan(cellRow, cellColumn) - 1 != cols - 1) {
                                    // if this is a spanned through cell we don't want to draw a line here
                                    if(t.isCellSpannedThroughVertically(cellRow, cellColumn)) {
                                        continue;
                                    }

                                    int x = t.getColumnPosition(cellColumn);
                                    int y = t.getRowPosition(cellRow);
                                    int rowHeight;
                                    int columnWidth = t.getColumnPosition(cellColumn + t.getCellHorizontalSpan(cellRow, cellColumn)) - x;
                                    if(cellRow < getModel().getRowCount() - 1) {
                                        rowHeight = t.getRowPosition(cellRow + 1) - y;
                                    } else {
                                        rowHeight = getHeight() - y;
                                    }

                                    g.drawLine(x + columnWidth, y, x + columnWidth, y + rowHeight);
                                }
                                if(t.getCellHorizontalSpan(cellRow, cellColumn) > 1){
                                    cellColumn += t.getCellHorizontalSpan(cellRow, cellColumn) - 1;
                                }
                            }
                        }
                    } else {
                        for(int col = 1 ; col < cols ; col++) {
                            int x = t.getColumnPosition(col);
                            g.drawLine(x, 0, x, actualHeight);
                            //g.drawLine(x+2, 0+2, x+2, actualHeight-2);
                        }
                    }
                }
            } else { // separate border
                //if ((!t.hasHorizontalSpanning()) && (!t.hasVerticalSpanning())) {
                    for(int row = 0 ; row < rows; row++) {
                        int y = t.getRowPosition(row);
                        int h;
                        if (row+1<rows) {
                            h=t.getRowPosition(row+1)-y;
                        } else {
                            h=getY()+actualHeight-y-2;
                        }
                        for(int col = 0 ; col < cols ; col++) {
                            int x = t.getColumnPosition(col);
                            int w;
                            if (col+1<cols) {
                                w=t.getColumnPosition(col+1)-x;
                            } else {
                                w=getX()+actualWidth-x-2;
                            }
                            Component comp=t.getComponentAt(row, col);
                            if ((comp.isVisible()) &&
                                    ((drawEmptyCellsBorder) ||
                                     ((comp.getWidth()-comp.getStyle().getPadding(false, Component.RIGHT) - comp.getStyle().getPadding(false, Component.LEFT)>0) &&
                                      (comp.getHeight()-comp.getStyle().getPadding(false, Component.TOP) - comp.getStyle().getPadding(false, Component.BOTTOM)>0)))) {
                                int rightMargin=comp.getStyle().getMargin(Component.RIGHT);
                                int bottomMargin=comp.getStyle().getMargin(Component.BOTTOM);
                                if (col==0) {
                                    rightMargin*=2; // Since the first cell includes margins from both sides (left/right) so the next cell location is farther away - but we don't want to paint the border up to it
                                }
                                if (row==0) {
                                    bottomMargin*=2;
                                }
                                g.drawRect(x+comp.getStyle().getMargin(Component.LEFT), y+comp.getStyle().getMargin(Component.TOP), w-2-rightMargin, h-2-bottomMargin);
                            }
                        }
                    }
            }

            g.translate(-xPos, -yPos);
        }
    }

    private Component createCellImpl(Object value, final int row, final int column, boolean editable) {
        Component c = createCell(value, row, column, editable);
        c.putClientProperty("row", new Integer(row));
        c.putClientProperty("column", new Integer(column));
        
        // we do this here to allow subclasses to return a text area or its subclass
        if(c instanceof TextArea) {
            ((TextArea)c).addActionListener(listener);
        } 

        Style s = c.getSelectedStyle();
        //s.setMargin(0, 0, 0, 0);
        s.setMargin(verticalBorderSpacing, verticalBorderSpacing, horizontalBorderSpacing, horizontalBorderSpacing);
        if ((drawBorder) && (innerBorder!=INNER_BORDERS_NONE)) {
            s.setBorder(null);
            s = c.getUnselectedStyle();
            s.setBorder(null);
        } else {
            s = c.getUnselectedStyle();
        }
        //s.setBgTransparency(0);
        //s.setMargin(0, 0, 0, 0);
        s.setMargin(verticalBorderSpacing, verticalBorderSpacing, horizontalBorderSpacing, horizontalBorderSpacing);
        return c;
    }

    /**
     * Creates a cell based on the given value
     *
     * @param value the new value object
     * @param row row number, -1 for the header rows
     * @param column column number
     * @param editable true if the cell is editable
     * @return cell component instance
     */
    protected Component createCell(Object value, int row, int column, boolean editable) {
        if(row == -1) {
            Label header = new Label((String)value);
            header.setUIID(getUIID() + "Header");
            header.getUnselectedStyle().setAlignment(titleAlignment);
            header.getSelectedStyle().setAlignment(titleAlignment);
            header.setFocusable(true);
            return header;
        }
        if(editable) {
            TextField cell = new TextField("" + value, -1);
            cell.setLeftAndRightEditingTrigger(false);
            cell.setUIID(getUIID() + "Cell");
            return cell;
        }
        Label cell = new Label("" + value);
        cell.setUIID(getUIID() + "Cell");
        cell.getUnselectedStyle().setAlignment(cellAlignment);
        cell.getSelectedStyle().setAlignment(cellAlignment);
        cell.setFocusable(true);
        return cell;
    }

    /**
     * {@inheritDoc}
     */
    public void initComponent() {
        // this can happen if deinitialize is invoked due to a menu command which modifies
        // the content of the table while the listener wasn't bound
        if(potentiallyDirtyModel) {
            updateModel();
            potentiallyDirtyModel = false;
        }
        model.addDataChangeListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void deinitialize() {
        // we unbind the listener to prevent a memory leak for the use case of keeping
        // the model while discarding the component
        // Prevent the model listener from being removed when the VKB is shown
        if(!Display.getInstance().isVirtualKeyboardShowing()) {
            potentiallyDirtyModel = true;
            model.removeDataChangeListener(listener);
        } else {
            potentiallyDirtyModel = false;
        }
    }

    /**
     * Replaces the underlying model
     *
     * @param model the new model
     */
    public void setModel(TableModel model) {
        this.model = model;
        updateModel();
        revalidate();
    }

    /**
     * Returns the model instance
     *
     * @return the model instance
     */
    public TableModel getModel() {
        return model;
    }

    /**
     * Indicates whether the table border should be drawn
     *
     * @return the drawBorder
     */
    public boolean isDrawBorder() {
        return drawBorder;
    }

    /**
     * Indicates whether the table border should be drawn
     *
     * @param drawBorder the drawBorder to set
     */
    public void setDrawBorder(boolean drawBorder) {
        if(this.drawBorder != drawBorder) {
            this.drawBorder = drawBorder;
            updateModel();
            revalidate();
        }
    }

    /**
     * Sets how to draw the inner border (All of it, only rows/columns, none, groups)
     * Note that setting to any mode other than NONE/ALL will result in the border drawing as collapsed whether this is a collpased border or not
     * 
     * @param innerBorder one of the INNER_BORDER_* constants
     */
    public void setInnerBorderMode(int innerBorder) {
        if ((innerBorder<INNER_BORDERS_NONE) || (innerBorder>INNER_BORDERS_ALL)) {
            throw new IllegalArgumentException("Inner border mode must be one of the INNER_BORDER_* constants");
        }

        if(this.innerBorder != innerBorder) {
            this.innerBorder=innerBorder;
            updateModel();
            revalidate();
        }
    }

    /**
     * Returns the current inner border mode
     * 
     * @return the current inner border mode (one of the INNER_BORDER_* constants)
     */
    public int getInnerBorderMode() {
        return innerBorder;
    }

    /**
     * Returns whether an inner border should be drawn after the specified row.
     * This allows customization in subclasses to create for example the effects of segments in atable, i.e. instead of a line after each row - lines after "chunks" of rows.
     * Note that this is queried only when the inner border mode is set to INNER_BORDER_ROWS
     * 
     * @param row The row in question
     * @return true to draw inner border, false otherwise
     */
    protected boolean shouldDrawInnerBorderAfterRow(int row) {
        return true;
    }

    /**
     * Indicates whether the borders of the cells should collapse to form a one line border
     *
     * @param collapseBorder true to collapse (default), false for separate borders
     */
    public void setCollapseBorder(boolean collapseBorder) {
        if (this.collapseBorder!=collapseBorder) {
            this.collapseBorder = collapseBorder;
            if ((horizontalBorderSpacing!=0) || (verticalBorderSpacing!=0)) { // Only if one of the spacing was not 0, then we need to update, since otherwise the margin is 0 for both collapse and separate modes
                updateMargins();
            }
            repaint();
        }
    }

    /**
     * Indicates whether empty cells should have borders (relevant only for separate borders and not for collapsed)
     * 
     * @param drawEmptyCellsBorder - true to draw (default), false otherwise
     */
    public void setDrawEmptyCellsBorder(boolean drawEmptyCellsBorder) {
        this.drawEmptyCellsBorder = drawEmptyCellsBorder;
        repaint();
    }

    /**
     * Sets the spacing of cells border (relevant only for separate borders and not for collapsed)
     *
     * @param horizontal - The horizontal spacing
     * @param vertical - The vertical spacing
     */
    public void setBorderSpacing(int horizontal, int vertical) {
        horizontalBorderSpacing=horizontal;
        verticalBorderSpacing=vertical;
        updateMargins();
    }

    private void updateMargins() {
        TableLayout t = (TableLayout)getLayout();
        int hSpace=horizontalBorderSpacing;
        int vSpace=verticalBorderSpacing;
        if (collapseBorder) { // not relevant for collapse border
            hSpace=0;
            vSpace=0;
        }
        if ((!t.hasHorizontalSpanning()) && (!t.hasVerticalSpanning())) {
            for(int row = 0 ; row < t.getRows(); row++) {
                for(int col = 0 ; col < t.getColumns() ; col++) {
                    Component cmp=null;
                    try {
                        cmp=t.getComponentAt(row, col);
                    } catch (Exception  e) {
                        // parent of cmp can be null as well - TODO - check why
                    }
                    if (cmp!=null) {
                        int leftMargin=(col==0)?hSpace:0;
                        int topMargin=(row==0)?vSpace:0;
                        cmp.getUnselectedStyle().setMargin(topMargin, vSpace, leftMargin, hSpace);
                        cmp.getSelectedStyle().setMargin(topMargin, vSpace, leftMargin, hSpace);
                    }
                }
            }
        }
        repaint();
    }


    /**
     * Indicates the alignment of the title see label alignment for details
     *
     * @return the title alignment
     * @see com.codename1.ui.Label#setAlignment(int)
     */
    public int getTitleAlignment() {
        return titleAlignment;
    }

    /**
     * Indicates the alignment of the title see label alignment for details
     *
     * @param titleAlignment the title alignment
     * @see com.codename1.ui.Label#setAlignment(int)
     */
    public void setTitleAlignment(int titleAlignment) {
        this.titleAlignment = titleAlignment;
        for(int iter = 0 ; iter < model.getColumnCount() ; iter++) {
            listener.dataChanged(-1, iter);
        }
    }


    /**
     * Returns the column in which the given cell is placed
     * 
     * @param cell the component representing the cell placed in the table
     * @return the column in which the cell was placed in the table
     */
    public int getCellColumn(Component cell) {
        Integer i = ((Integer)cell.getClientProperty("column"));
        if(i != null) {
            return i.intValue();
        }
        return -1;
    }

    /**
     * Returns the row in which the given cell is placed
     * 
     * @param cell the component representing the cell placed in the table
     * @return the row in which the cell was placed in the table
     */
    public int getCellRow(Component cell) {
        Integer i = ((Integer)cell.getClientProperty("row"));
        if(i != null) {
            return i.intValue();
        }
        return -1;
    }

    /**
     * Indicates the alignment of the cells see label alignment for details
     *
     * @see com.codename1.ui.Label#setAlignment(int)
     * @return the cell alignment
     */
    public int getCellAlignment() {
        return cellAlignment;
    }

    /**
     * Indicates the alignment of the cells see label alignment for details
     *
     * @param cellAlignment the table cell alignment
     * @see com.codename1.ui.Label#setAlignment(int)
     */
    public void setCellAlignment(int cellAlignment) {
        this.cellAlignment = cellAlignment;
        repaint();
    }

    /**
     * Indicates whether the table should render a table header as the first row
     *
     * @return the includeHeader
     */
    public boolean isIncludeHeader() {
        return includeHeader;
    }

    /**
     * Indicates whether the table should render a table header as the first row
     * 
     * @param includeHeader the includeHeader to set
     */
    public void setIncludeHeader(boolean includeHeader) {
        this.includeHeader = includeHeader;
        updateModel();
    }

    /**
     * Creates the table cell constraint for the given cell, this method can be overriden for
     * the purposes of modifying the table constraints.
     *
     * @param value the value of the cell
     * @param row the table row
     * @param column the table column
     * @return the table constraint
     */
    protected TableLayout.Constraint createCellConstraint(Object value, int row, int column) {
        if(includeHeader) {
            row++;
        }
        TableLayout t = (TableLayout)getLayout();
        return t.createConstraint(row, column);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {"data", "header"};
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] {com.codename1.impl.CodenameOneImplementation.getStringArray2DClass(), 
           com.codename1.impl.CodenameOneImplementation.getStringArrayClass()};
    }
    
    /**
     * {@inheritDoc}
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"String[][]", "String[]"};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("data")) {
            return ((DefaultTableModel)model).data;
        }
        if(name.equals("header")) {
            return ((DefaultTableModel)model).columnNames;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("data")) {
            setModel(new DefaultTableModel(((DefaultTableModel)model).columnNames, (String[][])value));
            return null;
        }
        if(name.equals("header")) {
            setModel(new DefaultTableModel((String[])value, ((DefaultTableModel)model).data, ((DefaultTableModel)model).editable));
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    class Listener implements DataChangedListener, ActionListener {
        private int editingColumn = -1;
        private int editingRow = -1;
        /**
         * {@inheritDoc}
         */
        public final void dataChanged(int row, int column) {
            // prevents the table from rebuilding on every text field edit which makes the table 
            // more usable on iOS devices with the VKB/Native editing
            if(editingColumn == column && editingRow == row) {
                editingColumn = -1;
                editingRow = -1;
                return;
            }
            Object value;
            boolean e;
            if(row < 0) {
                e = false;
                value = model.getColumnName(column);
            } else {
                value = model.getValueAt(row, column);
                e = model.isCellEditable(row, column);
            }
            Component cell = createCellImpl(value, row, column, e);

            TableLayout t = (TableLayout)getLayout();
            TableLayout.Constraint con = createCellConstraint(value, row, column);
            if(includeHeader) {
                row++;
            }

            Component c = t.getComponentAt(row, column);
            if(c != null) {
                removeComponent(c);
                
                // a repaint sent right before this might result in an artifact for some use cases so
                // removing visibility essentially cancels repaints
                c.setVisible(false);
            }

            addComponent(con, cell);
            layoutContainer();
            cell.requestFocus();
            revalidate();
        }

        public void actionPerformed(ActionEvent evt) {
            TextArea t = (TextArea)evt.getSource();
            int row = getCellRow(t);
            int column = getCellColumn(t);
            editingColumn = column;
            editingRow = row;
            getModel().setValueAt(row, column, t.getText());
        }
    }
}
