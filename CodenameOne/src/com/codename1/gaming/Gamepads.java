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
package com.codename1.gaming;

import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import java.util.ArrayList;

/// The connected game controllers.
///
/// Ports that read controllers report them here; applications ask for them with
/// `#getGamepads()` / `#getGamepad(int)` and listen with
/// `#addListener(GamepadListener)`. Where no port reports any controller the
/// list is simply empty.
///
/// **Focus navigation.** Unless turned off with `#setFocusNavigation(boolean)`, a
/// button no listener consumed drives the user interface the way the keyboard
/// does: the D-pad and the left stick move focus, A presses the focused
/// component and B goes back. An ordinary form is therefore usable from a couch
/// with no code written for it.
///
/// Everything here runs on the EDT. Ports call the `report*` methods from it.
public final class Gamepads {
    private static final ArrayList PADS = new ArrayList();
    private static final ArrayList LISTENERS = new ArrayList();
    private static boolean focusNavigation = true;

    // Stick-as-D-pad hysteresis: press past PRESS, release under RELEASE, so a
    // stick resting near the threshold does not chatter.
    static final float STICK_PRESS = 0.6f;
    static final float STICK_RELEASE = 0.4f;

    // Which direction each pad's left stick currently holds, as a D-pad button
    // or -1, one entry per axis (x, y), indexed by pad slot.
    private static int[] stickX = new int[0];
    private static int[] stickY = new int[0];

    private Gamepads() {
    }

    /// The connected controllers, in slot order. A new array every call.
    public static Gamepad[] getGamepads() {
        ArrayList out = new ArrayList();
        for (Object p : PADS) {
            if (p != null) {
                out.add(p);
            }
        }
        Gamepad[] r = new Gamepad[out.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = (Gamepad) out.get(i);
        }
        return r;
    }

    /// The controller in the given slot, or null when that slot is empty.
    public static Gamepad getGamepad(int index) {
        if (index < 0 || index >= PADS.size()) {
            return null;
        }
        return (Gamepad) PADS.get(index);
    }

    /// True when at least one controller is connected.
    public static boolean isAnyConnected() {
        for (Object p : PADS) {
            if (p != null) {
                return true;
            }
        }
        return false;
    }

    /// Registers a listener.
    public static void addListener(GamepadListener l) {
        if (l != null && !LISTENERS.contains(l)) {
            LISTENERS.add(l);
        }
    }

    /// Unregisters a listener.
    public static void removeListener(GamepadListener l) {
        LISTENERS.remove(l);
    }

    /// Whether unconsumed buttons drive focus navigation; on by default.
    public static boolean isFocusNavigation() {
        return focusNavigation;
    }

    /// Turns focus navigation from controller buttons on or off. A game that reads
    /// the controller itself and has no focusable components may turn it off.
    public static void setFocusNavigation(boolean b) {
        focusNavigation = b;
    }

    /// Called by a port when a controller connects; `index` is the slot it chose.
    /// Returns the controller object.
    public static Gamepad reportConnected(int index, String name) {
        if (index < 0) {
            return null;
        }
        while (PADS.size() <= index) {
            PADS.add(null);
        }
        Gamepad existing = (Gamepad) PADS.get(index);
        if (existing != null) {
            return existing;
        }
        Gamepad pad = new Gamepad(index, name);
        PADS.set(index, pad);
        ensureStickSlots(index);
        stickX[index] = -1;
        stickY[index] = -1;
        Object[] ls = LISTENERS.toArray();
        for (Object l : ls) {
            ((GamepadListener) l).gamepadConnected(pad);
        }
        return pad;
    }

    /// Called by a port when a controller disconnects. Releases every button it
    /// still held first, so nothing is left stuck down.
    public static void reportDisconnected(int index) {
        Gamepad pad = getGamepad(index);
        if (pad == null) {
            return;
        }
        for (int b = 0; b < Gamepad.BUTTON_COUNT; b++) {
            if (pad.isButtonDown(b)) {
                reportButton(index, b, false);
            }
        }
        for (int a = 0; a < Gamepad.AXIS_COUNT; a++) {
            reportAxis(index, a, 0);
        }
        pad.setConnected(false);
        PADS.set(index, null);
        Object[] ls = LISTENERS.toArray();
        for (Object l : ls) {
            ((GamepadListener) l).gamepadDisconnected(pad);
        }
    }

    /// Called by a port when a button changes.
    public static void reportButton(int index, int button, boolean down) {
        reportButton(index, button, down, true);
    }

    /// Called by a port when a button changes. A port that already delivers the
    /// controller to the user interface as keys (the Xbox build of the Windows
    /// port does) passes `navigate` false, so the press is not delivered twice.
    public static void reportButton(int index, int button, boolean down, boolean navigate) {
        Gamepad pad = getGamepad(index);
        if (pad == null || !pad.setButton(button, down)) {
            return;
        }
        boolean consumed = false;
        Object[] ls = LISTENERS.toArray();
        for (Object l : ls) {
            if (((GamepadListener) l).gamepadButton(pad, button, down)) {
                consumed = true;
            }
        }
        if (navigate && !consumed && focusNavigation) {
            navigate(button, down);
        }
    }

    /// Called by a port when an axis changes: sticks -1 .. 1, triggers 0 .. 1.
    /// The triggers are also reported as `Gamepad#BUTTON_LEFT_TRIGGER` and
    /// `Gamepad#BUTTON_RIGHT_TRIGGER` past halfway, and the left stick as the
    /// D-pad for focus navigation.
    public static void reportAxis(int index, int axis, float value) {
        Gamepad pad = getGamepad(index);
        if (pad == null) {
            return;
        }
        pad.setAxis(axis, value);
        float v = pad.getRawAxis(axis);
        if (axis == Gamepad.AXIS_LEFT_TRIGGER) {
            reportButton(index, Gamepad.BUTTON_LEFT_TRIGGER, v > 0.5f);
        } else if (axis == Gamepad.AXIS_RIGHT_TRIGGER) {
            reportButton(index, Gamepad.BUTTON_RIGHT_TRIGGER, v > 0.5f);
        } else if (axis == Gamepad.AXIS_LEFT_X) {
            stickX[index] = stickDirection(stickX[index], v,
                    Gamepad.BUTTON_DPAD_LEFT, Gamepad.BUTTON_DPAD_RIGHT);
        } else if (axis == Gamepad.AXIS_LEFT_Y) {
            stickY[index] = stickDirection(stickY[index], v,
                    Gamepad.BUTTON_DPAD_UP, Gamepad.BUTTON_DPAD_DOWN);
        }
    }

    /// The stick held as a D-pad direction, with hysteresis. Only drives focus
    /// navigation: it never sets the pad's D-pad buttons, which stay what the
    /// device reports.
    private static int stickDirection(int current, float v, int negative, int positive) {
        int next = current;
        if (current == -1) {
            if (v <= -STICK_PRESS) {
                next = negative;
            } else if (v >= STICK_PRESS) {
                next = positive;
            }
        } else {
            float along = current == negative ? -v : v;
            if (along < STICK_RELEASE) {
                next = -1;
                if (v <= -STICK_PRESS) {
                    next = negative;
                } else if (v >= STICK_PRESS) {
                    next = positive;
                }
            }
        }
        if (next != current && focusNavigation) {
            if (current != -1) {
                navigate(current, false);
            }
            if (next != -1) {
                navigate(next, true);
            }
        }
        return next;
    }

    private static void ensureStickSlots(int index) {
        if (stickX.length <= index) {
            int[] nx = new int[index + 1];
            int[] ny = new int[index + 1];
            System.arraycopy(stickX, 0, nx, 0, stickX.length);
            System.arraycopy(stickY, 0, ny, 0, stickY.length);
            stickX = nx;
            stickY = ny;
        }
    }

    /// The Codename One key code a button navigates with, or 0 for none.
    static int navigationKeyCode(int button) {
        Display d = Display.getInstance();
        switch (button) {
            case Gamepad.BUTTON_DPAD_UP:
                return d.getKeyCode(Display.GAME_UP);
            case Gamepad.BUTTON_DPAD_DOWN:
                return d.getKeyCode(Display.GAME_DOWN);
            case Gamepad.BUTTON_DPAD_LEFT:
                return d.getKeyCode(Display.GAME_LEFT);
            case Gamepad.BUTTON_DPAD_RIGHT:
                return d.getKeyCode(Display.GAME_RIGHT);
            case Gamepad.BUTTON_A:
                return d.getKeyCode(Display.GAME_FIRE);
            default:
                return 0;
        }
    }

    private static void navigate(int button, boolean down) {
        if (button == Gamepad.BUTTON_B) {
            if (!down) {
                goBack();
            }
            return;
        }
        int code = navigationKeyCode(button);
        // 0 and -1 are what getKeyCode answers for a game action the port has
        // no key for.
        if (code == 0 || code == -1) {
            return;
        }
        Display d = Display.getInstance();
        if (down) {
            d.keyPressed(code);
        } else {
            d.keyReleased(code);
        }
    }

    private static void goBack() {
        Form f = Display.getInstance().getCurrent();
        if (f == null) {
            return;
        }
        Command back = f.getBackCommand();
        if (back != null) {
            f.dispatchCommand(back, new ActionEvent(back, ActionEvent.Type.Command));
        }
    }
}
