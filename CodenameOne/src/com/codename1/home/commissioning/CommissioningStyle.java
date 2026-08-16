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
package com.codename1.home.commissioning;

/// How a backend adds a new accessory, so an app can word its own button
/// honestly.
public enum CommissioningStyle {

    /// The operating system runs the whole flow in its own UI, over your app.
    ///
    /// What both mobile backends do: iOS presents the `MatterSupport` add-device
    /// sheet, Android presents the Play services commissioning activity. Your
    /// app hands over a setup payload and a preference for where the accessory
    /// should land, and gets a single result when the user is finished.
    ///
    /// There is no progress reporting inside it, and the user can take a long
    /// time -- they may have to unscrew a fitting. Do not put a spinner with a
    /// short timeout behind this.
    OS_OWNED_UI,

    /// Your app cannot do it, but it can send the user to the ecosystem app
    /// that can.
    ///
    /// [Commissioner#openEcosystemApp()] is the whole capability here.
    ECOSYSTEM_APP_HANDOFF,

    /// Not possible from this platform at all -- watchOS, tvOS, macOS, or an
    /// Android device with no Play services.
    NONE
}
