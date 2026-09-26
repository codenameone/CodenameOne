/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.sql.Dialect;

/**
 * How a server keeps {@link HttpSession}s: the cookie, the lifetime and the
 * store, read from configuration when the server starts.
 *
 * <pre>
 *   cn1.session.cookie=CN1SESSION      # the cookie's name
 *   cn1.session.timeout=1800           # seconds of inactivity, 0 for never
 *   cn1.session.store=memory           # or jdbc
 *   cn1.session.same-site=Lax          # Lax, Strict or None
 *   cn1.session.secure=auto            # true, false, or auto (on under TLS)
 * </pre>
 *
 * <p>Nothing here runs for a request that does not ask for its session: the
 * cookie is parsed on the first {@code getSession}, and a request that never
 * calls it costs one field check when it ends.
 */
public final class Sessions {
    private static String cookieName = "CN1SESSION";
    private static int timeoutSeconds = 1800;
    private static String sameSite = "Lax";
    private static boolean secure;
    private static SessionStore store = new Memory();
    private static long lastPurge;
    private static final long PURGE_INTERVAL = 60000L;

    private Sessions() {
    }

    /**
     * Reads {@code cn1.session.*}. Called by the server when it starts.
     *
     * @param tls whether the server terminates TLS, for {@code secure=auto}
     * @param pool the database, for {@code store=jdbc}
     */
    public static synchronized void configure(Config config, boolean tls, DataSource pool)
            throws IOException {
        cookieName = config.get("cn1.session.cookie", "CN1SESSION");
        timeoutSeconds = config.getInt("cn1.session.timeout", 1800);
        String site = config.get("cn1.session.same-site", "Lax");
        if(!"Lax".equalsIgnoreCase(site) && !"Strict".equalsIgnoreCase(site)
                && !"None".equalsIgnoreCase(site)) {
            throw new IOException("cn1.session.same-site is \"" + site
                    + "\"; it must be Lax, Strict or None");
        }
        sameSite = site;
        String secureSetting = config.get("cn1.session.secure", "auto");
        secure = "auto".equalsIgnoreCase(secureSetting) ? tls
                : "true".equalsIgnoreCase(secureSetting);
        if("None".equalsIgnoreCase(sameSite) && !secure) {
            // Browsers drop a SameSite=None cookie that is not Secure, so the
            // session would silently never come back.
            throw new IOException("cn1.session.same-site=None needs a Secure cookie; "
                    + "set cn1.session.secure=true or serve TLS");
        }
        String kind = config.get("cn1.session.store", "memory");
        if("jdbc".equalsIgnoreCase(kind)) {
            if(pool == null) {
                throw new IOException("cn1.session.store=jdbc needs a database, and this "
                        + "server has none");
            }
            store = new Jdbc(pool);
        } else if("memory".equalsIgnoreCase(kind)) {
            if(!(store instanceof Memory)) {
                store = new Memory();
            }
        } else {
            throw new IOException("cn1.session.store is \"" + kind
                    + "\"; it must be memory or jdbc");
        }
    }

    /** Replaces the store, for one of the application's own. */
    public static synchronized void setStore(SessionStore replacement) {
        if(replacement == null) {
            throw new IllegalArgumentException("No store");
        }
        store = replacement;
    }

    /** The store sessions are kept in. */
    public static synchronized SessionStore getStore() {
        return store;
    }

    /** The name of the session cookie. */
    public static synchronized String getCookieName() {
        return cookieName;
    }

    /** A new, unguessable session id: 192 random bits. */
    static String newId() {
        try {
            return Base64Url.encode(Crypto.randomBytes(24));
        } catch (IOException err) {
            // A session id from anything weaker is a session anyone can guess,
            // so there is no fallback to fall back to.
            throw new IllegalStateException("No secure random source for a session id: "
                    + err.getMessage());
        }
    }

    /**
     * The request's session, creating one when {@code create} is set. What
     * {@code Request.getSession} calls.
     */
    static HttpSession find(String cookieValue, boolean create) throws IOException {
        SessionStore s;
        int timeout;
        synchronized(Sessions.class) {
            s = store;
            timeout = timeoutSeconds;
        }
        long now = System.currentTimeMillis();
        purgeIfDue(s, now);
        if(cookieValue != null && cookieValue.length() > 0) {
            HttpSession found = s.load(cookieValue);
            if(found != null && found.isValid() && !found.isExpired(now)) {
                found.touch(now);
                return found;
            }
            if(found != null) {
                s.delete(cookieValue);
            }
        }
        if(!create) {
            return null;
        }
        HttpSession created = new HttpSession(newId(), now, now, timeout);
        created.markNew();
        return created;
    }

    private static void purgeIfDue(SessionStore s, long now) {
        synchronized(Sessions.class) {
            if(now - lastPurge < PURGE_INTERVAL) {
                return;
            }
            lastPurge = now;
        }
        try {
            s.purgeExpired(now);
        } catch (IOException err) {
            System.err.println("Could not purge expired sessions: " + err.getMessage());
        }
    }

    /**
     * Stores what the request did to its session and adds the cookie the client
     * needs. Called by the server after the handler returns.
     */
    static HttpServer.Response finish(HttpSession session, HttpServer.Response response)
            throws IOException {
        if(session == null) {
            return response;
        }
        SessionStore s = getStore();
        String cookie = null;
        String previous = session.previousId();
        if(!session.isValid()) {
            s.delete(session.getId());
            if(previous != null) {
                s.delete(previous);
            }
            cookie = cookie("", 0);
        } else if(session.isDirty()) {
            boolean announce = session.isNew() || previous != null;
            s.save(session, previous);
            if(announce) {
                cookie = cookie(session.getId(), -1);
            }
            session.clean();
        } else if(s instanceof Jdbc) {
            ((Jdbc)s).touchIfStale(session);
        }
        if(cookie == null || response == null) {
            return response;
        }
        return withHeader(response, "Set-Cookie", cookie);
    }

    private static synchronized String cookie(String value, int maxAge) {
        StringBuilder sb = new StringBuilder(cookieName).append('=').append(value)
                .append("; Path=/; HttpOnly; SameSite=").append(sameSite);
        if(secure) {
            sb.append("; Secure");
        }
        if(maxAge >= 0) {
            sb.append("; Max-Age=").append(maxAge);
        }
        return sb.toString();
    }

    /**
     * {@code response} with one more header. The handler's own header map is
     * never modified -- it may be a constant shared by every response -- and a
     * header it already sets under the same name is kept beside this one.
     */
    static HttpServer.Response withHeader(HttpServer.Response response, String name,
                                          String value) {
        Map copy = response.extraHeaders == null ? new LinkedHashMap()
                : new LinkedHashMap(response.extraHeaders);
        Iterator keys = copy.keySet().iterator();
        while(keys.hasNext()) {
            Object key = keys.next();
            if(key != null && name.equalsIgnoreCase(String.valueOf(key))) {
                Object existing = copy.get(key);
                List both = new ArrayList();
                if(existing instanceof List) {
                    both.addAll((List)existing);
                } else if(existing != null) {
                    both.add(existing);
                }
                both.add(value);
                copy.put(key, both);
                response.extraHeaders = copy;
                return response;
            }
        }
        copy.put(name, value);
        response.extraHeaders = copy;
        return response;
    }

    /** The value of the cookie called {@code name} in a Cookie header, or null. */
    static String cookieValue(String header, String name) {
        if(header == null) {
            return null;
        }
        int at = 0;
        int n = header.length();
        while(at < n) {
            while(at < n && (header.charAt(at) == ' ' || header.charAt(at) == ';')) {
                at++;
            }
            int end = header.indexOf(';', at);
            if(end < 0) {
                end = n;
            }
            int eq = header.indexOf('=', at);
            if(eq > at && eq < end) {
                String key = header.substring(at, eq).trim();
                if(key.equals(name)) {
                    String value = header.substring(eq + 1, end).trim();
                    if(value.length() >= 2 && value.charAt(0) == '"'
                            && value.charAt(value.length() - 1) == '"') {
                        value = value.substring(1, value.length() - 1);
                    }
                    return value;
                }
            }
            at = end + 1;
        }
        return null;
    }

    /** Sessions in this process's memory. The default. */
    public static final class Memory implements SessionStore {
        private final Map sessions = new LinkedHashMap();

        public synchronized HttpSession load(String id) {
            return (HttpSession)sessions.get(id);
        }

        public synchronized void save(HttpSession session, String previousId) {
            if(previousId != null) {
                sessions.remove(previousId);
            }
            sessions.put(session.getId(), session);
        }

        public synchronized void delete(String id) {
            sessions.remove(id);
        }

        public synchronized int purgeExpired(long now) {
            int purged = 0;
            Iterator it = sessions.values().iterator();
            while(it.hasNext()) {
                HttpSession s = (HttpSession)it.next();
                if(s.isExpired(now) || !s.isValid()) {
                    it.remove();
                    purged++;
                }
            }
            return purged;
        }

        public synchronized int size() {
            return sessions.size();
        }
    }

    /**
     * Sessions in the server's database, in {@code cn1_http_session}, so every
     * instance of a server sees every session. Attributes are stored as JSON.
     */
    public static final class Jdbc implements SessionStore {
        private static final String TABLE = "cn1_http_session";
        /** How stale the stored last-access time may get before a read refreshes it. */
        private static final long TOUCH_INTERVAL = 60000L;
        private final DataSource pool;
        private boolean ready;

        public Jdbc(DataSource pool) {
            this.pool = pool;
        }

        private synchronized void prepare() throws IOException {
            if(ready) {
                return;
            }
            Dialect d = pool.dialect();
            pool.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " (id "
                    + d.assignedKeyColumn(Dialect.TEXT) + ", created "
                    + d.columnType(Dialect.BIGINT) + " NOT NULL, last_accessed "
                    + d.columnType(Dialect.BIGINT) + " NOT NULL, max_inactive "
                    + d.columnType(Dialect.INTEGER) + " NOT NULL, attributes "
                    + d.columnType(Dialect.TEXT) + ")", null);
            ready = true;
        }

        public HttpSession load(String id) throws IOException {
            prepare();
            Map row = pool.queryOne("SELECT created, last_accessed, max_inactive, attributes "
                    + "FROM " + TABLE + " WHERE id = ?", new Object[] {id});
            if(row == null) {
                return null;
            }
            HttpSession s = new HttpSession(id, number(row.get("created")),
                    number(row.get("last_accessed")), (int)number(row.get("max_inactive")));
            Object text = row.get("attributes");
            if(text instanceof String && ((String)text).length() > 0) {
                s.loadAttributes(Json.parseObject((String)text));
            }
            return s;
        }

        private static long number(Object value) {
            return value instanceof Number ? ((Number)value).longValue() : 0L;
        }

        public void save(HttpSession session, String previousId) throws IOException {
            prepare();
            String json = Json.write(session.attributesCopy());
            Object[] values = new Object[] {new Long(session.getLastAccessedTime()),
                    new Integer(session.getMaxInactiveInterval()), json, session.getId()};
            int updated = pool.execute("UPDATE " + TABLE + " SET last_accessed = ?, "
                    + "max_inactive = ?, attributes = ? WHERE id = ?", values);
            if(updated == 0) {
                pool.execute("INSERT INTO " + TABLE + " (id, created, last_accessed, "
                        + "max_inactive, attributes) VALUES (?, ?, ?, ?, ?)",
                        new Object[] {session.getId(), new Long(session.getCreationTime()),
                        new Long(session.getLastAccessedTime()),
                        new Integer(session.getMaxInactiveInterval()), json});
            }
            session.storedAccessed = session.getLastAccessedTime();
            if(previousId != null) {
                delete(previousId);
            }
        }

        void touchIfStale(HttpSession session) throws IOException {
            // Reads keep a session alive too, but writing the time on every one
            // would turn every request into a database write. Within a minute is
            // close enough for a timeout measured in tens of minutes.
            long now = System.currentTimeMillis();
            if(now - session.storedAccessed < TOUCH_INTERVAL) {
                return;
            }
            pool.execute("UPDATE " + TABLE + " SET last_accessed = ? WHERE id = ?",
                    new Object[] {new Long(now), session.getId()});
            session.storedAccessed = now;
        }

        public void delete(String id) throws IOException {
            prepare();
            pool.execute("DELETE FROM " + TABLE + " WHERE id = ?", new Object[] {id});
        }

        public int purgeExpired(long now) throws IOException {
            prepare();
            return pool.execute("DELETE FROM " + TABLE + " WHERE max_inactive > 0 AND "
                    + "last_accessed + max_inactive * 1000 < ?", new Object[] {new Long(now)});
        }

        public int size() {
            return -1;
        }
    }
}
