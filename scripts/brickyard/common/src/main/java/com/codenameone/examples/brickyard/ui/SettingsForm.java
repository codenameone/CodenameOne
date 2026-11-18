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
package com.codenameone.examples.brickyard.ui;

import com.codename1.ui.Label;
import com.codenameone.examples.brickyard.api.LimitsDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codename1.io.Preferences;
import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.components.Switch;
import com.codename1.ui.TextField;
import com.codenameone.examples.brickyard.AppConfig;
import com.codenameone.examples.brickyard.api.AccountDto;
import com.codenameone.examples.brickyard.engage.Notifications;
import com.codenameone.examples.brickyard.engage.Telemetry;
import com.codenameone.examples.brickyard.net.Session;

import java.util.Calendar;

/// Settings: the age gate's answer, the parental controls (a monthly Bricks
/// spending limit and chat), notifications, analytics, and signing out. The
/// parental controls are kept and enforced by the server (a purchase past the
/// limit, or a chat line with chat off, is refused there); this device keeps
/// a copy so the shop can warn before asking.
public final class SettingsForm {
    static final int CHILD_AGE = 13;
    static final int CHILD_DEFAULT_LIMIT = 0;
    private static final String LIMIT = "brickyard.spend.limit";
    private static final String SPENT = "brickyard.spend.spent";
    private static final String MONTH = "brickyard.spend.month";
    private static final String CHAT = "brickyard.chat";

    private SettingsForm() {
    }

    public static void show() {
        final Form previous = com.codename1.ui.CN.getCurrentForm();
        Form f = Ui.form("Settings", "settings");
        Ui.back(f, () -> {
            if (previous != null) {
                previous.showBack();
            } else {
                Nav.home();
            }
        });
        AccountDto me = Session.user();
        int age = me == null ? -1 : age(me.birthYear(), Calendar.getInstance().get(Calendar.YEAR));
        f.add(Ui.label("Age", "RowTitle"));
        f.add(Ui.text(ageText(age), "Body"));

        f.add(Ui.label("Parental controls", "RowTitle"));
        final TextField limit = Ui.field("Monthly Bricks limit (empty for none)", TextField.NUMERIC,
                "spendLimit");
        int current = limit(age);
        limit.setText(current < 0 ? "" : String.valueOf(current));
        final Switch chat = new Switch();
        final Label spentLabel = Ui.label("Spent this month: " + spent() + " Bricks", "Muted");
        final boolean[] loaded = {false};
        limit.addDataChangedListener((type, index) -> {
            String t = limit.getText().trim();
            Preferences.set(LIMIT, t.length() == 0 ? -1 : parse(t));
            if (loaded[0]) {
                saveLimits(limit, chat);
            }
        });
        f.add(limit);
        f.add(spentLabel);
        chat.setName("chatSwitch");
        chat.setValue(chatEnabled(age));
        chat.addChangeListener(e -> {
            Preferences.set(CHAT, chat.isValue());
            if (loaded[0]) {
                saveLimits(limit, chat);
            }
        });
        f.add(Ui.panel(Ui.label("Chat in experiences", "Body"), chat));
        // The server's limits are the ones that count.
        Api.account().limits(Net.to(l -> {
            limit.setText(l.spendLimit() < 0 ? "" : String.valueOf(l.spendLimit()));
            chat.setValue(l.chatEnabled());
            spentLabel.setText("Spent this month: " + l.spent() + " Bricks");
            Preferences.set(LIMIT, (int) l.spendLimit());
            Preferences.set(CHAT, l.chatEnabled());
            loaded[0] = true;
            f.revalidate();
        }, (status, message) -> loaded[0] = true));

        f.add(Ui.label("App", "RowTitle"));
        Switch push = new Switch();
        push.setName("pushSwitch");
        push.setValue(Notifications.enabled());
        push.addChangeListener(e -> Notifications.setEnabled(push.isValue()));
        f.add(Ui.panel(Ui.label("Notifications", "Body"), push));
        Switch analytics = new Switch();
        analytics.setName("analyticsSwitch");
        analytics.setValue(Telemetry.allowed());
        analytics.addChangeListener(e -> Telemetry.allow(analytics.isValue()));
        f.add(Ui.panel(Ui.label("Share usage statistics", "Body"), analytics));
        if (AppConfig.serverSwitchable()) {
            f.add(Ui.link("Server: " + AppConfig.serverUrl(), "serverButton", e -> chooseServer()));
        }
        if (AdminForm.allowed()) {
            f.add(Ui.secondary("Admin console", "adminButton", e -> AdminConsole.show()));
        }
        f.add(Ui.secondary("Sign out", "signOutButton", e -> Nav.signOut()));
        f.show();
    }

    private static void saveLimits(TextField limit, Switch chat) {
        String t = limit.getText().trim();
        int v = t.length() == 0 ? -1 : parse(t);
        Api.account().saveLimits(new LimitsDto(v, 0, chat.isValue()), Net.to(saved -> { }, Ui::failed));
    }

    static int age(int birthYear, int thisYear) {
        return birthYear <= 0 ? -1 : thisYear - birthYear;
    }

    static String ageText(int age) {
        if (age < 0) {
            return "No birth year on this account.";
        }
        if (age < CHILD_AGE) {
            return "Under " + CHILD_AGE + ": chat starts off and purchases need a parent's limit.";
        }
        return CHILD_AGE + " or older.";
    }

    /// The monthly limit, or -1 for none. A child's account starts at zero.
    static int limit(int age) {
        int stored = Preferences.get(LIMIT, Integer.MIN_VALUE);
        if (stored != Integer.MIN_VALUE) {
            return stored;
        }
        return age >= 0 && age < CHILD_AGE ? CHILD_DEFAULT_LIMIT : -1;
    }

    static boolean chatEnabled(int age) {
        return Preferences.get(CHAT, age < 0 || age >= CHILD_AGE);
    }

    /// Whether a purchase of `bricks` fits this month's limit.
    public static boolean allowSpend(int bricks) {
        AccountDto me = Session.user();
        int age = me == null ? -1 : age(me.birthYear(), Calendar.getInstance().get(Calendar.YEAR));
        return fits(limit(age), spent(), bricks);
    }

    static boolean fits(int limit, int spent, int bricks) {
        return limit < 0 || spent + bricks <= limit;
    }

    public static void recordSpend(int bricks) {
        Preferences.set(SPENT, spent() + bricks);
    }

    static int spent() {
        Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.YEAR) * 100 + c.get(Calendar.MONTH) + 1;
        if (Preferences.get(MONTH, 0) != month) {
            Preferences.set(MONTH, month);
            Preferences.set(SPENT, 0);
        }
        return Preferences.get(SPENT, 0);
    }

    private static int parse(String t) {
        int v = 0;
        for (int i = 0; i < t.length() && v < 1000000; i++) {
            char c = t.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }

    public static void chooseServer() {
        TextField url = Ui.field("http://localhost:8080", TextField.URL, "serverUrl");
        url.setText(AppConfig.serverUrl());
        Command ok = new Command("Use");
        Command picked = Dialog.show("Server", url, new Command[] {ok, new Command("Cancel")});
        if (picked == ok) {
            AppConfig.setServerUrl(url.getText());
            Session.start(AppConfig.serverUrl());
            SignInForm.show();
        }
    }
}
