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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.Bot;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.Command;
import com.codenameone.examples.skirmish.rules.Deck;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.MatchRules;
import com.codenameone.examples.skirmish.rules.Replay;
import com.codenameone.examples.skirmish.rules.RulesVersion;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ForgeTest {

    /// Plays a mission with a bot on each side that has a deck, as the app does.
    static Replay play(ContentPack pack, String missionId, long seed) {
        MissionDef m = pack.mission(missionId);
        MatchRules rules = pack.rulesFor(missionId);
        String[] mine = m.playerDeck != null ? m.playerDeck : Deck.starter();
        Match match = new Match(seed, mine, m.enemyDeck, rules);
        Bot me = new Bot(0, seed + 1);
        Bot them = new Bot(1, seed + 2, m.botThinkTicks);
        List<Command> log = new ArrayList<Command>();
        while (!match.isOver()) {
            Command a = me.decide(match, 0);
            if (a != null) {
                log.add(a);
            }
            Command b = them.decide(match, 0);
            if (b != null) {
                log.add(b);
            }
            match.step(log);
        }
        String ref = new ContentRef(pack.id, pack.hash(), missionId).toString();
        return new Replay(seed, mine, m.enemyDeck, log, RulesVersion.ID, ref, rules);
    }

    @Test
    void officialPackIsValid() {
        List<String> problems = Validator.check(Samples.official());
        assertTrue(problems.isEmpty(), problems.toString());
        assertTrue(Validator.isValid(Samples.blank("my-pack", "me")));
    }

    @Test
    void codecRoundTripsExactly() {
        ContentPack p = Samples.official();
        String text = PackCodec.write(p);
        ContentPack back = PackCodec.read(text);
        assertEquals(text, PackCodec.write(back));
        assertEquals(p.hash(), back.hash());
        assertEquals(3, back.missions.size());
        assertEquals(3, back.arenas.get(0).laneCount());
        assertEquals("barbarian", back.cardSet().get("stone-golem").model());
    }

    @Test
    void anyEditChangesTheHash() {
        ContentPack p = Samples.official();
        ContentPack q = p.copy();
        q.mission("hold-the-ford").waves.remove(0);
        assertNotEquals(p.hash(), q.hash());
    }

    @Test
    void missionsAreDeterministic() {
        ContentPack p = Samples.official();
        for (int i = 0; i < p.missions.size(); i++) {
            String id = p.missions.get(i).id;
            Replay r = play(p, id, 77 + i);
            Match a = r.play();
            Match b = Replay.decode(r.encode()).withRules(p.copy().rulesFor(id)).play();
            assertTrue(a.isOver(), id);
            assertEquals(a.stateHash(), b.stateHash(), id);
            assertEquals(a.result(), b.result(), id);
        }
    }

    @Test
    void customReplaysNameTheirContent() {
        ContentPack p = Samples.official();
        Replay r = play(p, "the-golem", 5);
        String text = r.encode();
        assertTrue(text.startsWith(Replay.MAGIC_CONTENT + "|"));
        Replay back = Replay.decode(text);
        ContentRef ref = ContentRef.parse(back.content());
        assertEquals(Samples.OFFICIAL_ID, ref.pack);
        assertEquals(p.hash(), ref.hash);
        assertEquals("the-golem", ref.mission);
    }

    @Test
    void wavesOnlyEnemyRoundTripsThroughReplays() {
        ContentPack p = Samples.official();
        Replay r = play(p, "hold-the-ford", 9);
        Replay back = Replay.decode(r.encode());
        assertEquals(null, back.deck(1));
        assertEquals(r.play().stateHash(), back.withRules(p.rulesFor("hold-the-ford")).play().stateHash());
    }

    @Test
    void defendIsWonByOutlastingTheClock() {
        ContentPack p = Samples.official();
        MatchRules rules = p.rulesFor("hold-the-ford").toBuilder().waves(new ArrayList<MatchRules.Wave>()).build();
        Match m = new Match(1, Deck.starter(), null, rules);
        m.runToEnd(new ArrayList<Command>());
        assertEquals(0, m.result());
        assertEquals(90 * Match.TICKS_PER_SECOND, m.tick());
    }

    @Test
    void raidIsLostWhenTheClockRunsOut() {
        MatchRules rules = new MatchRules.Builder().objective(MatchRules.RAID, 1)
                .regularTicks(40 * Match.TICKS_PER_SECOND).build();
        Match m = new Match(1, Deck.starter(), Deck.starter(), rules);
        m.runToEnd(new ArrayList<Command>());
        assertEquals(1, m.result());
    }

    @Test
    void wavesSpawnWithoutElixirOrHand() {
        MatchRules rules = new MatchRules.Builder()
                .wave(new MatchRules.Wave(3, 1, "ogre", 9 * Arena.TILE, 8 * Arena.TILE)).build();
        Match m = new Match(1, Deck.starter(), null, rules);
        int before = m.units().size();
        for (int i = 0; i < 5; i++) {
            m.step(null);
        }
        assertEquals(before + 1, m.units().size());
        assertEquals("ogre", m.units().get(m.units().size() - 1).card().id);
    }

    @Test
    void classicDecksCannotBeMissing() {
        assertThrows(IllegalArgumentException.class, () -> new Match(1, Deck.starter(), null));
    }

    @Test
    void troopsWalkAroundObstacles() {
        ContentPack p = Samples.official();
        Arena a = p.arena("three-fords");
        Arena.Obstacle rock = a.obstacles().get(0);
        MatchRules rules = new MatchRules.Builder().arena(a).cards(p.cardSet())
                .wave(new MatchRules.Wave(0, 0, "squire", rock.x, rock.y + 3 * Arena.TILE)).build();
        Match m = new Match(3, Deck.starter(), null, rules);
        for (int i = 0; i < 200 && !m.isOver(); i++) {
            m.step(null);
            for (int k = 0; k < m.units().size(); k++) {
                if ("squire".equals(m.units().get(k).card().id) && !m.units().get(k).isDeploying()) {
                    long dx = m.units().get(k).x() - rock.x;
                    long dy = m.units().get(k).y() - rock.y;
                    long min = rock.radius + m.units().get(k).card().radius() - 1;
                    assertTrue(dx * dx + dy * dy >= min * min, "inside the rock at tick " + m.tick());
                }
            }
        }
    }

    @Test
    void elixirRateIsExact() {
        MatchRules rules = new MatchRules.Builder().elixirPercent(0, 150).startElixir(0, 0).startElixir(1, 0).build();
        Match m = new Match(1, Deck.starter(), Deck.starter(), rules);
        for (int i = 0; i < 100; i++) {
            m.step(null);
        }
        assertEquals(150, m.player(0).elixir());
        assertEquals(100, m.player(1).elixir());
    }

    @Test
    void validatorRejectsWhatWouldHurt() {
        ContentPack p = Samples.official();
        p.cards.get(0).hp(1000000);
        p.cards.add(new CardDef("spawner", "Spawner", CardDef.BUILDING, 3).hp(500).spawns("watchtower", 20));
        p.mission("hold-the-ford").waves.add(new MatchRules.Wave(5, 1, "no-such-card", 0, 0));
        p.mission("break-the-line").dialog.get(0).text = "";
        CardDef taken = new CardDef("squire", "Again", CardDef.TROOP, 1).hp(1).speed(10);
        String problems = Validator.check(p).toString();
        assertTrue(problems.contains("hit points"), problems);
        assertTrue(problems.contains("can only spawn a troop"), problems);
        assertTrue(problems.contains("does not exist"), problems);
        assertTrue(problems.contains("text needs"), problems);
        p.cards.add(taken);
        assertFalse(Validator.isValid(p));
    }

    @Test
    void readerRefusesHostileDocuments() {
        assertThrows(IllegalArgumentException.class, () -> PackCodec.read("{\"format\":99}"));
        assertThrows(IllegalArgumentException.class, () -> PackCodec.read("{\"cards\":[{\"hp\":1.5}]}"));
        assertThrows(IllegalArgumentException.class, () -> PackCodec.read("{\"a\":1,\"a\":2}"));
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            deep.append('[');
        }
        assertThrows(IllegalArgumentException.class, () -> Json.parse(deep.toString()));
        assertThrows(IllegalArgumentException.class, () -> PackCodec.read("{\"cards\":[{\"hp\":99999999999}]}"));
    }

    @Test
    void campaignOpensInOrder() {
        CampaignDef c = Samples.official().campaign("border-war");
        assertTrue(c.isOpen(0, new int[] {0, 0, 0}));
        assertFalse(c.isOpen(1, new int[] {0, 0, 0}));
        assertTrue(c.isOpen(1, new int[] {1, 0, 0}));
        assertFalse(c.isOpen(2, new int[] {1, 1, 0}));
        assertTrue(c.isOpen(2, new int[] {2, 1, 0}));
    }

    @Test
    void textOutsideAsciiHashesTheSameEverywhere() {
        ContentPack p = Samples.blank("unicode", "Zoë");
        String text = PackCodec.write(p);
        for (int i = 0; i < text.length(); i++) {
            assertTrue(text.charAt(i) < 0x7f);
        }
        assertEquals("Zoë", PackCodec.read(text).author);
    }

    @Test
    void bundledCharactersPassTheModelCheck() throws Exception {
        java.io.File dir = new java.io.File("../common/src/main/resources");
        GlbInspector.Report knight = GlbInspector.inspect(java.nio.file.Files.readAllBytes(
                new java.io.File(dir, "model_knight.glb").toPath()));
        assertTrue(knight.ok(), knight.problem);
        assertTrue(knight.skinned);
        assertTrue(knight.animations.contains("Idle"), knight.animations.toString());
        byte[] cut = java.util.Arrays.copyOf(java.nio.file.Files.readAllBytes(
                new java.io.File(dir, "model_rock.glb").toPath()), 300);
        assertFalse(GlbInspector.inspect(cut).ok());
        assertFalse(GlbInspector.inspect("not a model at all, really".getBytes("UTF-8")).ok());
        assertEquals("Punch", GlbInspector.clip("idle=Idle;attack=Punch;death=Die", "attack"));
    }

    @Test
    void serverReproducesAMissionResult() {
        ContentPack p = Samples.official();
        MissionRun run = new MissionRun(p, "break-the-line", 21, null);
        Bot me = new Bot(0, 99);
        while (!run.match().isOver()) {
            Command c = me.decide(run.match(), 0);
            if (c != null) {
                run.play(c);
            }
            run.step();
        }
        Replay sent = Replay.decode(run.replay().encode());
        Match again = MissionRun.verify(p.copy(), "break-the-line", sent);
        assertEquals(run.match().stateHash(), again.stateHash());
        assertEquals(run.stars(), p.mission("break-the-line").stars(again));
        // A forged enemy move changes nothing: the server plays side 1 itself.
        List<Command> forged = new ArrayList<Command>(sent.commands());
        forged.add(new Command(5, 1, "fire-orb", 1000, 1000));
        Replay lie = new Replay(sent.seed(), sent.deck(0), sent.deck(1), forged, RulesVersion.ID, sent.content(),
                MatchRules.CLASSIC);
        assertEquals(again.stateHash(), MissionRun.verify(p, "break-the-line", lie).stateHash());
        Replay wrongDeck = new Replay(sent.seed(), Deck.starter(), sent.deck(1), sent.commands());
        assertThrows(IllegalArgumentException.class, () -> MissionRun.verify(p, "break-the-line", wrongDeck));
    }
}
