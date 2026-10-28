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

/// Pure-Java images: pixels, drawing, resampling and PNG/JPEG codecs that
/// need no display, so the same code runs on a device, in the simulator and
/// on a Codename One server.
///
/// - [RasterImage] holds non-premultiplied ARGB pixels and rotates, flips,
///   crops and resizes them.
/// - [RasterGraphics] draws antialiased lines, rectangles, ellipses, arcs,
///   polygons and Bezier paths ([RasterPath]), with transforms, clipping,
///   alpha, gradients ([RasterGradient]) and images. Text goes through the
///   [RasterFont] seam.
/// - [Resampler] is Pillow's resampling convolution, byte for byte.
/// - [PngCodec] and [JpegCodec] read and write files.
///
/// A server builds a social card like this:
///
/// ```java
/// RasterImage card = new RasterImage(1200, 630);
/// RasterGraphics g = card.createGraphics();
/// g.setPaint(RasterGradient.linear(0, 0, 0, 630,
///         new float[] {0, 1}, new int[] {0xff1e3a8a, 0xff0ea5e9}));
/// g.fillRect(0, 0, 1200, 630);
/// g.setPaint(null);
/// g.setColor(0xccffffff);
/// g.fillRoundRect(60, 60, 1080, 510, 48, 48);
/// byte[] png = PngCodec.encode(card);
/// ```
///
/// Every class is shared with the backend runtime, and depends on nothing
/// but the subset of the JDK every Codename One target provides.
package com.codename1.image;
