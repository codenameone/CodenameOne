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
package com.codenameone.examples.bazaar.taxes;

/// The US sales-tax provider that needs no account: each state's base rate,
/// with no county, city or district rates on top, and the five states with no
/// sales tax at zero. Groceries-style `reduced` goods are taxed at half the
/// base rate and `zero` and `exempt` goods at nothing -- a stand-in for the
/// product taxability rules a real provider applies, good for showing the
/// flow and for tests, not for filing.
public final class SimulatedSalesTaxProvider implements SalesTaxProvider {
    public static final String NAME = "simulated";

    /// State, base rate in basis points.
    private static final String[][] STATES = {
        {"AL", "400"}, {"AK", "0"}, {"AZ", "560"}, {"AR", "650"}, {"CA", "725"},
        {"CO", "290"}, {"CT", "635"}, {"DE", "0"}, {"DC", "600"}, {"FL", "600"},
        {"GA", "400"}, {"HI", "400"}, {"ID", "600"}, {"IL", "625"}, {"IN", "700"},
        {"IA", "600"}, {"KS", "650"}, {"KY", "600"}, {"LA", "445"}, {"ME", "550"},
        {"MD", "600"}, {"MA", "625"}, {"MI", "600"}, {"MN", "688"}, {"MS", "700"},
        {"MO", "423"}, {"MT", "0"}, {"NE", "550"}, {"NV", "685"}, {"NH", "0"},
        {"NJ", "663"}, {"NM", "488"}, {"NY", "400"}, {"NC", "475"}, {"ND", "500"},
        {"OH", "575"}, {"OK", "450"}, {"OR", "0"}, {"PA", "600"}, {"RI", "700"},
        {"SC", "600"}, {"SD", "420"}, {"TN", "700"}, {"TX", "625"}, {"UT", "610"},
        {"VT", "600"}, {"VA", "530"}, {"WA", "650"}, {"WV", "600"}, {"WI", "500"},
        {"WY", "400"},
    };

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public long basisPoints(String region, String category) {
        if (region == null) {
            return -1;
        }
        String state = region.trim();
        for (int iter = 0; iter < STATES.length; iter++) {
            if (STATES[iter][0].equalsIgnoreCase(state)) {
                long base = Long.parseLong(STATES[iter][1]);
                if (TaxRules.ZERO.equals(category) || TaxRules.EXEMPT.equals(category)) {
                    return 0;
                }
                if (TaxRules.REDUCED.equals(category) || TaxRules.SUPER_REDUCED.equals(category)) {
                    return base / 2;
                }
                return base;
            }
        }
        return -1;
    }
}
