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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.pdf.PdfCanvas;
import com.codename1.pdf.PdfDocument;
import com.codename1.pdf.PdfFont;
import com.codename1.pdf.PdfPage;
import com.codename1.pdf.PdfPageSize;
import com.codename1.qrcode.QrCode;
import com.codenameone.examples.bazaar.api.OpenLineDto;
import com.codenameone.examples.bazaar.api.UnfulfilledDto;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/// The pick list, made on the device: the same `com.codename1.pdf` the server
/// renders invoices with, here with no server involved.
///
/// The first part is what to take off the shelves, one row per SKU and
/// location with the quantities of every order added up; the second is one
/// block per order, with a QR code of the order to scan at the packing bench.
/// The QR code is drawn as squares on the PDF canvas, module by module, so it
/// stays sharp at any print size. Nothing here needs a display.
final class PickListPdf {
    private static final float MARGIN = 40f;
    private static final float ROW = 16f;
    private static final float QR_SIZE = 64f;
    private static final int INK = 0x111827;
    private static final int MUTED = 0x6b7280;
    private static final int RULE = 0xd1d5db;

    private final PdfDocument doc = new PdfDocument();
    private PdfPage page;
    private PdfCanvas canvas;
    private float y;

    private PickListPdf() {
    }

    /// The payload of an order's QR code.
    static String qrText(UnfulfilledDto order) {
        return "bazaar:order:" + order.orderId();
    }

    // tag::bazaar-picklist-pdf[]
    /// Builds the pick list for `orderIds` out of `queue` (every queued order
    /// when null).
    ///
    /// @param heading the store and the column, for the title
    /// @param printed when it was made, already formatted
    static byte[] build(String heading, String printed, List<UnfulfilledDto> queue,
            Collection<String> orderIds, java.util.Map<String, String> locationNames)
            throws IOException {
        PickListPdf p = new PickListPdf();
        p.doc.setTitle("Pick list").setCreator("Bazaar");
        List<Fulfil.PickLine> lines = Fulfil.pickList(queue, orderIds, locationNames);
        p.newPage();
        p.text(PdfFont.HELVETICA_BOLD, 20, INK, "Pick list", MARGIN);
        p.y += 22;
        p.text(PdfFont.HELVETICA, 10, MUTED, heading + "  -  " + printed, MARGIN);
        p.y += 14;
        p.text(PdfFont.HELVETICA, 10, MUTED, Fulfil.units(lines) + " units, " + lines.size()
                + " lines", MARGIN);
        p.y += 22;
        p.table(lines);
        p.orders(queue, orderIds, locationNames);
        return p.doc.toByteArray();
    }
    // end::bazaar-picklist-pdf[]

    private void newPage() {
        page = doc.addPage(PdfPageSize.A4);
        canvas = page.getCanvas();
        y = MARGIN + 10;
    }

    private void room(float needed) {
        if (y + needed > page.getHeight() - MARGIN) {
            newPage();
        }
    }

    private void text(PdfFont font, float size, int color, String text, float x) {
        canvas.setFillColor(color);
        canvas.setFont(font, size);
        canvas.drawText(text == null ? "" : text, x, y);
    }

    /// `text` cut to fit `width` points, with an ellipsis of three dots.
    static String fit(PdfFont font, float size, String text, float width) {
        if (text == null) {
            return "";
        }
        if (font.getStringWidth(text, size) <= width) {
            return text;
        }
        String cut = text;
        while (cut.length() > 0 && font.getStringWidth(cut + "...", size) > width) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    private void table(List<Fulfil.PickLine> lines) {
        float width = page.getWidth() - 2 * MARGIN;
        float[] at = {MARGIN, MARGIN + width * 0.20f, MARGIN + width * 0.38f,
            MARGIN + width * 0.78f, MARGIN + width * 0.86f};
        String[] head = {"Location", "SKU", "Item", "Qty", "Orders"};
        header(at, head);
        for (int iter = 0; iter < lines.size(); iter++) {
            if (y + ROW > page.getHeight() - MARGIN) {
                newPage();
                header(at, head);
            }
            Fulfil.PickLine l = lines.get(iter);
            StringBuilder orders = new StringBuilder();
            for (int o = 0; o < l.orders.size(); o++) {
                orders.append(o == 0 ? "#" : ", #").append(l.orders.get(o));
            }
            String[] cells = {l.location, l.sku, l.title, String.valueOf(l.quantity),
                orders.toString()};
            for (int c = 0; c < cells.length; c++) {
                float w = (c + 1 < at.length ? at[c + 1] : MARGIN + width) - at[c] - 4;
                PdfFont font = c == 3 ? PdfFont.HELVETICA_BOLD : PdfFont.HELVETICA;
                text(font, 9, INK, fit(font, 9, cells[c], w), at[c]);
            }
            canvas.setStrokeColor(RULE).setLineWidth(0.5f)
                    .drawLine(MARGIN, y + 5, MARGIN + width, y + 5);
            y += ROW;
        }
        if (lines.isEmpty()) {
            text(PdfFont.HELVETICA_OBLIQUE, 10, MUTED, "Nothing to pick", MARGIN);
            y += ROW;
        }
        y += 12;
    }

    private void header(float[] at, String[] head) {
        for (int c = 0; c < head.length; c++) {
            text(PdfFont.HELVETICA_BOLD, 9, MUTED, head[c], at[c]);
        }
        canvas.setStrokeColor(INK).setLineWidth(0.8f)
                .drawLine(MARGIN, y + 5, page.getWidth() - MARGIN, y + 5);
        y += ROW;
    }

    private void orders(List<UnfulfilledDto> queue, Collection<String> orderIds,
            java.util.Map<String, String> locationNames) {
        for (int iter = 0; queue != null && iter < queue.size(); iter++) {
            UnfulfilledDto u = queue.get(iter);
            if (orderIds != null && !orderIds.contains(u.orderId())) {
                continue;
            }
            int open = 0;
            for (int at = 0; u.lines() != null && at < u.lines().size(); at++) {
                open += u.lines().get(at).open() > 0 ? 1 : 0;
            }
            float block = Math.max(QR_SIZE, 30 + open * 12f) + 14;
            room(block);
            float top = y;
            qr(QrCode.encode(qrText(u), QrCode.ECC_MEDIUM), page.getWidth() - MARGIN - QR_SIZE,
                    top - 8, QR_SIZE);
            float textWidth = page.getWidth() - 2 * MARGIN - QR_SIZE - 12;
            y += 4;
            text(PdfFont.HELVETICA_BOLD, 12, INK, fit(PdfFont.HELVETICA_BOLD, 12, "#" + u.number()
                    + "  " + (u.customer() == null ? "" : u.customer()), textWidth), MARGIN);
            y += 14;
            if (u.shippingTitle() != null) {
                text(PdfFont.HELVETICA, 9, MUTED, fit(PdfFont.HELVETICA, 9, u.shippingTitle(),
                        textWidth), MARGIN);
                y += 12;
            }
            for (int at = 0; u.lines() != null && at < u.lines().size(); at++) {
                OpenLineDto l = u.lines().get(at);
                if (l.open() <= 0) {
                    continue;
                }
                String where = l.locationId() == null ? "" : l.locationId();
                if (locationNames != null && locationNames.get(where) != null) {
                    where = locationNames.get(where);
                }
                text(PdfFont.HELVETICA, 9, INK, fit(PdfFont.HELVETICA, 9, l.open() + " x "
                        + (l.sku() == null ? "" : l.sku() + "  ") + l.title()
                        + (where.length() == 0 ? "" : "  (" + where + ")"), textWidth), MARGIN + 8);
                y += 12;
            }
            y = Math.max(y, top + QR_SIZE) + 6;
            canvas.setStrokeColor(RULE).setLineWidth(0.5f)
                    .drawLine(MARGIN, y - 4, page.getWidth() - MARGIN, y - 4);
            y += 8;
        }
    }

    /// Draws a QR code into a square of `size` points, quiet zone included.
    private void qr(QrCode code, float x, float top, float size) {
        int n = code.getSize();
        int quiet = 2;
        float module = size / (n + 2 * quiet);
        canvas.setFillColor(0xffffff).fillRect(x, top, size, size);
        canvas.setFillColor(0x000000);
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                if (code.isDark(col, row)) {
                    // A hair wider than a module, so neighbours do not show
                    // a seam between them when the page is scaled.
                    canvas.rect(x + (col + quiet) * module, top + (row + quiet) * module,
                            module + 0.05f, module + 0.05f);
                }
            }
        }
        canvas.fill();
    }
}
