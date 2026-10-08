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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.ItemEvent;
import com.codename1.desktopcompat.java.awt.event.ItemListener;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.javax.swing.event.EventListenerList;

import java.util.EventListener;

/// The default state holder of a button.
///
/// Every change of state fires a `ChangeEvent`, a change of selection also
/// fires an `ItemEvent`, and releasing the button while it is armed fires an
/// `ActionEvent`. The modifiers of the action event are always zero, because
/// there is no current event to take them from. A model in a `ButtonGroup`
/// lets the group decide whether it can be selected.
public class DefaultButtonModel implements ButtonModel {

    public static final int ARMED = 1 << 0;

    public static final int SELECTED = 1 << 1;

    public static final int PRESSED = 1 << 2;

    public static final int ENABLED = 1 << 3;

    public static final int ROLLOVER = 1 << 4;

    /// The bit set of the state constants.
    protected int stateMask;

    protected String actionCommand;

    protected ButtonGroup group;

    protected int mnemonic;

    protected ChangeEvent changeEvent;

    protected EventListenerList listenerList = new EventListenerList();

    public DefaultButtonModel() {
        stateMask = 0;
        mnemonic = 0;
        actionCommand = null;
        setEnabled(true);
    }

    public void setActionCommand(String actionCommand) {
        this.actionCommand = actionCommand;
    }

    public String getActionCommand() {
        return actionCommand;
    }

    public boolean isArmed() {
        return (stateMask & ARMED) != 0;
    }

    public boolean isSelected() {
        return (stateMask & SELECTED) != 0;
    }

    public boolean isEnabled() {
        return (stateMask & ENABLED) != 0;
    }

    public boolean isPressed() {
        return (stateMask & PRESSED) != 0;
    }

    public boolean isRollover() {
        return (stateMask & ROLLOVER) != 0;
    }

    public void setArmed(boolean b) {
        if (isArmed() == b || !isEnabled()) {
            return;
        }
        if (b) {
            stateMask |= ARMED;
        } else {
            stateMask &= ~ARMED;
        }
        fireStateChanged();
    }

    public void setEnabled(boolean b) {
        if (isEnabled() == b) {
            return;
        }
        if (b) {
            stateMask |= ENABLED;
        } else {
            stateMask &= ~(ENABLED | ARMED | PRESSED);
        }
        fireStateChanged();
    }

    public void setSelected(boolean b) {
        boolean selected = b;
        if (group != null) {
            group.setSelected(this, selected);
            selected = group.isSelected(this);
        }
        if (isSelected() == selected) {
            return;
        }
        if (selected) {
            stateMask |= SELECTED;
        } else {
            stateMask &= ~SELECTED;
        }
        fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                selected ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
        fireStateChanged();
    }

    public void setPressed(boolean b) {
        if (isPressed() == b || !isEnabled()) {
            return;
        }
        if (b) {
            stateMask |= PRESSED;
        } else {
            stateMask &= ~PRESSED;
        }
        if (!isPressed() && isArmed()) {
            fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getActionCommand(),
                    System.currentTimeMillis(), 0));
        }
        fireStateChanged();
    }

    public void setRollover(boolean b) {
        if (isRollover() == b || !isEnabled()) {
            return;
        }
        if (b) {
            stateMask |= ROLLOVER;
        } else {
            stateMask &= ~ROLLOVER;
        }
        fireStateChanged();
    }

    public void setMnemonic(int key) {
        mnemonic = key;
        fireStateChanged();
    }

    public int getMnemonic() {
        return mnemonic;
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    protected void fireActionPerformed(ActionEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    public void addItemListener(ItemListener l) {
        listenerList.add(ItemListener.class, l);
    }

    public void removeItemListener(ItemListener l) {
        listenerList.remove(ItemListener.class, l);
    }

    public ItemListener[] getItemListeners() {
        return listenerList.getListeners(ItemListener.class);
    }

    protected void fireItemStateChanged(ItemEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ItemListener.class) {
                ((ItemListener) listeners[i + 1]).itemStateChanged(e);
            }
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    public Object[] getSelectedObjects() {
        return null;
    }

    public void setGroup(ButtonGroup group) {
        this.group = group;
    }

    public ButtonGroup getGroup() {
        return group;
    }
}
