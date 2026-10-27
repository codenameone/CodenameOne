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
package com.codenameone.examples.reel.ui;

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
import com.codenameone.examples.reel.AppConfig;
import com.codenameone.examples.reel.Prefs;
import com.codenameone.examples.reel.Telemetry;
import com.codenameone.examples.reel.api.AccountClosureDto;
import com.codenameone.examples.reel.api.PasswordChangeDto;
import com.codenameone.examples.reel.api.AccountDto;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.net.Session;
import com.codenameone.examples.reel.pay.WalletForm;

/// Settings: the account, and everything about the app a person may want
/// their own way.
///
/// Every control here reads and writes [Prefs] and nothing else, so this
/// screen does not know where a setting is kept: `Prefs` keeps it on the
/// device, and `net.Account` hears of the change and keeps it with the account.
/// The two under Appearance change what is already on screen -- the
/// language, light or dark -- and take effect at once: the
/// look and the words are applied, and the screens are built again in them,
/// this one included.
public final class SettingsForm {
    private SettingsForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Settings", "Settings");
        Ui.back(form, previous);
        final AccountDto user = Session.user();
        Container page = Ui.page();

        page.add(Ui.person(user.name(), user.email(), null));

        Ui.section(page, "Account");
        Container account = group();
        account.add(Ui.tap(Ui.row(FontImage.MATERIAL_PERSON, "Account",
                "Your name, devices and what is playing", chevron()), "profile",
                e -> ProfileForm.show(form, () -> show(previous))));
        account.add(Ui.tap(Ui.row(FontImage.MATERIAL_WORKSPACE_PREMIUM, "Plan",
                user.planName() == null || user.planName().length() == 0 ? "No plan"
                        : user.planName() + Ui.DOT + user.subscriptionStatus(), chevron()), "plan",
                e -> com.codenameone.examples.reel.viewer.PlansForm.show(form)));
        account.add(Ui.tap(Ui.row(FontImage.MATERIAL_RECEIPT_LONG, "Billing history",
                "Invoices for plans and rentals", chevron()), "invoices",
                e -> com.codenameone.examples.reel.viewer.PlansForm.invoices(form)));
        account.add(Ui.tap(Ui.row(FontImage.MATERIAL_LOCK, "Password",
                "Change the password you sign in with", chevron()), "password",
                e -> password(form)));
        account.add(Ui.tap(Ui.row(FontImage.MATERIAL_ACCOUNT_BALANCE_WALLET, "Wallet",
                "Your cards, and which of them pays by default", chevron()), "wallet",
                e -> WalletForm.show(form)));
        page.add(account);

        Ui.section(page, "Playback");
        Container play = group();
        play.add(Ui.row(FontImage.MATERIAL_SKIP_NEXT, "Autoplay next episode", null,
                Ui.toggle("autoplay", Prefs.autoplay(), Prefs::setAutoplay)));
        play.add(Ui.row(FontImage.MATERIAL_DATA_SAVER_ON, "Data saver", "Stream at 480p or lower",
                Ui.toggle("dataSaver", Prefs.dataSaver(), Prefs::setDataSaver)));
        play.add(Ui.label("Streaming quality", "SvLabel"));
        play.add(Ui.choice("quality", new String[] {Lang.tr("Auto"), "1080p", "720p", "480p"},
                new String[] {"", "1080", "720", "480"}, Prefs.quality(), Prefs::setQuality));
        page.add(play);

        Ui.section(page, "Subtitles");
        Container subs = group();
        subs.add(Ui.label("Show subtitles in", "SvLabel"));
        subs.add(Ui.choice("subtitleLanguage", new String[] {Lang.tr("Off"), "English", "Espa\u00f1ol",
                "Fran\u00e7ais", "Deutsch"}, new String[] {"", "en", "es", "fr", "de"}, Prefs.subtitles(),
                Prefs::setSubtitles, 3));
        subs.add(Ui.label("Size", "SvLabel"));
        subs.add(Ui.choice("captionSize", new String[] {Lang.tr("Small"), Lang.tr("Medium"), Lang.tr("Large")},
                new String[] {"small", "medium", "large"}, Prefs.captionSize(), Prefs::setCaptionSize));
        Label sample = new Label(Lang.tr("This is how subtitles look"), "RlCaptionSample");
        subs.add(com.codename1.ui.layouts.FlowLayout.encloseCenter(sample));
        subs.add(Ui.text("Used whenever a video has a track in that language. Change it for one video from "
                + "Audio & subtitles in the player.", "SvMuted"));
        page.add(subs);

        Ui.section(page, "Notifications");
        Container notify = group();
        notify.add(Ui.row(FontImage.MATERIAL_NEW_RELEASES, "New episodes", "Of titles on your list",
                Ui.toggle("notifyNewEpisodes", Prefs.notifyNewEpisodes(), Prefs::setNotifyNewEpisodes)));
        notify.add(Ui.row(FontImage.MATERIAL_SECURITY, "Account", "Sign-ins, billing, moderation",
                Ui.toggle("notifyAccount", Prefs.notifyAccount(), Prefs::setNotifyAccount)));
        page.add(notify);

        Ui.section(page, "Appearance");
        Container look = group();
        look.add(Ui.label("Language", "SvLabel"));
        String speaking = Lang.current();
        for (int iter = 0; iter < Lang.CODES.length; iter++) {
            final String code = Lang.CODES[iter];
            Label tick = new Label("", "SvRowIcon");
            if (code.equals(speaking)) {
                Ui.icon(tick, FontImage.MATERIAL_CHECK, 3.4f);
            }
            Container row = new Container(new BorderLayout());
            row.setUIID("SvRow");
            // A language is called what its speakers call it, whatever
            // language the rest of the screen is in.
            row.add(BorderLayout.CENTER, Ui.plain(Lang.name(code), "SvRowTitle"));
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
        look.add(Ui.label("Theme", "SvLabel"));
        look.add(Ui.choice("theme", new String[] {"System", "Light", "Dark"},
                new String[] {Prefs.THEME_SYSTEM, Prefs.THEME_LIGHT, Prefs.THEME_DARK},
                Prefs.theme(), value -> {
                    if (!value.equals(Prefs.theme())) {
                        Prefs.setTheme(value);
                        Telemetry.themeChanged(value);
                    }
                    again(previous);
                }));
        page.add(look);

        Ui.section(page, "Privacy");
        Container safe = group();
        // Kept on this device and not with the account: it is this
        // installation that reports or does not.
        safe.add(Ui.row(FontImage.MATERIAL_INSIGHTS, "Usage statistics",
                "Share which screens and features are used. Never your name, what you "
                + "watch or payment details.",
                Ui.toggle("usageStatistics", Telemetry.allowed(), Telemetry::allow)));
        page.add(safe);

        Ui.section(page, "About");
        Container about = group();
        about.add(Ui.row(FontImage.MATERIAL_INFO, "Reel",
                "A sample streaming service built with Codename One", null));
        about.add(Ui.tap(Ui.row(FontImage.MATERIAL_DESCRIPTION, "Terms of service", null,
                chevron()), "terms", e -> legal(form, "Terms of service",
                        "Reel is a sample. The films are Blender Foundation open movies under "
                        + "Creative Commons licenses, and no money changes hands.", null)));
        about.add(Ui.tap(Ui.row(FontImage.MATERIAL_POLICY, "Privacy policy", null, chevron()),
                "privacy", e -> legal(form, "Privacy policy",
                        "What you watch, how far and on which device is kept by the server you "
                        + "signed in to: to resume where you stopped, to recommend, to enforce "
                        + "your plan's streams and to notice a shared password. Your settings "
                        + "are kept with your account on that server.",
                        "With usage statistics turned on, the app also counts which screens "
                        + "are opened and which features are used, and reports that to "
                        + "Codename One's analytics service under a made-up id for this "
                        + "device. Your name, e-mail, phone number, places and card details "
                        + "are never part of it, and turning it off in Settings stops it.")));
        if (AppConfig.serverSwitchable()) {
            about.add(Ui.tap(Ui.row(FontImage.MATERIAL_DNS, "Server address", null, chevron()),
                    "server", e -> ServerForm.show(form)));
        }
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
        group.setUIID("SvGroup");
        return group;
    }

    /// The mark at the end of a row that leads somewhere.
    static Label chevron() {
        Label chevron = new Label("", "SvRowIcon");
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
        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        final Button save = Ui.primary("Change password", "submit", null);
        save.addActionListener(e -> {
            if (!replacement.getText().equals(again.getText())) {
                error.setText(Lang.tr("The two new passwords are not the same."));
                Ui.refresh(form);
                return;
            }
            PasswordChangeDto change = new PasswordChangeDto(current.getText(),
                    replacement.getText());
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
                "Your sign-in, your profiles, history, downloads, saved cards and settings "
                + "are removed for good. Invoices stay in the service's records, under the "
                + "email address you signed in with.",
                "SvBannerIconBad"));
        // Asked for again, as changing the password asks: being signed in is
        // what a borrowed phone is, too, and the server refuses without it.
        final TextField password = Ui.field(page, "Current password", "password", "",
                TextArea.PASSWORD);
        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        page.add(error);
        final Button go = Ui.danger("Delete my account", "confirmDelete", null);
        go.addActionListener(e -> {
            go.setEnabled(false);
            AccountClosureDto closure = new AccountClosureDto(password.getText());
            Api.account().close(closure, Net.to(gone -> Nav.deleted(), (status, message) -> {
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
        page.add(Ui.text(text, "SvText"));
        if (more != null) {
            page.add(Ui.text(more, "SvText"));
        }
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
