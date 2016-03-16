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
package com.codename1.ui;

import com.codename1.ui.events.ActionEvent;

/**
 * The NavigationCommand is a Command that navigates to a given Form.
 * The NavigationCommand calls the show() on the Form object that is returned from
 * getNextForm().
 *
 * @author Chen
 */
public class NavigationCommand extends Command{

    private Form nextForm;
    
    /**
     * Creates a new instance of NavigationCommand
     * 
     * @param command the string that will be placed on the Soft buttons\Menu
     */
    public NavigationCommand(String command) {
        super(command);
    }

    /**
     * Creates a new instance of NavigationCommand
     * 
     * @param command the string that will be placed on the Soft buttons\Menu
     * @param icon the icon representing the command
     */
    public NavigationCommand(String command, Image icon) {
        super(command, icon);
    }

    /**
     * Creates a new instance of NavigationCommand
     * 
     * @param command the string that will be placed on the Soft buttons\Menu
     * @param id user defined ID for a command simplifying switch statement code
     * working with a command
     */
    public NavigationCommand(String command, int id) {
        super(command, id);
    }

    /**
     * Creates a new instance of NavigationCommand
     * 
     * @param command the string that will be placed on the Soft buttons\Menu
     * @param icon the icon representing the command
     * @param id user defined ID for a command simplifying switch statement code
     * working with a command
     */
    public NavigationCommand(String command, Image icon, int id) {
        super(command, icon, id);
    }

    /**
     * Sets the Form to navigate to when the actionPerformed is invoked on this
     * Command
     *
     * @param nextForm The next Form
     */ 
    public void setNextForm(Form nextForm) {
        this.nextForm = nextForm;
    }
    
    /**
     * Gets the next Form
     * 
     * @return the next Form
     */ 
    public Form getNextForm(){
        return nextForm;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        super.actionPerformed(evt);
        Form n = getNextForm();
        if(n != null){
            n.show();
        }
    }
    
    
}
