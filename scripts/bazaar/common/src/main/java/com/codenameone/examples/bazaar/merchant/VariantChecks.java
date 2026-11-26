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
package com.codenameone.examples.bazaar.merchant;

import com.codenameone.examples.bazaar.ops.UnitPricing;

/// The variant editor's input checks, kept apart from its UI so a plain unit
/// test can load them without a display. A problem is returned as its
/// Bundle key, the English text; the editor translates it.
final class VariantChecks {
    private VariantChecks() {
    }

    /// Why a variant's customs and unit pricing cannot be saved, or null.
    /// `hsCode` and `country` are what [Customs] made of the fields.
    static String unitProblem(String hsCode, String country, long quantity, String unitName,
            int reference, String referenceName) {
        if (hsCode == null) {
            return ("An HS code has 6 to 10 digits");
        }
        if (country == null) {
            return ("Enter a two-letter country code, such as US or DE");
        }
        if (quantity < 0) {
            return ("Enter the measure as a number, such as 250 or 1.5");
        }
        if (quantity == 0 && unitName.length() == 0) {
            return null;
        }
        if (quantity == 0 || !UnitPricing.isUnit(unitName) || !UnitPricing.isUnit(referenceName)
                || !UnitPricing.compatible(unitName, referenceName)
                || reference < 1 || reference > UnitPricing.MAX_REFERENCE) {
            return ("Enter a measure, a unit and a price-per unit of the same kind");
        }
        return null;
    }
}
