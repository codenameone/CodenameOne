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
package com.codenameone.examples.bastion.ui;

import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codenameone.examples.bastion.api.UserDto;
import com.codenameone.examples.bastion.net.Session;

/// The way around the app: the side menu every signed-in screen has, and the
/// screen each entry leads to.
///
/// The admin entry is shown only to an account with the role, and that is a
/// courtesy, not the protection: the server refuses `/api/admin/**` to anyone
/// else whatever the app shows.
public final class Nav {
    private static Runnable signedOut;

    private Nav() {
    }

    /// What to show once the user has signed out, or been signed out.
    public static void install(Runnable whenSignedOut) {
        signedOut = whenSignedOut;
    }

    /// Adds the side menu to `form`.
    public static void menu(Form form) {
        Toolbar bar = form.getToolbar();
        UserDto user = Session.user();
        if (user != null) {
            Label who = new Label(user.displayName() == null ? user.username() : user.displayName(),
                    "BnMenuHeader");
            bar.addComponentToSideMenu(who);
        }
        bar.addMaterialCommandToSideMenu(Lang.tr("Accounts"), FontImage.MATERIAL_ACCOUNT_BALANCE,
                evt -> home());
        bar.addMaterialCommandToSideMenu(Lang.tr("Move money"), FontImage.MATERIAL_SWAP_HORIZ,
                evt -> TransferForm.show(null, false, Nav::home));
        bar.addMaterialCommandToSideMenu(Lang.tr("Payees"), FontImage.MATERIAL_PEOPLE,
                evt -> PayeesForm.show(Nav::home));
        bar.addMaterialCommandToSideMenu(Lang.tr("Cards"), FontImage.MATERIAL_CREDIT_CARD,
                evt -> CardsForm.show(Nav::home));
        bar.addMaterialCommandToSideMenu(Lang.tr("Exchange"), FontImage.MATERIAL_SHOW_CHART,
                evt -> ExchangeForm.show(Nav::home));
        bar.addMaterialCommandToSideMenu(Lang.tr("Send crypto"), FontImage.MATERIAL_CALL_MADE,
                evt -> WithdrawForm.show(null, Nav::home));
        bar.addMaterialCommandToSideMenu(Lang.tr("Security Center"), FontImage.MATERIAL_SECURITY,
                evt -> SecurityCenterForm.show(Nav::home));
        bar.addMaterialCommandToSideMenu(Lang.tr("Settings"), FontImage.MATERIAL_SETTINGS,
                evt -> SettingsForm.show(Nav::home));
        if (user != null && user.admin()) {
            bar.addMaterialCommandToSideMenu(Lang.tr("Administration"),
                    FontImage.MATERIAL_ADMIN_PANEL_SETTINGS, evt -> AdminForm.show(Nav::home));
        }
        bar.addMaterialCommandToSideMenu(Lang.tr("Sign out"), FontImage.MATERIAL_LOGOUT,
                evt -> signOut());
    }

    /// The dashboard; for an admin with no customer accounts, administration.
    public static void home() {
        UserDto user = Session.user();
        if (user != null && !user.customer() && user.admin()) {
            AdminForm.show(null);
        } else {
            HomeForm.show();
        }
    }

    public static void signOut() {
        Session.signOut(signedOut);
    }

    /// A form of the signed-in app: a back arrow to `back` (or none, for the
    /// dashboard) and the menu.
    public static Form form(String title, Runnable back) {
        Form form = Ui.form(title, back);
        menu(form);
        return form;
    }
}
