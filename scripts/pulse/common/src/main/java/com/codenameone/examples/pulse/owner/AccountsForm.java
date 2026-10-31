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
package com.codenameone.examples.pulse.owner;

import com.codename1.components.Switch;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.pulse.api.FlagDto;
import com.codenameone.examples.pulse.api.MemberDto;
import com.codenameone.examples.pulse.api.ModerationEventDto;
import com.codenameone.examples.pulse.api.RoleChangeDto;
import com.codenameone.examples.pulse.api.SuspendDto;
import com.codenameone.examples.pulse.api.UserDetailDto;
import com.codenameone.examples.pulse.api.UserDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.net.Session;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Skeleton;
import com.codenameone.examples.pulse.ui.StatRow;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// The studio's accounts, for the owner: everyone who can sign in, the roles
/// each holds, and the two things an owner does about an account that worries
/// them -- flag it for a closer look, or block it. Every one of those is kept,
/// with who did it and why, and shown on the account.
public final class AccountsForm {
    private static final String[] FILTERS = {"ALL", "STAFF", "FLAGGED", "BLOCKED"};
    private static final String[] FILTER_NAMES = {"All", "Staff", "Flagged", "Blocked"};

    private final Form form = Ui.form("Accounts", "OwnerAccounts");
    private final Container list = Ui.list("accounts");
    private final TextField search = new TextField("", Lang.tr("Search by name or e-mail"), 20,
            TextField.ANY);
    private String filter = "ALL";
    private List<UserDto> users;

    private AccountsForm() {
    }

    public static void show(Form previous) {
        new AccountsForm().open(previous);
    }

    private void open(Form previous) {
        Ui.back(form, previous);
        search.setUIID("SvSearchField");
        search.getHintLabel().setUIID("SvFieldHint");
        search.setName("accountSearch");
        search.addDataChangedListener((type, index) -> fill());
        String[] names = new String[FILTERS.length];
        for (int iter = 0; iter < names.length; iter++) {
            names[iter] = Lang.tr(FILTER_NAMES[iter]);
        }
        Container top = new Container(BoxLayout.y(), "SvSearchBar");
        top.add(search);
        // The list is small enough to filter here: one request, and the chips
        // answer at once.
        top.add(Ui.pills("accountFilter", names, FILTERS, filter, value -> {
            filter = value;
            fill();
        }));
        form.add(BorderLayout.NORTH, top);
        form.add(BorderLayout.CENTER, list);
        list.add(new Skeleton(6));
        Ui.pull(list, this::load);
        // Back from an account, whatever was done to it shows in the list.
        form.addShowListener(e -> load());
        form.show();
        CN.callSerially(() -> form.setFocused(null));
    }

    private void load() {
        Api.admin().users(Net.to(found -> {
            users = found;
            fill();
        }, Ui.failed(list, this::load)));
    }

    private void fill() {
        if (users == null) {
            return;
        }
        list.removeAll();
        String wanted = search.getText().trim();
        int shown = 0;
        for (int iter = 0; iter < users.size(); iter++) {
            final UserDto user = users.get(iter);
            if (!matches(user, filter) || !MembersForm.contains(user.displayName(), wanted)
                    && !MembersForm.contains(user.username(), wanted)) {
                continue;
            }
            list.add(Ui.tap(MembersForm.person(name(user), user.username(), state(user)),
                    "accountRow" + shown, e -> showAccount(form, user.username())));
            shown++;
        }
        if (shown == 0) {
            list.add(Ui.empty(FontImage.MATERIAL_MANAGE_ACCOUNTS, Lang.tr("No accounts here"),
                    Lang.tr("Try another filter.")));
        }
        Ui.refresh(form);
    }

    static boolean matches(UserDto user, String filter) {
        if ("STAFF".equals(filter)) {
            return user.trainer() || user.desk() || user.owner();
        }
        if ("FLAGGED".equals(filter)) {
            return user.flagged();
        }
        if ("BLOCKED".equals(filter)) {
            return user.suspended();
        }
        return true;
    }

    private static String name(UserDto user) {
        return user.displayName() == null || user.displayName().length() == 0
                ? user.username() : user.displayName();
    }

    /// Blocked outranks flagged, and either outranks a role.
    static Label state(UserDto user) {
        if (user.suspended()) {
            return Ui.badge(Lang.tr("Blocked"), "SvBadgeBad");
        }
        if (user.flagged()) {
            return Ui.badge(Lang.tr("Flagged"), "SvBadgeWarn");
        }
        // Staff by what they do; the highest role says the most.
        if (user.owner()) {
            return Ui.badge(Lang.tr("Owner"), "SvBadgeGood");
        }
        if (user.trainer()) {
            return Ui.badge(Lang.tr("Trainer"), "SvBadgeGood");
        }
        if (user.desk()) {
            return Ui.badge(Lang.tr("Front desk"), "SvBadgeGood");
        }
        return null;
    }

    // ---------------------------------------------------------------- account

    /// One account in full: who it is, what it has done here, its roles, and
    /// what owners have done to it.
    static void showAccount(final Form previous, final String username) {
        final Form form = Ui.form("Account", "OwnerAccount");
        Ui.back(form, previous);
        final Container page = Ui.page();
        page.setName("account");
        page.add(new Skeleton(6));
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.admin().user(username, Net.to(found -> fillAccount(form, page,
                found, load[0]), Ui.failed(page, load[0])));
        Ui.pull(page, load[0]);
        form.add(BorderLayout.CENTER, page);
        form.show();
        load[0].run();
    }

    private static void fillAccount(final Form form, Container page, UserDetailDto detail,
            final Runnable load) {
        page.removeAll();
        final UserDto user = detail.user();
        Container head = new Container(new BorderLayout(), "SvPanel");
        head.setName("accountHeader");
        head.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(Ui.avatar(name(user))));
        Container names = new Container(BoxLayout.y());
        names.add(Ui.plain(name(user), "SvHeading"));
        names.add(Ui.plain(user.username(), "SvMuted"));
        if (user.phone() != null && user.phone().length() > 0) {
            names.add(Ui.plain(user.phone() + (user.phoneVerified() ? "" : Ui.DOT
                    + Lang.tr("not verified")), "SvMuted"));
        }
        // Under the name rather than beside it, where it cut the address short.
        Label state = state(user);
        if (state != null) {
            state.getAllStyles().setMarginLeft(0);
            names.add(FlowLayout.encloseIn(state));
        }
        head.add(BorderLayout.CENTER, names);
        page.add(head);
        if (user.flagged() && user.flagReason() != null && user.flagReason().length() > 0) {
            page.add(Ui.callout(FontImage.MATERIAL_FLAG, Lang.tr("Flagged"), user.flagReason()));
        }

        StatRow tiles = Ui.stats(3);
        tiles.stat(String.valueOf(detail.visits()), "Visits", "accountVisits")
                .stat(String.valueOf(detail.noShows()), "No-shows", "accountNoShows")
                .stat(Ui.moneyShort(detail.spent(), null), "Spent", "accountSpent");
        page.add(tiles);

        // Roles: an owner cannot take their own owner role away, which would
        // leave this screen with nobody able to give it back.
        Ui.section(page, "Roles");
        boolean self = Session.user() != null
                && user.username().equals(Session.user().username());
        final boolean[] roles = {user.trainer(), user.desk(), user.owner()};
        String[] titles = {"Trainer", "Front desk", "Owner"};
        String[] about = {"Teaches classes, sees rosters and the live view.",
            "Checks people in at the door and takes walk-ins.",
            "Runs the studio: everything on these screens."};
        for (int iter = 0; iter < roles.length; iter++) {
            final int role = iter;
            Switch toggle = Ui.toggle("accountRole-" + iter, roles[iter], on -> {
                roles[role] = on;
                Api.admin().setRoles(user.username(), new RoleChangeDto(roles[0], roles[1],
                        roles[2]), Net.to(saved -> load.run(), (status, message) -> {
                            Ui.fail(status, message);
                            load.run();
                        }));
            });
            toggle.setEnabled(!(self && iter == 2));
            Container row = Ui.row((char) 0, Lang.tr(titles[iter]), Lang.tr(about[iter]),
                    toggle);
            page.add(row);
        }

        List<MemberDto> household = detail.household();
        if (household != null && household.size() > 1) {
            Ui.section(page, "Household");
            for (int iter = 0; iter < household.size(); iter++) {
                MemberDto member = household.get(iter);
                String who = "SELF".equals(member.relation()) ? Lang.tr("Account holder")
                        : Lang.tr("Child") + Ui.DOT + Lang.tr("Age {0}",
                        Integer.valueOf(member.age()));
                page.add(MembersForm.person(member.name(), who, member.waiverSigned() ? null
                        : Ui.badge(Lang.tr("No waiver"), "SvBadgeWarn")));
            }
        }

        Ui.section(page, "History");
        List<ModerationEventDto> events = detail.events();
        for (int iter = 0; events != null && iter < events.size(); iter++) {
            ModerationEventDto event = events.get(iter);
            String reason = event.reason() == null || event.reason().length() == 0 ? ""
                    : Ui.DOT + event.reason();
            page.add(Ui.row(icon(event.action()), Lang.tr(verb(event.action())),
                    Ui.when(event.at()) + Ui.DOT + event.by() + reason, null));
        }
        if (events == null || events.isEmpty()) {
            page.add(Ui.text("Nothing done to this account yet.", "SvMuted"));
        }

        if (!self) {
            page.add(user.flagged()
                    ? Ui.secondary("Clear the flag", "unflag", e -> flag(user, false, "", load))
                    : Ui.secondary("Flag for a closer look", "flag", e -> Ui.reason(form,
                    Lang.tr("Flag account"), Lang.tr("Other owners see the flag and why. The "
                    + "account keeps working."), Lang.tr("Flag"), why -> {
                        form.showBack();
                        flag(user, true, why, load);
                    })));
            page.add(user.suspended()
                    ? Ui.secondary("Lift the block", "unblock", e -> block(user, false, "", load))
                    : Ui.danger("Block account", "block", e -> Ui.reason(form,
                    Lang.tr("Block account"), Lang.tr("They are signed out at once and cannot "
                    + "sign in, book or check in until the block is lifted."), Lang.tr("Block"),
                    why -> {
                        form.showBack();
                        block(user, true, why, load);
                    })));
        }
        Ui.refresh(form);
    }

    private static void flag(UserDto user, boolean on, String why, final Runnable load) {
        Api.admin().flag(user.username(), new FlagDto(on, why), Net.to(saved -> load.run(),
                Ui::fail));
    }

    private static void block(UserDto user, boolean on, String why, final Runnable load) {
        Api.admin().suspend(user.username(), new SuspendDto(on, why), Net.to(saved -> load.run(),
                Ui::fail));
    }

    private static char icon(String action) {
        if ("block".equals(action)) {
            return FontImage.MATERIAL_BLOCK;
        }
        if ("unblock".equals(action)) {
            return FontImage.MATERIAL_LOCK_OPEN;
        }
        if ("flag".equals(action) || "unflag".equals(action)) {
            return FontImage.MATERIAL_FLAG;
        }
        if ("roles".equals(action)) {
            return FontImage.MATERIAL_BADGE;
        }
        return FontImage.MATERIAL_HISTORY;
    }

    private static String verb(String action) {
        if ("block".equals(action)) {
            return "Blocked";
        }
        if ("unblock".equals(action)) {
            return "Block lifted";
        }
        if ("flag".equals(action)) {
            return "Flagged";
        }
        if ("unflag".equals(action)) {
            return "Flag cleared";
        }
        if ("roles".equals(action)) {
            return "Roles changed";
        }
        if ("approve".equals(action)) {
            return "Approved";
        }
        if ("reject".equals(action)) {
            return "Rejected";
        }
        return action;
    }
}
