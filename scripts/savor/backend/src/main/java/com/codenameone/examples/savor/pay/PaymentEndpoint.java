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
package com.codenameone.examples.savor.pay;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.savor.Caller;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.api.CardDto;
import com.codenameone.examples.savor.api.PaymentApiServer;
import com.codenameone.examples.savor.api.PaymentConfigDto;
import com.codenameone.examples.savor.api.PaymentMethodDto;
import com.codenameone.examples.savor.api.PaymentSetupDto;
import com.codenameone.examples.savor.api.ReceiptDto;
import com.codenameone.examples.savor.api.TipDto;
import com.codenameone.examples.savor.order.Quotes;

import java.util.List;

/// The server's half of `PaymentApi`. Every method acts as the signed-in user
/// and on that user's own cards and receipts: whose they are is never a
/// parameter, so there is no id to change to reach someone else's.
@Component
public class PaymentEndpoint implements PaymentApiServer {
    private final Payments payments;
    private final Accounts accounts;
    private final Quotes fares;

    public PaymentEndpoint(Payments payments, Accounts accounts, Quotes fares) {
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
        return payments.startSetup(who, accounts.describe(who).displayName());
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
    public ReceiptDto receipt(String deliveryId) throws Exception {
        return payments.receipt(Caller.name(), false, deliveryId);
    }

    @Override
    public ReceiptDto tip(String id, TipDto tip) throws Exception {
        return payments.tip(Caller.name(), id, tip == null ? 0L : tip.amount());
    }
}
