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

import com.codename1.camera.CameraInfo;

/** camera's CameraDescription; a description from availableCameras carries its Codename One CameraInfo. */
public class CameraDescription {
    String name = "";
    CameraLensDirection lensDirection = CameraLensDirection.back;
    long sensorOrientation;
    CameraInfo info;

    public CameraDescription() {
    }

    public void name(String v) {
        name = v;
    }

    public void lensDirection(CameraLensDirection v) {
        lensDirection = v;
    }

    public void sensorOrientation(Long v) {
        sensorOrientation = v == null ? 0 : v.longValue();
    }

    public String name() {
        return name;
    }

    public CameraLensDirection lensDirection() {
        return lensDirection;
    }

    /** Codename One rotates the preview itself, so this is 90 for every camera, as most phones report. */
    public long sensorOrientation() {
        return sensorOrientation;
    }

    @Override
    public String toString() {
        return "CameraDescription(" + name + ", " + lensDirection + ", " + sensorOrientation + ")";
    }
}
