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
package com.codename1.ui.layouts;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

/**
 * Components are arranged in an equally sized grid based on available space
 *
 * @author Chen Fishbein
 */
public class GridLayout extends Layout{
    private boolean fillLastRow;
    private int rows;
    private int columns;

    /**
     * Auto fits columns/rows to available screen space
     */
    private boolean autoFit;
    
    /** 
     * Creates a new instance of GridLayout with the given rows and columns
     * 
     * @param rows - number of rows.
     * @param columns - number of columns.
     * @throws IllegalArgumentException if rows < 1 or columns < 1
     */
    public GridLayout(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        if(rows < 1 || columns < 1){
            throw new IllegalArgumentException("rows and columns must be greater " +
                    "then zero");
        }
    }

    private void autoSizeCols(Container parent, int width) {
        if(isAutoFit()) {
            int numOfcomponents = parent.getComponentCount();
            int maxWidth = 0;
            for(int iter = 0 ; iter < numOfcomponents ; iter++) {
                Component cmp = parent.getComponentAt(iter);
                Style s = cmp.getStyle();
                maxWidth = Math.max(cmp.getPreferredW() + s.getMargin(Component.LEFT) + s.getMargin(Component.RIGHT), maxWidth);
            }
            if(width < maxWidth) {
                width = Display.getInstance().getDisplayWidth();
            }
            // prevent arithmentic exception
            if(maxWidth <= 0) {
                columns = 1;
            } else {
                columns = Math.max(width / maxWidth, 1);
            }
            rows = Math.max(1, numOfcomponents / columns);
            if(numOfcomponents % columns > 0) {
                rows++;
            } 
        }
    }

    /**
     * @inheritDoc
     */    
    public void layoutContainer(Container parent) {
        int width = parent.getLayoutWidth() - parent.getSideGap() - parent.getStyle().getPadding(false, Component.RIGHT) - parent.getStyle().getPadding(false, Component.LEFT);
        int height = parent.getLayoutHeight() - parent.getBottomGap() - parent.getStyle().getPadding(false, Component.BOTTOM) - parent.getStyle().getPadding(false, Component.TOP);
        int numOfcomponents = parent.getComponentCount();

        autoSizeCols(parent, width);

        int x = parent.getStyle().getPadding(parent.isRTL(), Component.LEFT);
        int y = parent.getStyle().getPadding(false, Component.TOP);

        boolean rtl = parent.isRTL();
        if (rtl) {
        	x += parent.getSideGap();
        }
        int localColumns = columns;
        int cmpWidth = width / columns;
        int cmpHeight;
        if (numOfcomponents > rows * columns) {
            // actual rows number
            cmpHeight  = height / (numOfcomponents / columns + (numOfcomponents % columns == 0 ? 0 : 1));
        } else {
            cmpHeight  = height / rows;
        }
        int row = 0;        
        
        for(int i = 0 ; i < numOfcomponents ; i++){
            Component cmp = parent.getComponentAt(i);
            Style cmpStyle = cmp.getStyle();
            int marginLeft = cmpStyle.getMargin(parent.isRTL(), Component.LEFT);
            int marginTop = cmpStyle.getMargin(false, Component.TOP);
            cmp.setWidth(cmpWidth - marginLeft - cmpStyle.getMargin(parent.isRTL(), Component.RIGHT));
            cmp.setHeight(cmpHeight - marginTop - cmpStyle.getMargin(false, Component.BOTTOM));
            if (rtl) {
            	cmp.setX(x + (localColumns - 1 - (i % localColumns)) * cmpWidth + marginLeft);
            } else {
            	cmp.setX(x + (i % localColumns) * cmpWidth + marginLeft);
            }
            cmp.setY(y + row * cmpHeight + marginTop);
            if((i + 1) % columns == 0){
                row++;
                
                // check if we need to recalculate component widths
                if(fillLastRow && row == rows - 1) {
                    localColumns = numOfcomponents % columns;
                    if(localColumns == 0) {
                        localColumns = columns;
                    }
                    cmpWidth = width / localColumns;
                }
            }
        }
    }

    /**
     * @inheritDoc
     */    
    public Dimension getPreferredSize(Container parent) {        
        int width = 0;
        int height = 0;
        
        int numOfcomponents = parent.getComponentCount();
        for(int i=0; i< numOfcomponents; i++){
            Component cmp = parent.getComponentAt(i);
            width = Math.max(width, cmp.getPreferredW() + cmp.getStyle().getMargin(false, Component.LEFT)+ cmp.getStyle().getMargin(false, Component.RIGHT));
            height = Math.max(height, cmp.getPreferredH()+ cmp.getStyle().getMargin(false, Component.TOP)+ cmp.getStyle().getMargin(false, Component.BOTTOM));
        }

        autoSizeCols(parent, parent.getWidth());

        if(columns > 1){
            width = width*columns;
        }
        
        if(rows > 1){
            if(numOfcomponents>rows*columns){ //if there are more components than planned
               height =  height * (numOfcomponents/columns + (numOfcomponents%columns == 0 ? 0 : 1));
            }else{
                height = height*rows;
            }
        }
        
        return new Dimension(width + parent.getStyle().getPadding(false, Component.LEFT)+ parent.getStyle().getPadding(false, Component.RIGHT),
            height + parent.getStyle().getPadding(false, Component.TOP)+ parent.getStyle().getPadding(false, Component.BOTTOM));
    }
    
    /**
     * @inheritDoc
     */
    public String toString() {
        return "GridLayout";
    }

    /**
     * @return the rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * @return the columns
     */
    public int getColumns() {
        return columns;
    }

    /**
     * @inheritDoc
     */
    public boolean equals(Object o) {
        return super.equals(o) && ((GridLayout)o).getRows() == getRows() &&
                ((GridLayout)o).getColumns() == getColumns() && ((GridLayout)o).autoFit == autoFit;
    }

    /**
     * When set to true makes the grid layout fill the last row of the layout
     * entirely if the number of elements in that row is bigger.
     *
     * @return the fillLastRow
     */
    public boolean isFillLastRow() {
        return fillLastRow;
    }

    /**
     * When set to true makes the grid layout fill the last row of the layout
     * entirely if the number of elements in that row is bigger.
     * 
     * @param fillLastRow the fillLastRow to set
     */
    public void setFillLastRow(boolean fillLastRow) {
        this.fillLastRow = fillLastRow;
    }

    /**
     * Auto fits columns/rows to available screen space
     * @return the autoFit
     */
    public boolean isAutoFit() {
        return autoFit;
    }

    /**
     * Auto fits columns/rows to available screen space
     * @param autoFit the autoFit to set
     */
    public void setAutoFit(boolean autoFit) {
        this.autoFit = autoFit;
    }
}
