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
package com.codenameone.examples.bazaar.web.account;

import com.codename1.backend.DataSource;

import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Rows;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The browser sessions a person is signed in with (table `web_session`), so the
/// security page can list them and end every one but the current.
///
/// The session store keeps no index by user, so each signed-in page records its
/// session here ([WebPages] calls [#touch]). A row is keyed by a SHA-256 of the
/// session id: the table is never a list of cookies someone could replay. Ending
/// a session marks its row; the next page that session asks for finds the mark,
/// invalidates the session and answers 401, so a stolen cookie stops working on
/// its next request wherever it is.
@Component
public class WebSessions {
    /// How seldom the last-seen time is written for a session already recorded.
    static final long SEEN_EVERY = 60000L;

    private final DataSource db;

    public WebSessions(DataSource db) {
        this.db = db;
    }

    /// Records the request's session for `username`, or refuses it when it was
    /// signed out from elsewhere.
    public void touch(HttpServer.Request request, String username) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || username == null) {
            return;
        }
        String id = key(session.getId());
        long now = System.currentTimeMillis();
        Map row = db.queryOne("SELECT username, seen_at, revoked FROM web_session WHERE id = ?",
                new Object[] {id});
        if (row == null) {
            db.execute("INSERT INTO web_session (id, username, agent, address, created_at, "
                    + "seen_at, revoked) VALUES (?, ?, ?, ?, ?, ?, 0)",
                    new Object[] {id, username, cut(request.getHeader("User-Agent"), 200),
                        cut(request.getRemoteAddress(), 64), now, now});
            return;
        }
        if (Rows.number(row.get("revoked")) != 0 || !username.equals(row.get("username"))) {
            session.invalidate();
            throw new ResponseStatusException(401, "This session was signed out");
        }
        if (now - Rows.number(row.get("seen_at")) > SEEN_EVERY) {
            db.execute("UPDATE web_session SET seen_at = ? WHERE id = ?", new Object[] {now, id});
        }
    }

    /// One recorded session.
    public static final class Seen {
        public final String agent;
        public final String address;
        public final long createdAt;
        public final long seenAt;
        public final boolean current;

        Seen(String agent, String address, long createdAt, long seenAt, boolean current) {
            this.agent = agent;
            this.address = address;
            this.createdAt = createdAt;
            this.seenAt = seenAt;
            this.current = current;
        }
    }

    /// `username`'s live sessions, most recently used first: not ended, and seen
    /// within the current session's idle limit (an older one has expired).
    public List<Seen> list(HttpServer.Request request, String username) throws IOException {
        HttpSession session = request.getSession(false);
        String mine = session == null ? "" : key(session.getId());
        long idle = session == null ? 1800000L : session.getMaxInactiveInterval() * 1000L;
        if (idle <= 0) {
            idle = 30L * 86400000L;
        }
        List rows = db.query("SELECT id, agent, address, created_at, seen_at FROM web_session "
                + "WHERE username = ? AND revoked = 0 AND seen_at >= ? ORDER BY seen_at DESC",
                new Object[] {username, System.currentTimeMillis() - idle});
        List<Seen> out = new ArrayList<Seen>();
        for (int iter = 0; iter < rows.size() && iter < 50; iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new Seen(String.valueOf(r.get("agent")), String.valueOf(r.get("address")),
                    Rows.number(r.get("created_at")), Rows.number(r.get("seen_at")),
                    mine.equals(r.get("id"))));
        }
        return out;
    }

    /// Ends every session of `username` but the request's own; answers how many.
    public int endOthers(HttpServer.Request request, String username) throws IOException {
        HttpSession session = request.getSession(false);
        String mine = session == null ? "" : key(session.getId());
        return db.execute("UPDATE web_session SET revoked = 1 WHERE username = ? AND id <> ? "
                + "AND revoked = 0", new Object[] {username, mine});
    }

    static String key(String sessionId) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            return com.codenameone.examples.bazaar.Ids.hex(d.digest(sessionId.getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String cut(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
