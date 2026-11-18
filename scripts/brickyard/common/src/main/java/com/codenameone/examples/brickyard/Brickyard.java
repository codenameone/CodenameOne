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
package com.codenameone.examples.brickyard;

import com.codename1.payment.PurchaseCallback;
import com.codename1.system.Lifecycle;
import com.codenameone.examples.brickyard.engage.Ads;
import com.codenameone.examples.brickyard.engage.Friends;
import com.codenameone.examples.brickyard.engage.Notifications;
import com.codenameone.examples.brickyard.engage.Store;
import com.codenameone.examples.brickyard.engage.Telemetry;
import com.codenameone.examples.brickyard.net.Session;
import com.codenameone.examples.brickyard.ui.Nav;
import com.codenameone.examples.brickyard.ui.Ui;

/// The player app: browse experiences, play them, climb their leaderboards,
/// dress an avatar, and make experiences of your own.
///
/// Where things are:
///
/// - `ui` -- every screen, built from ordinary widgets that
///   `src/main/css/theme.css` dresses.
/// - `play` -- running a place.
/// - `studio` -- editing one.
/// - `engage` -- analytics, push, ads, invites and in-app purchases.
/// - `net` -- the session and the REST clients.
///
/// The class is the `PurchaseCallback` because that is how Codename One
/// reports a finished store purchase: to the main class.
public class Brickyard extends Lifecycle implements PurchaseCallback {
    @Override
    public void runApp() {
        Telemetry.start();
        Ads.start();
        Session.start(AppConfig.serverUrl());
        Nav.launch();
    }

    @Override
    public void start() {
        super.start();
        Notifications.start();
        Friends.start();
    }

    @Override
    public void stop() {
        super.stop();
        Telemetry.flush();
    }

    @Override
    public void itemPurchased(String sku) {
        Store.purchased(sku);
    }

    @Override
    public void itemPurchaseError(String sku, String errorMessage) {
        Ui.toast("The purchase did not go through: " + errorMessage);
    }

    @Override
    public void itemRefunded(String sku) {
        // The server hears about refunds from the store itself.
    }

    @Override
    public void subscriptionStarted(String sku) {
        Store.purchased(sku);
    }

    @Override
    public void subscriptionCanceled(String sku) {
    }

    @Override
    public void paymentFailed(String paymentCode, String failureReason) {
    }

    @Override
    public void paymentSucceeded(String paymentCode, double amount, String currency) {
    }
}
