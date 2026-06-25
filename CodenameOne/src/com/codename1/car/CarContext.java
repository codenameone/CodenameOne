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
package com.codename1.car;

import com.codename1.car.spi.CarBridge;
import java.util.ArrayList;

/// The live handle to a connected head unit. Created by the framework when a car connects and handed
/// to `CarApplication#onCreateRootScreen(CarContext)` and to every `CarActionListener`. Use it to
/// drive the head-unit back stack ([#pushScreen(CarScreen)], [#popScreen()]), surface transient
/// messages ([#showToast(String)]) and query driver-distraction limits.
///
/// The context delegates rendering to a platform `com.codename1.car.spi.CarBridge`. On the simulator
/// and on ports without in-car projection the bridge is absent, so every method here is a harmless
/// no-op and [#isConnected()] returns false -- application code never needs platform `if`s.
public final class CarContext {
    private final CarBridge bridge;
    private final ArrayList<CarScreen> stack = new ArrayList<CarScreen>();

    CarContext(CarBridge bridge) {
        this.bridge = bridge;
    }

    /// Returns true while a head unit is connected and rendering.
    public boolean isConnected() {
        return bridge != null && bridge.isConnected();
    }

    /// Returns the screen currently at the top of the in-car back stack, or null if none.
    public CarScreen getTopScreen() {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.get(stack.size() - 1);
    }

    /// Pushes a screen onto the head-unit back stack and renders it. The previous screen is paused.
    ///
    /// #### Parameters
    ///
    /// - `screen`: the screen to show
    public void pushScreen(CarScreen screen) {
        if (screen == null) {
            return;
        }
        CarScreen previous = getTopScreen();
        screen.attach(this);
        screen.dispatchCreate();
        stack.add(screen);
        if (previous != null) {
            previous.dispatchPause();
        }
        if (bridge != null) {
            bridge.pushScreen(screen);
        }
        screen.dispatchResume();
    }

    /// Pops the top screen, returning to the one beneath it. No effect when only the root remains.
    public void popScreen() {
        if (stack.size() <= 1) {
            return;
        }
        CarScreen top = stack.remove(stack.size() - 1);
        top.dispatchPause();
        if (bridge != null) {
            bridge.popScreen();
        }
        top.dispatchDestroy();
        top.detach();
        CarScreen newTop = getTopScreen();
        if (newTop != null) {
            newTop.dispatchResume();
        }
    }

    /// Tears down the in-car experience and dismisses the app from the head unit.
    public void finish() {
        if (bridge != null) {
            bridge.finish();
        }
    }

    /// Shows a short transient message on the head unit.
    ///
    /// #### Parameters
    ///
    /// - `message`: the text to display
    public void showToast(String message) {
        showToast(message, 2);
    }

    /// Shows a transient message on the head unit for a requested duration.
    ///
    /// #### Parameters
    ///
    /// - `message`: the text to display
    ///
    /// - `durationSeconds`: requested duration in seconds (the head unit may clamp it)
    public void showToast(String message, int durationSeconds) {
        if (bridge != null) {
            bridge.showToast(message, durationSeconds);
        }
    }

    /// Returns the maximum rows the head unit will show in a `CarListTemplate`, or 0 if unknown.
    /// Honour this when populating lists -- rows beyond the limit are dropped by the platform.
    ///
    /// #### Returns
    ///
    /// the row limit, or 0 if unknown
    public int getListRowLimit() {
        return bridge == null ? 0 : bridge.getListRowLimit();
    }

    /// Returns the maximum items the head unit will show in a `CarGridTemplate`, or 0 if unknown.
    ///
    /// #### Returns
    ///
    /// the grid item limit, or 0 if unknown
    public int getGridItemLimit() {
        return bridge == null ? 0 : bridge.getGridItemLimit();
    }

    // --- framework internals -------------------------------------------------

    void invalidateScreen(CarScreen screen) {
        if (bridge != null && stack.contains(screen)) {
            bridge.invalidate(screen);
        }
    }

    void setRootScreen(CarScreen root) {
        if (root == null) {
            return;
        }
        root.attach(this);
        root.dispatchCreate();
        stack.add(root);
        if (bridge != null) {
            bridge.pushScreen(root);
        }
        root.dispatchResume();
    }

    void destroy() {
        for (int i = stack.size() - 1; i >= 0; i--) {
            CarScreen s = stack.get(i);
            s.dispatchDestroy();
            s.detach();
        }
        stack.clear();
    }
}
