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

/// One side's elixir, deck cycle and crowns.
public final class PlayerState {
    /// Elixir is counted in ticks of regeneration: one elixir is 56 ticks, 2.8 seconds.
    public static final int ELIXIR_UNIT = 56;
    public static final int MAX_ELIXIR = 10 * ELIXIR_UNIT;
    public static final int START_ELIXIR = 5 * ELIXIR_UNIT;

    final String[] order;
    int elixir;
    int crowns;

    PlayerState(String[] deck, Rng rng, int startElixir) {
        elixir = startElixir;
        if (deck == null) {
            // A side that only plays scripted waves. It draws nothing from the
            // generator, so adding one to a mission does not reshuffle the other deck.
            order = new String[0];
            return;
        }
        order = new String[deck.length];
        System.arraycopy(deck, 0, order, 0, deck.length);
        for (int i = order.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            String t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
    }

    /// The raw elixir counter; divide by [#ELIXIR_UNIT] for whole elixir.
    public int elixir() { return elixir; }
    public int wholeElixir() { return elixir / ELIXIR_UNIT; }
    public int crowns() { return crowns; }

    /// False for a side that plays only a mission's scripted waves.
    public boolean hasDeck() { return order.length > 0; }

    /// Card in hand slot `i`, `0 <= i < Deck.HAND`.
    public String hand(int i) { return order[i]; }

    /// The card that enters the hand next.
    public String next() { return order[Deck.HAND]; }

    int handSlotOf(String cardId) {
        if (order.length == 0) {
            return -1;
        }
        for (int i = 0; i < Deck.HAND; i++) {
            if (order[i].equals(cardId)) {
                return i;
            }
        }
        return -1;
    }

    /// Plays the card in `slot`: it goes to the back of the queue and the front of
    /// the queue takes its place in the hand.
    void cycle(int slot) {
        String played = order[slot];
        order[slot] = order[Deck.HAND];
        for (int i = Deck.HAND; i < order.length - 1; i++) {
            order[i] = order[i + 1];
        }
        order[order.length - 1] = played;
    }
}
