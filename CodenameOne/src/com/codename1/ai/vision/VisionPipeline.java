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
package com.codename1.ai.vision;

import com.codename1.camera.CameraFrame;
import com.codename1.camera.CameraSession;
import com.codename1.camera.FrameListener;
import com.codename1.camera.FrameFormat;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;

/// Connects a camera frame stream to a reusable analyzer with keep-only-latest
/// backpressure. At most one analysis and one pending frame are retained, so a
/// slow model cannot build an unbounded queue. Results and errors arrive on
/// the EDT. The pipeline owns and closes the analyzer but not the camera
/// session.
///
/// Use this when the application already owns a {@link CameraSession} -- it is
/// taking photos or recording video as well as analyzing. When the screen only
/// needs a preview that analyzes, {@link VisionCameraView} does the same thing
/// and owns the session too, which is one less lifecycle to get right.
///
/// ```java
/// CameraSession session = Camera.open(Camera.getDefault(CameraFacing.BACK),
///         new CameraSessionOptions().frameMaxFps(10).captureAudio(false));
/// form.add(BorderLayout.CENTER, session.createView());
///
/// VisionPipeline<Barcode[]> pipeline = new VisionPipeline<Barcode[]>(
///         session, new BarcodeScanner(),
///         new VisionPipelineListener<Barcode[]>() {
///             public void result(Barcode[] codes, VisionImage source) {
///                 if (codes.length > 0) {
///                     found(codes[0].getValue());
///                 }
///             }
///             public void error(Throwable error) {
///                 Log.e(error);
///             }
///         });
///
/// // Closing the pipeline detaches the frame listener and closes the
/// // analyzer. The session is yours to close.
/// form.addCloseListener(e -> {
///     pipeline.close();
///     session.close();
/// });
/// ```
public final class VisionPipeline<T> implements AutoCloseable {
    private final CameraSession session;
    private final VisionAnalyzer<T> analyzer;
    private final VisionPipelineListener<T> listener;
    private final FrameListener frameListener;
    private PendingFrame pending;
    private boolean busy;
    private boolean closed;

    /// Attaches immediately as the session's frame listener.
    /// @param session active camera session whose frames should be analyzed
    /// @param analyzer reusable analyzer owned by this pipeline
    /// @param listener EDT result/error listener
    public VisionPipeline(CameraSession session, VisionAnalyzer<T> analyzer,
                          VisionPipelineListener<T> listener) {
        if (session == null || analyzer == null || listener == null) {
            throw new NullPointerException("session, analyzer, and listener are required");
        }
        this.session = session;
        this.analyzer = analyzer;
        this.listener = listener;
        frameListener = new FrameListener() {
            @Override
            public void onFrame(CameraFrame frame) {
                accept(frame);
            }
        };
        session.setFrameListener(frameListener);
    }

    private void accept(CameraFrame frame) {
        synchronized (this) {
            if (closed) {
                return;
            }
            if (busy) {
                if (pending == null) {
                    pending = new PendingFrame();
                }
                pending.copyFrom(frame);
                return;
            }
            busy = true;
        }
        process(VisionImage.fromCameraFrame(frame));
    }

    private void process(final VisionImage image) {
        final com.codename1.util.AsyncResource<T> operation;
        try {
            synchronized (this) {
                if (closed) {
                    busy = false;
                    pending = null;
                    return;
                }
            }
            // Native analyzers may complete synchronously or re-enter app
            // code. Do not invoke them while holding the pipeline monitor.
            operation = analyzer.process(image);
            if (operation == null) {
                throw new IllegalStateException(
                        "Vision analyzer returned no operation");
            }
        } catch (final Throwable error) {
            onFinished(new Runnable() {
                @Override
                public void run() {
                    listener.error(error);
                }
            });
            return;
        }
        operation.ready(new SuccessCallback<T>() {
            @Override
            public void onSucess(final T value) {
                onFinished(new Runnable() {
                    @Override
                    public void run() {
                        listener.result(value, image);
                    }
                });
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(final Throwable error) {
                onFinished(new Runnable() {
                    @Override
                    public void run() {
                        listener.error(error);
                    }
                });
            }
        });
    }

    private void onFinished(final Runnable notification) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                PendingFrame pendingFrame;
                synchronized (VisionPipeline.this) {
                    if (closed) {
                        busy = false;
                        pending = null;
                        return;
                    }
                    pendingFrame = pending;
                    pending = null;
                    busy = pendingFrame != null;
                }
                try {
                    notification.run();
                } finally {
                    if (pendingFrame != null) {
                        process(pendingFrame.toImage());
                    }
                }
            }
        });
    }

    /// Returns whether a frame is currently being analyzed.
    ///
    /// @return {@code true} while the open pipeline has an analysis in flight
    public boolean isBusy() {
        synchronized (this) {
            return busy;
        }
    }

    /// Stops accepting frames, detaches from the camera session, discards the
    /// pending frame, clears the busy state, and closes the analyzer. A pending
    /// frame already selected for dispatch is rechecked and discarded before
    /// it can reach the closed analyzer. Calling this method more than once
    /// has no effect.
    @Override
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            busy = false;
            pending = null;
        }
        session.removeFrameListener(frameListener);
        analyzer.close();
    }

    private static final class PendingFrame {
        private byte[] data;
        private boolean raw;
        private int width;
        private int height;
        private int rotationDegrees;
        private long timestampNanos;
        private FrameFormat format;

        void copyFrom(CameraFrame frame) {
            FrameFormat requested = frame.getFormat() == null
                    ? FrameFormat.JPEG : frame.getFormat();
            byte[] rawBytes = frame.getRawBytes();
            raw = requested != FrameFormat.JPEG
                    && rawBytes != null && rawBytes.length > 0;
            byte[] source = raw ? rawBytes : frame.getJpegBytes();
            if (source == null) {
                source = new byte[0];
            }
            if (data == null || data.length != source.length) {
                data = new byte[source.length];
            }
            System.arraycopy(source, 0, data, 0, source.length);
            width = frame.getWidth();
            height = frame.getHeight();
            rotationDegrees = frame.getRotationDegrees();
            timestampNanos = frame.getTimestampNanos();
            format = raw ? requested : FrameFormat.JPEG;
        }

        VisionImage toImage() {
            return VisionImage.detachedCameraData(data, raw, width, height,
                    rotationDegrees, timestampNanos, format);
        }
    }
}
