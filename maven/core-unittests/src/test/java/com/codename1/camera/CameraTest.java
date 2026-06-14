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
