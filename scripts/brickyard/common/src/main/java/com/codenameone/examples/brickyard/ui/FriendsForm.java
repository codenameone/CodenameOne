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

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.brickyard.api.FriendDto;
import com.codenameone.examples.brickyard.api.PresenceDto;
import com.codename1.ui.Label;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;

import java.util.List;

/// Friends: the list with what each is doing, joining one who is in a game,
/// adding one by name, and inviting someone new. An invite into a particular
/// experience is on that experience's page.
public final class FriendsForm {
    private FriendsForm() {
    }

    public static void show() {
        final Form f = Ui.section("Friends", "friends", 2);
        Container content = Ui.content(f);
        final TextField name = Ui.field("Friend's user name", TextField.ANY, "friendName");
        final Container list = new Container(BoxLayout.y());
        list.setName("friendList");
        content.add(Ui.panel(name, Ui.secondary("Add friend", "addFriendButton", e -> {
            String who = name.getText().trim();
            if (who.length() == 0) {
                return;
            }
            Api.account().addFriend(new FriendDto(who), Net.to(added -> {
                name.setText("");
                load(f, list);
            }, Ui::failed));
        })));
        content.add(Ui.label("Your friends", "RowTitle"));
        content.add(list);
        f.show();
        load(f, list);
    }

    static void load(final Form f, final Container list) {
        Api.account().friends(Net.to(friends -> {
            fill(list, friends);
            f.revalidate();
            Api.social().friendPresence(Net.to(presence -> {
                if (presence != null && friends != null && !friends.isEmpty()) {
                    list.removeAll();
                    for (FriendDto friend : friends) {
                        list.add(row(friend.username(), find(presence, friend.username())));
                    }
                    f.revalidate();
                }
            }, (status, message) -> { }));
        }, (status, message) -> {
            list.removeAll();
            list.add(Ui.text(message, "Muted"));
            f.revalidate();
        }));
    }

    static void fill(Container list, List<FriendDto> friends) {
        list.removeAll();
        if (friends == null || friends.isEmpty()) {
            list.add(Ui.text("No friends yet. Add one by name, or invite one from an experience's page.",
                    "Muted"));
            return;
        }
        for (FriendDto friend : friends) {
            list.add(Ui.panel(Ui.label(friend.username(), "CardTitle")));
        }
    }

    static PresenceDto find(List<PresenceDto> presence, String username) {
        for (PresenceDto p : presence) {
            if (username.equals(p.username())) {
                return p;
            }
        }
        return null;
    }

    static boolean inGame(PresenceDto p) {
        return p != null && "IN_GAME".equals(p.status()) && p.serverId() != null && p.serverId().length() > 0;
    }

    static String status(PresenceDto p) {
        if (inGame(p)) {
            return "Playing";
        }
        return p != null && "ONLINE".equals(p.status()) ? "Online" : "Offline";
    }

    /// A friend's row: their name, what they are doing (the experience's
    /// title once it loads) and, while they are in a game, a Join button.
    static Container row(final String username, final PresenceDto p) {
        final Label what = Ui.label(status(p), "CardMeta");
        if (!inGame(p)) {
            return Ui.panel(Ui.label(username, "CardTitle"), what);
        }
        Api.experiences().experience(p.experienceId(), Net.to(e -> {
            what.setText("Playing " + e.title());
            if (what.getParent() != null) {
                what.getParent().revalidate();
            }
        }, (status, message) -> { }));
        return Ui.panel(Ui.label(username, "CardTitle"), what,
                Ui.primary("Join", "join" + username, e -> join(username)));
    }

    /// Joins `friend` on the server they are playing on, through the
    /// server's join check (friends only, not blocked, in a game now).
    public static void join(String friend) {
        Api.social().joinFriend(friend, Net.to(target -> Api.experiences().experience(target.experienceId(),
                Net.to(e -> ExperienceForm.play(e, target.serverId()), Ui::failed)), Ui::failed));
    }
}
