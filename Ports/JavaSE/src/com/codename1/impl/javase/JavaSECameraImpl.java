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
package com.codename1.impl.javase;

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
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.geom.Dimension;
import com.codename1.util.AsyncResource;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/// JavaSE / simulator implementation of `CameraImpl`. Generates synthetic JPEG
/// frames so the rest of the app (AI modules, frame listeners, etc.) can be
/// exercised against the real `Camera` API without webcam hardware.
///
/// Source can be overridden:
/// - `-Dcn1.camera.source=/path/to/file.jpg` - emit the same image every tick.
/// - `-Dcn1.camera.source=/path/to/folder` - cycle through the JPEGs in that folder.
/// - default - generate a synthetic frame with a colored background and a tick
///   counter (zero on-disk dependencies, still a real JPEG decodable by AI modules).
///
/// Frame rate can be overridden with `-Dcn1.camera.fps=10`.
///
/// @hidden
public class JavaSECameraImpl extends CameraImpl {

    private static final int DEFAULT_PREVIEW_W = 640;
    private static final int DEFAULT_PREVIEW_H = 480;
    private static final int DEFAULT_PHOTO_W = 1920;
    private static final int DEFAULT_PHOTO_H = 1080;
    private static final String SOURCE_PROP = "cn1.camera.source";
    private static final String FPS_PROP = "cn1.camera.fps";

    private static boolean hintsEnsured;

    private final List<BufferedImage> sourceFrames = new ArrayList<>();
    private final AtomicInteger frameIndex = new AtomicInteger();
    private final AtomicBoolean listenerBusy = new AtomicBoolean();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> frameTask;
    private JPanel previewPanel;
    private CameraSessionOptions options;
    private CameraInfo info;
    private volatile BufferedImage currentFrame;
    private volatile FrameListener frameListener;
    private volatile FrameFormat frameFormat = FrameFormat.JPEG;
    private volatile int fpsCap = 15;
    private volatile boolean paused;
    private volatile boolean closed;
    private volatile String activeVideoPath;
    private volatile long videoStartNanos;

    public JavaSECameraImpl() {
        ensureSimulatorHints();
    }

    @Override
    public CameraInfo[] enumerateCameras() {
        return new CameraInfo[] {
            new CameraInfo("simulator-back", CameraFacing.BACK,
                    new Dimension[] { new Dimension(DEFAULT_PHOTO_W, DEFAULT_PHOTO_H),
                                      new Dimension(1280, 720),
                                      new Dimension(DEFAULT_PREVIEW_W, DEFAULT_PREVIEW_H) },
                    new Dimension[] { new Dimension(DEFAULT_PREVIEW_W, DEFAULT_PREVIEW_H),
                                      new Dimension(320, 240) },
                    false, true),
            new CameraInfo("simulator-front", CameraFacing.FRONT,
                    new Dimension[] { new Dimension(1280, 720),
                                      new Dimension(DEFAULT_PREVIEW_W, DEFAULT_PREVIEW_H) },
                    new Dimension[] { new Dimension(DEFAULT_PREVIEW_W, DEFAULT_PREVIEW_H) },
                    false, false)
        };
    }

    @Override
    public void open(String cameraId, CameraSessionOptions opts) throws IOException {
        this.options = opts == null ? new CameraSessionOptions() : opts;
        this.info = enumerateCameras()[0];
        if ("__permission_probe__".equals(cameraId)) {
            return;
        }
        if (cameraId != null) {
            for (CameraInfo c : enumerateCameras()) {
                if (cameraId.equals(c.getId())) {
                    this.info = c;
                    break;
                }
            }
        }
        loadSourceFrames();
        int requestedFps = Integer.getInteger(FPS_PROP, options.getFrameMaxFps() > 0 ? options.getFrameMaxFps() : 5);
        this.fpsCap = Math.max(1, Math.min(30, requestedFps));
        startScheduler();
    }

    @Override
    public PeerComponent createPreviewPeer() {
        if (previewPanel == null) {
            previewPanel = new JPanel() {
                @Override
                protected void paintComponent(java.awt.Graphics g) {
                    super.paintComponent(g);
                    BufferedImage f = currentFrame;
                    if (f != null) {
                        g.drawImage(f, 0, 0, getWidth(), getHeight(), null);
                    } else {
                        g.setColor(Color.DARK_GRAY);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                }
            };
            previewPanel.setBackground(Color.BLACK);
            previewPanel.setPreferredSize(new java.awt.Dimension(DEFAULT_PREVIEW_W, DEFAULT_PREVIEW_H));
        }
        return PeerComponent.create(previewPanel);
    }

    @Override
    public void takePhoto(PhotoCaptureOptions opts, AsyncResource<CapturedPhoto> result) {
        if (opts == null) opts = new PhotoCaptureOptions();
        final int w = opts.getWidth() > 0 ? opts.getWidth() : DEFAULT_PHOTO_W;
        final int h = opts.getHeight() > 0 ? opts.getHeight() : DEFAULT_PHOTO_H;
        final int quality = opts.getJpegQuality();
        final String customPath = opts.getFilePath();
        final AsyncResource<CapturedPhoto> out = result;
        ensureScheduler().execute(() -> {
            try {
                BufferedImage img = renderFrame(w, h, true);
                byte[] jpeg = encodeJpeg(img, quality);
                String path = customPath != null ? customPath : tempPath("photo", ".jpg");
                writeBytes(path, jpeg);
                CapturedPhoto cp = new CapturedPhoto(jpeg, path, w, h);
                Display.getInstance().callSerially(() -> out.complete(cp));
            } catch (Throwable t) {
                Log.e(t);
                Display.getInstance().callSerially(() -> out.error(t));
            }
        });
    }

    @Override
    public void startVideoRecording(String filePath, boolean audio) throws IOException {
        this.activeVideoPath = filePath;
        this.videoStartNanos = System.nanoTime();
        // Touch the destination so consumers can play it back even if no real
        // encoder runs in the simulator. The bytes are a placeholder still
        // frame; production ports produce real video.
        BufferedImage img = renderFrame(DEFAULT_PHOTO_W, DEFAULT_PHOTO_H, true);
        byte[] jpeg = encodeJpeg(img, 80);
        writeBytes(filePath, jpeg);
    }

    @Override
    public void stopVideoRecording(AsyncResource<String> result) {
        final String path = activeVideoPath;
        activeVideoPath = null;
        if (result != null) {
            Display.getInstance().callSerially(() -> result.complete(path));
        }
    }

    @Override
    public void setFrameListener(FrameListener listener, FrameFormat format, int maxFps) {
        this.frameListener = listener;
        this.frameFormat = format == null ? FrameFormat.JPEG : format;
        if (maxFps > 0) {
            this.fpsCap = Math.max(1, Math.min(30, maxFps));
            restartScheduler();
        }
    }

    @Override public void setFlashMode(FlashMode mode) { /* no-op in simulator */ }
    @Override public void setZoom(float ratio) { /* no-op in simulator */ }
    @Override public void focus(float xNorm, float yNorm) { /* no-op in simulator */ }

    @Override
    public void pause() {
        paused = true;
        if (frameTask != null) {
            frameTask.cancel(false);
            frameTask = null;
        }
    }

    @Override
    public void resume() {
        if (closed) return;
        paused = false;
        startScheduler();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (frameTask != null) {
            frameTask.cancel(false);
            frameTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        frameListener = null;
        sourceFrames.clear();
    }

    private void startScheduler() {
        if (paused || closed) return;
        ScheduledExecutorService s = ensureScheduler();
        if (frameTask != null) frameTask.cancel(false);
        long periodMs = Math.max(33, 1000L / Math.max(1, fpsCap));
        frameTask = s.scheduleAtFixedRate(this::tick, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    private void restartScheduler() {
        if (scheduler == null) return;
        startScheduler();
    }

    private ScheduledExecutorService ensureScheduler() {
        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "JavaSECameraImpl");
                    t.setDaemon(true);
                    return t;
                }
            });
        }
        return scheduler;
    }

    private void tick() {
        if (closed || paused) return;
        try {
            int w = options != null && options.getPreviewWidth() > 0
                    ? options.getPreviewWidth() : DEFAULT_PREVIEW_W;
            int h = options != null && options.getPreviewHeight() > 0
                    ? options.getPreviewHeight() : DEFAULT_PREVIEW_H;
            BufferedImage img = renderFrame(w, h, false);
            currentFrame = img;
            JPanel p = previewPanel;
            if (p != null) {
                javax.swing.SwingUtilities.invokeLater(p::repaint);
            }
            FrameListener l = frameListener;
            if (l == null) return;
            if (!listenerBusy.compareAndSet(false, true)) {
                // Previous frame still being processed; drop this one.
                return;
            }
            try {
                byte[] jpeg = encodeJpeg(img, 80);
                byte[] raw = frameFormat == FrameFormat.JPEG ? null : extractRaw(img, frameFormat);
                CameraFrame frame = new CameraFrame(jpeg, raw, w, h, 0,
                        System.nanoTime(), frameFormat);
                l.onFrame(frame);
            } finally {
                listenerBusy.set(false);
            }
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private void loadSourceFrames() {
        sourceFrames.clear();
        String src = System.getProperty(SOURCE_PROP);
        if (src == null || src.isEmpty()) return;
        try {
            Path p = Paths.get(src);
            if (Files.isDirectory(p)) {
                try (java.util.stream.Stream<Path> stream = Files.list(p)) {
                    stream.sorted()
                          .filter(f -> {
                              String n = f.getFileName().toString().toLowerCase();
                              return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png");
                          })
                          .forEach(f -> {
                              try (InputStream in = Files.newInputStream(f)) {
                                  BufferedImage img = ImageIO.read(in);
                                  if (img != null) sourceFrames.add(img);
                              } catch (IOException e) {
                                  Log.e(e);
                              }
                          });
                }
            } else if (Files.isRegularFile(p)) {
                try (InputStream in = Files.newInputStream(p)) {
                    BufferedImage img = ImageIO.read(in);
                    if (img != null) sourceFrames.add(img);
                }
            }
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private BufferedImage renderFrame(int width, int height, boolean stillCapture) {
        if (!sourceFrames.isEmpty()) {
            BufferedImage src = sourceFrames.get(frameIndex.getAndIncrement() % sourceFrames.size());
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(src, 0, 0, width, height, null);
            } finally {
                g.dispose();
            }
            return scaled;
        }
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int tick = frameIndex.getAndIncrement();
            float hue = (tick % 360) / 360f;
            g.setColor(Color.getHSBColor(hue, 0.4f, 0.9f));
            g.fillRect(0, 0, width, height);

            g.setColor(Color.BLACK.brighter());
            g.fillRect(0, 0, width, 40);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.drawString("Camera Simulator", 12, 28);

            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            String legend = (stillCapture ? "PHOTO " : "FRAME ") + tick
                    + "  " + width + "x" + height;
            g.drawString(legend, 12, height - 16);
        } finally {
            g.dispose();
        }
        return img;
    }

    private static byte[] encodeJpeg(BufferedImage img, int quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
        // ImageIO default JPEG writer doesn't honor quality without an ImageWriter
        // tweak; for the simulator the default 75% is fine. Quality kept for API
        // parity with the device ports.
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    private static byte[] extractRaw(BufferedImage img, FrameFormat fmt) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
        if (fmt == FrameFormat.RGBA8888) {
            byte[] out = new byte[w * h * 4];
            int o = 0;
            for (int i = 0; i < argb.length; i++) {
                int p = argb[i];
                out[o++] = (byte) ((p >> 16) & 0xff);
                out[o++] = (byte) ((p >> 8) & 0xff);
                out[o++] = (byte) (p & 0xff);
                out[o++] = (byte) ((p >> 24) & 0xff);
            }
            return out;
        }
        // NV21: full Y plane, then interleaved VU at half resolution.
        byte[] nv21 = new byte[w * h * 3 / 2];
        int yIdx = 0;
        int uvIdx = w * h;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = argb[y * w + x];
                int r = (p >> 16) & 0xff;
                int gg = (p >> 8) & 0xff;
                int b = p & 0xff;
                int yVal = (66 * r + 129 * gg + 25 * b + 128) >> 8;
                yVal += 16;
                nv21[yIdx++] = (byte) Math.max(0, Math.min(255, yVal));
                if ((y & 1) == 0 && (x & 1) == 0) {
                    int u = ((-38 * r - 74 * gg + 112 * b + 128) >> 8) + 128;
                    int v = ((112 * r - 94 * gg - 18 * b + 128) >> 8) + 128;
                    nv21[uvIdx++] = (byte) Math.max(0, Math.min(255, v));
                    nv21[uvIdx++] = (byte) Math.max(0, Math.min(255, u));
                }
            }
        }
        return nv21;
    }

    private static String tempPath(String prefix, String suffix) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String dir = fs.getAppHomePath();
        if (!dir.endsWith("/")) dir += "/";
        return dir + prefix + "_" + System.currentTimeMillis() + suffix;
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

    private static synchronized void ensureSimulatorHints() {
        if (hintsEnsured) return;
        hintsEnsured = true;
        java.util.Map<String, String> hints = Display.getInstance().getProjectBuildHints();
        if (hints == null) return;
        if (!hints.containsKey("ios.NSCameraUsageDescription")) {
            Display.getInstance().setProjectBuildHint("ios.NSCameraUsageDescription",
                    "Used to capture photos and video.");
        }
        if (!hints.containsKey("ios.NSMicrophoneUsageDescription")) {
            Display.getInstance().setProjectBuildHint("ios.NSMicrophoneUsageDescription",
                    "Used to capture audio for video recording.");
        }
    }
}
