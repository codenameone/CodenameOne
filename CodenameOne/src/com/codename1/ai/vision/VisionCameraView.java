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

import com.codename1.camera.Camera;
import com.codename1.camera.CameraFacing;
import com.codename1.camera.CameraInfo;
import com.codename1.camera.CameraSession;
import com.codename1.camera.CameraSessionOptions;
import com.codename1.camera.CameraView;
import com.codename1.camera.FlashMode;
import com.codename1.camera.FrameFormat;
import com.codename1.camera.ScaleType;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.layouts.BorderLayout;

/// A live camera preview that runs an analyzer over its frames.
///
/// This is the whole camera-to-analyzer pipeline as one component: it opens
/// the camera when it is shown, streams frames into the analyzer with
/// keep-only-the-newest backpressure, delivers results on the EDT, and
/// releases the camera when the form is left. Add it to a form and implement
/// the listener; there is no session, frame listener, or
/// {@link VisionImage} conversion to write.
///
/// ```java
/// FaceDetector detector = new FaceDetector();
/// VisionCameraView<Face[]> view = new VisionCameraView<Face[]>(detector);
/// view.setFacing(CameraFacing.FRONT);
/// view.setListener(new VisionPipelineListener<Face[]>() {
///     public void result(Face[] faces, VisionImage source) {
///         countLabel.setText(faces.length + " face(s)");
///     }
///     public void error(Throwable error) {
///         Log.e(error);
///     }
/// });
///
/// Form form = new Form("Faces", new BorderLayout());
/// form.add(BorderLayout.CENTER, view);
/// form.add(BorderLayout.SOUTH, countLabel);
/// form.show();
/// ```
///
/// The analyzer is supplied by the caller rather than named by a feature
/// constant, which is deliberate: the build pipeline decides which native
/// vision dependency to package from the analyzer classes an application
/// references, so constructing the analyzer in application code is what keeps
/// a face-detection app from also carrying the barcode and pose models.
///
/// **The preview is a native view.** Codename One components cannot be painted
/// over it on every platform -- iOS renders native peers behind the Codename
/// One layer, Android renders them in front -- so put a scanner's reticle,
/// hints, and buttons around the preview rather than on top of it. Result
/// geometry such as {@link Face#getBounds()} can still be drawn in a component
/// beside the preview using {@link VisionRect#toBounds(com.codename1.ui.Component)}.
///
/// One camera session may be open at a time, so a second view (or a
/// {@link com.codename1.capture.Capture} call) while this one is showing
/// reports an error to the listener instead of stealing the hardware.
///
/// @param <T> the analyzer's result type
public class VisionCameraView<T> extends Container implements AutoCloseable {
    private final VisionAnalyzer<T> analyzer;
    private final NonClosingAnalyzer<T> shared;
    private CameraFacing facing = CameraFacing.BACK;
    private ScaleType scaleType = ScaleType.CROP;
    private int maxFps = 10;
    private VisionPipelineListener<T> listener;
    private CameraSession session;
    private VisionPipeline<T> pipeline;
    private CameraView cameraView;
    private boolean closed;

    /// Creates a view that runs {@code analyzer} over the back camera.
    ///
    /// The view borrows the analyzer: leaving the form releases the camera but
    /// keeps the analyzer usable, and {@link #close()} releases it for good.
    ///
    /// @param analyzer the reusable analyzer to run over each frame
    /// @throws NullPointerException if {@code analyzer} is {@code null}
    public VisionCameraView(VisionAnalyzer<T> analyzer) {
        super(new BorderLayout());
        if (analyzer == null) {
            throw new NullPointerException("analyzer");
        }
        this.analyzer = analyzer;
        this.shared = new NonClosingAnalyzer<T>(analyzer);
    }

    /// Whether the current platform can open a camera and run this view's
    /// analyzer. Check this before showing the view: a target without either
    /// piece reports failures through the listener rather than throwing.
    ///
    /// @return {@code true} when both the camera and the analyzer are available
    public boolean isSupported() {
        return Camera.isSupported() && analyzer.isSupported();
    }

    /// Selects which camera to open. Takes effect the next time the view is
    /// shown; the default is {@link CameraFacing#BACK}.
    ///
    /// @param value the camera to prefer, or {@code null} for the back camera
    public void setFacing(CameraFacing value) {
        facing = value == null ? CameraFacing.BACK : value;
    }

    /// @return the camera this view opens
    public CameraFacing getFacing() {
        return facing;
    }

    /// Caps how many frames per second reach the analyzer. Frames arriving
    /// while an analysis is in flight are dropped in favor of the newest one
    /// regardless of this value, so the cap mainly saves the camera the work
    /// of producing frames nothing will read. The default is 10.
    ///
    /// @param value frames per second, or {@code 0} for uncapped
    public void setMaxFps(int value) {
        maxFps = Math.max(0, value);
    }

    /// @return the frame rate cap, or {@code 0} when uncapped
    public int getMaxFps() {
        return maxFps;
    }

    /// How the preview is fitted into this component's bounds. The default is
    /// {@link ScaleType#CROP}, which fills the view the way a camera app does.
    ///
    /// @param value the scaling to apply, or {@code null} for the default
    public void setScaleType(ScaleType value) {
        scaleType = value == null ? ScaleType.CROP : value;
        if (cameraView != null) {
            cameraView.setScaleType(scaleType);
        }
    }

    /// @return how the preview is fitted into this component's bounds
    public ScaleType getScaleType() {
        return scaleType;
    }

    /// Installs the callback for analysis results and recoverable failures.
    /// Both are delivered on the EDT.
    ///
    /// @param value the listener, or {@code null} to stop receiving results
    public void setListener(VisionPipelineListener<T> value) {
        listener = value;
    }

    /// @return the installed result listener, or {@code null}
    public VisionPipelineListener<T> getListener() {
        return listener;
    }

    /// The open camera session, for torch, zoom, focus, or a still capture.
    ///
    /// @return the session while the view is showing, or {@code null}
    public CameraSession getSession() {
        return session;
    }

    /// Turns the torch on or off, when the open camera has one. A camera
    /// without a flash ignores this.
    ///
    /// @param on whether the torch should be lit
    public void setTorchEnabled(boolean on) {
        if (session != null) {
            session.setFlashMode(on ? FlashMode.TORCH : FlashMode.OFF);
        }
    }

    /// @return {@code true} while the camera is open and frames are flowing
    public boolean isRunning() {
        return pipeline != null;
    }

    /// Opens the camera and starts analyzing. Called automatically when the
    /// view is shown; calling it again while running does nothing.
    // The session this opens is retained in the `session` field and closed by
    // stop(); PMD cannot follow ownership across that hand-off.
    @SuppressWarnings("PMD.CloseResource")
    public void start() {
        if (closed || pipeline != null) {
            return;
        }
        CameraSession opened;
        try {
            CameraInfo info = Camera.getDefault(facing);
            if (info == null) {
                throw new IllegalStateException(
                        "No camera is available on this device");
            }
            opened = Camera.open(info, new CameraSessionOptions()
                    .frameFormat(FrameFormat.JPEG)
                    .frameMaxFps(maxFps)
                    .captureAudio(false));
        } catch (Throwable error) {
            notifyError(error);
            return;
        }
        session = opened;
        cameraView = opened.createView();
        cameraView.setScaleType(scaleType);
        cameraView.setMirrored(facing == CameraFacing.FRONT);
        addComponent(BorderLayout.CENTER, cameraView);
        pipeline = new VisionPipeline<T>(opened, shared,
                new VisionPipelineListener<T>() {
                    @Override
                    public void result(T value, VisionImage source) {
                        VisionPipelineListener<T> target = listener;
                        if (target != null) {
                            target.result(value, source);
                        }
                    }

                    @Override
                    public void error(Throwable error) {
                        VisionPipelineListener<T> target = listener;
                        if (target != null) {
                            target.error(error);
                        }
                    }
                });
    }

    /// Stops analyzing and releases the camera, leaving the analyzer usable.
    /// Called automatically when the view stops being shown.
    public void stop() {
        if (pipeline != null) {
            pipeline.close();
            pipeline = null;
        }
        if (cameraView != null) {
            removeComponent(cameraView);
            cameraView = null;
        }
        if (session != null) {
            session.close();
            session = null;
        }
    }

    /// Releases the camera and the analyzer. Use this when the view will not
    /// be shown again; simply navigating to another form already releases the
    /// camera. Calling it more than once has no effect.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        stop();
        analyzer.close();
    }

    /// Opens the camera when the view becomes part of a shown form.
    @Override
    protected void initComponent() {
        super.initComponent();
        start();
    }

    /// Releases the camera when the view stops being shown.
    @Override
    protected void deinitialize() {
        stop();
        super.deinitialize();
    }

    private void notifyError(final Throwable error) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                VisionPipelineListener<T> target = listener;
                if (target != null) {
                    target.error(error);
                }
            }
        });
    }

    /// Hands the pipeline an analyzer it cannot close. The pipeline closes the
    /// analyzer it owns, and this view is shown and hidden repeatedly with the
    /// same caller-supplied analyzer, so the real one is released only by
    /// {@link VisionCameraView#close()}.
    private static final class NonClosingAnalyzer<T>
            implements VisionAnalyzer<T> {
        private final VisionAnalyzer<T> delegate;

        NonClosingAnalyzer(VisionAnalyzer<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isSupported() {
            return delegate.isSupported();
        }

        @Override
        public com.codename1.util.AsyncResource<T> process(VisionImage image) {
            return delegate.process(image);
        }

        @Override
        public void close() {
        }
    }
}
