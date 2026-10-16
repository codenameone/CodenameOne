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
package com.codenameone.examples.wayline.admin;

import com.codename1.components.SpanLabel;
import com.codename1.components.Switch;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.FlagDto;
import com.codenameone.examples.wayline.api.ModerationEventDto;
import com.codenameone.examples.wayline.api.RoleChangeDto;
import com.codenameone.examples.wayline.api.SuspendDto;
import com.codenameone.examples.wayline.api.UserDetailDto;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.StatRow;
import com.codenameone.examples.wayline.ui.Ui;
import java.util.List;

/// The accounts, and what an admin decides about each: its roles, whether it
/// is flagged for attention, and whether it may be used at all.
///
/// All of it takes effect at once. The server reads an account's roles and its
/// block on every request, not from the token the app is holding, so a driver
/// made an admin has the admin screens the next time their app asks who they
/// are, and a blocked account's next call is refused.
///
/// A flag and a block are both kept with a reason and with who gave it; the
/// account's screen shows that history, newest first.
public final class UsersForm {
    static final String ALL = "all";
    static final String FLAGGED = "flagged";
    private static final String[] FILTERS = {ALL, "riders", "drivers", "admins", FLAGGED,
        "blocked"};
    private static final String[] FILTER_NAMES = {"Everyone", "Riders", "Drivers", "Admins",
        "Flagged", "Blocked"};

    private final Panes panes;
    private final Container list = Ui.list("users");
    private final TextField search;
    private List<UserDto> users;
    private String filter;

    private UsersForm(Panes panes, String filter) {
        this.panes = panes;
        this.filter = filter == null || filter.length() == 0 ? ALL : filter;
        Container top = new Container(BoxLayout.y());
        top.setUIID("WlPage");
        search = new TextField("", Lang.tr("Search by name or e-mail"), 24, TextField.ANY);
        search.setUIID("WlField");
        search.getHintLabel().setUIID("WlFieldHint");
        search.setName("search");
        search.addDataChangedListener((type, index) -> fill());
        top.add(search);
        top.add(Ui.choice("filter", FILTER_NAMES, FILTERS, this.filter, value -> {
            this.filter = value;
            fill();
        }, 3));
        panes.list("People", "Users", Panes.listed(top, list),
                this::load);
    }

    private void load() {
        Api.admin().users(Net.to(found -> {
            users = found;
            fill();
        }, Ui.failed(list, this::load)));
    }

    /// @param filter one of the filters to open on, or empty for everyone
    static void show(Form previous, String filter) {
        show(Panes.phone(previous), filter);
    }

    static void show(Panes panes, String filter) {
        new UsersForm(panes, filter);
    }

    private void fill() {
        list.removeAll();
        String wanted = search.getText().trim();
        int shown = 0;
        for (int iter = 0; users != null && iter < users.size(); iter++) {
            final UserDto user = users.get(iter);
            if (!passes(user) || !matches(user, wanted)) {
                continue;
            }
            shown++;
            Container marks = new Container(new FlowLayout(Label.RIGHT, Label.CENTER));
            if (user.flagged()) {
                marks.add(Ui.badge("Flagged", "WlBadgeWarn"));
            }
            marks.add(Ui.badge(roles(user), user.suspended() ? "WlBadgeBad" : "WlBadge"));
            Container row = Ui.row((char) 0, user.displayName(), user.username(), marks);
            row.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(
                    Ui.avatar(user.displayName())));
            final Container picked = row;
            list.add(Ui.tap(row, "user-" + user.username(), e -> {
                panes.picked(picked);
                open(user.username());
            }));
        }
        if (shown == 0 && users != null) {
            list.add(Ui.empty(FontImage.MATERIAL_PERSON_SEARCH, "Nobody matches", null));
        }
        panes.refresh();
    }

    private boolean passes(UserDto user) {
        if (FLAGGED.equals(filter)) {
            return user.flagged();
        }
        if ("blocked".equals(filter)) {
            return user.suspended();
        }
        if ("admins".equals(filter)) {
            return user.admin();
        }
        if ("drivers".equals(filter)) {
            return user.driver();
        }
        if ("riders".equals(filter)) {
            return !user.driver() && !user.admin();
        }
        return true;
    }

    /// Whether `wanted` is in the name or the address, whatever the case. Both
    /// are compared a letter at a time: a name is not a protocol token, but
    /// folding it by the device's language would still miss an `I` in Turkish.
    private static boolean matches(UserDto user, String wanted) {
        return wanted.length() == 0 || contains(user.displayName(), wanted)
                || contains(user.username(), wanted);
    }

    private static boolean contains(String text, String wanted) {
        if (text == null) {
            return false;
        }
        for (int at = 0; at + wanted.length() <= text.length(); at++) {
            if (text.regionMatches(true, at, wanted, 0, wanted.length())) {
                return true;
            }
        }
        return false;
    }

    static String roles(UserDto user) {
        if (user.suspended()) {
            return "Blocked";
        }
        return user.admin() ? "Admin" : user.driver() ? "Driver" : "Rider";
    }

    /// One account in full, fetched each time it is shown.
    private void open(final String username) {
        Api.admin().user(username, Net.to(detail -> show(detail), Ui::fail));
    }

    private void show(final UserDetailDto detail) {
        final UserDto user = detail.user();
        Container page = Ui.page();

        Label standing = Ui.badge(user.suspended() ? "Blocked" : user.flagged() ? "Flagged"
                : "In good standing", user.suspended() ? "WlBadgeBad" : user.flagged()
                ? "WlBadgeWarn" : "WlBadgeGood");
        standing.setName("standing");
        page.add(Ui.person(user.displayName(), user.username(), standing));
        if (user.flagged() && user.flagReason() != null && user.flagReason().length() > 0) {
            SpanLabel why = Ui.text(user.flagReason(), "WlNotice");
            why.setName("flagReason");
            page.add(why);
        }

        StatRow numbers = Ui.stats(3);
        numbers.stat(String.valueOf(detail.rides()), "Rides", "userRides");
        numbers.stat(String.valueOf(detail.cancellations()), "Cancelled",
                "userCancellations");
        numbers.stat(detail.rating() > 0 ? Ui.tenths(detail.rating()) : "-", "Rating",
                "userRating");
        page.add(numbers);
        StatRow money = Ui.stats(2);
        money.stat(Ui.money(detail.spent(), null), "Spent riding", "userSpent");
        money.stat(Ui.money(detail.earned(), null), "Earned driving", "userEarned");
        page.add(money);

        Ui.section(page, "Contact");
        page.add(Ui.row(FontImage.MATERIAL_PHONE, user.phone(),
                user.phoneVerified() ? "Verified" : "Not verified", null));
        if (user.driver()) {
            page.add(Ui.row(FontImage.MATERIAL_DIRECTIONS_CAR, user.vehicle(), user.plate(), null));
        }

        Ui.section(page, "Access");
        final Switch driver = toggle("driver", user.driver());
        final Switch admin = toggle("admin", user.admin());
        page.add(Ui.row(FontImage.MATERIAL_DIRECTIONS_CAR, "Driver",
                "May go online and take rides", driver));
        page.add(Ui.row(FontImage.MATERIAL_INSIGHTS, "Admin",
                "May see and change everything here", admin));
        // A change the server refuses -- an admin removing their own role, say
        // -- is put back, so the switches always show what is true. Putting
        // one back is itself a change, which `settling` keeps from being sent.
        final boolean[] settling = new boolean[1];
        // The roles as the server last confirmed them: driver, admin.
        final boolean[] held = {user.driver(), user.admin()};
        final Runnable roles = () -> {
            if (settling[0]) {
                return;
            }
            RoleChangeDto change = new RoleChangeDto(driver.isValue(), admin.isValue());
            Api.admin().setRoles(user.username(), change, Net.to(saved -> {
                held[0] = saved.driver();
                held[1] = saved.admin();
            }, (status, message) -> {
                Ui.fail(status, message);
                settling[0] = true;
                driver.setValue(held[0]);
                admin.setValue(held[1]);
                settling[0] = false;
            }));
        };
        driver.addChangeListener(e -> roles.run());
        admin.addChangeListener(e -> roles.run());

        Ui.section(page, "Moderation");
        page.add(Ui.text("A flag marks the account for the admins and changes nothing for "
                + "its owner. A block signs it out and refuses it until lifted.", "WlMuted"));
        Container actions = new Container(new GridLayout(1, 2));
        actions.add(pair("WlPairLeft", user.flagged()
                ? Ui.secondary("Remove flag", "unflag", e -> flag(user, false, ""))
                : Ui.secondary("Flag", "flag", e -> panes.reason("Flag account",
                        Lang.tr("Why does {0} need the admins' attention?", user.displayName()),
                        "Flag", why -> flag(user, true, why)))));
        actions.add(pair("WlPairRight", user.suspended()
                ? Ui.secondary("Unblock", "unblock", e -> block(user, false, ""))
                : Ui.danger("Block", "block", e -> panes.reason("Block account",
                        Lang.tr("{0} is signed out everywhere and cannot sign in again "
                                + "until the block is lifted.", user.displayName()), "Block",
                        why -> block(user, true, why)))));
        page.add(actions);

        Ui.section(page, "History");
        List<ModerationEventDto> events = detail.events();
        if (events == null || events.isEmpty()) {
            page.add(Ui.label("Nothing has been done to this account.", "WlMuted"));
        }
        for (int iter = 0; events != null && iter < events.size(); iter++) {
            ModerationEventDto event = events.get(iter);
            Container row = Ui.row(icon(event.action()), action(event.action()),
                    event.reason() == null || event.reason().length() == 0 ? null : event.reason(),
                    null);
            row.setName("event-" + iter);
            Container when = new Container(BoxLayout.y());
            when.add(Ui.plain(Ui.when(event.at()), "WlRowDetail"));
            when.add(Ui.plain(event.by(), "WlRowDetail"));
            row.add(BorderLayout.EAST, when);
            page.add(row);
        }
        panes.detail(user.displayName(), "User", page);
    }

    private void flag(UserDto user, boolean flagged, String why) {
        FlagDto change = new FlagDto(flagged, why);
        Api.admin().flag(user.username(), change, Net.to(saved -> {
            Telemetry.userFlagged(flagged);
            open(user.username());
        }, Ui::fail));
    }

    private void block(UserDto user, boolean blocked, String why) {
        SuspendDto change = new SuspendDto(blocked, why);
        Api.admin().suspend(user.username(), change, Net.to(saved -> {
            Telemetry.userBlocked(blocked);
            open(user.username());
        }, Ui::fail));
    }

    private static Container pair(String uiid, Button button) {
        Container side = new Container(new BorderLayout());
        side.setUIID(uiid);
        side.add(BorderLayout.CENTER, button);
        return side;
    }

    private static char icon(String action) {
        return "block".equals(action) ? FontImage.MATERIAL_BLOCK
                : "unblock".equals(action) ? FontImage.MATERIAL_LOCK_OPEN
                : "flag".equals(action) ? FontImage.MATERIAL_FLAG
                : "unflag".equals(action) ? FontImage.MATERIAL_OUTLINED_FLAG
                : "approve".equals(action) ? FontImage.MATERIAL_CHECK_CIRCLE
                : "reject".equals(action) ? FontImage.MATERIAL_CANCEL
                : FontImage.MATERIAL_MANAGE_ACCOUNTS;
    }

    private static String action(String action) {
        return "block".equals(action) ? "Blocked" : "unblock".equals(action) ? "Unblocked"
                : "flag".equals(action) ? "Flagged" : "unflag".equals(action) ? "Flag removed"
                : "approve".equals(action) ? "Approved to drive" : "reject".equals(action)
                ? "Application sent back" : "Roles changed";
    }

    private static Switch toggle(String name, boolean value) {
        Switch toggle = new Switch();
        toggle.setName(name);
        toggle.setValue(value);
        return toggle;
    }
}
