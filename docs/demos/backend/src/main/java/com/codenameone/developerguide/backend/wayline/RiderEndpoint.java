/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.developerguide.backend.wayline;

import com.codename1.backend.annotations.Component;

// tag::wayline-contract-endpoint[]
/// The server's half of `RiderApi`. Every method acts as the signed-in user:
/// the rider is never a parameter.
@Component
public class RiderEndpoint implements RiderApiServer {
    private final Rides rides;

    public RiderEndpoint(Rides rides) {
        this.rides = rides;
    }

    @Override
    public FareQuoteDto quote(RideRequestDto request) throws Exception {
        return rides.quote(request);
    }

    @Override
    public RideDto request(RideRequestDto request) throws Exception {
        return rides.request(Caller.name(), request);
    }

    @Override
    public RideDto active() throws Exception {
        return rides.activeForRider(Caller.name());
    }

    @Override
    public RideDto cancel(String id) throws Exception {
        return rides.cancelByRider(Caller.name(), id);
    }
}
// end::wayline-contract-endpoint[]
