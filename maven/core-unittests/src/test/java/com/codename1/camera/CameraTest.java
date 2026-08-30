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
package com.codename1.camera;

import com.codename1.junit.UITestBase;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the static {@link Camera} entry point: support probing, camera
 * enumeration, default selection, the single-open-session contract, and the
 * permission-request shortcut. Drives a hand-written {@link RecordingCameraImpl}
 * installed through the test implementation.
 */
class CameraTest extends UITestBase {

    private CameraSession opened;

    @AfterEach
    void closeAnyOpenSession() {
        if (opened != null) {
            opened.close();
            opened = null;
        }
    }

    private RecordingCameraImpl install() {
        RecordingCameraImpl impl = new RecordingCameraImpl();
        implementation.setCameraImpl(impl);
        return impl;
    }

    private static CameraInfo camera(String id, CameraFacing facing) {
        return new CameraInfo(id, facing, null, null, false, false);
    }

    @Test
    void notSupportedWithoutBackend() {
        implementation.setCameraImpl(null);
        assertFalse(Camera.isSupported());
    }

    @Test
    void supportedWhenBackendPresent() {
        install();
        assertTrue(Camera.isSupported());
    }

    @Test
    void getCamerasEmptyWithoutBackend() {
        implementation.setCameraImpl(null);
        assertEquals(0, Camera.getCameras().length);
    }

    @Test
    void getCamerasReturnsEnumeratedList() {
        RecordingCameraImpl impl = install();
        impl.cameras = new CameraInfo[]{camera("back", CameraFacing.BACK), camera("front", CameraFacing.FRONT)};
        CameraInfo[] all = Camera.getCameras();
        assertEquals(2, all.length);
        assertEquals("back", all[0].getId());
    }

    @Test
    void getCamerasMapsNullEnumerationToEmptyArray() {
        RecordingCameraImpl impl = install();
        impl.enumerateReturnsNull = true;
        assertEquals(0, Camera.getCameras().length);
    }

    @Test
    void getDefaultMatchesRequestedFacing() {
        RecordingCameraImpl impl = install();
        impl.cameras = new CameraInfo[]{camera("back", CameraFacing.BACK), camera("front", CameraFacing.FRONT)};
        CameraInfo front = Camera.getDefault(CameraFacing.FRONT);
        assertNotNull(front);
        assertEquals("front", front.getId());
    }

    @Test
    void getDefaultFallsBackToFirstWhenFacingAbsent() {
        RecordingCameraImpl impl = install();
        impl.cameras = new CameraInfo[]{camera("back", CameraFacing.BACK)};
        CameraInfo any = Camera.getDefault(CameraFacing.EXTERNAL);
        assertNotNull(any);
        assertEquals("back", any.getId());
    }

    @Test
    void getDefaultNullWhenNoCameras() {
        install();
        assertNull(Camera.getDefault(CameraFacing.BACK));
    }

    @Test
    void openRejectsNullCameraInfo() {
        install();
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                Camera.open(null, new CameraSessionOptions());
            }
        });
    }

    @Test
    void openThrowsWhenUnsupported() {
        implementation.setCameraImpl(null);
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                Camera.open(camera("back", CameraFacing.BACK), new CameraSessionOptions());
            }
        });
    }

    @Test
    void openSucceedsAndPassesCameraIdToBackend() {
        RecordingCameraImpl impl = install();
        opened = Camera.open(camera("back", CameraFacing.BACK), new CameraSessionOptions());
        assertNotNull(opened);
        assertFalse(opened.isClosed());
        assertEquals("back", impl.openedCameraId);
        assertEquals(1, impl.openCount);
    }

    @Test
    void openWithNullOptionsUsesDefaults() {
        install();
        opened = Camera.open(camera("back", CameraFacing.BACK), null);
        assertNotNull(opened.getOptions());
    }

    @Test
    void secondOpenWhileActiveThrows() {
        install();
        opened = Camera.open(camera("back", CameraFacing.BACK), new CameraSessionOptions());
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                Camera.open(camera("front", CameraFacing.FRONT), new CameraSessionOptions());
            }
        });
    }

    /// The coexistence flow Camera's own documentation promises: pause the
    /// session, run a modal Capture, resume.
    ///
    /// The gate used to refuse this, because a paused session is not a closed
    /// one -- so Capture.capturePhoto() on the macOS port answered its listener
    /// with null having shown no capture UI at all. pause() is documented as
    /// releasing the hardware, and the gate exists to stop two consumers
    /// HOLDING the device, so a paused session must not block it.
    @Test
    void aPausedSessionLetsAModalCaptureOpen() {
        install();
        CameraSession background = Camera.open(camera("back", CameraFacing.BACK),
                new CameraSessionOptions());
        background.pause();
        assertTrue(background.isPaused());
        opened = Camera.open(camera("front", CameraFacing.FRONT),
                new CameraSessionOptions());
        assertFalse(opened.isClosed());
        background.close();
    }

    /// And exclusivity survives that handoff.
    ///
    /// When the modal capture closes, the paused session becomes the active one
    /// again -- the application is about to resume it -- so a third open() is
    /// still refused. Without the handback the capture's close() would leave no
    /// active session and the next open() would run alongside the resumed one.
    @Test
    void theHandoffGivesTheSessionBackWhenTheCaptureCloses() {
        install();
        CameraSession background = Camera.open(camera("back", CameraFacing.BACK),
                new CameraSessionOptions());
        background.pause();
        CameraSession modal = Camera.open(camera("front", CameraFacing.FRONT),
                new CameraSessionOptions());
        modal.close();
        background.resume();
        assertFalse(background.isPaused());
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                Camera.open(camera("front", CameraFacing.FRONT), new CameraSessionOptions());
            }
        });
        background.close();
    }

    /// Nested handoffs give every session back, in order.
    ///
    /// pause A, open and pause B, open C. A single preempted slot held only the
    /// most recent, so closing C restored B and closing B then left NO active
    /// session -- A was forgotten while still holding the hardware it was about
    /// to resume, and the next open() was accepted alongside it.
    ///
    /// Each level is checked by RESUMING the restored session and watching the
    /// next open() be refused: a restored session that is still paused holds
    /// nothing, so it correctly does not block one.
    @Test
    void nestedHandoffsUnwindToTheOutermostSession() {
        install();
        CameraSession a = Camera.open(camera("back", CameraFacing.BACK),
                new CameraSessionOptions());
        a.pause();
        CameraSession b = Camera.open(camera("front", CameraFacing.FRONT),
                new CameraSessionOptions());
        b.pause();
        CameraSession c = Camera.open(camera("back", CameraFacing.BACK),
                new CameraSessionOptions());
        c.close();
        // B is the active session again; resumed, it holds the device.
        b.resume();
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                Camera.open(camera("back", CameraFacing.BACK), new CameraSessionOptions());
            }
        });
        b.close();
        // ...and now A is, which is the one the single slot lost outright.
        a.resume();
        assertFalse(a.isPaused());
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                Camera.open(camera("front", CameraFacing.FRONT), new CameraSessionOptions());
            }
        });
        a.close();
        opened = Camera.open(camera("back", CameraFacing.BACK),
                new CameraSessionOptions());
        assertFalse(opened.isClosed());
    }

    /// A session closed while it waits in the chain is skipped, not handed back.
    @Test
    void aPreemptedSessionClosedWhileWaitingIsSkipped() {
        install();
        CameraSession a = Camera.open(camera("back", CameraFacing.BACK),
                new CameraSessionOptions());
        a.pause();
        CameraSession b = Camera.open(camera("front", CameraFacing.FRONT),
                new CameraSessionOptions());
        b.pause();
        CameraSession c = Camera.open(camera("back", CameraFacing.BACK),
                new CameraSessionOptions());
        b.close();
        c.close();
        // B was closed while waiting, so the handback skipped it and reached A.
        a.resume();
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                Camera.open(camera("back", CameraFacing.BACK), new CameraSessionOptions());
            }
        });
        a.close();
        opened = Camera.open(camera("back", CameraFacing.BACK),
                new CameraSessionOptions());
        assertFalse(opened.isClosed());
    }

    /// Closing the paused session instead of resuming it leaves nothing to hand
    /// back to, and the next open() succeeds.
    @Test
    void closingThePausedSessionDuringTheCaptureLeavesNoHandback() {
        install();
        CameraSession background = Camera.open(camera("back", CameraFacing.BACK),
                new CameraSessionOptions());
        background.pause();
        CameraSession modal = Camera.open(camera("front", CameraFacing.FRONT),
                new CameraSessionOptions());
        background.close();
        modal.close();
        opened = Camera.open(camera("back", CameraFacing.BACK),
                new CameraSessionOptions());
        assertFalse(opened.isClosed());
    }

    @Test
    void openAfterClosingPreviousSucceeds() {
        install();
        CameraSession first = Camera.open(camera("back", CameraFacing.BACK), new CameraSessionOptions());
        first.close();
        opened = Camera.open(camera("front", CameraFacing.FRONT), new CameraSessionOptions());
        assertFalse(opened.isClosed());
    }

    @Test
    void openWrapsBackendIOExceptionAndClosesProbe() {
        RecordingCameraImpl impl = install();
        impl.openFailure = new IOException("device busy");
        try {
            Camera.open(camera("back", CameraFacing.BACK), new CameraSessionOptions());
            fail("expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("back"));
        }
        // open() failed, so the impl was closed and no session is active.
        assertEquals(1, impl.closeCount);
    }

    @Test
    void requestPermissionsDeliversFalseWithoutBackend() {
        implementation.setCameraImpl(null);
        assertEquals(Boolean.FALSE, awaitPermission(false));
    }

    @Test
    void requestPermissionsDeliversTrueWhenOpenSucceeds() {
        install();
        assertEquals(Boolean.TRUE, awaitPermission(false));
    }

    @Test
    void requestPermissionsDeliversFalseWhenOpenThrows() {
        RecordingCameraImpl impl = install();
        impl.openFailure = new IOException("denied");
        assertEquals(Boolean.FALSE, awaitPermission(true));
    }

    @Test
    void requestPermissionsToleratesNullCallback() {
        install();
        // Must not throw even though there is no callback to deliver to.
        Camera.requestPermissions(false, null);
        flushSerialCalls();
    }

    private Boolean awaitPermission(boolean audio) {
        final AtomicReference<Boolean> result = new AtomicReference<Boolean>();
        Camera.requestPermissions(audio, new SuccessCallback<Boolean>() {
            public void onSucess(Boolean value) {
                result.set(value);
            }
        });
        int budget = 4000;
        while (result.get() == null && budget > 0) {
            flushSerialCalls();
            budget -= 5;
        }
        return result.get();
    }
}
