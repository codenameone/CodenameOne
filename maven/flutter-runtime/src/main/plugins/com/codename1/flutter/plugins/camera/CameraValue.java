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

import com.codename1.flutter.rendering.Size;

/** camera's CameraValue: an immutable snapshot, copied on every change. */
public class CameraValue {
    boolean initialized;
    boolean recording;
    boolean takingPicture;
    double previewWidth;
    double previewHeight;
    FlashMode flashMode = FlashMode.auto;
    String errorDescription;

    CameraValue() {
    }

    CameraValue copy() {
        CameraValue v = new CameraValue();
        v.initialized = initialized;
        v.recording = recording;
        v.takingPicture = takingPicture;
        v.previewWidth = previewWidth;
        v.previewHeight = previewHeight;
        v.flashMode = flashMode;
        v.errorDescription = errorDescription;
        return v;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isRecordingVideo() {
        return recording;
    }

    public boolean isTakingPicture() {
        return takingPicture;
    }

    public Size previewSize() {
        return previewWidth > 0 ? new Size(previewWidth, previewHeight) : null;
    }

    /** The plugin's convention: the landscape sensor's width over height. */
    public double aspectRatio() {
        if (previewWidth <= 0 || previewHeight <= 0) {
            return 1.0;
        }
        return Math.max(previewWidth, previewHeight) / Math.min(previewWidth, previewHeight);
    }

    public FlashMode flashMode() {
        return flashMode;
    }

    public String errorDescription() {
        return errorDescription;
    }

    public boolean hasError() {
        return errorDescription != null;
    }
}
