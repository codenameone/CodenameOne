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
package com.codenameone.examples.skirmish.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchTest {

    /// Plays two bots to the end, recording every command as the server would.
    static Replay botMatch(long seed) {
        Match m = new Match(seed, Deck.starter(), alternateDeck());
        Bot a = new Bot(0, seed + 1);
        Bot b = new Bot(1, seed + 2);
        List<Command> log = new ArrayList<Command>();
        while (!m.isOver()) {
            Command c0 = a.decide(m, 0);
            Command c1 = b.decide(m, 0);
            List<Command> now = new ArrayList<Command>();
            if (c0 != null) {
                now.add(c0);
            }
            if (c1 != null) {
                now.add(c1);
            }
            log.addAll(now);
            m.step(now);
            checkInvariants(m);
        }
        return new Replay(seed, Deck.starter(), alternateDeck(), log);
    }

    static String[] alternateDeck() {
        return new String[] {"spearmen", "drake", "imp-den", "bat-swarm",
            "squire", "ogre", "arrow-rain", "fire-orb"};
    }

    static void checkInvariants(Match m) {
        for (Unit u : m.units()) {
            assertTrue(u.hp() > 0 && u.hp() <= u.maxHp(), "hp of " + u.card().id);
            assertTrue(u.x() >= 0 && u.x() <= Arena.CLASSIC.width(), "x of " + u.card().id);
            assertTrue(u.y() >= 0 && u.y() <= Arena.CLASSIC.height(), "y of " + u.card().id);
        }
        for (int p = 0; p < 2; p++) {
            int e = m.player(p).elixir();
            assertTrue(e >= 0 && e <= PlayerState.MAX_ELIXIR);
        }
    }

    @Test
    void sameInputsSameMatch() {
        Replay r1 = botMatch(42);
        Replay r2 = botMatch(42);
        assertEquals(r1.encode(), r2.encode());
        assertEquals(r1.play().stateHash(), r2.play().stateHash());
    }

    @Test
    void differentSeedsDiffer() {
        assertNotEquals(botMatch(1).play().stateHash(), botMatch(2).play().stateHash());
    }

    @Test
    void replayRoundTripReproducesTheMatch() {
        Replay r = botMatch(7);
        Match live = r.play();
        Replay back = Replay.decode(r.encode());
        Match again = back.play();
        assertEquals(live.stateHash(), again.stateHash());
        assertEquals(live.result(), again.result());
        assertEquals(live.tick(), again.tick());
    }

    @Test
    void seekMatchesSteppingFromTheStart() {
        Replay r = botMatch(9);
        Match a = r.seek(900);
        Match b = r.start();
        for (int i = 0; i < 900 && !b.isOver(); i++) {
            b.step(r.commands());
        }
        assertEquals(a.stateHash(), b.stateHash());
    }

    @Test
    void matchesEndAndBotsActuallyFight() {
        int decided = 0;
        for (long seed = 100; seed < 120; seed++) {
            Replay r = botMatch(seed);
            Match m = r.play();
            assertTrue(m.isOver());
            assertTrue(m.tick() <= Match.MAX_TICKS);
            assertTrue(r.commands().size() > 10, "bots should play cards");
            if (m.result() != Match.DRAW) {
                decided++;
            }
        }
        assertTrue(decided > 0, "some bot matches should have a winner");
    }

    @Test
    void rejectsCardsNotInHandAndBadDrops() {
        Match m = new Match(5, Deck.starter(), Deck.starter());
        String inHand = m.player(0).hand(0);
        String notInHand = m.player(0).next();
        int ownY = 25 * Arena.TILE;
        int enemyY = 8 * Arena.TILE;
        assertFalse(m.canPlay(0, notInHand, Arena.CLASSIC.laneX(0), ownY));
        CardDef card = Cards.get(inHand);
        if (card.kind != CardDef.SPELL) {
            assertFalse(m.canPlay(0, inHand, Arena.CLASSIC.laneX(0), enemyY));
        }
        assertFalse(m.canPlay(0, inHand, -1, ownY));
        assertFalse(m.canPlay(0, "king-tower", Arena.CLASSIC.laneX(0), ownY));
    }

    @Test
    void playingACardCostsElixirAndCycles() {
        Match m = new Match(11, Deck.starter(), Deck.starter());
        String card = null;
        for (int i = 0; i < Deck.HAND; i++) {
            CardDef d = Cards.get(m.player(0).hand(i));
            if (d.cost <= 5 && d.kind == CardDef.TROOP) {
                card = d.id;
            }
        }
        assertNotNull(card);
        String next = m.player(0).next();
        int before = m.player(0).elixir();
        List<Command> cmds = new ArrayList<Command>();
        cmds.add(new Command(0, 0, card, Arena.CLASSIC.laneX(0), 25 * Arena.TILE));
        m.step(cmds);
        int cost = Cards.get(card).cost * PlayerState.ELIXIR_UNIT;
        assertEquals(before - cost + 1, m.player(0).elixir());
        boolean nextInHand = false;
        for (int i = 0; i < Deck.HAND; i++) {
            nextInHand |= m.player(0).hand(i).equals(next);
            assertNotEquals(card, m.player(0).hand(i));
        }
        assertTrue(nextInHand);
    }

    @Test
    void anUnopposedOgreTakesATower() {
        Match m = new Match(3, Deck.starter(), Deck.starter());
        // Give player 0 an ogre in hand by cycling cards out of the way for free.
        List<Command> none = new ArrayList<Command>();
        while (m.player(0).handSlotOf("ogre") < 0) {
            m.player(0).cycle(0);
        }
        List<Command> cmds = new ArrayList<Command>();
        cmds.add(new Command(0, 0, "ogre", Arena.CLASSIC.laneX(0), 20 * Arena.TILE));
        m.step(cmds);
        for (int i = 0; i < 60 * Match.TICKS_PER_SECOND && m.player(0).crowns() == 0; i++) {
            m.step(none);
        }
        assertEquals(1, m.player(0).crowns());
    }

    @Test
    void rngIsStable() {
        // Pinned so that a port computing SplitMix64 differently fails here, not
        // as a desync in a live match.
        Rng r = new Rng(0);
        assertEquals(0xE220A8397B1DCDAFL, r.nextLong());
    }

    @Test
    void isqrt() {
        for (long v = 0; v < 5000; v++) {
            long s = FixedMath.isqrt(v);
            assertTrue(s * s <= v && (s + 1) * (s + 1) > v, "isqrt " + v);
        }
        assertEquals(1000000L, FixedMath.isqrt(1000000000000L));
    }

    @Test
    void decksAreValidated() {
        assertTrue(Cards.isValidDeck(Deck.starter()));
        assertTrue(Cards.isValidDeck(alternateDeck()));
        String[] dup = Deck.starter();
        dup[1] = dup[0];
        assertFalse(Cards.isValidDeck(dup));
        String[] tower = Deck.starter();
        tower[0] = Cards.KING_TOWER;
        assertFalse(Cards.isValidDeck(tower));
    }

    @Test
    void hexSpellsHashesAsLongToHexStringDoes() {
        long[] values = {0L, 1L, 15L, 16L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 0xcbf29ce484222325L};
        for (long v : values) {
            assertEquals(Long.toHexString(v), Match.hex(v));
        }
    }
}
