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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// State kept for one client across requests, found again through a cookie.
///
/// ```java
/// @PostMapping("/login")
///   public void login(HttpServer.Request request, @RequestBody Map body) {
///       ...
///       HttpSession session = request.getSession(true);
///       session.changeSessionId();          // never keep a pre-login id
///       session.setAttribute("user", userId);
///   }
/// ```
///
/// A session is created only when something asks for one with
/// `getSession(true)`, so a server that never does sets no cookie and keeps
/// nothing. The cookie is `HttpOnly`, `SameSite=Lax` and, on a TLS
/// server, `Secure`; its name, lifetime and store are configured under
/// `cn1.session.*`.
///
/// Attributes live in the [SessionStore]. The in-memory store keeps any
/// object; the database store keeps what [Json] can write -- strings,
/// numbers, booleans, and maps and lists of those -- because it has to read them
/// back in another process.
///
/// A session-scoped bean is never written to a store: the server that
/// built it keeps it in memory for as long as the session lives, and runs its
/// destroy methods when the session is invalidated, expires or the server stops.
/// Another instance behind a load balancer builds its own.
public final class HttpSession {
    private String id;
    private final long created;
    private long lastAccessed;
    private int maxInactiveSeconds;
    private final Map attributes = new LinkedHashMap();
    private boolean invalid;
    private boolean fresh;
    private boolean dirty;
    private String previousId;
    /// Set by a store whose rotation found the old row already gone -- another
    /// request of the same client rotated or invalidated it first -- so the new
    /// id has nothing stored under it and must not be sent to the client.
    private boolean rotationLost;
    /// The attribute names this request set or removed, so a store that loads a
    /// copy per request can apply just these onto what another request saved in
    /// the meantime, instead of replacing it with this copy's stale whole map.
    private final java.util.Set changed = new java.util.HashSet();
    private boolean maxInactiveChanged;
    private Object[] beans;
    /// The last-access time the store holds, which lags the live one.
    long storedAccessed;

    HttpSession(String id, long created, long lastAccessed, int maxInactiveSeconds) {
        this.id = id;
        this.created = created;
        this.lastAccessed = lastAccessed;
        this.storedAccessed = lastAccessed;
        this.maxInactiveSeconds = maxInactiveSeconds;
    }

    /// The id the cookie carries. Secret: never log it.
    public synchronized String getId() {
        return id;
    }

    /// Whether this session was created by the current request.
    public synchronized boolean isNew() {
        return fresh;
    }

    public long getCreationTime() {
        return created;
    }

    public synchronized long getLastAccessedTime() {
        return lastAccessed;
    }

    /// Seconds of inactivity after which the session is discarded.
    public synchronized int getMaxInactiveInterval() {
        return maxInactiveSeconds;
    }

    public synchronized void setMaxInactiveInterval(int seconds) {
        maxInactiveSeconds = seconds;
        maxInactiveChanged = true;
        dirty = true;
    }

    public synchronized Object getAttribute(String name) {
        checkValid();
        return attributes.get(name);
    }

    public synchronized void setAttribute(String name, Object value) {
        checkValid();
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
        changed.add(name);
        dirty = true;
    }

    public synchronized void removeAttribute(String name) {
        checkValid();
        if (attributes.remove(name) != null) {
            changed.add(name);
            dirty = true;
        }
    }

    /// Removes and returns an attribute once across every request and server
    /// sharing the session store. A stale request copy cannot consume it again.
    /// @throws java.io.IOException if the store cannot perform the atomic operation
    /// @since 8.0
    public Object consumeAttribute(String name) throws java.io.IOException {
        String sessionId;
        Object before;
        synchronized (this) {
            checkValid();
            if (owner == null || fresh) {
                return consumeLocalAttribute(name);
            }
            sessionId = id;
            before = attributes.get(name);
        }
        Object taken = owner.getStore().consumeAttribute(sessionId, name);
        synchronized (this) {
            // Forget this request's snapshot without scheduling another removal
            // at save time: a later request may already have issued new options.
            if (attributes.get(name) == before) { //NOPMD CompareObjectsWithEquals - same snapshot
                attributes.remove(name);
                changed.remove(name);
            }
        }
        return taken;
    }

    synchronized Object consumeLocalAttribute(String name) {
        checkValid();
        Object taken = attributes.get(name);
        removeAttribute(name);
        return taken;
    }

    /// The attribute names, as a copy.
    public synchronized List getAttributeNames() {
        return new ArrayList(attributes.keySet());
    }

    /// Ends the session: its attributes are dropped and the client's cookie cleared.
    public synchronized void invalidate() {
        checkValid();
        invalid = true;
        attributes.clear();
        // The session-scoped beans stay until the request ends, when the server
        // runs their destroy methods; dropping them here would skip those.
        dirty = true;
    }

    public synchronized boolean isValid() {
        return !invalid;
    }

    /// Gives the session a new id, keeping its attributes, and sends the client the
    /// new cookie. Call it when a user signs in: an id a client held before
    /// authenticating is one an attacker may have planted.
    ///
    /// #### Returns
    ///
    /// the new id
    public String changeSessionId() {
        String next = Sessions.newId();
        synchronized (this) {
            checkValid();
            if (previousId == null) {
                previousId = id;
            }
            id = next;
            dirty = true;
            rotatedBy = SERVING.get();
        }
        return next;
    }

    /// The request the server is serving on this thread, for [#changeSessionId]
    /// to record who rotated.
    private static final ThreadLocal SERVING = new ThreadLocal();

    /// The request that rotated this session, or null outside a request.
    private Object rotatedBy;

    /// Marks `request` as the one this thread serves; answers what to restore.
    static Object enterRequest(Object request) {
        Object previous = SERVING.get();
        SERVING.set(request);
        return previous;
    }

    static void leaveRequest(Object previous) {
        SERVING.set(previous);
    }

    /// Whether a pending rotation is `request`'s own to store and announce.
    /// The memory store hands every request one shared object, so a request can
    /// find another's login halfway through: finishing THAT rotation stored it
    /// and sent the new id in its own response -- to whoever presented the old
    /// cookie, the attacker in a fixation -- and the login announced nothing.
    synchronized boolean rotatedFor(Object request) {
        return rotatedBy == null || rotatedBy == request; //NOPMD CompareObjectsWithEquals - the request itself, by identity
    }

    private void checkValid() {
        if (invalid) {
            throw new IllegalStateException("This session has been invalidated");
        }
    }

    // ---------------------------------------------------------------- store use

    synchronized void touch(long now) {
        lastAccessed = now;
    }

    /// Extra time before expiry, for a store whose stored last use lags.
    private long expiryGraceMillis;

    /// Allows `millis` past the timeout before expiry; see Sessions.Db.
    synchronized void setExpiryGrace(long millis) {
        expiryGraceMillis = millis;
    }

    synchronized boolean isExpired(long now) {
        return maxInactiveSeconds > 0
                && now - lastAccessed > maxInactiveSeconds * 1000L + expiryGraceMillis;
    }

    synchronized void markNew() {
        fresh = true;
        dirty = true;
    }

    synchronized boolean isDirty() {
        return dirty;
    }

    synchronized void clean() {
        dirty = false;
        fresh = false;
        previousId = null;
        rotatedBy = null;
        rotationLost = false;
        changed.clear();
        maxInactiveChanged = false;
    }

    /// The names this request set or removed, as a copy.
    synchronized java.util.Set changedNames() {
        return new java.util.HashSet(changed);
    }

    synchronized boolean isMaxInactiveChanged() {
        return maxInactiveChanged;
    }

    /// Takes the session back to `previous`, the id another request's
    /// rotation already announced; see Sessions.finish.
    synchronized void undoRotation(String previous) {
        id = previous;
        previousId = null;
        rotatedBy = null;
    }

    synchronized void markRotationLost() {
        rotationLost = true;
    }

    synchronized boolean isRotationLost() {
        return rotationLost;
    }

    /// The id the client held before [#changeSessionId], or null.
    synchronized String previousId() {
        return previousId;
    }

    /// A copy of the attributes, for a store to write.
    synchronized Map attributesCopy() {
        return new LinkedHashMap(attributes);
    }

    synchronized void loadAttributes(Map values) {
        attributes.clear();
        if (values != null) {
            attributes.putAll(values);
        }
    }

    /// The server whose sessions this belongs to, which keeps the session-scoped
    /// beans; null for a session made outside one.
    Sessions owner;

    /// The beans held on this object itself, when it has no owner; taken once.
    synchronized Object[] takeLocalBeans() {
        Object[] out = beans;
        beans = null;
        return out;
    }

    /// What generated code locks while it builds one of this session's beans: an
    /// object every loaded copy of the session shares.
    Object beanLock() {
        Sessions o = owner;
        return o == null ? this : o.beanLock(this);
    }

    /// The session-scoped beans of this session, by the slot the build gave
    /// each. Called by generated code.
    Object[] scopedBeans(int count) {
        Sessions o = owner;
        if (o != null) {
            return o.sharedBeans(this, count);
        }
        return localBeans(count);
    }

    private synchronized Object[] localBeans(int count) {
        if (beans == null || beans.length < count) {
            Object[] grown = new Object[count];
            if (beans != null) {
                System.arraycopy(beans, 0, grown, 0, beans.length);
            }
            beans = grown;
        }
        return beans;
    }
}
