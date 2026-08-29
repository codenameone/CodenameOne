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
package com.codename1.impl.mac;

import com.codename1.camera.CameraSessionOptions;
import com.codename1.impl.ios.IOSCameraImpl;
import com.codename1.impl.ios.IOSImplementation;
import java.io.IOException;

/// The low level camera backend, with a permission probe that actually asks.
///
/// `Camera.requestPermissions(audio, callback)` is implemented by opening a
/// session named `__permission_probe__` and reporting success when that does not
/// throw. The iOS backend returns from the probe immediately -- its comment says
/// so, and gives the reason: iOS resolves camera permission inline when the
/// session runs, and there is no synchronous way to ask. So the callback reported
/// granted whatever the real answer was.
///
/// macOS can do better, because AVFoundation's
/// `authorizationStatusForMediaType:` answers synchronously and without
/// prompting. A denial is now reported as one.
public class MacCameraImpl extends IOSCameraImpl {

    /// AVAuthorizationStatus, as AVFoundation numbers it.
    private static final int NOT_DETERMINED = 0;
    private static final int RESTRICTED = 1;
    private static final int DENIED = 2;

    @Override
    public void open(String cameraId, CameraSessionOptions opts) throws IOException {
        if ("__permission_probe__".equals(cameraId)) {
            CameraSessionOptions o = opts == null ? new CameraSessionOptions() : opts;
            int video = IOSImplementation.cameraAuthorizationStatus(false);
            if (video == DENIED || video == RESTRICTED) {
                throw new IOException("camera access has been denied for this application");
            }
            boolean audioUndecided = false;
            if (o.isCaptureAudio()) {
                int audio = IOSImplementation.cameraAuthorizationStatus(true);
                if (audio == DENIED || audio == RESTRICTED) {
                    throw new IOException("microphone access has been denied for this "
                            + "application, and this session was asked to capture audio");
                }
                audioUndecided = audio == NOT_DETERMINED;
            }
            // Either requested type being undecided has to prompt, not just the
            // camera. A session that already holds camera permission but has
            // never asked for the microphone would otherwise be reported as
            // fully granted without the microphone prompt ever being shown, and
            // then record a silent movie. Asking for an already-authorized type
            // is free: requestAccessForMediaType returns immediately without a
            // prompt, so the mixed state costs the user one dialog, the one
            // that was actually missing.
            if (video == NOT_DETERMINED || audioUndecided) {
                // Nobody has been asked yet, so ask and WAIT for the answer.
                // Firing the prompt and returning normally would report the
                // permission as granted before the user had touched the dialog,
                // and a later denial would arrive after the application had
                // already been told it could open a session.
                //
                // invokeAndBlock is what makes waiting legal here: the result
                // is delivered on the EDT, so blocking the EDT outright would
                // deadlock against the very callback being waited for. This is
                // the pattern createAudioRecorder() already uses in
                // IOSImplementation for the same reason.
                final boolean[] answered = new boolean[1];
                final boolean[] granted = new boolean[1];
                final Object lock = new Object();
                IOSImplementation.requestCameraAccess(o.isCaptureAudio(),
                        new com.codename1.util.SuccessCallback<Boolean>() {
                    @Override
                    public void onSucess(Boolean value) {
                        synchronized (lock) {
                            granted[0] = value != null && value.booleanValue();
                            answered[0] = true;
                            lock.notifyAll();
                        }
                    }
                });
                if (com.codename1.ui.Display.getInstance().isEdt()) {
                    com.codename1.ui.Display.getInstance().invokeAndBlock(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (lock) {
                                while (!answered[0]) {
                                    com.codename1.io.Util.wait(lock);
                                }
                            }
                        }
                    });
                } else {
                    // Off the EDT there is no dispatch thread to free, so an
                    // ordinary wait is both sufficient and correct.
                    synchronized (lock) {
                        while (!answered[0]) {
                            com.codename1.io.Util.wait(lock);
                        }
                    }
                }
                if (!granted[0]) {
                    throw new IOException("camera access was denied for this application");
                }
            }
            return;
        }
        super.open(cameraId, opts);
    }
}
