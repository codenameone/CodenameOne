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

package com.codename1.ui.util.xml.comps;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 *
 * @author Shai Almog
 */
@XmlRootElement(name="command")
@XmlAccessorType(XmlAccessType.FIELD)
public class CommandEntry {
    @XmlAttribute
    private String name;
    
    @XmlAttribute
    private String icon;
    
    @XmlAttribute
    private String rolloverIcon;
    
    @XmlAttribute
    private String pressedIcon;
    
    @XmlAttribute
    private String disabledIcon;
    
    @XmlAttribute
    private int id;
    
    @XmlAttribute
    private String action;
    
    @XmlAttribute
    private String argument;
    
    @XmlAttribute
    private boolean backCommand;

    /**
     * @return the value
     */
    public String getName() {
        return name;
    }

    /**
     * @param value the value to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * @return the rolloverIcon
     */
    public String getRolloverIcon() {
        return rolloverIcon;
    }

    /**
     * @param rolloverIcon the rolloverIcon to set
     */
    public void setRolloverIcon(String rolloverIcon) {
        this.rolloverIcon = rolloverIcon;
    }

    /**
     * @return the pressedIcon
     */
    public String getPressedIcon() {
        return pressedIcon;
    }

    /**
     * @param pressedIcon the pressedIcon to set
     */
    public void setPressedIcon(String pressedIcon) {
        this.pressedIcon = pressedIcon;
    }

    /**
     * @return the disabledIcon
     */
    public String getDisabledIcon() {
        return disabledIcon;
    }

    /**
     * @param disabledIcon the disabledIcon to set
     */
    public void setDisabledIcon(String disabledIcon) {
        this.disabledIcon = disabledIcon;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the argument
     */
    public String getArgument() {
        return argument;
    }

    /**
     * @param argument the argument to set
     */
    public void setArgument(String argument) {
        this.argument = argument;
    }

    /**
     * @return the backCommand
     */
    public boolean isBackCommand() {
        return backCommand;
    }

    /**
     * @param backCommand the backCommand to set
     */
    public void setBackCommand(boolean backCommand) {
        this.backCommand = backCommand;
    }
}
