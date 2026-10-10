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
package com.codenameone.developerguide.backend.wayline;

import java.io.IOException;

/// Another processor behind the same interface.
class HostedPayments implements PaymentProvider {
    private final String secret;
    private final String returnUrl;

    HostedPayments(String secret, String returnUrl) {
        this.secret = secret;
        this.returnUrl = returnUrl;
    }

    // tag::wayline-payment-second[]
    @Override
    public String name() {
        // A name of its own. The app opens the processor's page for any
        // provider whose setup says it is hosted, so it needs no change.
        return "hosted";
    }

    @Override
    public Setup startCardSetup(String customer, String setupId) throws IOException {
        Setup setup = new Setup();
        setup.hosted = true;
        setup.url = openCardPage(customer, returnUrl + "?result=done");
        setup.reference = setupId;
        return setup;
    }
    // end::wayline-payment-second[]

    @Override
    public String createCustomer(String username, String displayName) throws IOException {
        return secret.length() + ":" + username;
    }

    @Override
    public Card finishCardSetup(String customer, String reference, CardDto typed)
            throws IOException {
        return new Card();
    }

    @Override
    public String charge(String key, String customer, String token, long amountCents,
            String currency, String description) throws IOException {
        return key;
    }

    @Override
    public String refund(String chargeReference, long amountCents) throws IOException {
        return chargeReference;
    }

    private String openCardPage(String customer, String doneUrl) throws IOException {
        return doneUrl;
    }
}
