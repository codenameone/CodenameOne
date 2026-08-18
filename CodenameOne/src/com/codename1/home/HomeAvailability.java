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

/// Whether a home graph is usable right now, and when it is not, why.
///
/// Check this before anything else. Several of these states are recoverable by
/// the user, and the recovery differs -- sending someone to Google Play for a
/// provider update when what they actually need is to sign in helps nobody.
public enum HomeAvailability {

    /// Full support: the graph is readable, traits can be read and written,
    /// scenes can be run.
    AVAILABLE,

    /// **Matter commissioning works and nothing else does.** The graph is
    /// empty; no trait can be read or written.
    ///
    /// This is what Android reports by default, and it is the most important
    /// distinction in this enum. Google Play services can commission a Matter
    /// accessory into the user's Google Home with no setup at all, but reading
    /// or controlling that accessory afterwards needs the Google Home APIs,
    /// which need a Google Cloud project and a Home Developer Console
    /// registration that only the app's developer can create.
    ///
    /// Reporting `AVAILABLE` here would make the constant mean something
    /// entirely different on Android than on iOS, and an app written against
    /// the iOS meaning would show an empty home with no explanation. See
    /// [SmartHome#getConfigurationProblems()] for what to enable.
    COMMISSIONING_ONLY,

    /// Smart-home support exists and the user has not been asked yet. Call
    /// [SmartHome#requestAuthorization()].
    PERMISSION_REQUIRED,

    /// The backend needs a signed-in account before anything is visible.
    /// Google Home only.
    SIGN_IN_REQUIRED,

    /// Everything is authorized and there are no homes to show.
    ///
    /// Its own state rather than an empty `AVAILABLE`, because it is both
    /// common and actionable: HomeKit exists on every iOS device, so
    /// "supported" is not a useful answer to a user who has never opened the
    /// Home app. Send them to [SmartHome#openEcosystemApp()].
    NOT_CONFIGURED,

    /// Access is blocked by parental controls or device management. Not
    /// recoverable from inside the app.
    RESTRICTED,

    /// The platform provider app is missing -- Google Play services on
    /// Android. Recoverable via [SmartHome#openProviderSetup()].
    PROVIDER_NOT_INSTALLED,

    /// The platform provider is installed but too old. Also recoverable via
    /// [SmartHome#openProviderSetup()].
    PROVIDER_UPDATE_REQUIRED,

    /// A local, app-private simulated home. Reads and writes work and are
    /// durable, but nothing outside this app can see the accessories and no
    /// physical device is involved.
    ///
    /// Reported by the simulator, the desktop ports and the JavaScript port.
    /// The simulator can be scripted to report any other constant in this
    /// enum, so an app's recovery branches are reachable without a device.
    LOCAL_ONLY,

    /// No smart-home support on this port, OS version or build. Where the
    /// cause is a missing build hint or entitlement rather than the platform,
    /// [SmartHome#getConfigurationProblems()] says which.
    NOT_SUPPORTED,

    /// The user was asked and said no.
    ///
    /// Its own state rather than [#PERMISSION_REQUIRED], because the recovery
    /// is different and the wrong one gets nowhere: iOS asks for HomeKit once
    /// and never again, so an app that answers a refusal by calling
    /// [SmartHome#requestAuthorization()] gets the same refusal back with no
    /// prompt shown and no way for the user to see what is being asked.
    /// Send them to [SmartHome#openHomeSettings()] instead.
    ///
    /// New constants go after this one: the ports carry these across as
    /// ordinals.
    PERMISSION_DENIED,

    /// Nothing has connected to the backend yet, so there is nothing to
    /// report. Call [SmartHome#refresh()], which connects.
    ///
    /// Its own state rather than [#PERMISSION_REQUIRED], which iOS reported
    /// on every cold launch: creating HomeKit's manager is what prompts, so
    /// until something does, this process cannot tell a user who has already
    /// authorized from one who has been asked and refused from one with no
    /// home set up. Answering "not asked" for all three sent an app that
    /// follows the documented branch into requestAuthorization() when the
    /// user had already answered, and iOS asks once -- so nothing appeared on
    /// screen and the app returned having done nothing.
    ///
    /// Last in this enum, and new constants go after it.
    NOT_STARTED
}
