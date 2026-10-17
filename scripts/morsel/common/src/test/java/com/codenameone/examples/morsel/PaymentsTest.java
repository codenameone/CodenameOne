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
package com.codenameone.examples.morsel;

import java.util.Map;

/// Paying: a card that is refused, a card that is added, a delivery charged to it,
/// a tip on top, and the receipt that says all of it.
public class PaymentsTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        Actor courier = Actor.signIn(COURIER);
        courier.online(true, HERE.getLatitude() + 0.004, HERE.getLongitude() + 0.003);
        signIn(CUSTOMER, "Customer");

        click("menu-open");
        click("menu-wallet");
        waitForForm("Wallet");
        until(() -> has("method-cash"), 20000, "the wallet");
        click("addCard");
        waitForForm("Card");
        // A number that is not a card number is caught before it is sent.
        until(() -> has("saveCard"), 20000, "the card form");
        type("cardNumber", "4242 4242 4242 4241");
        type("cardExpiry", "12/34");
        type("cardCvc", "123");
        type("cardHolder", "Riley Customer");
        click("saveCard");
        until(() -> text("error").length() > 0, 20000, "a mistyped number to be caught");
        // A card the bank declines is refused by the server, in its words.
        type("cardNumber", "4000 0000 0000 0002");
        click("saveCard");
        until(() -> text("error").length() > 0 && has("saveCard")
                && findByName("saveCard").isEnabled(), 20000, "the declined card to be refused");
        type("cardNumber", "4242 4242 4242 4242");
        click("saveCard");
        waitForForm("Wallet");
        final String card = card();
        // Tapping it makes it the one that pays.
        click(card);
        Actor customer = Actor.signIn(CUSTOMER);
        until(() -> {
            Map<String, Object> preferences = customer.get("/api/account/preferences");
            String chosen = Actor.text(preferences, "defaultPaymentMethodId");
            return chosen.length() > 0 && !"cash".equals(chosen);
        }, 20000, "the card to become the account's default");
        shot("wallet");
        back();
        waitForForm("Customer");

        quote();
        until(() -> text("payWith").indexOf("4242") >= 0, 20000,
                "the delivery to be paid by the new card (it says \"" + text("payWith") + "\")");
        click("request");
        untilText("deliveryState", "Finding a courier");
        final String id = offered(courier);
        courier.post("/api/courier/offers/" + id + "/accept", null);
        courier.post("/api/courier/deliveries/" + id + "/arrived", null);
        courier.post("/api/courier/deliveries/" + id + "/start", null);
        courier.post("/api/courier/deliveries/" + id + "/complete", null);
        assertEqual(200, courier.status, "completing the delivery");
        untilText("deliveryState", "You have arrived");
        until(() -> text("paidWith").indexOf("4242") >= 0, 20000, "what the delivery was paid with");
        Map<String, Object> receipt = customer.get("/api/payments/receipts/" + id);
        assertEqual("paid", Actor.text(receipt, "status"), "the charge");
        assertEqual(0, ((Number) receipt.get("tip")).intValue(), "no tip before one is given");

        click("star-5");
        click("tip-15");
        click("done");
        until(() -> findByName("whereTo") != null, 20000, "the customer's screen to reset");
        until(() -> ((Number) customer.get("/api/payments/receipts/" + id).get("tip")).intValue()
                > 0, 20000, "the tip to be charged");

        // The delivery is first in the history, and opens as its receipt.
        click("menu-open");
        click("menu-history");
        waitForForm("Deliveries");
        click("delivery-0");
        waitForForm("Receipt");
        untilText("paymentStatus", "Paid");
        assertTrue(text("tipPaid").startsWith("$"), "the receipt shows the tip");
        assertFalse(has("sendTip"), "a delivery can be tipped once");
        shot("receipt");
        back();
        waitForForm("Deliveries");
        back();
        waitForForm("Customer");
        courier.online(false, 0, 0);
        return true;
    }

    /// The name of the row of the first card in the wallet.
    private String card() {
        final String[] found = new String[1];
        until(() -> {
            for (int iter = 0; iter < 6 && found[0] == null; iter++) {
                if (has("method-" + iter)) {
                    found[0] = "method-" + iter;
                }
            }
            return found[0] != null;
        }, 20000, "the card to be in the wallet");
        return found[0];
    }
}
