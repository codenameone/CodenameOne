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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.accessibility.Accessible;
import com.codename1.desktopcompat.javax.swing.event.PopupMenuEvent;
import com.codename1.desktopcompat.javax.swing.event.PopupMenuListener;
import com.codename1.desktopcompat.rt.MenuBridge;

/// A menu that pops up over a window.
///
/// The popup is a component of the window it opens in: `show` puts it in
/// the popup layer of that window's layered pane, at the given point of
/// the invoker, moved as far as needed to stay inside the window. It can
/// therefore not extend beyond its window, and it needs a Swing window --
/// one with a root pane -- to open in. A press outside the open popups
/// closes them and is not delivered; so does the escape key.
///
/// Set as the component popup menu of a component it opens on the popup
/// trigger: a press of the secondary mouse button, or a long press of a
/// finger.
public class JPopupMenu extends JComponent implements Accessible, MenuElement {

    private Component invoker;
    private String label;
    private boolean showing;
    private boolean paintBorder = true;
    private int desiredX;
    private int desiredY;

    public JPopupMenu() {
        this(null);
    }

    public JPopupMenu(String label) {
        this.label = label;
        setLayout(new MenuLayout());
        setOpaque(true);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
    }

    public JMenuItem add(JMenuItem menuItem) {
        super.add(menuItem);
        return menuItem;
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
        add(new JPopupMenu.Separator());
    }

    public void insert(Action a, int index) {
        JMenuItem mi = createActionComponent(a);
        mi.setAction(a);
        insert(mi, index);
    }

    public void insert(Component component, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        add(component, Math.min(index, getComponentCount()));
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
        MenuBridge.changed(this);
    }

    @Override
    public void remove(int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        if (pos > getComponentCount() - 1) {
            throw new IllegalArgumentException("index greater than the number of items.");
        }
        super.remove(pos);
        MenuBridge.changed(this);
    }

    public int getComponentIndex(Component c) {
        return getComponentZOrder(c);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        String old = this.label;
        this.label = label;
        firePropertyChange("label", old, label);
    }

    public Component getInvoker() {
        return invoker;
    }

    public void setInvoker(Component invoker) {
        this.invoker = invoker;
    }

    public boolean isBorderPainted() {
        return paintBorder;
    }

    public void setBorderPainted(boolean b) {
        paintBorder = b;
        repaint();
    }

    public Insets getMargin() {
        return new Insets(0, 0, 0, 0);
    }

    public boolean isPopupTrigger(MouseEvent e) {
        return e.isPopupTrigger();
    }

    public void pack() {
        if (showing) {
            Dimension d = getPreferredSize();
            setSize(d.width, d.height);
            validate();
        }
    }

    public void setPopupSize(Dimension d) {
        setPreferredSize(d);
    }

    public void setPopupSize(int width, int height) {
        setPopupSize(new Dimension(width, height));
    }

    /// Opens the popup at a point of `invoker`.
    public void show(Component invoker, int x, int y) {
        if (showing) {
            setVisible(false);
        }
        setInvoker(invoker);
        desiredX = x;
        desiredY = y;
        setVisible(true);
    }

    /// Whether the popup is open.
    @Override
    public boolean isVisible() {
        return showing;
    }

    /// Opens the popup at the point last given to `show`, or closes it.
    @Override
    public void setVisible(boolean b) {
        if (b == showing) {
            return;
        }
        if (b) {
            JRootPane root = invoker == null ? null : SwingUtilities.getRootPane(invoker);
            if (root == null) {
                return;
            }
            JLayeredPane lp = root.getLayeredPane();
            firePopupMenuWillBecomeVisible();
            showing = true;
            lp.add(this, JLayeredPane.POPUP_LAYER, 0);
            Dimension d = getPreferredSize();
            Point p = SwingUtilities.convertPoint(invoker, desiredX, desiredY, lp);
            int w = Math.min(d.width, lp.getWidth());
            int h = Math.min(d.height, lp.getHeight());
            setBounds(Math.max(0, Math.min(p.x, lp.getWidth() - w)), Math.max(0, Math.min(p.y, lp.getHeight() - h)),
                    w, h);
            validate();
            MenuSelectionManager.defaultManager().cn1Opened(this);
            lp.repaint();
        } else {
            for (int i = 0; i < getComponentCount(); i++) {
                Component c = getComponent(i);
                if (c instanceof JMenu) {
                    ((JMenu) c).setPopupMenuVisible(false);
                }
            }
            firePopupMenuWillBecomeInvisible();
            showing = false;
            Container parent = getParent();
            if (parent != null) {
                parent.remove(this);
                parent.repaint();
            }
            MenuSelectionManager.defaultManager().cn1Closed(this);
            if (invoker instanceof JMenu) {
                ((JMenu) invoker).cn1PopupClosed();
            }
        }
    }

    public void addPopupMenuListener(PopupMenuListener l) {
        listenerList.add(PopupMenuListener.class, l);
    }

    public void removePopupMenuListener(PopupMenuListener l) {
        listenerList.remove(PopupMenuListener.class, l);
    }

    public PopupMenuListener[] getPopupMenuListeners() {
        return listenerList.getListeners(PopupMenuListener.class);
    }

    protected void firePopupMenuWillBecomeVisible() {
        PopupMenuListener[] ls = getPopupMenuListeners();
        PopupMenuEvent e = ls.length == 0 ? null : new PopupMenuEvent(this);
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].popupMenuWillBecomeVisible(e);
        }
    }

    protected void firePopupMenuWillBecomeInvisible() {
        PopupMenuListener[] ls = getPopupMenuListeners();
        PopupMenuEvent e = ls.length == 0 ? null : new PopupMenuEvent(this);
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].popupMenuWillBecomeInvisible(e);
        }
    }

    protected void firePopupMenuCanceled() {
        PopupMenuListener[] ls = getPopupMenuListeners();
        PopupMenuEvent e = ls.length == 0 ? null : new PopupMenuEvent(this);
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].popupMenuCanceled(e);
        }
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
        java.util.ArrayList<MenuElement> l = new java.util.ArrayList<MenuElement>();
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

    /// The line between two groups of items of a popup menu.
    public static class Separator extends JSeparator {

        public Separator() {
            super(JSeparator.HORIZONTAL);
        }
    }

    /// Stacks the items, each as wide as the widest.
    private static final class MenuLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            int w = 0;
            int h = 0;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component c = parent.getComponent(i);
                if (c.isVisible()) {
                    Dimension d = c.getPreferredSize();
                    w = Math.max(w, d.width);
                    h += d.height;
                }
            }
            return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int w = parent.getWidth() - in.left - in.right;
            int y = in.top;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component c = parent.getComponent(i);
                if (c.isVisible()) {
                    int h = c.getPreferredSize().height;
                    c.setBounds(in.left, y, w, h);
                    y += h;
                }
            }
        }
    }
}
