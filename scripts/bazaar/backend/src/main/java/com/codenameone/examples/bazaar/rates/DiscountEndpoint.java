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
package com.codenameone.examples.bazaar.rates;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.DiscountApiServer;
import com.codenameone.examples.bazaar.api.DiscountDto;

import java.util.List;

/// The server's half of `DiscountApi`.
@Component
public class DiscountEndpoint implements DiscountApiServer {
    private final RateSettings settings;

    public DiscountEndpoint(RateSettings settings) {
        this.settings = settings;
    }

    @Override
    public List<DiscountDto> list(String storeId) throws Exception {
        return settings.discounts(Caller.name(), storeId);
    }

    @Override
    public DiscountDto save(String storeId, DiscountDto body) throws Exception {
        return settings.saveDiscount(Caller.name(), storeId, body);
    }

    @Override
    public DiscountDto delete(String storeId, String discountId) throws Exception {
        return settings.deleteDiscount(Caller.name(), storeId, discountId);
    }
}
