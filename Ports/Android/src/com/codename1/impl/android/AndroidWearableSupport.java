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

import com.codename1.wearable.spi.WearableBridge;

/// Registry that links the Android port to the Wearable Data Layer glue.
///
/// The runtime Android port carries no compile-time dependency on
/// `com.google.android.gms:play-services-wearable` -- it is only on the classpath when the app
/// references `com.codename1.wearable`, at which point the build injects a typed `WearableBridge`
/// implementation plus a `WearableListenerService` into the generated project. The injected bridge
/// registers itself here and `AndroidImplementation#getWearableBridge()` reads it back. Without the
/// glue this stays null and the `com.codename1.wearable` API degrades to a no-op, exactly as it does
/// on a phone with no watch.
///
/// This mirrors {@link AndroidCarSupport}, for the same reason: an optional Google dependency cannot
/// be referenced from the port itself.
///
/// The injected glue lives in the maven-plugin / BuildDaemon resources under
/// `com/codename1/builders/wearable/`.
public final class AndroidWearableSupport {
    private static volatile WearableBridge bridge;
    private static boolean lookedUp;

    private AndroidWearableSupport() {
    }

    /// Returns the injected bridge, or null when the app does not use the wearable API.
    ///
    /// Unlike the in-car glue -- which the system instantiates, so it can register itself -- nothing
    /// creates the wearable bridge on our behalf, so it is looked up reflectively on first use. The
    /// class only exists in the generated project when the build injected it, which is precisely the
    /// condition under which play-services-wearable is on the classpath.
    ///
    /// #### Parameters
    ///
    /// - `context`: the Android context the bridge needs
    ///
    /// #### Returns
    ///
    /// the wearable bridge, or null
    public static synchronized WearableBridge getBridge(android.content.Context context) {
        if (!lookedUp) {
            lookedUp = true;
            try {
                Class<?> c = Class.forName("com.codename1.impl.android.CN1WearableBridge");
                bridge = (WearableBridge) c.getConstructor(android.content.Context.class)
                        .newInstance(context);
            } catch (ClassNotFoundException notInjected) {
                // The app never references com.codename1.wearable; the API stays inert.
            } catch (Throwable err) {
                com.codename1.io.Log.p("Wearable: the Data Layer glue is present but could not be "
                        + "created: " + err);
            }
        }
        return bridge;
    }
}
