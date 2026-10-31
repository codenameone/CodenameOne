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
package com.codenameone.examples.dash.engage;

import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.push.PushClient;
import com.codename1.push.PushError;
import com.codename1.push.PushListener;
import com.codename1.push.PushMessage;
import com.codename1.push.PushSubscription;
import com.codename1.ui.CN;
import com.codenameone.examples.dash.AppConfig;
import com.codenameone.examples.dash.api.PushDeviceDto;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.net.Session;
import com.codenameone.examples.dash.ui.Nav;

/// Push notifications, the game's way of bringing a player back: a friend
/// accepted your challenge, your daily rewards are ready, and -- for admins
/// -- a player was flagged.
///
/// The device registers with `com.codename1.push`, then hands its token to
/// the server, which sends through the Codename One push service. A player
/// can turn them off in Settings; the server is told, and stops.
// tag::dash-push[]
public final class Notifications {
    private static final String ENABLED = "dash.push";
    private static PushClient client;
    private static PushSubscription subscription;

    private Notifications() {
    }

    public static boolean enabled() {
        return Preferences.get(ENABLED, true);
    }

    public static void setEnabled(boolean on) {
        Preferences.set(ENABLED, on);
        if (on) {
            start();
        } else {
            signingOut(() -> { });
        }
    }

    public static void start() {
        if (!available() || !enabled()) {
            return;
        }
        if (client == null) {
            client = PushClient.builder(AppConfig.PUSH_APP_KEY).listener(new Listener()).build();
        }
        client.register();
    }

    /// Tells the server where to reach this device; called after signing in.
    public static void signedIn() {
        PushDeviceDto device = device();
        if (device != null && Session.user() != null && enabled()) {
            Api.account().registerPush(device, Net.to(saved -> { }, (s, m) -> { }));
        }
    }

    public static void signingOut(Runnable then) {
        PushDeviceDto device = device();
        if (device == null || Session.user() == null) {
            then.run();
            return;
        }
        Api.account().unregisterPush(device, Net.to(gone -> then.run(), (s, m) -> then.run()));
    }

    /// Only on a phone with a push key configured; the simulator and the web
    /// build simply never ask.
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

    private static final class Listener implements PushListener {
        @Override
        public void onRegistration(PushSubscription registered) {
            subscription = registered;
            signedIn();
        }

        /// A tap on a notification opens the screen it is about.
        @Override
        public void onMessage(PushMessage message) {
            if (Session.user() == null) {
                return;
            }
            // The keys are the ones the server's GamePushes puts in `data`.
            Object challenge = message.getData().get("challenge");
            Object screen = message.getData().get("screen");
            Object campaign = message.getData().get("campaign");
            if (campaign != null) {
                Telemetry.pushOpened(String.valueOf(campaign));
            }
            if (challenge != null) {
                Nav.challenge(String.valueOf(challenge));
            } else if ("admin".equals(screen) && Session.user().admin()) {
                Nav.adminConsole();
            } else {
                Nav.screen(screen == null ? "" : String.valueOf(screen));
            }
        }

        @Override
        public void onError(PushError error) {
            Log.p("Push registration failed: " + error.getCode() + " " + error.getMessage());
        }
    }
}
// end::dash-push[]
