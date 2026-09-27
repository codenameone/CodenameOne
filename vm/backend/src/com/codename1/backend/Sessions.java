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
import java.util.HashMap;
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
    private String cookieName = "CN1SESSION";
    private int timeoutSeconds = 1800;
    private String sameSite = "Lax";
    private boolean secure;
    private SessionStore store = new Memory();
    private long lastPurge;
    private static final long PURGE_INTERVAL = 60000L;
    /** Runs the destroy methods of @SessionScope beans; null without an application. */
    private final Backend.Application application;
    /**
     * The @SessionScope beans of every session that has any, by session id.
     * Kept here rather than trusted to the store: a database store hands back a
     * NEW HttpSession on every request, so beans living only on that object
     * would be built again per request and never destroyed.
     */
    private final Map beans = new HashMap();
    private boolean closed;

    /**
     * Default settings and an in-memory store, for a server with no generated
     * application.
     */
    public Sessions() {
        this(null);
    }

    Sessions(Backend.Application application) {
        this.application = application;
    }

    /**
     * Reads {@code cn1.session.*} into a server's session settings. Called by the
     * server when it starts; every server has its own, because cookies are not
     * scoped by port and a client of two servers on one host would otherwise
     * present one server's session to the other.
     *
     * @param tls whether the server terminates TLS, for {@code secure=auto}
     * @param pool the database, for {@code store=jdbc}
     * @param application destroys the session-scoped beans, or null
     */
    public static Sessions configure(Config config, boolean tls, DataSource pool,
                                     Backend.Application application) throws IOException {
        Sessions out = new Sessions(application);
        out.cookieName = config.get("cn1.session.cookie", "CN1SESSION");
        if(!isToken(out.cookieName)) {
            // Emitted verbatim in Set-Cookie: an empty name, a space, a ';' or a
            // control character makes a header the browser ignores or the writer
            // drops, and every request then starts another session nobody can
            // come back to.
            throw new IOException("cn1.session.cookie is \"" + out.cookieName + "\"; a cookie "
                    + "name is one or more letters, digits and !#$%&'*+-.^_`|~");
        }
        out.timeoutSeconds = config.getInt("cn1.session.timeout", 1800);
        if(out.timeoutSeconds < 0) {
            // Zero is the documented "never"; a negative one is a typo that
            // isExpired() would also read as never, silently making every
            // sign-in permanent.
            throw new IOException("cn1.session.timeout is " + out.timeoutSeconds
                    + "; it must be a number of seconds, or 0 for sessions that never "
                    + "expire");
        }
        String site = config.get("cn1.session.same-site", "Lax");
        if(!"Lax".equalsIgnoreCase(site) && !"Strict".equalsIgnoreCase(site)
                && !"None".equalsIgnoreCase(site)) {
            throw new IOException("cn1.session.same-site is \"" + site
                    + "\"; it must be Lax, Strict or None");
        }
        out.sameSite = site;
        String secureSetting = config.get("cn1.session.secure", "auto").trim();
        if("auto".equalsIgnoreCase(secureSetting)) {
            out.secure = tls;
        } else if("true".equalsIgnoreCase(secureSetting)) {
            out.secure = true;
        } else if("false".equalsIgnoreCase(secureSetting)) {
            out.secure = false;
        } else {
            // Refused rather than read as false: a typo such as "tru" would
            // otherwise start a TLS server whose session cookies a browser also
            // sends over plain HTTP.
            throw new IOException("cn1.session.secure is \"" + secureSetting
                    + "\"; it must be auto, true or false");
        }
        if("None".equalsIgnoreCase(out.sameSite) && !out.secure) {
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
            out.store = new Jdbc(pool);
        } else if(!"memory".equalsIgnoreCase(kind)) {
            throw new IOException("cn1.session.store is \"" + kind
                    + "\"; it must be memory or jdbc");
        }
        return out;
    }

    /** Whether {@code name} is an RFC 6265 cookie-name: an HTTP token. */
    static boolean isToken(String name) {
        if(name == null || name.length() == 0) {
            return false;
        }
        for(int iter = 0 ; iter < name.length() ; iter++) {
            char c = name.charAt(iter);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || "!#$%&'*+-.^_`|~".indexOf(c) >= 0;
            if(!ok) {
                return false;
            }
        }
        return true;
    }

    /** Replaces the store, for one of the application's own. */
    public synchronized void setStore(SessionStore replacement) {
        if(replacement == null) {
            throw new IllegalArgumentException("No store");
        }
        store = replacement;
    }

    /** The store sessions are kept in. */
    public synchronized SessionStore getStore() {
        return store;
    }

    /** The name of the session cookie. */
    public synchronized String getCookieName() {
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
    HttpSession find(String cookieValue, boolean create) throws IOException {
        SessionStore s;
        int timeout;
        synchronized(this) {
            s = store;
            timeout = timeoutSeconds;
        }
        long now = System.currentTimeMillis();
        purgeIfDue(s, now);
        if(cookieValue != null && cookieValue.length() > 0) {
            HttpSession found = s.load(cookieValue);
            if(found != null && found.isValid() && !found.isExpired(now)) {
                found.touch(now);
                found.owner = this;
                synchronized(this) {
                    // The beans are in use from now until this request ends: a
                    // purge by another request during a long one -- even one that
                    // outlasts the timeout -- must not destroy what it is using.
                    Held held = (Held)beans.get(cookieValue);
                    if(held != null) {
                        held.lastAccessed = now;
                    }
                }
                return found;
            }
            if(found != null) {
                s.delete(cookieValue);
                destroy(take(cookieValue));
            }
        }
        if(!create) {
            return null;
        }
        HttpSession created = new HttpSession(newId(), now, now, timeout);
        created.markNew();
        created.owner = this;
        return created;
    }

    /**
     * Expires what is due, at most once a minute. Package-private so a test can
     * run it at a chosen {@code now} instead of waiting for the next lookup.
     */
    void purgeIfDue(SessionStore s, long now) {
        List expired = null;
        java.util.Set busy;
        synchronized(this) {
            if(now - lastPurge < PURGE_INTERVAL) {
                return;
            }
            lastPurge = now;
            busy = idsInUse();
            Iterator it = beans.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry e = (Map.Entry)it.next();
                Held h = (Held)e.getValue();
                if(h.maxInactiveSeconds > 0
                        && now - h.lastAccessed > h.maxInactiveSeconds * 1000L
                        && !busy.contains(e.getKey())) {
                    it.remove();
                    if(expired == null) {
                        expired = new ArrayList();
                    }
                    expired.add(h.beans);
                }
            }
        }
        if(expired != null) {
            for(int iter = 0 ; iter < expired.size() ; iter++) {
                destroy((Object[])expired.get(iter));
            }
        }
        try {
            // The store's own entries too spare the sessions in use: a request
            // that outlasts the timeout would otherwise lose its session -- the
            // memory store does not save it again unless it changed, and the
            // database store's touch and save find no row. A store of the
            // application's own sees the plain call.
            if(s instanceof Memory) {
                ((Memory)s).purgeExpired(now, busy);
            } else if(s instanceof Jdbc) {
                ((Jdbc)s).purgeExpired(now, busy);
            } else {
                s.purgeExpired(now);
            }
        } catch (IOException err) {
            System.err.println("Could not purge expired sessions: " + err.getMessage());
        }
    }

    /**
     * Stores what the request did to its session and adds the cookie the client
     * needs. Called by the server after the handler returns.
     */
    HttpServer.Response finish(HttpSession session, HttpServer.Response response)
            throws IOException {
        if(session == null) {
            return response;
        }
        SessionStore s = getStore();
        String cookie = null;
        String previous = session.previousId();
        if(response == null && session.isValid() && (session.isNew() || previous != null)) {
            // No response of the handler's to carry a cookie -- it returned
            // nothing, or threw, and the server answers 404 or 500 itself. A new
            // id stored now would be one the client never learns: kept until it
            // expired, with its beans. So a new session is dropped, and a
            // rotation is not made: the client keeps the id it has.
            if(session.isNew()) {
                destroy(take(session.getId()));
            } else {
                synchronized(this) {
                    Held moved = (Held)beans.remove(session.getId());
                    if(moved != null) {
                        beans.put(previous, moved);
                    }
                }
            }
            return null;
        }
        if(!session.isValid()) {
            s.delete(session.getId());
            // The beans end with the session, not with whatever next finds it gone.
            destroy(take(session.getId()));
            if(previous != null) {
                s.delete(previous);
                destroy(take(previous));
            }
            destroy(session.takeLocalBeans());
            cookie = cookie("", 0);
        } else {
            if(session.isDirty()) {
                boolean announce = session.isNew() || previous != null;
                s.save(session, previous);
                if(announce) {
                    cookie = cookie(session.getId(), -1);
                }
                session.clean();
            } else if(s instanceof Jdbc) {
                ((Jdbc)s).touchIfStale(session);
            }
            keep(session, previous);
        }
        if(cookie == null || response == null) {
            return response;
        }
        return withHeader(response, "Set-Cookie", cookie);
    }

    /**
     * After a request used the session: moves its beans to its new id when it
     * was rotated, and notes the use, which is what keeps them from expiring.
     */
    private void keep(HttpSession session, String previousId) {
        Object[] late = null;
        synchronized(this) {
            Held held = previousId == null ? null : (Held)beans.remove(previousId);
            if(held != null) {
                beans.put(session.getId(), held);
            } else {
                held = (Held)beans.get(session.getId());
            }
            if(held == null) {
                return;
            }
            held.lastAccessed = session.getLastAccessedTime();
            held.maxInactiveSeconds = session.getMaxInactiveInterval();
            if(closed) {
                // The server stopped while this request was in flight; nothing
                // will destroy what is kept after close().
                beans.remove(session.getId());
                late = held.beans;
            }
        }
        destroy(late);
    }

    /**
     * The session-scoped beans of the session with this id, shared by every
     * copy of it a request loads -- a database store hands each request its own
     * HttpSession, so beans kept on that object would be built once per copy.
     * Created atomically under this lock, the first time any copy asks.
     */
    synchronized Object[] sharedBeans(HttpSession session, int count) {
        Held held = holderFor(session);
        if(held.beans == null || held.beans.length < count) {
            Object[] grown = new Object[count];
            if(held.beans != null) {
                System.arraycopy(held.beans, 0, grown, 0, held.beans.length);
            }
            held.beans = grown;
        }
        return held.beans;
    }

    /** The object generated code locks while it builds one session's bean. */
    synchronized Object beanLock(HttpSession session) {
        return holderFor(session);
    }

    /**
     * Sessions requests are using right now -- the HttpSession object, with how
     * many requests hold it: the in-memory store hands concurrent requests one
     * object, the database store each its own copy. Their beans are never
     * expired while any is in use: a request can outlast the inactivity
     * timeout, and destroying its session's beans under it would hand it closed
     * resources. By object, not id, so a session rotated or replaced during the
     * request is still covered: the purge reads each one's CURRENT id.
     */
    private final Map inUse = new HashMap();

    /** A request has resolved {@code session}; counted until {@link #leave}. */
    synchronized void enter(HttpServer.Request request, HttpSession session) {
        if(request.sessionsInUse == null) {
            request.sessionsInUse = new ArrayList(1);
        } else if(request.sessionsInUse.contains(session)) {
            return;
        }
        request.sessionsInUse.add(session);
        int[] count = (int[])inUse.get(session);
        if(count == null) {
            count = new int[1];
            inUse.put(session, count);
        }
        count[0]++;
    }

    /** The request is over; the sessions it used may expire again. */
    synchronized void leave(HttpServer.Request request) {
        List used = request.sessionsInUse;
        if(used == null) {
            return;
        }
        request.sessionsInUse = null;
        long now = System.currentTimeMillis();
        for(int iter = 0 ; iter < used.size() ; iter++) {
            HttpSession session = (HttpSession)used.get(iter);
            int[] count = (int[])inUse.get(session);
            if(count != null && --count[0] <= 0) {
                inUse.remove(session);
            }
            Held held = (Held)beans.get(session.getId());
            if(held != null) {
                held.lastAccessed = now;
            }
        }
    }

    /** The current ids of the sessions in use. Under this lock. */
    private java.util.Set idsInUse() {
        java.util.Set ids = new java.util.HashSet();
        Iterator it = inUse.keySet().iterator();
        while(it.hasNext()) {
            ids.add(((HttpSession)it.next()).getId());
        }
        return ids;
    }

    private Held holderFor(HttpSession session) {
        String id = session.getId();
        Held held = (Held)beans.get(id);
        String previous = held == null ? session.previousId() : null;
        if(previous != null) {
            // Rotated by changeSessionId() earlier in this request: the beans
            // built under the old id are this session's still.
            held = (Held)beans.remove(previous);
            if(held != null) {
                beans.put(id, held);
            }
        }
        if(held == null) {
            held = new Held();
            held.lastAccessed = session.getLastAccessedTime();
            held.maxInactiveSeconds = session.getMaxInactiveInterval();
            beans.put(id, held);
        }
        return held;
    }

    private synchronized Object[] take(String id) {
        Held held = (Held)beans.remove(id);
        return held == null ? null : held.beans;
    }

    /** Runs the destroy methods of one session's beans. */
    private void destroy(Object[] sessionBeans) {
        if(application != null && sessionBeans != null) {
            ended(sessionBeans);
        }
    }

    private void ended(Object[] sessionBeans) {
        try {
            application.sessionEnded(sessionBeans);
        } catch (Throwable err) {
            System.err.println("Destroying a session's beans failed: " + err);
        }
    }

    /**
     * Destroys the beans of every session still open. Called when the server
     * stops, after its requests have drained, as Spring closes its session
     * scope with the context.
     */
    void close() {
        List all;
        synchronized(this) {
            closed = true;
            all = new ArrayList(beans.values());
            beans.clear();
        }
        for(int iter = 0 ; iter < all.size() ; iter++) {
            destroy(((Held)all.get(iter)).beans);
        }
    }

    private synchronized String cookie(String value, int maxAge) {
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

    /** One session's beans and when it was last used, for expiring them. */
    private static final class Held {
        Object[] beans;
        long lastAccessed;
        int maxInactiveSeconds;
    }

    /**
     * {@code response} with one more header, as a NEW Response. Neither the
     * handler's Response nor its header map is modified: either may be a constant
     * shared by every request, and a session cookie written into one would be
     * sent to the next client that got it -- a session handed to a stranger. A
     * header the handler already sets under the same name is kept beside this one.
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
                return response.withHeaders(copy);
            }
        }
        copy.put(name, value);
        return response.withHeaders(copy);
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

        public int purgeExpired(long now) {
            return purgeExpired(now, java.util.Collections.EMPTY_SET);
        }

        /** {@link #purgeExpired(long)}, sparing the sessions whose ids are in {@code busy}. */
        synchronized int purgeExpired(long now, java.util.Set busy) {
            int purged = 0;
            Iterator it = sessions.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry e = (Map.Entry)it.next();
                HttpSession s = (HttpSession)e.getValue();
                if(busy.contains(e.getKey()) && s.isValid()) {
                    continue;
                }
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
                    + d.columnType(Dialect.TEXT) + ", version "
                    + d.columnType(Dialect.BIGINT) + " NOT NULL)", null);
            ready = true;
        }

        public HttpSession load(String id) throws IOException {
            prepare();
            Map row = pool.queryOne("SELECT created, last_accessed, max_inactive, attributes "
                    + "FROM " + TABLE + " WHERE id = ?", new Object[] {id});
            if(row == null) {
                return null;
            }
            int maxInactive = (int)number(row.get("max_inactive"));
            HttpSession s = new HttpSession(id, number(row.get("created")),
                    number(row.get("last_accessed")), maxInactive);
            // The stored time lags the real last use by up to one touch interval,
            // so that much more is allowed before calling the session expired --
            // late by at most the interval, never early.
            s.expiryGraceMillis = touchInterval(maxInactive);
            Object text = row.get("attributes");
            if(text instanceof String && ((String)text).length() > 0) {
                s.loadAttributes(Json.parseObject((String)text));
            }
            return s;
        }

        private static long number(Object value) {
            return value instanceof Number ? ((Number)value).longValue() : 0L;
        }

        /** Attempts at an optimistic save before two requests' races are reported. */
        private static final int SAVE_ATTEMPTS = 8;

        /**
         * Two requests of one client load separate copies of the session, and a
         * copy written back whole would replace whatever the other request saved
         * meanwhile -- its attributes, and a newer last use with an older one.
         * So an existing session is saved by re-reading the row, applying only
         * the attributes THIS request set or removed, and writing it back only
         * if its version has not moved; the last use only ever goes forward.
         */
        public void save(HttpSession session, String previousId) throws IOException {
            prepare();
            if(previousId != null && !session.isNew()) {
                rotate(session, previousId);
                return;
            }
            if(session.isNew()) {
                // A new id: nobody else can have written this row yet.
                pool.execute("INSERT INTO " + TABLE + " (id, created, last_accessed, "
                        + "max_inactive, attributes, version) VALUES (?, ?, ?, ?, ?, 0)",
                        new Object[] {session.getId(), new Long(session.getCreationTime()),
                        new Long(session.getLastAccessedTime()),
                        new Integer(session.getMaxInactiveInterval()),
                        Json.write(session.attributesCopy())});
                session.storedAccessed = session.getLastAccessedTime();
                if(previousId != null) {
                    delete(previousId);
                }
                return;
            }
            java.util.Set changed = session.changedNames();
            Map mine = session.attributesCopy();
            Long used = new Long(session.getLastAccessedTime());
            for(int attempt = 0 ; attempt < SAVE_ATTEMPTS ; attempt++) {
                Map row = pool.queryOne("SELECT max_inactive, attributes, version FROM "
                        + TABLE + " WHERE id = ?", new Object[] {session.getId()});
                if(row == null) {
                    // The row is gone: another request invalidated this session,
                    // or it expired, while this one held its own copy. Writing it
                    // back would undo a logout with a stale cookie.
                    return;
                }
                Map merged = new LinkedHashMap();
                Object text = row.get("attributes");
                if(text instanceof String && ((String)text).length() > 0) {
                    merged.putAll(Json.parseObject((String)text));
                }
                java.util.Iterator names = changed.iterator();
                while(names.hasNext()) {
                    Object name = names.next();
                    if(mine.containsKey(name)) {
                        merged.put(name, mine.get(name));
                    } else {
                        merged.remove(name);
                    }
                }
                int maxInactive = session.isMaxInactiveChanged()
                        ? session.getMaxInactiveInterval() : (int)number(row.get("max_inactive"));
                long version = number(row.get("version"));
                int updated = pool.execute("UPDATE " + TABLE + " SET attributes = ?, "
                        + "max_inactive = ?, last_accessed = CASE WHEN last_accessed > ? "
                        + "THEN last_accessed ELSE ? END, version = ? WHERE id = ? AND "
                        + "version = ?", new Object[] {Json.write(merged),
                        new Integer(maxInactive), used, used, new Long(version + 1),
                        session.getId(), new Long(version)});
                if(updated > 0) {
                    session.storedAccessed = session.getLastAccessedTime();
                    return;
                }
                // Another request saved between the read and the write: read its
                // result and apply this request's changes to that instead.
            }
            throw new IOException("Session changes could not be saved: other requests "
                    + "kept changing it (" + SAVE_ATTEMPTS + " attempts)");
        }

        /**
         * Moves an existing session to its new id -- changeSessionId() at sign-in
         * -- in one transaction, and only while the old row still exists. Done
         * as an unconditional insert, a request holding a stale copy would
         * recreate a session another request had just invalidated, under a fresh
         * id it then hands the client: a logout undone. The old row is locked
         * where the engine can, so an invalidation and a rotation of one session
         * happen one after the other, and this request's changes are merged onto
         * the row as it stands, as a plain save does.
         */
        private void rotate(final HttpSession session, final String previousId)
                throws IOException {
            final String lock = "sqlite".equals(pool.dialect().getName()) ? "" : " FOR UPDATE";
            final java.util.Set changed = session.changedNames();
            final Map mine = session.attributesCopy();
            try {
                pool.inTransaction(new DataSource.Work() {
                    public Object run(Database db) throws Exception {
                        Map row = db.queryOne("SELECT created, max_inactive, attributes FROM "
                                + TABLE + " WHERE id = ?" + lock, new Object[] {previousId});
                        if(row == null) {
                            return null;            // invalidated meanwhile: stays gone
                        }
                        Map merged = new LinkedHashMap();
                        Object text = row.get("attributes");
                        if(text instanceof String && ((String)text).length() > 0) {
                            merged.putAll(Json.parseObject((String)text));
                        }
                        java.util.Iterator names = changed.iterator();
                        while(names.hasNext()) {
                            Object name = names.next();
                            if(mine.containsKey(name)) {
                                merged.put(name, mine.get(name));
                            } else {
                                merged.remove(name);
                            }
                        }
                        int maxInactive = session.isMaxInactiveChanged()
                                ? session.getMaxInactiveInterval()
                                : (int)number(row.get("max_inactive"));
                        db.execute("INSERT INTO " + TABLE + " (id, created, last_accessed, "
                                + "max_inactive, attributes, version) VALUES (?, ?, ?, ?, ?, 0)",
                                new Object[] {session.getId(), new Long(number(row.get("created"))),
                                new Long(session.getLastAccessedTime()),
                                new Integer(maxInactive), Json.write(merged)});
                        db.execute("DELETE FROM " + TABLE + " WHERE id = ?",
                                new Object[] {previousId});
                        return null;
                    }
                });
            } catch (IOException err) {
                throw err;
            } catch (Exception err) {
                throw new IOException("Could not rotate the session: " + err.getMessage());
            }
            session.storedAccessed = session.getLastAccessedTime();
        }

        void touchIfStale(HttpSession session) throws IOException {
            // Reads keep a session alive too, but writing the time on every one
            // would turn every request into a database write. Within a minute is
            // close enough for a timeout measured in tens of minutes.
            long now = System.currentTimeMillis();
            if(now - session.storedAccessed < touchInterval(session.getMaxInactiveInterval())) {
                return;
            }
            // Forward only: a slower request must not move the last use back.
            Long at = new Long(now);
            pool.execute("UPDATE " + TABLE + " SET last_accessed = CASE WHEN last_accessed "
                    + "> ? THEN last_accessed ELSE ? END WHERE id = ?",
                    new Object[] {at, at, session.getId()});
            session.storedAccessed = now;
        }

        /**
         * How stale the stored last use may get: a minute, or a quarter of the
         * timeout when that is shorter. A fixed minute let a ten-second session
         * used every five seconds expire, because its reads were never written.
         */
        static long touchInterval(int maxInactiveSeconds) {
            if(maxInactiveSeconds <= 0) {
                return TOUCH_INTERVAL;
            }
            return Math.min(TOUCH_INTERVAL, maxInactiveSeconds * 250L);
        }

        public void delete(String id) throws IOException {
            prepare();
            pool.execute("DELETE FROM " + TABLE + " WHERE id = ?", new Object[] {id});
        }

        public int purgeExpired(long now) throws IOException {
            return purgeExpired(now, java.util.Collections.EMPTY_SET);
        }

        /** {@link #purgeExpired(long)}, sparing the sessions whose ids are in {@code busy}. */
        int purgeExpired(long now, java.util.Set busy) throws IOException {
            prepare();
            // The same grace load() allows: the timeout plus the touch interval,
            // min(a minute, a quarter of the timeout).
            Long at = new Long(now);
            // One placeholder per session in use -- as many as requests running,
            // never the table.
            StringBuilder spare = new StringBuilder();
            List params = new ArrayList();
            params.add(at);
            params.add(at);
            if(!busy.isEmpty()) {
                spare.append(" AND id NOT IN (");
                Iterator ids = busy.iterator();
                for(int iter = 0 ; ids.hasNext() ; iter++) {
                    spare.append(iter == 0 ? "?" : ", ?");
                    params.add(ids.next());
                }
                spare.append(')');
            }
            //
            // Decimal multipliers, never integer ones: max_inactive is an INTEGER
            // column, and PostgreSQL multiplies INTEGER by an integer literal in
            // 32 bits, so a 30-day timeout overflowed and every purge failed.
            // CAST(... AS BIGINT) is not MySQL; a decimal literal widens on all
            // three engines, exactly on PostgreSQL and MySQL, and within a
            // double's exact range on SQLite.
            return pool.execute("DELETE FROM " + TABLE + " WHERE max_inactive > 0 AND "
                    + "(last_accessed + max_inactive * 1250.0 < ? OR "
                    + "last_accessed + max_inactive * 1000.0 + " + TOUCH_INTERVAL + " < ?)"
                    + spare.toString(), params.toArray());
        }

        public int size() {
            return -1;
        }
    }
}
