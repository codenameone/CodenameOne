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
package com.codename1.presentation;

import com.codename1.ui.geom.GeneralPath;
import com.codename1.wordprocessing.LayoutItem;
import com.codename1.wordprocessing.Measurer;
import com.codename1.wordprocessing.Picture;
import java.util.List;

/// What `SlideRenderer` paints a slide with, in slide points: one implementation for the
/// screen and one for PDF, so both draw exactly the same shapes and text.
interface SlidePainter {
    /// The measurer text is laid out with.
    Measurer measurer();

    /// Starts an element: rotates about (`cx`, `cy`), moves by (`dx`, `dy`), scales about the
    /// centre and multiplies the opacity. `clip` is the box an entrance wipe reveals, or null.
    void push(float cx, float cy, float rotation, float dx, float dy, float scale, float alpha,
            float[] clip);

    /// Ends what `push` started.
    void pop();

    /// Fills a path with a solid color.
    void fill(GeneralPath path, int color, float alpha);

    /// Fills a path with a linear gradient.
    void gradient(GeneralPath path, int from, int to, float angle, float alpha, float x,
            float y, float w, float h);

    /// Strokes a path.
    void stroke(GeneralPath path, LineStyle line);

    /// Draws a picture into a box, clipped to it, showing the uncropped part.
    void picture(Picture p, float x, float y, float w, float h, float cropL, float cropT,
            float cropR, float cropB, boolean flipH, boolean flipV, float alpha);

    /// Draws laid out text items with their page's top left at (`x`, `y`), scaled.
    void text(List<LayoutItem> items, Measurer measurer, float x, float y, float scale);

    /// Draws a chart into a box.
    void chart(ChartElement chart, float x, float y, float w, float h);
}
