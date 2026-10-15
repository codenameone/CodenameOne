/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.notify;

import com.codename1.push.PushClient;
import com.codename1.push.PushError;
import com.codename1.push.PushListener;
import com.codename1.push.PushMessage;
import com.codename1.push.PushSubscription;
import com.codename1.ui.CN;
import com.codenameone.examples.wayline.AppConfig;
import com.codenameone.examples.wayline.api.PushDeviceDto;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.net.Session;
import com.codenameone.examples.wayline.ui.Nav;

/// Push notifications: the ride news that reaches a phone in a pocket.
///
/// The phone registers with the platform's push service once, and the token
/// it gets is handed to the Wayline server for whoever is signed in. Signing
/// out takes it back, so the next person to use the phone is not told about
/// the last one's rides. The server decides what to send and when; see the
/// server's `RidePushes`.
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

    // tag::wayline-push-register[]
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
            Api.account().registerPush(device, Net.to(saved -> { }, QUIET));
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
        Api.account().unregisterPush(device, Net.to(gone -> then.run(),
                (status, message) -> then.run()));
    }
    // end::wayline-push-register[]

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
                CN.getPlatformName());
    }

    // tag::wayline-push-listener[]
    /// Called on the event thread. A message is about a ride: the app opens
    /// the screen that shows it, which reads the ride from the server. The
    /// message's own words are for the notification shade, not for the app.
    private static final class Listener implements PushListener {
        @Override
        public void onRegistration(PushSubscription registered) {
            subscription = registered;
            signedIn();
        }

        @Override
        public void onMessage(PushMessage message) {
            Object ride = message.getData().get("rideId");
            Object role = message.getData().get("role");
            // A message with no title is a silent one: the Live Activity
            // moving on, which the push client has already applied.
            String title = message.getTitle();
            if (ride != null && title != null && title.length() > 0
                    && Session.user() != null) {
                Nav.showRide(role == null ? Nav.RIDER : String.valueOf(role));
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
    // end::wayline-push-listener[]
}
