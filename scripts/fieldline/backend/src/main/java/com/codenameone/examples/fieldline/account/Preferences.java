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
package com.codenameone.examples.fieldline.account;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.appkit.server.Text;
import com.codenameone.examples.fieldline.api.PreferencesDto;
import com.codenameone.examples.fieldline.domain.Preference;
import com.codenameone.examples.fieldline.domain.Profile;
import com.codenameone.examples.fieldline.repository.PreferenceRepository;
import com.codenameone.examples.fieldline.repository.ProfileRepository;

import java.io.IOException;

/// How one person wants the app: language, theme, what they are told about,
/// whether their jobs are copied into the phone's calendar, and whether the
/// assistant looks at their photos.
///
/// Kept on the server so that they follow the account from one phone to the
/// next. The language lives in the profile, where a dispatcher sees it.
@Component
public class Preferences {
    private static final String[] LANGUAGES = {"en", "es", "fr", "de", "he"};
    private static final String[] THEMES = {"system", "light", "dark"};

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
        if (row == null) {
            // What someone who never opened the settings gets.
            return new PreferencesDto(profile.getLanguage(), THEMES[0], true, true, false, true);
        }
        return new PreferencesDto(profile.getLanguage(), row.getTheme(), row.isNotifyAssignments(),
                row.isNotifyScheduleChanges(), row.isCalendarSync(), row.isAiAssist());
    }

    /// Saves all of them. The theme and language are checked against the
    /// values they can take: they are words the server and app act on, not
    /// text to show back.
    @Transactional
    public PreferencesDto save(String username, PreferencesDto wanted) throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        // First, before anything is read: two saves sent together for an
        // account with no row yet would each find none and each insert one.
        profiles.hold(username);
        String theme = Text.oneOf(wanted.theme(), THEMES, THEMES[0], "the theme");
        String language = Text.oneOf(wanted.language(), LANGUAGES, LANGUAGES[0], "the language");
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
        row.setNotifyAssignments(wanted.notifyAssignments());
        row.setNotifyScheduleChanges(wanted.notifyScheduleChanges());
        row.setCalendarSync(wanted.calendarSync());
        row.setAiAssist(wanted.aiAssist());
        if (unsaved) {
            preferences.insert(row);
        }
        return get(username);
    }
}
