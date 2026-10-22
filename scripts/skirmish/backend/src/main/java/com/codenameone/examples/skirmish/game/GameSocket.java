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
import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.WebSocketMapping;
import com.codenameone.examples.skirmish.account.Accounts;
import com.codenameone.examples.skirmish.api.GameProtocol;
import com.codenameone.examples.skirmish.domain.Profile;
import java.util.Map;

/// The game socket. It is opened with a one-shot ticket from
/// `POST /api/game/ticket`, then carries [GameProtocol] messages.
// tag::skirmish-socket[]
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
        if (!(who instanceof String) || message == null || message.length() > 2048) {
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
        } else if (GameProtocol.PLAY.equals(type)) {
            rooms.play(username, string(m, "card"), number(m, "x"), number(m, "y"));
        } else if (GameProtocol.SYNC.equals(type)) {
            rooms.sync(username, number(m, "t"), string(m, "h"));
        } else if (GameProtocol.EMOTE.equals(type)) {
            if (!accounts.profile(username).isMuted()) {
                rooms.emote(username, string(m, "id"));
            }
        } else if (GameProtocol.QUEUE.equals(type)) {
            rooms.queue(accounts.profile(username), session);
        } else if (GameProtocol.CHALLENGE.equals(type)) {
            Profile p = accounts.profile(username);
            rooms.challenge(p, session, string(m, "code"));
        } else if (GameProtocol.CANCEL.equals(type)) {
            rooms.cancel(username);
        } else if (GameProtocol.LEAVE.equals(type)) {
            rooms.leave(username);
        } else if (GameProtocol.RESUME.equals(type)) {
            rooms.connected(username, false, session);
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
    public void onClose(WebSocketSession session, int code, String reason) {
        Object who = session.getAttachment();
        if (who instanceof String) {
            rooms.disconnected((String) who, session);
        }
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
// end::skirmish-socket[]
