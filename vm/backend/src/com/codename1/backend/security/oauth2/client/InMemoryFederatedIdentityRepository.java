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
package com.codename1.backend.security.oauth2.client;

import com.codename1.backend.security.SecuritySchema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Identities kept in this process: gone when it stops, and unknown to any
/// other. For development and tests; see [JdbcFederatedIdentityRepository].
public final class InMemoryFederatedIdentityRepository implements FederatedIdentityRepository {
    /// provider, NUL, subject to the user name.
    private final Map<String, String> links = new LinkedHashMap<String, String>();

    private static String key(String provider, String subject) {
        return provider + '\0' + subject;
    }

    @Override
    public synchronized String findUsername(String provider, String subject) {
        return links.get(key(provider, subject));
    }

    @Override
    public synchronized String link(String provider, String subject, String username) {
        String existing = links.get(key(provider, subject));
        if (existing != null) {
            return existing;
        }
        links.put(key(provider, subject), username);
        return username;
    }

    @Override
    public synchronized boolean unlink(String provider, String subject) {
        return links.remove(key(provider, subject)) != null;
    }

    @Override
    public synchronized List<String[]> findByUsername(String username) {
        List<String[]> out = new ArrayList<String[]>();
        String wanted = SecuritySchema.usernameKey(username);
        for (Map.Entry<String, String> link : links.entrySet()) {
            if (SecuritySchema.usernameKey(link.getValue()).equals(wanted)) {
                int nul = link.getKey().indexOf('\0');
                out.add(new String[] {link.getKey().substring(0, nul),
                        link.getKey().substring(nul + 1)});
            }
        }
        return out;
    }
}
