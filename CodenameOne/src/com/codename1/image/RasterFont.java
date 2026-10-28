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
package com.codename1.image;

/// The seam through which [RasterGraphics#drawString(String, double, double)]
/// draws text: a font turns a string into an outline, and the graphics fills
/// it like any other path.
///
/// [TrueTypeFont] is the implementation that ships: TrueType fonts read from
/// bytes, with an embedded default. Another font engine plugs in here
/// without any change to the graphics.
@com.codename1.impl.SharedWithBackend
public interface RasterFont {
    /// Returns the outline of `text` with its baseline starting at `(x, y)`.
    ///
    /// #### Parameters
    ///
    /// - `text`: the string to lay out
    /// - `x`: left end of the baseline
    /// - `y`: the baseline
    ///
    /// #### Returns
    ///
    /// the glyph outlines, filled with the non-zero rule
    RasterPath getOutline(String text, double x, double y);

    /// Returns the advance width of `text`.
    ///
    /// #### Parameters
    ///
    /// - `text`: the string
    ///
    /// #### Returns
    ///
    /// the width in user-space units
    double stringWidth(String text);

    /// Returns the distance from the top of the line to the baseline.
    ///
    /// #### Returns
    ///
    /// the ascent in user-space units
    double getAscent();

    /// Returns the distance from the baseline to the bottom of the line.
    ///
    /// #### Returns
    ///
    /// the descent in user-space units
    double getDescent();
}
