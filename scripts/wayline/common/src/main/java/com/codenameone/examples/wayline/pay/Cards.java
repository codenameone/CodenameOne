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

/// What a card number, an expiry date and a security code look like.
///
/// Checking them here is a courtesy: it says "that is not a card number" before
/// a request is made. The decision is the provider's, which checks again and
/// is the only one who knows whether the card is real.
public final class Cards {
    private Cards() {
    }

    /// Only the digits of `text`.
    public static String digits(String text) {
        StringBuilder digits = new StringBuilder();
        for (int iter = 0; text != null && iter < text.length(); iter++) {
            char letter = text.charAt(iter);
            if (letter >= '0' && letter <= '9') {
                digits.append(letter);
            }
        }
        return digits.toString();
    }

    /// The digits of `text` in fours, as a card has them: `4242 4242`.
    public static String group(String text) {
        String digits = digits(text);
        if (digits.length() > 19) {
            digits = digits.substring(0, 19);
        }
        StringBuilder grouped = new StringBuilder();
        for (int iter = 0; iter < digits.length(); iter++) {
            if (iter > 0 && iter % 4 == 0) {
                grouped.append(' ');
            }
            grouped.append(digits.charAt(iter));
        }
        return grouped.toString();
    }

    /// The digits of `text` as a month and a year: `0428` is `04/28`.
    public static String expiry(String text) {
        String digits = digits(text);
        if (digits.length() > 4) {
            digits = digits.substring(0, 4);
        }
        return digits.length() <= 2 ? digits : digits.substring(0, 2) + "/" + digits.substring(2);
    }

    public static int month(String expiry) {
        String digits = digits(expiry);
        return digits.length() < 2 ? 0 : Integer.parseInt(digits.substring(0, 2));
    }

    /// The year in full: a card says `28` and means 2028.
    public static int year(String expiry) {
        String digits = digits(expiry);
        return digits.length() < 4 ? 0 : 2000 + Integer.parseInt(digits.substring(2, 4));
    }

    /// The network a number belongs to, from how it starts; empty while that
    /// cannot be told.
    public static String brand(String digits) {
        if (digits.startsWith("4")) {
            return "Visa";
        }
        if (digits.startsWith("34") || digits.startsWith("37")) {
            return "Amex";
        }
        if (digits.startsWith("6011") || digits.startsWith("65")) {
            return "Discover";
        }
        if (digits.length() >= 2) {
            int two = Integer.parseInt(digits.substring(0, 2));
            if (two >= 51 && two <= 55 || two >= 22 && two <= 27) {
                return "Mastercard";
            }
        }
        return "";
    }

    /// What is wrong with what was typed, in English, or null when nothing is.
    public static String check(String number, String expiry, String cvc) {
        String digits = digits(number);
        if (digits.length() < 12 || digits.length() > 19 || !luhn(digits)) {
            return "That is not a card number. Check the digits.";
        }
        int month = month(expiry);
        if (month < 1 || month > 12 || year(expiry) == 0) {
            return "Write the expiry date as month and year: 04/28.";
        }
        String code = cvc == null ? "" : cvc.trim();
        if (code.length() < 3 || code.length() > 4 || digits(code).length() != code.length()) {
            return "The security code is the three or four digits on the card.";
        }
        return null;
    }

    /// The check digit every card number ends in, which catches a mistyped one.
    public static boolean luhn(String digits) {
        int sum = 0;
        boolean twice = false;
        for (int iter = digits.length() - 1; iter >= 0; iter--) {
            int digit = digits.charAt(iter) - '0';
            if (twice) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            twice = !twice;
        }
        return digits.length() > 0 && sum % 10 == 0;
    }
}
