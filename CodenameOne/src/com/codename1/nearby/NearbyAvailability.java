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
package com.codename1.nearby;

/// How much of a `com.codename1.nearby` feature is really usable right now.
///
/// This is deliberately finer than a boolean, because the three interesting
/// cases behave differently: an app on a desktop simulator should show its
/// full UI against simulated peers, an app on an iPhone SE should hide the
/// ranging feature outright, and an app whose user switched the radio off
/// should ask them to switch it back on rather than hide anything.
public enum NearbyAvailability {
    /// The real platform feature is present and usable.
    AVAILABLE,

    /// A simulated implementation is active. Everything works, but nothing
    /// outside this process can see it -- this is what the desktop ports,
    /// the simulator and the JavaScript port report so that ranging and
    /// association UI is developable without hardware. Never returned by a
    /// device port.
    LOCAL_ONLY,

    /// The platform supports the feature but a required permission has not
    /// been granted. Recoverable: call the entry point's
    /// `requestPermissions` method.
    UNAUTHORIZED,

    /// The platform supports the feature but the radio it needs is off or
    /// temporarily unavailable. Recoverable without any action from the app
    /// beyond asking the user to enable it.
    TEMPORARILY_UNAVAILABLE,

    /// This port, OS version or device cannot do it at all. Hide the
    /// feature.
    NOT_SUPPORTED
}
