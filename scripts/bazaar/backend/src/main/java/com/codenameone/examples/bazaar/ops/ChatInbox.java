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
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.CannedReplyDto;
import com.codenameone.examples.bazaar.api.ChatConversationDto;
import com.codenameone.examples.bazaar.api.ChatMessageDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.web.security.Throttle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Customer chat: a shopper writes from the floating storefront chat, the
/// store answers from its inbox, and the shopper reads the thread live
/// ([ChatLive], [ChatSocket]) and, when the chat is closed and an address was
/// left, by email.
///
/// A shopper's message is rate limited per client and per address under
/// the `chat` rule before anything is stored or sent, so the widget cannot
/// be used to mail-bomb a store's staff or a stranger's inbox.
@Component
public class ChatInbox {
    public static final String OPEN = "OPEN";
    public static final String CLOSED = "CLOSED";
    static final int MAX_BODY = 2000;

    private final DataSource db;
    private final StoreAccess access;
    private final Emails emails;
    private final Throttle throttle;
    private final ChatLive live;

    public ChatInbox(DataSource db, StoreAccess access, Emails emails, Throttle throttle,
            ChatLive live) {
        this.db = db;
        this.access = access;
        this.emails = emails;
        this.throttle = throttle;
        this.live = live;
    }

    /// Tells both sides of a conversation, live, about a message just written:
    /// the visitor sees a staff message under the store's name (never the
    /// staff member's account), the staff see who wrote and the conversation.
    private void push(String storeId, String conversationId, String name, ChatMessageDto m)
            throws IOException {
        java.util.Map<String, Object> f = ChatLive.frame("message");
        f.put("chat", conversationId);
        f.put("id", m.id());
        f.put("staff", Boolean.valueOf(m.staff()));
        f.put("body", m.body());
        f.put("at", Long.valueOf(m.at()));
        java.util.Map<String, Object> toStaff = new java.util.LinkedHashMap<String, Object>(f);
        toStaff.put("author", m.author());
        toStaff.put("name", name);
        live.toStaff(storeId, toStaff, null);
        // The visitor's own message is acknowledged on the socket it came
        // from (ChatSocket); echoing it here would show it twice.
        if (m.staff()) {
            Map store = db.queryOne("SELECT name FROM store WHERE id = ?", new Object[] {storeId});
            f.put("author", store == null ? "" : Rows.text(store.get("name")));
            live.toVisitor(conversationId, f, null);
        }
    }

    static String body(String text) {
        String b = text == null ? "" : text.trim();
        if (b.length() == 0) {
            throw new ResponseStatusException(400, "Write a message");
        }
        return Codes.cut(b, MAX_BODY);
    }

    static boolean email(String e) {
        int at = e.indexOf('@');
        return at > 0 && e.indexOf('.', at) > at + 1 && e.indexOf(' ') < 0 && e.length() <= 200
                && !e.endsWith(".");
    }

    /// A shopper's first message from the floating chat: a new conversation,
    /// answered with its id. The address is optional -- a shopper can chat
    /// without giving one -- but one that is given must look like one, and a
    /// signed-in `customer` is linked and lends the account's address.
    /// Starting conversations is held to the `chat` rule per client, since
    /// each can end in mail to the shop's staff.
    @Transactional(rollbackFor = IOException.class)
    public String start(String storeId, String client, String customer, String emailAddress,
            String name, String text) throws IOException {
        String b = body(text);
        throttle.check("chat", "c:" + client);
        String e = emailAddress == null ? "" : emailAddress.trim();
        if (e.length() > 0 && !email(e)) {
            throw new ResponseStatusException(400, "That email address does not look right");
        }
        String c = customer == null ? "" : customer;
        if (e.length() == 0 && c.length() > 0 && email(c)) {
            e = c;
        }
        String n = name != null && name.trim().length() > 0 ? Codes.cut(name.trim(), 100)
                : e.length() > 0 ? Codes.cut(e.substring(0, e.indexOf('@')), 100) : "Visitor";
        long now = System.currentTimeMillis();
        String id = Ids.next();
        String t = Ids.next() + Ids.next();
        db.execute("INSERT INTO chat_conversation (id, store_id, token, email, name, subject, status, "
                + "assignee, created_at, updated_at, customer, visitor_read, staff_read) VALUES "
                + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {
                    id, storeId, t, e, n, Codes.cut(b, 80), OPEN, "", Long.valueOf(now),
                    Long.valueOf(now), Codes.cut(c, 200), Long.valueOf(now), Long.valueOf(0)});
        String mid = message(storeId, id, n, false, b, now);
        push(storeId, id, n, new ChatMessageDto(mid, n, false, b, now));
        return id;
    }

    /// Another shopper message on a conversation that exists; reopens it.
    /// Held to the `chat-live` rule per conversation.
    @Transactional(rollbackFor = IOException.class)
    public ChatMessageDto say(String storeId, String conversationId, String text)
            throws IOException {
        String b = body(text);
        throttle.check("chat-live", "v:" + conversationId);
        Map c = row(conversationId);
        if (c == null || !storeId.equals(Rows.text(c.get("store_id")))) {
            throw new ResponseStatusException(404, "No such conversation");
        }
        long now = System.currentTimeMillis();
        String id = message(storeId, conversationId, Rows.text(c.get("name")), false, b, now);
        db.execute("UPDATE chat_conversation SET status = ?, updated_at = ?, visitor_read = ? "
                + "WHERE id = ?", new Object[] {OPEN, Long.valueOf(now), Long.valueOf(now),
                    conversationId});
        ChatMessageDto m = new ChatMessageDto(id, Rows.text(c.get("name")), false, b, now);
        push(storeId, conversationId, Rows.text(c.get("name")), m);
        return m;
    }

    /// A conversation's row by its id (store, token, names, read marks), or null.
    @Transactional(readOnly = true)
    public Map row(String conversationId) throws IOException {
        return conversationId == null || conversationId.length() == 0
                || conversationId.length() > 32 ? null : db.queryOne(
                "SELECT id, store_id, token, email, name, status, customer, visitor_read, "
                + "staff_read FROM chat_conversation WHERE id = ?", new Object[] {conversationId});
    }

    /// The conversation a visitor's lasting token opens at `storeId`, or null.
    @Transactional(readOnly = true)
    public Map byToken(String storeId, String token) throws IOException {
        Map c = token == null || token.length() == 0 || token.length() > 64 ? null
                : db.queryOne("SELECT id, store_id FROM chat_conversation WHERE token = ?",
                        new Object[] {token});
        return c == null || !storeId.equals(Rows.text(c.get("store_id"))) ? null
                : row(Rows.text(c.get("id")));
    }

    /// The newest conversation a signed-in customer has at `storeId`, or
    /// null: how a conversation follows its customer to another device.
    @Transactional(readOnly = true)
    public Map latestOf(String storeId, String customer) throws IOException {
        if (customer == null || customer.length() == 0) {
            return null;
        }
        List rows = db.query("SELECT id FROM chat_conversation WHERE store_id = ? AND customer = ? "
                + "ORDER BY updated_at DESC", new Object[] {storeId, customer});
        return rows.isEmpty() ? null : row(Rows.text(((Map) rows.get(0)).get("id")));
    }

    /// Links a guest's conversation to the customer who signed in, and takes
    /// the account's address when the conversation had none.
    @Transactional(rollbackFor = IOException.class)
    public void link(String conversationId, String customer) throws IOException {
        if (customer == null || customer.length() == 0) {
            return;
        }
        db.execute("UPDATE chat_conversation SET customer = ? WHERE id = ? AND customer = ''",
                new Object[] {Codes.cut(customer, 200), conversationId});
        if (email(customer)) {
            db.execute("UPDATE chat_conversation SET email = ? WHERE id = ? AND email = ''",
                    new Object[] {customer, conversationId});
        }
    }

    /// The address a shopper left from the widget, for replies by mail.
    @Transactional(rollbackFor = IOException.class)
    public void leaveEmail(String conversationId, String emailAddress) throws IOException {
        String e = emailAddress == null ? "" : emailAddress.trim();
        if (!email(e)) {
            throw new ResponseStatusException(400, "That email address does not look right");
        }
        throttle.check("chat-live", "v:" + conversationId);
        db.execute("UPDATE chat_conversation SET email = ? WHERE id = ?",
                new Object[] {e, conversationId});
    }

    /// Records that one side has read the conversation up to `at`.
    @Transactional(rollbackFor = IOException.class)
    public void seen(String conversationId, boolean staff, long at) throws IOException {
        String column = staff ? "staff_read" : "visitor_read";
        db.execute("UPDATE chat_conversation SET " + column + " = ? WHERE id = ? AND " + column
                + " < ?", new Object[] {Long.valueOf(at), conversationId, Long.valueOf(at)});
    }

    /// The thread of a conversation the caller already holds, oldest first.
    @Transactional(readOnly = true)
    public List<ChatMessageDto> threadOf(String conversationId) throws IOException {
        return messages(conversationId);
    }

    /// Checks that `username` may work the store's inbox (staff, with the
    /// customers area), as every live staff frame must: the socket has no
    /// filter chain in front of it.
    @Transactional(readOnly = true)
    public void requireInbox(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        access.requireArea(username, storeId, Perms.CUSTOMERS, true, StoreAccess.ANYONE);
    }

    /// The conversation `chatId` for a member of staff: refused (404) unless it
    /// is the store's and the caller may work its inbox.
    @Transactional(readOnly = true)
    public Map staffConversation(String username, String storeId, String chatId)
            throws IOException {
        return conversation(username, storeId, chatId, true);
    }

    private String message(String storeId, String conversationId, String author, boolean staff,
            String body, long now) throws IOException {
        String id = Ids.next();
        db.execute("INSERT INTO chat_message (id, store_id, conversation_id, author, staff, body, "
                + "created_at) VALUES (?, ?, ?, ?, ?, ?, ?)", new Object[] {id, storeId,
                    conversationId, Codes.cut(author, 200), Integer.valueOf(staff ? 1 : 0), body,
                    Long.valueOf(now)});
        return id;
    }

    private List<ChatMessageDto> messages(String conversationId) throws IOException {
        List rows = db.query("SELECT id, author, staff, body, created_at FROM chat_message WHERE "
                + "conversation_id = ? ORDER BY created_at, id", new Object[] {conversationId});
        List<ChatMessageDto> out = new ArrayList<ChatMessageDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new ChatMessageDto(Rows.text(r.get("id")), Rows.text(r.get("author")),
                    Rows.number(r.get("staff")) != 0, Rows.text(r.get("body")),
                    Rows.number(r.get("created_at"))));
        }
        return out;
    }

    /// The select-list column counting a conversation's unread shopper
    /// messages: those written after the staff's read mark.
    private static final String UNREAD = "(SELECT COUNT(*) FROM chat_message m WHERE "
            + "m.conversation_id = c.id AND m.staff = 0 AND m.created_at > c.staff_read) AS u";

    @Transactional(readOnly = true)
    public List<ChatConversationDto> list(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.CUSTOMERS, false, StoreAccess.ANYONE);
        List rows = db.query("SELECT c.id, c.email, c.name, c.subject, c.status, c.assignee, c.updated_at, "
                + "(SELECT COUNT(*) FROM chat_message m WHERE m.conversation_id = c.id) AS n, " + UNREAD + " FROM "
                + "chat_conversation c WHERE c.store_id = ? ORDER BY c.updated_at DESC", new Object[] {storeId});
        List<ChatConversationDto> out = new ArrayList<ChatConversationDto>();
        for (int iter = 0; iter < rows.size() && iter < 500; iter++) {
            out.add(dto((Map) rows.get(iter)));
        }
        return out;
    }

    private static ChatConversationDto dto(Map r) {
        return new ChatConversationDto(Rows.text(r.get("id")), Rows.text(r.get("email")),
                Rows.text(r.get("name")), Rows.text(r.get("subject")), Rows.text(r.get("status")),
                Rows.text(r.get("assignee")), (int) Rows.number(r.get("n")),
                Rows.number(r.get("updated_at")), (int) Rows.number(r.get("u")));
    }

    private Map conversation(String username, String storeId, String chatId, boolean write)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Map c = chatId == null || chatId.length() > 32 ? null : db.queryOne(
                "SELECT c.id, c.store_id, c.email, c.name, c.subject, c.status, c.assignee, c.updated_at, "
                + "(SELECT COUNT(*) FROM chat_message m WHERE m.conversation_id = c.id) AS n, " + UNREAD + " "
                + "FROM chat_conversation c WHERE c.id = ?", new Object[] {chatId});
        StoreAccess.requireIn(c == null ? null : Rows.text(c.get("store_id")), storeId, "conversation");
        access.requireArea(username, storeId, Perms.CUSTOMERS, write, StoreAccess.ANYONE);
        return c;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> messages(String username, String storeId, String chatId)
            throws IOException {
        return messages(Rows.text(conversation(username, storeId, chatId, false).get("id")));
    }

    /// A staff reply from the inbox page or the API ([#answer]).
    @Transactional(rollbackFor = IOException.class)
    public List<ChatMessageDto> reply(String username, String storeId, String chatId, String text)
            throws IOException {
        answer(username, storeId, chatId, text);
        return messages(chatId);
    }

    /// [#reply], answering the one message written. The shopper sees it live
    /// when the chat is open somewhere; otherwise it is mailed, when the
    /// conversation has an address.
    @Transactional(rollbackFor = IOException.class)
    public ChatMessageDto answer(String username, String storeId, String chatId, String text)
            throws IOException {
        Map c = conversation(username, storeId, chatId, true);
        String b = body(text);
        long now = System.currentTimeMillis();
        String id = message(storeId, chatId, username, true, b, now);
        db.execute("UPDATE chat_conversation SET status = ?, updated_at = ?, staff_read = ?, "
                + "assignee = CASE WHEN assignee = '' THEN ? ELSE assignee END WHERE id = ?",
                new Object[] {OPEN, Long.valueOf(now), Long.valueOf(now), Codes.cut(username, 200),
                    chatId});
        String to = Rows.text(c.get("email"));
        if (to.length() > 0 && !live.visitorOnline(chatId)) {
            emails.custom(storeId, to, "Re: " + Rows.text(c.get("subject")), b);
        }
        ChatMessageDto m = new ChatMessageDto(id, username, true, b, now);
        push(storeId, chatId, Rows.text(c.get("name")), m);
        return m;
    }

    @Transactional(rollbackFor = IOException.class)
    public ChatConversationDto assign(String username, String storeId, String chatId, String to)
            throws IOException {
        conversation(username, storeId, chatId, true);
        String who = to == null ? "" : to.trim();
        // "me" is the caller, whoever they are -- the owner is not a staff row.
        if ("me".equals(who)) {
            who = username;
        } else if (who.length() > 0) {
            Map s = db.queryOne("SELECT id FROM store_staff WHERE store_id = ? AND username = ?",
                    new Object[] {storeId, who});
            if (s == null) {
                throw new ResponseStatusException(400, "No such staff member");
            }
        }
        db.execute("UPDATE chat_conversation SET assignee = ? WHERE id = ?", new Object[] {who, chatId});
        return dto(conversation(username, storeId, chatId, false));
    }

    @Transactional(rollbackFor = IOException.class)
    public ChatConversationDto close(String username, String storeId, String chatId) throws IOException {
        conversation(username, storeId, chatId, true);
        db.execute("UPDATE chat_conversation SET status = ?, updated_at = ? WHERE id = ?",
                new Object[] {CLOSED, Long.valueOf(System.currentTimeMillis()), chatId});
        return dto(conversation(username, storeId, chatId, false));
    }

    @Transactional(readOnly = true)
    public List<CannedReplyDto> canned(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.CUSTOMERS, false, StoreAccess.ANYONE);
        List rows = db.query("SELECT id, title, body FROM canned_reply WHERE store_id = ? ORDER BY title",
                new Object[] {storeId});
        List<CannedReplyDto> out = new ArrayList<CannedReplyDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new CannedReplyDto(Rows.text(r.get("id")), Rows.text(r.get("title")),
                    Rows.text(r.get("body"))));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<CannedReplyDto> saveCanned(String username, String storeId, CannedReplyDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, true, StoreAccess.MANAGERS);
        String title = wanted == null || wanted.title() == null ? "" : wanted.title().trim();
        if (title.length() == 0 || title.length() > 100) {
            throw new ResponseStatusException(400, "A canned reply has a title");
        }
        db.execute("INSERT INTO canned_reply (id, store_id, title, body, created_at) VALUES (?, ?, ?, ?, ?)",
                new Object[] {Ids.next(), storeId, title, body(wanted.body()),
                    Long.valueOf(System.currentTimeMillis())});
        return canned(username, storeId);
    }
}
