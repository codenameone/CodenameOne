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

import com.codename1.backend.Json;
import com.codenameone.examples.splat.api.GameProtocol;
import com.codenameone.examples.splat.rules.RulesVersion;
import com.codenameone.examples.splat.rules.Slot;
import java.util.List;

/// Builds the game socket's outgoing JSON. Strings go through [Json#write], so a
/// player name can never break a message.
final class Messages {
    private Messages() {
    }

    static String simple(String type) {
        return "{\"type\":\"" + type + "\"}";
    }

    static String error(String message) {
        return "{\"type\":\"" + GameProtocol.ERROR + "\",\"message\":" + Json.write(message) + "}";
    }

    /// `startIn` is how many milliseconds of countdown are left; `you` is -1
    /// for a spectator. `slots` are the roster in [Slot#encode] form: with the
    /// seed, the field and the mode, everything a client needs to build the same
    /// [com.codenameone.examples.splat.rules.Match]; `snap` is where it stands now.
    static String start(MatchRoom room, int you, long startIn) {
        StringBuilder players = new StringBuilder();
        StringBuilder slots = new StringBuilder();
        for (int i = 0; i < room.seats.length; i++) {
            if (i > 0) {
                players.append(',');
                slots.append(',');
            }
            // A bot has no account, so no username: an empty one, which a
            // client knows not to call.
            String u = room.seats[i].username;
            players.append(Json.write(u == null ? "" : u));
            slots.append(Json.write(room.replay.slots().get(i).encode()));
        }
        return "{\"type\":\"" + GameProtocol.START + "\",\"match\":\"" + room.id + "\",\"level\":"
                + Json.write(room.level) + ",\"seed\":\"" + room.match.seed() + "\",\"you\":" + you
                + ",\"players\":[" + players + "],\"slots\":[" + slots + "],\"rules\":" + RulesVersion.ID
                + ",\"startIn\":" + Math.max(0, startIn) + ",\"snap\":\"" + ints(room.match.snapshot()) + "\"}";
    }

    /// `ack` is the sequence number of the newest of the receiver's inputs the
    /// state includes, -1 for a spectator; `e` the events since the last one.
    static String snap(int tick, int ack, int[] state, String events) {
        return "{\"type\":\"" + GameProtocol.SNAP + "\",\"t\":" + tick + ",\"ack\":" + ack + ",\"s\":\""
                + ints(state) + "\",\"e\":\"" + events + "\"}";
    }

    static String ints(int[] v) {
        return join(v);
    }

    /// `players` are the members' usernames, in the order of `names`: what a
    /// client addresses voice signaling to.
    static String party(String code, String url, String level, String host, List<String> names,
            List<String> players) {
        return "{\"type\":\"" + GameProtocol.PARTY + "\",\"code\":" + Json.write(code) + ",\"url\":"
                + Json.write(url) + ",\"level\":" + Json.write(level) + ",\"host\":" + Json.write(host)
                + ",\"names\":[" + array(names) + "],\"players\":[" + array(players) + "]}";
    }

    private static String array(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(Json.write(items.get(i)));
        }
        return sb.toString();
    }

    static String emote(String from, String id) {
        return "{\"type\":\"" + GameProtocol.EMOTE + "\",\"from\":" + Json.write(from) + ",\"id\":" + Json.write(id) + "}";
    }

    static String chat(String from, String text) {
        return "{\"type\":\"" + GameProtocol.CHAT + "\",\"from\":" + Json.write(from) + ",\"text\":" + Json.write(text) + "}";
    }

    /// WebRTC signaling, relayed untouched: `data` is an offer, an answer or
    /// an ICE candidate, opaque to the server.
    static String voice(String from, String data) {
        return "{\"type\":\"" + GameProtocol.VOICE + "\",\"from\":" + Json.write(from) + ",\"data\":" + Json.write(data) + "}";
    }

    static String mute(String from, boolean on) {
        return "{\"type\":\"" + GameProtocol.MUTE + "\",\"from\":" + Json.write(from) + ",\"on\":" + on + "}";
    }

    static String end(String matchId, int[] places, int[] scores, int trophies, int coins) {
        return "{\"type\":\"" + GameProtocol.END + "\",\"match\":\"" + matchId + "\",\"places\":\"" + join(places)
                + "\",\"scores\":\"" + join(scores) + "\",\"trophies\":" + trophies + ",\"coins\":" + coins + "}";
    }

    static String join(int[] v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.toString();
    }
}
