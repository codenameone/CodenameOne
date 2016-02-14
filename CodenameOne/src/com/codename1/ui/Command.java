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
import com.codename1.ui.events.ActionListener;
import java.util.HashMap;

/**
 * The Command class provides a useful extension to the ActionListener 
 * interface in cases where the same functionality may be accessed by several controls.
 * 
 * @author Nir Shabi
 */
public class Command implements ActionListener{
    private boolean disposesDialog = true;
    private Image icon;
    private Image pressedIcon;
    private Image rolloverIcon;
    private Image disabledIcon;
    private String command;
    private boolean enabled = true;
    /**
     * Simplifies code dealing with commands allowing them to be used in switch statements
     * more easily
     */
    private int commandId;

    private HashMap<String, Object> clientProperties;

    /**
     * Creates a new instance of Command
     * 
     * @param command the string that will be placed on the Soft buttons\Menu
     */
    public Command(String command) {
        this.command = command;
    }

    /**
     * Creates a new instance of Command
     * 
     * @param command the string that will be placed on the Soft buttons\Menu
     * @param icon the icon representing the command
     */
    public Command(String command, Image icon) {
        this.command = command;
        this.icon = icon;
    }


    /**
     * Creates a new instance of Command
     * 
     * @param command the string that will be placed on the Soft buttons\Menu
     * @param id user defined ID for a command simplifying switch statement code
     * working with a command
     */
    public Command(String command, int id) {
        this.command = command;
        this.commandId = id;
    }
    
    /**
     * Creates a new instance of Command
     * 
     * @param command the string that will be placed on the Soft buttons\Menu
     * @param icon the icon representing the command
     * @param id user defined ID for a command simplifying switch statement code
     * working with a command
     */
    public Command(String command, Image icon, int id) {
        this.command = command;
        this.commandId = id;
        this.icon = icon;
    }
    
    /**
     * Return the command ID
     * 
     * @return the command ID
     */
    public int getId() {
        return commandId;
    }
    
    /**
     * gets the Command Name
     * 
     * @return the Command name
     */
    public String getCommandName() {
        return command;
    }

    /**
     * sets the Command name
     * 
     * @param command
     */
    public void setCommandName(String command) {
        this.command = command;
    }
    
    /**
     * Returns the icon representing the command
     * 
     * @return an icon representing the command
     */
    public Image getIcon() {
        return icon;
    }

    /**
     * Sets the icon for the command
     * @param icon the new icon
     */
    public void setIcon(Image icon) {
        this.icon = icon;
    }
    
    /**
     * Returns a string representation of the object
     * 
     * @return Returns a string representation of the object
     */
    public String toString() {
        return getCommandName();
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in
     * pressed state
     *
     * @return icon used
     */
    public Image getPressedIcon() {
        return pressedIcon;
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in
     * the disabled state
     *
     * @return icon used
     */
    public Image getDisabledIcon() {
        return disabledIcon;
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in
     * rolled over state
     *
     * @return icon used
     */
    public Image getRolloverIcon() {
        return rolloverIcon;
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in
     * rolled over state
     *
     * @param rolloverIcon icon to use
     */
    public void setRolloverIcon(Image rolloverIcon) {
        this.rolloverIcon = rolloverIcon;
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in
     * pressed state
     *
     * @param pressedIcon icon used
     */
    public void setPressedIcon(Image pressedIcon) {
        this.pressedIcon = pressedIcon;
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in
     * the disabled state
     *
     * @param disabledIcon icon used
     */
    public void setDisabledIcon(Image disabledIcon) {
        this.disabledIcon = disabledIcon;
    }

    /**
     * compare two commands
     * 
     * @param obj a Command Object to compare
     * @return true if the obj has the same command name
     */
    public boolean equals(Object obj) {
        if(!(obj instanceof Command)) {
            return false;
        }
        if(((Command)obj).command == null) {
            return (obj != null) && obj.getClass() == getClass() && command == null &&
                ((Command)obj).icon == icon && ((Command)obj).commandId == commandId && 
                (clientProperties == ((Command)obj).clientProperties || clientProperties != null && clientProperties.equals(((Command)obj).clientProperties));
        } else {
            return (obj != null) && obj.getClass() == getClass() && ((Command)obj).command.equals(command) &&
                ((Command)obj).icon == icon && ((Command)obj).commandId == commandId &&
                (clientProperties == ((Command)obj).clientProperties || clientProperties != null && clientProperties.equals(((Command)obj).clientProperties));
        }
    }

    /**
     * Allows storing commands in a vector/hashtable
     * 
     * @return unique hashcode for the command class
     */
    public int hashCode() {
        return getClass().hashCode() + commandId;
    }
    
    /**
     * This method is called when the soft button/Menu item is clicked
     * 
     * @param evt the Event Object
     */
    public void actionPerformed(ActionEvent evt) {
    }

    /**
     * Indicates whether this command causes the dialog to dispose implicitly, defaults to true
     */
    public void setDisposesDialog(boolean disposesDialog) {
        this.disposesDialog = disposesDialog;
    }
    
    /**
     * Indicates whether this command causes the dialog to dispose implicitly, defaults to true
     */
    public boolean isDisposesDialog() {
        return disposesDialog;
    }

    /**
     * Allows disabling/enabling the command
     *
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Allows disabling/enabling the command
     *
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * The client properties are a useful way to associate meta-data with a command
     * without subclassing
     * @param key an arbitrary user key
     * @return an arbitrary user object
     */
    public Object getClientProperty(String key) {
        if(clientProperties != null) {
            return clientProperties.get(key);
        }
        return null;
    }

    /**
     * The client properties are a useful way to associate meta-data with a command
     * without sub classing
     * @param key an arbitrary user key
     * @param value an arbitrary user object, null to remove
     */
    public void putClientProperty(String key, Object value) {
        if(clientProperties == null) {
            clientProperties = new HashMap<String, Object>();
        }
        if(value == null) {
            clientProperties.remove(key);
        } else {
            clientProperties.put(key, value);
        }
    }
}
