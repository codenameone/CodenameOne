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
package com.codename1.car.spi;

import com.codename1.car.CarScreen;

/// Internal service-provider interface implemented by each platform port to render the portable
/// `com.codename1.car` template tree onto the native in-car UI (Apple CarPlay's `CPTemplate`
/// hierarchy or Google Android Auto's `androidx.car.app` templates).
///
/// Application code never touches this interface -- it is obtained by the `com.codename1.car`
/// framework classes from `com.codename1.ui.Display#getCarBridge()` and driven through the public
/// `com.codename1.car.CarContext` / `com.codename1.car.CarScreen` API. The base implementation
/// returns `null`, which is why the public API degrades to a harmless no-op on the simulator and on
/// ports without an in-car projection (so application code needs no platform `if` statements).
///
/// A bridge renders a screen by pulling its template through `CarScreen.createTemplate()` (the
/// framework calls `CarScreen#dispatchCreateTemplate()` on its behalf) and is responsible for
/// invoking the relevant `com.codename1.car.CarActionListener` when the user activates a row,
/// grid item or action -- the bridge identifies the activated element by the indices it assigned
/// while rendering.
public interface CarBridge {

    /// Pushes a screen onto the in-car back stack and renders its template. The pushed screen
    /// becomes the visible top of stack.
    ///
    /// #### Parameters
    ///
    /// - `screen`: the screen to push and render
    void pushScreen(CarScreen screen);

    /// Pops the top screen off the in-car back stack, returning to the previous screen. Has no
    /// effect when only the root screen remains.
    void popScreen();

    /// Re-pulls and re-renders the template for the supplied screen (call after mutating the model
    /// behind a screen, mirroring `androidx.car.app.Screen#invalidate()`). No effect if the screen
    /// is not currently on the stack.
    ///
    /// #### Parameters
    ///
    /// - `screen`: the screen whose template should be rebuilt
    void invalidate(CarScreen screen);

    /// Tears down the in-car experience, dismissing the app from the head unit.
    void finish();

    /// Returns true while a head unit (CarPlay / Android Auto) is currently connected and the
    /// projected UI is live.
    ///
    /// #### Returns
    ///
    /// true if a car is connected
    boolean isConnected();

    /// Shows a short transient message on the head unit (CarPlay banner / Android Auto toast).
    ///
    /// #### Parameters
    ///
    /// - `message`: the text to display
    ///
    /// - `durationSeconds`: requested display duration in seconds; the head unit may clamp it
    void showToast(String message, int durationSeconds);

    /// Returns the maximum number of rows the head unit will display in a single list, or `0` when
    /// the limit is unknown. Driver-distraction rules cap this aggressively (often 6-12); honour it
    /// when populating a `com.codename1.car.CarListTemplate`.
    ///
    /// #### Returns
    ///
    /// the row limit, or 0 if unknown
    int getListRowLimit();

    /// Returns the maximum number of items the head unit will display in a single grid, or `0` when
    /// the limit is unknown.
    ///
    /// #### Returns
    ///
    /// the grid item limit, or 0 if unknown
    int getGridItemLimit();
}
