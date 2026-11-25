/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.ui.Container;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;

import java.util.ArrayList;
import java.util.List;

/// A text field that turns each entry into a removable `Chip`: a tag input,
/// a recipient field or a list of keywords.
///
/// ```java
/// ChipInput tags = new ChipInput();
/// tags.setHint("Add a tag");
/// tags.addValue("design");
/// tags.addValue("mobile");
/// tags.addActionListener(e -> Log.p("Tags: " + tags.getValues()));
/// form.add(tags);
/// ```
///
/// An entry becomes a chip when the user presses Done or types a separator
/// (a comma by default, see `#setSeparators(String)`). Empty entries and, unless
/// `#setAllowDuplicates(boolean)` says otherwise, repeated ones are ignored.
/// Each chip has a close button that removes it. Action listeners fire after
/// every addition and removal.
///
/// UIIDs: `ChipInput` for the container and `ChipInputField` for the text
/// field, falling back to `TextField`; the chips use the `Chip` UIIDs.
public class ChipInput extends Container {
    private final TextField field = new TextField();
    private final List<String> values = new ArrayList<String>();
    private final EventDispatcher listeners = new EventDispatcher();
    private String separators = ",";
    private boolean allowDuplicates;

    /// An empty tag input.
    public ChipInput() {
        super(new FlowLayout());
        setUIID("ChipInput");
        field.setUIID(getUIManager().isThemeStyleDefined("ChipInputField") ? "ChipInputField" : "TextField");
        field.setColumns(8);
        field.setDoneListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                commit();
            }
        });
        field.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                splitOnSeparator();
            }
        });
        add(field);
    }

    @Override
    protected void initUnselectedStyle(Style unselectedStyle) {
        super.initUnselectedStyle(unselectedStyle);
        if (unselectedStyle != null && !getUIManager().isThemeStyleDefined(getUIID())) {
            unselectedStyle.setBgTransparency(0);
        }
    }

    /// The text field the user types into.
    ///
    /// #### Returns
    ///
    /// the field
    public TextField getTextField() {
        return field;
    }

    /// Sets the hint shown in the empty field.
    ///
    /// #### Parameters
    ///
    /// - `hint`: the hint
    public void setHint(String hint) {
        field.setHint(hint);
    }

    /// The characters that end an entry as it is typed.
    ///
    /// #### Returns
    ///
    /// the separators, "," by default
    public String getSeparators() {
        return separators;
    }

    /// Sets the characters that end an entry as it is typed, such as ", ;" to
    /// split on commas, spaces and semicolons. Done always ends one.
    ///
    /// #### Parameters
    ///
    /// - `separators`: the separator characters, or "" for Done only
    public void setSeparators(String separators) {
        this.separators = separators == null ? "" : separators;
    }

    /// Whether the same value can be added twice.
    ///
    /// #### Returns
    ///
    /// false by default
    public boolean isAllowDuplicates() {
        return allowDuplicates;
    }

    /// Allows or refuses repeated values. Values compare ignoring case.
    ///
    /// #### Parameters
    ///
    /// - `allowDuplicates`: true to allow them
    public void setAllowDuplicates(boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
    }

    /// The values, in the order they were added.
    ///
    /// #### Returns
    ///
    /// a copy of the values
    public List<String> getValues() {
        return new ArrayList<String>(values);
    }

    /// Adds a value as a chip, as if the user had typed it.
    ///
    /// #### Parameters
    ///
    /// - `value`: the value; surrounding white space is dropped
    ///
    /// #### Returns
    ///
    /// true if a chip was added, false for an empty or refused duplicate value
    public boolean addValue(String value) {
        String v = value == null ? "" : value.trim();
        if (v.length() == 0 || (!allowDuplicates && indexOf(v) >= 0)) {
            return false;
        }
        values.add(v);
        final Chip chip = new Chip(v).setRemovable(true);
        chip.addRemoveListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                removeChip(chip);
            }
        });
        addComponent(getComponentIndex(field), chip);
        revalidateLater();
        fire();
        return true;
    }

    /// Removes a value and its chip.
    ///
    /// #### Parameters
    ///
    /// - `value`: the value, compared ignoring case
    ///
    /// #### Returns
    ///
    /// true if it was there
    public boolean removeValue(String value) {
        int i = indexOf(value);
        if (i < 0) {
            return false;
        }
        removeChipAt(i);
        return true;
    }

    /// Removes every value.
    public void clearValues() {
        while (!values.isEmpty()) {
            values.remove(0);
            removeComponent(getComponentAt(0));
        }
        revalidateLater();
        fire();
    }

    /// Adds a listener called after every addition and removal.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addActionListener(ActionListener l) {
        listeners.addListener(l);
    }

    /// Removes a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeActionListener(ActionListener l) {
        listeners.removeListener(l);
    }

    /// Turns the text in the field into a chip, as Done does.
    public void commit() {
        String t = field.getText();
        if (t != null && t.trim().length() > 0) {
            addValue(t);
            field.setText("");
        }
    }

    private void removeChip(Chip chip) {
        int i = getComponentIndex(chip);
        if (i >= 0 && i < values.size()) {
            removeChipAt(i);
        }
    }

    private void removeChipAt(int i) {
        values.remove(i);
        removeComponent(getComponentAt(i));
        revalidateLater();
        fire();
    }

    private int indexOf(String v) {
        if (v == null) {
            return -1;
        }
        String t = v.trim();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equalsIgnoreCase(t)) {
                return i;
            }
        }
        return -1;
    }

    private void splitOnSeparator() {
        if (separators.length() == 0) {
            return;
        }
        String t = field.getText();
        if (t == null) {
            return;
        }
        int start = 0;
        boolean split = false;
        for (int i = 0; i < t.length(); i++) {
            if (separators.indexOf(t.charAt(i)) >= 0) {
                addValue(t.substring(start, i));
                start = i + 1;
                split = true;
            }
        }
        if (split) {
            field.setText(t.substring(start));
        }
    }

    private void fire() {
        listeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Change));
    }
}
