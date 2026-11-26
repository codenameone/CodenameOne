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
import com.codenameone.examples.bazaar.api.TaxApiServer;
import com.codenameone.examples.bazaar.api.TaxRateDto;

import java.util.List;

/// The server's half of `TaxApi`.
@Component
public class TaxEndpoint implements TaxApiServer {
    private final RateSettings settings;

    public TaxEndpoint(RateSettings settings) {
        this.settings = settings;
    }

    @Override
    public List<TaxRateDto> list(String storeId) throws Exception {
        return settings.taxRates(Caller.name(), storeId);
    }

    @Override
    public TaxRateDto save(String storeId, TaxRateDto body) throws Exception {
        return settings.saveTaxRate(Caller.name(), storeId, body);
    }

    @Override
    public TaxRateDto delete(String storeId, String rateId) throws Exception {
        return settings.deleteTaxRate(Caller.name(), storeId, rateId);
    }
}
