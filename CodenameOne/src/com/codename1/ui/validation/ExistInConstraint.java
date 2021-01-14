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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Creates a validation constraint to ensure input value exists in a list of items
 *
 * @author Diamond Mubaarak
 * @since 7.0
 */
public class ExistInConstraint implements Constraint {

    private final List<String> items;
    private final boolean caseSensitive;
    private final String errorMessage;

    /**
     * Creates a new ExistIn constraint
     *
     * @param items         the array items the input value must exist in
     * @param caseSensitive compare the input with the items with case-sensitivity or not
     * @param errorMessage  the default error message if the constraint fails
     */
    public ExistInConstraint(List<String> items, boolean caseSensitive, String errorMessage) {
        this.items = new ArrayList(items);
        this.caseSensitive = caseSensitive;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a new ExistIn constraint
     *
     * @param items         the array items the input value must exist in
     * @param caseSensitive compare the input with the items with case-sensitivity or not
     * @param errorMessage  the default error message if the constraint fails
     */
    public ExistInConstraint(String[] items, boolean caseSensitive, String errorMessage) {
        this(Arrays.asList(items), caseSensitive, errorMessage);
    }

    /**
     * Creates a new ExistIn constraint
     *
     * @param items        the array items the input value must exist in
     * @param errorMessage the default error message if the constraint fails
     */
    public ExistInConstraint(List<String> items, String errorMessage) {
        this(items, false, errorMessage);
    }

    /**
     * Creates a new ExistIn constraint
     *
     * @param items        the array items the input value must exist in
     * @param errorMessage the default error message if the constraint fails
     */
    public ExistInConstraint(String[] items, String errorMessage) {
        this(items, false, errorMessage);
    }

    /**
     * Creates a new ExistIn constraint
     *
     * @param items the array items the input value must exist in
     */
    public ExistInConstraint(List<String> items) {
        this(items, false, null);
    }

    /**
     * Creates a new ExistIn constraint
     *
     * @param items the array items the input value must exist in
     */
    public ExistInConstraint(String[] items) {
        this(items, false, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(Object value) {
        if (!caseSensitive) {
            return containsCaseInsensitive(value.toString(), items);
        }

        return items.contains(value.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultFailMessage() {
        if (errorMessage == null) {
            if (items.size() == 1) {
                return "Input value can only be \"" + items.get(0) + "\"";
            }
            return "Input value must be one of " + items.toString();
        }

        return errorMessage;
    }

    boolean containsCaseInsensitive(String s, List<String> items) {
        for (String item : items) {
            if (item.equalsIgnoreCase(s)) {
                return true;
            }
        }

        return false;
    }
}
