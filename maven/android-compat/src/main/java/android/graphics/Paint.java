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
package android.graphics;

import com.codename1.androidcompat.runtime.FontCache;
import com.codename1.ui.Font;

/// How to draw: color, stroke, text size and typeface.
public class Paint {

    public static final int ANTI_ALIAS_FLAG = 0x01;
    public static final int FILTER_BITMAP_FLAG = 0x02;
    public static final int DITHER_FLAG = 0x04;
    public static final int UNDERLINE_TEXT_FLAG = 0x08;
    public static final int STRIKE_THRU_TEXT_FLAG = 0x10;
    public static final int FAKE_BOLD_TEXT_FLAG = 0x20;
    public static final int LINEAR_TEXT_FLAG = 0x40;
    public static final int SUBPIXEL_TEXT_FLAG = 0x80;
    public static final int EMBEDDED_BITMAP_TEXT_FLAG = 0x400;

    public enum Style { FILL, STROKE, FILL_AND_STROKE }

    public enum Cap { BUTT, ROUND, SQUARE }

    public enum Join { MITER, ROUND, BEVEL }

    public enum Align { LEFT, CENTER, RIGHT }

    public static class FontMetrics {
        public float top;
        public float ascent;
        public float descent;
        public float bottom;
        public float leading;
    }

    public static class FontMetricsInt {
        public int top;
        public int ascent;
        public int descent;
        public int bottom;
        public int leading;
    }

    private int flags;
    private int color = 0xff000000;
    private Style style = Style.FILL;
    private float strokeWidth;
    private Cap cap = Cap.BUTT;
    private Join join = Join.MITER;
    private float miter = 4;
    private float textSize = 12;
    private Typeface typeface;
    private Align align = Align.LEFT;
    private float letterSpacing;
    private Shader shader;
    private ColorFilter colorFilter;
    private Xfermode xfermode;
    private float shadowRadius;
    private float shadowDx;
    private float shadowDy;
    private int shadowColor;
    private float textScaleX = 1;

    public Paint() {
        this(0);
    }

    public Paint(int flags) {
        this.flags = flags;
    }

    public Paint(Paint p) {
        set(p);
    }

    public void set(Paint p) {
        flags = p.flags;
        color = p.color;
        style = p.style;
        strokeWidth = p.strokeWidth;
        cap = p.cap;
        join = p.join;
        miter = p.miter;
        textSize = p.textSize;
        typeface = p.typeface;
        align = p.align;
        letterSpacing = p.letterSpacing;
        shader = p.shader;
        colorFilter = p.colorFilter;
        xfermode = p.xfermode;
        shadowRadius = p.shadowRadius;
        shadowDx = p.shadowDx;
        shadowDy = p.shadowDy;
        shadowColor = p.shadowColor;
        textScaleX = p.textScaleX;
    }

    public void reset() {
        set(new Paint());
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public final boolean isAntiAlias() {
        return (flags & ANTI_ALIAS_FLAG) != 0;
    }

    public void setAntiAlias(boolean aa) {
        flags = aa ? flags | ANTI_ALIAS_FLAG : flags & ~ANTI_ALIAS_FLAG;
    }

    public final boolean isFilterBitmap() {
        return (flags & FILTER_BITMAP_FLAG) != 0;
    }

    public void setFilterBitmap(boolean filter) {
        flags = filter ? flags | FILTER_BITMAP_FLAG : flags & ~FILTER_BITMAP_FLAG;
    }

    public void setDither(boolean dither) {
        flags = dither ? flags | DITHER_FLAG : flags & ~DITHER_FLAG;
    }

    public final boolean isUnderlineText() {
        return (flags & UNDERLINE_TEXT_FLAG) != 0;
    }

    public void setUnderlineText(boolean u) {
        flags = u ? flags | UNDERLINE_TEXT_FLAG : flags & ~UNDERLINE_TEXT_FLAG;
    }

    public final boolean isStrikeThruText() {
        return (flags & STRIKE_THRU_TEXT_FLAG) != 0;
    }

    public void setStrikeThruText(boolean s) {
        flags = s ? flags | STRIKE_THRU_TEXT_FLAG : flags & ~STRIKE_THRU_TEXT_FLAG;
    }

    public final boolean isFakeBoldText() {
        return (flags & FAKE_BOLD_TEXT_FLAG) != 0;
    }

    public void setFakeBoldText(boolean b) {
        flags = b ? flags | FAKE_BOLD_TEXT_FLAG : flags & ~FAKE_BOLD_TEXT_FLAG;
    }

    public void setSubpixelText(boolean s) {
        flags = s ? flags | SUBPIXEL_TEXT_FLAG : flags & ~SUBPIXEL_TEXT_FLAG;
    }

    public void setLinearText(boolean l) {
        flags = l ? flags | LINEAR_TEXT_FLAG : flags & ~LINEAR_TEXT_FLAG;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getAlpha() {
        return color >>> 24;
    }

    public void setAlpha(int a) {
        color = (color & 0x00ffffff) | ((a & 0xff) << 24);
    }

    public void setARGB(int a, int r, int g, int b) {
        color = (a << 24) | (r << 16) | (g << 8) | b;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float width) {
        strokeWidth = width;
    }

    public Cap getStrokeCap() {
        return cap;
    }

    public void setStrokeCap(Cap cap) {
        this.cap = cap;
    }

    public Join getStrokeJoin() {
        return join;
    }

    public void setStrokeJoin(Join join) {
        this.join = join;
    }

    public float getStrokeMiter() {
        return miter;
    }

    public void setStrokeMiter(float miter) {
        this.miter = miter;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public float getTextScaleX() {
        return textScaleX;
    }

    public void setTextScaleX(float scaleX) {
        textScaleX = scaleX;
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public Typeface setTypeface(Typeface typeface) {
        this.typeface = typeface;
        return typeface;
    }

    public Align getTextAlign() {
        return align;
    }

    public void setTextAlign(Align align) {
        this.align = align;
    }

    public float getLetterSpacing() {
        return letterSpacing;
    }

    public void setLetterSpacing(float letterSpacing) {
        this.letterSpacing = letterSpacing;
    }

    public Shader getShader() {
        return shader;
    }

    public Shader setShader(Shader shader) {
        this.shader = shader;
        return shader;
    }

    public ColorFilter getColorFilter() {
        return colorFilter;
    }

    public ColorFilter setColorFilter(ColorFilter filter) {
        this.colorFilter = filter;
        return filter;
    }

    public Xfermode getXfermode() {
        return xfermode;
    }

    public Xfermode setXfermode(Xfermode xfermode) {
        this.xfermode = xfermode;
        return xfermode;
    }

    public void setShadowLayer(float radius, float dx, float dy, int shadowColor) {
        this.shadowRadius = radius;
        this.shadowDx = dx;
        this.shadowDy = dy;
        this.shadowColor = shadowColor;
    }

    public void clearShadowLayer() {
        shadowRadius = 0;
    }

    public float getShadowLayerRadius() {
        return shadowRadius;
    }

    public float getShadowLayerDx() {
        return shadowDx;
    }

    public float getShadowLayerDy() {
        return shadowDy;
    }

    public int getShadowLayerColor() {
        return shadowColor;
    }

    // ------------------------------------------------------------ text metrics

    /// The Codename One font this paint draws text with.
    public Font cn1Font() {
        Typeface tf = typeface;
        if (isFakeBoldText()) {
            tf = Typeface.create(tf, Typeface.BOLD);
        }
        return FontCache.font(tf, textSize);
    }

    public float ascent() {
        return -cn1Font().getAscent();
    }

    public float descent() {
        return cn1Font().getDescent();
    }

    public float getFontSpacing() {
        return cn1Font().getHeight();
    }

    public float getFontMetrics(FontMetrics m) {
        Font f = cn1Font();
        if (m != null) {
            m.ascent = -f.getAscent();
            m.descent = f.getDescent();
            m.top = m.ascent;
            m.bottom = m.descent;
            m.leading = Math.max(0, f.getHeight() - f.getAscent() - f.getDescent());
        }
        return f.getHeight();
    }

    public FontMetrics getFontMetrics() {
        FontMetrics m = new FontMetrics();
        getFontMetrics(m);
        return m;
    }

    public int getFontMetricsInt(FontMetricsInt m) {
        Font f = cn1Font();
        if (m != null) {
            m.ascent = -f.getAscent();
            m.descent = f.getDescent();
            m.top = m.ascent;
            m.bottom = m.descent;
            m.leading = Math.max(0, f.getHeight() - f.getAscent() - f.getDescent());
        }
        return f.getHeight();
    }

    public FontMetricsInt getFontMetricsInt() {
        FontMetricsInt m = new FontMetricsInt();
        getFontMetricsInt(m);
        return m;
    }

    public float measureText(String text) {
        return measureText(text, 0, text.length());
    }

    public float measureText(String text, int start, int end) {
        if (start >= end) {
            return 0;
        }
        Font f = cn1Font();
        float w = f.substringWidth(text, start, end - start);
        if (letterSpacing != 0) {
            w += letterSpacing * textSize * (end - start);
        }
        return w * textScaleX;
    }

    public float measureText(CharSequence text, int start, int end) {
        return measureText(text.toString(), start, end);
    }

    public float measureText(char[] text, int index, int count) {
        return measureText(new String(text, index, count));
    }

    public int getTextWidths(String text, float[] widths) {
        return getTextWidths(text, 0, text.length(), widths);
    }

    public int getTextWidths(String text, int start, int end, float[] widths) {
        Font f = cn1Font();
        for (int i = start; i < end; i++) {
            widths[i - start] = f.charWidth(text.charAt(i)) * textScaleX;
        }
        return end - start;
    }

    public void getTextBounds(String text, int start, int end, Rect bounds) {
        Font f = cn1Font();
        bounds.set(0, -f.getAscent(), Math.round(measureText(text, start, end)), f.getDescent());
    }

    public void getTextBounds(CharSequence text, int start, int end, Rect bounds) {
        getTextBounds(text.toString(), start, end, bounds);
    }

    public void getTextBounds(char[] text, int index, int count, Rect bounds) {
        getTextBounds(new String(text, index, count), 0, count, bounds);
    }

    /// How many characters of `text` fit in `maxWidth`.
    public int breakText(String text, boolean measureForwards, float maxWidth, float[] measuredWidth) {
        int n = text.length();
        float w = 0;
        int count = 0;
        Font f = cn1Font();
        if (measureForwards) {
            for (int i = 0; i < n; i++) {
                float cw = f.charWidth(text.charAt(i)) * textScaleX;
                if (w + cw > maxWidth) {
                    break;
                }
                w += cw;
                count++;
            }
        } else {
            for (int i = n - 1; i >= 0; i--) {
                float cw = f.charWidth(text.charAt(i)) * textScaleX;
                if (w + cw > maxWidth) {
                    break;
                }
                w += cw;
                count++;
            }
        }
        if (measuredWidth != null && measuredWidth.length > 0) {
            measuredWidth[0] = w;
        }
        return count;
    }

    public int breakText(CharSequence text, int start, int end, boolean measureForwards, float maxWidth,
                         float[] measuredWidth) {
        return breakText(text.subSequence(start, end).toString(), measureForwards, maxWidth, measuredWidth);
    }
}
