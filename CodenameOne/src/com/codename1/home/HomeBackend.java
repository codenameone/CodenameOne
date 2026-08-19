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
package com.codename1.home;

/// Which platform service is behind this [SmartHome] instance.
///
/// Provided so an app can **explain** a limitation, not so it can branch
/// around one. Every real capability question has its own query --
/// [SmartHome#getAvailability()], [Trait] support on a service,
/// [TraitSubscription#isPushDelivery()],
/// [com.codename1.home.commissioning.Commissioner#isSupported()] -- and code
/// that switches on this enum instead will be wrong the first time a backend
/// gains or loses something. Use it for the sentence you show the user.
public enum HomeBackend {

    /// Apple HomeKit, on iOS, iPadOS, watchOS, tvOS and macOS.
    HOMEKIT,

    /// The Google Home APIs on Android: the full structure, room, device and
    /// trait graph.
    GOOGLE_HOME,

    /// Google Play services Matter commissioning on Android, with no
    /// accessory graph behind it. Pairs with
    /// [HomeAvailability#COMMISSIONING_ONLY].
    MATTER_COMMISSIONING_ONLY,

    /// The app-private simulated home used by the simulator, the desktop
    /// ports and the JavaScript port.
    LOCAL,

    /// No backend at all -- the inert fallback.
    NONE
}
