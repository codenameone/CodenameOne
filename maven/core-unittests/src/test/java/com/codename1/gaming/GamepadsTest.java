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

import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link Gamepads}, {@link Gamepad} and the controller half of
/// {@link GameInput}: slots, dead zone, trigger buttons, listener consumption and
/// focus navigation from the D-pad and the left stick.
class GamepadsTest extends UITestBase {

    @AfterEach
    void disconnectAll() {
        for (int i = 0; i < 8; i++) {
            Gamepads.reportDisconnected(i);
        }
        Gamepads.setFocusNavigation(true);
    }

    private static class KeyForm extends Form {
        final List<Integer> pressed = new ArrayList<Integer>();
        final List<Integer> released = new ArrayList<Integer>();

        @Override
        public void keyPressed(int keyCode) {
            pressed.add(keyCode);
        }

        @Override
        public void keyReleased(int keyCode) {
            released.add(keyCode);
        }
    }

    private KeyForm showKeyForm() {
        KeyForm f = new KeyForm();
        f.show();
        flushSerialCalls();
        return f;
    }

    @Test
    void connectAndDisconnectFillSlots() {
        assertFalse(Gamepads.isAnyConnected());
        Gamepad a = Gamepads.reportConnected(0, "Pad A");
        Gamepads.reportConnected(2, "Pad C");
        assertTrue(Gamepads.isAnyConnected());
        assertEquals(2, Gamepads.getGamepads().length);
        assertSame(a, Gamepads.getGamepad(0));
        assertNull(Gamepads.getGamepad(1));
        assertEquals("Pad A", a.getName());
        assertSame(a, Gamepads.reportConnected(0, "again"), "a second connect of a slot is the same pad");
        Gamepads.reportDisconnected(0);
        assertFalse(a.isConnected());
        assertNull(Gamepads.getGamepad(0));
        assertEquals(1, Gamepads.getGamepads().length);
    }

    @Test
    void deadZoneZeroesAndRescales() {
        assertEquals(0f, Gamepad.applyDeadZone(0.1f, 0.15f), 0.0001);
        assertEquals(0f, Gamepad.applyDeadZone(-0.15f, 0.15f), 0.0001);
        assertEquals(1f, Gamepad.applyDeadZone(1f, 0.15f), 0.0001);
        assertEquals(-1f, Gamepad.applyDeadZone(-1f, 0.15f), 0.0001);
        assertEquals(0.5f, Gamepad.applyDeadZone(0.575f, 0.15f), 0.0001);

        Gamepad p = Gamepads.reportConnected(0, "p");
        Gamepads.reportAxis(0, Gamepad.AXIS_RIGHT_X, 0.1f);
        assertEquals(0f, p.getAxis(Gamepad.AXIS_RIGHT_X), 0.0001);
        assertEquals(0.1f, p.getRawAxis(Gamepad.AXIS_RIGHT_X), 0.0001);
        Gamepads.reportAxis(0, Gamepad.AXIS_LEFT_TRIGGER, 0.1f);
        assertEquals(0.1f, p.getAxis(Gamepad.AXIS_LEFT_TRIGGER), 0.0001, "triggers have no dead zone");
        Gamepads.reportAxis(0, Gamepad.AXIS_RIGHT_X, 7f);
        assertEquals(1f, p.getRawAxis(Gamepad.AXIS_RIGHT_X), 0.0001, "values are clamped");
    }

    @Test
    void triggerPastHalfwayIsAButton() {
        Gamepad p = Gamepads.reportConnected(0, "p");
        Gamepads.reportAxis(0, Gamepad.AXIS_RIGHT_TRIGGER, 0.4f);
        assertFalse(p.isButtonDown(Gamepad.BUTTON_RIGHT_TRIGGER));
        Gamepads.reportAxis(0, Gamepad.AXIS_RIGHT_TRIGGER, 0.8f);
        assertTrue(p.isButtonDown(Gamepad.BUTTON_RIGHT_TRIGGER));
        Gamepads.reportAxis(0, Gamepad.AXIS_RIGHT_TRIGGER, 0f);
        assertFalse(p.isButtonDown(Gamepad.BUTTON_RIGHT_TRIGGER));
    }

    @Test
    void listenerSeesButtonsAndCanConsume() {
        KeyForm f = showKeyForm();
        final int[] seen = new int[1];
        GamepadListener l = new GamepadListener() {
            public void gamepadConnected(Gamepad pad) {
            }

            public void gamepadDisconnected(Gamepad pad) {
            }

            public boolean gamepadButton(Gamepad pad, int button, boolean down) {
                seen[0]++;
                return true;
            }
        };
        Gamepads.addListener(l);
        try {
            Gamepads.reportConnected(0, "p");
            Gamepads.reportButton(0, Gamepad.BUTTON_DPAD_DOWN, true);
            Gamepads.reportButton(0, Gamepad.BUTTON_DPAD_DOWN, true);
            flushSerialCalls();
            assertEquals(1, seen[0], "a repeated report of the same state is not a change");
            assertTrue(f.pressed.isEmpty(), "a consumed button does not navigate");
        } finally {
            Gamepads.removeListener(l);
        }
    }

    @Test
    void dpadAndAButtonNavigate() {
        KeyForm f = showKeyForm();
        Gamepads.reportConnected(0, "p");
        Gamepads.reportButton(0, Gamepad.BUTTON_DPAD_DOWN, true);
        Gamepads.reportButton(0, Gamepad.BUTTON_DPAD_DOWN, false);
        Gamepads.reportButton(0, Gamepad.BUTTON_A, true);
        Gamepads.reportButton(0, Gamepad.BUTTON_A, false);
        flushSerialCalls();
        Display d = Display.getInstance();
        assertEquals(2, f.pressed.size());
        assertEquals(d.getKeyCode(Display.GAME_DOWN), f.pressed.get(0).intValue());
        assertEquals(d.getKeyCode(Display.GAME_FIRE), f.pressed.get(1).intValue());
        assertEquals(2, f.released.size());
    }

    @Test
    void stickNavigatesWithHysteresis() {
        KeyForm f = showKeyForm();
        Gamepads.reportConnected(0, "p");
        Gamepads.reportAxis(0, Gamepad.AXIS_LEFT_X, 0.5f);
        flushSerialCalls();
        assertTrue(f.pressed.isEmpty(), "under the press threshold");
        Gamepads.reportAxis(0, Gamepad.AXIS_LEFT_X, 0.7f);
        Gamepads.reportAxis(0, Gamepad.AXIS_LEFT_X, 0.5f);
        flushSerialCalls();
        assertEquals(1, f.pressed.size(), "between the thresholds the direction holds");
        assertTrue(f.released.isEmpty());
        Gamepads.reportAxis(0, Gamepad.AXIS_LEFT_X, 0.1f);
        flushSerialCalls();
        assertEquals(1, f.released.size());
        assertEquals(Display.getInstance().getKeyCode(Display.GAME_RIGHT), f.released.get(0).intValue());
    }

    @Test
    void navigationCanBeTurnedOff() {
        KeyForm f = showKeyForm();
        Gamepads.setFocusNavigation(false);
        Gamepads.reportConnected(0, "p");
        Gamepads.reportButton(0, Gamepad.BUTTON_DPAD_UP, true);
        Gamepads.reportAxis(0, Gamepad.AXIS_LEFT_Y, -1f);
        flushSerialCalls();
        assertTrue(f.pressed.isEmpty());
    }

    @Test
    void disconnectReleasesHeldButtons() {
        KeyForm f = showKeyForm();
        Gamepads.reportConnected(0, "p");
        Gamepads.reportButton(0, Gamepad.BUTTON_DPAD_LEFT, true);
        Gamepads.reportDisconnected(0);
        flushSerialCalls();
        assertEquals(1, f.released.size(), "a pad unplugged mid-press must not leave the key down");
    }

    @Test
    void gameInputReadsControllers() {
        GameInput in = new GameInput();
        Gamepads.reportConnected(0, "p");
        Gamepads.reportButton(0, Gamepad.BUTTON_X, true, false);
        assertTrue(in.isGamepadButtonDown(Gamepad.BUTTON_X));
        assertTrue(in.wasGamepadButtonPressed(Gamepad.BUTTON_X));
        in.clearFrameEdges();
        assertFalse(in.wasGamepadButtonPressed(Gamepad.BUTTON_X));
        Gamepads.reportButton(0, Gamepad.BUTTON_X, false, false);
        assertTrue(in.wasGamepadButtonReleased(Gamepad.BUTTON_X));

        Gamepads.reportAxis(0, Gamepad.AXIS_LEFT_X, 1f);
        assertEquals(1f, in.getAxisX(), 0.0001, "the left stick stands in for the joystick");
        in.setAxis(-0.5f, 0f);
        assertEquals(-0.5f, in.getAxisX(), 0.0001, "a held on-screen joystick wins");
    }
}
