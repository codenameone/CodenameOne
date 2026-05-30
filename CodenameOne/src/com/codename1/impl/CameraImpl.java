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
package com.codename1.impl;

import com.codename1.camera.CameraInfo;
import com.codename1.camera.CameraSessionOptions;
import com.codename1.camera.CapturedPhoto;
import com.codename1.camera.FlashMode;
import com.codename1.camera.FrameFormat;
import com.codename1.camera.FrameListener;
import com.codename1.camera.PhotoCaptureOptions;
import com.codename1.ui.PeerComponent;
import com.codename1.util.AsyncResource;

import java.io.IOException;

/// Per-session platform contract behind `com.codename1.camera.CameraSession`.
///
/// **Not part of the public API.** Each port (iOS / Android / JavaSE / JavaScript)
/// subclasses this with a concrete implementation; `CodenameOneImplementation#createCameraImpl()`
/// is the factory. One `CameraImpl` instance backs exactly one `CameraSession`.
///
/// @hidden
public abstract class CameraImpl {

    /// Returns the set of cameras the platform exposes. May be called before
    /// `#open(String, CameraSessionOptions)` to let the application pick.
    public abstract CameraInfo[] enumerateCameras();

    /// Open the named camera and configure capture pipelines according to `opts`.
    /// Implementations should honor the audio/no-audio flag and only request
    /// microphone permission when audio capture is requested.
    public abstract void open(String cameraId, CameraSessionOptions opts) throws IOException;

    /// Create a preview component for this session. Called at most once per
    /// session; the implementation should return a native peer that renders the
    /// live preview into the form. May return `null` on platforms that don't
    /// support a live preview.
    public abstract PeerComponent createPreviewPeer();

    /// Capture a still photo with the given per-shot options.
    /// Resolve the result on the EDT.
    public abstract void takePhoto(PhotoCaptureOptions opts, AsyncResource<CapturedPhoto> result);

    /// Begin recording video to `filePath`. The path is a FileSystemStorage path;
    /// the actual extension produced may differ depending on the platform's
    /// container format.
    public abstract void startVideoRecording(String filePath, boolean audio) throws IOException;

    /// Stop the in-progress recording. If `result` is non-null, resolve it on
    /// the EDT with the final file path.
    public abstract void stopVideoRecording(AsyncResource<String> result);

    /// Install (or remove, when `listener` is null) a single frame listener.
    /// Implementations must invoke the listener on a background thread and
    /// must drop frames silently when the previous invocation is still running.
    public abstract void setFrameListener(FrameListener listener, FrameFormat format, int maxFps);

    public abstract void setFlashMode(FlashMode mode);

    /// 1.0 = no zoom; values above 1.0 zoom in. Implementations clamp to
    /// the supported range.
    public abstract void setZoom(float ratio);

    /// Tap-to-focus at the normalized (`0.0` to `1.0`) preview coordinate.
    public abstract void focus(float xNorm, float yNorm);

    /// Release the underlying hardware but keep the session object usable.
    /// Followed by `#resume()` to re-acquire.
    public abstract void pause();
    public abstract void resume();

    /// Release all native resources for this session. Subsequent calls become
    /// no-ops.
    public abstract void close();
}
