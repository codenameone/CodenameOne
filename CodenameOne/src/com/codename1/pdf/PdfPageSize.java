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

/// A page size in points (1/72 inch).
@com.codename1.impl.SharedWithBackend
public final class PdfPageSize {
    /// ISO A3, 297 by 420 mm.
    public static final PdfPageSize A3 = new PdfPageSize(841.89f, 1190.55f);
    /// ISO A4, 210 by 297 mm.
    public static final PdfPageSize A4 = new PdfPageSize(595.28f, 841.89f);
    /// ISO A5, 148 by 210 mm.
    public static final PdfPageSize A5 = new PdfPageSize(419.53f, 595.28f);
    /// US Letter, 8.5 by 11 inches.
    public static final PdfPageSize LETTER = new PdfPageSize(612, 792);
    /// US Legal, 8.5 by 14 inches.
    public static final PdfPageSize LEGAL = new PdfPageSize(612, 1008);
    /// US Tabloid, 11 by 17 inches.
    public static final PdfPageSize TABLOID = new PdfPageSize(792, 1224);

    private final float width;
    private final float height;

    /// A size of `width` by `height` points.
    public PdfPageSize(float width, float height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        this.width = width;
        this.height = height;
    }

    /// A size of `widthMm` by `heightMm` millimeters.
    public static PdfPageSize millimeters(float widthMm, float heightMm) {
        return new PdfPageSize(widthMm * 72f / 25.4f, heightMm * 72f / 25.4f);
    }

    /// The width in points.
    public float getWidth() {
        return width;
    }

    /// The height in points.
    public float getHeight() {
        return height;
    }

    /// This size turned on its side: the longer edge horizontal.
    public PdfPageSize landscape() {
        return width >= height ? this : new PdfPageSize(height, width);
    }

    /// This size upright: the longer edge vertical.
    public PdfPageSize portrait() {
        return height >= width ? this : new PdfPageSize(height, width);
    }
}
