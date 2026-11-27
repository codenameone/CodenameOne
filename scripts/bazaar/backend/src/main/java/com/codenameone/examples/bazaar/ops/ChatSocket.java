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
package com.codenameone.examples.bazaar.ops;

import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.WebSocketMapping;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.ChatMessageDto;
import com.codenameone.examples.bazaar.api.UserDto;
import com.codenameone.examples.bazaar.live.LiveTickets;
import com.codenameone.examples.bazaar.web.security.Throttle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The floating chat's socket, shared by the shopper's widget and the
/// store's inbox ([ChatLive] keeps who is who).
///
/// - A **visitor** connects with `?ticket=`, a single-use ticket the storefront
///   hands out from `POST /s/{handle}/chat/session` (`ChatWidget`).
/// - **Staff** connect with `?staff=<live ticket>&store=<id>`: the same
///   single-use ticket the app's live channel uses ([LiveTickets]), so the web
///   inbox and the app reach the chat the same way. The account must be on
///   the store's staff with the customers area, checked when the socket opens
///   and again for every conversation a frame names -- no filter chain stands
///   in front of a WebSocket.
///
/// A handshake from another origin is refused, so a page elsewhere cannot
/// ride a visitor's ticket. Frames are small JSON objects with an `op`; the
/// server answers with `type` frames (see `chat.js` and `inbox-live.js`).
@WebSocketMapping("/ws/chat")
public class ChatSocket implements WebSocket {
    /// The largest frame read: an SDP offer with its candidates fits easily.
    static final int MAX_FRAME = 40000;

    private final ChatLive live;
    private final ChatInbox inbox;
    private final LiveTickets tickets;
    private final Accounts accounts;
    private final Throttle throttle;
    private final DataSource db;

    public ChatSocket(ChatLive live, ChatInbox inbox, LiveTickets tickets, Accounts accounts,
            Throttle throttle, DataSource db) {
        this.live = live;
        this.inbox = inbox;
        this.tickets = tickets;
        this.accounts = accounts;
        this.throttle = throttle;
        this.db = db;
    }

    @Override
    public void onOpen(WebSocketSession session) throws Exception {
        if (!sameOrigin(session.getHandshakeHeader("Origin"), session.getHandshakeHeader("Host"))) {
            session.close(4403, "origin");
            return;
        }
        String query = session.getQuery();
        String staffTicket = param(query, "staff");
        if (staffTicket != null) {
            openStaff(session, staffTicket, param(query, "store"));
            return;
        }
        ChatLive.Ticket t = live.redeem(param(query, "ticket"));
        if (t == null) {
            session.close(4401, "ticket");
            return;
        }
        ChatLive.Peer p = live.visitor(session, t);
        Map<String, Object> hello = ChatLive.frame("hello");
        hello.put("store", t.storeName);
        hello.put("online", Integer.valueOf(live.staffOnline(t.storeId)));
        hello.put("ice", live.iceServers("v" + session.getId()));
        List<Map<String, Object>> lines = new ArrayList<Map<String, Object>>();
        if (p.conversationId.length() > 0) {
            Map c = inbox.row(p.conversationId);
            hello.put("staffRead", Long.valueOf(c == null ? 0 : Rows.number(c.get("staff_read"))));
            hello.put("email", Boolean.valueOf(c != null && Rows.text(c.get("email")).length() > 0));
            List<ChatMessageDto> thread = inbox.threadOf(p.conversationId);
            for (int iter = 0; iter < thread.size(); iter++) {
                lines.add(line(thread.get(iter), t.storeName));
            }
        }
        hello.put("messages", lines);
        session.sendText(Json.write(hello));
    }

    private void openStaff(WebSocketSession session, String ticket, String storeId)
            throws IOException {
        String username = tickets.redeem(ticket);
        UserDto user = username == null ? null : accounts.describe(username);
        if (user == null || user.suspended() || storeId == null || storeId.length() > 32) {
            session.close(4401, "ticket");
            return;
        }
        try {
            inbox.requireInbox(username, storeId);
        } catch (ResponseStatusException refused) {
            session.close(4403, "store");
            return;
        }
        Map store = db.queryOne("SELECT name FROM store WHERE id = ?", new Object[] {storeId});
        live.staff(session, storeId, store == null ? "" : Rows.text(store.get("name")), username);
        Map<String, Object> hello = ChatLive.frame("hello");
        hello.put("online", Integer.valueOf(live.staffOnline(storeId)));
        hello.put("ice", live.iceServers("s" + session.getId()));
        session.sendText(Json.write(hello));
    }

    static Map<String, Object> line(ChatMessageDto m, String storeName) {
        Map<String, Object> f = new LinkedHashMap<String, Object>();
        f.put("id", m.id());
        f.put("author", m.staff() && storeName != null ? storeName : m.author());
        f.put("staff", Boolean.valueOf(m.staff()));
        f.put("body", m.body());
        f.put("at", Long.valueOf(m.at()));
        return f;
    }

    @Override
    public void onText(WebSocketSession session, String message) throws Exception {
        Object a = session.getAttachment();
        if (message == null || message.length() > MAX_FRAME || a == null) {
            return;
        }
        Map f;
        try {
            f = Json.parseObject(message);
        } catch (IOException bad) {
            return;
        }
        String op = text(f, "op");
        if ("ping".equals(op)) {
            session.sendText("{\"type\":\"pong\"}");
            return;
        }
        try {
            if (a instanceof ChatLive.Peer) {
                ChatLive.Peer p = (ChatLive.Peer) a;
                if (p.staff) {
                    staffFrame(session, p, op, f);
                } else {
                    visitorFrame(session, p, op, f);
                }
            }
        } catch (ResponseStatusException refused) {
            Map<String, Object> e = ChatLive.frame("error");
            e.put("status", Integer.valueOf(refused.getStatus()));
            e.put("reason", refused.getReason());
            e.put("ref", text(f, "ref"));
            session.sendText(Json.write(e));
        }
    }

    private void visitorFrame(WebSocketSession session, ChatLive.Peer p, String op, Map f)
            throws IOException {
        String chat = p.conversationId;
        if ("send".equals(op)) {
            ChatMessageDto m;
            if (chat.length() == 0) {
                String id = inbox.start(p.storeId, p.client, p.username, text(f, "email"),
                        text(f, "name"), text(f, "body"));
                live.attach(session, id);
                Map c = inbox.row(id);
                Map<String, Object> t = ChatLive.frame("token");
                t.put("token", c == null ? "" : Rows.text(c.get("token")));
                session.sendText(Json.write(t));
                List<ChatMessageDto> thread = inbox.threadOf(id);
                m = thread.get(thread.size() - 1);
            } else {
                m = inbox.say(p.storeId, chat, text(f, "body"));
            }
            Map<String, Object> ack = ChatLive.frame("sent");
            ack.put("ref", text(f, "ref"));
            ack.put("id", m.id());
            ack.put("at", Long.valueOf(m.at()));
            session.sendText(Json.write(ack));
            return;
        }
        if (chat.length() == 0) {
            return;
        }
        if ("email".equals(op)) {
            inbox.leaveEmail(chat, text(f, "email"));
            session.sendText("{\"type\":\"email\"}");
            return;
        }
        throttle.check("chat-signal", "v:" + chat);
        if ("typing".equals(op)) {
            Map<String, Object> t = ChatLive.frame("typing");
            t.put("chat", chat);
            t.put("staff", Boolean.FALSE);
            live.toStaff(p.storeId, t, null);
        } else if ("seen".equals(op)) {
            long at = Math.min(number(f, "at"), System.currentTimeMillis());
            inbox.seen(chat, false, at);
            Map<String, Object> s = ChatLive.frame("seen");
            s.put("chat", chat);
            s.put("staff", Boolean.FALSE);
            s.put("at", Long.valueOf(at));
            live.toStaff(p.storeId, s, null);
        } else if ("call".equals(op)) {
            visitorCall(session, p, chat, f);
        }
    }

    private void visitorCall(WebSocketSession session, ChatLive.Peer p, String chat, Map f)
            throws IOException {
        String kind = text(f, "kind");
        if ("RING".equals(kind)) {
            boolean video = Boolean.TRUE.equals(f.get("video"));
            if (!live.ring(chat, p.storeId, session, video)) {
                Map<String, Object> u = ChatLive.frame("call");
                u.put("kind", "UNAVAILABLE");
                session.sendText(Json.write(u));
                return;
            }
            Map c = inbox.row(chat);
            Map<String, Object> r = ChatLive.frame("call");
            r.put("chat", chat);
            r.put("kind", "RING");
            r.put("video", Boolean.valueOf(video));
            r.put("name", c == null ? "" : Rows.text(c.get("name")));
            live.toStaff(p.storeId, r, null);
        } else if ("END".equals(kind)) {
            if (live.onCall(chat, session)) {
                live.end(chat, session);
            }
        } else if ("OFFER".equals(kind) || "ICE".equals(kind)) {
            live.relay(session, p, chat, kind, signal(chat, kind, f));
        }
    }

    private void staffFrame(WebSocketSession session, ChatLive.Peer p, String op, Map f)
            throws IOException {
        String chat = text(f, "chat");
        // Every frame that names a conversation is checked against the store
        // and the caller's inbox rights, as a REST call would be.
        Map c = inbox.staffConversation(p.username, p.storeId, chat);
        String id = Rows.text(c.get("id"));
        if ("watch".equals(op)) {
            Map<String, Object> t = ChatLive.frame("thread");
            t.put("chat", id);
            t.put("name", Rows.text(c.get("name")));
            t.put("online", Boolean.valueOf(live.visitorOnline(id)));
            Map row = inbox.row(id);
            t.put("visitorRead", Long.valueOf(row == null ? 0 : Rows.number(row.get("visitor_read"))));
            List<Map<String, Object>> lines = new ArrayList<Map<String, Object>>();
            List<ChatMessageDto> thread = inbox.threadOf(id);
            for (int iter = 0; iter < thread.size(); iter++) {
                lines.add(line(thread.get(iter), null));
            }
            t.put("messages", lines);
            session.sendText(Json.write(t));
            Map<String, Object> call = live.callState(id);
            if (call != null) {
                session.sendText(Json.write(call));
                List<String> setup = live.setup(id);
                for (int iter = 0; iter < setup.size(); iter++) {
                    session.sendText(setup.get(iter));
                }
            }
            return;
        }
        if ("send".equals(op)) {
            inbox.answer(p.username, p.storeId, id, text(f, "body"));
            return;
        }
        throttle.check("chat-signal", "s:" + p.username);
        if ("typing".equals(op)) {
            Map<String, Object> t = ChatLive.frame("typing");
            t.put("staff", Boolean.TRUE);
            live.toVisitor(id, t, null);
            live.toStaff(p.storeId, withChat(t, id), session);
        } else if ("seen".equals(op)) {
            long at = Math.min(number(f, "at"), System.currentTimeMillis());
            inbox.seen(id, true, at);
            Map<String, Object> s = ChatLive.frame("seen");
            s.put("staff", Boolean.TRUE);
            s.put("at", Long.valueOf(at));
            live.toVisitor(id, s, null);
        } else if ("call".equals(op)) {
            String kind = text(f, "kind");
            if ("END".equals(kind)) {
                if (live.onCall(id, session)) {
                    live.end(id, session);
                }
            } else if ("ANSWER".equals(kind) || "ICE".equals(kind) || "DECLINE".equals(kind)) {
                live.relay(session, p, id, kind, signal(id, kind, f));
            }
        }
    }

    private static Map<String, Object> withChat(Map<String, Object> f, String chat) {
        Map<String, Object> out = new LinkedHashMap<String, Object>(f);
        out.put("chat", chat);
        return out;
    }

    /// The frame a call signal is passed on as: only the fields WebRTC needs,
    /// never whatever else the sender put in it.
    private static Map<String, Object> signal(String chat, String kind, Map f) {
        Map<String, Object> s = ChatLive.frame("call");
        s.put("chat", chat);
        s.put("kind", kind);
        Object sdp = f.get("sdp");
        if (sdp instanceof String) {
            s.put("sdp", sdp);
        }
        Object candidate = f.get("candidate");
        if (candidate instanceof Map) {
            s.put("candidate", candidate);
        }
        if (f.get("video") instanceof Boolean) {
            s.put("video", f.get("video"));
        }
        return s;
    }

    @Override
    public void onBinary(WebSocketSession session, byte[] message, int offset, int length) {
        session.close(1003, "text only");
    }

    @Override
    public void onClose(WebSocketSession session, int code, String reason) {
        live.remove(session);
    }

    @Override
    public void onError(WebSocketSession session, Exception error) {
        System.err.println("[chat] websocket " + session.getId() + " failed: " + error);
    }

    /// Whether the handshake came from this site's own pages. A browser always
    /// sends `Origin` on a WebSocket handshake; a client with none (a test, a
    /// native app) still needs a ticket.
    static boolean sameOrigin(String origin, String host) {
        if (origin == null || origin.length() == 0) {
            return true;
        }
        if (host == null) {
            return false;
        }
        int scheme = origin.indexOf("://");
        return scheme > 0 && origin.substring(scheme + 3).equalsIgnoreCase(host);
    }

    static String text(Map f, String key) {
        Object v = f.get(key);
        return v instanceof String ? (String) v : "";
    }

    static long number(Map f, String key) {
        Object v = f.get(key);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    /// One parameter of a query string (tickets and ids are plain, nothing to
    /// decode), or null.
    static String param(String query, String name) {
        if (query == null) {
            return null;
        }
        int at = 0;
        String key = name + "=";
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
