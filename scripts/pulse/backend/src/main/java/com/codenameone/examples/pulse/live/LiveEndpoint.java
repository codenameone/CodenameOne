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
package com.codenameone.examples.pulse.live;

import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.WebSocketMapping;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.api.UserDto;

/// The live channel: one WebSocket per signed-in app, down which the server
/// says what changed ([LiveHub]).
///
/// No filter chain covers this path, deliberately: the handshake cannot carry
/// the bearer token on every platform. The connection is let in by the ticket
/// in its address instead ([LiveTickets]), and one that presents none, or one
/// already used, is closed before it is told anything.
@WebSocketMapping("/ws/live")
public class LiveEndpoint implements WebSocket {
    private final LiveTickets tickets;
    private final Accounts accounts;
    private final LiveHub hub;

    public LiveEndpoint(LiveTickets tickets, Accounts accounts, LiveHub hub) {
        this.tickets = tickets;
        this.accounts = accounts;
        this.hub = hub;
    }

    @Override
    public void onOpen(WebSocketSession session) throws Exception {
        String username = tickets.redeem(ticket(session.getQuery()));
        UserDto user = username == null ? null : accounts.describe(username);
        if (user == null || user.suspended()) {
            session.close(4401, "ticket");
            return;
        }
        // Who this connection is, kept on the connection: the one piece of
        // state a WebSocket has that a request does not.
        session.setAttachment(username);
        hub.add(username, staff(user), session);
        // Read again now that the connection is in the hub. A block or a
        // withdrawn staff role commits and then drops the account's
        // connections; one that committed after the read above and dropped
        // before the add found nothing to drop, and this connection would
        // carry what was read before -- for staff, every check-in at the door.
        // Either the change is seen here, or its drop comes after the add and
        // closes this connection.
        UserDto now = accounts.describe(username);
        if (now == null || now.suspended() || staff(now) != staff(user)) {
            hub.remove(username, session);
            session.close(4403, "changed");
            return;
        }
        session.sendText("{\"type\":\"ready\"}");
    }

    @Override
    public void onText(WebSocketSession session, String message) throws Exception {
        // The app says one thing, to learn the connection is still alive on a
        // network that drops idle ones without telling either end.
        if (session.getAttachment() != null && message != null
                && message.indexOf("\"ping\"") >= 0) {
            session.sendText("{\"type\":\"pong\"}");
        }
    }

    @Override
    public void onBinary(WebSocketSession session, byte[] message, int offset, int length)
            throws Exception {
        session.close(1003, "text only");
    }

    @Override
    public void onClose(WebSocketSession session, int code, String reason) {
        Object username = session.getAttachment();
        if (username instanceof String) {
            hub.remove((String) username, session);
        }
    }

    /// The `ticket` parameter of a query string, or null. A ticket is hex, so
    /// there is nothing in it to decode.
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

    /// Whether an account is sent what only staff see.
    private static boolean staff(UserDto user) {
        return user.desk() || user.owner() || user.trainer();
    }
}
