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

import com.codename1.appkit.Net;
import com.codename1.qrcode.QrCode;
import com.codename1.qrcode.QrCodeImage;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.Sheet;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.ReceiptSendDto;
import com.codenameone.examples.bazaar.docs.Pdfs;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// After the sale: change due, print, e-mail or text, and a QR code of the
/// order number for a later return. Printing works offline from the
/// locally built PDF; sending and the QR code wait for the upload, and the
/// sheet fills them in when the outbox reports it.
final class ReceiptView {
    private ReceiptView() {
    }

    static void show(final Receipt r) {
        final Sheet sheet = new Sheet(null, Lang.tr("Sale complete"));
        sheet.setName("pos-receipt");
        final Container c = sheet.getContentPane();
        c.setLayout(BoxLayout.y());
        c.setScrollableY(true);
        for (int iter = 0; iter < r.totals.size(); iter++) {
            String[] p = r.totals.get(iter);
            boolean big = "Change".equals(p[0]) || "Total".equals(p[0]);
            Container row = new Container(new BorderLayout());
            row.setUIID(big ? "PosGrand" : "PosTotalRow");
            row.add(BorderLayout.WEST, PosUi.label(p[0], big ? "PosGrandLabel" : "PosTotalRowLabel"));
            row.add(BorderLayout.EAST, PosUi.data(p[1], big ? "PosGrandValue" : "PosTotalRowValue"));
            c.add(row);
        }
        final Label order = PosUi.data("", "PosMuted");
        order.setName("pos-receipt-order");
        final Container qr = new Container(new BorderLayout());
        c.add(order).add(qr);
        Container actions = new Container(new GridLayout(1, 3));
        actions.add(PosUi.icon(FontImage.MATERIAL_PRINT, "Print", "pos-receipt-print", "PosChoice",
                e -> print(r)));
        actions.add(PosUi.icon(FontImage.MATERIAL_EMAIL, "E-mail", "pos-receipt-email", "PosChoice",
                e -> send(r, false)));
        actions.add(PosUi.icon(FontImage.MATERIAL_SMS, "Text", "pos-receipt-sms", "PosChoice",
                e -> send(r, true)));
        c.add(actions);
        c.add(PosUi.key(Lang.tr("New sale"), "pos-new-sale", "PosPay", e -> sheet.back()));
        final Runnable update = () -> {
            int n = r.number();
            if (n > 0) {
                order.setText(Lang.tr("Order #{0}", n));
                if (qr.getComponentCount() == 0) {
                    QrCode code = QrCode.encode(Wedge.ORDER_PREFIX + n, QrCode.ECC_MEDIUM);
                    qr.add(BorderLayout.CENTER, new Label(QrCodeImage.create(code, 6), "PosQr"));
                }
            } else {
                order.setText(Lang.tr("The order number comes when the sale is uploaded"));
            }
            c.revalidateLater();
        };
        update.run();
        PosUi.live(order, update, false);
        sheet.show();
    }

    private static void print(Receipt r) {
        try {
            String file = Pdfs.write("receipt-" + r.clientId + ".pdf", r.pdf());
            if (file != null) {
                Pdfs.print(file);
            }
        } catch (java.io.IOException err) {
            Ui.say("The receipt could not be made");
        }
    }

    private static void send(final Receipt r, final boolean sms) {
        final String orderId = r.orderId();
        if (orderId.length() == 0) {
            Ui.say("The receipt can be sent once the sale is uploaded");
            return;
        }
        final Sheet ask = new Sheet(null, Lang.tr(sms ? "Text the receipt" : "E-mail the receipt"));
        Container c = ask.getContentPane();
        c.setLayout(BoxLayout.y());
        final TextField to = new TextField("", Lang.tr(sms ? "Phone number" : "E-mail address"), 20,
                sms ? TextField.PHONENUMBER : TextField.EMAILADDR);
        to.setUIID("PosField");
        to.setName("pos-receipt-to");
        to.setDoneListener(e -> {
            String v = to.getText().trim();
            if (v.length() == 0) {
                return;
            }
            ask.back();
            Api.pos().sendReceipt(PosState.storeId(), orderId,
                    new ReceiptSendDto(sms ? "" : v, sms ? v : ""),
                    Net.to(done -> Ui.say("Receipt sent"), Ui::fail));
        });
        c.add(to);
        ask.show();
        to.startEditingAsync();
    }
}
