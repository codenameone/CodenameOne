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

package com.codename1.designer;

/**
 * Command instance with additional fields and information which is used within the
 * UI builder to allow easier command mutation
 * 
 * @author Shai Almog
 */
public class ActionCommand extends com.codename1.ui.Command {
    private static int commandUniqueId = 1;
    private String action;
    private boolean backCommand;
    private String argument;
    public ActionCommand(String name, com.codename1.ui.Image icon, int id, String action, boolean backCommand, String argument) {
        super(name, icon, id);
        this.argument = argument;
        this.action = action;
        this.backCommand = backCommand;
        if(id >= commandUniqueId) {
            commandUniqueId = id + 1;
        }
    }

    public static int getCommandUniqueId() {
        return commandUniqueId;
    }

    public String getAction() {
        return action;
    }

    public boolean isBackCommand() {
        return backCommand;
    }

    public String getArgument() {
        return argument;
    }

    public boolean equals(Object o) {
        return super.equals(o) && o instanceof ActionCommand && 
                (action == ((ActionCommand)o).action || action != null && action.equals(((ActionCommand)o).action)) &&
                ((ActionCommand)o).backCommand == backCommand;
    }
}
