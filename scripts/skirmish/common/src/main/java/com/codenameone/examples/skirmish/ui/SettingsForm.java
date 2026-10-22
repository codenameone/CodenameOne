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
package com.codenameone.examples.skirmish.ui;

import com.codename1.components.Switch;
import com.codename1.io.Preferences;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.skirmish.AppConfig;
import com.codenameone.examples.skirmish.api.ProfileDto;
import com.codenameone.examples.skirmish.battle.Sfx;
import com.codenameone.examples.skirmish.engage.Notifications;
import com.codenameone.examples.skirmish.engage.Telemetry;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.net.Session;

/// Settings: the battle name, sound, analytics consent, push, the server.
public final class SettingsForm {
    static final String SOUND_KEY = "skirmish.sound";

    private SettingsForm() {
    }

    /// Applies what was chosen in an earlier run.
    public static void apply() {
        Sfx.setMuted(!Preferences.get(SOUND_KEY, true));
    }

    public static void show() {
        Form f = Ui.form("Settings", "settings");
        Ui.back(f, Nav::home);
        final TextField name = new TextField(Session.user().displayName(), "Battle name", 16, TextField.ANY);
        name.setUIID("NameField");
        name.setName("displayName");
        f.add(Ui.label("Battle name", "SectionTitle"));
        f.add(Ui.panel(name, Ui.stone("Rename", "rename", e -> Api.account().saveProfile(
                new ProfileDto(name.getText().trim()), Net.to(p -> {
                    Session.setUser(p);
                    Ui.toast("Renamed");
                }, Ui::failed)))));
        f.add(Ui.label("Game", "SectionTitle"));
        f.add(Ui.panel(
                toggle("Sound", "sound", !Sfx.isMuted(), on -> {
                    Preferences.set(SOUND_KEY, on);
                    Sfx.setMuted(!on);
                }),
                toggle("Share anonymous usage", "analytics", Telemetry.allowed(), Telemetry::allow),
                toggle("Notifications", "push", Notifications.enabled(), Notifications::setEnabled)));
        if (AppConfig.serverSwitchable()) {
            f.add(Ui.stone("Server: " + AppConfig.serverUrl(), "serverButton", e -> chooseServer()));
        }
        f.add(Ui.button("Sign out", "DangerButton", "signOut", e -> Nav.signOut()));
        f.show();
    }

    interface Flipped {
        void flipped(boolean on);
    }

    private static Container toggle(String text, String name, boolean value, final Flipped flipped) {
        final Switch s = new Switch();
        s.setName(name);
        s.setValue(value);
        s.addActionListener(e -> flipped.flipped(s.isValue()));
        Container row = new Container(new BorderLayout());
        row.setUIID("SettingRow");
        row.add(BorderLayout.CENTER, new Label(text, "SettingLabel")).add(BorderLayout.EAST, s);
        return row;
    }

    /// Points the app at another server: a phone reaching the developer's
    /// laptop, or a test server.
    public static void chooseServer() {
        TextField url = new TextField(AppConfig.serverUrl(), "http://192.168.1.10:8080", 30, TextField.URL);
        url.setName("serverUrl");
        if (Dialog.show("Server", url, new com.codename1.ui.Command("Cancel"),
                new com.codename1.ui.Command("Use")).getCommandName().equals("Use")) {
            AppConfig.setServerUrl(url.getText().trim());
            Session.start(AppConfig.serverUrl());
            SignInForm.show();
        }
    }
}
