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
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.ReleasableComponent;
import com.codename1.ui.events.PointerEvent;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
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
///
/// An open popup under the pointer takes the event before the scene of
/// the host does; see [StagePopup].
///
/// ### A native component is pressed where its node is drawn
///
/// Codename One finds the component under a pointer from the bounds of
/// the components, and the bounds of a peer know nothing of a scale, a
/// rotation or a transform of the node or of a parent of it: a button in
/// a group scaled to fit its window is drawn in one place and its peer
/// lies in another, often outside the form altogether. Left to Codename
/// One such a button is never pressed, and a press where its peer lies,
/// where nothing is drawn, presses it.
///
/// So the scene decides. The node it picks names the control, the scene
/// point is taken into the control's own coordinates and from there into
/// its peer's, and when that is not where the pointer is on the display
/// the press, the drags and the release of the gesture go to the native
/// component directly, at that point, and the host passes nothing on. A
/// pointer over a native component whose node is drawn elsewhere is
/// swallowed the same way. Where the two places agree, which is every
/// scene with no such transform, Codename One delivers the event as it
/// always did.
public final class SceneInput {

    private SceneInput() {
    }

    private static int touch;
    private static int alt;

    private static boolean display() {
        return Display.isInitialized();
    }

    /// Returns whether the pointer is a finger: the display is a touch
    /// screen and the platform is not a desktop, where the pointer is a
    /// mouse whatever the screen can do. A scene then fires a touch event
    /// ahead of every mouse event.
    public static boolean touchInput() {
        if (touch != 0) {
            return touch > 0;
        }
        return display() && Display.getInstance().isTouchScreenDevice() && !Display.getInstance().isDesktop();
    }

    /// Fixes the answer of [#touchInput()]: positive for a finger,
    /// negative for a mouse, zero for what the display says.
    public static void setTouchInput(int value) {
        touch = value;
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
        if (alt != 0) {
            return alt > 0;
        }
        return display() && Display.getInstance().isAltKeyDown();
    }

    /// Fixes the answer of [#altDown()]: positive for down, negative for
    /// up, zero for what the display says.
    public static void setAltDown(int value) {
        alt = value;
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
        gesture = PLAIN;
        captured = null;
        capturedOwner = null;
        if (StagePopup.pointer(MouseEvent.MOUSE_PRESSED, x, y, button())) {
            return true;
        }
        if (scene == null || root == null) {
            return false;
        }
        double sx = sceneX(root, x);
        double sy = sceneY(root, y);
        if (scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, sx, sy, button())) {
            return true;
        }
        Control owner = controlAt(scene, sx, sy);
        if (owner != null) {
            if (!place(owner, sx, sy) || !apart(placeX, x) && !apart(placeY, y)) {
                return false;
            }
            Component target = deepest(owner.cn1Native(), placeX, placeY);
            gesture = DIRECT;
            captured = target;
            capturedOwner = owner;
            if (target.isEnabled()) {
                if (target.isFocusable()) {
                    target.requestFocus();
                }
                target.pointerPressed(placeX, placeY);
            }
            return true;
        }
        if (phantom(root, x, y, sx, sy)) {
            gesture = SWALLOWED;
            return true;
        }
        return false;
    }

    /// A pointer moved while down.
    public static boolean dragged(Scene scene, Component root, int x, int y) {
        if (StagePopup.pointer(MouseEvent.MOUSE_DRAGGED, x, y, button())) {
            return true;
        }
        if (scene == null || root == null) {
            return false;
        }
        double sx = sceneX(root, x);
        double sy = sceneY(root, y);
        boolean consumed = scene.cn1Pointer(MouseEvent.MOUSE_DRAGGED, sx, sy, button());
        if (gesture == DIRECT) {
            if (!consumed && captured != null && captured.isEnabled() && capturedOwner != null
                    && capturedOwner.getScene() == scene && place(capturedOwner, sx, sy)) {
                captured.pointerDragged(placeX, placeY);
            }
            return true;
        }
        return consumed || gesture == SWALLOWED;
    }

    /// A pointer came up.
    public static boolean released(Scene scene, Component root, int x, int y) {
        int was = gesture;
        Component target = captured;
        Control owner = capturedOwner;
        gesture = PLAIN;
        captured = null;
        capturedOwner = null;
        if (StagePopup.pointer(MouseEvent.MOUSE_RELEASED, x, y, button())) {
            cancel(target);
            return true;
        }
        if (scene == null || root == null) {
            return false;
        }
        double sx = sceneX(root, x);
        double sy = sceneY(root, y);
        boolean consumed = scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, sx, sy, button());
        if (was == DIRECT) {
            if (target != null && target.isEnabled() && !consumed && owner != null && owner.getScene() == scene
                    && place(owner, sx, sy) && target.contains(placeX, placeY)) {
                target.pointerReleased(placeX, placeY);
            } else {
                cancel(target);
            }
            return true;
        }
        return consumed || was == SWALLOWED;
    }

    /// A mouse moved with no button down.
    public static boolean hover(Scene scene, Component root, int x, int y) {
        if (StagePopup.pointer(MouseEvent.MOUSE_MOVED, x, y, MouseButton.NONE)) {
            return true;
        }
        if (scene == null || root == null) {
            return false;
        }
        double sx = sceneX(root, x);
        double sy = sceneY(root, y);
        if (scene.cn1Pointer(MouseEvent.MOUSE_MOVED, sx, sy, MouseButton.NONE)) {
            return true;
        }
        Control owner = controlAt(scene, sx, sy);
        if (owner != null) {
            // Codename One would light up whatever peer lies under the pointer; the
            // node's own hover state is what shows where the node is drawn.
            return place(owner, sx, sy) && (apart(placeX, x) || apart(placeY, y));
        }
        return phantom(root, x, y, sx, sy);
    }

    // ------------------------------------------- native components

    private static final int PLAIN = 0;
    private static final int DIRECT = 1;
    private static final int SWALLOWED = 2;

    /// What became of the press of the gesture under way: left to
    /// Codename One, delivered here to a native component, or kept from
    /// Codename One altogether.
    private static int gesture;
    private static Component captured;
    private static Control capturedOwner;
    private static int placeX;
    private static int placeY;

    /// Two display positions further apart than the rounding of a chain
    /// of peers accounts for.
    private static boolean apart(int a, int b) {
        int d = a - b;
        return d > 2 || d < -2;
    }

    /// Returns the control a scene point is over: the nearest one, from
    /// the node the scene picks upwards, whose native component the
    /// point lies in.
    private static Control controlAt(Scene scene, double sx, double sy) {
        Parent root = scene.getRoot();
        Node n = root == null ? null : root.cn1Pick(sx, sy);
        while (n != null) {
            if (n instanceof Control) {
                Control c = (Control) n;
                Component nat = c.cn1Native();
                if (nat != null && place(c, sx, sy) && nat.contains(placeX, placeY)) {
                    return c;
                }
            }
            n = n.getParent();
        }
        return null;
    }

    /// Takes a scene point to the display position it has in the peer of
    /// a node, which is where Codename One believes the node to be.
    private static boolean place(Node node, double sx, double sy) {
        Component peer = node.cn1Peer();
        Point2D local = node.sceneToLocal(sx, sy);
        if (peer == null || local == null) {
            return false;
        }
        placeX = peer.getAbsoluteX() + Units.toPixels(local.getX());
        placeY = peer.getAbsoluteY() + Units.toPixels(local.getY());
        return true;
    }

    /// Returns the innermost component of a native at a display
    /// position, or the native itself where that is the peer of a node.
    private static Component deepest(Component nat, int x, int y) {
        if (!(nat instanceof Container)) {
            return nat;
        }
        Component at = ((Container) nat).getComponentAt(x, y);
        for (Component c = at; c != null && c != nat; c = c.getParent()) {
            if (c instanceof FxPeer) {
                return nat;
            }
        }
        return at == null ? nat : at;
    }

    /// Returns whether Codename One would deliver a pointer at a display
    /// position to a native component whose node is not drawn there.
    private static boolean phantom(Component root, int x, int y, double sx, double sy) {
        if (!(root instanceof Container)) {
            return false;
        }
        Component c = ((Container) root).getComponentAt(x, y);
        if (c == null || c instanceof FxPeer) {
            return false;
        }
        while (c != null && c != root && !(c instanceof FxPeer)) {
            c = c.getParent();
        }
        if (!(c instanceof FxPeer)) {
            return false;
        }
        Node owner = ((FxPeer) c).node();
        return owner != null && place(owner, sx, sy) && (apart(placeX, x) || apart(placeY, y));
    }

    /// Lets go of a native component that was pressed here and is not to
    /// act on the release: the pointer came up somewhere else.
    private static void cancel(Component target) {
        if (target instanceof ReleasableComponent) {
            ((ReleasableComponent) target).setReleased();
        }
    }

    /// A touch was held in place: a context menu request.
    public static boolean longPress(Scene scene, Component root, int x, int y) {
        if (StagePopup.covers(x, y)) {
            return true;
        }
        return scene != null && root != null && scene.cn1ContextMenu(sceneX(root, x), sceneY(root, y));
    }

    /// The wheel turned; deltas in display pixels.
    public static boolean wheel(Scene scene, Component root, int x, int y, int deltaX, int deltaY) {
        if (StagePopup.wheel(x, y, deltaX, deltaY)) {
            return true;
        }
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
