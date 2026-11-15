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
package com.codenameone.examples.brickyard.engage;

import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.push.PushClient;
import com.codename1.push.PushError;
import com.codename1.push.PushListener;
import com.codename1.push.PushMessage;
import com.codename1.push.PushSubscription;
import com.codename1.ui.CN;
import com.codenameone.examples.brickyard.AppConfig;
import com.codenameone.examples.brickyard.net.Session;
import com.codenameone.examples.brickyard.ui.ExperienceForm;
import com.codenameone.examples.brickyard.ui.Nav;

/// Push, through `com.codename1.push`. A message may carry an `experience`
/// to open; anything else lands on home.
public final class Notifications {
    private static final String ENABLED = "brickyard.push";
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

    public static boolean available() {
        if (AppConfig.PUSH_APP_KEY.length() == 0 || CN.isSimulator()) {
            return false;
        }
        String platform = CN.getPlatformName();
        return "and".equals(platform) || "ios".equals(platform);
    }

    /// The registration, once the platform has answered; null before.
    public static PushSubscription subscription() {
        return subscription;
    }

    private static final class Listener implements PushListener {
        @Override
        public void onRegistration(PushSubscription registered) {
            subscription = registered;
        }

        @Override
        public void onMessage(PushMessage message) {
            if (Session.user() == null) {
                return;
            }
            Object campaign = message.getData().get("campaign");
            if (campaign != null) {
                Telemetry.pushOpened(String.valueOf(campaign));
            }
            Object experience = message.getData().get("experience");
            if (experience != null) {
                ExperienceForm.show(String.valueOf(experience));
            } else {
                Nav.home();
            }
        }

        @Override
        public void onError(PushError error) {
            Log.p("Push registration failed: " + error.getCode() + " " + error.getMessage());
        }
    }
}
