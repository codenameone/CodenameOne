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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/// Pollable snapshot of keyboard and pointer state for a `GameView`.
///
/// Games generally prefer to *poll* the current input state from inside their
/// update loop ("is the left key down right now?") rather than reacting to event
/// callbacks. `GameInput` collects the raw Codename One key and pointer events
/// delivered to its owning `GameView` and exposes them as simple queries.
///
/// Two kinds of query are available:
///
/// - **Level** state -- `#isKeyDown(int)`, `#isPointerDown()` -- true for as long
///   as the key/pointer is held.
///
/// - **Edge** state -- `#wasKeyPressed(int)`, `#wasKeyReleased(int)`,
///   `#wasPointerPressed()`, `#wasPointerReleased()` -- true only during the single
///   frame in which the transition happened. Edges are cleared by the `GameView`
///   at the end of every frame, after `GameView#update(double)` has run.
///
/// All state is written and read on the Codename One EDT, so no synchronization is
/// required.
public class GameInput {
    private final Set keysDown = new HashSet();
    private final Set pressedEdge = new HashSet();
    private final Set releasedEdge = new HashSet();

    private int pointerX;
    private int pointerY;
    private boolean pointerDown;
    private boolean pointerPressedEdge;
    private boolean pointerReleasedEdge;

    /// Package private -- only `GameView` constructs the input.
    GameInput() {
    }

    void keyDown(int keyCode) {
        Integer k = Integer.valueOf(keyCode);
        if (!keysDown.contains(k)) {
            keysDown.add(k);
            pressedEdge.add(k);
        }
    }

    void keyUp(int keyCode) {
        Integer k = Integer.valueOf(keyCode);
        keysDown.remove(k);
        releasedEdge.add(k);
    }

    void pointer(int x, int y, boolean down, boolean pressed, boolean released) {
        pointerX = x;
        pointerY = y;
        pointerDown = down;
        if (pressed) {
            pointerPressedEdge = true;
        }
        if (released) {
            pointerReleasedEdge = true;
        }
    }

    /// Clears the per frame edge state. Called by `GameView` at the end of each
    /// frame once the update has consumed the edges.
    void clearFrameEdges() {
        pressedEdge.clear();
        releasedEdge.clear();
        pointerPressedEdge = false;
        pointerReleasedEdge = false;
    }

    /// Returns true while the given raw key code is held down.
    ///
    /// #### Parameters
    ///
    /// - `keyCode`: a Codename One key code as delivered to
    /// `com.codename1.ui.Component#keyPressed(int)`
    public boolean isKeyDown(int keyCode) {
        return keysDown.contains(Integer.valueOf(keyCode));
    }

    /// Returns true during the single frame in which the given key went down.
    public boolean wasKeyPressed(int keyCode) {
        return pressedEdge.contains(Integer.valueOf(keyCode));
    }

    /// Returns true during the single frame in which the given key was released.
    public boolean wasKeyReleased(int keyCode) {
        return releasedEdge.contains(Integer.valueOf(keyCode));
    }

    /// Returns true while any currently held key maps to the given game action.
    ///
    /// Game actions abstract over device specific key codes for the directional
    /// pad and fire button. See `com.codename1.ui.Display#GAME_UP`,
    /// `com.codename1.ui.Display#GAME_DOWN`, `com.codename1.ui.Display#GAME_LEFT`,
    /// `com.codename1.ui.Display#GAME_RIGHT` and `com.codename1.ui.Display#GAME_FIRE`.
    ///
    /// #### Parameters
    ///
    /// - `gameAction`: one of the `GAME_*` constants on `com.codename1.ui.Display`
    public boolean isGameKeyDown(int gameAction) {
        Display d = Display.getInstance();
        Iterator it = keysDown.iterator();
        while (it.hasNext()) {
            Integer k = (Integer) it.next();
            if (d.getGameAction(k.intValue()) == gameAction) {
                return true;
            }
        }
        return false;
    }

    /// The last known pointer x position, relative to the `GameView`'s top left.
    public int getPointerX() {
        return pointerX;
    }

    /// The last known pointer y position, relative to the `GameView`'s top left.
    public int getPointerY() {
        return pointerY;
    }

    /// Returns true while the pointer (finger / mouse button) is held down.
    public boolean isPointerDown() {
        return pointerDown;
    }

    /// Returns true during the single frame in which the pointer went down.
    public boolean wasPointerPressed() {
        return pointerPressedEdge;
    }

    /// Returns true during the single frame in which the pointer was released.
    public boolean wasPointerReleased() {
        return pointerReleasedEdge;
    }
}
