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
package com.codenameone.examples.dash;

import com.codename1.appreview.AppReview;
import com.codename1.payment.PurchaseCallback;
import com.codename1.system.Lifecycle;
import com.codenameone.examples.dash.run.Sfx;
import com.codenameone.examples.dash.engage.Ads;
import com.codenameone.examples.dash.engage.Friends;
import com.codenameone.examples.dash.engage.Notifications;
import com.codenameone.examples.dash.engage.Store;
import com.codenameone.examples.dash.engage.Telemetry;
import com.codenameone.examples.dash.net.Session;
import com.codenameone.examples.dash.ui.Nav;
import com.codenameone.examples.dash.ui.SettingsForm;
import com.codenameone.examples.dash.ui.Ui;

/// The game. It opens on whatever the stored session allows -- home, the
/// admin console, or a challenge link it was opened with -- and on the sign-in
/// screen otherwise.
///
/// Where things are:
///
/// - `rules` (its own module) -- the simulation. The server runs the same
///   classes, so the client never decides a score on its own.
/// - `run` -- the track, drawn with `com.codename1.gaming` in 3D and, where
///   a level says so, as a side-scroller, and the drivers that feed it: solo,
///   live race, replay.
/// - `ui` -- every other screen, built from ordinary widgets that
///   `src/main/css/theme.css` dresses as a game.
/// - `admin` -- the operator's console.
/// - `engage` -- analytics, push, ads and in-app purchases.
/// - `net` -- the session, the REST clients and the game socket.
///
/// The class is the `PurchaseCallback` because that is how Codename One
/// reports a finished store purchase: to the main class.
public class Dash extends Lifecycle implements PurchaseCallback {
    @Override
    public void runApp() {
        SettingsForm.apply();
        Sfx.load();
        Telemetry.start();
        Ads.start();
        // tag::dash-app-review[]
        // The store's own rating prompt, asked only of a player who has come
        // back: five launches over three days, and never twice in a month.
        // ResultForm asks after a win, the moment a player is happiest.
        AppReview.getInstance().setMinimumLaunches(5).setMinimumDaysInstalled(3)
                .setDaysBetweenPrompts(30).registerSession();
        // end::dash-app-review[]
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

    // tag::dash-purchase-callback[]
    @Override
    public void itemPurchased(String sku) {
        Store.purchased(sku);
    }

    @Override
    public void itemPurchaseError(String sku, String errorMessage) {
        Ui.toast("The purchase did not go through: " + errorMessage);
    }
    // end::dash-purchase-callback[]

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
