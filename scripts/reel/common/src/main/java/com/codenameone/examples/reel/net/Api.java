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
package com.codenameone.examples.reel.net;

import com.codename1.io.rest.RestClients;
import com.codenameone.examples.reel.api.AccountApi;
import com.codenameone.examples.reel.api.AdminApi;
import com.codenameone.examples.reel.api.BillingApi;
import com.codenameone.examples.reel.api.CatalogApi;
import com.codenameone.examples.reel.api.LearningApi;
import com.codenameone.examples.reel.api.LibraryApi;
import com.codenameone.examples.reel.api.ModerationApi;
import com.codenameone.examples.reel.api.PaymentApi;
import com.codenameone.examples.reel.api.PlaybackApi;
import com.codenameone.examples.reel.api.StudioApi;

/// One client per contract, all against the server the app is connected to.
public final class Api {
    private static AccountApi account;
    private static AdminApi admin;
    private static BillingApi billing;
    private static CatalogApi catalog;
    private static LearningApi learning;
    private static LibraryApi library;
    private static ModerationApi moderation;
    private static PaymentApi payment;
    private static PlaybackApi playback;
    private static StudioApi studio;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        admin = RestClients.create(AdminApi.class, baseUrl);
        billing = RestClients.create(BillingApi.class, baseUrl);
        catalog = RestClients.create(CatalogApi.class, baseUrl);
        learning = RestClients.create(LearningApi.class, baseUrl);
        library = RestClients.create(LibraryApi.class, baseUrl);
        moderation = RestClients.create(ModerationApi.class, baseUrl);
        payment = RestClients.create(PaymentApi.class, baseUrl);
        playback = RestClients.create(PlaybackApi.class, baseUrl);
        studio = RestClients.create(StudioApi.class, baseUrl);
    }

    public static AccountApi account() {
        return account;
    }

    public static AdminApi admin() {
        return admin;
    }

    public static BillingApi billing() {
        return billing;
    }

    public static CatalogApi catalog() {
        return catalog;
    }

    public static LearningApi learning() {
        return learning;
    }

    public static LibraryApi library() {
        return library;
    }

    public static ModerationApi moderation() {
        return moderation;
    }

    public static PaymentApi payments() {
        return payment;
    }

    public static PlaybackApi playback() {
        return playback;
    }

    public static StudioApi studio() {
        return studio;
    }
}
