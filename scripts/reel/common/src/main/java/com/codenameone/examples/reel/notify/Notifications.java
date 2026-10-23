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
package com.codenameone.examples.reel.notify;

import com.codename1.push.PushClient;
import com.codename1.push.PushError;
import com.codename1.push.PushListener;
import com.codename1.push.PushMessage;
import com.codename1.push.PushSubscription;
import com.codename1.ui.CN;
import com.codenameone.examples.reel.AppConfig;
import com.codenameone.examples.reel.api.PushDeviceDto;
import com.codenameone.examples.reel.Prefs;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.net.Session;
import com.codenameone.examples.reel.ui.Nav;

/// Push notifications: the delivery news that reaches a phone in a pocket.
///
/// The phone registers with the platform's push service once, and the token
/// it gets is handed to the Reel server for whoever is signed in. Signing
/// out takes it back, so the next person to use the phone is not told about
/// the last one's deliveries. The server decides what to send and when; see the
/// server's `DeliveryPushes`.
///
/// Only on a phone (Android and iOS) and only with [AppConfig#PUSH_APP_KEY]
/// set. The simulator has no push service to register with, and a browser
/// build is told everything over the live channel while its page is open.
public final class Notifications {
    private static final Net.Failed QUIET = (status, message) -> { };
    private static PushClient client;
    private static PushSubscription subscription;

    private Notifications() {
    }

    // tag::reel-push-register[]
    /// Asks the platform for a push token. Called every time the app comes to
    /// the foreground; registering is idempotent, and the platform answers
    /// with the token it already gave when nothing has changed.
    public static void start() {
        if (!available()) {
            return;
        }
        if (client == null) {
            client = PushClient.builder(AppConfig.PUSH_APP_KEY)
                    .listener(new Listener())
                    .build();
        }
        client.register();
    }

    /// Someone has signed in: the server learns where to reach them.
    public static void signedIn() {
        PushDeviceDto device = device();
        if (device != null && Session.user() != null) {
            final String owner = Session.owner();
            Api.account().registerPush(device, Net.to(saved -> {
                // Answered for an account that has since signed out, and
                // perhaps after the next one's registration: sent again now,
                // this one lands last and the phone is the current account's.
                if (Session.user() != null && !owner.equals(Session.owner())) {
                    signedIn();
                }
            }, (status, message) -> {
                // A failure may be a lost answer to a registration that did
                // commit, so it is reconciled the same way.
                if (Session.user() != null && !owner.equals(Session.owner())) {
                    signedIn();
                }
            }));
        }
    }

    /// Someone is signing out: the server forgets this phone for them, and
    /// then `then` runs. It runs whether or not the server could be reached;
    /// signing out never waits on a notification.
    public static void signingOut(Runnable then) {
        PushDeviceDto device = device();
        if (device == null || Session.user() == null) {
            then.run();
            return;
        }
        // A failure is not kept for retrying. The row it would remove names
        // this phone's token, and the next sign-in on the phone takes that row
        // over for whoever signs in, as the server keeps a phone on the
        // account that signed in on it last. What is lost to an outage is
        // the window until then, on the phone of the account that signed out
        // -- noted in the implementor's guide among the hardening a
        // production deployment adds, not worth a durable queue in a sample.
        Api.account().unregisterPush(device, Net.to(gone -> then.run(),
                (status, message) -> then.run()));
    }
    // end::reel-push-register[]

    /// Whether this build can be sent notifications at all.
    public static boolean available() {
        if (AppConfig.PUSH_APP_KEY.length() == 0 || CN.isSimulator()) {
            return false;
        }
        String platform = CN.getPlatformName();
        return "and".equals(platform) || "ios".equals(platform);
    }

    private static PushDeviceDto device() {
        if (subscription == null) {
            return null;
        }
        return new PushDeviceDto(subscription.getTransportId(), subscription.getToken(),
                CN.getPlatformName(), Prefs.notifyNewEpisodes(), Prefs.notifyAccount());
    }

    // tag::reel-push-listener[]
    /// Called on the event thread.
    private static final class Listener implements PushListener {
        @Override
        public void onRegistration(PushSubscription registered) {
            subscription = registered;
            signedIn();
        }

        @Override
        public void onMessage(PushMessage message) {
            // A message names the title it is about (a new episode, a
            // course's certificate); the app opens that title, which reads
            // it from the server. The message's words are for the shade.
            Object title = message.getData().get("titleId");
            if (title instanceof String && Session.user() != null) {
                com.codenameone.examples.reel.viewer.TitleForm.show(CN.getCurrentForm(), (String) title);
            }
        }

        @Override
        public void onError(PushError error) {
            // Nothing to tell the user: the app works without notifications,
            // and asks again the next time it comes to the foreground.
            System.out.println("Push registration failed: " + error.getCode() + " "
                    + error.getMessage());
        }
    }
    // end::reel-push-listener[]
}
