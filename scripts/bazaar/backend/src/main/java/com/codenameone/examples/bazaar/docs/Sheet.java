/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.docs;

import com.codenameone.examples.bazaar.ops.Amounts;
import com.codename1.pdf.PdfCanvas;
import com.codename1.pdf.PdfDocument;
import com.codename1.pdf.PdfFont;
import com.codename1.pdf.PdfLayout;
import com.codename1.pdf.PdfPage;
import com.codename1.pdf.PdfPageSize;
import com.codename1.pdf.PdfParagraph;
import com.codename1.pdf.PdfTable;
import com.codenameone.examples.bazaar.Days;

/// The house style every Bazaar document shares: a band in the store's
/// colour carrying its name and the document's title, a footer with the
/// store's contact line and page numbers, and helpers for the paragraphs,
/// tables and money in between.
final class Sheet {
    static final int INK = 0x1f2328;
    static final int MUTED = 0x6e7781;
    static final int RULE = 0xd0d7de;
    static final int ZEBRA = 0xf6f8fa;
    static final int DEFAULT_ACCENT = 0x1f6feb;
    static final float MARGIN = 48;
    static final float BAND = 86;

    final PdfDocument doc = new PdfDocument();
    final PdfLayout layout;
    final int accent;

    /// @param accent the store's colour, 0 for the default blue
    Sheet(final String store, final String title, final String reference, int accent,
            final String footer) {
        this.accent = accent == 0 ? DEFAULT_ACCENT : accent & 0xffffff;
        doc.setTitle(title + " " + reference).setAuthor(store).setCreator("Bazaar");
        layout = new PdfLayout(doc, PdfPageSize.A4, MARGIN);
        layout.setMargins(BAND + 28, MARGIN, 60, MARGIN);
        final int band = this.accent;
        layout.setPageDecorator(new PdfLayout.PageDecorator() {
            @Override
            public void decorate(PdfPage page, int number, int count) {
                PdfCanvas c = page.getCanvas();
                float w = page.getWidth();
                c.setFillColor(band).fillRect(0, 0, w, BAND);
                c.setFillColor(0xffffff).setFont(PdfFont.HELVETICA_BOLD, 22)
                        .drawText(fit(store, PdfFont.HELVETICA_BOLD, 22, w / 2), MARGIN, 50);
                c.setFont(PdfFont.HELVETICA_BOLD, 16);
                float tw = PdfFont.HELVETICA_BOLD.getStringWidth(title, 16);
                c.drawText(title, w - MARGIN - tw, 44);
                c.setFont(PdfFont.HELVETICA, 10);
                float rw = PdfFont.HELVETICA.getStringWidth(reference, 10);
                c.drawText(reference, w - MARGIN - rw, 62);
                float h = page.getHeight();
                c.setStrokeColor(RULE).setLineWidth(0.5f).drawLine(MARGIN, h - 44, w - MARGIN,
                        h - 44);
                c.setFillColor(MUTED).setFont(PdfFont.HELVETICA, 8)
                        .drawText(fit(footer, PdfFont.HELVETICA, 8, w - 2 * MARGIN - 70),
                                MARGIN, h - 30);
                String pages = "Page " + number + " of " + count;
                c.drawText(pages, w - MARGIN - PdfFont.HELVETICA.getStringWidth(pages, 8),
                        h - 30);
            }
        });
    }

    Sheet heading(String text) {
        layout.add(new PdfParagraph(text, PdfFont.HELVETICA_BOLD, 11).setSpaceBefore(6)
                .setSpaceAfter(4));
        return this;
    }

    Sheet text(String text) {
        layout.add(new PdfParagraph(text, PdfFont.HELVETICA, 10));
        return this;
    }

    Sheet muted(String text) {
        layout.add(new PdfParagraph().append(text, PdfFont.HELVETICA, 9, MUTED));
        return this;
    }

    Sheet space(float points) {
        layout.addSpace(points);
        return this;
    }

    Sheet rule() {
        layout.addRule(0.5f, RULE);
        return this;
    }

    /// Two blocks side by side, each a title over lines of text.
    Sheet blocks(String leftTitle, String[] left, String rightTitle, String[] right) {
        PdfTable t = new PdfTable(1, 1).setCellPadding(2).setBorder(0, 0xffffff)
                .setSpaceAfter(8);
        t.addCell(new PdfParagraph().append(leftTitle,
                PdfFont.HELVETICA_BOLD, 8, MUTED));
        t.addCell(new PdfParagraph().append(rightTitle,
                PdfFont.HELVETICA_BOLD, 8, MUTED));
        t.addCell(lines(left));
        t.addCell(lines(right));
        layout.add(t);
        return this;
    }

    private static PdfParagraph lines(String[] rows) {
        PdfParagraph p = new PdfParagraph();
        boolean first = true;
        for (int iter = 0; iter < rows.length; iter++) {
            if (rows[iter] == null || rows[iter].length() == 0) {
                continue;
            }
            p.append((first ? "" : "\n") + rows[iter], first ? PdfFont.HELVETICA_BOLD
                    : PdfFont.HELVETICA, 10, INK);
            first = false;
        }
        if (first) {
            p.append("-", PdfFont.HELVETICA, 10, MUTED);
        }
        return p;
    }

    /// A table whose first row is a header in the store's colour; `right`
    /// marks the columns aligned right (numbers).
    Table table(float[] weights, String[] header, boolean[] right) {
        return new Table(weights, header, right);
    }

    /// Label and value rows aligned right under a table: subtotal, tax,
    /// total. The last row is set in bold when `strongLast`.
    Sheet totals(String[][] rows, boolean strongLast) {
        PdfTable t = new PdfTable(3, 1.4f, 1).setCellPadding(3).setBorder(0, 0xffffff)
                .setSpaceAfter(10);
        for (int iter = 0; iter < rows.length; iter++) {
            boolean strong = strongLast && iter == rows.length - 1;
            PdfFont f = strong ? PdfFont.HELVETICA_BOLD : PdfFont.HELVETICA;
            float size = strong ? 12 : 10;
            int bg = strong ? ZEBRA : -1;
            t.addCell(new PdfParagraph(), 1, -1);
            t.addCell(new PdfParagraph(rows[iter][0], f, size)
                    .setAlignment(PdfParagraph.ALIGN_RIGHT), 1, bg);
            t.addCell(new PdfParagraph(rows[iter][1], f, size)
                    .setAlignment(PdfParagraph.ALIGN_RIGHT), 1, bg);
        }
        layout.add(t);
        return this;
    }

    byte[] finish() throws java.io.IOException {
        layout.finish();
        return doc.toByteArray();
    }

    /// A table of items.
    final class Table {
        private final PdfTable table;
        private final boolean[] right;
        private int row;

        Table(float[] weights, String[] header, boolean[] right) {
            this.right = right;
            table = new PdfTable(weights).setHeaderRows(1).setCellPadding(5)
                    .setBorder(0.25f, RULE).setSpaceAfter(6);
            for (int iter = 0; iter < header.length; iter++) {
                table.addCell(align(new PdfParagraph().append(header[iter],
                        PdfFont.HELVETICA_BOLD, 9, 0xffffff), iter), 1, accent);
            }
        }

        Table row(String... cells) {
            int bg = row++ % 2 == 1 ? ZEBRA : -1;
            for (int iter = 0; iter < cells.length; iter++) {
                table.addCell(align(new PdfParagraph(cells[iter] == null ? "" : cells[iter],
                        PdfFont.HELVETICA, 9.5f), iter), 1, bg);
            }
            return this;
        }

        /// A row whose first cell is a box to tick.
        Table checkRow(String... cells) {
            int bg = row++ % 2 == 1 ? ZEBRA : -1;
            // ZapfDingbats 'o' is a square box, which is what a packer ticks.
            table.addCell(new PdfParagraph("o", PdfFont.ZAPF_DINGBATS, 12), 1, bg);
            for (int iter = 0; iter < cells.length; iter++) {
                table.addCell(align(new PdfParagraph(cells[iter] == null ? "" : cells[iter],
                        PdfFont.HELVETICA, 9.5f), iter + 1), 1, bg);
            }
            return this;
        }

        private PdfParagraph align(PdfParagraph p, int column) {
            return column < right.length && right[column]
                    ? p.setAlignment(PdfParagraph.ALIGN_RIGHT) : p;
        }

        Sheet end() {
            layout.add(table);
            return Sheet.this;
        }
    }

    // ---------------------------------------------------------------- text

    /// Minor units as money in the currency's own decimals: `$41.59`,
    /// `EUR 12.00`, `JPY 1,250`; negative with a leading minus.
    /// Integer arithmetic only.
    static String money(long cents, String currency) {
        StringBuilder out = new StringBuilder();
        if (cents < 0) {
            out.append('-');
            cents = -cents;
        }
        String c = currency == null ? "" : currency;
        if ("USD".equalsIgnoreCase(c) || "CAD".equalsIgnoreCase(c) || "AUD".equalsIgnoreCase(c)
                || c.length() == 0) {
            out.append('$');
        } else {
            out.append(c).append(' ');
        }
        long unit = Amounts.unitOf(c);
        String whole = Long.toString(cents / unit);
        for (int iter = 0; iter < whole.length(); iter++) {
            if (iter > 0 && (whole.length() - iter) % 3 == 0) {
                out.append(',');
            }
            out.append(whole.charAt(iter));
        }
        String all = Amounts.plain(cents, c);
        int dot = all.indexOf('.');
        if (dot >= 0) {
            out.append(all.substring(dot));
        }
        return out.toString();
    }

    static String date(long millis) {
        return millis <= 0 ? "" : Days.iso(millis);
    }

    /// Date and UTC time, `2026-11-20 14:05 UTC`.
    static String dateTime(long millis) {
        if (millis <= 0) {
            return "";
        }
        long minutes = (millis % 86400000L) / 60000L;
        long h = minutes / 60;
        long m = minutes % 60;
        return Days.iso(millis) + " " + (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m
                + " UTC";
    }

    /// `text` cut with "..." to fit `width` points.
    static String fit(String text, PdfFont font, float size, float width) {
        if (text == null) {
            return "";
        }
        if (font.getStringWidth(text, size) <= width) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && font.getStringWidth(cut + "...", size) > width) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }
}
