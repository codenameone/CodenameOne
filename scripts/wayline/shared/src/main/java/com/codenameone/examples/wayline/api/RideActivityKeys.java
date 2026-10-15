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
package com.codenameone.examples.wayline.api;

/// The words the app and the server agree on for a ride's Live Activity: the
/// "your driver is on the way" card on the lock screen.
///
/// The app starts the activity with a state built from these keys, and the
/// server sends later states with the same keys.
public final class RideActivityKeys {
    /// The kind of activity.
    public static final String KIND = "ride";
    /// What is happening, in words: "On the way", "Your driver is here".
    public static final String STATUS = "status";
    /// The driver's name.
    public static final String DRIVER = "driver";
    /// The car, as the driver described it.
    public static final String CAR = "car";
    /// The car's license plate.
    public static final String PLATE = "plate";
    /// When the car gets where it is going next, in words: "4 min", or empty
    /// once it is there.
    public static final String ETA = "eta";
    /// How far along the ride is, a number from 0 to 1 for the progress bar.
    public static final String PROGRESS = "progress";

    /// The values of [#PROGRESS] at each step.
    public static final double ON_THE_WAY = 0.25d;
    public static final double ARRIVED = 0.5d;
    public static final double IN_TRIP = 0.75d;
    public static final double DONE = 1d;

    private RideActivityKeys() {
    }
}
