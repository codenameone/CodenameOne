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
package com.codenameone.examples.bazaar.pay;

import com.codenameone.examples.bazaar.shop.PayLabels;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.appkit.Net;
import com.codenameone.examples.bazaar.api.ReceiptDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// What an order cost and how it was paid.
///
/// Every number here is the server's, in cents, and is shown as it came.
public final class ReceiptForm {
    private ReceiptForm() {
    }

    /// Fetches the shopper's receipt for order `orderId` and shows it.
    public static void open(final Form previous, String orderId) {
        Api.payments().receipt(orderId, Net.to(receipt -> show(previous, receipt, false),
                Ui::fail));
    }

    /// @param admin true for an admin, who also sees whose order it was
    public static void show(final Form previous, final ReceiptDto receipt, final boolean admin) {
        final Form form = Ui.form("Receipt", "Receipt");
        Ui.back(form, previous);
        form.add(BorderLayout.CENTER, page(receipt, admin));
        form.show();
    }

    /// What a receipt says: for a caller with its own place to show one,
    /// which the admin console is.
    ///
    /// @param admin true to name the customer, which an admin needs and a shopper does not
    public static Container page(final ReceiptDto receipt, final boolean admin) {
        Container page = Ui.page();
        final String currency = receipt.currency();

        Label total = Ui.plain(Ui.money(receipt.amount(), currency), "SvAmount");
        total.setName("total");
        page.add(total);
        Label status = Ui.badge(status(receipt.status()), badge(receipt.status()));
        status.setName("paymentStatus");
        page.add(FlowLayout.encloseCenter(status));
        page.add(FlowLayout.encloseCenter(Ui.plain(
                receipt.paidAt() > 0 ? Ui.when(receipt.paidAt()) : "", "SvMuted")));

        Ui.section(page, "Order");
        Container order = new Container(BoxLayout.y());
        order.add(Ui.row(FontImage.MATERIAL_RECEIPT_LONG, receipt.orderId(), null, null));
        if (admin && receipt.customerUsername() != null && receipt.customerUsername().length() > 0) {
            order.add(Ui.row(FontImage.MATERIAL_PERSON, receipt.customerUsername(), "Customer",
                    null));
        }
        page.add(order);

        Ui.section(page, "Paid");
        Container paid = new Container(BoxLayout.y());
        paid.setUIID("SvPanel");
        paid.add(Ui.fact("Total", Ui.money(receipt.amount(), currency), "SvFactTotal"));
        if (receipt.refunded() > 0) {
            paid.add(Ui.fact("Refunded", "-" + Ui.money(receipt.refunded(), currency)));
        }
        paid.add(Ui.fact("Paid with", "cash".equals(receipt.status()) ? Lang.tr("Cash")
                : PayLabels.translatable(receipt.methodLabel()) ? Lang.tr(receipt.methodLabel())
                : receipt.methodLabel()));
        page.add(paid);
        return page;
    }

    /// An amount typed with or without a decimal point, in cents; 0 for
    /// anything that is not an amount.
    static long cents(String typed) {
        String text = typed.trim().replace(',', '.');
        int point = text.indexOf('.');
        String whole = point < 0 ? text : text.substring(0, point);
        String part = point < 0 ? "" : text.substring(point + 1);
        if (part.length() > 2) {
            part = part.substring(0, 2);
        }
        while (part.length() < 2) {
            part += "0";
        }
        if (Cards.digits(whole).length() != whole.length()
                || Cards.digits(part).length() != part.length() || whole.length() > 6) {
            return 0;
        }
        return (whole.length() == 0 ? 0 : Long.parseLong(whole)) * 100 + Long.parseLong(part);
    }

    /// A payment's state, in words.
    public static String status(String status) {
        // The server's own payment states, as the admin's "By status" counts
        // them; every one of them used to read "Payment pending".
        if ("PAID".equals(status)) {
            return "Paid";
        }
        if ("AUTHORIZED".equals(status)) {
            return "Authorized";
        }
        if ("PARTIALLY_REFUNDED".equals(status)) {
            return "Partially refunded";
        }
        if ("REFUNDED".equals(status)) {
            return "Refunded";
        }
        if ("VOIDED".equals(status)) {
            return "Voided";
        }
        if ("REFUND_PENDING".equals(status)) {
            return "Refund pending";
        }
        if ("REFUND_FAILED".equals(status)) {
            return "Refund failed";
        }
        if ("FAILED".equals(status)) {
            return "Payment failed";
        }
        return "paid".equals(status) ? "Paid" : "cash".equals(status) ? "Paid in cash"
                : "refunded".equals(status) ? "Refunded"
                : "refund_pending".equals(status) ? "Refund pending"
                : "refund_failed".equals(status) ? "Refund failed"
                : "failed".equals(status) ? "Payment failed" : "Payment pending";
    }

    public static String badge(String status) {
        return "paid".equals(status) || "cash".equals(status) || "PAID".equals(status)
                ? "SvBadgeGood"
                : "failed".equals(status) || "refund_failed".equals(status)
                || "FAILED".equals(status) || "REFUND_FAILED".equals(status) ? "SvBadgeBad"
                : "SvBadgeWarn";
    }
}
