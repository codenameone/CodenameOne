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

        Ui.section(list, "Friends");
        for (int iter = 0; friends != null && iter < friends.size(); iter++) {
            final FriendDto friend = friends.get(iter);
            String detail = INCOMING.equals(friend.state()) ? Lang.tr("Wants to be friends")
                    : REQUESTED.equals(friend.state()) ? Lang.tr("Asked, waiting for them")
                    : Lang.tr("{0} week streak", Integer.valueOf(friend.weekStreak()));
            list.add(Ui.row(FontImage.MATERIAL_PERSON, friend.name(), detail, null));
            Container actions = new Container(new FlowLayout(Component.RIGHT));
            if (INCOMING.equals(friend.state())) {
                actions.add(Ui.link("Accept", "accept-" + iter,
                        e -> Api.social().accept(friend.account(), Net.to(done -> load.run(),
                                Ui::fail))));
            }
            if (FRIENDS.equals(friend.state())) {
                actions.add(Ui.link("Invite to a class", "invite-" + iter,
                        e -> invite(form, friend)));
            }
            actions.add(Ui.link(INCOMING.equals(friend.state()) ? "Decline" : "Remove",
                    "remove-" + iter, e -> Api.social().remove(friend.account(),
                            Net.to(done -> load.run(), Ui::fail))));
            list.add(actions);
        }
        if (friends == null || friends.isEmpty()) {
            list.add(Ui.text("No friends yet. Training is better together.", "SvMuted"));
        }

        Ui.section(list, "Feed");
        for (int iter = 0; feed != null && iter < feed.size(); iter++) {
            final FeedItemDto item = feed.get(iter);
            StringBuilder detail = new StringBuilder(Ui.when(item.at()));
            if (item.minutes() > 0) {
                detail.append(Ui.DOT).append(Lang.tr("{0} min", Integer.valueOf(item.minutes())));
            }
            if (item.avgHr() > 0) {
                detail.append(Ui.DOT).append(item.avgHr()).append(" bpm");
            }
            list.add(Ui.row(FontImage.MATERIAL_DIRECTIONS_RUN, item.name() + ": " + item.title(),
                    detail.toString(), Ui.badge(String.valueOf(item.kudos()),
                    item.kudoed() ? "SvBadgeGood" : "SvBadge")));
            if (!item.kudoed()) {
                list.add(FlowLayout.encloseRight(Ui.link("Give kudos", "kudos-" + iter,
                        e -> Api.social().kudos(item.id(), Net.to(done -> load.run(),
                                Ui::fail)))));
            }
        }
        if (feed == null || feed.isEmpty()) {
            list.add(Ui.text("Your friends' classes and workouts appear here.", "SvMuted"));
        }
        Ui.refresh(form);
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
