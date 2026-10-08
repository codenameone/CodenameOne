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
import com.codename1.desktopcompat.java.awt.FlowLayout;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.rt.MenuBridge;
import com.codename1.desktopcompat.rt.WindowHost;
import java.util.ArrayList;

/// A row of menus.
///
/// Set on a frame with `setJMenuBar` it is not drawn: the frame's host
/// shows its items as Codename One commands, which reach the native menu
/// bar on the desktop ports and the overflow menu elsewhere; see
/// [com.codename1.desktopcompat.rt.MenuBridge]. Added to a container like
/// any component, or set on a dialog that floats over a form, it is a row
/// of menus that open popups.
///
/// The accelerators of its items work while its window is the focused
/// one, drawn or not.
public class JMenuBar extends JComponent implements MenuElement {

    private boolean paintBorder = true;
    private Insets margin;

    public JMenuBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setFocusable(false);
    }

    public JMenu add(JMenu c) {
        super.add(c);
        return c;
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
        MenuBridge.changed(this);
    }

    @Override
    public void remove(int index) {
        super.remove(index);
        MenuBridge.changed(this);
    }

    public JMenu getMenu(int index) {
        Component c = getComponent(index);
        return c instanceof JMenu ? (JMenu) c : null;
    }

    public int getMenuCount() {
        return getComponentCount();
    }

    public void setHelpMenu(JMenu menu) {
        throw new Error("setHelpMenu() not yet implemented.");
    }

    public JMenu getHelpMenu() {
        throw new Error("getHelpMenu() not yet implemented.");
    }

    public int getComponentIndex(Component c) {
        return getComponentZOrder(c);
    }

    public void setSelected(Component sel) {
        if (sel instanceof JMenu) {
            ((JMenu) sel).setPopupMenuVisible(true);
        }
    }

    public boolean isSelected() {
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof JMenu && ((JMenu) c).isPopupMenuVisible()) {
                return true;
            }
        }
        return false;
    }

    public boolean isBorderPainted() {
        return paintBorder;
    }

    public void setBorderPainted(boolean b) {
        paintBorder = b;
        repaint();
    }

    public void setMargin(Insets m) {
        margin = m == null ? null : new Insets(m.top, m.left, m.bottom, m.right);
        revalidate();
    }

    public Insets getMargin() {
        return margin == null ? new Insets(0, 0, 0, 0) : new Insets(margin.top, margin.left, margin.bottom,
                margin.right);
    }

    /// Whether the host of the window this bar is the menu bar of shows
    /// it as commands, so that it takes no room in the window.
    public boolean cn1Bridged() {
        JRootPane root = SwingUtilities.getRootPane(this);
        if (root == null || root.getJMenuBar() != this) {
            return false;
        }
        Window w = SwingUtilities.getWindowAncestor(root);
        WindowHost h = w == null ? null : w.cn1Host();
        return h != null && h.takesCommands();
    }

    /// The items of every menu and submenu, in order.
    public JMenuItem[] cn1Items() {
        ArrayList<JMenuItem> out = new ArrayList<JMenuItem>();
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof JMenu) {
                ((JMenu) c).cn1Items(out);
            } else if (c instanceof JMenuItem) {
                out.add((JMenuItem) c);
            }
        }
        return out.toArray(new JMenuItem[out.size()]);
    }

    /// Runs the first enabled item whose accelerator is the key stroke.
    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (super.processKeyBinding(ks, e, condition, pressed)) {
            return true;
        }
        if (condition != WHEN_IN_FOCUSED_WINDOW) {
            return false;
        }
        JMenuItem[] items = cn1Items();
        for (int i = 0; i < items.length; i++) {
            if (ks.equals(items[i].getAccelerator()) && items[i].isEnabled()) {
                items[i].cn1Choose();
                return true;
            }
        }
        return false;
    }

    @Override
    public void processMouseEvent(MouseEvent event, MenuElement[] path, MenuSelectionManager manager) {
    }

    @Override
    public void processKeyEvent(KeyEvent e, MenuElement[] path, MenuSelectionManager manager) {
    }

    @Override
    public void menuSelectionChanged(boolean isIncluded) {
    }

    @Override
    public MenuElement[] getSubElements() {
        ArrayList<MenuElement> l = new ArrayList<MenuElement>();
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof MenuElement) {
                l.add((MenuElement) c);
            }
        }
        return l.toArray(new MenuElement[l.size()]);
    }

    @Override
    public Component getComponent() {
        return this;
    }
}
