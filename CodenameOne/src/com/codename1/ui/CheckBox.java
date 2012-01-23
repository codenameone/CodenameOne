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

import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.geom.*;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.LookAndFeel;

/**
 * Checkbox is a button that can be selected or deselected, and which displays
 * its state to the user.
 * 
 * @author Chen Fishbein
 */
public class CheckBox extends Button {
    
    private boolean selected= false;
    
    private boolean oppositeSide;

    /**
     * Constructs a checkbox with the given text
     * 
     * @param text to display next to the checkbox
     */
    public CheckBox(String text) {
        this(text, null);
    }

    /**
     * Constructs a checkbox with no text
     */
    public CheckBox() {
        this("");
    }
    
    /**
     * Constructs a checkbox with the given icon
     * 
     * @param icon icon to display next to the checkbox
     */
    public CheckBox(Image icon) {
        this("", icon);
    }

    /**
     * Constructs a checkbox with the given text and icon
     * 
     * @param text to display next to the checkbox
     * @param icon icon to display next to the text
     */
    public CheckBox(String text,Image icon) {
        super(text,icon);
        setUIID("CheckBox");
        updateSide();
    }
    
    
    /**
     * Return true if the checkbox is selected
     * 
     * @return true if the checkbox is selected
     */
    public boolean isSelected() {
        return selected;
    }
    
    /**
     * Selects the current checkbox
     * 
     * @param selected value for selection
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }
    
    /**
     * @inheritDoc
     */
    public void released(int x, int y) {
        selected = !isSelected();
        super.released(x, y);
    }
    
    /**
     * @inheritDoc
     */
    public void paint(Graphics g) {
        if(isToggle()) {
            getUIManager().getLookAndFeel().drawButton(g, this);
        } else {
            getUIManager().getLookAndFeel().drawCheckBox(g, this);
        }
    }

    /**
     * @inheritDoc
     */
    protected Dimension calcPreferredSize(){
        return getUIManager().getLookAndFeel().getCheckBoxPreferredSize(this);
    }

    /**
     * @inheritDoc
     */
    protected String paramString() {
        return super.paramString() + ", selected = " +selected;
    }

    void initComponentImpl() {
        super.initComponentImpl();
    }

    private void updateSide() {
        Boolean v = getUIManager().isThemeConstant("checkBoxOppositeSideBool");
        if(v != null) {
            oppositeSide = v.booleanValue();
        }
    }

    /**
     * @inheritDoc
     */
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        updateSide();
    }

    int getAvaliableSpaceForText() {
        if(isToggle()) {
            return super.getAvaliableSpaceForText();
        }
        LookAndFeel l = getUIManager().getLookAndFeel();
        if(l instanceof DefaultLookAndFeel) {
            Image[] rButtonImages = ((DefaultLookAndFeel)l).getCheckBoxImages();
            if (rButtonImages != null) {
                int index = isSelected() ? 1 : 0;
                return super.getAvaliableSpaceForText() - rButtonImages[index].getWidth();
            }
        }
        return super.getAvaliableSpaceForText() - (getHeight() + getGap());
    }

    /**
     * Places the check box on the opposite side at the far end
     *
     * @return the oppositeSide
     */
    public boolean isOppositeSide() {
        return oppositeSide;
    }

    /**
     * Places the check box on the opposite side at the far end
     *
     * @param oppositeSide the oppositeSide to set
     */
    public void setOppositeSide(boolean oppositeSide) {
        this.oppositeSide = oppositeSide;
    }
}
