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
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;
import com.codename1.html5.js.browser.TimerHandler;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.canvas.CanvasImageSource;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;
import com.codename1.html5.js.dom.HTMLCanvasElement;
import com.codename1.html5.js.dom.HTMLVideoElement;
import com.codename1.impl.CameraImpl;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.geom.Dimension;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/// JavaScript / browser implementation of `CameraImpl` backed by the
/// MediaDevices `getUserMedia` API. Preview is an HTMLVideoElement wrapped
/// as a Codename One peer; frames and stills come from
/// `canvas.toDataURL('image/jpeg')`.
///
/// **Browser caveats**: getUserMedia requires a secure context (HTTPS or
/// localhost). On iOS Safari it also requires a user gesture - the first
/// call to `Camera.open(...)` should originate from a tap, not from the
/// app's `start()`.
///
/// Video recording is not implemented in v1 (it would route through the
/// `MediaRecorder` API and emit `.webm`); callers needing video should fall
/// back to the legacy `com.codename1.capture.Capture#captureVideo`.
///
/// @hidden
public class HTML5CameraImpl extends CameraImpl {

    private static final int DEFAULT_W = 640;
    private static final int DEFAULT_H = 480;

    private interface MediaStream extends JSObject { }
    private interface HTMLVideoElementEx extends HTMLVideoElement {
        @JSProperty void setSrcObject(MediaStream stream);
    }

    @JSFunctor private interface UserMediaSuccess extends JSObject {
        void onStream(JSObject stream);
    }
    @JSFunctor private interface UserMediaError extends JSObject {
        void onError(JSObject error);
    }

    private MediaStream stream;
    private HTMLVideoElement video;
    private HTMLCanvasElement scratchCanvas;
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

        if ("__permission_probe__".equals(cameraId)) return;
        if (!supportsMediaDevices()) {
            throw new IOException("navigator.mediaDevices.getUserMedia is not available");
        }

        final Object lock = new Object();
        final JSObject[] err = new JSObject[1];
        final JSObject[] streamOut = new JSObject[1];

        getUserMediaPromise(isFrontFacing ? "user" : "environment",
                options.isCaptureAudio(),
                new UserMediaSuccess() {
                    @Override public void onStream(JSObject s) {
                        streamOut[0] = s;
                        synchronized (lock) { lock.notifyAll(); }
                    }
                },
                new UserMediaError() {
                    @Override public void onError(JSObject e) {
                        err[0] = e;
                        synchronized (lock) { lock.notifyAll(); }
                    }
                });

        long deadline = System.currentTimeMillis() + 8000;
        synchronized (lock) {
            while (streamOut[0] == null && err[0] == null
                    && System.currentTimeMillis() < deadline) {
                try { lock.wait(200); } catch (InterruptedException ignored) { }
            }
        }
        if (err[0] != null) {
            throw new IOException("getUserMedia failed: " + getErrorName(err[0]));
        }
        if (streamOut[0] == null) {
            throw new IOException("getUserMedia timed out");
        }
        this.stream = (MediaStream) streamOut[0];
        this.video = (HTMLVideoElement) Window.current().getDocument().createElement("video");
        this.video.setAutoplay(true);
        this.video.setMuted(true);
        this.video.setAttribute("playsinline", "");
        ((HTMLVideoElementEx) this.video).setSrcObject(this.stream);
    }

    @Override
    public PeerComponent createPreviewPeer() {
        if (video == null) return null;
        return PeerComponent.create(video);
    }

    @Override
    public void takePhoto(PhotoCaptureOptions opts, AsyncResource<CapturedPhoto> result) {
        if (video == null) {
            result.error(new IllegalStateException("Camera not opened"));
            return;
        }
        if (opts == null) opts = new PhotoCaptureOptions();
        try {
            int w = opts.getWidth() > 0 ? opts.getWidth() : videoWidth(video);
            int h = opts.getHeight() > 0 ? opts.getHeight() : videoHeight(video);
            if (w <= 0) w = DEFAULT_W;
            if (h <= 0) h = DEFAULT_H;
            HTMLCanvasElement canvas = (HTMLCanvasElement)
                    Window.current().getDocument().createElement("canvas");
            canvas.setAttribute("width", String.valueOf(w));
            canvas.setAttribute("height", String.valueOf(h));
            CanvasRenderingContext2D ctx = (CanvasRenderingContext2D) canvas.getContext("2d");
            ctx.drawImage((CanvasImageSource) video, 0, 0, w, h);
            String dataUrl = canvasToJpegDataUrl(canvas, opts.getJpegQuality() / 100.0);
            byte[] jpeg = decodeBase64DataUrl(dataUrl);
            String path = opts.getFilePath() != null ? opts.getFilePath() : tempPath();
            writeBytes(path, jpeg);
            final CapturedPhoto cp = new CapturedPhoto(jpeg, path, w, h);
            final AsyncResource<CapturedPhoto> r = result;
            Display.getInstance().callSerially(new Runnable() {
                @Override public void run() { r.complete(cp); }
            });
        } catch (Throwable t) {
            final Throwable e = t;
            final AsyncResource<CapturedPhoto> r = result;
            Display.getInstance().callSerially(new Runnable() {
                @Override public void run() { r.error(e); }
            });
        }
    }

    @Override
    public void startVideoRecording(String filePath, boolean audio) throws IOException {
        // Browser video recording would route through MediaRecorder ->
        // Blob -> ArrayBuffer -> byte[] -> file. Out of scope for v1.
        throw new IOException("Video recording not implemented on the JavaScript port. "
                + "Use com.codename1.capture.Capture.captureVideo() instead.");
    }

    @Override
    public void stopVideoRecording(AsyncResource<String> result) {
        if (result != null) result.error(new IllegalStateException("Recording not active"));
    }

    @Override
    public void setFrameListener(FrameListener listener, FrameFormat format, int maxFps) {
        this.frameListener = listener;
        this.frameFormat = format == null ? FrameFormat.JPEG : format;
        if (maxFps > 0) this.frameMaxFps = Math.max(1, Math.min(15, maxFps));

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
        if (closed || video == null) return;
        FrameListener l = frameListener;
        if (l == null) return;
        if (!listenerBusy.compareAndSet(false, true)) return;
        try {
            int w = videoWidth(video);
            int h = videoHeight(video);
            if (w <= 0 || h <= 0) return;
            if (scratchCanvas == null) {
                scratchCanvas = (HTMLCanvasElement)
                        Window.current().getDocument().createElement("canvas");
            }
            scratchCanvas.setAttribute("width", String.valueOf(w));
            scratchCanvas.setAttribute("height", String.valueOf(h));
            CanvasRenderingContext2D ctx = (CanvasRenderingContext2D)
                    scratchCanvas.getContext("2d");
            ctx.drawImage((CanvasImageSource) video, 0, 0, w, h);
            String dataUrl = canvasToJpegDataUrl(scratchCanvas, 0.8);
            byte[] jpeg = decodeBase64DataUrl(dataUrl);
            l.onFrame(new CameraFrame(jpeg, null, w, h, 0,
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
        if (video != null) try { video.pause(); } catch (Throwable ignored) { }
    }

    @Override
    public void resume() {
        if (video != null) try { video.play(); } catch (Throwable ignored) { }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (frameTimerHandle != 0) {
            Window.clearInterval(frameTimerHandle);
            frameTimerHandle = 0;
        }
        if (stream != null) {
            try { stopMediaStream(stream); } catch (Throwable ignored) { }
            stream = null;
        }
        if (video != null) {
            try { video.pause(); } catch (Throwable ignored) { }
            video = null;
        }
        scratchCanvas = null;
        frameListener = null;
    }

    // --------------------------------------------------------------------
    // JS bindings
    // --------------------------------------------------------------------

    @JSBody(params = {},
            script = "return !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);")
    private static native boolean supportsMediaDevices();

    @JSBody(params = {"facing", "audio", "onSuccess", "onError"},
            script = "var c={video:{facingMode:facing},audio:!!audio};"
                   + "navigator.mediaDevices.getUserMedia(c).then(function(s){onSuccess(s);})"
                   + ".catch(function(e){onError(e);});")
    private static native void getUserMediaPromise(String facing, boolean audio,
                                                   UserMediaSuccess onSuccess,
                                                   UserMediaError onError);

    @JSBody(params = {"e"}, script = "return e && e.name ? e.name : ('' + e);")
    private static native String getErrorName(JSObject e);

    @JSBody(params = {"v"}, script = "return v.videoWidth || 0;")
    private static native int videoWidth(HTMLVideoElement v);

    @JSBody(params = {"v"}, script = "return v.videoHeight || 0;")
    private static native int videoHeight(HTMLVideoElement v);

    @JSBody(params = {"canvas", "quality"},
            script = "return canvas.toDataURL('image/jpeg', quality);")
    private static native String canvasToJpegDataUrl(HTMLCanvasElement canvas, double quality);

    @JSBody(params = {"stream"},
            script = "var t = stream.getTracks(); for (var i=0;i<t.length;i++) t[i].stop();")
    private static native void stopMediaStream(MediaStream stream);

    private static byte[] decodeBase64DataUrl(String dataUrl) {
        int comma = dataUrl.indexOf(',');
        String b64 = comma >= 0 ? dataUrl.substring(comma + 1) : dataUrl;
        return java.util.Base64.getDecoder().decode(b64);
    }

    private static String tempPath() {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String dir = fs.getAppHomePath();
        if (!dir.endsWith("/")) dir += "/";
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
}
