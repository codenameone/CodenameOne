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
package com.codenameone.examples.splat.game;

import com.codename1.backend.WebSocketSession;
import com.codenameone.examples.splat.api.GameProtocol;
import com.codenameone.examples.splat.rules.Event;
import com.codenameone.examples.splat.rules.Input;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.rules.Mode;
import com.codenameone.examples.splat.rules.Replay;
import com.codenameone.examples.splat.rules.Slot;
import com.codenameone.examples.splat.rules.Trig;
import java.util.ArrayList;
import java.util.List;

/// One match in progress, and the server's authority over it.
///
/// Unlike a lockstep game, nobody else's inputs ever reach a client: the server
/// steps the [Match] with whatever each player sent last and sends every player
/// a snapshot of the result every [GameProtocol#SNAP_EVERY] ticks, with the
/// sequence number of their own last input it used. The client restores the
/// snapshot and replays its inputs the server has not used yet, which is
/// prediction and reconciliation in a dozen lines, because both sides run the
/// same rules. Nothing here is thread safe; [Rooms] holds the lock.
// tag::splat-match-room[]
final class MatchRoom {
    /// Inputs one seat may send in a second. A client sends one per tick.
    static final int FLOOD_LIMIT = Match.TICK_HZ + 12;
    /// A turn larger than this in one tick is a mouse flick at best; many of them
    /// in a match is an aimbot.
    static final int SNAP_TURN = Trig.FULL / 3;

    final String id;
    final String level;
    final Mode mode;
    final Seat[] seats;
    final Match match;
    final Replay replay;
    final long startedAt;
    final List<WebSocketSession> spectators = new ArrayList<WebSocketSession>();
    private final Input[] pending;
    /// Wipes each seat got away with, for the achievement nobody should be proud of.
    private final int[] wipes;
    /// Events since the last snapshot, already encoded.
    private final StringBuilder events = new StringBuilder();

    MatchRoom(String id, String level, String arena, Mode mode, long seed, Seat[] seats, long startedAt) {
        this.id = id;
        this.level = level;
        this.mode = mode;
        this.seats = seats;
        List<Slot> slots = new ArrayList<Slot>();
        for (int i = 0; i < seats.length; i++) {
            Seat s = seats[i];
            // Bots of mixed skill, so a bot-filled match has someone to laugh at.
            int skill = s.isBot() ? 35 + 15 * (i % 4) : 0;
            slots.add(new Slot(s.name, s.team, s.marker, skill));
        }
        this.replay = new Replay(seed, arena, mode, slots);
        this.match = replay.newMatch();
        this.startedAt = startedAt;
        this.pending = new Input[seats.length];
        this.wipes = new int[seats.length];
    }

    int seatOf(String username) {
        for (int i = 0; i < seats.length; i++) {
            if (seats[i].username.equals(username)) {
                return i;
            }
        }
        return -1;
    }

    /// A player's input with its sequence number. Answers false when it is
    /// dropped: a flood, an old or repeated sequence, or a seat that is not theirs.
    boolean input(int seat, int seq, Input in) {
        if (seat < 0 || match.over() || in == null) {
            return false;
        }
        Seat s = seats[seat];
        if (seq <= s.lastSeq) {
            return false;
        }
        int now = match.tick();
        int inWindow = 0;
        int n = Math.min(s.recentCount, s.recent.length);
        for (int i = 0; i < n; i++) {
            if (now - s.recent[i] < Match.TICK_HZ) {
                inWindow++;
            }
        }
        if (inWindow >= FLOOD_LIMIT) {
            s.floods++;
            return false;
        }
        s.recent[s.recentCount % s.recent.length] = now;
        s.recentCount++;
        Input previous = pending[seat] != null ? pending[seat] : match.player(seat).input();
        if (Math.abs(Trig.delta(previous.yaw, in.yaw)) > SNAP_TURN) {
            s.snaps++;
        }
        s.lastSeq = seq;
        pending[seat] = in;
        return true;
    }

    void step() {
        int now = match.tick();
        Input[] frame = new Input[seats.length];
        for (int i = 0; i < seats.length; i++) {
            frame[i] = pending[i];
            if (pending[i] != null) {
                seats[i].appliedSeq = seats[i].lastSeq;
            } else if (seats[i].left) {
                frame[i] = Input.IDLE;
            }
            pending[i] = null;
        }
        replay.record(now, frame);
        match.step(frame);
        List<Event> happened = match.events();
        for (int i = 0; i < happened.size(); i++) {
            Event e = happened.get(i);
            if (e.kind == Event.WIPE_OK) {
                wipes[e.a]++;
            }
            if (e.kind == Event.SHOT) {
                // the snapshot carries the ball; sending every shot doubles the traffic
                continue;
            }
            if (events.length() > 0) {
                events.append(';');
            }
            events.append(e.kind).append(',').append(e.tick).append(',').append(e.a).append(',').append(e.b)
                    .append(',').append(e.x).append(',').append(e.y).append(',').append(e.z);
        }
    }

    /// Takes the events gathered since the last snapshot.
    String drainEvents() {
        String out = events.toString();
        events.setLength(0);
        return out;
    }

    /// Over when the rules say so, or when no human is left on the field.
    boolean isOver() {
        if (match.over()) {
            return true;
        }
        for (Seat s : seats) {
            if (!s.isBot() && !s.left) {
                return false;
            }
        }
        return true;
    }

    /// 0 for the winning team, 1 for the losers, and everyone 0 on a draw.
    int[] places() {
        int[] out = new int[seats.length];
        int w = match.winner();
        for (int i = 0; i < seats.length; i++) {
            out[i] = w < 0 || seats[i].team == w ? 0 : 1;
        }
        return out;
    }

    int[] wipes() {
        return wipes.clone();
    }

    int humans() {
        int h = 0;
        for (Seat s : seats) {
            if (!s.isBot()) {
                h++;
            }
        }
        return h;
    }

    /// The seat with the most paints, for the admin's live list.
    int leader() {
        int best = 0;
        for (int i = 1; i < seats.length; i++) {
            if (match.player(i).paints() > match.player(best).paints()) {
                best = i;
            }
        }
        return best;
    }
}
// end::splat-match-room[]
