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
package com.codename1.impl.linux;

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

/// Linux implementation of the `com.codename1.camera` `CameraImpl` (the "device
/// camera API"), backed by GStreamer (v4l2src -> appsink, in cn1_linux_media.c,
/// loaded with dlopen). A capture session runs a worker thread keeping the latest
/// frame; the Java side polls it for the preview and for stills -- a real webcam
/// frame, never synthetic.
///
/// The preview is an image-based `PeerComponent` (the same approach as the
/// WebView2 browser peer): it repaints the latest captured frame, so it renders in
/// the offscreen screenshot pipeline as well as in a live window, without needing a
/// composited child surface. Zoom and focus are V4L2 controls set on the device
/// node, and quietly do nothing on a webcam without them. Video recording tees
/// the capture into a GStreamer encoder and muxer chosen from what is installed.
/// V4L2 has no flash control, so flash is a no-op.
public class LinuxCameraImpl extends CameraImpl {
    private volatile long session;
    private CameraSessionOptions options;
    private volatile FrameListener frameListener;
    private volatile FrameFormat frameFormat = FrameFormat.JPEG;
    private volatile int frameMaxFps = 15;
    private final AtomicBoolean listenerBusy = new AtomicBoolean();
    private String recordingPath;
    // Held while a recording finalizes, so close() cannot free the native session
    // under the stop worker.
    private final Object nativeLock = new Object();

    @Override
    public CameraInfo[] enumerateCameras() {
        String packed = LinuxNative.cameraEnumerate();
        if (packed == null || packed.length() == 0) {
            return new CameraInfo[0];
        }
        java.util.List<String> entries = splitChar(packed, ';');
        java.util.List<CameraInfo> out = new java.util.ArrayList<CameraInfo>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            String entry = entries.get(i);
            if (entry.length() == 0) {
                continue;
            }
            // A USB/built-in desktop webcam has no API-reported facing or flash;
            // the second field says whether V4L2 offers an auto focus control.
            // The id is the device index the session opens by.
            java.util.List<String> fields = splitChar(entry, '|');
            boolean autoFocus = fields.size() > 1 && "af".equals(fields.get(1));
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
        long s = LinuxNative.cameraSessionStart(index, options.getPreviewWidth(), options.getPreviewHeight());
        if (s == 0) {
            throw new IOException("No Linux camera available (id=" + cameraId + ")");
        }
        this.session = s;
    }

    @Override
    public PeerComponent createPreviewPeer() {
        if (session == 0) {
            return null;
        }
        return new LinuxCameraPreview();
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
                        argb = LinuxNative.cameraSessionLatestFrame(s, dims);
                        if (argb == null) {
                            Thread.sleep(50);
                        }
                    }
                    if (argb == null || dims[0] <= 0 || dims[1] <= 0) {
                        complete(result, null, new IOException("No camera frame available"));
                        return;
                    }
                    final int w = dims[0], h = dims[1];
                    byte[] png = LinuxNative.encodeArgbToPng(argb, w, h);
                    String filePath = o.getFilePath();
                    if (filePath != null && filePath.length() > 0 && png != null) {
                        OutputStream os = LinuxImplementation.getInstance().openOutputStream(filePath);
                        os.write(png);
                        os.close();
                    }
                    complete(result, new CapturedPhoto(png, filePath, w, h), null);
                } catch (Throwable err) {
                    complete(result, null, err);
                }
            }
        }, "cn1-linux-takephoto");
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
            int[] argb = LinuxNative.cameraSessionLatestFrame(session, dims);
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
                byte[] png = LinuxNative.encodeArgbToPng(argb, w, h);
                frame = new CameraFrame(png, null, w, h, 0, System.currentTimeMillis() * 1000000L, FrameFormat.JPEG);
            }
            l.onFrame(frame);
        } catch (Throwable t) {
            Log.e(t);
        } finally {
            listenerBusy.set(false);
        }
    }

    // ----- recording, zoom, focus, flash --------------------------------------

    @Override
    public void startVideoRecording(String filePath, boolean audio) throws IOException {
        if (session == 0) {
            throw new IOException("Camera session not open");
        }
        if (filePath == null || filePath.length() == 0) {
            throw new IOException("No file path for the video recording");
        }
        if (recordingPath != null) {
            throw new IOException("A video recording is already in progress");
        }
        // The path is a FileSystemStorage one (possibly a file:// URL); GStreamer's
        // filesink wants the native path.
        String nativePath = LinuxImplementation.getInstance().toNativePath(filePath);
        String err = LinuxNative.cameraSessionStartRecording(session, nativePath, audio);
        if (err != null) {
            throw new IOException(err);
        }
        recordingPath = filePath;
    }

    @Override
    public void stopVideoRecording(final AsyncResource<String> result) {
        final String path = recordingPath;
        final long s = session;
        recordingPath = null;
        if (path == null || s == 0) {
            if (result != null) {
                result.error(new IllegalStateException("No video recording in progress"));
            }
            return;
        }
        // Finalizing waits for the muxer to write its index: off the EDT.
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean ok;
                synchronized (nativeLock) {
                    // close() may have freed the session first.
                    ok = session == s && LinuxNative.cameraSessionStopRecording(s);
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
                            result.error(new IOException("The video recording did not finalize cleanly: " + path));
                        }
                    }
                });
            }
        }, "cn1-linux-stopvideo");
        t.start();
    }

    @Override
    public void setFlashMode(FlashMode mode) {
        // V4L2 defines no flash or torch control, and desktop webcams have no
        // flash, so there is nothing to drive.
    }

    @Override
    public void setZoom(float ratio) {
        // V4L2_CID_ZOOM_ABSOLUTE, clamped natively; a no-op without the control.
        if (session != 0) {
            LinuxNative.cameraSessionSetZoom(session, ratio);
        }
    }

    @Override
    public void focus(float xNorm, float yNorm) {
        // V4L2 has no focus point: a tap triggers auto focus where the device
        // has one, and does nothing where it does not.
        if (session != 0) {
            LinuxNative.cameraSessionFocus(session);
        }
    }

    @Override
    public void pause() {
        if (session != 0) {
            LinuxNative.cameraSessionSetPaused(session, true);
        }
    }

    @Override
    public void resume() {
        if (session != 0) {
            LinuxNative.cameraSessionSetPaused(session, false);
        }
    }

    @Override
    public void close() {
        long s = session;
        session = 0;
        frameListener = null;
        recordingPath = null;
        if (s != 0) {
            synchronized (nativeLock) {
                LinuxNative.cameraSessionStop(s);
            }
        }
    }

    /// Image-based preview peer: repaints the latest captured frame (and drives the
    /// optional frame listener) on a UITimer, mirroring the WebView2 peer so it
    /// renders both in a live window and in the offscreen screenshot pipeline.
    private final class LinuxCameraPreview extends PeerComponent {
        private UITimer poller;

        LinuxCameraPreview() {
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
            int[] argb = LinuxNative.cameraSessionLatestFrame(session, dims);
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
