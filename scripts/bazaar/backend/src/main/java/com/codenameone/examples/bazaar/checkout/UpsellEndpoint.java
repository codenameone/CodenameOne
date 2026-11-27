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
package com.codenameone.examples.bazaar.checkout;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.BuyNowDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.UpsellApiServer;
import com.codenameone.examples.bazaar.api.UpsellDto;

/// The server's half of `UpsellApi`.
@Component
public class UpsellEndpoint implements UpsellApiServer {
    private final Upsells upsells;

    public UpsellEndpoint(Upsells upsells) {
        this.upsells = upsells;
    }

    @Override
    public UpsellDto upsell(String storeId) throws Exception {
        return upsells.get(Caller.name(), storeId);
    }

    @Override
    public UpsellDto setUpsell(String storeId, UpsellDto upsell) throws Exception {
        return upsells.set(Caller.name(), storeId, upsell);
    }

    @Override
    public UpsellDto offer(String storeId, String orderId) throws Exception {
        UpsellDto o = upsells.offer(Caller.name(), storeId, orderId);
        return o == null ? new UpsellDto("", "", 0, 0) : o;
    }

    @Override
    public OrderDto accept(String storeId, String orderId) throws Exception {
        return upsells.accept(Caller.name(), storeId, orderId);
    }

    @Override
    public OrderDto buyNow(String storeId, BuyNowDto buy) throws Exception {
        return upsells.buyNow(Caller.name(), storeId, buy);
    }
}
