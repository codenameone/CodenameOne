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
package com.codenameone.examples.parley.account;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.parley.Text;
import com.codenameone.examples.parley.api.PreferencesDto;
import com.codenameone.examples.parley.contact.Contacts;
import com.codenameone.examples.parley.domain.Preference;
import com.codenameone.examples.parley.domain.Profile;
import com.codenameone.examples.parley.repository.PreferenceRepository;
import com.codenameone.examples.parley.repository.ProfileRepository;

import java.io.IOException;

/// How one person wants the app, and what they let other people see.
///
/// Kept on the server so they follow the account from one device to the next,
/// and because the server is what enforces the privacy ones: a last seen that
/// is hidden is never sent to anybody, rather than sent and politely not
/// shown. Read receipts and typing are the exception -- they travel inside
/// encrypted messages the server cannot open -- so the app honours those
/// itself. The language lives in the profile, where the admin sees it.
@Component
public class Preferences {
    private static final String[] LANGUAGES = {"", "en", "es", "fr", "de", "he"};
    private static final String[] THEMES = {"system", "light", "dark"};
    private static final String[] AUDIENCES = {Contacts.EVERYONE, Contacts.CONTACTS,
        Contacts.NOBODY};
    private static final int[] TIMERS = {0, 30, 300, 3600, 86400, 604800};

    private final ProfileRepository profiles;
    private final PreferenceRepository preferences;

    public Preferences(ProfileRepository profiles, PreferenceRepository preferences) {
        this.profiles = profiles;
        this.preferences = preferences;
    }

    @Transactional(readOnly = true)
    public PreferencesDto get(String username) throws IOException {
        Profile profile = profiles.findById(username);
        if (profile == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        Preference row = preferences.findById(username);
        String language = profile.getLanguage();
        if (row == null) {
            // What someone who never opened the settings gets.
            return new PreferencesDto(language, THEMES[0], true, true, true, true, true,
                    Contacts.EVERYONE, Contacts.EVERYONE, 0, false, false);
        }
        return new PreferencesDto(language, row.getTheme(), row.isNotifyMessages(),
                row.isNotifyGroups(), row.isNotifyCalls(), row.isReadReceipts(),
                row.isTypingIndicators(), row.getLastSeen(), row.getProfilePhoto(),
                row.getDisappearingSeconds(), row.isScreenSecurity(), row.isAppLock());
    }

    /// Saves all of them. Each is checked against the values it can take: these
    /// are words the server later acts on, not text to show back. One
    /// transaction, so a value that is refused leaves none of the others saved.
    @Transactional
    public PreferencesDto save(String username, PreferencesDto wanted) throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        // First, before anything is read: two saves sent together for an
        // account with no row yet would each find none and each insert one.
        profiles.hold(username);
        String theme = Text.oneOf(wanted.theme(), THEMES, THEMES[0], "the theme");
        String language = wanted.language() == null || wanted.language().length() == 0 ? ""
                : Text.oneOf(wanted.language(), LANGUAGES, "", "the language");
        String lastSeen = Text.oneOf(wanted.lastSeen(), AUDIENCES, Contacts.EVERYONE,
                "who sees your last seen");
        String photo = Text.oneOf(wanted.profilePhoto(), AUDIENCES, Contacts.EVERYONE,
                "who sees your photo");
        boolean known = false;
        for (int iter = 0; iter < TIMERS.length; iter++) {
            known |= TIMERS[iter] == wanted.disappearingSeconds();
        }
        if (!known) {
            throw new ResponseStatusException(400, "That is not a disappearing-message timer");
        }
        Profile profile = profiles.findById(username);
        if (profile == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        profile.setLanguage(language);
        Preference row = preferences.findById(username);
        boolean unsaved = row == null;
        if (unsaved) {
            row = new Preference();
            row.setUsername(username);
        }
        row.setTheme(theme);
        row.setNotifyMessages(wanted.notifyMessages());
        row.setNotifyGroups(wanted.notifyGroups());
        row.setNotifyCalls(wanted.notifyCalls());
        row.setReadReceipts(wanted.readReceipts());
        row.setTypingIndicators(wanted.typingIndicators());
        row.setLastSeen(lastSeen);
        row.setProfilePhoto(photo);
        row.setDisappearingSeconds(wanted.disappearingSeconds());
        row.setScreenSecurity(wanted.screenSecurity());
        row.setAppLock(wanted.appLock());
        if (unsaved) {
            preferences.insert(row);
        }
        return get(username);
    }
}
