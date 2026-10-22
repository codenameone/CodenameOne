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
package com.codenameone.examples.skirmish.game;

import com.codename1.backend.WebSocketSession;
import com.codenameone.examples.skirmish.api.GameProtocol;
import com.codenameone.examples.skirmish.rules.Bot;
import com.codenameone.examples.skirmish.rules.Command;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.Replay;
import java.util.ArrayList;
import java.util.List;

/// One match being played: the authoritative [Match], who is in it, and the log
/// of every command, which is also the replay.
///
/// Nothing here trusts a client. A play is only an intent: the room stamps it
/// with the tick it will execute on, appends it to the log and sends the log on.
/// The shared rules then decide, identically on every machine, whether it was
/// legal. Rooms are driven by [Rooms] under its lock.
// tag::skirmish-room[]
final class MatchRoom {
    /// Plays a seat may make in [#FLOOD_WINDOW] ticks before the rest are dropped.
    static final int FLOOD_LIMIT = 6;
    static final int FLOOD_WINDOW = 20;

    final String id;
    final Match match;
    final Seat[] seats;
    final Bot bot;
    final List<Command> log = new ArrayList<Command>();
    final List<WebSocketSession> spectators = new ArrayList<WebSocketSession>();
    final long startedAt;
    /// Index into [#log] of the first command not yet sent.
    int sent;
    int lastSentTick = -1;
    int forfeit = -1;
    final java.util.Map<Integer, Long> hashes = new java.util.HashMap<Integer, Long>();

    MatchRoom(String id, long seed, Seat a, Seat b, long now) {
        this.id = id;
        this.seats = new Seat[] {a, b};
        this.match = new Match(seed, a.deck, b.deck);
        this.bot = b.isBot() ? new Bot(1, seed ^ 0x1234L) : null;
        this.startedAt = now;
    }

    int seatOf(String username) {
        for (int i = 0; i < 2; i++) {
            if (seats[i].username.equals(username)) {
                return i;
            }
        }
        return -1;
    }

    /// Accepts a play intent from `player`. Answers false when it was dropped.
    boolean intent(int player, String card, int x, int y) {
        if (match.isOver()) {
            return false;
        }
        Seat seat = seats[player];
        int now = match.tick();
        int inWindow = 0;
        for (int i = 0; i < seat.recentCount; i++) {
            if (now - seat.recent[i] < FLOOD_WINDOW) {
                inWindow++;
            }
        }
        if (inWindow >= FLOOD_LIMIT) {
            seat.floods++;
            return false;
        }
        seat.recent[seat.recentCount % seat.recent.length] = now;
        seat.recentCount = Math.min(seat.recentCount + 1, seat.recent.length);
        // A cheap early answer; the rules check again when the command executes.
        if (!match.canPlay(player, card, x, y)) {
            seat.rejected++;
            return false;
        }
        log.add(new Command(now + GameProtocol.INPUT_DELAY, player, card, x, y));
        return true;
    }

    /// Advances one tick, letting the bot decide first.
    void step() {
        if (bot != null) {
            Command c = bot.decide(match, GameProtocol.INPUT_DELAY);
            if (c != null) {
                log.add(c);
            }
        }
        match.step(log);
        if (match.tick() % GameProtocol.HASH_EVERY == 0) {
            hashes.put(Integer.valueOf(match.tick()), Long.valueOf(match.stateHash()));
            hashes.remove(Integer.valueOf(match.tick() - 10 * GameProtocol.HASH_EVERY));
        }
    }

    /// The result: [Match#result], or the side that did not leave.
    int result() {
        return forfeit >= 0 ? 1 - forfeit : match.result();
    }

    boolean isOver() {
        return forfeit >= 0 || match.isOver();
    }

    Replay replay() {
        return new Replay(match.seed(), seats[0].deck, seats[1].deck, log);
    }
}
// end::skirmish-room[]
