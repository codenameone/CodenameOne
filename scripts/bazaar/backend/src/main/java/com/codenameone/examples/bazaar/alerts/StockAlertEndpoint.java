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
package com.codenameone.examples.bazaar.alerts;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.PreorderDto;
import com.codenameone.examples.bazaar.api.StockAlertApiServer;
import com.codenameone.examples.bazaar.api.StockAlertDto;
import com.codenameone.examples.bazaar.cart.Codes;

import java.util.List;

/// The server's half of `StockAlertApi`.
@Component
public class StockAlertEndpoint implements StockAlertApiServer {
    private final StockAlerts alerts;

    public StockAlertEndpoint(StockAlerts alerts) {
        this.alerts = alerts;
    }

    @Override
    public StockAlertDto subscribe(String storeId, StockAlertDto body) throws Exception {
        return alerts.subscribe(Codes.callerOrNull(), storeId, body);
    }

    @Override
    public PreorderDto preorderOf(String storeId, String variantId) throws Exception {
        return alerts.preorderOf(storeId, variantId);
    }

    @Override
    public List<StockAlertDto> list(String storeId) throws Exception {
        return alerts.list(Caller.name(), storeId);
    }

    @Override
    public List<PreorderDto> preorders(String storeId) throws Exception {
        return alerts.preorders(Caller.name(), storeId);
    }

    @Override
    public PreorderDto savePreorder(String storeId, String variantId, PreorderDto body)
            throws Exception {
        return alerts.savePreorder(Caller.name(), storeId, variantId, body);
    }
}
