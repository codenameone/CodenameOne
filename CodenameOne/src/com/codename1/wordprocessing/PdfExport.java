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
package com.codename1.wordprocessing;

import com.codename1.pdf.PdfCanvas;
import com.codename1.pdf.PdfDocument;
import com.codename1.pdf.PdfImage;
import com.codename1.pdf.PdfPage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/// Writes a laid out document as PDF, page for page as `DocumentView` shows it.
///
/// ```java
/// DocumentModel model = new DocumentModel(new Document());
/// model.insertBlock(0, new Paragraph(model.newId('p'), RichText.of("Quarterly review")));
/// PdfMeasurer fonts = new PdfMeasurer();
/// ByteArrayOutputStream out = new ByteArrayOutputStream();
/// try {
///     PdfExport.write(new DocumentLayout(model.getDocument(), fonts), fonts, out);
///     Log.p("PDF bytes: " + out.size());
/// } catch (IOException e) {
///     Log.e(e);
/// }
/// ```
///
/// Links become link annotations, tracked insertions are underlined and deletions struck
/// through, and an embedded component is drawn from its snapshot, or as an empty frame when it
/// has none. Pictures other than PNG and JPEG are drawn as frames.
public final class PdfExport {
    private static final int INSERT_COLOR = 0x1f6feb;
    private static final int DELETE_COLOR = 0xc0392b;

    private PdfExport() {
    }

    /// Lays a document out with the standard PDF fonts and writes it.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document
    ///
    /// - `out`: the stream; it is flushed but not closed
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the stream fails
    public static void write(Document doc, OutputStream out) throws IOException {
        PdfMeasurer fonts = new PdfMeasurer();
        PdfDocument pdf = toPdf(new DocumentLayout(doc, fonts), fonts);
        if (doc.title != null && doc.title.length() > 0) {
            pdf.setTitle(doc.title);
        }
        pdf.save(out);
        out.flush();
    }

    /// Writes a layout.
    ///
    /// #### Parameters
    ///
    /// - `layout`: the layout; for the text to fit its lines it should have been made with
    ///   `fonts`
    ///
    /// - `fonts`: the fonts text is drawn in
    ///
    /// - `out`: the stream; it is flushed but not closed
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the stream fails
    public static void write(DocumentLayout layout, PdfMeasurer fonts, OutputStream out)
            throws IOException {
        toPdf(layout, fonts).save(out);
        out.flush();
    }

    /// Draws a layout into a new PDF document, for callers that add metadata or pages of
    /// their own before saving.
    ///
    /// #### Parameters
    ///
    /// - `layout`: the layout
    ///
    /// - `fonts`: the fonts text is drawn in
    ///
    /// #### Returns
    ///
    /// the PDF document
    public static PdfDocument toPdf(DocumentLayout layout, PdfMeasurer fonts) {
        PdfDocument pdf = new PdfDocument();
        Map<String, PdfImage> images = new HashMap<String, PdfImage>();
        for (int i = 0; i < layout.getPageCount(); i++) {
            LayoutPage lp = layout.getPage(i);
            PdfPage page = pdf.addPage(lp.getWidth(), lp.getHeight());
            PdfCanvas c = page.getCanvas();
            for (LayoutItem it : lp.getItems()) {
                paint(page, c, it, fonts, images);
            }
        }
        return pdf;
    }

    private static void paint(PdfPage page, PdfCanvas c, LayoutItem it, PdfMeasurer fonts,
            Map<String, PdfImage> images) {
        switch (it.kind) {
            case LayoutItem.FILL:
                c.setFillColor(it.color).fillRect(it.x, it.y, it.width, it.height);
                break;
            case LayoutItem.LINE:
                c.setStrokeColor(it.color).setLineWidth(0.5f)
                        .drawLine(it.x, it.y, it.x + it.width, it.y + it.height);
                break;
            case LayoutItem.TEXT:
                text(page, c, it, fonts);
                break;
            case LayoutItem.PICTURE:
                image(c, it, it.picture, images);
                break;
            case LayoutItem.COMPONENT:
                Picture snap = it.component.toPicture("snapshot");
                if (snap != null) {
                    image(c, it, snap, images);
                } else {
                    frame(c, it);
                }
                break;
            default:
                break;
        }
    }

    private static void text(PdfPage page, PdfCanvas c, LayoutItem it, PdfMeasurer fonts) {
        String s = it.text;
        if (s == null || s.length() == 0) {
            return;
        }
        TextStyle st = it.style;
        Revision r = st.revision;
        int color = r == null ? st.color : r.isDeletion() ? DELETE_COLOR : INSERT_COLOR;
        float size = PdfMeasurer.drawnSize(st);
        c.setFillColor(color).setFont(fonts.font(st), size).drawText(s, it.x, it.baseline);
        String trimmed = s;
        while (trimmed.endsWith(" ")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        float w = fonts.width(trimmed, st);
        boolean underline = st.underline || r != null && !r.isDeletion()
                || st.link != null && st.link.length() > 0;
        if (underline && w > 0) {
            c.setStrokeColor(color).setLineWidth(Math.max(0.5f, size / 18))
                    .drawLine(it.x, it.baseline + size * 0.12f, it.x + w,
                            it.baseline + size * 0.12f);
        }
        if ((st.strike || r != null && r.isDeletion()) && w > 0) {
            float y = it.baseline - size * 0.3f;
            c.setStrokeColor(color).setLineWidth(Math.max(0.5f, size / 18))
                    .drawLine(it.x, y, it.x + w, y);
        }
        if (st.link != null && st.link.length() > 0 && !st.link.startsWith("#") && w > 0) {
            page.addLink(it.x, it.y, w, it.height, st.link);
        }
    }

    private static void image(PdfCanvas c, LayoutItem it, Picture p,
            Map<String, PdfImage> images) {
        String key = p.id + ":" + p.dataLength() + ":" + p.hashCode();
        PdfImage img = images.get(key);
        if (img == null && !images.containsKey(key)) {
            if ("image/png".equals(p.mimeType) || "image/jpeg".equals(p.mimeType)) {
                try {
                    img = PdfImage.create(p.data());
                } catch (IOException e) {
                    img = null;
                }
            }
            images.put(key, img);
        }
        if (img != null) {
            c.drawImage(img, it.x, it.y, it.width, it.height);
        } else {
            frame(c, it);
        }
    }

    private static void frame(PdfCanvas c, LayoutItem it) {
        c.setStrokeColor(0x9aa3ad).setLineWidth(0.5f).strokeRect(it.x, it.y, it.width,
                it.height);
    }
}
