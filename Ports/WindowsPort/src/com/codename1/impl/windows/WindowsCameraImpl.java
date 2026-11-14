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
package com.codename1.impl.windows;

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
import com.codename1.ui.Image;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.util.UITimer;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/// Windows implementation of the `com.codename1.camera` `CameraImpl` (the "device
/// camera API"), backed by Media Foundation (cn1_windows_camera.cpp). A capture
/// session runs a source-reader loop on a worker thread keeping the latest frame;
/// the Java side polls it for the preview and for stills -- a real webcam frame,
/// never synthetic.
///
/// The preview is an image-based `PeerComponent` (the same approach as the
/// WebView2 browser peer): it repaints the latest captured frame, so it renders in
/// the offscreen screenshot pipeline as well as in a live window, without needing a
/// composited child surface.
///
/// Zoom and focus go through the device's UVC camera controls (`IAMCameraControl`);
/// a webcam without them ignores the call. UVC has no focus point, so `focus`
/// triggers the device's auto focus wherever the tap was. Video is H.264 MP4
/// written by a Media Foundation sink writer from the frames the session already
/// reads, with AAC audio from the default microphone when asked for. Webcams have
/// no torch or flash Media Foundation can drive, so `setFlashMode` does nothing.
public class WindowsCameraImpl extends CameraImpl {
    private long session;
    // Serializes finalizing a recording with tearing the session down: both end
    // in cn1CameraStopRecording, and close() frees the native session.
    private final Object nativeLock = new Object();
    // What finalizing the recording answered when close() did it before a
    // pending stopVideoRecording got there; that stop reports it.
    private boolean finalizedOnClose;
    private CameraSessionOptions options;
    private volatile FrameListener frameListener;
    private volatile FrameFormat frameFormat = FrameFormat.JPEG;
    private volatile int frameMaxFps = 15;
    private final AtomicBoolean listenerBusy = new AtomicBoolean();
    private String recordingPath;

    @Override
    public CameraInfo[] enumerateCameras() {
        String packed = WindowsNative.cameraEnumerate();
        if (packed == null || packed.length() == 0) {
            return new CameraInfo[0];
        }
        java.util.List<String> entries = splitChar(packed, ';');
        java.util.List<CameraInfo> out = new java.util.ArrayList<CameraInfo>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).length() == 0) {
                continue;
            }
            // "name|external|flash|autofocus". A desktop webcam has no API-reported
            // facing and no flash; auto focus comes from the device's focus control.
            // The id is the device index the session opens by.
            java.util.List<String> fields = splitChar(entries.get(i), '|');
            boolean autoFocus = fields.size() > 3 && "1".equals(fields.get(3));
            out.add(new CameraInfo(String.valueOf(out.size()), CameraFacing.EXTERNAL,
                    new Dimension[0], new Dimension[0], false, autoFocus));
        }
        return out.toArray(new CameraInfo[out.size()]);
    }

    private static java.util.List<String> splitChar(String s, char sep) {
        // String.split is unavailable on the clean (ParparVM) target; walk manually.
        java.util.List<String> out = new java.util.ArrayList<String>();
        int start = 0;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) == sep) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out;
    }

    @Override
    public void open(String cameraId, CameraSessionOptions opts) throws IOException {
        this.options = opts == null ? new CameraSessionOptions() : opts;
        if ("__permission_probe__".equals(cameraId)) {
            // Desktop camera access has no separate permission gate; the actual
            // open below fails honestly if no device is present.
            return;
        }
        int index = 0;
        if (cameraId != null && cameraId.length() > 0) {
            try {
                index = Integer.parseInt(cameraId);
            } catch (NumberFormatException ignore) {
                index = 0;
            }
        }
        long s = WindowsNative.cameraSessionStart(index, options.getPreviewWidth(), options.getPreviewHeight());
        if (s == 0) {
            throw new IOException("No Windows camera available (id=" + cameraId + ")");
        }
        this.session = s;
    }

    @Override
    public PeerComponent createPreviewPeer() {
        if (session == 0) {
            return null;
        }
        return new WindowsCameraPreview();
    }

    @Override
    public void takePhoto(final PhotoCaptureOptions opts, final AsyncResource<CapturedPhoto> result) {
        if (session == 0) {
            result.error(new IllegalStateException("Camera session not open"));
            return;
        }
        final long s = session;
        final PhotoCaptureOptions o = opts == null ? new PhotoCaptureOptions() : opts;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Grab the freshest captured frame; wait briefly for the first.
                    int[] dims = new int[2];
                    int[] argb = null;
                    for (int i = 0; i < 40 && argb == null; i++) {
                        argb = WindowsNative.cameraSessionLatestFrame(s, dims);
                        if (argb == null) {
                            Thread.sleep(50);
                        }
                    }
                    if (argb == null || dims[0] <= 0 || dims[1] <= 0) {
                        complete(result, null, new IOException("No camera frame available"));
                        return;
                    }
                    final int w = dims[0], h = dims[1];
                    byte[] png = WindowsNative.encodeArgbToPng(argb, w, h);
                    String filePath = o.getFilePath();
                    if (filePath != null && filePath.length() > 0 && png != null) {
                        OutputStream os = WindowsImplementation.getInstance().openOutputStream(filePath);
                        os.write(png);
                        os.close();
                    }
                    complete(result, new CapturedPhoto(png, filePath, w, h), null);
                } catch (Throwable err) {
                    complete(result, null, err);
                }
            }
        }, "cn1-windows-takephoto");
        // NB: no setDaemon -- java.lang.Thread.setDaemon is not in the clean
        // (ParparVM) runtime; the worker exits as soon as the frame is captured.
        t.start();
    }

    private static void complete(final AsyncResource<CapturedPhoto> result,
            final CapturedPhoto photo, final Throwable err) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (err != null) {
                    result.error(err instanceof Exception ? (Exception) err : new IOException(String.valueOf(err)));
                } else {
                    result.complete(photo);
                }
            }
        });
    }

    @Override
    public void setFrameListener(FrameListener listener, FrameFormat format, int maxFps) {
        this.frameListener = listener;
        this.frameFormat = format == null ? FrameFormat.JPEG : format;
        this.frameMaxFps = Math.max(1, maxFps);
    }

    /// Polls the latest frame and delivers it to the active FrameListener; driven
    /// by the preview peer's timer so it shares the session's frame cadence.
    private void deliverFrameIfListening() {
        FrameListener l = frameListener;
        if (l == null || session == 0) {
            return;
        }
        if (!listenerBusy.compareAndSet(false, true)) {
            return;
        }
        try {
            int[] dims = new int[2];
            int[] argb = WindowsNative.cameraSessionLatestFrame(session, dims);
            if (argb == null || dims[0] <= 0 || dims[1] <= 0) {
                return;
            }
            int w = dims[0], h = dims[1];
            CameraFrame frame;
            if (frameFormat == FrameFormat.RGBA8888) {
                byte[] rgba = new byte[w * h * 4];
                for (int i = 0; i < argb.length; i++) {
                    int p = argb[i];
                    rgba[i * 4] = (byte) (p >> 16);
                    rgba[i * 4 + 1] = (byte) (p >> 8);
                    rgba[i * 4 + 2] = (byte) p;
                    rgba[i * 4 + 3] = (byte) (p >>> 24);
                }
                frame = new CameraFrame(null, rgba, w, h, 0, System.currentTimeMillis() * 1000000L, FrameFormat.RGBA8888);
            } else {
                byte[] png = WindowsNative.encodeArgbToPng(argb, w, h);
                frame = new CameraFrame(png, null, w, h, 0, System.currentTimeMillis() * 1000000L, FrameFormat.JPEG);
            }
            l.onFrame(frame);
        } catch (Throwable t) {
            Log.e(t);
        } finally {
            listenerBusy.set(false);
        }
    }

    // ----- video, zoom, focus, flash ---------------------------------------------

    @Override
    public void startVideoRecording(String filePath, boolean audio) throws IOException {
        if (session == 0) {
            throw new IOException("Camera session not open");
        }
        if (filePath == null || filePath.length() == 0) {
            throw new IOException("No file path for the video recording");
        }
        // The path is a FileSystemStorage path (possibly a file: URL); the sink
        // writer wants the plain Windows path.
        String nativePath = WindowsImplementation.getInstance().toNativePath(filePath);
        int status = WindowsNative.cameraSessionStartRecording(session, nativePath, audio);
        if (status < 0) {
            if (status == -1) {
                throw new IOException("A video recording is already running");
            }
            if (status == -2) {
                throw new IOException("The camera has not delivered a frame yet");
            }
            throw new IOException("Could not start the H.264 MP4 writer for " + filePath);
        }
        if (status == 1) {
            Log.p("Windows camera: no audio capture device available, recording video only");
        }
        recordingPath = filePath;
    }

    @Override
    public void stopVideoRecording(final AsyncResource<String> result) {
        final String path = recordingPath;
        recordingPath = null;
        final long s = session;
        if (path == null || s == 0) {
            if (result != null) {
                result.error(new IllegalStateException("No video recording is running"));
            }
            return;
        }
        // Finalize drains the encoder and writes the MP4 index, which can take a
        // moment: do it off the EDT and complete the resource back on it.
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean ok;
                synchronized (nativeLock) {
                    // close() may have freed the session meanwhile; it finalized
                    // the recording first and left the outcome here.
                    ok = session == s ? WindowsNative.cameraSessionStopRecording(s) : finalizedOnClose;
                }
                if (result == null) {
                    return;
                }
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        if (ok) {
                            result.complete(path);
                        } else {
                            result.error(new IOException("The video recording could not be finalized: " + path));
                        }
                    }
                });
            }
        }, "cn1-windows-stopvideo");
        t.start();
    }

    @Override
    public void setFlashMode(FlashMode mode) {
        // Webcams have no flash, and Media Foundation exposes no torch or LED
        // control for UVC devices (the capture LED is the privacy indicator,
        // driven by the device itself), so there is nothing to set.
    }

    @Override
    public void setZoom(float ratio) {
        if (session != 0) {
            WindowsNative.cameraSessionSetZoom(session, ratio);
        }
    }

    @Override
    public void focus(float xNorm, float yNorm) {
        // UVC has no focus point: the coordinate is ignored and the device's
        // auto focus is triggered instead.
        if (session != 0) {
            WindowsNative.cameraSessionFocus(session);
        }
    }

    @Override
    public void pause() {
        if (session != 0) {
            WindowsNative.cameraSessionSetPaused(session, true);
        }
    }

    @Override
    public void resume() {
        if (session != 0) {
            WindowsNative.cameraSessionSetPaused(session, false);
        }
    }

    @Override
    public void close() {
        frameListener = null;
        recordingPath = null;
        synchronized (nativeLock) {
            long s = session;
            session = 0;
            if (s != 0) {
                // Finalize a running recording here, so a stop that is still on
                // its way reports a file that is really complete.
                finalizedOnClose = WindowsNative.cameraSessionStopRecording(s);
                WindowsNative.cameraSessionStop(s);
            }
        }
    }

    /// Image-based preview peer: repaints the latest captured frame (and drives the
    /// optional frame listener) on a UITimer, mirroring the WebView2 peer so it
    /// renders both in a live window and in the offscreen screenshot pipeline.
    private final class WindowsCameraPreview extends PeerComponent {
        private UITimer poller;

        WindowsCameraPreview() {
            super(null);
        }

        @Override
        protected boolean shouldRenderPeerImage() {
            return true;
        }

        @Override
        protected Dimension calcPreferredSize() {
            int w = options != null ? options.getPreviewWidth() : 0;
            int h = options != null ? options.getPreviewHeight() : 0;
            if (w <= 0 || h <= 0) {
                Display d = Display.getInstance();
                return new Dimension(d.getDisplayWidth(), d.getDisplayHeight() / 2);
            }
            return new Dimension(w, h);
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            // The top level rather than the form: getComponentForm() is null by design
            // inside a Window, so this timer never started for a peer hosted in one.
            com.codename1.ui.TopLevelContainer peerTop = getTopLevelContainer();
            if (poller == null && peerTop != null) {
                int fps = Math.max(1, frameMaxFps);
                int periodMs = Math.max(33, 1000 / fps);
                poller = UITimer.timer(periodMs, true, peerTop, new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                        deliverFrameIfListening();
                    }
                });
            }
        }

        @Override
        protected void deinitialize() {
            if (poller != null) {
                poller.cancel();
                poller = null;
            }
            super.deinitialize();
        }

        private void refresh() {
            if (session == 0) {
                return;
            }
            int[] dims = new int[2];
            int[] argb = WindowsNative.cameraSessionLatestFrame(session, dims);
            if (argb == null || dims[0] <= 0 || dims[1] <= 0) {
                return;
            }
            try {
                Image img = Image.createImage(argb, dims[0], dims[1]);
                if (img != null) {
                    setPeerImage(img);
                    repaint();
                }
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }
}
