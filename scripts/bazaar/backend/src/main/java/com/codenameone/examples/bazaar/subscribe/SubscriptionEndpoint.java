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
package com.codenameone.examples.bazaar.subscribe;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.RenewalDto;
import com.codenameone.examples.bazaar.api.SellingPlanDto;
import com.codenameone.examples.bazaar.api.SubscribeDto;
import com.codenameone.examples.bazaar.api.SubscriptionApiServer;
import com.codenameone.examples.bazaar.api.SubscriptionDto;
import com.codenameone.examples.bazaar.cart.Codes;
import java.util.List;

/// The server's half of `SubscriptionApi`.
@Component
public class SubscriptionEndpoint implements SubscriptionApiServer {
    private final Subscriptions subscriptions;

    public SubscriptionEndpoint(Subscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public List<SellingPlanDto> plans(String storeId) throws Exception {
        return subscriptions.plans(Caller.name(), storeId);
    }

    @Override
    public SellingPlanDto createPlan(String storeId, SellingPlanDto body) throws Exception {
        return subscriptions.savePlan(Caller.name(), storeId, null, body);
    }

    @Override
    public SellingPlanDto savePlan(String storeId, String planId, SellingPlanDto body)
            throws Exception {
        return subscriptions.savePlan(Caller.name(), storeId, planId, body);
    }

    @Override
    public List<SubscriptionDto> list(String storeId) throws Exception {
        return subscriptions.list(Caller.name(), storeId);
    }

    @Override
    public SubscriptionDto detail(String storeId, String subscriptionId) throws Exception {
        return subscriptions.detail(Caller.name(), storeId, subscriptionId);
    }

    @Override
    public SubscriptionDto pause(String storeId, String subscriptionId) throws Exception {
        return subscriptions.change(Caller.name(), storeId, subscriptionId, "pause");
    }

    @Override
    public SubscriptionDto resume(String storeId, String subscriptionId) throws Exception {
        return subscriptions.change(Caller.name(), storeId, subscriptionId, "resume");
    }

    @Override
    public SubscriptionDto skip(String storeId, String subscriptionId) throws Exception {
        return subscriptions.change(Caller.name(), storeId, subscriptionId, "skip");
    }

    @Override
    public SubscriptionDto cancel(String storeId, String subscriptionId) throws Exception {
        return subscriptions.change(Caller.name(), storeId, subscriptionId, "cancel");
    }

    @Override
    public List<RenewalDto> calendar(String storeId, String from, String to) throws Exception {
        return subscriptions.calendar(Caller.name(), storeId, from, to);
    }

    @Override
    public List<SellingPlanDto> productPlans(String storeId, String productId) throws Exception {
        return subscriptions.productPlans(storeId, productId);
    }

    @Override
    public SubscriptionDto subscribe(String storeId, SubscribeDto body) throws Exception {
        return subscriptions.subscribe(Codes.callerOrNull(), storeId, body);
    }

    @Override
    public List<SubscriptionDto> mine(String storeId) throws Exception {
        return subscriptions.mine(Codes.callerOrNull(), storeId);
    }

    @Override
    public SubscriptionDto pauseMine(String storeId, String subscriptionId) throws Exception {
        return subscriptions.changeMine(Codes.callerOrNull(), storeId, subscriptionId, "pause");
    }

    @Override
    public SubscriptionDto resumeMine(String storeId, String subscriptionId) throws Exception {
        return subscriptions.changeMine(Codes.callerOrNull(), storeId, subscriptionId, "resume");
    }

    @Override
    public SubscriptionDto skipMine(String storeId, String subscriptionId) throws Exception {
        return subscriptions.changeMine(Codes.callerOrNull(), storeId, subscriptionId, "skip");
    }

    @Override
    public SubscriptionDto cancelMine(String storeId, String subscriptionId) throws Exception {
        return subscriptions.changeMine(Codes.callerOrNull(), storeId, subscriptionId, "cancel");
    }
}
