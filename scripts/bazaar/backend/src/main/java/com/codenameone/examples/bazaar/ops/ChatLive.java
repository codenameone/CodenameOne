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

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.Json;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bazaar.Ids;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The floating storefront chat's live side: who is connected, the tickets a
/// browser opens its socket with, and the call a shopper is placing.
///
/// Two kinds of connection meet here, both on [ChatSocket]:
///
/// - a **visitor**, the widget on a storefront page, attached to one
///   conversation (or to none yet, before its first message);
/// - **staff**, the web inbox (and any client holding a live ticket), attached
///   to one store and hearing every conversation in it.
///
/// Messages themselves are rows ([ChatInbox]); what passes through here is the
/// news that one arrived, typing, read marks, presence and the WebRTC
/// signalling of a call. The server never touches call media: the two browsers
/// exchange an offer, an answer and their ICE candidates through [#relay] and
/// then talk directly (or through a TURN server when one is configured). This
/// is Parley's call model (`call/Calls`) cut down to what a shop needs: one
/// shopper rings the store, the first member of staff who answers takes the
/// call, and a member of staff who opens the conversation after it began
/// ringing still gets the offer and the candidates so far.
///
/// Like [com.codenameone.examples.bazaar.live.LiveHub], the state is this
/// process's: more than one instance needs a channel between them.
@Component
public class ChatLive {
    /// How long a visitor ticket is good for.
    static final long TICKET_MILLIS = 30000L;
    /// How long a call rings before it is given up as missed.
    static final long RING_MILLIS = 45000L;
    /// How much signalling a ringing call keeps for a late answerer.
    static final int MAX_SETUP = 32768;

    private final String stun;
    private final String turn;
    private final String turnSecret;
    private final int turnTtl;

    private final Map<String, Ticket> tickets = new HashMap<String, Ticket>();
    private final Map<String, List<WebSocketSession>> visitors =
            new HashMap<String, List<WebSocketSession>>();
    private final Map<String, List<WebSocketSession>> staff =
            new HashMap<String, List<WebSocketSession>>();
    private final Map<String, Call> calls = new HashMap<String, Call>();

    public ChatLive(@Value("${bazaar.ice.stun:stun:stun.l.google.com:19302}") String stun,
            @Value("${bazaar.ice.turn:}") String turn,
            @Value("${bazaar.ice.secret:}") String turnSecret,
            @Value("${bazaar.ice.ttl:86400}") int turnTtl) {
        this.stun = stun == null ? "" : stun.trim();
        this.turn = turn == null ? "" : turn.trim();
        this.turnSecret = turnSecret == null ? "" : turnSecret;
        this.turnTtl = turnTtl;
    }

    /// What a visitor ticket opens: a store, and the conversation (empty
    /// before the first message) and signed-in customer (empty for a guest).
    public static final class Ticket {
        public final String storeId;
        public final String storeName;
        public final String conversationId;
        public final String customer;
        /// The address the ticket was asked from, which the `chat` rule keys on.
        public final String client;
        final long expires;

        Ticket(String storeId, String storeName, String conversationId, String customer,
                String client, long expires) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.conversationId = conversationId;
            this.customer = customer;
            this.client = client;
            this.expires = expires;
        }
    }

    /// What a connection is, kept as its attachment.
    public static final class Peer {
        public final boolean staff;
        public final String storeId;
        public final String storeName;
        /// A staff member's username, or the signed-in customer (may be empty).
        public final String username;
        /// A visitor's client address (see [Ticket#client]); empty for staff.
        public final String client;
        /// The visitor's conversation; empty until the first message.
        public String conversationId;

        Peer(boolean staff, String storeId, String storeName, String username,
                String conversationId, String client) {
            this.staff = staff;
            this.storeId = storeId;
            this.storeName = storeName;
            this.username = username;
            this.conversationId = conversationId;
            this.client = client;
        }
    }

    /// A call a visitor placed: who answered (null while ringing) and what a
    /// late answerer needs to join it.
    static final class Call {
        final String storeId;
        final WebSocketSession caller;
        final boolean video;
        final long started;
        WebSocketSession answerer;
        final List<String> setup = new ArrayList<String>();
        int setupSize;

        Call(String storeId, WebSocketSession caller, boolean video, long started) {
            this.storeId = storeId;
            this.caller = caller;
            this.video = video;
            this.started = started;
        }
    }

    // ---------------------------------------------------------------- tickets

    /// A single-use ticket for a visitor's socket. Random, it names nothing,
    /// and it is stale in [#TICKET_MILLIS]: the conversation token, which is
    /// the visitor's lasting key, never travels in a WebSocket address.
    public String issue(String storeId, String storeName, String conversationId, String customer,
            String client) throws IOException {
        String ticket = Ids.hex(Crypto.randomBytes(24));
        long now = System.currentTimeMillis();
        synchronized (this) {
            // Swept as tickets are issued: one nobody came back for is a few bytes.
            if (tickets.size() > 256) {
                List<String> stale = new ArrayList<String>();
                for (Map.Entry<String, Ticket> e : tickets.entrySet()) {
                    if (e.getValue().expires < now) {
                        stale.add(e.getKey());
                    }
                }
                for (int iter = 0; iter < stale.size(); iter++) {
                    tickets.remove(stale.get(iter));
                }
            }
            tickets.put(ticket, new Ticket(storeId, storeName,
                    conversationId == null ? "" : conversationId,
                    customer == null ? "" : customer, client == null ? "" : client,
                    now + TICKET_MILLIS));
        }
        return ticket;
    }

    /// The ticket's grant, used up; null when it is unknown, used or stale.
    public synchronized Ticket redeem(String ticket) {
        if (ticket == null || ticket.length() == 0 || ticket.length() > 128) {
            return null;
        }
        Ticket t = tickets.remove(ticket);
        return t == null || t.expires < System.currentTimeMillis() ? null : t;
    }

    // ------------------------------------------------------------ connections

    Peer visitor(WebSocketSession session, Ticket t) {
        Peer p = new Peer(false, t.storeId, t.storeName, t.customer, t.conversationId, t.client);
        session.setAttachment(p);
        if (t.conversationId.length() > 0) {
            attach(session, t.conversationId);
        }
        return p;
    }

    /// Files a visitor's connection under its conversation, once it has one.
    synchronized void attach(WebSocketSession session, String conversationId) {
        add(visitors, conversationId, session);
        ((Peer) session.getAttachment()).conversationId = conversationId;
    }

    Peer staff(WebSocketSession session, String storeId, String storeName, String username) {
        Peer p = new Peer(true, storeId, storeName, username, "", "");
        session.setAttachment(p);
        int online;
        synchronized (this) {
            add(staff, storeId, session);
            online = staffOnline(storeId);
        }
        presence(storeId, online);
        return p;
    }

    void remove(WebSocketSession session) {
        Object a = session.getAttachment();
        if (!(a instanceof Peer)) {
            return;
        }
        Peer p = (Peer) a;
        int online = -1;
        List<String> ended = new ArrayList<String>();
        synchronized (this) {
            if (p.staff) {
                drop(staff, p.storeId, session);
                online = staffOnline(p.storeId);
            } else if (p.conversationId.length() > 0) {
                drop(visitors, p.conversationId, session);
            }
            for (Map.Entry<String, Call> e : calls.entrySet()) {
                if (e.getValue().caller == session || e.getValue().answerer == session) {
                    ended.add(e.getKey());
                }
            }
        }
        for (int iter = 0; iter < ended.size(); iter++) {
            end(ended.get(iter), session);
        }
        if (online >= 0) {
            presence(p.storeId, online);
        }
    }

    private static void add(Map<String, List<WebSocketSession>> map, String key,
            WebSocketSession session) {
        List<WebSocketSession> open = map.get(key);
        if (open == null) {
            open = new ArrayList<WebSocketSession>();
            map.put(key, open);
        }
        if (!open.contains(session)) {
            open.add(session);
        }
    }

    private static void drop(Map<String, List<WebSocketSession>> map, String key,
            WebSocketSession session) {
        List<WebSocketSession> open = map.get(key);
        if (open != null) {
            open.remove(session);
            if (open.isEmpty()) {
                map.remove(key);
            }
        }
    }

    /// How many staff connections a store has open: the widget's "online".
    public synchronized int staffOnline(String storeId) {
        List<WebSocketSession> open = staff.get(storeId);
        return open == null ? 0 : open.size();
    }

    /// Whether the visitor of a conversation has the widget open anywhere.
    public synchronized boolean visitorOnline(String conversationId) {
        List<WebSocketSession> open = visitors.get(conversationId);
        return open != null && !open.isEmpty();
    }

    // ----------------------------------------------------------------- frames

    /// Sends `frame` to the conversation's visitor connections, except `but`.
    public void toVisitor(String conversationId, Map<String, Object> frame, WebSocketSession but) {
        List<WebSocketSession> to;
        synchronized (this) {
            List<WebSocketSession> open = visitors.get(conversationId);
            to = open == null ? new ArrayList<WebSocketSession>()
                    : new ArrayList<WebSocketSession>(open);
        }
        send(to, Json.write(frame), but);
    }

    /// Sends `frame` to the store's staff connections, except `but`.
    public void toStaff(String storeId, Map<String, Object> frame, WebSocketSession but) {
        List<WebSocketSession> to;
        synchronized (this) {
            List<WebSocketSession> open = staff.get(storeId);
            to = open == null ? new ArrayList<WebSocketSession>()
                    : new ArrayList<WebSocketSession>(open);
        }
        send(to, Json.write(frame), but);
    }

    private void presence(String storeId, int online) {
        Map<String, Object> f = frame("presence");
        f.put("online", Integer.valueOf(online));
        List<WebSocketSession> to = new ArrayList<WebSocketSession>();
        synchronized (this) {
            for (List<WebSocketSession> open : visitors.values()) {
                for (int iter = 0; iter < open.size(); iter++) {
                    Object a = open.get(iter).getAttachment();
                    if (a instanceof Peer && storeId.equals(((Peer) a).storeId)) {
                        to.add(open.get(iter));
                    }
                }
            }
        }
        send(to, Json.write(f), null);
    }

    static Map<String, Object> frame(String type) {
        Map<String, Object> f = new LinkedHashMap<String, Object>();
        f.put("type", type);
        return f;
    }

    /// Sends outside the lock; a connection that cannot be written to is
    /// dropped and its close callback forgets it.
    static void send(List<WebSocketSession> to, String text, WebSocketSession but) {
        for (int iter = 0; iter < to.size(); iter++) {
            WebSocketSession s = to.get(iter);
            if (s == but) {
                continue;
            }
            try {
                s.sendText(text);
            } catch (IOException gone) {
                System.err.println("[chat] websocket " + s.getId() + " dropped: " + gone);
                s.abort();
            }
        }
    }

    // ------------------------------------------------------------------ calls

    /// The ICE servers a browser builds its peer connection with: the STUN
    /// server `bazaar.ice.stun` names, and a TURN server only when
    /// `bazaar.ice.turn` and its shared secret `bazaar.ice.secret` are both
    /// configured. TURN credentials follow the TURN REST convention (coturn's
    /// `use-auth-secret`), as Parley's do: a user name that is an expiry time
    /// and a password that is its HMAC under the secret, so nothing long-lived
    /// reaches a browser and no credential is written into the sample.
    public List<Map<String, Object>> iceServers(String who) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        if (stun.length() > 0) {
            Map<String, Object> s = new LinkedHashMap<String, Object>();
            s.put("urls", split(stun));
            out.add(s);
        }
        if (turn.length() > 0 && turnSecret.length() > 0) {
            String user = (System.currentTimeMillis() / 1000L + turnTtl) + ":" + who;
            String password = Base64.encode(Crypto.hmac(Crypto.SHA1,
                    turnSecret.getBytes(StandardCharsets.UTF_8),
                    user.getBytes(StandardCharsets.UTF_8)));
            Map<String, Object> s = new LinkedHashMap<String, Object>();
            s.put("urls", split(turn));
            s.put("username", user);
            s.put("credential", password);
            out.add(s);
        }
        return out;
    }

    private static List<String> split(String list) {
        List<String> out = new ArrayList<String>();
        int at = 0;
        while (at <= list.length()) {
            int comma = list.indexOf(',', at);
            if (comma < 0) {
                comma = list.length();
            }
            String one = list.substring(at, comma).trim();
            if (one.length() > 0) {
                out.add(one);
            }
            at = comma + 1;
        }
        return out;
    }

    /// A visitor rings the store. Answers false when a call is already up on
    /// the conversation or nobody on the staff is connected to take it.
    boolean ring(String conversationId, String storeId, WebSocketSession caller, boolean video) {
        expire();
        synchronized (this) {
            if (calls.containsKey(conversationId) || staffOnline(storeId) == 0) {
                return false;
            }
            calls.put(conversationId, new Call(storeId, caller, video,
                    System.currentTimeMillis()));
            return true;
        }
    }

    /// The call frame a member of staff opening the conversation needs:
    /// RING while it rings, TAKEN once someone answered, null for no call.
    synchronized Map<String, Object> callState(String conversationId) {
        Call c = calls.get(conversationId);
        if (c == null) {
            return null;
        }
        Map<String, Object> f = frame("call");
        f.put("chat", conversationId);
        f.put("kind", c.answerer == null ? "RING" : "TAKEN");
        f.put("video", Boolean.valueOf(c.video));
        return f;
    }

    /// The signalling a ringing call kept, for a member of staff who opened
    /// the conversation after it began; empty once someone answered.
    synchronized List<String> setup(String conversationId) {
        Call c = calls.get(conversationId);
        return c == null || c.answerer != null ? new ArrayList<String>()
                : new ArrayList<String>(c.setup);
    }

    /// Passes a call signal on. From the visitor it goes to whoever answered,
    /// or while ringing to every staff connection of the store (and is kept
    /// for a late answerer). From staff, ANSWER claims the call -- the first
    /// one wins, and the others are told it was taken -- and anything else
    /// goes to the visitor only from the member of staff who has it.
    /// Answers false when the signal belongs to no call this sender is in.
    boolean relay(WebSocketSession from, Peer peer, String conversationId, String kind,
            Map<String, Object> f) {
        String text = Json.write(f);
        WebSocketSession to = null;
        List<WebSocketSession> fan = null;
        boolean taken = false;
        String storeId;
        synchronized (this) {
            Call c = calls.get(conversationId);
            if (c == null) {
                return false;
            }
            storeId = c.storeId;
            if (!peer.staff) {
                if (c.caller != from) {
                    return false;
                }
                if (c.answerer != null) {
                    to = c.answerer;
                } else {
                    if (c.setupSize + text.length() <= MAX_SETUP) {
                        c.setup.add(text);
                        c.setupSize += text.length();
                    }
                    List<WebSocketSession> open = staff.get(c.storeId);
                    fan = open == null ? new ArrayList<WebSocketSession>()
                            : new ArrayList<WebSocketSession>(open);
                }
            } else {
                if (!storeId.equals(peer.storeId)) {
                    return false;
                }
                if (c.answerer == null && "ANSWER".equals(kind)) {
                    c.answerer = from;
                    c.setup.clear();
                    taken = true;
                } else if (c.answerer == null && "DECLINE".equals(kind)) {
                    // One member of staff declining leaves it ringing for the rest.
                    return true;
                } else if (c.answerer != from) {
                    return false;
                }
                to = c.caller;
            }
        }
        if (fan != null) {
            send(fan, text, null);
            return true;
        }
        send(single(to), text, null);
        if (taken) {
            Map<String, Object> t = frame("call");
            t.put("chat", conversationId);
            t.put("kind", "TAKEN");
            toStaff(storeId, t, from);
        }
        return true;
    }

    private static List<WebSocketSession> single(WebSocketSession s) {
        List<WebSocketSession> out = new ArrayList<WebSocketSession>();
        if (s != null) {
            out.add(s);
        }
        return out;
    }

    /// Ends a call, telling whoever is still on it (and the staff, while it
    /// was ringing). `by` is the connection that ended it, or null.
    void end(String conversationId, WebSocketSession by) {
        Call c;
        synchronized (this) {
            c = calls.remove(conversationId);
        }
        if (c == null) {
            return;
        }
        Map<String, Object> f = frame("call");
        f.put("chat", conversationId);
        f.put("kind", "END");
        String text = Json.write(f);
        if (c.caller != by) {
            send(single(c.caller), text, null);
        }
        if (c.answerer == null) {
            toStaff(c.storeId, f, by);
        } else if (c.answerer != by) {
            send(single(c.answerer), text, null);
        }
    }

    /// Whether `session` is on the conversation's call (either end).
    synchronized boolean onCall(String conversationId, WebSocketSession session) {
        Call c = calls.get(conversationId);
        return c != null && (c.caller == session || c.answerer == session);
    }

    /// Gives up the calls that rang out unanswered.
    void expire() {
        List<String> missed = new ArrayList<String>();
        long now = System.currentTimeMillis();
        synchronized (this) {
            for (Map.Entry<String, Call> e : calls.entrySet()) {
                if (e.getValue().answerer == null && now - e.getValue().started > RING_MILLIS) {
                    missed.add(e.getKey());
                }
            }
        }
        for (int iter = 0; iter < missed.size(); iter++) {
            end(missed.get(iter), null);
        }
    }
}
