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
package com.codename1.media;

import com.codename1.ui.Image;

/// A single decoded video frame produced by a `VideoReader`. The pixels are exposed
/// as a Codename One native ARGB `int[]` (the same layout used by
/// `Image#getRGB()` and `Image#createImage(int[], int, int)`), and can also be
/// copied out as a tightly packed RGBA byte array for interoperability with native
/// pixel buffers.
///
/// Instances are immutable and safe to hand off between threads.
///
/// @author Shai Almog
///
/// #### Since
///
/// 8.0
public final class VideoFrame {
    private final int[] argb;
    private final int width;
    private final int height;
    private final long timestampMillis;

    /// Creates a new immutable frame. The supplied array is referenced directly (not
    /// copied) and must not be mutated by the caller after construction.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the pixels in ARGB order, length must equal `width * height`
    ///
    /// - `width`: the frame width in pixels
    ///
    /// - `height`: the frame height in pixels
    ///
    /// - `timestampMillis`: the presentation timestamp of this frame in milliseconds
    public VideoFrame(int[] argb, int width, int height, long timestampMillis) {
        if (argb == null || argb.length != width * height) {
            throw new IllegalArgumentException("argb length must equal width*height");
        }
        this.argb = argb;
        this.width = width;
        this.height = height;
        this.timestampMillis = timestampMillis;
    }

    /// The presentation timestamp of this frame within the source clip in milliseconds.
    public long getTimestampMillis() {
        return timestampMillis;
    }

    /// The frame width in pixels.
    public int getWidth() {
        return width;
    }

    /// The frame height in pixels.
    public int getHeight() {
        return height;
    }

    /// The raw pixels of this frame in ARGB order (the Codename One native layout). The
    /// returned array is the live backing array; treat it as read-only.
    ///
    /// #### Returns
    ///
    /// an `int[]` of length `getWidth() * getHeight()`
    public int[] getARGB() {
        return argb;
    }

    /// Copies the pixels of this frame into the supplied buffer as tightly packed RGBA
    /// bytes (`R, G, B, A` per pixel, row major). This is convenient when feeding the
    /// frame to native APIs that expect an RGBA byte buffer.
    ///
    /// #### Parameters
    ///
    /// - `dest`: a buffer of at least `getWidth() * getHeight() * 4` bytes
    public void getRGBA(byte[] dest) {
        int count = width * height;
        if (dest == null || dest.length < count * 4) {
            throw new IllegalArgumentException("dest must hold at least width*height*4 bytes");
        }
        int o = 0;
        for (int i = 0; i < count; i++) {
            int p = argb[i];
            dest[o++] = (byte) ((p >> 16) & 0xff);
            dest[o++] = (byte) ((p >> 8) & 0xff);
            dest[o++] = (byte) (p & 0xff);
            dest[o++] = (byte) ((p >> 24) & 0xff);
        }
    }

    /// Wraps this frame's pixels in a Codename One `Image` for display or further
    /// processing. A new image is created on every call.
    ///
    /// #### Returns
    ///
    /// an `Image` containing the frame pixels
    public Image toImage() {
        return Image.createImage(argb, width, height);
    }
}
