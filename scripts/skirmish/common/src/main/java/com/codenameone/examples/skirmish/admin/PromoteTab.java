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
package com.codenameone.examples.skirmish.admin;

import com.codename1.analytics.invite.Invite;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Label;
import com.codenameone.examples.skirmish.api.BroadcastDto;
import com.codenameone.examples.skirmish.api.InviterDto;
import com.codenameone.examples.skirmish.api.PromoteDto;
import com.codenameone.examples.skirmish.api.SpotlightDto;
import com.codenameone.examples.skirmish.engage.Friends;
import com.codenameone.examples.skirmish.forge.Fields;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.ui.Ui;
import java.util.Date;

/// The console's Promote tab: how a game developer brings players in and back.
///
/// The reports, the query tool and the pipelines have pages of their own;
/// this one is the hand-sent side. The numbers here are the game's own
/// economy: who joined through an invite and who was paid for it. Behaviour
/// inside the app, and what each campaign brought back, is in Codename One
/// analytics.
///
/// - **Broadcast** sends a Codename One push to a segment of players, with the
///   screen a tap opens and a campaign name. The app reports the tap under
///   that campaign, so the console shows what each broadcast brought back.
/// - **Spotlight** is the banner on every player's home screen.
/// - **Campaign link** makes an invite link for an advert or a post, with no
///   player's code in it; everyone it brings in is reported under its
///   campaign.
// tag::skirmish-promote-tab[]
final class PromoteTab {
    private static final String[] SEGMENTS = {"all", "lapsed", "new", "invited"};
    private static final String[] SEGMENT_NAMES = {"Everyone", "Away for a while", "Joined recently",
        "Came through an invite"};
    static final String[] SCREENS = {"", "shop", "forge", "friends", "deck", "battle"};
    static final String[] SCREEN_NAMES = {"Home", "Shop", "Forge", "Friends", "Deck", "Battle"};

    private final Container col = Desk.page();
    private String segment = SEGMENTS[0];
    private int days = 7;
    private String title = "";
    private String body = "";
    private String screen = "";
    private String campaign = "";
    private String spotText = "";
    private String spotPack = "";
    private String spotScreen = "";
    private String linkCampaign = "launch";

    PromoteTab() {
        col.setName("promote");
    }

    Container column() {
        return col;
    }

    void refresh() {
        Api.admin().promote(Net.to(p -> Ui.later(() -> show(p)), Ui::failed));
    }

    private void show(PromoteDto p) {
        col.removeAll();
        broadcastComposer();
        if (!p.broadcasts().isEmpty()) {
            col.add(Desk.section("Sent"));
            col.add(Desk.tableRow(true, "Title", "Campaign", "Players", "Sent"));
            SimpleDateFormat fmt = new SimpleDateFormat("MMM d, HH:mm");
            for (BroadcastDto b : p.broadcasts()) {
                col.add(Desk.tableRow(false, b.title(), b.campaign(), String.valueOf(b.targets()),
                        fmt.format(new Date(b.sentAt()))));
            }
        }
        spotlightEditor(p.spotlight());
        campaignLink();
        col.add(Desk.section("Invitations"));
        col.add(Desk.cards(AdminConsole.wide() ? 4 : 2,
                Desk.card("Players", String.valueOf(p.players()), null),
                Desk.card("New this week", String.valueOf(p.joinedThisWeek()), null),
                Desk.card("Invited", String.valueOf(p.invited()), "joined through a link"),
                Desk.card("Played", String.valueOf(p.invitedPlayed()), "of those, fought a battle")));
        if (!p.inviters().isEmpty()) {
            col.add(Desk.tableRow(true, "Top inviters", "Joined", "Played"));
            for (InviterDto i : p.inviters()) {
                col.add(Desk.tableRow(false, i.displayName(), String.valueOf(i.joined()), String.valueOf(i.played())));
            }
        }
        col.revalidate();
    }

    private void broadcastComposer() {
        col.add(Desk.section("Broadcast a push"));
        Container form = Desk.panel();
        form.add(Desk.field("To", Desk.choice("promoteSegment", SEGMENT_NAMES, Fields.indexOf(SEGMENTS, segment),
                i -> segment = SEGMENTS[i])));
        form.add(Desk.field("Days", Desk.number("promoteDays", days, v -> days = v)));
        form.add(Desk.field("Title", Desk.text("promoteTitle", title, "Season two is here", 60, v -> title = v)));
        form.add(Desk.field("Message", Desk.area("promoteBody", body, 200, v -> body = v)));
        form.add(Desk.field("Opens", Desk.choice("promoteScreen", SCREEN_NAMES, Fields.indexOf(SCREENS, screen),
                i -> screen = SCREENS[i])));
        form.add(Desk.field("Campaign", Desk.text("promoteCampaign", campaign, "season-two", 40,
                v -> campaign = v)));
        form.add(Desk.help("Days counts back from today: how long a lapsed player has been away, or how "
                + "recently a new one joined. The campaign names this push in analytics. For a push that goes "
                + "out by itself whenever a player qualifies, make a pipeline."));
        form.add(Desk.actions(
                Desk.button("Count players", "promoteCount", e -> Api.admin().audience(draft(), Net.to(
                        b -> Ui.later(() -> Ui.toast(b.targets() + " players would get this")), Ui::failed))),
                Desk.primary("Send", "promoteSend", e -> send())));
        col.add(form);
    }

    private BroadcastDto draft() {
        return new BroadcastDto(null, segment, days, title.trim(), body.trim(), screen, campaign.trim(), 0,
                null, 0L);
    }

    /// Counted first and confirmed: a push to every player cannot be taken
    /// back, and the server allows only a few a day.
    private void send() {
        final BroadcastDto b = draft();
        Api.admin().audience(b, Net.to(counted -> Ui.later(() -> {
            if (!Dialog.show("Send push", "Send \"" + b.title() + "\" to " + counted.targets() + " players?",
                    "Send", "Cancel")) {
                return;
            }
            Api.admin().broadcast(b, Net.to(sent -> Ui.later(() -> {
                Ui.toast("Sent to " + sent.targets() + " players");
                title = "";
                body = "";
                campaign = "";
                refresh();
            }), Ui::failed));
        }), Ui::failed));
    }

    private void spotlightEditor(SpotlightDto current) {
        if (current != null) {
            spotText = current.text() == null ? "" : current.text();
            spotPack = current.packId() == null ? "" : current.packId();
            spotScreen = current.screen() == null ? "" : current.screen();
        }
        col.add(Desk.section("Home spotlight"));
        Container form = Desk.panel();
        form.add(Desk.field("Banner", Desk.text("spotlightText", spotText, "", 140, v -> spotText = v)));
        form.add(Desk.field("Forge pack", Desk.text("spotlightPack", spotPack, "pack id", 40, v -> spotPack = v)));
        form.add(Desk.field("Opens", Desk.choice("spotlightScreen", SCREEN_NAMES, Fields.indexOf(SCREENS, spotScreen),
                i -> spotScreen = SCREENS[i])));
        form.add(Desk.help("Shown at the top of every player's home screen. A pack id wins over the "
                + "screen. Save an empty banner to take it down."));
        form.add(Desk.actions(Desk.primary("Save spotlight", "spotlightSave", e -> Api.admin().spotlight(
                new SpotlightDto(spotText.trim(), spotPack.trim(), spotScreen, 0L),
                Net.to(saved -> Ui.later(() -> Ui.toast("Spotlight saved")), Ui::failed)))));
        col.add(form);
    }

    private void campaignLink() {
        col.add(Desk.section("Campaign link"));
        Container form = Desk.panel();
        form.add(Desk.field("Campaign", Desk.text("linkCampaign", linkCampaign, "launch", 40,
                v -> linkCampaign = v)));
        form.add(Desk.help("An invite link for an advert, a post or a newsletter. It opens the store for "
                + "anyone without the game, and the players it brings in are reported under its campaign."));
        final Container shown = new Container();
        form.add(shown);
        form.add(Desk.actions(Desk.primary("Make link", "linkMake", e -> {
            String name = linkCampaign.trim();
            if (name.length() == 0) {
                Ui.toast("Name the campaign first");
                return;
            }
            Invite link = Friends.campaignLink(name);
            Display.getInstance().copyToClipboard(link.getUrl());
            shown.removeAll();
            shown.add(Desk.field("Link", new Label(link.getUrl(), "DeskValue")));
            Ui.toast("Link copied");
            col.revalidate();
        })));
        col.add(form);
    }
}
// end::skirmish-promote-tab[]
