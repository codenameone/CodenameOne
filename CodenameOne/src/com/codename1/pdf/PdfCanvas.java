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
package com.codename1.pdf;

import com.codename1.util.Bidi;
import com.codename1.impl.pdf.FontState;
import com.codename1.impl.pdf.PdfFormat;

/// Draws the content of one [PdfPage].
///
/// Coordinates are in points (1/72 inch) with the origin at the **top
/// left** of the page and `y` growing downwards, as in Codename One's
/// `Graphics`; text is placed by its baseline. Shapes are built as a path
/// and then filled, stroked or used as a clip:
///
/// ```java
/// PdfDocument doc = new PdfDocument();
/// PdfPage page = doc.addPage(PdfPageSize.A4);
/// PdfCanvas c = page.getCanvas();
/// c.setFillColor(0x1565c0);
/// c.fillRect(36, 36, page.getWidth() - 72, 40);
/// c.setFillColor(0xffffff);
/// c.setFont(PdfFont.HELVETICA_BOLD, 18);
/// c.drawText("Invoice 2031", 48, 62);
/// c.setStrokeColor(0x999999);
/// c.setLineWidth(0.5f);
/// c.drawLine(36, 90, page.getWidth() - 36, 90);
/// ```
///
/// [#save()] and [#restore()] bracket changes to the color, line style,
/// transform, clip and font.
@com.codename1.impl.SharedWithBackend
public final class PdfCanvas {
    /// Butt line cap: the stroke ends at the end point.
    public static final int CAP_BUTT = 0;
    /// Round line cap.
    public static final int CAP_ROUND = 1;
    /// Square line cap: the stroke extends half its width past the end.
    public static final int CAP_SQUARE = 2;
    /// Mitered line join.
    public static final int JOIN_MITER = 0;
    /// Round line join.
    public static final int JOIN_ROUND = 1;
    /// Beveled line join.
    public static final int JOIN_BEVEL = 2;

    private static final double KAPPA = 0.5522847498;

    private final PdfPage page;
    private final StringBuilder content = new StringBuilder(); // NOPMD AvoidStringBufferField - the builder accumulates the output for the object lifetime
    private PdfFont font;
    private float fontSize;
    private FontState fontState;
    private PdfFont[] savedFonts = new PdfFont[8];
    private float[] savedSizes = new float[8];
    private FontState[] savedStates = new FontState[8];
    private int depth;
    private float curX;
    private float curY;

    PdfCanvas(PdfPage page) {
        this.fontState = null;
        this.page = page;
        // The page's own drawing runs inside an outer q, so an overlay can
        // start again from the default state whatever was left open.
        content.append("q\n");
        baseMatrix();
    }

    private void baseMatrix() {
        content.append("1 0 0 -1 0 ");
        PdfFormat.number(content, page.getHeight());
        content.append(" cm\n");
    }

    /// Closes every open save and every transform, clip or color the page
    /// content left behind, so what is drawn next (a header or footer)
    /// starts from the default state.
    void beginOverlay() {
        while (depth > 0) {
            restore();
        }
        content.append("Q\nq\n");
        baseMatrix();
    }

    String content() {
        // Close what the caller left saved, then the outer q, so the stream
        // is balanced however the canvas was used; the builder is untouched.
        StringBuilder close = new StringBuilder(content.length() + 2 * depth + 2);
        close.append(content);
        for (int i = 0; i < depth; i++) {
            close.append("Q\n");
        }
        return close.append("Q\n").toString();
    }

    private PdfCanvas op(String operator) {
        content.append(operator).append('\n');
        return this;
    }

    private void n(double v) {
        PdfFormat.number(content, v);
        content.append(' ');
    }

    /// Saves the graphics state: colors, line style, transform, clip and
    /// font.
    public PdfCanvas save() {
        if (depth == savedFonts.length) {
            PdfFont[] f = new PdfFont[depth * 2];
            float[] z = new float[depth * 2];
            FontState[] st = new FontState[depth * 2];
            System.arraycopy(savedFonts, 0, f, 0, depth);
            System.arraycopy(savedSizes, 0, z, 0, depth);
            System.arraycopy(savedStates, 0, st, 0, depth);
            savedFonts = f;
            savedSizes = z;
            savedStates = st;
        }
        savedFonts[depth] = font;
        savedSizes[depth] = fontSize;
        savedStates[depth] = fontState;
        depth++;
        return op("q");
    }

    /// Restores the state saved by the matching [#save()].
    public PdfCanvas restore() {
        if (depth == 0) {
            throw new IllegalStateException("restore() without save()");
        }
        depth--;
        font = savedFonts[depth];
        fontSize = savedSizes[depth];
        fontState = savedStates[depth];
        savedFonts[depth] = null;
        savedStates[depth] = null;
        return op("Q");
    }

    /// Moves the origin by `dx`, `dy`.
    public PdfCanvas translate(float dx, float dy) {
        return transform(1, 0, 0, 1, dx, dy);
    }

    /// Scales later drawing by `sx`, `sy` around the origin.
    public PdfCanvas scale(float sx, float sy) {
        return transform(sx, 0, 0, sy, 0, 0);
    }

    /// Rotates later drawing by `radians` clockwise around the origin.
    public PdfCanvas rotate(double radians) {
        float c = (float) Math.cos(radians);
        float s = (float) Math.sin(radians);
        return transform(c, s, -s, c, 0, 0);
    }

    /// Concatenates the matrix `[a b c d e f]` with the current transform:
    /// a point `(x, y)` maps to `(a*x + c*y + e, b*x + d*y + f)`.
    public PdfCanvas transform(float a, float b, float c, float d, float e, float f) {
        n(a);
        n(b);
        n(c);
        n(d);
        n(e);
        n(f);
        return op("cm");
    }

    /// Sets the color shapes and text are filled with, `0xRRGGBB`.
    public PdfCanvas setFillColor(int rgb) {
        color(rgb);
        return op("rg");
    }

    /// Sets the color lines are stroked with, `0xRRGGBB`.
    public PdfCanvas setStrokeColor(int rgb) {
        color(rgb);
        return op("RG");
    }

    private void color(int rgb) {
        n(((rgb >> 16) & 0xff) / 255.0);
        n(((rgb >> 8) & 0xff) / 255.0);
        n((rgb & 0xff) / 255.0);
    }

    /// Sets the opacity of fills and text, from 0 (invisible) to 1.
    public PdfCanvas setFillAlpha(float alpha) {
        content.append('/').append(page.document().extGState("ca", clamp(alpha))).append(' ');
        return op("gs");
    }

    /// Sets the opacity of strokes, from 0 (invisible) to 1.
    public PdfCanvas setStrokeAlpha(float alpha) {
        content.append('/').append(page.document().extGState("CA", clamp(alpha))).append(' ');
        return op("gs");
    }

    private static float clamp(float a) {
        return a < 0 ? 0 : a > 1 ? 1 : a;
    }

    /// Sets the width of stroked lines in points.
    public PdfCanvas setLineWidth(float width) {
        n(width);
        return op("w");
    }

    /// Sets the line cap, one of the `CAP_` constants.
    public PdfCanvas setLineCap(int cap) {
        content.append(cap).append(' ');
        return op("J");
    }

    /// Sets the line join, one of the `JOIN_` constants.
    public PdfCanvas setLineJoin(int join) {
        content.append(join).append(' ');
        return op("j");
    }

    /// Sets the miter limit of mitered joins.
    public PdfCanvas setMiterLimit(float limit) {
        n(limit);
        return op("M");
    }

    /// Sets the dash pattern: alternating dash and gap lengths, starting
    /// `phase` points into the pattern. Null or empty draws solid lines.
    public PdfCanvas setDash(float[] pattern, float phase) {
        content.append('[');
        if (pattern != null) {
            for (int i = 0; i < pattern.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                n(pattern[i]);
            }
        }
        content.append("] ");
        n(phase);
        return op("d");
    }

    /// Starts a new subpath at `(x, y)`.
    public PdfCanvas moveTo(float x, float y) {
        n(x);
        n(y);
        curX = x;
        curY = y;
        return op("m");
    }

    /// Adds a straight segment to `(x, y)`.
    public PdfCanvas lineTo(float x, float y) {
        n(x);
        n(y);
        curX = x;
        curY = y;
        return op("l");
    }

    /// Adds a cubic Bezier segment through control points `(x1, y1)` and
    /// `(x2, y2)` to `(x3, y3)`.
    public PdfCanvas curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        n(x1);
        n(y1);
        n(x2);
        n(y2);
        n(x3);
        n(y3);
        curX = x3;
        curY = y3;
        return op("c");
    }

    /// Adds a quadratic Bezier segment through control point `(cx, cy)` to
    /// `(x, y)`.
    public PdfCanvas quadTo(float cx, float cy, float x, float y) {
        float x1 = curX + 2f / 3f * (cx - curX);
        float y1 = curY + 2f / 3f * (cy - curY);
        float x2 = x + 2f / 3f * (cx - x);
        float y2 = y + 2f / 3f * (cy - y);
        return curveTo(x1, y1, x2, y2, x, y);
    }

    /// Closes the current subpath with a straight segment to its start.
    public PdfCanvas closePath() {
        return op("h");
    }

    /// Adds a rectangle as a closed subpath.
    public PdfCanvas rect(float x, float y, float w, float h) {
        n(x);
        n(y);
        n(w);
        n(h);
        return op("re");
    }

    /// Adds a rectangle with corners rounded to `radius` points.
    public PdfCanvas roundRect(float x, float y, float w, float h, float radius) {
        float r = Math.min(radius, Math.min(w, h) / 2);
        if (r <= 0) {
            return rect(x, y, w, h);
        }
        float k = (float) (r * KAPPA);
        moveTo(x + r, y);
        lineTo(x + w - r, y);
        curveTo(x + w - r + k, y, x + w, y + r - k, x + w, y + r);
        lineTo(x + w, y + h - r);
        curveTo(x + w, y + h - r + k, x + w - r + k, y + h, x + w - r, y + h);
        lineTo(x + r, y + h);
        curveTo(x + r - k, y + h, x, y + h - r + k, x, y + h - r);
        lineTo(x, y + r);
        curveTo(x, y + r - k, x + r - k, y, x + r, y);
        return closePath();
    }

    /// Adds the ellipse inscribed in the given rectangle.
    public PdfCanvas ellipse(float x, float y, float w, float h) {
        float rx = w / 2;
        float ry = h / 2;
        float cx = x + rx;
        float cy = y + ry;
        float kx = (float) (rx * KAPPA);
        float ky = (float) (ry * KAPPA);
        moveTo(cx + rx, cy);
        curveTo(cx + rx, cy + ky, cx + kx, cy + ry, cx, cy + ry);
        curveTo(cx - kx, cy + ry, cx - rx, cy + ky, cx - rx, cy);
        curveTo(cx - rx, cy - ky, cx - kx, cy - ry, cx, cy - ry);
        curveTo(cx + kx, cy - ry, cx + rx, cy - ky, cx + rx, cy);
        return closePath();
    }

    /// Fills the current path (nonzero winding) and ends it.
    public PdfCanvas fill() {
        return op("f");
    }

    /// Fills the current path with the even-odd rule and ends it.
    public PdfCanvas fillEvenOdd() {
        return op("f*");
    }

    /// Strokes the current path and ends it.
    public PdfCanvas stroke() {
        return op("S");
    }

    /// Fills, then strokes the current path, and ends it.
    public PdfCanvas fillAndStroke() {
        return op("B");
    }

    /// Intersects the clip with the current path (nonzero winding) and
    /// ends the path. Bracket with [#save()] and [#restore()] to undo.
    public PdfCanvas clip() {
        return op("W n");
    }

    /// Intersects the clip with the current path (even-odd) and ends it.
    public PdfCanvas clipEvenOdd() {
        return op("W* n");
    }

    /// Fills a rectangle.
    public PdfCanvas fillRect(float x, float y, float w, float h) {
        return rect(x, y, w, h).fill();
    }

    /// Strokes the outline of a rectangle.
    public PdfCanvas strokeRect(float x, float y, float w, float h) {
        return rect(x, y, w, h).stroke();
    }

    /// Strokes a line from `(x1, y1)` to `(x2, y2)`.
    public PdfCanvas drawLine(float x1, float y1, float x2, float y2) {
        return moveTo(x1, y1).lineTo(x2, y2).stroke();
    }

    /// Sets the font later text is drawn with.
    public PdfCanvas setFont(PdfFont font, float size) {
        this.font = font;
        this.fontSize = size;
        this.fontState = page.document().fontState(font);
        return this;
    }

    /// The current font, null before [#setFont(PdfFont, float)].
    public PdfFont getFont() {
        return font;
    }

    /// The current font size in points.
    public float getFontSize() {
        return fontSize;
    }

    /// Draws `text` in the current font and fill color with its baseline
    /// starting at `(x, y)`. Line breaks are not interpreted; lay out
    /// several lines with [PdfLayout].
    ///
    /// Text is given in reading order. Right-to-left scripts (Hebrew,
    /// Arabic) are reordered for display by the Unicode bidirectional
    /// algorithm, with the direction taken from the first strong character,
    /// and Arabic letters are joined, using the font's own contextual forms
    /// when it is a TrueType font that has them.
    public PdfCanvas drawText(String text, float x, float y) {
        return drawText(text, x, y, PdfParagraph.DIRECTION_AUTO);
    }

    /// Draws `text` as [#drawText(String, float, float)] does, in a
    /// paragraph of direction `direction`, one of the
    /// `PdfParagraph.DIRECTION_` constants. The text still starts at `x`.
    public PdfCanvas drawText(String text, float x, float y, int direction) {
        if (font == null) {
            setFont(PdfFont.HELVETICA, 12);
        }
        boolean rtl = direction == PdfParagraph.DIRECTION_RTL
                || direction == PdfParagraph.DIRECTION_AUTO && Bidi.direction(text) == Bidi.RTL;
        if (Bidi.isLeftToRight(text, rtl)) {
            drawRun(text, x, y, false);
            return this;
        }
        byte[] levels = Bidi.levels(text, rtl);
        float cx = x;
        int[][] runs = visualRuns(levels);
        for (int i = 0; i < runs.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            String piece = text.substring(runs[i][0], runs[i][1]);
            drawRun(piece, cx, y, (levels[runs[i][0]] & 1) != 0);
            cx += font.getStringWidth(piece, fontSize);
        }
        return this;
    }

    /// The level runs of `levels` as `{start, end}` pairs, left to right.
    static int[][] visualRuns(byte[] levels) {
        int n = levels.length;
        java.util.List<int[]> runs = new java.util.ArrayList<int[]>();
        int start = 0;
        for (int i = 1; i <= n; i++) {
            if (i == n || levels[i] != levels[start]) {
                runs.add(new int[] {start, i});
                start = i;
            }
        }
        byte[] rl = new byte[runs.size()];
        for (int i = 0; i < rl.length; i++) {
            rl[i] = levels[runs.get(i)[0]];
        }
        int[] order = Bidi.visualOrder(rl);
        int[][] out = new int[order.length][];
        for (int i = 0; i < order.length; i++) {
            out[i] = runs.get(order[i]);
        }
        return out;
    }

    /// Draws one directional run, `text` in logical order.
    void drawRun(String text, float x, float y, boolean rtl) {
        page.useFont(fontState);
        content.append("BT /").append(fontState.resourceName).append(' ');
        n(fontSize);
        content.append("Tf 1 0 0 -1 ");
        n(x);
        n(y);
        content.append("Tm ");
        fontState.showText(content, text, rtl);
        op(" Tj ET");
    }

    /// Draws `image` scaled into the rectangle `(x, y, w, h)`.
    public PdfCanvas drawImage(PdfImage image, float x, float y, float w, float h) {
        String name = page.useImage(image);
        content.append("q ");
        n(w);
        content.append("0 0 ");
        n(-h);
        n(x);
        n(y + h);
        content.append("cm /").append(name);
        return op(" Do Q");
    }

    /// Appends raw content-stream operators, for what this class does not
    /// cover. The text must be valid PDF content in the page's flipped
    /// coordinate system; nothing checks it.
    public PdfCanvas appendRaw(String operators) {
        return op(operators);
    }
}
