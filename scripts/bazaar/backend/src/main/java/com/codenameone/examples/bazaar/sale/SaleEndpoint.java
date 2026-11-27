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
package com.codenameone.examples.bazaar.sale;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.DiscountRulesDto;
import com.codenameone.examples.bazaar.api.LiveSaleDto;
import com.codenameone.examples.bazaar.api.PromoEventDto;
import com.codenameone.examples.bazaar.api.RescheduleDto;
import com.codenameone.examples.bazaar.api.SaleApiServer;
import com.codenameone.examples.bazaar.api.SaleEventDto;

import java.time.Instant;
import java.util.List;

/// The server's half of `SaleApi`.
@Component
public class SaleEndpoint implements SaleApiServer {
    private final SaleEvents sales;

    public SaleEndpoint(SaleEvents sales) {
        this.sales = sales;
    }

    @Override
    public List<SaleEventDto> list(String storeId) throws Exception {
        return sales.list(Caller.name(), storeId);
    }

    @Override
    public SaleEventDto get(String storeId, String saleId) throws Exception {
        return sales.get(Caller.name(), storeId, saleId);
    }

    @Override
    public SaleEventDto create(String storeId, SaleEventDto body) throws Exception {
        return sales.create(Caller.name(), storeId, body);
    }

    @Override
    public SaleEventDto cancel(String storeId, String saleId) throws Exception {
        return sales.cancel(Caller.name(), storeId, saleId);
    }

    @Override
    public List<PromoEventDto> calendar(String storeId, String from, String to) throws Exception {
        return sales.calendar(Caller.name(), storeId, from, to);
    }

    @Override
    public PromoEventDto reschedule(String storeId, RescheduleDto body) throws Exception {
        return sales.reschedule(Caller.name(), storeId, body);
    }

    @Override
    public DiscountRulesDto rules(String storeId, String discountId) throws Exception {
        return sales.rules(Caller.name(), storeId, discountId);
    }

    @Override
    public DiscountRulesDto saveRules(String storeId, String discountId, DiscountRulesDto body)
            throws Exception {
        return sales.saveRules(Caller.name(), storeId, discountId, body);
    }

    @Override
    public List<LiveSaleDto> live(String storeId) throws Exception {
        return sales.live(storeId, Instant.now());
    }
}
