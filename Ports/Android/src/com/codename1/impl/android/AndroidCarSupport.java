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

import com.codename1.car.spi.CarBridge;

/// Registry that links the Android port to the Android Auto glue. The runtime Android port carries
/// no compile-time dependency on `androidx.car.app` (it is only on the classpath when the
/// `android.androidAuto` build hint bundles it). The build injects a typed `CarAppService` /
/// `Session` / `Screen` plus a `CarBridge` implementation into the generated project; that injected
/// `CarAppService` registers its bridge here on creation, and `AndroidImplementation#getCarBridge()`
/// reads it back. When Android Auto is not bundled this stays null, so the `com.codename1.car` API
/// degrades to a no-op exactly as it does on the simulator.
///
/// The injected glue lives in the maven-plugin / BuildDaemon resources under
/// `com/codename1/builders/car/` and is copied into the app's source tree only when the app
/// references `com.codename1.car`.
public final class AndroidCarSupport {
    private static volatile CarBridge bridge;

    private AndroidCarSupport() {
    }

    /// Called by the injected Android Auto `CarAppService` glue to publish the live bridge for the
    /// connected head unit.
    ///
    /// #### Parameters
    ///
    /// - `b`: the bridge for the connected head unit, or null on disconnect
    public static void setBridge(CarBridge b) {
        bridge = b;
    }

    /// Returns the bridge published by the injected glue, or null when Android Auto is not bundled or
    /// no head unit is connected.
    ///
    /// #### Returns
    ///
    /// the current car bridge, or null
    public static CarBridge getBridge() {
        return bridge;
    }
}
