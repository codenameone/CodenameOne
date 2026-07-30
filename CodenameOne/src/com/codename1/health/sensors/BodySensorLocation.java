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
package com.codename1.health.sensors;

/// The values of the Body Sensor Location characteristic, `0x2A38`, as
/// reported by [SensorSession#getBodySensorLocation()].
///
/// Worth surfacing: a wrist-worn optical sensor and a chest strap have
/// materially different accuracy during hard efforts, and telling the user
/// which one is feeding a reading is more honest than presenting both as
/// equivalent.
public final class BodySensorLocation {

    /// Somewhere the profile does not name.
    public static final int OTHER = 0;
    /// Chest -- a strap; the most accurate of these placements.
    public static final int CHEST = 1;
    /// Wrist -- an optical sensor in a watch or band.
    public static final int WRIST = 2;
    /// Finger.
    public static final int FINGER = 3;
    /// Hand.
    public static final int HAND = 4;
    /// Earlobe.
    public static final int EAR_LOBE = 5;
    /// Foot.
    public static final int FOOT = 6;

    private BodySensorLocation() {
    }

    /// Whether `location` is one of the placements this profile defines.
    ///
    /// Everything from 7 up is reserved, and a peripheral that answers
    /// with one is not describing a placement at all. Passed through, it
    /// reached the app as a real location that [#describe(int)] could
    /// only call "Unknown" -- which reads as a sensor worn somewhere
    /// unnamed rather than as no answer.
    public static boolean isDefined(int location) {
        return location >= OTHER && location <= FOOT;
    }

    /// A human-readable name for a location constant.
    public static String describe(int location) {
        switch (location) {
            case CHEST: return "Chest";
            case WRIST: return "Wrist";
            case FINGER: return "Finger";
            case HAND: return "Hand";
            case EAR_LOBE: return "Ear lobe";
            case FOOT: return "Foot";
            case OTHER: return "Other";
            default: return "Unknown";
        }
    }
}
