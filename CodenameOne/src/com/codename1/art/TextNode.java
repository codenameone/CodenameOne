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
package com.codename1.art;

import java.util.ArrayList;
import java.util.List;

/// Styled text. The first line's baseline starts at `x, y`; with a box width set, lines wrap
/// at that width and align within it.
public class TextNode extends ArtNode {
    /// Lines start at `x`.
    public static final int ALIGN_LEFT = 0;
    /// Lines are centered on the box (or on `x` with no box).
    public static final int ALIGN_CENTER = 1;
    /// Lines end at the box's right edge (or at `x` with no box).
    public static final int ALIGN_RIGHT = 2;

    private final List<TextRun> runs = new ArrayList<TextRun>();
    private double x;
    private double y;
    private double boxWidth;
    private int align = ALIGN_LEFT;
    private float lineHeight = 1.2f;

    /// Creates an empty text node at the origin.
    public TextNode() {
    }

    /// Creates a text node with one run.
    ///
    /// #### Parameters
    ///
    /// - `text`: the characters
    /// - `size`: the font size
    /// - `color`: the color as `0xAARRGGBB`
    public TextNode(String text, float size, int color) {
        runs.add(new TextRun(text, null, size, new SolidPaint(color)));
    }

    /// The runs. The list is live.
    public List<TextRun> getRuns() {
        return runs;
    }

    /// All the characters of every run.
    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < runs.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            sb.append(runs.get(i).getText());
        }
        return sb.toString();
    }

    /// Baseline origin x.
    public double getX() {
        return x;
    }

    /// Baseline origin y.
    public double getY() {
        return y;
    }

    /// Sets the baseline origin.
    ///
    /// #### Parameters
    ///
    /// - `x`: x
    /// - `y`: y of the first baseline
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /// The wrapping width, or 0 for none.
    public double getBoxWidth() {
        return boxWidth;
    }

    /// Sets the wrapping width.
    ///
    /// #### Parameters
    ///
    /// - `boxWidth`: the width; 0 for no wrapping
    public void setBoxWidth(double boxWidth) {
        this.boxWidth = boxWidth;
    }

    /// One of the `ALIGN_` constants.
    public int getAlign() {
        return align;
    }

    /// Sets the alignment.
    ///
    /// #### Parameters
    ///
    /// - `align`: one of the `ALIGN_` constants
    public void setAlign(int align) {
        this.align = align;
    }

    /// Line height as a multiple of the font size.
    public float getLineHeight() {
        return lineHeight;
    }

    /// Sets the line height.
    ///
    /// #### Parameters
    ///
    /// - `lineHeight`: a multiple of the font size
    public void setLineHeight(float lineHeight) {
        this.lineHeight = lineHeight;
    }

    /// The largest run size, or 12.
    float maxSize() {
        float m = 0;
        for (int i = 0; i < runs.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            m = Math.max(m, runs.get(i).getSize());
        }
        return m <= 0 ? 12 : m;
    }

    @Override
    public double[] getLocalBounds() {
        return TextLayout.bounds(this);
    }

    @Override
    public ArtNode copy() {
        TextNode t = new TextNode();
        for (int i = 0; i < runs.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            t.runs.add(runs.get(i).copy());
        }
        t.x = x;
        t.y = y;
        t.boxWidth = boxWidth;
        t.align = align;
        t.lineHeight = lineHeight;
        return copyBase(t);
    }
}
