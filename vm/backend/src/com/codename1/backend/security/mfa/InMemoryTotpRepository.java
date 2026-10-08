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
package com.codename1.backend.security.mfa;

import com.codename1.backend.Crypto;
import com.codename1.backend.security.SecuritySchema;
import java.util.HashMap;
import java.util.Map;

/// Secrets kept in this process: gone when it stops. For development and
/// tests; see [JdbcTotpRepository] otherwise.
public final class InMemoryTotpRepository implements TotpRepository {
    private final Map<String, TotpCredential> byUser = new HashMap<String, TotpCredential>();

    @Override
    public synchronized void save(String username, byte[] secret) {
        byUser.put(SecuritySchema.usernameKey(username),
                new TotpCredential(username, secret, false, -1));
    }

    @Override
    public synchronized TotpCredential find(String username) {
        return username == null ? null : byUser.get(SecuritySchema.usernameKey(username));
    }

    @Override
    public synchronized boolean confirm(String username) {
        TotpCredential current = find(username);
        if (current == null || current.isConfirmed()) {
            return false;
        }
        byUser.put(SecuritySchema.usernameKey(username), new TotpCredential(
                current.getUsername(), current.getSecret(), true, current.getLastUsedStep()));
        return true;
    }

    @Override
    public synchronized boolean advance(String username, long step) {
        TotpCredential current = find(username);
        if (current == null || current.getLastUsedStep() >= step) {
            return false;
        }
        byUser.put(SecuritySchema.usernameKey(username), new TotpCredential(
                current.getUsername(), current.getSecret(), current.isConfirmed(), step));
        return true;
    }

    @Override
    public synchronized boolean confirm(String username, byte[] expectedSecret, long step) {
        return accept(username, expectedSecret, step, true);
    }

    @Override
    public synchronized boolean advance(String username, byte[] expectedSecret, long step) {
        return accept(username, expectedSecret, step, false);
    }

    private boolean accept(String username, byte[] expectedSecret, long step, boolean confirming) {
        TotpCredential current = find(username);
        if (current == null || current.isConfirmed() == confirming
                || current.getLastUsedStep() >= step || expectedSecret == null
                || !Crypto.equalsConstantTime(current.getSecret(), expectedSecret)) {
            return false;
        }
        byUser.put(SecuritySchema.usernameKey(username), new TotpCredential(
                current.getUsername(), current.getSecret(), true, step));
        return true;
    }

    @Override
    public synchronized boolean delete(String username) {
        return username != null && byUser.remove(SecuritySchema.usernameKey(username)) != null;
    }
}
