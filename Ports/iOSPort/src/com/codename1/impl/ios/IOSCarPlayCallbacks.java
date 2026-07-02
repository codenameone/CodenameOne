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
package com.codename1.impl.ios;

import com.codename1.car.Car;
import com.codename1.car.CarActionListener;
import com.codename1.car.CarContext;
import com.codename1.ui.Display;

/// Static callback surface invoked from the native CarPlay scene delegate
/// (`CodenameOne_CarPlaySceneDelegate`). Mirrors the `IOSDeviceIntegrity` pattern: the static
/// initializer calls each native callback once (guarded so it has no effect) purely to keep the
/// ParparVM dead-code eliminator from stripping the callback targets, which otherwise have no Java
/// caller.
final class IOSCarPlayCallbacks {
    private static IOSCarBridge bridge;
    private static boolean dceGuard;

    static {
        // Keep the native callback targets reachable for the iOS VM optimizer.
        dceGuard = true;
        nativeCarConnected();
        nativeCarDisconnected();
        nativeElementSelected(-1, null);
        dceGuard = false;
    }

    private IOSCarPlayCallbacks() {
    }

    /// Returns the singleton CarPlay bridge, creating it on first use.
    static synchronized IOSCarBridge getBridge(IOSNative nativeInstance) {
        if (bridge == null) {
            bridge = new IOSCarBridge(nativeInstance);
        }
        return bridge;
    }

    // ---- Callbacks invoked from native code (do not rename) ----------------

    /// Called from native when a CarPlay head unit connects and the interface controller is ready.
    public static void nativeCarConnected() {
        if (dceGuard) {
            return;
        }
        IOSCarBridge b = getBridge(IOSImplementation.nativeInstance);
        Car.startSession(b);
    }

    /// Called from native when the CarPlay head unit disconnects.
    public static void nativeCarDisconnected() {
        if (dceGuard) {
            return;
        }
        Car.endSession();
    }

    /// Called from native when the user selects a row / grid item / action. The {@code elementId}
    /// matches the id assigned while the template's JSON was built.
    public static void nativeElementSelected(final int screenId, final String elementId) {
        if (dceGuard) {
            return;
        }
        if (bridge == null || elementId == null) {
            return;
        }
        final CarActionListener l = bridge.takeListener(screenId, elementId);
        if (l == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                CarContext ctx = Car.getCurrentContext();
                if (ctx != null) {
                    IOSCarBridge.invoke(l, ctx);
                }
            }
        });
    }
}
