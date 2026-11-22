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
package com.codenameone.examples.bazaar.ops;

/// Unit pricing ("$4.00 / 100 g") of a variant sold by measure, in integer
/// arithmetic shared by the server and the app.
///
/// A variant records the measure it is sold in (`unitQuantity` thousandths of
/// `unitName`: 750000 and `ml` for a 750 ml bottle) and the reference the unit
/// price is shown per (`unitReference` of `referenceName`: 1 and `l`). Both
/// units must measure the same thing -- mass, volume, length, area or count;
/// a price of grams per litre is no answer at all.
public final class UnitPricing {
    /// No unit price: no measure, an unknown unit, or units that measure
    /// different things.
    public static final long UNKNOWN = -1;

    /// The largest measure accepted, in thousandths: a million units.
    public static final long MAX_QUANTITY = 1000000000L;

    /// The largest reference accepted ("per 10000 g").
    public static final int MAX_REFERENCE = 10000;

    /// The units offered, each with the dimension it measures and its size in
    /// the smallest unit of that dimension.
    private static final String[] UNITS = {"mg", "g", "kg", "ml", "cl", "l", "mm", "cm", "m",
        "m2", "oz", "lb", "ct"};
    private static final char[] DIMENSION = {'w', 'w', 'w', 'v', 'v', 'v', 'd', 'd', 'd', 'a',
        'i', 'i', 'c'};
    private static final long[] FACTOR = {1, 1000, 1000000, 1, 10, 1000, 1, 10, 1000, 1, 1, 16,
        1};

    private UnitPricing() {
    }

    /// The units a merchant may choose from, in display order.
    public static String[] units() {
        String[] out = new String[UNITS.length];
        System.arraycopy(UNITS, 0, out, 0, UNITS.length);
        return out;
    }

    /// Whether `unit` is one of [#units()] (exact, as stored).
    public static boolean isUnit(String unit) {
        return index(unit) >= 0;
    }

    /// Whether two units measure the same thing.
    public static boolean compatible(String a, String b) {
        int x = index(a);
        int y = index(b);
        return x >= 0 && y >= 0 && DIMENSION[x] == DIMENSION[y];
    }

    /// The price per reference, in cents, rounded half up; [#UNKNOWN] when
    /// the measure is absent, zero or out of range, a unit is unknown, the
    /// two units measure different things, or the answer would not fit.
    ///
    /// @param priceCents the price of the variant, as shown
    /// @param unitQuantity the measure sold, in thousandths of `unitName`
    /// @param unitName the measure's unit
    /// @param reference how many `referenceName` the price is shown per
    /// @param referenceName the reference's unit
    /// @return the unit price in cents, or [#UNKNOWN]
    public static long unitPriceCents(long priceCents, long unitQuantity, String unitName,
            int reference, String referenceName) {
        int u = index(unitName);
        int r = index(referenceName);
        if (priceCents < 0 || u < 0 || r < 0 || DIMENSION[u] != DIMENSION[r]
                || unitQuantity <= 0 || unitQuantity > MAX_QUANTITY || reference <= 0
                || reference > MAX_REFERENCE) {
            return UNKNOWN;
        }
        // price * (reference * FACTOR[r] * 1000) / (unitQuantity * FACTOR[u]):
        // at most 1e13 over 1e15, reduced before multiplying so that a large
        // price still fits.
        long num = reference * FACTOR[r] * 1000L;
        long den = unitQuantity * FACTOR[u];
        long g = gcd(num, den);
        num /= g;
        den /= g;
        long price = priceCents;
        g = gcd(price, den);
        price /= g;
        den /= g;
        long half = den / 2;
        if (price != 0 && num > (Long.MAX_VALUE - half) / price) {
            return UNKNOWN;
        }
        return (price * num + half) / den;
    }

    /// "100 g" for a reference of 100 grams; a reference of one is just the
    /// unit, so a price reads "/ kg" rather than "/ 1 kg".
    public static String per(int reference, String referenceName) {
        return reference == 1 ? referenceName : reference + " " + referenceName;
    }

    /// A measure for people: "750 ml", "1.5 kg" (thousandths, trailing
    /// zeros dropped).
    public static String measure(long unitQuantity, String unitName) {
        return quantity(unitQuantity) + " " + unitName;
    }

    /// Thousandths as a plain decimal: 1500 is "1.5", 750000 is "750".
    public static String quantity(long thousandths) {
        long whole = thousandths / 1000;
        long frac = thousandths % 1000;
        StringBuilder b = new StringBuilder();
        b.append(whole);
        if (frac != 0) {
            b.append('.');
            if (frac < 100) {
                b.append('0');
            }
            if (frac < 10) {
                b.append('0');
            }
            String f = Long.toString(frac);
            int end = f.length();
            while (end > 0 && f.charAt(end - 1) == '0') {
                end--;
            }
            b.append(f.substring(0, end));
        }
        return b.toString();
    }

    /// A typed measure ("750", "1.5", "0,75") in thousandths; 0 for an empty
    /// text and -1 when it is not a plain decimal of at most three places
    /// within [#MAX_QUANTITY].
    public static long parseQuantity(String text) {
        String t = text == null ? "" : text.trim();
        if (t.length() == 0) {
            return 0;
        }
        long whole = 0;
        long frac = 0;
        int places = -1;
        int digits = 0;
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if ((c == '.' || c == ',') && places < 0) {
                places = 0;
            } else if (c >= '0' && c <= '9') {
                digits++;
                if (places < 0) {
                    whole = whole * 10 + (c - '0');
                    if (whole > MAX_QUANTITY / 1000) {
                        return -1;
                    }
                } else if (places < 3) {
                    frac = frac * 10 + (c - '0');
                    places++;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        }
        if (digits == 0) {
            return -1;
        }
        for (int p = Math.max(0, places); p < 3; p++) {
            frac *= 10;
        }
        long out = whole * 1000 + frac;
        return out > MAX_QUANTITY ? -1 : out;
    }

    private static int index(String unit) {
        if (unit == null) {
            return -1;
        }
        for (int iter = 0; iter < UNITS.length; iter++) {
            if (UNITS[iter].equals(unit)) {
                return iter;
            }
        }
        return -1;
    }

    private static long gcd(long a, long b) {
        long x = Math.abs(a);
        long y = Math.abs(b);
        while (y != 0) {
            long t = x % y;
            x = y;
            y = t;
        }
        return x == 0 ? 1 : x;
    }
}
