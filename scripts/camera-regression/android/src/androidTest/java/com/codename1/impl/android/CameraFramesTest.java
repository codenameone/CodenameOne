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
package com.codename1.impl.android;

import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.media.Image;
import junit.framework.TestCase;
import android.media.ImageReader;
import android.media.ImageWriter;
import com.codename1.camera.CameraFrame;
import com.codename1.camera.FrameFormat;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Local device tests of production frame delivery, Android JPEG and ML Kit. */
public class CameraFramesTest extends TestCase {
    public void testQrAndPdf417AcrossPlaneLayouts() throws Exception {
        for (BarcodeFormat format : new BarcodeFormat[] {BarcodeFormat.QR_CODE, BarcodeFormat.PDF_417}) {
            for (int pixelStride : new int[] {1, 2}) {
                for (int padding : new int[] {0, 32}) {
                    verifyBarcode(format, pixelStride, padding);
                }
            }
        }
    }

    private void verifyBarcode(BarcodeFormat format, int pixelStride, int padding) throws Exception {
        String value = "CN1 camera regression 0123456789";
        BitMatrix code = new MultiFormatWriter().encode(value, format, 640, 480);
        TestImage image = new TestImage(code, pixelStride, padding);
        TestPlane[] p = image.planes;
        byte[] raw = AndroidCameraFrameConverter.toNV21(image.width, image.height,
                p[0].buffer, p[0].rowStride, p[0].pixelStride,
                p[1].buffer, p[1].rowStride, p[1].pixelStride,
                p[2].buffer, p[2].rowStride, p[2].pixelStride);
        CameraFrame frame = new CameraFrame(null, raw, image.width, image.height,
                0, 0, FrameFormat.NV21);
        image.close();
        BarcodeScanner scanner = BarcodeScanning.getClient();
        try {
            List<Barcode> codes = Tasks.await(scanner.process(InputImage.fromByteArray(
                    frame.getRawBytes(), frame.getWidth(), frame.getHeight(), 0,
                    InputImage.IMAGE_FORMAT_NV21)), 30, TimeUnit.SECONDS);
            boolean found = false;
            for (Barcode barcode : codes) if (value.equals(barcode.getRawValue())) found = true;
            assertTrue(format + " stride=" + pixelStride + " padding=" + padding, found);

        } finally {
            scanner.close();
        }
    }

    public void testNativeImageDeliveryAndJpegFallback() throws Exception {
        for (FrameFormat requested : new FrameFormat[] {FrameFormat.NV21, FrameFormat.JPEG}) {
            try (NativeInput input = new NativeInput()) {
                Proxy proxy = new Proxy(input.image);
                proxy.info.rotation = 90;
                final CameraFrame[] result = new CameraFrame[1];
                CameraFrameHarness harness = new CameraFrameHarness();
                harness.setFrameListener(frame -> result[0] = frame, requested, 0);
                harness.deliver(proxy);
                assertTrue(proxy.closed);
                assertNotNull(result[0]);
                assertEquals(90, result[0].getRotationDegrees());
                if (requested == FrameFormat.JPEG) assertNull(result[0].getRawBytes());
                else assertNotNull(result[0].getRawBytes());
                byte[] jpeg = result[0].getJpegBytes();
                assertNotNull(BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length));
                assertSame(jpeg, result[0].getJpegBytes());
                BarcodeScanner scanner = BarcodeScanning.getClient();
                try {
                    InputImage image = requested == FrameFormat.NV21
                            ? InputImage.fromByteArray(result[0].getRawBytes(), 320, 240, 90, InputImage.IMAGE_FORMAT_NV21)
                            : InputImage.fromBitmap(BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length), 90);
                    List<Barcode> codes = Tasks.await(scanner.process(image), 30, TimeUnit.SECONDS);
                    assertEquals(1, codes.size());
                    assertEquals("rotation", codes.get(0).getRawValue());
                } finally { scanner.close(); }
            }
        }
    }

    public void testThrottledAndFailedFramesStillClose() throws Exception {
        CameraFrameHarness harness = new CameraFrameHarness();
        final int[] count = {0};
        harness.setFrameListener(frame -> count[0]++, FrameFormat.NV21, 1);
        try (NativeInput first = new NativeInput(); NativeInput second = new NativeInput()) {
            Proxy p1 = new Proxy(first.image), p2 = new Proxy(second.image);
            harness.deliver(p1);
            harness.deliver(p2);
            assertEquals(1, count[0]);
            assertTrue(p1.closed && p2.closed);
        }
        harness.setFrameListener(frame -> count[0]++, FrameFormat.NV21, 0);
        try (NativeInput invalid = new NativeInput()) {
            Proxy bad = new Proxy(invalid.image);
            bad.failRead = true;
            harness.deliver(bad);
            assertTrue(bad.closed);
            assertEquals(1, count[0]);
        }
        try (NativeInput recovery = new NativeInput()) {
            Proxy good = new Proxy(recovery.image);
            harness.deliver(good);
            assertTrue(good.closed);
            assertEquals(2, count[0]);
        }
    }

    public void testEmulatorCameraStreamReachesFrameListener() throws Exception {
        final android.app.Instrumentation instrumentation =
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
        android.content.Context context = instrumentation.getTargetContext();
        instrumentation.getUiAutomation().grantRuntimePermission(context.getPackageName(),
                android.Manifest.permission.CAMERA);
        android.content.Intent intent = new android.content.Intent(context,
                com.codename1.camera.regression.CameraTestActivity.class);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        android.app.Activity activity = instrumentation.startActivitySync(intent);
        android.os.HandlerThread thread = new android.os.HandlerThread("CameraRegression");
        thread.start();
        android.os.Handler handler = new android.os.Handler(thread.getLooper());
        android.hardware.camera2.CameraManager manager = (android.hardware.camera2.CameraManager)
                context.getSystemService(android.content.Context.CAMERA_SERVICE);
        final java.util.concurrent.CountDownLatch delivered = new java.util.concurrent.CountDownLatch(3);
        final java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        final android.hardware.camera2.CameraDevice[] device = new android.hardware.camera2.CameraDevice[1];
        final android.hardware.camera2.CameraCaptureSession[] session = new android.hardware.camera2.CameraCaptureSession[1];
        ImageReader reader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);
        CameraFrameHarness harness = new CameraFrameHarness();
        harness.setFrameListener(frame -> {
            assertEquals(640 * 480 * 3 / 2, frame.getRawBytes().length);
            delivered.countDown();
        }, FrameFormat.NV21, 15);
        reader.setOnImageAvailableListener(source -> {
            Image image = source.acquireLatestImage();
            if (image == null) return;
            Proxy proxy = new Proxy(image);
            try {
                harness.deliver(proxy);
                assertTrue(proxy.closed);
            } catch (Throwable failure) { error.set(failure); }
        }, handler);
        try {
            assertTrue("Emulator needs an enabled camera", manager.getCameraIdList().length > 0);
            manager.openCamera(manager.getCameraIdList()[0], new android.hardware.camera2.CameraDevice.StateCallback() {
                public void onOpened(android.hardware.camera2.CameraDevice camera) {
                    device[0] = camera;
                    try {
                        camera.createCaptureSession(java.util.Collections.singletonList(reader.getSurface()),
                                new android.hardware.camera2.CameraCaptureSession.StateCallback() {
                            public void onConfigured(android.hardware.camera2.CameraCaptureSession capture) {
                                session[0] = capture;
                                try {
                                    android.hardware.camera2.CaptureRequest.Builder request = camera.createCaptureRequest(
                                            android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW);
                                    request.addTarget(reader.getSurface());
                                    capture.setRepeatingRequest(request.build(), null, handler);
                                } catch (Throwable failure) { error.set(failure); }
                            }
                            public void onConfigureFailed(android.hardware.camera2.CameraCaptureSession capture) {
                                error.set(new AssertionError("Camera configuration failed"));
                            }
                        }, handler);
                    } catch (Throwable failure) { error.set(failure); }
                }
                public void onDisconnected(android.hardware.camera2.CameraDevice camera) { camera.close(); }
                public void onError(android.hardware.camera2.CameraDevice camera, int code) {
                    error.set(new AssertionError("Camera error " + code)); camera.close();
                }
            }, handler);
            assertTrue("No camera frames; error=" + error.get(), delivered.await(20, TimeUnit.SECONDS));
            assertNull(error.get());
        } finally {
            java.util.concurrent.CountDownLatch stopped = new java.util.concurrent.CountDownLatch(1);
            handler.post(() -> {
                if (session[0] != null) session[0].close();
                if (device[0] != null) device[0].close();
                reader.close();
                stopped.countDown();
            });
            stopped.await(5, TimeUnit.SECONDS);
            thread.quitSafely();
            thread.join(5000);
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    static class NativeInput implements AutoCloseable {
        final ImageReader reader;
        final ImageWriter writer;
        final Image image;
        NativeInput() throws Exception {
            reader = ImageReader.newInstance(320, 240, ImageFormat.YUV_420_888, 2);
            writer = ImageWriter.newInstance(reader.getSurface(), 2);
            image = writer.dequeueInputImage();
            BitMatrix code = new MultiFormatWriter().encode("rotation", BarcodeFormat.QR_CODE, 320, 240);
            Image.Plane[] planes = image.getPlanes();
            for (int i = 0; i < 3; i++) {
                int w = i == 0 ? 320 : 160, h = i == 0 ? 240 : 120;
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                    int value = i == 0 ? (code.get(x, y) ? 16 : 235) : 128;
                    planes[i].getBuffer().put(y * planes[i].getRowStride()
                            + x * planes[i].getPixelStride(), (byte) value);
                }
            }
        }
        public void close() { image.close(); writer.close(); reader.close(); }
    }

    public static class Info {
        int rotation;
        public int getRotationDegrees() { return rotation; }
    }
    public static class Proxy {
        final Image image;
        final Info info = new Info();
        boolean closed;
        Proxy(Image image) { this.image = image; }
        boolean failRead;
        public Image getImage() { if (failRead) throw new IllegalStateException("test"); return image; }
        public Info getImageInfo() { return info; }
        public void close() { closed = true; image.close(); }
    }
    static class TestImage {
        final int width, height;
        final TestPlane[] planes;
        TestImage(BitMatrix code, int pixelStride, int padding) {
            width = code.getWidth(); height = code.getHeight();
            planes = new TestPlane[] {new TestPlane(width, height, 1, padding),
                    new TestPlane(width / 2, height / 2, pixelStride, padding),
                    new TestPlane(width / 2, height / 2, pixelStride, padding)};
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
                planes[0].buffer.put(y * planes[0].rowStride + x, (byte) (code.get(x, y) ? 16 : 235));
            for (int i = 1; i < 3; i++) for (int y = 0; y < height / 2; y++)
                for (int x = 0; x < width / 2; x++) planes[i].buffer.put(
                        y * planes[i].rowStride + x * pixelStride, (byte) 128);
        }
        public int getFormat() { return ImageFormat.YUV_420_888; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public long getTimestamp() { return 0; }
        public TestPlane[] getPlanes() { return planes; }
        public void close() {
            for (TestPlane p : planes) for (int i = 0; i < p.buffer.limit(); i++) p.buffer.put(i, (byte) 0);
        }
    }
    static class TestPlane {
        final ByteBuffer buffer;
        final int rowStride, pixelStride;
        TestPlane(int width, int height, int step, int padding) {
            pixelStride = step; rowStride = width * step + padding;
            buffer = ByteBuffer.allocateDirect((height - 1) * rowStride + (width - 1) * step + 1);
        }
        public int getRowStride() { return rowStride; }
        public int getPixelStride() { return pixelStride; }
        public ByteBuffer getBuffer() { return buffer; }
    }
}
