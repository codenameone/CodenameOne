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
package com.codename1.nearby.ranging;

/// What the current device can actually measure. Ask this before building a
/// UI: a device may support distance but not direction, which is the common
/// case on Android hardware and on any iPhone whose peer is behind it.
///
/// [#UNSUPPORTED] is an all-false instance returned where ranging is absent,
/// so calling code needs no null check.
public final class RangingCapabilities {

    /// All-false capabilities, returned by [Ranging#getCapabilities()] on a
    /// platform or device with no UWB at all.
    public static final RangingCapabilities UNSUPPORTED =
            new RangingCapabilities(false, false, false, false, false, false);

    private final boolean distance;
    private final boolean direction;
    private final boolean elevation;
    private final boolean cameraAssistance;
    private final boolean accessoryRanging;
    private final boolean backgroundRanging;

    /// Ports construct this; application code reads it from
    /// [Ranging#getCapabilities()].
    ///
    /// #### Parameters
    ///
    /// - `distance`: precise distance measurement is available
    /// - `direction`: horizontal direction (azimuth) is available
    /// - `elevation`: vertical direction (elevation) is available
    /// - `cameraAssistance`: camera assistance can sharpen direction
    /// - `accessoryRanging`: third-party UWB accessories can be ranged
    /// - `backgroundRanging`: a session may keep running in the background
    public RangingCapabilities(boolean distance, boolean direction,
            boolean elevation, boolean cameraAssistance,
            boolean accessoryRanging, boolean backgroundRanging) {
        this.distance = distance;
        this.direction = direction;
        this.elevation = elevation;
        this.cameraAssistance = cameraAssistance;
        this.accessoryRanging = accessoryRanging;
        this.backgroundRanging = backgroundRanging;
    }

    /// `true` when the device can measure distance to a peer. This is the
    /// baseline capability: a device that answers `false` here has no usable
    /// UWB radio and [Ranging#isSupported()] will also be `false`.
    public boolean isDistanceSupported() {
        return distance;
    }

    /// `true` when the device can report the horizontal direction to a peer.
    /// Both platforms only produce a direction while the peer is roughly in
    /// front of the device, so an update may still omit it -- always check
    /// [RangingUpdate#hasDirection()] as well.
    public boolean isDirectionSupported() {
        return direction;
    }

    /// `true` when the device can report elevation as well as azimuth.
    public boolean isElevationSupported() {
        return elevation;
    }

    /// `true` when the platform can use the camera to converge on a sharper
    /// direction. iOS only, and only while an AR session is running; the
    /// Codename One API does not turn it on by itself.
    public boolean isCameraAssistanceSupported() {
        return cameraAssistance;
    }

    /// `true` when third-party UWB accessories can be ranged, as opposed to
    /// only other phones. See [RangingSession#startAccessory].
    public boolean isAccessoryRangingSupported() {
        return accessoryRanging;
    }

    /// `true` when a session may keep delivering updates while the app is in
    /// the background. On iOS this additionally requires the
    /// `com.apple.developer.nearby-interaction` entitlement, which Codename
    /// One never injects on its own -- see the developer guide.
    public boolean isBackgroundRangingSupported() {
        return backgroundRanging;
    }

    public String toString() {
        return "RangingCapabilities[distance=" + distance
                + ", direction=" + direction
                + ", elevation=" + elevation
                + ", cameraAssistance=" + cameraAssistance
                + ", accessoryRanging=" + accessoryRanging
                + ", backgroundRanging=" + backgroundRanging + "]";
    }
}
