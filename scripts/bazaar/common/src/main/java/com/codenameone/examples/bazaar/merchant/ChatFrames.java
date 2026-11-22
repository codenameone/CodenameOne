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
package com.codenameone.examples.bazaar.merchant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The frames of the store chat socket (`/ws/chat`), as maps: what the inbox
/// sends, and typed reads of what the server sends back. The wire format is
/// the one the web inbox and the shopper's widget use, so the app is one more
/// member of staff on the same conversations.
///
/// A parsed frame can carry numbers as `Double` or `Long` and booleans as
/// `Boolean` or the strings `"true"`/`"false"`, depending on how the parser
/// was set up; the readers here accept every form.
final class ChatFrames {
    static final String RING = "RING";
    static final String OFFER = "OFFER";
    static final String ANSWER = "ANSWER";
    static final String ICE = "ICE";
    static final String DECLINE = "DECLINE";
    static final String TAKEN = "TAKEN";
    static final String END = "END";

    private ChatFrames() {
    }

    private static Map<String, Object> op(String op, String chat) {
        Map<String, Object> f = new LinkedHashMap<String, Object>();
        f.put("op", op);
        if (chat != null) {
            f.put("chat", chat);
        }
        return f;
    }

    /// Keeps a connection alive. The chat socket answers `{"op":"ping"}`; a
    /// frame without `op` would be read as a conversation frame and refused.
    static Map<String, Object> ping() {
        return op("ping", null);
    }

    /// Asks for a conversation's thread, and for its call state when one rings.
    static Map<String, Object> watch(String chat) {
        return op("watch", chat);
    }

    static Map<String, Object> send(String chat, String body) {
        Map<String, Object> f = op("send", chat);
        f.put("body", body);
        return f;
    }

    static Map<String, Object> typing(String chat) {
        return op("typing", chat);
    }

    /// The staff read the thread up to `at`, a server time from a message.
    static Map<String, Object> seen(String chat, long at) {
        Map<String, Object> f = op("seen", chat);
        f.put("at", Long.valueOf(at));
        return f;
    }

    /// A call signal with no payload: DECLINE or END.
    static Map<String, Object> call(String chat, String kind) {
        Map<String, Object> f = op("call", chat);
        f.put("kind", kind);
        return f;
    }

    static Map<String, Object> answer(String chat, String sdp) {
        Map<String, Object> f = call(chat, ANSWER);
        f.put("sdp", sdp);
        return f;
    }

    /// A local ICE candidate, as the object a browser's `toJSON()` makes: the
    /// server relays a candidate only in that shape.
    static Map<String, Object> ice(String chat, String sdpMid, int sdpMLineIndex,
            String candidate) {
        Map<String, Object> c = new LinkedHashMap<String, Object>();
        c.put("candidate", candidate);
        c.put("sdpMid", sdpMid == null ? "" : sdpMid);
        c.put("sdpMLineIndex", Integer.valueOf(sdpMLineIndex));
        Map<String, Object> f = call(chat, ICE);
        f.put("candidate", c);
        return f;
    }

    static String text(Map f, String key) {
        Object v = f == null ? null : f.get(key);
        return v instanceof String ? (String) v : "";
    }

    static long number(Map f, String key) {
        Object v = f == null ? null : f.get(key);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        if (!(v instanceof String)) {
            return 0;
        }
        String s = (String) v;
        boolean negative = s.startsWith("-");
        long out = 0;
        for (int iter = negative ? 1 : 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c == '.') {
                break;
            }
            if (c < '0' || c > '9') {
                return 0;
            }
            out = out * 10 + (c - '0');
        }
        return negative ? -out : out;
    }

    static boolean truth(Map f, String key) {
        Object v = f == null ? null : f.get(key);
        return Boolean.TRUE.equals(v) || "true".equals(v);
    }

    static Map map(Map f, String key) {
        Object v = f == null ? null : f.get(key);
        return v instanceof Map ? (Map) v : null;
    }

    static List list(Map f, String key) {
        Object v = f == null ? null : f.get(key);
        return v instanceof List ? (List) v : new ArrayList();
    }

    /// The URLs of one ICE server from a `hello` frame: a list, or one string.
    static String[] urls(Map server) {
        Object v = server == null ? null : server.get("urls");
        List<String> out = new ArrayList<String>();
        if (v instanceof String) {
            out.add((String) v);
        } else if (v instanceof List) {
            List all = (List) v;
            for (int iter = 0; iter < all.size(); iter++) {
                if (all.get(iter) instanceof String) {
                    out.add((String) all.get(iter));
                }
            }
        }
        return out.toArray(new String[out.size()]);
    }
}
