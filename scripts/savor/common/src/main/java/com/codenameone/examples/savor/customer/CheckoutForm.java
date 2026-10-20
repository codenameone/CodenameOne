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
package com.codenameone.examples.savor.customer;

import com.codename1.maps.LatLng;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.savor.Prefs;
import com.codenameone.examples.savor.Telemetry;
import com.codenameone.examples.savor.api.CartDto;
import com.codenameone.examples.savor.api.PaymentMethodDto;
import com.codenameone.examples.savor.api.QuoteDto;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.pay.WalletForm;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Nav;
import com.codenameone.examples.savor.ui.Skeleton;
import com.codenameone.examples.savor.ui.Ui;

/// The cart, and everything the order still needs before the kitchen sees
/// it: where it goes, how it is paid, a promo, a tip, and what it all comes to
/// as the server prices it.
public final class CheckoutForm {
    private static final String[] TIPS = {"0", "10", "15", "20"};
    private static final String[] TIP_NAMES = {"None", "10%", "15%", "20%"};

    private final Form form;
    private final Form previous;
    private final Container lines = new Container(BoxLayout.y());
    private final Container totals = new Container(BoxLayout.y());
    private final TextField note = new TextField("", Lang.tr("Notes for the courier"));
    private final TextField promo = new TextField("", Lang.tr("Promo code"));
    private final Button method = new Button("", "SvRowValue");
    private final Button place;
    private String methodId;
    private String promoCode;
    private int tipPercent = Prefs.tipPercent();
    private boolean placing;
    /// Whether the totals shown are the price of the cart as it is now.
    private boolean quoted;
    /// The food's price as the server last quoted it, or -1 before any quote.
    /// The cart keeps the prices the menu had when it was read, and a merchant
    /// may have changed them since.
    private long pricedSubtotal = -1;
    /// The number of the latest quote asked for.
    private int quotes;

    private CheckoutForm(Form previous) {
        this.previous = previous;
        form = Ui.form(Lang.tr("Your order"), "checkout");
        Ui.back(form, previous);
        Container page = Ui.page();
        page.add(Ui.plain(Cart.restaurant() == null ? "" : Cart.restaurant().name(),
                "SvRestaurantTitle"));
        page.add(lines);

        Ui.section(page, Lang.tr("Deliver to"));
        String to = CustomerForm.destinationName();
        page.add(Ui.row(FontImage.MATERIAL_PLACE, to == null ? Lang.tr("Current location") : to,
                null, null));
        note.setName("note");
        note.setUIID("SvField");
        note.getHintLabel().setUIID("SvFieldHint");
        page.add(note);

        Ui.section(page, Lang.tr("Payment"));
        method.setName("payWith");
        method.addActionListener(e -> WalletForm.pick(form, methodId, picked -> {
            pick(picked);
            form.showBack();
        }));
        page.add(Ui.row(FontImage.MATERIAL_CREDIT_CARD, Lang.tr("Pay with"), null, method));

        Ui.section(page, Lang.tr("Promo"));
        Container promoRow = new Container(new BorderLayout());
        promo.setName("promo");
        promo.setUIID("SvField");
        promo.getHintLabel().setUIID("SvFieldHint");
        promoRow.add(BorderLayout.CENTER, promo);
        promoRow.add(BorderLayout.EAST, Ui.link("Apply", "apply-promo", e -> {
            promoCode = promo.getText().trim().length() == 0 ? null : promo.getText().trim();
            quote();
        }));
        page.add(promoRow);

        Ui.section(page, Lang.tr("Tip your courier"));
        page.add(Ui.choice("tip", trAll(TIP_NAMES), TIPS, String.valueOf(tipPercent), picked -> {
            tipPercent = Integer.parseInt(picked);
            quote();
        }));
        page.add(Ui.plain(Lang.tr("All of the tip goes to the courier."), "SvMutedSmall"));

        Ui.section(page, Lang.tr("Summary"));
        page.add(totals);
        form.add(BorderLayout.CENTER, page);

        place = Ui.primary("Place order", "place", e -> place());
        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvAddBar");
        bar.setSafeArea(true);
        bar.add(BorderLayout.CENTER, place);
        form.add(BorderLayout.SOUTH, bar);
        fillLines();
        loadMethods();
        quote();
    }

    /// Opens the cart over `previous`.
    public static void show(Form previous) {
        if (Cart.count() == 0) {
            return;
        }
        new CheckoutForm(previous).form.show();
    }

    private static String[] trAll(String[] texts) {
        String[] done = new String[texts.length];
        for (int iter = 0; iter < texts.length; iter++) {
            done[iter] = Lang.tr(texts[iter]);
        }
        return done;
    }

    private void fillLines() {
        lines.removeAll();
        for (final Cart.Line line : Cart.lines()) {
            Container row = new Container(new BorderLayout());
            row.setUIID("SvCartLine");
            Container words = new Container(BoxLayout.y());
            words.add(Ui.plain(line.dish.name(), "SvDishName"));
            if (line.options.size() > 0) {
                words.add(Ui.plain(line.optionNames(), "SvMutedSmall"));
            }
            words.add(Ui.plain(Ui.money(line.unitCents() * line.quantity, null), "SvDishPrice"));
            row.add(BorderLayout.CENTER, words);
            Container stepper = new Container(BoxLayout.x());
            stepper.setUIID("SvStepper");
            stepper.add(Ui.flat(line.quantity == 1 ? FontImage.MATERIAL_DELETE_OUTLINE
                    : FontImage.MATERIAL_REMOVE, "less-" + line.dish.id(), e -> change(line, -1)));
            stepper.add(new Label(String.valueOf(line.quantity), "SvQuantity"));
            stepper.add(Ui.flat(FontImage.MATERIAL_ADD, "more-" + line.dish.id(),
                    e -> change(line, 1)));
            row.add(BorderLayout.EAST, stepper);
            lines.add(row);
        }
        lines.add(Ui.link("Add items", "add-items", e -> previous.showBack()));
        form.revalidateLater();
    }

    private void change(Cart.Line line, int by) {
        Cart.set(line, line.quantity + by);
        if (Cart.count() == 0) {
            previous.showBack();
            return;
        }
        fillLines();
        quote();
    }

    private void loadMethods() {
        Api.payments().methods(Net.to(found -> {
            for (PaymentMethodDto each : found) {
                if (methodId == null && each.isDefault()) {
                    pick(each);
                }
            }
            if (methodId == null && !found.isEmpty()) {
                pick(found.get(0));
            }
        }, Ui::fail));
    }

    private void pick(PaymentMethodDto picked) {
        methodId = picked.id();
        method.setText(WalletForm.label(picked));
        form.revalidateLater();
    }

    private long tipCents() {
        long subtotal = pricedSubtotal >= 0 ? pricedSubtotal : Cart.subtotalCents();
        return subtotal * tipPercent / 100;
    }

    private CartDto cart() {
        LatLng to = CustomerForm.destination();
        String address = CustomerForm.destinationName();
        return Cart.dto(to.getLatitude(), to.getLongitude(),
                address == null ? Lang.tr("Current location") : address,
                note.getText().trim(), promoCode, tipCents(), methodId);
    }

    /// Prices the cart as it is now. Until that price is back the order cannot
    /// be placed: the button would still show the old total, and the server
    /// would charge for the changed cart. Only the latest request's answer is
    /// used; one overtaken by a later change is dropped.
    private void quote() {
        final int asked = ++quotes;
        quoted = false;
        place.setEnabled(false);
        totals.removeAll();
        if (!CustomerForm.located()) {
            // Priced, and then placed, against the fallback city otherwise.
            totals.add(Ui.plain(Lang.tr("Choose where the food goes"), "SvMuted"));
            form.revalidateLater();
            return;
        }
        totals.add(new Skeleton(3));
        form.revalidateLater();
        Api.orders().quote(cart(), Net.to(quote -> {
            if (asked != quotes) {
                return;
            }
            long before = pricedSubtotal;
            pricedSubtotal = quote.subtotalCents();
            if (before != pricedSubtotal && quote.tipCents() != tipCents()) {
                // The tip was worked out from prices the server no longer
                // charges: asked again with the percentage of the real food
                // price, which converges, as the subtotal does not move with it.
                quote();
                return;
            }
            totals(quote);
            quoted = true;
            place.setEnabled(!placing);
        }, (status, message) -> {
            if (asked != quotes) {
                return;
            }
            totals.removeAll();
            totals.add(Ui.text(message, "SvError"));
            form.revalidateLater();
        }));
    }

    private void totals(QuoteDto quote) {
        String currency = quote.currency();
        totals.removeAll();
        totals.add(Ui.fact(Lang.tr("Food"), Ui.money(quote.subtotalCents(), currency)));
        totals.add(Ui.fact(Lang.tr("Delivery"), quote.deliveryFeeCents() == 0 ? Lang.tr("Free")
                : Ui.money(quote.deliveryFeeCents(), currency)));
        totals.add(Ui.fact(Lang.tr("Service"), Ui.money(quote.serviceFeeCents(), currency)));
        if (quote.smallOrderCents() > 0) {
            totals.add(Ui.fact(Lang.tr("Small order"), Ui.money(quote.smallOrderCents(), currency)));
        }
        totals.add(Ui.fact(Lang.tr("Tax"), Ui.money(quote.taxCents(), currency)));
        if (quote.discountCents() > 0) {
            totals.add(Ui.fact(Lang.tr("Discount"), "-" + Ui.money(quote.discountCents(), currency),
                    "SvDiscount"));
        }
        if (quote.tipCents() > 0) {
            totals.add(Ui.fact(Lang.tr("Tip"), Ui.money(quote.tipCents(), currency)));
        }
        totals.add(Ui.fact(Lang.tr("Total"), Ui.money(quote.totalCents(), currency), "SvTotal"));
        if (quote.promoMessage() != null && quote.promoMessage().length() > 0) {
            totals.add(Ui.plain(quote.promoMessage(), "SvMutedSmall"));
        }
        totals.add(Ui.plain(Lang.tr("Arrives in about {0} min", String.valueOf(quote.etaMinutes())),
                "SvMutedSmall"));
        Telemetry.orderQuoted(Cart.lines().size());
        place.setText(Lang.tr("Place order") + Ui.DOT + Ui.money(quote.totalCents(), currency));
        form.revalidateLater();
    }

    private void place() {
        if (placing || !quoted) {
            return;
        }
        if (!CustomerForm.located()) {
            Ui.say("Choose where the food goes");
            return;
        }
        if (methodId == null) {
            Ui.say("Choose how to pay");
            return;
        }
        placing = true;
        place.setEnabled(false);
        Prefs.setTipPercent(tipPercent);
        Api.orders().place(cart(), Net.to(order -> {
            placing = false;
            Telemetry.orderPlaced("cash".equals(methodId), promoCode != null);
            Cart.clear();
            // The checkout is done with: back from the order goes home.
            Form home = Nav.build();
            if (home != null) {
                TrackForm.show(home, order.id());
            }
        }, (status, message) -> {
            placing = false;
            place.setEnabled(quoted);
            Ui.fail(status, message);
        }));
    }
}
