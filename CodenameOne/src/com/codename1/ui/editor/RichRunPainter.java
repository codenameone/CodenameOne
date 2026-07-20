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
package com.codename1.ui.editor;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;

import java.util.HashMap;
import java.util.Map;

/// Shared inline-run styling primitive for the rich text subsystem: it turns a
/// {@link TextStyle} plus a paragraph block type into a concrete pixel size and
/// derived {@link Font}, and paints one styled run (foreground color, highlight
/// background, inline-code tint, underline and strike-through decorations).
///
/// <p>The same style semantics power both the editable {@link RichView} and the
/// read-only rich text renderer, so a run built for one draws identically in the
/// other. Font size honors an absolute {@link TextStyle#getFontSizePx()} when set,
/// otherwise the relative {@link TextStyle#getFontSizeLevel()}, with heading scale
/// applied on top in both cases.</p>
///
/// @author Codename One
public final class RichRunPainter {

    private Font baseFont;
    private int baseSizePx;
    private int textColor = 0x000000;
    private final Map<Long, Font> fontCache = new HashMap<Long, Font>();

    /// Creates a painter. Call {@link #setBaseFont(Font)} before use.
    public RichRunPainter() {
    }

    /// Sets the base font from which sized/weighted run fonts are derived. The
    /// base size defaults to the font height.
    ///
    /// @param f the base font
    public void setBaseFont(Font f) {
        if (f != baseFont) { // NOPMD - intentional identity comparison; drop stale derived fonts
            baseFont = f;
            fontCache.clear();
            if (f != null && baseSizePx <= 0) {
                baseSizePx = f.getHeight();
            }
        }
    }

    /// Overrides the base (unscaled) run size in pixels; defaults to the base
    /// font height.
    ///
    /// @param px the base size in pixels
    public void setBaseSizePx(int px) {
        baseSizePx = px;
    }

    /// The base (unscaled) run size in pixels.
    /// @return base size in pixels
    public int getBaseSizePx() {
        return baseSizePx;
    }

    /// The default foreground color (0xRRGGBB) used when a run does not set one.
    /// @param rgb the color
    public void setTextColor(int rgb) {
        textColor = rgb;
    }

    // ------------------------------------------------------------------
    // Size / font
    // ------------------------------------------------------------------

    /// The heading scale factor for a paragraph block type ({@code RichBlocks.H1}..H6).
    /// @param blockType the block type
    /// @return the scale multiplier
    public static float headingScale(int blockType) {
        if (blockType == RichBlocks.H1) {
            return 2.0f;
        }
        if (blockType == RichBlocks.H1 + 1) {
            return 1.5f;
        }
        if (blockType == RichBlocks.H1 + 2) {
            return 1.25f;
        }
        if (blockType == RichBlocks.H1 + 3) {
            return 1.1f;
        }
        if (blockType == RichBlocks.H1 + 4) {
            return 0.9f;
        }
        if (blockType == RichBlocks.H1 + 5) {
            return 0.8f;
        }
        return 1.0f;
    }

    /// Whether a block type is a heading (h1..h6).
    /// @param blockType the block type
    /// @return true for headings
    public static boolean isHeading(int blockType) {
        return blockType >= RichBlocks.H1 && blockType <= RichBlocks.H1 + 5;
    }

    /// The relative-level scale factor (levels 1..7; 0/3 = 1.0).
    /// @param level the size level
    /// @return the scale multiplier
    public static float sizeLevelScale(int level) {
        switch (level) {
            case 1:
                return 0.7f;
            case 2:
                return 0.85f;
            case 4:
                return 1.15f;
            case 5:
                return 1.5f;
            case 6:
                return 2.0f;
            case 7:
                return 2.5f;
            default:
                return 1.0f;
        }
    }

    /// The pixel size for a run: an absolute {@link TextStyle#getFontSizePx()}
    /// (&gt; 0) wins over the relative level, and the paragraph heading scale is
    /// applied on top of either.
    ///
    /// @param blockType the paragraph block type
    /// @param st the run style
    /// @return the run size in pixels
    public int runPx(int blockType, TextStyle st) {
        if (st != null && st.getFontSizePx() > 0) {
            return Math.round(st.getFontSizePx() * headingScale(blockType));
        }
        int level = st == null ? 0 : st.getFontSizeLevel();
        return Math.round(baseSizePx * headingScale(blockType) * sizeLevelScale(level));
    }

    /// Derives a cached font at the given pixel size and weight/slant from the
    /// base font. A non-TTF base font cannot be resized and is returned as-is.
    ///
    /// @param px the pixel size
    /// @param bold bold weight
    /// @param italic italic slant
    /// @return the derived font
    public Font fontFor(int px, boolean bold, boolean italic) {
        if (baseFont == null) {
            return Font.getDefaultFont();
        }
        if (!baseFont.isTTFNativeFont()) {
            return baseFont;
        }
        if (px < 6) {
            px = 6;
        }
        long key = ((long) px << 2) | (bold ? 2 : 0) | (italic ? 1 : 0);
        Font f = fontCache.get(Long.valueOf(key));
        if (f != null) {
            return f;
        }
        int style = (bold ? Font.STYLE_BOLD : 0) | (italic ? Font.STYLE_ITALIC : 0);
        try {
            f = baseFont.derive(px, style);
        } catch (Throwable t) {
            f = baseFont;
        }
        fontCache.put(Long.valueOf(key), f);
        return f;
    }

    /// The concrete font a run resolves to (size from {@link #runPx}, bold from
    /// the style or a heading block, italic from the style).
    ///
    /// @param blockType the paragraph block type
    /// @param st the run style
    /// @return the run font
    public Font runFont(int blockType, TextStyle st) {
        boolean bold = (st != null && st.isBold()) || isHeading(blockType);
        boolean italic = st != null && st.isItalic();
        return fontFor(runPx(blockType, st), bold, italic);
    }

    // ------------------------------------------------------------------
    // Paint
    // ------------------------------------------------------------------

    /// Paints one styled run at {@code (x, y)} within a row of {@code lineHeight},
    /// drawing highlight/inline-code background, then the glyphs with color and
    /// underline/strike decorations. The caller supplies the already-resolved run
    /// font (from {@link #runFont}).
    ///
    /// @param g graphics
    /// @param run the run text
    /// @param st the run style
    /// @param rf the resolved run font
    /// @param x left pixel
    /// @param y top pixel of the row
    /// @param lineHeight the row height
    /// @return the painted advance width
    public int paintRun(Graphics g, String run, TextStyle st, Font rf, int x, int y, int lineHeight) {
        int w = rf.stringWidth(run);
        int ry = y + Math.max(0, lineHeight - rf.getHeight());
        if (st != null && st.getHighlight() >= 0) {
            g.setColor(st.getHighlight());
            g.fillRect(x, y, w, lineHeight);
        } else if (st != null && st.isMonospace()) {
            // no monospace family ships with the framework fonts; give inline code a subtle tint
            int alpha = g.getAlpha();
            g.setColor(textColor);
            g.setAlpha(24);
            g.fillRect(x, y, w, lineHeight);
            g.setAlpha(alpha);
        }
        int decoration = 0;
        if (st != null && st.isUnderline()) {
            decoration |= Style.TEXT_DECORATION_UNDERLINE;
        }
        if (st != null && st.isStrike()) {
            decoration |= Style.TEXT_DECORATION_STRIKETHRU;
        }
        g.setColor(st != null && st.getForeColor() >= 0 ? st.getForeColor() : textColor);
        g.setFont(rf);
        g.drawString(run, x, ry, decoration);
        return w;
    }
}
