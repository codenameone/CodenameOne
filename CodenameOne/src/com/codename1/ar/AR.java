/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ar;

import com.codename1.impl.ARImpl;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

import java.io.IOException;

/// Entry point for the cross-platform augmented reality API.
///
/// AR sessions track the device's pose in the real world through the camera,
/// detect surfaces, and composite virtual 3D content into the camera image.
/// The typical flow: check `#isSupported()`, `#open(ARSessionOptions)` a
/// session, add its `ARSession#createView()` to a form, then place content by
/// hit testing taps against detected planes and hanging `ARNode`s on the
/// resulting anchors.
///
/// **Permissions and dependencies**: simply referencing classes in this
/// package causes the build pipeline to inject the camera permission, the
/// `NSCameraUsageDescription` plist entry (iOS, override via the
/// `ios.NSCameraUsageDescription` build hint) and the ARCore dependency
/// (Android). Devices without AR support report `#isSupported()` false;
/// always guard AR functionality behind that check.
///
/// ```java
/// if (AR.isSupported()) {
///     ARSession s = AR.open(new ARSessionOptions());
///     Form f = new Form("AR", new BorderLayout());
///     f.add(BorderLayout.CENTER, s.createView());
///     f.show();
///     // on tap: hit test and place a model
///     s.hitTest(xNorm, yNorm).ready(hits -> {
///         if (hits.length > 0) {
///             ARAnchor a = hits[0].createAnchor();
///             a.setNode(new ARNode(ARModel.fromGltf(modelBytes)));
///         }
///     });
/// }
/// ```
public final class AR {
    private static final Object ACTIVE_LOCK = new Object();
    private static ARSession active;

    private AR() {
    }

    /// True when the running platform has a working AR implementation. False
    /// on platforms and devices without AR support.
    public static boolean isSupported() {
        return Display.getInstance().getARBackend() != null;
    }

    /// Returns which AR features this device supports. On unsupported
    /// platforms every capability reads false.
    public static ARCapabilities getCapabilities() {
        ARImpl probe = newImpl();
        if (probe == null) {
            return ARCapabilities.UNSUPPORTED;
        }
        try {
            ARCapabilities caps = probe.getCapabilities();
            return caps == null ? ARCapabilities.UNSUPPORTED : caps;
        } finally {
            closeQuietly(probe);
        }
    }

    /// Opens an AR session. Throws `IllegalStateException` when AR is
    /// unsupported or a session is already open; close the old session first.
    ///
    /// #### Parameters
    ///
    /// - `opts`: the session configuration; null uses the defaults
    ///
    /// #### Returns
    ///
    /// the running session
    public static ARSession open(ARSessionOptions opts) {
        if (opts == null) {
            opts = new ARSessionOptions();
        }
        // The check-and-set is atomic so the "one open session at a time"
        // contract holds under concurrent open() calls; contention is
        // essentially zero since opening AR is a foreground user action.
        synchronized (ACTIVE_LOCK) {
            if (active != null && !active.isClosed()) {
                throw new IllegalStateException(
                        "Only one ARSession may be open at a time. Close the existing session first.");
            }
            ARImpl impl = newImpl();
            if (impl == null) {
                throw new IllegalStateException("AR is not supported on this platform.");
            }
            ARSession session = new ARSession(impl, opts);
            try {
                impl.open(opts);
            } catch (IOException e) {
                session.close();
                throw new RuntimeException("Could not open the AR session", e);
            }
            active = session;
            return session;
        }
    }

    /// Requests the camera permission needed for AR. The callback receives
    /// true when granted; it is invoked on the EDT.
    ///
    /// #### Parameters
    ///
    /// - `callback`: receives the grant result; may be null
    public static void requestPermissions(final SuccessCallback<Boolean> callback) {
        final ARImpl impl = newImpl();
        if (impl == null) {
            fireLater(callback, Boolean.FALSE);
            return;
        }
        AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        result.ready(new SuccessCallback<Boolean>() {
            @Override public void onSucess(Boolean value) {
                closeQuietly(impl);
                if (callback != null) {
                    callback.onSucess(Boolean.valueOf(value != null && value.booleanValue()));
                }
            }
        });
        result.except(new SuccessCallback<Throwable>() {
            @Override public void onSucess(Throwable t) {
                closeQuietly(impl);
                if (callback != null) {
                    callback.onSucess(Boolean.FALSE);
                }
            }
        });
        impl.requestPermissions(result);
    }

    private static void fireLater(final SuccessCallback<Boolean> callback, final Boolean value) {
        if (callback == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override public void run() {
                callback.onSucess(value);
            }
        });
    }

    private static void closeQuietly(ARImpl impl) {
        try {
            impl.close();
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private static ARImpl newImpl() {
        return Display.getInstance().getARBackend();
    }

    // Identity comparison is intentional: only the exact ARSession instance
    // returned from open() may clear the active slot.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    static void clearActive(ARSession s) {
        synchronized (ACTIVE_LOCK) {
            if (active == s) {
                active = null;
            }
        }
    }
}
