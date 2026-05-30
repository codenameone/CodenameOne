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
package com.codename1.camera;

import com.codename1.impl.CameraImpl;
import com.codename1.util.AsyncResource;

import java.io.IOException;

/// Active camera session. Obtained from `Camera#open(CameraInfo, CameraSessionOptions)`.
///
/// Only one `CameraSession` may be open at a time; opening a second throws
/// `IllegalStateException`. Closing the session releases the camera hardware
/// and invalidates any `CameraView` returned by `#createView()`.
///
/// Sessions may be paused (releasing hardware while keeping the session object)
/// and resumed; this is the right pattern when the app temporarily uses the
/// classic `com.codename1.capture.Capture` API while a session is open, since
/// the OS-level camera device is single-tenant.
public final class CameraSession implements AutoCloseable {
    private final CameraImpl impl;
    private final CameraInfo info;
    private final CameraSessionOptions options;
    private FrameListener frameListener;
    private CameraView view;
    private boolean closed;

    CameraSession(CameraImpl impl, CameraInfo info, CameraSessionOptions options) {
        this.impl = impl;
        this.info = info;
        this.options = options;
    }

    /// Create the live preview component. Each session owns one view; subsequent
    /// calls return the same instance.
    public CameraView createView() {
        if (view == null) {
            view = new CameraView(this, impl.createPreviewPeer());
        }
        return view;
    }

    /// Capture a still photo with default options.
    public AsyncResource<CapturedPhoto> takePhoto() {
        return takePhoto(new PhotoCaptureOptions());
    }

    public AsyncResource<CapturedPhoto> takePhoto(PhotoCaptureOptions opts) {
        AsyncResource<CapturedPhoto> out = new AsyncResource<CapturedPhoto>();
        impl.takePhoto(opts == null ? new PhotoCaptureOptions() : opts, out);
        return out;
    }

    /// Begin recording video to the given FileSystemStorage path. The returned
    /// `VideoRecording` is the handle used to stop the recording.
    public VideoRecording startVideoRecording(String filePath) {
        try {
            impl.startVideoRecording(filePath, options.isCaptureAudio());
        } catch (IOException e) {
            throw new RuntimeException("Could not start video recording", e);
        }
        return new VideoRecording(impl, filePath);
    }

    /// Install a frame listener. At most one frame listener may be active per
    /// session; installing a second replaces the first. Pass `null` to remove.
    public void setFrameListener(FrameListener l) {
        this.frameListener = l;
        impl.setFrameListener(l, options.getFrameFormat(), options.getFrameMaxFps());
    }

    /// Backwards-compatible alias for `#setFrameListener(FrameListener)`.
    public void addFrameListener(FrameListener l) { setFrameListener(l); }

    public void removeFrameListener(FrameListener l) {
        if (this.frameListener == l) {
            setFrameListener(null);
        }
    }

    public void setFlashMode(FlashMode m) { impl.setFlashMode(m); }
    public void setZoom(float ratio) { impl.setZoom(ratio); }

    /// Request a focus operation at the normalized preview coordinate
    /// (`0.0` top-left, `1.0` bottom-right).
    public void focus(float xNorm, float yNorm) { impl.focus(xNorm, yNorm); }

    public CameraInfo getInfo() { return info; }

    public CameraSessionOptions getOptions() { return options; }

    /// Release the hardware but keep this session object alive. Pair with
    /// `#resume()`.
    public void pause() { impl.pause(); }

    public void resume() { impl.resume(); }

    /// Release the session. Idempotent.
    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            impl.close();
        } finally {
            Camera.clearActive(this);
        }
    }

    public boolean isClosed() { return closed; }

    CameraImpl getImpl() { return impl; }
}
