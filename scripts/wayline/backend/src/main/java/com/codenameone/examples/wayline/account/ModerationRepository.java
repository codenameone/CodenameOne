/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.wayline.account;

import com.codename1.backend.annotations.Component;
import com.codename1.orm.session.Session;
import com.codenameone.examples.wayline.domain.ModerationEvent;

import java.util.List;

/// Where the record of what the admins did is read from and written to.
@Component
public class ModerationRepository {
    private final Session session;

    public ModerationRepository(Session session) {
        this.session = session;
    }

    /// When the newest event of an account was written, or null when it has
    /// none.
    public Long newest(String username) {
        return session.createQuery("select max(e.createdAt) from ModerationEvent e "
                + "where e.username = :username", Long.class)
                .setParameter("username", username).first();
    }

    public void add(ModerationEvent event) {
        session.persist(event);
    }

    /// An account's events, newest first, and no more than `limit` of them.
    public List<ModerationEvent> of(String username, int limit) {
        return session.query(ModerationEvent.class).eq("username", username)
                .orderBy("createdAt", false).orderBy("id", true).limit(limit).list();
    }

    /// Deletes every event of an account. A bulk statement: whatever the
    /// session had loaded before it is stale afterwards.
    public void removeAll(String username) {
        session.createQuery("delete from ModerationEvent e where e.username = :username")
                .setParameter("username", username).executeUpdate();
    }
}
