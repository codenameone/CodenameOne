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

/// Which payment labels a shopper reads in their own language. The server
/// stores the label a payment was taken with: a generic one ("Card", "SEPA
/// Direct Debit") is a Bundle key and is translated; a brand, a wallet or a
/// card's "Visa 4242" is a name and is shown as it is.
///
/// Plain on purpose, so the rule is tested without loading the UI or Lang.
public final class PayLabels {
    /// The generic labels, each a key in every Bundle.
    static final String[] GENERIC = {"Card", "SEPA Direct Debit"};

    private PayLabels() {
    }

    /// Whether `label` is a generic label to look up in the Bundle.
    public static boolean translatable(String label) {
        for (int iter = 0; label != null && iter < GENERIC.length; iter++) {
            if (GENERIC[iter].equals(label)) {
                return true;
            }
        }
        return false;
    }
}
