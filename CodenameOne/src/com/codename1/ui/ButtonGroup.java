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

import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionSource;

import java.util.ArrayList;

/// This class is used to create a multiple-exclusion scope for
/// `com.codename1.ui.RadioButton`.
/// Creating a set of `com.codename1.ui.RadioButton` components with the same `ButtonGroup` object
/// means that only one `com.codename1.ui.RadioButton` can be selected among those within
/// the specific `ButtonGroup`.
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
/// @author Nir Shabi
public class ButtonGroup implements ActionSource<ActionEvent> {


    private final ArrayList<RadioButton> buttons = new ArrayList<RadioButton>();
    private int selectedIndex = -1;

    /// Creates a new instance of ButtonsGroup
    public ButtonGroup() {
    }

    /// Adds all the radio buttons to the group
    ///
    /// #### Parameters
    ///
    /// - `rb`
    public ButtonGroup(RadioButton... rb) {
        addAll(rb);
    }

    /// Adds the RadioButtons to the group
    ///
    /// #### Parameters
    ///
    /// - `rb`: a RadioButtons to add
    public void addAll(RadioButton... rb) {
        for (RadioButton r : rb) {
            add(r);
        }
    }

    /// Adds a RadioButton to the group
    ///
    /// #### Parameters
    ///
    /// - `rb`: a RadioButton to add
    public void add(RadioButton rb) {
        if (rb == null) {
            return;
        }
        if (!buttons.contains(rb)) {
            buttons.add(rb);
            if (rb.isSelected()) {
                setSelected(buttons.indexOf(rb));
            }
            rb.setButtonGroup(this);
        }
    }

    /// removes a RadioButton from the group
    ///
    /// #### Parameters
    ///
    /// - `rb`: a RadioButton to remove
    public void remove(RadioButton rb) {
        if (rb == null) {
            return;
        }
        buttons.remove(rb);
        if (rb.isSelected()) {
            clearSelection();
        }
        rb.setButtonGroup(null);
    }

    /// Clears the selection such that none of the buttons in the ButtonGroup are selected.
    public void clearSelection() {
        if (selectedIndex != -1) {
            if (selectedIndex < buttons.size()) {
                buttons.get(selectedIndex).setSelected(false);
            }
            selectedIndex = -1;
        }

    }

    /// Returns the number of buttons in the group.
    ///
    /// #### Returns
    ///
    /// number of radio buttons in the group
    public int getButtonCount() {
        return buttons.size();
    }

    /// Returns whether a radio button in the group is selected.
    ///
    /// #### Returns
    ///
    /// true if a selection was made in the radio button group
    public boolean isSelected() {
        return selectedIndex != -1;
    }

    /// Return the index of the selected button within the group
    ///
    /// #### Returns
    ///
    /// the index of the selected button within the group
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /// Return the selected radio button within the group
    ///
    /// #### Returns
    ///
    /// the selected radio button within the group
    public RadioButton getSelected() {
        return getRadioButton(selectedIndex);
    }

    /// Selects the given radio button
    ///
    /// #### Parameters
    ///
    /// - `rb`: the radio button to set as selected
    public void setSelected(RadioButton rb) {
        if (rb != null) {
            int index = buttons.indexOf(rb);
            if (index < 0) {
                add(rb);
                index = buttons.indexOf(rb);
            }
            setSelected(index);
        } else {
            clearSelection();
        }
    }

    /// Sets the selected Radio button by index
    ///
    /// #### Parameters
    ///
    /// - `index`: the index of the radio button to mark as selected
    public void setSelected(int index) {
        if (index < 0 || index >= getButtonCount()) {
            throw new IllegalArgumentException("Index out of bounds");
        }

        if (selectedIndex == index) {
            return;
        }

        if (selectedIndex != -1) {
            //unselect last selected Radio button
            buttons.get(selectedIndex).setSelectedImpl(false);
        }
        buttons.get(index).setSelectedImpl(true);
        selectedIndex = index;
    }

    /// Returns the radio button at the given group index
    ///
    /// #### Parameters
    ///
    /// - `index`: offset within the group starting with 0 and no larger than getButtonCount()
    ///
    /// #### Returns
    ///
    /// the radio button instance
    public RadioButton getRadioButton(int index) {
        if (index >= 0 && index < getButtonCount()) {
            return buttons.get(index);
        }
        return null;
    }

    /// Adds an action listener to all the buttons in the group
    ///
    /// #### Parameters
    ///
    /// - `al`: the listener
    @Override
    public void addActionListener(ActionListener<ActionEvent> al) {
        for (RadioButton rb : buttons) {
            rb.addActionListener(al);
        }
    }

    /// Removes an action listener from all the buttons in the group
    ///
    /// #### Parameters
    ///
    /// - `al`: the listener
    @Override
    public void removeActionListener(ActionListener<ActionEvent> al) {
        for (RadioButton rb : buttons) {
            rb.removeActionListener(al);
        }
    }
}
