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
package com.codenameone.examples.reel.billing;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.reel.Caller;
import com.codenameone.examples.reel.api.BillingApiServer;
import com.codenameone.examples.reel.api.CouponCheckDto;
import com.codenameone.examples.reel.api.InvoiceDto;
import com.codenameone.examples.reel.api.PlanDto;
import com.codenameone.examples.reel.api.PurchaseReceiptDto;
import com.codenameone.examples.reel.api.RentDto;
import com.codenameone.examples.reel.api.RentalDto;
import com.codenameone.examples.reel.api.SubscribeDto;
import com.codenameone.examples.reel.api.SubscriptionDto;

import java.util.List;

@Component
public class BillingEndpoint implements BillingApiServer {
    private final Billing billing;

    public BillingEndpoint(Billing billing) {
        this.billing = billing;
    }

    /// Public: the plan picker is shown before signing up.
    @Override
    public List<PlanDto> plans() throws Exception {
        return billing.plans(false);
    }

    @Override
    public SubscriptionDto subscription() throws Exception {
        return billing.subscription(Caller.name());
    }

    @Override
    public SubscriptionDto subscribe(SubscribeDto subscribe) throws Exception {
        return billing.subscribe(Caller.name(), subscribe);
    }

    @Override
    public SubscriptionDto cancel() throws Exception {
        return billing.cancel(Caller.name());
    }

    @Override
    public SubscriptionDto resume() throws Exception {
        return billing.resume(Caller.name());
    }

    @Override
    public CouponCheckDto checkCoupon(String code, String plan) throws Exception {
        Caller.name();
        return billing.checkCoupon(code, plan);
    }

    @Override
    public List<InvoiceDto> invoices() throws Exception {
        return billing.invoices(Caller.name());
    }

    @Override
    public List<RentalDto> rentals() throws Exception {
        return billing.rentals(Caller.name());
    }

    @Override
    public RentalDto rent(RentDto rent) throws Exception {
        return billing.rent(Caller.name(), rent);
    }

    @Override
    public SubscriptionDto verifyReceipt(PurchaseReceiptDto receipt) throws Exception {
        return billing.storePurchase(Caller.name(), receipt);
    }
}
