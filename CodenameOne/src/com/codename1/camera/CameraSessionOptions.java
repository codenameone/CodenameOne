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

/// Configuration for a `CameraSession`. Fluent builder.
///
/// All sizes are advisory; the platform may snap to its nearest supported
/// resolution.
public final class CameraSessionOptions {
    private int previewWidth;
    private int previewHeight;
    private int photoWidth;
    private int photoHeight;
    private FrameFormat frameFormat = FrameFormat.JPEG;
    private int frameMaxFps = 15;
    private boolean captureAudio = true;
    private boolean enableStabilization;

    public CameraSessionOptions previewSize(int width, int height) {
        this.previewWidth = width;
        this.previewHeight = height;
        return this;
    }

    public CameraSessionOptions photoSize(int width, int height) {
        this.photoWidth = width;
        this.photoHeight = height;
        return this;
    }

    /// Format for frames delivered to `FrameListener`. Default `FrameFormat#JPEG`.
    public CameraSessionOptions frameFormat(FrameFormat format) {
        this.frameFormat = format == null ? FrameFormat.JPEG : format;
        return this;
    }

    /// Cap on frame delivery rate to `FrameListener`s. Default 15.
    /// Set to 0 to deliver every frame the camera produces.
    public CameraSessionOptions frameMaxFps(int fps) {
        this.frameMaxFps = Math.max(0, fps);
        return this;
    }

    /// Whether to capture audio for video recording. Default true.
    /// When false, no microphone permission is requested at session open.
    public CameraSessionOptions captureAudio(boolean b) {
        this.captureAudio = b;
        return this;
    }

    /// Whether to request optical/electronic stabilization when supported.
    /// Default false.
    public CameraSessionOptions enableStabilization(boolean b) {
        this.enableStabilization = b;
        return this;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }
    public int getPreviewHeight() {
        return previewHeight;
    }
    public int getPhotoWidth() {
        return photoWidth;
    }
    public int getPhotoHeight() {
        return photoHeight;
    }
    public FrameFormat getFrameFormat() {
        return frameFormat;
    }
    public int getFrameMaxFps() {
        return frameMaxFps;
    }
    public boolean isCaptureAudio() {
        return captureAudio;
    }
    public boolean isStabilizationEnabled() {
        return enableStabilization;
    }
}
