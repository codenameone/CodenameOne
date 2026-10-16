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
package com.codenameone.examples.wayline.net;

import com.codename1.io.Preferences;
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
///
/// A change is not forgotten when its request fails, either. The device's
/// values stay marked as unsent, on the device, until the server has taken
/// them: they are sent again a little later, and an account load that finds
/// them unsent -- the app restarted before the network came back -- sends them
/// rather than overwriting them with what the server still holds.
public final class Account {
    /// How long after the last change the values are sent.
    private static final int SETTLE_MILLIS = 700;
    /// How long after a request failed it is sent again.
    private static final int RETRY_MILLIS = 30000;
    /// The keys under which each half is marked unsent; the value is the
    /// account it was not sent for.
    private static final String UNSENT_PREFERENCES = "wayline.account.unsent.preferences";
    private static final String UNSENT_PROFILE = "wayline.account.unsent.profile";

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
    /// How many requests of each half were sent. An answer clears the unsent
    /// mark only if no later request of its half went out after it.
    private static int preferencesSends;
    private static int profileSends;

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
        // What another account left unsent is not this one's to send.
        String owner = Session.user().username();
        if (!isUnsent(UNSENT_PREFERENCES, owner)) {
            Preferences.delete(UNSENT_PREFERENCES);
        }
        if (!isUnsent(UNSENT_PROFILE, owner)) {
            Preferences.delete(UNSENT_PROFILE);
        }
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
                    if (described != null && isUnsent(UNSENT_PROFILE, owner)) {
                        // The device has a change the server never took:
                        // the server's name and gender are kept, the rest is
                        // the device's, and is sent.
                        profile = described;
                        profileLoaded = true;
                        profileChanged = true;
                        schedule(SETTLE_MILLIS);
                    } else {
                        apply(described);
                        profileLoaded = described != null;
                    }
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
                if (preferences != null && isUnsent(UNSENT_PREFERENCES, owner)) {
                    // As for the profile: the device's are newer.
                    preferencesLoaded = true;
                    preferencesChanged = true;
                    schedule(SETTLE_MILLIS);
                } else {
                    apply(preferences);
                    preferencesLoaded = preferences != null;
                }
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
        schedule(SETTLE_MILLIS);
    }

    /// Sends what has changed `millis` from now, unless a send is already
    /// waiting.
    private static void schedule(int millis) {
        if (!waiting) {
            waiting = true;
            CN.setTimeout(millis, () -> CN.callSerially(() -> {
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
        final String owner = Session.user().username();
        if (preferencesChanged) {
            preferencesChanged = false;
            sent = preferences();
            // Marked before it goes, and cleared once the server has it: until
            // then the device's values are the account's newest.
            Preferences.set(UNSENT_PREFERENCES, owner);
            final int send = ++preferencesSends;
            Api.account().savePreferences(sent, Net.to(saved -> {
                if (send == preferencesSends) {
                    Preferences.delete(UNSENT_PREFERENCES);
                }
            }, (status, message) -> {
                if (refused(status)) {
                    // Refused: sent again, it would be refused again.
                    if (send == preferencesSends) {
                        Preferences.delete(UNSENT_PREFERENCES);
                    }
                } else if (signedInAs(owner)) {
                    preferencesChanged = true;
                    schedule(RETRY_MILLIS);
                }
            }));
        }
        if (profileChanged) {
            profileChanged = false;
            final ProfileDto wanted = described();
            Preferences.set(UNSENT_PROFILE, owner);
            final int send = ++profileSends;
            Api.account().saveProfile(wanted, Net.to(user -> {
                if (send == profileSends) {
                    Preferences.delete(UNSENT_PROFILE);
                }
                // Signing out sends what is waiting and does not wait for the
                // answer. If somebody else has signed in by the time it comes,
                // this profile is not theirs: kept, its name and gender would
                // go out with their next change and overwrite their own.
                if (signedInAs(owner)) {
                    profile = wanted;
                }
            }, (status, message) -> {
                if (refused(status)) {
                    if (send == profileSends) {
                        Preferences.delete(UNSENT_PROFILE);
                    }
                } else if (signedInAs(owner)) {
                    profileChanged = true;
                    schedule(RETRY_MILLIS);
                }
            }));
        }
    }

    /// Whether the server refused a request for what it carried, so that sent
    /// again it would be refused again. Anything else -- the server not
    /// reached (0), busy, failing, or the session needing a new token -- may
    /// succeed later, and the change is kept for then.
    private static boolean refused(int status) {
        return status >= 400 && status < 500 && status != 401 && status != 408
                && status != 429;
    }

    /// Whether `key` marks a change that `owner`'s account has not taken.
    private static boolean isUnsent(String key, String owner) {
        return owner != null && owner.equals(Preferences.get(key, ""));
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
