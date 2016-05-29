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
 * Creates a validation constraint based on minimum input length
 *
 * @author Shai Almog
 */
public class LengthConstraint implements Constraint {
    private int length;
    private String errorMessage;

    /**
     * Creates a new length constraint
     * @param length the length of the constraint
     * @param errorMessage the default error message if the constraint fails
     */
    public LengthConstraint(int length, String errorMessage) {
        this.length = length;
        this.errorMessage = errorMessage;
    }


    /**
     * Creates a new length constraint
     * @param length the length of the constraint
     */
    public LengthConstraint(int length) {
        this(length, null);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(Object value) {
        return value != null && value.toString().length() >= length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultFailMessage() {
        if(errorMessage == null) {
            if(length == 1) {
                return "A value is required"; 
            }
            return "Input must be at least " + length + " characters";
        }
        return errorMessage;
    }
}
