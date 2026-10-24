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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.event.ItemEvent;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.accessibility.Accessible;
import com.codename1.desktopcompat.rt.MenuBridge;
import com.codename1.desktopcompat.rt.MenuItemPeer;

/// An item of a menu.
///
/// In a popup menu it is a Codename One button without a frame; in the
/// menu bar of a frame it is a Codename One command, see
/// [com.codename1.desktopcompat.rt.MenuBridge]. Either way choosing it
/// closes the open popups and runs `doClick()`.
///
/// An item made from an action follows it: the action's name, icon,
/// enabled state and accelerator are the item's, and stay so when the
/// action changes. The accelerator works while the item is in the menu
/// bar of the focused window. The mnemonic is recorded only.
public class JMenuItem extends AbstractButton implements Accessible, MenuElement {

    private KeyStroke accelerator;
    private PropertyChangeListener actionFollower;
    private long lastChosen;

    public JMenuItem() {
        this(null, (Icon) null);
    }

    public JMenuItem(Icon icon) {
        this(null, icon);
    }

    public JMenuItem(String text) {
        this(text, (Icon) null);
    }

    public JMenuItem(Action a) {
        this();
        setAction(a);
    }

    public JMenuItem(String text, Icon icon) {
        setModel(new DefaultButtonModel());
        init(text, icon);
        outOfTheFocus();
    }

    public JMenuItem(String text, int mnemonic) {
        setModel(new DefaultButtonModel());
        init(text, null);
        setMnemonic(mnemonic);
        outOfTheFocus();
    }

    /// A menu and its items are not part of the keyboard focus, as in
    /// Swing, where a menu item is made not focusable: the focus stays
    /// with the component that had it while a menu is open. The widget
    /// that draws an item would take it otherwise, and the window of an
    /// application opened with the last menu of its bar drawn as chosen,
    /// since the form gave its first focus to it.
    private void outOfTheFocus() {
        setFocusable(false);
    }

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new MenuItemPeer(this);
    }

    public KeyStroke getAccelerator() {
        return accelerator;
    }

    public void setAccelerator(KeyStroke keyStroke) {
        KeyStroke old = accelerator;
        accelerator = keyStroke;
        firePropertyChange("accelerator", old, keyStroke);
        MenuBridge.changed(this);
    }

    public boolean isArmed() {
        return getModel().isArmed();
    }

    public void setArmed(boolean b) {
        getModel().setArmed(b);
    }

    /// Closes the open popup menus, then clicks.
    @Override
    public void doClick(int pressTime) {
        MenuSelectionManager.defaultManager().clearSelectedPath();
        super.doClick(pressTime);
    }

    /// Chooses the item on behalf of a command or an accelerator.
    ///
    /// On a desktop port the same key press can arrive twice, once
    /// through the native menu's shortcut and once as a key event; two
    /// calls within a quarter of a second count as one.
    public void cn1Choose() {
        long now = System.currentTimeMillis();
        if (!isEnabled() || (now - lastChosen >= 0 && now - lastChosen < 250)) {
            return;
        }
        lastChosen = now;
        doClick();
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        MenuBridge.changed(this);
    }

    @Override
    public void setIcon(Icon defaultIcon) {
        super.setIcon(defaultIcon);
        MenuBridge.changed(this);
    }

    @Override
    public void setEnabled(boolean b) {
        boolean old = isEnabled();
        super.setEnabled(b);
        if (old != b) {
            MenuBridge.changed(this);
        }
    }

    @Override
    public void setVisible(boolean b) {
        boolean old = isVisible();
        super.setVisible(b);
        if (old != b) {
            MenuBridge.changed(this);
        }
    }

    @Override
    protected void fireItemStateChanged(ItemEvent event) {
        super.fireItemStateChanged(event);
        MenuBridge.changed(this);
    }

    /// Takes the action's name, icon, enabled state, command and
    /// accelerator, makes it an action listener, and follows its later
    /// changes.
    @Override
    public void setAction(Action a) {
        Action old = getAction();
        if (old != null && actionFollower != null) {
            old.removePropertyChangeListener(actionFollower);
        }
        super.setAction(a);
        if (a != null) {
            Object icon = a.getValue(Action.SMALL_ICON);
            if (icon instanceof Icon) {
                setIcon((Icon) icon);
            }
            Object ks = a.getValue(Action.ACCELERATOR_KEY);
            if (ks instanceof KeyStroke) {
                setAccelerator((KeyStroke) ks);
            }
            if (actionFollower == null) {
                actionFollower = new ActionFollower(this);
            }
            a.addPropertyChangeListener(actionFollower);
        }
    }

    @Override
    public void processMouseEvent(MouseEvent event, MenuElement[] path, MenuSelectionManager manager) {
    }

    @Override
    public void processKeyEvent(KeyEvent event, MenuElement[] path, MenuSelectionManager manager) {
    }

    @Override
    public void menuSelectionChanged(boolean isIncluded) {
        setArmed(isIncluded);
    }

    @Override
    public MenuElement[] getSubElements() {
        return new MenuElement[0];
    }

    @Override
    public Component getComponent() {
        return this;
    }

    /// Copies an action's changes to the item made from it.
    private static final class ActionFollower implements PropertyChangeListener {

        private final JMenuItem item;

        ActionFollower(JMenuItem item) {
            this.item = item;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String name = evt.getPropertyName();
            Object v = evt.getNewValue();
            if ("enabled".equals(name)) {
                item.setEnabled(Boolean.TRUE.equals(v));
            } else if (Action.NAME.equals(name)) {
                item.setText(v instanceof String ? (String) v : "");
            } else if (Action.SMALL_ICON.equals(name)) {
                item.setIcon(v instanceof Icon ? (Icon) v : null);
            } else if (Action.ACCELERATOR_KEY.equals(name)) {
                item.setAccelerator(v instanceof KeyStroke ? (KeyStroke) v : null);
            } else if (Action.ACTION_COMMAND_KEY.equals(name)) {
                item.setActionCommand(v instanceof String ? (String) v : null);
            }
        }
    }
}
