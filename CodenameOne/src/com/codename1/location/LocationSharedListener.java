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
package com.codename1.location;

/// Receives the location a [LocationButton] obtained.
///
/// One method, so it can be written as a lambda:
///
/// ```java
/// LocationButton b = new LocationButton();
/// b.addLocationSharedListener(loc -> {
///     if (loc == null) {
///         infoLabel.setText("Location not shared");
///     } else {
///         infoLabel.setText(loc.getLatitude() + ", " + loc.getLongitude());
///     }
///     infoLabel.getParent().revalidate();
/// });
/// ```
public interface LocationSharedListener {

    /// Invoked on the EDT once the button has finished, whether or not it
    /// produced a location.
    ///
    /// #### Parameters
    ///
    /// - `location`: the location the user shared, or null when they declined
    ///   the request, no fix arrived before the button's timeout, or the
    ///   platform's own control failed. The last of those can arrive without a
    ///   tap: the platform opens its session when the control is attached, so
    ///   it can fail before anyone touches the button.
    void locationShared(Location location);
}
