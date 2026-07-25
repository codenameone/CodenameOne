/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

import com.codename1.camera.CameraFrame;
import com.codename1.camera.CameraSession;
import com.codename1.camera.FrameListener;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;

/**
 * Connects a camera frame stream to an analyzer with keep-only-latest
 * backpressure. The pipeline owns the analyzer and closes it on close.
 */
public final class VisionPipeline<T> implements AutoCloseable {
    private final CameraSession session;
    private final VisionAnalyzer<T> analyzer;
    private final VisionPipelineListener<T> listener;
    private final FrameListener frameListener;
    private VisionImage pending;
    private boolean busy;
    private boolean closed;

    public VisionPipeline(CameraSession session, VisionAnalyzer<T> analyzer,
                          VisionPipelineListener<T> listener) {
        if (session == null || analyzer == null || listener == null) {
            throw new NullPointerException("session, analyzer, and listener are required");
        }
        this.session = session;
        this.analyzer = analyzer;
        this.listener = listener;
        frameListener = new FrameListener() {
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
        analyzer.process(image).ready(new SuccessCallback<T>() {
            public void onSucess(final T value) {
                onFinished(new Runnable() {
                    public void run() {
                        listener.result(value, image);
                    }
                });
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(final Throwable error) {
                onFinished(new Runnable() {
                    public void run() {
                        listener.error(error);
                    }
                });
            }
        });
    }

    private void onFinished(final Runnable notification) {
        Display.getInstance().callSerially(new Runnable() {
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
                notification.run();
                if (next != null) {
                    process(next);
                }
            }
        });
    }

    public boolean isBusy() {
        synchronized (this) {
            return busy;
        }
    }

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
