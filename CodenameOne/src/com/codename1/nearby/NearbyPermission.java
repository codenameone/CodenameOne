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

/// The runtime permissions the `com.codename1.nearby` APIs may need, named
/// by what the app is trying to do rather than by any one platform's
/// permission string.
///
/// Each entry point takes these as varargs in its `requestPermissions`
/// method and maps them to whatever the running platform actually asks for:
/// on Android a set of manifest permissions, on iOS an authorization prompt
/// raised by the first call that needs it. A port that needs no permission
/// for a given constant reports it granted rather than failing.
public enum NearbyPermission {
    /// Precision ranging. Android `UWB_RANGING`; on iOS the Nearby
    /// Interaction authorization prompted by the first session.
    RANGING,

    /// Discovering nearby devices to advertise to or range against. Android
    /// `BLUETOOTH_SCAN` plus `NEARBY_WIFI_DEVICES` (or location below API
    /// 33); on iOS the local network authorization.
    DISCOVERY,

    /// Advertising this device so others can find it. Android
    /// `BLUETOOTH_ADVERTISE`; no iOS equivalent.
    ADVERTISE,

    /// Connecting to a discovered device and moving payloads. Android
    /// `BLUETOOTH_CONNECT`; no iOS equivalent.
    CONNECT
}
