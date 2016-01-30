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

import com.codename1.util.regex.RE;

/**
 * Creates a validation constraint based on a regular expression
 *
 * @author Shai Almog
 */
public class RegexConstraint implements Constraint {
    private static final String validEmailRegex = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
    private static final String validURLRegex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    private RE regex;
    private String errorMessage;

    /**
     * Creates a new regex constraint
     * @param regex the regular expression
     * @param errorMessage the default error message if the constraint fails
     */
    public RegexConstraint(String regex, String errorMessage) {
        this.regex = new RE(regex);
        this.errorMessage = errorMessage;
    }

    /**
     * Generates a valid email constraint by using a regular expression
     * @param errorMessage error message for the constraint
     * @return a constraint that will fail if the input isn't a valid email
     */
    public static Constraint validEmail(String errorMessage) {
        return new RegexConstraint(validEmailRegex, errorMessage);
    }

    /**
     * Generates a valid email constraint by using a regular expression
     * @return a constraint that will fail if the input isn't a valid email
     */
    public static Constraint validEmail() {
        return new RegexConstraint(validEmailRegex, "Invalid Email Address");
    }


    /**
     * Generates a valid URL constraint by using a regular expression
     * @param errorMessage error message for the constraint
     * @return a constraint that will fail if the input isn't a valid email
     */
    public static Constraint validURL(String errorMessage) {
        return new RegexConstraint(validURLRegex, errorMessage);
    }

    /**
     * Generates a valid URL constraint by using a regular expression
     * @return a constraint that will fail if the input isn't a valid email
     */
    public static Constraint validURL() {
        return new RegexConstraint(validURLRegex, "Invalid URL");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(Object value) {
        return value != null && regex.match(value.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultFailMessage() {
        return errorMessage;
    }
}
