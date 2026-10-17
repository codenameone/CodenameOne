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
package com.codenameone.examples.savor.ui;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.savor.Prefs;
import com.codenameone.examples.savor.api.PlaceDto;
import com.codenameone.examples.savor.api.ProfileDto;
import com.codenameone.examples.savor.api.UserDto;
import com.codenameone.examples.savor.map.Locator;
import com.codenameone.examples.savor.net.Account;
import com.codenameone.examples.savor.net.Session;
import com.codenameone.examples.savor.customer.PlaceSearchForm;

/// Who the user is to the service: their name, who to tell if something goes
/// wrong, and the two places they go most.
public final class ProfileForm {
    private static final String[] GENDERS = {"unspecified", "female", "male", "nonbinary"};
    private static final String[] GENDER_NAMES = {"Rather not say", "Woman", "Man",
        "Non-binary"};

    private ProfileForm() {
    }

    /// @param saved where to go once the profile is saved: a screen that shows
    ///     the name is built again there
    public static void show(final Form previous, final Runnable saved) {
        final Form form = Ui.form("Profile", "Profile");
        Ui.back(form, previous);
        UserDto user = Session.user();
        ProfileDto profile = Account.profile();
        Container page = Ui.page();

        Ui.section(page, "About you");
        final TextField name = Ui.field(page, "Name", "name", "What couriers will call you");
        name.setText(profile == null ? user.displayName() : profile.name());
        final String[] gender = {profile == null || profile.gender() == null
                || profile.gender().length() == 0 ? GENDERS[0] : profile.gender()};
        page.add(Ui.label("Gender", "WlLabel"));
        // Two rows of two: four of these do not fit side by side in German.
        page.add(Ui.choice("gender", GENDER_NAMES, GENDERS, gender[0],
                value -> gender[0] = value, 2));
        page.add(Ui.text("Customers who ask for a woman at the wheel are offered to couriers "
                + "who say they are one.", "WlMuted"));

        Ui.section(page, "Emergency contact");
        final TextField contact = Ui.field(page, "Their name", "emergencyName", "");
        contact.setText(Prefs.emergencyContactName());
        final TextField number = Ui.field(page, "Their phone number", "emergencyPhone",
                "+1 555 0100", TextArea.PHONENUMBER);
        number.setText(Prefs.emergencyContactPhone());

        Ui.section(page, "Saved places");
        Container places = SettingsForm.group();
        places.add(place(form, previous, saved, FontImage.MATERIAL_HOME, "Home", "home",
                Prefs.home()));
        places.add(place(form, previous, saved, FontImage.MATERIAL_WORK, "Work", "work",
                Prefs.work()));
        page.add(places);

        final SpanLabel error = Ui.text("", "WlError");
        error.setName("error");
        final Button save = Ui.primary("Save", "submit", null);
        save.addActionListener(e -> {
            save.setEnabled(false);
            error.setText("");
            Prefs.setEmergencyContactName(contact.getText());
            Prefs.setEmergencyContactPhone(number.getText());
            Account.saveProfile(name.getText().trim(), gender[0], saved, (status, message) -> {
                save.setEnabled(true);
                error.setText(message);
                Ui.refresh(form);
            });
        });
        page.add(error).add(save);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    private static Container place(final Form form, final Form previous, final Runnable saved,
            char icon, String title, final String which, PlaceDto place) {
        return Ui.tap(Ui.row(icon, title, place == null ? Lang.tr("Not set") : place.name(),
                SettingsForm.chevron()), "place-" + which,
                e -> PlaceSearchForm.show(form, Locator.here(), picked -> {
                    if ("home".equals(which)) {
                        Prefs.setHome(picked);
                    } else {
                        Prefs.setWork(picked);
                    }
                    show(previous, saved);
                }));
    }
}
