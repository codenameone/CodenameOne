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
package com.codenameone.examples.bazaar.pos;

import com.codename1.pdf.PdfCanvas;
import com.codename1.pdf.PdfDocument;
import com.codename1.pdf.PdfFont;
import com.codename1.pdf.PdfPage;
import com.codename1.pdf.PdfPageSize;
import com.codenameone.examples.bazaar.api.PosSaleDto;
import com.codenameone.examples.bazaar.api.PosTenderDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// What the customer is handed, kept from the moment the sale is paid so it
/// can be printed with the network down. The order number arrives later,
/// when the outbox has uploaded the sale.
final class Receipt {
    final String clientId;
    final String store;
    final String register;
    final long when;
    final List<String[]> lines = new ArrayList<String[]>();
    final List<String[]> totals = new ArrayList<String[]>();
    /// Whether the sale took cash, which opens the drawer.
    boolean cash;
    /// The shift goal milestone this sale crossed (25, 50, 75, 100), or 0.
    int milestone;

    private Receipt(String clientId, String store, String register, long when) {
        this.clientId = clientId;
        this.store = store;
        this.register = register;
        this.when = when;
    }

    static Receipt of(PosSaleDto sale, PosCart cart, PosPricing.Totals t, long change) {
        Receipt r = new Receipt(sale.clientId(), PosState.storeName(), PosState.registerName(),
                sale.createdAt());
        long[] units = cart.unitPrices();
        for (int iter = 0; iter < cart.items().size(); iter++) {
            PosCart.Item i = cart.items().get(iter);
            String v = i.item.variantTitle();
            String title = i.item.title() + (v == null || v.length() == 0 || "Default".equals(v)
                    ? "" : " - " + v);
            r.lines.add(new String[] {i.quantity + " x " + title,
                PosState.money(units[iter] * i.quantity)});
        }
        r.totals.add(new String[] {"Subtotal", PosState.money(t.subtotalCents)});
        if (t.discountCents > 0) {
            r.totals.add(new String[] {"Discount", PosState.money(-t.discountCents)});
        }
        if (t.taxCents > 0) {
            r.totals.add(new String[] {t.taxInclusive ? "Tax included" : "Tax",
                PosState.money(t.taxCents)});
        }
        r.totals.add(new String[] {"Total", PosState.money(t.totalCents)});
        if (sale.tipCents() > 0) {
            // The card carries the tip on top of the total; without this row
            // the tender line read larger than the total with no reason.
            r.totals.add(new String[] {"Tip", PosState.money(sale.tipCents())});
        }
        for (int iter = 0; iter < sale.tenders().size(); iter++) {
            PosTenderDto td = sale.tenders().get(iter);
            r.cash |= "CASH".equals(td.kind());
            r.totals.add(new String[] {TenderSheet.kindName(td.kind()),
                PosState.money(td.amountCents())});
        }
        if (change > 0) {
            r.totals.add(new String[] {"Change", PosState.money(change)});
        }
        return r;
    }

    /// The order number once uploaded, or 0.
    int number() {
        Outbox.Entry e = PosState.hasRegister() ? PosState.outbox().find(clientId) : null;
        return e == null ? 0 : e.number;
    }

    String orderId() {
        Outbox.Entry e = PosState.hasRegister() ? PosState.outbox().find(clientId) : null;
        return e == null ? "" : e.orderId;
    }

    /// An 80 mm till-roll PDF, tall enough for every line.
    byte[] pdf() throws IOException {
        float row = 13f;
        float height = 120f + row * (lines.size() + totals.size()) + 60f;
        PdfDocument doc = new PdfDocument();
        doc.setTitle("Receipt").setCreator("Bazaar");
        PdfPage page = doc.addPage(PdfPageSize.millimeters(80f, height * 25.4f / 72f));
        PdfCanvas c = page.getCanvas();
        float left = 12f;
        float right = page.getWidth() - 12f;
        float y = 24f;
        c.setFillColor(0x000000);
        c.setFont(PdfFont.HELVETICA_BOLD, 12);
        c.drawText(store == null ? "" : store, left, y);
        y += row + 2;
        c.setFont(PdfFont.HELVETICA, 8);
        int n = number();
        c.drawText(register + "  " + com.codenameone.examples.bazaar.ui.Ui.when(when), left, y);
        y += row;
        c.drawText(n > 0 ? "Order #" + n : "Sale " + clientId.substring(0, Math.min(8,
                clientId.length())), left, y);
        y += row * 1.5f;
        for (int iter = 0; iter < lines.size(); iter++) {
            y = pair(c, PdfFont.HELVETICA, lines.get(iter), left, right, y, row);
        }
        y += row * 0.5f;
        for (int iter = 0; iter < totals.size(); iter++) {
            boolean grand = "Total".equals(totals.get(iter)[0]);
            y = pair(c, grand ? PdfFont.HELVETICA_BOLD : PdfFont.HELVETICA, totals.get(iter), left,
                    right, y, row);
        }
        y += row;
        c.setFont(PdfFont.HELVETICA_OBLIQUE, 8);
        c.drawText("Thank you", left, y);
        return doc.toByteArray();
    }

    private static float pair(PdfCanvas c, PdfFont font, String[] p, float left, float right,
            float y, float row) {
        c.setFont(font, 9);
        String text = p[0];
        while (text.length() > 4 && font.getStringWidth(text, 9) > right - left - 60) {
            text = text.substring(0, text.length() - 2);
        }
        c.drawText(text, left, y);
        c.drawText(p[1], right - font.getStringWidth(p[1], 9), y);
        return y + row;
    }
}
