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

import com.codename1.wordprocessing.DocumentLayout;
import com.codename1.wordprocessing.LayoutItem;
import com.codename1.wordprocessing.LayoutPage;
import com.codename1.wordprocessing.Measurer;

/// The text of a shape laid out in its box: the word processing layout, where its first line
/// starts, and how much it was shrunk to fit.
///
/// `SlideRenderer` paints from this, and a `SlideView` editing the text in place uses it to
/// put its editor exactly over the painted words.
public final class TextBoxLayout {
    /// The layout, in points, of a page as wide as the box less nothing (the insets are the
    /// page margins); multiply by `shrink` to get box points.
    public final DocumentLayout layout;

    /// Where the layout's top is in the box, in box points: the anchor's offset.
    public final float offsetY;

    /// The factor the text is drawn at, 1 unless the frame shrinks text to fit.
    public final float shrink;

    /// The height the text uses in box points, insets included.
    public final float usedHeight;

    private TextBoxLayout(DocumentLayout layout, float offsetY, float shrink, float usedHeight) {
        this.layout = layout;
        this.offsetY = offsetY;
        this.shrink = shrink;
        this.usedHeight = usedHeight;
    }

    /// Lays a frame out in a box.
    ///
    /// #### Parameters
    ///
    /// - `frame`: the text
    ///
    /// - `width`: the box width in points
    ///
    /// - `height`: the box height in points
    ///
    /// - `measurer`: measures text; the painter must use the same kind
    ///
    /// #### Returns
    ///
    /// the layout
    public static TextBoxLayout of(TextFrame frame, float width, float height,
            Measurer measurer) {
        float shrink = 1;
        DocumentLayout l = new DocumentLayout(frame.toDocument(width, height), measurer);
        float used = used(l, frame);
        if (TextFrame.FIT_SHRINK.equals(frame.autofit) && used > height && used > 0) {
            for (int i = 0; i < 3 && used * shrink > height; i++) {
                shrink = Math.max(0.25f, shrink * height / (used * shrink));
                l = new DocumentLayout(frame.toDocument(width / shrink, height / shrink),
                        measurer);
                used = used(l, frame);
            }
        }
        float u = used * shrink;
        float off = 0;
        if (u < height) {
            if (TextFrame.MIDDLE.equals(frame.anchor)) {
                off = (height - u) / 2;
            } else if (TextFrame.BOTTOM.equals(frame.anchor)) {
                off = height - u;
            }
        }
        return new TextBoxLayout(l, off, shrink, u);
    }

    private static float used(DocumentLayout l, TextFrame frame) {
        float bottom = frame.top;
        if (l.getPageCount() > 0) {
            LayoutPage p = l.getPage(0);
            for (LayoutItem it : p.getItems()) {
                bottom = Math.max(bottom, it.y + it.height);
            }
        }
        return bottom + frame.bottom;
    }
}
