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
package com.codenameone.examples.bazaar.ui;

import com.codename1.appkit.Net;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.bazaar.AppConfig;
import com.codenameone.examples.bazaar.Prefs;
import com.codenameone.examples.bazaar.Telemetry;
import com.codenameone.examples.bazaar.admin.AdminSections;
import com.codenameone.examples.bazaar.api.UserDto;
import com.codenameone.examples.bazaar.live.LiveChannel;
import com.codenameone.examples.bazaar.net.Account;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.net.Session;
import com.codenameone.examples.bazaar.notify.Notifications;
import java.util.ArrayList;
import java.util.List;

/// Decides which screen the app is on.
///
/// The app is four apps in one -- shopping, running a store, ringing up sales
/// at the counter and administering the platform -- and which of them a user
/// may enter is the server's decision: it is the roles on their account, as
/// `/api/me` reports them (`merchant`, `admin`). Each mode has a home screen,
/// a [Shell] over the mode's [Sections], and the user is in one of them at a
/// time. Hiding a mode here is a courtesy and not the protection; the server
/// refuses a merchant's or an admin's call from anyone without the role,
/// whatever the app shows.
public final class Nav {
    /// Shopping: the storefront. Everyone may.
    public static final String SHOP = "shop";
    /// Running a store: staff of a store.
    public static final String MERCHANT = "merchant";
    /// The register: quick sale entry for a cashier, inside the merchant's side.
    public static final String POS = "pos";
    /// The platform console.
    public static final String ADMIN = "admin";

    /// The home screen of a mode.
    public interface Home {
        /// Fills the screen in and starts it talking to the server. Called
        /// once, before the screen is first shown.
        void start();

        Form form();
    }

    /// The home screen last built, and the mode it is the home of.
    private static Form homeForm;
    private static String homeMode = "";

    private Nav() {
    }

    /// Whether `form` is still the home screen, for an answer that arrives
    /// after the user has gone to another mode or signed out and should then
    /// change nothing.
    public static boolean isHome(Form form) {
        return form != null && form == homeForm;
    }

    /// The first screen: wherever the stored session leads.
    public static void launch() {
        AdminSections.install();
        Session.onSignedOut(() -> {
            LiveChannel.stop();
            Telemetry.signedOut("expired");
            WelcomeForm.show();
        });
        Form starting = Ui.form("", "Starting");
        starting.getToolbar().hideToolbar();
        starting.show();
        Session.restore(Nav::enter, WelcomeForm::show, Nav::unreachable);
    }

    /// Someone has just signed in, or been found still signed in: their
    /// account's settings are fetched, applied, and then their home is shown in
    /// the language and the colours they chose.
    public static void enter() {
        Notifications.signedIn();
        // Prices are shown in the server's currency, which only the payment
        // config says.
        Api.payments().config(Net.to(config -> Ui.setCurrency(config.currency()), (status, message) -> { }));
        Account.load(() -> {
            Look.apply();
            Lang.apply();
            if (Telemetry.asked()) {
                home();
            } else {
                ConsentForm.show(Nav::home);
            }
        });
    }

    /// Shows the home screen of the mode the user is in.
    public static void home() {
        Form home = build();
        if (home != null) {
            home.show();
        }
    }

    /// The account was asked about again: back to the home screen only if
    /// the mode it opens in is no longer the one on screen.
    public static void refreshed() {
        UserDto user = Session.user();
        if (user == null || !mode(user).equals(homeMode)) {
            home();
        }
    }

    /// Builds the home screen of the mode the user is in, without showing it.
    /// Null, and the welcome screen showing, when nobody is signed in.
    public static Form build() {
        UserDto user = Session.user();
        if (user == null) {
            WelcomeForm.show();
            return null;
        }
        LiveChannel.start();
        String mode = mode(user);
        Telemetry.role(mode);
        Home home = new Shell(mode);
        home.start();
        homeForm = home.form();
        homeMode = mode;
        return homeForm;
    }

    /// The modes `user` may enter, in the order they are offered.
    public static List<String> modes(UserDto user) {
        List<String> modes = new ArrayList<String>();
        modes.add(SHOP);
        if (user != null && user.merchant()) {
            modes.add(MERCHANT);
            modes.add(POS);
        }
        if (user != null && user.admin()) {
            modes.add(ADMIN);
        }
        return modes;
    }

    /// What a mode is called where the user switches to it.
    public static String label(String mode) {
        return ADMIN.equals(mode) ? "Administration" : MERCHANT.equals(mode) ? "Your store"
                : POS.equals(mode) ? "Point of sale" : "Shopping";
    }

    public static char icon(String mode) {
        return ADMIN.equals(mode) ? FontImage.MATERIAL_ADMIN_PANEL_SETTINGS
                : MERCHANT.equals(mode) ? FontImage.MATERIAL_STORE
                : POS.equals(mode) ? FontImage.MATERIAL_POINT_OF_SALE
                : FontImage.MATERIAL_SHOPPING_BAG;
    }

    /// The mode to open for `user`.
    ///
    /// The one they chose last, while their account still allows it. With no
    /// choice made, the one their account is for: an admin administers, store
    /// staff run their store, and everyone else shops. A choice is remembered
    /// for the account that made it.
    public static String mode(UserDto user) {
        String saved = AppConfig.mode(Session.owner(user.username()));
        if (saved != null && modes(user).contains(saved)) {
            return saved;
        }
        return user.admin() ? ADMIN : user.merchant() ? MERCHANT : SHOP;
    }

    /// The user chose `mode`.
    public static void switchTo(String mode) {
        UserDto user = Session.user();
        AppConfig.setMode(mode, user == null ? "" : Session.owner(user.username()));
        home();
    }

    /// A notification about an order was opened: the home of the side it was
    /// for (`shop` or `merchant`), on its Orders section. Nothing changes when
    /// that home is already on screen; it hears of the change over the live
    /// channel too.
    public static void showOrder(String role) {
        UserDto user = Session.user();
        if (user == null) {
            return;
        }
        String wanted = MERCHANT.equals(role) && user.merchant() ? MERCHANT : SHOP;
        if (!(wanted.equals(homeMode) && homeForm != null && CN.getCurrentForm() == homeForm)) {
            AppConfig.setMode(wanted, Session.owner(user.username()));
            home();
        }
        Shell shell = Shell.current();
        if (shell != null && wanted.equals(shell.mode())) {
            shell.show(Sections.ORDERS);
        }
    }

    public static void signOut() {
        leave("user", true);
    }

    /// The account was closed: signed out, and what the device kept for the
    /// usage reports forgotten with it.
    public static void deleted() {
        leave("deleted", false);
        Telemetry.forget();
        Prefs.ownedBy("");
    }

    private static void leave(final String reason, boolean forgetDevice) {
        Account.flush();
        LiveChannel.stop();
        homeForm = null;
        homeMode = "";
        Runnable out = () -> {
            Telemetry.signedOut(reason);
            Session.signOut(WelcomeForm::show);
        };
        if (forgetDevice) {
            Notifications.signingOut(out);
        } else {
            out.run();
        }
    }

    /// Points the app at another server, which ends the session with this one.
    public static void changeServer(final String url) {
        Runnable switchOver = () -> {
            AppConfig.setServerUrl(url);
            Session.start(AppConfig.serverUrl());
            launch();
        };
        if (AppConfig.sameServer(url, Session.serverUrl()) || Session.user() == null) {
            LiveChannel.stop();
            switchOver.run();
            return;
        }
        // Signed in: sign out against the server still in use first, so it
        // does not keep a live refresh token and this device's push
        // registration.
        Account.flush();
        LiveChannel.stop();
        homeForm = null;
        homeMode = "";
        Notifications.signingOut(() -> {
            Telemetry.signedOut("server");
            Session.signOut(switchOver);
        });
    }

    /// There is a session, and no server to use it with.
    private static void unreachable() {
        Form form = Ui.form("Bazaar", "Unreachable");
        Container page = Ui.page();
        page.add(Ui.label("No connection", "SvTitle"));
        page.add(Ui.text(Lang.tr("The server at {0} did not answer.", Session.serverUrl()),
                "SvText"));
        page.add(Ui.primary("Try again", "retry", e -> launch()));
        if (AppConfig.serverSwitchable()) {
            page.add(Ui.secondary("Server address", "server", e -> ServerForm.show(form)));
        }
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
