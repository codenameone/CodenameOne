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

import com.codename1.ui.Graphics;

/// The drawing surface callback for a `CarNavigationTemplate`. Navigation is the one in-car category
/// that grants a pixel surface (for the moving map): on Android Auto an `androidx.car.app.SurfaceCallback`
/// backed `Surface`, on CarPlay the `CPMapTemplate`'s window. Codename One adapts that native surface
/// to a `com.codename1.ui.Graphics` so the map can be drawn with the normal CN1 drawing primitives.
///
/// Only navigation apps holding the platform navigation entitlement
/// (`ios.carplay.navigation` build hint on iOS; the `androidx.car.app.category.NAVIGATION` intent
/// filter the builder injects on Android) receive these callbacks.
public interface CarSurfaceCallback {

    /// Invoked when the map surface becomes available or is resized. Cache the dimensions; the next
    /// [#renderSurface(Graphics, int, int)] reflects them.
    ///
    /// #### Parameters
    ///
    /// - `width`: the surface width in pixels
    ///
    /// - `height`: the surface height in pixels
    void surfaceAvailable(int width, int height);

    /// Invoked when the surface should be (re)drawn. Draw the map into the supplied graphics; the
    /// bridge blits the result to the native head-unit surface. Keep drawing cheap -- this is the car
    /// display refresh path.
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics targeting an off-screen buffer the size of the surface
    ///
    /// - `width`: the surface width in pixels
    ///
    /// - `height`: the surface height in pixels
    void renderSurface(Graphics g, int width, int height);

    /// Invoked when the visible (non-occluded) region of the surface changes, e.g. when the head unit
    /// overlays panels. Centre critical map content (current position, next manoeuvre) within this
    /// rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: left edge of the visible area, in pixels
    ///
    /// - `y`: top edge of the visible area, in pixels
    ///
    /// - `width`: width of the visible area, in pixels
    ///
    /// - `height`: height of the visible area, in pixels
    void visibleAreaChanged(int x, int y, int width, int height);

    /// Invoked when the surface is torn down (head unit disconnect, navigation ended). Release any
    /// cached buffers.
    void surfaceDestroyed();
}
