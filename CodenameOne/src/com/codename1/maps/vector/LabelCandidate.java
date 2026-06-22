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

/// A single label to draw, captured at tile-decode time. Its anchor is stored
/// in integer-zoom world pixels (256px tiles) so the engine can convert it to
/// the screen at any fractional camera zoom without re-walking the tile.
final class LabelCandidate {

    final String text;
    final double worldX;
    final double worldY;
    final int tileZoom;
    final int textColor;
    final int haloColor;
    final double sizePx;

    LabelCandidate(String text, double worldX, double worldY, int tileZoom,
                   int textColor, int haloColor, double sizePx) {
        this.text = text;
        this.worldX = worldX;
        this.worldY = worldY;
        this.tileZoom = tileZoom;
        this.textColor = textColor;
        this.haloColor = haloColor;
        this.sizePx = sizePx;
    }
}
