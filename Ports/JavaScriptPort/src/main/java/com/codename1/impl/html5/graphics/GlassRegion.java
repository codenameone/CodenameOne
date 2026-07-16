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
 * Records a complete named Liquid Glass material for ordered host-side replay.
 * The host applies the native affine colour transform, browser Gaussian blur,
 * rounded shape mask, edge refraction and specular rim to the surface's own
 * already-painted pixels.
 */
public final class GlassRegion implements ExecutableOp {
    private final int x, y, width, height;
    private final float radius, cornerRadius, saturation, scale, offset, refraction, specular;

    public GlassRegion(int x, int y, int width, int height, float radius, float cornerRadius,
            float saturation, float scale, float offset, float refraction, float specular) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.radius = radius;
        this.cornerRadius = cornerRadius;
        this.saturation = saturation;
        this.scale = scale;
        this.offset = offset;
        this.refraction = refraction;
        this.specular = specular;
    }

    @Override
    public void execute(CanvasRenderingContext2D context) {
        if (!(context instanceof SurfaceCommandRecorder)) {
            return;
        }
        ((SurfaceCommandRecorder) context).glassSelfRegion(x, y, width, height,
                radius, cornerRadius, saturation, scale, offset, refraction, specular);
    }

    @Override
    public String getDescription() {
        return "GlassRegion";
    }
}
