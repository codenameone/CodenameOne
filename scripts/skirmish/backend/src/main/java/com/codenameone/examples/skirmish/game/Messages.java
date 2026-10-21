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

import com.codename1.backend.Json;
import com.codenameone.examples.skirmish.api.GameProtocol;
import com.codenameone.examples.skirmish.rules.Command;
import com.codenameone.examples.skirmish.rules.Match;
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

    static String start(MatchRoom room, int you) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"").append(GameProtocol.START).append("\",\"match\":\"").append(room.id)
                .append("\",\"seed\":\"").append(room.match.seed()).append("\",\"you\":").append(you)
                .append(",\"deck0\":").append(Json.write(join(room.seats[0].deck)))
                .append(",\"deck1\":").append(Json.write(join(room.seats[1].deck)))
                .append(",\"name0\":").append(Json.write(room.seats[0].name))
                .append(",\"name1\":").append(Json.write(room.seats[1].name))
                .append(",\"trophies0\":").append(room.seats[0].trophies)
                .append(",\"trophies1\":").append(room.seats[1].trophies)
                .append(",\"rules\":").append(Json.write(com.codenameone.examples.skirmish.rules.RulesVersion.ID))
                .append(",\"tick\":").append(room.match.tick())
                .append(",\"log\":").append(Json.write(commands(room.log, 0)))
                .append('}');
        return sb.toString();
    }

    static String ticks(int through, List<Command> log, int from) {
        return "{\"type\":\"" + GameProtocol.TICKS + "\",\"t\":" + through + ",\"c\":"
                + Json.write(commands(log, from)) + "}";
    }

    static String hash(int tick, long hash) {
        return "{\"type\":\"" + GameProtocol.HASH + "\",\"t\":" + tick + ",\"h\":\"" + Match.hex(hash) + "\"}";
    }

    static String emote(int player, String id) {
        return "{\"type\":\"" + GameProtocol.EMOTE + "\",\"player\":" + player + ",\"id\":" + Json.write(id) + "}";
    }

    static String end(int result, int crowns0, int crowns1, int trophies, int coins, String matchId) {
        return "{\"type\":\"" + GameProtocol.END + "\",\"result\":" + result + ",\"crowns0\":" + crowns0
                + ",\"crowns1\":" + crowns1 + ",\"trophies\":" + trophies + ",\"coins\":" + coins
                + ",\"match\":\"" + matchId + "\"}";
    }

    /// Commands joined by `;`, in the replay format.
    static String commands(List<Command> log, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < log.size(); i++) {
            if (i > from) {
                sb.append(';');
            }
            sb.append(log.get(i).encode());
        }
        return sb.toString();
    }

    static String join(String[] items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(items[i]);
        }
        return sb.toString();
    }
}
