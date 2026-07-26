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
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;

/// Connects a camera frame stream to a reusable analyzer with keep-only-latest
/// backpressure. At most one analysis and one pending frame are retained, so a
/// slow model cannot build an unbounded queue. Results and errors arrive on
/// the EDT. The pipeline owns and closes the analyzer but not the camera
/// session.
public final class VisionPipeline<T> implements AutoCloseable {
    private final CameraSession session;
    private final VisionAnalyzer<T> analyzer;
    private final VisionPipelineListener<T> listener;
    private final FrameListener frameListener;
    private VisionImage pending;
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
                accept(VisionImage.fromCameraFrame(frame));
            }
        };
        session.setFrameListener(frameListener);
    }

    private void accept(VisionImage image) {
        synchronized (this) {
            if (closed) {
                return;
            }
            if (busy) {
                pending = image;
                return;
            }
            busy = true;
        }
        process(image);
    }

    private void process(final VisionImage image) {
        final com.codename1.util.AsyncResource<T> operation;
        try {
            operation = analyzer.process(image);
            if (operation == null) {
                throw new IllegalStateException("Vision analyzer returned no operation");
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
                VisionImage next;
                synchronized (VisionPipeline.this) {
                    if (closed) {
                        busy = false;
                        pending = null;
                        return;
                    }
                    next = pending;
                    pending = null;
                    busy = next != null;
                }
                try {
                    notification.run();
                } finally {
                    if (next != null) {
                        process(next);
                    }
                }
            }
        });
    }

    /// Returns whether a frame is currently being analyzed.
    ///
    /// @return {@code true} while an analysis is in flight
    public boolean isBusy() {
        synchronized (this) {
            return busy;
        }
    }

    /// Stops accepting frames, detaches from the camera session, discards the
    /// pending frame, and closes the analyzer. Calling this method more than
    /// once has no effect.
    @Override
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            pending = null;
        }
        session.removeFrameListener(frameListener);
        analyzer.close();
    }
}
