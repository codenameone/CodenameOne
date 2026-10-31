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
package com.codenameone.examples.dash.game;

import com.codename1.backend.WebSocketSession;
import com.codenameone.examples.dash.api.GameProtocol;
import com.codenameone.examples.dash.rules.Bot;
import com.codenameone.examples.dash.rules.Catalog;
import com.codenameone.examples.dash.rules.Input;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.Replay;
import com.codenameone.examples.dash.rules.Run;
import com.codenameone.examples.dash.rules.Runner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// One race in progress. The server runs the same [Run] every client runs:
/// a player's input is stamped [GameProtocol#INPUT_DELAY] ticks ahead, so it
/// reaches every client before that tick is simulated, and the room's log is
/// the whole race. Nothing here is thread safe; [Rooms] holds the lock.
// tag::dash-race-room[]
final class RaceRoom {
    /// Inputs one seat may send in [#FLOOD_WINDOW] ticks. A thumb manages
    /// about eight a second; a script manages sixty.
    static final int FLOOD_LIMIT = 16;
    static final int FLOOD_WINDOW = Run.TICKS_PER_SECOND;

    final String id;
    final String level;
    final Seat[] seats;
    final Run run;
    final Replay replay;
    final Bot[] bots;
    /// When tick 0 runs: the end of the countdown.
    final long startedAt;
    final List<WebSocketSession> spectators = new ArrayList<WebSocketSession>();
    /// Every input in stamp order, as `{tick, seat, bits}`; what TICKS sends.
    final List<int[]> log = new ArrayList<int[]>();
    private final Map<Integer, int[]> pending = new HashMap<Integer, int[]>();
    /// The server's hash every [GameProtocol#HASH_EVERY] ticks, for SYNC.
    final Map<Integer, Long> hashes = new HashMap<Integer, Long>();
    int sent;
    int lastSentTick;

    RaceRoom(String id, String level, LevelDef def, Catalog catalog, long seed, Seat[] seats, long startedAt) {
        this.id = id;
        this.level = level;
        this.seats = seats;
        this.run = new Run(def, catalog, seed, seats.length);
        this.replay = new Replay(def.id, def.contentHash(), seed, seats.length);
        this.startedAt = startedAt;
        this.bots = new Bot[seats.length];
        for (int i = 0; i < seats.length; i++) {
            if (seats[i].isBot()) {
                // Bots of mixed skill, seeded from the race so a replay of the
                // room makes the same choices.
                bots[i] = new Bot(seed + 977L * i, 55 + 12 * (i % 4));
            }
        }
    }

    int seatOf(String username) {
        for (int i = 0; i < seats.length; i++) {
            if (seats[i].username.equals(username)) {
                return i;
            }
        }
        return -1;
    }

    /// A player's input. Answers false when it is dropped: a flood, a runner
    /// that is out, or a race that has not started.
    boolean intent(int seat, int bits) {
        if (seat < 0 || run.over() || !run.runner(seat).running()) {
            return false;
        }
        Seat s = seats[seat];
        int now = run.tick();
        int inWindow = 0;
        for (int i = 0; i < s.recentCount; i++) {
            if (now - s.recent[i] < FLOOD_WINDOW) {
                inWindow++;
            }
        }
        if (inWindow >= FLOOD_LIMIT) {
            s.floods++;
            return false;
        }
        s.recent[s.recentCount % s.recent.length] = now;
        s.recentCount = s.recentCount < s.recent.length ? s.recentCount + 1 : s.recentCount;
        int at = now + GameProtocol.INPUT_DELAY;
        int clean = bits & Input.ALL;
        if (clean == 0) {
            return false;
        }
        int[] row = pending.get(Integer.valueOf(at));
        if (row == null) {
            row = new int[seats.length];
            pending.put(Integer.valueOf(at), row);
        }
        row[seat] |= clean;
        log.add(new int[] {at, seat, clean});
        return true;
    }

    void step() {
        int now = run.tick();
        int[] row = pending.remove(Integer.valueOf(now));
        if (row == null) {
            row = new int[seats.length];
        }
        for (int i = 0; i < seats.length; i++) {
            if (bots[i] != null) {
                int b = bots[i].decide(run, i);
                if (b != 0) {
                    row[i] |= b;
                    log.add(new int[] {now, i, b});
                }
            }
            if (row[i] != 0) {
                replay.record(now, i, row[i]);
            }
        }
        run.step(row);
        if (run.tick() % GameProtocol.HASH_EVERY == 0) {
            hashes.put(Integer.valueOf(run.tick()), Long.valueOf(run.stateHash()));
            hashes.remove(Integer.valueOf(run.tick() - GameProtocol.HASH_EVERY * 20));
        }
    }

    /// Over when the rules say so, or when no player is still running: bots
    /// racing each other is not worth simulating.
    boolean isOver() {
        if (run.over()) {
            return true;
        }
        for (int i = 0; i < seats.length; i++) {
            if (!seats[i].isBot() && !seats[i].left && run.runner(i).running()) {
                return false;
            }
        }
        return true;
    }

    /// Each seat's place, 0 first: runners who finished by when they did,
    /// then everyone else by score.
    int[] places() {
        int n = seats.length;
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            // Insertion sort: six runners at most, and no comparator object.
            int j = i;
            while (j > 0 && ahead(i, order[j - 1])) {
                order[j] = order[j - 1];
                j--;
            }
            order[j] = i;
        }
        int[] place = new int[n];
        for (int p = 0; p < n; p++) {
            place[order[p]] = p;
        }
        return place;
    }

    private boolean ahead(int a, int b) {
        Runner ra = run.runner(a);
        Runner rb = run.runner(b);
        if (ra.finished != rb.finished) {
            return ra.finished;
        }
        if (ra.finished && ra.endTick != rb.endTick) {
            return ra.endTick < rb.endTick;
        }
        return ra.score() > rb.score();
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

    /// The seat in front right now, for the live list.
    int leader() {
        int best = 0;
        for (int i = 1; i < seats.length; i++) {
            if (run.runner(i).z > run.runner(best).z) {
                best = i;
            }
        }
        return best;
    }
}
// end::dash-race-room[]
