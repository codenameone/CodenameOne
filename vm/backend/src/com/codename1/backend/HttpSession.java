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

/**
 * State kept for one client across requests, found again through a cookie.
 *
 * <pre>
 *   &#64;PostMapping("/login")
 *   public void login(HttpServer.Request request, &#64;RequestBody Map body) {
 *       ...
 *       HttpSession session = request.getSession(true);
 *       session.changeSessionId();          // never keep a pre-login id
 *       session.setAttribute("user", userId);
 *   }
 * </pre>
 *
 * <p>A session is created only when something asks for one with
 * {@code getSession(true)}, so a server that never does sets no cookie and keeps
 * nothing. The cookie is {@code HttpOnly}, {@code SameSite=Lax} and, on a TLS
 * server, {@code Secure}; its name, lifetime and store are configured under
 * {@code cn1.session.*}. See {@link Sessions}.
 *
 * <p>Attributes live in the {@link SessionStore}. The in-memory store keeps any
 * object; the database store keeps what {@link Json} can write -- strings,
 * numbers, booleans, and maps and lists of those -- because it has to read them
 * back in another process.
 *
 * <p>A {@code @SessionScope} bean lives on the session object itself and is
 * never written to a store, so a session read back from the database store
 * starts with a fresh one.
 */
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
    private Object[] beans;
    /** The last-access time the store holds, which lags the live one. */
    long storedAccessed;

    HttpSession(String id, long created, long lastAccessed, int maxInactiveSeconds) {
        this.id = id;
        this.created = created;
        this.lastAccessed = lastAccessed;
        this.storedAccessed = lastAccessed;
        this.maxInactiveSeconds = maxInactiveSeconds;
    }

    /** The id the cookie carries. Secret: never log it. */
    public synchronized String getId() {
        return id;
    }

    /** Whether this session was created by the current request. */
    public synchronized boolean isNew() {
        return fresh;
    }

    public long getCreationTime() {
        return created;
    }

    public synchronized long getLastAccessedTime() {
        return lastAccessed;
    }

    /** Seconds of inactivity after which the session is discarded. */
    public synchronized int getMaxInactiveInterval() {
        return maxInactiveSeconds;
    }

    public synchronized void setMaxInactiveInterval(int seconds) {
        maxInactiveSeconds = seconds;
        dirty = true;
    }

    public synchronized Object getAttribute(String name) {
        checkValid();
        return attributes.get(name);
    }

    public synchronized void setAttribute(String name, Object value) {
        checkValid();
        if(value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
        dirty = true;
    }

    public synchronized void removeAttribute(String name) {
        checkValid();
        if(attributes.remove(name) != null) {
            dirty = true;
        }
    }

    /** The attribute names, as a copy. */
    public synchronized List getAttributeNames() {
        return new ArrayList(attributes.keySet());
    }

    /** Ends the session: its attributes are dropped and the client's cookie cleared. */
    public synchronized void invalidate() {
        checkValid();
        invalid = true;
        attributes.clear();
        beans = null;
        dirty = true;
    }

    public synchronized boolean isValid() {
        return !invalid;
    }

    /**
     * Gives the session a new id, keeping its attributes, and sends the client the
     * new cookie. Call it when a user signs in: an id a client held before
     * authenticating is one an attacker may have planted.
     *
     * @return the new id
     */
    public String changeSessionId() {
        String next = Sessions.newId();
        synchronized(this) {
            checkValid();
            if(previousId == null) {
                previousId = id;
            }
            id = next;
            dirty = true;
        }
        return next;
    }

    private void checkValid() {
        if(invalid) {
            throw new IllegalStateException("This session has been invalidated");
        }
    }

    // ---------------------------------------------------------------- store use

    synchronized void touch(long now) {
        lastAccessed = now;
    }

    synchronized boolean isExpired(long now) {
        return maxInactiveSeconds > 0 && now - lastAccessed > maxInactiveSeconds * 1000L;
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
    }

    /** The id the client held before {@link #changeSessionId}, or null. */
    synchronized String previousId() {
        return previousId;
    }

    /** A copy of the attributes, for a store to write. */
    synchronized Map attributesCopy() {
        return new LinkedHashMap(attributes);
    }

    synchronized void loadAttributes(Map values) {
        attributes.clear();
        if(values != null) {
            attributes.putAll(values);
        }
    }

    /**
     * The {@code @SessionScope} beans of this session, by the slot the build gave
     * each. Called by generated code.
     */
    public synchronized Object[] scopedBeans(int count) {
        if(beans == null || beans.length < count) {
            Object[] grown = new Object[count];
            if(beans != null) {
                System.arraycopy(beans, 0, grown, 0, beans.length);
            }
            beans = grown;
        }
        return beans;
    }
}
