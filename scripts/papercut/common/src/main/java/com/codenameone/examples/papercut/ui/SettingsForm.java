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
package com.codenameone.examples.papercut.ui;

import com.codename1.appkit.Net;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codenameone.examples.papercut.AppConfig;
import com.codenameone.examples.papercut.api.ProfileDto;
import com.codenameone.examples.papercut.engage.Notifications;
import com.codenameone.examples.papercut.net.Api;
import com.codenameone.examples.papercut.net.Session;
import com.codenameone.examples.papercut.play.Sfx;

/// Sound, the daily-sheet notification, the name on the boards, the server
/// (so a phone can reach a laptop on the same network), and signing out.
public final class SettingsForm {
    private SettingsForm() {
    }

    public static void show() {
        Form f = Ui.form("Settings", "settings");
        Ui.desktopColumn(f);
        Ui.back(f, Nav::home);
        final CheckBox sound = CheckBox.createToggle("Sound");
        sound.setName("sound");
        sound.setSelected(!Sfx.isMuted());
        sound.addActionListener(e -> Sfx.setMuted(!sound.isSelected()));
        f.add(Ui.panel(Ui.label("Sound", "CardTitle"), sound));
        if (Notifications.available()) {
            final CheckBox push = CheckBox.createToggle("Tell me when today's sheet is out");
            push.setName("push");
            push.setSelected(Notifications.enabled());
            push.addActionListener(e -> Notifications.setEnabled(push.isSelected()));
            f.add(Ui.panel(push));
        }
        if (Session.user() != null) {
            final TextField name = new TextField(Session.user().displayName(), "Name on the boards", 20,
                    TextField.ANY);
            name.setName("displayName");
            f.add(Ui.panel(Ui.label("You", "CardTitle"), name,
                    Ui.primary("Save", "saveProfile", e -> Api.account().saveProfile(
                            new ProfileDto(name.getText().trim(), Session.user().quill()),
                            Net.to(p -> Ui.later(() -> {
                                Session.setUser(p);
                                Ui.toast("Saved");
                            }), Ui::failed))),
                    Ui.secondary("Sign out", "signOut", e -> Nav.signOut())));
        }
        if (AppConfig.serverSwitchable()) {
            final TextField server = new TextField(AppConfig.serverUrl(), "Server", 30, TextField.URL);
            server.setName("server");
            f.add(Ui.panel(Ui.label("Server", "CardTitle"), server, Ui.secondary("Use this server",
                    "useServer", e -> {
                        AppConfig.setServerUrl(server.getText());
                        Session.start(AppConfig.serverUrl());
                        Nav.launch();
                    })));
        }
        f.show();
    }
}
