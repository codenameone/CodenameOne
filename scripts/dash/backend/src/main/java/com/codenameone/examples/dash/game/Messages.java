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

import com.codename1.backend.Json;
import com.codenameone.examples.dash.api.GameProtocol;
import com.codenameone.examples.dash.rules.RulesVersion;
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
    /// for a spectator.
    static String start(RaceRoom room, int you, long startIn) {
        StringBuilder names = new StringBuilder();
        StringBuilder runners = new StringBuilder();
        StringBuilder players = new StringBuilder();
        for (int i = 0; i < room.seats.length; i++) {
            if (i > 0) {
                names.append(',');
                runners.append(',');
                players.append(',');
            }
            names.append(Json.write(room.seats[i].name));
            runners.append(Json.write(room.seats[i].runner));
            // A bot has no account, so no username: an empty one, which a
            // client knows not to call.
            String u = room.seats[i].username;
            players.append(Json.write(u == null ? "" : u));
        }
        return "{\"type\":\"" + GameProtocol.START + "\",\"race\":\"" + room.id + "\",\"level\":"
                + Json.write(room.level) + ",\"seed\":\"" + room.run.seed() + "\",\"you\":" + you
                + ",\"names\":[" + names + "],\"runners\":[" + runners + "],\"players\":[" + players + "],\"rules\":"
                + Json.write(RulesVersion.ID) + ",\"tick\":" + room.run.tick() + ",\"startIn\":"
                + Math.max(0, startIn) + ",\"log\":" + Json.write(inputs(room.log, 0)) + "}";
    }

    static String ticks(int through, List<int[]> log, int from) {
        return "{\"type\":\"" + GameProtocol.TICKS + "\",\"t\":" + through + ",\"i\":"
                + Json.write(inputs(log, from)) + "}";
    }

    static String hash(int tick, long hash) {
        return "{\"type\":\"" + GameProtocol.HASH + "\",\"t\":" + tick + ",\"h\":\"" + Long.toHexString(hash) + "\"}";
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

    static String end(String raceId, int[] places, int[] scores, int trophies, int coins) {
        return "{\"type\":\"" + GameProtocol.END + "\",\"race\":\"" + raceId + "\",\"places\":\"" + join(places)
                + "\",\"scores\":\"" + join(scores) + "\",\"trophies\":" + trophies + ",\"coins\":" + coins + "}";
    }

    /// Inputs as `tick,seat,bits` joined by `;`, the replay's own format.
    static String inputs(List<int[]> log, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < log.size(); i++) {
            if (i > from) {
                sb.append(';');
            }
            int[] e = log.get(i);
            sb.append(e[0]).append(',').append(e[1]).append(',').append(e[2]);
        }
        return sb.toString();
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
