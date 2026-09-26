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
import com.codename1.impl.html5.HTML5Implementation.NativeImage;

/**
 * In-place Graphics.colorMatrixRegion. Records
 * {@link SurfaceCommandRecorder#OP_COLOR_MATRIX_SELF_REGION}: the host reads the
 * region back, runs com.codename1.ui.plaf.ColorMatrixBlend's per-pixel math over
 * it and draws the result back through the current clip.
 *
 * <p>The mask is resolved when the op EXECUTES, not when it is queued, the same
 * way {@link DrawImage} resolves its image: a mutable mask is flushed at that
 * point so the host holds its pixels before the op that samples them.</p>
 */
public class ColorMatrixRegion implements ExecutableOp {
    final int x, y, w, h;
    final float[] matrix;
    final NativeImage mask;
    final float cornerRadius, amount;

    public ColorMatrixRegion(int x, int y, int w, int h, float[] matrix, NativeImage mask,
                             float cornerRadius, float amount) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        // Copied: the op runs after the caller returns and may reuse its array.
        float[] m = new float[12];
        if (matrix != null) {
            System.arraycopy(matrix, 0, m, 0, Math.min(12, matrix.length));
        }
        this.matrix = m;
        this.mask = mask;
        this.cornerRadius = cornerRadius;
        this.amount = amount;
    }

    @Override
    public void execute(CanvasRenderingContext2D context) {
        if (!(context instanceof SurfaceCommandRecorder)) {
            return;
        }
        SurfaceCommandRecorder recorder = (SurfaceCommandRecorder) context;
        if (mask == null) {
            recorder.colorMatrixSelfRegion(x, y, w, h, matrix, cornerRadius, amount,
                    SurfaceCommandRecorder.COLOR_MATRIX_MASK_NONE, 0, null);
        } else {
            mask.recordColorMatrixRegion(recorder, x, y, w, h, matrix, cornerRadius, amount);
        }
    }

    @Override
    public String getDescription() {
        return "ColorMatrixRegion";
    }
}
