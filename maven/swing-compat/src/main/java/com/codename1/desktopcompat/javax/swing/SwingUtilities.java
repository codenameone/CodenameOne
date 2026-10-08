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
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

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
    public static void invokeAndWait(Runnable doRun) throws InterruptedException, InvocationTargetException {
        EventQueue.invokeAndWait(doRun);
    }

    /// Runs the key bindings for a key event whose target is not a Swing
    /// component: the bindings of its Swing ancestors, then those any
    /// component of its window registered for the whole window.
    public static boolean processKeyBindings(KeyEvent event) {
        if (event == null || event.isConsumed()) {
            return false;
        }
        Component c = event.getComponent();
        if (c == null) {
            return false;
        }
        return JComponent.cn1KeyBindings(c, event);
    }

    /// Runs `action` for a key binding. The action command is the
    /// action's own, or else the character of the key.
    public static boolean notifyAction(Action action, KeyStroke ks, KeyEvent event, Object sender, int modifiers) {
        if (action == null || !action.isEnabled()) {
            return false;
        }
        Object cmd = action.getValue(Action.ACTION_COMMAND_KEY);
        String command = null;
        if (cmd != null) {
            command = cmd.toString();
        } else if (event != null && event.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
            command = String.valueOf(event.getKeyChar());
        }
        action.actionPerformed(new ActionEvent(sender, ActionEvent.ACTION_PERFORMED, command,
                event == null ? 0 : event.getWhen(), modifiers));
        return true;
    }

    public static Rectangle convertRectangle(Component source, Rectangle aRectangle, Component destination) {
        Point p = convertPoint(source, aRectangle.x, aRectangle.y, destination);
        return new Rectangle(p.x, p.y, aRectangle.width, aRectangle.height);
    }

    public static MouseEvent convertMouseEvent(Component source, MouseEvent sourceEvent, Component destination) {
        Point p = convertPoint(source, sourceEvent.getX(), sourceEvent.getY(), destination);
        Component newSource = destination != null ? destination : source;
        return new MouseEvent(newSource, sourceEvent.getID(), sourceEvent.getWhen(),
                sourceEvent.getModifiers() | sourceEvent.getModifiersEx(), p.x, p.y, sourceEvent.getClickCount(),
                sourceEvent.isPopupTrigger(), sourceEvent.getButton());
    }

    public static Container getAncestorNamed(String name, Component comp) {
        if (comp == null || name == null) {
            return null;
        }
        Container parent = comp.getParent();
        while (parent != null && !name.equals(parent.getName())) {
            parent = parent.getParent();
        }
        return parent;
    }

    public static Container getUnwrappedParent(Component component) {
        return component.getParent();
    }

    public static boolean isRectangleContainingRectangle(Rectangle a, Rectangle b) {
        return b.x >= a.x && b.x + b.width <= a.x + a.width && b.y >= a.y && b.y + b.height <= a.y + a.height;
    }

    public static Rectangle getLocalBounds(Component aComponent) {
        return new Rectangle(0, 0, aComponent.getWidth(), aComponent.getHeight());
    }

    public static Rectangle calculateInnerArea(JComponent c, Rectangle r) {
        if (c == null) {
            return null;
        }
        Rectangle rect = r == null ? new Rectangle() : r;
        Insets in = c.getInsets();
        rect.x = in.left;
        rect.y = in.top;
        rect.width = c.getWidth() - in.left - in.right;
        rect.height = c.getHeight() - in.top - in.bottom;
        return rect;
    }

    /// Places the icon and the text of a label or a button inside
    /// `viewR`, storing where each goes in `iconR` and `textR`, and
    /// returns the text to draw: the text itself, or a clipped copy that
    /// ends in three dots when it is wider than the room it has.
    public static String layoutCompoundLabel(FontMetrics fm, String text, Icon icon, int verticalAlignment,
            int horizontalAlignment, int verticalTextPosition, int horizontalTextPosition, Rectangle viewR,
            Rectangle iconR, Rectangle textR, int textIconGap) {
        if (icon != null) {
            iconR.width = icon.getIconWidth();
            iconR.height = icon.getIconHeight();
        } else {
            iconR.width = 0;
            iconR.height = 0;
        }
        boolean empty = text == null || text.length() == 0;
        String out = text;
        int gap = empty || icon == null ? 0 : textIconGap;
        if (empty) {
            textR.width = 0;
            textR.height = 0;
            out = "";
        } else {
            int avail = horizontalTextPosition == CENTER ? viewR.width : viewR.width - (iconR.width + gap);
            textR.width = fm.stringWidth(text);
            textR.height = fm.getHeight();
            if (textR.width > avail) {
                out = clip(fm, text, avail);
                textR.width = fm.stringWidth(out);
            }
        }
        if (verticalTextPosition == TOP) {
            textR.y = horizontalTextPosition != CENTER ? 0 : -(textR.height + gap);
        } else if (verticalTextPosition == CENTER) {
            textR.y = iconR.height / 2 - textR.height / 2;
        } else {
            textR.y = horizontalTextPosition != CENTER ? iconR.height - textR.height : iconR.height + gap;
        }
        if (horizontalTextPosition == LEFT || horizontalTextPosition == LEADING) {
            textR.x = -(textR.width + gap);
        } else if (horizontalTextPosition == CENTER) {
            textR.x = iconR.width / 2 - textR.width / 2;
        } else {
            textR.x = iconR.width + gap;
        }
        int labelX = Math.min(iconR.x, textR.x);
        int labelW = Math.max(iconR.x + iconR.width, textR.x + textR.width) - labelX;
        int labelY = Math.min(iconR.y, textR.y);
        int labelH = Math.max(iconR.y + iconR.height, textR.y + textR.height) - labelY;
        int dx;
        int dy;
        if (verticalAlignment == TOP) {
            dy = viewR.y - labelY;
        } else if (verticalAlignment == CENTER) {
            dy = viewR.y + viewR.height / 2 - (labelY + labelH / 2);
        } else {
            dy = viewR.y + viewR.height - (labelY + labelH);
        }
        if (horizontalAlignment == LEFT || horizontalAlignment == LEADING) {
            dx = viewR.x - labelX;
        } else if (horizontalAlignment == RIGHT || horizontalAlignment == TRAILING) {
            dx = viewR.x + viewR.width - (labelX + labelW);
        } else {
            dx = viewR.x + viewR.width / 2 - (labelX + labelW / 2);
        }
        textR.x += dx;
        textR.y += dy;
        iconR.x += dx;
        iconR.y += dy;
        return out;
    }

    public static String layoutCompoundLabel(JComponent c, FontMetrics fm, String text, Icon icon,
            int verticalAlignment, int horizontalAlignment, int verticalTextPosition, int horizontalTextPosition,
            Rectangle viewR, Rectangle iconR, Rectangle textR, int textIconGap) {
        return layoutCompoundLabel(fm, text, icon, verticalAlignment, horizontalAlignment, verticalTextPosition,
                horizontalTextPosition, viewR, iconR, textR, textIconGap);
    }

    private static String clip(FontMetrics fm, String text, int avail) {
        String dots = "...";
        int room = avail - fm.stringWidth(dots);
        if (room <= 0) {
            return dots;
        }
        int n = 0;
        int w = 0;
        while (n < text.length()) {
            int cw = fm.charWidth(text.charAt(n));
            if (w + cw > room) {
                break;
            }
            w += cw;
            n++;
        }
        return text.substring(0, n) + dots;
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
            if (p instanceof RootPaneContainer) {
                return ((RootPaneContainer) p).getRootPane();
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
