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

import com.codename1.camera.CameraFacing;
import com.codename1.camera.CameraFrame;
import com.codename1.camera.CameraInfo;
import com.codename1.camera.CameraSessionOptions;
import com.codename1.camera.CapturedPhoto;
import com.codename1.camera.FlashMode;
import com.codename1.camera.FrameFormat;
import com.codename1.camera.FrameListener;
import com.codename1.camera.PhotoCaptureOptions;
import com.codename1.camera.ScaleType;
import com.codename1.impl.CameraImpl;
import com.codename1.ui.PeerComponent;
import com.codename1.util.AsyncResource;

import java.io.IOException;

/**
 * Hand-written {@link CameraImpl} double for the high-level vision tests. One
 * instance backs both the enumeration probe and the opened session, which is
 * how {@code TestCodenameOneImplementation.setCameraImpl} hands the same
 * backend to every {@code Camera} call.
 */
class FakeCameraImpl extends CameraImpl {
    CameraInfo[] cameras = {
        new CameraInfo("back", CameraFacing.BACK, null, null, true, true),
        new CameraInfo("front", CameraFacing.FRONT, null, null, false, true)
    };
    IOException openFailure;
    String openedCameraId;
    int openCount;
    int closeCount;
    boolean captureAudio;
    FrameListener frameListener;
    FlashMode lastFlashMode;
    Boolean previewMirrored;
    ScaleType previewScaleType;

    void deliver(byte[] jpeg) {
        FrameListener listener = frameListener;
        if (listener != null) {
            listener.onFrame(new CameraFrame(jpeg, null, 4, 3, 0, 1L,
                    FrameFormat.JPEG));
        }
    }

    @Override
    public CameraInfo[] enumerateCameras() {
        return cameras;
    }

    @Override
    public void open(String cameraId, CameraSessionOptions opts) throws IOException {
        if (openFailure != null) {
            throw openFailure;
        }
        openedCameraId = cameraId;
        captureAudio = opts != null && opts.isCaptureAudio();
        openCount++;
    }

    @Override
    public PeerComponent createPreviewPeer() {
        return null;
    }

    @Override
    public void takePhoto(PhotoCaptureOptions opts, AsyncResource<CapturedPhoto> result) {
        result.complete(new CapturedPhoto(new byte[] {1}, "file://photo.jpg", 4, 3));
    }

    @Override
    public void startVideoRecording(String filePath, boolean audio) {
    }

    @Override
    public void stopVideoRecording(AsyncResource<String> result) {
        if (result != null) {
            result.complete("file://video.mp4");
        }
    }

    @Override
    public void setFrameListener(FrameListener listener, FrameFormat format, int maxFps) {
        frameListener = listener;
    }

    @Override
    public void setPreviewMirrored(boolean mirrored) {
        previewMirrored = Boolean.valueOf(mirrored);
    }

    @Override
    public void setPreviewScaleType(ScaleType scaleType) {
        previewScaleType = scaleType;
    }

    @Override
    public void setFlashMode(FlashMode mode) {
        lastFlashMode = mode;
    }

    @Override
    public void setZoom(float ratio) {
    }

    @Override
    public void focus(float xNorm, float yNorm) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void close() {
        closeCount++;
    }
}
