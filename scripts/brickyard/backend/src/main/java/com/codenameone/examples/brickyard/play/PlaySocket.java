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
package com.codenameone.examples.brickyard.play;

import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.WebSocketMapping;
import com.codenameone.examples.brickyard.account.Accounts;
import java.io.IOException;

/// The game socket. A valid ticket seats the session on its room; from then
/// on every binary frame is a replication message, both ways. Nothing here
/// touches the engine: input is queued onto the room's own thread.
@WebSocketMapping("/ws/play")
public class PlaySocket implements WebSocket {
    /// Messages larger than this are not a replication message.
    static final int MAX_FRAME = 64 * 1024;

    private final PlayTickets tickets;
    private final PlayServers servers;
    private final Accounts accounts;

    public PlaySocket(PlayTickets tickets, PlayServers servers, Accounts accounts) {
        this.tickets = tickets;
        this.servers = servers;
        this.accounts = accounts;
    }

    /// The session's seat: its room and the link the room writes to.
    static final class Bound implements PlayRoom.Link {
        final PlayRoom room;
        final WebSocketSession session;

        Bound(PlayRoom room, WebSocketSession session) {
            this.room = room;
            this.session = session;
        }

        @Override
        public void send(byte[] message) {
            if (!session.isOpen()) {
                return;
            }
            try {
                session.sendBinary(message);
            } catch (IOException gone) {
                // The close callback disconnects the seat.
            }
        }
    }

    @Override
    public void onOpen(WebSocketSession session) throws Exception {
        PlayTickets.Seat seat = tickets.redeem(ticket(session.getQuery()));
        PlayRoom room = seat == null ? null : servers.room(seat.serverId);
        if (room == null || room.isClosed() || !accounts.exists(seat.username)) {
            session.close(4401, "ticket");
            return;
        }
        Bound b = new Bound(room, session);
        session.setAttachment(b);
        String display = accounts.describe(seat.username).displayName();
        room.connect(b, Math.abs((long) seat.username.hashCode()) + 1, seat.username,
                display == null || display.length() == 0 ? seat.username : display);
    }

    @Override
    public void onText(WebSocketSession session, String message) throws Exception {
        session.close(1003, "binary only");
    }

    @Override
    public void onBinary(WebSocketSession session, byte[] message, int offset, int length) throws Exception {
        Object a = session.getAttachment();
        if (!(a instanceof Bound) || length > MAX_FRAME) {
            return;
        }
        byte[] copy = new byte[length];
        System.arraycopy(message, offset, copy, 0, length);
        ((Bound) a).room.receive((Bound) a, copy);
    }

    @Override
    public void onClose(WebSocketSession session, int code, String reason) {
        Object a = session.getAttachment();
        if (a instanceof Bound) {
            ((Bound) a).room.disconnect((Bound) a);
        }
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
