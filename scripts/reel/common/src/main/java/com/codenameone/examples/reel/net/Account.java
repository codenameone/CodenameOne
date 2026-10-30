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

import com.codenameone.examples.reel.Prefs;
import com.codenameone.examples.reel.api.AppConfigDto;
import com.codenameone.examples.reel.api.FeatureFlagDto;
import com.codenameone.examples.reel.api.HouseholdProfileDto;

import java.util.ArrayList;
import java.util.List;

/// What the app knows about the service and the account beyond who is
/// signed in: the service's branding and switches (`/api/config`, public),
/// and the household profiles, one of which is watching.
public final class Account {
    private static AppConfigDto config;
    private static List<HouseholdProfileDto> profiles = new ArrayList<HouseholdProfileDto>();

    private Account() {
    }

    public static void start() {
    }

    /// Fetches the service's configuration; runs `done` whether or not it came.
    public static void config(final Runnable done) {
        Api.admin().config(Net.to(c -> {
            config = c;
            done.run();
        }, (status, message) -> done.run()));
    }

    /// Fetches the configuration and the profiles of the account signed in.
    public static void load(final Runnable done) {
        config(() -> {
            if (Session.user() == null) {
                done.run();
                return;
            }
            Api.account().profiles(Net.to(list -> {
                profiles = list == null ? new ArrayList<HouseholdProfileDto>() : list;
                if (profile() == null) {
                    Prefs.setProfile("");
                }
                done.run();
            }, (status, message) -> done.run()));
        });
    }

    public static AppConfigDto config() {
        return config;
    }

    /// Whether a feature switch is on; true while the configuration is unknown,
    /// because the server refuses what is off anyway.
    public static boolean enabled(String key) {
        if (config == null || config.flags() == null) {
            return true;
        }
        for (FeatureFlagDto f : config.flags()) {
            if (key.equals(f.key())) {
                return f.enabled();
            }
        }
        return false;
    }

    public static List<HouseholdProfileDto> profiles() {
        return profiles;
    }

    public static void setProfiles(List<HouseholdProfileDto> list) {
        profiles = list == null ? new ArrayList<HouseholdProfileDto>() : list;
    }

    /// The profile watching, or null until one is picked.
    public static HouseholdProfileDto profile() {
        String id = Prefs.profile();
        for (HouseholdProfileDto p : profiles) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /// The id the API takes for "the profile watching": empty means the
    /// account's main profile.
    public static String profileId() {
        HouseholdProfileDto p = profile();
        return p == null ? "" : p.id();
    }

    /// Whether the account holds a role.
    public static boolean has(String role) {
        return Session.user() != null && Session.user().roles() != null && Session.user().roles().contains(role);
    }
}
