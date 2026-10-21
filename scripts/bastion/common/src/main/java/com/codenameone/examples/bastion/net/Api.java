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
package com.codenameone.examples.bastion.net;

import com.codename1.io.rest.RestClients;
import com.codenameone.examples.bastion.api.AccountApi;
import com.codenameone.examples.bastion.api.AdminApi;
import com.codenameone.examples.bastion.api.BankingApi;
import com.codenameone.examples.bastion.api.ExchangeApi;

/// The typed clients, generated at build time from the contracts in the
/// `shared` module. Nothing else in the app builds an API URL.
public final class Api {
    private static AccountApi account;
    private static BankingApi banking;
    private static ExchangeApi exchange;
    private static AdminApi admin;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        banking = RestClients.create(BankingApi.class, baseUrl);
        exchange = RestClients.create(ExchangeApi.class, baseUrl);
        admin = RestClients.create(AdminApi.class, baseUrl);
    }

    public static AccountApi account() {
        return account;
    }

    public static BankingApi banking() {
        return banking;
    }

    public static ExchangeApi exchange() {
        return exchange;
    }

    public static AdminApi admin() {
        return admin;
    }
}
