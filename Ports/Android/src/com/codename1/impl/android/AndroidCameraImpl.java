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
import java.lang.reflect.InvocationTargetException;
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
    // A focus asked for before CameraX bound, applied once it has; NaN when
    // none is pending. Without it a scanner's first center focus is dropped
    // and the next one is seconds away.
    private volatile float pendingFocusX = Float.NaN;
    private volatile float pendingFocusY = Float.NaN;
    private Object preview;
    private Object imageCapture;
    private Object imageAnalysis;
    private View previewView;
    private final AtomicBoolean listenerBusy = new AtomicBoolean();
    /**
     * The NV21 array of the last frame, handed back to the converter for the
     * next one. Only touched while listenerBusy is held, which is one frame
     * at a time. CameraFrame documents its arrays as valid only during
     * onFrame, which is what makes handing the same one out again legal.
     */
    private byte[] nv21Buffer;
    private volatile FrameListener frameListener;
    private volatile FrameFormat frameFormat = FrameFormat.JPEG;
    private volatile long frameIntervalNanos;
    private long lastFrameNanos;
    private CameraInfo info;
    private CameraSessionOptions options;
    private ExecutorService cameraExecutor;
    private boolean isFrontFacing;
    private volatile boolean closed;
    // The selector the session was bound with, kept so pause/resume and video
    // recording can bind and unbind use cases against the same camera.
    private Object cameraSelector;
    // True between pause() and resume(): the use cases are unbound and CameraX
    // has released the device.
    private volatile boolean paused;
    // Video recording state. VideoCapture is bound only while a recording runs;
    // see startVideoRecording for why it is not bound with the other use cases.
    private Object videoCapture;
    private Object recording;
    private String recordingPath;
    private boolean imageCaptureDroppedForVideo;
    private final Object recordingLock = new Object();
    private AsyncResource<String> pendingStop;
    private Throwable recordingFailure;
    private boolean recordingFinalized;

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
        CameraInfo[] real = enumerateFromCamera2();
        if (real != null && real.length > 0) {
            return real;
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

    /// The device's real cameras, as Camera2 reports them: one entry per lens
    /// facing CameraX can select ("back" and "front", the ids open() reads),
    /// taken from the first Camera2 device with that facing -- the one CameraX
    /// picks for `requireLensFacing`. Flash and auto focus are what the device
    /// reports, so a fixed-focus front camera says so instead of claiming focus.
    /// Null when Camera2 cannot be queried; the caller then falls back to a
    /// conservative table.
    private CameraInfo[] enumerateFromCamera2() {
        try {
            android.hardware.camera2.CameraManager mgr =
                    (android.hardware.camera2.CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            if (mgr == null) {
                return null;
            }
            CameraInfo back = null;
            CameraInfo front = null;
            String[] ids = mgr.getCameraIdList();
            for (int i = 0; i < ids.length; i++) {
                android.hardware.camera2.CameraCharacteristics cc = mgr.getCameraCharacteristics(ids[i]);
                Integer facing = cc.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                if (facing == null) {
                    continue;
                }
                boolean isFront = facing.intValue()
                        == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT;
                boolean isBack = facing.intValue()
                        == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK;
                if ((isFront && front != null) || (isBack && back != null) || (!isFront && !isBack)) {
                    continue;
                }
                CameraInfo ci = camera2Info(isFront ? "front" : "back",
                        isFront ? CameraFacing.FRONT : CameraFacing.BACK, cc);
                if (isFront) {
                    front = ci;
                } else {
                    back = ci;
                }
            }
            java.util.List<CameraInfo> out = new java.util.ArrayList<CameraInfo>(2);
            if (back != null) {
                out.add(back);
            }
            if (front != null) {
                out.add(front);
            }
            return out.toArray(new CameraInfo[out.size()]);
        } catch (Throwable t) {
            Log.w(TAG, "Could not enumerate cameras through Camera2: " + t);
            return null;
        }
    }

    private static CameraInfo camera2Info(String id, CameraFacing facing,
                                          android.hardware.camera2.CameraCharacteristics cc) {
        Boolean flash = cc.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
        Float minFocus = cc.get(android.hardware.camera2.CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        int[] afModes = cc.get(android.hardware.camera2.CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        // A fixed-focus lens reports a minimum focus distance of 0 and offers
        // only CONTROL_AF_MODE_OFF.
        boolean autoFocus = false;
        if (minFocus != null && minFocus.floatValue() > 0f && afModes != null) {
            for (int i = 0; i < afModes.length; i++) {
                if (afModes[i] != android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_OFF) {
                    autoFocus = true;
                    break;
                }
            }
        }
        Dimension[] photo = new Dimension[0];
        Dimension[] previewSizes = new Dimension[0];
        android.hardware.camera2.params.StreamConfigurationMap map =
                cc.get(android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map != null) {
            photo = toDimensions(map.getOutputSizes(ImageFormat.JPEG));
            previewSizes = toDimensions(map.getOutputSizes(android.graphics.SurfaceTexture.class));
        }
        return new CameraInfo(id, facing, photo, previewSizes,
                flash != null && flash.booleanValue(), autoFocus);
    }

    private static Dimension[] toDimensions(android.util.Size[] sizes) {
        if (sizes == null) {
            return new Dimension[0];
        }
        Dimension[] out = new Dimension[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            out[i] = new Dimension(sizes[i].getWidth(), sizes[i].getHeight());
        }
        return out;
    }

    @Override
    public void open(String cameraId, CameraSessionOptions opts) throws IOException {
        this.options = opts == null ? new CameraSessionOptions() : opts;
        this.isFrontFacing = "front".equals(cameraId);
        this.info = null;
        CameraInfo[] all = enumerateCameras();
        for (int i = 0; i < all.length; i++) {
            if (all[i].getFacing() == (isFrontFacing ? CameraFacing.FRONT : CameraFacing.BACK)) {
                this.info = all[i];
                break;
            }
        }
        if (this.info == null) {
            if (!"__permission_probe__".equals(cameraId)) {
                throw new IOException("This device has no " + (isFrontFacing ? "front" : "back") + " camera");
            }
            this.info = all.length > 0 ? all[0]
                    : new CameraInfo(cameraId, CameraFacing.BACK, new Dimension[0], new Dimension[0], false, false);
        }

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
            cameraSelector = selector;

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
                    .invoke(iaBuilder, analysisTargetSize(options));
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
                        float fx = pendingFocusX;
                        float fy = pendingFocusY;
                        pendingFocusX = Float.NaN;
                        if (!Float.isNaN(fx)) {
                            focus(fx, fy);
                        }
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
        if (imageCaptureDroppedForVideo) {
            result.error(new IllegalStateException("This device cannot take a photo while it records video"));
            return;
        }
        if (paused) {
            result.error(new IllegalStateException("Camera session is paused"));
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
    public void startVideoRecording(final String filePath, final boolean audio) throws IOException {
        // CameraX records through VideoCapture<Recorder> (androidx.camera:
        // camera-video, which the build injects with the rest of CameraX).
        //
        // VideoCapture is bound only while a recording runs. Preview +
        // ImageCapture + ImageAnalysis + VideoCapture is a combination a
        // LIMITED-level device does not guarantee, and binding it up front
        // would make open() fail on such a device for every session, including
        // the ones that never record. If the device refuses the fourth use
        // case, ImageCapture is unbound for the length of the recording and
        // bound again afterwards; a takePhoto() meanwhile reports why.
        if (closed || camera == null) {
            throw new IOException("Camera not opened");
        }
        if (paused) {
            throw new IOException("Camera session is paused");
        }
        if (audio) {
            requireAudioPermission();
        }
        synchronized (recordingLock) {
            if (recording != null) {
                throw new IOException("A video recording is already in progress");
            }
            pendingStop = null;
            recordingFailure = null;
            recordingFinalized = false;
        }
        final File outputFile = new File(absolutePath(filePath));
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        try {
            final Class<?> clsRecorderBuilder = Class.forName("androidx.camera.video.Recorder$Builder");
            final Class<?> clsVideoOutput = Class.forName("androidx.camera.video.VideoOutput");
            final Class<?> clsVideoCapture = Class.forName("androidx.camera.video.VideoCapture");
            final Class<?> clsFileOutputOptionsBuilder = Class.forName("androidx.camera.video.FileOutputOptions$Builder");
            final Class<?> clsFileOutputOptions = Class.forName("androidx.camera.video.FileOutputOptions");
            final Class<?> clsPendingRecording = Class.forName("androidx.camera.video.PendingRecording");
            final Class<?> clsConsumer = Class.forName("androidx.core.util.Consumer");

            final Object recorder = clsRecorderBuilder.getMethod("build")
                    .invoke(clsRecorderBuilder.getConstructor().newInstance());
            final Object vc = clsVideoCapture.getMethod("withOutput", clsVideoOutput).invoke(null, recorder);
            Object foBuilder = clsFileOutputOptionsBuilder.getConstructor(File.class).newInstance(outputFile);
            final Object fileOptions = clsFileOutputOptionsBuilder.getMethod("build").invoke(foBuilder);
            final Object listener = Proxy.newProxyInstance(clsConsumer.getClassLoader(),
                    new Class<?>[] { clsConsumer }, new RecordEventHandler(this, filePath));

            runOnMainAndWait(new MainTask() {
                @Override public void run() throws Exception {
                    try {
                        bindUseCases(new Object[] { vc });
                    } catch (Throwable first) {
                        // Too many use cases for this device: make room.
                        unbindUseCases(new Object[] { imageCapture });
                        imageCaptureDroppedForVideo = true;
                        try {
                            bindUseCases(new Object[] { vc });
                        } catch (Exception second) {
                            restoreAfterVideo(null);
                            throw second;
                        }
                    }
                    videoCapture = vc;
                    Object pending = recorder.getClass().getMethod("prepareRecording",
                            Context.class, clsFileOutputOptions)
                            .invoke(recorder, activity.getApplicationContext(), fileOptions);
                    if (audio) {
                        pending = clsPendingRecording.getMethod("withAudioEnabled").invoke(pending);
                    }
                    Object rec = clsPendingRecording.getMethod("start", Executor.class, clsConsumer)
                            .invoke(pending, mainExecutor(), listener);
                    synchronized (recordingLock) {
                        recording = rec;
                        recordingPath = filePath;
                    }
                }
            });
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            Throwable cause = t instanceof InvocationTargetException && t.getCause() != null ? t.getCause() : t;
            throw new IOException("Could not start video recording: " + cause, cause);
        }
    }

    @Override
    public void stopVideoRecording(AsyncResource<String> result) {
        Object rec;
        boolean finalized;
        Throwable failure;
        String path;
        synchronized (recordingLock) {
            rec = recording;
            finalized = recordingFinalized;
            failure = recordingFailure;
            path = recordingPath;
            if (rec == null && !finalized) {
                if (result != null) {
                    result.error(new IllegalStateException("Video recording not active"));
                }
                return;
            }
            if (!finalized) {
                pendingStop = result;
            }
        }
        if (finalized) {
            // The recording already ended on its own (an error, or the storage
            // filled up): report that outcome.
            synchronized (recordingLock) {
                recordingFinalized = false;
                recordingPath = null;
            }
            deliverStop(result, path, failure);
            return;
        }
        try {
            rec.getClass().getMethod("stop").invoke(rec);
        } catch (Throwable t) {
            synchronized (recordingLock) {
                pendingStop = null;
            }
            deliverStop(result, null, t);
        }
    }

    /// Called on the Android main thread with every VideoRecordEvent; only the
    /// Finalize event matters -- it is the one that says the file is complete.
    private static final class RecordEventHandler implements InvocationHandler {
        private final AndroidCameraImpl owner;
        private final String path;

        RecordEventHandler(AndroidCameraImpl owner, String path) {
            this.owner = owner;
            this.path = path;
        }

        @Override public Object invoke(Object p, Method m, Object[] a) {
            if ("accept".equals(m.getName()) && a != null && a.length == 1 && a[0] != null) {
                Object event = a[0];
                if (event.getClass().getName().endsWith("VideoRecordEvent$Finalize")) {
                    Throwable failure = null;
                    try {
                        boolean hasError = ((Boolean) event.getClass().getMethod("hasError").invoke(event)).booleanValue();
                        if (hasError) {
                            int code = ((Integer) event.getClass().getMethod("getError").invoke(event)).intValue();
                            Object cause = event.getClass().getMethod("getCause").invoke(event);
                            // ERROR_SOURCE_INACTIVE (4) still writes a usable file when
                            // the camera was released mid-recording; every other code
                            // means the file is missing or truncated.
                            if (code != 4) {
                                failure = new IOException("Video recording failed (CameraX error " + code + ")"
                                        + (cause instanceof Throwable ? ": " + ((Throwable) cause).getMessage() : ""));
                            }
                        }
                    } catch (Throwable t) {
                        failure = t;
                    }
                    owner.onRecordingFinalized(path, failure);
                }
                return null;
            }
            if ("hashCode".equals(m.getName())) {
                return Integer.valueOf(System.identityHashCode(p));
            }
            if ("equals".equals(m.getName())) {
                return Boolean.valueOf(a != null && a.length == 1 && a[0] == p);
            }
            if ("toString".equals(m.getName())) {
                return "RecordEventHandler";
            }
            return null;
        }
    }

    private void onRecordingFinalized(String path, Throwable failure) {
        AsyncResource<String> stop;
        synchronized (recordingLock) {
            recording = null;
            stop = pendingStop;
            pendingStop = null;
            if (stop == null) {
                // Nobody has asked to stop yet: keep the outcome for them.
                recordingFinalized = true;
                recordingFailure = failure;
            } else {
                recordingPath = null;
            }
        }
        try {
            restoreAfterVideo(videoCapture);
        } catch (Throwable t) {
            Log.w(TAG, "Could not restore the camera after recording: " + t);
        }
        if (stop != null) {
            deliverStop(stop, path, failure);
        }
    }

    // Main thread only.
    private void restoreAfterVideo(Object vc) throws Exception {
        if (vc != null) {
            unbindUseCases(new Object[] { vc });
        }
        videoCapture = null;
        if (imageCaptureDroppedForVideo) {
            imageCaptureDroppedForVideo = false;
            if (!closed && !paused) {
                bindUseCases(new Object[] { imageCapture });
            }
        }
    }

    private static void deliverStop(final AsyncResource<String> result, final String path, final Throwable failure) {
        if (result == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override public void run() {
                if (failure != null) {
                    result.error(failure);
                } else {
                    result.complete(path);
                }
            }
        });
    }

    private interface MainTask {
        void run() throws Exception;
    }

    private static final class MainTaskRunner implements Runnable {
        private final MainTask task;
        private final Exception[] err;
        private final CountDownLatch done;

        MainTaskRunner(MainTask task, Exception[] err, CountDownLatch done) {
            this.task = task;
            this.err = err;
            this.done = done;
        }

        // Whether the task started, or was abandoned before it could. A task
        // the caller gave up on must not run later: it would bind use cases
        // and start a recording the caller was told had failed.
        private boolean started;
        private boolean cancelled;

        synchronized boolean cancel() {
            if (started) {
                return false;
            }
            cancelled = true;
            return true;
        }

        private synchronized boolean begin() {
            if (cancelled) {
                return false;
            }
            started = true;
            return true;
        }

        @Override public void run() {
            if (!begin()) {
                done.countDown();
                return;
            }
            try {
                task.run();
            } catch (Exception e) {
                err[0] = e;
            } finally {
                done.countDown();
            }
        }
    }

    /// Runs `task` on the Android main thread -- where CameraX wants binding
    /// and PreviewView calls -- and waits for it, up to five seconds.
    private void runOnMainAndWait(final MainTask task) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                task.run();
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(String.valueOf(e), e);
            }
            return;
        }
        final Exception[] err = new Exception[1];
        final CountDownLatch done = new CountDownLatch(1);
        MainTaskRunner runner = new MainTaskRunner(task, err, done);
        activity.runOnUiThread(runner);
        boolean interrupted = false;
        try {
            for (;;) {
                try {
                    if (!done.await(5, TimeUnit.SECONDS)) {
                        if (runner.cancel()) {
                            throw new IOException("Timed out waiting for the Android main thread");
                        }
                        // Already running: its outcome is the answer, so wait for it.
                        continue;
                    }
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        if (err[0] != null) {
            Throwable cause = err[0] instanceof InvocationTargetException && err[0].getCause() != null
                    ? err[0].getCause() : err[0];
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException(String.valueOf(cause), cause);
        }
    }

    // Main thread only.
    private void bindUseCases(Object[] cases) throws Exception {
        if (cameraProvider == null) {
            throw new IOException("Camera not opened");
        }
        Object lifecycleOwner = clsProcessLifecycleOwner.getMethod("get").invoke(null);
        Object useCases = java.lang.reflect.Array.newInstance(clsUseCase, cases.length);
        for (int i = 0; i < cases.length; i++) {
            java.lang.reflect.Array.set(useCases, i, cases[i]);
        }
        Method bind = clsProcessCameraProvider.getMethod("bindToLifecycle",
                clsLifecycleOwner, clsCameraSelector, useCases.getClass());
        camera = bind.invoke(cameraProvider, lifecycleOwner, cameraSelector, useCases);
    }

    // Main thread only.
    private void unbindUseCases(Object[] cases) throws Exception {
        if (cameraProvider == null) {
            return;
        }
        int n = 0;
        for (int i = 0; i < cases.length; i++) {
            if (cases[i] != null) {
                n++;
            }
        }
        Object useCases = java.lang.reflect.Array.newInstance(clsUseCase, n);
        int j = 0;
        for (int i = 0; i < cases.length; i++) {
            if (cases[i] != null) {
                java.lang.reflect.Array.set(useCases, j++, cases[i]);
            }
        }
        clsProcessCameraProvider.getMethod("unbind", useCases.getClass()).invoke(cameraProvider, useCases);
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
                frame = img == null ? null : createFrame(img, rotation, now, frameFormat, nv21Buffer);
                if (frame != null && frame.getRawBytes() != null) {
                    nv21Buffer = frame.getRawBytes();
                }
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

    /**
     * The resolution frames are analysed at when the session names none.
     *
     * A session that asks for NV21 frames -- every Vision scanner -- used to
     * get 640x480, which is too few pixels per module for a dense code such
     * as a PDF417: it read on a phone with a sharp lens and failed on others.
     * It now gets 1440x1080. That is 4:3 on purpose: the Preview use case is
     * 4:3 by default, so what is analysed stays exactly what is shown and a
     * rectangle a detector reports still maps onto the preview. 1080p would
     * crop the analysed field to 16:9 for no more pixels across a code that
     * fills the short side. This is a request, not a guarantee: CameraX picks
     * the nearest size the device supports for the three use cases bound
     * together, so a limited camera answers with less.
     *
     * Every other session keeps 640x480: JPEG, and RGBA8888, which this port
     * answers with JPEG. Each of their frames is compressed on the analysis
     * thread whether or not anyone reads it closely.
     *
     * CameraX reads the size in the display's current rotation, not the
     * sensor's, so the default is turned to match. The display is asked, not
     * the window: in split screen a window can be wider than tall on a
     * display that is portrait. A size the application asked for is passed
     * through as it always was.
     */
    private android.util.Size analysisTargetSize(CameraSessionOptions options) {
        boolean portrait = false;
        try {
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            portrait = metrics.heightPixels > metrics.widthPixels;
        } catch (Throwable t) {
            Log.w(TAG, "Could not read the display orientation: " + t);
        }
        int[] size = AndroidCameraFrameConverter.analysisSize(
                options.getPreviewWidth(), options.getPreviewHeight(),
                options.getFrameFormat() == FrameFormat.NV21,
                portrait);
        return new android.util.Size(size[0], size[1]);
    }

    private static void closeImageProxy(Object imageProxy) {
        try {
            imageProxy.getClass().getMethod("close").invoke(imageProxy);
        } catch (Throwable ignored) { }
    }

    private static CameraFrame createFrame(Image image, int rotation, long timestamp,
                                           FrameFormat requested, byte[] reuse) {
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
                planes[2].getBuffer(), planes[2].getRowStride(), planes[2].getPixelStride(),
                reuse);
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
        if (!closed && !paused && camera == null) {
            pendingFocusY = yNorm;
            pendingFocusX = xNorm;
            return;
        }
        if (closed || paused || camera == null || (info != null && !info.hasAutoFocus())) {
            return;
        }
        final float x = Math.max(0f, Math.min(1f, xNorm));
        final float y = Math.max(0f, Math.min(1f, yNorm));
        // PreviewView's factory maps a point on the view through its scale type
        // and rotation onto the sensor, so (x, y) means the same spot the user
        // sees. It has to be asked on the main thread, and only works once the
        // view is laid out; before that the point is taken relative to the
        // analysed frame, which covers the same field of view.
        activity.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    if (closed || paused || camera == null) {
                        return;
                    }
                    Class<?> clsFactory = Class.forName("androidx.camera.core.MeteringPointFactory");
                    Object factory = null;
                    float px = x;
                    float py = y;
                    if (previewView != null && previewView.getWidth() > 0 && previewView.getHeight() > 0) {
                        factory = clsPreviewView.getMethod("getMeteringPointFactory").invoke(previewView);
                        px = x * previewView.getWidth();
                        py = y * previewView.getHeight();
                    }
                    if (factory == null) {
                        factory = Class.forName("androidx.camera.core.SurfaceOrientedMeteringPointFactory")
                                .getConstructor(float.class, float.class)
                                .newInstance(Float.valueOf(1f), Float.valueOf(1f));
                    }
                    Object point = clsFactory.getMethod("createPoint", float.class, float.class)
                            .invoke(factory, Float.valueOf(px), Float.valueOf(py));
                    Class<?> clsPoint = Class.forName("androidx.camera.core.MeteringPoint");
                    Class<?> clsBuilder = Class.forName("androidx.camera.core.FocusMeteringAction$Builder");
                    // The one-argument builder meters AF, AE and AWB at the point
                    // and lets CameraX return to continuous focus after 5 s.
                    Object builder = clsBuilder.getConstructor(clsPoint).newInstance(point);
                    Object action = clsBuilder.getMethod("build").invoke(builder);
                    Object cameraControl = cameraControl();
                    if (cameraControl != null) {
                        cameraControlMethod("startFocusAndMetering",
                                Class.forName("androidx.camera.core.FocusMeteringAction"))
                                .invoke(cameraControl, action);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Could not focus: " + t);
                }
            }
        });
    }

    @Override
    public void pause() {
        // CameraSession's contract: pause releases the hardware (so a modal
        // Capture, or another app, can open the camera) while the session
        // object survives. CameraX releases the device when its last use case
        // is unbound. A recording in progress ends here, the same way it ends
        // when CameraX loses the camera: its file is finalized and stop()
        // reports it.
        if (closed || paused) {
            return;
        }
        paused = true;
        Object rec;
        synchronized (recordingLock) {
            rec = recording;
        }
        if (rec != null) {
            try {
                rec.getClass().getMethod("stop").invoke(rec);
            } catch (Throwable t) {
                Log.w(TAG, "Could not stop the recording on pause: " + t);
            }
        }
        try {
            runOnMainAndWait(new MainTask() {
                @Override public void run() throws Exception {
                    unbindUseCases(new Object[] { preview, imageCapture, imageAnalysis, videoCapture });
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Could not release the camera on pause", e);
        }
    }

    @Override
    public void resume() {
        if (closed || !paused) {
            return;
        }
        try {
            runOnMainAndWait(new MainTask() {
                @Override public void run() throws Exception {
                    paused = false;
                    bindUseCases(imageCaptureDroppedForVideo
                            ? new Object[] { preview, imageAnalysis }
                            : new Object[] { preview, imageCapture, imageAnalysis });
                }
            });
        } catch (IOException e) {
            paused = false;
            Log.e(TAG, "Could not re-acquire the camera on resume", e);
        }
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
        Object rec;
        synchronized (recordingLock) {
            rec = recording;
        }
        if (rec != null) {
            // Finalizes the file; a pending stopVideoRecording still gets it.
            try {
                rec.getClass().getMethod("stop").invoke(rec);
            } catch (Throwable t) {
                Log.w(TAG, "Could not stop the recording on close: " + t);
            }
        }
        // Native state belongs to the main-thread callback until it completes.
        // A stalled main thread must not turn open's timeout into an EDT hang.
        final CountDownLatch unbound = new CountDownLatch(1);
        boolean dispatched = false;
        boolean interrupted = false;
        try {
            activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    try {
                        // Deferred cleanup must not unbind a newer session,
                        // so only this session's use cases are named.
                        unbindUseCases(new Object[] { preview, imageCapture, imageAnalysis, videoCapture });
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
        videoCapture = null;
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
