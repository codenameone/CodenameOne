/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.ui.Display;
import java.util.ArrayList;
import java.util.List;

/// On-screen controls for touch devices: an optional analog `VirtualJoystick` plus
/// any number of `VirtualButton`s. The framework draws them over the game and routes
/// touches into the view's `GameInput`, so a touch game reads input exactly like a
/// keyboard game -- `GameInput#isGameKeyDown(int)`, `GameInput#isKeyDown(int)` and
/// the joystick's analog `GameInput#getAxisX()` / `getAxisY()` all work whether the
/// player is on a phone or a desktop.
///
/// Get the instance from `GameView#getControls()` and add the controls you want,
/// placing them in the view's pixel coordinates (origin at the top left):
///
/// ```java
/// int fire = Display.getInstance().getKeyCode(Display.GAME_FIRE);
/// getControls().addJoystick(140, getHeight() - 140, 90);
/// getControls().addButton(fire, getWidth() - 120, getHeight() - 120, 55).setLabel("A");
/// ```
///
/// Multi-touch is supported, so the player can steer with the stick and press a
/// button at the same time.
public class TouchControls {
    private final GameInput input;
    private VirtualJoystick joystick;
    private final List buttons = new ArrayList();
    private boolean visible = true;

    // current digital state of the joystick's four directions
    private boolean left;
    private boolean right;
    private boolean up;
    private boolean down;

    TouchControls(GameInput input) {
        this.input = input;
    }

    /// Adds (or replaces) the analog joystick at the given center and radius. Returns
    /// it so you can tune the dead zone etc.
    public VirtualJoystick addJoystick(float centerX, float centerY, float radius) {
        joystick = new VirtualJoystick(centerX, centerY, radius);
        return joystick;
    }

    /// Adds a button mapped to the given key code. Use
    /// `com.codename1.ui.Display#getKeyCode(int)` to map a game action.
    public VirtualButton addButton(int keyCode, float centerX, float centerY, float radius) {
        VirtualButton b = new VirtualButton(keyCode, centerX, centerY, radius);
        buttons.add(b);
        return b;
    }

    /// Whether the controls are drawn and active. Hide them on platforms with a real
    /// keyboard if you prefer.
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (!visible) {
            reset();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public VirtualJoystick getJoystick() {
        return joystick;
    }

    int getButtonCount() {
        return buttons.size();
    }

    VirtualButton getButton(int i) {
        return (VirtualButton) buttons.get(i);
    }

    boolean hasControls() {
        return joystick != null || !buttons.isEmpty();
    }

    /// Routes the current set of touch points (view-local pixels) into the controls
    /// and on into `GameInput`. Called by `GameView` for every pointer event.
    void onTouches(int[] xs, int[] ys, boolean anyDown) {
        if (!visible) {
            return;
        }
        if (joystick != null) {
            if (joystick.isActive()) {
                int idx = anyDown ? nearestTouch(xs, ys, joystick.getKnobX(), joystick.getKnobY()) : -1;
                if (idx >= 0) {
                    joystick.drag(xs[idx], ys[idx]);
                } else {
                    joystick.release();
                }
            } else if (anyDown) {
                int idx = firstTouchIn(xs, ys, joystick);
                if (idx >= 0) {
                    joystick.press(xs[idx], ys[idx]);
                }
            }
            applyJoystick();
        }
        for (Object o : buttons) {
            VirtualButton b = (VirtualButton) o;
            boolean now = anyDown && anyTouchIn(xs, ys, b);
            if (now != b.isPressed()) {
                b.setPressed(now);
                if (now) {
                    input.keyDown(b.getKeyCode());
                } else {
                    input.keyUp(b.getKeyCode());
                }
            }
        }
    }

    private void applyJoystick() {
        float dz = joystick.getDeadZone();
        input.setAxis(joystick.getAxisX(), joystick.getAxisY());
        left = setDir(left, Display.GAME_LEFT, joystick.getAxisX() <= -dz);
        right = setDir(right, Display.GAME_RIGHT, joystick.getAxisX() >= dz);
        up = setDir(up, Display.GAME_UP, joystick.getAxisY() <= -dz);
        down = setDir(down, Display.GAME_DOWN, joystick.getAxisY() >= dz);
    }

    private boolean setDir(boolean wasDown, int gameAction, boolean nowDown) {
        if (nowDown == wasDown) {
            return wasDown;
        }
        int keyCode = Display.getInstance().getKeyCode(gameAction);
        if (nowDown) {
            input.keyDown(keyCode);
        } else {
            input.keyUp(keyCode);
        }
        return nowDown;
    }

    private void reset() {
        if (joystick != null) {
            joystick.release();
            applyJoystick();
        }
        for (Object o : buttons) {
            VirtualButton b = (VirtualButton) o;
            if (b.isPressed()) {
                b.setPressed(false);
                input.keyUp(b.getKeyCode());
            }
        }
    }

    private int firstTouchIn(int[] xs, int[] ys, VirtualJoystick j) {
        for (int i = 0; i < xs.length; i++) {
            if (j.contains(xs[i], ys[i])) {
                return i;
            }
        }
        return -1;
    }

    private boolean anyTouchIn(int[] xs, int[] ys, VirtualButton b) {
        for (int i = 0; i < xs.length; i++) {
            if (b.contains(xs[i], ys[i])) {
                return true;
            }
        }
        return false;
    }

    private int nearestTouch(int[] xs, int[] ys, float px, float py) {
        int best = -1;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < xs.length; i++) {
            float dx = xs[i] - px;
            float dy = ys[i] - py;
            float d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }
}
