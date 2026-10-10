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
package com.codenameone.examples.wayline.account;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.wayline.Text;
import com.codenameone.examples.wayline.api.PreferencesDto;
import com.codenameone.examples.wayline.domain.Preference;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.pay.Payments;

import java.io.IOException;

/// How one person wants the app and their rides: language, theme, units, what
/// they are told about, and what a ride is asked for with unless they say
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
    private static final String[] DRIVER_GENDERS = {"any", "women"};

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
        Profile profile = profiles.find(username);
        if (profile == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        Preference row = preferences.find(username);
        PreferencesDto dto = new PreferencesDto();
        dto.language = profile.language;
        dto.defaultPaymentMethodId = payments.defaultMethod(username);
        if (row == null) {
            // What someone who never opened the settings gets.
            dto.theme = THEMES[0];
            dto.units = UNITS[0];
            dto.notifyRideUpdates = true;
            dto.notifyReceipts = true;
            dto.driverGender = DRIVER_GENDERS[0];
            dto.shareTripsWith = "";
            return dto;
        }
        dto.theme = row.theme;
        dto.units = row.units;
        dto.notifyRideUpdates = row.notifyRides;
        dto.notifyReceipts = row.notifyReceipts;
        dto.notifyPromotions = row.notifyPromotions;
        dto.driverGender = row.driverGender;
        dto.quietRide = row.quietRide;
        dto.accessibleVehicle = row.wheelchair;
        dto.petFriendly = row.petFriendly;
        dto.defaultTipPercent = row.defaultTip;
        dto.shareTripsWith = row.shareTripsWith;
        return dto;
    }

    /// Saves all of them. Each is checked against the values it can take: these
    /// are words the server later acts on, not text to show back. One
    /// transaction, so a value that is refused leaves none of the others saved.
    @Transactional
    public PreferencesDto save(String username, PreferencesDto wanted) throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        int tip = wanted.defaultTipPercent;
        if (tip != 0 && tip != 10 && tip != 15 && tip != 20) {
            throw new ResponseStatusException(400, "A default tip is 0, 10, 15 or 20 percent");
        }
        String share = wanted.shareTripsWith == null ? "" : wanted.shareTripsWith.trim();
        String theme = Text.oneOf(wanted.theme, THEMES, THEMES[0], "the theme");
        String units = Text.oneOf(wanted.units, UNITS, UNITS[0], "the units");
        String driverGender = Text.oneOf(wanted.driverGender, DRIVER_GENDERS, DRIVER_GENDERS[0],
                "the driver");
        String shareWith = share.length() == 0 ? "" : Accounts.phone(share);
        String language = Text.oneOf(wanted.language, LANGUAGES, LANGUAGES[0], "the language");
        String method = wanted.defaultPaymentMethodId == null ? ""
                : wanted.defaultPaymentMethodId.trim();
        if (method.length() > 0 && !method.equals(payments.defaultMethod(username))) {
            // Answers 404 for a card that is not this user's, before anything
            // else is saved.
            payments.makeDefault(username, method);
        }
        // Read after the card was marked, not before: marking one may be a
        // statement over every card of the account, and what was read before
        // such a statement is no longer the session's to write.
        Profile profile = profiles.find(username);
        if (profile == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        profile.language = language;
        Preference row = preferences.find(username);
        boolean unsaved = row == null;
        if (unsaved) {
            row = new Preference();
            row.username = username;
        }
        row.theme = theme;
        row.units = units;
        row.notifyRides = wanted.notifyRideUpdates;
        row.notifyReceipts = wanted.notifyReceipts;
        row.notifyPromotions = wanted.notifyPromotions;
        row.driverGender = driverGender;
        row.quietRide = wanted.quietRide;
        row.wheelchair = wanted.accessibleVehicle;
        row.petFriendly = wanted.petFriendly;
        row.defaultTip = tip;
        row.shareTripsWith = shareWith;
        if (unsaved) {
            preferences.add(row);
        }
        return get(username);
    }
}
