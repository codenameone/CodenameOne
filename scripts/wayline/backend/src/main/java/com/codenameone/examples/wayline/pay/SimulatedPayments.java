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
package com.codenameone.examples.wayline.pay;

import com.codenameone.examples.wayline.Days;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.api.CardDto;

import java.io.IOException;

/// A processor that moves no money. What the sample runs on until it is given
/// a real one's key, and what every test but that provider's own runs against.
///
/// It behaves like the real thing where the app can tell the difference: a
/// card has to look like a card, and the well-known test numbers do what they
/// do at a real processor's test mode.
///
/// - `4242 4242 4242 4242` is accepted, as is any other number that passes the
///   check digit.
/// - `4000 0000 0000 0002` is declined when it is added.
/// - `4000 0000 0000 0341` is accepted and then declined when it is charged,
///   which is how a payment that fails at the end of a ride is tried out.
///
/// The card number is read here, in this method, and goes no further: the
/// brand and the last four digits are all that is returned or stored.
public class SimulatedPayments implements PaymentProvider {
    static final String DECLINED_NUMBER = "4000000000000002";
    static final String FAILS_LATER_NUMBER = "4000000000000341";
    private static final String FAILING_TOKEN = "sim_pm_fail_";

    @Override
    public String name() {
        return "simulated";
    }

    @Override
    public String createCustomer(String username, String displayName) throws IOException {
        return "sim_cus_" + Ids.next();
    }

    @Override
    public Setup startCardSetup(String customer, String setupId) {
        Setup setup = new Setup();
        setup.reference = "sim_seti_" + setupId;
        return setup;
    }

    @Override
    public Card finishCardSetup(String customer, String reference, CardDto typed)
            throws IOException {
        if (typed == null) {
            throw new PaymentDeclinedException("Enter the card's details");
        }
        String number = digits(typed.number);
        if (number.length() < 12 || number.length() > 19 || !luhn(number)) {
            throw new PaymentDeclinedException("That is not a card number");
        }
        int year = typed.expYear < 100 ? typed.expYear + 2000 : typed.expYear;
        String today = Days.iso(System.currentTimeMillis());
        int thisYear = Integer.parseInt(today.substring(0, 4));
        int thisMonth = Integer.parseInt(today.substring(5, 7));
        if (typed.expMonth < 1 || typed.expMonth > 12 || year > thisYear + 25
                || year < thisYear || (year == thisYear && typed.expMonth < thisMonth)) {
            throw new PaymentDeclinedException("That card has expired");
        }
        String cvc = typed.cvc == null ? "" : typed.cvc.trim();
        if (cvc.length() < 3 || cvc.length() > 4 || digits(cvc).length() != cvc.length()) {
            throw new PaymentDeclinedException("Check the card's security code");
        }
        if (DECLINED_NUMBER.equals(number)) {
            throw new PaymentDeclinedException("Your card was declined");
        }
        Card card = new Card();
        card.brand = brand(number);
        card.last4 = number.substring(number.length() - 4);
        card.expMonth = typed.expMonth;
        card.expYear = year;
        card.token = (FAILS_LATER_NUMBER.equals(number) ? FAILING_TOKEN : "sim_pm_") + Ids.next();
        return card;
    }

    @Override
    public String charge(String key, String customer, String token, long amountCents,
            String currency, String description) throws IOException {
        if (token.startsWith(FAILING_TOKEN)) {
            throw new PaymentDeclinedException("Your card was declined");
        }
        return "sim_ch_" + key;
    }

    @Override
    public String refund(String chargeReference, long amountCents) throws IOException {
        return "sim_re_" + Ids.next();
    }

    /// The digits of what was typed, with the spaces and dashes people put in a
    /// card number taken out. Anything else in it leaves it too short to pass.
    private static String digits(String value) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; value != null && iter < value.length(); iter++) {
            char c = value.charAt(iter);
            if (c >= '0' && c <= '9') {
                out.append(c);
            } else if (c != ' ' && c != '-') {
                return "";
            }
        }
        return out.toString();
    }

    /// The check digit every card number carries, which catches a mistyped one.
    private static boolean luhn(String number) {
        int sum = 0;
        boolean doubled = false;
        for (int iter = number.length() - 1; iter >= 0; iter--) {
            int digit = number.charAt(iter) - '0';
            if (doubled) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubled = !doubled;
        }
        return sum % 10 == 0;
    }

    private static String brand(String number) {
        if (number.startsWith("4")) {
            return "Visa";
        }
        if (number.startsWith("34") || number.startsWith("37")) {
            return "Amex";
        }
        if (number.startsWith("6011") || number.startsWith("65")) {
            return "Discover";
        }
        int two = Integer.parseInt(number.substring(0, 2));
        if ((two >= 51 && two <= 55) || (two >= 22 && two <= 27)) {
            return "Mastercard";
        }
        return "Card";
    }
}
