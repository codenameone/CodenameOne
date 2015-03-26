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
 * Groups several constraints as if they are one constraint
 *
 * @author Shai Almog
 */
public class GroupConstraint implements Constraint {
    private Constraint[] group;
    private String failMessage = null;
    
    /**
     * Create a new constraint group
     * @param group the group
     */
    public GroupConstraint(Constraint... group) {
        this.group = group;
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean isValid(Object value) {
        for(Constraint c : group) {
            if(!c.isValid(value)) {
                failMessage = c.getDefaultFailMessage();
                return false;
            }
        }
        return true;
    }

    /**
     * @inheritDoc
     */
    @Override
    public String getDefaultFailMessage() {
        return failMessage;
    }
}
