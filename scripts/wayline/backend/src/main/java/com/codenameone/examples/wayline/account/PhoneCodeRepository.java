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
import com.codenameone.examples.wayline.domain.PhoneCode;

/// Where the code an account was texted is kept. An account has at most one.
@Component
public class PhoneCodeRepository {
    private final Session session;

    public PhoneCodeRepository(Session session) {
        this.session = session;
    }

    /// The code an account is waiting to type, or null when it has none.
    public PhoneCode find(String username) {
        return session.find(PhoneCode.class, username);
    }

    public void add(PhoneCode code) {
        session.persist(code);
    }

    public void remove(PhoneCode code) {
        session.remove(code);
    }

    /// Counts one guess against an account's code. The database does the sum,
    /// so two guesses sent at once each take one; nothing is read and written
    /// back. Answers false when the account has no code to guess at.
    public boolean countGuess(String username) {
        return session.increment(PhoneCode.class, username, "attempts", 1L);
    }
}
