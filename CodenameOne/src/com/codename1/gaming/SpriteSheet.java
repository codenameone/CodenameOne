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
package com.codename1.gaming;

import com.codename1.ui.Image;

/// Slices a single texture atlas image into a grid of equally sized frames.
///
/// Frames are addressed either by linear index (row major, starting at 0) or by
/// column/row. Each frame is cut once with
/// `com.codename1.ui.Image#subImage(int, int, int, int, boolean)` and then cached,
/// because cutting a sub image copies pixel data and is far too expensive to repeat
/// every animation frame.
public class SpriteSheet {
    private final Image sheet;
    private final int frameWidth;
    private final int frameHeight;
    private final int columns;
    private final int rows;
    private final Image[] cache;

    /// Creates a sprite sheet over the given image.
    ///
    /// #### Parameters
    ///
    /// - `sheet`: the atlas image
    ///
    /// - `frameWidth`: width of a single frame in pixels
    ///
    /// - `frameHeight`: height of a single frame in pixels
    public SpriteSheet(Image sheet, int frameWidth, int frameHeight) {
        if (sheet == null) {
            throw new IllegalArgumentException("sheet is null");
        }
        if (frameWidth <= 0 || frameHeight <= 0) {
            throw new IllegalArgumentException("frame size must be positive");
        }
        this.sheet = sheet;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.columns = sheet.getWidth() / frameWidth;
        this.rows = sheet.getHeight() / frameHeight;
        this.cache = new Image[columns * rows];
    }

    /// The number of frame columns in the sheet.
    public int getColumns() {
        return columns;
    }

    /// The number of frame rows in the sheet.
    public int getRows() {
        return rows;
    }

    /// The total number of frames (columns times rows).
    public int getFrameCount() {
        return cache.length;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    /// Returns the frame at the given linear index, cutting and caching it on first
    /// access.
    public Image getFrame(int index) {
        if (index < 0 || index >= cache.length) {
            throw new IndexOutOfBoundsException("frame index " + index + " out of 0.." + (cache.length - 1));
        }
        Image img = cache[index];
        if (img == null) {
            int col = index % columns;
            int row = index / columns;
            img = sheet.subImage(col * frameWidth, row * frameHeight, frameWidth, frameHeight, true);
            cache[index] = img;
        }
        return img;
    }

    /// Returns the frame at the given column and row.
    public Image getFrame(int col, int row) {
        return getFrame(row * columns + col);
    }
}
