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
///     s.setFrameListener(frame -> analyze(frame.getJpegBytes()));
/// }
/// ```
///
/// **For on-device analysis, do not write the frame listener yourself.**
/// `com.codename1.ai.vision.VisionCameraView` is a component that owns the
/// session, streams frames into an analyzer with keep-only-the-newest
/// backpressure, and delivers results on the EDT; and
/// `com.codename1.ai.vision.CodeScanner` is an entire barcode scanner screen
/// in one call. Use this class directly when the application drives the
/// camera itself -- a custom capture UI, video recording, or torch and zoom
/// control.
public final class Camera {
    private static final Object ACTIVE_LOCK = new Object();
    private static CameraSession active;
    /// Paused sessions a later open() was let past, oldest first.
    ///
    /// A STACK rather than one slot, because the handoffs nest: pause A, open
    /// and pause B, open C. A single slot held only the most recent, so closing
    /// C restored B and closing B then left no active session at all -- A was
    /// forgotten while still holding the hardware it was about to resume, and
    /// the next open() was accepted alongside it.
    private static final java.util.ArrayList<CameraSession> preempted =
            new java.util.ArrayList<CameraSession>();

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
            try {
                probe.close();
            } catch (Throwable t) {
                Log.e(t);
            }
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
            // A PAUSED session is holding nothing, and letting it through is
            // the documented coexistence flow: pause the session, run
            // Capture.capturePhoto/captureVideo, resume. Refusing here made
            // that flow throw -- the modal capture then answered its listener
            // with null having shown no UI at all -- so the API promised a
            // handoff its own exclusivity gate forbade.
            //
            // The gate still means what it says: it exists to stop two
            // consumers HOLDING the device, and pause() is documented as
            // releasing the hardware while keeping the session object alive.
            if (active != null && !active.isClosed() && !active.isPaused()) {
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
                try {
                    impl.close();
                } catch (Throwable t) {
                    Log.e(t);
                }
                throw new RuntimeException("Could not open camera " + info.getId(), e);
            }
            // Remembered so exclusivity survives the handoff. Without this the
            // capture's own close() would leave no active session at all, and
            // the paused one -- which the application is about to resume --
            // would no longer stop a third open().
            //
            // Recorded only once the successor has actually opened, and it has
            // to be this way round. Nothing is preempted by an open that
            // failed: the paused session never lost the camera, and it stays
            // the active one. Recording it before the attempt meant every
            // unsuccessful retry -- a camera briefly busy is the ordinary case
            // -- left another entry that no path removed, because the throw
            // leaves through neither close(). The list only drains through
            // clearActive(), which the successor that never existed can never
            // call, so a long-lived paused session accumulated one entry per
            // retry for as long as it lived.
            //
            // Nothing between the exclusivity check and here can have changed
            // what is being preempted: ACTIVE_LOCK is held across the whole
            // method, and active is only ever reassigned under it.
            if (active != null && !active.isClosed() && active.isPaused()) {
                preempted.add(active);
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
            try {
                impl.close();
            } catch (Throwable t) {
                Log.e(t);
            }
        }
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

    private static CameraImpl newImpl() {
        return Display.getInstance().getCameraBackend();
    }

    // Identity comparison is intentional: only the exact CameraSession
    // instance we returned from open() may clear the active slot, so
    /// Releases a session's hardware, under the lock open() reads.
    ///
    /// The flag and the release become visible together, so an opener cannot
    /// see a session that is still "running" while its hardware is already
    /// gone -- which would refuse the very handoff the application paused for.
    static void pauseSession(CameraSession s) {
        synchronized (ACTIVE_LOCK) {
            s.pauseUnderLock();
        }
    }

    /// Takes the hardware back, refusing if another session holds the slot.
    ///
    /// pause() and resume() never had to consult anything while sessions could
    /// not overlap. They can now -- that is the documented pause / Capture /
    /// resume handoff -- and resume() reacquires the hardware immediately, so a
    /// session resumed while the capture that was let past is still open puts
    /// two consumers on the device. On the Apple backend resume() also takes
    /// the singleton frame-callback target back, so the running capture's
    /// frames start arriving at the resumed session's listener.
    ///
    /// The invariant is the one open() already enforces: only the ACTIVE
    /// session may hold the camera. In the documented flow the capture's
    /// close() has handed the slot back by the time the application resumes, so
    /// that flow is unaffected; resuming before then is the misuse this names.
    ///
    /// Check and reacquisition are ONE step, under the same lock open() takes.
    /// Checking and then resuming outside the lock left a window where an
    /// opener saw this session paused, installed a successor and started it,
    /// while this thread took the hardware back anyway. open() holds this lock
    /// across its own native call for exactly that reason, and says so.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    static void resumeSession(CameraSession s) {
        synchronized (ACTIVE_LOCK) {
            if (active != null && active != s && !active.isClosed()) {
                throw new IllegalStateException(
                    "Another CameraSession is using the camera. Close it before resuming this one.");
            }
            s.resumeUnderLock();
        }
    }

    // PMD.CompareObjectsWithEquals doesn't apply: these are identity tests on
    // session objects, which is the whole point of them.
    //
    // PMD.CloseResource does not apply either, and following it would be a bug.
    // The session taken off the stack here is being handed BACK as the active
    // one, because the application paused it around a capture and is about to
    // resume it. This method does not own it and must not close it; closing it
    // would destroy a live session at the moment it regains the camera. The
    // only session being closed anywhere near here is the caller's own, which
    // is why it is calling.
    @SuppressWarnings({"PMD.CompareObjectsWithEquals", "PMD.CloseResource"})
    static void clearActive(CameraSession s) {
        synchronized (ACTIVE_LOCK) {
            if (active == s) {
                // Hand back to the most recent session this one was let past,
                // if there is still one to hand back to: the application paused
                // it around a modal capture and is about to resume it, and it
                // has to be the active session again for the next open() to be
                // refused. Sessions the application closed while they waited
                // are discarded on the way.
                active = null;
                while (!preempted.isEmpty()) {
                    CameraSession candidate = preempted.remove(preempted.size() - 1);
                    if (!candidate.isClosed()) {
                        active = candidate;
                        break;
                    }
                }
            } else {
                // Closed while preempted -- the application closed the paused
                // session instead of resuming it -- so there is nothing to hand
                // back to for that one.
                for (int i = preempted.size() - 1; i >= 0; i--) {
                    if (preempted.get(i) == s) {
                        preempted.remove(i);
                    }
                }
            }
        }
    }
}
