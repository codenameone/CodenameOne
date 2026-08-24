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
package com.codename1.nearby.companion;

/// Told when an associated device comes into or goes out of range. Both
/// methods are called on the EDT.
///
/// Presence is the reason companion association is worth using for an
/// accessory the app talks to regularly: the OS watches for the device and
/// wakes the app, instead of the app burning battery on a scan it runs
/// itself.
public interface PresenceListener {

    /// The associated device came into range.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device that appeared
    void deviceAppeared(CompanionDevice device);

    /// The associated device went out of range.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device that disappeared
    void deviceDisappeared(CompanionDevice device);
}
