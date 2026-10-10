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

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.wayline.Caller;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.api.CardDto;
import com.codenameone.examples.wayline.api.PaymentApiServer;
import com.codenameone.examples.wayline.api.PaymentConfigDto;
import com.codenameone.examples.wayline.api.PaymentMethodDto;
import com.codenameone.examples.wayline.api.PaymentSetupDto;
import com.codenameone.examples.wayline.api.ReceiptDto;
import com.codenameone.examples.wayline.api.TipDto;
import com.codenameone.examples.wayline.ride.Fares;

import java.util.List;

/// The server's half of `PaymentApi`. Every method acts as the signed-in user
/// and on that user's own cards and receipts: whose they are is never a
/// parameter, so there is no id to change to reach someone else's.
@Component
public class PaymentEndpoint implements PaymentApiServer {
    private final Payments payments;
    private final Accounts accounts;
    private final Fares fares;

    public PaymentEndpoint(Payments payments, Accounts accounts, Fares fares) {
        this.payments = payments;
        this.accounts = accounts;
        this.fares = fares;
    }

    @Override
    public PaymentConfigDto config() throws Exception {
        Caller.name();
        return payments.config(fares.currency());
    }

    @Override
    public List<PaymentMethodDto> methods() throws Exception {
        return payments.methods(Caller.name());
    }

    @Override
    public PaymentSetupDto startSetup() throws Exception {
        String who = Caller.name();
        return payments.startSetup(who, accounts.describe(who).displayName);
    }

    @Override
    public PaymentMethodDto completeSetup(String id, CardDto card) throws Exception {
        return payments.completeSetup(Caller.name(), id, card);
    }

    @Override
    public PaymentMethodDto makeDefault(String id) throws Exception {
        return payments.makeDefault(Caller.name(), id);
    }

    @Override
    public PaymentMethodDto remove(String id) throws Exception {
        return payments.remove(Caller.name(), id);
    }

    @Override
    public List<ReceiptDto> receipts() throws Exception {
        return payments.receipts(Caller.name());
    }

    @Override
    public ReceiptDto receipt(String rideId) throws Exception {
        return payments.receipt(Caller.name(), false, rideId);
    }

    @Override
    public ReceiptDto tip(String id, TipDto tip) throws Exception {
        return payments.tip(Caller.name(), id, tip == null ? 0L : tip.amount);
    }
}
