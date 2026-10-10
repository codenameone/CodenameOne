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
package com.codenameone.examples.wayline;

import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.account.DemoAccounts;
import com.codenameone.examples.wayline.api.RegisterDto;

import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.jwt;

/// The people a test needs, made fresh for it.
///
/// Every test class shares one running server and one database, so a test that
/// borrowed a demo account would meet whatever the test before it left behind:
/// a ride under way, a driver still on line. Each test makes its own people and
/// works in its own city instead.
final class People {
    static final String PASSWORD = "a-long-password";

    private People() {
    }

    /// Opens an account whose phone number is already proved, and returns its
    /// name.
    static String rider(Accounts accounts, String name) throws Exception {
        return open(accounts, name, false);
    }

    static String driver(Accounts accounts, String name) throws Exception {
        return open(accounts, name, true);
    }

    private static String open(Accounts accounts, String name, boolean driver) throws Exception {
        RegisterDto request = new RegisterDto();
        request.email = name + "@test.example";
        request.password = PASSWORD;
        request.displayName = name;
        request.phone = "+15550111";
        request.driver = driver;
        request.vehicle = "Green Fiat 500";
        request.plate = "T " + name;
        String username = accounts.register(request).username;
        accounts.markPhoneVerified(username);
        if (driver) {
            // Registering asks to drive and no more. An admin's grant is what
            // makes the account an approved driver, of a standard car.
            accounts.setRoles(DemoAccounts.ADMIN, username, true, false);
        }
        return username;
    }

    /// The request, made as a rider. The authorities are stated here and not
    /// read from the account: these are the tests of what a role may do, and
    /// `AccessTest` is the one that signs in for real.
    static MockRequestBuilder asRider(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_RIDER"));
    }

    static MockRequestBuilder asDriver(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_RIDER", "ROLE_DRIVER"));
    }

    static MockRequestBuilder asAdmin(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_RIDER", "ROLE_ADMIN"));
    }

    /// The JSON of a ride request between two points.
    static String ride(double fromLat, double fromLng, double toLat, double toLng) {
        return "{\"pickupLat\":" + fromLat + ",\"pickupLng\":" + fromLng
                + ",\"pickupAddress\":\"Here\",\"dropoffLat\":" + toLat + ",\"dropoffLng\":" + toLng
                + ",\"dropoffAddress\":\"There\"}";
    }

    static String position(boolean online, double lat, double lng) {
        return "{\"online\":" + online + ",\"lat\":" + lat + ",\"lng\":" + lng + ",\"heading\":90}";
    }
}
