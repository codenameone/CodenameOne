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

import com.codename1.ui.geom.Dimension;

/// Describes a physical camera available on the device.
///
/// Returned by `Camera#getCameras()` and `Camera#getDefault(CameraFacing)`.
/// Instances are immutable and may be passed to `Camera#open(CameraInfo, CameraSessionOptions)`.
public final class CameraInfo {
    private final String id;
    private final CameraFacing facing;
    private final Dimension[] photoSizes;
    private final Dimension[] previewSizes;
    private final boolean hasFlash;
    private final boolean hasAutoFocus;

    /// Used by platform implementations. Application code obtains instances through
    /// `Camera#getCameras()`.
    public CameraInfo(String id, CameraFacing facing,
                      Dimension[] photoSizes, Dimension[] previewSizes,
                      boolean hasFlash, boolean hasAutoFocus) {
        this.id = id;
        this.facing = facing;
        this.photoSizes = photoSizes == null ? new Dimension[0] : photoSizes;
        this.previewSizes = previewSizes == null ? new Dimension[0] : previewSizes;
        this.hasFlash = hasFlash;
        this.hasAutoFocus = hasAutoFocus;
    }

    /// Opaque, platform-specific identifier. Stable for the lifetime of the process.
    public String getId() {
        return id;
    }

    public CameraFacing getFacing() {
        return facing;
    }

    /// Supported still-photo resolutions, largest first. May be empty on platforms
    /// (e.g. the JavaSE simulator) that don't expose a discrete list.
    public Dimension[] getPhotoSizes() {
        return photoSizes;
    }

    /// Supported preview/frame-stream resolutions. May be empty on platforms that
    /// don't expose a discrete list.
    public Dimension[] getPreviewSizes() {
        return previewSizes;
    }

    public boolean hasFlash() {
        return hasFlash;
    }
    public boolean hasAutoFocus() {
        return hasAutoFocus;
    }
}
