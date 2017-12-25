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
import java.util.Arrays;
import java.util.Comparator;

/**
 * A proxy that wraps the table model providing sorting API that can be leveraged by the table
 *
 * @author Shai Almog
 */
public class SortableTableModel implements TableModel {
    private TableModel model;
    private int[] sorted;

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
    public SortableTableModel(final int column, final boolean asc, final TableModel model, final Comparator cmp) {
        this.model = model;

        sorted = new int[model.getRowCount()];
        Integer[] sortedTemp = new Integer[sorted.length]; 

        for(int iter = 0 ; iter < sorted.length ; iter++) {
            sortedTemp[iter] = iter;
        }

        // sort(int[]) doesn't accept a comparator how stupid is that...
        Arrays.sort(sortedTemp, new Comparator<Object>()  {
            public int compare(Object o1, Object o2) {
                int i1 = (Integer)o1;
                int i2 = (Integer)o2;
                if(asc) {
                    return cmp.compare(model.getValueAt(i1, column), model.getValueAt(i2, column)) * -1;
                }
                return cmp.compare(model.getValueAt(i1, column), model.getValueAt(i2, column));
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
}
