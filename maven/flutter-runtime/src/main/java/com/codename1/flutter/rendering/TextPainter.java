/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.rendering;

import com.codename1.flutter.Canvas;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.TextDirection;

/**
 * Lays out and paints a span of styled text — Flutter's {@code TextPainter}.
 * Used directly by custom painters: configure it with a {@code text} span and a
 * {@code textDirection}, call {@link #layout}, read {@link #width}/{@link
 * #height}/{@link #size}, then {@link #paint} onto a {@link Canvas}. This pass
 * captures the configuration and reports a zero-size layout; real text measuring
 * and glyph painting are deferred to the text layer.
 */
public class TextPainter {

    private Object text;
    private TextDirection textDirection;
    private TextAlign textAlign;
    private Double textScaleFactor;
    private Integer maxLines;
    private String ellipsis;
    private double width;
    private double height;
    // Whether the last layout() needed more than maxLines lines -- Flutter reports
    // only that case. Measurement ignores ellipsis and textScaleFactor.
    private boolean didExceedMaxLines;

    public TextPainter() {
    }

    public void text(Object v) {
        this.text = v;
    }

    public void textDirection(TextDirection v) {
        this.textDirection = v;
    }

    public void textAlign(TextAlign v) {
        this.textAlign = v;
    }

    public void textScaleFactor(double v) {
        this.textScaleFactor = v;
    }

    public void maxLines(int v) {
        this.maxLines = v;
    }

    /** The Dart {@code int} the transpiler passes. */
    public void maxLines(long v) {
        this.maxLines = (int) v;
    }

    public void ellipsis(String v) {
        this.ellipsis = v;
    }

    public void textWidthBasis(Object v) {
    }

    public void strutStyle(Object v) {
    }

    public void locale(Object v) {
    }

    /**
     * Computes the visual layout within the given width bounds. A null bound
     * means the Flutter default (0 / infinity).
     */
    public void layout(Double minWidth, Double maxWidth) {
        // Measured with the Codename One font the span's style resolves to, wrapping at
        // word boundaries (sizes in logical pixels, as Flutter reports them). Headless
        // there is no font to measure with, and the size stays zero.
        width = 0;
        height = 0;
        didExceedMaxLines = false;
        String plain = text instanceof com.codename1.flutter.widgets.InlineSpan
                ? ((com.codename1.flutter.widgets.InlineSpan) text).toPlainText()
                : (text == null ? "" : String.valueOf(text));
        com.codename1.ui.Font font = fontFor(text instanceof com.codename1.flutter.widgets.InlineSpan
                ? ((com.codename1.flutter.widgets.InlineSpan) text).style() : null);
        if (font == null) {
            return;
        }
        double scale = Dp.scale();
        if (scale <= 0) {
            scale = 1;
        }
        double maxPx = maxWidth == null || maxWidth.isInfinite() ? Double.MAX_VALUE : maxWidth * scale;
        int lines = 0;
        double widest = 0;
        int limit = maxLines == null ? Integer.MAX_VALUE : maxLines.intValue();
        int start = 0;
        int len = plain.length();
        while (start <= len) {
            int nl = plain.indexOf('\n', start);
            String para = plain.substring(start, nl < 0 ? len : nl);
            // Greedy word wrap of one paragraph.
            int i = 0;
            do {
                int end = i;
                int lastBreak = -1;
                while (end < para.length()) {
                    if (para.charAt(end) == ' ') {
                        lastBreak = end;
                    }
                    if (font.stringWidth(para.substring(i, end + 1)) > maxPx) {
                        break;
                    }
                    end++;
                }
                if (end < para.length()) {
                    end = lastBreak > i ? lastBreak : Math.max(end, i + 1);
                }
                widest = Math.max(widest, font.stringWidth(para.substring(i, end)));
                lines++;
                i = end;
                while (i < para.length() && para.charAt(i) == ' ') {
                    i++;
                }
            } while (i < para.length());
            if (nl < 0) {
                break;
            }
            start = nl + 1;
        }
        if (lines > limit) {
            didExceedMaxLines = true;
            lines = limit;
        }
        double w = Math.min(widest, maxPx);
        if (minWidth != null) {
            w = Math.max(w, minWidth * scale);
        }
        width = w / scale;
        height = lines * font.getHeight() / scale;
    }

    private static com.codename1.ui.Font fontFor(com.codename1.flutter.TextStyle ts) {
        if (!com.codename1.ui.Display.isInitialized()) {
            return null;
        }
        com.codename1.ui.Font base = null;
        if (ts != null) {
            base = com.codename1.flutter.fonts.FontResolver.resolve(ts.fontFamily(), ts.getFontWeight(), false);
            if (base == null && ts.getFontSize() != null) {
                base = com.codename1.flutter.fonts.FontResolver.platformFace(ts.getFontWeight(), false);
            }
        }
        if (base == null) {
            base = com.codename1.ui.Font.getDefaultFont();
        }
        if (ts != null && ts.getFontSize() != null) {
            try {
                return base.derive((float) Dp.px(ts.getFontSize()), com.codename1.ui.Font.STYLE_PLAIN);
            } catch (Exception cannotDerive) {
                return base;
            }
        }
        return base;
    }

    /// Flutter's {@code didExceedMaxLines}, as of the last {@link #layout}.
    public boolean didExceedMaxLines() {
        return didExceedMaxLines;
    }

    /// Flutter's {@code text}.
    public com.codename1.flutter.widgets.InlineSpan text() {
        return text instanceof com.codename1.flutter.widgets.InlineSpan
                ? (com.codename1.flutter.widgets.InlineSpan) text : null;
    }

    /**
     * Paints the laid-out text with its top-left at {@code offset} (an Offset).
     */
    public void paint(Canvas canvas, Object offset) {
    }

    public Size size() {
        return new Size(width, height);
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }
}
