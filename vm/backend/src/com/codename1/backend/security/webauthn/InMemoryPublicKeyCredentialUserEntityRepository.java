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
package com.codename1.backend.security.webauthn;

import com.codename1.backend.Base64Url;
import java.util.HashMap;
import java.util.Map;

/// User handles kept in this process: gone when it stops, so that every
/// passkey registered before then finds no user after. For a test or a
/// demonstration; a deployment keeps them in the database with
/// [JdbcPublicKeyCredentialUserEntityRepository].
public final class InMemoryPublicKeyCredentialUserEntityRepository
        implements PublicKeyCredentialUserEntityRepository {
    private final Map<String, PublicKeyCredentialUserEntity> byName =
            new HashMap<String, PublicKeyCredentialUserEntity>();
    private final Map<String, PublicKeyCredentialUserEntity> byId =
            new HashMap<String, PublicKeyCredentialUserEntity>();

    /// A name as users are keyed by it: `A` to `Z` folded to lower case and
    /// nothing else, as the database-backed repository keys them. Folded here
    /// rather than by the schema's own method, so that a server which keeps
    /// passkeys in memory does not carry the schema.
    private static String key(String username) {
        char[] chars = username.toCharArray();
        for (int iter = 0 ; iter < chars.length ; iter++) {
            char c = chars[iter];
            if (c >= 'A' && c <= 'Z') {
                chars[iter] = (char) (c + ('a' - 'A'));
            }
        }
        return new String(chars);
    }

    @Override
    public synchronized PublicKeyCredentialUserEntity findById(byte[] id) {
        return id == null ? null : byId.get(Base64Url.encode(id));
    }

    @Override
    public synchronized PublicKeyCredentialUserEntity findByUsername(String username) {
        return username == null ? null : byName.get(key(username));
    }

    @Override
    public synchronized PublicKeyCredentialUserEntity save(PublicKeyCredentialUserEntity user) {
        String key = key(user.getName());
        PublicKeyCredentialUserEntity first = byName.get(key);
        if (first != null) {
            return first;
        }
        String id = Base64Url.encode(user.getId());
        if (byId.containsKey(id)) {
            throw new IllegalArgumentException("Another user has this handle already");
        }
        byName.put(key, user);
        byId.put(id, user);
        return user;
    }

    @Override
    public synchronized void delete(byte[] id) {
        if (id == null) {
            return;
        }
        PublicKeyCredentialUserEntity gone = byId.remove(Base64Url.encode(id));
        if (gone != null) {
            byName.remove(key(gone.getName()));
        }
    }
}
