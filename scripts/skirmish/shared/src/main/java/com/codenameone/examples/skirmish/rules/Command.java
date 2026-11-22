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

/// A player's only way to affect a match: play a card from the hand at a point.
///
/// The server stamps [#tick], the tick the command executes on, a few ticks in the
/// future, and broadcasts it. Every client and the server then apply the same
/// command on the same tick, which is all lockstep needs.
public final class Command {
    public final int tick;
    public final int player;
    public final String cardId;
    public final int x;
    public final int y;

    public Command(int tick, int player, String cardId, int x, int y) {
        this.tick = tick;
        this.player = player;
        this.cardId = cardId;
        this.x = x;
        this.y = y;
    }

    public Command atTick(int t) {
        return new Command(t, player, cardId, x, y);
    }

    /// `tick,player,card,x,y`; card ids never contain a comma.
    public String encode() {
        return tick + "," + player + "," + cardId + "," + x + "," + y;
    }

    public static Command decode(String s) {
        String[] p = Text.split(s, ',');
        if (p.length != 5) {
            throw new IllegalArgumentException("bad command " + s);
        }
        return new Command(Integer.parseInt(p[0]), Integer.parseInt(p[1]), p[2],
                Integer.parseInt(p[3]), Integer.parseInt(p[4]));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Command)) {
            return false;
        }
        Command c = (Command) o;
        return c.tick == tick && c.player == player && c.cardId.equals(cardId) && c.x == x && c.y == y;
    }

    @Override
    public int hashCode() {
        return encode().hashCode();
    }

    @Override
    public String toString() {
        return encode();
    }
}
