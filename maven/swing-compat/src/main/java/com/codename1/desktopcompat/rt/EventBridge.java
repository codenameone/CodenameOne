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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.KeyboardFocusManager;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.FocusEvent;
import com.codename1.desktopcompat.java.awt.event.InputEvent;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.awt.event.MouseWheelEvent;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JPopupMenu;
import com.codename1.desktopcompat.javax.swing.JScrollPane;
import com.codename1.desktopcompat.javax.swing.JInternalFrame;
import com.codename1.desktopcompat.javax.swing.MenuSelectionManager;
import com.codename1.desktopcompat.javax.swing.SwingUtilities;
import com.codename1.ui.Display;
import com.codename1.ui.plaf.UIManager;

/// Turns Codename One input into AWT events.
///
/// Pointer and key input enters at the host of a window (see
/// [WindowHost]). A pointer event goes to the deepest visible component
/// under the pointer that wants mouse events, or the nearest ancestor that
/// does; when that component is disabled the event is dropped. The
/// component pressed keeps receiving the drags and the release. Key events
/// go to the focus owner, or to the window when no component of it owns
/// the focus.
///
/// What an application can rely on:
///
/// - Modifiers and the button come from an [InputState]; see
///   [DisplayInputState] for what the ports report.
/// - A press with the secondary button is a popup trigger. So is the
///   release that ends a long press of a finger or stylus, which produces
///   no click. Either shows the component popup menu of the component
///   under the pointer.
/// - Clicks count up while they follow each other within half a second,
///   on the same component, with the same button, without moving.
/// - A press outside an open popup menu closes it and goes no further.
/// - A key pressed that no listener or key binding consumed moves the
///   focus when it is the tab key; escape closes an open popup menu.
public final class EventBridge {

    private static final int CLICK_INTERVAL = 500;

    private static final InputState DISPLAY_STATE = new DisplayInputState();

    private static InputState input = DISPLAY_STATE;
    private static Component focusOwner;
    private static Component pressTarget;
    private static Component hoverTarget;
    private static Component lastClickTarget;
    private static long lastClickTime;
    private static int lastClickButton;
    private static int clickCount;
    private static int pressX;
    private static int pressY;
    private static int pressButton;
    private static boolean dragged;
    private static boolean swallowed;
    private static boolean longPressed;
    private static boolean popupShown;

    private EventBridge() {
    }

    /// Replaces the source of modifiers and buttons; `null` restores the
    /// display's. For tests.
    public static void setInputState(InputState state) {
        input = state == null ? DISPLAY_STATE : state;
    }

    // ------------------------------------------------------------ focus

    /// The component key events go to, or `null`.
    public static Component focusOwner() {
        return focusOwner;
    }

    /// Moves the focus to `c`, firing focus lost and focus gained.
    /// `toPeer` also focuses the Codename One widget behind it.
    public static void setFocusOwner(Component c, boolean toPeer) {
        Component old = focusOwner;
        if (old == c) {
            return;
        }
        focusOwner = c;
        if (old != null) {
            old.dispatchEvent(new FocusEvent(old, FocusEvent.FOCUS_LOST, false, c));
        }
        if (c != null) {
            c.dispatchEvent(new FocusEvent(c, FocusEvent.FOCUS_GAINED, false, old));
            com.codename1.ui.Component p = c.cn1PeerOrNull();
            if (toPeer && p instanceof Peer && ((Peer) p).nativeLook() && p.isFocusable()
                    && p.getTopLevelContainer() != null) {
                p.requestFocus();
            }
        }
        KeyboardFocusManager.cn1FocusChanged(old, c);
    }

    /// The Codename One widget behind `c` lost the focus.
    public static void nativeFocusLost(Component c) {
        if (focusOwner == c) {
            setFocusOwner(null, false);
        }
    }

    /// Drops every reference to a component that left its window.
    public static void forget(Component c) {
        if (focusOwner == c) {
            focusOwner = null;
        }
        if (pressTarget == c) {
            pressTarget = null;
        }
        if (hoverTarget == c) {
            hoverTarget = null;
        }
        if (lastClickTarget == c) {
            lastClickTarget = null;
        }
    }

    /// Forgets the last click, so that the next one counts as the first
    /// wherever and whenever it lands. The count otherwise belongs to a
    /// place and a button: a click within half a second of the last one,
    /// with the same button and within a few pixels of it, counts up.
    public static void resetClickCount() {
        lastClickTarget = null;
        lastClickTime = 0;
        clickCount = 0;
    }

    // ------------------------------------------------------------ theme

    /// The background a window has when none was set: the theme's form
    /// background, white with no display.
    public static Color defaultBackground() {
        if (!Display.isInitialized()) {
            return Color.WHITE;
        }
        return new Color(UIManager.getInstance().getComponentStyle("Form").getBgColor() & 0xffffff);
    }

    /// The foreground a window has when none was set: the theme's label
    /// color, black with no display.
    public static Color defaultForeground() {
        if (!Display.isInitialized()) {
            return Color.BLACK;
        }
        return new Color(UIManager.getInstance().getComponentStyle("Label").getFgColor() & 0xffffff);
    }

    // ------------------------------------------------------------ pointer

    /// The component that receives a mouse event at `hit`: the nearest
    /// component from `hit` up that wants mouse events, unless it is
    /// disabled.
    private static Component mouseTarget(Component hit) {
        Component c = hit;
        while (c != null && !c.cn1WantsMouse()) {
            c = c.getParent();
        }
        return c != null && c.isEnabled() ? c : null;
    }

    private static int buttonMask(int button) {
        switch (button) {
            case MouseEvent.BUTTON1:
                return InputEvent.BUTTON1_MASK;
            case MouseEvent.BUTTON2:
                return InputEvent.BUTTON2_MASK;
            case MouseEvent.BUTTON3:
                return InputEvent.BUTTON3_MASK;
            default:
                return 0;
        }
    }

    private static int buttonDownMask(int button) {
        switch (button) {
            case MouseEvent.BUTTON1:
                return InputEvent.BUTTON1_DOWN_MASK;
            case MouseEvent.BUTTON2:
                return InputEvent.BUTTON2_DOWN_MASK;
            case MouseEvent.BUTTON3:
                return InputEvent.BUTTON3_DOWN_MASK;
            default:
                return 0;
        }
    }

    /// Both encodings of a set of extended keyboard modifiers.
    private static int both(int ex) {
        int m = ex;
        if ((ex & InputEvent.SHIFT_DOWN_MASK) != 0) {
            m |= InputEvent.SHIFT_MASK;
        }
        if ((ex & InputEvent.CTRL_DOWN_MASK) != 0) {
            m |= InputEvent.CTRL_MASK;
        }
        if ((ex & InputEvent.ALT_DOWN_MASK) != 0) {
            m |= InputEvent.ALT_MASK;
        }
        if ((ex & InputEvent.META_DOWN_MASK) != 0) {
            m |= InputEvent.META_MASK;
        }
        if ((ex & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            m |= InputEvent.ALT_GRAPH_MASK;
        }
        return m;
    }

    /// The position of a window relative point in `target`'s coordinates.
    private static int[] local(Window w, Component target, int x, int y) {
        int lx = x;
        int ly = y;
        for (Component c = target; c != null && c != w; c = c.getParent()) {
            lx -= c.getX();
            ly -= c.getY();
        }
        return new int[]{lx, ly};
    }

    private static void send(Window w, Component target, int id, int x, int y, int clicks, int button,
            boolean trigger) {
        int[] p = local(w, target, x, y);
        boolean down = id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_DRAGGED;
        int mods = both(input.modifiers()) | buttonMask(button) | (down ? buttonDownMask(button) : 0);
        target.dispatchEvent(new MouseEvent(target, id, System.currentTimeMillis(), mods, p[0], p[1], clicks,
                trigger, id == MouseEvent.MOUSE_DRAGGED || id == MouseEvent.MOUSE_MOVED
                || id == MouseEvent.MOUSE_ENTERED || id == MouseEvent.MOUSE_EXITED ? MouseEvent.NOBUTTON : button));
    }

    private static void hover(Window w, Component target, int x, int y) {
        if (target == hoverTarget) {
            return;
        }
        Component old = hoverTarget;
        hoverTarget = target;
        if (old != null) {
            Window ow = SwingUtilities.getRoot(old) instanceof Window ? (Window) SwingUtilities.getRoot(old) : w;
            send(ow, old, MouseEvent.MOUSE_EXITED, x, y, 0, MouseEvent.NOBUTTON, false);
        }
        if (target != null) {
            send(w, target, MouseEvent.MOUSE_ENTERED, x, y, 0, MouseEvent.NOBUTTON, false);
        }
    }

    /// Shows the component popup menu that applies at `hit`, if any.
    private static boolean showPopup(Window w, Component hit, int x, int y) {
        for (Component c = hit; c != null; c = c.getParent()) {
            if (c instanceof JComponent) {
                JPopupMenu menu = ((JComponent) c).getComponentPopupMenu();
                if (menu != null) {
                    if (!c.isEnabled()) {
                        return false;
                    }
                    int[] p = local(w, c, x, y);
                    menu.show(c, p[0], p[1]);
                    return true;
                }
            }
            if (c.cn1WantsMouse() && c != hit) {
                return false;
            }
        }
        return false;
    }

    /// As [#pointerEvent]; kept for hosts that do not care whether the
    /// event was swallowed.
    public static void pointer(Window w, int id, int deviceX, int deviceY) {
        pointerEvent(w, id, deviceX, deviceY);
    }

    /// Delivers pointer input that reached the host of `w`. `id` is one of
    /// `MOUSE_PRESSED`, `MOUSE_RELEASED`, `MOUSE_DRAGGED` and
    /// `MOUSE_MOVED`; the position is in display coordinates.
    ///
    /// Answers `true` when the layer used the event up -- it closed a
    /// popup menu -- and the host must keep it from the Codename One
    /// components under the pointer.
    public static boolean pointerEvent(Window w, int id, int deviceX, int deviceY) {
        com.codename1.ui.Component rp = w.cn1PeerOrNull();
        if (rp == null) {
            return false;
        }
        if (Display.isInitialized() && Display.getInstance().isScrollWheeling()) {
            // A desktop port scrolls with the wheel by sending a press, a
            // drag and a release; those are not the mouse's buttons.
            return false;
        }
        int x = Units.toLogical(deviceX - rp.getAbsoluteX());
        int y = Units.toLogical(deviceY - rp.getAbsoluteY());
        switch (id) {
            case MouseEvent.MOUSE_PRESSED: {
                Component hit = w.findComponentAt(x, y);
                MenuSelectionManager menus = MenuSelectionManager.defaultManager();
                if (menus.cn1PopupShowing() && !menus.cn1Inside(hit)) {
                    menus.clearSelectedPath();
                    pressTarget = null;
                    swallowed = true;
                    return true;
                }
                swallowed = false;
                longPressed = false;
                popupShown = false;
                JInternalFrame.cn1PressedIn(hit);
                Component target = mouseTarget(hit);
                hover(w, target, x, y);
                int button = input.button();
                long now = System.currentTimeMillis();
                int slop = Math.max(2, (int) (4 * Units.scale()));
                // As on a desktop the count belongs to the place and the
                // button, not to the component that happens to listen.
                boolean again = target != null && lastClickTarget != null && button == lastClickButton
                        && now - lastClickTime < CLICK_INTERVAL && Math.abs(deviceX - pressX) <= slop
                        && Math.abs(deviceY - pressY) <= slop;
                clickCount = again ? clickCount + 1 : 1;
                pressTarget = target;
                pressX = deviceX;
                pressY = deviceY;
                pressButton = button;
                dragged = false;
                boolean trigger = button == MouseEvent.BUTTON3;
                if (target != null) {
                    send(w, target, id, x, y, clickCount, button, trigger);
                }
                if (trigger) {
                    popupShown = showPopup(w, hit, x, y);
                }
                return false;
            }
            case MouseEvent.MOUSE_DRAGGED: {
                if (swallowed) {
                    return true;
                }
                int slop = Math.max(2, (int) (3 * Units.scale()));
                if (Math.abs(deviceX - pressX) > slop || Math.abs(deviceY - pressY) > slop) {
                    dragged = true;
                }
                if (pressTarget != null) {
                    send(w, pressTarget, id, x, y, 0, pressButton, false);
                }
                return false;
            }
            case MouseEvent.MOUSE_RELEASED: {
                if (swallowed) {
                    swallowed = false;
                    return true;
                }
                Component target = pressTarget;
                pressTarget = null;
                boolean trigger = longPressed;
                longPressed = false;
                if (target != null) {
                    send(w, target, id, x, y, clickCount, pressButton, trigger);
                    if (!dragged && !trigger) {
                        lastClickTime = System.currentTimeMillis();
                        lastClickTarget = target;
                        lastClickButton = pressButton;
                        send(w, target, MouseEvent.MOUSE_CLICKED, x, y, clickCount, pressButton, false);
                    }
                }
                if (trigger && !popupShown) {
                    showPopup(w, w.findComponentAt(x, y), x, y);
                }
                return false;
            }
            case MouseEvent.MOUSE_MOVED: {
                Component target = mouseTarget(w.findComponentAt(x, y));
                hover(w, target, x, y);
                if (target != null) {
                    send(w, target, id, x, y, 0, MouseEvent.NOBUTTON, false);
                }
                return false;
            }
            default:
                return false;
        }
    }

    /// The pointer left the host of `w`: the component it was over gets
    /// mouse exited.
    public static void pointerExit(Window w) {
        if (hoverTarget != null && (hoverTarget == w || w.isAncestorOf(hoverTarget))) {
            Component old = hoverTarget;
            hoverTarget = null;
            send(w, old, MouseEvent.MOUSE_EXITED, -1, -1, 0, MouseEvent.NOBUTTON, false);
        }
    }

    /// The host of `w` reported a long press at a display position. With
    /// a finger or a stylus that is a popup trigger: the component popup
    /// menu under it shows at once, and the release that follows is
    /// delivered as a popup trigger and makes no click.
    public static void longPress(Window w, int deviceX, int deviceY) {
        com.codename1.ui.Component rp = w.cn1PeerOrNull();
        if (rp == null || !input.touch() || swallowed || dragged) {
            return;
        }
        longPressed = true;
        int x = Units.toLogical(deviceX - rp.getAbsoluteX());
        int y = Units.toLogical(deviceY - rp.getAbsoluteY());
        popupShown = showPopup(w, w.findComponentAt(x, y), x, y);
    }

    /// Delivers a wheel movement at a display position: `rotation` clicks,
    /// positive toward the user. Answers whether a listener received it.
    public static boolean wheel(Window w, int deviceX, int deviceY, int rotation) {
        return wheel(w, deviceX, deviceY, rotation, 0);
    }

    /// As [#wheel(Window, int, int, int)] with extra extended modifiers,
    /// which is how a horizontal movement is told apart: it carries
    /// `SHIFT_DOWN_MASK`, as it does on the desktop.
    ///
    /// The event goes to the nearest component from the one under the
    /// pointer up that has a `MouseWheelListener`. The answer is `true`
    /// when there was one, enabled or not: the wheel is that component's
    /// then, and nothing else may act on it.
    public static boolean wheel(Window w, int deviceX, int deviceY, int rotation, int extraModifiers) {
        com.codename1.ui.Component rp = w.cn1PeerOrNull();
        if (rp == null || rotation == 0) {
            return false;
        }
        int x = Units.toLogical(deviceX - rp.getAbsoluteX());
        int y = Units.toLogical(deviceY - rp.getAbsoluteY());
        Component target = w.findComponentAt(x, y);
        while (target != null && target.getMouseWheelListeners().length == 0) {
            target = target.getParent();
        }
        if (target == null) {
            return false;
        }
        if (target.isEnabled()) {
            int[] p = local(w, target, x, y);
            target.dispatchEvent(new MouseWheelEvent(target, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(),
                    both(input.modifiers() | extraModifiers), p[0], p[1], 0, false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotation));
        }
        return true;
    }

    /// Whether the wheel at a display position is over a scroll pane that
    /// was told not to scroll with it.
    private static boolean wheelScrollingOff(Window w, int deviceX, int deviceY) {
        com.codename1.ui.Component rp = w.cn1PeerOrNull();
        if (rp == null) {
            return false;
        }
        Component hit = w.findComponentAt(Units.toLogical(deviceX - rp.getAbsoluteX()),
                Units.toLogical(deviceY - rp.getAbsoluteY()));
        for (Component c = hit; c != null; c = c.getParent()) {
            if (c instanceof JScrollPane) {
                return !((JScrollPane) c).isWheelScrollingEnabled();
            }
        }
        return false;
    }

    /// Delivers a Codename One wheel event. Codename One's vertical delta
    /// is positive when the wheel turned away from the user, the opposite
    /// of AWT's rotation, and so is its horizontal delta.
    ///
    /// Codename One fires its wheel listeners before it scrolls anything,
    /// and scrolls the nearest scrollable container itself when nobody
    /// consumed the event. So the event is consumed here exactly when the
    /// scrolling must not happen: a `MouseWheelListener` of the application
    /// received it -- on the desktop such a listener replaces the scroll
    /// pane's own handling, and an application that zooms with the wheel
    /// must not see the content scroll as well -- or the scroll pane under
    /// the pointer has wheel scrolling turned off. Left alone, the wheel
    /// scrolls the viewport once.
    public static void wheel(Window w, com.codename1.ui.events.WheelEvent we) {
        int dy = we.getDeltaY();
        int dx = we.getDeltaX();
        boolean taken = false;
        if (dy != 0) {
            taken = wheel(w, we.getX(), we.getY(), dy > 0 ? -1 : 1, 0);
        } else if (dx != 0) {
            taken = wheel(w, we.getX(), we.getY(), dx > 0 ? -1 : 1, InputEvent.SHIFT_DOWN_MASK);
        }
        if (taken || wheelScrollingOff(w, we.getX(), we.getY())) {
            we.consume();
        }
    }

    // ------------------------------------------------------------ keys

    /// Delivers a key that reached the host of `w`: key pressed followed
    /// by key typed where the key has a character and neither control,
    /// alt nor meta is held, or key released. A key held down arrives as
    /// further key pressed events.
    ///
    /// Answers whether the key is spoken for, in which case the host does
    /// not hand it to the form as well. Tab is: the focus has moved by
    /// the traversal policy, and the form of a desktop given the same key
    /// moves the focus of its own components a second time, by an order
    /// of its own.
    public static boolean key(Window w, boolean pressed, int code) {
        int vk = KeyMap.virtualKey(code);
        MenuSelectionManager menus = MenuSelectionManager.defaultManager();
        if (vk == KeyEvent.VK_ESCAPE && menus.cn1PopupShowing()) {
            if (pressed) {
                menus.clearSelectedPath();
            }
            return false;
        }
        Container c = w;
        Component target = focusOwner != null && (focusOwner == w || c.isAncestorOf(focusOwner)) ? focusOwner : w;
        long now = System.currentTimeMillis();
        char ch = KeyMap.keyChar(code);
        int mods = input.modifiers();
        KeyEvent e = new KeyEvent(target, pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED, now, mods, vk, ch);
        deliver(target, e);
        if (!pressed) {
            return vk == KeyEvent.VK_TAB;
        }
        boolean command = (mods & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK
                | InputEvent.META_DOWN_MASK)) != 0;
        if (ch != KeyEvent.CHAR_UNDEFINED && !command) {
            deliver(target, new KeyEvent(target, KeyEvent.KEY_TYPED, now, mods, KeyEvent.VK_UNDEFINED, ch));
        }
        if (!e.isConsumed() && vk == KeyEvent.VK_TAB && !command && target.getFocusTraversalKeysEnabled()) {
            KeyboardFocusManager m = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0) {
                m.focusPreviousComponent(target);
            } else {
                m.focusNextComponent(target);
            }
        }
        return vk == KeyEvent.VK_TAB;
    }

    /// Hands the event to the target, and to the key bindings when the
    /// target is not a Swing component, which looks them up itself.
    private static void deliver(Component target, KeyEvent e) {
        target.dispatchEvent(e);
        if (!e.isConsumed() && !(target instanceof JComponent)) {
            SwingUtilities.processKeyBindings(e);
        }
    }
}
