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
package com.codename1.impl.android;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.codename1.camera.CameraFacing;
import com.codename1.camera.CameraFrame;
import com.codename1.camera.CameraInfo;
import com.codename1.camera.CameraSessionOptions;
import com.codename1.camera.CapturedPhoto;
import com.codename1.camera.FlashMode;
import com.codename1.camera.FrameFormat;
import com.codename1.camera.FrameListener;
import com.codename1.camera.PhotoCaptureOptions;
import com.codename1.impl.CameraImpl;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Display;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.geom.Dimension;
import com.codename1.util.AsyncResource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/// Android implementation of `CameraImpl` backed by CameraX
/// (`androidx.camera:camera-core` + `camera-camera2` + `camera-lifecycle`
/// + `camera-view` + `camera-video`). Those dependencies are added to the
/// end-user app's `build.gradle` automatically by
/// `AiDependencyTable` (entry "com/codename1/camera/").
///
/// Because the Codename One Android port itself does not have CameraX on
/// its compile classpath, all CameraX calls go through reflection. This
/// matches the existing pattern in `com.codename1.io.oidc.OidcBrowserNativeImpl`
/// and `com.codename1.io.webauthn.WebAuthnNativeImpl`.
///
/// @hidden
public class AndroidCameraImpl extends CameraImpl {

    private static final String TAG = "AndroidCameraImpl";

    // Reflectively-loaded CameraX class references. Resolved lazily because
    // they may not be present at port load time -- only the end-user app
    // build pulls them in via Gradle.
    private static volatile boolean cxResolved;
    private static Class<?> clsProcessCameraProvider;
    private static Class<?> clsCameraSelector;
    private static Class<?> clsCameraSelectorBuilder;
    private static Class<?> clsPreview;
    private static Class<?> clsPreviewBuilder;
    private static Class<?> clsImageCapture;
    private static Class<?> clsImageCaptureBuilder;
    private static Class<?> clsImageAnalysis;
    private static Class<?> clsImageAnalysisBuilder;
    private static Class<?> clsImageAnalysisAnalyzer;
    private static Class<?> clsUseCase;
    private static Class<?> clsCameraXProxy;
    private static Class<?> clsPreviewView;
    private static Class<?> clsProcessLifecycleOwner;
    private static Class<?> clsLifecycleOwner;
    private static Object frontLensFacing;
    private static Object backLensFacing;

    private final Activity activity;
    private Object cameraProvider;
    private Object camera;
    private Object preview;
    private Object imageCapture;
    private Object imageAnalysis;
    private View previewView;
    private final AtomicBoolean listenerBusy = new AtomicBoolean();
    private volatile FrameListener frameListener;
    private volatile FrameFormat frameFormat = FrameFormat.JPEG;
    private volatile long frameIntervalNanos;
    private long lastFrameNanos;
    private CameraInfo info;
    private CameraSessionOptions options;
    private ExecutorService cameraExecutor;
    private boolean isFrontFacing;
    private volatile boolean closed;

    public AndroidCameraImpl(Activity activity) {
        this.activity = activity;
    }

    // --------------------------------------------------------------------
    // Reflection setup
    // --------------------------------------------------------------------

    private static synchronized boolean ensureCameraXResolved() {
        if (cxResolved) {
            return clsProcessCameraProvider != null;
        }
        cxResolved = true;
        try {
            clsProcessCameraProvider = Class.forName("androidx.camera.lifecycle.ProcessCameraProvider");
            clsCameraSelector = Class.forName("androidx.camera.core.CameraSelector");
            clsCameraSelectorBuilder = Class.forName("androidx.camera.core.CameraSelector$Builder");
            clsPreview = Class.forName("androidx.camera.core.Preview");
            clsPreviewBuilder = Class.forName("androidx.camera.core.Preview$Builder");
            clsImageCapture = Class.forName("androidx.camera.core.ImageCapture");
            clsImageCaptureBuilder = Class.forName("androidx.camera.core.ImageCapture$Builder");
            clsImageAnalysis = Class.forName("androidx.camera.core.ImageAnalysis");
            clsImageAnalysisBuilder = Class.forName("androidx.camera.core.ImageAnalysis$Builder");
            clsImageAnalysisAnalyzer = Class.forName("androidx.camera.core.ImageAnalysis$Analyzer");
            clsUseCase = Class.forName("androidx.camera.core.UseCase");
            clsPreviewView = Class.forName("androidx.camera.view.PreviewView");
            clsProcessLifecycleOwner = Class.forName("androidx.lifecycle.ProcessLifecycleOwner");
            clsLifecycleOwner = Class.forName("androidx.lifecycle.LifecycleOwner");

            frontLensFacing = clsCameraSelector.getField("LENS_FACING_FRONT").get(null);
            backLensFacing = clsCameraSelector.getField("LENS_FACING_BACK").get(null);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "CameraX classes not present on classpath: " + t.getMessage());
            clsProcessCameraProvider = null;
            return false;
        }
    }

    // --------------------------------------------------------------------
    // CameraImpl
    // --------------------------------------------------------------------

    @Override
    public CameraInfo[] enumerateCameras() {
        if (!ensureCameraXResolved()) {
            return new CameraInfo[] {
                new CameraInfo("back", CameraFacing.BACK,
                        new Dimension[0], new Dimension[0], false, true)
            };
        }
        return new CameraInfo[] {
            new CameraInfo("back", CameraFacing.BACK,
                    new Dimension[] { new Dimension(1920, 1080), new Dimension(1280, 720) },
                    new Dimension[] { new Dimension(1280, 720),  new Dimension(640, 480) },
                    true, true),
            new CameraInfo("front", CameraFacing.FRONT,
                    new Dimension[] { new Dimension(1280, 720),  new Dimension(640, 480) },
                    new Dimension[] { new Dimension(1280, 720) },
                    false, true)
        };
    }

    @Override
    public void open(String cameraId, CameraSessionOptions opts) throws IOException {
        this.options = opts == null ? new CameraSessionOptions() : opts;
        this.isFrontFacing = "front".equals(cameraId);
        this.info = isFrontFacing ? enumerateCameras()[1] : enumerateCameras()[0];

        if ("__permission_probe__".equals(cameraId)) {
            requireCameraPermission();
            if (options.isCaptureAudio()) requireAudioPermission();
            return;
        }

        if (!ensureCameraXResolved()) {
            throw new IOException("CameraX is not on the runtime classpath. "
                    + "Make sure the build server injected the androidx.camera deps.");
        }

        requireCameraPermission();
        if (options.isCaptureAudio()) requireAudioPermission();

        cameraExecutor = Executors.newSingleThreadExecutor();

        try {
            // Build a CameraSelector for our facing.
            Object selectorBuilder = clsCameraSelectorBuilder.getConstructor().newInstance();
            clsCameraSelectorBuilder.getMethod("requireLensFacing", int.class)
                    .invoke(selectorBuilder, isFrontFacing ? frontLensFacing : backLensFacing);
            Object selector = clsCameraSelectorBuilder.getMethod("build").invoke(selectorBuilder);

            // Preview
            Object previewBuilder = clsPreviewBuilder.getConstructor().newInstance();
            preview = clsPreviewBuilder.getMethod("build").invoke(previewBuilder);

            // ImageCapture
            Object icBuilder = clsImageCaptureBuilder.getConstructor().newInstance();
            imageCapture = clsImageCaptureBuilder.getMethod("build").invoke(icBuilder);

            // ImageAnalysis (only built if we need frame delivery)
            Object iaBuilder = clsImageAnalysisBuilder.getConstructor().newInstance();
            clsImageAnalysisBuilder.getMethod("setTargetResolution",
                    Class.forName("android.util.Size"))
                    .invoke(iaBuilder,
                            new android.util.Size(
                                    options.getPreviewWidth() > 0 ? options.getPreviewWidth() : 640,
                                    options.getPreviewHeight() > 0 ? options.getPreviewHeight() : 480));
            // STRATEGY_KEEP_ONLY_LATEST = 0 in CameraX
            try {
                clsImageAnalysisBuilder.getMethod("setBackpressureStrategy", int.class)
                        .invoke(iaBuilder, 0);
            } catch (Throwable ignored) { /* old CameraX */ }
            imageAnalysis = clsImageAnalysisBuilder.getMethod("build").invoke(iaBuilder);

            installFrameAnalyzer();

            // Get the singleton ProcessCameraProvider asynchronously and bind.
            final Object cameraSelectorFinal = selector;
            Method getInstance = clsProcessCameraProvider.getMethod("getInstance", Context.class);
            final Object futureProvider = getInstance.invoke(null, activity.getApplicationContext());
            // ListenableFuture#addListener(Runnable, Executor)
            Method addListener = futureProvider.getClass()
                    .getMethod("addListener", Runnable.class, Executor.class);
            final IOException[] openErr = new IOException[1];
            final Object lock = new Object();
            final boolean[] done = new boolean[1];
            addListener.invoke(futureProvider, new Runnable() {
                @Override public void run() {
                    try {
                        if (closed) {
                            throw new IOException("Camera closed before binding");
                        }
                        // CameraX checks Android's main thread in both PreviewView and
                        // setSurfaceProvider. The caller is normally the CN1 EDT.
                        // Attach the provider before binding, so the camera cannot start
                        // opening with a Preview that has no surface provider.
                        preparePreview();
                        cameraProvider = futureProvider.getClass().getMethod("get").invoke(futureProvider);
                        Object lifecycleOwner = clsProcessLifecycleOwner
                                .getMethod("get").invoke(null);
                        // bindToLifecycle(LifecycleOwner, CameraSelector, UseCase[])
                        Method bind = clsProcessCameraProvider.getMethod("bindToLifecycle",
                                clsLifecycleOwner, clsCameraSelector,
                                java.lang.reflect.Array.newInstance(clsUseCase, 0).getClass());
                        Object useCases = java.lang.reflect.Array.newInstance(clsUseCase, 3);
                        java.lang.reflect.Array.set(useCases, 0, preview);
                        java.lang.reflect.Array.set(useCases, 1, imageCapture);
                        java.lang.reflect.Array.set(useCases, 2, imageAnalysis);
                        camera = bind.invoke(cameraProvider, lifecycleOwner, cameraSelectorFinal, useCases);
                    } catch (Throwable t) {
                        openErr[0] = new IOException("CameraX bind failed: " + t.getMessage(), t);
                    } finally {
                        synchronized (lock) { done[0] = true; lock.notifyAll(); }
                    }
                }
            }, mainExecutor());

            // Wait up to 5s for binding.
            synchronized (lock) {
                long deadline = System.currentTimeMillis() + 5000;
                while (!done[0] && System.currentTimeMillis() < deadline) {
                    try { lock.wait(200); } catch (InterruptedException ignored) { }
                }
            }
            if (openErr[0] != null) throw openErr[0];
            if (camera == null) throw new IOException("CameraX bind timed out");
        } catch (IOException e) {
            close();
            throw e;
        } catch (Throwable t) {
            close();
            throw new IOException("Could not open Android camera: " + t.getMessage(), t);
        }
    }

    // Called only by the main-executor binding callback. Keep the native View
    // setup here, but create the CN1 peer on the calling (CN1 EDT) thread.
    private void preparePreview() throws Exception {
        Constructor<?> ctor = clsPreviewView.getConstructor(Context.class);
        previewView = (View) ctor.newInstance(activity);
        Object surfaceProvider = clsPreviewView.getMethod("getSurfaceProvider")
                .invoke(previewView);
        Class<?> surfaceProviderCls = Class.forName(
                "androidx.camera.core.Preview$SurfaceProvider");
        clsPreview.getMethod("setSurfaceProvider", surfaceProviderCls)
                .invoke(preview, surfaceProvider);
    }

    @Override
    public PeerComponent createPreviewPeer() {
        return closed || previewView == null ? null : PeerComponent.create(previewView);
    }

    @Override
    public void takePhoto(final PhotoCaptureOptions opts, final AsyncResource<CapturedPhoto> result) {
        if (imageCapture == null) {
            result.error(new IllegalStateException("Camera not opened"));
            return;
        }
        // CameraX's in-memory OnImageCapturedCallback is an abstract class
        // that java.lang.reflect.Proxy can't subclass. Use the to-file
        // takePicture(OutputFileOptions, Executor, OnImageSavedCallback)
        // overload instead -- OnImageSavedCallback is an interface, so Proxy
        // works. We read the resulting JPEG back into memory before
        // resolving the AsyncResource.
        try {
            final String filePath = opts.getFilePath() != null
                    ? opts.getFilePath()
                    : tempPhotoPath();
            final File outputFile = new File(absolutePath(filePath));
            Class<?> ofoBuilderCls = Class.forName(
                    "androidx.camera.core.ImageCapture$OutputFileOptions$Builder");
            Constructor<?> ofoCtor = ofoBuilderCls.getConstructor(File.class);
            Object ofoBuilder = ofoCtor.newInstance(outputFile);
            Object outputFileOptions = ofoBuilderCls.getMethod("build").invoke(ofoBuilder);

            Class<?> savedCbCls = Class.forName(
                    "androidx.camera.core.ImageCapture$OnImageSavedCallback");
            Object savedCb = Proxy.newProxyInstance(savedCbCls.getClassLoader(),
                    new Class<?>[] { savedCbCls },
                    new ImageSavedHandler(outputFile, filePath, result));

            Class<?> ofoCls = Class.forName(
                    "androidx.camera.core.ImageCapture$OutputFileOptions");
            clsImageCapture.getMethod("takePicture", ofoCls, Executor.class, savedCbCls)
                    .invoke(imageCapture, outputFileOptions, cameraExecutor, savedCb);
        } catch (Throwable t) {
            result.error(t);
        }
    }

    /// Static inner class so SpotBugs doesn't flag it as SIC_INNER_SHOULD_BE_STATIC_ANON
    /// (and so we don't pin the AndroidCameraImpl instance for the duration of
    /// a slow shutter / write).
    private static final class ImageSavedHandler implements InvocationHandler {
        private final File outputFile;
        private final String filePath;
        private final AsyncResource<CapturedPhoto> result;

        ImageSavedHandler(File outputFile, String filePath, AsyncResource<CapturedPhoto> result) {
            this.outputFile = outputFile;
            this.filePath = filePath;
            this.result = result;
        }

        @Override public Object invoke(Object p, Method m, Object[] a) {
            String n = m.getName();
            if ("onImageSaved".equals(n)) {
                Display.getInstance().callSerially(new Runnable() {
                    @Override public void run() {
                        try {
                            byte[] bytes = readAll(outputFile);
                            int[] wh = readJpegSize(bytes);
                            result.complete(new CapturedPhoto(
                                    bytes, filePath, wh[0], wh[1]));
                        } catch (Throwable t) {
                            result.error(t);
                        }
                    }
                });
                return null;
            }
            if ("onError".equals(n)) {
                final Throwable t = (Throwable) a[0];
                Display.getInstance().callSerially(new Runnable() {
                    @Override public void run() { result.error(t); }
                });
                return null;
            }
            return null;
        }
    }

    @Override
    public void startVideoRecording(String filePath, boolean audio) throws IOException {
        // Video recording via CameraX requires VideoCapture which is shipped
        // in androidx.camera:camera-video. Implementing it via reflection in
        // this v1 is intentionally limited: most apps use the legacy
        // com.codename1.capture.Capture#captureVideo for full-file recording
        // and the new API for preview/photo/frames. A full reflective
        // implementation would mirror the takePicture-to-file pattern above
        // against Recorder + PendingRecording.
        throw new IOException("Video recording via CameraX is not yet supported on Android. "
                + "Use com.codename1.capture.Capture.captureVideo() for video.");
    }

    @Override
    public void stopVideoRecording(AsyncResource<String> result) {
        if (result != null) {
            result.error(new IllegalStateException("Video recording not active"));
        }
    }

    @Override
    public void setFrameListener(FrameListener listener, FrameFormat format, int maxFps) {
        this.frameFormat = format == null ? FrameFormat.JPEG : format;
        this.frameIntervalNanos = maxFps > 0 ? 1000000000L / maxFps : 0;
        this.frameListener = listener;
    }

    private void installFrameAnalyzer() {
        if (imageAnalysis == null) return;
        try {
            Object analyzer = Proxy.newProxyInstance(
                    clsImageAnalysisAnalyzer.getClassLoader(),
                    new Class<?>[] { clsImageAnalysisAnalyzer },
                    new InvocationHandler() {
                        @Override public Object invoke(Object p, Method m, Object[] a) {
                            if ("analyze".equals(m.getName())) {
                                onImageProxy(a[0]);
                            }
                            return null;
                        }
                    });
            clsImageAnalysis.getMethod("setAnalyzer", Executor.class, clsImageAnalysisAnalyzer)
                    .invoke(imageAnalysis, cameraExecutor, analyzer);
        } catch (Throwable t) {
            Log.w(TAG, "Could not install frame analyzer: " + t.getMessage());
        }
    }

    private void onImageProxy(Object imageProxy) {
        FrameListener l = frameListener;
        if (l == null) {
            // Always close the proxy or CameraX will stop delivering.
            closeImageProxy(imageProxy);
            return;
        }
        if (!listenerBusy.compareAndSet(false, true)) {
            closeImageProxy(imageProxy);
            return;
        }
        try {
            long now = System.nanoTime();
            if (lastFrameNanos != 0 && now - lastFrameNanos < frameIntervalNanos) {
                return;
            }
            CameraFrame frame;
            try {
                Image img = (Image) imageProxy.getClass().getMethod("getImage").invoke(imageProxy);
                Object imageInfo = imageProxy.getClass().getMethod("getImageInfo").invoke(imageProxy);
                int rotation = (Integer) imageInfo.getClass().getMethod("getRotationDegrees").invoke(imageInfo);
                frame = img == null ? null : createFrame(img, rotation, now, frameFormat);
            } catch (Throwable t) {
                Log.w(TAG, "Frame conversion failed: " + t.getMessage());
                return;
            }
            if (frame != null) {
                lastFrameNanos = now;
                l.onFrame(frame);
            }
        } finally {
            listenerBusy.set(false);
            closeImageProxy(imageProxy);
        }
    }

    private static void closeImageProxy(Object imageProxy) {
        try {
            imageProxy.getClass().getMethod("close").invoke(imageProxy);
        } catch (Throwable ignored) { }
    }

    private static CameraFrame createFrame(Image image, int rotation, long timestamp,
                                           FrameFormat requested) {
        final int w = image.getWidth();
        final int h = image.getHeight();
        if (image.getFormat() == ImageFormat.JPEG) {
            ByteBuffer buf = image.getPlanes()[0].getBuffer().duplicate();
            byte[] jpeg = new byte[buf.remaining()];
            buf.get(jpeg);
            return new CameraFrame(jpeg, null, w, h, rotation, timestamp, requested);
        }
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Unsupported camera image format");
        }
        Image.Plane[] planes = image.getPlanes();
        final byte[] nv21 = AndroidCameraFrameConverter.toNV21(w, h,
                planes[0].getBuffer(), planes[0].getRowStride(), planes[0].getPixelStride(),
                planes[1].getBuffer(), planes[1].getRowStride(), planes[1].getPixelStride(),
                planes[2].getBuffer(), planes[2].getRowStride(), planes[2].getPixelStride());
        CameraFrame.JpegEncoder encoder = new CameraFrame.JpegEncoder() {
            public byte[] encode() {
                YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, w, h, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if (!yuv.compressToJpeg(new Rect(0, 0, w, h), 80, baos)) {
                    throw new IllegalStateException("Could not encode camera frame");
                }
                return baos.toByteArray();
            }
        };
        if (requested == FrameFormat.NV21) {
            return new CameraFrame(null, nv21, w, h, rotation, timestamp, requested, encoder);
        }
        // JPEG remains the fallback for raw formats this port cannot expose.
        return new CameraFrame(encoder.encode(), null, w, h, rotation, timestamp, requested);
    }

    @Override
    public void setFlashMode(FlashMode mode) {
        // TORCH is not an ImageCapture flash mode: ImageCapture only decides
        // whether the flash fires for a still, so mapping TORCH onto it lit
        // nothing and the scanner's torch button did nothing on Android. The
        // continuous light is CameraControl#enableTorch, which needs no
        // permission beyond CAMERA.
        boolean torch = mode == FlashMode.TORCH;
        if (imageCapture != null) {
            try {
                // ImageCapture.FLASH_MODE_AUTO = 0, _ON = 1, _OFF = 2. With the
                // torch lit the still needs no flash of its own.
                int code = mode == FlashMode.ON ? 1
                        : mode == FlashMode.AUTO ? 0
                        : 2;
                clsImageCapture.getMethod("setFlashMode", int.class).invoke(imageCapture, code);
            } catch (Throwable t) {
                Log.w(TAG, "Could not set flash mode: " + t);
            }
        }
        try {
            Object cameraControl = cameraControl();
            if (cameraControl != null) {
                cameraControlMethod("enableTorch", boolean.class).invoke(cameraControl, torch);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not switch torch: " + t);
        }
    }

    @Override
    public void setZoom(float ratio) {
        try {
            Object cameraControl = cameraControl();
            if (cameraControl != null) {
                cameraControlMethod("setZoomRatio", float.class).invoke(cameraControl, ratio);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not set zoom: " + t);
        }
    }

    // The bound camera and its control are instances of CameraX classes that
    // are not public, and Method#invoke refuses a public method reached through
    // a non-public class. Resolve both methods on the public interfaces.
    private Object cameraControl() throws Exception {
        if (camera == null) {
            return null;
        }
        return Class.forName("androidx.camera.core.Camera")
                .getMethod("getCameraControl").invoke(camera);
    }

    private static Method cameraControlMethod(String name, Class<?> argument) throws Exception {
        return Class.forName("androidx.camera.core.CameraControl").getMethod(name, argument);
    }

    @Override
    public void focus(float xNorm, float yNorm) {
        // Reflective focus would need MeteringPointFactory + FocusMeteringAction
        // builder reflection. Left as a documented no-op in v1.
    }

    @Override
    public void pause() {
        // CameraX is bound to ProcessLifecycleOwner -- it pauses automatically
        // when the activity does. Explicit pause for a custom session would
        // require unbinding/rebinding; deferred.
    }

    @Override
    public void resume() {
        // See pause().
    }

    @Override
    public void close() {
        final ExecutorService executor;
        synchronized (this) {
            if (closed) return;
            closed = true;
            executor = cameraExecutor;
            cameraExecutor = null;
        }
        frameListener = null;
        // Native state belongs to the main-thread callback until it completes.
        // A stalled main thread must not turn open's timeout into an EDT hang.
        final CountDownLatch unbound = new CountDownLatch(1);
        boolean dispatched = false;
        boolean interrupted = false;
        try {
            activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    try {
                        if (cameraProvider != null) {
                            Object useCases = java.lang.reflect.Array.newInstance(clsUseCase, 3);
                            java.lang.reflect.Array.set(useCases, 0, preview);
                            java.lang.reflect.Array.set(useCases, 1, imageCapture);
                            java.lang.reflect.Array.set(useCases, 2, imageAnalysis);
                            // Deferred cleanup must not unbind a newer session.
                            clsProcessCameraProvider.getMethod("unbind", useCases.getClass())
                                    .invoke(cameraProvider, useCases);
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "Could not unbind camera", t);
                    } finally {
                        clearCameraReferences();
                        unbound.countDown();
                    }
                }
            });
            dispatched = true;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            for (;;) {
                try {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0 || !unbound.await(remaining, TimeUnit.NANOSECONDS)) {
                        Log.w(TAG, "Camera teardown deferred until Android main thread resumes");
                    }
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Could not dispatch camera teardown", t);
        } finally {
            // If dispatched, only that callback may clear the references: it
            // can still need them after this bounded wait has returned.
            if (!dispatched) {
                clearCameraReferences();
            }
            if (executor != null) {
                executor.shutdown();
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void clearCameraReferences() {
        previewView = null;
        camera = null;
        preview = null;
        imageCapture = null;
        imageAnalysis = null;
        cameraProvider = null;
    }

    // --------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------

    private void requireCameraPermission() throws IOException {
        if (!AndroidNativeUtil.checkForPermission(Manifest.permission.CAMERA,
                "Required to access the camera.")) {
            throw new IOException("Camera permission denied");
        }
    }

    private void requireAudioPermission() throws IOException {
        if (!AndroidNativeUtil.checkForPermission(Manifest.permission.RECORD_AUDIO,
                "Required to capture audio for video recording.")) {
            throw new IOException("Microphone permission denied");
        }
    }

    private static Executor mainExecutor() {
        final Handler h = new Handler(Looper.getMainLooper());
        return new Executor() {
            @Override public void execute(Runnable r) { h.post(r); }
        };
    }

    private static String tempPhotoPath() {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String dir = fs.getAppHomePath();
        if (!dir.endsWith("/")) dir += "/";
        return dir + "cn1_photo_" + System.currentTimeMillis() + ".jpg";
    }

    private static String absolutePath(String fsStoragePath) {
        // Map FileSystemStorage path (file://...) to a plain filesystem path.
        if (fsStoragePath.startsWith("file:")) {
            return fsStoragePath.substring(fsStoragePath.indexOf(':') + 1);
        }
        return fsStoragePath;
    }

    private static byte[] readAll(File f) throws IOException {
        java.io.InputStream in = new java.io.FileInputStream(f);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) baos.write(buf, 0, n);
            return baos.toByteArray();
        } finally {
            in.close();
        }
    }

    private static int[] readJpegSize(byte[] bytes) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
        return new int[] { o.outWidth, o.outHeight };
    }
}
