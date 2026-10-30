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
package com.codenameone.examples.pulse.pay;

import com.codename1.components.SpanLabel;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.pulse.Telemetry;
import com.codenameone.examples.pulse.api.CardDto;
import com.codenameone.examples.pulse.api.PaymentSetupDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;

/// Adds a card.
///
/// How depends on who takes the payments, and the server says which when the
/// setup starts. A real provider has a page of its own for typing a card into:
/// the app opens it in the browser, never sees the number, and asks the server
/// what was saved when the user comes back. The simulated provider a
/// development server uses has no such page, so the card is typed here -- which
/// is also what lets the whole flow be tried, and tested, with no account
/// anywhere.
public final class CardForm {
    /// The setup the browser was opened for, to finish when the app is back.
    private static CardForm waiting;
    /// What a hosted setup is finished with: the card went to the processor's
    /// own page, and nothing of it is here to send.
    private static final CardDto NO_CARD = new CardDto(null, 0, 0, null, null);

    private final Form form = Ui.form("Add a card", "Card");
    private final Form previous;
    private final Runnable added;
    private final Container page = Ui.page();
    private final SpanLabel error = Ui.text("", "SvError");
    private String setupId;

    private CardForm(Form previous, Runnable added) {
        this.previous = previous;
        this.added = added;
        Ui.back(form, previous);
        error.setName("error");
        form.add(BorderLayout.CENTER, page);
    }

    /// @param added run once the card is in the wallet; it is what shows the
    ///     next screen
    public static void show(Form previous, Runnable added) {
        final CardForm card = new CardForm(previous, added);
        card.form.show();
        Api.payments().startSetup(Net.to(card::begin, (status, message) -> {
            Ui.fail(status, message);
            previous.showBack();
        }));
    }

    /// The app is in front again. If it left to add a card, see what came of it.
    public static void resumed() {
        if (waiting != null) {
            waiting.finish(NO_CARD);
        }
    }

    private void begin(PaymentSetupDto setup) {
        setupId = setup.id();
        if (setup.hosted()) {
            hosted(setup.url());
        } else {
            typed();
        }
        Ui.refresh(form);
    }

    private void hosted(final String url) {
        page.add(Ui.banner(FontImage.MATERIAL_LOCK, "Your card goes straight to the bank",
                "The payment provider's own page opens in your browser. Pulse never sees "
                + "the number. Come back here when you have finished.", "SvBannerIcon"));
        page.add(error);
        page.add(Ui.primary("Open the secure page", "openHosted", e -> {
            waiting = this;
            CN.execute(url);
        }));
        page.add(Ui.secondary("I have finished", "finishHosted", e -> finish(NO_CARD)));
    }

    private void typed() {
        final Label number = Ui.plain(mask(""), "SvCardNumber");
        final Label holder = Ui.plain("", "SvCardSmall");
        final Label expires = Ui.plain("MM/YY", "SvCardSmall");
        final Label brand = Ui.plain("", "SvCardBrand");
        Container face = new Container(new BorderLayout());
        face.setUIID("SvCardFace");
        face.add(BorderLayout.NORTH, brand);
        face.add(BorderLayout.CENTER, number);
        Container foot = new Container(new BorderLayout());
        foot.add(BorderLayout.CENTER, holder);
        foot.add(BorderLayout.EAST, expires);
        face.add(BorderLayout.SOUTH, foot);
        page.add(face);

        final TextField digits = Ui.field(page, "Card number", "cardNumber",
                "4242 4242 4242 4242", TextArea.NUMERIC);
        Container pair = new Container(new GridLayout(1, 2));
        Container left = new Container(BoxLayout.y());
        Container right = new Container(BoxLayout.y());
        left.setUIID("SvPairLeft");
        right.setUIID("SvPairRight");
        final TextField expiry = Ui.field(left, "Expires", "cardExpiry", "MM/YY",
                TextArea.NUMERIC);
        final TextField cvc = Ui.field(right, "Security code", "cardCvc", "123",
                TextArea.NUMERIC);
        pair.add(left).add(right);
        page.add(pair);
        final TextField name = Ui.field(page, "Name on the card", "cardHolder", "");
        // Each field is written back in the shape a card has it in. Writing it
        // back is itself a change, so the listener stops when there is nothing
        // left to correct.
        digits.addDataChangedListener((type, index) -> {
            String shaped = Cards.group(digits.getText());
            if (!shaped.equals(digits.getText())) {
                digits.setText(shaped);
            }
            number.setText(mask(Cards.digits(shaped)));
            brand.setText(Cards.brand(Cards.digits(shaped)));
            face.revalidate();
        });
        expiry.addDataChangedListener((type, index) -> {
            String shaped = Cards.expiry(expiry.getText());
            if (!shaped.equals(expiry.getText())) {
                expiry.setText(shaped);
            }
            expires.setText(shaped.length() == 0 ? "MM/YY" : shaped);
            face.revalidate();
        });
        name.addDataChangedListener((type, index) -> {
            holder.setText(name.getText());
            face.revalidate();
        });
        page.add(Ui.text("This server takes no real payments. Use the test card "
                + "4242 4242 4242 4242 with any date ahead and any three digits.", "SvNotice"));
        page.add(error);
        page.add(Ui.primary("Save card", "saveCard", e -> {
            String wrong = Cards.check(digits.getText(), expiry.getText(), cvc.getText());
            if (wrong != null) {
                error.setText(Lang.tr(wrong));
                Ui.refresh(form);
                return;
            }
            CardDto card = new CardDto(Cards.digits(digits.getText()),
                    Cards.month(expiry.getText()), Cards.year(expiry.getText()),
                    cvc.getText().trim(), name.getText().trim());
            finish(card);
        }));
    }

    private void finish(CardDto card) {
        waiting = null;
        error.setText("");
        Api.payments().completeSetup(setupId, card, Net.to(method -> {
            Telemetry.cardAdded();
            added.run();
        },
                (status, message) -> {
                    error.setText(message);
                    Ui.refresh(form);
                }));
    }

    /// The number as the card shows it: what is typed, and dots for the rest.
    private static String mask(String digits) {
        StringBuilder shown = new StringBuilder();
        for (int iter = 0; iter < 16; iter++) {
            if (iter > 0 && iter % 4 == 0) {
                shown.append("  ");
            }
            shown.append(iter < digits.length() ? digits.charAt(iter) : '*');
        }
        return shown.toString();
    }
}
