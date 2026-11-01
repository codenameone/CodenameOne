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
package com.codenameone.examples.pulse.member;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.pulse.net.Session;
import com.codenameone.examples.pulse.api.UserDto;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.Sheet;
import com.codename1.ui.Button;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.pulse.api.FeedItemDto;
import com.codenameone.examples.pulse.api.FriendDto;
import com.codenameone.examples.pulse.api.FriendRequestDto;
import com.codenameone.examples.pulse.api.InviteDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// Friends at the studio: asking someone by email, answering those who asked,
/// what friends have been doing (with kudos for it), and inviting a friend to
/// a class one is going to.
public final class SocialForm {
    /// The friendship states the server reports.
    public static final String FRIENDS = "FRIENDS";
    public static final String REQUESTED = "REQUESTED";
    public static final String INCOMING = "INCOMING";
    private static final long DAY = 24L * 3600000L;

    private SocialForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Friends", "Social");
        Ui.back(form, previous);
        final Container list = Ui.list("social");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.social().friends(Net.to(friends -> Api.social().feed(Net.to(
                feed -> fill(form, list, friends, feed, load[0]), Ui.failed(list, load[0]))),
                Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.show();
        load[0].run();
    }

    private static void fill(final Form form, Container list, List<FriendDto> friends,
            List<FeedItemDto> feed, final Runnable load) {
        list.removeAll();
        Ui.section(list, "Friends");
        for (int iter = 0; friends != null && iter < friends.size(); iter++) {
            list.add(friend(form, friends.get(iter), iter, load));
        }
        if (friends == null || friends.isEmpty()) {
            list.add(Ui.text("No friends yet. Training is better together.", "SvMuted"));
        }

        Ui.section(list, "Feed");
        UserDto me = Session.user();
        for (int iter = 0; feed != null && iter < feed.size(); iter++) {
            list.add(feedRow(feed.get(iter), iter, me, load));
        }
        if (feed == null || feed.isEmpty()) {
            list.add(Ui.text("Your friends' classes and workouts appear here.", "SvMuted"));
        }

        Ui.section(list, "Add a friend");
        final TextField email = Ui.field(list, "Their email", "friendEmail",
                "friend@example.com", TextArea.EMAILADDR);
        list.add(Ui.secondary("Send request", "requestFriend", e -> {
            String address = email.getText().trim();
            if (address.indexOf('@') < 1) {
                Ui.say(Lang.tr("Enter their email address."));
                return;
            }
            Api.social().request(new FriendRequestDto(address), Net.to(sent -> {
                Ui.say(Lang.tr("Request sent"));
                load.run();
            }, Ui::fail));
        }));
        Ui.refresh(form);
    }

    // A friend: a tap offers what can be done with them, and a request that
    // came in has its Accept right on the row.
    private static Container friend(final Form form, final FriendDto friend, int iter,
            final Runnable load) {
        boolean incoming = INCOMING.equals(friend.state());
        String detail = incoming ? Lang.tr("Wants to be friends")
                : REQUESTED.equals(friend.state()) ? Lang.tr("Asked, waiting for them")
                : friend.weekStreak() > 0 ? Lang.tr("{0} week streak",
                        Integer.valueOf(friend.weekStreak())) : Lang.tr("Getting started");
        Component end = null;
        if (incoming) {
            Button accept = new Button(Lang.tr("Accept"), "SvPillBook");
            accept.setName("accept-" + iter);
            accept.addActionListener(e -> Api.social().accept(friend.account(),
                    Net.to(done -> load.run(), Ui::fail)));
            end = accept;
        } else if (FRIENDS.equals(friend.state())) {
            Button invite = new Button(Lang.tr("Invite"), "SvPillQueue");
            invite.setName("invite-" + iter);
            invite.addActionListener(e -> invite(form, friend));
            end = invite;
        }
        Container row = new Container(new BorderLayout(), "SvRow");
        row.setName("friend-" + iter);
        row.add(BorderLayout.WEST, Ui.avatar(friend.name()));
        Container lines = new Container(BoxLayout.y(), "SvAvatarLines");
        lines.add(Ui.label(friend.name(), "SvRowTitle"));
        lines.add(Ui.label(detail, "SvRowDetail"));
        Container east = new Container(new FlowLayout(Component.RIGHT, Component.CENTER));
        if (end != null) {
            east.add(end);
        }
        // The rarer actions are a tap on the friend, not a button of their
        // own, which left too little room for the name.
        row.add(BorderLayout.CENTER, Ui.tap(lines, "remove-" + iter, e -> {
            final Sheet sheet = new Sheet(null, friend.name());
            sheet.getContentPane().setLayout(BoxLayout.y());
            sheet.getContentPane().add(Ui.link(incoming ? "Decline" : "Remove friend",
                    "confirm-remove-" + iter, ev -> {
                        sheet.back();
                        Api.social().remove(friend.account(), Net.to(done -> load.run(),
                                Ui::fail));
                    }));
            sheet.show();
        }));
        row.add(BorderLayout.EAST, east);
        return row;
    }

    // One thing a friend did. Kudos is a pill with the count on it: a tap
    // gives kudos once, and after that it shows they were given.
    private static Container feedRow(final FeedItemDto item, int iter, UserDto me,
            final Runnable load) {
        boolean mine = me != null && item.name() != null && item.name().equals(me.displayName());
        StringBuilder detail = new StringBuilder(Ui.when(item.at()));
        if (item.minutes() > 0) {
            detail.append(Ui.DOT).append(Lang.tr("{0} min", Integer.valueOf(item.minutes())));
        }
        Container row = new Container(new BorderLayout(), "SvRow");
        row.setName("feed-" + iter);
        row.add(BorderLayout.WEST, Ui.avatar(item.name()));
        Container lines = new Container(BoxLayout.y(), "SvAvatarLines");
        lines.add(Ui.text(Lang.tr(mine ? "You did {1}" : "{0} did {1}",
                first(item.name()), WorkoutsForm.title(item.title())), "SvRowTitle"));
        // When on one line and the effort on the next, as on the workouts
        // screen: a single line wrapped mid-way on a phone.
        lines.add(Ui.plain(detail.toString(), "SvRowDetail"));
        if (item.avgHr() > 0) {
            lines.add(Ui.plain(Lang.tr("{0} bpm avg", Integer.valueOf(item.avgHr())),
                    "SvRowDetail"));
        }
        row.add(BorderLayout.CENTER, lines);
        final Button kudos = new Button(String.valueOf(item.kudos()),
                mine ? "SvKudosMine" : item.kudoed() ? "SvKudosGiven" : "SvKudos");
        Ui.icon(kudos, item.kudoed() ? FontImage.MATERIAL_FAVORITE
                : FontImage.MATERIAL_FAVORITE_BORDER, 2.8f);
        kudos.setName("kudos-" + iter);
        kudos.setAccessibilityText(Lang.tr("{0} kudos", Integer.valueOf(item.kudos())));
        if (!item.kudoed() && !mine) {
            kudos.addActionListener(e -> {
                kudos.setEnabled(false);
                Api.social().kudos(item.id(), Net.to(done -> load.run(), Ui::fail));
            });
        } else {
            kudos.setEnabled(false);
        }
        row.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(kudos));
        return row;
    }

    private static String first(String name) {
        int space = name == null ? -1 : name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name == null ? "" : name;
    }

    /// Picks one of the classes the member is booked into and invites
    /// `friend` to it.
    private static void invite(final Form previous, final FriendDto friend) {
        final Form form = Ui.form(Lang.tr("Invite {0}", friend.name()), "Invite");
        Ui.back(form, previous);
        final Container list = Ui.list("inviteClasses");
        form.add(BorderLayout.CENTER, list);
        form.show();
        Api.studio().classes("", System.currentTimeMillis(), 7, Net.to(classes -> {
            list.removeAll();
            Ui.section(list, "Which class?");
            for (int iter = 0; classes != null && iter < classes.size(); iter++) {
                final SessionDto session = classes.get(iter);
                if (Classes.CANCELLED.equals(session.state())
                        || session.startsAt() > System.currentTimeMillis() + 7 * DAY) {
                    continue;
                }
                boolean going = session.mine() != null && !session.mine().isEmpty();
                list.add(Ui.tap(Ui.row(FontImage.MATERIAL_FITNESS_CENTER, session.name(),
                        Classes.detail(session), going ? Ui.badge(Lang.tr("You are going"),
                        "SvBadgeGood") : null), "inviteTo-" + iter,
                        e -> Api.social().invite(new InviteDto(session.id(), friend.account()),
                                Net.to(sent -> {
                                    Ui.say(Lang.tr("{0} is invited", friend.name()));
                                    previous.showBack();
                                }, Ui::fail))));
            }
            Ui.refresh(form);
        }, Ui.failed(list, () -> invite(previous, friend))));
    }
}
