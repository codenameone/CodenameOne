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

import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.UIManager;

/**
 * RadioButton is a {@link Button} that maintains a selection state exclusively
 * within a specific {@link ButtonGroup}
 * 
 * @author Chen Fishbein
 */
public class RadioButton extends Button {

    
    private boolean selected= false;
    
    /**
     * The group in which this button is a part
     */
    private ButtonGroup group;

    private boolean oppositeSide;

    /**
     * Constructs a radio with the given text
     * 
     * @param text to display next to the button
     */
    public RadioButton(String text) {
        this(text, null);
    }
    
    /**
     * Creates an empty radio button
     */
    public RadioButton() {
        this("");
    }
    
    /**
     * Constructs a radio with the given icon
     * 
     * @param icon icon to show next to the button
     */
    public RadioButton(Image icon) {
        this("", icon);
    }

    /**
     * Constructs a radio with the given text and icon
     * 
     * @param text to display next to the button
     * @param icon icon to show next to the button
     */
    public RadioButton(String text,Image icon) {
        super(text,icon);
        setUIID("RadioButton");
    }

    /**
     * @inheritDoc
     */
    protected void initLaf(UIManager uim) {
        super.initLaf(uim);
        Boolean v = getUIManager().isThemeConstant("radioOppositeSideBool");
        if (v != null) {
            oppositeSide = v.booleanValue();
        }

    }
    
    
    
    /**
     * @inheritDoc
     */
    public String toString() {
        return "Radio Button " + getText();
    }

    int getAvaliableSpaceForText() {
        if(isToggle()) {
            return super.getAvaliableSpaceForText();
        }
        LookAndFeel l = getUIManager().getLookAndFeel();
        if(l instanceof DefaultLookAndFeel) {
            Image[] rButtonImages = ((DefaultLookAndFeel)l).getRadioButtonImages();
            if (rButtonImages != null) {
                int index = isSelected() ? 1 : 0;
                return super.getAvaliableSpaceForText() - rButtonImages[index].getWidth();
            }
        }
        return super.getAvaliableSpaceForText() - (getHeight() + getGap());
    }
    
    /**
     * Returns true if the radio button is selected
     * 
     * @return true if the radio button is selected
     */
    public boolean isSelected() {
        return selected;
    }

    void setSelectedImpl(boolean selected) {
        this.selected = selected;
        repaint();
    }
    
    /**
     * Selects the current radio button
     * 
     * @param selected value for selection
     */
    public void setSelected(boolean selected) {
        setSelectedImpl(selected);
        if(group != null && selected) {
            group.setSelected(this);
        }
    }
    
    /**
     * @inheritDoc
     */
    public void released(int x, int y) {
        // prevent the radio button from being "turned off"
        if(!isSelected()) {
            setSelected(true);
        }
        super.released(x, y);
    }
    
    /**
     * @inheritDoc
     */
    public void paint(Graphics g) {
        if(isToggle()) {
            getUIManager().getLookAndFeel().drawButton(g, this);
        } else {
            getUIManager().getLookAndFeel().drawRadioButton(g, this);
        }
    }
    
    /**
     * @inheritDoc
     */
    protected Dimension calcPreferredSize(){
        return getUIManager().getLookAndFeel().getRadioButtonPreferredSize(this);
    }
    
    /**
     * Setting a new button group
     * 
     * @param group a new button group
     */
    void setButtonGroup(ButtonGroup group) {
        this.group = group;
    }

    /**
     * @inheritDoc
     */
    void fireActionEvent() {
        if(group != null) {
            group.setSelected(this);
        }
        super.fireActionEvent();
    }

    /**
     * This is a helper method to ease the usage of button groups
     *
     * @param groupName a name for the goup
     */
    public void setGroup(String groupName) {
        putClientProperty("$group", groupName);
        initNamedGroup();
    }

    void initComponentImpl() {
        super.initComponentImpl();
        initNamedGroup();
    }


    private void initNamedGroup() {
        if(isInitialized()) {
            String s = getGroup();
            if(s != null) {
                Form f = getComponentForm();
                ButtonGroup b = (ButtonGroup)f.getClientProperty("$radio" + s);
                if(b == null) {
                    b = new ButtonGroup();
                    f.putClientProperty("$radio" + s, b);
                }
                b.add(this);
            }
        }
    }

    /**
     * This is a helper method to ease the usage of button groups
     *
     * @return the name of the group
     */
    public String getGroup() {
        return (String)getClientProperty("$group");
    }

    /**
     * Places the radio box on the opposite side at the far end
     *
     * @return the oppositeSide
     */
    public boolean isOppositeSide() {
        return oppositeSide;
    }

    /**
     * Places the radio box on the opposite side at the far end
     *
     * @param oppositeSide the oppositeSide to set
     */
    public void setOppositeSide(boolean oppositeSide) {
        this.oppositeSide = oppositeSide;
    }
}
