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
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.table.TableModel;
import com.codename1.ui.util.EventDispatcher;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * HTMLTableModel is a TableModel that is adapted to HTMLTables and their creation. The difference from other models is:<br>
 * - There are no column names, on the other hand each cell anywhere can be defined as a header (which merely changes it style).<br>
 * - There is no prior declaration of the number of columns and rows in the table.<br>
 * - Cells can be added only using the addCell method which adds them to the end of the current row.<br>
 * - When a row ends, the commitRow method is called which results in creating a new empty row.<br>
 * - Since a cell in an HTML table can be practically everthing (From a simple string to a whole document), the objects added to the table are in fact Codename One components.
 *
 * @author Ofir Leitner
 */
class HTMLTableModel implements TableModel{

    Vector rows=new Vector();
    Vector headers=new Vector();
    int maxColumn;
    Vector currentRow = new Vector();
    Vector lastCommittedRow;
    Hashtable constraints = new Hashtable();
    private EventDispatcher dispatcher = new EventDispatcher();
    HTMLElement captionTextTag;
    Vector segmentEnds;
    int curSegmentType=SEGMENT_TBODY;
    int rowInsretionPos=-1;
    int bodyInsertionPos=0;
    boolean hasTHead,hasTFoot;

    static final int SEGMENT_THEAD = 0;
    static final int SEGMENT_TBODY = 1;
    static final int SEGMENT_TFOOT = 2;

    /**
     * Adds the given component as a cell to the end of the current row of the table
     *
     * @param cell The component to add
     * @param isHeader true if this is a header cell (Element.TAG_TH), false otherwise
     * @param constraint Specific constraints for this cell (alignment, spanning)
     */
    void addCell(Component cell,boolean isHeader,CellConstraint constraint) {
        if (isHeader) {
            headers.addElement(cell);
        }
        currentRow.addElement(cell);
        if (currentRow.size()>maxColumn) {
            maxColumn=currentRow.size();
        }
        if (constraint!=null) {
            constraints.put(cell,constraint);
        }
    }

    /**
     * Sets the given alignment as a constraint to all cells in the table
     * 
     * @param isHorizontal true to set horizontal alignment, false for vertical
     * @param align The requested alignment
     */
    void setAlignToAll(boolean isHorizontal,int align) {
        for (Enumeration e=constraints.elements();e.hasMoreElements();) {
            CellConstraint cc=(CellConstraint)e.nextElement();
            if (isHorizontal) {
                cc.setHorizontalAlign(align);
            } else {
                cc.setVerticalAlign(align);
            }
        }
    }

    /**
     * Returns the constraint for the specified object/cell
     * 
     * @param object The object/cell 
     * @return the constraint for the specified object/cell
     */
    CellConstraint getConstraint(Object object) {
        return (CellConstraint)constraints.get(object);
    }

    /**
     * Checks if the object is a header
     * 
     * @param object The object/cell in question
     * @return true if object is a header, false otherwise
     */
    boolean isHeader(Object object) {
        return headers.contains(object);
    }

    /**
     * Commits the current row. This opens a new empty row.
     */
    void commitRow() {
        if (rowInsretionPos==-1) {
            rows.addElement(currentRow);
        } else {
            rows.insertElementAt(currentRow,rowInsretionPos++);
        }
        if (curSegmentType!=SEGMENT_TFOOT) {
            bodyInsertionPos++;
        }
        lastCommittedRow=currentRow;
        currentRow=new Vector();
    }

   /**
    *  Commits the current row only if it is not empty
    */
    void commitRowIfNotEmpty() {
        if (currentRow.size()>0) {
            commitRow();
        }
    }

    /**
     * Signals a start for a segment (THEAD, TFOOT, TBODY)
     *
     * @param segmentType The segment type
     */
    void startSegment(int segmentType) {
        if ((segmentType==SEGMENT_THEAD) && (!hasTHead)) { // Can only have one THEAD, second one will be considered as TBODY
            rowInsretionPos=0;
            hasTHead=true;
            curSegmentType=SEGMENT_THEAD;
        } else if ((segmentType==SEGMENT_TFOOT) && (!hasTFoot)) { // Can only have one TFOOT, second one will be considered as TBODY
            rowInsretionPos=-1;
            hasTFoot=true;
            curSegmentType=SEGMENT_TFOOT;
        } else { //body
            rowInsretionPos=bodyInsertionPos;
            curSegmentType=SEGMENT_TBODY;
        }

    }

    /**
     * Signals the end of the current segment
     */
    void endSegment() {
        if (lastCommittedRow!=null) {
            if (segmentEnds==null) {
                segmentEnds=new Vector();
            }
            segmentEnds.addElement(lastCommittedRow);
        }
        curSegmentType=SEGMENT_TBODY; //-1;
        rowInsretionPos=bodyInsertionPos;
    }



    // TableModel methods:

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return rows.size();
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        return maxColumn;
    }

    /**
     * {@inheritDoc}
     */
    public String getColumnName(int i) {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(int row, int column) {
        if (row>=rows.size()) {
            return null;
        }
        Vector columns=(Vector)rows.elementAt(row);
        if (column>=columns.size()) {
            return null;
        }
        return columns.elementAt(column);
    }

    /**
     * {@inheritDoc}
     */
    public void setValueAt(int row, int column, Object o) {
        Vector columns=(Vector)rows.elementAt(row);
        columns.removeElementAt(column);
        columns.setElementAt(o, column);
        dispatcher.fireDataChangeEvent(column, row);
    }

    /**
     * @inheritDoc
     */
    public void addDataChangeListener(DataChangedListener d) {
        dispatcher.addListener(d);
    }

    /**
     * @inheritDoc
     */
    public void removeDataChangeListener(DataChangedListener d) {
        dispatcher.removeListener(d);
    }

    /**
     * Checks if the specified row ends a segment in the table (and as such a line should be drawn after it)
     * This is used when a the 'rules' attribute in the 'table' tag was set to 'groups'
     *
     * @param row The row to check
     * @return true if the specified row ends a segment in the table, false otherwise
     */
    boolean isSegmentEnd(int row) {
        if ((segmentEnds==null) || (row<0) || (row>=rows.size())) {
            return false;
        }
        return segmentEnds.contains(rows.elementAt(row));
        
    }

}
