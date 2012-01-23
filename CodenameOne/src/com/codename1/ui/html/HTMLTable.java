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
package com.codename1.ui.html;

import com.codename1.ui.Component;
import com.codename1.ui.table.Table;
import com.codename1.ui.table.TableLayout.Constraint;

/**
 * HTMLTable extends Codename One's Table and overrides some it's rendering and constaints methods do adapt to HTMLTableModel
 *
 * @author Ofir Leitner
 */
class HTMLTable extends Table {

    boolean innerBorderGroups; // Denotes that the inner border mode is groups

    /**
     * Constant denoting that only inner borders rows should be drawn, only when seperating between table segments
     * The groups/segments feature is not available to Table, but is available internally to HTMLTable
     */
    static final int INNER_BORDERS_GROUPS = 4;

    /**
     * Constructs an HTMLTAble with the given model, which has to be HTMLTableModel
     * 
     * @param model The HTMLTableModel for this Table
     */
    HTMLTable(HTMLTableModel model) {
        super(model,false);
        setCollapseBorder(false); //default for HTML tables
    }

    public String getUIID() {
        return "HTMLTable";
    }

    /**
     * This method is overriden to return the component that is contained in the HTMLTableModel
     * (Since our model contains the actual component and does not store data that can be rendered using toString we can't use the original createCell method)
     *
     * @param value the new value object
     * @param row row number, -1 for the header rows
     * @param column column number
     * @param editable true if the cell is editable
     * @return cell component instance
     */
    protected Component createCell(Object value, int row, int column, boolean editable) {
        Component cmp=null;
       if (value instanceof Component) {
            cmp=(Component)value;
            // TODO - table cells styling - needs to propogate downwards since the cell is usually a container, on the other hand can't wipe out the style of the container's components - TBD
//            boolean isHeader=((HTMLTableModel)getModel()).isHeader(value);
//            if (isHeader) {
//                cmp.setUIID("TableHeader");
//            } else {
//                cmp.setUIID("TableCell");
//            }
        } else {
            cmp=super.createCell(value, row, column, editable);
        }
        cmp.setFocusable(false);
        return cmp;
    }

    /**
     * This method is overriden to fetch the constraints from the associated HTMLTableModel and converts it to TableLayout.Constraint
     *
     * @param value the value of the cell
     * @param row the table row
     * @param column the table column
     * @return the table constraint
     */
    protected Constraint createCellConstraint(Object value, int row, int column) {
        CellConstraint cConstraint=((HTMLTableModel)getModel()).getConstraint(value);
        if (cConstraint==null) { // Can be null for cells that were "spanned over"
            return super.createCellConstraint(value, row, column);
        }
        Constraint constraint = new Constraint();
        constraint.setHorizontalAlign(cConstraint.align);
        constraint.setVerticalAlign(cConstraint.valign);
        constraint.setHorizontalSpan(cConstraint.spanHorizontal);
        constraint.setVerticalSpan(cConstraint.spanVertical);
        constraint.setWidthPercentage(cConstraint.width);
        constraint.setHeightPercentage(cConstraint.height);

        return constraint;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean shouldDrawInnerBorderAfterRow(int row) {
        if (innerBorderGroups) {
            return ((HTMLTableModel)getModel()).isSegmentEnd(row);
        } else {
            return true;
        }
    }

    /**
     * Overrides Table.setInnerBorderMode to accept also the INNER_BORDER_GROUPS constant that implement the 'groups' value for the 'rules' attribute on the 'table' tag.
     * 
     * @param innerBorder The inner border mode, one of the INNER_BORDER_* constants
     */
    public void setInnerBorderMode(int innerBorder) {
        if (innerBorder==INNER_BORDERS_GROUPS) {
            innerBorderGroups=true;
            innerBorder=INNER_BORDERS_ROWS;
        } else {
            innerBorderGroups=false;
        }
        super.setInnerBorderMode(innerBorder);
    }




}
