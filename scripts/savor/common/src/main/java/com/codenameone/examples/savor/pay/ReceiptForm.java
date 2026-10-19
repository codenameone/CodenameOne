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
package com.codenameone.examples.savor.pay;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.savor.Telemetry;
import com.codenameone.examples.savor.api.ReasonDto;
import com.codenameone.examples.savor.api.ReceiptDto;
import com.codenameone.examples.savor.api.TipDto;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Ui;

/// What a delivery cost, how it was paid, and -- for the customer -- the chance to
/// leave a tip.
///
/// Every number here is the server's, in cents, and is shown as it came. The
/// one sum the app does is the tip a percentage stands for, which it then sends
/// as an amount: what "15%" means is decided where the customer can see it.
public final class ReceiptForm {
    private static final int[] PERCENTS = {10, 15, 20};

    private ReceiptForm() {
    }

    /// Fetches the customer's receipt for order `orderId` and shows it.
    public static void open(final Form previous, String orderId) {
        Api.payments().receipt(orderId, Net.to(receipt -> show(previous, receipt, false),
                Ui::fail));
    }

    /// @param admin true for an admin, who may return the money and may not tip
    public static void show(final Form previous, final ReceiptDto receipt, final boolean admin) {
        final Form form = Ui.form("Receipt", "Receipt");
        Ui.back(form, previous);
        Container page = page(receipt, admin);
        actions(form, previous, page, receipt, admin);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    /// What a receipt says, without what can be done about it: for a caller
    /// with its own place to show one, which the admin console is.
    ///
    /// @param admin true to name the customer, which an admin needs and a customer does not
    public static Container page(final ReceiptDto receipt, final boolean admin) {
        Container page = Ui.page();
        final String currency = receipt.currency();

        Label total = Ui.plain(Ui.money(receipt.total(), currency), "SvAmount");
        total.setName("total");
        page.add(total);
        Label status = Ui.badge(status(receipt.status()), badge(receipt.status()));
        status.setName("paymentStatus");
        page.add(FlowLayout.encloseCenter(status));
        page.add(FlowLayout.encloseCenter(Ui.plain(
                receipt.paidAt() > 0 ? Ui.when(receipt.paidAt()) : "", "SvMuted")));

        Ui.section(page, "Order");
        Container order = new Container(BoxLayout.y());
        order.add(Ui.row(FontImage.MATERIAL_STOREFRONT, receipt.restaurantName(), null, null));
        order.add(Ui.row(FontImage.MATERIAL_PLACE, receipt.dropoffName(), null, null));
        if (receipt.courierName() != null && receipt.courierName().length() > 0) {
            order.add(Ui.row(FontImage.MATERIAL_TWO_WHEELER, receipt.courierName(),
                    receipt.vehicle(), null));
        }
        if (admin && receipt.customerUsername() != null && receipt.customerUsername().length() > 0) {
            order.add(Ui.row(FontImage.MATERIAL_PERSON, receipt.customerUsername(), "Customer",
                    null));
        }
        page.add(order);

        Ui.section(page, "Paid");
        Container paid = new Container(BoxLayout.y());
        paid.setUIID("SvPanel");
        paid.add(Ui.fact("Food", Ui.money(receipt.subtotal(), currency)));
        paid.add(Ui.fact("Delivery", Ui.money(receipt.deliveryFee(), currency)));
        paid.add(Ui.fact("Service fee", Ui.money(receipt.serviceFee(), currency)));
        if (receipt.smallOrderFee() > 0) {
            paid.add(Ui.fact("Small order fee", Ui.money(receipt.smallOrderFee(), currency)));
        }
        paid.add(Ui.fact("Tax", Ui.money(receipt.tax(), currency)));
        if (receipt.discount() > 0) {
            paid.add(Ui.fact("Discount", "-" + Ui.money(receipt.discount(), currency)));
        }
        if (receipt.tip() > 0) {
            Container tipped = Ui.fact("Tip", Ui.money(receipt.tip(), currency));
            tipped.getComponentAt(1).setName("tipPaid");
            paid.add(tipped);
        }
        paid.add(Ui.fact("Total", Ui.money(receipt.total(), currency), "SvFactTotal"));
        paid.add(Ui.fact("Paid with", "cash".equals(receipt.status()) ? Lang.tr("Cash")
                : receipt.methodLabel()));
        page.add(paid);
        return page;
    }

    private static void actions(final Form form, final Form previous, Container page,
            final ReceiptDto receipt, final boolean admin) {
        final String currency = receipt.currency();
        boolean settled = "paid".equals(receipt.status()) || "cash".equals(receipt.status());
        if (!admin && receipt.tipUnanswered() > 0) {
            unanswered(previous, page, receipt);
        } else if (!admin && settled && receipt.tip() == 0) {
            tip(form, previous, page, receipt);
        }
        if (admin && "paid".equals(receipt.status())) {
            page.add(Ui.danger("Refund this order", "refund", e -> Ui.reason(form,
                    "Refund", Lang.tr("{0} goes back to the card it came from.",
                            Ui.money(receipt.total(), currency)), "Refund",
                    why -> {
                        ReasonDto reason = new ReasonDto(why);
                        Api.admin().refundOrder(receipt.orderId(), reason, Net.to(refunded -> {
                            Telemetry.deliveryRefunded("receipt");
                            show(previous, refunded, true);
                        }, Ui::fail));
                    })));
        }
    }

    /// A tip whose charge never answered: sent again for the same amount,
    /// which the server charges at most once.
    private static void unanswered(final Form previous, Container page, final ReceiptDto receipt) {
        final long cents = receipt.tipUnanswered();
        page.add(Ui.text(Lang.tr("Your tip of {0} has not gone through yet.",
                Ui.money(cents, receipt.currency())), "SvMuted"));
        page.add(Ui.primary("Try the tip again", "retryTip", e ->
                Api.payments().tip(receipt.orderId(), new TipDto(cents), Net.to(tipped -> {
                    Ui.say("Thank you. Your tip was sent.");
                    show(previous, tipped, false);
                }, Ui::fail))));
    }

    /// The tip: three shares of the food to tap, or an amount to type.
    private static void tip(final Form form, final Form previous, Container page,
            final ReceiptDto receipt) {
        Ui.section(page, "Add a tip");
        page.add(Ui.text(receipt.courierName() == null || receipt.courierName().length() == 0
                ? Lang.tr("All of it goes to your courier.")
                : Lang.tr("All of it goes to {0}.", receipt.courierName()), "SvMuted"));
        final long fare = receipt.subtotal();
        String[] labels = new String[PERCENTS.length];
        String[] values = new String[PERCENTS.length];
        for (int iter = 0; iter < PERCENTS.length; iter++) {
            long cents = Math.max(100L, Math.round(fare * PERCENTS[iter] / 100d));
            values[iter] = String.valueOf(cents);
            labels[iter] = PERCENTS[iter] + "%  " + Ui.money(cents, receipt.currency());
        }
        final TextField custom = new TextField("", Lang.tr("Another amount"), 8, TextArea.DECIMAL);
        custom.setUIID("SvField");
        custom.getHintLabel().setUIID("SvFieldHint");
        custom.setName("tipCustom");
        final long[] chosen = {0};
        Container presets = Ui.choice("tip", labels, values, "", value -> {
            chosen[0] = Long.parseLong(value);
            custom.setText("");
        });
        final String[] amounts = values;
        // The amounts are numbers; nothing in them is a word to look up.
        for (int iter = 0; iter < presets.getComponentCount(); iter++) {
            ((Button) presets.getComponentAt(iter)).setShouldLocalize(false);
        }
        page.add(presets);
        page.add(custom);
        page.add(Ui.primary("Send tip", "sendTip", e -> {
            long cents = custom.getText().trim().length() > 0 ? cents(custom.getText())
                    : chosen[0];
            if (cents <= 0) {
                Ui.say("Choose an amount for the tip");
                return;
            }
            TipDto tip = new TipDto(cents);
            // Which of the shares it was, when it was one of them.
            int share = 0;
            for (int iter = 0; iter < amounts.length; iter++) {
                if (custom.getText().trim().length() == 0
                        && amounts[iter].equals(String.valueOf(cents))) {
                    share = PERCENTS[iter];
                }
            }
            final int reported = share;
            Api.payments().tip(receipt.orderId(), tip, Net.to(tipped -> {
                Telemetry.tipped("receipt", reported);
                Ui.say("Thank you. Your tip was sent.");
                show(previous, tipped, false);
            }, Ui::fail));
        }));
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
        return "paid".equals(status) ? "Paid" : "cash".equals(status) ? "Paid in cash"
                : "refunded".equals(status) ? "Refunded"
                : "refund_pending".equals(status) ? "Refund pending"
                : "refund_failed".equals(status) ? "Refund failed"
                : "failed".equals(status) ? "Payment failed" : "Payment pending";
    }

    public static String badge(String status) {
        return "paid".equals(status) || "cash".equals(status) ? "SvBadgeGood"
                : "failed".equals(status) || "refund_failed".equals(status) ? "SvBadgeBad"
                : "SvBadgeWarn";
    }
}
