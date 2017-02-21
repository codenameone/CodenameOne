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

import com.codename1.cloud.BindTarget;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.geom.*;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.util.EventDispatcher;

/**
 *<p>CheckBox is a button that can be selected or deselected and displays
 * its state to the user. Check out {@link com.codename1.ui.RadioButton} for
 * a more exclusive selection approach. Both components support a toggle button
 * mode using the {@link com.codename1.ui.Button#setToggle(boolean)} API.</p>
 * 
 * <script src="https://gist.github.com/codenameone/dc7fccf13dc102bc5ea0.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-radiobutton-checkbox.png" alt="Sample usage of CheckBox/RadioButton/ButtonGroup" />
 * @author Chen Fishbein
 */
public class CheckBox extends Button {
    
    private boolean selected= false;
    
    private boolean oppositeSide;
    private EventDispatcher bindListeners = null;

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
     * {@inheritDoc}
     */
    public void released(int x, int y) {
        selected = !isSelected();
        super.released(x, y);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fireActionEvent(int x, int y) {
        super.fireActionEvent(x, y);
        if(bindListeners != null) {
            if(isSelected()) {
                bindListeners.fireBindTargetChange(this, "selected", Boolean.FALSE, Boolean.TRUE);
            } else {
                bindListeners.fireBindTargetChange(this, "selected", Boolean.TRUE, Boolean.FALSE);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void paint(Graphics g) {
        if(isToggle()) {
            getUIManager().getLookAndFeel().drawButton(g, this);
        } else {
            getUIManager().getLookAndFeel().drawCheckBox(g, this);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected Dimension calcPreferredSize(){
        return getUIManager().getLookAndFeel().getCheckBoxPreferredSize(this);
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
    
    /**
     * {@inheritDoc}
     */
    public String[] getBindablePropertyNames() {
        return new String[] {"selected"};
    }
    
    /**
     * {@inheritDoc}
     */
    public Class[] getBindablePropertyTypes() {
        return new Class[] {Boolean.class};
    }
    
    /**
     * {@inheritDoc}
     */
    public void bindProperty(String prop, BindTarget target) {
        if(prop.equals("selected")) {
            if(bindListeners == null) {
                bindListeners = new EventDispatcher();
            }
            bindListeners.addListener(target);
            return;
        }
        super.bindProperty(prop, target);
    }
    
    /**
     * {@inheritDoc}
     */
    public void unbindProperty(String prop, BindTarget target) {
        if(prop.equals("selected")) {
            if(bindListeners == null) {
                return;
            }
            bindListeners.removeListener(target);
            if(!bindListeners.hasListeners()) {
                bindListeners = null;
            }
            return;
        }
        super.unbindProperty(prop, target);
    }
    
    /**
     * {@inheritDoc}
     */
    public Object getBoundPropertyValue(String prop) {
        if(prop.equals("selected")) {
            if(isSelected()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return super.getBoundPropertyValue(prop);
    }

    /**
     * {@inheritDoc}
     */
    public void setBoundPropertyValue(String prop, Object value) {
        if(prop.equals("selected")) {
            setSelected(value != null && ((Boolean)value).booleanValue());
            return;
        }
        super.setBoundPropertyValue(prop, value);
    }
    
    /**
     * Shorthand for creating the check box setting the icon/text and making it into 
     * a toggle button
     * 
     * @param text the text for the button
     * @param icon the icon for the button
     * @return a check box
     */
    public static CheckBox createToggle(String text, Image icon) {
        CheckBox cb = new CheckBox(text, icon);
        cb.setToggle(true);
        return cb;
    }
    
    
    /**
     * Shorthand for creating the check box setting the icon/text and making it into 
     * a toggle button
     * 
     * @param text the text for the button
     * @return a check box
     */
    public static CheckBox createToggle(String text) {
        CheckBox cb = new CheckBox(text, null);
        cb.setToggle(true);
        return cb;
    }
    
    
    /**
     * Shorthand for creating the check box setting the icon/text and making it into 
     * a toggle button
     * 
     * @param icon the icon for the button
     * @return a check box
     */
    public static CheckBox createToggle(Image icon) {
        CheckBox cb = new CheckBox("", icon);
        cb.setToggle(true);
        return cb;
    }
    
}
