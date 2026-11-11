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
package com.codename1.backend.security.mail;

import com.codename1.backend.security.SecuritySchema;
import java.util.HashMap;
import java.util.Map;

/// Addresses kept in this process: gone when it stops. For development and
/// tests; see [JdbcVerifiedEmailRepository] otherwise.
public final class InMemoryVerifiedEmailRepository implements VerifiedEmailRepository {
    private final Map<String, String> emails = new HashMap<String, String>();
    private final Map<String, Long> verified = new HashMap<String, Long>();
    private final Map<String, String> proofs = new HashMap<String, String>();

    @Override
    public synchronized void savePending(String username, String email, String proof) {
        String user = SecuritySchema.usernameKey(username);
        emails.put(user, email);
        proofs.put(user, proof);
        verified.remove(user);
    }

    @Override
    public synchronized boolean markVerified(String username, String proof, long now) {
        String user = SecuritySchema.usernameKey(username);
        String recorded = proofs.get(user);
        if (proof == null || recorded == null || !recorded.equals(proof)) {
            return false;
        }
        // Used up with the link it stands for.
        proofs.remove(user);
        verified.put(user, Long.valueOf(now));
        return true;
    }

    @Override
    public synchronized String findEmail(String username) {
        return username == null ? null : emails.get(SecuritySchema.usernameKey(username));
    }

    @Override
    public synchronized String findVerifiedEmail(String username) {
        return isVerified(username) ? findEmail(username) : null;
    }

    @Override
    public synchronized boolean isVerified(String username) {
        return username != null && verified.containsKey(SecuritySchema.usernameKey(username));
    }

    @Override
    public synchronized void delete(String username) {
        String user = SecuritySchema.usernameKey(username);
        emails.remove(user);
        proofs.remove(user);
        verified.remove(user);
    }
}
