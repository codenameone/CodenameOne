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
package com.codename1.ar;

import com.codename1.junit.UITestBase;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the static {@link AR} entry point: support probing,
 * capabilities, the single-open-session contract and the permission request.
 * Drives a hand-written {@link RecordingARImpl} installed through the test
 * implementation.
 */
class ARTest extends UITestBase {

    private ARSession opened;

    @AfterEach
    void closeAnyOpenSession() {
        if (opened != null) {
            opened.close();
            opened = null;
        }
    }

    private RecordingARImpl install() {
        RecordingARImpl impl = new RecordingARImpl();
        implementation.setARImpl(impl);
        return impl;
    }

    @Test
    void notSupportedWithoutBackend() {
        implementation.setARImpl(null);
        assertFalse(AR.isSupported());
    }

    @Test
    void supportedWhenBackendPresent() {
        install();
        assertTrue(AR.isSupported());
    }

    @Test
    void capabilitiesAllFalseWithoutBackend() {
        implementation.setARImpl(null);
        ARCapabilities caps = AR.getCapabilities();
        assertFalse(caps.isWorldTrackingSupported());
        assertFalse(caps.isPlaneDetectionSupported());
        assertFalse(caps.isImageTrackingSupported());
        assertFalse(caps.isFaceTrackingSupported());
        assertFalse(caps.isLightEstimationSupported());
    }

    @Test
    void capabilitiesComeFromBackendAndProbeIsClosed() {
        RecordingARImpl impl = install();
        impl.capabilities = new ARCapabilities(true, true, false, false, true);
        ARCapabilities caps = AR.getCapabilities();
        assertTrue(caps.isWorldTrackingSupported());
        assertFalse(caps.isImageTrackingSupported());
        assertEquals(1, impl.closeCount);
    }

    @Test
    void nullBackendCapabilitiesMapToUnsupported() {
        RecordingARImpl impl = install();
        impl.capabilitiesReturnsNull = true;
        assertFalse(AR.getCapabilities().isWorldTrackingSupported());
    }

    @Test
    void openThrowsWhenUnsupported() {
        implementation.setARImpl(null);
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                AR.open(new ARSessionOptions());
            }
        });
    }

    @Test
    void openSucceedsAndPassesOptionsToBackend() {
        RecordingARImpl impl = install();
        ARSessionOptions opts = new ARSessionOptions()
                .planeDetection(ARPlaneDetection.HORIZONTAL_AND_VERTICAL);
        opened = AR.open(opts);
        assertNotNull(opened);
        assertFalse(opened.isClosed());
        assertSame(opts, impl.openedOptions);
        assertEquals(1, impl.openCount);
        assertNotNull(impl.sink);
    }

    @Test
    void openWithNullOptionsUsesDefaults() {
        install();
        opened = AR.open(null);
        assertNotNull(opened.getOptions());
        assertEquals(ARTrackingMode.WORLD, opened.getOptions().getTrackingMode());
        assertEquals(ARPlaneDetection.HORIZONTAL, opened.getOptions().getPlaneDetection());
        assertTrue(opened.getOptions().isLightEstimation());
    }

    @Test
    void secondOpenWhileActiveThrows() {
        install();
        opened = AR.open(null);
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                AR.open(null);
            }
        });
    }

    @Test
    void openAfterClosingPreviousSucceeds() {
        install();
        ARSession first = AR.open(null);
        first.close();
        opened = AR.open(null);
        assertFalse(opened.isClosed());
    }

    @Test
    void openWrapsBackendIOExceptionAndClosesImpl() {
        RecordingARImpl impl = install();
        impl.openFailure = new IOException("no camera");
        try {
            AR.open(null);
            fail("expected RuntimeException");
        } catch (RuntimeException expected) {
            assertNotNull(expected.getMessage());
        }
        assertEquals(1, impl.closeCount);
        // The failed session did not occupy the active slot.
        impl.openFailure = null;
        opened = AR.open(null);
        assertNotNull(opened);
    }

    @Test
    void requestPermissionsDeliversFalseWithoutBackend() {
        implementation.setARImpl(null);
        assertEquals(Boolean.FALSE, awaitPermission());
    }

    @Test
    void requestPermissionsDeliversBackendResultAndClosesProbe() {
        RecordingARImpl impl = install();
        assertEquals(Boolean.TRUE, awaitPermission());
        assertEquals(1, impl.closeCount);
    }

    @Test
    void requestPermissionsDeliversFalseOnBackendError() {
        RecordingARImpl impl = install();
        impl.permissionFailure = new RuntimeException("denied");
        assertEquals(Boolean.FALSE, awaitPermission());
        assertEquals(1, impl.closeCount);
    }

    @Test
    void requestPermissionsToleratesNullCallback() {
        install();
        AR.requestPermissions(null);
        flushSerialCalls();
    }

    private Boolean awaitPermission() {
        final AtomicReference<Boolean> result = new AtomicReference<Boolean>();
        AR.requestPermissions(new SuccessCallback<Boolean>() {
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
