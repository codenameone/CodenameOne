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
package com.codenameone.examples.skirmish.net;

import com.codename1.io.rest.RestClients;
import com.codenameone.examples.skirmish.api.AccountApi;
import com.codenameone.examples.skirmish.api.AdminApi;
import com.codenameone.examples.skirmish.api.ForgeApi;
import com.codenameone.examples.skirmish.api.GameApi;
import com.codenameone.examples.skirmish.api.ProgressApi;
import com.codenameone.examples.skirmish.api.ShopApi;

/// The generated REST clients, one per contract in the shared module, all
/// pointed at the server [Session] is using.
public final class Api {
    private static AccountApi account;
    private static GameApi game;
    private static ShopApi shop;
    private static AdminApi admin;
    private static ProgressApi progress;
    private static ForgeApi forge;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        game = RestClients.create(GameApi.class, baseUrl);
        shop = RestClients.create(ShopApi.class, baseUrl);
        admin = RestClients.create(AdminApi.class, baseUrl);
        progress = RestClients.create(ProgressApi.class, baseUrl);
        forge = RestClients.create(ForgeApi.class, baseUrl);
    }

    public static AccountApi account() {
        return account;
    }

    public static GameApi game() {
        return game;
    }

    public static ShopApi shop() {
        return shop;
    }

    public static AdminApi admin() {
        return admin;
    }

    public static ProgressApi progress() {
        return progress;
    }

    public static ForgeApi forge() {
        return forge;
    }
}
