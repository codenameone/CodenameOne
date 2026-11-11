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
package com.codenameone.examples.splat.ui;

import com.codename1.components.SpanButton;
import com.codename1.io.Preferences;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codenameone.examples.splat.api.ClaimDto;
import com.codenameone.examples.splat.api.PlayerDto;
import com.codenameone.examples.splat.api.ProgressDto;
import com.codenameone.examples.splat.api.QuestDto;
import com.codenameone.examples.splat.api.SpotlightDto;
import com.codenameone.examples.splat.engage.Ads;
import com.codenameone.examples.splat.engage.Telemetry;
import com.codenameone.examples.splat.net.Api;
import com.codename1.appkit.Net;
import com.codenameone.examples.splat.net.Session;
import com.codenameone.examples.splat.play.OperatorStage;

/// The main menu, laid out like the front end of every military shooter: your
/// operator standing in the light on the right, a column of capitalised
/// orders down the left, your rank and wallet in the corner, and the daily
/// orders and supply drops along the bottom.
// tag::splat-home[]
public final class HomeForm {
    private HomeForm() {
    }

    /// The banner the operator sets from the console's Promote tab, as of the
    /// last time home was shown.
    private static SpotlightDto spotlight;

    public static void show() {
        final PlayerDto p = Session.user();
        Form f = Ui.form("", "home");
        f.setLayout(new LayeredLayout());
        f.getToolbar().hideToolbar();
        f.setScrollable(false);
        f.setUIID("MenuForm");
        boolean wide = CN.getDisplayWidth() > CN.getDisplayHeight();
        f.add(new OperatorStage(OperatorStage.operatorFor(p.username()), p.runner(), wide ? 0.9f : 0.55f));
        Label shade = new Label();
        shade.setUIID("MenuShade");
        f.add(shade);
        final Container front = new Container(new BorderLayout());
        front.setSafeArea(true);
        f.add(front);
        fill(f, front, p, Rewards.progress());
        f.show();
        Api.social().spotlight(Net.to(found -> {
            spotlight = found;
            if (Session.user() != null && CN.getCurrentForm() == f) {
                fill(f, front, Session.user(), Rewards.progress());
                f.revalidate();
            }
        }, (s, m) -> { }));
        Rewards.refresh(() -> {
            if (Session.user() == null || CN.getCurrentForm() != f) {
                return;
            }
            ProgressDto now = Rewards.progress();
            if (now != null) {
                Telemetry.describe(now.level(), Session.user().trophies());
            }
            fill(f, front, Session.user(), now);
            f.revalidate();
            offerTutorial(now);
        });
    }

    /// A new player is walked through basic training before anything else.
    /// Offered once per account on this device.
    private static void offerTutorial(ProgressDto now) {
        if (now == null || now.tutorialDone()) {
            return;
        }
        String key = "tutorialOffered-" + Session.user().username();
        if (Preferences.get(key, false)) {
            return;
        }
        Preferences.set(key, true);
        MatchForm.practice("lot7:TEAM_PAINT", Nav::home);
    }

    private static void fill(Form f, Container front, PlayerDto p, ProgressDto g) {
        front.removeAll();

        // Top: the game's mark on the left, your rank card on the right.
        Container top = new Container(new BorderLayout());
        top.setUIID("MenuTop");
        Container mark = BoxLayout.encloseY(new Label("SPLAT", "MenuLogo"),
                new Label("MULTIPLAYER  //  PAINTBALL", "MenuLogoSub"));
        top.add(BorderLayout.CENTER, mark);
        Container card = new Container(BoxLayout.y());
        card.setUIID("RankCard");
        Container nameRow = new Container(new FlowLayout(Component.RIGHT));
        nameRow.add(new Label(Ui.caps(p.displayName()), "PlayerName"));
        if (g != null) {
            Label lvl = new Label(String.valueOf(g.level()), "LevelBadge");
            lvl.setName("level");
            nameRow.add(lvl);
        }
        card.add(nameRow);
        int into = g == null ? 0 : g.xp() - g.levelStart();
        int span = g == null ? 1 : g.levelEnd() - g.levelStart();
        card.add(Ui.bar(into, Math.max(1, span), 0xf2b632));
        Label league = new Label(Ui.caps(leagueName(p.trophies())), "LeagueName");
        Label trophies = new Label(String.valueOf(p.trophies()), "Trophies");
        trophies.setName("Trophies");
        Container wallet = new Container(new FlowLayout(Component.RIGHT));
        Button coins = Ui.button("CP " + p.coins(), "Coins", "Coins", e -> ShopForm.show());
        Button gems = Ui.button("GB " + p.gems(), "Gems", "Gems", e -> ShopForm.show());
        Button gear = Ui.button("", "IconButton", "settingsButton", e -> SettingsForm.show());
        gear.setMaterialIcon(FontImage.MATERIAL_SETTINGS, 3.6f);
        wallet.add(league).add(trophies).add(coins).add(gems).add(gear);
        card.add(wallet);
        top.add(BorderLayout.EAST, card);
        front.add(BorderLayout.NORTH, top);

        // Left: the menu, one order per line, the first one primed.
        Container menu = new Container(BoxLayout.y());
        menu.setUIID("MenuColumn");
        if (p.suspended()) {
            menu.add(Ui.text("ACCOUNT SUSPENDED. MATCHES AND LEADERBOARDS ARE CLOSED UNTIL IT IS LIFTED.",
                    "Warning"));
        }
        menu.add(item("FIND A MATCH", "Quick play, 4v4, any yard", "quickRaceButton", true,
                () -> MatchLobby.quick(null)));
        menu.add(item("OPERATIONS", "Modes, yards, practice vs bots", "runButton", false, PlayForm::show));
        menu.add(item("PARTY UP", "Private lobby with your squad", "partyButton", false,
                () -> MatchLobby.host(null)));
        menu.add(item("MISSION BUILDER", "Design a field, play it with friends", "builderButton", false,
                MissionsForm::show));
        menu.add(item("OPERATOR", "Marker and kit", "loadoutButton", false, LoadoutForm::show));
        int ready = 0;
        if (g != null) {
            for (QuestDto q : g.quests()) {
                if (!q.claimed() && q.progress() >= q.goal()) {
                    ready++;
                }
            }
        }
        final ProgressDto prog = g;
        menu.add(item("ORDERS" + (ready > 0 ? "  [" + ready + "]" : ""), "Daily challenges", "missionsButton", false,
                () -> missions(prog)));
        menu.add(item("BARRACKS", "Medals and service record", "achievementsButton", false,
                AchievementsForm::show));
        menu.add(item("REPLAYS", "Watch it back. It was a bounce.", "replaysButton", false, HistoryForm::show));
        menu.add(item("LEADERBOARDS", "Who paints the most", "leadersButton", false, LeaderboardForm::show));
        menu.add(item("SOCIAL", "Friends", "friendsButton", false, FriendsForm::show));
        menu.add(item("CREWS", "Clans, but with juice boxes", "crewsButton", false, CrewsForm::show));
        menu.add(item("STORE", "Camo that is clearly a hunting jacket", "shopButton", false, ShopForm::show));
        if (p.admin()) {
            menu.add(item("COMMAND", "Admin console", "adminButton", false, Nav::adminConsole));
        }
        if (g != null && g.chests() > 0) {
            // in the list, not under it: the bottom strip is the news and the
            // ad, and a third row there was cut off on a phone
            menu.add(supplyDrops(g));
        }
        Container left = new Container(new BorderLayout());
        left.add(BorderLayout.NORTH, menu);
        left.setScrollableY(true);
        front.add(BorderLayout.WEST, left);

        // Bottom: news, supply drops, the ad.
        Container bottom = new Container(BoxLayout.y());
        bottom.setUIID("MenuBottom");
        Component spot = spotlightBanner();
        if (spot != null) {
            bottom.add(spot);
        }
        Component banner = Ads.banner(Ui.adFree());
        if (banner != null) {
            bottom.add(banner);
        }
        front.add(BorderLayout.SOUTH, bottom);
    }

    /// One line of the menu: the order in capitals and, under it, what it
    /// does. The primed one carries the amber bar.
    private static Component item(String title, String sub, String name, boolean primed, final Runnable go) {
        Button b = Ui.button(title, primed ? "MenuItemTextPrimed" : "MenuItemText", name, e -> go.run());
        // a phone held upright has room for the whole list only without the
        // second lines, and so does a desktop window under 1000 pixels once
        // the fonts are at their desktop size; the primed order keeps its own
        boolean tall = CN.isDesktop() ? CN.getDisplayHeight() < 1000
                : CN.getDisplayHeight() > CN.getDisplayWidth() && !CN.isTablet();
        Container c = tall && !primed ? BoxLayout.encloseY(b)
                : BoxLayout.encloseY(b, new Label(Ui.caps(sub), "MenuItemSub"));
        c.setUIID(primed ? "MenuItemPrimed" : "MenuItem");
        c.setLeadComponent(b);
        return c;
    }

    /// Supply drops waiting: one button that opens the next, with the count.
    private static Container supplyDrops(ProgressDto g) {
        Container c = new Container(new FlowLayout());
        c.setName("chests");
        final Button b = Ui.button("OPEN SUPPLY DROP" + (g.chests() > 1 ? "  x" + g.chests() : ""), "CrateSlot",
                "chest0", null);
        b.addActionListener(e -> {
            b.setEnabled(false);
            Api.progress().openChest(Net.to(r -> Ui.later(() -> Rewards.show(r, HomeForm::show)),
                    (s, m) -> Ui.later(() -> Ui.failed(s, m))));
        });
        c.add(b);
        return c;
    }

    /// Today's quests in a sheet over home, each claimable the moment it is done.
    private static void missions(ProgressDto g) {
        Dialog d = new Dialog("DAILY ORDERS", BoxLayout.y());
        d.setName("missions");
        d.getContentPane().setScrollableY(true);
        if (g == null || g.quests().isEmpty()) {
            d.add(new Label("No orders. Stand easy.", "Muted"));
        } else {
            for (QuestDto q : g.quests()) {
                d.add(quest(q));
            }
        }
        Button close = Ui.button("DISMISSED", "DialogAction", "closeMissions", e -> d.dispose());
        d.add(close);
        d.showPacked(BorderLayout.CENTER, true);
    }

    private static Container quest(final QuestDto q) {
        Container card = new Container(new BorderLayout());
        card.setUIID("QuestCard");
        card.setName("quest-" + q.id());
        Container text = new Container(BoxLayout.y());
        text.add(new Label(q.title(), "QuestTitle"));
        Container barRow = new Container(new BorderLayout());
        barRow.add(BorderLayout.CENTER, Ui.bar(q.progress(), q.goal(), 0xffc94a));
        barRow.add(BorderLayout.EAST, new Label(" " + q.progress() + "/" + q.goal(), "XpText"));
        text.add(barRow);
        text.add(new Label("+" + q.coins() + " coins  +" + q.xp() + " XP", "QuestReward"));
        card.add(BorderLayout.CENTER, text);
        if (q.claimed()) {
            Label done = new Label("Done", "Claimed");
            done.setMaterialIcon(FontImage.MATERIAL_CHECK_CIRCLE);
            card.add(BorderLayout.EAST, FlowLayout.encloseMiddle(done));
        } else if (q.progress() >= q.goal()) {
            Button claim = Ui.button("Claim", "ClaimButton", "claim-" + q.id(), null);
            claim.addActionListener(e -> {
                claim.setEnabled(false);
                Api.progress().claimQuest(new ClaimDto(q.id()), Net.to(r -> Ui.later(() -> Rewards.show(r,
                        HomeForm::show)), (s, m) -> Ui.later(() -> Ui.failed(s, m))));
            });
            card.add(BorderLayout.EAST, FlowLayout.encloseMiddle(claim));
        }
        return card;
    }

    // tag::splat-spotlight[]
    /// The spotlight: one line of news from the operator, and a tap that goes
    /// where it is about -- a Forge pack, or a screen of the game.
    private static Component spotlightBanner() {
        final SpotlightDto s = spotlight;
        if (s == null || s.text() == null || s.text().length() == 0) {
            return null;
        }
        // A SpanButton, so a long line wraps instead of being cut off.
        SpanButton b = new SpanButton(s.text());
        b.setUIID("Spotlight");
        b.setTextUIID("SpotlightText");
        b.setIconUIID("SpotlightIcon");
        b.setName("spotlight");
        b.setMaterialIcon(FontImage.MATERIAL_CAMPAIGN, 4);
        // a span measures its first layout at its unwrapped width, so the
        // strip under the menu was sized for one line and cut the second;
        // a news line is at most two, so ask for two from the start; a wide
        // screen fits it on one, and two would leave an empty band
        boolean wide = CN.getDisplayWidth() > CN.convertToPixels(150);
        b.getTextComponent().setRows(wide ? 1 : 2);
        b.getTextComponent().setGrowByContent(true);
        b.addActionListener(e -> {
            Nav.screen(s.screen() == null ? "" : s.screen());
        });
        return b;
    }
    // end::splat-spotlight[]

    /// The league a trophy count has reached. Cosmetic only: a quick race
    /// fills with whoever is waiting, not by league.
    static String leagueName(int trophies) {
        if (trophies < 300) {
            return "Private, Weekend Division";
        }
        if (trophies < 800) {
            return "Corporal of the Car Park";
        }
        if (trophies < 1500) {
            return "Sergeant, Snack Bar Ops";
        }
        return "Commander of the Cul-de-Sac";
    }
}
// end::splat-home[]
