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
import com.codenameone.examples.bazaar.api.ShippingApiServer;
import com.codenameone.examples.bazaar.api.ShippingRateDto;
import com.codenameone.examples.bazaar.api.ShippingZoneDto;

import java.util.List;

/// The server's half of `ShippingApi`.
@Component
public class ShippingEndpoint implements ShippingApiServer {
    private final RateSettings settings;

    public ShippingEndpoint(RateSettings settings) {
        this.settings = settings;
    }

    @Override
    public List<ShippingZoneDto> zones(String storeId) throws Exception {
        return settings.zones(Caller.name(), storeId);
    }

    @Override
    public ShippingZoneDto saveZone(String storeId, ShippingZoneDto body) throws Exception {
        return settings.saveZone(Caller.name(), storeId, body);
    }

    @Override
    public ShippingZoneDto deleteZone(String storeId, String zoneId) throws Exception {
        return settings.deleteZone(Caller.name(), storeId, zoneId);
    }

    @Override
    public ShippingRateDto saveRate(String storeId, ShippingRateDto body) throws Exception {
        return settings.saveRate(Caller.name(), storeId, body);
    }

    @Override
    public ShippingRateDto deleteRate(String storeId, String rateId) throws Exception {
        return settings.deleteRate(Caller.name(), storeId, rateId);
    }
}
