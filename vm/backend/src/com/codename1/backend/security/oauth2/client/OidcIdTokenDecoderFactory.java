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

import com.codename1.backend.security.oauth2.core.OAuth2Error;
import com.codename1.backend.security.oauth2.core.OAuth2ErrorCodes;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidator;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidatorResult;
import com.codename1.backend.security.oauth2.jose.jws.JwsAlgorithm;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.jwt.JwtDecoder;
import com.codename1.backend.security.oauth2.jwt.JwtTimestampValidator;
import com.codename1.backend.security.oauth2.jwt.RemoteJwkSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Makes, and keeps, the decoder that verifies the ID tokens of one
/// registration.
///
/// A token is accepted when its signature verifies under a key the provider
/// publishes at its `jwks_uri`, with a public key algorithm the registration
/// allows -- never one the token merely names -- and when
///
/// - `iss` is the registration's issuer, or for a provider with one issuer per
///   tenant, the template with the token's own `tid` in it;
/// - `aud` contains the client id, and when it names more than one audience,
///   `azp` is the client id;
/// - `exp` has not passed, and `iat` and `sub` are there.
///
/// The `nonce` is checked by the sign-in, which is what knows the value that
/// was sent.
public final class OidcIdTokenDecoderFactory {
    private final RemoteJwkSet.Fetcher fetcher;
    private final Map<String, JwtDecoder> decoders = new HashMap<String, JwtDecoder>();

    public OidcIdTokenDecoderFactory() {
        this(RemoteJwkSet.WEB);
    }

    /// @param fetcher what reads the provider's keys
    public OidcIdTokenDecoderFactory(RemoteJwkSet.Fetcher fetcher) {
        this.fetcher = fetcher;
    }

    /// The decoder of `registration`: one for as long as the server runs, so
    /// the provider's keys are fetched once and kept.
    public synchronized JwtDecoder createDecoder(ClientRegistration registration) {
        JwtDecoder decoder = decoders.get(registration.getRegistrationId());
        if (decoder == null) {
            ClientRegistration.ProviderDetails p = registration.getProviderDetails();
            if (p.getJwkSetUri() == null) {
                throw new IllegalArgumentException("The registration "
                        + registration.getRegistrationId() + " has no jwkSetUri to verify an "
                        + "ID token with");
            }
            List<JwsAlgorithm> algorithms = new ArrayList<JwsAlgorithm>();
            String[] named = p.getIdTokenAlgorithms();
            if (named != null) {
                for (String name : named) {
                    SignatureAlgorithm algorithm = SignatureAlgorithm.from(name);
                    if (algorithm != null && !algorithms.contains(algorithm)) {
                        algorithms.add(algorithm);
                    }
                }
            }
            if (algorithms.isEmpty()) {
                algorithms.add(SignatureAlgorithm.RS256);
                algorithms.add(SignatureAlgorithm.ES256);
            }
            DefaultJwtDecoder made = DefaultJwtDecoder.withJwkSource(
                    new RemoteJwkSet(p.getJwkSetUri(), fetcher))
                    .jwsAlgorithms(algorithms.toArray(new JwsAlgorithm[algorithms.size()]))
                    .build();
            made.setJwtValidator(new IdTokenValidator(registration));
            decoder = made;
            decoders.put(registration.getRegistrationId(), decoder);
        }
        return decoder;
    }

    /// The claims an ID token must have right, whatever its signature.
    static final class IdTokenValidator implements OAuth2TokenValidator<Jwt> {
        private final ClientRegistration registration;
        private final JwtTimestampValidator timestamps = new JwtTimestampValidator();

        IdTokenValidator(ClientRegistration registration) {
            this.registration = registration;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            List<OAuth2Error> errors = new ArrayList<OAuth2Error>(
                    timestamps.validate(token).getErrors());
            ClientRegistration.ProviderDetails p = registration.getProviderDetails();
            String issuer = token.getIssuer();
            String expected = p.getIssuerUri();
            if (expected == null && p.getIssuerTemplate() != null) {
                String tenant = token.getClaimAsString("tid");
                expected = !ClientRegistration.ProviderDetails.isTenant(tenant) ? null
                        : replace(p.getIssuerTemplate(), "{tenantid}", tenant);
            }
            if (issuer == null || expected == null || !expected.equals(issuer)) {
                errors.add(invalid("The iss claim is not the registration's issuer"));
            }
            String clientId = registration.getClientId();
            List<String> audience = token.getAudience();
            Object authorizedParty = token.getClaims().get("azp");
            if (!audience.contains(clientId)) {
                errors.add(invalid("The aud claim does not name this client"));
            } else if ((audience.size() > 1 || authorizedParty != null)
                    && !clientId.equals(authorizedParty)) {
                errors.add(invalid("The token's azp claim does not name this client"));
            }
            if (token.getExpiresAt() == null) {
                errors.add(invalid("The exp claim is missing"));
            }
            if (token.getIssuedAt() == null) {
                errors.add(invalid("The iat claim is missing"));
            }
            if (token.getSubject() == null || token.getSubject().length() == 0) {
                errors.add(invalid("The sub claim is missing"));
            }
            return errors.isEmpty() ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(errors);
        }

        private static OAuth2Error invalid(String description) {
            return new OAuth2Error(OAuth2ErrorCodes.INVALID_ID_TOKEN, description, null);
        }

        private static String replace(String text, String what, String with) {
            int at = text.indexOf(what);
            return at < 0 ? text : text.substring(0, at) + with
                    + text.substring(at + what.length());
        }
    }
}
