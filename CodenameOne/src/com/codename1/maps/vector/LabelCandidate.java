/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.vector;

import com.codename1.ui.Font;

/// A single label to draw, captured at tile-decode time. Its anchor is stored
/// in integer-zoom world pixels (256px tiles) so the engine can convert it to
/// the screen at any fractional camera zoom without re-walking the tile.
///
/// A road name also carries the road's geometry in [#path], in the same world
/// pixels, so the text can be laid along the road the way street maps show it
/// instead of floating horizontally over it.
final class LabelCandidate {

    final String text;
    final double worldX;
    final double worldY;
    final int tileZoom;
    final int textColor;
    final int haloColor;
    final double sizePx;
    /// Interleaved `x,y` world pixels of the line the label follows, or null
    /// for a label placed at a point. [#worldX]/[#worldY] is its midpoint.
    final double[] path;

    // What [LabelEngine] measured this text as, and the font it measured it
    // in: every label in view is measured on every frame, most of them only
    // to be found in the way of another.
    private Font measuredFont;
    private int measuredHeight;
    int textWidth;
    boolean perGlyph;
    /// The widths the text is laid along a line by, or null when it has only
    /// been measured whole.
    int[] cellWidths;

    /// Whether the measurements here are of this text in `font`, while it
    /// answered the height `height`.
    boolean measured(Font font, int height) {
        return measuredFont == font && measuredHeight == height; //NOPMD CompareObjectsWithEquals
    }

    void remember(Font font, int height, int width, boolean glyphs, int[] widths) {
        measuredFont = font;
        measuredHeight = height;
        textWidth = width;
        perGlyph = glyphs;
        cellWidths = widths;
    }

    LabelCandidate(String text, double worldX, double worldY, int tileZoom,
                   int textColor, int haloColor, double sizePx) {
        this(text, worldX, worldY, tileZoom, textColor, haloColor, sizePx, null);
    }

    LabelCandidate(String text, double worldX, double worldY, int tileZoom,
                   int textColor, int haloColor, double sizePx, double[] path) {
        this.text = text;
        this.worldX = worldX;
        this.worldY = worldY;
        this.tileZoom = tileZoom;
        this.textColor = textColor;
        this.haloColor = haloColor;
        this.sizePx = sizePx;
        this.path = path;
    }
}
