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
import com.codename1.desktopcompat.javax.swing.event.MenuEvent;
import com.codename1.desktopcompat.javax.swing.event.MenuListener;
import com.codename1.desktopcompat.rt.MenuBridge;
import java.util.ArrayList;

/// A menu: an item that opens a popup menu of further items.
///
/// Drawn in a window -- in a menu bar that is a component, or as a
/// submenu in a popup -- a click opens the popup below a top level menu
/// and beside a submenu, and a second click closes it. In the menu bar of
/// a frame the menu is not drawn: its items become commands under its
/// name.
public class JMenu extends JMenuItem implements MenuElement {

    private JPopupMenu popupMenu;
    private int delay;

    public JMenu() {
        this("");
    }

    public JMenu(String s) {
        super(s);
    }

    public JMenu(Action a) {
        this();
        setAction(a);
    }

    public JMenu(String s, boolean b) {
        this(s);
    }

    private JPopupMenu ensurePopupMenu() {
        if (popupMenu == null) {
            popupMenu = new JPopupMenu();
            popupMenu.setInvoker(this);
        }
        return popupMenu;
    }

    public JPopupMenu getPopupMenu() {
        return ensurePopupMenu();
    }

    public boolean isPopupMenuVisible() {
        return popupMenu != null && popupMenu.isVisible();
    }

    public void setPopupMenuVisible(boolean b) {
        if (b == isPopupMenuVisible()) {
            return;
        }
        if (b) {
            if (!isShowing() || !isEnabled()) {
                return;
            }
            MenuSelectionManager.defaultManager().cn1CloseOthers(this);
            if (isTopLevelMenu()) {
                ensurePopupMenu().show(this, 0, getHeight());
            } else {
                ensurePopupMenu().show(this, getWidth(), 0);
            }
            if (isPopupMenuVisible()) {
                getModel().setSelected(true);
                fireMenuSelected();
            }
        } else {
            popupMenu.setVisible(false);
        }
    }

    /// The popup of this menu closed.
    void cn1PopupClosed() {
        getModel().setSelected(false);
        fireMenuDeselected();
    }

    /// Opens or closes the popup; a menu has no action of its own.
    @Override
    public void doClick(int pressTime) {
        setPopupMenuVisible(!isPopupMenuVisible());
    }

    public boolean isTopLevelMenu() {
        return getParent() instanceof JMenuBar;
    }

    public int getDelay() {
        return delay;
    }

    /// Recorded only: popups open at once.
    public void setDelay(int d) {
        if (d < 0) {
            throw new IllegalArgumentException("Delay must be a positive integer");
        }
        delay = d;
    }

    public JMenuItem add(JMenuItem menuItem) {
        ensurePopupMenu().add(menuItem);
        return menuItem;
    }

    @Override
    public Component add(Component c) {
        ensurePopupMenu().add(c);
        return c;
    }

    @Override
    public Component add(Component c, int index) {
        ensurePopupMenu().add(c, index);
        return c;
    }

    public JMenuItem add(String s) {
        return add(new JMenuItem(s));
    }

    public JMenuItem add(Action a) {
        JMenuItem mi = createActionComponent(a);
        mi.setAction(a);
        add(mi);
        return mi;
    }

    protected JMenuItem createActionComponent(Action a) {
        return new JMenuItem();
    }

    public void addSeparator() {
        ensurePopupMenu().addSeparator();
    }

    public void insert(String s, int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        ensurePopupMenu().insert(new JMenuItem(s), pos);
    }

    public JMenuItem insert(JMenuItem mi, int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        ensurePopupMenu().insert(mi, pos);
        return mi;
    }

    public JMenuItem insert(Action a, int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        JMenuItem mi = new JMenuItem(a);
        ensurePopupMenu().insert(mi, pos);
        return mi;
    }

    public void insertSeparator(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        ensurePopupMenu().insert(new JPopupMenu.Separator(), index);
    }

    public JMenuItem getItem(int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        Component c = getMenuComponent(pos);
        return c instanceof JMenuItem ? (JMenuItem) c : null;
    }

    public int getItemCount() {
        return getMenuComponentCount();
    }

    public boolean isTearOff() {
        throw new Error("boolean isTearOff() {} not yet implemented");
    }

    public void remove(JMenuItem item) {
        if (popupMenu != null) {
            popupMenu.remove(item);
        }
    }

    @Override
    public void remove(int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        if (pos > getItemCount()) {
            throw new IllegalArgumentException("index greater than the number of items.");
        }
        if (popupMenu != null) {
            popupMenu.remove(pos);
        }
    }

    @Override
    public void remove(Component c) {
        if (popupMenu != null) {
            popupMenu.remove(c);
        }
    }

    @Override
    public void removeAll() {
        if (popupMenu != null) {
            popupMenu.removeAll();
        }
    }

    public int getMenuComponentCount() {
        return popupMenu == null ? 0 : popupMenu.getComponentCount();
    }

    public Component getMenuComponent(int n) {
        return popupMenu == null ? null : popupMenu.getComponent(n);
    }

    public Component[] getMenuComponents() {
        return popupMenu == null ? new Component[0] : popupMenu.getComponents();
    }

    public boolean isMenuComponent(Component c) {
        if (c == this) {
            return true;
        }
        Component[] cs = getMenuComponents();
        for (int i = 0; i < cs.length; i++) {
            if (cs[i] == c || (cs[i] instanceof JMenu && ((JMenu) cs[i]).isMenuComponent(c))) {
                return true;
            }
        }
        return false;
    }

    /// A menu has no accelerator.
    @Override
    public void setAccelerator(KeyStroke keyStroke) {
        throw new Error("setAccelerator() is not defined for JMenu.  Use setMnemonic() instead.");
    }

    public void addMenuListener(MenuListener l) {
        listenerList.add(MenuListener.class, l);
    }

    public void removeMenuListener(MenuListener l) {
        listenerList.remove(MenuListener.class, l);
    }

    public MenuListener[] getMenuListeners() {
        return listenerList.getListeners(MenuListener.class);
    }

    protected void fireMenuSelected() {
        MenuListener[] ls = getMenuListeners();
        MenuEvent e = ls.length == 0 ? null : new MenuEvent(this);
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].menuSelected(e);
        }
    }

    protected void fireMenuDeselected() {
        MenuListener[] ls = getMenuListeners();
        MenuEvent e = ls.length == 0 ? null : new MenuEvent(this);
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].menuDeselected(e);
        }
    }

    protected void fireMenuCanceled() {
        MenuListener[] ls = getMenuListeners();
        MenuEvent e = ls.length == 0 ? null : new MenuEvent(this);
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].menuCanceled(e);
        }
    }

    @Override
    public void menuSelectionChanged(boolean isIncluded) {
        setSelected(isIncluded);
    }

    @Override
    public MenuElement[] getSubElements() {
        return popupMenu == null ? new MenuElement[0] : new MenuElement[]{popupMenu};
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        MenuBridge.changed(this);
    }

    /// The items of this menu and its submenus, in order.
    void cn1Items(ArrayList<JMenuItem> out) {
        Component[] cs = getMenuComponents();
        for (int i = 0; i < cs.length; i++) {
            if (cs[i] instanceof JMenu) {
                ((JMenu) cs[i]).cn1Items(out);
            } else if (cs[i] instanceof JMenuItem) {
                out.add((JMenuItem) cs[i]);
            }
        }
    }
}
