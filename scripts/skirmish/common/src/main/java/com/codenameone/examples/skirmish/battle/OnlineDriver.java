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
package com.codenameone.examples.skirmish.battle;

import com.codenameone.examples.skirmish.api.GameProtocol;
import com.codenameone.examples.skirmish.net.GameLink;
import com.codenameone.examples.skirmish.rules.Command;
import com.codenameone.examples.skirmish.rules.Match;
import java.util.Map;

/// Lockstep with the server.
///
/// The server owns the clock. Every couple of ticks it says how far a client
/// may simulate (`t`) and which plays it accepted since last time, each
/// stamped for a tick a little in the future. The client runs the same rules
/// up to `t` and never further, so both sides see every play land on the same
/// tick. Every [GameProtocol#HASH_EVERY] ticks the client sends a hash of its
/// state; a client that differs gets the whole log again and re-simulates.
///
/// A spectator -- an admin in the console -- is the same thing with no seat.
// tag::skirmish-lockstep[]
public final class OnlineDriver extends Driver {
    /// What the battle screen wants to hear besides the ticks.
    public interface Listener {
        void emote(int seat, String id);

        void ended(int result, int crowns0, int crowns1, int trophies, int coins, String matchId);

        void lost();
    }

    private final GameLink link;
    private final boolean spectator;
    private Listener listener;
    private String matchId;
    private String[] names = {"", ""};
    private int allowed;

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

    /// A `start` message: the first one, or the server's answer to a hash
    /// that did not match. Either way the match is rebuilt from its log.
    private void restart(Map<String, Object> m) {
        matchId = GameLink.text(m, "match");
        me = spectator ? 0 : GameLink.number(m, "you");
        names = new String[] {GameLink.text(m, "name0"), GameLink.text(m, "name1")};
        match = new Match(Long.parseLong(GameLink.text(m, "seed")),
                split(GameLink.text(m, "deck0"), ','), split(GameLink.text(m, "deck1"), ','));
        log.clear();
        append(GameLink.text(m, "log"));
        allowed = GameLink.number(m, "tick");
        // Catch up at once to where the server already is.
        while (match.tick() < allowed && !match.isOver()) {
            match.step(log);
        }
    }

    /// Everything the server sends during a match comes through here.
    public void message(Map<String, Object> m) {
        String type = GameLink.text(m, "type");
        if (GameProtocol.TICKS.equals(type)) {
            append(GameLink.text(m, "c"));
            allowed = Math.max(allowed, GameLink.number(m, "t"));
        } else if (GameProtocol.START.equals(type)) {
            restart(m);
        } else if (GameProtocol.EMOTE.equals(type) && listener != null) {
            listener.emote(GameLink.number(m, "player"), GameLink.text(m, "id"));
        } else if (GameProtocol.END.equals(type) && listener != null) {
            listener.ended(GameLink.number(m, "result"), GameLink.number(m, "crowns0"),
                    GameLink.number(m, "crowns1"), GameLink.number(m, "trophies"),
                    GameLink.number(m, "coins"), GameLink.text(m, "match"));
        } else if (GameProtocol.KICKED.equals(type) && listener != null) {
            listener.lost();
        }
    }

    @Override
    public boolean canStep() {
        return match.tick() < allowed && !match.isOver();
    }

    @Override
    public void step() {
        super.step();
        if (!spectator && match.tick() % GameProtocol.HASH_EVERY == 0) {
            link.send("{\"type\":\"sync\",\"t\":" + match.tick() + ",\"h\":\""
                    + Match.hex(match.stateHash()) + "\"}");
        }
    }

    /// Only asks. The play appears when the server stamps it and sends it
    /// back -- to both players at once.
    @Override
    public void play(String card, int x, int y) {
        if (!spectator && match.canPlay(me, card, x, y)) {
            link.send("{\"type\":\"play\",\"card\":\"" + card + "\",\"x\":" + x + ",\"y\":" + y + "}");
        }
    }

    @Override
    public boolean interactive() {
        return !spectator;
    }

    public void emote(String id) {
        link.send("{\"type\":\"emote\",\"id\":\"" + id + "\"}");
    }

    public void leave() {
        link.send("{\"type\":\"leave\"}");
    }

    @Override
    public String name(int seat) {
        if (!spectator && seat == me) {
            return "You";
        }
        return names[seat];
    }

    @Override
    public void dispose() {
        link.close();
    }

    private void append(String encoded) {
        for (String c : split(encoded, ';')) {
            if (c.length() > 0) {
                log.add(Command.decode(c));
            }
        }
    }

    /// `String.split` takes a regular expression and is missing on some
    /// targets; this takes one character.
    static String[] split(String s, char sep) {
        if (s == null || s.length() == 0) {
            return new String[0];
        }
        int n = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == sep) {
                n++;
            }
        }
        String[] out = new String[n];
        int from = 0;
        int k = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == sep) {
                out[k++] = s.substring(from, i);
                from = i + 1;
            }
        }
        return out;
    }
}
// end::skirmish-lockstep[]
