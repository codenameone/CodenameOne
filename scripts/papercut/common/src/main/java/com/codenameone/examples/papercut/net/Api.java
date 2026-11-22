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
package com.codenameone.examples.papercut.net;

import com.codename1.io.rest.RestClients;
import com.codenameone.examples.papercut.api.AccountApi;
import com.codenameone.examples.papercut.api.AdminApi;
import com.codenameone.examples.papercut.api.FriendsApi;
import com.codenameone.examples.papercut.api.ProgressApi;
import com.codenameone.examples.papercut.api.RunApi;
import com.codenameone.examples.papercut.api.ShopApi;

/// The generated REST clients, one per contract in the shared module, all
/// pointed at the server [Session] is using.
public final class Api {
    private static AccountApi account;
    private static RunApi runs;
    private static FriendsApi friends;
    private static ShopApi shop;
    private static ProgressApi progress;
    private static AdminApi admin;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        runs = RestClients.create(RunApi.class, baseUrl);
        friends = RestClients.create(FriendsApi.class, baseUrl);
        shop = RestClients.create(ShopApi.class, baseUrl);
        progress = RestClients.create(ProgressApi.class, baseUrl);
        admin = RestClients.create(AdminApi.class, baseUrl);
    }

    public static AccountApi account() {
        return account;
    }

    public static RunApi runs() {
        return runs;
    }

    public static FriendsApi friends() {
        return friends;
    }

    public static ShopApi shop() {
        return shop;
    }

    public static ProgressApi progress() {
        return progress;
    }

    public static AdminApi admin() {
        return admin;
    }
}
