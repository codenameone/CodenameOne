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
package com.codename1.presentation;

import com.codename1.pdf.PdfCanvas;
import com.codename1.pdf.PdfDocument;
import com.codename1.pdf.PdfImage;
import com.codename1.pdf.PdfPage;
import com.codename1.ui.Image;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.PathIterator;
import com.codename1.wordprocessing.LayoutItem;
import com.codename1.wordprocessing.Measurer;
import com.codename1.wordprocessing.PdfExport;
import com.codename1.wordprocessing.PdfMeasurer;
import com.codename1.wordprocessing.Picture;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// PDF export: one page per visible slide, at the slide's size in points, drawn by
/// `SlideRenderer` with a painter over `com.codename1.pdf.PdfCanvas`. Text is real PDF text
/// laid out with `PdfMeasurer` and drawn by `PdfExport.drawItems`, so it is selectable and
/// breaks exactly as a document's does.
public final class SlidePdfExport {

    private SlidePdfExport() {
    }

    static void write(Presentation p, OutputStream out) throws IOException {
        toPdf(p).save(out);
    }

    /// The presentation as a PDF document, one page per visible slide, for adding pages or
    /// metadata before saving.
    ///
    /// #### Parameters
    ///
    /// - `p`: the presentation
    ///
    /// #### Returns
    ///
    /// the document
    public static PdfDocument toPdf(Presentation p) {
        PdfDocument pdf = new PdfDocument();
        PdfMeasurer fonts = new PdfMeasurer();
        Map<String, PdfImage> images = new HashMap<String, PdfImage>();
        for (Slide s : p.slides) {
            if (s.hidden) {
                continue;
            }
            PdfPage page = pdf.addPage(p.width, p.height);
            PdfCanvas c = page.getCanvas();
            SlideRenderer.paintSlide(new Painter(page, c, fonts, images), p, s, null);
        }
        if (pdf.getPageCount() == 0) {
            pdf.addPage(p.width, p.height);
        }
        return pdf;
    }

    /// Paints on a PDF page, in points.
    private static final class Painter implements SlidePainter {
        private final PdfPage page;
        private final PdfCanvas c;
        private final PdfMeasurer fonts;
        private final Map<String, PdfImage> images;
        private final float[] alphas = new float[64];
        private int depth;
        private float alpha = 1;

        Painter(PdfPage page, PdfCanvas c, PdfMeasurer fonts, Map<String, PdfImage> images) {
            this.page = page;
            this.c = c;
            this.fonts = fonts;
            this.images = images;
        }

        @Override
        public Measurer measurer() {
            return fonts;
        }

        @Override
        public void push(float cx, float cy, float rotation, float dx, float dy, float scale,
                float a, float[] clip) {
            c.save();
            if (depth < alphas.length) {
                alphas[depth] = alpha;
            }
            depth++;
            alpha *= a;
            if (rotation != 0 || dx != 0 || dy != 0 || scale != 1) {
                c.translate(cx + dx, cy + dy);
                if (rotation != 0) {
                    c.rotate(Math.toRadians(rotation));
                }
                if (scale != 1) {
                    c.scale(scale, scale);
                }
                c.translate(-cx, -cy);
            }
            if (clip != null) {
                c.rect(clip[0], clip[1], clip[2], clip[3]).clip();
            }
        }

        @Override
        public void pop() {
            c.restore();
            depth--;
            alpha = depth < alphas.length ? alphas[depth] : alpha;
        }

        private void path(GeneralPath path) {
            PathIterator it = path.getPathIterator();
            float[] k = new float[6];
            while (!it.isDone()) {
                switch (it.currentSegment(k)) {
                    case PathIterator.SEG_MOVETO:
                        c.moveTo(k[0], k[1]);
                        break;
                    case PathIterator.SEG_LINETO:
                        c.lineTo(k[0], k[1]);
                        break;
                    case PathIterator.SEG_QUADTO:
                        c.quadTo(k[0], k[1], k[2], k[3]);
                        break;
                    case PathIterator.SEG_CUBICTO:
                        c.curveTo(k[0], k[1], k[2], k[3], k[4], k[5]);
                        break;
                    default:
                        c.closePath();
                        break;
                }
                it.next();
            }
        }

        private void fillPath(GeneralPath p) {
            path(p);
            if (p.getWindingRule() == GeneralPath.WIND_EVEN_ODD) {
                c.fillEvenOdd();
            } else {
                c.fill();
            }
        }

        @Override
        public void fill(GeneralPath p, int color, float a) {
            c.setFillColor(color).setFillAlpha(clamp(a * alpha));
            fillPath(p);
        }

        @Override
        public void gradient(GeneralPath p, int from, int to, float angle, float a, float x,
                float y, float w, float h) {
            // the PDF writer has no shading objects: bands of solid color under the shape's
            // clip, fine enough that adjacent bands differ by under one color step
            c.save();
            path(p);
            c.clip();
            c.setFillAlpha(clamp(a * alpha));
            float[][] bands = SlideRenderer.gradientBands(x, y, w, h, angle, 48);
            for (int i = 0; i < bands.length; i++) {
                float[] q = bands[i];
                c.setFillColor(SlideRenderer.mix(from, to, (i + 0.5f) / bands.length));
                c.moveTo(q[0], q[1]).lineTo(q[2], q[3]).lineTo(q[4], q[5]).lineTo(q[6], q[7])
                        .closePath().fill();
            }
            c.restore();
        }

        @Override
        public void stroke(GeneralPath p, LineStyle line) {
            c.setStrokeColor(line.color).setStrokeAlpha(clamp(line.alpha * alpha))
                    .setLineWidth(Math.max(0.25f, line.width)).setLineCap(1).setLineJoin(1);
            if (LineStyle.DASH.equals(line.dash)) {
                c.setDash(new float[] {line.width * 4, line.width * 3}, 0);
            } else if (LineStyle.DOT.equals(line.dash)) {
                c.setDash(new float[] {line.width, line.width * 2}, 0);
            } else {
                c.setDash(null, 0);
            }
            path(p);
            c.stroke();
            c.setDash(null, 0);
        }

        private PdfImage image(Picture p) {
            String key = p.id + ":" + p.dataLength() + ":" + p.hashCode();
            PdfImage img = images.get(key);
            if (img == null && !images.containsKey(key)) {
                if (!p.isLinked() && ("image/png".equals(p.mimeType)
                        || "image/jpeg".equals(p.mimeType))) {
                    try {
                        img = PdfImage.create(p.data());
                    } catch (IOException e) {
                        img = null;
                    }
                }
                images.put(key, img);
            }
            return img;
        }

        @Override
        public void picture(Picture p, float x, float y, float w, float h, float cl, float ct,
                float cr, float cb, boolean flipH, boolean flipV, float a) {
            PdfImage img = image(p);
            if (img == null) {
                c.setStrokeColor(0x9aa3ad).setStrokeAlpha(alpha).setLineWidth(0.5f)
                        .setDash(null, 0).strokeRect(x, y, w, h);
                return;
            }
            float fw = w / Math.max(0.01f, 1 - cl - cr);
            float fh = h / Math.max(0.01f, 1 - ct - cb);
            c.save();
            c.rect(x, y, w, h).clip();
            c.setFillAlpha(clamp(a * alpha));
            float ix = x - cl * fw;
            float iy = y - ct * fh;
            if (flipH || flipV) {
                c.translate(ix + fw / 2, iy + fh / 2);
                c.scale(flipH ? -1 : 1, flipV ? -1 : 1);
                c.translate(-(ix + fw / 2), -(iy + fh / 2));
            }
            c.drawImage(img, ix, iy, fw, fh);
            c.restore();
        }

        @Override
        public void text(List<LayoutItem> items, Measurer m, float x, float y, float s) {
            c.save();
            c.translate(x, y);
            if (s != 1) {
                c.scale(s, s);
            }
            c.setFillAlpha(alpha).setStrokeAlpha(alpha).setDash(null, 0);
            PdfExport.drawItems(page, c, items, fonts, images);
            c.restore();
        }

        @Override
        public void chart(ChartElement chart, float x, float y, float w, float h) {
            Image img = SlideRenderer.chartImage(chart, w, h);
            int[] argb = img.getRGB();
            c.save();
            c.setFillAlpha(alpha);
            c.drawImage(PdfImage.create(argb, img.getWidth(), img.getHeight()), x, y, w, h);
            c.restore();
        }

        private static float clamp(float a) {
            return Math.max(0, Math.min(1, a));
        }
    }
}
