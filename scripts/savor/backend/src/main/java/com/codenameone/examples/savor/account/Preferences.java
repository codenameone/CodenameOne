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
package com.codenameone.examples.savor.account;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.savor.Text;
import com.codenameone.examples.savor.api.PreferencesDto;
import com.codenameone.examples.savor.domain.Preference;
import com.codenameone.examples.savor.domain.Profile;
import com.codenameone.examples.savor.pay.Payments;
import com.codenameone.examples.savor.repository.PreferenceRepository;
import com.codenameone.examples.savor.repository.ProfileRepository;

import java.io.IOException;

/// How one person wants the app and their deliveries: language, theme, units, what
/// they are told about, and what a delivery is asked for with unless they say
/// otherwise.
///
/// Kept on the server so that they follow the account from one phone to the
/// next. Two of them live elsewhere, because something else reads them: the
/// language is in the profile, where the admin sees it, and the default way to
/// pay is the mark on the saved card itself, so that removing a card cannot
/// leave a preference pointing at it.
@Component
public class Preferences {
    private static final String[] LANGUAGES = {"en", "es", "fr", "de", "he"};
    private static final String[] THEMES = {"system", "light", "dark"};
    private static final String[] UNITS = {"km", "mi"};

    private final ProfileRepository profiles;
    private final PreferenceRepository preferences;
    private final Payments payments;

    public Preferences(ProfileRepository profiles, PreferenceRepository preferences,
            Payments payments) {
        this.profiles = profiles;
        this.preferences = preferences;
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public PreferencesDto get(String username) throws IOException {
        Profile profile = profiles.findById(username);
        if (profile == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        Preference row = preferences.findById(username);
        String language = profile.getLanguage();
        String method = payments.defaultMethod(username);
        if (row == null) {
            // What someone who never opened the settings gets.
            return new PreferencesDto(language, THEMES[0], UNITS[0], true, true, false, method,
                    0);
        }
        return new PreferencesDto(language, row.getTheme(), row.getUnits(), row.isNotifyDeliveries(),
                row.isNotifyReceipts(), row.isNotifyPromotions(), method, row.getDefaultTip());
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
        // account with no row of settings yet would each find none and each
        // insert one, and the second insert fails on the key. The profile is
        // there from registration, so held, one save waits and finds the
        // other's row.
        profiles.hold(username);
        int tip = wanted.defaultTipPercent();
        if (tip != 0 && tip != 10 && tip != 15 && tip != 20) {
            throw new ResponseStatusException(400, "A default tip is 0, 10, 15 or 20 percent");
        }
        String theme = Text.oneOf(wanted.theme(), THEMES, THEMES[0], "the theme");
        String units = Text.oneOf(wanted.units(), UNITS, UNITS[0], "the units");
        String language = Text.oneOf(wanted.language(), LANGUAGES, LANGUAGES[0], "the language");
        String method = wanted.defaultPaymentMethodId() == null ? ""
                : wanted.defaultPaymentMethodId().trim();
        if (method.length() > 0 && !method.equals(payments.defaultMethod(username))) {
            // Answers 404 for a card that is not this user's, before anything
            // else is saved.
            payments.makeDefault(username, method);
        }
        // Read after the card was marked, not before: marking one may be a
        // statement over every card of the account, and what was read before
        // such a statement is no longer the session's to write.
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
        row.setUnits(units);
        row.setNotifyDeliveries(wanted.notifyDeliveryUpdates());
        row.setNotifyReceipts(wanted.notifyReceipts());
        row.setNotifyPromotions(wanted.notifyPromotions());
        row.setDefaultTip(tip);
        if (unsaved) {
            preferences.insert(row);
        }
        return get(username);
    }
}
