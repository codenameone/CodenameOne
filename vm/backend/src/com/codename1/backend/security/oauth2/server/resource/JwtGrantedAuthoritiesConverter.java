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
package com.codename1.backend.security.oauth2.server.resource;

import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.SimpleGrantedAuthority;
import com.codename1.backend.security.oauth2.core.Converter;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/// Reads a token's authorities out of one claim: each value, with a prefix in
/// front.
///
/// Unless told otherwise the claim is `scope` -- one text, the scopes separated
/// by spaces, as RFC 8693 and most issuers write it -- or else `scp`, and the
/// prefix is `SCOPE_`. A token with `"scope": "orders:read orders:write"` is
/// granted `SCOPE_orders:read` and `SCOPE_orders:write`, which is what
/// `hasAuthority("SCOPE_orders:read")` asks for.
///
/// ```java
/// JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
/// roles.setAuthoritiesClaimName("roles");     // ["ADMIN", "USER"]
/// roles.setAuthorityPrefix("ROLE_");          // so hasRole("ADMIN") matches
/// ```
public final class JwtGrantedAuthoritiesConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {
    private String authorityPrefix = "SCOPE_";
    private String authoritiesClaimName;

    /// What goes in front of every value; `SCOPE_` unless set, and empty for
    /// none.
    public void setAuthorityPrefix(String authorityPrefix) {
        if (authorityPrefix == null) {
            throw new IllegalArgumentException("authorityPrefix cannot be null");
        }
        this.authorityPrefix = authorityPrefix;
    }

    /// The claim to read, in place of `scope` and `scp`.
    public void setAuthoritiesClaimName(String authoritiesClaimName) {
        if (authoritiesClaimName == null || authoritiesClaimName.length() == 0) {
            throw new IllegalArgumentException("authoritiesClaimName cannot be empty");
        }
        this.authoritiesClaimName = authoritiesClaimName;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> granted = new ArrayList<GrantedAuthority>();
        for (String value : values(jwt)) {
            granted.add(new SimpleGrantedAuthority(authorityPrefix + value));
        }
        return granted;
    }

    private List<String> values(Jwt jwt) {
        Object claim;
        if (authoritiesClaimName != null) {
            claim = jwt.getClaim(authoritiesClaimName);
        } else {
            claim = jwt.getClaim("scope");
            if (claim == null) {
                claim = jwt.getClaim("scp");
            }
        }
        List<String> out = new ArrayList<String>();
        if (claim instanceof String) {
            // Words between spaces, without String.split: the translated
            // runtime has no regular expressions.
            String text = (String) claim;
            int start = 0;
            for (int iter = 0 ; iter <= text.length() ; iter++) {
                if (iter == text.length() || text.charAt(iter) == ' ') {
                    if (iter > start) {
                        out.add(text.substring(start, iter));
                    }
                    start = iter + 1;
                }
            }
        } else if (claim instanceof Collection) {
            for (Object value : (Collection<?>) claim) {
                if (value != null && value.toString().length() > 0) {
                    out.add(value.toString());
                }
            }
        }
        return out;
    }
}
