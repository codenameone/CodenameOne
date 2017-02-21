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
package com.codename1.ui.validation;

/**
 * Forces the value to be a number potentially within specific bounds
 *
 * @author Shai Almog
 */
public class NumericConstraint implements Constraint {
    private boolean dec;
    private double minimum;
    private double maximum;
    private String errorMessage;

    /**
     * Creates a new numeric constraint
     * @param dec whether the number is decimal or integer, true for decimal
     * @param minimum the minimal value to a number or Double.NaN for no minimum value
     * @param maximum the maximum value to a number or Double.NaN for no minimum value
     * @param errorMessage the default error message if the constraint fails
     */
    public NumericConstraint(boolean dec, double minimum, double maximum, String errorMessage) {
        this.dec = dec;
        this.minimum = minimum;
        this.maximum = maximum;
        this.errorMessage = errorMessage;
    }


    /**
     * Creates a new numeric constraint
     * @param dec whether the number is decimal or integer, true for decimal
     */
    public NumericConstraint(boolean dec) {
        this(dec, Double.NaN, Double.NaN, null);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(Object value) {
        if(value != null) {
            String s = value.toString();
            if(!dec) {
                try {
                    return checkRange(Integer.parseInt(s));
                } catch(NumberFormatException e) {
                    return false;
                }
            }
            try {
                return checkRange(Double.parseDouble(s));
            } catch(NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private boolean checkRange(double v) {
        if(minimum != Double.NaN) {
            if(maximum != Double.NaN) {
                return v >= minimum && v <= maximum;
            }
            return v >= minimum;
        } else {
            if(maximum != Double.NaN) {
                return v <= maximum;
            }
        }        
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultFailMessage() {
        if(errorMessage == null) {
            String round = "";
            if(!dec) {
                round = "round ";
            } 
            if(minimum != Double.NaN) {
                if(maximum != Double.NaN) {
                    return "The value must be a valid " + round + "number between " + minimum + " and " + maximum;
                }
                return "The value must be a valid "  + round + "number larger than " + minimum;
            }  else {
                if(maximum != Double.NaN) {
                    return "The value must be a valid " + round + "number larger than " + maximum;
                }
            }
            return "The value must be a valid " + round + "number";
        }
        return errorMessage;
    }
}
