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
        RegisterDto request = new RegisterDto(name + "@test.example", PASSWORD, name, "+15550111",
                driver, "Green Fiat 500", "T " + name, false, null);
        String username = accounts.register(request).username();
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
