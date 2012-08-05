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
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;

/**
 * A powerful button like component that allows multiple rows/and an icon to be added
 * every row/icon can have its own UIID. Internally the multi-button is a container with
 * a lead component. Up to 4 rows are supported.
 *
 * @author Shai Almog
 */
public class MultiButton extends Container {
    private Label firstRow = new Label("MultiButton");
    private Label secondRow = new Label();
    private Label thirdRow = new Label();
    private Label forthRow = new Label();
    private Label icon = new Label();
    private Button emblem = new Button();
    private boolean invert;
    private String group;    
    
    /**
     * Default constructor allowing the designer to create an instance of this class
     */
    public MultiButton() {
        setLayout(new BorderLayout());
        BorderLayout bl = new BorderLayout();
        //bl.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE);
        Container iconContainer = new Container(bl);
        iconContainer.addComponent(BorderLayout.CENTER, icon);
        Container labels = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        Container labelsBorder = new Container(new BorderLayout());
        labelsBorder.addComponent(BorderLayout.SOUTH, labels);
        addComponent(BorderLayout.CENTER, labelsBorder);
        addComponent(BorderLayout.WEST, iconContainer);
        bl = new BorderLayout();
        //bl.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE);
        Container emblemContainer = new Container(bl);
        emblemContainer.addComponent(BorderLayout.CENTER, emblem);
        addComponent(BorderLayout.EAST, emblemContainer);
        labelsBorder.addComponent(BorderLayout.CENTER, firstRow);
        labels.addComponent(secondRow);
        labels.addComponent(thirdRow);
        labels.addComponent(forthRow);
        firstRow.setUIID("MultiLine1");
        secondRow.setUIID("MultiLine2");
        thirdRow.setUIID("MultiLine3");
        forthRow.setUIID("MultiLine4");
        firstRow.setName("Line1");
        secondRow.setName("Line2");
        thirdRow.setName("Line3");
        forthRow.setName("Line4");
        icon.setName("icon");
        emblem.setName("emblem");
        emblem.setUIID("Emblem");
        setLeadComponent(emblem);
        setUIID("MultiButton");
        Image i = UIManager.getInstance().getThemeImageConstant("defaultEmblemImage");
        if(i != null) {
            emblem.setIcon(i);
        }
    }
    
    /**
     * Turns the multi-button into a checkbox multi-button
     * 
     * @param b true for a checkbox multi-button
     */
    public void setCheckBox(boolean b) {
        if(b != isCheckBox()) {
            Container par = emblem.getParent();
            Button old = emblem;
            if(b) {
                emblem = new CheckBox();
            } else {
                emblem = new Button();
            }
            emblem.setUIID(old.getUIID());
            if(old.getCommand() != null) {
                Image img = old.getIcon();
                emblem.setCommand(old.getCommand());
                emblem.setText("");
                emblem.setIcon(img);
            }
            par.replace(old, emblem, null);
            setLeadComponent(emblem);
        }
    }
    
    /**
     * Adds an action listener
     * 
     * @param al the action listener
     */
    public void addActionListener(ActionListener al) {
        emblem.addActionListener(al);
    }

    /**
     * Removes an action listener
     * 
     * @param al the action listener
     */
    public void removeActionListener(ActionListener al) {
        emblem.removeActionListener(al);
    }
    
    /**
     * Sets the command for the component, it doesn't affe
     * 
     * @param c the command
     */
    public void setCommand(Command c) {
        Image img = emblem.getIcon();
        emblem.setCommand(c);
        emblem.setIcon(img);
        emblem.setText("");
    }

    /**
     * Returns the command for the emblem
     * 
     * @return the command instance
     */
    public Command getCommand() {
        return emblem.getCommand();
    }
    
    /**
     * Returns true if this is a checkbox button
     * 
     * @return true for a checkbox button
     */
    public boolean isCheckBox() {
        return emblem instanceof CheckBox;
    }
    
    /**
     * Turns the multi-button into a radio multi-button
     * 
     * @param b true for a radio multi-button
     */
    public void setRadioButton(boolean b) {
        if(b != isRadioButton()) {
            Container par = emblem.getParent();
            Button old = emblem;
            if(b) {
                emblem = new RadioButton();
                if(group != null) {
                    ((RadioButton)emblem).setGroup(group);
                }
            } else {
                emblem = new Button();
            }
            emblem.setUIID(old.getUIID());
            if(old.getCommand() != null) {
                Image img = old.getIcon();
                emblem.setCommand(old.getCommand());
                emblem.setText("");
                emblem.setIcon(img);
            }
            par.replace(old, emblem, null);
            setLeadComponent(emblem);
        }
    }
    
    /**
     * Returns true if this is a radio button
     * 
     * @return true for a radio button
     */
    public boolean isRadioButton() {
        return emblem instanceof RadioButton;
    }
    
    /**
     * Returns true if the checkbox/radio button is selected
     * @return true if the checkbox/radio button is selected
     */
    public boolean isSelected() {
        return (emblem instanceof RadioButton || emblem instanceof CheckBox) && emblem.isSelected();
    }
    
    /**
     * Toggles the selected state for the radio button/check box modes
     * @param b true for checked false for unchecked
     */
    public void setSelected(boolean b) {
        if(emblem instanceof RadioButton) {
            ((RadioButton)emblem).setSelected(b);
            return;
        }
        if(emblem instanceof CheckBox) {
            ((CheckBox)emblem).setSelected(b);
            return;
        }
    }
    
    /**
     * Indicates the first two labels should be side by side
     * 
     * @param b true to place the first two labels side by side
     */
    public void setHorizontalLayout(boolean b) {
        if(isHorizontalLayout() != b) {
            secondRow.getParent().removeComponent(secondRow);
            if(b) {
                Container c = firstRow.getParent();
                c.addComponent(BorderLayout.EAST, secondRow);
            } else {
                Container c = thirdRow.getParent();
                c.addComponent(0, secondRow);
            }
        }
    }
    
    /**
     * Indicates whether the first two labels are be side by side
     * 
     * @return true if the first two labels are side by side
     */
    public boolean isHorizontalLayout() {
        return secondRow.getParent().getLayout() instanceof BorderLayout;
    }
    
    /**
     * Inverts the order of the first two entries so the second line appears first. 
     * This only works in horizontal mode!
     * 
     * @param b true to place the second row entry as the first entry
     */
    public void setInvertFirstTwoEntries(boolean b) {
        if(b != invert) {
            invert = b;
            if(isHorizontalLayout()) {
                Container c = firstRow.getParent();
                c.removeComponent(secondRow);
                if(invert) {
                    c.addComponent(BorderLayout.WEST, secondRow);
                } else {
                    c.addComponent(BorderLayout.EAST, secondRow);
                }
            }
        }
    }
    
    /**
     * Inverts the order of the first two entries so the second line appears first. 
     * This only works in horizontal mode!
     * 
     * @return true when the second row entry should be placed before the first entry
     */
    public boolean isInvertFirstTwoEntries() {
        return invert;
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
     * Sets the emblem
     * 
     * @param i the icon
     */
    public void setEmblem(Image i) {
        emblem.setIcon(i);
    }
    
    /**
     * Returns the emblem image
     * 
     * @return the image instance
     */
    public Image getEmblem() {
        return emblem.getIcon();
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
     * Sets the emblem position based on border layout constraints
     * 
     * @param s position either North/South/East/West
     */
    public void setEmblemPosition(String t) {
        removeComponent(emblem.getParent());
        addComponent(t, emblem.getParent());
        revalidate();
    }
    
    /**
     * Returns the emblem position based on border layout constraints
     * 
     * @return position either North/South/East/West
     */
    public String getEmblemPosition() {
        return (String)getLayout().getComponentConstraint(emblem.getParent());
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
     * Sets the name of the emblem (important for use in generic renderers
     * 
     * @param t name to set
     */
    public void setEmblemName(String t) {
        emblem.setName(t);
    }
    
    /**
     * Returns the name of the emblem
     * 
     * @return the name
     */
    public String getEmblemName() {
        return emblem.getName();
    }

    /**
     * Sets the UIID of the emblem
     * 
     * @param t UIID to set
     */
    public void setEmblemUIID(String t) {
        emblem.setUIID(t);
    }
    
    /**
     * Returns the UIID of the Emblem
     * 
     * @return the UIID 
     */
    public String getEmblemUIID() {
        return emblem.getUIID();
    }


    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {
            "line1", "line2", "line3", "line4", "name1", "name2", "name3", "name4", 
            "uiid1", "uiid2", "uiid3", "uiid4", "icon", "iconName", "iconUiid", "iconPosition",
            "emblem", "emblemName", "emblemUiid", "emblemPosition", "horizontalLayout", 
            "invertFirstTwoEntries", "checkBox", "radioButton", "group", "selected"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {
           String.class,// line1
           String.class,// line2
           String.class,// line3
           String.class,// line4
           String.class,// name1
           String.class,// name2
           String.class,// name3
           String.class,// name4
           String.class,// uiid1
           String.class,// uiid2
           String.class,// uiid3
           String.class,// uiid4
           Image.class,// icon
           String.class,// iconName
           String.class,// iconUiid
           String.class,// iconPosition
           Image.class,// emblem
           String.class,// emblemName
           String.class,// emblemUiid
           String.class,// emblemPosition
           Boolean.class,
           Boolean.class,
           Boolean.class,
           Boolean.class,
           String.class,// group
           Boolean.class // selected
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
        if(name.equals("emblem")) {
            return getEmblem();
        }
        if(name.equals("emblemName")) {
            return getEmblemName();
        }
        if(name.equals("emblemUiid")) {
            return getEmblemUIID();
        }
        if(name.equals("emblemPosition")) {
            return getEmblemPosition();
        }
        if(name.equals("horizontalLayout")) {
            if(isHorizontalLayout()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if(name.equals("invertFirstTwoEntries")) {
            if(isInvertFirstTwoEntries()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if(name.equals("checkBox")) {
            if(isCheckBox()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if(name.equals("radioButton")) {
            if(isRadioButton()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if(name.equals("group")) {
            return getGroup();
        }
        if(name.equals("selected")) {
            if(isSelected()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
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
        if(name.equals("emblem")) {
            setEmblem((Image)value);
            return null;
        }
        if(name.equals("emblemUiid")) {
            setEmblemUIID((String)value);
            return null;
        }
        if(name.equals("emblemName")) {
            setEmblemName((String)value);
            return null;
        }
        if(name.equals("emblemPosition")) {
            setEmblemPosition((String)value);
            return null;
        }
        if(name.equals("horizontalLayout")) {
            setHorizontalLayout(((Boolean)value).booleanValue());
            return null;
        }
        if(name.equals("invertFirstTwoEntries")) {
            setInvertFirstTwoEntries(((Boolean)value).booleanValue());
            return null;
        }
        if(name.equals("checkBox")) {
            setCheckBox(((Boolean)value).booleanValue());
            return null;
        }
        if(name.equals("radioButton")) {
            setRadioButton(((Boolean)value).booleanValue());
            return null;
        }
        if(name.equals("group")) {
            setGroup((String)value);
            return null;
        }
        if(name.equals("selected")) {
            setSelected(((Boolean)value).booleanValue());
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * Indicates the group for the radio button
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * Indicates the group for the radio button
     * @param group the group to set
     */
    public void setGroup(String group) {
        this.group = group;
        if(emblem instanceof RadioButton) {
            ((RadioButton)emblem).setGroup(group);
        }
    }
}
