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
package com.codenameone.examples.parley.net;

import com.codename1.io.Preferences;
import com.codename1.ui.CN;
import com.codenameone.examples.parley.Prefs;
import com.codenameone.examples.parley.api.PreferencesDto;
import com.codenameone.examples.parley.api.ProfileDto;

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
    private static final String UNSENT_PREFERENCES = "parley.account.unsent.preferences";
    private static final String UNSENT_PROFILE = "parley.account.unsent.profile";

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
        String owner = Session.owner();
        if (!isUnsent(UNSENT_PREFERENCES, owner)) {
            Preferences.delete(UNSENT_PREFERENCES);
        }
        if (!isUnsent(UNSENT_PROFILE, owner)) {
            Preferences.delete(UNSENT_PROFILE);
        }
        fetch(done);
    }

    private static void fetch(final Runnable done) {
        final String owner = Session.owner();
        // An answer from a session that has since ended still runs `done`.
        // Signing out and in while the account loads is rare, and the worst
        // of it is the home screen shown a moment before its settings, which
        // the new session's own load applies when it ends. The data itself is
        // only taken for the account signed in now, which is checked below.
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

    /// Saves the name, which only the profile screen changes.
    public static void saveProfile(final String name, final String about, final Runnable done,
            final Net.Failed failed) {
        if (!profileLoaded) {
            // The rest of the profile goes out with the name, and it has not
            // been seen yet; see send().
            fetch(() -> {
                if (profileLoaded) {
                    saveProfile(name, about, done, failed);
                } else {
                    failed.failed(0, "Could not reach the server. Check your connection.");
                }
            });
            return;
        }
        ProfileDto wanted = new ProfileDto(name, about);
        final String owner = Session.owner();
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
        if (applying || Session.user() == null) {
            return;
        }
        preferencesChanged = true;
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
        final String owner = Session.owner();
        if (preferencesChanged) {
            preferencesChanged = false;
            sent = preferences();
            // Marked before it goes, and cleared once the server has it: until
            // then the device's values are the account's newest.
            Preferences.set(UNSENT_PREFERENCES, owner);
            final int send = ++preferencesSends;
            Api.account().savePreferences(sent, Net.to(saved -> {
                if (send == preferencesSends) {
                    sentFor(UNSENT_PREFERENCES, owner);
                }
            }, (status, message) -> {
                if (refused(status)) {
                    // Refused: sent again, it would be refused again.
                    if (send == preferencesSends) {
                        sentFor(UNSENT_PREFERENCES, owner);
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
                    sentFor(UNSENT_PROFILE, owner);
                }
                // Signing out sends what is waiting and does not wait for the
                // answer. If somebody else has signed in by the time it comes,
                // this profile is not theirs: kept, its name would
                // go out with their next change and overwrite their own.
                if (signedInAs(owner)) {
                    profile = wanted;
                }
            }, (status, message) -> {
                if (refused(status)) {
                    if (send == profileSends) {
                        sentFor(UNSENT_PROFILE, owner);
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

    /// The server has what `owner` left unsent under `key`. The mark is one
    /// key for every account, so an answer that comes after somebody else
    /// signed in leaves theirs alone: it may be a change of their own that
    /// never went through, and cleared it would be lost to the server's copy.
    private static void sentFor(String key, String owner) {
        if (isUnsent(key, owner)) {
            Preferences.delete(key);
        }
    }

    /// Whether the account signed in now is `owner` ([Session#owner()]): an
    /// answer that comes back after the session it was asked in has ended is
    /// not acted on.
    private static boolean signedInAs(String owner) {
        return owner != null && owner.equals(Session.owner());
    }

    private static PreferencesDto preferences() {
        String language = Prefs.language();
        return new PreferencesDto(
                language.length() == 0 ? com.codenameone.examples.parley.ui.Lang.current()
                        : language,
                Prefs.theme(),
                Prefs.notifyMessages(),
                Prefs.notifyGroups(),
                Prefs.notifyCalls(),
                Prefs.readReceipts(),
                Prefs.typingIndicators(),
                Prefs.lastSeen(),
                Prefs.profilePhoto(),
                Prefs.disappearingSeconds(),
                Prefs.screenSecurity(),
                Prefs.appLock());
    }

    /// The profile to send: the one the server gave, or the name signed in
    /// with.
    private static ProfileDto described() {
        return profile != null ? profile
                : new ProfileDto(Session.user().displayName(), Session.user().about());
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
            Prefs.setNotifyMessages(from.notifyMessages());
            Prefs.setNotifyGroups(from.notifyGroups());
            Prefs.setNotifyCalls(from.notifyCalls());
            Prefs.setReadReceipts(from.readReceipts());
            Prefs.setTypingIndicators(from.typingIndicators());
            if (from.lastSeen() != null && from.lastSeen().length() > 0) {
                Prefs.setLastSeen(from.lastSeen());
            }
            if (from.profilePhoto() != null && from.profilePhoto().length() > 0) {
                Prefs.setProfilePhoto(from.profilePhoto());
            }
            Prefs.setDisappearingSeconds(from.disappearingSeconds());
            Prefs.setScreenSecurity(from.screenSecurity());
            Prefs.setAppLock(from.appLock());
            sent = from;
        } finally {
            applying = false;
        }
    }

    private static void apply(ProfileDto from) {
        if (from != null) {
            profile = from;
        }
    }
}
