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
package com.codename1.python.runtime;

/// One image of PIL's `_cn1image` module: `w` by `h` pixels of `bands`
/// interleaved bytes (1 for "L", 2 for "LA", 3 for "RGB", 4 for "RGBA").
/// It is what a [PyImageEngine] receives and returns.
public final class PyImageData {
    /// Width in pixels.
    public final int w;
    /// Height in pixels.
    public final int h;
    /// Bytes per pixel.
    public final int bands;
    /// The pixels, row by row, `w * h * bands` bytes.
    public final byte[] d;

    /// A zero-filled image.
    public PyImageData(int w, int h, int bands) {
        this.w = w;
        this.h = h;
        this.bands = bands;
        this.d = new byte[w * h * bands];
    }
}
