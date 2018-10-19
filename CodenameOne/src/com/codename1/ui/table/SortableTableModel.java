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

package com.codename1.ui.table;

import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.validation.Constraint;
import com.codename1.ui.validation.Validator;
import java.util.Arrays;
import java.util.Comparator;

/**
 * A proxy that wraps the table model providing sorting API that can be leveraged by the table
 *
 * @author Shai Almog
 */
public class SortableTableModel extends AbstractTableModel {
    private final TableModel model;
    private int[] sorted;
    private final boolean asc;
    private Comparator cmp;
    private final int sortColumn;

    /**
     * Returns the underlying table model
     * @return the model
     */
    public TableModel getUnderlying() {
        return model;
    }
    
    /**
     * Sorts a table based on the given column
     * @param column the column to sort
     * @param asc the direction ascending/descending
     * @param model the underlying model that will be sorted
     * @param cmp a comparator used for comparing the cells in the column 
     */
    public SortableTableModel(int column, boolean asc, TableModel model, Comparator cmp) {
        this.model = model;
        this.asc = asc;
        this.cmp = cmp;
        this.sortColumn = column;
        initTable(model, asc, cmp, column);
    } 

    /**
     * Returns the position of the row when sorted
     * @param row the row in the visual table
     * @return the position in the underlying model
     */
    public int getSortedPosition(int row) {
        return sorted[row];
    }
     
    private void initTable(final TableModel model1, final boolean asc,
        final Comparator cmp, final int column) {
        sorted = new int[model1.getRowCount()];
        Integer[] sortedTemp = new Integer[sorted.length];
        for(int iter = 0 ; iter < sorted.length ; iter++) {
            sortedTemp[iter] = iter;
        }
        // sort(int[]) doesn't accept a comparator how stupid is that...
        Arrays.sort(sortedTemp,
            new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                int i1 = (Integer)o1;
                int i2 = (Integer)o2;
                if(asc) {
                    return cmp.compare(model1.getValueAt(i1, column),
                        model1.getValueAt(i2, column)) * -1;
                }
                return cmp.compare(model1.getValueAt(i1, column),
                    model1.getValueAt(i2, column));
            }
        });
        for(int iter = 0 ; iter < sorted.length ; iter++) {
            sorted[iter] = sortedTemp[iter];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return model.getRowCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount() {
        return model.getColumnCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(int i) {
        return model.getColumnName(i);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        return model.isCellEditable(sorted[row], column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValueAt(int row, int column) {
        if(model.getRowCount() != sorted.length) {
            initTable(model, asc, cmp, sortColumn);
        }
        return model.getValueAt(sorted[row], column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(int row, int column, Object o) {
        model.setValueAt(sorted[row], column, o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDataChangeListener(DataChangedListener d) {
        model.addDataChangeListener(d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeDataChangeListener(DataChangedListener d) {
        model.removeDataChangeListener(d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Constraint getValidationConstraint(int row, int column) {
        if(model instanceof AbstractTableModel) {
            return ((AbstractTableModel)model).getValidationConstraint(row, column);
        }
        return super.getValidationConstraint(row, column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class getCellType(int row, int column) {
        if(model instanceof AbstractTableModel) {
            return ((AbstractTableModel)model).getCellType(row, column);
        }
        return super.getCellType(row, column);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getMultipleChoiceOptions(int row, int column) {
        if(model instanceof AbstractTableModel) {
            return ((AbstractTableModel)model).getMultipleChoiceOptions(row, column);
        }
        return super.getMultipleChoiceOptions(row, column);
    }    

    @Override
    public void setValidator(Validator validator) {
        if(model instanceof AbstractTableModel) {
            ((AbstractTableModel)model).setValidator(validator);
            return;
        }
        super.setValidator(validator);
    }

    @Override
    public Validator getValidator() {
        if(model instanceof AbstractTableModel) {
            return ((AbstractTableModel)model).getValidator();
        }
        return super.getValidator();
    }
}
