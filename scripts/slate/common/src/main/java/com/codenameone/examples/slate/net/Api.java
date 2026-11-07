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
package com.codenameone.examples.slate.net;

import com.codename1.io.rest.RestClients;
import com.codenameone.examples.slate.api.AccountApi;
import com.codenameone.examples.slate.api.AdminApi;
import com.codenameone.examples.slate.api.AppointmentApi;
import com.codenameone.examples.slate.api.BusinessApi;
import com.codenameone.examples.slate.api.OwnerApi;
import com.codenameone.examples.slate.api.PaymentApi;
import com.codenameone.examples.slate.api.ProviderApi;

/// The server, as the app calls it.
///
/// Each of these is an implementation the build generated from an interface in
/// the `shared` module. The server implements the other half of the same
/// interface, so a path or a field that changes on one side stops the other
/// from compiling.
public final class Api {
    private static AccountApi account;
    private static BusinessApi business;
    private static AppointmentApi appointments;
    private static PaymentApi payments;
    private static ProviderApi provider;
    private static OwnerApi owner;
    private static AdminApi admin;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        business = RestClients.create(BusinessApi.class, baseUrl);
        appointments = RestClients.create(AppointmentApi.class, baseUrl);
        payments = RestClients.create(PaymentApi.class, baseUrl);
        provider = RestClients.create(ProviderApi.class, baseUrl);
        owner = RestClients.create(OwnerApi.class, baseUrl);
        admin = RestClients.create(AdminApi.class, baseUrl);
    }

    /// Registration, the account, its preferences and push devices.
    public static AccountApi account() {
        return account;
    }

    /// The business, its services, providers, locations and free slots.
    public static BusinessApi business() {
        return business;
    }

    /// The caller's own appointments, holds and waitlist entries.
    public static AppointmentApi appointments() {
        return appointments;
    }

    /// Saved cards and the server's payment config.
    public static PaymentApi payments() {
        return payments;
    }

    /// A provider's own diary, hours, time off and calendars.
    public static ProviderApi provider() {
        return provider;
    }

    /// Running the business: catalogue, policy, the team's day, the numbers.
    public static OwnerApi owner() {
        return owner;
    }

    /// Moderation and account administration.
    public static AdminApi admin() {
        return admin;
    }
}
