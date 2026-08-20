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
package com.codename1.ai.vision;

import com.codename1.camera.CameraFacing;
import com.codename1.camera.CameraInfo;
import com.codename1.camera.CameraView;
import com.codename1.camera.ScaleType;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisionCameraViewTest extends UITestBase {
    private FakeCameraImpl camera;
    private FakeVisionImpl vision;
    private VisionCameraView<Barcode[]> view;

    @BeforeEach
    void installBackends() {
        camera = new FakeCameraImpl();
        vision = new FakeVisionImpl();
        implementation.setCameraImpl(camera);
        implementation.setVisionImpl(vision);
    }

    @AfterEach
    void releaseView() {
        if (view != null) {
            view.close();
            view = null;
        }
    }

    @Test
    void constructionRequiresAnAnalyzer() {
        assertThrows(NullPointerException.class,
                () -> new VisionCameraView<Barcode[]>(null));
    }

    @Test
    void startOpensTheCameraAndStopReleasesIt() {
        view = new VisionCameraView<Barcode[]>(new BarcodeScanner());
        assertFalse(view.isRunning());
        assertNull(view.getSession());

        view.start();
        assertTrue(view.isRunning());
        assertNotNull(view.getSession());
        assertEquals(1, camera.openCount);
        assertEquals("back", camera.openedCameraId);

        // Starting twice must not open a second session: the platform allows
        // only one, and the second open would throw.
        view.start();
        assertEquals(1, camera.openCount);

        view.stop();
        assertFalse(view.isRunning());
        assertNull(view.getSession());
    }

    @Test
    void theScannerNeverAsksForTheMicrophone() {
        // CameraSessionOptions captures audio by default, which would prompt
        // for a microphone permission a scanner has no business requesting.
        view = new VisionCameraView<Barcode[]>(new BarcodeScanner());
        view.start();
        assertFalse(camera.captureAudio);
    }

    @Test
    void facingSelectsTheCamera() {
        view = new VisionCameraView<Barcode[]>(new BarcodeScanner());
        view.setFacing(CameraFacing.FRONT);
        view.start();
        assertEquals("front", camera.openedCameraId);
        assertEquals(CameraFacing.FRONT, view.getFacing());

        view.setFacing(null);
        assertEquals(CameraFacing.BACK, view.getFacing());
    }

    @Test
    void framesReachTheAnalyzerAndResultsReachTheListener() {
        Barcode[] codes = {
            new Barcode("hello", BarcodeFormat.QR_CODE, null,
                    VisionRect.EMPTY, null)
        };
        vision.result = codes;
        final List<Barcode[]> received = new ArrayList<Barcode[]>();
        view = new VisionCameraView<Barcode[]>(new BarcodeScanner());
        view.setListener(new VisionPipelineListener<Barcode[]>() {
            public void result(Barcode[] value, VisionImage source) {
                received.add(value);
            }

            public void error(Throwable error) {
            }
        });
        view.start();

        camera.deliver(new byte[] {1, 2, 3});
        flushSerialCalls();

        assertEquals(1, vision.analyzeCount);
        assertEquals(VisionFeature.BARCODE_SCANNING, vision.lastFeature);
        assertEquals(1, received.size());
        assertSame(codes, received.get(0));
    }

    @Test
    void aCameraThatCannotOpenIsReportedToTheListenerRatherThanThrown() {
        camera.openFailure = new IOException("permission denied");
        final List<Throwable> errors = new ArrayList<Throwable>();
        view = new VisionCameraView<Barcode[]>(new BarcodeScanner());
        view.setListener(new VisionPipelineListener<Barcode[]>() {
            public void result(Barcode[] value, VisionImage source) {
            }

            public void error(Throwable error) {
                errors.add(error);
            }
        });

        view.start();
        flushSerialCalls();

        assertFalse(view.isRunning());
        assertEquals(1, errors.size());
    }

    @Test
    void leavingTheFormReleasesTheCameraButKeepsTheAnalyzerUsable() {
        CountingAnalyzer analyzer = new CountingAnalyzer();
        view = new VisionCameraView<Barcode[]>(analyzer);
        view.start();
        assertEquals(1, camera.openCount);

        // The pipeline closes the analyzer it was handed. If that were the
        // caller's analyzer, showing the view a second time would process
        // into a closed one.
        view.stop();
        assertEquals(0, analyzer.closeCount);

        view.start();
        assertEquals(2, camera.openCount);
        camera.deliver(new byte[] {1});
        flushSerialCalls();
        assertEquals(1, analyzer.processCount);

        view.close();
        assertEquals(1, analyzer.closeCount);
        view.close();
        assertEquals(1, analyzer.closeCount, "close is idempotent");
        view = null;
    }

    @Test
    void showingTheFormStartsTheCameraAndLeavingItStops() {
        view = new VisionCameraView<Barcode[]>(new BarcodeScanner());
        Form form = new Form("Scanner", new BorderLayout());
        form.add(BorderLayout.CENTER, view);
        form.show();
        flushSerialCalls();
        assertTrue(view.isRunning(), "the camera opens when the view is shown");

        new Form("Elsewhere").show();
        flushSerialCalls();
        assertFalse(view.isRunning(),
                "the camera is released when the view stops being shown");
    }

    @Test
    void unsupportedIsReportedBeforeTheScreenIsShown() {
        view = new VisionCameraView<Barcode[]>(new BarcodeScanner());
        assertTrue(view.isSupported());
        vision.supported = false;
        assertFalse(view.isSupported());
    }

    @Test
    void aBackendWithNoCamerasIsNotSupported() {
        // Camera.isSupported() is true whenever the port has a backend, which
        // a camera-less device still does. Reporting support there would have
        // start() fail on a screen this method said was fine to show.
        camera.cameras = new CameraInfo[0];
        view = new VisionCameraView<Barcode[]>(new BarcodeScanner());
        assertFalse(view.isSupported());
    }

    @Test
    void theMirrorFollowsTheCameraThatOpenedNotTheOneRequested() {
        // Camera.getDefault falls back to the first available camera when the
        // requested facing does not exist. Mirroring on the request would show
        // that rear preview reversed.
        camera.cameras = new CameraInfo[] {
            new CameraInfo("back", CameraFacing.BACK, null, null, true, true)
        };
        view = new VisionCameraView<Barcode[]>(new BarcodeScanner());
        view.setFacing(CameraFacing.FRONT);
        view.start();

        assertEquals("back", camera.openedCameraId);
        assertFalse(previewView().isMirrored());
        // Recording the flag is not enough: it has to reach the port, or the
        // preview renders unmirrored while isMirrored() claims otherwise.
        assertEquals(Boolean.FALSE, camera.previewMirrored);
    }

    @Test
    void aFrontCameraPreviewIsMirrored() {
        view = new VisionCameraView<Barcode[]>(new BarcodeScanner());
        view.setFacing(CameraFacing.FRONT);
        view.start();

        assertEquals("front", camera.openedCameraId);
        assertTrue(previewView().isMirrored());
        assertEquals(Boolean.TRUE, camera.previewMirrored);
    }

    @Test
    void theScaleTypeReachesThePort() {
        view = new VisionCameraView<Barcode[]>(new BarcodeScanner());
        view.setScaleType(ScaleType.FIT);
        view.start();
        assertEquals(ScaleType.FIT, camera.previewScaleType);

        // Changing it while running has to reach the port too.
        view.setScaleType(ScaleType.FILL);
        assertEquals(ScaleType.FILL, camera.previewScaleType);
    }

    private CameraView previewView() {
        assertEquals(1, view.getComponentCount(),
                "the preview is the view's only child while running");
        return (CameraView) view.getComponentAt(0);
    }

    /** Counts what the view does to a caller-supplied analyzer. */
    private static final class CountingAnalyzer
            implements VisionAnalyzer<Barcode[]> {
        int processCount;
        int closeCount;

        public boolean isSupported() {
            return true;
        }

        public com.codename1.util.AsyncResource<Barcode[]> process(
                VisionImage image) {
            processCount++;
            com.codename1.util.AsyncResource<Barcode[]> out =
                    new com.codename1.util.AsyncResource<Barcode[]>();
            out.complete(new Barcode[0]);
            return out;
        }

        public void close() {
            closeCount++;
        }
    }
}
