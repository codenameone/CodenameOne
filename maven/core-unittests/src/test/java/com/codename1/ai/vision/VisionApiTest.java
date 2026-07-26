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

import com.codename1.camera.FrameFormat;
import com.codename1.impl.VisionImpl;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VisionApiTest extends UITestBase {
    @Test
    void visionImageDefensivelyCopiesInputAndOutput() {
        byte[] source = new byte[] {1, 2, 3};
        VisionImage image = VisionImage.encoded(source);
        source[0] = 9;
        assertEquals(1, image.getEncodedBytes()[0]);
        byte[] returned = image.getEncodedBytes();
        returned[1] = 9;
        assertEquals(2, image.getEncodedBytes()[1]);
    }

    @Test
    void pixelInputNormalizesRotation() {
        VisionImage image = VisionImage.pixels(new byte[] {1, 2, 3, 4},
                1, 1, FrameFormat.RGBA8888, -90);
        assertEquals(270, image.getRotationDegrees());
        assertThrows(IllegalArgumentException.class,
                () -> VisionImage.pixels(new byte[] {1, 2, 3, 4},
                        1, 1, FrameFormat.RGBA8888, 45));
    }

    @Test
    void backendMetadataIsOptionalAndImmutable() {
        Map<String, String> values = new HashMap<String, String>();
        values.put("nativeType", "qr");
        VisionMetadata metadata = new VisionMetadata("ml-kit", values);
        values.put("nativeType", "changed");
        assertEquals("ml-kit", metadata.getBackendId());
        assertEquals("qr", metadata.get("nativeType"));
        assertThrows(UnsupportedOperationException.class,
                () -> metadata.getValues().put("x", "y"));
        Barcode barcode = new Barcode("value", "QR_CODE", null,
                VisionRect.EMPTY, null, metadata);
        assertSame(metadata, barcode.getMetadata());
    }

    @Test
    void analyzerForwardsFeatureBackendAndOptions() {
        RecordingVisionImpl backend = new RecordingVisionImpl();
        implementation.setVisionImpl(backend);
        VisionOptions options = new VisionOptions()
                .backend(VisionBackends.mlKitBarcodeScanning())
                .minimumConfidence(.6f)
                .maximumResults(3);
        TextRecognizer recognizer = new TextRecognizer(options);
        options.backend(VisionBackends.auto())
                .minimumConfidence(.1f)
                .maximumResults(1);
        TextRecognitionResult result = await(recognizer.process(
                VisionImage.encoded(new byte[] {1})));
        assertEquals("hello", result.getText());
        assertEquals(VisionFeature.TEXT_RECOGNITION, backend.feature);
        assertEquals("ml-kit", backend.backend);
        assertNotSame(options, backend.options);
        assertEquals(.6f, backend.options.getMinimumConfidence());
        assertEquals(3, backend.options.getMaximumResults());
        recognizer.close();
        assertEquals(1, backend.closeCount);
    }

    @Test
    void unsupportedAnalyzerReturnsFailedResource() {
        implementation.setVisionImpl(null);
        AsyncResource<TextRecognitionResult> result =
                new TextRecognizer().process(VisionImage.encoded(new byte[] {1}));
        assertTrue(result.isDone());
        assertThrows(AsyncResource.AsyncExecutionException.class, result::get);
    }

    private <T> T await(AsyncResource<T> resource) {
        final AtomicReference<T> value = new AtomicReference<T>();
        resource.ready(new SuccessCallback<T>() {
            public void onSucess(T result) {
                value.set(result);
            }
        });
        flushSerialCalls();
        assertTrue(resource.isDone());
        assertNotNull(value.get());
        return resource.get();
    }

    private static final class RecordingVisionImpl extends VisionImpl {
        VisionFeature feature;
        String backend;
        VisionOptions options;
        int closeCount;

        public boolean isSupported(VisionFeature feature, String backendId) {
            return true;
        }

        @SuppressWarnings("unchecked")
        public <T> AsyncResource<T> analyze(VisionFeature value, String backendId,
                                             VisionImage image, VisionOptions opts) {
            feature = value;
            backend = backendId;
            options = opts;
            AsyncResource<T> result = new AsyncResource<T>();
            result.complete((T) new TextRecognitionResult("hello", null));
            return result;
        }

        public void close() {
            closeCount++;
        }
    }
}
