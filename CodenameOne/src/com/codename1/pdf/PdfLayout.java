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

import java.util.ArrayList;
import java.util.List;

/// Flows paragraphs, tables, images and spacing down the pages of a
/// [PdfDocument], adding pages as they fill up.
///
/// ```java
/// String longText = "Revenue grew in every region this quarter.";
/// PdfDocument doc = new PdfDocument();
/// PdfLayout layout = new PdfLayout(doc, PdfPageSize.A4, 54);
/// layout.setPageDecorator(new PdfLayout.PageDecorator() {
///     public void decorate(PdfPage page, int number, int count) {
///         page.getCanvas().setFont(PdfFont.HELVETICA, 9)
///                 .drawText("Page " + number + " of " + count, 54, page.getHeight() - 30);
///     }
/// });
/// layout.add(new PdfParagraph("Quarterly report", PdfFont.HELVETICA_BOLD, 20));
/// layout.add(new PdfParagraph(longText, PdfFont.TIMES_ROMAN, 11)
///         .setAlignment(PdfParagraph.ALIGN_JUSTIFY));
/// layout.finish();
/// byte[] pdf = doc.toByteArray();
/// ```
///
/// The layout draws through the pages' ordinary canvases, so free drawing
/// can be mixed in: [#getCurrentPage()] and [#getY()] say where the flow
/// stands.
@com.codename1.impl.SharedWithBackend
public final class PdfLayout {
    /// Draws the parts of a page outside the flow: running headers,
    /// footers, page numbers.
    public interface PageDecorator {
        /// Decorates `page`, number `number` (from 1) of the `count` pages
        /// the layout produced. Called by [PdfLayout#finish()], when the
        /// total is known.
        void decorate(PdfPage page, int number, int count);
    }

    /// Images are placed against the left margin.
    public static final int ALIGN_LEFT = 0;
    /// Images are centered.
    public static final int ALIGN_CENTER = 1;
    /// Images are placed against the right margin.
    public static final int ALIGN_RIGHT = 2;

    private final PdfDocument document;
    private final PdfPageSize size;
    private float top;
    private float right;
    private float bottom;
    private float left;
    private PageDecorator decorator;
    private final List<PdfPage> pages = new ArrayList<PdfPage>();
    private PdfPage page;
    private float y;
    private boolean finished;

    /// A layout adding pages of `size` to `document`, with `margin` points
    /// on every side.
    public PdfLayout(PdfDocument document, PdfPageSize size, float margin) {
        this.document = document;
        this.size = size;
        top = right = bottom = left = margin;
    }

    /// Sets the four margins in points. Applies from the next page.
    public PdfLayout setMargins(float top, float right, float bottom, float left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
        return this;
    }

    /// Sets the decorator called for every page by [#finish()].
    public PdfLayout setPageDecorator(PageDecorator decorator) {
        this.decorator = decorator;
        return this;
    }

    /// The page the flow is on, starting one if there is none yet.
    public PdfPage getCurrentPage() {
        if (page == null) {
            newPage();
        }
        return page;
    }

    /// The distance from the top of the current page to where the next
    /// element starts.
    public float getY() {
        getCurrentPage();
        return y;
    }

    /// The width between the side margins.
    public float getContentWidth() {
        return size.getWidth() - left - right;
    }

    private float limit() {
        return size.getHeight() - bottom;
    }

    private void newPage() {
        page = document.addPage(size);
        pages.add(page);
        y = top;
    }

    private void ensure(float height) {
        getCurrentPage();
        if (y + height > limit() && y > top) {
            newPage();
        }
    }

    /// Adds vertical space. Space that runs past the bottom margin ends
    /// the page and is not carried over.
    public PdfLayout addSpace(float points) {
        getCurrentPage();
        y += points;
        if (y > limit()) {
            newPage();
        }
        return this;
    }

    /// Continues the flow at the top of a new page.
    public PdfLayout addPageBreak() {
        newPage();
        return this;
    }

    /// Adds a horizontal line across the content width.
    public PdfLayout addRule(float lineWidth, int rgb) {
        ensure(lineWidth + 8);
        y += 4;
        page.getCanvas().save().setStrokeColor(rgb).setLineWidth(lineWidth)
                .drawLine(left, y, left + getContentWidth(), y).restore();
        y += lineWidth + 4;
        return this;
    }

    /// Adds `image` `width` points wide, keeping its aspect ratio; a width
    /// wider than the content is reduced to fit, and so is an image taller
    /// than a page.
    public PdfLayout addImage(PdfImage image, float width, int alignment) {
        float w = Math.min(width, getContentWidth());
        float h = w * image.getHeight() / image.getWidth();
        float maxH = limit() - top;
        if (h > maxH) {
            w = w * maxH / h;
            h = maxH;
        }
        ensure(h);
        float x = left;
        if (alignment == ALIGN_CENTER) {
            x += (getContentWidth() - w) / 2;
        } else if (alignment == ALIGN_RIGHT) {
            x += getContentWidth() - w;
        }
        page.getCanvas().drawImage(image, x, y, w, h);
        y += h + 6;
        return this;
    }

    /// Adds a paragraph, continuing it on new pages as needed.
    public PdfLayout add(PdfParagraph paragraph) {
        getCurrentPage();
        if (y > top) {
            y += paragraph.spaceBefore;
        }
        List<PdfParagraph.Line> lines = paragraph.lines(getContentWidth());
        for (int i = 0; i < lines.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            PdfParagraph.Line line = lines.get(i);
            ensure(line.height);
            page.getCanvas().save();
            paragraph.draw(page, line, left, y, getContentWidth());
            page.getCanvas().restore();
            y += line.height;
        }
        y += paragraph.spaceAfter;
        return this;
    }

    /// Adds a table, splitting it across pages as needed.
    public PdfLayout add(PdfTable table) {
        getCurrentPage();
        float total = 0;
        for (int i = 0; i < table.weights.length; i++) {
            total += table.weights[i];
        }
        float width = getContentWidth();
        float[] colX = new float[table.weights.length + 1];
        colX[0] = left;
        for (int i = 0; i < table.weights.length; i++) {
            colX[i + 1] = colX[i] + width * table.weights[i] / total;
        }
        List<List<PdfTable.Cell>> rows = table.rows();
        int headers = Math.min(table.headerRows, rows.size());
        boolean repeat = headers > 0;
        float headerHeight = 0;
        for (int r = 0; r < rows.size(); r++) {
            if (r == headers && repeat) {
                // A header that fills more than half a page would leave a
                // line or two of body per continuation page; it is drawn
                // once and not repeated. The rows' own heights are summed,
                // since a header can itself cross a page break.
                repeat = headerHeight <= (limit() - top) / 2;
            }
            float h = drawRow(table, rows.get(r), colX, r >= headers && repeat ? rows.subList(0, headers) : null);
            if (r < headers) {
                headerHeight += h;
            }
        }
        y += table.spaceAfter;
        return this;
    }

    /// Draws one row, across pages as needed; answers the total height of
    /// its fragments.
    private float drawRow(PdfTable table, List<PdfTable.Cell> row, float[] colX,
            List<List<PdfTable.Cell>> headerRows) {
        float drawn = 0;
        int n = row.size();
        float pad = table.padding;
        float[] cx = new float[n];
        float[] cw = new float[n];
        List<List<PdfParagraph.Line>> lines = new ArrayList<List<PdfParagraph.Line>>();
        int col = 0;
        for (int i = 0; i < n; i++) {
            PdfTable.Cell c = row.get(i);
            cx[i] = colX[col];
            cw[i] = colX[col + c.colspan] - colX[col];
            col += c.colspan;
            lines.add(c.paragraph.lines(Math.max(1, cw[i] - 2 * pad)));
        }
        int[] next = new int[n];
        while (true) {
            float avail = limit() - y - 2 * pad;
            float firstLine = 0;
            for (int i = 0; i < n; i++) {
                if (next[i] < lines.get(i).size()) {
                    firstLine = Math.max(firstLine, lines.get(i).get(next[i]).height);
                }
            }
            if (firstLine > avail && y > top) {
                newPage();
                if (headerRows != null) {
                    for (int h = 0; h < headerRows.size(); h++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                        drawRow(table, headerRows.get(h), colX, null);
                    }
                }
                avail = limit() - y - 2 * pad;
            }
            float rowHeight = 0;
            int[] end = new int[n];
            for (int i = 0; i < n; i++) {
                List<PdfParagraph.Line> ls = lines.get(i);
                float h = 0;
                int k = next[i];
                while (k < ls.size() && (h + ls.get(k).height <= avail || k == next[i])) {
                    h += ls.get(k).height;
                    k++;
                }
                end[i] = k;
                rowHeight = Math.max(rowHeight, h);
            }
            float height = rowHeight + 2 * pad;
            PdfCanvas c = page.getCanvas();
            c.save();
            for (int i = 0; i < n; i++) {
                PdfTable.Cell cell = row.get(i);
                if (cell.background >= 0) {
                    c.setFillColor(cell.background).fillRect(cx[i], y, cw[i], height);
                }
                float ly = y + pad;
                for (int k = next[i]; k < end[i]; k++) {
                    PdfParagraph.Line line = lines.get(i).get(k);
                    c.save();
                    cell.paragraph.draw(page, line, cx[i] + pad, ly, cw[i] - 2 * pad);
                    c.restore();
                    ly += line.height;
                }
                if (table.borderWidth > 0) {
                    c.setStrokeColor(table.borderColor).setLineWidth(table.borderWidth)
                            .strokeRect(cx[i], y, cw[i], height);
                }
            }
            c.restore();
            y += height;
            drawn += height;
            boolean done = true;
            for (int i = 0; i < n; i++) {
                next[i] = end[i];
                if (next[i] < lines.get(i).size()) {
                    done = false;
                }
            }
            if (done) {
                return drawn;
            }
            newPage();
            if (headerRows != null) {
                for (int h = 0; h < headerRows.size(); h++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                    drawRow(table, headerRows.get(h), colX, null);
                }
            }
        }
    }

    /// Runs the page decorator over every page this layout added. Call it
    /// once, after the last element.
    public void finish() {
        if (finished) {
            return;
        }
        finished = true;
        if (decorator == null) {
            return;
        }
        for (int i = 0; i < pages.size(); i++) {
            PdfPage p = pages.get(i);
            p.getCanvas().beginOverlay();
            decorator.decorate(p, i + 1, pages.size());
        }
    }
}
