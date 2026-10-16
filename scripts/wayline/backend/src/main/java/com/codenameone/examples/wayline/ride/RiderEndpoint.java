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
package com.codenameone.examples.wayline.ride;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.wayline.Caller;
import com.codenameone.examples.wayline.Roles;
import com.codenameone.examples.wayline.api.FareQuoteDto;
import com.codenameone.examples.wayline.api.NearbyDriverDto;
import com.codenameone.examples.wayline.api.RatingDto;
import com.codenameone.examples.wayline.api.RideActivityDto;
import com.codenameone.examples.wayline.api.RideDto;
import com.codenameone.examples.wayline.api.RideRequestDto;
import com.codenameone.examples.wayline.api.RiderApiServer;
import com.codenameone.examples.wayline.push.RidePushes;

import java.util.List;

// tag::wayline-contract-endpoint[]
/// The server's half of `RiderApi`. Every method acts as the signed-in user and
/// on that user's rides only: the rider is never a parameter.
@Component
public class RiderEndpoint implements RiderApiServer {
    private final Rides rides;
    private final RidePushes pushes;

    public RiderEndpoint(Rides rides, RidePushes pushes) {
        this.rides = rides;
        this.pushes = pushes;
    }

    @Override
    public FareQuoteDto quote(RideRequestDto request) throws Exception {
        return rides.quote(request);
    }

    @Override
    public RideDto request(RideRequestDto request) throws Exception {
        return rides.request(Caller.name(), request);
    }
    // end::wayline-contract-endpoint[]

    @Override
    public RideDto active() throws Exception {
        return rides.activeForRider(Caller.name());
    }

    @Override
    public List<RideDto> history() throws Exception {
        return rides.historyForRider(Caller.name());
    }

    @Override
    public RideDto ride(String id) throws Exception {
        return rides.get(Caller.name(), Caller.hasRole(Roles.ADMIN), id);
    }

    @Override
    public RideDto cancel(String id) throws Exception {
        return rides.cancelByRider(Caller.name(), id);
    }

    @Override
    public List<NearbyDriverDto> nearby(double lat, double lng) throws Exception {
        Caller.name();
        return rides.nearbyDrivers(lat, lng);
    }

    @Override
    public RideDto rate(String id, RatingDto rating) throws Exception {
        return rides.rate(Caller.name(), id, rating == null ? 0 : rating.stars());
    }

    @Override
    public RideActivityDto activity(String id, RideActivityDto activity) throws Exception {
        return pushes.attach(Caller.name(), id, activity);
    }
}
