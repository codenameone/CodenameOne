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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/// A complete, self-contained game of blackjack ("Duke Jack") — the rules engine the
/// Game Builder board tutorial wires into its companion class. It has no Codename One or
/// rendering dependency: feed it input (deal / hit / stand), read the hands and the outcome,
/// and draw them however you like (the tutorial draws them as card elements on a felt board).
///
/// Rules implemented: a single 52-card shoe, Ace counts 11 or 1 (whichever keeps the hand
/// from busting), naturals (two-card 21) beat a drawn 21, the dealer hits below 17 and stands
/// on 17+, player bust is an immediate loss, dealer bust pays the player, equal totals push.
public final class Blackjack {

    /// Card ranks are 1..13 (1 = Ace, 11 = Jack, 12 = Queen, 13 = King); suits 0..3.
    public static final int SUIT_SPADES = 0;
    public static final int SUIT_HEARTS = 1;
    public static final int SUIT_DIAMONDS = 2;
    public static final int SUIT_CLUBS = 3;

    /// One playing card. `rank` 1..13, `suit` 0..3.
    public static final class Card {
        public final int rank;
        public final int suit;

        public Card(int rank, int suit) {
            this.rank = rank;
            this.suit = suit;
        }

        /// Blackjack point value: face cards are 10, an Ace is 11 here (reduced to 1 later
        /// by {@link #handValue} when a hand would otherwise bust).
        public int value() {
            if (rank == 1) {
                return 11;
            }
            return Math.min(10, rank);
        }

        public boolean isAce() {
            return rank == 1;
        }

        /// "A", "2".."10", "J", "Q", "K".
        public String rankLabel() {
            switch (rank) {
                case 1: return "A";
                case 11: return "J";
                case 12: return "Q";
                case 13: return "K";
                default: return Integer.toString(rank);
            }
        }

        /// True for the picture cards (Jack, Queen, King) — the Duke faces in the tutorial art.
        public boolean isFace() {
            return rank >= 11;
        }
    }

    /// Phase of a round.
    public enum Phase { PLAYER_TURN, DEALER_TURN, DONE }

    /// Final result of a finished round, from the player's point of view.
    public enum Outcome { PLAYER_BLACKJACK, PLAYER_WIN, DEALER_WIN, PUSH }

    private final List<Card> shoe = new ArrayList<>();
    private final List<Card> player = new ArrayList<>();
    private final List<Card> dealer = new ArrayList<>();
    private int cursor;
    private Phase phase = Phase.PLAYER_TURN;
    private Outcome outcome;

    /// Builds and shuffles a fresh 52-card shoe, then deals the opening hand (two cards each;
    /// the dealer's second card is the face-down "hole" card until the player stands).
    public Blackjack(Random rng) {
        for (int suit = 0; suit < 4; suit++) {
            for (int rank = 1; rank <= 13; rank++) {
                shoe.add(new Card(rank, suit));
            }
        }
        // Fisher-Yates with the supplied RNG (pass a seeded Random for a reproducible deal).
        for (int i = shoe.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Card tmp = shoe.get(i);
            shoe.set(i, shoe.get(j));
            shoe.set(j, tmp);
        }
        player.add(draw());
        dealer.add(draw());
        player.add(draw());
        dealer.add(draw());
        // a two-card 21 ends the round immediately
        if (handValue(player) == 21) {
            phase = Phase.DONE;
            settle();
        }
    }

    private Card draw() {
        return shoe.get(cursor++);
    }

    /// The best (non-busting if possible) total of a hand: count every Ace as 11, then drop
    /// each Ace to 1 while the hand is over 21.
    public static int handValue(List<Card> hand) {
        int total = 0;
        int aces = 0;
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.get(i);
            total += c.value();
            if (c.isAce()) {
                aces++;
            }
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    /// A two-card 21 (Ace + ten-value card).
    public static boolean isBlackjack(List<Card> hand) {
        return hand.size() == 2 && handValue(hand) == 21;
    }

    /// The player takes another card. A bust (over 21) ends the round.
    public void hit() {
        if (phase != Phase.PLAYER_TURN) {
            return;
        }
        player.add(draw());
        if (handValue(player) >= 21) {   // 21 auto-stands; >21 busts
            stand();
        }
    }

    /// The player stops drawing; the dealer then plays and the round settles.
    public void stand() {
        if (phase == Phase.DONE) {
            return;
        }
        phase = Phase.DEALER_TURN;
        // dealer reveals the hole card and hits until 17 (stands on all 17s), unless the
        // player has already busted (the dealer needn't draw against a bust).
        if (handValue(player) <= 21) {
            while (handValue(dealer) < 17) {
                dealer.add(draw());
            }
        }
        phase = Phase.DONE;
        settle();
    }

    private void settle() {
        int p = handValue(player);
        int d = handValue(dealer);
        if (p > 21) {
            outcome = Outcome.DEALER_WIN;
        } else if (isBlackjack(player) && !isBlackjack(dealer)) {
            outcome = Outcome.PLAYER_BLACKJACK;
        } else if (d > 21) {
            outcome = Outcome.PLAYER_WIN;
        } else if (p > d) {
            outcome = Outcome.PLAYER_WIN;
        } else if (p < d) {
            outcome = Outcome.DEALER_WIN;
        } else {
            outcome = Outcome.PUSH;
        }
    }

    public Phase phase() {
        return phase;
    }

    /// The settled result, or null until the round is {@link Phase#DONE}.
    public Outcome outcome() {
        return outcome;
    }

    public List<Card> playerHand() {
        return player;
    }

    public List<Card> dealerHand() {
        return dealer;
    }

    public int playerValue() {
        return handValue(player);
    }

    public int dealerValue() {
        return handValue(dealer);
    }

    /// Whether the dealer's hole (second) card is still hidden — true during the player's turn.
    public boolean dealerHoleHidden() {
        return phase == Phase.PLAYER_TURN;
    }

    /// A short human-readable result line for the HUD ("Blackjack! Duke wins", etc.).
    public String resultText() {
        if (outcome == null) {
            return "";
        }
        switch (outcome) {
            case PLAYER_BLACKJACK: return "Blackjack! Duke wins";
            case PLAYER_WIN: return "Duke wins";
            case DEALER_WIN: return playerValue() > 21 ? "Bust! Duke loses" : "Dealer wins";
            default: return "Push";
        }
    }
}
