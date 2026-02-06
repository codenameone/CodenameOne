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
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;

/// RadioButton is a `Button` that maintains a selection state exclusively
/// within a specific `ButtonGroup`. Check out `com.codename1.ui.CheckBox` for
/// a looser selection approach. Both components support a toggle button
/// mode using the `com.codename1.ui.Button#setToggle(boolean)` API.
///
/// ```java
/// CheckBox cb1 = new CheckBox("CheckBox No Icon");
/// cb1.setSelected(true);
/// CheckBox cb2 = new CheckBox("CheckBox With Icon", icon);
/// CheckBox cb3 = new CheckBox("CheckBox Opposite True", icon);
/// CheckBox cb4 = new CheckBox("CheckBox Opposite False", icon);
/// cb3.setOppositeSide(true);
/// cb4.setOppositeSide(false);
/// RadioButton rb1 = new RadioButton("Radio 1");
/// RadioButton rb2 = new RadioButton("Radio 2");
/// RadioButton rb3 = new RadioButton("Radio 3", icon);
/// new ButtonGroup(rb1, rb2, rb3);
/// rb2.setSelected(true);
/// hi.add(cb1).add(cb2).add(cb3).add(cb4).add(rb1).add(rb2).add(rb3);
/// ```
///
/// @author Chen Fishbein
public class RadioButton extends Button {

    private boolean unselectAllowed;

    private boolean selected;

    /// The group in which this button is a part
    private ButtonGroup group;

    private boolean oppositeSide;

    private EventDispatcher bindListeners;
    private EventDispatcher changeListeners;

    /// Constructs a radio with the given text
    ///
    /// #### Parameters
    ///
    /// - `text`: to display next to the button
    public RadioButton(String text) {
        this(text, null);
    }

    /// Creates an empty radio button
    public RadioButton() {
        this("");
    }

    /// Constructs a radio with the given icon
    ///
    /// #### Parameters
    ///
    /// - `icon`: icon to show next to the button
    public RadioButton(Image icon) {
        this("", icon);
    }

    /// Constructs a radio with the given text and icon
    ///
    /// #### Parameters
    ///
    /// - `text`: to display next to the button
    ///
    /// - `icon`: icon to show next to the button
    public RadioButton(String text, Image icon) {
        super(text, icon);
        setUIIDFinal("RadioButton");
    }

    /// Shorthand for creating the radio button, adding it to a group, setting the icon/text and making it into
    /// a toggle button
    ///
    /// #### Parameters
    ///
    /// - `text`: the text for the button
    ///
    /// - `icon`: the icon for the button
    ///
    /// - `bg`: the button group
    ///
    /// #### Returns
    ///
    /// a radio button
    public static RadioButton createToggle(String text, Image icon, ButtonGroup bg) {
        RadioButton rb = new RadioButton(text, icon);
        bg.add(rb);
        rb.setToggle(true);
        return rb;
    }

    /// Shorthand for creating the radio button, adding it to a group, setting the icon/text and making it into
    /// a toggle button
    ///
    /// #### Parameters
    ///
    /// - `text`: the text for the button
    ///
    /// - `bg`: the button group
    ///
    /// #### Returns
    ///
    /// a radio button
    public static RadioButton createToggle(String text, ButtonGroup bg) {
        return createToggle(text, null, bg);
    }

    /// Shorthand for creating the radio button, adding it to a group, setting the icon/text and making it into
    /// a toggle button
    ///
    /// #### Parameters
    ///
    /// - `icon`: the icon for the button
    ///
    /// - `bg`: the button group
    ///
    /// #### Returns
    ///
    /// a radio button
    public static RadioButton createToggle(Image icon, ButtonGroup bg) {
        return createToggle(null, icon, bg);
    }

    /// Shorthand for creating the radio button, adding it to a group, setting the command and making it into
    /// a toggle button
    ///
    /// #### Parameters
    ///
    /// - `cmd`: the command
    ///
    /// - `bg`: the button group
    ///
    /// #### Returns
    ///
    /// a radio button
    public static RadioButton createToggle(Command cmd, ButtonGroup bg) {
        RadioButton rb = new RadioButton(cmd.getCommandName(), cmd.getIcon());
        rb.setCommand(cmd);
        bg.add(rb);
        rb.setToggle(true);
        return rb;
    }

    /// {@inheritDoc}
    @Override
    protected void initLaf(UIManager uim) {
        super.initLaf(uim);
        Boolean v = getUIManager().isThemeConstant("radioOppositeSideBool");
        if (v != null) {
            oppositeSide = v.booleanValue();
        }

    }

    /// {@inheritDoc}
    @Override
    protected void fireActionEvent(int x, int y) {
        super.fireActionEvent(x, y);
        if (bindListeners != null) {
            if (isSelected()) {
                bindListeners.fireBindTargetChange(this, "selected", Boolean.TRUE, Boolean.FALSE);
            } else {
                bindListeners.fireBindTargetChange(this, "selected", Boolean.FALSE, Boolean.TRUE);
            }
        }
    }


    /// {@inheritDoc}
    @Override
    public String toString() {
        return "Radio Button " + getText();
    }

    @Override
    int getAvaliableSpaceForText() {
        if (isToggle()) {
            return super.getAvaliableSpaceForText();
        }
        LookAndFeel l = getUIManager().getLookAndFeel();
        if (l instanceof DefaultLookAndFeel) {
            Image[] rButtonImages = ((DefaultLookAndFeel) l).getRadioButtonImages();
            if (rButtonImages != null) {
                int index = isSelected() ? 1 : 0;
                return super.getAvaliableSpaceForText() - rButtonImages[index].getWidth();
            }
        }
        return super.getAvaliableSpaceForText() - (getHeight() + getGap());
    }

    /// Returns true if the radio button is selected
    ///
    /// #### Returns
    ///
    /// true if the radio button is selected
    @Override
    public boolean isSelected() {
        return selected;
    }

    /// Selects the current radio button
    ///
    /// #### Parameters
    ///
    /// - `selected`: value for selection
    public void setSelected(boolean selected) {
        setSelectedImpl(selected);
        if (group != null && selected) {
            group.setSelected(this);
        }
    }

    void setSelectedImpl(boolean selected) {
        boolean changed = selected != this.selected;
        this.selected = selected;
        if (changed) {
            fireChangeEvent();
        }
        repaint();
    }

    /// Returns true if this RadioButton can be unselected
    ///
    /// #### Returns
    ///
    /// true to allow deselection of radio buttons
    public boolean isUnselectAllowed() {
        return unselectAllowed;
    }

    /// Allows unselecting a selected RadioButton.
    /// This is useful for when implementing a ButtonGroup that allows no selection or a single selection.,
    ///
    /// #### Parameters
    ///
    /// - `unselectAllowed`: true to allow deselection of a radio button, false for the default behavior
    public void setUnselectAllowed(boolean unselectAllowed) {
        this.unselectAllowed = unselectAllowed;
    }

    /// {@inheritDoc}
    @Override
    public void released(int x, int y) {
        // prevent the radio button from being "turned off" unless unselectAllowed
        if (!isSelected() || unselectAllowed) {
            setSelected(!isSelected());
        }
        super.released(x, y);
    }

    /// {@inheritDoc}
    @Override
    public void paint(Graphics g) {
        if (isToggle()) {
            getUIManager().getLookAndFeel().drawButton(g, this);
        } else {
            getUIManager().getLookAndFeel().drawRadioButton(g, this);
        }
    }

    /// {@inheritDoc}
    @Override
    protected Dimension calcPreferredSize() {
        return getUIManager().getLookAndFeel().getRadioButtonPreferredSize(this);
    }

    /// Returns the parent button group
    ///
    /// #### Returns
    ///
    /// the parent button group
    public ButtonGroup getButtonGroup() {
        return group;
    }

    /// Setting a new button group
    ///
    /// #### Parameters
    ///
    /// - `group`: a new button group
    void setButtonGroup(ButtonGroup group) {
        this.group = group;
    }

    /// {@inheritDoc}
    @Override
    void fireActionEvent() {
        if (group != null) {
            group.setSelected(this);
        }
        super.fireActionEvent();
    }

    @Override
    void initComponentImpl() {
        super.initComponentImpl();
        initNamedGroup();
    }

    private void initNamedGroup() {
        if (isInitialized()) {
            String s = getGroup();
            if (s != null) {
                Form f = getComponentForm();
                ButtonGroup b = (ButtonGroup) f.getClientProperty("$radio" + s);
                if (b == null) {
                    b = new ButtonGroup();
                    f.putClientProperty("$radio" + s, b);
                }
                b.add(this);
            }
        }
    }

    /// This is a helper method to ease the usage of button groups
    ///
    /// #### Returns
    ///
    /// the name of the group
    public String getGroup() {
        return (String) getClientProperty("$group");
    }

    /// This is a helper method to ease the usage of button groups
    ///
    /// #### Parameters
    ///
    /// - `groupName`: a name for the goup
    public void setGroup(String groupName) {
        putClientProperty("$group", groupName);
        initNamedGroup();
    }

    /// Places the radio box on the opposite side at the far end
    ///
    /// #### Returns
    ///
    /// the oppositeSide
    @Override
    public boolean isOppositeSide() {
        return oppositeSide;
    }

    /// Places the radio box on the opposite side at the far end
    ///
    /// #### Parameters
    ///
    /// - `oppositeSide`: the oppositeSide to set
    public void setOppositeSide(boolean oppositeSide) {
        this.oppositeSide = oppositeSide;
    }

    /// {@inheritDoc}
    @Override
    public String[] getBindablePropertyNames() {
        return new String[]{"selected"};
    }

    /// {@inheritDoc}
    @Override
    public Class[] getBindablePropertyTypes() {
        return new Class[]{Boolean.class};
    }

    /// {@inheritDoc}
    ///
    /// #### Deprecated
    ///
    /// uses the deprecated BindTarget interface
    @Override
    public void bindProperty(String prop, BindTarget target) {
        if ("selected".equals(prop)) {
            if (bindListeners == null) {
                bindListeners = new EventDispatcher();
            }
            bindListeners.addListener(target);
            return;
        }
        super.bindProperty(prop, target);
    }

    /// {@inheritDoc}
    ///
    /// #### Deprecated
    ///
    /// uses the deprecated BindTarget interface
    @Override
    public void unbindProperty(String prop, BindTarget target) {
        if ("selected".equals(prop)) {
            if (bindListeners == null) {
                return;
            }
            bindListeners.removeListener(target);
            if (!bindListeners.hasListeners()) {
                bindListeners = null;
            }
            return;
        }
        super.unbindProperty(prop, target);
    }

    /// {@inheritDoc}
    @Override
    public Object getBoundPropertyValue(String prop) {
        if ("selected".equals(prop)) {
            if (isSelected()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return super.getBoundPropertyValue(prop);
    }

    /// {@inheritDoc}
    @Override
    public void setBoundPropertyValue(String prop, Object value) {
        if ("selected".equals(prop)) {
            setSelected(value != null && ((Boolean) value).booleanValue());
            return;
        }
        super.setBoundPropertyValue(prop, value);
    }

    /// Adds a listener to be notified when the the checkbox's selected value changes.  The difference
    /// between a change listener and an action listener is that a change listener is fired
    /// whenever there is a change, but action events are only fired when the change is a result
    /// of the user clicking on the checkbox.
    ///
    /// #### Parameters
    ///
    /// - `l`: Listener to be notified when selected value changes.
    ///
    /// #### Since
    ///
    /// 6.0
    ///
    /// #### See also
    ///
    /// - #removeChangeListener(com.codename1.ui.events.ActionListener)
    public void addChangeListener(ActionListener l) {
        if (changeListeners == null) {
            changeListeners = new EventDispatcher();
        }
        changeListeners.addListener(l);
    }

    /// Removes a change change listener.
    ///
    /// #### Parameters
    ///
    /// - `l`
    ///
    /// #### Since
    ///
    /// 6.0
    ///
    /// #### See also
    ///
    /// - #addChangeListener(com.codename1.ui.events.ActionListener)
    public void removeChangeListeners(ActionListener l) {
        if (changeListeners != null) {
            changeListeners.removeListener(l);
        }
    }

    private void fireChangeEvent() {
        if (changeListeners != null) {
            ActionEvent evt = new ActionEvent(this, ActionEvent.Type.Change);
            changeListeners.fireActionEvent(evt);
        }
    }
}
