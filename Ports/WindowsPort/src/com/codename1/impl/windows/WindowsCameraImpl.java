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
/// composited child surface. A generic desktop webcam exposes no flash / optical
/// zoom / focus-point / hardware video encoder through the source reader, so those
/// are honestly reported unsupported rather than faked, per the port's rule.
public class WindowsCameraImpl extends CameraImpl {
    private long session;
    private CameraSessionOptions options;
    private volatile FrameListener frameListener;
    private volatile FrameFormat frameFormat = FrameFormat.JPEG;
    private volatile int frameMaxFps = 15;
    private final AtomicBoolean listenerBusy = new AtomicBoolean();

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
            // A USB/built-in desktop webcam has no API-reported facing, flash or
            // auto-focus point; the id is the device index the session opens by.
            out.add(new CameraInfo(String.valueOf(out.size()), CameraFacing.EXTERNAL,
                    new Dimension[0], new Dimension[0], false, false));
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

    // ----- capabilities a generic desktop webcam does not expose: honest no-ops --

    @Override
    public void startVideoRecording(String filePath, boolean audio) throws IOException {
        // Source-reader webcam capture exposes no hardware video sink; a real MF
        // sink-writer pipeline is a separate feature, so report unsupported rather
        // than pretend to record.
        throw new IOException("Video recording is not supported by the Windows camera port");
    }

    @Override
    public void stopVideoRecording(AsyncResource<String> result) {
        if (result != null) {
            result.error(new IOException("Video recording is not supported by the Windows camera port"));
        }
    }

    @Override
    public void setFlashMode(FlashMode mode) {
        // Webcams have no controllable flash.
    }

    @Override
    public void setZoom(float ratio) {
        // No optical zoom on a generic webcam.
    }

    @Override
    public void focus(float xNorm, float yNorm) {
        // No focus-point control through the source reader.
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
        long s = session;
        session = 0;
        frameListener = null;
        if (s != 0) {
            WindowsNative.cameraSessionStop(s);
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
            if (poller == null && getComponentForm() != null) {
                int fps = Math.max(1, frameMaxFps);
                int periodMs = Math.max(33, 1000 / fps);
                poller = UITimer.timer(periodMs, true, getComponentForm(), new Runnable() {
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
