/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

import com.codename1.ui.geom.Rectangle;

/// Describes the physical fold posture of a foldable or dual screen device such as a Galaxy Fold,
/// Galaxy Flip, Pixel Fold or Surface Duo.
///
/// Obtain the live posture with `DevicePosture#getInstance()`. The returned object reads the
/// current posture on demand, so a single instance can be kept and queried whenever needed; it is
/// never null. On devices that are not foldable, or platforms that do not report fold information,
/// the posture is `POSTURE_UNKNOWN`, `#isFoldable()` is false and `#getFoldBounds(Rectangle)`
/// returns null.
///
/// To be notified when the device is folded, unfolded or changes posture register a listener with
/// `com.codename1.ui.Display#addPostureListener(com.codename1.ui.events.ActionListener)`.
///
/// When the fold separates the screen into two logical areas (book or tabletop posture) the hinge
/// rectangle returned by `#getFoldBounds(Rectangle)` reports the region the hinge occludes, similar
/// to the way `com.codename1.ui.Form#getSafeArea()` reports the area obscured by a notch. Lay out
/// interactive content to avoid that region, or split a master and detail experience across the two
/// halves.
public final class DevicePosture {

    /// The posture could not be determined, typically because the device is not foldable.
    public static final int POSTURE_UNKNOWN = 0;

    /// The device is open and flat (a continuous surface). This is the normal posture of an unfolded
    /// device and the only posture reported by non-foldable devices that still report fold state.
    public static final int POSTURE_FLAT = 1;

    /// The device is half opened, forming a laptop, book or tabletop posture with the hinge between
    /// roughly 30 and 150 degrees.
    public static final int POSTURE_HALF_OPENED = 2;

    /// The device is folded shut.
    public static final int POSTURE_CLOSED = 3;

    /// There is no fold separating the display.
    public static final int FOLD_ORIENTATION_NONE = 0;

    /// The hinge runs vertically, splitting the display into a left and a right half.
    public static final int FOLD_ORIENTATION_VERTICAL = 1;

    /// The hinge runs horizontally, splitting the display into a top and a bottom half.
    public static final int FOLD_ORIENTATION_HORIZONTAL = 2;

    private static DevicePosture instance;

    private DevicePosture() {
    }

    /// Returns the shared device posture instance. The returned object reads live posture data, so
    /// the same instance reflects the current posture every time it is queried.
    ///
    /// #### Returns
    ///
    /// the shared device posture, never null
    public static DevicePosture getInstance() {
        if (instance == null) {
            instance = new DevicePosture();
        }
        return instance;
    }

    /// True if the device is a foldable or dual screen device.
    public boolean isFoldable() {
        return Display.impl.isFoldable();
    }

    /// The current posture, one of the `POSTURE_*` constants.
    public int getPosture() {
        return Display.impl.getDevicePosture();
    }

    /// The current hinge angle in degrees between `0` (closed) and `180` (flat), or `-1` when the
    /// device does not report a hinge angle.
    public int getHingeAngle() {
        return Display.impl.getHingeAngle();
    }

    /// The orientation of the fold, one of the `FOLD_ORIENTATION_*` constants.
    public int getFoldOrientation() {
        return Display.impl.getFoldOrientation();
    }

    /// True if the fold currently separates the display into two distinct logical areas (a book or
    /// tabletop posture) rather than presenting a single continuous surface.
    public boolean isSeparating() {
        return Display.impl.isPostureSeparating();
    }

    /// True if the device is in a tabletop or book posture (half opened and separating the display).
    public boolean isTableTop() {
        return getPosture() == POSTURE_HALF_OPENED && isSeparating();
    }

    /// Returns the bounds of the region occluded by the hinge in display coordinates, or null when
    /// there is no separating fold. When the fold is a thin crease the rectangle has zero width or
    /// height along the hinge.
    ///
    /// #### Parameters
    ///
    /// - `rect`: a rectangle to populate and return, or null to allocate a new one
    ///
    /// #### Returns
    ///
    /// the hinge bounds, or null when there is no separating fold
    public Rectangle getFoldBounds(Rectangle rect) {
        return Display.impl.getFoldBounds(rect);
    }
}
