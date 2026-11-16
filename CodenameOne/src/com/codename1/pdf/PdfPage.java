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

import com.codename1.impl.pdf.FontState;
import com.codename1.impl.pdf.PdfFormat;
import java.util.ArrayList;
import java.util.List;

/// One page of a [PdfDocument], created by [PdfDocument#addPage(PdfPageSize)].
/// Draw on it through [#getCanvas()]; add clickable areas with
/// [#addLink(float, float, float, float, String)].
@com.codename1.impl.SharedWithBackend
public final class PdfPage {
    private final PdfDocument document;
    private final float width;
    private final float height;
    private PdfCanvas canvas;
    private final List<FontState> fonts = new ArrayList<FontState>();
    private final List<String> imageNames = new ArrayList<String>();
    private final List<PdfImage> images = new ArrayList<PdfImage>();
    private final List<Object[]> links = new ArrayList<Object[]>();
    int objectNumber;

    PdfPage(PdfDocument document, float width, float height) {
        this.document = document;
        this.width = width;
        this.height = height;
    }

    PdfDocument document() {
        return document;
    }

    /// The width in points.
    public float getWidth() {
        return width;
    }

    /// The height in points.
    public float getHeight() {
        return height;
    }

    /// The zero-based position of this page in its document.
    public int getIndex() {
        return document.indexOf(this);
    }

    /// The canvas that draws this page's content.
    public PdfCanvas getCanvas() {
        if (canvas == null) {
            canvas = new PdfCanvas(this);
        }
        return canvas;
    }

    /// Makes the rectangle `(x, y, w, h)` open `uri` when clicked.
    public PdfPage addLink(float x, float y, float w, float h, String uri) {
        links.add(new Object[] {new float[] {x, y, w, h}, uri});
        return this;
    }

    /// Makes the rectangle `(x, y, w, h)` jump to the top of `target`, a
    /// page of the same document, when clicked.
    public PdfPage addLink(float x, float y, float w, float h, PdfPage target) {
        links.add(new Object[] {new float[] {x, y, w, h}, target});
        return this;
    }

    void useFont(FontState state) {
        if (!fonts.contains(state)) {
            fonts.add(state);
        }
    }

    String useImage(PdfImage image) {
        String name = document.imageName(image);
        if (!images.contains(image)) {
            images.add(image);
            imageNames.add(name);
        }
        return name;
    }

    String content() {
        return canvas == null ? "" : canvas.content();
    }

    String resources() {
        StringBuilder sb = new StringBuilder("<<");
        if (!fonts.isEmpty()) {
            sb.append("/Font <<");
            for (int i = 0; i < fonts.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                FontState f = fonts.get(i);
                sb.append('/').append(f.resourceName).append(' ').append(f.objectNumber).append(" 0 R ");
            }
            sb.append(">> ");
        }
        if (!images.isEmpty()) {
            sb.append("/XObject <<");
            for (int i = 0; i < images.size(); i++) {
                sb.append('/').append(imageNames.get(i)).append(' ')
                        .append(document.imageObject(images.get(i))).append(" 0 R ");
            }
            sb.append(">> ");
        }
        String gs = document.extGStateResources();
        if (gs.length() > 0) {
            sb.append("/ExtGState <<").append(gs).append(">> ");
        }
        sb.append(">>");
        return sb.toString();
    }

    String annotations() {
        if (links.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" /Annots [");
        for (int i = 0; i < links.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Object[] link = links.get(i);
            float[] r = (float[]) link[0];
            sb.append("<</Type /Annot /Subtype /Link /Border [0 0 0] /Rect [");
            PdfFormat.number(sb, r[0]);
            sb.append(' ');
            PdfFormat.number(sb, height - r[1] - r[3]);
            sb.append(' ');
            PdfFormat.number(sb, r[0] + r[2]);
            sb.append(' ');
            PdfFormat.number(sb, height - r[1]);
            sb.append(']');
            if (link[1] instanceof PdfPage) {
                sb.append(" /Dest [").append(((PdfPage) link[1]).objectNumber).append(" 0 R /XYZ null null null]");
            } else {
                sb.append(" /A <</S /URI /URI ");
                PdfFormat.literal(sb, String.valueOf(link[1]));
                sb.append(">>");
            }
            sb.append(">> ");
        }
        sb.append(']');
        return sb.toString();
    }
}
