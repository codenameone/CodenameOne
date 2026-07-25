/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
                .backend(VisionBackends.mlKit()).minimumConfidence(.6f);
        TextRecognizer recognizer = new TextRecognizer(options);
        TextRecognitionResult result = await(recognizer.process(
                VisionImage.encoded(new byte[] {1})));
        assertEquals("hello", result.getText());
        assertEquals(VisionFeature.TEXT_RECOGNITION, backend.feature);
        assertEquals("ml-kit", backend.backend);
        assertSame(options, backend.options);
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
