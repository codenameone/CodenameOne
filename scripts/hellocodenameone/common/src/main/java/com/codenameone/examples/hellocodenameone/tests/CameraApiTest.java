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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.camera.Camera;
import com.codename1.camera.CameraFacing;
import com.codename1.camera.CameraFrame;
import com.codename1.camera.CameraInfo;
import com.codename1.camera.CameraSession;
import com.codename1.camera.CameraSessionOptions;
import com.codename1.camera.CameraView;
import com.codename1.camera.CapturedPhoto;
import com.codename1.camera.FrameListener;
import com.codename1.ui.Display;
import com.codename1.ui.PeerComponent;
import com.codename1.util.AsyncResource;

import java.util.concurrent.atomic.AtomicInteger;

/// End-to-end exercise of the low-level `com.codename1.camera.Camera` API
/// against whichever per-port `CameraImpl` is in use.
///
/// On the JavaSE simulator (where `JavaSECameraImpl` synthesises frames) and
/// JavaScript (where the Playwright runner supplies Chromium's deterministic
/// fake media device) this runs the full assertion chain in CI without a
/// physical webcam or permission prompt. On iOS / Android the camera open call
/// would still surface an OS permission dialog, so the test self-skips there.
/// The native-port code paths are verified separately:
///   - iOS: device build sanity + XCTest screenshot suite
///   - Android: instrumentation tests against a granted-permissions
///     `--grant-runtime-permissions` install
///   - JavaScript: this test, through getUserMedia, MediaStream, video/canvas
///     capture and JPEG encoding
///
/// No `Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot` -- this is an
/// assertion test, not a screenshot test.
public class CameraApiTest extends BaseTest {

    private static final long FRAME_TIMEOUT_MS = 8000;
    private static final long PHOTO_TIMEOUT_MS = 8000;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        String platform = Display.getInstance().getPlatformName();
        // JavaSE uses its synthetic CameraImpl. The JavaScript Playwright runner
        // supplies Chromium's fake media device, which still exercises the real
        // HTML5 getUserMedia/video/canvas/JPEG path. Native mobile ports need an
        // OS permission dialog, and Windows does not implement host webcam
        // capture yet, so those remain outside this cross-port headless test.
        boolean isHeadlessCameraSupported = "HTML5".equals(platform)
                || (!"ios".equals(platform)
                && !"and".equals(platform)
                && !"win".equals(platform));
        if (!isHeadlessCameraSupported) {
            System.out.println("CN1SS:INFO:test=CameraApiTest status=SKIPPED reason=needs-runtime-permission-on-" + platform);
            done();
            return true;
        }

        try {
            if (!Camera.isSupported()) {
                fail("Camera.isSupported() should be true on the simulator");
                return false;
            }

            CameraInfo[] all = Camera.getCameras();
            if (all == null || all.length == 0) {
                fail("Camera.getCameras() returned no cameras");
                return false;
            }
            CameraInfo back = Camera.getDefault(CameraFacing.BACK);
            if (back == null) {
                fail("Camera.getDefault(BACK) returned null");
                return false;
            }

            // Open the session, attach a frame listener, verify at least one
            // frame is delivered, take a photo, close. The session bracket
            // is explicit instead of try-with-resources because the test
            // framework targets Java 5 syntax.
            CameraSessionOptions opts = new CameraSessionOptions()
                    .previewSize(320, 240)
                    .frameMaxFps(10)
                    .captureAudio(false);
            CameraSession session = Camera.open(back, opts);
            if (session == null) {
                fail("Camera.open returned null session");
                return false;
            }
            try {
                PeerComponent peer = session.createView().getPreviewPeer();
                if (peer == null) {
                    fail("CameraView returned a null preview peer");
                    return false;
                }

                final AtomicInteger frameCount = new AtomicInteger();
                final int[] capturedW = new int[1];
                final int[] capturedH = new int[1];
                final int[] jpegLen = new int[1];
                session.setFrameListener(new FrameListener() {
                    @Override public void onFrame(CameraFrame frame) {
                        if (frame == null) return;
                        capturedW[0] = frame.getWidth();
                        capturedH[0] = frame.getHeight();
                        byte[] jpeg = frame.getJpegBytes();
                        if (jpeg != null) {
                            jpegLen[0] = jpeg.length;
                        }
                        frameCount.incrementAndGet();
                    }
                });

                long deadline = System.currentTimeMillis() + FRAME_TIMEOUT_MS;
                while (frameCount.get() == 0 && System.currentTimeMillis() < deadline) {
                    sleep(50);
                }
                if (frameCount.get() == 0) {
                    fail("No camera frames delivered within " + FRAME_TIMEOUT_MS + "ms");
                    return false;
                }
                if (capturedW[0] <= 0 || capturedH[0] <= 0) {
                    fail("Frame had invalid dimensions: " + capturedW[0] + "x" + capturedH[0]);
                    return false;
                }
                if (jpegLen[0] <= 0) {
                    fail("Frame JPEG bytes were empty");
                    return false;
                }
                if (!isJpeg(jpegLen[0])) {
                    // jpegLen is just the length; defensive size check.
                    fail("Frame JPEG appears too small: " + jpegLen[0] + " bytes");
                    return false;
                }

                // Trying to open a second session must fail per the
                // documented "one open session at a time" contract.
                try {
                    Camera.open(back, new CameraSessionOptions());
                    fail("Camera.open should have thrown IllegalStateException on double-open");
                    return false;
                } catch (IllegalStateException expected) {
                    // expected
                }

                // takePhoto: returns AsyncResource that must resolve within
                // the timeout. Don't block the EDT.
                AsyncResource<CapturedPhoto> photoR = session.takePhoto();
                CapturedPhoto photo = awaitPhoto(photoR, PHOTO_TIMEOUT_MS);
                if (photo == null) {
                    fail("takePhoto did not complete within " + PHOTO_TIMEOUT_MS + "ms");
                    return false;
                }
                byte[] photoBytes = photo.getJpegBytes();
                if (photoBytes == null || photoBytes.length == 0) {
                    fail("CapturedPhoto had no JPEG bytes");
                    return false;
                }
                if (photo.getFilePath() == null || photo.getFilePath().length() == 0) {
                    fail("CapturedPhoto had no file path");
                    return false;
                }
                if (photo.getWidth() <= 0 || photo.getHeight() <= 0) {
                    fail("CapturedPhoto had invalid dimensions: "
                            + photo.getWidth() + "x" + photo.getHeight());
                    return false;
                }

                session.setFrameListener(null);
            } finally {
                session.close();
            }

            // After close, opening a fresh session must succeed (close
            // released the single-active-session lock).
            CameraSession second = Camera.open(back, new CameraSessionOptions());
            if (second == null) {
                fail("Second Camera.open after close returned null");
                return false;
            }
            second.close();

            done();
            return true;
        } catch (Throwable t) {
            fail("CameraApiTest threw: " + t);
            t.printStackTrace();
            return false;
        }
    }

    private static boolean isJpeg(int byteCount) {
        // The synthetic encoder produces JPEGs typically 4-30KB at 320x240.
        // 100 bytes is the floor below which no real JPEG header + entropy
        // body can fit; serves as a sanity check, not a strict assertion.
        return byteCount >= 100;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }

    private static CapturedPhoto awaitPhoto(AsyncResource<CapturedPhoto> resource, long timeoutMs) {
        try {
            // AsyncResource.get(timeout) uses invokeAndBlock when called on the
            // EDT, so completion callbacks can still be dispatched. The old
            // hand-written wait registered ready() on the EDT and then blocked
            // that same EDT, guaranteeing a false timeout on asynchronous ports.
            return resource.get((int) timeoutMs);
        } catch (Throwable t) {
            return null;
        }
    }

}
