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
package com.codename1.backend.security.apikey;

import com.codename1.backend.security.AbstractAuthenticationToken;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.SimpleGrantedAuthority;
import java.util.ArrayList;
import java.util.List;

/// Who a request is from, when an API key says so. The principal is the
/// [ApiKey], the name is the key's owner, and each of the key's scopes is the
/// authority `SCOPE_x` -- the same authorities a token with those scopes gets,
/// so one set of rules covers both.
public final class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
    private final ApiKey apiKey;

    public ApiKeyAuthenticationToken(ApiKey apiKey) {
        super(authorities(apiKey));
        this.apiKey = apiKey;
        super.setAuthenticated(true);
    }

    private static List<GrantedAuthority> authorities(ApiKey apiKey) {
        List<GrantedAuthority> granted = new ArrayList<GrantedAuthority>();
        for (String scope : apiKey.getScopes()) {
            granted.add(new SimpleGrantedAuthority("SCOPE_" + scope));
        }
        return granted;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return apiKey;
    }

    /// Null: the key itself is not kept once it has been recognized.
    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public String getName() {
        return apiKey.getOwner();
    }
}
