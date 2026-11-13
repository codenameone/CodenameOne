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

/**
 * The camera port's device-independent arithmetic: packing YUV_420_888 planes
 * without assuming a device-specific layout, and choosing the analysis size.
 */
final class AndroidCameraFrameConverter {
    private AndroidCameraFrameConverter() {
    }

    /** Long side of the analysis size an NV21 session gets when it asks for none. */
    static final int NV21_ANALYSIS_LONG_SIDE = 1440;
    /** Short side of the analysis size an NV21 session gets when it asks for none. */
    static final int NV21_ANALYSIS_SHORT_SIDE = 1080;
    /** Long side of the analysis size any other session gets when it asks for none. */
    static final int OTHER_ANALYSIS_LONG_SIDE = 640;
    /** Short side of the analysis size any other session gets when it asks for none. */
    static final int OTHER_ANALYSIS_SHORT_SIDE = 480;

    /**
     * Picks the frame analysis size as {width, height}. A request naming both
     * dimensions is returned unchanged. Anything else gets the default for the
     * kind of frame asked for, turned to the display's orientation: half a
     * request (one dimension zero) cannot be completed without knowing the
     * aspect ratio the caller had in mind, and pairing it with one default
     * dimension produced sizes such as 1920x480.
     */
    static int[] analysisSize(int requestedWidth, int requestedHeight,
                              boolean nv21Frames, boolean portrait) {
        if (requestedWidth > 0 && requestedHeight > 0) {
            return new int[] {requestedWidth, requestedHeight};
        }
        int longSide = nv21Frames ? NV21_ANALYSIS_LONG_SIDE : OTHER_ANALYSIS_LONG_SIDE;
        int shortSide = nv21Frames ? NV21_ANALYSIS_SHORT_SIDE : OTHER_ANALYSIS_SHORT_SIDE;
        return portrait ? new int[] {shortSide, longSide} : new int[] {longSide, shortSide};
    }

    static byte[] toNV21(int width, int height,
                         ByteBuffer y, int yRowStride, int yPixelStride,
                         ByteBuffer u, int uRowStride, int uPixelStride,
                         ByteBuffer v, int vRowStride, int vPixelStride) {
        return toNV21(width, height, y, yRowStride, yPixelStride,
                u, uRowStride, uPixelStride, v, vRowStride, vPixelStride, null);
    }

    /**
     * As above, writing into {@code reuse} when it is exactly the size the
     * frame needs and allocating otherwise. A 1440x1080 frame is 2.3 MB, so a
     * session hands back the array it was given last time and the port itself
     * allocates none per frame; a consumer that keeps the pixels still makes
     * its own copy. Every byte of the result is written, so nothing of the
     * previous frame is left.
     */
    static byte[] toNV21(int width, int height,
                         ByteBuffer y, int yRowStride, int yPixelStride,
                         ByteBuffer u, int uRowStride, int uPixelStride,
                         ByteBuffer v, int vRowStride, int vPixelStride,
                         byte[] reuse) {
        long pixels = (long) width * height;
        if (width <= 0 || height <= 0 || (width & 1) != 0 || (height & 1) != 0
                || pixels + pixels / 2 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid YUV dimensions");
        }
        int length = (int) (pixels + pixels / 2);
        byte[] result = reuse != null && reuse.length == length ? reuse : new byte[length];
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
        if (pixelStride == 1 && step == 1) {
            // The luma plane, two thirds of the frame: whole rows at a time.
            // A duplicate keeps the caller's position where it was.
            ByteBuffer rows = plane.duplicate();
            for (int row = 0; row < height; row++) {
                rows.position(base + row * rowStride);
                rows.get(out, offset, width);
                offset += width;
            }
            return;
        }
        for (int row = 0; row < height; row++) {
            int input = base + row * rowStride;
            for (int col = 0; col < width; col++) {
                out[offset] = plane.get(input + col * pixelStride);
                offset += step;
            }
        }
    }
}
