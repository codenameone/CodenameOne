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
package com.codenameone.examples.dash.run;

import com.codenameone.examples.dash.api.GameProtocol;
import com.codenameone.examples.dash.net.GameLink;
import com.codenameone.examples.dash.rules.Run;
import com.codenameone.examples.dash.rules.Text;
import java.util.List;
import java.util.Map;

/// A live race, in lockstep with the server.
///
/// The server owns the clock. A few times a second it says how far a client
/// may simulate (`t`) and which inputs it accepted since last time, each
/// stamped [GameProtocol#INPUT_DELAY] ticks in the future. Every client runs
/// the same rules on the same seed up to `t` and never further, so every
/// runner's jump lands on the same tick on every screen. Every
/// [GameProtocol#HASH_EVERY] ticks a client sends a hash of its state; one
/// that differs gets the whole log again and re-simulates.
///
/// A spectator -- an admin in the console -- is the same thing with no seat.
// tag::dash-lockstep[]
public final class RaceDriver extends Driver {
    /// What the race screen wants to hear besides the ticks.
    public interface Listener {
        void emote(String from, String id);

        void chat(String from, String text);

        /// The race is over: `places[i]` is runner i's place, 0 for the winner.
        void ended(int[] places, int[] scores, int trophies, int coins, String raceId);

        void lost();
    }

    private final GameLink link;
    private final boolean spectator;
    private Listener listener;
    private Levels.Playable level;
    private String raceId;
    private String[] names = new String[0];
    private int allowed;
    private long startsAt;
    private int[] log = new int[96];
    private int logSize;
    private int next;
    private int[] inputs;
    private int lastSent;
    private int sentThisTick;

    public RaceDriver(GameLink link, Levels.Playable level, Map<String, Object> start, boolean spectator) {
        this.link = link;
        this.level = level;
        this.spectator = spectator;
        restart(start);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public String raceId() {
        return raceId;
    }

    /// A `start` message: the first one, or the server's answer to a hash
    /// that did not match. Either way the run is rebuilt from its log.
    private void restart(Map<String, Object> m) {
        raceId = GameLink.text(m, "race");
        me = spectator ? 0 : GameLink.number(m, "you");
        names = strings(m.get("names"));
        run = new Run(level.level, level.catalog, Long.parseLong(GameLink.text(m, "seed")),
                Math.max(1, names.length));
        inputs = new int[run.runnerCount()];
        logSize = 0;
        next = 0;
        append(GameLink.text(m, "log"));
        allowed = GameLink.number(m, "tick");
        startsAt = System.currentTimeMillis() + GameLink.number(m, "startIn");
        // Catch up at once to where the server already is.
        while (run.tick() < allowed && !run.over()) {
            stepRun();
        }
    }

    /// Everything the server sends during a race comes through here.
    public void message(Map<String, Object> m) {
        String type = GameLink.text(m, "type");
        if (GameProtocol.TICKS.equals(type)) {
            append(GameLink.text(m, "i"));
            allowed = Math.max(allowed, GameLink.number(m, "t"));
        } else if (GameProtocol.START.equals(type)) {
            restart(m);
        } else if (listener == null) {
            return;
        } else if (GameProtocol.EMOTE.equals(type)) {
            listener.emote(GameLink.text(m, "from"), GameLink.text(m, "id"));
        } else if (GameProtocol.CHAT.equals(type)) {
            listener.chat(GameLink.text(m, "from"), GameLink.text(m, "text"));
        } else if (GameProtocol.END.equals(type)) {
            listener.ended(ints(GameLink.text(m, "places")), ints(GameLink.text(m, "scores")),
                    GameLink.number(m, "trophies"), GameLink.number(m, "coins"), GameLink.text(m, "race"));
        } else if (GameProtocol.KICKED.equals(type)) {
            listener.lost();
        }
    }

    @Override
    public boolean canStep() {
        return run.tick() < allowed && !run.over();
    }

    @Override
    public void step() {
        stepRun();
        if (!spectator && run.tick() % GameProtocol.HASH_EVERY == 0) {
            link.send("{\"type\":\"" + GameProtocol.SYNC + "\",\"t\":" + run.tick() + ",\"h\":\""
                    + Long.toHexString(run.stateHash()) + "\"}");
        }
    }

    private void stepRun() {
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = 0;
        }
        int tick = run.tick();
        while (next < logSize && log[next * 3] <= tick) {
            int seat = log[next * 3 + 1];
            if (log[next * 3] == tick && seat >= 0 && seat < inputs.length) {
                inputs[seat] |= log[next * 3 + 2];
            }
            next++;
        }
        run.step(inputs);
    }

    /// Only asks. The input takes effect when the server stamps it and sends
    /// it back -- to every runner at once. At most one message a tick: a
    /// swipe fires once, not once per frame it is held.
    @Override
    public void input(int bits) {
        if (spectator || bits == 0 || run.over()) {
            return;
        }
        if (lastSent == run.tick() && (sentThisTick & bits) == bits) {
            return;
        }
        if (lastSent != run.tick()) {
            sentThisTick = 0;
        }
        lastSent = run.tick();
        sentThisTick |= bits;
        link.send("{\"type\":\"" + GameProtocol.INPUT + "\",\"b\":" + bits + "}");
    }

    @Override
    public boolean interactive() {
        return !spectator;
    }

    @Override
    public int countdownTicks() {
        long left = startsAt - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) (left * Run.TICKS_PER_SECOND / 1000);
    }

    public void emote(String id) {
        link.send("{\"type\":\"" + GameProtocol.EMOTE + "\",\"id\":\"" + id + "\"}");
    }

    public void leave() {
        link.send("{\"type\":\"" + GameProtocol.LEAVE + "\"}");
    }

    public GameLink link() {
        return link;
    }

    @Override
    public String name(int runner) {
        if (!spectator && runner == me) {
            return "You";
        }
        return runner < names.length ? names[runner] : super.name(runner);
    }

    @Override
    public void dispose() {
        link.close();
    }

    /// `tick,seat,bits;...`, appended in the order the server stamped them.
    private void append(String encoded) {
        List<String> items = Text.split(encoded, ';');
        for (int i = 0; i < items.size(); i++) {
            List<String> f = Text.split(items.get(i), ',');
            if (f.size() != 3) {
                continue;
            }
            if ((logSize + 1) * 3 > log.length) {
                int[] n = new int[log.length * 2];
                System.arraycopy(log, 0, n, 0, logSize * 3);
                log = n;
            }
            log[logSize * 3] = Integer.parseInt(f.get(0));
            log[logSize * 3 + 1] = Integer.parseInt(f.get(1));
            log[logSize * 3 + 2] = Integer.parseInt(f.get(2));
            logSize++;
        }
    }

    static int[] ints(String csv) {
        if (csv == null || csv.length() == 0) {
            return new int[0];
        }
        List<String> f = Text.split(csv, ',');
        int[] out = new int[f.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = f.get(i).length() == 0 ? 0 : Integer.parseInt(f.get(i));
        }
        return out;
    }

    static String[] strings(Object list) {
        if (!(list instanceof List)) {
            return new String[0];
        }
        List<?> l = (List<?>) list;
        String[] out = new String[l.size()];
        for (int i = 0; i < out.length; i++) {
            Object v = l.get(i);
            out[i] = v == null ? "" : v.toString();
        }
        return out;
    }
}
// end::dash-lockstep[]
