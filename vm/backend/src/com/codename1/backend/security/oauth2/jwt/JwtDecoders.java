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
package com.codename1.backend.security.oauth2.jwt;

import com.codename1.backend.Json;
import com.codename1.backend.security.oauth2.jose.jws.JwsAlgorithm;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Makes a decoder for an issuer from what the issuer says about itself.
///
/// ```java
/// JwtDecoder decoder = JwtDecoders.fromIssuerLocation("https://accounts.example.com");
/// ```
///
/// The issuer's metadata is read from, in order,
/// `<issuer>/.well-known/openid-configuration`,
/// `<host>/.well-known/openid-configuration/<path>` and
/// `<host>/.well-known/oauth-authorization-server/<path>` (OpenID Connect
/// Discovery and RFC 8414). The metadata must name this very issuer, or it is
/// refused: that is what stops one tenant's metadata being served for
/// another's. The decoder that comes back verifies with the keys at the
/// metadata's `jwks_uri` and requires every token's `iss` to be the issuer.
///
/// The metadata is fetched by this call. The keys are fetched when the first
/// token arrives.
public final class JwtDecoders {
    private JwtDecoders() {
    }

    /// A decoder for the tokens of `issuer`.
    ///
    /// - `IllegalArgumentException`: when the issuer's metadata cannot be read,
    /// names another issuer, or has no `jwks_uri`
    public static JwtDecoder fromIssuerLocation(String issuer) {
        return fromIssuerLocation(issuer, RemoteJwkSet.WEB);
    }

    /// The same, for an issuer that serves OpenID Connect Discovery.
    public static JwtDecoder fromOidcIssuerLocation(String issuer) {
        return fromIssuerLocation(issuer);
    }

    /// [#fromIssuerLocation(String)] reading through `fetcher`.
    public static JwtDecoder fromIssuerLocation(String issuer, RemoteJwkSet.Fetcher fetcher) {
        Map metadata = metadata(issuer, fetcher);
        Object jwks = metadata.get("jwks_uri");
        if (!(jwks instanceof String) || ((String) jwks).length() == 0) {
            throw new IllegalArgumentException("The metadata of " + issuer + " has no jwks_uri");
        }
        DefaultJwtDecoder decoder = DefaultJwtDecoder.withJwkSource(
                new RemoteJwkSet((String) jwks, fetcher)).jwsAlgorithms(algorithms(metadata)).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    /// The metadata document of `issuer`, checked to be its own.
    ///
    /// - `IllegalArgumentException`: when it cannot be read or names another
    /// issuer
    public static Map metadata(String issuer, RemoteJwkSet.Fetcher fetcher) {
        if (issuer == null || issuer.length() == 0) {
            throw new IllegalArgumentException("issuer cannot be empty");
        }
        String trimmed = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
        int scheme = trimmed.indexOf("://");
        int pathStart = scheme < 0 ? -1 : trimmed.indexOf('/', scheme + 3);
        String host = pathStart < 0 ? trimmed : trimmed.substring(0, pathStart);
        String path = pathStart < 0 ? "" : trimmed.substring(pathStart);
        String[] locations = {trimmed + "/.well-known/openid-configuration",
            host + "/.well-known/openid-configuration" + path,
            host + "/.well-known/oauth-authorization-server" + path};
        String last = "nothing was tried";
        for (int iter = 0 ; iter < locations.length ; iter++) {
            if (iter > 0 && locations[iter].equals(locations[iter - 1])) {
                continue;
            }
            Map metadata;
            try {
                metadata = Json.parseObject(fetcher.fetch(locations[iter]));
            } catch (IOException err) {
                last = err.getMessage();
                continue;
            } catch (RuntimeException err) {
                last = String.valueOf(err.getMessage());
                continue;
            }
            Object named = metadata.get("issuer");
            if (!issuer.equals(named)) {
                throw new IllegalArgumentException("The Issuer \"" + named + "\" provided in the "
                        + "configuration metadata did not match the requested issuer \"" + issuer
                        + "\"");
            }
            return metadata;
        }
        throw new IllegalArgumentException("Unable to resolve the Configuration with the provided "
                + "Issuer of \"" + issuer + "\": " + last);
    }

    /// The public key algorithms the issuer says it signs with that this runtime
    /// verifies; RS256 when it does not say. The key still has to be of the
    /// algorithm's kind, so a list read from the issuer widens nothing a key
    /// does not back.
    private static JwsAlgorithm[] algorithms(Map metadata) {
        List<JwsAlgorithm> found = new ArrayList<JwsAlgorithm>();
        Object listed = metadata.get("id_token_signing_alg_values_supported");
        if (listed instanceof List) {
            for (Object name : (List) listed) {
                SignatureAlgorithm algorithm = name instanceof String
                        ? SignatureAlgorithm.from((String) name) : null;
                if (algorithm != null && !found.contains(algorithm)) {
                    found.add(algorithm);
                }
            }
        }
        if (found.isEmpty()) {
            found.add(SignatureAlgorithm.RS256);
        }
        return found.toArray(new JwsAlgorithm[found.size()]);
    }
}
