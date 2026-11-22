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
package com.codenameone.examples.papercut;

import com.codename1.payment.PurchaseCallback;
import com.codename1.system.Lifecycle;
import com.codenameone.examples.papercut.engage.Ads;
import com.codenameone.examples.papercut.engage.Notifications;
import com.codenameone.examples.papercut.engage.Store;
import com.codenameone.examples.papercut.net.Session;
import com.codenameone.examples.papercut.play.Sfx;
import com.codenameone.examples.papercut.ui.Nav;
import com.codenameone.examples.papercut.ui.Ui;

/// A twin-stick arena shooter on a sheet of notebook paper.
///
/// Where things are:
///
/// - `play` -- the game: [com.codenameone.examples.papercut.play.Arena] is the
///   whole simulation, with no UI in it, and
///   [com.codenameone.examples.papercut.play.ArenaView] draws it as cut paper
///   with `com.codename1.gaming` and reads the thumbs, keys and gamepad.
/// - `ui` -- every other screen, ordinary widgets that `src/main/css/theme.css`
///   dresses as paper on a desk.
/// - `net` -- the session and the REST clients generated from `shared`.
/// - `engage` -- the store, ads and push.
/// - `admin` -- the console, for an account with the ADMIN role.
public class Papercut extends Lifecycle implements PurchaseCallback {
    @Override
    public void runApp() {
        Ui.desktopFonts();
        Sfx.load();
        Ads.start();
        Session.start(AppConfig.serverUrl());
        Nav.launch();
    }

    @Override
    public void start() {
        super.start();
        Notifications.start();
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
        // Papercut sells no subscriptions.
    }

    @Override
    public void paymentFailed(String paymentCode, String failureReason) {
        // Not used: purchases go through the platform store.
    }

    @Override
    public void paymentSucceeded(String paymentCode, double amount, String currency) {
        // Not used: purchases go through the platform store.
    }
}
