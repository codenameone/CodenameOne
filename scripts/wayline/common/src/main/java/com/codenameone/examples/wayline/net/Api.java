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
package com.codenameone.examples.wayline.net;

import com.codename1.io.rest.RestClients;
import com.codenameone.examples.wayline.api.AccountApi;
import com.codenameone.examples.wayline.api.AdminApi;
import com.codenameone.examples.wayline.api.DriverApi;
import com.codenameone.examples.wayline.api.DrivingApi;
import com.codenameone.examples.wayline.api.GeoApi;
import com.codenameone.examples.wayline.api.PaymentApi;
import com.codenameone.examples.wayline.api.RiderApi;

/// The server, as the app calls it.
///
/// Each of these is an implementation the build generated from an interface in
/// the `shared` module. The server implements the other half of the same
/// interface, so a path or a field that changes on one side stops the other
/// from compiling.
public final class Api {
    private static AccountApi account;
    private static RiderApi rider;
    private static DriverApi driver;
    private static GeoApi geo;
    private static AdminApi admin;
    private static PaymentApi payments;
    private static DrivingApi driving;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        rider = RestClients.create(RiderApi.class, baseUrl);
        driver = RestClients.create(DriverApi.class, baseUrl);
        geo = RestClients.create(GeoApi.class, baseUrl);
        admin = RestClients.create(AdminApi.class, baseUrl);
        payments = RestClients.create(PaymentApi.class, baseUrl);
        driving = RestClients.create(DrivingApi.class, baseUrl);
    }

    public static AccountApi account() {
        return account;
    }

    public static RiderApi rider() {
        return rider;
    }

    public static DriverApi driver() {
        return driver;
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

    /// Applying to drive, which is open to everyone; [#driver] is for those
    /// whose application was approved.
    public static DrivingApi driving() {
        return driving;
    }
}
