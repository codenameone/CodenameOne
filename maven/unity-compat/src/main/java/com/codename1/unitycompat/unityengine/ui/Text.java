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
package com.codename1.unitycompat.unityengine.ui;

import com.codename1.unitycompat.unityengine.Component;
import com.codename1.unitycompat.unityengine.DrawCommand;

/// `UnityEngine.UI.Text`, the legacy text component: a string, drawn in
/// its rectangle at a size and an alignment.
///
/// The runtime has no font. It passes on what was asked for -- the
/// string, the size, the alignment, whether to wrap and whether to pick
/// the best size that fits -- and whatever paints lays the lines out with
/// the font it has. The font asset a scene names is not used: the text is
/// drawn in the platform's own, so its width differs from Unity's by
/// however much the two fonts do.
///
/// Rich text markup is drawn as written.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Text extends MaskableGraphic {
    private String text = "";
    private int fontSize = 14;
    private int fontStyle;
    private int alignment;
    private boolean bestFit;
    private int minSize = 10;
    private int maxSize = 40;
    /// False when a long line is left to run out of the rectangle.
    private boolean wrap = true;

    /// What a scene file sets.
    public void $setup(String value, int size, int style, int anchor, boolean fit, int min, int max,
            boolean wraps) {
        text = value == null ? "" : value;
        fontSize = size;
        fontStyle = style;
        alignment = anchor;
        bestFit = fit;
        minSize = min;
        maxSize = max;
        wrap = wraps;
    }

    @Override
    public Component $new() {
        return new Text();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        Text t = (Text) source;
        $setup(t.text, t.fontSize, t.fontStyle, t.alignment, t.bestFit, t.minSize, t.maxSize, t.wrap);
    }

    /// Describes this text to a draw command whose rectangle is already
    /// set. `scale` is surface pixels per canvas unit.
    public void $fill(DrawCommand d, float scale) {
        d.text = text;
        d.color = $drawColor();
        d.fontSize = fontSize * scale;
        d.bestFit = bestFit;
        d.minFontSize = minSize * scale;
        d.maxFontSize = maxSize * scale;
        d.alignment = alignment;
        d.fontStyle = fontStyle;
        d.wrap = wrap;
    }

    public String get_text() {
        return text;
    }

    public void set_text(String value) {
        text = value == null ? "" : value;
    }

    public int get_fontSize() {
        return fontSize;
    }

    public void set_fontSize(int value) {
        fontSize = value;
    }

    public int get_alignment() {
        return alignment;
    }

    public void set_alignment(int value) {
        alignment = value;
    }

    public int get_fontStyle() {
        return fontStyle;
    }

    public void set_fontStyle(int value) {
        fontStyle = value;
    }

    public boolean get_resizeTextForBestFit() {
        return bestFit;
    }

    public void set_resizeTextForBestFit(boolean value) {
        bestFit = value;
    }

    public int get_resizeTextMinSize() {
        return minSize;
    }

    public void set_resizeTextMinSize(int value) {
        minSize = value;
    }

    public int get_resizeTextMaxSize() {
        return maxSize;
    }

    public void set_resizeTextMaxSize(int value) {
        maxSize = value;
    }
}
