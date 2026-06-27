/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.camera.CameraFacing;
import com.codename1.camera.CameraFrame;
import com.codename1.camera.CameraInfo;
import com.codename1.camera.CameraSessionOptions;
import com.codename1.camera.CapturedPhoto;
import com.codename1.camera.FlashMode;
import com.codename1.camera.FrameFormat;
import com.codename1.camera.FrameListener;
import com.codename1.camera.PhotoCaptureOptions;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.browser.TimerHandler;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.HTMLVideoElement;
import com.codename1.impl.CameraImpl;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.geom.Dimension;
import com.codename1.util.AsyncResource;
import com.codename1.util.Base64;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/// JavaScript / browser implementation of `CameraImpl` backed by the
/// MediaDevices `getUserMedia` API.
///
/// On the worker ParparVM port `navigator.mediaDevices`, the `<video>` element
/// and the capture `<canvas>` all live on the MAIN thread -- they cannot be
/// touched from the worker and a MediaStream cannot be structured-cloned across
/// the worker boundary. So the whole media session runs on the main thread
/// behind host natives (`nativeCameraOpen/Size/Grab/Close`): the worker only
/// holds the opaque `<video>` host-ref, which it hands to `PeerComponent` for
/// the live preview and back to the host to grab still frames as JPEG.
///
/// **Browser caveats**: getUserMedia requires a secure context (HTTPS or
/// localhost) and a user gesture -- `Camera.open(...)` must originate from a tap.
///
/// Video recording is not implemented (it would route through `MediaRecorder`);
/// callers needing video should use `com.codename1.capture.Capture#captureVideo`.
///
/// @hidden
public class HTML5CameraImpl extends CameraImpl {

    private static final int DEFAULT_W = 640;
    private static final int DEFAULT_H = 480;

    private HTMLVideoElement video;
    private CameraSessionOptions options;
    private CameraInfo info;
    private final AtomicBoolean listenerBusy = new AtomicBoolean();
    private volatile FrameListener frameListener;
    private volatile FrameFormat frameFormat = FrameFormat.JPEG;
    private int frameMaxFps = 5;
    private int frameTimerHandle;
    private boolean closed;
    private boolean isFrontFacing;

    @Override
    public CameraInfo[] enumerateCameras() {
        return new CameraInfo[] {
            new CameraInfo("user", CameraFacing.FRONT,
                    new Dimension[0], new Dimension[0], false, true),
            new CameraInfo("environment", CameraFacing.BACK,
                    new Dimension[0], new Dimension[0], false, true)
        };
    }

    @Override
    public void open(String cameraId, CameraSessionOptions opts) throws IOException {
        this.options = opts == null ? new CameraSessionOptions() : opts;
        this.frameMaxFps = Math.max(1, Math.min(15, options.getFrameMaxFps() > 0 ? options.getFrameMaxFps() : 5));
        this.isFrontFacing = "user".equals(cameraId) || "front".equals(cameraId);
        this.info = new CameraInfo(cameraId == null ? "environment" : cameraId,
                isFrontFacing ? CameraFacing.FRONT : CameraFacing.BACK,
                new Dimension[0], new Dimension[0], false, true);

        if ("__permission_probe__".equals(cameraId)) {
            return;
        }
        if (!nativeCameraSupported()) {
            throw new IOException("navigator.mediaDevices.getUserMedia is not available");
        }
        // Blocking host call: getUserMedia + build the <video>, on the main
        // thread. Returns the opaque video host-ref (or null on deny/timeout).
        HTMLVideoElement v = nativeCameraOpen(isFrontFacing ? "user" : "environment",
                options.isCaptureAudio());
        if (v == null) {
            throw new IOException("getUserMedia failed: " + nativeCameraLastError());
        }
        this.video = v;
    }

    @Override
    public PeerComponent createPreviewPeer() {
        if (video == null) {
            return null;
        }
        return PeerComponent.create(video);
    }

    @Override
    public void takePhoto(PhotoCaptureOptions opts, AsyncResource<CapturedPhoto> result) {
        if (video == null) {
            result.error(new IllegalStateException("Camera not opened"));
            return;
        }
        final PhotoCaptureOptions o = opts == null ? new PhotoCaptureOptions() : opts;
        try {
            CapturedPhoto cp = grab(o.getWidth(), o.getHeight(), o.getJpegQuality(), o.getFilePath());
            final CapturedPhoto fcp = cp;
            final AsyncResource<CapturedPhoto> r = result;
            Display.getInstance().callSerially(new Runnable() {
                @Override public void run() { r.complete(fcp); }
            });
        } catch (Throwable t) {
            final Throwable e = t;
            final AsyncResource<CapturedPhoto> r = result;
            Display.getInstance().callSerially(new Runnable() {
                @Override public void run() { r.error(e); }
            });
        }
    }

    /// Grab a single JPEG still from the live video on the main thread. Returns a
    /// CapturedPhoto (and optionally persists it to {@code filePath}).
    private CapturedPhoto grab(int reqW, int reqH, int quality, String filePath) throws IOException {
        String packed = nativeCameraGrab(video, reqW, reqH,
                (quality > 0 ? quality : 90) / 100.0);
        if (packed == null) {
            throw new IOException("Failed to capture camera frame");
        }
        int c1 = packed.indexOf(',');
        int c2 = packed.indexOf(',', c1 + 1);
        if (c1 < 0 || c2 < 0) {
            throw new IOException("Malformed camera frame response");
        }
        int w = parseInt(packed.substring(0, c1), DEFAULT_W);
        int h = parseInt(packed.substring(c1 + 1, c2), DEFAULT_H);
        byte[] jpeg = Base64.decode(packed.substring(c2 + 1).getBytes());
        String path = filePath != null ? filePath : tempPath();
        writeBytes(path, jpeg);
        return new CapturedPhoto(jpeg, path, w, h);
    }

    @Override
    public void startVideoRecording(String filePath, boolean audio) throws IOException {
        // Browser video recording would route through MediaRecorder -> Blob ->
        // ArrayBuffer -> byte[] -> file. Out of scope for v1.
        throw new IOException("Video recording not implemented on the JavaScript port. "
                + "Use com.codename1.capture.Capture.captureVideo() instead.");
    }

    @Override
    public void stopVideoRecording(AsyncResource<String> result) {
        if (result != null) {
            result.error(new IllegalStateException("Recording not active"));
        }
    }

    @Override
    public void setFrameListener(FrameListener listener, FrameFormat format, int maxFps) {
        this.frameListener = listener;
        this.frameFormat = format == null ? FrameFormat.JPEG : format;
        if (maxFps > 0) {
            this.frameMaxFps = Math.max(1, Math.min(15, maxFps));
        }

        if (listener == null) {
            if (frameTimerHandle != 0) {
                Window.clearInterval(frameTimerHandle);
                frameTimerHandle = 0;
            }
            return;
        }
        if (frameTimerHandle != 0) {
            Window.clearInterval(frameTimerHandle);
        }
        int periodMs = Math.max(67, 1000 / frameMaxFps);
        frameTimerHandle = Window.setInterval(new TimerHandler() {
            @Override public void onTimer() { deliverFrame(); }
        }, periodMs);
    }

    private void deliverFrame() {
        if (closed || video == null) {
            return;
        }
        FrameListener l = frameListener;
        if (l == null) {
            return;
        }
        if (!listenerBusy.compareAndSet(false, true)) {
            return;
        }
        try {
            CapturedPhoto cp = grab(0, 0, 80, null);
            l.onFrame(new CameraFrame(cp.getJpegBytes(), null, cp.getWidth(), cp.getHeight(), 0,
                    System.nanoTime(), frameFormat));
        } catch (Throwable t) {
            Log.e(t);
        } finally {
            listenerBusy.set(false);
        }
    }

    @Override public void setFlashMode(FlashMode mode) { /* MediaStreamTrack constraints; deferred */ }
    @Override public void setZoom(float ratio) { /* MediaStreamTrack constraints; deferred */ }
    @Override public void focus(float xNorm, float yNorm) { /* no-op in browser */ }

    @Override
    public void pause() {
        if (video != null) {
            try { video.pause(); } catch (Throwable ignored) { }
        }
    }

    @Override
    public void resume() {
        if (video != null) {
            try { video.play(); } catch (Throwable ignored) { }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (frameTimerHandle != 0) {
            Window.clearInterval(frameTimerHandle);
            frameTimerHandle = 0;
        }
        if (video != null) {
            try { nativeCameraClose(video); } catch (Throwable ignored) { }
            video = null;
        }
        frameListener = null;
    }

    private static int parseInt(String s, int dflt) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Throwable t) {
            return dflt;
        }
    }

    private static String tempPath() {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String dir = fs.getAppHomePath();
        if (!dir.endsWith("/")) {
            dir += "/";
        }
        return dir + "cn1_photo_" + System.currentTimeMillis() + ".jpg";
    }

    private static void writeBytes(String path, byte[] data) throws IOException {
        OutputStream os = null;
        try {
            os = FileSystemStorage.getInstance().openOutputStream(path);
            os.write(data);
        } finally {
            if (os != null) {
                try { os.close(); } catch (IOException ignored) { }
            }
        }
    }

    // --------------------------------------------------------------------
    // JS bindings. Each is overridden in port.js on the worker port to route to
    // the main thread (the @JSBody body is the TeaVM main-thread fallback).
    // --------------------------------------------------------------------

    @JSBody(params = {},
            script = "return !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);")
    private static native boolean nativeCameraSupported();

    // Opens getUserMedia + builds the <video> on the main thread and returns it
    // (or null on deny/timeout -- the error name is in nativeCameraLastError()).
    // The TeaVM @JSBody fallback can't await getUserMedia synchronously.
    @JSBody(params = {"facing", "audio"}, script = "return null;")
    private static native HTMLVideoElement nativeCameraOpen(String facing, boolean audio);

    @JSBody(params = {}, script = "return (self.__cn1_camera_error||'');")
    private static native String nativeCameraLastError();

    // Returns "w,h,<base64-jpeg>" for a single frame grabbed from the video.
    @JSBody(params = {"video", "w", "h", "quality"}, script = "return null;")
    private static native String nativeCameraGrab(HTMLVideoElement video, int w, int h, double quality);

    @JSBody(params = {"video"},
            script = "try{var s=video&&video.srcObject; if(s){var t=s.getTracks();for(var i=0;i<t.length;i++)t[i].stop();} if(video){video.pause(); if(video.parentNode)video.parentNode.removeChild(video); video.srcObject=null;}}catch(e){}")
    private static native void nativeCameraClose(HTMLVideoElement video);
}
