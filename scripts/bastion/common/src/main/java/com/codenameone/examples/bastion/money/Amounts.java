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
package com.codenameone.examples.bastion.money;

/// Amounts as text and back, in **integer arithmetic only**.
///
/// A balance is a whole number of the asset's smallest unit (see [Assets]).
/// Turning one into a `double` to print it is how a bank app shows
/// `0.30000000000000004` or, worse, posts one cent less than the user typed:
/// a double has 53 bits, and 0.1 is not one of the numbers it can hold. So the
/// text is cut out of the digits of the `long` itself, and what the user types
/// is read digit by digit into a `long`, refused rather than rounded when it
/// has more decimal places than the asset has.
public final class Amounts {
    /// What [#parse] answers for text that is not an amount.
    public static final long INVALID = -1;

    private Amounts() {
    }

    /// `units` with `decimals` places and thousands separators:
    /// `format(123456, 2)` is `1,234.56`. Crypto shows every place it has
    /// down to `minFraction`, trailing zeros dropped: `0.015 BTC`, not
    /// `0.01500000`.
    public static String format(long units, int decimals, int minFraction) {
        boolean negative = units < 0;
        // Long.MIN_VALUE has no positive twin; it is never a balance, but the
        // digits are taken from the negative side so it still prints.
        String digits = negative ? Long.toString(units).substring(1) : Long.toString(units);
        while (digits.length() <= decimals) {
            digits = "0" + digits;
        }
        String whole = digits.substring(0, digits.length() - decimals);
        String fraction = digits.substring(digits.length() - decimals);
        int keep = fraction.length();
        while (keep > minFraction && fraction.charAt(keep - 1) == '0') {
            keep--;
        }
        fraction = fraction.substring(0, keep);
        StringBuilder out = new StringBuilder();
        if (negative) {
            out.append('-');
        }
        for (int iter = 0; iter < whole.length(); iter++) {
            if (iter > 0 && (whole.length() - iter) % 3 == 0) {
                out.append(',');
            }
            out.append(whole.charAt(iter));
        }
        if (fraction.length() > 0) {
            out.append('.').append(fraction);
        }
        return out.toString();
    }

    /// `units` of `asset` the way the app shows money: `$1,234.56`,
    /// `1,234.56 EUR`, `0.015 BTC`.
    public static String format(long units, String asset) {
        int decimals = Assets.decimals(asset);
        boolean crypto = Assets.isCrypto(asset);
        String number = format(units, decimals, crypto ? Math.min(2, decimals) : decimals);
        if ("USD".equals(asset)) {
            return number.startsWith("-") ? "-$" + number.substring(1) : "$" + number;
        }
        return number + " " + asset;
    }

    /// The number alone, as an input field shows it: no separators.
    public static String plain(long units, String asset) {
        int decimals = Assets.decimals(asset);
        String text = format(units, decimals, Assets.isCrypto(asset) ? 0 : decimals);
        return text.replace(",", "");
    }

    /// What the user typed, in units of an asset with `decimals` places, or
    /// [#INVALID]. Accepts `12`, `12.5`, `1,200.50`, `.5`; refuses a sign,
    /// a second point, more places than the asset has, and anything that
    /// would not fit in a `long`. Zero is a valid amount here; whether it is
    /// a valid payment is the caller's question.
    public static long parse(String text, int decimals) {
        if (text == null) {
            return INVALID;
        }
        String trimmed = text.trim();
        if (trimmed.length() == 0) {
            return INVALID;
        }
        long value = 0;
        int places = -1;
        boolean anyDigit = false;
        for (int iter = 0; iter < trimmed.length(); iter++) {
            char c = trimmed.charAt(iter);
            if (c == ',' && places < 0) {
                continue;
            }
            if (c == '.') {
                if (places >= 0) {
                    return INVALID;
                }
                places = 0;
                continue;
            }
            if (c < '0' || c > '9') {
                return INVALID;
            }
            if (places >= 0) {
                places++;
                if (places > decimals) {
                    return INVALID;
                }
            }
            anyDigit = true;
            value = times10plus(value, c - '0');
            if (value < 0) {
                return INVALID;
            }
        }
        if (!anyDigit) {
            return INVALID;
        }
        for (int pad = Math.max(places, 0); pad < decimals; pad++) {
            value = times10plus(value, 0);
            if (value < 0) {
                return INVALID;
            }
        }
        return value;
    }

    /// [#parse] for an amount of `asset`.
    public static long parse(String text, String asset) {
        return parse(text, Assets.decimals(asset));
    }

    /// What `quantity` units of an asset with `baseDecimals` places is worth
    /// at `price` (quote units per one whole base unit), rounded down -- the
    /// server's rule for a valuation that is shown, not charged. Split into
    /// whole and part so that a large holding at a large price cannot
    /// overflow on the way: `q * p / 10^d` would, `(q / 10^d) * p` does not.
    public static long value(long quantity, int baseDecimals, long price) {
        long scale = pow10(baseDecimals);
        long whole = quantity / scale;
        long part = quantity % scale;
        return whole * price + part * price / scale;
    }

    /// 10 to the `n`, for the decimals an asset can have.
    public static long pow10(int n) {
        long out = 1;
        for (int iter = 0; iter < n; iter++) {
            out *= 10;
        }
        return out;
    }

    /// `value * 10 + digit`, or -1 when it would overflow.
    private static long times10plus(long value, int digit) {
        if (value > (Long.MAX_VALUE - digit) / 10) {
            return -1;
        }
        return value * 10 + digit;
    }
}
