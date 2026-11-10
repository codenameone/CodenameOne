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

/// One connected game controller.
///
/// Buttons and axes use the standard layout every modern controller is mapped
/// to -- the Xbox layout, which is also what Steam Input presents for a Steam
/// Deck, a Steam Controller or a PlayStation pad. Query it with
/// `#isButtonDown(int)` and `#getAxis(int)`, or be told about changes through a
/// `GamepadListener` registered with `Gamepads`.
///
/// Instances are owned by `Gamepads` and live for as long as the controller is
/// connected; `#isConnected()` turns false once it is gone. All state is written
/// and read on the EDT.
public final class Gamepad {
    /// The bottom face button (A on Xbox, Cross on PlayStation).
    public static final int BUTTON_A = 0;
    /// The right face button (B on Xbox, Circle on PlayStation).
    public static final int BUTTON_B = 1;
    /// The left face button (X on Xbox, Square on PlayStation).
    public static final int BUTTON_X = 2;
    /// The top face button (Y on Xbox, Triangle on PlayStation).
    public static final int BUTTON_Y = 3;
    /// The left shoulder button.
    public static final int BUTTON_LEFT_SHOULDER = 4;
    /// The right shoulder button.
    public static final int BUTTON_RIGHT_SHOULDER = 5;
    /// The back / view / select button.
    public static final int BUTTON_BACK = 6;
    /// The start / menu button.
    public static final int BUTTON_START = 7;
    /// The guide / home button. Steam keeps this one for itself while a game runs
    /// under it, so do not rely on receiving it.
    public static final int BUTTON_GUIDE = 8;
    /// Pressing the left stick.
    public static final int BUTTON_LEFT_STICK = 9;
    /// Pressing the right stick.
    public static final int BUTTON_RIGHT_STICK = 10;
    /// D-pad up.
    public static final int BUTTON_DPAD_UP = 11;
    /// D-pad down.
    public static final int BUTTON_DPAD_DOWN = 12;
    /// D-pad left.
    public static final int BUTTON_DPAD_LEFT = 13;
    /// D-pad right.
    public static final int BUTTON_DPAD_RIGHT = 14;
    /// The left trigger, reported as a button once pulled past halfway.
    public static final int BUTTON_LEFT_TRIGGER = 15;
    /// The right trigger, reported as a button once pulled past halfway.
    public static final int BUTTON_RIGHT_TRIGGER = 16;
    /// The number of buttons.
    public static final int BUTTON_COUNT = 17;

    /// The left stick, -1 (left) to 1 (right).
    public static final int AXIS_LEFT_X = 0;
    /// The left stick, -1 (up) to 1 (down).
    public static final int AXIS_LEFT_Y = 1;
    /// The right stick, -1 (left) to 1 (right).
    public static final int AXIS_RIGHT_X = 2;
    /// The right stick, -1 (up) to 1 (down).
    public static final int AXIS_RIGHT_Y = 3;
    /// The left trigger, 0 (released) to 1 (fully pulled).
    public static final int AXIS_LEFT_TRIGGER = 4;
    /// The right trigger, 0 (released) to 1 (fully pulled).
    public static final int AXIS_RIGHT_TRIGGER = 5;
    /// The number of axes.
    public static final int AXIS_COUNT = 6;

    private final int index;
    private final String name;
    private final boolean[] buttons = new boolean[BUTTON_COUNT];
    private final float[] axes = new float[AXIS_COUNT];
    private boolean connected = true;
    private float deadZone = 0.15f;

    Gamepad(int index, String name) {
        this.index = index;
        this.name = name == null ? "" : name;
    }

    /// The slot this controller occupies, 0 for the first one connected. A slot
    /// freed by a disconnect is reused by the next controller.
    public int getIndex() {
        return index;
    }

    /// The name the device reports, for display only; it may be empty.
    public String getName() {
        return name;
    }

    /// False once the controller has been disconnected.
    public boolean isConnected() {
        return connected;
    }

    void setConnected(boolean connected) {
        this.connected = connected;
    }

    /// True while the button is held.
    ///
    /// #### Parameters
    ///
    /// - `button`: one of the `BUTTON_*` constants
    public boolean isButtonDown(int button) {
        return button >= 0 && button < BUTTON_COUNT && buttons[button];
    }

    /// The axis value with the dead zone applied: a stick inside the dead zone
    /// reads exactly 0, and the remaining travel is rescaled so the value still
    /// reaches 1 at the edge. Triggers are not subject to the dead zone.
    ///
    /// #### Parameters
    ///
    /// - `axis`: one of the `AXIS_*` constants
    public float getAxis(int axis) {
        if (axis < 0 || axis >= AXIS_COUNT) {
            return 0;
        }
        float v = axes[axis];
        if (axis == AXIS_LEFT_TRIGGER || axis == AXIS_RIGHT_TRIGGER) {
            return v;
        }
        return applyDeadZone(v, deadZone);
    }

    /// The axis value exactly as the device reported it, with no dead zone.
    public float getRawAxis(int axis) {
        if (axis < 0 || axis >= AXIS_COUNT) {
            return 0;
        }
        return axes[axis];
    }

    /// The stick dead zone, 0.15 by default.
    public float getDeadZone() {
        return deadZone;
    }

    /// Sets the stick dead zone, clamped to 0 .. 0.9. Worn sticks rest a little
    /// off center; the dead zone stops that from reading as movement.
    public void setDeadZone(float deadZone) {
        if (deadZone < 0) {
            deadZone = 0;
        } else if (deadZone > 0.9f) {
            deadZone = 0.9f;
        }
        this.deadZone = deadZone;
    }

    /// Rumbles the controller, on ports and controllers that support it; it is
    /// silently ignored everywhere else.
    ///
    /// #### Parameters
    ///
    /// - `strong`: the low frequency (heavy) motor, 0 to 1
    ///
    /// - `weak`: the high frequency (light) motor, 0 to 1
    ///
    /// - `durationMillis`: how long the rumble lasts
    public void rumble(float strong, float weak, int durationMillis) {
        if (connected) {
            com.codename1.ui.Display.getInstance().rumbleGamepad(index,
                    clamp01(strong), clamp01(weak), durationMillis);
        }
    }

    boolean setButton(int button, boolean down) {
        if (button < 0 || button >= BUTTON_COUNT || buttons[button] == down) {
            return false;
        }
        buttons[button] = down;
        return true;
    }

    void setAxis(int axis, float value) {
        if (axis < 0 || axis >= AXIS_COUNT) {
            return;
        }
        if (value > 1) {
            value = 1;
        } else if (value < -1) {
            value = -1;
        }
        axes[axis] = value;
    }

    static float applyDeadZone(float v, float deadZone) {
        float mag = v < 0 ? -v : v;
        if (mag <= deadZone) {
            return 0;
        }
        float scaled = (mag - deadZone) / (1 - deadZone);
        if (scaled > 1) {
            scaled = 1;
        }
        return v < 0 ? -scaled : scaled;
    }

    private static float clamp01(float v) {
        if (v < 0) {
            return 0;
        }
        return v > 1 ? 1 : v;
    }
}
