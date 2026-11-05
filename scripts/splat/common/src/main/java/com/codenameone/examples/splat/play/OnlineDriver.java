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
package com.codenameone.examples.splat.play;

import com.codenameone.examples.splat.api.GameProtocol;
import com.codenameone.examples.splat.net.GameLink;
import com.codenameone.examples.splat.rules.Catalog;
import com.codenameone.examples.splat.rules.Event;
import com.codenameone.examples.splat.rules.Input;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.rules.Mode;
import com.codenameone.examples.splat.rules.Slot;
import com.codenameone.examples.splat.rules.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A match the server is the authority on.
///
/// Every tick the player's input goes to the server with a sequence number and
/// is applied here at once, so the marker fires and the legs move with no wait.
/// Every few ticks a snapshot arrives with the newest sequence number the server
/// used: the match is put back exactly where the server says it is, and the
/// inputs the server has not seen yet are played over it again. When the guess
/// was right nothing moves; when it was wrong -- someone else's ball got there
/// first -- the correction is one snapshot old and small.
///
/// Other players run on the last input the server knew for them, which is what
/// the server itself does until a newer one arrives.
// tag::splat-prediction[]
public final class OnlineDriver extends Driver {
    public interface Listener {
        void emote(String from, String id);

        void chat(String from, String text);

        void ended(int[] places, int[] scores, int trophies, int coins, String matchId);

        void lost();
    }

    /// The most inputs the phone runs ahead of the last snapshot. Past this the
    /// server has gone quiet, and guessing further only makes the snap back worse.
    static final int MAX_AHEAD = 20;

    private final GameLink link;
    private final boolean spectator;
    private Listener listener;
    private String matchId;
    private String level;
    private long startsAt;
    private int seq;
    private int serverTick;
    private final List<int[]> pendingSeq = new ArrayList<int[]>();
    private final List<Input> pending = new ArrayList<Input>();
    private Input[] frame = new Input[0];

    public OnlineDriver(GameLink link, Map<String, Object> start, boolean spectator) {
        this.link = link;
        this.spectator = spectator;
        restart(start);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public String matchId() {
        return matchId;
    }

    public String level() {
        return level;
    }

    private void restart(Map<String, Object> m) {
        matchId = GameLink.text(m, "match");
        level = GameLink.text(m, "level");
        me = spectator ? 0 : GameLink.number(m, "you");
        List<Slot> slots = new ArrayList<Slot>();
        Object list = m.get("slots");
        if (list instanceof List) {
            for (Object o : (List<?>) list) {
                slots.add(Slot.decode(String.valueOf(o)));
            }
        }
        names = new String[slots.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = slots.get(i).name;
        }
        int colon = level.indexOf(':');
        match = new Match(Long.parseLong(GameLink.text(m, "seed")), Catalog.arena(level.substring(0, colon)),
                Mode.byName(level.substring(colon + 1)), slots);
        frame = new Input[slots.size()];
        pending.clear();
        pendingSeq.clear();
        String snap = GameLink.text(m, "snap");
        if (snap.length() > 0) {
            match.restore(ints(snap));
        }
        serverTick = match.tick();
        startsAt = System.currentTimeMillis() + GameLink.number(m, "startIn");
    }

    public void message(Map<String, Object> m) {
        String type = GameLink.text(m, "type");
        if (GameProtocol.SNAP.equals(type)) {
            snap(GameLink.number(m, "t"), GameLink.number(m, "ack"), GameLink.text(m, "s"), GameLink.text(m, "e"));
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
                    GameLink.number(m, "trophies"), GameLink.number(m, "coins"), GameLink.text(m, "match"));
        } else if (GameProtocol.KICKED.equals(type)) {
            listener.lost();
        }
    }

    /// Reconciliation: back to the server's state, then the unacknowledged
    /// inputs again on top of it.
    private void snap(int tick, int ack, String state, String events) {
        if (state.length() == 0 || tick < serverTick) {
            return;
        }
        serverTick = tick;
        match.restore(ints(state));
        while (!pendingSeq.isEmpty() && pendingSeq.get(0)[0] <= ack) {
            pendingSeq.remove(0);
            pending.remove(0);
        }
        for (int i = 0; i < pending.size() && !match.over(); i++) {
            stepWith(pending.get(i));
        }
        // The server's events are the ones that count: a PAINTED the phone
        // guessed and the server did not agree with is never shown.
        List<String> items = Text.split(events, ';');
        for (int i = 0; i < items.size(); i++) {
            int[] f = ints(items.get(i));
            if (f.length == 7) {
                emit(new Event(f[0], f[1], f[2], f[3], f[4], f[5], f[6]));
            }
        }
    }

    @Override
    public boolean canStep() {
        return countdownTicks() == 0 && !match.over() && (spectator || pending.size() < MAX_AHEAD);
    }

    @Override
    public void step(Input in) {
        if (spectator) {
            stepWith(null);
            return;
        }
        // The tick the player is looking at is the newest snapshot, which is what
        // the server rewinds a shot to.
        Input stamped = new Input(in.strafe, in.forward, in.yaw, in.pitch, in.buttons, serverTick);
        seq++;
        link.send("{\"type\":\"" + GameProtocol.INPUT + "\",\"s\":" + seq + ",\"i\":\"" + stamped.encode() + "\"}");
        pendingSeq.add(new int[] {seq});
        pending.add(stamped);
        stepWith(stamped);
        // Your own shot is heard at once; everything else waits for the server.
        List<Event> happened = match.events();
        for (int i = 0; i < happened.size(); i++) {
            Event e = happened.get(i);
            if (e.kind == Event.SHOT && e.a == me) {
                emit(e);
            }
        }
    }

    private void stepWith(Input in) {
        for (int i = 0; i < frame.length; i++) {
            frame[i] = null;
        }
        if (in != null && me < frame.length) {
            frame[me] = in;
        }
        match.step(frame);
    }

    @Override
    public boolean interactive() {
        return !spectator;
    }

    @Override
    public int countdownTicks() {
        long left = startsAt - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) (left * Match.TICK_HZ / 1000) + 1;
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
    public void dispose() {
        link.close();
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
}
// end::splat-prediction[]
