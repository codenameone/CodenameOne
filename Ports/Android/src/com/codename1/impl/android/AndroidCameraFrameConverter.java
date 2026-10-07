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
package com.codename1.impl.android;

import java.nio.ByteBuffer;

/** Packs camera YUV_420_888 planes without assuming a device-specific layout. */
final class AndroidCameraFrameConverter {
    private AndroidCameraFrameConverter() {
    }

    static byte[] toNV21(int width, int height,
                         ByteBuffer y, int yRowStride, int yPixelStride,
                         ByteBuffer u, int uRowStride, int uPixelStride,
                         ByteBuffer v, int vRowStride, int vPixelStride) {
        long pixels = (long) width * height;
        if (width <= 0 || height <= 0 || (width & 1) != 0 || (height & 1) != 0
                || pixels + pixels / 2 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid YUV dimensions");
        }
        byte[] result = new byte[(int) (pixels + pixels / 2)];
        copyPlane(y, yRowStride, yPixelStride, width, height, result, 0, 1);
        // NV21 interleaves V then U. Planes can be planar, overlapping, padded,
        // sliced, or read-only; absolute reads preserve their positions.
        copyPlane(v, vRowStride, vPixelStride, width / 2, height / 2,
                result, (int) pixels, 2);
        copyPlane(u, uRowStride, uPixelStride, width / 2, height / 2,
                result, (int) pixels + 1, 2);
        return result;
    }

    private static void copyPlane(ByteBuffer plane, int rowStride, int pixelStride,
                                  int width, int height, byte[] out,
                                  int offset, int step) {
        long rowBytes = (long) (width - 1) * pixelStride + 1;
        long end = (long) plane.position() + (long) (height - 1) * rowStride + rowBytes;
        if (rowStride <= 0 || pixelStride <= 0 || rowStride < rowBytes
                || end > plane.limit()) {
            throw new IllegalArgumentException("Truncated or invalid YUV plane");
        }
        int base = plane.position();
        for (int row = 0; row < height; row++) {
            int input = base + row * rowStride;
            for (int col = 0; col < width; col++) {
                out[offset] = plane.get(input + col * pixelStride);
                offset += step;
            }
        }
    }
}
