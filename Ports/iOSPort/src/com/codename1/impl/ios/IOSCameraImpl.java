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
package com.codename1.impl.ios;

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
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.geom.Dimension;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/// iOS implementation of `CameraImpl`. Bridges to `CN1Camera.{h,m}` which
/// wraps `AVCaptureSession` (preview + frame stream + still + video).
///
/// @hidden
public class IOSCameraImpl extends CameraImpl {

    // One Java instance is the active camera at any moment; native callbacks
    // arrive from the AVFoundation queue without context, so we route them
    // through this static.
    private static volatile IOSCameraImpl ACTIVE;

    // Outstanding async callbacks (photo capture, video stop) keyed by an
    // integer id we pass to native.
    private static final Map<Integer, AsyncResource<CapturedPhoto>> PHOTO_CBS =
            new HashMap<Integer, AsyncResource<CapturedPhoto>>();
    private static final Map<Integer, AsyncResource<String>> VIDEO_CBS =
            new HashMap<Integer, AsyncResource<String>>();
    private static final AtomicInteger NEXT_CB = new AtomicInteger(1);

    private long sessionPeer;
    private CameraInfo info;
    private CameraSessionOptions options;
    private final AtomicBoolean listenerBusy = new AtomicBoolean();
    private volatile FrameListener frameListener;
    private volatile FrameFormat frameFormat = FrameFormat.JPEG;

    @Override
    public CameraInfo[] enumerateCameras() {
        String packed = IOSImplementation.nativeInstance.cn1CameraEnumerate();
        if (packed == null || packed.isEmpty()) return new CameraInfo[0];
        // packed: "id|facing|hasFlash|hasAutoFocus;id|facing|..."
        String[] entries = packed.split(";");
        CameraInfo[] out = new CameraInfo[entries.length];
        for (int i = 0; i < entries.length; i++) {
            String[] f = entries[i].split("\\|");
            if (f.length < 4) continue;
            CameraFacing facing = "front".equals(f[1]) ? CameraFacing.FRONT
                                : "back".equals(f[1])  ? CameraFacing.BACK
                                : CameraFacing.EXTERNAL;
            out[i] = new CameraInfo(f[0], facing,
                    new Dimension[0], new Dimension[0],
                    "1".equals(f[2]), "1".equals(f[3]));
        }
        return out;
    }

    @Override
    public void open(String cameraId, CameraSessionOptions opts) throws IOException {
        this.options = opts == null ? new CameraSessionOptions() : opts;
        if ("__permission_probe__".equals(cameraId)) {
            // iOS handles camera permission inline at session-run time. We
            // can't synchronously probe; assume granted and let the actual
            // open fail later if the user denies.
            this.info = new CameraInfo(cameraId, CameraFacing.BACK,
                    new Dimension[0], new Dimension[0], false, false);
            return;
        }
        long peer = IOSImplementation.nativeInstance.cn1CameraOpen(
                cameraId == null ? "" : cameraId,
                options.getPreviewWidth(),
                options.getPreviewHeight(),
                options.isCaptureAudio());
        if (peer == 0) {
            throw new IOException("Could not open iOS camera (id=" + cameraId + ")");
        }
        this.sessionPeer = peer;
        this.info = new CameraInfo(cameraId, CameraFacing.BACK,
                new Dimension[0], new Dimension[0], false, true);
        ACTIVE = this;
    }

    @Override
    public PeerComponent createPreviewPeer() {
        if (sessionPeer == 0) return null;
        long viewPeer = IOSImplementation.nativeInstance.cn1CameraCreatePreviewView(sessionPeer);
        if (viewPeer == 0) return null;
        return IOSImplementation.instance.createNativePeer(new long[] { viewPeer });
    }

    @Override
    public void takePhoto(PhotoCaptureOptions opts, AsyncResource<CapturedPhoto> result) {
        if (sessionPeer == 0) {
            result.error(new IllegalStateException("Camera session not open"));
            return;
        }
        if (opts == null) opts = new PhotoCaptureOptions();
        int cbId = NEXT_CB.getAndIncrement();
        synchronized (PHOTO_CBS) {
            PHOTO_CBS.put(cbId, result);
        }
        IOSImplementation.nativeInstance.cn1CameraTakePhoto(
                sessionPeer, opts.getWidth(), opts.getHeight(),
                opts.getJpegQuality(),
                opts.getFilePath() == null ? "" : opts.getFilePath(),
                cbId);
    }

    @Override
    public void startVideoRecording(String filePath, boolean audio) throws IOException {
        if (sessionPeer == 0) {
            throw new IOException("Camera session not open");
        }
        if (!IOSImplementation.nativeInstance.cn1CameraStartVideo(sessionPeer, filePath, audio)) {
            throw new IOException("Could not start video recording");
        }
    }

    @Override
    public void stopVideoRecording(AsyncResource<String> result) {
        if (sessionPeer == 0) {
            if (result != null) result.error(new IllegalStateException("Camera session not open"));
            return;
        }
        int cbId = result == null ? 0 : NEXT_CB.getAndIncrement();
        if (result != null) {
            synchronized (VIDEO_CBS) {
                VIDEO_CBS.put(cbId, result);
            }
        }
        IOSImplementation.nativeInstance.cn1CameraStopVideo(sessionPeer, cbId);
    }

    @Override
    public void setFrameListener(FrameListener listener, FrameFormat format, int maxFps) {
        this.frameListener = listener;
        this.frameFormat = format == null ? FrameFormat.JPEG : format;
        if (sessionPeer != 0) {
            IOSImplementation.nativeInstance.cn1CameraSetFrameDelivery(
                    sessionPeer, listener != null, Math.max(1, maxFps));
        }
    }

    @Override
    public void setFlashMode(FlashMode mode) {
        if (sessionPeer == 0) return;
        int code = mode == null ? 0
                : mode == FlashMode.OFF ? 0
                : mode == FlashMode.ON ? 1
                : mode == FlashMode.AUTO ? 2 : 3;
        IOSImplementation.nativeInstance.cn1CameraSetFlash(sessionPeer, code);
    }

    @Override
    public void setZoom(float ratio) {
        if (sessionPeer == 0) return;
        IOSImplementation.nativeInstance.cn1CameraSetZoom(sessionPeer, ratio);
    }

    @Override
    public void focus(float xNorm, float yNorm) {
        if (sessionPeer == 0) return;
        IOSImplementation.nativeInstance.cn1CameraFocus(sessionPeer, xNorm, yNorm);
    }

    @Override
    public void pause() {
        if (sessionPeer != 0) IOSImplementation.nativeInstance.cn1CameraPause(sessionPeer);
    }

    @Override
    public void resume() {
        if (sessionPeer != 0) IOSImplementation.nativeInstance.cn1CameraResume(sessionPeer);
    }

    @Override
    public void close() {
        long s = sessionPeer;
        sessionPeer = 0;
        frameListener = null;
        if (ACTIVE == this) ACTIVE = null;
        if (s != 0) IOSImplementation.nativeInstance.cn1CameraClose(s);
    }

    // ---------------------------------------------------------------------
    // Native -> Java callbacks. Called from CN1Camera.m on the AVFoundation
    // delivery queue (frames) or the main queue (photo/video completion).
    // ---------------------------------------------------------------------

    /// Called from native code with a single video frame.
    public static void onFrameDelivered(byte[] jpegBytes, int width, int height,
                                        int rotationDegrees, long timestampNanos) {
        IOSCameraImpl self = ACTIVE;
        if (self == null) return;
        FrameListener l = self.frameListener;
        if (l == null) return;
        if (!self.listenerBusy.compareAndSet(false, true)) {
            return; // drop frame; previous still in flight
        }
        try {
            CameraFrame f = new CameraFrame(jpegBytes, null, width, height,
                    rotationDegrees, timestampNanos, self.frameFormat);
            l.onFrame(f);
        } catch (Throwable t) {
            Log.e(t);
        } finally {
            self.listenerBusy.set(false);
        }
    }

    /// Called from native code when a still photo capture completes successfully.
    public static void onPhotoCaptured(int callbackId, byte[] jpeg,
                                       String filePath, int width, int height) {
        final AsyncResource<CapturedPhoto> cb;
        synchronized (PHOTO_CBS) {
            cb = PHOTO_CBS.remove(callbackId);
        }
        if (cb == null) return;
        final CapturedPhoto p = new CapturedPhoto(jpeg, filePath, width, height);
        Display.getInstance().callSerially(new Runnable() {
            @Override public void run() { cb.complete(p); }
        });
    }

    /// Called from native code when a still photo capture fails.
    public static void onPhotoFailed(int callbackId, String error) {
        final AsyncResource<CapturedPhoto> cb;
        synchronized (PHOTO_CBS) {
            cb = PHOTO_CBS.remove(callbackId);
        }
        if (cb == null) return;
        final String msg = error == null ? "iOS camera photo capture failed" : error;
        Display.getInstance().callSerially(new Runnable() {
            @Override public void run() { cb.error(new IOException(msg)); }
        });
    }

    /// Called from native code when video recording stops and the file is finalized.
    public static void onVideoStopped(int callbackId, String filePath) {
        if (callbackId == 0) return;
        final AsyncResource<String> cb;
        synchronized (VIDEO_CBS) {
            cb = VIDEO_CBS.remove(callbackId);
        }
        if (cb == null) return;
        final String path = filePath;
        Display.getInstance().callSerially(new Runnable() {
            @Override public void run() { cb.complete(path); }
        });
    }
}
