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

/// What kind of thing is being associated.
///
/// The profile is a request for elevated privileges as much as a
/// description: Android grants a watch profile the right to run in the
/// background and stream notifications, and shows the user a correspondingly
/// stronger consent dialog. Ask for [#GENERIC] unless the device really is
/// one of the specific kinds, because the specific profiles cost the user a
/// scarier prompt.
public enum CompanionProfile {
    /// No elevated privileges. The right answer for a sensor, a tag, a
    /// fitness accessory -- anything that is not one of the categories the
    /// platform treats specially.
    GENERIC,

    /// A watch. On Android this is `DEVICE_PROFILE_WATCH`, which carries
    /// background and notification privileges.
    WATCH,

    /// A head-mounted display. Android `DEVICE_PROFILE_GLASSES`.
    GLASSES,

    /// A nearby computer, for cross-device flows. Android
    /// `DEVICE_PROFILE_COMPUTER`.
    COMPUTER
}
