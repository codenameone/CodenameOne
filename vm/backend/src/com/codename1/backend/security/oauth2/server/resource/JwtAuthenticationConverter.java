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

import com.codename1.backend.security.AbstractAuthenticationToken;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.oauth2.core.Converter;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import java.util.Collection;

/// Makes the [JwtAuthenticationToken] of a verified token: its authorities,
/// through a [JwtGrantedAuthoritiesConverter] unless another is set, and its
/// name, from the `sub` claim unless another is named.
///
/// ```java
/// JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
/// converter.setPrincipalClaimName("preferred_username");
/// http.oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
/// ```
public final class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private Converter<Jwt, Collection<GrantedAuthority>> authorities =
            new JwtGrantedAuthoritiesConverter();
    private String principalClaimName = "sub";

    /// What reads the authorities out of a token.
    public void setJwtGrantedAuthoritiesConverter(
            Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter) {
        if (jwtGrantedAuthoritiesConverter == null) {
            throw new IllegalArgumentException("jwtGrantedAuthoritiesConverter cannot be null");
        }
        this.authorities = jwtGrantedAuthoritiesConverter;
    }

    /// The claim `Authentication.getName()` answers with; `sub` unless set.
    public void setPrincipalClaimName(String principalClaimName) {
        if (principalClaimName == null || principalClaimName.length() == 0) {
            throw new IllegalArgumentException("principalClaimName cannot be empty");
        }
        this.principalClaimName = principalClaimName;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, authorities.convert(jwt),
                jwt.getClaimAsString(principalClaimName));
    }
}
