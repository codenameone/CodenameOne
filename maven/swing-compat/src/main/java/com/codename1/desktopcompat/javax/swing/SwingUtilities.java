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
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.EventQueue;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;

/// The Swing helpers for the event dispatch thread and the component
/// tree.
public class SwingUtilities implements SwingConstants {

    private SwingUtilities() {
    }

    public static void invokeLater(Runnable doRun) {
        EventQueue.invokeLater(doRun);
    }

    /// See `EventQueue.invokeAndWait`: on the event dispatch thread the
    /// runnable simply runs.
    public static void invokeAndWait(Runnable doRun) throws InterruptedException {
        EventQueue.invokeAndWait(doRun);
    }

    public static boolean isEventDispatchThread() {
        return EventQueue.isDispatchThread();
    }

    public static Window getWindowAncestor(Component c) {
        for (Container p = c.getParent(); p != null; p = p.getParent()) {
            if (p instanceof Window) {
                return (Window) p;
            }
        }
        return null;
    }

    public static Window windowForComponent(Component c) {
        return getWindowAncestor(c);
    }

    public static Component getRoot(Component c) {
        for (Component p = c; p != null; p = p.getParent()) {
            if (p instanceof Window) {
                return p;
            }
        }
        return null;
    }

    public static JRootPane getRootPane(Component c) {
        for (Component p = c; p != null; p = p.getParent()) {
            if (p instanceof JRootPane) {
                return (JRootPane) p;
            }
            if (p instanceof JFrame) {
                return ((JFrame) p).getRootPane();
            }
            if (p instanceof JDialog) {
                return ((JDialog) p).getRootPane();
            }
        }
        return null;
    }

    public static Container getAncestorOfClass(Class<?> c, Component comp) {
        if (comp == null || c == null) {
            return null;
        }
        Container parent = comp.getParent();
        while (parent != null && !c.isInstance(parent)) {
            parent = parent.getParent();
        }
        return parent;
    }

    public static boolean isDescendingFrom(Component a, Component b) {
        if (a == b) {
            return true;
        }
        for (Container p = a.getParent(); p != null; p = p.getParent()) {
            if (p == b) {
                return true;
            }
        }
        return false;
    }

    public static Component getDeepestComponentAt(Component parent, int x, int y) {
        if (!parent.contains(x, y)) {
            return null;
        }
        if (parent instanceof Container) {
            return ((Container) parent).findComponentAt(x, y);
        }
        return parent;
    }

    public static void convertPointToScreen(Point p, Component c) {
        Point s = c.getLocationOnScreen();
        p.x += s.x;
        p.y += s.y;
    }

    public static void convertPointFromScreen(Point p, Component c) {
        Point s = c.getLocationOnScreen();
        p.x -= s.x;
        p.y -= s.y;
    }

    public static Point convertPoint(Component source, int x, int y, Component destination) {
        Point p = new Point(x, y);
        if (source != null) {
            convertPointToScreen(p, source);
        }
        if (destination != null) {
            convertPointFromScreen(p, destination);
        }
        return p;
    }

    public static Point convertPoint(Component source, Point aPoint, Component destination) {
        return convertPoint(source, aPoint.x, aPoint.y, destination);
    }

    public static boolean isLeftMouseButton(MouseEvent anEvent) {
        return anEvent.getButton() == MouseEvent.BUTTON1;
    }

    public static boolean isMiddleMouseButton(MouseEvent anEvent) {
        return anEvent.getButton() == MouseEvent.BUTTON2;
    }

    public static boolean isRightMouseButton(MouseEvent anEvent) {
        return anEvent.getButton() == MouseEvent.BUTTON3;
    }

    public static int computeStringWidth(FontMetrics fm, String str) {
        return str == null ? 0 : fm.stringWidth(str);
    }

    /// Does nothing: there are no UI delegates to refresh.
    public static void updateComponentTreeUI(Component c) {
    }
}
