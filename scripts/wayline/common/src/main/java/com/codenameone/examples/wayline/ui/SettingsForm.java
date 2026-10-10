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
package com.codenameone.examples.wayline.ui;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.wayline.Prefs;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.PasswordChangeDto;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.net.Session;
import com.codenameone.examples.wayline.pay.WalletForm;

/// Settings: the account, and everything about the app a person may want
/// their own way.
///
/// Every control here reads and writes [Prefs] and nothing else, so this
/// screen does not know where a setting is kept: `Prefs` keeps it on the
/// device, and `net.Account` hears of the change and keeps it with the account.
/// The three under Appearance change what is already on screen -- the
/// language, light or dark, miles or kilometres -- and take effect at once: the
/// look and the words are applied, and the screens are built again in them,
/// this one included.
public final class SettingsForm {
    private SettingsForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Settings", "Settings");
        Ui.back(form, previous);
        final UserDto user = Session.user();
        Container page = Ui.page();

        page.add(Ui.person(user.displayName, user.username, null));

        Ui.section(page, "Account");
        Container account = group();
        account.add(Ui.tap(Ui.row(FontImage.MATERIAL_PERSON, "Profile",
                "Name, emergency contact, home and work", chevron()), "profile",
                e -> ProfileForm.show(form, () -> show(previous))));
        account.add(Ui.row(FontImage.MATERIAL_PHONE, user.phone,
                user.phoneVerified ? null : "Needed before your first ride",
                user.phoneVerified ? Ui.badge("Verified", "WlBadgeGood")
                        : Ui.link("Verify", "verify",
                                e -> VerifyPhoneForm.show(() -> show(previous)))));
        if (user.driver) {
            account.add(Ui.row(FontImage.MATERIAL_DIRECTIONS_CAR, user.vehicle, user.plate,
                    null));
        }
        account.add(Ui.tap(Ui.row(FontImage.MATERIAL_LOCK, "Password",
                "Change the password you sign in with", chevron()), "password",
                e -> password(form)));
        page.add(account);

        Ui.section(page, "Ride preferences");
        Container ride = group();
        ride.add(Ui.label("Driver", "WlLabel"));
        ride.add(Ui.choice("driverGender", new String[] {"Any driver", "Women only"},
                new String[] {Prefs.GENDER_ANY, Prefs.GENDER_WOMEN}, Prefs.driverGender(),
                Prefs::setDriverGender));
        ride.add(Ui.row(FontImage.MATERIAL_VOLUME_OFF, "Quiet ride",
                "Ask the driver to keep conversation short",
                Ui.toggle("quietRide", Prefs.quietRide(), Prefs::setQuietRide)));
        ride.add(Ui.row(FontImage.MATERIAL_ACCESSIBLE, "Accessible vehicle",
                "Only cars that take a wheelchair",
                Ui.toggle("accessibleVehicle", Prefs.accessibleVehicle(),
                        Prefs::setAccessibleVehicle)));
        ride.add(Ui.row(FontImage.MATERIAL_PETS, "Pet friendly",
                "Tell the driver an animal is coming",
                Ui.toggle("petFriendly", Prefs.petFriendly(), Prefs::setPetFriendly)));
        page.add(ride);

        Ui.section(page, "Payment");
        Container pay = group();
        pay.add(Ui.tap(Ui.row(FontImage.MATERIAL_ACCOUNT_BALANCE_WALLET, "Wallet",
                "Your cards, and which of them pays by default", chevron()), "wallet",
                e -> WalletForm.show(form)));
        pay.add(Ui.label("Tip offered first", "WlLabel"));
        pay.add(Ui.choice("tip", new String[] {"None", "10%", "15%", "20%"},
                new String[] {"0", "10", "15", "20"}, String.valueOf(Prefs.tipPercent()),
                value -> Prefs.setTipPercent(Integer.parseInt(value))));
        page.add(pay);

        Ui.section(page, "Notifications");
        Container notify = group();
        notify.add(Ui.row(FontImage.MATERIAL_NOTIFICATIONS, "Ride updates",
                "Driver found, arriving, at the door",
                Ui.toggle("notifyRideUpdates", Prefs.notifyRideUpdates(),
                        Prefs::setNotifyRideUpdates)));
        notify.add(Ui.row(FontImage.MATERIAL_RECEIPT, "Receipts", "After every ride",
                Ui.toggle("notifyReceipts", Prefs.notifyReceipts(), Prefs::setNotifyReceipts)));
        notify.add(Ui.row(FontImage.MATERIAL_LOCAL_OFFER, "Offers", "Discounts and news",
                Ui.toggle("notifyPromotions", Prefs.notifyPromotions(),
                        Prefs::setNotifyPromotions)));
        page.add(notify);

        Ui.section(page, "Appearance");
        Container look = group();
        look.add(Ui.label("Language", "WlLabel"));
        String speaking = Lang.current();
        for (int iter = 0; iter < Lang.CODES.length; iter++) {
            final String code = Lang.CODES[iter];
            Label tick = new Label("", "WlRowIcon");
            if (code.equals(speaking)) {
                Ui.icon(tick, FontImage.MATERIAL_CHECK, 3.4f);
            }
            Container row = new Container(new BorderLayout());
            row.setUIID("WlRow");
            // A language is called what its speakers call it, whatever
            // language the rest of the screen is in.
            row.add(BorderLayout.CENTER, Ui.plain(Lang.name(code), "WlRowTitle"));
            row.add(BorderLayout.EAST, tick);
            look.add(Ui.tap(row, "language-" + code, e -> {
                boolean other = !code.equals(Lang.current());
                Prefs.setLanguage(code);
                if (other) {
                    Telemetry.languageChanged(code);
                }
                again(previous);
            }));
        }
        look.add(Ui.label("Theme", "WlLabel"));
        look.add(Ui.choice("theme", new String[] {"System", "Light", "Dark"},
                new String[] {Prefs.THEME_SYSTEM, Prefs.THEME_LIGHT, Prefs.THEME_DARK},
                Prefs.theme(), value -> {
                    if (!value.equals(Prefs.theme())) {
                        Prefs.setTheme(value);
                        Telemetry.themeChanged(value);
                    }
                    again(previous);
                }));
        look.add(Ui.label("Distances", "WlLabel"));
        look.add(Ui.choice("units", new String[] {"Kilometres", "Miles"},
                new String[] {Prefs.UNITS_KM, Prefs.UNITS_MILES}, Prefs.units(), value -> {
                    Prefs.setUnits(value);
                    again(previous);
                }));
        page.add(look);

        Ui.section(page, "Privacy and safety");
        Container safe = group();
        String contact = Prefs.emergencyContactName();
        safe.add(Ui.row(FontImage.MATERIAL_SHARE_LOCATION, "Share my trips",
                contact.length() == 0
                        ? Lang.tr("Name an emergency contact in your profile first")
                        : Lang.tr("Saves {0}'s number with your account as your trip contact. "
                                + "No message is sent.", contact),
                Ui.toggle("shareTrips", Prefs.shareTrips(), Prefs::setShareTrips)));
        // Kept on this device and not with the account: it is this
        // installation that reports or does not.
        safe.add(Ui.row(FontImage.MATERIAL_INSIGHTS, "Usage statistics",
                "Share which screens and features are used. Never your name, places or "
                + "payment details.",
                Ui.toggle("usageStatistics", Telemetry.allowed(), Telemetry::allow)));
        page.add(safe);

        Ui.section(page, "About");
        Container about = group();
        about.add(Ui.row(FontImage.MATERIAL_INFO, "Wayline",
                "A sample ride service built with Codename One", null));
        about.add(Ui.tap(Ui.row(FontImage.MATERIAL_DESCRIPTION, "Terms of service", null,
                chevron()), "terms", e -> legal(form, "Terms of service",
                        "Wayline is a sample. No real rides are booked, no real drivers are "
                        + "sent and no money changes hands.", null)));
        about.add(Ui.tap(Ui.row(FontImage.MATERIAL_POLICY, "Privacy policy", null, chevron()),
                "privacy", e -> legal(form, "Privacy policy",
                        "Your position is sent to the server you signed in to while you "
                        + "ride or drive, and to nobody else. Your settings are kept with "
                        + "your account on that server.",
                        "With usage statistics turned on, the app also counts which screens "
                        + "are opened and which features are used, and reports that to "
                        + "Codename One's analytics service under a made-up id for this "
                        + "device. Your name, e-mail, phone number, places and card details "
                        + "are never part of it, and turning it off in Settings stops it.")));
        about.add(Ui.tap(Ui.row(FontImage.MATERIAL_DNS, "Server address", null, chevron()),
                "server", e -> ServerForm.show(form)));
        page.add(about);

        page.add(Ui.secondary("Sign out", "signOut", e -> Nav.signOut()));
        page.add(Ui.link("Delete my account", "deleteAccount", e -> delete(form)));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    /// Applies the look and the language, and shows everything again in them.
    ///
    /// A screen that is already built keeps the words and colours it was built
    /// with, so the mode's first screen is built anew to go back to, and this
    /// one over it.
    private static void again(Form previous) {
        Look.apply();
        Lang.apply();
        Form home = Nav.build();
        show(home == null ? previous : home);
    }

    static Container group() {
        Container group = new Container(BoxLayout.y());
        group.setUIID("WlGroup");
        return group;
    }

    /// The mark at the end of a row that leads somewhere.
    static Label chevron() {
        Label chevron = new Label("", "WlRowIcon");
        Ui.icon(chevron, FontImage.MATERIAL_CHEVRON_RIGHT, 3.4f);
        return chevron;
    }

    /// Changing the password. The server wants the current one as well as the
    /// new: being signed in is what a borrowed phone is, too.
    private static void password(final Form previous) {
        final Form form = Ui.form("Password", "Password");
        Ui.back(form, previous);
        Container page = Ui.page();
        final TextField current = Ui.field(page, "Current password", "current", "",
                TextArea.PASSWORD);
        final TextField replacement = Ui.field(page, "New password", "replacement",
                "At least 8 characters", TextArea.PASSWORD);
        final TextField again = Ui.field(page, "New password, again", "again", "",
                TextArea.PASSWORD);
        final SpanLabel error = Ui.text("", "WlError");
        error.setName("error");
        final Button save = Ui.primary("Change password", "submit", null);
        save.addActionListener(e -> {
            if (!replacement.getText().equals(again.getText())) {
                error.setText(Lang.tr("The two new passwords are not the same."));
                Ui.refresh(form);
                return;
            }
            PasswordChangeDto change = new PasswordChangeDto();
            change.current = current.getText();
            change.replacement = replacement.getText();
            save.setEnabled(false);
            error.setText("");
            Api.account().changePassword(change, Net.to(user -> {
                Ui.say("Your password was changed");
                previous.showBack();
            }, (status, message) -> {
                save.setEnabled(true);
                error.setText(message);
                Ui.refresh(form);
            }));
        });
        page.add(error).add(save);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    /// Closing the account, which cannot be taken back, so it is its own
    /// screen that says what goes and has one red button.
    private static void delete(final Form previous) {
        final Form form = Ui.form("Delete account", "DeleteAccount");
        Ui.back(form, previous);
        Container page = Ui.page();
        page.add(Ui.banner(FontImage.MATERIAL_DELETE_FOREVER, "Delete your account?",
                "Your sign-in, your profile, your saved cards and your settings are removed "
                + "for good. Rides you took stay in the service's records without your name.",
                "WlBannerIconBad"));
        final SpanLabel error = Ui.text("", "WlError");
        page.add(error);
        final Button go = Ui.danger("Delete my account", "confirmDelete", null);
        go.addActionListener(e -> {
            go.setEnabled(false);
            Api.account().deleteAccount(Net.to(gone -> Nav.deleted(), (status, message) -> {
                go.setEnabled(true);
                error.setText(message);
                Ui.refresh(form);
            }));
        });
        page.add(go);
        page.add(Ui.link("Keep my account", "keep", e -> previous.showBack()));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    private static void legal(Form previous, String title, String text, String more) {
        Form form = Ui.form(title, "Legal");
        Ui.back(form, previous);
        Container page = Ui.page();
        page.add(Ui.text(text, "WlText"));
        if (more != null) {
            page.add(Ui.text(more, "WlText"));
        }
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
