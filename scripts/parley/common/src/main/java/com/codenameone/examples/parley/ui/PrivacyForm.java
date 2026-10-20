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
package com.codenameone.examples.parley.ui;

import com.codename1.security.Biometrics;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.parley.Prefs;
import com.codenameone.examples.parley.Telemetry;

/// The privacy settings: who sees what about this account, what it tells
/// others, and what the phone shows.
///
/// Last seen and the profile photo are enforced by the server, which is the
/// one that hands them out. Read receipts and typing are enforced by this
/// app, which simply does not send them; turning read receipts off also
/// stops this account seeing other people's, as in every messenger, so that
/// it is not a way to watch without being watched.
public final class PrivacyForm {
    static final String[] TIMER_LABELS = {"Off", "5 min", "1 hour", "1 day", "1 week"};
    static final String[] TIMER_VALUES = {"0", "300", "3600", "86400", "604800"};

    private PrivacyForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Privacy", "Privacy");
        Ui.back(form, previous);
        Container page = Ui.page();

        Ui.section(page, "Who can see");
        Container see = SettingsForm.group();
        String[] who = {"Everyone", "My contacts", "Nobody"};
        String[] whoValues = {Prefs.EVERYONE, Prefs.CONTACTS, Prefs.NOBODY};
        see.add(Ui.label("Last seen and online", "PlLabel"));
        see.add(Ui.choice("lastSeen", who, whoValues, Prefs.lastSeen(), Prefs::setLastSeen));
        see.add(Ui.label("Profile photo", "PlLabel"));
        see.add(Ui.choice("profilePhoto", who, whoValues, Prefs.profilePhoto(),
                Prefs::setProfilePhoto));
        page.add(see);

        Ui.section(page, "Messages");
        Container messages = SettingsForm.group();
        messages.add(Ui.row(FontImage.MATERIAL_DONE_ALL, "Read receipts",
                "Off: nobody sees when you read, and you do not see when they do",
                Ui.toggle("readReceipts", Prefs.readReceipts(), Prefs::setReadReceipts)));
        messages.add(Ui.row(FontImage.MATERIAL_KEYBOARD, "Typing indicator",
                "Show others when you are typing",
                Ui.toggle("typing", Prefs.typingIndicators(), Prefs::setTypingIndicators)));
        messages.add(Ui.label("Default timer for new chats", "PlLabel"));
        messages.add(Ui.choice("defaultTimer", TIMER_LABELS, TIMER_VALUES,
                String.valueOf(Prefs.disappearingSeconds()), value -> {
                    int seconds = Integer.parseInt(value);
                    Prefs.setDisappearingSeconds(seconds);
                    Telemetry.timerChanged(seconds);
                }));
        messages.add(Ui.text("Messages in a chat with a timer are deleted from every phone in "
                + "it when the timer runs out, and from the server if nobody fetched them.",
                "PlMuted"));
        page.add(messages);

        Ui.section(page, "This phone");
        Container phone = SettingsForm.group();
        phone.add(Ui.row(FontImage.MATERIAL_SCREEN_LOCK_PORTRAIT, "Screen security",
                "Keep chats out of screenshots and the app switcher",
                Ui.toggle("screenSecurity", Prefs.screenSecurity(), on -> {
                    Prefs.setScreenSecurity(on);
                    Privacy.shown();
                })));
        if (Biometrics.getInstance().isSupported()) {
            phone.add(Ui.row(FontImage.MATERIAL_FINGERPRINT, "App lock",
                    "Ask for your fingerprint, face or passcode to open Parley",
                    Ui.toggle("appLock", Prefs.appLock(), Prefs::setAppLock)));
        }
        page.add(phone);

        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    /// The words for a timer, for the chat's title bar and info screen.
    public static String timer(int seconds) {
        for (int iter = 0; iter < TIMER_VALUES.length; iter++) {
            if (TIMER_VALUES[iter].equals(String.valueOf(seconds))) {
                return TIMER_LABELS[iter];
            }
        }
        return seconds + " s";
    }
}
