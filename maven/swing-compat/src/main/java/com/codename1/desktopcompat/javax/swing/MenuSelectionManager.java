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
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.javax.swing.event.EventListenerList;
import java.util.ArrayList;

/// Knows which popup menus are open and closes them together.
///
/// The selected path is the chain of open popup menus, each preceded by
/// the menu that opened it. `setSelectedPath` with an empty path closes
/// every popup; any other path is ignored, because menus open and close
/// through their own components here.
public class MenuSelectionManager {

    private static final MenuSelectionManager INSTANCE = new MenuSelectionManager();

    protected ChangeEvent changeEvent;
    protected EventListenerList listenerList = new EventListenerList();

    private final ArrayList<JPopupMenu> open = new ArrayList<JPopupMenu>();

    public MenuSelectionManager() {
    }

    public static MenuSelectionManager defaultManager() {
        return INSTANCE;
    }

    public void setSelectedPath(MenuElement[] path) {
        if (path == null || path.length == 0) {
            clearSelectedPath();
        }
    }

    public MenuElement[] getSelectedPath() {
        ArrayList<MenuElement> l = new ArrayList<MenuElement>();
        for (int i = 0; i < open.size(); i++) {
            JPopupMenu p = open.get(i);
            if (p.getInvoker() instanceof JMenu) {
                l.add((JMenu) p.getInvoker());
            }
            l.add(p);
        }
        return l.toArray(new MenuElement[l.size()]);
    }

    /// Closes every open popup menu.
    public void clearSelectedPath() {
        while (!open.isEmpty()) {
            JPopupMenu p = open.get(open.size() - 1);
            p.setVisible(false);
            open.remove(p);
        }
    }

    public boolean isComponentPartOfCurrentMenu(Component c) {
        return !open.isEmpty() && cn1Inside(c);
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
        ChangeListener[] ls = getChangeListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            if (changeEvent == null) {
                changeEvent = new ChangeEvent(this);
            }
            ls[i].stateChanged(changeEvent);
        }
    }

    /// Whether any popup menu is open.
    public boolean cn1PopupShowing() {
        return !open.isEmpty();
    }

    /// Whether `c` is part of an open popup menu or of a menu bar, so
    /// that a press on it is the menu's business and does not close the
    /// popups.
    public boolean cn1Inside(Component c) {
        Component k = c;
        while (k != null) {
            if (k instanceof JMenuBar || (k instanceof JPopupMenu && open.contains(k))) {
                return true;
            }
            k = k.getParent();
        }
        return false;
    }

    /// A popup menu opened.
    public void cn1Opened(JPopupMenu p) {
        if (!open.contains(p)) {
            open.add(p);
            fireStateChanged();
        }
    }

    /// A popup menu closed.
    public void cn1Closed(JPopupMenu p) {
        if (open.remove(p)) {
            fireStateChanged();
        }
    }

    /// Closes the open popup menus that `menu` is not in.
    public void cn1CloseOthers(JMenu menu) {
        JPopupMenu[] all = open.toArray(new JPopupMenu[open.size()]);
        for (int i = all.length - 1; i >= 0; i--) {
            boolean related = false;
            Component k = menu;
            while (k != null) {
                if (k == all[i]) {
                    related = true;
                    break;
                }
                k = k instanceof JPopupMenu ? ((JPopupMenu) k).getInvoker() : k.getParent();
            }
            if (!related) {
                all[i].setVisible(false);
            }
        }
    }
}
