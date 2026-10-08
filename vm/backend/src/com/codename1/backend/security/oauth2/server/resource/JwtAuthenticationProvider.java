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
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.AuthenticationProvider;
import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.oauth2.core.Converter;
import com.codename1.backend.security.oauth2.jwt.BadJwtException;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.jwt.JwtDecoder;
import com.codename1.backend.security.oauth2.jwt.JwtException;

/// Authenticates a bearer token by verifying it as a JWT.
///
/// A token the decoder refuses is an [InvalidBearerTokenException] carrying
/// the decoder's reason, which the entry point sends back as `invalid_token`.
/// A token the decoder could not judge -- the issuer's keys could not be
/// fetched -- is an [AuthenticationServiceException]: the server's failure, and
/// not reported to the caller as a bad token.
public final class JwtAuthenticationProvider implements AuthenticationProvider {
    private final JwtDecoder jwtDecoder;
    private Converter<Jwt, ? extends AbstractAuthenticationToken> converter =
            new JwtAuthenticationConverter();

    public JwtAuthenticationProvider(JwtDecoder jwtDecoder) {
        if (jwtDecoder == null) {
            throw new IllegalArgumentException("jwtDecoder cannot be null");
        }
        this.jwtDecoder = jwtDecoder;
    }

    /// What makes an authentication of a verified token; a
    /// [JwtAuthenticationConverter] unless set.
    public void setJwtAuthenticationConverter(
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter) {
        if (jwtAuthenticationConverter == null) {
            throw new IllegalArgumentException("jwtAuthenticationConverter cannot be null");
        }
        this.converter = jwtAuthenticationConverter;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (!(authentication instanceof BearerTokenAuthenticationToken)) {
            return null;
        }
        BearerTokenAuthenticationToken bearer = (BearerTokenAuthenticationToken) authentication;
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(bearer.getToken());
        } catch (BadJwtException failed) {
            throw new InvalidBearerTokenException(failed.getMessage(), failed);
        } catch (JwtException failed) {
            throw new AuthenticationServiceException(failed.getMessage(), failed);
        }
        AbstractAuthenticationToken token = converter.convert(jwt);
        if (token == null) {
            throw new AuthenticationServiceException("The JWT converter made no authentication");
        }
        if (token.getDetails() == null) {
            token.setDetails(bearer.getDetails());
        }
        return token;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
