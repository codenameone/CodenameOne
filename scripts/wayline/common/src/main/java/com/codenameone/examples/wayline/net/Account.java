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
package com.codenameone.examples.wayline.net;

import com.codename1.ui.CN;
import com.codenameone.examples.wayline.Prefs;
import com.codenameone.examples.wayline.api.PlaceDto;
import com.codenameone.examples.wayline.api.PreferencesDto;
import com.codenameone.examples.wayline.api.ProfileDto;
import com.codenameone.examples.wayline.api.UserDto;

/// Keeps what the user chose in Settings with their account.
///
/// [Prefs] is still what every screen reads and writes, and still on the
/// device: that copy is what the app starts from before the server has
/// answered, and all there is for someone who is signed out. This class is the
/// other half. When someone signs in it fetches the account's values and puts
/// them into `Prefs`, so a phone they have never used looks like the one they
/// left; and from then on it is told of every change and sends it back.
///
/// A change is not sent the moment it is made. A name typed into a field
/// changes with every letter, so changes are gathered for a moment and sent as
/// one request with everything in it.
public final class Account {
    /// How long after the last change the values are sent.
    private static final int SETTLE_MILLIS = 700;
    private static final Net.Failed QUIET = (status, message) -> { };

    /// True while the account's values are being put into `Prefs`, which
    /// reports each of them as a change like any other.
    private static boolean applying;
    private static boolean preferencesChanged;
    private static boolean profileChanged;
    private static boolean waiting;
    /// Whether each half of the account's values has arrived since it signed
    /// in. Until one has, the device's copy of it is not the account's -- it
    /// is the defaults, or what an earlier session left -- and is not sent.
    private static boolean preferencesLoaded;
    private static boolean profileLoaded;
    /// The profile as the server last gave it: the name and the gender are
    /// part of it, and are sent back as they were with the rest.
    private static ProfileDto profile;
    /// What a request carries beyond the settings, for a test to look at.
    private static PreferencesDto sent;

    private Account() {
    }

    /// Starts listening to `Prefs`. Called once, when the app starts.
    public static void start() {
        Prefs.onChange(Account::changed);
    }

    /// Fetches the account's settings and profile into `Prefs`, then runs
    /// `done` -- also when the server did not answer, with what the device
    /// had. Each half is asked for whether or not the other came, and one that
    /// did not come is asked for again before anything of it is saved.
    public static void load(final Runnable done) {
        if (Session.user() == null) {
            done.run();
            return;
        }
        profile = null;
        preferencesLoaded = false;
        profileLoaded = false;
        fetch(done);
    }

    private static void fetch(final Runnable done) {
        final String owner = Session.user().username();
        final Runnable thenProfile = () -> {
            if (profileLoaded || !signedInAs(owner)) {
                done.run();
                return;
            }
            Api.account().profile(Net.to(described -> {
                if (signedInAs(owner)) {
                    apply(described);
                    profileLoaded = described != null;
                }
                done.run();
            }, (status, message) -> done.run()));
        };
        if (preferencesLoaded) {
            thenProfile.run();
            return;
        }
        Api.account().preferences(Net.to(preferences -> {
            if (signedInAs(owner)) {
                apply(preferences);
                preferencesLoaded = preferences != null;
            }
            thenProfile.run();
        }, (status, message) -> thenProfile.run()));
    }

    /// The profile last fetched or saved, or null before there is one.
    public static ProfileDto profile() {
        return profile;
    }

    /// Saves the name and the gender, which only the profile screen changes.
    public static void saveProfile(final String name, final String gender, final Runnable done,
            final Net.Failed failed) {
        if (!profileLoaded) {
            // The rest of the profile goes out with the name, and it has not
            // been seen yet; see send().
            fetch(() -> {
                if (profileLoaded) {
                    saveProfile(name, gender, done, failed);
                } else {
                    failed.failed(0, "Could not reach the server. Check your connection.");
                }
            });
            return;
        }
        ProfileDto wanted = described(name, gender);
        final String owner = Session.user() == null ? null : Session.user().username();
        Api.account().saveProfile(wanted, Net.to(user -> {
            // The answer is about the account that asked; see send().
            if (signedInAs(owner)) {
                profile = wanted;
                Session.setUser(user);
            }
            done.run();
        }, failed));
    }

    /// The preferences sent last, or null when none were.
    public static PreferencesDto lastSent() {
        return sent;
    }

    /// Sends what is waiting now, for a screen that is about to end the session.
    public static void flush() {
        waiting = false;
        send();
    }

    private static void changed(String name) {
        if (applying || Session.user() == null || Prefs.RECENTS.equals(name)) {
            return;
        }
        if (Prefs.EMERGENCY_NAME.equals(name) || Prefs.EMERGENCY_PHONE.equals(name)
                || Prefs.HOME.equals(name) || Prefs.WORK.equals(name)) {
            profileChanged = true;
        }
        // The contact's number is in both: it is who a trip is shared with.
        if (!Prefs.EMERGENCY_NAME.equals(name) && !Prefs.HOME.equals(name)
                && !Prefs.WORK.equals(name)) {
            preferencesChanged = true;
        }
        if (!waiting) {
            waiting = true;
            CN.setTimeout(SETTLE_MILLIS, () -> CN.callSerially(() -> {
                if (waiting) {
                    waiting = false;
                    send();
                }
            }));
        }
    }

    private static void send() {
        if (Session.user() == null) {
            preferencesChanged = false;
            profileChanged = false;
            return;
        }
        if (preferencesChanged && !preferencesLoaded || profileChanged && !profileLoaded) {
            // The account's values never arrived, so what the device holds in
            // their place is not known to be this account's: sent, it would
            // replace on the server what was never seen here. They are asked
            // for again instead, and the change that brought this about gives
            // way to them. A later change is saved as usual once they are in.
            preferencesChanged = preferencesChanged && preferencesLoaded;
            profileChanged = profileChanged && profileLoaded;
            fetch(() -> { });
        }
        if (preferencesChanged) {
            preferencesChanged = false;
            sent = preferences();
            Api.account().savePreferences(sent, Net.to(saved -> { }, QUIET));
        }
        if (profileChanged) {
            profileChanged = false;
            final ProfileDto wanted = described();
            final String owner = Session.user().username();
            Api.account().saveProfile(wanted, Net.to(user -> {
                // Signing out sends what is waiting and does not wait for the
                // answer. If somebody else has signed in by the time it comes,
                // this profile is not theirs: kept, its name and gender would
                // go out with their next change and overwrite their own.
                if (signedInAs(owner)) {
                    profile = wanted;
                }
            }, QUIET));
        }
    }

    /// Whether the account signed in now is `username`: an answer that comes
    /// back after the session it was asked in has ended is not acted on.
    private static boolean signedInAs(String username) {
        UserDto now = Session.user();
        return now != null && username != null && username.equals(now.username());
    }

    private static PreferencesDto preferences() {
        String language = Prefs.language();
        return new PreferencesDto(
                language.length() == 0 ? com.codenameone.examples.wayline.ui.Lang.current()
                        : language,
                Prefs.theme(),
                Prefs.units(),
                Prefs.notifyRideUpdates(),
                Prefs.notifyReceipts(),
                Prefs.notifyPromotions(),
                Prefs.driverGender(),
                Prefs.quietRide(),
                Prefs.accessibleVehicle(),
                Prefs.petFriendly(),
                Prefs.paymentMethod(),
                Prefs.tipPercent(),
                Prefs.shareTrips() ? Prefs.emergencyContactPhone() : "");
    }

    /// The profile to send: the one the server gave, with the device's values
    /// for what `Prefs` holds.
    private static ProfileDto described() {
        return described(profile == null ? Session.user().displayName() : profile.name(),
                profile == null ? Session.user().gender() : profile.gender());
    }

    /// The same, with `name` and `gender` in place of the ones held.
    private static ProfileDto described(String name, String gender) {
        PlaceDto home = Prefs.home();
        PlaceDto work = Prefs.work();
        return new ProfileDto(name, gender, Prefs.emergencyContactName(),
                Prefs.emergencyContactPhone(), home == null ? "" : home.name(),
                work == null ? "" : work.name());
    }

    private static void apply(PreferencesDto from) {
        if (from == null) {
            return;
        }
        applying = true;
        try {
            // An account that has never chosen a language leaves the device's.
            if (from.language() != null && from.language().length() > 0) {
                Prefs.setLanguage(from.language());
            }
            Prefs.setTheme(from.theme());
            Prefs.setUnits(from.units());
            Prefs.setNotifyRideUpdates(from.notifyRideUpdates());
            Prefs.setNotifyReceipts(from.notifyReceipts());
            Prefs.setNotifyPromotions(from.notifyPromotions());
            Prefs.setDriverGender(from.driverGender());
            Prefs.setQuietRide(from.quietRide());
            Prefs.setAccessibleVehicle(from.accessibleVehicle());
            Prefs.setPetFriendly(from.petFriendly());
            String method = from.defaultPaymentMethodId();
            if (method != null && method.length() > 0) {
                Prefs.setPaymentMethod(method);
            }
            Prefs.setTipPercent(from.defaultTipPercent());
            String sharedWith = from.shareTripsWith();
            Prefs.setShareTrips(sharedWith != null && sharedWith.length() > 0);
        } finally {
            applying = false;
        }
    }

    private static void apply(ProfileDto from) {
        if (from == null) {
            return;
        }
        profile = from;
        applying = true;
        try {
            Prefs.setEmergencyContactName(from.emergencyContactName());
            Prefs.setEmergencyContactPhone(from.emergencyContactPhone());
            Prefs.setHome(place(Prefs.home(), from.homeAddress()));
            Prefs.setWork(place(Prefs.work(), from.workAddress()));
        } finally {
            applying = false;
        }
    }

    /// The place for an address the account has. The server keeps the words
    /// and not the position, so the place this device already has is kept when
    /// it is the same one; another is left without a position, for the screen
    /// that goes there to look up.
    private static PlaceDto place(PlaceDto known, String address) {
        if (address == null || address.length() == 0) {
            return null;
        }
        if (known != null && address.equals(known.name())) {
            return known;
        }
        return new PlaceDto(address, "", 0d, 0d);
    }
}
