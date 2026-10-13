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
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.domain.ClosedAccount;
import java.io.IOException;

/// The e-mails that had an account once and may not have another.
///
/// A ride outlives the account that took it, under the e-mail that account
/// signed in with, and so does a token until it expires. Were the address free
/// to register again, whoever took it would be handed the rides, the receipts
/// and the ratings of the person who left, and that person's old token would
/// sign in to the new account. So closing an account retires its address for
/// good. Only a hash of it is kept: enough to refuse it, and not a list of who
/// has left.
@Component
public class ClosedAccountRepository {
    private final Session session;

    public ClosedAccountRepository(Session session) {
        this.session = session;
    }

    /// Whether `username` belonged to an account that was closed.
    public boolean closed(String username) throws IOException {
        return session.find(ClosedAccount.class, Ids.sha256(username)) != null;
    }

    /// Retires `username`. Call it in the transaction that deletes the account.
    public void close(String username) throws IOException {
        String hash = Ids.sha256(username);
        if (session.find(ClosedAccount.class, hash) == null) {
            ClosedAccount row = new ClosedAccount();
            row.usernameHash = hash;
            row.closedAt = System.currentTimeMillis();
            session.persist(row);
        }
    }
}
