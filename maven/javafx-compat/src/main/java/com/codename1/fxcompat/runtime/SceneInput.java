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
package com.codename1.fxcompat.runtime;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.events.PointerEvent;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/// Turns the pointer and key callbacks of the Codename One component that
/// hosts a scene into the scene's input, and answers the state of the
/// modifier keys.
///
/// Every host (form, dialog, desktop window) forwards its callbacks here
/// first. A method answers `true` when a JavaFX event filter consumed the
/// event, in which case the host does not pass it on to Codename One, so
/// the native component under the pointer never sees it. An event consumed
/// by a handler in the bubbling phase still reaches the native component:
/// a button pressed is a button pressed.
public final class SceneInput {

    private SceneInput() {
    }

    private static boolean display() {
        return Display.isInitialized();
    }

    /// Returns whether Shift is down.
    public static boolean shiftDown() {
        return display() && Display.getInstance().isShiftKeyDown();
    }

    /// Returns whether Control is down.
    public static boolean controlDown() {
        return display() && Display.getInstance().isControlKeyDown();
    }

    /// Returns whether Alt is down.
    public static boolean altDown() {
        return display() && Display.getInstance().isAltKeyDown();
    }

    /// Returns whether Meta (Command on a Mac) is down.
    public static boolean metaDown() {
        return display() && Display.getInstance().isMetaKeyDown();
    }

    /// Returns whether the platform's shortcut modifier is Meta rather
    /// than Control, which is so on Apple's platforms.
    public static boolean shortcutIsMeta() {
        if (!display()) {
            return false;
        }
        String name = Display.getInstance().getPlatformName();
        return "mac".equalsIgnoreCase(name) || "ios".equalsIgnoreCase(name);
    }

    private static MouseButton button() {
        PointerEvent e = display() ? Display.getInstance().getCurrentPointerEvent() : null;
        if (e == null) {
            return MouseButton.PRIMARY;
        }
        if (e.isSecondaryButton()) {
            return MouseButton.SECONDARY;
        }
        return e.isMiddleButton() ? MouseButton.MIDDLE : MouseButton.PRIMARY;
    }

    private static double sceneX(Component root, int x) {
        return Units.toLogical(x - root.getAbsoluteX());
    }

    private static double sceneY(Component root, int y) {
        return Units.toLogical(y - root.getAbsoluteY());
    }

    /// A pointer went down at a display position.
    public static boolean pressed(Scene scene, Component root, int x, int y) {
        return scene != null && root != null
                && scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, sceneX(root, x), sceneY(root, y), button());
    }

    /// A pointer moved while down.
    public static boolean dragged(Scene scene, Component root, int x, int y) {
        return scene != null && root != null
                && scene.cn1Pointer(MouseEvent.MOUSE_DRAGGED, sceneX(root, x), sceneY(root, y), button());
    }

    /// A pointer came up.
    public static boolean released(Scene scene, Component root, int x, int y) {
        return scene != null && root != null
                && scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, sceneX(root, x), sceneY(root, y), button());
    }

    /// A mouse moved with no button down.
    public static boolean hover(Scene scene, Component root, int x, int y) {
        return scene != null && root != null
                && scene.cn1Pointer(MouseEvent.MOUSE_MOVED, sceneX(root, x), sceneY(root, y), MouseButton.NONE);
    }

    /// A touch was held in place: a context menu request.
    public static boolean longPress(Scene scene, Component root, int x, int y) {
        return scene != null && root != null && scene.cn1ContextMenu(sceneX(root, x), sceneY(root, y));
    }

    /// The wheel turned; deltas in display pixels.
    public static boolean wheel(Scene scene, Component root, int x, int y, int deltaX, int deltaY) {
        return scene != null && root != null && scene.cn1Wheel(sceneX(root, x), sceneY(root, y),
                Units.toLogical(deltaX), Units.toLogical(deltaY));
    }

    /// A key went down; `keyCode` is Codename One's.
    public static boolean keyPressed(Scene scene, int keyCode) {
        if (scene == null) {
            return false;
        }
        KeyCode code = code(keyCode);
        boolean consumed = scene.cn1Key(KeyEvent.KEY_PRESSED, code, text(keyCode));
        if (keyCode >= 32 && keyCode != 127) {
            consumed |= scene.cn1Key(KeyEvent.KEY_TYPED, KeyCode.UNDEFINED, text(keyCode));
        }
        return consumed;
    }

    /// A key came up.
    public static boolean keyReleased(Scene scene, int keyCode) {
        return scene != null && scene.cn1Key(KeyEvent.KEY_RELEASED, code(keyCode), text(keyCode));
    }

    private static String text(int keyCode) {
        return keyCode >= 32 && keyCode != 127 && keyCode <= 0xffff ? String.valueOf((char) keyCode) : "";
    }

    /// Returns the JavaFX key of a Codename One key code.
    public static KeyCode code(int keyCode) {
        if (keyCode >= 'a' && keyCode <= 'z') {
            return letter(keyCode - 'a');
        } else if (keyCode >= 'A' && keyCode <= 'Z') {
            return letter(keyCode - 'A');
        } else if (keyCode >= '0' && keyCode <= '9') {
            return DIGITS[keyCode - '0'];
        }
        switch (keyCode) {
            case 8:
                return KeyCode.BACK_SPACE;
            case 9:
                return KeyCode.TAB;
            case 10:
            case 13:
                return KeyCode.ENTER;
            case 27:
                return KeyCode.ESCAPE;
            case 32:
                return KeyCode.SPACE;
            case 127:
                return KeyCode.DELETE;
            default:
                break;
        }
        if (display() && keyCode < 0) {
            int action = Display.getInstance().getGameAction(keyCode);
            switch (action) {
                case Display.GAME_UP:
                    return KeyCode.UP;
                case Display.GAME_DOWN:
                    return KeyCode.DOWN;
                case Display.GAME_LEFT:
                    return KeyCode.LEFT;
                case Display.GAME_RIGHT:
                    return KeyCode.RIGHT;
                case Display.GAME_FIRE:
                    return KeyCode.ENTER;
                default:
                    break;
            }
        }
        return KeyCode.UNDEFINED;
    }

    private static final KeyCode[] DIGITS = {KeyCode.DIGIT0, KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3,
        KeyCode.DIGIT4, KeyCode.DIGIT5, KeyCode.DIGIT6, KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9};

    private static final KeyCode[] LETTERS = {KeyCode.A, KeyCode.B, KeyCode.C, KeyCode.D, KeyCode.E, KeyCode.F,
        KeyCode.G, KeyCode.H, KeyCode.I, KeyCode.J, KeyCode.K, KeyCode.L, KeyCode.M, KeyCode.N, KeyCode.O, KeyCode.P,
        KeyCode.Q, KeyCode.R, KeyCode.S, KeyCode.T, KeyCode.U, KeyCode.V, KeyCode.W, KeyCode.X, KeyCode.Y,
        KeyCode.Z};

    private static KeyCode letter(int index) {
        return LETTERS[index];
    }
}
