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
package com.codename1.health;

/// Whether a platform health store is usable right now, and if not, why.
/// Check this before anything else -- on Android the provider app may be
/// missing or out of date, which is recoverable by the user.
public enum HealthAvailability {

    /// A platform health store is present and usable.
    AVAILABLE,

    /// No platform store, but this port keeps health data locally (the
    /// simulator, and the desktop and JavaScript ports). Reads and writes
    /// work and are durable, but the data is this app's own -- nothing is
    /// shared with other apps, and no device or third-party app writes
    /// into it.
    ///
    /// Apps that only aggregate their own measurements can treat this as
    /// working. Apps whose whole purpose is reading what other apps
    /// recorded should tell the user the feature needs a phone.
    LOCAL_ONLY,

    /// Android only: the Health Connect provider is installed but too old.
    /// Send the user to [Health#openProviderSetup()].
    PROVIDER_UPDATE_REQUIRED,

    /// Android only: the Health Connect provider app is not installed.
    /// Send the user to [Health#openProviderSetup()].
    PROVIDER_NOT_INSTALLED,

    /// This platform has no health support at all.
    NOT_SUPPORTED
}
