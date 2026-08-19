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

import com.codename1.camera.CameraFrame;
import com.codename1.camera.FrameFormat;
import com.codename1.impl.VisionImpl;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
    void encodedInputPreservesExplicitDisplayRotation() {
        VisionImage image = VisionImage.encoded(
                new byte[] {1, 2, 3}, -90);
        assertEquals(270, image.getRotationDegrees());
        assertThrows(IllegalArgumentException.class,
                () -> VisionImage.encoded(new byte[] {1}, 45));
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
    void pixelInputRejectsEncodedAndMissingFormats() {
        byte[] raw = new byte[] {1, 2, 3, 4};
        assertThrows(IllegalArgumentException.class,
                () -> VisionImage.pixels(raw, 1, 1,
                        FrameFormat.JPEG, 0));
        assertThrows(IllegalArgumentException.class,
                () -> VisionImage.pixels(raw, 1, 1, null, 0));
    }

    @Test
    void segmentationMaskRejectsOverflowingPixelCount() {
        assertThrows(IllegalArgumentException.class,
                () -> new SegmentationMask(65536, 65536, new float[0]));
    }

    @Test
    void optionsRejectNaNConfidence() {
        assertThrows(IllegalArgumentException.class,
                () -> new VisionOptions().minimumConfidence(Float.NaN));
        assertEquals(0f, new VisionOptions()
                .minimumConfidence(Float.NEGATIVE_INFINITY)
                .getMinimumConfidence());
        assertEquals(1f, new VisionOptions()
                .minimumConfidence(Float.POSITIVE_INFINITY)
                .getMinimumConfidence());
    }

    @Test
    void cameraFrameCopiesOnlyTheRequestedFormat() {
        byte[] jpeg = new byte[] {1, 2};
        byte[] pixels = new byte[] {3, 4, 5, 6};
        VisionImage raw = VisionImage.fromCameraFrame(new CameraFrame(
                jpeg, pixels, 1, 1, 0, 7L, FrameFormat.RGBA8888));
        jpeg[0] = 9;
        pixels[0] = 9;
        assertNull(raw.getEncodedBytes());
        assertArrayEquals(new byte[] {3, 4, 5, 6}, raw.getPixels());
        assertEquals(7L, raw.getTimestampNanos());

        VisionImage encoded = VisionImage.fromCameraFrame(new CameraFrame(
                jpeg, pixels, 1, 1, 0, 8L, FrameFormat.JPEG));
        assertNotNull(encoded.getEncodedBytes());
        assertNull(encoded.getPixels());

        VisionImage fallback = VisionImage.fromCameraFrame(new CameraFrame(
                jpeg, null, 1, 1, 0, 9L, FrameFormat.NV21));
        assertNotNull(fallback.getEncodedBytes());
        assertNull(fallback.getPixels());
        assertEquals(FrameFormat.JPEG, fallback.getFormat());
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
    void textScriptDefaultsToNullAndSurvivesTheOptionSnapshot() {
        RecordingVisionImpl backend = new RecordingVisionImpl();
        implementation.setVisionImpl(backend);
        assertNull(new VisionOptions().getTextScript(),
                "No script means the platform default, not an assumed Latin");

        VisionOptions options = new VisionOptions()
                .textScript(TextScript.japanese());
        TextRecognizer recognizer = new TextRecognizer(options);
        // Mutating the caller's options after construction must not reach the
        // port: the analyzer holds its own snapshot.
        options.textScript(TextScript.korean());
        await(recognizer.process(VisionImage.encoded(new byte[] {1})));

        assertSame(TextScript.japanese(), backend.options.getTextScript());
        assertEquals("japanese", backend.options.getTextScript().getId());
        recognizer.close();
    }

    @Test
    void everyTextScriptHasItsOwnStableIdentifier() {
        // The ids are a port-boundary contract: the Android adapter class and
        // the Apple recognition languages are both selected from them.
        assertEquals("latin", TextScript.latin().getId());
        assertEquals("chinese", TextScript.chinese().getId());
        assertEquals("devanagari", TextScript.devanagari().getId());
        assertEquals("japanese", TextScript.japanese().getId());
        assertEquals("korean", TextScript.korean().getId());
        assertEquals("korean", TextScript.korean().toString());

        Set<String> ids = new HashSet<String>();
        TextScript[] all = {TextScript.latin(), TextScript.chinese(),
                TextScript.devanagari(), TextScript.japanese(),
                TextScript.korean()};
        for (TextScript script : all) {
            assertTrue(ids.add(script.getId()), script.getId());
        }
    }

    @Test
    void nullTextScriptRestoresThePlatformDefault() {
        RecordingVisionImpl backend = new RecordingVisionImpl();
        implementation.setVisionImpl(backend);
        TextRecognizer recognizer = new TextRecognizer(new VisionOptions()
                .textScript(TextScript.chinese())
                .textScript(null));
        await(recognizer.process(VisionImage.encoded(new byte[] {1})));
        assertNull(backend.options.getTextScript());
        recognizer.close();
    }

    @Test
    void unsupportedAnalyzerReturnsFailedResource() {
        implementation.setVisionImpl(null);
        AsyncResource<TextRecognitionResult> result =
                new TextRecognizer().process(VisionImage.encoded(new byte[] {1}));
        assertTrue(result.isDone());
        assertThrows(AsyncResource.AsyncExecutionException.class, result::get);
    }

    @Test
    void cancelledAnalysisSuppressesLateBackendCallbacks() {
        DeferredVisionImpl backend = new DeferredVisionImpl();
        implementation.setVisionImpl(backend);
        TextRecognizer recognizer = new TextRecognizer();
        AsyncResource<TextRecognitionResult> result = recognizer.process(
                VisionImage.encoded(new byte[] {1}));
        final int[] callbackCount = new int[1];
        result.ready(new SuccessCallback<TextRecognitionResult>() {
            public void onSucess(TextRecognitionResult value) {
                callbackCount[0]++;
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable error) {
                callbackCount[0]++;
            }
        });

        assertTrue(result.cancel(false));
        backend.pending.complete(new TextRecognitionResult("late", null));
        backend.pending.error(new RuntimeException("later error"));
        flushSerialCalls();

        assertTrue(result.isCancelled());
        assertEquals(0, callbackCount[0]);
        recognizer.close();
    }

    @Test
    void analyzerCreationAndCloseAreSerialized() throws Exception {
        final RecordingVisionImpl backend = new RecordingVisionImpl();
        implementation.setVisionImpl(backend);
        final CountDownLatch creationEntered = new CountDownLatch(1);
        final CountDownLatch allowCreation = new CountDownLatch(1);
        implementation.setVisionImplCreationHook(new Runnable() {
            public void run() {
                creationEntered.countDown();
                try {
                    allowCreation.await();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(error);
                }
            }
        });
        final TextRecognizer recognizer = new TextRecognizer();
        final AtomicReference<Throwable> failure =
                new AtomicReference<Throwable>();
        Thread processor = new Thread(new Runnable() {
            public void run() {
                try {
                    recognizer.process(
                            VisionImage.encoded(new byte[] {1}));
                } catch (Throwable error) {
                    failure.set(error);
                }
            }
        }, "vision-process");
        Thread closer = new Thread(new Runnable() {
            public void run() {
                recognizer.close();
            }
        }, "vision-close");

        processor.start();
        assertTrue(creationEntered.await(2, TimeUnit.SECONDS));
        closer.start();
        try {
            long deadline = System.currentTimeMillis() + 2000;
            while (closer.getState() != Thread.State.BLOCKED
                    && closer.isAlive()
                    && System.currentTimeMillis() < deadline) {
                Thread.yield();
            }
            assertEquals(Thread.State.BLOCKED, closer.getState());
        } finally {
            allowCreation.countDown();
        }
        processor.join(2000);
        closer.join(2000);

        assertFalse(processor.isAlive());
        assertFalse(closer.isAlive());
        assertNull(failure.get());
        assertEquals(1, backend.closeCount);
        assertThrows(IllegalStateException.class,
                () -> recognizer.process(
                        VisionImage.encoded(new byte[] {1})));
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

    private static final class DeferredVisionImpl extends VisionImpl {
        final AsyncResource<TextRecognitionResult> pending =
                new AsyncResource<TextRecognitionResult>();

        public boolean isSupported(VisionFeature feature, String backendId) {
            return true;
        }

        @SuppressWarnings("unchecked")
        public <T> AsyncResource<T> analyze(VisionFeature feature,
                                            String backendId,
                                            VisionImage image,
                                            VisionOptions options) {
            return (AsyncResource<T>) pending;
        }

        public void close() {
        }
    }
}
