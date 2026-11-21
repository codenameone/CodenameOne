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

/// Customer chat: a shopper writes from the storefront widget, the store
/// answers from its inbox, and the shopper reads the thread by its token
/// (and gets an email for each reply).
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

    public ChatInbox(DataSource db, StoreAccess access, Emails emails, Throttle throttle) {
        this.db = db;
        this.access = access;
        this.emails = emails;
        this.throttle = throttle;
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

    /// A shopper's message: a new conversation, or another message on the
    /// conversation `token` names. Answers the conversation's token.
    @Transactional(rollbackFor = IOException.class)
    public String ask(String storeId, String client, String token, String name, String emailAddress,
            String text) throws IOException {
        String b = body(text);
        throttle.check("chat", "c:" + client);
        long now = System.currentTimeMillis();
        if (token != null && token.length() > 0) {
            Map c = token.length() > 64 ? null : db.queryOne(
                    "SELECT id, store_id FROM chat_conversation WHERE token = ?", new Object[] {token});
            if (c == null || !storeId.equals(Rows.text(c.get("store_id")))) {
                throw new ResponseStatusException(404, "No such conversation");
            }
            message(storeId, Rows.text(c.get("id")), "customer", false, b, now);
            db.execute("UPDATE chat_conversation SET status = ?, updated_at = ? WHERE id = ?",
                    new Object[] {OPEN, Long.valueOf(now), Rows.text(c.get("id"))});
            return token;
        }
        String e = emailAddress == null ? "" : emailAddress.trim();
        if (!email(e)) {
            throw new ResponseStatusException(400, "Give an email address so the shop can answer");
        }
        throttle.check("chat", "e:" + e);
        String n = name == null || name.trim().length() == 0 ? e.substring(0, e.indexOf('@'))
                : Codes.cut(name.trim(), 100);
        String id = Ids.next();
        String t = Ids.next() + Ids.next();
        db.execute("INSERT INTO chat_conversation (id, store_id, token, email, name, subject, status, "
                + "assignee, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {
                    id, storeId, t, e, n, Codes.cut(b, 80), OPEN, "", Long.valueOf(now),
                    Long.valueOf(now)});
        message(storeId, id, n, false, b, now);
        return t;
    }

    private void message(String storeId, String conversationId, String author, boolean staff,
            String body, long now) throws IOException {
        db.execute("INSERT INTO chat_message (id, store_id, conversation_id, author, staff, body, "
                + "created_at) VALUES (?, ?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(), storeId,
                    conversationId, Codes.cut(author, 200), Integer.valueOf(staff ? 1 : 0), body,
                    Long.valueOf(now)});
    }

    /// The thread a shopper's token opens, oldest first; 404 for any other store.
    @Transactional(readOnly = true)
    public List<ChatMessageDto> thread(String storeId, String token) throws IOException {
        Map c = token == null || token.length() > 64 ? null : db.queryOne(
                "SELECT id, store_id FROM chat_conversation WHERE token = ?", new Object[] {token});
        if (c == null || !storeId.equals(Rows.text(c.get("store_id")))) {
            throw new ResponseStatusException(404, "No such conversation");
        }
        return messages(Rows.text(c.get("id")));
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

    @Transactional(readOnly = true)
    public List<ChatConversationDto> list(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.CUSTOMERS, false, StoreAccess.ANYONE);
        List rows = db.query("SELECT c.id, c.email, c.name, c.subject, c.status, c.assignee, c.updated_at, "
                + "(SELECT COUNT(*) FROM chat_message m WHERE m.conversation_id = c.id) AS n FROM "
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
                Rows.number(r.get("updated_at")));
    }

    private Map conversation(String username, String storeId, String chatId, boolean write)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Map c = chatId == null || chatId.length() > 32 ? null : db.queryOne(
                "SELECT c.id, c.store_id, c.email, c.name, c.subject, c.status, c.assignee, c.updated_at, "
                + "(SELECT COUNT(*) FROM chat_message m WHERE m.conversation_id = c.id) AS n "
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

    /// A staff reply, emailed to the shopper.
    @Transactional(rollbackFor = IOException.class)
    public List<ChatMessageDto> reply(String username, String storeId, String chatId, String text)
            throws IOException {
        Map c = conversation(username, storeId, chatId, true);
        String b = body(text);
        long now = System.currentTimeMillis();
        message(storeId, chatId, username, true, b, now);
        db.execute("UPDATE chat_conversation SET status = ?, updated_at = ?, assignee = CASE WHEN "
                + "assignee = '' THEN ? ELSE assignee END WHERE id = ?", new Object[] {OPEN,
                    Long.valueOf(now), Codes.cut(username, 200), chatId});
        emails.custom(storeId, Rows.text(c.get("email")), "Re: " + Rows.text(c.get("subject")), b);
        return messages(chatId);
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
