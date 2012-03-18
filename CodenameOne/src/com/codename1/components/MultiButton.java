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
package com.codename1.components;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.GenericListCellRenderer;

/**
 * A powerful button like component that allows multiple rows/and an icon to be added
 * every row/icon can have its own UIID. Internally the multi-button is a container with
 * a lead component. Up to 4 rows are supported.
 *
 * @author Shai Almog
 */
public class MultiButton extends Container {
    private Button firstRow = new Button();
    private Label secondRow = new Label();
    private Label thirdRow = new Label();
    private Label forthRow = new Label();
    private Label icon = new Label();
    private Container labels = new Container(new BoxLayout(BoxLayout.Y_AXIS));
    
    /**
     * Default constructor allowing the designer to create an instance of this class
     */
    public MultiButton() {
        setLayout(new BorderLayout());
        BorderLayout bl = new BorderLayout();
        bl.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE);
        Container iconContainer = new Container(bl);
        iconContainer.addComponent(BorderLayout.CENTER, icon);
        addComponent(BorderLayout.CENTER, labels);
        addComponent(BorderLayout.WEST, iconContainer);
        labels.addComponent(firstRow);
        labels.addComponent(secondRow);
        labels.addComponent(thirdRow);
        labels.addComponent(forthRow);
        firstRow.setUIID("Label");
        setLeadComponent(firstRow);
        setUIID("Button");
    }
    
    /**
     * Sets the content of the row
     * 
     * @param t text to set
     */
    public void setTextLine1(String t) {
        firstRow.setText(t);
    }
    
    /**
     * Returns the content of the row
     * 
     * @return the text 
     */
    public String getTextLine1() {
        return firstRow.getText();
    }

    /**
     * Sets the name of the row (important for use in generic renderers
     * 
     * @param t name to set
     */
    public void setNameLine1(String t) {
        firstRow.setName(t);
    }
    
    /**
     * Returns the name of the row
     * 
     * @return the name
     */
    public String getNameLine1() {
        return firstRow.getName();
    }

    /**
     * Sets the UIID of the row
     * 
     * @param t UIID to set
     */
    public void setUIIDLine1(String t) {
        firstRow.setUIID(t);
    }
    
    /**
     * Returns the UIID of the row
     * 
     * @return the UIID 
     */
    public String getUIIDLine1() {
        return firstRow.getUIID();
    }

    /**
     * Sets the content of the row
     * 
     * @param t text to set
     */
    public void setTextLine2(String t) {
        secondRow.setText(t);
    }
    
    /**
     * Returns the content of the row
     * 
     * @return the text 
     */
    public String getTextLine2() {
        return secondRow.getText();
    }

    /**
     * Sets the name of the row (important for use in generic renderers
     * 
     * @param t name to set
     */
    public void setNameLine2(String t) {
        secondRow.setName(t);
    }
    
    /**
     * Returns the name of the row
     * 
     * @return the name
     */
    public String getNameLine2() {
        return secondRow.getName();
    }

    /**
     * Sets the UIID of the row
     * 
     * @param t UIID to set
     */
    public void setUIIDLine2(String t) {
        secondRow.setUIID(t);
    }
    
    /**
     * Returns the UIID of the row
     * 
     * @return the UIID 
     */
    public String getUIIDLine2() {
        return secondRow.getUIID();
    }

    /**
     * Sets the content of the row
     * 
     * @param t text to set
     */
    public void setTextLine3(String t) {
        thirdRow.setText(t);
    }
    
    /**
     * Returns the content of the row
     * 
     * @return the text 
     */
    public String getTextLine3() {
        return thirdRow.getText();
    }

    /**
     * Sets the name of the row (important for use in generic renderers
     * 
     * @param t name to set
     */
    public void setNameLine3(String t) {
        thirdRow.setName(t);
    }
    
    /**
     * Returns the name of the row
     * 
     * @return the name
     */
    public String getNameLine3() {
        return thirdRow.getName();
    }

    /**
     * Sets the UIID of the row
     * 
     * @param t UIID to set
     */
    public void setUIIDLine3(String t) {
        thirdRow.setUIID(t);
    }
    
    /**
     * Returns the UIID of the row
     * 
     * @return the UIID 
     */
    public String getUIIDLine3() {
        return thirdRow.getUIID();
    }

    /**
     * Sets the content of the row
     * 
     * @param t text to set
     */
    public void setTextLine4(String t) {
        forthRow.setText(t);
    }
    
    /**
     * Returns the content of the row
     * 
     * @return the text 
     */
    public String getTextLine4() {
        return forthRow.getText();
    }

    /**
     * Sets the name of the row (important for use in generic renderers
     * 
     * @param t name to set
     */
    public void setNameLine4(String t) {
        forthRow.setName(t);
    }
    
    /**
     * Returns the name of the row
     * 
     * @return the name
     */
    public String getNameLine4() {
        return forthRow.getName();
    }

    /**
     * Sets the UIID of the row
     * 
     * @param t UIID to set
     */
    public void setUIIDLine4(String t) {
        forthRow.setUIID(t);
    }
    
    /**
     * Returns the UIID of the row
     * 
     * @return the UIID 
     */
    public String getUIIDLine4() {
        return forthRow.getUIID();
    }


    /**
     * Sets the icon
     * 
     * @param i the icon
     */
    public void setIcon(Image i) {
        icon.setIcon(i);
    }
    
    /**
     * Returns the icon image
     * 
     * @return the image instance
     */
    public Image getIcon() {
        return icon.getIcon();
    }

    /**
     * Sets the icon position based on border layout constraints
     * 
     * @param s position either North/South/East/West
     */
    public void setIconPosition(String t) {
        removeComponent(icon.getParent());
        addComponent(t, icon.getParent());
        revalidate();
    }
    
    /**
     * Returns the icon position based on border layout constraints
     * 
     * @return position either North/South/East/West
     */
    public String getIconPosition() {
        return (String)getLayout().getComponentConstraint(icon.getParent());
    }

    
    /**
     * Sets the name of the icon (important for use in generic renderers
     * 
     * @param t name to set
     */
    public void setIconName(String t) {
        icon.setName(t);
    }
    
    /**
     * Returns the name of the icon
     * 
     * @return the name
     */
    public String getIconName() {
        return icon.getName();
    }

    /**
     * Sets the UIID of the icon
     * 
     * @param t UIID to set
     */
    public void setIconUIID(String t) {
        icon.setUIID(t);
    }
    
    /**
     * Returns the UIID of the Icon
     * 
     * @return the UIID 
     */
    public String getIconUIID() {
        return icon.getUIID();
    }


    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {
            "line1", "line2", "line3", "line4", "name1", "name2", "name3", "name4", 
            "uiid1", "uiid2", "uiid3", "uiid4", "icon", "iconName", "iconUiid", "iconPosition"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {
           String.class,
           String.class,
           String.class,
           String.class,
           String.class,
           String.class,
           String.class,
           String.class,
           String.class,
           String.class,
           String.class,
           String.class,
           Image.class,
           String.class,
           String.class,
           String.class
       };
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if(name.equals("line1")) {
            return getTextLine1();
        }
        if(name.equals("line2")) {
            return getTextLine2();
        }
        if(name.equals("line3")) {
            return getTextLine3();
        }
        if(name.equals("line4")) {
            return getTextLine4();
        }
        if(name.equals("name1")) {
            return getNameLine1();
        }
        if(name.equals("name2")) {
            return getNameLine2();
        }
        if(name.equals("name3")) {
            return getNameLine3();
        }
        if(name.equals("name4")) {
            return getNameLine4();
        }
        if(name.equals("uiid1")) {
            return getUIIDLine1();
        }
        if(name.equals("uiid2")) {
            return getUIIDLine2();
        }
        if(name.equals("uiid3")) {
            return getUIIDLine3();
        }
        if(name.equals("uiid4")) {
            return getUIIDLine4();
        }
        if(name.equals("icon")) {
            return getIcon();
        }
        if(name.equals("iconName")) {
            return getIconName();
        }
        if(name.equals("iconUiid")) {
            return getIconUIID();
        }
        if(name.equals("iconPosition")) {
            return getIconPosition();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("line1")) {
            setTextLine1((String)value);
            return null;
        }
        if(name.equals("line2")) {
            setTextLine2((String)value);
            return null;
        }
        if(name.equals("line3")) {
            setTextLine3((String)value);
            return null;
        }
        if(name.equals("line4")) {
            setTextLine4((String)value);
            return null;
        }
        if(name.equals("name1")) {
            setNameLine1((String)value);
            return null;
        }
        if(name.equals("name2")) {
            setNameLine2((String)value);
            return null;
        }
        if(name.equals("name3")) {
            setNameLine3((String)value);
            return null;
        }
        if(name.equals("name4")) {
            setNameLine4((String)value);
            return null;
        }
        if(name.equals("uiid1")) {
            setUIIDLine1((String)value);
            return null;
        }
        if(name.equals("uiid2")) {
            setUIIDLine2((String)value);
            return null;
        }
        if(name.equals("uiid3")) {
            setUIIDLine3((String)value);
            return null;
        }
        if(name.equals("uiid4")) {
            setUIIDLine4((String)value);
            return null;
        }
        if(name.equals("icon")) {
            setIcon((Image)value);
            return null;
        }
        if(name.equals("iconUiid")) {
            setIconUIID((String)value);
            return null;
        }
        if(name.equals("iconName")) {
            setIconName((String)value);
            return null;
        }
        if(name.equals("iconPosition")) {
            setIconPosition((String)value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }
}
