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

import com.codename1.camera.CameraInfo;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ready-made scanner screen: what an application gets from one call, and
 * what it gets when the user backs out or the camera refuses to open.
 */
class CodeScannerTest extends UITestBase {
    private FakeCameraImpl camera;
    private FakeVisionImpl vision;

    @BeforeEach
    void installBackends() {
        camera = new FakeCameraImpl();
        vision = new FakeVisionImpl();
        implementation.setCameraImpl(camera);
        implementation.setVisionImpl(vision);
    }

    @Test
    void supportNeedsBothTheCameraAndTheBarcodeBackend() {
        assertTrue(CodeScanner.isSupported());

        vision.supported = false;
        assertFalse(CodeScanner.isSupported(),
                "a target without a barcode backend cannot scan");

        vision.supported = true;
        implementation.setCameraImpl(null);
        assertFalse(CodeScanner.isSupported(),
                "a target without a camera cannot scan");

        // A backend that enumerates nothing is the camera-less device: the
        // port has an implementation, the hardware does not exist, and the
        // scan would fail immediately after this said it would not.
        implementation.setCameraImpl(camera);
        camera.cameras = new CameraInfo[0];
        assertFalse(CodeScanner.isSupported());
    }

    @Test
    void theFirstDecodedCodeEndsTheScanAndRestoresThePreviousForm() {
        Form caller = new Form("Caller");
        caller.show();
        flushSerialCalls();

        vision.result = new Barcode[] {
            new Barcode("https://codenameone.com", BarcodeFormat.QR_CODE,
                    null, VisionRect.EMPTY, null)
        };
        Recorder recorder = new Recorder(CodeScanner.scan());
        flushSerialCalls();
        assertNotSame(caller, Display.getInstance().getCurrent(),
                "the scanner shows its own form");

        camera.deliver(new byte[] {1, 2, 3});
        flushSerialCalls();

        assertEquals(1, recorder.values.size());
        assertNotNull(recorder.values.get(0));
        assertEquals("https://codenameone.com",
                recorder.values.get(0).getValue());
        assertSame(caller, Display.getInstance().getCurrent(),
                "the caller's form comes back");
        assertNull(camera.frameListener,
                "the camera is released once the scan ends");
    }

    @Test
    void aCodeInAnUnwantedFormatDoesNotEndTheScan() {
        new Form("Caller").show();
        flushSerialCalls();

        vision.result = new Barcode[] {
            new Barcode("4006381333931", BarcodeFormat.EAN_13, null,
                    VisionRect.EMPTY, null)
        };
        Recorder recorder = new Recorder(CodeScanner.scan(
                new CodeScannerOptions().formats(BarcodeFormat.QR_CODE)));
        flushSerialCalls();

        camera.deliver(new byte[] {1});
        flushSerialCalls();
        assertTrue(recorder.values.isEmpty(),
                "a stray product barcode must not end a QR scan");

        vision.result = new Barcode[] {
            new Barcode("wanted", BarcodeFormat.QR_CODE, null,
                    VisionRect.EMPTY, null)
        };
        camera.deliver(new byte[] {2});
        flushSerialCalls();
        assertEquals(1, recorder.values.size());
        assertEquals("wanted", recorder.values.get(0).getValue());
    }

    @Test
    void aFrameWithNoCodeKeepsScanning() {
        new Form("Caller").show();
        flushSerialCalls();

        vision.result = new Barcode[0];
        Recorder recorder = new Recorder(CodeScanner.scan());
        flushSerialCalls();

        camera.deliver(new byte[] {1});
        camera.deliver(new byte[] {2});
        flushSerialCalls();
        assertTrue(recorder.values.isEmpty());
        assertTrue(recorder.errors.isEmpty());
    }

    @Test
    void backingOutCompletesWithNullRatherThanFailing() {
        Form caller = new Form("Caller");
        caller.show();
        flushSerialCalls();

        vision.result = new Barcode[0];
        Recorder recorder = new Recorder(CodeScanner.scan());
        flushSerialCalls();

        Form scanner = Display.getInstance().getCurrent();
        Command back = scanner.getBackCommand();
        assertNotNull(back, "the scanner must be dismissable, including with"
                + " the Android hardware back button");
        scanner.dispatchCommand(back, new ActionEvent(back));
        flushSerialCalls();

        assertEquals(1, recorder.values.size());
        assertNull(recorder.values.get(0), "cancelling is a null result");
        assertTrue(recorder.errors.isEmpty());
        assertSame(caller, Display.getInstance().getCurrent());
    }

    @Test
    void aCameraThatCannotOpenFailsTheScanAndLeavesTheScreen() {
        Form caller = new Form("Caller");
        caller.show();
        flushSerialCalls();
        camera.openFailure = new IOException("permission denied");

        Recorder recorder = new Recorder(CodeScanner.scan());
        // Two drains: showing the form opens the camera, and the failure that
        // open reports is delivered on the next EDT cycle.
        flushSerialCalls();
        flushSerialCalls();

        assertTrue(recorder.values.isEmpty());
        assertEquals(1, recorder.errors.size());
        assertSame(caller, Display.getInstance().getCurrent(),
                "a scanner that cannot open must not strand the user on an"
                        + " empty screen");
    }

    @Test
    void optionsDefendTheirFormatArray() {
        String[] formats = {BarcodeFormat.QR_CODE};
        CodeScannerOptions options = new CodeScannerOptions().formats(formats);
        formats[0] = BarcodeFormat.EAN_13;
        assertEquals(BarcodeFormat.QR_CODE, options.getFormats()[0]);
        options.getFormats()[0] = BarcodeFormat.EAN_13;
        assertEquals(BarcodeFormat.QR_CODE, options.getFormats()[0]);
        assertEquals(0, options.formats((String[]) null).getFormats().length);
    }

    /** Collects whatever the scan resolves to. */
    private static final class Recorder {
        final List<Barcode> values = new ArrayList<Barcode>();
        final List<Throwable> errors = new ArrayList<Throwable>();

        Recorder(AsyncResource<Barcode> resource) {
            resource.ready(new SuccessCallback<Barcode>() {
                public void onSucess(Barcode value) {
                    values.add(value);
                }
            }).except(new SuccessCallback<Throwable>() {
                public void onSucess(Throwable error) {
                    errors.add(error);
                }
            });
        }
    }
}
