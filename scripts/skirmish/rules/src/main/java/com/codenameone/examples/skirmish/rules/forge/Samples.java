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
package com.codenameone.examples.skirmish.rules.forge;

import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.Cards;
import com.codenameone.examples.skirmish.rules.Deck;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.MatchRules;
import com.codenameone.examples.skirmish.rules.TargetKind;

import java.util.ArrayList;
import java.util.List;

/// Packs made in code: the game's own campaign, which the server publishes as
/// official content, and the empty pack a creator starts from.
///
/// The official campaign is built exactly as a creator would build one in the
/// Forge -- nothing here is beyond the editor -- so it doubles as the worked
/// example of every feature a pack has.
public final class Samples {
    public static final String OFFICIAL_ID = "border-war";
    private static final int T = Arena.TILE;
    private static final int S = Match.TICKS_PER_SECOND;

    private Samples() {
    }

    /// A new pack with one mission on the classic field against the bot, so a
    /// creator can press Play before changing anything.
    public static ContentPack blank(String id, String author) {
        ContentPack p = new ContentPack();
        p.id = id;
        p.name = "My pack";
        p.author = author == null ? "" : author;
        MissionDef m = new MissionDef();
        m.id = "first-mission";
        m.name = "First mission";
        m.briefing = "Beat the bot on the classic field.";
        m.enemyDeck = Deck.starter();
        p.missions.add(m);
        return p;
    }

    /// The Border War: three missions and the campaign that tells their story.
    public static ContentPack official() {
        ContentPack p = new ContentPack();
        p.id = OFFICIAL_ID;
        p.name = "The Border War";
        p.author = "Skirmish";
        p.description = "Elda's campaign: hold the ford, break the line and face the stone golem.";

        p.cards.add(new CardDef("stone-golem", "Stone Golem", CardDef.TROOP, 7)
                .hp(5200).damage(320).hitTicks(36).range(1000).speed(24).buildingsOnly().radius(800)
                .model("barbarian").tint(0x9a9a9a).scalePercent(130)
                .description("Slow, enormous and only interested in towers."));
        p.cards.add(new CardDef("bone-archers", "Bone Archers", CardDef.TROOP, 3)
                .hp(220).damage(80).hitTicks(18).range(5000).speed(50)
                .targets(TargetKind.GROUND_AND_AIR).count(3).radius(350)
                .model("skeleton_rogue").tint(0xd8d0c0)
                .description("Three archers that crumble at the first touch."));
        p.cards.add(new CardDef("watchtower", "Watchtower", CardDef.TOWER, 0)
                .hp(1600).damage(55).hitTicks(16).range(6500).sight(6500)
                .targets(TargetKind.GROUND_AND_AIR).radius(1300)
                .description("A light tower: less to break, still dangerous."));

        p.arenas.add(threeFords());

        p.missions.add(holdTheFord());
        p.missions.add(breakTheLine());
        p.missions.add(theGolem());

        CampaignDef c = new CampaignDef();
        c.id = "border-war";
        c.name = "The Border War";
        c.intro = "Raiders have crossed the river. Elda needs a commander.";
        c.outro = "The border is quiet again. Elda will remember this.";
        c.missions.add("hold-the-ford");
        c.missions.add("break-the-line");
        c.missions.add("the-golem");
        c.finaleStars = 3;
        p.campaigns.add(c);
        return p;
    }

    private static Arena threeFords() {
        int w = 20 * T;
        int h = 34 * T;
        List<Arena.Tower> towers = new ArrayList<Arena.Tower>();
        Arena.mirrored(towers, h, Cards.KING_TOWER, 10 * T, 31 * T, true);
        Arena.mirrored(towers, h, Cards.PRINCESS_TOWER, 3 * T + T / 2, 27 * T, false);
        Arena.mirrored(towers, h, "watchtower", 10 * T, 26 * T, false);
        Arena.mirrored(towers, h, Cards.PRINCESS_TOWER, 16 * T + T / 2, 27 * T, false);
        List<Arena.Obstacle> rocks = new ArrayList<Arena.Obstacle>();
        rocks.add(new Arena.Obstacle("rock", 7 * T, 21 * T, 700));
        rocks.add(new Arena.Obstacle("rock", 13 * T, 13 * T, 700));
        rocks.add(new Arena.Obstacle("tree", 13 * T, 21 * T, 600));
        rocks.add(new Arena.Obstacle("tree", 7 * T, 13 * T, 600));
        return new Arena("three-fords", "Three Fords", "meadow", w, h, 16 * T, 18 * T,
                new int[] {3 * T + T / 2, 10 * T, 16 * T + T / 2}, T, Arena.sortByOwner(towers), rocks);
    }

    private static String[] playerDeck() {
        return new String[] {"squire", "twin-archers", "spearmen", "ogre",
            "sharpshooter", "bat-swarm", "fire-orb", "arrow-rain"};
    }

    private static DialogStep line(int second, String text) {
        DialogStep d = new DialogStep();
        d.tick = second * S;
        d.text = text;
        return d;
    }

    private static MissionDef holdTheFord() {
        MissionDef m = new MissionDef();
        m.id = "hold-the-ford";
        m.name = "Hold the Ford";
        m.briefing = "Raiders are coming in waves. Keep your king standing for ninety seconds.";
        m.playerDeck = playerDeck();
        m.seconds = 90;
        m.objective = MatchRules.DEFEND;
        m.objectiveCount = 3;
        m.playerStartElixir = 7;
        m.kingsAsleep = false;
        int[] left = {3 * T + T / 2, 10 * T};
        int[] right = {14 * T + T / 2, 10 * T};
        for (int k = 0; k < 6; k++) {
            int at = (8 + k * 13) * S;
            int[] lane = k % 2 == 0 ? left : right;
            m.waves.add(new MatchRules.Wave(at, 1, k < 3 ? "spearmen" : "squire", lane[0], lane[1]));
            if (k >= 2) {
                m.waves.add(new MatchRules.Wave(at + 2 * S, 1, "twin-archers", lane[0], lane[1] - T));
            }
        }
        m.waves.add(new MatchRules.Wave(80 * S, 1, "ogre", 9 * T, 9 * T));
        m.dialog.add(line(0, "They're crossing at both bridges. Hold until the horns sound!"));
        DialogStep ask = line(7, "Spearmen on the left. Meet them with your squire.");
        ask.playCard = "squire";
        ask.targetX = 3 * T + T / 2;
        ask.targetY = 22 * T;
        m.dialog.add(ask);
        m.dialog.add(line(78, "Their ogre! Everything you have, now."));
        m.twoStarTowers = 2;
        m.rewardCoins = 120;
        m.rewardXp = 40;
        return m;
    }

    private static MissionDef breakTheLine() {
        MissionDef m = new MissionDef();
        m.id = "break-the-line";
        m.name = "Break the Line";
        m.briefing = "Three fords, three towers. Topple one of theirs within two minutes.";
        m.arena = "three-fords";
        m.playerDeck = new String[] {"squire", "bone-archers", "spearmen", "ogre",
            "sharpshooter", "bat-swarm", "fire-orb", "arrow-rain"};
        m.enemyDeck = new String[] {"squire", "twin-archers", "spearmen", "imp-den",
            "sharpshooter", "drake", "fire-orb", "arrow-rain"};
        m.botThinkTicks = 14;
        m.seconds = 120;
        m.objective = MatchRules.RAID;
        m.objectiveCount = 1;
        m.dialog.add(line(0, "Three crossings this time. Their middle tower is the weakest."));
        m.threeStarSeconds = 75;
        m.rewardCoins = 160;
        m.rewardXp = 60;
        return m;
    }

    private static MissionDef theGolem() {
        MissionDef m = new MissionDef();
        m.id = "the-golem";
        m.name = "The Stone Golem";
        m.briefing = "Their warlord brought a golem. Win the battle; mind the giant.";
        m.arena = "three-fords";
        m.enemyDeck = new String[] {"stone-golem", "bone-archers", "spearmen", "imp-den",
            "sharpshooter", "drake", "fire-orb", "arrow-rain"};
        m.botThinkTicks = 10;
        m.enemyElixirPercent = 120;
        m.waves.add(new MatchRules.Wave(5 * S, 1, "stone-golem", 10 * T, 9 * T));
        DialogStep d = line(4, "That's the golem. It only wants towers -- swarm it.");
        d.speaker = "elda";
        m.dialog.add(d);
        m.twoStarTowers = 3;
        m.rewardCoins = 250;
        m.rewardXp = 100;
        return m;
    }
}
