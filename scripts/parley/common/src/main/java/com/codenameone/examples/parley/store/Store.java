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
package com.codenameone.examples.parley.store;

import com.codenameone.examples.parley.api.ConversationDto;
import com.codenameone.examples.parley.api.MemberDto;
import com.codenameone.examples.parley.chat.Messenger;
import com.codenameone.examples.parley.crypto.Codec;
import com.codenameone.examples.parley.crypto.Vault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The conversations and their history as this device knows them.
///
/// The server keeps a message only until each device has fetched it, so this
/// is the only copy of the history there is. It lives in the [Vault]: an index
/// with a line per conversation, and one document per conversation holding
/// its most recent [#KEPT] messages. Everything is read on the EDT and kept in
/// memory once read.
public final class Store {
    /// What screens hear: something in this conversation changed, or "" for
    /// the list as a whole.
    public interface Listener {
        void changed(String conversationId);
    }

    /// Messages kept per conversation; older ones are let go.
    public static final int KEPT = 500;

    private static final String INDEX = "index";

    private static final Map<String, ConversationDto> conversations = new HashMap<String, ConversationDto>();
    private static final Map<String, Map<String, Object>> summaries = new HashMap<String, Map<String, Object>>();
    private static final Map<String, List<Message>> history = new HashMap<String, List<Message>>();
    private static final List<Listener> listeners = new ArrayList<Listener>();
    /// Whether the server's list has arrived since the vault opened. Before
    /// it does, the cached summaries are all there is to show; after it, the
    /// server's list is the truth even when it is empty.
    private static boolean loaded;

    private Store() {
    }

    /// Reads the index of the open vault.
    @SuppressWarnings("unchecked")
    public static void load() {
        clear();
        Map<String, Object> index = Vault.get(INDEX);
        if (index == null) {
            return;
        }
        for (Map.Entry<String, Object> each : index.entrySet()) {
            if (each.getValue() instanceof Map) {
                summaries.put(each.getKey(), (Map<String, Object>) each.getValue());
            }
        }
    }

    public static void clear() {
        loaded = false;
        conversations.clear();
        summaries.clear();
        history.clear();
    }

    public static void listen(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void unlisten(Listener listener) {
        listeners.remove(listener);
    }

    public static void changed(String conversationId) {
        Listener[] now = listeners.toArray(new Listener[listeners.size()]);
        for (Listener each : now) {
            each.changed(conversationId);
        }
    }

    /// Replaces what the server says about the conversations: the list after
    /// a fetch. Conversations no longer in it are ones this account left or
    /// was removed from; their history stays until the user deletes it.
    public static void conversations(List<ConversationDto> fresh) {
        loaded = true;
        conversations.clear();
        for (ConversationDto each : fresh) {
            conversation(each, false);
        }
        saveIndex();
        changed("");
    }

    public static void conversation(ConversationDto fresh, boolean announce) {
        conversations.put(fresh.id(), fresh);
        Map<String, Object> summary = summary(fresh.id());
        summary.put("title", fresh.title());
        summary.put("kind", fresh.kind());
        summary.put("bridge", fresh.bridge() == null ? "" : fresh.bridge());
        summary.put("pinned", fresh.pinned() ? "1" : "");
        summary.put("archived", fresh.archived() ? "1" : "");
        summary.put("muted", fresh.muted() ? "1" : "");
        if (Codec.num(summary.get("lastAt")) < fresh.lastActivityAt()
                && Codec.str(summary.get("last")).length() == 0) {
            summary.put("lastAt", Long.valueOf(fresh.lastActivityAt()));
        }
        if (announce) {
            saveIndex();
            changed(fresh.id());
        }
    }

    public static ConversationDto conversation(String id) {
        return conversations.get(id);
    }

    /// Conversation ids, pinned first, then most recent first.
    public static List<String> ids(final boolean archived) {
        List<String> out = new ArrayList<String>();
        for (String id : summaries.keySet()) {
            if (flag(id, "archived") == archived && (!loaded || conversations.containsKey(id))) {
                out.add(id);
            }
        }
        Collections.sort(out, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                boolean pa = flag(a, "pinned");
                boolean pb = flag(b, "pinned");
                if (pa != pb) {
                    return pa ? -1 : 1;
                }
                long la = lastAt(a);
                long lb = lastAt(b);
                return la == lb ? a.compareTo(b) : (la > lb ? -1 : 1);
            }
        });
        return out;
    }

    public static String title(String id) {
        return Codec.str(summary(id).get("title"));
    }

    public static String kind(String id) {
        return Codec.str(summary(id).get("kind"));
    }

    public static String lastText(String id) {
        return Codec.str(summary(id).get("last"));
    }

    public static long lastAt(String id) {
        return Codec.num(summary(id).get("lastAt"));
    }

    public static int unread(String id) {
        return (int) Codec.num(summary(id).get("unread"));
    }

    public static int unreadTotal() {
        int total = 0;
        for (String id : summaries.keySet()) {
            if (!flag(id, "archived")) {
                total += unread(id);
            }
        }
        return total;
    }

    public static boolean flag(String id, String name) {
        return "1".equals(summary(id).get(name));
    }

    public static boolean bridged(String id) {
        return Codec.str(summary(id).get("bridge")).length() > 0;
    }

    public static void markRead(String id) {
        if (unread(id) != 0) {
            summary(id).put("unread", Long.valueOf(0));
            saveIndex();
            changed("");
        }
    }

    /// The members other than `me`, for a direct chat's title and avatar.
    public static String peer(String id, String me) {
        ConversationDto row = conversations.get(id);
        if (row == null || row.members() == null) {
            return "";
        }
        for (MemberDto each : row.members()) {
            if (!each.username().equals(me)) {
                return each.username();
            }
        }
        return "";
    }

    /// The history of a conversation, oldest first, without the messages
    /// whose disappearing timer has run out.
    @SuppressWarnings("unchecked")
    public static List<Message> messages(String id) {
        List<Message> out = history.get(id);
        if (out == null) {
            out = new ArrayList<Message>();
            Map<String, Object> document = Vault.get(name(id));
            Object list = document == null ? null : document.get("messages");
            if (list instanceof List) {
                for (Object each : (List<Object>) list) {
                    if (each instanceof Map) {
                        out.add(Message.fromMap((Map<String, Object>) each));
                    }
                }
            }
            history.put(id, out);
        }
        expire(id, out);
        return out;
    }

    /// This account's messages still marked as sending, by conversation: after
    /// a restart nothing is sending them any more.
    public static Map<String, List<Message>> unsent(String me) {
        Map<String, List<Message>> out = new HashMap<String, List<Message>>();
        for (String id : summaries.keySet()) {
            for (Message each : messages(id)) {
                if (each.status == Message.SENDING && me.equals(each.from)) {
                    List<Message> list = out.get(id);
                    if (list == null) {
                        list = new ArrayList<Message>();
                        out.put(id, list);
                    }
                    list.add(each);
                }
            }
        }
        return out;
    }

    public static Message find(String id, String key) {
        List<Message> list = messages(id);
        for (int iter = list.size() - 1; iter >= 0; iter--) {
            if (list.get(iter).key().equals(key)) {
                return list.get(iter);
            }
        }
        return null;
    }

    /// Adds a message, or updates the one with the same key; answers false
    /// when it was already there (a copy delivered twice).
    public static boolean add(String id, Message message, String me) {
        List<Message> list = messages(id);
        if (find(id, message.key()) != null) {
            return false;
        }
        int at = list.size();
        while (at > 0 && list.get(at - 1).at > message.at) {
            at--;
        }
        list.add(at, message);
        while (list.size() > KEPT) {
            list.remove(0);
        }
        Map<String, Object> summary = summary(id);
        if (message.at >= Codec.num(summary.get("lastAt"))) {
            summary.put("last", preview(message));
            summary.put("lastAt", Long.valueOf(message.at));
        }
        if (!message.mine(me) && !Message.SYSTEM.equals(message.kind)) {
            summary.put("unread", Long.valueOf(unread(id) + 1));
        }
        save(id);
        return true;
    }

    /// Writes a conversation's history after a message in it changed.
    public static void save(String id) {
        List<Message> list = history.get(id);
        if (list == null) {
            return;
        }
        List<Object> out = new ArrayList<Object>();
        long soonest = 0;
        for (Message each : list) {
            out.add(each.toMap());
            if (each.expiresAt > 0 && (soonest == 0 || each.expiresAt < soonest)) {
                soonest = each.expiresAt;
            }
        }
        // The index says when the next message here disappears, so that it
        // goes on time in a conversation nobody has opened since the app
        // started, whose history is not in memory.
        summary(id).put("expires", Long.valueOf(soonest));
        Map<String, Object> document = new HashMap<String, Object>();
        document.put("messages", out);
        Vault.put(name(id), document);
        saveIndex();
        changed(id);
    }

    /// Deletes the history of a conversation on this device.
    public static void wipe(String id) {
        history.remove(id);
        Vault.remove(name(id));
        Map<String, Object> summary = summary(id);
        summary.put("last", "");
        summary.put("unread", Long.valueOf(0));
        saveIndex();
        changed(id);
    }

    /// Drops every message whose disappearing timer ran out, everywhere.
    public static void expireAll() {
        long now = System.currentTimeMillis();
        for (String id : new ArrayList<String>(summaries.keySet())) {
            long expires = Codec.num(summaries.get(id).get("expires"));
            if (history.containsKey(id) || (expires > 0 && expires <= now)) {
                // messages() reads the history in and expires it.
                messages(id);
            }
        }
    }

    private static void expire(String id, List<Message> list) {
        long now = System.currentTimeMillis();
        boolean gone = false;
        // The unread messages are the newest incoming ones; those among them
        // that expire unread come off the badge with them.
        int unread = unread(id);
        String me = Messenger.me();
        int seen = 0;
        int lost = 0;
        for (int iter = list.size() - 1; iter >= 0; iter--) {
            Message message = list.get(iter);
            boolean counted = seen < unread && !message.mine(me) && !Message.SYSTEM.equals(message.kind);
            if (counted) {
                seen++;
            }
            long expires = message.expiresAt;
            if (expires > 0 && expires <= now) {
                list.remove(iter);
                gone = true;
                if (counted) {
                    lost++;
                }
            }
        }
        Map<String, Object> summary = summary(id);
        if (gone) {
            summary.put("unread", Long.valueOf(unread - lost));
            summary.put("last", list.isEmpty() ? "" : preview(list.get(list.size() - 1)));
            save(id);
        } else if (Codec.num(summary.get("expires")) > 0 && Codec.num(summary.get("expires")) <= now) {
            // A stale mark, from a history written before it was kept.
            save(id);
        }
    }

    /// One line about a message for the chat list.
    public static String preview(Message message) {
        if (message.deleted) {
            return "This message was deleted";
        }
        if (Message.IMAGE.equals(message.kind)) {
            return "Photo";
        }
        if (Message.VIDEO.equals(message.kind)) {
            return "Video";
        }
        if (Message.VOICE.equals(message.kind)) {
            return "Voice message";
        }
        if (Message.FILE.equals(message.kind)) {
            return message.media == null ? "File" : Codec.str(message.media.get("name"));
        }
        return message.text;
    }

    private static Map<String, Object> summary(String id) {
        Map<String, Object> out = summaries.get(id);
        if (out == null) {
            out = new HashMap<String, Object>();
            summaries.put(id, out);
        }
        return out;
    }

    private static void saveIndex() {
        Vault.put(INDEX, new HashMap<String, Object>(summaries));
    }

    private static String name(String id) {
        return "c-" + id;
    }
}
