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
package com.codenameone.examples.bazaar.trade;

import java.io.IOException;

/// Who answers whether a VAT number is issued: [ViesRegistry] over the
/// European Commission's service, or nobody ([#NONE]), in which case only the
/// format is checked. [com.codenameone.examples.bazaar.Services#vatRegistry]
/// picks one from `bazaar.vat.registry`.
public interface VatRegistry {
    /// The registry that is never asked: every lookup answers null.
    VatRegistry NONE = new VatRegistry() {
        @Override
        public Answer lookup(String country, String number) {
            return null;
        }
    };

    /// Asks about `number` (without its prefix) in `country`. Answers null when
    /// there is no registry; throws when the registry could not answer, which
    /// is an outage and never a "no".
    Answer lookup(String country, String number) throws IOException;

    /// What the registry said.
    final class Answer {
        public final boolean valid;
        public final String name;
        public final String reference;

        public Answer(boolean valid, String name, String reference) {
            this.valid = valid;
            this.name = name == null ? "" : name;
            this.reference = reference == null ? "" : reference;
        }
    }
}
