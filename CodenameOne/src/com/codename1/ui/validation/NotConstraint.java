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
 * Negates a group of constraints, such that, if any of its child constraints is true, it returns false.
 *
 * @author Diamond Mubaarak
 * @since 7.0
 */
public class NotConstraint implements Constraint {

    private final Constraint[] constraints;
    private String failMessage = null;

    /**
     * Creates a new NotConstraint
     *
     * @param children the child constraints
     */
    public NotConstraint(Constraint... children) {
        this.constraints = children;
    }

    /**
     * Creates a new NotConstraint
     *
     * @param failMessage the default error message if the constraint fails
     * @param children    the child constraints
     */
    public NotConstraint(String failMessage, Constraint... children) {
        this.failMessage = failMessage;
        this.constraints = children;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(Object value) {
        for (Constraint c : constraints) {
            if (c.isValid(value)) return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultFailMessage() {
        if (failMessage == null) {
            return "Input value is invalid";
        }

        return failMessage;
    }
}
