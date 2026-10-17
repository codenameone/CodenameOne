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
package com.codenameone.examples.morsel.delivery;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.morsel.Caller;
import com.codenameone.examples.morsel.Roles;
import com.codenameone.examples.morsel.api.FareQuoteDto;
import com.codenameone.examples.morsel.api.NearbyCourierDto;
import com.codenameone.examples.morsel.api.RatingDto;
import com.codenameone.examples.morsel.api.DeliveryActivityDto;
import com.codenameone.examples.morsel.api.DeliveryDto;
import com.codenameone.examples.morsel.api.DeliveryRequestDto;
import com.codenameone.examples.morsel.api.CustomerApiServer;
import com.codenameone.examples.morsel.push.DeliveryPushes;

import java.util.List;

// tag::morsel-contract-endpoint[]
/// The server's half of `CustomerApi`. Every method acts as the signed-in user and
/// on that user's deliveries only: the customer is never a parameter.
@Component
public class CustomerEndpoint implements CustomerApiServer {
    private final Deliveries deliveries;
    private final DeliveryPushes pushes;

    public CustomerEndpoint(Deliveries deliveries, DeliveryPushes pushes) {
        this.deliveries = deliveries;
        this.pushes = pushes;
    }

    @Override
    public FareQuoteDto quote(DeliveryRequestDto request) throws Exception {
        return deliveries.quote(request);
    }

    @Override
    public DeliveryDto request(DeliveryRequestDto request) throws Exception {
        return deliveries.request(Caller.name(), request);
    }
    // end::morsel-contract-endpoint[]

    @Override
    public DeliveryDto active() throws Exception {
        return deliveries.activeForCustomer(Caller.name());
    }

    @Override
    public List<DeliveryDto> history() throws Exception {
        return deliveries.historyForCustomer(Caller.name());
    }

    @Override
    public DeliveryDto delivery(String id) throws Exception {
        return deliveries.get(Caller.name(), Caller.hasRole(Roles.ADMIN), id);
    }

    @Override
    public DeliveryDto cancel(String id) throws Exception {
        return deliveries.cancelByCustomer(Caller.name(), id);
    }

    @Override
    public List<NearbyCourierDto> nearby(double lat, double lng) throws Exception {
        Caller.name();
        return deliveries.nearbyCouriers(lat, lng);
    }

    @Override
    public DeliveryDto rate(String id, RatingDto rating) throws Exception {
        return deliveries.rate(Caller.name(), id, rating == null ? 0 : rating.stars());
    }

    @Override
    public DeliveryActivityDto activity(String id, DeliveryActivityDto activity) throws Exception {
        return pushes.attach(Caller.name(), id, activity);
    }
}
