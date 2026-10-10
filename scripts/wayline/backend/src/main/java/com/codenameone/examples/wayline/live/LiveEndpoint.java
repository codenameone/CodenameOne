/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.live;

import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.WebSocketMapping;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.api.UserDto;

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
        if (user == null || user.suspended) {
            session.close(4401, "ticket");
            return;
        }
        // Who this connection is, kept on the connection: the one piece of
        // state a WebSocket has that a request does not.
        session.setAttachment(username);
        hub.add(username, user.admin, session);
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
}
