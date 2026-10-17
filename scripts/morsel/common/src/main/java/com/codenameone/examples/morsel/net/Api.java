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
package com.codenameone.examples.morsel.net;

import com.codename1.io.rest.RestClients;
import com.codenameone.examples.morsel.api.AccountApi;
import com.codenameone.examples.morsel.api.AdminApi;
import com.codenameone.examples.morsel.api.CourierApi;
import com.codenameone.examples.morsel.api.OnboardingApi;
import com.codenameone.examples.morsel.api.GeoApi;
import com.codenameone.examples.morsel.api.PaymentApi;
import com.codenameone.examples.morsel.api.CustomerApi;

/// The server, as the app calls it.
///
/// Each of these is an implementation the build generated from an interface in
/// the `shared` module. The server implements the other half of the same
/// interface, so a path or a field that changes on one side stops the other
/// from compiling.
public final class Api {
    private static AccountApi account;
    private static CustomerApi customer;
    private static CourierApi courier;
    private static GeoApi geo;
    private static AdminApi admin;
    private static PaymentApi payments;
    private static OnboardingApi onboarding;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        customer = RestClients.create(CustomerApi.class, baseUrl);
        courier = RestClients.create(CourierApi.class, baseUrl);
        geo = RestClients.create(GeoApi.class, baseUrl);
        admin = RestClients.create(AdminApi.class, baseUrl);
        payments = RestClients.create(PaymentApi.class, baseUrl);
        onboarding = RestClients.create(OnboardingApi.class, baseUrl);
    }

    public static AccountApi account() {
        return account;
    }

    public static CustomerApi customer() {
        return customer;
    }

    public static CourierApi courier() {
        return courier;
    }

    public static GeoApi geo() {
        return geo;
    }

    public static AdminApi admin() {
        return admin;
    }

    public static PaymentApi payments() {
        return payments;
    }

    /// Applying to drive, which is open to everyone; [#courier] is for those
    /// whose application was approved.
    public static OnboardingApi onboarding() {
        return onboarding;
    }
}
