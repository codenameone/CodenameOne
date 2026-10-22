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

import com.codenameone.examples.parley.crypto.Codec;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/// One message as this device keeps it, after decryption.
///
/// A message is known everywhere by its author's `from` and the `clientId`
/// the author's device gave it, which travel inside the encrypted payload;
/// reactions, edits, deletions and receipts all name their target that way.
/// The server's own id is different for every send and never reaches here.
public final class Message {
    public static final String TEXT = "text";
    public static final String IMAGE = "image";
    public static final String VIDEO = "video";
    public static final String VOICE = "voice";
    public static final String FILE = "file";
    /// Said by the server: someone joined, the timer changed.
    public static final String SYSTEM = "system";

    /// Delivery state of a message this account sent, in the order it moves.
    public static final int FAILED = -1;
    public static final int SENDING = 0;
    public static final int SENT = 1;
    public static final int DELIVERED = 2;
    public static final int READ = 3;

    public String clientId = "";
    public String from = "";
    public String kind = TEXT;
    public String text = "";
    public long at;
    public long expiresAt;
    /// The attachment: id, key, digest, mime, size, name, duration, thumb.
    public Map<String, Object> media;
    /// The message this one answers: its key and a line of its text.
    public String replyTo = "";
    public String replyText = "";
    /// username -> emoji.
    public final Map<String, String> reactions = new LinkedHashMap<String, String>();
    public boolean edited;
    public boolean deleted;
    public int status = SENT;
    /// Whether this came through a bridge or the server rather than end to
    /// end encrypted.
    public boolean plain;

    /// The key that names this message in a conversation.
    public String key() {
        return from + "/" + clientId;
    }

    public boolean mine(String me) {
        return from.equals(me);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("cid", clientId);
        out.put("from", from);
        out.put("kind", kind);
        out.put("text", text);
        out.put("at", Long.valueOf(at));
        out.put("exp", Long.valueOf(expiresAt));
        if (media != null) {
            out.put("media", media);
        }
        out.put("reply", replyTo);
        out.put("replyText", replyText);
        out.put("reactions", new HashMap<String, Object>(reactions));
        out.put("edited", edited ? "1" : "");
        out.put("deleted", deleted ? "1" : "");
        out.put("status", Long.valueOf(status));
        out.put("plain", plain ? "1" : "");
        return out;
    }

    @SuppressWarnings("unchecked")
    public static Message fromMap(Map<String, Object> in) {
        Message out = new Message();
        out.clientId = Codec.str(in.get("cid"));
        out.from = Codec.str(in.get("from"));
        out.kind = Codec.str(in.get("kind"));
        out.text = Codec.str(in.get("text"));
        out.at = Codec.num(in.get("at"));
        out.expiresAt = Codec.num(in.get("exp"));
        Object media = in.get("media");
        if (media instanceof Map) {
            out.media = (Map<String, Object>) media;
        }
        out.replyTo = Codec.str(in.get("reply"));
        out.replyText = Codec.str(in.get("replyText"));
        Object reactions = in.get("reactions");
        if (reactions instanceof Map) {
            for (Map.Entry<String, Object> each : ((Map<String, Object>) reactions).entrySet()) {
                out.reactions.put(each.getKey(), Codec.str(each.getValue()));
            }
        }
        out.edited = "1".equals(in.get("edited"));
        out.deleted = "1".equals(in.get("deleted"));
        out.status = (int) Codec.num(in.get("status"));
        out.plain = "1".equals(in.get("plain"));
        return out;
    }
}
