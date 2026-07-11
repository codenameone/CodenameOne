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

import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.surfaces.Surfaces;

import java.util.Map;

/// Static callback surface invoked from the native surfaces glue (the `cn1surface://` deep link
/// handling in `CodenameOne_GLAppDelegate`). Mirrors the `IOSCarPlayCallbacks` pattern: the static
/// initializer calls each native callback once (guarded so it has no effect) purely to keep the
/// ParparVM dead-code eliminator from stripping the callback targets, which otherwise have no Java
/// caller.
final class IOSSurfaceCallbacks {
    private static IOSSurfaceBridge bridge;
    private static boolean dceGuard;

    static {
        // Keep the native callback targets reachable for the iOS VM optimizer.
        dceGuard = true;
        nativeSurfaceAction(null, null, null);
        dceGuard = false;
    }

    private IOSSurfaceCallbacks() {
    }

    /// Returns the singleton surfaces bridge, creating it on first use.
    static synchronized IOSSurfaceBridge getBridge(IOSNative nativeInstance) {
        if (bridge == null) {
            bridge = new IOSSurfaceBridge(nativeInstance);
        }
        return bridge;
    }

    // ---- Callbacks invoked from native code (do not rename) ----------------

    /// Called from native when the user taps a widget or live activity element carrying an
    /// action; the parameters mirror the canonical deep link
    /// `cn1surface://a?src=..&id=..&p=<url-encoded JSON>`. `Surfaces.dispatchAction` handles EDT
    /// marshaling and cold-start queuing internally, so invoking this early in the launch
    /// sequence is safe.
    public static void nativeSurfaceAction(String source, String actionId, String paramsJson) {
        if (dceGuard) {
            return;
        }
        Map<String, Object> params = null;
        if (paramsJson != null && paramsJson.length() > 0) {
            try {
                params = JSONParser.parseJSON(paramsJson);
            } catch (Throwable t) {
                Log.e(t);
            }
        }
        Surfaces.dispatchAction(source, actionId, params);
    }
}
