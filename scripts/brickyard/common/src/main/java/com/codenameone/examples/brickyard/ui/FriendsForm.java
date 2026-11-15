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
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;

import java.util.List;

/// Friends: the list, adding one by name, and inviting someone new. An
/// invite into a particular experience is on that experience's page.
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
}
