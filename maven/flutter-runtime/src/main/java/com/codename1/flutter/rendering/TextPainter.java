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
        // Measurement deferred; dimensions remain zero for this pass.
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
