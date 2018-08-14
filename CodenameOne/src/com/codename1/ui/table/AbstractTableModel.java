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

import com.codename1.ui.validation.Constraint;
import com.codename1.ui.validation.Validator;

/**
 * This abstract class extends table model with various capabilities such
 * as type information etc.
 *
 * @author Shai Almog
 */
public abstract class AbstractTableModel implements TableModel {
    private Validator validator;
    
    /**
     * Allows the table to hint the class type of a specific cell
     * @param row the row in the table
     * @param column the column
     * @return the class representing this cell
     */
    public Class getCellType(int row, int column) {
        Object o = getValueAt(row, column);
        if(o == null) {
            return String.class;
        }
        return o.getClass();
    }
    
    /**
     * If the cell has a validation constraint it's returned here
     * @param row the row
     * @param column the column for the cell
     * @return null for no constraints
     */
    public Constraint getValidationConstraint(int row, int column) {
        return null;
    }

    /**
     * Allows the table cell to feature multiple choice for a specific entry
     * @param row the row in the table
     * @param column the column
     * @return the string values matching the entries or null if this isn't a multiple
     * choice option
     */
    public String[] getMultipleChoiceOptions(int row, int column) {
        return null;
    }

    /**
     * A validator can be defined here so a validation constraint can bind to a table model cell
     * 
     * @return the validator
     */
    public Validator getValidator() {
        return validator;
    }

    /**
     * A validator can be defined here so a validation constraint can bind to a table model cell
     * @param validator the validator to set
     */
    public void setValidator(Validator validator) {
        this.validator = validator;
    }
}
