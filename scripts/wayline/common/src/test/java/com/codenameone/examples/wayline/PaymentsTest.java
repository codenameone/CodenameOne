/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline;

import java.util.Map;

/// Paying: a card that is refused, a card that is added, a ride charged to it,
/// a tip on top, and the receipt that says all of it.
public class PaymentsTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        Actor driver = Actor.signIn(DRIVER);
        driver.online(true, HERE.getLatitude() + 0.004, HERE.getLongitude() + 0.003);
        signIn(RIDER, "Rider");

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
        type("cardHolder", "Riley Rider");
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
        Actor rider = Actor.signIn(RIDER);
        until(() -> {
            Map<String, Object> preferences = rider.get("/api/account/preferences");
            String chosen = Actor.text(preferences, "defaultPaymentMethodId");
            return chosen.length() > 0 && !"cash".equals(chosen);
        }, 20000, "the card to become the account's default");
        shot("wallet");
        back();
        waitForForm("Rider");

        quote();
        until(() -> text("payWith").indexOf("4242") >= 0, 20000,
                "the ride to be paid by the new card (it says \"" + text("payWith") + "\")");
        click("request");
        untilText("rideState", "Finding a driver");
        final String id = offered(driver);
        driver.post("/api/driver/offers/" + id + "/accept", null);
        driver.post("/api/driver/rides/" + id + "/arrived", null);
        driver.post("/api/driver/rides/" + id + "/start", null);
        driver.post("/api/driver/rides/" + id + "/complete", null);
        assertEqual(200, driver.status, "completing the ride");
        untilText("rideState", "You have arrived");
        until(() -> text("paidWith").indexOf("4242") >= 0, 20000, "what the ride was paid with");
        Map<String, Object> receipt = rider.get("/api/payments/receipts/" + id);
        assertEqual("paid", Actor.text(receipt, "status"), "the charge");
        assertEqual(0, ((Number) receipt.get("tip")).intValue(), "no tip before one is given");

        click("star-5");
        click("tip-15");
        click("done");
        until(() -> findByName("whereTo") != null, 20000, "the rider's screen to reset");
        until(() -> ((Number) rider.get("/api/payments/receipts/" + id).get("tip")).intValue()
                > 0, 20000, "the tip to be charged");

        // The ride is first in the history, and opens as its receipt.
        click("menu-open");
        click("menu-history");
        waitForForm("Rides");
        click("ride-0");
        waitForForm("Receipt");
        untilText("paymentStatus", "Paid");
        assertTrue(text("tipPaid").startsWith("$"), "the receipt shows the tip");
        assertFalse(has("sendTip"), "a ride can be tipped once");
        shot("receipt");
        back();
        waitForForm("Rides");
        back();
        waitForForm("Rider");
        driver.online(false, 0, 0);
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
