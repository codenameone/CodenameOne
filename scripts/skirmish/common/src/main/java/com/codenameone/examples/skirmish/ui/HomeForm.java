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

import com.codename1.io.Preferences;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.skirmish.api.ClaimDto;
import com.codenameone.examples.skirmish.api.PlayerDto;
import com.codenameone.examples.skirmish.api.ProgressDto;
import com.codenameone.examples.skirmish.api.QuestDto;
import com.codenameone.examples.skirmish.api.SpotlightDto;
import com.codenameone.examples.skirmish.engage.Ads;
import com.codenameone.examples.skirmish.engage.Telemetry;
import com.codenameone.examples.skirmish.forge.ForgeHome;
import com.codenameone.examples.skirmish.forge.PackScreen;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.net.Session;
import java.util.List;

/// The menu between battles, built to pull the player into the next one: their
/// hero and level up top, the battle button where the thumb is, chests waiting
/// to be opened, today's quests with a claim button the moment one is done,
/// and the deck as a row of cards.
// tag::skirmish-home[]
public final class HomeForm {
    private HomeForm() {
    }

    /// The banner the operator sets from the console's Promote tab, as of the
    /// last time home was shown.
    private static SpotlightDto spotlight;

    public static void show() {
        final PlayerDto p = Session.user();
        Form f = Ui.form("", "home");
        f.setLayout(new BorderLayout());
        f.getToolbar().hideToolbar();
        final Container header = new Container(new BorderLayout());
        header.setUIID("HomeHeader");
        // The toolbar is hidden here, so nothing else keeps the header out
        // from under the notch.
        header.setSafeArea(true);
        f.add(BorderLayout.NORTH, header);
        final Container col = Ui.column();
        Container scroll = new Container(BoxLayout.y());
        scroll.setScrollableY(true);
        scroll.add(Ui.centeredColumn(col));
        f.add(BorderLayout.CENTER, scroll);
        Component banner = Ads.banner(Ui.adFree());
        if (banner != null) {
            f.add(BorderLayout.SOUTH, banner);
        }
        fill(f, header, col, p, Rewards.progress());
        f.show();
        Api.social().spotlight(Net.to(found -> {
            spotlight = found;
            if (Session.user() != null && CN.getCurrentForm() == f) {
                fill(f, header, col, Session.user(), Rewards.progress());
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
            fill(f, header, col, Session.user(), now);
            f.revalidate();
            offerTutorial(now);
        });
    }

    /// A new player is walked through a training battle before anything else.
    /// Offered once per account on this device; "How to play" brings it back.
    private static void offerTutorial(ProgressDto now) {
        if (now == null || now.tutorialDone()) {
            return;
        }
        String key = "tutorialOffered-" + Session.user().username();
        if (Preferences.get(key, false)) {
            return;
        }
        Preferences.set(key, true);
        Tutorial.start();
    }

    private static void fill(Form f, Container header, Container col, PlayerDto p, ProgressDto g) {
        header.removeAll();
        col.removeAll();
        header.add(BorderLayout.WEST, new Label(CardArt.face(CardArt.heroOf(p.username()), CN.convertToPixels(11)),
                "Container"));
        Container who = new Container(BoxLayout.y());
        Container nameRow = BoxLayout.encloseX(new Label(p.displayName(), "PlayerNameBig"));
        if (g != null) {
            Label lvl = new Label("Lv " + g.level(), "LevelBadge");
            lvl.setName("level");
            nameRow.add(lvl);
            who.add(nameRow);
            Container xp = new Container(new BorderLayout());
            xp.add(BorderLayout.CENTER, Ui.bar(g.xp() - g.levelStart(), g.levelEnd() - g.levelStart(), 0x7be07b));
            Container pad = BoxLayout.encloseY(xp);
            pad.getAllStyles().setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_DIPS);
            pad.getAllStyles().setPadding(0.6f, 0.6f, 1.5f, 1.5f);
            who.add(pad);
            who.add(new Label((g.xp() - g.levelStart()) + " / " + (g.levelEnd() - g.levelStart()) + " XP", "XpText"));
        } else {
            who.add(nameRow);
        }
        header.add(BorderLayout.CENTER, who);
        Container wallet = new Container(BoxLayout.y());
        wallet.add(stat(FontImage.MATERIAL_EMOJI_EVENTS, p.trophies(), "Trophies"));
        wallet.add(stat(FontImage.MATERIAL_MONETIZATION_ON, p.coins(), "Coins"));
        wallet.add(stat(FontImage.MATERIAL_DIAMOND, p.gems(), "Gems"));
        header.add(BorderLayout.EAST, wallet);

        if (p.suspended()) {
            col.add(Ui.text("Your account is suspended. Online battles are closed to you until it is lifted.",
                    "Warning"));
        }
        Container stage = new Container(new BorderLayout());
        stage.setUIID("HeroStage");
        Label hero = new Label(CardArt.hero(CardArt.heroOf(p.username()), CN.convertToPixels(30)), "Container");
        hero.setName("hero");
        stage.add(BorderLayout.CENTER, FlowLayout.encloseCenter(hero));
        Label arena = new Label(arenaName(p.trophies()), "ArenaName");
        stage.add(BorderLayout.SOUTH, arena);
        col.add(stage);
        Component banner = spotlightBanner();
        if (banner != null) {
            col.add(banner);
        }
        Button battle = Ui.button("BATTLE", "BattleButton", "battleButton", e -> MatchmakingForm.queue());
        battle.setMaterialIcon(FontImage.MATERIAL_SPORTS_ESPORTS);
        col.add(battle);
        col.add(Ui.row(Ui.stone("Practice", "practiceButton", e -> BattleForm.practice()),
                Ui.stone("Challenge", "challengeButton", e -> MatchmakingForm.host())));
        if (g != null) {
            col.add(Ui.label("Chests", "SectionTitle"));
            col.add(chests(g));
            col.add(Ui.label("Daily quests", "SectionTitle"));
            for (QuestDto q : g.quests()) {
                col.add(quest(q));
            }
        }
        col.add(Ui.label("Your deck", "SectionTitle"));
        col.add(deck(p.deck()));
        col.add(Ui.wood("Forge", "forgeButton", e -> ForgeHome.show()));
        Container nav = new Container(new GridLayout(2, 4));
        nav.add(tile("Friends", FontImage.MATERIAL_GROUP_ADD, "friendsButton", e -> FriendsForm.show()));
        nav.add(tile("Deck", FontImage.MATERIAL_STYLE, "deckButton", e -> DeckForm.show()));
        nav.add(tile("Shop", FontImage.MATERIAL_STOREFRONT, "shopButton", e -> ShopForm.show()));
        nav.add(tile("Trophies", FontImage.MATERIAL_MILITARY_TECH, "achievementsButton",
                e -> AchievementsForm.show()));
        nav.add(tile("Replays", FontImage.MATERIAL_REPLAY, "replaysButton", e -> HistoryForm.show()));
        nav.add(tile("Leaders", FontImage.MATERIAL_LEADERBOARD, "leadersButton", e -> LeaderboardForm.show()));
        nav.add(tile("How to play", FontImage.MATERIAL_SCHOOL, "tutorialButton", e -> Tutorial.start()));
        nav.add(tile("Settings", FontImage.MATERIAL_SETTINGS, "settingsButton", e -> SettingsForm.show()));
        col.add(nav);
        if (p.admin()) {
            col.add(Ui.button("Admin console", "AdminButton", "adminButton", e -> Nav.adminConsole()));
        }
    }

    /// The four chest slots. A full one opens on a tap; an empty one says how
    /// to fill it.
    private static Container chests(ProgressDto g) {
        Container c = new Container(new GridLayout(1, 4));
        c.setName("chests");
        int size = CN.convertToPixels(9);
        for (int i = 0; i < 4; i++) {
            if (i < g.chests()) {
                Button b = new Button("Open", CardArt.chest(size, false), "ChestSlot");
                b.setName("chest" + i);
                b.setTextPosition(Component.BOTTOM);
                b.addActionListener(e -> {
                    b.setEnabled(false);
                    Api.progress().openChest(Net.to(r -> Ui.later(() -> Rewards.show(r, HomeForm::show)),
                            (s, m) -> Ui.later(() -> Ui.failed(s, m))));
                });
                c.add(b);
            } else {
                Label l = new Label("Empty", "ChestSlotEmpty");
                l.setTextPosition(Component.BOTTOM);
                l.setIcon(FontImage.createMaterial(FontImage.MATERIAL_LOCK, "ChestSlotEmpty", 5));
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

    private static Container deck(List<String> cards) {
        Container c = new Container(new GridLayout(1, 8));
        c.setUIID("DeckStrip");
        c.setName("deckStrip");
        if (cards != null) {
            int w = Math.min(CN.convertToPixels(10),
                    (Math.min(CN.convertToPixels(Ui.MAX_COLUMN_MM), CN.getDisplayWidth()) - CN.convertToPixels(12)) / 8);
            for (String id : cards) {
                c.add(new Label(CardArt.card(id, w, false), "CardTile"));
            }
        }
        return c;
    }

    // tag::skirmish-spotlight[]
    /// The spotlight: one line of news from the operator, and a tap that goes
    /// where it is about -- a Forge pack, or a screen of the game.
    private static Component spotlightBanner() {
        final SpotlightDto s = spotlight;
        if (s == null || s.text() == null || s.text().length() == 0) {
            return null;
        }
        Button b = Ui.button(s.text(), "Spotlight", "spotlight", e -> {
            if (s.packId() != null && s.packId().length() > 0) {
                PackScreen.show(s.packId());
            } else {
                Nav.screen(s.screen() == null ? "" : s.screen());
            }
        });
        b.setMaterialIcon(FontImage.MATERIAL_CAMPAIGN, 4);
        return b;
    }
    // end::skirmish-spotlight[]

    private static Button tile(String text, char icon, String name,
            com.codename1.ui.events.ActionListener<com.codename1.ui.events.ActionEvent> go) {
        Button b = Ui.button(text, "NavTile", name, go);
        b.setMaterialIcon(icon, 4.5f);
        b.setTextPosition(Component.BOTTOM);
        return b;
    }

    private static Label stat(char icon, int value, String uiid) {
        Label l = new Label(String.valueOf(value), uiid);
        l.setName(uiid);
        l.setMaterialIcon(icon);
        return l;
    }

    /// The arena a trophy count has reached. Cosmetic only: matchmaking
    /// pairs by trophies, not by arena.
    static String arenaName(int trophies) {
        if (trophies < 300) {
            return "Training Grounds";
        }
        if (trophies < 800) {
            return "Goblin Gorge";
        }
        if (trophies < 1500) {
            return "Frost Peak";
        }
        return "Royal Arena";
    }
}
// end::skirmish-home[]
