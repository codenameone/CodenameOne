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
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.skirmish.AppConfig;
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
/// Nothing here is a home-grown metric. Engagement, retention, the invite
/// funnel and revenue per campaign are what the app reports through Codename
/// One analytics, and are read in the Codename One console, which the first
/// button opens. The server's numbers below are the game's own economy: who
/// joined through an invite and who was paid for it.
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
    private static final String[] SCREENS = {"", "shop", "forge", "friends", "deck", "battle"};
    private static final String[] SCREEN_NAMES = {"Home", "Shop", "Forge", "Friends", "Deck", "Battle"};

    private final Container col;
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

    PromoteTab(Container col) {
        this.col = col;
    }

    Container column() {
        return col;
    }

    void refresh() {
        Api.admin().promote(Net.to(p -> Ui.later(() -> show(p)), Ui::failed));
    }

    private void show(PromoteDto p) {
        col.removeAll();
        col.add(Ui.label("Engagement", "SectionTitle"));
        col.add(Fields.help("Daily players, retention, the invite funnel and revenue per campaign are reported by "
                + "the app through Codename One analytics."));
        col.add(Ui.wood("Open Codename One analytics", "openAnalytics",
                e -> CN.execute(AppConfig.ANALYTICS_CONSOLE)));
        Container numbers = new Container(new GridLayout(2, 2));
        numbers.add(AdminConsole.stat("Players", p.players()));
        numbers.add(AdminConsole.stat("New", p.joinedThisWeek()));
        numbers.add(AdminConsole.stat("Invited", p.invited()));
        numbers.add(AdminConsole.stat("Played", p.invitedPlayed()));
        col.add(numbers);
        col.add(Fields.help("New: joined in the last seven days. Invited: joined through a friend's link. "
                + "Played: of those, played a battle."));
        if (!p.inviters().isEmpty()) {
            col.add(Ui.label("Top inviters", "SectionTitle"));
            for (InviterDto i : p.inviters()) {
                col.add(Ui.label(i.displayName() + ": " + i.joined() + " joined, " + i.played() + " played",
                        "ForgeHelp"));
            }
        }
        broadcastComposer();
        if (!p.broadcasts().isEmpty()) {
            col.add(Ui.label("Sent", "SectionTitle"));
            for (BroadcastDto b : p.broadcasts()) {
                col.add(Ui.label(b.title() + " (" + b.campaign() + ")", "ForgeLabel"));
                col.add(Ui.label(b.targets() + " players, " + b.segment() + ", by " + b.sentBy() + ", "
                        + new Date(b.sentAt()), "ForgeHelp"));
            }
        }
        spotlightEditor(p.spotlight());
        campaignLink();
        col.revalidate();
    }

    private void broadcastComposer() {
        col.add(Ui.label("Broadcast a push", "SectionTitle"));
        col.add(Fields.choice("To", "promoteSegment", SEGMENT_NAMES, Fields.indexOf(SEGMENTS, segment),
                i -> segment = SEGMENTS[i]));
        col.add(Fields.stepper("Days", "promoteDays", days, 1, 365, 1, "",
                v -> days = v));
        col.add(Fields.text("Title", "promoteTitle", title, 60, v -> title = v));
        col.add(Fields.area("Message", "promoteBody", body, 200, v -> body = v));
        col.add(Fields.choice("Opens", "promoteScreen", SCREEN_NAMES, Fields.indexOf(SCREENS, screen),
                i -> screen = SCREENS[i]));
        col.add(Fields.text("Campaign", "promoteCampaign", campaign, 40, v -> campaign = v));
        col.add(Fields.help("Days counts back from today: how long a lapsed player has been away, or how "
                + "recently a new one joined. The campaign names this push in analytics."));
        col.add(Ui.row(
                Ui.stone("Count players", "promoteCount", e -> Api.admin().audience(draft(), Net.to(
                        b -> Ui.later(() -> Ui.toast(b.targets() + " players would get this")), Ui::failed))),
                Ui.wood("Send", "promoteSend", e -> send())));
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
        col.add(Ui.label("Home spotlight", "SectionTitle"));
        col.add(Fields.text("Banner", "spotlightText", spotText, 140, v -> spotText = v));
        col.add(Fields.text("Forge pack", "spotlightPack", spotPack, 40, v -> spotPack = v));
        col.add(Fields.choice("Opens", "spotlightScreen", SCREEN_NAMES, Fields.indexOf(SCREENS, spotScreen),
                i -> spotScreen = SCREENS[i]));
        col.add(Fields.help("Shown at the top of every player's home screen. A pack id wins over the "
                + "screen. Save an empty banner to take it down."));
        col.add(Ui.stone("Save spotlight", "spotlightSave", e -> Api.admin().spotlight(
                new SpotlightDto(spotText.trim(), spotPack.trim(), spotScreen, 0L),
                Net.to(saved -> Ui.later(() -> Ui.toast("Spotlight saved")), Ui::failed))));
    }

    private void campaignLink() {
        col.add(Ui.label("Campaign link", "SectionTitle"));
        col.add(Fields.text("Campaign", "linkCampaign", linkCampaign, 40, v -> linkCampaign = v));
        col.add(Fields.help("An invite link for an advert, a post or a newsletter. It opens the store for "
                + "anyone without the game, and the players it brings in are reported under its campaign."));
        final Container shown = new Container();
        col.add(Ui.stone("Make link", "linkMake", e -> {
            String name = linkCampaign.trim();
            if (name.length() == 0) {
                Ui.toast("Name the campaign first");
                return;
            }
            Invite link = Friends.campaignLink(name);
            Display.getInstance().copyToClipboard(link.getUrl());
            shown.removeAll();
            shown.add(Ui.text(link.getUrl(), "ChallengeUrl"));
            Ui.toast("Link copied");
            col.revalidate();
        }));
        col.add(shown);
    }
}
// end::skirmish-promote-tab[]
