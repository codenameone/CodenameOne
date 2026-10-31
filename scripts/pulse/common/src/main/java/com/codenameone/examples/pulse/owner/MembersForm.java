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

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.pulse.api.OwnerMemberDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Skeleton;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// The studio's members, searchable by name and narrowed by a row of chips:
/// who is at risk of lapsing, whose card failed, who is frozen, new or gone.
/// A member leads to everything the owner knows about them.
public final class MembersForm {
    /// The chips' filters, as the server names them; empty is everyone.
    private static final String[] FILTERS = {"", "AT_RISK", "PAST_DUE", "FROZEN", "NEW",
        "CANCELLED"};
    private static final String[] FILTER_NAMES = {"All", "At risk", "Past due", "Frozen", "New",
        "Cancelled"};
    private static final long DAY = 86400000L;

    private final Form form = Ui.form("Members", "OwnerMembers");
    private final Container list = Ui.list("members");
    private final TextField search = new TextField("", Lang.tr("Search by name"), 20,
            TextField.ANY);
    private String filter = "";
    private List<OwnerMemberDto> members;

    private MembersForm() {
    }

    public static void show(Form previous) {
        new MembersForm().open(previous);
    }

    private void open(Form previous) {
        Ui.back(form, previous);
        search.setUIID("SvField");
        search.getHintLabel().setUIID("SvFieldHint");
        search.setName("memberSearch");
        search.addDataChangedListener((type, index) -> fill());
        String[] names = new String[FILTERS.length];
        String[] values = new String[FILTERS.length];
        for (int iter = 0; iter < names.length; iter++) {
            names[iter] = Lang.tr(FILTER_NAMES[iter]);
            // A chip's name is `filter-` and this: "ALL" reads better than nothing.
            values[iter] = FILTERS[iter].length() == 0 ? "ALL" : FILTERS[iter];
        }
        Container top = new Container(BoxLayout.y(), "SvSearchBar");
        top.add(search);
        top.add(Ui.pills("filter", names, values, "ALL", value -> {
            filter = "ALL".equals(value) ? "" : value;
            list.removeAll();
            list.add(new Skeleton(5));
            list.revalidate();
            load();
        }));
        form.add(BorderLayout.NORTH, top);
        form.add(BorderLayout.CENTER, list);
        Ui.pull(list, this::load);
        form.show();
        // The list is what an owner came for; the search waits to be tapped
        // rather than showing focused on arrival. Showing focuses the first
        // focusable component, so the focus is let go once that is done.
        CN.callSerially(() -> form.setFocused(null));
        load();
    }

    private void load() {
        final String asked = filter;
        Api.owner().members(asked, Net.to(found -> {
            // A chip tapped while the last answer was on its way wins.
            if (asked.equals(filter)) {
                members = found;
                fill();
            }
        }, Ui.failed(list, this::load)));
    }

    private void fill() {
        if (members == null) {
            return;
        }
        list.removeAll();
        String wanted = search.getText().trim();
        int shown = 0;
        for (int iter = 0; iter < members.size(); iter++) {
            final OwnerMemberDto member = members.get(iter);
            if (!contains(member.name(), wanted)) {
                continue;
            }
            String detail = (member.planName() == null ? "" : member.planName() + Ui.DOT)
                    + lastVisit(member.lastVisit()) + Ui.DOT
                    + Lang.tr("{0} in 30 days", Integer.valueOf(member.visits30()));
            list.add(Ui.tap(person(member.name(), detail, state(member)), "memberRow" + shown,
                    e -> MemberDetailForm.show(form, member.id())));
            shown++;
        }
        if (shown == 0) {
            list.add(Ui.empty(FontImage.MATERIAL_PERSON_SEARCH, wanted.length() > 0
                    ? Lang.tr("Nobody by that name") : Lang.tr("No members here"),
                    Lang.tr("Try another filter.")));
        }
        Ui.refresh(form);
    }

    /// Whether `text` has `part` in it, whatever the case: compared a
    /// character at a time, which no device language changes.
    static boolean contains(String text, String part) {
        if (part.length() == 0) {
            return true;
        }
        if (text == null) {
            return false;
        }
        for (int at = 0; at + part.length() <= text.length(); at++) {
            if (text.regionMatches(true, at, part, 0, part.length())) {
                return true;
            }
        }
        return false;
    }

    /// A row about a person: their initials, their name as it is, a line under
    /// it and `value` at the end.
    static Container person(String name, String detail, Component value) {
        Container row = line(name, detail, value);
        row.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(Ui.avatar(name)));
        return row;
    }

    /// A row of a title, a detail line and a value on the trailing edge, with no avatar.
    static Container line(String name, String detail, Component value) {
        Container row = new Container(new BorderLayout(), "SvRow");
        Container lines = new Container(BoxLayout.y());
        lines.add(Ui.plain(name, "SvRowTitle"));
        if (detail != null && detail.length() > 0) {
            lines.add(Ui.text(detail, "SvRowDetail"));
        }
        row.add(BorderLayout.CENTER, lines);
        if (value != null) {
            row.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(value));
        }
        return row;
    }

    /// The pill that says where a member stands; at risk outranks active.
    static Label state(OwnerMemberDto member) {
        String state = member.state();
        if ("PAST_DUE".equals(state)) {
            return Ui.badge(Lang.tr("Past due"), "SvBadgeBad");
        }
        if ("CANCELLED".equals(state)) {
            return Ui.badge(Lang.tr("Cancelled"), "SvBadge");
        }
        if ("FROZEN".equals(state)) {
            return Ui.badge(Lang.tr("Frozen"), "SvBadge");
        }
        if (member.away()) {
            return Ui.badge(Lang.tr("Away"), "SvBadge");
        }
        if (member.atRisk()) {
            return Ui.badge(Lang.tr("At risk"), "SvBadgeWarn");
        }
        return Ui.badge(Lang.tr("Active"), "SvBadgeGood");
    }

    /// When someone was last in: "Today", "Yesterday", "3 days ago".
    static String lastVisit(long at) {
        if (at <= 0) {
            return Lang.tr("Never in");
        }
        long days = (System.currentTimeMillis() - at) / DAY;
        if (days <= 0) {
            return Lang.tr("In today");
        }
        if (days == 1) {
            return Lang.tr("Yesterday");
        }
        return Lang.tr("{0} days ago", Long.valueOf(days));
    }
}
