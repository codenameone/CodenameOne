/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import java.util.ArrayList;

/// Rive `Text`: runs of styled text laid out into lines and drawn as glyph
/// outlines with each run's style paints, as upstream draws text.
///
/// Shaping is simpler than upstream's HarfBuzz: cmap glyphs, `hmtx` advances,
/// `kern` pair kerning and letter spacing, left to right only, no ligatures.
/// Lines break at spaces (inside a word only when the word alone is too
/// wide) and at newlines. Sizing, wrapping, alignment, vertical alignment,
/// paragraph spacing, line height, origin and the overflow modes are ported;
/// `fitFontSize` scales like `fit`. Vertical trim, text backgrounds,
/// follow-path and variation modifiers and text input editing are not.
public class Text extends TextBase {
    private static final int SIZING_AUTO_WIDTH = 0;
    private static final int SIZING_FIXED = 2;
    private static final int OVERFLOW_HIDDEN = 1;
    private static final int OVERFLOW_CLIPPED = 2;
    private static final int OVERFLOW_ELLIPSIS = 3;
    private static final int OVERFLOW_FIT = 4;
    private static final int OVERFLOW_FIT_FONT_SIZE = 5;

    private final ArrayList<TextValueRun> runs = new ArrayList<TextValueRun>();
    private final ArrayList<TextModifierGroup> groups = new ArrayList<TextModifierGroup>();
    private final ArrayList<Bucket> buckets = new ArrayList<Bucket>();
    private int bucketCount;
    private final Mat2D textTransform = new Mat2D();
    private final Mat2D glyphTransform = new Mat2D();
    private final RivePath clipRect = new RivePath();

    // Glyphs of the whole text, in logical order.
    private int glyphCount;
    private int[] gId = new int[32];
    private float[] gAdv = new float[32];
    private int[] gText = new int[32];
    private byte[] gKind = new byte[32];
    private TextStyle[] gStyle = new TextStyle[32];
    private static final byte KIND_GLYPH = 0;
    private static final byte KIND_SPACE = 1;
    private static final byte KIND_NEWLINE = 2;

    // Code points of the whole text.
    private int cpCount;
    private boolean[] cpSpace = new boolean[32];
    private int[][] units = new int[4][33];

    // Lines.
    private int lineCount;
    private int[] lStart = new int[8];
    private int[] lEnd = new int[8];
    private float[] lWidth = new float[8];
    private float[] lBaseline = new float[8];
    private float[] lBottom = new float[8];

    private float boundsWidth;
    private float boundsHeight;
    private float linesMaxWidth;
    // The box a parent layout resolved (controlSize), NaN until it does.
    private float slotWidth = Float.NaN;
    private float slotHeight = Float.NaN;
    private int slotWidthScale = -1;
    private int slotHeightScale = -1;

    /// One style's glyphs, one path per distinct glyph opacity.
    private static final class Bucket {
        TextStylePaint style;
        final ArrayList<RivePath> paths = new ArrayList<RivePath>();
        float[] opacities = new float[2];
        int count;

        void add(RivePath glyph, float opacity, Mat2D m) {
            int i = 0;
            while (i < count && Float.floatToIntBits(opacities[i]) != Float.floatToIntBits(opacity)) {
                i++;
            }
            if (i == count) {
                if (count == paths.size()) {
                    paths.add(new RivePath());
                }
                if (count == opacities.length) {
                    float[] n = new float[count * 2];
                    System.arraycopy(opacities, 0, n, 0, count);
                    opacities = n;
                }
                opacities[count] = opacity;
                paths.get(count).rewind();
                count++;
            }
            paths.get(i).addPath(glyph, m);
        }
    }

    void addModifierGroup(TextModifierGroup g) {
        groups.add(g);
    }

    /// The number of lines of the last layout (for tests).
    public int getLineCount() {
        return lineCount;
    }

    /// The number of glyphs of the last layout, spaces and newlines included.
    public int getGlyphCount() {
        return glyphCount;
    }

    /// Width of the text's box after layout.
    public float getBoundsWidth() {
        return boundsWidth;
    }

    /// Height of the text's box after layout.
    public float getBoundsHeight() {
        return boundsHeight;
    }

    /// The drawn width of line `i`.
    public float getLineWidth(int i) {
        return lWidth[i];
    }

    /// The baseline of line `i`, from the top of the text.
    public float getLineBaseline(int i) {
        return lBaseline[i];
    }

    @Override
    public int onAddedClean(Artboard context) {
        int code = super.onAddedClean(context);
        ArrayList<Component> kids = getChildren();
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            if (kids.get(i) instanceof TextValueRun) {
                runs.add((TextValueRun) kids.get(i));
            }
        }
        return code;
    }

    @Override
    public void update(int value) {
        super.update(value);
        if (hasDirt(value, DIRT_TEXT_SHAPE)) {
            shape();
        }
        if (hasDirt(value, DIRT_TEXT_SHAPE | DIRT_RENDER_OPACITY | DIRT_PAINT)) {
            for (int i = 0; i < bucketCount; i++) {
                buckets.get(i).style.updatePaints(renderOpacity, getBlendModeValue());
            }
        }
    }

    private void ensureGlyphs(int n) {
        if (n <= gId.length) {
            return;
        }
        int c = Math.max(n, gId.length * 2);
        int[] a = new int[c];
        System.arraycopy(gId, 0, a, 0, glyphCount);
        gId = a;
        a = new int[c];
        System.arraycopy(gText, 0, a, 0, glyphCount);
        gText = a;
        float[] f = new float[c];
        System.arraycopy(gAdv, 0, f, 0, glyphCount);
        gAdv = f;
        byte[] k = new byte[c];
        System.arraycopy(gKind, 0, k, 0, glyphCount);
        gKind = k;
        TextStyle[] s = new TextStyle[c];
        System.arraycopy(gStyle, 0, s, 0, glyphCount);
        gStyle = s;
    }

    private void ensureCodePoints(int n) {
        if (n < cpSpace.length) {
            return;
        }
        boolean[] b = new boolean[Math.max(n + 1, cpSpace.length * 2)];
        System.arraycopy(cpSpace, 0, b, 0, cpCount);
        cpSpace = b;
    }

    private void addLine(int start, int end) {
        if (lineCount == lStart.length) {
            int c = lineCount * 2;
            int[] a = new int[c];
            System.arraycopy(lStart, 0, a, 0, lineCount);
            lStart = a;
            a = new int[c];
            System.arraycopy(lEnd, 0, a, 0, lineCount);
            lEnd = a;
            float[] f = new float[c];
            System.arraycopy(lWidth, 0, f, 0, lineCount);
            lWidth = f;
            f = new float[c];
            System.arraycopy(lBaseline, 0, f, 0, lineCount);
            lBaseline = f;
            f = new float[c];
            System.arraycopy(lBottom, 0, f, 0, lineCount);
            lBottom = f;
        }
        float w = 0f;
        int last = end;
        while (last > start && gKind[last - 1] != KIND_GLYPH) {
            last--;
        }
        for (int i = start; i < last; i++) {
            w += gAdv[i];
        }
        lStart[lineCount] = start;
        lEnd[lineCount] = end;
        lWidth[lineCount] = w;
        lineCount++;
    }

    /// Builds the glyph list from the runs: upstream's shaping step.
    private void collectGlyphs() {
        glyphCount = 0;
        cpCount = 0;
        for (int r = 0; r < runs.size(); r++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            TextValueRun run = runs.get(r);
            TextStyle style = run.getStyle();
            RiveFont font = null;
            if (style != null) {
                style.addUser(this);
                font = style.getFont();
            }
            String s = run.getText();
            int len = s == null ? 0 : s.length();
            int prev = -1;
            float size = style == null ? 0f : style.getFontSize();
            float spacing = style == null ? 0f : style.getLetterSpacing();
            for (int i = 0; i < len; i++) {
                int c = s.charAt(i);
                if (c >= 0xD800 && c <= 0xDBFF && i + 1 < len) {
                    int lo = s.charAt(i + 1);
                    if (lo >= 0xDC00 && lo <= 0xDFFF) {
                        c = 0x10000 + ((c - 0xD800) << 10) + (lo - 0xDC00);
                        i++;
                    }
                }
                boolean newline = c == '\n' || c == 0x2028 || c == 0x2029;
                // U+00A0 (no-break space) is deliberately not a space here: it
                // must never be a line break opportunity, so it is a glyph.
                boolean space = c == ' ' || c == '\t' || c == '\r';
                ensureCodePoints(cpCount + 1);
                cpSpace[cpCount] = space || newline;
                int textIndex = cpCount++;
                if (font == null) {
                    continue;
                }
                ensureGlyphs(glyphCount + 1);
                int g = newline ? 0 : font.glyphFor(c);
                gId[glyphCount] = g;
                gText[glyphCount] = textIndex;
                gStyle[glyphCount] = style;
                if (newline) {
                    gKind[glyphCount] = KIND_NEWLINE;
                    gAdv[glyphCount] = 0f;
                    prev = -1;
                } else {
                    gKind[glyphCount] = space ? KIND_SPACE : KIND_GLYPH;
                    gAdv[glyphCount] = font.advance(g) * size + spacing;
                    if (prev >= 0) {
                        gAdv[glyphCount - 1] += font.kern(prev, g) * size;
                    }
                    prev = g;
                }
                glyphCount++;
            }
        }
    }

    /// Greedy line breaking of the paragraph `[ps, pe)`.
    private void breakParagraph(int ps, int pe, float wrapWidth) {
        int lineStart = ps;
        int lastBreak = -1;
        float x = 0f;
        for (int i = ps; i < pe; i++) {
            if (gKind[i] == KIND_SPACE) {
                x += gAdv[i];
                lastBreak = i + 1;
                continue;
            }
            if (x + gAdv[i] > wrapWidth && i > lineStart) {
                int end = lastBreak > lineStart ? lastBreak : i;
                addLine(lineStart, end);
                lineStart = end;
                lastBreak = -1;
                x = 0f;
                for (int k = lineStart; k < i; k++) {
                    x += gAdv[k];
                }
            }
            x += gAdv[i];
        }
        addLine(lineStart, pe);
    }

    private static float lineAscent(TextStyle s) {
        RiveFont f = s.getFont();
        float lh = s.getLineHeight();
        if (lh < 0f) {
            return f.getAscent() * s.getFontSize();
        }
        float baseline = -f.getAscent();
        float height = baseline + f.getDescent();
        return height <= 0f ? 0f : -baseline / height * lh;
    }

    private static float lineDescent(TextStyle s) {
        float lh = s.getLineHeight();
        if (lh < 0f) {
            return s.getFont().getDescent() * s.getFontSize();
        }
        return lh + lineAscent(s);
    }

    private Bucket bucketFor(TextStylePaint style) {
        for (int i = 0; i < bucketCount; i++) {
            Bucket b = buckets.get(i);
            if (b.style == style) { // NOPMD CompareObjectsWithEquals - identity is the intent
                return b;
            }
        }
        if (bucketCount == buckets.size()) {
            buckets.add(new Bucket());
        }
        Bucket b = buckets.get(bucketCount++);
        b.style = style;
        b.count = 0;
        return b;
    }

    private void emitGlyph(int i, float x, float y) {
        TextStyle style = gStyle[i];
        if (!(style instanceof TextStylePaint) || gKind[i] != KIND_GLYPH) {
            return;
        }
        RivePath path = style.getFont().glyphPath(gId[i]);
        if (path.isEmpty()) {
            return;
        }
        float size = style.getFontSize();
        float center = gAdv[i] / 2f;
        Mat2D m = glyphTransform;
        m.set(size, 0f, 0f, size, -center, 0f);
        float opacity = 1f;
        for (int k = 0; k < groups.size(); k++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            TextModifierGroup g = groups.get(k);
            float coverage = g.glyphCoverage(gText[i], 1);
            g.transform(coverage, m);
            if (g.modifiesOpacity()) {
                opacity = g.computeOpacity(opacity, coverage);
            }
        }
        if (opacity <= 0f) {
            return;
        }
        m.v[4] += x + center;
        m.v[5] += y;
        bucketFor((TextStylePaint) style).add(path, opacity, m);
    }

    private void computeUnits() {
        if (units[0].length < cpCount + 1) {
            units = new int[4][cpCount + 1];
        }
        int noSpace = 0;
        int words = 0;
        boolean inWord = false;
        for (int i = 0; i < cpCount; i++) {
            units[0][i] = i;
            if (cpSpace[i]) {
                units[1][i] = -1;
                units[2][i] = -1;
                inWord = false;
            } else {
                units[1][i] = noSpace++;
                if (!inWord) {
                    words++;
                    inWord = true;
                }
                units[2][i] = words - 1;
            }
            units[3][i] = -1;
        }
        for (int l = 0; l < lineCount; l++) {
            for (int g = lStart[l]; g < lEnd[l]; g++) {
                units[3][gText[g]] = l;
            }
        }
        units[0][cpCount] = cpCount;
        units[1][cpCount] = noSpace;
        units[2][cpCount] = words;
        units[3][cpCount] = lineCount;
    }

    /// Lays the text out and rebuilds the glyph geometry.
    /// Breaks every paragraph into lines at `wrapWidth`, filling the line
    /// arrays; answers the total height and leaves the widest line in
    /// `linesMaxWidth`.
    private float breakLines(float wrapWidth) {
        lineCount = 0;
        // Paragraphs end at newlines; each newline stays on its paragraph's
        // last line so an empty paragraph still has a line and a style.
        float curY = 0f;
        float maxWidth = 0f;
        int ps = 0;
        int paragraph = 0;
        while (ps < glyphCount) {
            int pe = ps;
            while (pe < glyphCount && gKind[pe] != KIND_NEWLINE) {
                pe++;
            }
            int firstLine = lineCount;
            breakParagraph(ps, pe, wrapWidth);
            if (pe < glyphCount) {
                lEnd[lineCount - 1] = pe + 1;
            }
            float y = 0f;
            for (int l = firstLine; l < lineCount; l++) {
                float asc = 0f;
                float realAscent = 0f;
                float des = 0f;
                for (int g = lStart[l]; g < lEnd[l]; g++) {
                    TextStyle s = gStyle[g];
                    asc = Math.min(asc, lineAscent(s));
                    des = Math.max(des, lineDescent(s));
                    realAscent = Math.min(realAscent, s.getFont().getAscent() * s.getFontSize());
                }
                if (paragraph == 0 && l == firstLine) {
                    y = -realAscent;
                } else {
                    y -= asc;
                }
                lBaseline[l] = curY + y;
                y += des;
                lBottom[l] = curY + y;
                maxWidth = Math.max(maxWidth, lWidth[l]);
            }
            curY += y + mParagraphSpacing;
            paragraph++;
            ps = pe + 1;
        }
        linesMaxWidth = maxWidth;
        return curY - mParagraphSpacing;
    }

    /// Port of `Text::effectiveSizing`: a layout that boxes the text on
    /// both axes turns it into a fixed box; a hug axis keeps the authored
    /// sizing.
    int effectiveSizing() {
        if (slotWidthScale < 0 || slotWidthScale == LayoutComponent.SCALE_HUG
                || slotHeightScale == LayoutComponent.SCALE_HUG) {
            return mSizingValue;
        }
        return SIZING_FIXED;
    }

    float effectiveWidth() {
        return Float.isNaN(slotWidth) ? mWidth : slotWidth;
    }

    float effectiveHeight() {
        return Float.isNaN(slotHeight) ? mHeight : slotHeight;
    }

    /// Port of `Text::measureLayout`/`Text::measure`: the size this text
    /// takes inside `width` x `height` (NaN for unbounded). Shapes into the
    /// line scratch only; the drawn glyphs are left alone.
    void measureLayout(float width, float height, float[] out) {
        float maxW = Float.isNaN(width) ? Float.MAX_VALUE : width;
        float maxH = Float.isNaN(height) ? Float.MAX_VALUE : height;
        int sizing = effectiveSizing();
        float measuringWidth = sizing == SIZING_AUTO_WIDTH ? Float.MAX_VALUE : mWidth;
        float fitWidth = Math.min(maxW, measuringWidth);
        collectGlyphs();
        if (glyphCount == 0) {
            out[0] = 0;
            out[1] = 0;
            return;
        }
        if (mSizingValue == SIZING_FIXED && mOriginValue != 1) {
            out[0] = Math.min(maxW, mWidth);
            out[1] = Math.min(maxH, mHeight);
            return;
        }
        boolean wrap = mWrapValue == 0 && !(Float.isNaN(width) && sizing != 1);
        breakLines(wrap ? fitWidth : Float.MAX_VALUE);
        float minY = 0;
        float shift = 0;
        if (mOriginValue == 1 && lineCount > 0) {
            shift = -lBaseline[0];
            minY = shift;
        }
        float computedHeight = 0;
        boolean wantEllipsis = mOverflowValue == OVERFLOW_ELLIPSIS && mSizingValue == SIZING_FIXED;
        for (int l = 0; l < lineCount; l++) {
            float bottom = lBottom[l] + shift;
            if (wantEllipsis && bottom > maxH) {
                if (l == 0) {
                    computedHeight = bottom;
                }
                break;
            }
            computedHeight = bottom;
        }
        float bw;
        float bh;
        if (mSizingValue == SIZING_AUTO_WIDTH) {
            bw = linesMaxWidth;
            bh = Math.max(minY, computedHeight);
        } else if (mSizingValue == SIZING_FIXED) {
            bw = mWidth;
            bh = minY + mHeight;
        } else {
            bw = mWidth;
            bh = Math.max(minY, computedHeight);
        }
        out[0] = Math.min(maxW, bw);
        out[1] = Math.min(maxH, bh);
    }

    /// Port of `Text::controlSize`: the box the parent layout resolved.
    void controlSize(float width, float height, int widthScale, int heightScale) {
        if (widthScale != slotWidthScale || heightScale != slotHeightScale
                || Float.floatToIntBits(width) != Float.floatToIntBits(slotWidth)
                || Float.floatToIntBits(height) != Float.floatToIntBits(slotHeight)) {
            slotWidth = width;
            slotHeight = height;
            slotWidthScale = widthScale;
            slotHeightScale = heightScale;
            layoutChanged();
        }
    }

    private void shape() {
        for (int i = 0; i < bucketCount; i++) {
            buckets.get(i).count = 0;
        }
        bucketCount = 0;
        lineCount = 0;
        collectGlyphs();
        int sizing = effectiveSizing();
        float boxW = effectiveWidth();
        float boxH = effectiveHeight();
        if (glyphCount == 0) {
            boundsWidth = sizing == SIZING_AUTO_WIDTH ? 0f : boxW;
            boundsHeight = sizing == SIZING_FIXED ? boxH : 0f;
            return;
        }
        boolean wrap = sizing != SIZING_AUTO_WIDTH && mWrapValue == 0;
        float wrapWidth = wrap ? boxW : Float.MAX_VALUE;

        float totalHeight = breakLines(wrapWidth);
        float maxWidth = linesMaxWidth;
        float shiftY = 0f;
        if (mOriginValue == 1) {
            shiftY = -lBaseline[0];
        }

        boolean fixed = sizing == SIZING_FIXED;
        boundsWidth = sizing == SIZING_AUTO_WIDTH ? maxWidth : boxW;
        boundsHeight = fixed ? boxH : totalHeight;
        float layoutWidth = boundsWidth;

        int overflow = mOverflowValue;
        int visibleLines = lineCount;
        int ellipsisLine = -1;
        if (fixed && (overflow == OVERFLOW_HIDDEN || overflow == OVERFLOW_ELLIPSIS)) {
            visibleLines = 0;
            while (visibleLines < lineCount && lBottom[visibleLines] <= boundsHeight + 0.01f) {
                visibleLines++;
            }
        }
        if (overflow == OVERFLOW_ELLIPSIS && visibleLines > 0) {
            int last = visibleLines - 1;
            if (visibleLines < lineCount || lWidth[last] > boundsWidth) {
                ellipsisLine = last;
            }
        }
        computeUnits();
        if (!groups.isEmpty()) {
            for (int k = 0; k < groups.size(); k++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                groups.get(k).computeCoverage(cpCount, units);
            }
        }
        for (int l = 0; l < visibleLines; l++) {
            int end = lEnd[l];
            float width = lWidth[l];
            TextStyle ellipsisStyle = null;
            int dot = 0;
            int dots = 0;
            float dotAdv = 0f;
            if (l == ellipsisLine) {
                ellipsisStyle = gStyle[Math.max(lStart[l], end - 1)];
                RiveFont f = ellipsisStyle.getFont();
                dot = f.glyphFor(0x2026);
                dots = 1;
                if (dot == 0) {
                    dot = f.glyphFor('.');
                    dots = 3;
                }
                dotAdv = f.advance(dot) * ellipsisStyle.getFontSize() + ellipsisStyle.getLetterSpacing();
                float ellipsisWidth = dotAdv * dots;
                while (end > lStart[l]) {
                    float w = 0f;
                    for (int g = lStart[l]; g < end; g++) {
                        w += gAdv[g];
                    }
                    if (w + ellipsisWidth <= boundsWidth) {
                        break;
                    }
                    end--;
                }
                width = ellipsisWidth;
                for (int g = lStart[l]; g < end; g++) {
                    width += gAdv[g];
                }
            }
            float x;
            if (mAlignValue == 1) {
                x = layoutWidth - width;
            } else if (mAlignValue == 2) {
                x = layoutWidth / 2f - width / 2f;
            } else {
                x = 0f;
            }
            float y = lBaseline[l] + shiftY;
            for (int g = lStart[l]; g < end; g++) {
                emitGlyph(g, x, y);
                x += gAdv[g];
            }
            if (ellipsisStyle instanceof TextStylePaint) {
                RivePath path = ellipsisStyle.getFont().glyphPath(dot);
                float size = ellipsisStyle.getFontSize();
                for (int k = 0; k < dots; k++) {
                    glyphTransform.set(size, 0f, 0f, size, x, y);
                    bucketFor((TextStylePaint) ellipsisStyle).add(path, 1f, glyphTransform);
                    x += dotAdv;
                }
            }
        }

        // Fit and vertical alignment, upstream's step 7.
        float scale = 1f;
        float xOffset = -boundsWidth * mOriginX;
        float yOffset = -boundsHeight * mOriginY;
        if (overflow == OVERFLOW_FIT || overflow == OVERFLOW_FIT_FONT_SIZE) {
            float xScale = (sizing != SIZING_AUTO_WIDTH || fixed) && maxWidth > boundsWidth
                    ? boundsWidth / maxWidth : 1f;
            float baseline = mFitFromBaseline ? lBaseline[0] : 0f;
            float yScale = fixed && totalHeight > boundsHeight
                    ? (boundsHeight - baseline) / (totalHeight - baseline) : 1f;
            if (xScale != 1f || yScale != 1f) {
                scale = Math.max(0f, Math.min(xScale, yScale));
                yOffset += baseline * (1f - scale);
                if (mAlignValue == 2) {
                    xOffset += (boundsWidth - maxWidth * scale) / 2f;
                } else if (mAlignValue == 1) {
                    xOffset += boundsWidth - maxWidth * scale;
                }
            }
        }
        if (mVerticalAlignValue != 0 && fixed) {
            yOffset = -boundsHeight * mOriginY;
            if (mVerticalAlignValue == 2) {
                yOffset += (boundsHeight - totalHeight * scale) / 2f;
            } else if (mVerticalAlignValue == 1) {
                yOffset += boundsHeight - totalHeight * scale;
            }
        }
        textTransform.set(scale, 0f, 0f, scale, xOffset, yOffset);
        clipRect.rewind();
        float cx = -boundsWidth * mOriginX;
        float cy = -boundsHeight * mOriginY;
        clipRect.moveTo(cx, cy);
        clipRect.lineTo(cx + boundsWidth, cy);
        clipRect.lineTo(cx + boundsWidth, cy + boundsHeight);
        clipRect.lineTo(cx, cy + boundsHeight);
        clipRect.close();
    }

    @Override
    public void draw(RiveRenderer renderer) {
        if (isHidden() || bucketCount == 0) {
            return;
        }
        boolean clipped = clip(renderer);
        renderer.save();
        renderer.transform(worldTransform);
        if (mOverflowValue == OVERFLOW_CLIPPED) {
            renderer.clipPath(clipRect);
        }
        renderer.transform(textTransform);
        for (int i = 0; i < bucketCount; i++) {
            Bucket b = buckets.get(i);
            for (int k = 0; k < b.count; k++) {
                b.style.drawPath(renderer, b.paths.get(k), b.opacities[k]);
            }
        }
        renderer.restore();
        if (clipped) {
            renderer.restore();
        }
    }

    private void layoutChanged() {
        addDirt(DIRT_TEXT_SHAPE);
    }

    @Override
    protected void alignValueChanged() {
        layoutChanged();
    }

    @Override
    protected void sizingValueChanged() {
        layoutChanged();
    }

    @Override
    protected void overflowValueChanged() {
        layoutChanged();
    }

    @Override
    protected void widthChanged() {
        layoutChanged();
    }

    @Override
    protected void heightChanged() {
        layoutChanged();
    }

    @Override
    protected void originXChanged() {
        layoutChanged();
    }

    @Override
    protected void originYChanged() {
        layoutChanged();
    }

    @Override
    protected void paragraphSpacingChanged() {
        layoutChanged();
    }

    @Override
    protected void originValueChanged() {
        layoutChanged();
    }

    @Override
    protected void wrapValueChanged() {
        layoutChanged();
    }

    @Override
    protected void verticalAlignValueChanged() {
        layoutChanged();
    }
}
