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
package com.codenameone.examples.reel.viewer;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.reel.Prefs;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.api.HouseholdProfileDto;
import com.codenameone.examples.reel.api.PinDto;
import com.codenameone.examples.reel.api.ProfileEditDto;
import com.codenameone.examples.reel.net.Account;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Device;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.ui.Ui;
import java.util.List;

/// "Who is watching?": the household's profiles, a PIN for a protected one,
/// and adding or editing profiles (a kids profile, a rating limit, a PIN).
///
/// The PIN is checked by the server, which answers a key; that key goes with
/// every request made as the profile ([Device]), and the server refuses a
/// protected profile's requests without it. Edits need the key of a grown-up
/// profile once one has a PIN.
public final class ProfilesForm {
    private ProfilesForm() {
    }

    /// Whether a profile has to be picked before the home screen: more than
    /// one profile and none picked, or the picked one is protected and not
    /// unlocked since the app started.
    public static boolean needed() {
        List<HouseholdProfileDto> all = Account.profiles();
        if (all.isEmpty() || !Account.enabled(Features.PROFILES) && all.size() == 1 && !all.get(0).pinProtected()) {
            return false;
        }
        HouseholdProfileDto picked = Account.profile();
        if (picked == null) {
            return all.size() > 1 || all.get(0).pinProtected();
        }
        return picked.pinProtected() && Device.profileKey().length() == 0;
    }

    public static void show(final Runnable picked) {
        final Form form = Ui.form("Who is watching?", "Profiles");
        form.getToolbar().hideToolbar();
        final Container page = Ui.page();
        page.add(Ui.label("Who is watching?", "SvTitle"));
        final Container grid = new Container(new GridLayout(2));
        page.add(grid);
        form.add(BorderLayout.CENTER, page);
        fill(form, grid, picked);
        if (Account.enabled(Features.PROFILES)) {
            page.add(Ui.secondary("Add a profile", "addProfile", e -> edit(form, null, () -> show(picked))));
        }
        form.show();
    }

    private static void fill(final Form form, Container grid, final Runnable picked) {
        List<HouseholdProfileDto> all = Account.profiles();
        for (int iter = 0; iter < all.size(); iter++) {
            final HouseholdProfileDto p = all.get(iter);
            Container tile = new Container(BoxLayout.y());
            tile.setUIID("RlProfileTile");
            tile.add(Ui.avatar(p.name()));
            tile.add(Ui.label(p.name(), "RlProfileName"));
            if (p.kids()) {
                tile.add(Ui.badge("Kids", "SvBadgeGood"));
            } else if (p.pinProtected()) {
                tile.add(Ui.badge("PIN", "SvBadge"));
            }
            Button tap = new Button();
            tap.setUIID("Container");
            tap.setName("profile-" + iter);
            tap.addActionListener(e -> pick(form, p, picked));
            tile.add(tap);
            tile.setLeadComponent(tap);
            grid.add(tile);
        }
    }

    private static void pick(final Form form, final HouseholdProfileDto p, final Runnable picked) {
        if (!p.pinProtected()) {
            unlock(p, null, picked, null);
            return;
        }
        final Form pinForm = Ui.form(p.name(), "Pin");
        Ui.back(pinForm, form);
        Container page = Ui.page();
        page.add(Ui.label("Enter the profile's PIN", "SvLabel"));
        final TextField pin = Ui.field(page, "PIN", "pin", "4 digits", TextArea.NUMERIC | TextArea.PASSWORD);
        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        page.add(error);
        page.add(Ui.primary("Continue", "submit", e -> unlock(p, pin.getText(), picked, error)));
        pinForm.add(BorderLayout.CENTER, page);
        pinForm.show();
    }

    private static void unlock(final HouseholdProfileDto p, String pin, final Runnable picked, final SpanLabel error) {
        Api.account().unlockProfile(p.id(), new PinDto(pin), Net.to(unlocked -> {
            Device.setProfileKey(unlocked.key());
            Prefs.setProfile(p.id());
            picked.run();
        }, (status, message) -> {
            if (error != null) {
                error.setText(message);
                error.getParent().revalidate();
            } else {
                Ui.fail(status, message);
            }
        }));
    }

    /// Adds (`p` null) or edits a profile.
    public static void edit(final Form previous, final HouseholdProfileDto p, final Runnable saved) {
        final Form form = Ui.form(p == null ? "New profile" : p.name(), "EditProfile");
        Ui.back(form, previous);
        Container page = Ui.page();
        final TextField name = Ui.field(page, "Name", "name", "Who watches here");
        final boolean[] kids = {p != null && p.kids()};
        final String[] rating = {p == null ? "NC-17" : p.maxRating()};
        if (p != null) {
            name.setText(p.name());
        }
        page.add(Ui.row(FontImage.MATERIAL_CHILD_CARE, "Kids profile", "Only titles rated G and PG",
                Ui.toggle("kids", kids[0], on -> kids[0] = on)));
        page.add(Ui.label("Highest rating", "SvLabel"));
        page.add(Ui.choice("maxRating", new String[] {"G", "PG", "PG-13", "R", "NC-17"},
                new String[] {"G", "PG", "PG-13", "R", "NC-17"}, rating[0], value -> rating[0] = value));
        final TextField pin = Ui.field(page, "PIN", "pin", p != null && p.pinProtected()
                ? "Leave empty to keep the PIN" : "Optional, 4 digits", TextArea.NUMERIC | TextArea.PASSWORD);
        final boolean[] clear = {false};
        if (p != null && p.pinProtected()) {
            page.add(Ui.row(FontImage.MATERIAL_LOCK_OPEN, "Remove the PIN", null,
                    Ui.toggle("clearPin", false, on -> clear[0] = on)));
        }
        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        page.add(error);
        page.add(Ui.primary("Save", "submit", e -> {
            ProfileEditDto edit = new ProfileEditDto(name.getText().trim(), null, kids[0], rating[0],
                    pin.getText(), clear[0], null);
            Net.Failed failed = (status, message) -> {
                error.setText(message);
                form.revalidate();
            };
            Runnable reload = () -> Api.account().profiles(Net.to(list -> {
                Account.setProfiles(list);
                saved.run();
            }, failed));
            if (p == null) {
                Api.account().createProfile(edit, Net.to(created -> reload.run(), failed));
            } else {
                Api.account().updateHouseholdProfile(p.id(), edit, Net.to(updated -> reload.run(), failed));
            }
        }));
        if (p != null && Account.profiles().size() > 1) {
            page.add(Ui.link("Delete this profile", "deleteProfile", e -> Api.account().deleteProfile(p.id(),
                    Net.to(gone -> Api.account().profiles(Net.to(list -> {
                        Account.setProfiles(list);
                        saved.run();
                    }, Ui::fail)), Ui::fail))));
        }
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
