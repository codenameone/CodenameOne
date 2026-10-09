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
package com.codename1.unitycompat.unityengine;

import UnityEngine.Touch;
import UnityEngine.Vector3;

/// `UnityEngine.Input`: the mouse, fingers on a touch screen, the keyboard,
/// and the named axes and buttons of the classic input manager.
///
/// A host reports presses and releases as they happen, through
/// [UnityRuntime#keyPressed] and its siblings. They take effect at the start
/// of the next frame, so that "down" is true for exactly one frame, which is
/// what `GetKeyDown` promises -- even for a key pressed and released between
/// two frames, which reads as down and up in the same one.
///
/// #### Touches
///
/// A host reports the fingers on the screen as a whole: every time one
/// lands, moves or lifts, where all of them now are
/// ([UnityRuntime#touches]). It does not say which is which, because the
/// platforms underneath do not either, so fingers are told apart here: each
/// reported point continues the finger nearest it, nearest pairs first; a
/// point left over is a new finger, and a finger left over has lifted. Two
/// fingers that cross between two reports closer than they moved would swap
/// identities; reports come many times a second, so they do not in practice.
///
/// From that come Unity's phases, one per finger per frame: `Began` for the
/// frame a finger lands, `Moved` or `Stationary` while it stays, `Ended` for
/// one frame after it lifts, and then it is gone. A finger that lands and
/// lifts between two frames is still seen, as `Began` in one frame and
/// `Ended` in the next. `fingerId` is the lowest number no finger on the
/// screen has, and `Input.touches` lists fingers by it.
///
/// `tapCount` counts landings that follow the last lift within 0.3 seconds
/// and 40 pixels; Unity leaves the thresholds to the platform, so that is
/// an approximation. `Canceled` is never reported.
///
/// The first finger also drives mouse button 0 and `mousePosition`, as
/// Unity's `simulateMouseWithTouches` does by default: the host reports it
/// as a pointer too. With that property false on a host with a touch
/// screen, pointer reports are ignored.
///
/// Keys are numbered as `UnityEngine.KeyCode`. The axes are Unity's default
/// ones unless generated code replaced them with the project's own from its
/// input settings.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Input {
    private static final int KEYS = 512;
    /// The key names of the classic input manager that are longer than a
    /// character, and the code each stands for. Constants, because a script
    /// may ask by name on every frame.
    private static final String[] KEY_NAMES = {
        "space", "return", "enter", "escape", "backspace", "tab", "delete", "up", "down", "right", "left",
        "left shift", "right shift", "left ctrl", "right ctrl", "left alt", "right alt", "mouse 0", "mouse 1",
        "mouse 2", "insert", "home", "end", "page up", "page down",
    };
    private static final int[] KEY_CODES = {
        32, 13, 271, 27, 8, 9, 127, 273, 274, 275, 276, 304, 303, 306, 305, 308, 307, 323, 324, 325, 277, 278,
        279, 280, 281,
    };
    private static final boolean[] held = new boolean[KEYS];
    private static final boolean[] down = new boolean[KEYS];
    private static final boolean[] up = new boolean[KEYS];
    /// Presses and releases since the last frame: the key, negative for a
    /// release.
    private static int[] events = new int[32];
    private static int eventCount;
    private static int heldCount;
    private static boolean anyDown;
    private static boolean touched;
    static float mouseX;
    static float mouseY;

    private static final int MOUSE0 = 323;

    /// One axis or button: its name and the keys that drive it.
    private static final class Axis {
        String name;
        int negative;
        int positive;
        int altNegative;
        int altPositive;
        /// For an axis the mouse drives: 0 for x, 1 for y, 2 for the wheel;
        /// -1 for one keys drive.
        int mouse = -1;
        float sensitivity;
        float value;
    }

    // What the mouse did between the last two frames.
    private static float lastMouseX;
    private static float lastMouseY;
    private static float mouseDeltaX;
    private static float mouseDeltaY;
    private static boolean mouseKnown;

    private static final int FINGERS = 16;
    private static final int NONE = 0;
    /// On the screen.
    private static final int DOWN = 1;
    /// Lifted: reported as `Ended` this frame and gone the next.
    private static final int LIFTED = 2;
    /// Landed and lifted before a frame saw it: `Began` now, `Ended` next.
    private static final int TAPPED = 3;
    private static final int[] fingerState = new int[FINGERS];
    private static final boolean[] fingerNew = new boolean[FINGERS];
    private static final boolean[] fingerTaken = new boolean[FINGERS];
    private static final float[] fingerX = new float[FINGERS];
    private static final float[] fingerY = new float[FINGERS];
    private static final float[] fingerFromX = new float[FINGERS];
    private static final float[] fingerFromY = new float[FINGERS];
    private static final int[] fingerTaps = new int[FINGERS];
    /// Reports since the last frame, each a count followed by that many x
    /// and y pairs.
    private static float[] reports = new float[64];
    private static int reportLength;
    private static final boolean[] pointTaken = new boolean[FINGERS];
    private static Touch[] frameTouches = new Touch[0];
    private static boolean touchHost;
    private static boolean multiTouch = true;
    private static boolean simulateMouse = true;
    private static float clock;
    private static float tapTime;
    private static float tapX;
    private static float tapY;
    private static int tapCount;

    private static Axis[] axes = new Axis[0];

    private Input() {
    }

    static void reset() {
        for (int i = 0; i < KEYS; i++) {
            held[i] = false;
            down[i] = false;
            up[i] = false;
        }
        eventCount = 0;
        heldCount = 0;
        anyDown = false;
        touched = false;
        mouseX = 0f;
        mouseY = 0f;
        lastMouseX = 0f;
        lastMouseY = 0f;
        mouseDeltaX = 0f;
        mouseDeltaY = 0f;
        mouseKnown = false;
        for (int i = 0; i < FINGERS; i++) {
            fingerState[i] = NONE;
            fingerNew[i] = false;
        }
        reportLength = 0;
        frameTouches = new Touch[0];
        touchHost = false;
        multiTouch = true;
        simulateMouse = true;
        clock = 0f;
        tapTime = Float.NEGATIVE_INFINITY;
        tapCount = 0;
        axes = new Axis[0];
        // The axes of a new Unity project that a keyboard drives.
        $axis("Horizontal", 276, 275, 97, 100);
        $axis("Vertical", 274, 273, 115, 119);
        $axis("Fire1", 0, 306, 0, MOUSE0);
        $axis("Fire2", 0, 308, 0, MOUSE0 + 1);
        $axis("Fire3", 0, 304, 0, MOUSE0 + 2);
        $axis("Jump", 0, 32, 0, 0);
        $axis("Submit", 0, 13, 0, 271);
        $axis("Cancel", 0, 27, 0, 0);
        $mouseAxis("Mouse X", 0, 0.1f);
        $mouseAxis("Mouse Y", 1, 0.1f);
        $mouseAxis("Mouse ScrollWheel", 2, 0.1f);
    }

    /// Defines an axis the mouse drives, beside any other of the same
    /// name: 0 for its movement in x, 1 in y, 2 for the wheel, which no
    /// host reports and which therefore reads zero. The value is how far
    /// the pointer moved since the last frame, in pixels, times the
    /// sensitivity. Generated code calls this with the project's input
    /// settings.
    public static void $mouseAxis(String name, int mouseAxis, float sensitivity) {
        $addAxis(name, 0, 0, 0, 0);
        Axis a = axes[axes.length - 1];
        a.mouse = mouseAxis;
        a.sensitivity = sensitivity;
    }

    /// Defines an axis, or redefines one of the same name: the key codes
    /// of its negative and positive buttons and of their alternatives, zero
    /// for none. Generated code calls this with the project's input
    /// settings.
    public static void $axis(String name, int negative, int positive, int altNegative, int altPositive) {
        Axis a = null;
        for (int i = 0; i < axes.length; i++) { // NOPMD ForLoopCanBeForeach
            if (axes[i].name.equals(name)) {
                a = axes[i];
            }
        }
        if (a == null) {
            a = new Axis();
            a.name = name;
            Axis[] grown = new Axis[axes.length + 1];
            System.arraycopy(axes, 0, grown, 0, axes.length);
            grown[axes.length] = a;
            axes = grown;
        }
        a.negative = negative;
        a.positive = positive;
        a.altNegative = altNegative;
        a.altPositive = altPositive;
    }

    /// Forgets every axis, before generated code defines the project's.
    public static void $clearAxes() {
        axes = new Axis[0];
    }

    /// Defines an axis beside any other of the same name. Unity's input
    /// settings list an axis once for each way of driving it -- the keys,
    /// then a joystick -- and a button twice for two sets of keys; all of
    /// one name act as one.
    public static void $addAxis(String name, int negative, int positive, int altNegative, int altPositive) {
        Axis a = new Axis();
        a.name = name;
        a.negative = negative;
        a.positive = positive;
        a.altNegative = altNegative;
        a.altPositive = altPositive;
        Axis[] grown = new Axis[axes.length + 1];
        System.arraycopy(axes, 0, grown, 0, axes.length);
        grown[axes.length] = a;
        axes = grown;
    }

    // ------------------------------------------------------- from the host

    private static void post(int event) {
        if (eventCount == events.length) {
            int[] grown = new int[eventCount * 2];
            System.arraycopy(events, 0, grown, 0, eventCount);
            events = grown;
        }
        events[eventCount++] = event;
    }

    static void keyPressed(int key) {
        if (key > 0 && key < KEYS) {
            post(key);
        }
    }

    static void keyReleased(int key) {
        if (key > 0 && key < KEYS) {
            post(-key);
        }
    }

    /// A pointer report from a touch screen is a finger standing in for
    /// the mouse, which a script may have switched off.
    private static boolean mouseIgnored() {
        return touchHost && !simulateMouse;
    }

    static void pointerPressed(float x, float y) {
        if (mouseIgnored()) {
            return;
        }
        if (touchHost || !mouseKnown) {
            // A finger landing is not the mouse travelling there.
            lastMouseX = x;
            lastMouseY = y;
            mouseKnown = true;
        }
        mouseX = x;
        mouseY = y;
        post(MOUSE0);
    }

    static void pointerReleased(float x, float y) {
        if (mouseIgnored()) {
            return;
        }
        mouseX = x;
        mouseY = y;
        post(-MOUSE0);
    }

    static void pointerMoved(float x, float y) {
        if (mouseIgnored()) {
            return;
        }
        if (!mouseKnown) {
            lastMouseX = x;
            lastMouseY = y;
            mouseKnown = true;
        }
        mouseX = x;
        mouseY = y;
    }

    static void touchSupported(boolean supported) {
        touchHost = supported;
    }

    /// Where every finger on the screen now is; `count` zero when the last
    /// one lifted.
    static void touches(float[] x, float[] y, int count) {
        int n = count > FINGERS ? FINGERS : count;
        if (reportLength + 1 + n * 2 > reports.length) {
            float[] grown = new float[(reportLength + 1 + n * 2) * 2];
            System.arraycopy(reports, 0, grown, 0, reportLength);
            reports = grown;
        }
        reports[reportLength++] = n;
        for (int i = 0; i < n; i++) {
            reports[reportLength++] = x[i];
            reports[reportLength++] = y[i];
        }
    }

    /// Works one report into the fingers: `count` points starting at
    /// `reports[at]`.
    private static void report(int at, int count) {
        int n = multiTouch || count < 1 ? count : 1;
        for (int i = 0; i < FINGERS; i++) {
            fingerTaken[i] = fingerState[i] != DOWN;
            pointTaken[i] = i >= n;
        }
        // Nearest pairs first. Of equals, the lower finger and then the
        // earlier point, so that the outcome does not depend on anything
        // but the report.
        while (true) {
            int bestFinger = -1;
            int bestPoint = -1;
            float best = 0f;
            for (int f = 0; f < FINGERS; f++) {
                if (fingerTaken[f]) {
                    continue;
                }
                for (int p = 0; p < n; p++) {
                    if (pointTaken[p]) {
                        continue;
                    }
                    float dx = reports[at + p * 2] - fingerX[f];
                    float dy = reports[at + p * 2 + 1] - fingerY[f];
                    float a = dx * dx;
                    float b = dy * dy;
                    float d = a + b;
                    if (bestFinger < 0 || d < best) {
                        bestFinger = f;
                        bestPoint = p;
                        best = d;
                    }
                }
            }
            if (bestFinger < 0) {
                break;
            }
            fingerTaken[bestFinger] = true;
            pointTaken[bestPoint] = true;
            fingerX[bestFinger] = reports[at + bestPoint * 2];
            fingerY[bestFinger] = reports[at + bestPoint * 2 + 1];
        }
        // A finger no point continued has lifted.
        for (int f = 0; f < FINGERS; f++) {
            if (!fingerTaken[f]) {
                fingerState[f] = fingerNew[f] ? TAPPED : LIFTED;
                tapTime = clock;
                tapX = fingerX[f];
                tapY = fingerY[f];
                tapCount = fingerTaps[f];
            }
        }
        // A point that continued none is a new finger.
        for (int p = 0; p < n; p++) {
            if (pointTaken[p]) {
                continue;
            }
            int f = 0;
            while (f < FINGERS && fingerState[f] != NONE) {
                f++;
            }
            if (f == FINGERS) {
                break;
            }
            float x = reports[at + p * 2];
            float y = reports[at + p * 2 + 1];
            float dx = x - tapX;
            float dy = y - tapY;
            boolean again = clock - tapTime <= 0.3f && dx < 40f && dx > -40f && dy < 40f && dy > -40f;
            fingerState[f] = DOWN;
            fingerNew[f] = true;
            fingerX[f] = x;
            fingerY[f] = y;
            fingerFromX[f] = x;
            fingerFromY[f] = y;
            fingerTaps[f] = again ? tapCount + 1 : 1;
        }
    }

    /// Turns the reports since the last frame into this frame's touches.
    private static void touchFrame(float dt) {
        clock += dt;
        for (int f = 0; f < FINGERS; f++) {
            if (fingerState[f] == LIFTED) {
                // Its frame as Ended was the last one.
                fingerState[f] = NONE;
            } else if (fingerState[f] == TAPPED) {
                fingerState[f] = LIFTED;
            }
            fingerNew[f] = false;
            fingerFromX[f] = fingerX[f];
            fingerFromY[f] = fingerY[f];
        }
        int at = 0;
        while (at < reportLength) {
            int count = (int) reports[at];
            report(at + 1, count);
            at += 1 + count * 2;
        }
        reportLength = 0;
        int live = 0;
        for (int f = 0; f < FINGERS; f++) {
            if (fingerState[f] != NONE) {
                live++;
            }
        }
        if (live == 0 && frameTouches.length == 0) {
            return;
        }
        Touch[] now = Touch.$newArray(live);
        int i = 0;
        for (int f = 0; f < FINGERS; f++) {
            int state = fingerState[f];
            if (state == NONE) {
                continue;
            }
            Touch t = now[i++];
            t.m_FingerId = f;
            t.m_Position.x = fingerX[f];
            t.m_Position.y = fingerY[f];
            t.m_RawPosition.x = fingerX[f];
            t.m_RawPosition.y = fingerY[f];
            t.m_TimeDelta = dt;
            t.m_TapCount = fingerTaps[f];
            float dx = fingerX[f] - fingerFromX[f];
            float dy = fingerY[f] - fingerFromY[f];
            if (fingerNew[f]) {
                // Began, even for one that has lifted already: that is next
                // frame's news.
                t.m_Phase = 0;
            } else {
                t.m_PositionDelta.x = dx;
                t.m_PositionDelta.y = dy;
                t.m_Phase = state == LIFTED ? 3 : dx != 0f || dy != 0f ? 1 : 2;
            }
        }
        frameTouches = now;
    }

    /// Applies what the host reported since the last frame.
    static void beginFrame(float dt) {
        if (touched) {
            for (int i = 0; i < KEYS; i++) {
                down[i] = false;
                up[i] = false;
            }
        }
        touched = eventCount > 0;
        anyDown = false;
        for (int i = 0; i < eventCount; i++) {
            int e = events[i];
            if (e > 0) {
                if (!held[e]) {
                    held[e] = true;
                    heldCount++;
                    down[e] = true;
                    anyDown = true;
                }
            } else if (held[-e]) {
                held[-e] = false;
                heldCount--;
                up[-e] = true;
            }
        }
        eventCount = 0;
        touchFrame(dt);
        mouseDeltaX = mouseKnown ? mouseX - lastMouseX : 0f;
        mouseDeltaY = mouseKnown ? mouseY - lastMouseY : 0f;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        // GetAxis eases towards the key's value at Unity's default rate of
        // three units a second, and snaps through zero on a reversal.
        float step = 3f * dt;
        for (int i = 0; i < axes.length; i++) { // NOPMD ForLoopCanBeForeach
            Axis a = axes[i];
            float target = raw(a);
            if (a.mouse >= 0) {
                // Movement is not eased: it is what happened.
                a.value = target;
                continue;
            }
            float v = a.value;
            if ((target > 0f && v < 0f) || (target < 0f && v > 0f)) {
                v = 0f;
            }
            if (v < target) {
                v = v + step > target ? target : v + step;
            } else if (v > target) {
                v = v - step < target ? target : v - step;
            }
            a.value = v;
        }
    }

    private static boolean isHeld(int key) {
        return key > 0 && key < KEYS && held[key];
    }

    private static float raw(Axis a) {
        if (a.mouse >= 0) {
            return a.mouse == 0 ? mouseDeltaX * a.sensitivity : a.mouse == 1 ? mouseDeltaY * a.sensitivity : 0f;
        }
        float v = 0f;
        if (isHeld(a.positive) || isHeld(a.altPositive)) {
            v += 1f;
        }
        if (isHeld(a.negative) || isHeld(a.altNegative)) {
            v -= 1f;
        }
        return v;
    }

    /// The first axis of a name at or after `from`, or -1. The first one
    /// asked for has to exist, as in Unity.
    private static int axis(String name, int from) {
        for (int i = from; i < axes.length; i++) {
            if (axes[i].name.equals(name)) {
                return i;
            }
        }
        if (from == 0) {
            throw new IllegalArgumentException("Input Axis " + name + " is not setup.");
        }
        return -1;
    }

    /// The key a name of the classic input manager stands for -- `"a"`,
    /// `"space"`, `"left ctrl"`, `"mouse 0"` -- or zero. Names are lower
    /// case, as Unity writes them; a single character is itself.
    public static int $keyCode(String name) {
        if (name == null || name.length() == 0) {
            return 0;
        }
        if (name.length() == 1) {
            char c = name.charAt(0);
            return c >= 'A' && c <= 'Z' ? c + 32 : c < KEYS ? c : 0;
        }
        for (int i = 0; i < KEY_NAMES.length; i++) { // NOPMD ForLoopCanBeForeach
            if (KEY_NAMES[i].equals(name)) {
                return KEY_CODES[i];
            }
        }
        if (name.length() >= 2 && name.length() <= 3 && name.charAt(0) == 'f') {
            int n = 0;
            for (int i = 1; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c < '0' || c > '9') {
                    return 0;
                }
                n = n * 10 + (c - '0');
            }
            return n >= 1 && n <= 15 ? 281 + n : 0;
        }
        return 0;
    }

    // ------------------------------------------------------------- keyboard

    public static boolean get_anyKey() {
        return heldCount > 0;
    }

    public static boolean get_anyKeyDown() {
        return anyDown;
    }

    public static boolean GetKey(int key) {
        return isHeld(key);
    }

    public static boolean GetKeyDown(int key) {
        return key > 0 && key < KEYS && down[key];
    }

    public static boolean GetKeyUp(int key) {
        return key > 0 && key < KEYS && up[key];
    }

    public static boolean GetKey(String name) {
        return GetKey($keyCode(name));
    }

    public static boolean GetKeyDown(String name) {
        return GetKeyDown($keyCode(name));
    }

    public static boolean GetKeyUp(String name) {
        return GetKeyUp($keyCode(name));
    }

    // ----------------------------------------------------- axes and buttons

    public static float GetAxisRaw(String axisName) {
        float v = 0f;
        for (int i = axis(axisName, 0); i >= 0; i = axis(axisName, i + 1)) {
            float r = raw(axes[i]);
            if (r != 0f) {
                v = r;
            }
        }
        return v;
    }

    /// Of several axes of one name, the one farthest from rest.
    public static float GetAxis(String axisName) {
        float v = 0f;
        for (int i = axis(axisName, 0); i >= 0; i = axis(axisName, i + 1)) {
            float a = axes[i].value;
            if ((a < 0f ? -a : a) > (v < 0f ? -v : v)) {
                v = a;
            }
        }
        return v;
    }

    public static boolean GetButton(String buttonName) {
        for (int i = axis(buttonName, 0); i >= 0; i = axis(buttonName, i + 1)) {
            if (isHeld(axes[i].positive) || isHeld(axes[i].altPositive)) {
                return true;
            }
        }
        return false;
    }

    public static boolean GetButtonDown(String buttonName) {
        for (int i = axis(buttonName, 0); i >= 0; i = axis(buttonName, i + 1)) {
            if (GetKeyDown(axes[i].positive) || GetKeyDown(axes[i].altPositive)) {
                return true;
            }
        }
        return false;
    }

    public static boolean GetButtonUp(String buttonName) {
        for (int i = axis(buttonName, 0); i >= 0; i = axis(buttonName, i + 1)) {
            if (GetKeyUp(axes[i].positive) || GetKeyUp(axes[i].altPositive)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- mouse

    public static boolean GetMouseButtonDown(int button) {
        return button >= 0 && button < 3 && down[MOUSE0 + button];
    }

    public static boolean GetMouseButtonUp(int button) {
        return button >= 0 && button < 3 && up[MOUSE0 + button];
    }

    public static boolean GetMouseButton(int button) {
        return button >= 0 && button < 3 && held[MOUSE0 + button];
    }

    public static Vector3 get_mousePosition(Vector3 ret) {
        ret.x = mouseX;
        ret.y = mouseY;
        ret.z = 0f;
        return ret;
    }

    /// A mouse, or a finger standing in for one.
    public static boolean get_mousePresent() {
        return !mouseIgnored();
    }

    // -------------------------------------------------------------- touches

    public static int get_touchCount() {
        return frameTouches.length;
    }

    /// This frame's touches, lowest `fingerId` first. A new array of
    /// copies each time, as in Unity.
    public static Touch[] get_touches() {
        Touch[] out = Touch.$newArray(frameTouches.length);
        for (int i = 0; i < out.length; i++) {
            out[i].$assign(frameTouches[i]);
        }
        return out;
    }

    public static Touch GetTouch(int index, Touch ret) {
        if (index < 0 || index >= frameTouches.length) {
            throw new IllegalArgumentException("Index out of bounds.");
        }
        ret.$assign(frameTouches[index]);
        return ret;
    }

    /// Whether the host said the device has a touch screen.
    public static boolean get_touchSupported() {
        return touchHost;
    }

    public static boolean get_multiTouchEnabled() {
        return multiTouch;
    }

    /// Off, only the first finger of each report is followed.
    public static void set_multiTouchEnabled(boolean value) {
        multiTouch = value;
    }

    public static boolean get_simulateMouseWithTouches() {
        return simulateMouse;
    }

    public static void set_simulateMouseWithTouches(boolean value) {
        simulateMouse = value;
    }
}
