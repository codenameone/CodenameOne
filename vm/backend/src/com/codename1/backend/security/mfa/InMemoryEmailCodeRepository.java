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
package com.codename1.backend.security.mfa;

import com.codename1.backend.security.SecuritySchema;
import java.util.HashMap;
import java.util.Map;

/// Mailed codes kept in this process: gone when it stops. For development and
/// tests; see [JdbcEmailCodeRepository] otherwise.
public final class InMemoryEmailCodeRepository implements EmailCodeRepository {
    private final Map<String, EmailCode> byUser = new HashMap<String, EmailCode>();

    @Override
    public synchronized EmailCode find(String username) {
        return username == null ? null : byUser.get(SecuritySchema.usernameKey(username));
    }

    @Override
    public synchronized void setEnabled(String username, boolean enabled) {
        byUser.put(SecuritySchema.usernameKey(username), new EmailCode(enabled, "", 0, 0, 0));
    }

    @Override
    public synchronized boolean saveCode(String username, String codeHash, long expiresAt,
            long sentAt, long sentBefore, int max) {
        String user = SecuritySchema.usernameKey(username);
        EmailCode was = byUser.get(user);
        if (was == null || !was.isEnabled()
                || was.getSentAt() > sentBefore && was.getSentAt() <= sentAt
                || was.getAttempts() >= max && was.getExpiresAt() >= sentAt) {
            return false;
        }
        byUser.put(user, new EmailCode(true, codeHash, expiresAt,
                was.getExpiresAt() < sentAt ? 0 : was.getAttempts(), sentAt));
        return true;
    }

    @Override
    public synchronized boolean countAttempt(String username, int max) {
        String user = SecuritySchema.usernameKey(username);
        EmailCode was = byUser.get(user);
        if (was == null || was.getAttempts() >= max) {
            return false;
        }
        byUser.put(user, new EmailCode(was.isEnabled(), was.getCodeHash(), was.getExpiresAt(),
                was.getAttempts() + 1, was.getSentAt()));
        return true;
    }

    @Override
    public synchronized boolean clear(String username, String codeHash, boolean signedIn) {
        String user = SecuritySchema.usernameKey(username);
        EmailCode was = byUser.get(user);
        if (was == null || codeHash == null || codeHash.length() == 0
                || !codeHash.equals(was.getCodeHash())) {
            return false;
        }
        byUser.put(user, new EmailCode(was.isEnabled(), "", 0, was.getAttempts(),
                signedIn ? 0 : was.getSentAt()));
        return true;
    }
}
