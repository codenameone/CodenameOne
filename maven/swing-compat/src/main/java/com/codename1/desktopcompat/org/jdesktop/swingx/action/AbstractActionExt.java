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
package com.codename1.desktopcompat.org.jdesktop.swingx.action;

import com.codename1.desktopcompat.java.awt.event.ItemEvent;
import com.codename1.desktopcompat.java.awt.event.ItemListener;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.AbstractAction;
import com.codename1.desktopcompat.javax.swing.Action;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.KeyStroke;

/// An action with typed accessors for the standard values, a group, and a
/// selected state for actions that are toggles.
///
/// The copy constructor copies the values this class has
/// accessors for; a device cannot enumerate more than
/// [AbstractAction#getKeys()] offers, which it also copies.
public abstract class AbstractActionExt extends AbstractAction implements ItemListener {

    public static final String LARGE_ICON = "SwingLargeIconKey";
    public static final String GROUP = "__Group__";
    public static final String IS_STATE = "__State__";

    public AbstractActionExt() {
        this((String) null);
    }

    public AbstractActionExt(AbstractActionExt action) {
        Object[] keys = action.getKeys();
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] instanceof String) {
                    String key = (String) keys[i];
                    putValue(key, action.getValue(key));
                }
            }
        }
        setEnabled(action.isEnabled());
    }

    public AbstractActionExt(String name) {
        this(name, (Icon) null);
    }

    public AbstractActionExt(String name, Icon icon) {
        super(name, icon);
    }

    public AbstractActionExt(String name, String command) {
        this(name);
        setActionCommand(command);
    }

    public AbstractActionExt(String name, String command, Icon icon) {
        super(name, icon);
        setActionCommand(command);
    }

    public String getShortDescription() {
        return cn1String(Action.SHORT_DESCRIPTION);
    }

    public void setShortDescription(String desc) {
        putValue(Action.SHORT_DESCRIPTION, desc);
        if (desc != null && getLongDescription() == null) {
            setLongDescription(desc);
        }
    }

    public String getLongDescription() {
        return cn1String(Action.LONG_DESCRIPTION);
    }

    public void setLongDescription(String desc) {
        putValue(Action.LONG_DESCRIPTION, desc);
        if (desc != null && getShortDescription() == null) {
            setShortDescription(desc);
        }
    }

    public Icon getSmallIcon() {
        Object v = getValue(SMALL_ICON);
        return v instanceof Icon ? (Icon) v : null;
    }

    public void setSmallIcon(Icon icon) {
        putValue(SMALL_ICON, icon);
    }

    public Icon getLargeIcon() {
        Object v = getValue(LARGE_ICON);
        return v instanceof Icon ? (Icon) v : null;
    }

    public void setLargeIcon(Icon icon) {
        putValue(LARGE_ICON, icon);
    }

    public void setName(String name) {
        putValue(Action.NAME, name);
    }

    public String getName() {
        return cn1String(Action.NAME);
    }

    /// Takes the first character of `mnemonic` as the mnemonic.
    public void setMnemonic(String mnemonic) {
        if (mnemonic != null && mnemonic.length() > 0) {
            putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnemonic.charAt(0)));
        }
    }

    public void setMnemonic(int mnemonic) {
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnemonic));
    }

    /// The mnemonic, or 0 when there is none.
    public int getMnemonic() {
        Object v = getValue(Action.MNEMONIC_KEY);
        return v instanceof Integer ? ((Integer) v).intValue() : 0;
    }

    public void setActionCommand(String key) {
        putValue(Action.ACTION_COMMAND_KEY, key);
    }

    public String getActionCommand() {
        return cn1String(Action.ACTION_COMMAND_KEY);
    }

    /// The key stroke stored under [Action#ACCELERATOR_KEY], or null.
    public KeyStroke getAccelerator() {
        Object v = getValue(Action.ACCELERATOR_KEY);
        return v instanceof KeyStroke ? (KeyStroke) v : null;
    }

    public void setAccelerator(KeyStroke key) {
        putValue(Action.ACCELERATOR_KEY, key);
    }

    public void setGroup(Object group) {
        putValue(GROUP, group);
    }

    public Object getGroup() {
        return getValue(GROUP);
    }

    /// Removes every property change listener.
    public void dispose() {
        PropertyChangeListener[] ls = getPropertyChangeListeners();
        for (int i = 0; i < ls.length; i++) {
            removePropertyChangeListener(ls[i]);
        }
    }

    /// Whether the action is a toggle with a selected state.
    public boolean isStateAction() {
        return Boolean.TRUE.equals(getValue(IS_STATE));
    }

    public void setStateAction() {
        setStateAction(true);
    }

    public void setStateAction(boolean state) {
        putValue(IS_STATE, Boolean.valueOf(state));
    }

    public boolean isSelected() {
        return Boolean.TRUE.equals(getValue(SELECTED_KEY));
    }

    public void setSelected(boolean newValue) {
        putValue(SELECTED_KEY, Boolean.valueOf(newValue));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        sb.append(getClass().getName());
        sb.append(":name=").append(getName());
        sb.append(",command=").append(getActionCommand());
        sb.append(",enabled=").append(isEnabled());
        sb.append(']');
        return sb.toString();
    }

    /// Does nothing; a state action overrides this to follow the toggle
    /// it is attached to.
    @Override
    public void itemStateChanged(ItemEvent e) {
    }

    private String cn1String(String key) {
        Object v = getValue(key);
        return v instanceof String ? (String) v : null;
    }
}
