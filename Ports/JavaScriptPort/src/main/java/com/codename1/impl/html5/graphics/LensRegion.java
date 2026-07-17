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

package com.codename1.impl.html5.graphics;

import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 * In-place iOS 26 selection-drop lens for the Tabs glass indicator. Records
 * {@link SurfaceCommandRecorder#OP_LENS_SELF_REGION}: the host clips to the
 * region and applies the same per-pixel droplet shader as the JavaSE
 * Simulator: centre magnification, rim refraction, chromatic aberration,
 * luminance-keyed accent tint, saturation and subtle glass lighting.
 *
 * <p>Nothing crosses the worker&lt;-&gt;host barrier: the worker records the lens
 * parameters and the host applies them to the destination canvas during
 * ordered command replay.</p>
 */
public class LensRegion implements ExecutableOp {
    final int x, y, w, h;
    final float cornerRadius, magnify, aberration, tintStrength;
    final int tintColor;

    public LensRegion(int x, int y, int w, int h, float cornerRadius, float magnify,
                      float aberration, int tintColor, float tintStrength) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.cornerRadius = cornerRadius;
        this.magnify = magnify;
        this.aberration = aberration;
        this.tintColor = tintColor;
        this.tintStrength = tintStrength;
    }

    @Override
    public void execute(CanvasRenderingContext2D context) {
        if (!(context instanceof SurfaceCommandRecorder)) {
            return;
        }
        ((SurfaceCommandRecorder) context).lensSelfRegion(x, y, w, h, cornerRadius,
                magnify, aberration, tintColor, tintStrength);
    }

    @Override
    public String getDescription() {
        return "LensRegion";
    }
}
