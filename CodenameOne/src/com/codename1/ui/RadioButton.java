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
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;

/**
 * <p>RadioButton is a {@link Button} that maintains a selection state exclusively
 * within a specific {@link ButtonGroup}. Check out {@link com.codename1.ui.CheckBox} for
 * a looser selection approach. Both components support a toggle button
 * mode using the {@link com.codename1.ui.Button#setToggle(boolean)} API.</p>
 * 
 * <script src="https://gist.github.com/codenameone/dc7fccf13dc102bc5ea0.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-radiobutton-checkbox.png" alt="Sample usage of CheckBox/RadioButton/ButtonGroup" />
 * @author Chen Fishbein
 */
public class RadioButton extends Button {

    private boolean unselectAllowed; 
    
    private boolean selected;
    
    /**
     * The group in which this button is a part
     */
    private ButtonGroup group;

    private boolean oppositeSide;

    private EventDispatcher bindListeners;

    /**
     * Constructs a radio with the given text
     * 
     * @param text to display next to the button
     */
    public RadioButton(String text) {
        this(text, null);
    }

    /**
     * Shorthand for creating the radio button, adding it to a group, setting the icon/text and making it into 
     * a toggle button
     * 
     * @param text the text for the button
     * @param icon the icon for the button
     * @param bg the button group
     * @return a radio button
     */
    public static RadioButton createToggle(String text, Image icon, ButtonGroup bg) {
        RadioButton rb = new RadioButton(text, icon);
        bg.add(rb);
        rb.setToggle(true);
        return rb;
    }

    /**
     * Shorthand for creating the radio button, adding it to a group, setting the icon/text and making it into 
     * a toggle button
     * 
     * @param text the text for the button
     * @param bg the button group
     * @return a radio button
     */
    public static RadioButton createToggle(String text, ButtonGroup bg) {
        return createToggle(text, null, bg);
    }
    
    /**
     * Shorthand for creating the radio button, adding it to a group, setting the icon/text and making it into 
     * a toggle button
     * 
     * @param icon the icon for the button
     * @param bg the button group
     * @return a radio button
     */
    public static RadioButton createToggle(Image icon, ButtonGroup bg) {
        return createToggle(null, icon, bg);
    }
    
    /**
     * Shorthand for creating the radio button, adding it to a group, setting the command and making it into 
     * a toggle button
     * @param cmd the command
     * @param bg the button group
     * @return a radio button
     */
    public static RadioButton createToggle(Command cmd, ButtonGroup bg) {
        RadioButton rb = new RadioButton(cmd.getCommandName(), cmd.getIcon());
        rb.setCommand(cmd);
        bg.add(rb);
        rb.setToggle(true);
        return rb;
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
     * {@inheritDoc}
     */
    protected void initLaf(UIManager uim) {
        super.initLaf(uim);
        Boolean v = getUIManager().isThemeConstant("radioOppositeSideBool");
        if (v != null) {
            oppositeSide = v.booleanValue();
        }

    }
    
    void fireActionEvent(int x, int y) {
        super.fireActionEvent(x, y);
        if(bindListeners != null) {
            if(isSelected()) {
                bindListeners.fireBindTargetChange(this, "selected", Boolean.TRUE, Boolean.FALSE);
            } else {
                bindListeners.fireBindTargetChange(this, "selected", Boolean.FALSE, Boolean.TRUE);
            }
        }
    }
    
    
    /**
     * {@inheritDoc}
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
     * Returns true if this RadioButton can be unselected
     * @return true to allow deselection of radio buttons
     */
    public boolean isUnselectAllowed() {
        return unselectAllowed;
    }

    /**
     * Allows unselecting a selected RadioButton.
     * This is useful for when implementing a ButtonGroup that allows no selection or a single selection., 
     * @param unselectAllowed true to allow deselection of a radio button, false for the default behavior
     */
    public void setUnselectAllowed(boolean unselectAllowed) {
        this.unselectAllowed = unselectAllowed;
    }
    
    /**
     * {@inheritDoc}
     */
    public void released(int x, int y) {
        // prevent the radio button from being "turned off" unless unselectAllowed
        if(!isSelected() || unselectAllowed) { 
            setSelected(!isSelected()); 
        }
        super.released(x, y);
    }
    
    /**
     * {@inheritDoc}
     */
    public void paint(Graphics g) {
        if(isToggle()) {
            getUIManager().getLookAndFeel().drawButton(g, this);
        } else {
            getUIManager().getLookAndFeel().drawRadioButton(g, this);
        }
    }
    
    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
}
