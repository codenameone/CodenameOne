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
package com.codenameone.examples.skirmish.ui;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.skirmish.api.FriendDto;
import com.codenameone.examples.skirmish.api.InviteDto;
import com.codenameone.examples.skirmish.engage.Friends;
import com.codenameone.examples.skirmish.engage.Telemetry;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import java.util.List;

/// Invite a friend: the player's link, what an invite pays, and everyone who
/// joined through it.
///
/// The link itself is made on the device by Codename One's invite links
/// ([Friends#share]); the server only knows the code inside it, and pays
/// both players once the friend has played a battle.
public final class FriendsForm {
    private FriendsForm() {
    }

    public static void show() {
        final Form f = Ui.form("Friends", "friends");
        Ui.back(f, Nav::home);
        Telemetry.screen("friends");
        final Container col = Ui.column();
        f.add(Ui.centeredColumn(col));
        f.show();
        Api.social().invite(Net.to(mine -> Ui.later(() -> {
            fillInvite(col, mine);
            f.revalidate();
            Api.social().friends(Net.to(list -> Ui.later(() -> {
                fillFriends(col, list);
                f.revalidate();
            }), Ui::failed));
        }), Ui::failed));
    }

    private static void fillInvite(Container col, final InviteDto mine) {
        col.add(Ui.label("Invite a friend", "SectionTitle"));
        col.add(Ui.text("Your friend starts with " + mine.friendCoins() + " coins. When they have played "
                + "their first battle, you get " + mine.inviterCoins() + " coins, for up to "
                + mine.maxRewarded() + " friends.", "ForgeHelpText"));
        Label code = Ui.label(mine.code(), "InviteCode");
        code.setName("inviteCode");
        col.add(Ui.centered(code));
        col.add(Ui.wood("Invite friends", "inviteButton", e -> Friends.share(mine)));
        Label count = Ui.label(mine.joined() + " joined, " + mine.rewarded() + " rewarded", "ArenaName");
        count.setName("inviteCount");
        col.add(count);
    }

    private static void fillFriends(Container col, List<FriendDto> list) {
        col.add(Ui.label("Joined through you", "SectionTitle"));
        if (list.isEmpty()) {
            col.add(Ui.label("Nobody yet. Share your link!", "Empty"));
            return;
        }
        for (FriendDto d : list) {
            Container c = new Container(new BorderLayout());
            c.setUIID("AchievementRow");
            Label icon = new Label("", "Container");
            icon.setIcon(FontImage.createMaterial(d.rewarded() ? FontImage.MATERIAL_MONETIZATION_ON
                    : FontImage.MATERIAL_HOURGLASS_EMPTY, d.rewarded() ? "RewardAchievement" : "XpText", 6));
            c.add(BorderLayout.WEST, icon);
            Container text = new Container(BoxLayout.y());
            text.add(new Label(d.displayName(), "QuestTitle"));
            text.add(new Label(d.trophies() + " trophies, "
                    + ("played".equals(d.state()) ? "has played" : "has not played yet"), "XpText"));
            c.add(BorderLayout.CENTER, text);
            col.add(c);
        }
    }
}
