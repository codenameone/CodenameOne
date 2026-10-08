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
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.FocusEvent;
import com.codename1.desktopcompat.java.awt.event.InputEvent;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.awt.event.MouseWheelEvent;
import com.codename1.ui.Display;
import com.codename1.ui.events.PointerEvent;
import com.codename1.ui.plaf.UIManager;

/// Turns Codename One input into AWT events.
///
/// Pointer and key input enters at the form of a window. A pointer event
/// goes to the deepest visible component under the pointer that wants
/// mouse events, or the nearest ancestor that does; the component pressed
/// keeps receiving the drags and the release. Key events go to the focus
/// owner, or to the window when no component of it owns the focus.
public final class EventBridge {

    private static final int CLICK_INTERVAL = 500;

    private static Component focusOwner;
    private static Component pressTarget;
    private static Component hoverTarget;
    private static long lastClickTime;
    private static int clickCount;
    private static int pressX;
    private static int pressY;
    private static int pressButton;
    private static boolean dragged;

    private EventBridge() {
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
                    && p.getComponentForm() != null) {
                p.requestFocus();
            }
        }
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

    private static Component mouseTarget(Component hit) {
        Component c = hit;
        while (c != null && !c.cn1WantsMouse()) {
            c = c.getParent();
        }
        return c;
    }

    private static int button() {
        if (!Display.isInitialized()) {
            return MouseEvent.BUTTON1;
        }
        PointerEvent pe = Display.getInstance().getCurrentPointerEvent();
        if (pe == null) {
            return MouseEvent.BUTTON1;
        }
        switch (pe.getButton()) {
            case PointerEvent.BUTTON_SECONDARY:
                return MouseEvent.BUTTON3;
            case PointerEvent.BUTTON_MIDDLE:
                return MouseEvent.BUTTON2;
            default:
                return MouseEvent.BUTTON1;
        }
    }

    private static int keyModifiers() {
        if (!Display.isInitialized()) {
            return 0;
        }
        PointerEvent pe = Display.getInstance().getCurrentPointerEvent();
        int m = 0;
        if (pe != null) {
            if (pe.isShiftDown()) {
                m |= InputEvent.SHIFT_MASK;
            }
            if (pe.isControlDown()) {
                m |= InputEvent.CTRL_MASK;
            }
            if (pe.isAltDown()) {
                m |= InputEvent.ALT_MASK;
            }
            if (pe.isMetaDown()) {
                m |= InputEvent.META_MASK;
            }
        }
        return m;
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

    private static void send(Window w, Component target, int id, int x, int y, int clicks, int button) {
        int[] p = local(w, target, x, y);
        int mods = keyModifiers() | buttonMask(button);
        target.dispatchEvent(new MouseEvent(target, id, System.currentTimeMillis(), mods, p[0], p[1], clicks,
                id == MouseEvent.MOUSE_PRESSED && button == MouseEvent.BUTTON3, button));
    }

    private static void hover(Window w, Component target, int x, int y) {
        if (target == hoverTarget) {
            return;
        }
        Component old = hoverTarget;
        hoverTarget = target;
        if (old != null) {
            send(w, old, MouseEvent.MOUSE_EXITED, x, y, 0, MouseEvent.NOBUTTON);
        }
        if (target != null) {
            send(w, target, MouseEvent.MOUSE_ENTERED, x, y, 0, MouseEvent.NOBUTTON);
        }
    }

    /// Delivers pointer input that reached the form of `w`. `id` is one of
    /// `MOUSE_PRESSED`, `MOUSE_RELEASED`, `MOUSE_DRAGGED` and
    /// `MOUSE_MOVED`; the position is in display coordinates.
    public static void pointer(Window w, int id, int deviceX, int deviceY) {
        com.codename1.ui.Component rp = w.cn1PeerOrNull();
        if (rp == null) {
            return;
        }
        int x = Units.toLogical(deviceX - rp.getAbsoluteX());
        int y = Units.toLogical(deviceY - rp.getAbsoluteY());
        switch (id) {
            case MouseEvent.MOUSE_PRESSED: {
                Component target = mouseTarget(w.findComponentAt(x, y));
                hover(w, target, x, y);
                pressTarget = target;
                pressX = deviceX;
                pressY = deviceY;
                pressButton = button();
                dragged = false;
                if (target != null) {
                    long now = System.currentTimeMillis();
                    clickCount = now - lastClickTime < CLICK_INTERVAL
                            ? clickCount + 1 : 1;
                    send(w, target, id, x, y, clickCount, pressButton);
                }
                break;
            }
            case MouseEvent.MOUSE_DRAGGED: {
                int slop = Math.max(2, (int) (3 * Units.scale()));
                if (Math.abs(deviceX - pressX) > slop || Math.abs(deviceY - pressY) > slop) {
                    dragged = true;
                }
                if (pressTarget != null) {
                    send(w, pressTarget, id, x, y, 0, pressButton);
                }
                break;
            }
            case MouseEvent.MOUSE_RELEASED: {
                Component target = pressTarget;
                pressTarget = null;
                if (target != null) {
                    send(w, target, id, x, y, clickCount, pressButton);
                    if (!dragged) {
                        lastClickTime = System.currentTimeMillis();
                        send(w, target, MouseEvent.MOUSE_CLICKED, x, y, clickCount, pressButton);
                    }
                }
                break;
            }
            case MouseEvent.MOUSE_MOVED: {
                Component target = mouseTarget(w.findComponentAt(x, y));
                hover(w, target, x, y);
                if (target != null) {
                    send(w, target, id, x, y, 0, MouseEvent.NOBUTTON);
                }
                break;
            }
            default:
                break;
        }
    }

    /// Delivers a wheel movement at a display position: `rotation` clicks,
    /// positive toward the user.
    public static void wheel(Window w, int deviceX, int deviceY, int rotation) {
        com.codename1.ui.Component rp = w.cn1PeerOrNull();
        if (rp == null || rotation == 0) {
            return;
        }
        int x = Units.toLogical(deviceX - rp.getAbsoluteX());
        int y = Units.toLogical(deviceY - rp.getAbsoluteY());
        Component target = w.findComponentAt(x, y);
        while (target != null && target.getMouseWheelListeners().length == 0) {
            target = target.getParent();
        }
        if (target != null) {
            int[] p = local(w, target, x, y);
            target.dispatchEvent(new MouseWheelEvent(target, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(),
                    keyModifiers(), p[0], p[1], 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotation));
        }
    }

    // ------------------------------------------------------------ keys

    private static int virtualKey(int code) {
        if (code >= 'a' && code <= 'z') {
            return code - ('a' - 'A');
        }
        if ((code >= 'A' && code <= 'Z') || (code >= '0' && code <= '9')) {
            return code;
        }
        switch (code) {
            case ' ':
                return KeyEvent.VK_SPACE;
            case '\n':
            case '\r':
                return KeyEvent.VK_ENTER;
            case 8:
                return KeyEvent.VK_BACK_SPACE;
            case '\t':
                return KeyEvent.VK_TAB;
            case 27:
                return KeyEvent.VK_ESCAPE;
            case 127:
                return KeyEvent.VK_DELETE;
            default:
                break;
        }
        if (Display.isInitialized()) {
            switch (Display.getInstance().getGameAction(code)) {
                case Display.GAME_UP:
                    return KeyEvent.VK_UP;
                case Display.GAME_DOWN:
                    return KeyEvent.VK_DOWN;
                case Display.GAME_LEFT:
                    return KeyEvent.VK_LEFT;
                case Display.GAME_RIGHT:
                    return KeyEvent.VK_RIGHT;
                case Display.GAME_FIRE:
                    return KeyEvent.VK_ENTER;
                default:
                    break;
            }
        }
        return code > ' ' && code < 0xffff ? KeyEvent.getExtendedKeyCodeForChar(code) : KeyEvent.VK_UNDEFINED;
    }

    private static char keyChar(int code) {
        if (code == '\r') {
            return '\n';
        }
        if (code == '\n' || code == '\t' || code == 8 || code == 27 || code == 127 || (code >= ' ' && code < 0xffff)) {
            return (char) code;
        }
        return KeyEvent.CHAR_UNDEFINED;
    }

    /// Delivers a key that reached the form of `w`: key pressed followed
    /// by key typed where the key has a character, or key released.
    public static void key(Window w, boolean pressed, int code) {
        Container c = w;
        Component target = focusOwner != null && (focusOwner == w || c.isAncestorOf(focusOwner)) ? focusOwner : w;
        long now = System.currentTimeMillis();
        char ch = keyChar(code);
        target.dispatchEvent(new KeyEvent(target, pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED, now, 0,
                virtualKey(code), ch));
        if (pressed && ch != KeyEvent.CHAR_UNDEFINED) {
            target.dispatchEvent(new KeyEvent(target, KeyEvent.KEY_TYPED, now, 0, KeyEvent.VK_UNDEFINED, ch));
        }
    }
}
