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
package com.codenameone.examples.bazaar.shop;

import com.codename1.appkit.Net;
import com.codename1.appkit.Skeleton;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CheckoutOptionsDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.PaymentMethodDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.net.Session;
import com.codenameone.examples.bazaar.pay.CardForm;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;
import java.util.UUID;

/// Checkout, in four steps on one page: where it goes (a saved address or a
/// new one), how (the shipping options the server quotes for that address),
/// how it is paid (a card from the wallet, or a new one through the existing
/// card form) and a last look before Place order.
///
/// Placing an order carries an idempotency key made once for this checkout:
/// a retry after a dropped connection sends the same key, and the server
/// answers with the order it already made rather than making a second.
final class CheckoutForm {
    private static final int ESC = 27;

    private final Form form = Ui.form(Lang.tr("Checkout"), "Checkout");
    private final Form previous;
    private final Container page = Ui.page();
    private final Container steps = new Container(new GridLayout(1, 4));
    private final Container addressBox = new Container(BoxLayout.y());
    private final Container shippingBox = new Container(BoxLayout.y());
    private final Container paymentBox = new Container(BoxLayout.y());
    private final Container reviewBox = new Container(BoxLayout.y());
    /// Made once: every attempt to place this checkout's order sends it.
    private final String key = UUID.randomUUID().toString();
    private AddressDto address;
    private String rateId;
    private String methodId;
    private boolean cashPicked;
    private CartDto quoted;
    /// What the store's hosted checkout adds (see [CheckoutHandoff]); null
    /// until the server answers.
    private CheckoutOptionsDto options;
    private int step;
    private boolean placing;
    /// Whether the default saved address was taken without asking already.
    private boolean picked;
    private TextField name;
    private TextField line1;
    private TextField line2;
    private TextField city;
    private TextField region;
    private TextField postal;
    private TextField country;
    private TextField phone;
    private TextField email;
    private TextField note;

    private CheckoutForm(Form previous) {
        this.previous = previous;
        Ui.back(form, previous);
        form.addKeyListener(ESC, e -> previous.showBack());
        steps.setUIID("SvSteps");
        page.setName("checkout");
        if (Layouts.split()) {
            Container split = new Container(new GridLayout(1, 2));
            Container left = new Container(BoxLayout.y());
            left.add(addressBox).add(shippingBox).add(paymentBox);
            split.add(left).add(reviewBox);
            page.add(steps).add(split);
        } else {
            page.add(steps).add(addressBox).add(shippingBox).add(paymentBox).add(reviewBox);
        }
        form.add(BorderLayout.CENTER, page);
    }

    static void show() {
        if (ShopState.store() == null || ShopState.cart() == null) {
            return;
        }
        CheckoutForm checkout = new CheckoutForm(CN.getCurrentForm());
        checkout.drawSteps();
        checkout.drawAddress();
        checkout.form.show();
        Api.checkout().options(ShopState.store().id(), "", Net.to(o -> {
            checkout.options = o;
            if (checkout.step >= 3) {
                checkout.drawReview();
            }
        }, (status, message) -> { }));
    }

    // ----------------------------------------------------------------- steps

    private void drawSteps() {
        steps.removeAll();
        String[] names = {"Address", "Shipping", "Payment", "Review"};
        for (int iter = 0; iter < names.length; iter++) {
            boolean done = iter <= step;
            Container s = new Container(BoxLayout.y());
            s.setUIID(done ? "SvStepDone" : "SvStep");
            s.add(ShopUi.stepBar(done));
            s.add(Ui.label(names[iter], done ? "SvStepText" : "SvStepTextTodo"));
            steps.add(s);
        }
    }

    private void go(int to) {
        step = to;
        drawSteps();
        Ui.refresh(form);
    }

    // --------------------------------------------------------------- address

    private void drawAddress() {
        addressBox.removeAll();
        Ui.section(addressBox, "Shipping address");
        List<AddressDto> saved = ShopState.addresses();
        if (address == null && !picked && !saved.isEmpty()) {
            // A repeat checkout: the default address (first) and the default
            // payment method are taken as they are, so the order is one tap
            // on Place order away. Either can still be changed.
            picked = true;
            address = saved.get(0);
            quote();
        }
        if (!saved.isEmpty()) {
            for (final AddressDto a : saved) {
                Container row = option(FontImage.MATERIAL_LOCATION_ON, Addresses.summary(a), null,
                        address != null && Addresses.same(a, address));
                addressBox.add(Ui.tap(row, "address-saved", e -> {
                    address = a;
                    drawAddress();
                    quote();
                }));
            }
            addressBox.add(Ui.label("Or a new address", "SvLabel"));
        }
        AddressDto a = address;
        name = field("Full name", "ship-name", a == null ? displayName() : a.name());
        line1 = field("Address", "ship-line1", a == null ? "" : a.line1());
        line2 = field("Apartment, suite (optional)", "ship-line2", a == null ? "" : a.line2());
        city = field("City", "ship-city", a == null ? "" : a.city());
        Container pair = new Container(new GridLayout(1, 2));
        Container left = new Container(BoxLayout.y());
        Container right = new Container(BoxLayout.y());
        left.setUIID("SvPairLeft");
        right.setUIID("SvPairRight");
        region = Ui.field(left, "Region", "ship-region", "");
        postal = Ui.field(right, "Postal code", "ship-postal", "");
        region.setText(a == null ? "" : a.region());
        postal.setText(a == null ? "" : a.postal());
        pair.add(left).add(right);
        addressBox.add(pair);
        country = field("Country", "ship-country", a == null ? "US" : a.country());
        phone = field("Phone", "ship-phone", a == null ? phoneOf() : a.phone());
        phone.setConstraint(TextField.PHONENUMBER);
        email = field("Email for the receipt", "ship-email", emailOf());
        email.setConstraint(TextField.EMAILADDR);
        // Enter in the last field continues.
        email.setDoneListener(e -> useTypedAddress());
        postal.setDoneListener(e -> useTypedAddress());
        addressBox.add(Ui.primary("Continue to shipping", "address-continue",
                e -> useTypedAddress()));
    }

    private TextField field(String caption, String name, String value) {
        TextField f = Ui.field(addressBox, caption, name, "");
        f.setText(value == null ? "" : value);
        return f;
    }

    private static String displayName() {
        return Session.user() == null ? "" : Session.user().displayName();
    }

    private static String phoneOf() {
        return Session.user() == null || Session.user().phone() == null ? ""
                : Session.user().phone();
    }

    private static String emailOf() {
        String user = Session.user() == null ? "" : Session.user().username();
        return user != null && user.indexOf('@') > 0 ? user : "";
    }

    private void useTypedAddress() {
        if (text(name).length() == 0 || text(line1).length() == 0 || text(city).length() == 0
                || text(country).length() == 0) {
            Ui.say("Fill in the name, address, city and country");
            return;
        }
        address = new AddressDto(text(name), text(line1), text(line2), text(city), text(region),
                text(postal), text(country), text(phone), 0, 0);
        quote();
    }

    private static String text(TextField f) {
        return f == null ? "" : Addresses.clean(f.getText());
    }

    // -------------------------------------------------------------- shipping

    /// Asks the server what shipping to this address costs, and the totals
    /// with the chosen option.
    private void quote() {
        shippingBox.removeAll();
        Ui.section(shippingBox, "Shipping");
        shippingBox.add(new Skeleton(3, "SvSkeleton"));
        go(Math.max(step, 1));
        Api.cart().estimate(ShopState.store().id(), ShopState.token(),
                new EstimateDto(address, rateId), Net.to(cart -> {
                    quoted = cart;
                    ShopState.setCart(cart);
                    drawShipping();
                }, (status, message) -> {
                    shippingBox.removeAll();
                    Ui.section(shippingBox, "Shipping");
                    shippingBox.add(Ui.callout(FontImage.MATERIAL_LOCAL_SHIPPING,
                            "No shipping here", message));
                    Ui.refresh(form);
                }));
    }

    private void drawShipping() {
        shippingBox.removeAll();
        Ui.section(shippingBox, "Shipping");
        List<ShippingOptionDto> options = quoted.shippingOptions();
        if (options == null || options.isEmpty()) {
            shippingBox.add(Ui.callout(FontImage.MATERIAL_LOCAL_SHIPPING, "No shipping here",
                    Lang.tr("This store does not ship to that address yet.")));
            Ui.refresh(form);
            return;
        }
        if (rateId == null) {
            rateId = quoted.shippingRateId() != null ? quoted.shippingRateId()
                    : options.get(0).rateId();
            if (quoted.shippingRateId() == null) {
                // Priced without a rate: ask again with the first one.
                quote();
                return;
            }
        }
        for (final ShippingOptionDto o : options) {
            String eta = o.etaDaysMax() <= 0 ? null : o.etaDaysMin() == o.etaDaysMax()
                    ? Lang.tr("{0} days", String.valueOf(o.etaDaysMax()))
                    : Lang.tr("{0}-{1} days", String.valueOf(o.etaDaysMin()),
                            String.valueOf(o.etaDaysMax()));
            Label price = Ui.plain(o.priceCents() == 0 ? Lang.tr("Free")
                    : ShopState.money(o.priceCents()), "SvRowValue");
            char icon = "PICKUP".equals(o.kind()) ? FontImage.MATERIAL_STOREFRONT
                    : FontImage.MATERIAL_LOCAL_SHIPPING;
            Container row = option(icon, o.name(), eta, o.rateId().equals(rateId), price);
            shippingBox.add(Ui.tap(row, "rate-" + o.rateId(), e -> {
                rateId = o.rateId();
                ShopUi.tap();
                quote();
            }));
        }
        loadPayment();
    }

    // --------------------------------------------------------------- payment

    private void loadPayment() {
        go(Math.max(step, 2));
        paymentBox.removeAll();
        Ui.section(paymentBox, "Payment");
        paymentBox.add(new Skeleton(2, "SvSkeleton"));
        Api.payments().methods(Net.to(this::drawPayment, (status, message) -> {
            paymentBox.removeAll();
            Ui.section(paymentBox, "Payment");
            paymentBox.add(Ui.text(message, "SvError"));
            Ui.refresh(form);
        }));
    }

    private void drawPayment(List<PaymentMethodDto> methods) {
        paymentBox.removeAll();
        Ui.section(paymentBox, "Payment");
        if (methodId == null) {
            for (PaymentMethodDto m : methods) {
                if (m.isDefault() || methodId == null) {
                    methodId = m.id();
                }
            }
        }
        for (final PaymentMethodDto m : methods) {
            boolean cash = "cash".equals(m.kind());
            String label = cash ? Lang.tr("Cash") : m.label() != null ? m.label() : m.brand();
            String expiry = m.expMonth() > 0 ? Lang.tr("Expires {0}/{1}",
                    String.valueOf(m.expMonth()), String.valueOf(m.expYear() % 100)) : null;
            Container row = option(cash ? FontImage.MATERIAL_PAYMENTS : FontImage.MATERIAL_CREDIT_CARD,
                    label, expiry,
                    m.id().equals(methodId));
            if (m.id().equals(methodId)) {
                cashPicked = cash;
            }
            paymentBox.add(Ui.tap(row, "method-" + m.id(), e -> {
                methodId = m.id();
                ShopUi.tap();
                drawPayment(methods);
            }));
        }
        paymentBox.add(Ui.secondary("Add a card", "add-card", e -> CardForm.show(form, () -> {
            form.showBack();
            methodId = null;
            loadPayment();
        })));
        drawReview();
    }

    // ---------------------------------------------------------------- review

    private void drawReview() {
        reviewBox.removeAll();
        Ui.section(reviewBox, "Review");
        if (quoted != null) {
            CartPane.rows(reviewBox, quoted);
        }
        Container noteBox = new Container(BoxLayout.y());
        note = Ui.field(noteBox, "Note for the store (optional)", "order-note", "");
        reviewBox.add(noteBox);
        Button place = Ui.primary("Place order", "place-order", null);
        place.setEnabled(methodId != null && rateId != null && address != null
                && !CheckoutHandoff.required(options));
        place.addActionListener(e -> place(place));
        reviewBox.add(place);
        if (CheckoutHandoff.offersMore(options)) {
            // Wallets, pay later, tips and the store's questions are taken
            // on the store's own checkout page, by the same server flow.
            final String url = options.webCheckoutUrl();
            Button web = Ui.secondary("Pay another way", "pay-another-way", null);
            web.addActionListener(e -> CN.execute(url));
            reviewBox.add(web);
        }
        // Cash is not authorized anywhere: say what happens to it instead.
        reviewBox.add(Ui.text(cashPicked
                ? "You pay in cash when the order is handed over."
                : "Your card is authorized now and charged when the order ships.",
                "SvMutedSmall"));
        go(Math.max(step, methodId != null ? 3 : 2));
    }

    private void place(final Button button) {
        if (placing) {
            return;
        }
        placing = true;
        button.setEnabled(false);
        button.setText(Lang.tr("Placing order..."));
        final String storeId = ShopState.store().id();
        String mail = text(email).length() > 0 ? text(email) : emailOf();
        Api.checkout().place(storeId, key, new PlaceOrderDto(address, rateId, methodId, mail,
                text(note), key, 0, null), Net.to(order -> {
                    placing = false;
                    ShopState.rememberAddress(address);
                    ShopState.loadCart(null);
                    OrderPlaced.show(order, previous);
                }, (status, message) -> {
                    placing = false;
                    button.setEnabled(true);
                    button.setText(Lang.tr("Place order"));
                    Ui.fail(status, message);
                }));
    }

    // ---------------------------------------------------------------- pieces

    private static Container option(char icon, String title, String detail, boolean picked) {
        return option(icon, title, detail, picked, null);
    }

    /// A choice: `value` (a price) and the check of the picked one share the
    /// right-hand column, and every row keeps the check's room, so prices
    /// stay lined up whichever row is picked.
    private static Container option(char icon, String title, String detail, boolean picked,
            Component value) {
        Label check = new Label("", "ShopCheck");
        FontImage.setMaterialIcon(check, FontImage.MATERIAL_CHECK_CIRCLE, 3.4f);
        check.setVisible(picked);
        Container east = new Container(BoxLayout.x());
        if (value != null) {
            east.add(value);
        }
        east.add(check);
        Container row = Ui.row(icon, title, detail, east);
        row.setUIID(picked ? "SvOptionPicked" : "SvOption");
        return row;
    }
}
