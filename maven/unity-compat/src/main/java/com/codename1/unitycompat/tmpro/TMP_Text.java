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
package com.codename1.unitycompat.tmpro;

import com.codename1.unitycompat.unityengine.Component;
import com.codename1.unitycompat.unityengine.DrawCommand;
import com.codename1.unitycompat.unityengine.ui.MaskableGraphic;

/// `TMPro.TMP_Text`: what the two TextMesh Pro components share -- a
/// string, a size, an alignment.
///
/// This is not TextMesh Pro. It is the members a script uses, over the
/// text drawing the runtime already has: the string is drawn in the
/// platform's font at the size asked for. The font asset, its material,
/// outlines, gradients, character and line spacing, and rich text tags
/// are not read; tags are drawn as written.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public abstract class TMP_Text extends MaskableGraphic {
    private String text = "";
    private float fontSize = 36f;
    private float fontSizeMin = 18f;
    private float fontSizeMax = 72f;
    private boolean autoSize;
    private boolean wrap = true;
    /// `TextAlignmentOptions`: the column in the low byte, the row in the
    /// next.
    private int alignment = 0x101;
    /// `FontStyles`, a set of bits: 1 bold, 2 italic.
    private int fontStyle;
    /// The margins inside the rectangle, in its own units: left, top,
    /// right, bottom.
    private float marginLeft;
    private float marginTop;
    private float marginRight;
    private float marginBottom;

    /// What a scene file sets: the margins inside the rectangle.
    public void $margins(float left, float top, float right, float bottom) {
        marginLeft = left;
        marginTop = top;
        marginRight = right;
        marginBottom = bottom;
    }

    /// What a scene file sets.
    public void $setup(String value, float size, float min, float max, boolean fits, boolean wraps, int aligned,
            int style) {
        text = value == null ? "" : value;
        fontSize = size;
        fontSizeMin = min;
        fontSizeMax = max;
        autoSize = fits;
        wrap = wraps;
        alignment = aligned;
        fontStyle = style;
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        TMP_Text t = (TMP_Text) source;
        $setup(t.text, t.fontSize, t.fontSizeMin, t.fontSizeMax, t.autoSize, t.wrap, t.alignment, t.fontStyle);
        $margins(t.marginLeft, t.marginTop, t.marginRight, t.marginBottom);
    }

    /// Describes this text to a draw command whose rectangle is set.
    /// `scale` is surface pixels to one unit of the font size, `unit`
    /// surface pixels to one unit of the rectangle, which the margins are
    /// measured in.
    protected final void fill(DrawCommand d, float scale, float unit) {
        float left = marginLeft * unit;
        float top = marginTop * unit;
        float wide = marginLeft + marginRight;
        float down = marginTop + marginBottom;
        wide = wide * unit;
        down = down * unit;
        // Margins that leave nothing are a rectangle drawn too small for
        // its text, which TextMesh Pro overflows: the text keeps the
        // whole rectangle then.
        if (d.width - wide > 1f) {
            d.x += left;
            d.width -= wide;
        }
        if (d.height - down > 1f) {
            d.y += top;
            d.height -= down;
        }
        d.text = text;
        d.color = $drawColor();
        d.fontSize = fontSize * scale;
        d.bestFit = autoSize;
        d.minFontSize = fontSizeMin * scale;
        d.maxFontSize = fontSizeMax * scale;
        int across = alignment & 0xff;
        int up = (alignment >> 8) & 0xff;
        // Justified and flush text is set from the left; a baseline or
        // geometry row is set as the row nearest to it.
        int column = (across & 2) != 0 || (across & 32) != 0 ? 1 : (across & 4) != 0 ? 2 : 0;
        int row = (up & 2) != 0 || (up & 16) != 0 || (up & 32) != 0 ? 1 : (up & 4) != 0 || (up & 8) != 0 ? 2 : 0;
        d.alignment = row * 3 + column;
        d.fontStyle = fontStyle & 3;
        d.wrap = wrap;
    }

    @Override
    public boolean $paint(DrawCommand d, float scale) {
        fill(d, scale, scale);
        return true;
    }

    public String get_text() {
        return text;
    }

    public void set_text(String value) {
        text = value == null ? "" : value;
    }

    public void SetText(String value) {
        set_text(value);
    }

    public float get_fontSize() {
        return fontSize;
    }

    public void set_fontSize(float value) {
        fontSize = value;
    }

    public boolean get_enableAutoSizing() {
        return autoSize;
    }

    public void set_enableAutoSizing(boolean value) {
        autoSize = value;
    }

    public float get_fontSizeMin() {
        return fontSizeMin;
    }

    public void set_fontSizeMin(float value) {
        fontSizeMin = value;
    }

    public float get_fontSizeMax() {
        return fontSizeMax;
    }

    public void set_fontSizeMax(float value) {
        fontSizeMax = value;
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

    public boolean get_enableWordWrapping() {
        return wrap;
    }

    public void set_enableWordWrapping(boolean value) {
        wrap = value;
    }
}
