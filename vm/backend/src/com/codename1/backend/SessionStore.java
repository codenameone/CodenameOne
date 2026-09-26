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

/**
 * Where {@link HttpSession}s are kept between requests.
 *
 * <p>Two are provided, chosen by {@code cn1.session.store}: {@code memory} (the
 * default) and {@code jdbc}, which keeps them in the server's database so any
 * instance behind a load balancer can serve any client. Implement this for
 * another -- a cache server -- and pass it to {@link Sessions#setStore}.
 */
public interface SessionStore {
    /** The session with this id, or null when there is none or it expired. */
    HttpSession load(String id) throws IOException;

    /**
     * Records a session after a request that created or changed it. When its id
     * changed, {@code previousId} names the entry to drop; otherwise it is null.
     */
    void save(HttpSession session, String previousId) throws IOException;

    /** Forgets a session. */
    void delete(String id) throws IOException;

    /** Drops every session that has expired by {@code now}; answers how many. */
    int purgeExpired(long now) throws IOException;

    /** How many sessions are kept, or -1 when that is expensive to know. */
    int size();
}
