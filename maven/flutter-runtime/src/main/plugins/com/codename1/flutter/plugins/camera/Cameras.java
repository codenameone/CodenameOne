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
import com.codename1.ui.Display;

import dart.async.Future;
import dart.core.DartList;

/** camera's availableCameras(), from Codename One's Camera.getCameras(); empty headless or without camera support. */
public final class Cameras {
    private Cameras() {
    }

    public static Future<DartList<CameraDescription>> availableCameras() {
        DartList<CameraDescription> out = new DartList<CameraDescription>();
        if (Display.isInitialized() && Camera.isSupported()) {
            CameraInfo[] all = Camera.getCameras();
            for (int i = 0; all != null && i < all.length; i++) {
                CameraDescription d = new CameraDescription();
                d.info = all[i];
                d.name = all[i].getId();
                d.lensDirection = all[i].getFacing() == CameraFacing.FRONT ? CameraLensDirection.front
                        : all[i].getFacing() == CameraFacing.EXTERNAL ? CameraLensDirection.external
                        : CameraLensDirection.back;
                d.sensorOrientation = 90;
                out.add(d);
            }
        }
        return Future.value(out);
    }
}
