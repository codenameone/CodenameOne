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
package com.codenameone.examples.reel.admin;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.reel.Telemetry;
import com.codenameone.examples.reel.api.FlagDto;
import com.codenameone.examples.reel.api.RoleChangeDto;
import com.codenameone.examples.reel.api.SuspendDto;
import com.codenameone.examples.reel.api.UserDetailDto;
import com.codenameone.examples.reel.api.UserDto;
import com.codenameone.examples.reel.moderation.ModerationForm;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Nav;
import com.codenameone.examples.reel.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// One account as an operator sees it: plan, devices, what it is watching,
/// billing, its moderation and sharing record, and what can be done to it.
/// Every action against the account asks why, and the reason is kept.
final class UserForm {
    static final String[] ROLES = {Nav.ROLE_CREATOR, Nav.ROLE_MODERATOR, Nav.ROLE_ADMIN};

    private UserForm() {
    }

    static void show(final Form previous, final String username) {
        final Form form = Ui.form(username, "AdminUser");
        Ui.back(form, previous);
        final Container page = Ui.list("userDetail");
        form.add(BorderLayout.CENTER, page);
        form.show();
        load(form, page, username);
    }

    private static void load(final Form form, final Container page, final String username) {
        Api.admin().user(username, Net.to(d -> fill(form, page, d), Ui.failed(page, () -> load(form, page, username))));
    }

    private static void fill(final Form form, final Container page, final UserDetailDto d) {
        final UserDto u = d.user();
        final Runnable reload = () -> load(form, page, u.username());
        page.removeAll();
        Container facts = Ui.page();
        facts.add(Ui.fact("Name", u.name())).add(Ui.fact("Email", u.email()))
                .add(Ui.fact("Plan", u.planCode() == null || u.planCode().length() == 0 ? Lang.tr("None")
                        : u.planCode() + (u.subscriptionStatus() == null ? ""
                        : Ui.DOT + Lang.tr(u.subscriptionStatus()))))
                .add(Ui.fact("Joined", Ui.day(u.createdAt())))
                .add(Ui.fact("Last seen", u.lastSeenAt() == 0 ? "-" : Ui.when(u.lastSeenAt())));
        page.add(facts);

        Ui.section(page, "Roles");
        final List<String> roles = new ArrayList<String>(u.roles() == null ? new ArrayList<String>() : u.roles());
        for (int iter = 0; iter < ROLES.length; iter++) {
            final String role = ROLES[iter];
            page.add(Ui.row((char) 0, Lang.tr(role), null, Ui.toggle("role-" + role, roles.contains(role), on -> {
                if (on) {
                    roles.add(role);
                } else {
                    roles.remove(role);
                }
                Api.admin().setRoles(u.username(), new RoleChangeDto(roles), Net.to(x -> { }, (code, message) -> {
                    Ui.fail(code, message);
                    reload.run();
                }));
            })));
        }

        Ui.section(page, "Actions");
        page.add(u.suspended()
                ? Ui.secondary("Reinstate", "reinstate", e -> Ui.reason(form, "Reinstate", "Why is the account back?",
                        "Reinstate", why -> suspend(form, u, false, why, reload)))
                : Ui.danger("Suspend", "suspend", e -> Ui.reason(form, "Suspend",
                        "Suspending signs the account out everywhere and stops its streams and downloads.",
                        "Suspend", why -> suspend(form, u, true, why, reload))));
        page.add(Ui.secondary(u.flagged() ? "Clear flag" : "Flag for review", "flag", e -> Ui.reason(form,
                u.flagged() ? "Clear flag" : "Flag", "Kept on the account's record.", u.flagged() ? "Clear" : "Flag",
                why -> Api.admin().flag(u.username(), new FlagDto(!u.flagged(), why), Net.to(x -> {
                    form.showBack();
                    reload.run();
                }, Ui::fail)))));
        page.add(Ui.link("Moderation record", "moderationRecord", e -> ModerationForm.events(form, u.username())));

        Ui.section(page, "Devices");
        for (int iter = 0; d.devices() != null && iter < d.devices().size(); iter++) {
            page.add(Ui.row(FontImage.MATERIAL_DEVICES, d.devices().get(iter).name(), d.devices().get(iter).platform()
                    + Ui.DOT + d.devices().get(iter).lastCountry() + Ui.DOT + Ui.when(d.devices().get(iter).lastSeenAt()),
                    null));
        }
        if (d.sessions() != null && !d.sessions().isEmpty()) {
            Ui.section(page, "Watching now");
            for (int iter = 0; iter < d.sessions().size(); iter++) {
                page.add(Ui.row(FontImage.MATERIAL_CAST, d.sessions().get(iter).titleName(),
                        d.sessions().get(iter).deviceName() + Ui.DOT + d.sessions().get(iter).ip(), null));
            }
        }
        if (d.sharingAlerts() != null && !d.sharingAlerts().isEmpty()) {
            Ui.section(page, "Sharing alerts");
            for (int iter = 0; iter < d.sharingAlerts().size(); iter++) {
                page.add(Ui.row(FontImage.MATERIAL_GROUP_OFF, Lang.tr(d.sharingAlerts().get(iter).status()),
                        d.sharingAlerts().get(iter).detail(), null));
            }
        }
        Ui.section(page, "Billing");
        for (int iter = 0; d.invoices() != null && iter < d.invoices().size(); iter++) {
            page.add(Ui.row(FontImage.MATERIAL_RECEIPT, d.invoices().get(iter).description(),
                    Ui.day(d.invoices().get(iter).createdAt()) + Ui.DOT + Lang.tr(d.invoices().get(iter).status()),
                    Ui.label(Ui.money(d.invoices().get(iter).amountCents(), d.invoices().get(iter).currency()),
                            "SvMuted")));
        }
        page.revalidate();
    }

    private static void suspend(final Form form, UserDto u, final boolean suspended, String why,
            final Runnable reload) {
        Api.admin().suspend(u.username(), new SuspendDto(suspended, why), Net.to(x -> {
            Telemetry.userSuspended(suspended);
            form.showBack();
            reload.run();
        }, Ui::fail));
    }
}
