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
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.SelectableIconHolder;
import com.codename1.ui.TextHolder;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionSource;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.UIManager;
import java.util.StringTokenizer;

/**
 * <p>A powerful button like component that allows multiple rows/and an icon to be added
 * every row/icon can have its own UIID. Internally the multi-button is a container with
 * a lead component. Up to 4 rows are supported.</p>
 * 
 * <script src="https://gist.github.com/codenameone/c0991e96258f813df91e.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-multibutton.png" alt="MultiButton usages Sample" />
 *
 * @see SpanButton
 * @author Shai Almog
 */
public class MultiButton extends Container implements ActionSource, SelectableIconHolder, TextHolder {
    private Label firstRow = new Label("MultiButton");
    private Label secondRow = new Label();
    private Label thirdRow = new Label();
    private Label forthRow = new Label();
    private Button icon = new Button();
    private Button emblem = new Button();
    private boolean invert;
    private String group; 
    private int gap;
    
    /**
     * Initializes a multibutton with the first line of text
     * @param line1 first line of text
     */
    public MultiButton(String line1) {
        this();
        setTextLine1(line1);
    }
    
    /**
     * Default constructor allowing the designer to create an instance of this class
     */
    public MultiButton() {
        setLayout(new BorderLayout());
        setFocusable(true);
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
        icon.setUIID("Label");
        setLeadComponent(emblem);
        setUIID("MultiButton");
        Image i = UIManager.getInstance().getThemeImageConstant("defaultEmblemImage");
        if(i != null) {
            emblem.setIcon(i);
        }
        icon.bindStateTo(emblem);
    }
    
    /**
     * Changes the layout so the lines of the button are grouped together
     * @param l true to group the lines together
     */
    public void setLinesTogetherMode(boolean l) {
        if(l != isLinesTogetherMode()) {
            if(l) {
                firstRow.getParent().removeComponent(firstRow);
                Container p = secondRow.getParent();
                p.addComponent(0, firstRow);
                Container pp = p.getParent();
                pp.removeComponent(p);
                pp.addComponent(BorderLayout.CENTER, p);
            } else {
                secondRow.getParent().removeComponent(secondRow);
                thirdRow.getParent().addComponent(0, secondRow);
            }
        }
    }
    
    /**
     * Indicates if the lines are grouped together on this button
     * @return 
     */
    public boolean isLinesTogetherMode() {
        return firstRow.getParent() == secondRow.getParent();
    }
    
    /**
     * Allows us to gain direct access to the icon component so we can set it directly without going
     * via the other methods, this is especially useful for classes such as the ImageDownloadService
     * which can then update the icon seamlessly.
     * @return the component used internally to represent the icon
     */
    public Label getIconComponent() {
        return icon;
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
            emblem.setName(old.getName());
            java.util.List actionListeners = (java.util.List)old.getListeners();
            if(actionListeners != null) {
                for(int iter = 0 ; iter < actionListeners.size() ; iter++) {
                    emblem.addActionListener((ActionListener)actionListeners.get(iter));
                }
            }
            if(old.getCommand() != null) {
                Image img = old.getIcon();
                emblem.setCommand(old.getCommand());
                emblem.setText("");
                emblem.setIcon(img);
            } else {
                emblem.setText(old.getText());
                if(old.getIcon() != null) {
                    emblem.setIcon(old.getIcon());
                }
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
     * {@inheritDoc }
     * @param l 
     */
    @Override
    public void addLongPressListener(ActionListener l) {
        emblem.addLongPressListener(l);
    }

    /**
     * {@inheritDoc}
     * @param l 
     */
    @Override
    public void removeLongPressListener(ActionListener l) {
        emblem.removeLongPressListener(l);
    }

    /**
     * {@inheritDoc}
     * @param l 
     */
    @Override
    public void addPointerPressedListener(ActionListener l) {
        emblem.addPointerPressedListener(l);
    }

    /**
     * {@inheritDoc}
     * @param l 
     */
    @Override
    public void removePointerPressedListener(ActionListener l) {
        emblem.removePointerPressedListener(l);
    }
    
    /**
     * {@inheritDoc}
     * @param l 
     */
    public void addPointerReleasedListener(ActionListener l) {
        emblem.addPointerReleasedListener(l);
    }
    
    /**
     * {@inheritDoc}
     * @param l 
     */
    public void removePointerReleasedListener(ActionListener l) {
        emblem.removePointerReleasedListener(l);
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
            emblem.setName(old.getName());
            emblem.setUIID(old.getUIID());
            java.util.List actionListeners = (java.util.List)old.getListeners();
            if(actionListeners != null) {
                for(int iter = 0 ; iter < actionListeners.size() ; iter++) {
                    emblem.addActionListener((ActionListener)actionListeners.get(iter));
                }
            }
            if(old.getCommand() != null) {
                Image img = old.getIcon();
                emblem.setCommand(old.getCommand());
                emblem.setText("");
                emblem.setIcon(img);
            }
            par.replace(old, emblem, null);
            setLeadComponent(emblem);
            emblem.setShowEvenIfBlank(true);
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
            if(isHorizontalLayout()) {
                secondRow.getParent().getParent().removeComponent(secondRow.getParent());
            }
            secondRow.getParent().removeComponent(secondRow);
            if(b) {
                Container wrapper = new Container();
                Container c = firstRow.getParent();
                wrapper.addComponent(secondRow);
                c.addComponent(BorderLayout.EAST, wrapper);
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
        return secondRow.getParent().getLayout() instanceof FlowLayout;
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
     * Sets the name of the row (important for use in generic renderers)
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
        updateGap();
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
     * @param t position either North/South/East/West
     */
    public void setIconPosition(String t) {
        String ip = getEmblemPosition();
        if(ip != null && ip.equals(t)) {
            String ep = getIconPosition();
            removeComponent(icon.getParent());
            setEmblemPosition(ep);
        } else {
            removeComponent(icon.getParent());
        }
        addComponent(t, icon.getParent());
        updateGap();
        revalidateLater();
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
     * @param t position either North/South/East/West
     */
    public void setEmblemPosition(String t) {
        String ip = getIconPosition();
        if(ip != null && ip.equals(t)) {
            String ep = getEmblemPosition();
            removeComponent(emblem.getParent());
            setIconPosition(ep);
        } else {
            removeComponent(emblem.getParent());
        }
        addComponent(t, emblem.getParent());
        revalidateLater();
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
     * Sets the line 1 text
     * {@inheritDoc}
     */
    @Override
    public void setText(String text) {
        setTextLine1(text);
    }

    /**
     * Sets textLine1, textLine2, textLine3, and textLine4 in single method with single string
     * using "\n" as a delimiter.
     * @param text The text to set.
     * @since 8.0
     */
    public void setTextLines(String text) {
        //String currTextVal = btn.getText();
        String newTextVal0 = text;
        int line = 0;
        StringTokenizer strtok = new StringTokenizer(newTextVal0, "\n");
        while (strtok.hasMoreTokens()) {
            line++;
            String currTextVal;
            switch (line) {
                case 1: currTextVal = getTextLine1(); break;
                case 2: currTextVal = getTextLine2(); break;
                case 3: currTextVal = getTextLine3(); break;
                case 4: currTextVal = getTextLine4(); break;
                default: currTextVal = getText();
            }
            String newTextVal = strtok.nextToken().trim();
            if (!com.codename1.compat.java.util.Objects.equals(currTextVal, newTextVal)) {
                switch (line) {
                    case 1: setTextLine1(newTextVal); break;
                    case 2: setTextLine2(newTextVal); break;
                    case 3: setTextLine3(newTextVal); break;
                    case 4: setTextLine4(newTextVal); break;
                    default: setText(newTextVal);

                }
            }
        }
        while (line < 4) {
            line++;
            String currTextVal;
            switch (line) {
                case 1: currTextVal = getTextLine1(); break;
                case 2: currTextVal = getTextLine2(); break;
                case 3: currTextVal = getTextLine3(); break;
                case 4: currTextVal = getTextLine4(); break;
                default: currTextVal = getText();
            }
            if (!com.codename1.compat.java.util.Objects.equals(currTextVal, "")) {
                switch (line) {
                    case 1: setTextLine1(""); break;
                    case 2: setTextLine2(""); break;
                    case 3: setTextLine3(""); break;
                    case 4: setTextLine4(""); break;
                    default: setText(""); break;

                }

            }
        }
    }

    /**
     * Gets all text in multibutton in a single string delimited by "\n" character.
     * @since 8.0
     * @return String with textLine1 to textLine4 delimited by "\n"
     */
    public String getTextLines() {
        return getTextLine1() + "\n" + getTextLine2() + "\n" + getTextLine3() + "\n" + getTextLine4();
    }

    /**
     * Returns the line 1 text
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return getTextLine1();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {
            "line1", "line2", "line3", "line4", "name1", "name2", "name3", "name4", 
            "uiid1", "uiid2", "uiid3", "uiid4", "icon", "iconName", "iconUiid", "iconPosition",
            "emblem", "emblemName", "emblemUiid", "emblemPosition", "horizontalLayout", 
            "invertFirstTwoEntries", "checkBox", "radioButton", "group", "selected",
            "maskName"
        };
    }

    /**
     * {@inheritDoc}
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
           Boolean.class, // selected
           String.class
       };
    }

    /**
     * {@inheritDoc}
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
        if(name.equals("maskName")) {
            return getMaskName();
        }
        return null;
    }

    /**
     * {@inheritDoc}
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
        if(name.equals("maskName")) {
            setMaskName((String)value);
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

    /**
     * Set the mask name for the icon
     * @return the maskName
     */
    public String getMaskName() {
        return icon.getMaskName();
    }

    /**
     * The mask name for the icon
     * @param maskName the maskName to set
     */
    public void setMaskName(String maskName) {
        icon.setMaskName(maskName);
    }

    /**
     * Indicates if text should be localized when set to the component, by default
     * all text is localized so this allows disabling automatic localization for 
     * a specific component.
     * @return the shouldLocalize value
     */
    public boolean isShouldLocalize() {
        return firstRow.isShouldLocalize();
    }

    /**
     * Indicates if text should be localized when set to the component, by default
     * all text is localized so this allows disabling automatic localization for 
     * a specific component.
     * @param shouldLocalize the shouldLocalize to set
     */
    public void setShouldLocalize(boolean shouldLocalize) {
        firstRow.setShouldLocalize(shouldLocalize);
        secondRow.setShouldLocalize(shouldLocalize);
        thirdRow.setShouldLocalize(shouldLocalize);
        forthRow.setShouldLocalize(shouldLocalize);
    }
    
    /**
     * Sets the button group for a radio button mode multibutton
     * @param bg the button group
     */
    public void setGroup(ButtonGroup bg) {
        bg.add((RadioButton)emblem);
    }

   /**
     * {@inheritDoc }
     * @since 7.0
     * 
     */
    @Override
    public void setGap(int gap) {
        if (gap != this.gap) {
            this.gap = gap;
            updateGap();
        }
    }

    /**
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public int getGap() {
        return gap;
    }

    /**
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public void setTextPosition(int textPosition) {
        switch (textPosition) {
            case Component.TOP:
                setIconPosition(BorderLayout.SOUTH);
                break;
            case Component.BOTTOM:
                setIconPosition(BorderLayout.NORTH);
                break;
            case Component.LEFT:
                setIconPosition(BorderLayout.EAST);
                break;
            case Component.RIGHT:
                setIconPosition(BorderLayout.WEST);
                break;
            default:
                setIconPosition(BorderLayout.EAST);
        }
        
    }

    /**
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public int getTextPosition() {
        String iconPosition = getIconPosition();
        if (BorderLayout.NORTH.equals(iconPosition)) {
            return Component.BOTTOM;
        }
        if (BorderLayout.SOUTH.equals(iconPosition)) {
            return Component.TOP;
        }
        if (BorderLayout.EAST.equals(iconPosition)) {
            return Component.LEFT;
        }
        if (BorderLayout.WEST.equals(iconPosition)) {
            return Component.RIGHT;
        }
        return Component.LEFT;
        
    }
    
    
    private void updateGap() {
        if (getIcon() == null) {
            $(icon).setMargin(0);
        } else if (BorderLayout.NORTH.equals(getIconPosition())) {
            $(icon).selectAllStyles().setMargin(0, 0, gap, 0);
        } else if (BorderLayout.SOUTH.equals(getIconPosition())) {
            $(icon).selectAllStyles().setMargin(gap, 0, 0, 0);
        } else if (BorderLayout.EAST.equals(getIconPosition())) {
            $(icon).selectAllStyles().setMargin(0, 0, 0, gap);
        } else if (BorderLayout.WEST.equals(getIconPosition())) {
            $(icon).selectAllStyles().setMargin(0, gap, 0, 0);
        }
    }
    

    /**
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public Component getIconStyleComponent() {
        return icon;
    }

    /**
     * {@inheritDoc }
     * @since 8.0
     */
    @Override
    public void setMaterialIcon(char c, float size) {
        icon.setMaterialIcon(c, size);
    }

    /**
     * {@inheritDoc }
     * @since 8.0
     */
    @Override
    public void setFontIcon(Font font, char c, float size) {
        icon.setFontIcon(font, c, size);
    }

    /**
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public void setRolloverIcon(Image arg0) {
        icon.setRolloverIcon(arg0);
    }

    /**
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public Image getRolloverIcon() {
        return icon.getRolloverIcon();
    }

    
    /**
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public void setPressedIcon(Image arg0) {
        icon.setPressedIcon(arg0);
    }

    /**
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public Image getPressedIcon() {
        return icon.getPressedIcon();
    }

    /**
     * 
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public void setDisabledIcon(Image arg0) {
        icon.setDisabledIcon(arg0);
    }

    /**
     * 
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public Image getDisabledIcon() {
        return icon.getDisabledIcon();
    }

    /**
     * 
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public void setRolloverPressedIcon(Image icn) {
        icon.setRolloverPressedIcon(icn);
    }

    /**
     * 
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public Image getRolloverPressedIcon() {
        return icon.getRolloverPressedIcon();
    }

    /**
     * {@inheritDoc }
     * @since 7.0
     */
    @Override
    public Image getIconFromState() {
        return icon.getIconFromState();
    }

    /**
     * Sets the badge text to be used on this label.  Badges are rendered in the
     * upper right corner of the label inside round border.  The style of the badge can be
     * configured using {@link #setBadgeUIID(java.lang.String) }, but the default style uses
     * the "Badge" UIID, which, by default, uses white text on a red round border background.
     *
     * @param badgeText The text to include in the badge.   null or empty strings will result in the
     * badge not being rendered.
     * @since 8.0
     * @see #getBadgeText()
     * @see #getBadgeStyleComponent()
     * @see #setBadgeUIID(java.lang.String)
     */
    public void setBadgeText(String badgeText) {
        icon.setBadgeText(badgeText);
    }

    /**
     * Gets the text to be used in a badge on this label.
     * @return the badge text to be used on this label.  May return if no text is set.
     * @since 8.0
     * @see #setBadgeText(java.lang.String)
     * @see #setBadgeUIID(java.lang.String)
     * @see #getBadgeStyleComponent()
     */
    public String getBadgeText() {
        return icon.getBadgeText();
    }

    /**
     * Sets the style that should be used for rendering badges.  By default it will use
     * the "Badge" UIID, which rendered 1.5mm white text on a red round border.
     *
     * @param badgeUIID The UIID to use for the badge.
     * @since 8.0
     * @see #setBadgeText(java.lang.String)
     * @see #getBadgeStyleComponent()
     */
    public void setBadgeUIID(String badgeUIID) {
        icon.setBadgeUIID(badgeUIID);
    }

    /**
     * Gets a component that can be used for the style of the badge.
     * @return The component whose style can be used to style the badge.  May return null if none set.
     * @since 8.0
     * @see #setBadgeText(java.lang.String)
     * @see #setBadgeUIID(java.lang.String)
     * @see #getBadgeText()
     */
    public Component getBadgeStyleComponent() {
        return icon.getBadgeStyleComponent();
    }
}
