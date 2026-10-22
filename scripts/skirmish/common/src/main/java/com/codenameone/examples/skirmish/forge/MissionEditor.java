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
package com.codenameone.examples.skirmish.forge;

import com.codename1.ui.CheckBox;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.Deck;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.MatchRules;
import com.codenameone.examples.skirmish.rules.forge.ContentPack;
import com.codenameone.examples.skirmish.rules.forge.DialogStep;
import com.codenameone.examples.skirmish.rules.forge.MissionDef;
import com.codenameone.examples.skirmish.rules.forge.Validator;
import com.codenameone.examples.skirmish.ui.CardArt;
import com.codenameone.examples.skirmish.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// A mission: where it is fought, what winning means, who the enemy is --
/// the bot with a deck, scripted waves on a timeline, or both -- the story
/// told along the way, and what it pays.
public final class MissionEditor {
    private static final String[] OBJECTIVES = {"Win the battle", "Defend the castle", "Raid: win crowns"};
    private static final String[] LANES = {"Left lane", "Middle lane", "Right lane"};
    private static final String[] DEPTHS = {"At the river", "Middle", "Back line"};
    private static final String[] SPEAKERS = {"elda", "knight", "barbarian", "rogue_hooded"};
    private static final String[] SPEAKER_NAMES = {"Elda", "Knight", "Barbarian", "Rogue"};
    private static final String[] BOTS = {"Slow", "Steady", "Quick", "Relentless"};
    private static final int[] BOT_TICKS = {40, 20, 12, 6};

    private final int index;
    private final MissionDef m;

    private MissionEditor(int index) {
        this.index = index;
        this.m = PackEditor.pack.missions.get(index);
    }

    static void create() {
        MissionDef m = new MissionDef();
        String id = "mission";
        for (int n = 2; PackEditor.pack.mission(id) != null; n++) {
            id = "mission-" + n;
        }
        m.id = id;
        m.name = "New mission";
        m.enemyDeck = Deck.starter();
        PackEditor.pack.missions.add(m);
        new MissionEditor(PackEditor.pack.missions.size() - 1).show();
    }

    static void edit(int index) {
        new MissionEditor(index).show();
    }

    private void show() {
        final ContentPack p = PackEditor.pack;
        Form f = Ui.form("Mission", "missionEditor");
        Ui.back(f, PackEditor::show);
        f.add(Ui.wood("Test play", "missionTest", e -> PackEditor.save(() -> MissionPlay.start(p, m.id, true,
                this::show))));
        f.add(Fields.text("Name", "missionName", m.name, 40, v -> m.name = v));
        f.add(Fields.area("Briefing", "missionBriefing", m.briefing, Validator.MAX_LONG_TEXT, v -> m.briefing = v));

        f.add(Ui.label("The battle", "SectionTitle"));
        final List<String> arenaIds = new ArrayList<String>();
        List<String> arenaNames = new ArrayList<String>();
        arenaIds.add("classic");
        arenaNames.add("Classic field");
        for (Arena a : p.arenas) {
            arenaIds.add(a.id());
            arenaNames.add(a.name());
        }
        f.add(Fields.choice("Arena", "missionArena", arenaNames.toArray(new String[arenaNames.size()]),
                Math.max(0, arenaIds.indexOf(m.arena)), v -> m.arena = arenaIds.get(v)));
        f.add(Fields.choice("Goal", "missionGoal", OBJECTIVES, m.objective, v -> {
            m.objective = v;
            show();
        }));
        if (m.objective == MatchRules.RAID) {
            f.add(Fields.stepper("Crowns to win", "missionCrowns", m.objectiveCount, 1, 3, 1, "",
                    v -> m.objectiveCount = v));
        }
        if (m.objective == MatchRules.DEFEND) {
            f.add(Fields.help("The player wins by keeping their castle standing until the clock runs out."));
        }
        f.add(Fields.stepper("Length", "missionSeconds", m.seconds, 30, 600, 15, " s", v -> m.seconds = v));
        f.add(Fields.stepper("Double elixir from", "missionDouble", m.doubleElixirFromSecond, 0, 600, 15, " s",
                v -> m.doubleElixirFromSecond = v));
        f.add(Fields.stepper("Your starting elixir", "missionElixir0", m.playerStartElixir, 0, 10, 1, "",
                v -> m.playerStartElixir = v));
        f.add(Fields.stepper("Your elixir rate", "missionRate0", m.playerElixirPercent, 25, 400, 25, "%",
                v -> m.playerElixirPercent = v));
        f.add(Fields.check("Castles wake only when hit", "missionAsleep", m.kingsAsleep, v -> m.kingsAsleep = v));

        f.add(Ui.label("Your deck", "SectionTitle"));
        f.add(Fields.check("Fixed deck for this mission", "missionFixedDeck", m.playerDeck != null, v -> {
            m.playerDeck = v ? Deck.starter() : null;
            show();
        }));
        if (m.playerDeck != null) {
            f.add(deckRow(m.playerDeck, "missionPlayerDeck", true));
        } else {
            f.add(Fields.help("The player brings their own deck."));
        }

        f.add(Ui.label("The enemy", "SectionTitle"));
        f.add(Fields.check("The bot plays a deck", "missionBot", m.enemyDeck != null, v -> {
            m.enemyDeck = v ? Deck.starter() : null;
            show();
        }));
        if (m.enemyDeck != null) {
            f.add(deckRow(m.enemyDeck, "missionEnemyDeck", false));
            f.add(Fields.choice("Bot", "missionBotSpeed", BOTS, botIndex(), v -> m.botThinkTicks = BOT_TICKS[v]));
            f.add(Fields.stepper("Enemy starting elixir", "missionElixir1", m.enemyStartElixir, 0, 10, 1, "",
                    v -> m.enemyStartElixir = v));
            f.add(Fields.stepper("Enemy elixir rate", "missionRate1", m.enemyElixirPercent, 25, 400, 25, "%",
                    v -> m.enemyElixirPercent = v));
        }
        waves(f);

        f.add(Ui.label("Story", "SectionTitle"));
        f.add(Fields.help("Lines said during the battle. A line that stops the clock waits for the player "
                + "to tap Next."));
        for (int i = 0; i < m.dialog.size(); i++) {
            final int at = i;
            DialogStep s = m.dialog.get(i);
            f.add(line(clock(s.tick) + "  " + speakerName(s.speaker) + (s.freeze ? "" : " (in passing)"), s.text,
                    "line-" + i, () -> {
                        m.dialog.remove(at);
                        show();
                    }));
        }
        f.add(Ui.stone("Add a line", "addLine", e -> addLine()));

        f.add(Ui.label("Stars and rewards", "SectionTitle"));
        f.add(Fields.stepper("Second star: towers left", "missionTwoStar", m.twoStarTowers, 0, 6, 1, "",
                v -> m.twoStarTowers = v));
        f.add(Fields.stepper("Third star: win within", "missionThreeStar", m.threeStarSeconds, 0, 600, 15, " s",
                v -> m.threeStarSeconds = v));
        f.add(Fields.help("0 seconds: the third star is for losing no tower at all."));
        f.add(Fields.stepper("Coins", "missionCoins", m.rewardCoins, 0, Validator.MAX_REWARD_COINS, 10, "",
                v -> m.rewardCoins = v));
        f.add(Fields.stepper("Experience", "missionXp", m.rewardXp, 0, Validator.MAX_REWARD_XP, 5, "",
                v -> m.rewardXp = v));
        f.add(Fields.help("Paid once per star won. Community missions pay at most a little, so nobody farms "
                + "their own; official ones pay what is set here."));
        f.add(Ui.row(Ui.stone("Done", "missionDone", e -> PackEditor.show()),
                Ui.stone("Delete mission", "missionDelete", e -> {
                    PackEditor.pack.missions.remove(index);
                    PackEditor.show();
                })));
        f.show();
    }

    private int botIndex() {
        for (int i = 0; i < BOT_TICKS.length; i++) {
            if (m.botThinkTicks >= BOT_TICKS[i]) {
                return i;
            }
        }
        return BOT_TICKS.length - 1;
    }

    /// The scripted waves, in time order: each a card the enemy plays at a
    /// second of the battle, whatever the bot is doing.
    private void waves(Form f) {
        f.add(Ui.label("Waves", "SectionTitle"));
        for (int i = 0; i < m.waves.size(); i++) {
            final int at = i;
            MatchRules.Wave w = m.waves.get(i);
            CardDef c = PackEditor.pack.cardSet().get(w.card);
            f.add(line(clock(w.tick) + "  " + (c == null ? w.card : c.name), where(w), "wave-" + i, () -> {
                m.waves.remove(at);
                show();
            }));
        }
        f.add(Ui.stone("Add a wave", "addWave", e -> addWave()));
    }

    private String where(MatchRules.Wave w) {
        Arena a = arena();
        return LANES[Math.min(LANES.length - 1, laneName(a, a.laneOf(w.x)))];
    }

    private static int laneName(Arena a, int lane) {
        if (a.laneCount() == 1) {
            return 1;
        }
        if (a.laneCount() == 2) {
            return lane == 0 ? 0 : 2;
        }
        return lane;
    }

    private Arena arena() {
        Arena a = PackEditor.pack.arena(m.arena);
        return a == null ? Arena.CLASSIC : a;
    }

    private Container line(String title, String detail, String name, final Runnable remove) {
        Container c = new Container(new BorderLayout());
        c.setUIID("ForgeRow");
        c.add(BorderLayout.CENTER, BoxLayout.encloseY(Ui.label(title, "ForgeLabel"), Ui.text(detail, "ForgeHelp")));
        c.add(BorderLayout.EAST, Ui.button("x", "ForgeStep", name + "-remove", e -> remove.run()));
        return c;
    }

    static String clock(int tick) {
        int s = tick / Match.TICKS_PER_SECOND;
        return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
    }

    private static String speakerName(String id) {
        return SPEAKER_NAMES[Fields.indexOf(SPEAKERS, id)];
    }

    private void addWave() {
        final List<CardDef> cards = PackEditor.pack.cardSet().playable();
        final String[] names = new String[cards.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = cards.get(i).name;
        }
        final int[] v = {10, 0, 0, 0};
        final Dialog d = new Dialog("Add a wave", BoxLayout.y());
        d.add(Fields.stepper("At", "waveSecond", v[0], 0, m.seconds, 5, " s", x -> v[0] = x));
        d.add(Fields.choice("Card", "waveCard", names, 0, x -> v[1] = x));
        final Arena a = arena();
        String[] lanes = new String[a.laneCount()];
        for (int i = 0; i < lanes.length; i++) {
            lanes[i] = LANES[laneName(a, i)];
        }
        d.add(Fields.choice("Lane", "waveLane", lanes, 0, x -> v[2] = x));
        d.add(Fields.choice("Where", "waveDepth", DEPTHS, 0, x -> v[3] = x));
        d.add(Ui.wood("Add", "waveAdd", e -> {
            d.dispose();
            // The enemy's half is the top: y runs from its back line to the river.
            int top = a.riverTop();
            int y = v[3] == 0 ? top - Arena.TILE * 2 : v[3] == 1 ? top / 2 : Arena.TILE * 3;
            m.waves.add(insertAt(v[0] * Match.TICKS_PER_SECOND),
                    new MatchRules.Wave(v[0] * Match.TICKS_PER_SECOND, 1, cards.get(v[1]).id, a.laneX(v[2]), y));
            show();
        }));
        d.add(Ui.stone("Cancel", "waveCancel", e -> d.dispose()));
        d.show();
    }

    private int insertAt(int tick) {
        int i = 0;
        while (i < m.waves.size() && m.waves.get(i).tick <= tick) {
            i++;
        }
        return i;
    }

    private void addLine() {
        final DialogStep s = new DialogStep();
        s.text = "";
        final int[] second = {0};
        final Dialog d = new Dialog("Add a line", BoxLayout.y());
        d.add(Fields.stepper("At", "lineSecond", 0, 0, m.seconds, 5, " s", x -> second[0] = x));
        d.add(Fields.choice("Who", "lineSpeaker", SPEAKER_NAMES, 0, x -> s.speaker = SPEAKERS[x]));
        d.add(Fields.area("Says", "lineText", "", Validator.MAX_TEXT, x -> s.text = x));
        CheckBox stop = Fields.check("Stop the clock", "lineFreeze", true, x -> s.freeze = x);
        d.add(stop);
        d.add(Ui.wood("Add", "lineAdd", e -> {
            if (s.text.trim().length() == 0) {
                return;
            }
            d.dispose();
            s.tick = second[0] * Match.TICKS_PER_SECOND;
            int i = 0;
            while (i < m.dialog.size() && m.dialog.get(i).tick <= s.tick) {
                i++;
            }
            m.dialog.add(i, s);
            show();
        }));
        d.add(Ui.stone("Cancel", "lineCancel", e -> d.dispose()));
        d.show();
    }

    /// A deck as a row of small cards; a tap opens the picker.
    private Container deckRow(final String[] deck, String name, final boolean player) {
        Container row = new Container(BoxLayout.x());
        row.setScrollableX(true);
        int w = com.codename1.ui.CN.convertToPixels(9);
        CardArt.use(PackEditor.pack.cardSet());
        for (String id : deck) {
            row.add(new com.codename1.ui.Label(CardArt.card(id, w, false), "Container"));
        }
        CardArt.use(null);
        com.codename1.ui.Button pick = Ui.stone("Choose cards", name, e -> pickDeck(player));
        return BoxLayout.encloseY(row, pick);
    }

    private void pickDeck(final boolean player) {
        final List<CardDef> cards = PackEditor.pack.cardSet().playable();
        String[] now = player ? m.playerDeck : m.enemyDeck;
        final List<String> chosen = new ArrayList<String>();
        for (String s : now) {
            chosen.add(s);
        }
        final Dialog d = new Dialog("Choose " + Deck.SIZE + " cards", BoxLayout.y());
        d.setScrollableY(true);
        for (final CardDef c : cards) {
            CheckBox b = Fields.check(c.name + " (" + c.cost + ")", "pick-" + c.id, chosen.contains(c.id), v -> {
                if (v) {
                    chosen.add(c.id);
                } else {
                    chosen.remove(c.id);
                }
            });
            d.add(b);
        }
        d.add(Ui.wood("Use these", "deckUse", e -> {
            if (chosen.size() != Deck.SIZE) {
                Ui.toast("Pick exactly " + Deck.SIZE + " cards; " + chosen.size() + " are picked");
                return;
            }
            d.dispose();
            String[] deck = chosen.toArray(new String[Deck.SIZE]);
            if (player) {
                m.playerDeck = deck;
            } else {
                m.enemyDeck = deck;
            }
            show();
        }));
        d.add(Ui.stone("Cancel", "deckCancel", e -> d.dispose()));
        d.show();
    }
}
