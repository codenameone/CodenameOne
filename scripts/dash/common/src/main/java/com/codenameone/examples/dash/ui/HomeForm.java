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
package com.codenameone.examples.dash.ui;

import com.codename1.components.SpanButton;
import com.codename1.io.Preferences;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.dash.api.ClaimDto;
import com.codenameone.examples.dash.api.PlayerDto;
import com.codenameone.examples.dash.api.ProgressDto;
import com.codenameone.examples.dash.api.QuestDto;
import com.codenameone.examples.dash.api.SpotlightDto;
import com.codenameone.examples.dash.engage.Ads;
import com.codenameone.examples.dash.engage.Telemetry;
import com.codenameone.examples.dash.forge.PackScreen;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.net.Session;

/// The menu between runs, built to pull the player into the next one: their
/// runner and level up top, the run button where the thumb is, a race with
/// friends one tap away, chests waiting to be opened and today's quests with
/// a claim button the moment one is done.
// tag::dash-home[]
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
        f.add(Kit.backdrop());
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

    /// A new player is walked through the tutorial level before anything else.
    /// Offered once per account on this device; "First steps", at the top of
    /// the level list, brings it back.
    private static void offerTutorial(ProgressDto now) {
        if (now == null || now.tutorialDone()) {
            return;
        }
        String key = "tutorialOffered-" + Session.user().username();
        if (Preferences.get(key, false)) {
            return;
        }
        Preferences.set(key, true);
        RunForm.tutorial();
    }

    private static void fill(Form f, Container front, PlayerDto p, ProgressDto g) {
        front.removeAll();
        String runner = Portraits.of(p.runner());

        // Top bar: the avatar ringed by level progress, the name and level
        // over what the player holds, and the settings gear.
        Container top = new Container(new BorderLayout());
        top.setUIID("TopBar");
        int into = g == null ? 0 : g.xp() - g.levelStart();
        int span = g == null ? 1 : g.levelEnd() - g.levelStart();
        top.add(BorderLayout.WEST, Kit.xpRing(Portraits.face(runner, CN.convertToPixels(9)), into, span));
        Container nameRow = new Container(BoxLayout.x());
        nameRow.add(new Label(p.displayName(), "PlayerName"));
        if (g != null) {
            Label lvl = new Label("LV " + g.level(), "LevelBadge");
            lvl.setName("level");
            nameRow.add(lvl);
        }
        Container wallet = new Container(BoxLayout.x());
        wallet.add(Kit.pill(FontImage.MATERIAL_MONETIZATION_ON, p.coins(), "Coins", "Coins", e -> ShopForm.show()));
        wallet.add(Kit.pill(FontImage.MATERIAL_DIAMOND, p.gems(), "Gems", "Gems", e -> ShopForm.show()));
        top.add(BorderLayout.CENTER, BoxLayout.encloseY(nameRow, wallet));
        Button gear = Ui.button("", "IconButton", "settingsButton", e -> SettingsForm.show());
        gear.setMaterialIcon(FontImage.MATERIAL_SETTINGS, 4.2f);
        top.add(BorderLayout.EAST, FlowLayout.encloseMiddle(gear));
        front.add(BorderLayout.NORTH, top);

        // The middle: rails down both sides, the runner on its stage between.
        Container mid = new Container(new BorderLayout());
        int ready = 0;
        if (g != null) {
            for (QuestDto q : g.quests()) {
                if (!q.claimed() && q.progress() >= q.goal()) {
                    ready++;
                }
            }
        }
        final ProgressDto prog = g;
        mid.add(BorderLayout.WEST, Kit.rail(
                Kit.tile("Missions", FontImage.MATERIAL_TASK_ALT, "missionsButton", ready,
                        e -> missions(prog)),
                Kit.tile("Daily", FontImage.MATERIAL_TODAY, "dailyButton", 0, e -> RunForm.daily()),
                Kit.tile("Trophies", FontImage.MATERIAL_MILITARY_TECH, "achievementsButton", 0,
                        e -> AchievementsForm.show()),
                Kit.tile("Replays", FontImage.MATERIAL_REPLAY, "replaysButton", 0, e -> HistoryForm.show())));
        mid.add(BorderLayout.EAST, Kit.rail(
                Kit.tile("Runner", FontImage.MATERIAL_CHECKROOM, "runnerButton", 0, e -> RunnerForm.show()),
                Kit.tile("Friends", FontImage.MATERIAL_GROUP_ADD, "friendsButton", 0, e -> FriendsForm.show()),
                Kit.tile("Crews", FontImage.MATERIAL_GROUPS, "crewsButton", 0, e -> CrewsForm.show()),
                Kit.tile("Leaders", FontImage.MATERIAL_LEADERBOARD, "leadersButton", 0,
                        e -> LeaderboardForm.show())));
        Container center = new Container(new BorderLayout());
        Component spot = spotlightBanner();
        if (p.suspended()) {
            center.add(BorderLayout.NORTH, Ui.text("Your account is suspended. Races and leaderboards are "
                    + "closed to you until it is lifted.", "Warning"));
        }
        Component hero = Kit.stage(Portraits.figure(runner, CN.convertToPixels(26)));
        hero.setName("hero");
        center.add(BorderLayout.CENTER, hero);
        Label league = new Label(leagueName(p.trophies()), "LeagueName");
        league.setMaterialIcon(FontImage.MATERIAL_SHIELD, 3.4f);
        Label trophies = new Label(String.valueOf(p.trophies()), "Trophies");
        trophies.setName("Trophies");
        trophies.setMaterialIcon(FontImage.MATERIAL_EMOJI_EVENTS, 3.4f);
        center.add(BorderLayout.SOUTH, FlowLayout.encloseCenter(league, trophies));
        mid.add(BorderLayout.CENTER, center);
        front.add(BorderLayout.CENTER, mid);

        // The bottom: crates, the big button, races, then the bar.
        Container bottom = new Container(BoxLayout.y());
        bottom.setUIID("HomeBottom");
        if (spot != null) {
            bottom.add(spot);
        }
        if (g != null) {
            bottom.add(chests(g));
        }
        Button run = Ui.button("PLAY", "GoButton", "runButton", e -> LevelsForm.show());
        run.setMaterialIcon(FontImage.MATERIAL_PLAY_ARROW, 6f);
        bottom.add(run);
        Button quick = Ui.button("Quick race", "RaceChip", "quickRaceButton", e -> RaceLobby.quick(null));
        quick.setMaterialIcon(FontImage.MATERIAL_BOLT, 3.6f);
        Button party = Ui.button("Party", "RaceChip", "partyButton", e -> RaceLobby.host(null));
        party.setMaterialIcon(FontImage.MATERIAL_HEADSET_MIC, 3.6f);
        bottom.add(GridLayout.encloseIn(2, quick, party));
        bottom.add(Kit.navBar("homeNav", p.admin()));
        Component banner = Ads.banner(Ui.adFree());
        if (banner != null) {
            bottom.add(banner);
        }
        front.add(BorderLayout.SOUTH, bottom);
    }

    /// Today's quests in a sheet over home, each claimable the moment it is done.
    private static void missions(ProgressDto g) {
        Dialog d = new Dialog("Missions", BoxLayout.y());
        d.setName("missions");
        d.getContentPane().setScrollableY(true);
        if (g == null || g.quests().isEmpty()) {
            d.add(new Label("No missions right now.", "Muted"));
        } else {
            for (QuestDto q : g.quests()) {
                d.add(quest(q));
            }
        }
        Button close = Ui.button("Close", "DialogAction", "closeMissions", e -> d.dispose());
        d.add(close);
        d.showPacked(BorderLayout.CENTER, true);
    }

    /// The four chest slots. A full one opens on a tap; an empty one says how
    /// to fill it.
    private static Container chests(ProgressDto g) {
        Container c = new Container(new GridLayout(1, 4));
        c.setName("chests");
        int size = CN.convertToPixels(5.5f);
        for (int i = 0; i < 4; i++) {
            if (i < g.chests()) {
                Button b = new Button("Open", Portraits.crate(size, false), "CrateSlot");
                b.setName("chest" + i);
                b.setTextPosition(Component.BOTTOM);
                b.addActionListener(e -> {
                    b.setEnabled(false);
                    Api.progress().openChest(Net.to(r -> Ui.later(() -> Rewards.show(r, HomeForm::show)),
                            (s, m) -> Ui.later(() -> Ui.failed(s, m))));
                });
                c.add(b);
            } else {
                Label l = new Label("Empty", "CrateSlotEmpty");
                l.setTextPosition(Component.BOTTOM);
                l.setIcon(FontImage.createMaterial(FontImage.MATERIAL_LOCK, "CrateSlotEmpty", 3.6f));
                c.add(l);
            }
        }
        return c;
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

    // tag::dash-spotlight[]
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
        b.addActionListener(e -> {
            if (s.packId() != null && s.packId().length() > 0) {
                PackScreen.show(s.packId());
            } else {
                Nav.screen(s.screen() == null ? "" : s.screen());
            }
        });
        return b;
    }
    // end::dash-spotlight[]

    /// The league a trophy count has reached. Cosmetic only: a quick race
    /// fills with whoever is waiting, not by league.
    static String leagueName(int trophies) {
        if (trophies < 300) {
            return "Rookie Lane";
        }
        if (trophies < 800) {
            return "Bronze Circuit";
        }
        if (trophies < 1500) {
            return "Neon League";
        }
        return "Skyline Masters";
    }
}
// end::dash-home[]
