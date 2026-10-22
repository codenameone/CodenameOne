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
package com.codenameone.examples.parley.live;

import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.WebSocketMapping;
import com.codenameone.examples.parley.account.Accounts;
import com.codenameone.examples.parley.api.UserDto;
import com.codenameone.examples.parley.call.Calls;
import com.codenameone.examples.parley.device.Devices;

/// The live channel: `/ws/live?ticket=...&device=...`.
///
/// A ticket from `POST /api/live/ticket` opens one connection, once; a socket
/// cannot carry the bearer token the API takes, and a token in a URL ends up
/// in logs. The device must be one of the ticket holder's own, because what
/// arrives here is that device's envelopes.
@WebSocketMapping("/ws/live")
public class LiveEndpoint implements WebSocket {
    private final LiveTickets tickets;
    private final Accounts accounts;
    private final Devices devices;
    private final LiveHub hub;
    private final Calls calls;

    public LiveEndpoint(LiveTickets tickets, Accounts accounts, Devices devices, LiveHub hub,
            Calls calls) {
        this.tickets = tickets;
        this.accounts = accounts;
        this.devices = devices;
        this.hub = hub;
        this.calls = calls;
    }

    @Override
    public void onOpen(WebSocketSession session) throws Exception {
        String username = tickets.redeem(param(session.getQuery(), "ticket"));
        String device = param(session.getQuery(), "device");
        UserDto user = username == null ? null : accounts.describe(username);
        if (user == null || user.suspended() || device == null
                || !devices.owns(username, device)) {
            session.close(4401, "ticket");
            return;
        }
        LiveHub.Peer peer = new LiveHub.Peer(username, device, user.admin());
        session.setAttachment(peer);
        hub.add(peer, session);
        // Read again once registered: a block or an unlink that landed
        // between the first read and the registration would otherwise leave
        // this connection open, since its drop() ran before there was
        // anything to drop.
        UserDto now = accounts.describe(username);
        if (now == null || now.suspended() || now.admin() != user.admin()
                || !devices.owns(username, device)) {
            hub.remove(peer, session);
            session.close(4403, "changed");
            return;
        }
        devices.seen(username, device);
        session.sendText("{\"type\":\"ready\"}");
        calls.ringDevice(username, device);
    }

    @Override
    public void onText(WebSocketSession session, String message) throws Exception {
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
        Object who = session.getAttachment();
        if (who instanceof LiveHub.Peer) {
            LiveHub.Peer peer = (LiveHub.Peer) who;
            hub.remove(peer, session);
            try {
                devices.seen(peer.username(), peer.device());
            } catch (Exception gone) {
                // The account or the device went with the connection.
            }
        }
    }

    static String param(String query, String name) {
        if (query == null) {
            return null;
        }
        String key = name + "=";
        int at = 0;
        while (at < query.length()) {
            int end = query.indexOf('&', at);
            if (end < 0) {
                end = query.length();
            }
            if (query.startsWith(key, at)) {
                return query.substring(at + key.length(), end);
            }
            at = end + 1;
        }
        return null;
    }
}
