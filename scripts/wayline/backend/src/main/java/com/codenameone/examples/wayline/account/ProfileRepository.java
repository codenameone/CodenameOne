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
import com.codenameone.examples.wayline.domain.Profile;

import java.util.List;

/// Where profiles are read from and written to.
///
/// A repository holds the queries of one kind of entity and nothing else: no
/// rule about who may do what, and no transaction of its own. It works on the
/// session of the transaction its caller opened, so a service method marked
/// `@Transactional` is what makes several of these calls one unit of work.
@Component
public class ProfileRepository {
    private final Session session;

    public ProfileRepository(Session session) {
        this.session = session;
    }

    /// The profile of an account, or null when the name has none.
    public Profile find(String username) {
        if (username == null || username.length() == 0 || username.length() > 190) {
            return null;
        }
        return session.find(Profile.class, username);
    }

    /// Every profile, newest first.
    public List<Profile> all() {
        return session.query(Profile.class).orderBy("createdAt", false)
                .orderBy("username", true).list();
    }

    /// Stores a new profile. One already read from here needs no call at all:
    /// the transaction writes whatever changed in it when it commits.
    public void add(Profile profile) {
        session.persist(profile);
    }

    public void remove(Profile profile) {
        session.remove(profile);
    }
}
