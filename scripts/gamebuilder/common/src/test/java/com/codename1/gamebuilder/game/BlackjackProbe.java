/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.gamebuilder.game;

import com.codename1.gamebuilder.game.Blackjack.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/// Headless self-check for the {@link Blackjack} engine (surefire is disabled for this module,
/// so validation runs through a {@code main}). Exits non-zero on any failed assertion.
public final class BlackjackProbe {
    private static int checks;
    private static int fails;

    private BlackjackProbe() {
    }

    public static void main(String[] args) {
        // ---- hand valuation, including the Ace 11/1 rule -------------------------------
        eq("A+K is 21", 21, val(c(1, 0), c(13, 1)));
        eq("two Aces + 9 is 21 (one soft, one hard)", 21, val(c(1, 0), c(1, 1), c(9, 2)));
        eq("three Aces + 8 is 21", 21, val(c(1, 0), c(1, 1), c(1, 2), c(8, 3)));
        eq("K+Q+5 busts to 25", 25, val(c(13, 0), c(12, 1), c(5, 2)));
        eq("soft 17 (A+6)", 17, val(c(1, 0), c(6, 1)));
        eq("A+6+10 is a hard 17", 17, val(c(1, 0), c(6, 1), c(10, 2)));
        eq("J counts as 10", 20, val(c(11, 0), c(10, 1)));

        ok("A+K is a blackjack", Blackjack.isBlackjack(hand(c(1, 0), c(13, 1))));
        ok("three-card 21 is NOT a blackjack", !Blackjack.isBlackjack(hand(c(7, 0), c(7, 1), c(7, 2))));
        ok("K+Q is not a blackjack", !Blackjack.isBlackjack(hand(c(13, 0), c(12, 1))));

        // ---- full rounds over many deals: every game terminates with a legal outcome ----
        for (int seed = 0; seed < 2000; seed++) {
            Blackjack g = new Blackjack(new Random(seed));
            // a simple player policy: hit below 17, then stand
            int guard = 0;
            while (g.phase() == Blackjack.Phase.PLAYER_TURN && g.playerValue() < 17 && guard++ < 12) {
                g.hit();
            }
            if (g.phase() == Blackjack.Phase.PLAYER_TURN) {
                g.stand();
            }
            ok("round " + seed + " reaches DONE", g.phase() == Blackjack.Phase.DONE);
            ok("round " + seed + " has an outcome", g.outcome() != null);
            int p = g.playerValue();
            int d = g.dealerValue();
            if (p > 21) {
                ok("player bust => dealer win (seed " + seed + ")",
                        g.outcome() == Blackjack.Outcome.DEALER_WIN);
            } else if (Blackjack.isBlackjack(g.playerHand())) {
                // a player natural settles at the deal — the dealer does not draw against it
                ok("player natural pays blackjack or pushes (seed " + seed + ")",
                        g.outcome() == Blackjack.Outcome.PLAYER_BLACKJACK
                                || g.outcome() == Blackjack.Outcome.PUSH);
            } else {
                // when the player stands pat, the dealer must have drawn to at least 17
                ok("dealer drew to 17+ or busted (seed " + seed + ")", d >= 17);
                if (d > 21) {
                    ok("dealer bust => player win (seed " + seed + ")",
                            g.outcome() == Blackjack.Outcome.PLAYER_WIN
                                    || g.outcome() == Blackjack.Outcome.PLAYER_BLACKJACK);
                } else if (p > d) {
                    ok("higher total wins (seed " + seed + ")",
                            g.outcome() == Blackjack.Outcome.PLAYER_WIN
                                    || g.outcome() == Blackjack.Outcome.PLAYER_BLACKJACK);
                } else if (p < d) {
                    ok("lower total loses (seed " + seed + ")",
                            g.outcome() == Blackjack.Outcome.DEALER_WIN);
                }
            }
        }

        // a blackjack must beat a drawn (3+ card) 21 — find one deterministically
        boolean sawNatural = false;
        for (int seed = 0; seed < 5000 && !sawNatural; seed++) {
            Blackjack g = new Blackjack(new Random(seed));
            if (Blackjack.isBlackjack(g.playerHand())) {
                sawNatural = true;
                ok("a natural settles immediately as DONE", g.phase() == Blackjack.Phase.DONE);
                // dealer also blackjack => push; else player blackjack
                if (Blackjack.isBlackjack(g.dealerHand())) {
                    ok("two naturals push", g.outcome() == Blackjack.Outcome.PUSH);
                } else {
                    ok("player natural pays blackjack", g.outcome() == Blackjack.Outcome.PLAYER_BLACKJACK);
                }
            }
        }
        ok("at least one natural occurred across 5000 deals", sawNatural);

        System.out.println("[BlackjackProbe] " + (checks - fails) + "/" + checks + " checks passed");
        if (fails > 0) {
            System.out.println("[BlackjackProbe] FAILED");
            System.exit(1);
        }
        System.out.println("[BlackjackProbe] OK");
    }

    private static Card c(int rank, int suit) {
        return new Card(rank, suit);
    }

    private static List<Card> hand(Card... cards) {
        List<Card> h = new ArrayList<>();
        for (Card card : cards) {
            h.add(card);
        }
        return h;
    }

    private static int val(Card... cards) {
        return Blackjack.handValue(hand(cards));
    }

    private static void eq(String what, int expected, int actual) {
        checks++;
        if (expected != actual) {
            fails++;
            System.out.println("  FAIL: " + what + " expected " + expected + " got " + actual);
        }
    }

    private static void ok(String what, boolean cond) {
        checks++;
        if (!cond) {
            fails++;
            System.out.println("  FAIL: " + what);
        }
    }
}
