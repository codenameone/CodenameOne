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
package javafx.scene.image;

import javafx.scene.paint.Color;

/// Writes the pixels of an image. It may be written to from any thread,
/// as in JavaFX; what shows the image follows on the application
/// thread. The buffer forms of JavaFX, which need `java.nio`, are not
/// part of this layer.
public interface PixelWriter {

    /// Sets a pixel from `0xAARRGGBB`, not premultiplied.
    void setArgb(int x, int y, int argb);

    /// Sets a pixel from a colour.
    void setColor(int x, int y, Color c);

    /// Copies a rectangle of pixels from a reader.
    void setPixels(int dstx, int dsty, int w, int h, PixelReader reader, int srcx, int srcy);
}
