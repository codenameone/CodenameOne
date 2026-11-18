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
package com.codename1.impl.pdf;

/// What the PDF renderer draws on. Every coordinate is in device pixels,
/// y down; colors are ARGB with alpha already folded in.
public interface PdfDevice {
    /// Line caps and joins, as PDF numbers them.
    int CAP_BUTT = 0;
    /// Round cap.
    int CAP_ROUND = 1;
    /// Square cap.
    int CAP_SQUARE = 2;

    /// Pushes the clip.
    void save();

    /// Pops the clip.
    void restore();

    /// Intersects the clip with `path`.
    void clip(PdfPath path, boolean evenOdd);

    /// Fills `path`.
    void fill(PdfPath path, boolean evenOdd, int argb);

    /// Strokes `path` with a line `width` pixels wide.
    void stroke(PdfPath path, float width, int cap, int join, float miterLimit, int argb);

    /// Draws an image whose pixel `(x, y)` (row 0 at the top) lands at
    /// `(m[0] x + m[2] y + m[4], m[1] x + m[3] y + m[5])`.
    void image(int[] argb, int width, int height, double[] m);

    /// Decodes a baseline or progressive JPEG to ARGB, writing its size to
    /// `size`; null when it cannot.
    int[] decodeJpeg(byte[] data, int[] size);

    /// Draws text in a font the document does not embed and that has no
    /// outlines of its own: `family` is 0 sans, 1 serif, 2 monospace;
    /// `m` maps a 1-unit em, baseline at the origin, y up, to the device.
    void text(String text, int family, boolean bold, boolean italic, double[] m, int argb);

    /// Starts drawing into a new transparent layer over the device
    /// rectangle `(x, y, width, height)`; everything drawn until
    /// [#endLayer()] goes into it, in the same device coordinates, and the
    /// clip is kept. Layers nest. Used for blend modes, soft masks and
    /// transparency groups, which the renderer composites itself.
    void beginLayer(int x, int y, int width, int height);

    /// Ends the innermost layer and returns its pixels, ARGB, row by row.
    int[] endLayer();

    /// The pixels of the current surface (the innermost layer, or the
    /// page) in the device rectangle `(x, y, width, height)`, or null when
    /// the device cannot read them back; then blend modes draw as normal.
    int[] pixels(int x, int y, int width, int height);
}
