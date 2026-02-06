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

import static com.codename1.ui.ComponentSelector.$;

/// A powerful button like component that allows multiple rows/and an icon to be added
/// every row/icon can have its own UIID. Internally the multi-button is a container with
/// a lead component. Up to 4 rows are supported.
///
/// ```java
/// MultiButton twoLinesNoIcon = new MultiButton("MultiButton");
/// twoLinesNoIcon.setTextLine2("Line 2");
/// MultiButton oneLineIconEmblem = new MultiButton("Icon + Emblem");
/// oneLineIconEmblem.setIcon(icon);
/// oneLineIconEmblem.setEmblem(emblem);
/// MultiButton twoLinesIconEmblem = new MultiButton("Icon + Emblem");
/// twoLinesIconEmblem.setIcon(icon);
/// twoLinesIconEmblem.setEmblem(emblem);
/// twoLinesIconEmblem.setTextLine2("Line 2");
///
/// MultiButton twoLinesIconEmblemHorizontal = new MultiButton("Icon + Emblem");
/// twoLinesIconEmblemHorizontal.setIcon(icon);
/// twoLinesIconEmblemHorizontal.setEmblem(emblem);
/// twoLinesIconEmblemHorizontal.setTextLine2("Line 2 Horizontal");
/// twoLinesIconEmblemHorizontal.setHorizontalLayout(true);
///
/// MultiButton twoLinesIconCheckBox = new MultiButton("CheckBox");
/// twoLinesIconCheckBox.setIcon(icon);
/// twoLinesIconCheckBox.setCheckBox(true);
/// twoLinesIconCheckBox.setTextLine2("Line 2");
///
/// MultiButton fourLinesIcon = new MultiButton("With Icon");
/// fourLinesIcon.setIcon(icon);
/// fourLinesIcon.setTextLine2("Line 2");
/// fourLinesIcon.setTextLine3("Line 3");
/// fourLinesIcon.setTextLine4("Line 4");
///
/// hi.add(oneLineIconEmblem).
///         add(twoLinesNoIcon).
///         add(twoLinesIconEmblem).
///         add(twoLinesIconEmblemHorizontal).
///         add(twoLinesIconCheckBox).
///         add(fourLinesIcon);
/// ```
///
/// @author Shai Almog
///
/// #### See also
///
/// - SpanButton
public class MultiButton extends Container implements ActionSource, SelectableIconHolder, TextHolder {
    private final Label firstRow = new Label("MultiButton");
    private final Label secondRow = new Label();
    private final Label thirdRow = new Label();
    private final Label forthRow = new Label();
    private final Button icon = new Button();
    private Button emblem = new Button();
    private boolean invert;
    private String group;
    private int gap;

    /// Initializes a multibutton with the first line of text
    ///
    /// #### Parameters
    ///
    /// - `line1`: first line of text
    public MultiButton(String line1) {
        this();
        setTextLine1(line1);
    }

    /// Default constructor allowing the designer to create an instance of this class
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
        setUIIDFinal("MultiButton");
        Image i = UIManager.getInstance().getThemeImageConstant("defaultEmblemImage");
        if (i != null) {
            emblem.setIcon(i);
        }
        icon.bindStateTo(emblem);
    }

    /// Indicates if the lines are grouped together on this button
    public boolean isLinesTogetherMode() {
        return firstRow.getParent() == secondRow.getParent(); //NOPMD CompareObjectsWithEquals
    }

    /// Changes the layout so the lines of the button are grouped together
    ///
    /// #### Parameters
    ///
    /// - `l`: true to group the lines together
    public void setLinesTogetherMode(boolean l) {
        if (l != isLinesTogetherMode()) {
            if (l) {
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

    /// Allows us to gain direct access to the icon component so we can set it directly without going
    /// via the other methods, this is especially useful for classes such as the ImageDownloadService
    /// which can then update the icon seamlessly.
    ///
    /// #### Returns
    ///
    /// the component used internally to represent the icon
    public Label getIconComponent() {
        return icon;
    }

    /// Adds an action listener
    ///
    /// #### Parameters
    ///
    /// - `al`: the action listener
    @Override
    public void addActionListener(ActionListener al) {
        emblem.addActionListener(al);
    }

    /// Removes an action listener
    ///
    /// #### Parameters
    ///
    /// - `al`: the action listener
    @Override
    public void removeActionListener(ActionListener al) {
        emblem.removeActionListener(al);
    }

    /// {@inheritDoc }
    ///
    /// #### Parameters
    ///
    /// - `l`
    @Override
    public void addLongPressListener(ActionListener l) {
        emblem.addLongPressListener(l);
    }

    /// {@inheritDoc}
    ///
    /// #### Parameters
    ///
    /// - `l`
    @Override
    public void removeLongPressListener(ActionListener l) {
        emblem.removeLongPressListener(l);
    }

    /// {@inheritDoc}
    ///
    /// #### Parameters
    ///
    /// - `l`
    @Override
    public void addPointerPressedListener(ActionListener l) {
        emblem.addPointerPressedListener(l);
    }

    /// {@inheritDoc}
    ///
    /// #### Parameters
    ///
    /// - `l`
    @Override
    public void removePointerPressedListener(ActionListener l) {
        emblem.removePointerPressedListener(l);
    }

    /// {@inheritDoc}
    ///
    /// #### Parameters
    ///
    /// - `l`
    @Override
    public void addPointerReleasedListener(ActionListener l) {
        emblem.addPointerReleasedListener(l);
    }

    /// {@inheritDoc}
    ///
    /// #### Parameters
    ///
    /// - `l`
    @Override
    public void removePointerReleasedListener(ActionListener l) {
        emblem.removePointerReleasedListener(l);
    }

    /// Returns the command for the emblem
    ///
    /// #### Returns
    ///
    /// the command instance
    public Command getCommand() {
        return emblem.getCommand();
    }

    /// Sets the command for the component, it doesn't affe
    ///
    /// #### Parameters
    ///
    /// - `c`: the command
    public void setCommand(Command c) {
        Image img = emblem.getIcon();
        emblem.setCommand(c);
        emblem.setIcon(img);
        emblem.setText("");
    }

    /// Returns true if this is a checkbox button
    ///
    /// #### Returns
    ///
    /// true for a checkbox button
    public boolean isCheckBox() {
        return emblem instanceof CheckBox;
    }

    /// Turns the multi-button into a checkbox multi-button
    ///
    /// #### Parameters
    ///
    /// - `b`: true for a checkbox multi-button
    public void setCheckBox(boolean b) {
        if (b != isCheckBox()) {
            Container par = emblem.getParent();
            Button old = emblem;
            if (b) {
                emblem = new CheckBox();
            } else {
                emblem = new Button();
            }
            emblem.setUIID(old.getUIID());
            emblem.setName(old.getName());
            java.util.List actionListeners = (java.util.List) old.getListeners();
            if (actionListeners != null) {
                for (int iter = 0; iter < actionListeners.size(); iter++) {
                    emblem.addActionListener((ActionListener) actionListeners.get(iter));
                }
            }
            if (old.getCommand() != null) {
                Image img = old.getIcon();
                emblem.setCommand(old.getCommand());
                emblem.setText("");
                emblem.setIcon(img);
            } else {
                emblem.setText(old.getText());
                if (old.getIcon() != null) {
                    emblem.setIcon(old.getIcon());
                }
            }
            par.replace(old, emblem, null);
            setLeadComponent(emblem);
        }
    }

    /// Returns true if this is a radio button
    ///
    /// #### Returns
    ///
    /// true for a radio button
    public boolean isRadioButton() {
        return emblem instanceof RadioButton;
    }

    /// Turns the multi-button into a radio multi-button
    ///
    /// #### Parameters
    ///
    /// - `b`: true for a radio multi-button
    public void setRadioButton(boolean b) {
        if (b != isRadioButton()) {
            Container par = emblem.getParent();
            Button old = emblem;
            if (b) {
                emblem = new RadioButton();
                if (group != null) {
                    ((RadioButton) emblem).setGroup(group);
                }
            } else {
                emblem = new Button();
            }
            emblem.setName(old.getName());
            emblem.setUIID(old.getUIID());
            java.util.List actionListeners = (java.util.List) old.getListeners();
            if (actionListeners != null) {
                for (int iter = 0; iter < actionListeners.size(); iter++) {
                    emblem.addActionListener((ActionListener) actionListeners.get(iter));
                }
            }
            if (old.getCommand() != null) {
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

    /// Returns true if the checkbox/radio button is selected
    ///
    /// #### Returns
    ///
    /// true if the checkbox/radio button is selected
    public boolean isSelected() {
        return (emblem instanceof RadioButton || emblem instanceof CheckBox) && emblem.isSelected();
    }

    /// Toggles the selected state for the radio button/check box modes
    ///
    /// #### Parameters
    ///
    /// - `b`: true for checked false for unchecked
    public void setSelected(boolean b) {
        if (emblem instanceof RadioButton) {
            ((RadioButton) emblem).setSelected(b);
            return;
        }
        if (emblem instanceof CheckBox) {
            ((CheckBox) emblem).setSelected(b);
        }
    }

    /// Indicates whether the first two labels are be side by side
    ///
    /// #### Returns
    ///
    /// true if the first two labels are side by side
    public boolean isHorizontalLayout() {
        return secondRow.getParent().getLayout() instanceof FlowLayout;
    }

    /// Indicates the first two labels should be side by side
    ///
    /// #### Parameters
    ///
    /// - `b`: true to place the first two labels side by side
    public void setHorizontalLayout(boolean b) {
        if (isHorizontalLayout() != b) {
            if (isHorizontalLayout()) {
                secondRow.getParent().getParent().removeComponent(secondRow.getParent());
            }
            secondRow.getParent().removeComponent(secondRow);
            if (b) {
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

    /// Inverts the order of the first two entries so the second line appears first.
    /// This only works in horizontal mode!
    ///
    /// #### Returns
    ///
    /// true when the second row entry should be placed before the first entry
    public boolean isInvertFirstTwoEntries() {
        return invert;
    }

    /// Inverts the order of the first two entries so the second line appears first.
    /// This only works in horizontal mode!
    ///
    /// #### Parameters
    ///
    /// - `b`: true to place the second row entry as the first entry
    public void setInvertFirstTwoEntries(boolean b) {
        if (b != invert) {
            invert = b;
            if (isHorizontalLayout()) {
                Container c = firstRow.getParent();
                c.removeComponent(secondRow);
                if (invert) {
                    c.addComponent(BorderLayout.WEST, secondRow);
                } else {
                    c.addComponent(BorderLayout.EAST, secondRow);
                }
            }
        }
    }

    /// Returns the content of the row
    ///
    /// #### Returns
    ///
    /// the text
    public String getTextLine1() {
        return firstRow.getText();
    }

    /// Sets the content of the row
    ///
    /// #### Parameters
    ///
    /// - `t`: text to set
    public void setTextLine1(String t) {
        firstRow.setText(t);
    }

    /// Returns the name of the row
    ///
    /// #### Returns
    ///
    /// the name
    public String getNameLine1() {
        return firstRow.getName();
    }

    /// Sets the name of the row (important for use in generic renderers)
    ///
    /// #### Parameters
    ///
    /// - `t`: name to set
    public void setNameLine1(String t) {
        firstRow.setName(t);
    }

    /// Returns the UIID of the row
    ///
    /// #### Returns
    ///
    /// the UIID
    public String getUIIDLine1() {
        return firstRow.getUIID();
    }

    /// Sets the UIID of the row
    ///
    /// #### Parameters
    ///
    /// - `t`: UIID to set
    public void setUIIDLine1(String t) {
        firstRow.setUIID(t);
    }

    /// Returns the content of the row
    ///
    /// #### Returns
    ///
    /// the text
    public String getTextLine2() {
        return secondRow.getText();
    }

    /// Sets the content of the row
    ///
    /// #### Parameters
    ///
    /// - `t`: text to set
    public void setTextLine2(String t) {
        secondRow.setText(t);
    }

    /// Returns the name of the row
    ///
    /// #### Returns
    ///
    /// the name
    public String getNameLine2() {
        return secondRow.getName();
    }

    /// Sets the name of the row (important for use in generic renderers
    ///
    /// #### Parameters
    ///
    /// - `t`: name to set
    public void setNameLine2(String t) {
        secondRow.setName(t);
    }

    /// Returns the UIID of the row
    ///
    /// #### Returns
    ///
    /// the UIID
    public String getUIIDLine2() {
        return secondRow.getUIID();
    }

    /// Sets the UIID of the row
    ///
    /// #### Parameters
    ///
    /// - `t`: UIID to set
    public void setUIIDLine2(String t) {
        secondRow.setUIID(t);
    }

    /// Returns the content of the row
    ///
    /// #### Returns
    ///
    /// the text
    public String getTextLine3() {
        return thirdRow.getText();
    }

    /// Sets the content of the row
    ///
    /// #### Parameters
    ///
    /// - `t`: text to set
    public void setTextLine3(String t) {
        thirdRow.setText(t);
    }

    /// Returns the name of the row
    ///
    /// #### Returns
    ///
    /// the name
    public String getNameLine3() {
        return thirdRow.getName();
    }

    /// Sets the name of the row (important for use in generic renderers
    ///
    /// #### Parameters
    ///
    /// - `t`: name to set
    public void setNameLine3(String t) {
        thirdRow.setName(t);
    }

    /// Returns the UIID of the row
    ///
    /// #### Returns
    ///
    /// the UIID
    public String getUIIDLine3() {
        return thirdRow.getUIID();
    }

    /// Sets the UIID of the row
    ///
    /// #### Parameters
    ///
    /// - `t`: UIID to set
    public void setUIIDLine3(String t) {
        thirdRow.setUIID(t);
    }

    /// Returns the content of the row
    ///
    /// #### Returns
    ///
    /// the text
    public String getTextLine4() {
        return forthRow.getText();
    }

    /// Sets the content of the row
    ///
    /// #### Parameters
    ///
    /// - `t`: text to set
    public void setTextLine4(String t) {
        forthRow.setText(t);
    }

    /// Returns the name of the row
    ///
    /// #### Returns
    ///
    /// the name
    public String getNameLine4() {
        return forthRow.getName();
    }

    /// Sets the name of the row (important for use in generic renderers
    ///
    /// #### Parameters
    ///
    /// - `t`: name to set
    public void setNameLine4(String t) {
        forthRow.setName(t);
    }

    /// Returns the UIID of the row
    ///
    /// #### Returns
    ///
    /// the UIID
    public String getUIIDLine4() {
        return forthRow.getUIID();
    }

    /// Sets the UIID of the row
    ///
    /// #### Parameters
    ///
    /// - `t`: UIID to set
    public void setUIIDLine4(String t) {
        forthRow.setUIID(t);
    }

    /// Returns the icon image
    ///
    /// #### Returns
    ///
    /// the image instance
    @Override
    public Image getIcon() {
        return icon.getIcon();
    }

    /// Sets the icon
    ///
    /// #### Parameters
    ///
    /// - `i`: the icon
    @Override
    public void setIcon(Image i) {
        icon.setIcon(i);
        updateGap();
    }

    /// Returns the emblem image
    ///
    /// #### Returns
    ///
    /// the image instance
    public Image getEmblem() {
        return emblem.getIcon();
    }

    /// Sets the emblem
    ///
    /// #### Parameters
    ///
    /// - `i`: the icon
    public void setEmblem(Image i) {
        emblem.setIcon(i);
    }

    /// Returns the icon position based on border layout constraints
    ///
    /// #### Returns
    ///
    /// position either North/South/East/West
    public String getIconPosition() {
        return (String) getLayout().getComponentConstraint(icon.getParent());
    }

    /// Sets the icon position based on border layout constraints
    ///
    /// #### Parameters
    ///
    /// - `t`: position either North/South/East/West
    public void setIconPosition(String t) {
        String ip = getEmblemPosition();
        if (ip != null && ip.equals(t)) {
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

    /// Returns the emblem position based on border layout constraints
    ///
    /// #### Returns
    ///
    /// position either North/South/East/West
    public String getEmblemPosition() {
        return (String) getLayout().getComponentConstraint(emblem.getParent());
    }

    /// Sets the emblem position based on border layout constraints
    ///
    /// #### Parameters
    ///
    /// - `t`: position either North/South/East/West
    public void setEmblemPosition(String t) {
        String ip = getIconPosition();
        if (ip != null && ip.equals(t)) {
            String ep = getEmblemPosition();
            removeComponent(emblem.getParent());
            setIconPosition(ep);
        } else {
            removeComponent(emblem.getParent());
        }
        addComponent(t, emblem.getParent());
        revalidateLater();
    }

    /// Returns the name of the icon
    ///
    /// #### Returns
    ///
    /// the name
    public String getIconName() {
        return icon.getName();
    }

    /// Sets the name of the icon (important for use in generic renderers
    ///
    /// #### Parameters
    ///
    /// - `t`: name to set
    public void setIconName(String t) {
        icon.setName(t);
    }

    /// Returns the UIID of the Icon
    ///
    /// #### Returns
    ///
    /// the UIID
    @Override
    public String getIconUIID() {
        return icon.getUIID();
    }

    /// Sets the UIID of the icon
    ///
    /// #### Parameters
    ///
    /// - `t`: UIID to set
    @Override
    public void setIconUIID(String t) {
        icon.setUIID(t);
    }

    /// Returns the name of the emblem
    ///
    /// #### Returns
    ///
    /// the name
    public String getEmblemName() {
        return emblem.getName();
    }

    /// Sets the name of the emblem (important for use in generic renderers
    ///
    /// #### Parameters
    ///
    /// - `t`: name to set
    public void setEmblemName(String t) {
        emblem.setName(t);
    }

    /// Returns the UIID of the Emblem
    ///
    /// #### Returns
    ///
    /// the UIID
    public String getEmblemUIID() {
        return emblem.getUIID();
    }

    /// Sets the UIID of the emblem
    ///
    /// #### Parameters
    ///
    /// - `t`: UIID to set
    public void setEmblemUIID(String t) {
        emblem.setUIID(t);
    }

    /// Gets all text in multibutton in a single string delimited by "\n" character.
    ///
    /// #### Returns
    ///
    /// String with textLine1 to textLine4 delimited by "\n"
    ///
    /// #### Since
    ///
    /// 8.0
    public String getTextLines() {
        return getTextLine1() + "\n" + getTextLine2() + "\n" + getTextLine3() + "\n" + getTextLine4();
    }

    /// Sets textLine1, textLine2, textLine3, and textLine4 in single method with single string
    /// using "\n" as a delimiter.
    ///
    /// #### Parameters
    ///
    /// - `text`: The text to set.
    ///
    /// #### Since
    ///
    /// 8.0
    public void setTextLines(String text) {
        //String currTextVal = btn.getText();
        String newTextVal0 = text;
        int line = 0;
        StringTokenizer strtok = new StringTokenizer(newTextVal0, "\n");
        while (strtok.hasMoreTokens()) {
            line++;
            String currTextVal;
            switch (line) {
                case 1:
                    currTextVal = getTextLine1();
                    break;
                case 2:
                    currTextVal = getTextLine2();
                    break;
                case 3:
                    currTextVal = getTextLine3();
                    break;
                case 4:
                    currTextVal = getTextLine4();
                    break;
                default:
                    currTextVal = getText();
            }
            String newTextVal = strtok.nextToken().trim();
            if (!com.codename1.compat.java.util.Objects.equals(currTextVal, newTextVal)) {
                switch (line) {
                    case 1:
                        setTextLine1(newTextVal);
                        break;
                    case 2:
                        setTextLine2(newTextVal);
                        break;
                    case 3:
                        setTextLine3(newTextVal);
                        break;
                    case 4:
                        setTextLine4(newTextVal);
                        break;
                    default:
                        setText(newTextVal);

                }
            }
        }
        while (line < 4) {
            line++;
            String currTextVal;
            switch (line) {
                case 1:
                    currTextVal = getTextLine1();
                    break;
                case 2:
                    currTextVal = getTextLine2();
                    break;
                case 3:
                    currTextVal = getTextLine3();
                    break;
                case 4:
                    currTextVal = getTextLine4();
                    break;
                default:
                    currTextVal = getText();
            }
            if (!com.codename1.compat.java.util.Objects.equals(currTextVal, "")) {
                switch (line) {
                    case 1:
                        setTextLine1("");
                        break;
                    case 2:
                        setTextLine2("");
                        break;
                    case 3:
                        setTextLine3("");
                        break;
                    case 4:
                        setTextLine4("");
                        break;
                    default:
                        setText("");
                        break;

                }

            }
        }
    }

    /// Returns the line 1 text
    /// {@inheritDoc}
    @Override
    public String getText() {
        return getTextLine1();
    }

    /// Sets the line 1 text
    /// {@inheritDoc}
    @Override
    public void setText(String text) {
        setTextLine1(text);
    }

    /// {@inheritDoc}
    @Override
    public String[] getPropertyNames() {
        return new String[]{
                "line1", "line2", "line3", "line4", "name1", "name2", "name3", "name4",
                "uiid1", "uiid2", "uiid3", "uiid4", "icon", "iconName", "iconUiid", "iconPosition",
                "emblem", "emblemName", "emblemUiid", "emblemPosition", "horizontalLayout",
                "invertFirstTwoEntries", "checkBox", "radioButton", "group", "selected",
                "maskName"
        };
    }

    /// {@inheritDoc}
    @Override
    public Class[] getPropertyTypes() {
        return new Class[]{
                String.class, // line1
                String.class, // line2
                String.class, // line3
                String.class, // line4
                String.class, // name1
                String.class, // name2
                String.class, // name3
                String.class, // name4
                String.class, // uiid1
                String.class, // uiid2
                String.class, // uiid3
                String.class, // uiid4
                Image.class, // icon
                String.class, // iconName
                String.class, // iconUiid
                String.class, // iconPosition
                Image.class, // emblem
                String.class, // emblemName
                String.class, // emblemUiid
                String.class, // emblemPosition
                Boolean.class,
                Boolean.class,
                Boolean.class,
                Boolean.class,
                String.class, // group
                Boolean.class, // selected
                String.class
        };
    }

    /// {@inheritDoc}
    @Override
    public Object getPropertyValue(String name) {
        if ("line1".equals(name)) {
            return getTextLine1();
        }
        if ("line2".equals(name)) {
            return getTextLine2();
        }
        if ("line3".equals(name)) {
            return getTextLine3();
        }
        if ("line4".equals(name)) {
            return getTextLine4();
        }
        if ("name1".equals(name)) {
            return getNameLine1();
        }
        if ("name2".equals(name)) {
            return getNameLine2();
        }
        if ("name3".equals(name)) {
            return getNameLine3();
        }
        if ("name4".equals(name)) {
            return getNameLine4();
        }
        if ("uiid1".equals(name)) {
            return getUIIDLine1();
        }
        if ("uiid2".equals(name)) {
            return getUIIDLine2();
        }
        if ("uiid3".equals(name)) {
            return getUIIDLine3();
        }
        if ("uiid4".equals(name)) {
            return getUIIDLine4();
        }
        if ("icon".equals(name)) {
            return getIcon();
        }
        if ("iconName".equals(name)) {
            return getIconName();
        }
        if ("iconUiid".equals(name)) {
            return getIconUIID();
        }
        if ("iconPosition".equals(name)) {
            return getIconPosition();
        }
        if ("emblem".equals(name)) {
            return getEmblem();
        }
        if ("emblemName".equals(name)) {
            return getEmblemName();
        }
        if ("emblemUiid".equals(name)) {
            return getEmblemUIID();
        }
        if ("emblemPosition".equals(name)) {
            return getEmblemPosition();
        }
        if ("horizontalLayout".equals(name)) {
            if (isHorizontalLayout()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if ("invertFirstTwoEntries".equals(name)) {
            if (isInvertFirstTwoEntries()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if ("checkBox".equals(name)) {
            if (isCheckBox()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if ("radioButton".equals(name)) {
            if (isRadioButton()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if ("group".equals(name)) {
            return getGroup();
        }
        if ("selected".equals(name)) {
            if (isSelected()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if ("maskName".equals(name)) {
            return getMaskName();
        }
        return null;
    }

    /// {@inheritDoc}
    @Override
    public String setPropertyValue(String name, Object value) {
        if ("line1".equals(name)) {
            setTextLine1((String) value);
            return null;
        }
        if ("line2".equals(name)) {
            setTextLine2((String) value);
            return null;
        }
        if ("line3".equals(name)) {
            setTextLine3((String) value);
            return null;
        }
        if ("line4".equals(name)) {
            setTextLine4((String) value);
            return null;
        }
        if ("name1".equals(name)) {
            setNameLine1((String) value);
            return null;
        }
        if ("name2".equals(name)) {
            setNameLine2((String) value);
            return null;
        }
        if ("name3".equals(name)) {
            setNameLine3((String) value);
            return null;
        }
        if ("name4".equals(name)) {
            setNameLine4((String) value);
            return null;
        }
        if ("uiid1".equals(name)) {
            setUIIDLine1((String) value);
            return null;
        }
        if ("uiid2".equals(name)) {
            setUIIDLine2((String) value);
            return null;
        }
        if ("uiid3".equals(name)) {
            setUIIDLine3((String) value);
            return null;
        }
        if ("uiid4".equals(name)) {
            setUIIDLine4((String) value);
            return null;
        }
        if ("icon".equals(name)) {
            setIcon((Image) value);
            return null;
        }
        if ("iconUiid".equals(name)) {
            setIconUIID((String) value);
            return null;
        }
        if ("iconName".equals(name)) {
            setIconName((String) value);
            return null;
        }
        if ("iconPosition".equals(name)) {
            setIconPosition((String) value);
            return null;
        }
        if ("emblem".equals(name)) {
            setEmblem((Image) value);
            return null;
        }
        if ("emblemUiid".equals(name)) {
            setEmblemUIID((String) value);
            return null;
        }
        if ("emblemName".equals(name)) {
            setEmblemName((String) value);
            return null;
        }
        if ("emblemPosition".equals(name)) {
            setEmblemPosition((String) value);
            return null;
        }
        if ("horizontalLayout".equals(name)) {
            setHorizontalLayout(((Boolean) value).booleanValue());
            return null;
        }
        if ("invertFirstTwoEntries".equals(name)) {
            setInvertFirstTwoEntries(((Boolean) value).booleanValue());
            return null;
        }
        if ("checkBox".equals(name)) {
            setCheckBox(((Boolean) value).booleanValue());
            return null;
        }
        if ("radioButton".equals(name)) {
            setRadioButton(((Boolean) value).booleanValue());
            return null;
        }
        if ("group".equals(name)) {
            setGroup((String) value);
            return null;
        }
        if ("selected".equals(name)) {
            setSelected(((Boolean) value).booleanValue());
            return null;
        }
        if ("maskName".equals(name)) {
            setMaskName((String) value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /// Indicates the group for the radio button
    ///
    /// #### Returns
    ///
    /// the group
    public String getGroup() {
        return group;
    }

    /// Indicates the group for the radio button
    ///
    /// #### Parameters
    ///
    /// - `group`: the group to set
    public void setGroup(String group) {
        this.group = group;
        if (emblem instanceof RadioButton) {
            ((RadioButton) emblem).setGroup(group);
        }
    }

    /// Sets the button group for a radio button mode multibutton
    ///
    /// #### Parameters
    ///
    /// - `bg`: the button group
    public void setGroup(ButtonGroup bg) {
        bg.add((RadioButton) emblem);
    }

    /// Set the mask name for the icon
    ///
    /// #### Returns
    ///
    /// the maskName
    public String getMaskName() {
        return icon.getMaskName();
    }

    /// The mask name for the icon
    ///
    /// #### Parameters
    ///
    /// - `maskName`: the maskName to set
    public void setMaskName(String maskName) {
        icon.setMaskName(maskName);
    }

    /// Indicates if text should be localized when set to the component, by default
    /// all text is localized so this allows disabling automatic localization for
    /// a specific component.
    ///
    /// #### Returns
    ///
    /// the shouldLocalize value
    public boolean isShouldLocalize() {
        return firstRow.isShouldLocalize();
    }

    /// Indicates if text should be localized when set to the component, by default
    /// all text is localized so this allows disabling automatic localization for
    /// a specific component.
    ///
    /// #### Parameters
    ///
    /// - `shouldLocalize`: the shouldLocalize to set
    public void setShouldLocalize(boolean shouldLocalize) {
        firstRow.setShouldLocalize(shouldLocalize);
        secondRow.setShouldLocalize(shouldLocalize);
        thirdRow.setShouldLocalize(shouldLocalize);
        forthRow.setShouldLocalize(shouldLocalize);
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
    @Override
    public int getGap() {
        return gap;
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
    @Override
    public void setGap(int gap) {
        if (gap != this.gap) {
            this.gap = gap;
            updateGap();
        }
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
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

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
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


    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
    @Override
    public Component getIconStyleComponent() {
        return icon;
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 8.0
    @Override
    public void setMaterialIcon(char c, float size) {
        icon.setMaterialIcon(c, size);
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 8.0
    @Override
    public void setFontIcon(Font font, char c, float size) {
        icon.setFontIcon(font, c, size);
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
    @Override
    public Image getRolloverIcon() {
        return icon.getRolloverIcon();
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
    @Override
    public void setRolloverIcon(Image arg0) {
        icon.setRolloverIcon(arg0);
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
    @Override
    public Image getPressedIcon() {
        return icon.getPressedIcon();
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
    @Override
    public void setPressedIcon(Image arg0) {
        icon.setPressedIcon(arg0);
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
    @Override
    public Image getDisabledIcon() {
        return icon.getDisabledIcon();
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
    @Override
    public void setDisabledIcon(Image arg0) {
        icon.setDisabledIcon(arg0);
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
    @Override
    public Image getRolloverPressedIcon() {
        return icon.getRolloverPressedIcon();
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
    @Override
    public void setRolloverPressedIcon(Image icn) {
        icon.setRolloverPressedIcon(icn);
    }

    /// {@inheritDoc }
    ///
    /// #### Since
    ///
    /// 7.0
    @Override
    public Image getIconFromState() {
        return icon.getIconFromState();
    }

    /// Gets the text to be used in a badge on this label.
    ///
    /// #### Returns
    ///
    /// the badge text to be used on this label.  May return if no text is set.
    ///
    /// #### Since
    ///
    /// 8.0
    ///
    /// #### See also
    ///
    /// - #setBadgeText(java.lang.String)
    ///
    /// - #setBadgeUIID(java.lang.String)
    ///
    /// - #getBadgeStyleComponent()
    public String getBadgeText() {
        return icon.getBadgeText();
    }

    /// Sets the badge text to be used on this label.  Badges are rendered in the
    /// upper right corner of the label inside round border.  The style of the badge can be
    /// configured using `#setBadgeUIID(java.lang.String)`, but the default style uses
    /// the "Badge" UIID, which, by default, uses white text on a red round border background.
    ///
    /// #### Parameters
    ///
    /// - `badgeText`: @param badgeText The text to include in the badge.   null or empty strings will result in the
    ///                  badge not being rendered.
    ///
    /// #### Since
    ///
    /// 8.0
    ///
    /// #### See also
    ///
    /// - #getBadgeText()
    ///
    /// - #getBadgeStyleComponent()
    ///
    /// - #setBadgeUIID(java.lang.String)
    public void setBadgeText(String badgeText) {
        icon.setBadgeText(badgeText);
    }

    /// Sets the style that should be used for rendering badges.  By default it will use
    /// the "Badge" UIID, which rendered 1.5mm white text on a red round border.
    ///
    /// #### Parameters
    ///
    /// - `badgeUIID`: The UIID to use for the badge.
    ///
    /// #### Since
    ///
    /// 8.0
    ///
    /// #### See also
    ///
    /// - #setBadgeText(java.lang.String)
    ///
    /// - #getBadgeStyleComponent()
    public void setBadgeUIID(String badgeUIID) {
        icon.setBadgeUIID(badgeUIID);
    }

    /// Gets a component that can be used for the style of the badge.
    ///
    /// #### Returns
    ///
    /// The component whose style can be used to style the badge.  May return null if none set.
    ///
    /// #### Since
    ///
    /// 8.0
    ///
    /// #### See also
    ///
    /// - #setBadgeText(java.lang.String)
    ///
    /// - #setBadgeUIID(java.lang.String)
    ///
    /// - #getBadgeText()
    public Component getBadgeStyleComponent() {
        return icon.getBadgeStyleComponent();
    }
}
