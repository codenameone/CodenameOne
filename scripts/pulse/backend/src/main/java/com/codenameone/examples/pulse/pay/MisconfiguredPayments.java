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
package com.codenameone.examples.pulse.pay;

import com.codenameone.examples.pulse.api.CardDto;

import java.io.IOException;

/// The payment provider a server gets when the one it was told to use cannot
/// be set up: every call fails, and says why.
///
/// The problem is logged when the server starts and again on every call,
/// naming the settings to fix. Each call throws an `IOException`, as an outage
/// of the processor would, so a customer is told payments are unavailable and no
/// delivery is charged to a card nobody can reach.
public class MisconfiguredPayments implements PaymentProvider {
    private final String provider;
    private final String problem;

    /// @param provider the provider that was asked for, which is what [#name]
    ///     answers so that stored references keep naming it
    /// @param problem what is wrong, naming the settings to fix
    public MisconfiguredPayments(String provider, String problem) {
        this.provider = provider;
        this.problem = problem;
        System.err.println("[config] " + problem);
    }

    @Override
    public String name() {
        return provider;
    }

    @Override
    public String createCustomer(String username, String displayName) throws IOException {
        throw fail();
    }

    @Override
    public void forgetCustomer(String customer) throws IOException {
        throw fail();
    }

    @Override
    public Setup startCardSetup(String customer, String setupId) throws IOException {
        throw fail();
    }

    @Override
    public Card finishCardSetup(String customer, String reference, CardDto typed)
            throws IOException {
        throw fail();
    }

    @Override
    public void detachCard(String customer, String token) throws IOException {
        throw fail();
    }

    @Override
    public String charge(String key, String customer, String token, long amountCents,
            String currency, String description) throws IOException {
        throw fail();
    }

    @Override
    public String authorize(String key, String customer, String token, long amountCents,
            String currency, String description) throws IOException {
        throw fail();
    }

    @Override
    public void capture(String reference, long amountCents) throws IOException {
        throw fail();
    }

    @Override
    public void release(String reference) throws IOException {
        throw fail();
    }

    @Override
    public Refund refund(String chargeReference, long amountCents) throws IOException {
        throw fail();
    }

    @Override
    public String refundStatus(String refundReference) throws IOException {
        throw fail();
    }

    private IOException fail() {
        System.err.println("[config] a payment call was refused: " + problem);
        return new IOException(problem);
    }
}
