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

/// A source of US sales-tax rates: what a line costs in tax when it ships to
/// a state (and, for a real provider, a street address).
///
/// [SimulatedSalesTaxProvider] is the one that ships, a fixed table by state
/// and nothing else. A real provider -- Avalara AvaTax, TaxJar, Stripe Tax,
/// Vertex -- is the same interface over its HTTP API, registered in
/// [TaxRules#provider]; the Implementors guide says what that takes.
public interface SalesTaxProvider {
    /// The name a store chooses the provider by in its tax settings.
    String name();

    /// The rate, in basis points, for goods of `category` (`standard`,
    /// `reduced`, `zero`, `exempt`...) shipped to `region`, a two-letter US
    /// state; -1 when the provider has no answer, which leaves the store's
    /// own rates in charge.
    long basisPoints(String region, String category);
}
