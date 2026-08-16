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
package com.codename1.impl.android;

/// Registry that links the Android port to the injected smart-home bridge.
///
/// The runtime Android port carries no compile-time dependency on Play
/// services or the Google Home APIs -- they are on the classpath only when the
/// app actually uses `com.codename1.home`. The build injects an
/// implementation of [SmartHomeDelegate] into the generated project and
/// registers it here during startup; [AndroidHomeBridge] reads it back.
///
/// When smart home is not bundled this stays null and the API degrades to
/// reporting itself unsupported, exactly as [AndroidHealthSupport] does
/// without Health Connect.
public final class AndroidSmartHomeSupport {

    private static volatile SmartHomeDelegate delegate;

    private AndroidSmartHomeSupport() {
    }

    /// Called by the injected bridge to publish itself at app startup.
    ///
    /// #### Parameters
    ///
    /// - `d`: the bridge, or `null` to clear it
    public static void setDelegate(SmartHomeDelegate d) {
        delegate = d;
    }

    /// The registered bridge, or null when smart home is not bundled.
    ///
    /// #### Returns
    ///
    /// the bridge, or `null`
    public static SmartHomeDelegate getDelegate() {
        return delegate;
    }
}
