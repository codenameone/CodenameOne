/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.camera;

import com.codename1.camera.Camera;
import com.codename1.camera.CameraFacing;
import com.codename1.camera.CameraInfo;
import com.codename1.camera.CameraSession;
import com.codename1.camera.CameraSessionOptions;
import com.codename1.camera.CapturedPhoto;
import com.codename1.camera.VideoRecording;
import com.codename1.flutter.foundation.ValueNotifier;
import com.codename1.flutter.plugins.crossfile.XFile;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;

import dart.async.Completer;
import dart.async.Future;

/**
 * camera's CameraController over a Codename One {@link CameraSession}: the preview
 * is the session's CameraView, takePicture its takePhoto and video its
 * startVideoRecording. The resolution preset picks a preview size; imageFormatGroup
 * is accepted and photos are always JPEG. Without a display, or where the platform
 * has no camera API, initialize fails with CameraException("cameraNotAvailable").
 */
public class CameraController extends ValueNotifier<CameraValue> {
    private final CameraDescription description;
    private final ResolutionPreset preset;
    private boolean enableAudio = true;
    private CameraSession session;
    private VideoRecording recording;

    public CameraController(CameraDescription description, ResolutionPreset preset) {
        super(new CameraValue());
        this.description = description;
        this.preset = preset;
    }

    public void enableAudio(Boolean v) {
        enableAudio = v == null || v.booleanValue();
    }

    public void imageFormatGroup(ImageFormatGroup v) {
    }

    public CameraDescription description() {
        return description;
    }

    CameraSession session() {
        return session;
    }

    public Future<Object> initialize() {
        if (!Display.isInitialized() || !Camera.isSupported()) {
            return error("cameraNotAvailable", "no camera on this platform");
        }
        CameraInfo info = description == null ? null : description.info;
        if (info == null) {
            info = Camera.getDefault(description != null && description.lensDirection == CameraLensDirection.front
                    ? CameraFacing.FRONT : CameraFacing.BACK);
        }
        if (info == null) {
            return error("cameraNotAvailable", "no camera matches " + description);
        }
        int[] size = size(preset);
        session = Camera.open(info, new CameraSessionOptions().previewSize(size[0], size[1]).captureAudio(enableAudio));
        if (session == null) {
            return error("CameraAccessDenied", "the camera could not be opened");
        }
        CameraValue v = value().copy();
        v.initialized = true;
        v.previewWidth = size[0];
        v.previewHeight = size[1];
        value(v);
        return Future.value(null);
    }

    private static int[] size(ResolutionPreset p) {
        if (p == ResolutionPreset.low) {
            return new int[] {320, 240};
        }
        if (p == ResolutionPreset.medium) {
            return new int[] {720, 480};
        }
        if (p == ResolutionPreset.high) {
            return new int[] {1280, 720};
        }
        if (p == ResolutionPreset.veryHigh) {
            return new int[] {1920, 1080};
        }
        return new int[] {3840, 2160};
    }

    private <T> Future<T> error(String code, String why) {
        CameraValue v = value().copy();
        v.errorDescription = why;
        value(v);
        return Future.error(new CameraException(code, why));
    }

    public Future<XFile> takePicture() {
        if (session == null) {
            return error("Uninitialized CameraController", "takePicture() was called on an uninitialized CameraController.");
        }
        final Completer<XFile> done = new Completer<XFile>();
        CameraValue v = value().copy();
        v.takingPicture = true;
        value(v);
        session.takePhoto().ready(new SuccessCallback<CapturedPhoto>() {
            @Override
            public void onSucess(CapturedPhoto p) {
                CameraValue v = value().copy();
                v.takingPicture = false;
                value(v);
                String path = p.getFilePath();
                if (path == null) {
                    path = FileSystemStorage.getInstance().getAppHomePath() + "cn1_camera_" + System.currentTimeMillis() + ".jpg";
                    try {
                        java.io.OutputStream out = FileSystemStorage.getInstance().openOutputStream(path);
                        out.write(p.getJpegBytes());
                        out.close();
                    } catch (java.io.IOException e) {
                        done.completeError(new CameraException("captureFailure", e.getMessage()));
                        return;
                    }
                }
                done.complete(new XFile(path));
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable t) {
                CameraValue v = value().copy();
                v.takingPicture = false;
                value(v);
                done.completeError(new CameraException("captureFailure", String.valueOf(t.getMessage())));
            }
        });
        return done.future();
    }

    public Future<Object> startVideoRecording(Object onAvailable) {
        if (session == null) {
            return error("Uninitialized CameraController", "startVideoRecording() was called on an uninitialized CameraController.");
        }
        String path = FileSystemStorage.getInstance().getAppHomePath() + "cn1_video_" + System.currentTimeMillis() + ".mp4";
        recording = session.startVideoRecording(path);
        CameraValue v = value().copy();
        v.recording = true;
        value(v);
        return Future.value(null);
    }

    public Future<XFile> stopVideoRecording() {
        if (recording == null) {
            return error("No video is recording", "stopVideoRecording() was called with no recording.");
        }
        final Completer<XFile> done = new Completer<XFile>();
        final VideoRecording r = recording;
        recording = null;
        r.stopAndAwait().ready(new SuccessCallback<String>() {
            @Override
            public void onSucess(String path) {
                CameraValue v = value().copy();
                v.recording = false;
                value(v);
                done.complete(new XFile(path == null ? r.getRequestedPath() : path));
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable t) {
                CameraValue v = value().copy();
                v.recording = false;
                value(v);
                done.completeError(new CameraException("videoRecordingFailed", String.valueOf(t.getMessage())));
            }
        });
        return done.future();
    }

    public Future<Object> setFlashMode(FlashMode mode) {
        CameraValue v = value().copy();
        v.flashMode = mode;
        value(v);
        if (session != null) {
            session.setFlashMode(mode == FlashMode.always ? com.codename1.camera.FlashMode.ON
                    : mode == FlashMode.torch ? com.codename1.camera.FlashMode.TORCH
                    : mode == FlashMode.off ? com.codename1.camera.FlashMode.OFF
                    : com.codename1.camera.FlashMode.AUTO);
        }
        return Future.value(null);
    }

    public Future<Object> setZoomLevel(double zoom) {
        if (session != null) {
            session.setZoom((float) zoom);
        }
        return Future.value(null);
    }

    public Future<Object> pausePreview() {
        if (session != null) {
            session.pause();
        }
        return Future.value(null);
    }

    public Future<Object> resumePreview() {
        if (session != null) {
            session.resume();
        }
        return Future.value(null);
    }

    @Override
    public void dispose() {
        if (recording != null) {
            recording.stop();
            recording = null;
        }
        if (session != null) {
            session.close();
            session = null;
        }
        super.dispose();
    }
}
