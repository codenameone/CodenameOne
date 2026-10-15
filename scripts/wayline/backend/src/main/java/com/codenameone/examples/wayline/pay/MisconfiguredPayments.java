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

import com.codenameone.examples.wayline.api.CardDto;

import java.io.IOException;

/// The payment provider a server gets when the one it was told to use cannot
/// be set up: every call fails, and says why.
///
/// The problem is logged when the server starts and again on every call,
/// naming the settings to fix. Each call throws an `IOException`, as an outage
/// of the processor would, so a rider is told payments are unavailable and no
/// ride is charged to a card nobody can reach.
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
