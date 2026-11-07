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
import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.WebSocketMapping;
import com.codenameone.examples.splat.account.Accounts;
import com.codenameone.examples.splat.api.GameProtocol;
import com.codenameone.examples.splat.domain.Profile;
import com.codenameone.examples.splat.rules.Input;
import java.util.Map;

/// The game socket. It is opened with a one-shot ticket from
/// `POST /api/game/ticket`, then carries [GameProtocol] messages.
// tag::splat-socket[]
@WebSocketMapping("/ws/game")
public class GameSocket implements WebSocket {
    private final GameTickets tickets;
    private final Accounts accounts;
    private final Rooms rooms;

    public GameSocket(GameTickets tickets, Accounts accounts, Rooms rooms) {
        this.tickets = tickets;
        this.accounts = accounts;
        this.rooms = rooms;
    }

    @Override
    public void onOpen(WebSocketSession session) throws Exception {
        String username = tickets.redeem(ticket(session.getQuery()));
        if (username == null || !accounts.isEnabled(username)) {
            session.close(4401, "ticket");
            return;
        }
        session.setAttachment(username);
        session.sendText(Messages.simple(GameProtocol.READY));
        rooms.connected(username, accounts.isAdmin(username), session);
    }

    @Override
    public void onText(WebSocketSession session, String message) throws Exception {
        Object who = session.getAttachment();
        if (!(who instanceof String) || message == null || message.length() > GameProtocol.MAX_MESSAGE) {
            return;
        }
        String username = (String) who;
        Map m;
        try {
            m = Json.parseObject(message);
        } catch (Exception bad) {
            session.sendText(Messages.error("Not a message"));
            return;
        }
        String type = string(m, "type");
        if (GameProtocol.PING.equals(type)) {
            session.sendText(Messages.simple(GameProtocol.PONG));
        } else if (GameProtocol.INPUT.equals(type)) {
            Input in = input(string(m, "i"));
            if (in != null) {
                rooms.input(username, number(m, "s"), in);
            }
        } else if (GameProtocol.VOICE.equals(type)) {
            rooms.voice(username, string(m, "to"), string(m, "data"));
        } else if (GameProtocol.MUTE.equals(type)) {
            rooms.mute(username, Boolean.TRUE.equals(m.get("on")));
        } else if (GameProtocol.EMOTE.equals(type) || GameProtocol.CHAT.equals(type)) {
            if (!rooms.maySay(username)) {
                return;
            }
            Profile p = accounts.profile(username);
            if (p.isMuted()) {
                return;
            }
            if (GameProtocol.EMOTE.equals(type)) {
                rooms.emote(username, p.getDisplayName(), string(m, "id"));
            } else {
                rooms.chat(username, p.getDisplayName(), string(m, "text"));
            }
        } else if (GameProtocol.QUICK.equals(type)) {
            rooms.quick(accounts.profile(username), session, string(m, "level"));
        } else if (GameProtocol.HOST.equals(type)) {
            rooms.host(accounts.profile(username), session, string(m, "level"), string(m, "crew"));
        } else if (GameProtocol.JOIN.equals(type)) {
            rooms.join(accounts.profile(username), session, string(m, "code"));
        } else if (GameProtocol.GO.equals(type)) {
            rooms.go(username, string(m, "level"));
        } else if (GameProtocol.CANCEL.equals(type)) {
            rooms.cancel(username);
        } else if (GameProtocol.LEAVE.equals(type)) {
            rooms.leave(username);
        } else if (GameProtocol.RESUME.equals(type)) {
            rooms.connected(username, accounts.isAdmin(username), session);
        } else if (GameProtocol.SPECTATE.equals(type)) {
            if (!accounts.isAdmin(username) || !rooms.spectate(string(m, "match"), session)) {
                session.sendText(Messages.error("No such match"));
            }
        }
    }

    @Override
    public void onBinary(WebSocketSession session, byte[] message, int offset, int length) throws Exception {
        session.close(1003, "text only");
    }

    @Override
    public void onError(WebSocketSession session, Exception error) {
        System.err.println("[socket] error on " + session.getAttachment() + ": " + error);
    }

    @Override
    public void onClose(WebSocketSession session, int code, String reason) {
        Object who = session.getAttachment();
        if (who instanceof String) {
            rooms.disconnected((String) who, session);
        }
    }

    /// Parses an input without trusting it: anything malformed is dropped.
    static Input input(String s) {
        if (s == null || s.length() > 64) {
            return null;
        }
        java.util.List<String> w = com.codenameone.examples.splat.rules.Text.words(s);
        if (w.size() != 6) {
            return null;
        }
        int[] v = new int[6];
        for (int i = 0; i < 6; i++) {
            String p = w.get(i);
            int start = p.startsWith("-") ? 1 : 0;
            if (p.length() == start || p.length() > 9) {
                return null;
            }
            for (int c = start; c < p.length(); c++) {
                if (p.charAt(c) < '0' || p.charAt(c) > '9') {
                    return null;
                }
            }
            v[i] = Integer.parseInt(p);
        }
        return new Input(v[0], v[1], v[2], v[3], v[4], v[5]);
    }

    private static String string(Map m, String key) {
        Object v = m.get(key);
        return v instanceof String ? (String) v : null;
    }

    private static int number(Map m, String key) {
        Object v = m.get(key);
        return v instanceof Number ? ((Number) v).intValue() : -1;
    }

    static String ticket(String query) {
        if (query == null) {
            return null;
        }
        int at = 0;
        while (at < query.length()) {
            int end = query.indexOf('&', at);
            if (end < 0) {
                end = query.length();
            }
            if (query.startsWith("ticket=", at)) {
                return query.substring(at + 7, end);
            }
            at = end + 1;
        }
        return null;
    }
}
// end::splat-socket[]
