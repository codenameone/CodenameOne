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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ai.vision.BarcodeScanner;
import com.codename1.ai.vision.Barcode;
import com.codename1.ai.vision.DocumentScanner;
import com.codename1.ai.vision.FaceDetector;
import com.codename1.ai.vision.ImageLabeler;
import com.codename1.ai.vision.PoseDetector;
import com.codename1.ai.vision.SelfieSegmenter;
import com.codename1.ai.vision.TextRecognizer;
import com.codename1.ai.vision.VisionAnalyzer;
import com.codename1.ai.vision.VisionBackends;
import com.codename1.ai.vision.VisionImage;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.camera.CameraFrame;
import com.codename1.camera.FrameFormat;
import com.codename1.io.Log;
import com.codename1.util.AsyncResource;
import com.codename1.util.Base64;
import com.codename1.util.SuccessCallback;

/**
 * Cross-port, non-visual contract coverage for the built-in vision API.
 *
 * <p>In addition to portable ownership/lifecycle checks, supported mobile
 * ports decode a bundled deterministic QR image. That assertion crosses the
 * Java/native image boundary, native detector, JSON result mapping, format
 * normalization, and geometry mapping without camera hardware.</p>
 */
public class VisionOnDeviceApiTest extends BaseTest {
    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            checkImageOwnership();
            checkOptions();
            checkAnalyzerCapabilitiesAndLifecycle();
            return checkNativeQrDecode();
        } catch (Throwable t) {
            fail("On-device vision API test failed: " + t);
            return false;
        }
    }

    private boolean checkNativeQrDecode() throws Exception {
        final BarcodeScanner scanner = new BarcodeScanner();
        if (!scanner.isSupported()) {
            Log.p("VisionOnDeviceApiTest: native QR assertion skipped; "
                    + "barcode backend unsupported");
            scanner.close();
            done();
            return true;
        }
        String png = "iVBORw0KGgoAAAANSUhEUgAAAMAAAADAAQAAAAB6p1GqAAAA5UlEQVR4Xu2UQQ7DIAwEnVOewU8L/JRncIJ6TaqkJT17pbBSImBy2HhtpP+R/B58tMCkBSY9AyRRBX1vFauNA2R9akh72Wo4tgygqtHQRKJiCVzA7LKBjAUT6IhWT/V9bAnAGIMM9j0GrmBIcTx3/kDjbHZlmNdy/Q9H0NBkJVZEm3kAxlJkR9u1a7SuAEUEQx31CxIA4VSxWecBuCnKS9B5mQPYAGi3aQVxa0QOAH82ALGijqddX2BGm5lOdCD0PrqNCCTrNgRMArpFW5Gu3GTuA8YYZKtjul4ZruBOC0xaYNJzwRtNmtsMgNUTWwAAAABJRU5ErkJggg==";
        AsyncResource<Barcode[]> operation = scanner.process(
                VisionImage.encoded(Base64.decode(png.getBytes("UTF-8"))));
        operation.ready(new SuccessCallback<Barcode[]>() {
            public void onSucess(Barcode[] values) {
                try {
                    check(values.length > 0,
                            "native QR detector returned no result");
                    check("CN1-VISION-QR".equals(values[0].getValue()),
                            "native QR payload mismatch");
                    check("QR_CODE".equals(values[0].getFormat()),
                            "native QR format was not normalized");
                    check(values[0].getCorners().length == 4,
                            "native QR detector did not return four corners");
                    done();
                } catch (Throwable t) {
                    fail("On-device vision API test failed: " + t);
                } finally {
                    scanner.close();
                }
            }
        });
        operation.except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable error) {
                try {
                    fail("On-device vision API test failed: " + error);
                } finally {
                    scanner.close();
                }
            }
        });
        // Native completion is asynchronous. Returning lets the EDT service
        // Apple Vision/ML Kit callbacks while the suite runner polls isDone().
        return true;
    }

    private void checkImageOwnership() {
        byte[] encodedSource = new byte[] {1, 2, 3};
        VisionImage encoded = VisionImage.encoded(encodedSource);
        encodedSource[0] = 9;
        checkEqual(1, encoded.getEncodedBytes()[0],
                "VisionImage must copy encoded input");
        byte[] encodedOutput = encoded.getEncodedBytes();
        encodedOutput[1] = 9;
        checkEqual(2, encoded.getEncodedBytes()[1],
                "VisionImage must copy encoded output");

        byte[] jpeg = new byte[] {4, 5};
        byte[] pixels = new byte[] {10, 20, 30, 40};
        CameraFrame frame = new CameraFrame(jpeg, pixels, 1, 1, -90,
                123456789L, FrameFormat.RGBA8888);
        VisionImage cameraImage = VisionImage.fromCameraFrame(frame);
        jpeg[0] = 99;
        pixels[0] = 99;
        check(cameraImage.getEncodedBytes() == null,
                "raw VisionImage must not retain camera JPEG bytes");
        checkEqual(10, cameraImage.getPixels()[0],
                "VisionImage must own camera pixel bytes");
        checkEqual(1, cameraImage.getWidth(), "camera image width");
        checkEqual(1, cameraImage.getHeight(), "camera image height");
        checkEqual(270, cameraImage.getRotationDegrees(),
                "camera image rotation normalization");
        checkEqual(123456789L, cameraImage.getTimestampNanos(),
                "camera image timestamp");
        check(cameraImage.getFormat() == FrameFormat.RGBA8888,
                "camera image format");

        byte[] fallbackJpeg = new byte[] {50, 60};
        VisionImage fallbackImage = VisionImage.fromCameraFrame(
                new CameraFrame(fallbackJpeg, null, 1, 1, 0, 1L,
                        FrameFormat.NV21));
        fallbackJpeg[0] = 99;
        checkEqual(50, fallbackImage.getEncodedBytes()[0],
                "JPEG-only port fallback must be copied");
        check(fallbackImage.getPixels() == null,
                "JPEG-only port fallback must not expose raw pixels");
        check(fallbackImage.getFormat() == FrameFormat.JPEG,
                "JPEG-only port fallback must report JPEG");
    }

    private void checkOptions() {
        VisionOptions options = new VisionOptions()
                .backend(VisionBackends.appleVision())
                .minimumConfidence(2f)
                .maximumResults(-1);
        check("apple-vision".equals(options.getBackend().getId()),
                "explicit Apple Vision backend id");
        checkEqual(1f, options.getMinimumConfidence(),
                "confidence upper clamp");
        checkEqual(0, options.getMaximumResults(),
                "maximum result lower clamp");
        options.backend(null).minimumConfidence(-1f);
        check("auto".equals(options.getBackend().getId()),
                "null backend must restore auto");
        checkEqual(0f, options.getMinimumConfidence(),
                "confidence lower clamp");
    }

    private void checkAnalyzerCapabilitiesAndLifecycle() {
        VisionAnalyzer<?>[] analyzers = new VisionAnalyzer<?>[] {
                new TextRecognizer(),
                new BarcodeScanner(),
                new FaceDetector(),
                new ImageLabeler(),
                new PoseDetector(),
                new SelfieSegmenter(),
                new DocumentScanner()
        };
        String[] names = new String[] {
                "text", "barcode", "face", "label", "pose", "segmentation",
                "document"
        };
        VisionImage input = VisionImage.encoded(new byte[] {1});
        for (int i = 0; i < analyzers.length; i++) {
            VisionAnalyzer<?> analyzer = analyzers[i];
            boolean supported = analyzer.isSupported();
            Log.p("VisionOnDeviceApiTest: " + names[i]
                    + " supported=" + supported);
            analyzer.close();
            check(!analyzer.isSupported(),
                    names[i] + " analyzer must report unsupported after close");
            boolean rejected = false;
            try {
                analyzer.process(input);
            } catch (IllegalStateException expected) {
                rejected = true;
            }
            check(rejected, names[i] + " analyzer accepted input after close");
        }
    }

    private void check(boolean value, String label) {
        if (!value) {
            throw new IllegalStateException(label);
        }
    }

    private void checkEqual(int expected, int actual, String label) {
        if (expected != actual) {
            throw new IllegalStateException(label + ": expected "
                    + expected + " got " + actual);
        }
    }

    private void checkEqual(long expected, long actual, String label) {
        if (expected != actual) {
            throw new IllegalStateException(label + ": expected "
                    + expected + " got " + actual);
        }
    }

    private void checkEqual(float expected, float actual, String label) {
        if (Math.abs(expected - actual) > 0.0001f) {
            throw new IllegalStateException(label + ": expected "
                    + expected + " got " + actual);
        }
    }
}
