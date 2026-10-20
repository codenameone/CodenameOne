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
package com.codenameone.examples.savor.net;

import com.codename1.io.rest.RestClients;
import com.codenameone.examples.savor.api.AccountApi;
import com.codenameone.examples.savor.api.AdminApi;
import com.codenameone.examples.savor.api.CatalogApi;
import com.codenameone.examples.savor.api.CourierApi;
import com.codenameone.examples.savor.api.OnboardingApi;
import com.codenameone.examples.savor.api.GeoApi;
import com.codenameone.examples.savor.api.MerchantApi;
import com.codenameone.examples.savor.api.OrderApi;
import com.codenameone.examples.savor.api.PaymentApi;

/// The server, as the app calls it.
///
/// Each of these is an implementation the build generated from an interface in
/// the `shared` module. The server implements the other half of the same
/// interface, so a path or a field that changes on one side stops the other
/// from compiling.
public final class Api {
    private static AccountApi account;
    private static CatalogApi catalog;
    private static OrderApi orders;
    private static MerchantApi merchant;
    private static CourierApi courier;
    private static GeoApi geo;
    private static AdminApi admin;
    private static PaymentApi payments;
    private static OnboardingApi onboarding;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        catalog = RestClients.create(CatalogApi.class, baseUrl);
        orders = RestClients.create(OrderApi.class, baseUrl);
        merchant = RestClients.create(MerchantApi.class, baseUrl);
        courier = RestClients.create(CourierApi.class, baseUrl);
        geo = RestClients.create(GeoApi.class, baseUrl);
        admin = RestClients.create(AdminApi.class, baseUrl);
        payments = RestClients.create(PaymentApi.class, baseUrl);
        onboarding = RestClients.create(OnboardingApi.class, baseUrl);
    }

    public static AccountApi account() {
        return account;
    }

    /// Restaurants and their menus, and applying to open one.
    public static CatalogApi catalog() {
        return catalog;
    }

    /// The customer's orders, favourites and addresses.
    public static OrderApi orders() {
        return orders;
    }

    /// The restaurant the caller is on the staff of.
    public static MerchantApi merchant() {
        return merchant;
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

    /// Applying to deliver, which is open to everyone; [#courier] is for those
    /// whose application was approved.
    public static OnboardingApi onboarding() {
        return onboarding;
    }
}
