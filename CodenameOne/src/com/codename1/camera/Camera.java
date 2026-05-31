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
package com.codename1.camera;

import com.codename1.impl.CameraImpl;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;

import java.io.IOException;

/// Entry point for the low-level cross-platform camera API.
///
/// This API gives the application direct access to the device camera: live
/// preview, frame streaming, still capture, video recording, flash and focus
/// control. It is intended for use cases that the file-based
/// `com.codename1.capture.Capture` API cannot serve - real-time barcode
/// scanning, document boundary detection, custom in-app camera UIs.
///
/// **Permissions**: simply referencing classes in this package causes the build
/// pipeline to inject `NSCameraUsageDescription` /
/// `NSMicrophoneUsageDescription` (iOS) and `android.permission.CAMERA` /
/// `android.permission.RECORD_AUDIO` plus the CameraX gradle dependencies
/// (Android). Developers may override the plist strings via the
/// `ios.NSCameraUsageDescription` build hint.
///
/// **Coexistence with `Capture`**: the old `com.codename1.capture.Capture`
/// API continues to work unchanged. Both may be used in the same app, but
/// only one camera consumer may hold the device at a time. Call
/// `CameraSession#pause()` before invoking `Capture.capturePhoto(...)` and
/// `CameraSession#resume()` afterwards.
///
/// ```java
/// if (Camera.isSupported()) {
///     CameraSession s = Camera.open(Camera.getDefault(CameraFacing.BACK),
///                                   new CameraSessionOptions());
///     CameraView v = s.createView();
///     // add v to a Form...
///     s.setFrameListener(frame ->
///         BarcodeScanner.scan(frame.getJpegBytes()).ready(codes -> { ... }));
/// }
/// ```
///
/// #### Since
///
/// 8.1
public final class Camera {
    private static final Object ACTIVE_LOCK = new Object();
    private static CameraSession active;

    private Camera() { }

    /// True when the running platform has a working camera implementation.
    /// False on platforms (or simulator runs) where the camera back-end could
    /// not be initialized.
    public static boolean isSupported() {
        return Display.getInstance().getCameraBackend() != null;
    }

    /// Enumerate cameras visible to the platform. May be empty.
    public static CameraInfo[] getCameras() {
        CameraImpl probe = newImpl();
        if (probe == null) {
            return new CameraInfo[0];
        }
        try {
            CameraInfo[] out = probe.enumerateCameras();
            return out == null ? new CameraInfo[0] : out;
        } finally {
            try { probe.close(); } catch (Throwable t) { Log.e(t); }
        }
    }

    /// Convenience that returns the first camera matching the given facing,
    /// or `null` if none. When no facing-specific camera is found and any
    /// camera exists, the first available camera is returned.
    public static CameraInfo getDefault(CameraFacing facing) {
        CameraInfo[] all = getCameras();
        for (CameraInfo c : all) {
            if (c.getFacing() == facing) {
                return c;
            }
        }
        return all.length > 0 ? all[0] : null;
    }

    /// Open a camera session. Throws `IllegalStateException` if a session is
    /// already open; close the old session first.
    public static CameraSession open(CameraInfo info, CameraSessionOptions opts) {
        if (info == null) {
            throw new IllegalArgumentException("CameraInfo must not be null");
        }
        if (opts == null) {
            opts = new CameraSessionOptions();
        }
        // The check-and-set has to be atomic to keep SpotBugs happy and to
        // honour the "one open session at a time" contract under
        // concurrent open() calls. The native impl.open() call below runs
        // under the lock as well -- it's a foreground operation that the
        // user kicked off, contention is essentially zero, and we'd rather
        // serialise the rare race than ship a TOCTOU bug.
        synchronized (ACTIVE_LOCK) {
            if (active != null && !active.isClosed()) {
                throw new IllegalStateException(
                    "Only one CameraSession may be open at a time. Close the existing session first.");
            }
            CameraImpl impl = newImpl();
            if (impl == null) {
                throw new IllegalStateException("Camera is not supported on this platform.");
            }
            try {
                impl.open(info.getId(), opts);
            } catch (IOException e) {
                try { impl.close(); } catch (Throwable t) { Log.e(t); }
                throw new RuntimeException("Could not open camera " + info.getId(), e);
            }
            active = new CameraSession(impl, info, opts);
            return active;
        }
    }

    /// Request runtime permission for camera (and optionally microphone). The
    /// callback receives `true` when both are granted, `false` otherwise.
    /// On iOS this is a no-op that delivers `true` immediately; the system
    /// prompts the first time the camera is actually started.
    public static void requestPermissions(final boolean audio, final SuccessCallback<Boolean> callback) {
        CameraImpl impl = newImpl();
        if (impl == null) {
            fireLater(callback, Boolean.FALSE);
            return;
        }
        try {
            impl.open("__permission_probe__", new CameraSessionOptions().captureAudio(audio));
            // open() throws on permission denial; if we get here permissions are granted.
            fireLater(callback, Boolean.TRUE);
        } catch (Throwable t) {
            fireLater(callback, Boolean.FALSE);
        } finally {
            try { impl.close(); } catch (Throwable t) { Log.e(t); }
        }
    }

    private static void fireLater(final SuccessCallback<Boolean> callback, final Boolean value) {
        if (callback == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override public void run() { callback.onSucess(value); }
        });
    }

    private static CameraImpl newImpl() {
        return Display.getInstance().getCameraBackend();
    }

    // Identity comparison is intentional: only the exact CameraSession
    // instance we returned from open() may clear the active slot, so
    // PMD.CompareObjectsWithEquals doesn't apply.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    static void clearActive(CameraSession s) {
        synchronized (ACTIVE_LOCK) {
            if (active == s) {
                active = null;
            }
        }
    }
}
