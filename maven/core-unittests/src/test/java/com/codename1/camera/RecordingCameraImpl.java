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
package com.codename1.camera;

import com.codename1.impl.CameraImpl;
import com.codename1.ui.PeerComponent;
import com.codename1.util.AsyncResource;

import java.io.IOException;

/**
 * Hand-written {@link CameraImpl} test double (no Mockito). Records the calls
 * the {@code Camera} / {@code CameraSession} layer makes against it and lets a
 * test pre-configure the cameras to enumerate, the photo to deliver, and
 * whether {@link #open} should fail.
 */
class RecordingCameraImpl extends CameraImpl {
    CameraInfo[] cameras = new CameraInfo[0];
    boolean enumerateReturnsNull;
    IOException openFailure;

    String openedCameraId;
    int openCount;
    int closeCount;
    int pauseCount;
    int resumeCount;
    FlashMode lastFlashMode;
    float lastZoom = Float.NaN;
    float lastFocusX = Float.NaN;
    float lastFocusY = Float.NaN;
    FrameListener lastFrameListener;
    boolean frameListenerCleared;
    String videoPath;
    boolean videoAudio;

    PhotoCaptureOptions lastPhotoOptions;
    CapturedPhoto photoToDeliver = new CapturedPhoto(new byte[]{1, 2, 3}, "file://photo.jpg", 4, 3);

    @Override
    public CameraInfo[] enumerateCameras() {
        return enumerateReturnsNull ? null : cameras;
    }

    @Override
    public void open(String cameraId, CameraSessionOptions opts) throws IOException {
        if (openFailure != null) {
            throw openFailure;
        }
        openedCameraId = cameraId;
        openCount++;
    }

    @Override
    public PeerComponent createPreviewPeer() {
        return null;
    }

    @Override
    public void takePhoto(PhotoCaptureOptions opts, AsyncResource<CapturedPhoto> result) {
        lastPhotoOptions = opts;
        result.complete(photoToDeliver);
    }

    @Override
    public void startVideoRecording(String filePath, boolean audio) throws IOException {
        this.videoPath = filePath;
        this.videoAudio = audio;
    }

    @Override
    public void stopVideoRecording(AsyncResource<String> result) {
        if (result != null) {
            result.complete(videoPath);
        }
    }

    @Override
    public void setFrameListener(FrameListener listener, FrameFormat format, int maxFps) {
        this.lastFrameListener = listener;
        if (listener == null) {
            frameListenerCleared = true;
        }
    }

    @Override
    public void setFlashMode(FlashMode mode) {
        this.lastFlashMode = mode;
    }

    @Override
    public void setZoom(float ratio) {
        this.lastZoom = ratio;
    }

    @Override
    public void focus(float xNorm, float yNorm) {
        this.lastFocusX = xNorm;
        this.lastFocusY = yNorm;
    }

    @Override
    public void pause() {
        pauseCount++;
    }

    @Override
    public void resume() {
        resumeCount++;
    }

    @Override
    public void close() {
        closeCount++;
    }
}
