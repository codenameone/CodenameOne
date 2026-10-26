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
package com.codename1.backend.security.oauth2.client.credentials;

import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.oauth2.client.DefaultAuthorizationCodeTokenResponseClient;
import com.codename1.backend.security.oauth2.client.OAuth2AccessTokenResponse;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A token endpoint: where a grant posts its form, and how the answer is read.
///
/// The transport is replaceable so a grant can be exercised without a socket --
/// the tests and the native self-check post to an in-process fake and assert on
/// the exact form that was sent.
public final class TokenEndpoint {
    /// Lifetime assumed for a token whose response gives no `expires_in`. RFC 6749
    /// leaves the default to the server; assuming a short one costs an extra
    /// exchange now and then, assuming a long one presents a dead token.
    public static final long DEFAULT_LIFETIME_SECONDS = 300;

    /// Sends one form-encoded POST.
    public interface Transport {
        /// @param uri the token endpoint
        /// @param headers `Name: value` lines
        /// @param form the `application/x-www-form-urlencoded` body
        /// @return what the endpoint answered
        /// @throws IOException when it could not be reached
        Response post(String uri, List<String> headers, String form) throws IOException;
    }

    /// A token endpoint's answer.
    public static final class Response {
        private final int status;
        private final String body;

        public Response(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public int getStatus() {
            return status;
        }

        public String getBody() {
            return body;
        }
    }

    /// The transport every grant uses unless it is given another: [Web].
    public static final Transport WEB = new Transport() {
        @Override
        public Response post(String uri, List<String> headers, String form) throws IOException {
            Web.Result result = Web.request("POST", uri, headers, OAuth2Parameters.utf8(form));
            return new Response(result.getStatus(), result.getBodyAsString());
        }
    };

    private final String uri;
    private final Transport transport;
    private final Clock clock;

    public TokenEndpoint(String uri, Transport transport, Clock clock) {
        if (uri == null || uri.length() == 0) {
            throw new IllegalArgumentException("A token endpoint URI is required");
        }
        this.uri = uri;
        this.transport = transport == null ? WEB : transport;
        this.clock = clock == null ? Clock.SYSTEM : clock;
    }

    public String getUri() {
        return uri;
    }

    Clock getClock() {
        return clock;
    }

    /// Posts `form` and reads the token out of the answer.
    ///
    /// @param extraHeaders more `Name: value` lines, such as `Authorization`
    /// @throws TokenAcquisitionException when no token came back; its message
    /// carries no part of either body
    AccessToken exchange(String form, List<String> extraHeaders) {
        List<String> headers = new ArrayList<String>();
        headers.add("Content-Type: application/x-www-form-urlencoded");
        headers.add("Accept: application/json");
        if (extraHeaders != null) {
            headers.addAll(extraHeaders);
        }
        String where = describe(uri);
        Response response;
        try {
            response = transport.post(uri, headers, form);
        } catch (IOException err) {
            // The cause stays attached for whoever debugs the network; its
            // message is the transport's, which never sees the form.
            throw new TokenAcquisitionException("The token endpoint " + where
                    + " could not be reached", -1, null, true, err);
        }
        int status = response.getStatus();
        Map fields = parse(response.getBody());
        Object error = fields == null ? null : fields.get("error");
        if (status < 200 || status > 299 || error != null) {
            String code = error == null ? null
                    : DefaultAuthorizationCodeTokenResponseClient.sanitizeErrorCode(
                            String.valueOf(error));
            boolean retryable = status >= 500 || status == 429 || status < 0;
            throw new TokenAcquisitionException("The token endpoint " + where + " answered "
                    + status + (code == null ? "" : " (" + code + ")"), status, code,
                    retryable, null);
        }
        if (fields == null) {
            throw new TokenAcquisitionException("The token endpoint " + where + " answered "
                    + status + " with something that is not a token response", status, null,
                    false, null);
        }
        OAuth2AccessTokenResponse token = new OAuth2AccessTokenResponse(fields);
        if (token.getAccessToken() == null) {
            throw new TokenAcquisitionException("The token endpoint " + where
                    + " answered without an access token", status, null, false, null);
        }
        long lifetime = token.getExpiresIn() > 0 ? token.getExpiresIn()
                : DEFAULT_LIFETIME_SECONDS;
        return new AccessToken(token.getAccessToken(),
                token.getTokenType() == null ? "Bearer" : token.getTokenType(),
                clock.currentTimeMillis() + lifetime * 1000L, token.getScopes());
    }

    private static Map parse(String body) {
        if (body == null) {
            return null;
        }
        String trimmed = body.trim();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        try {
            return Json.parseObject(trimmed);
        } catch (IOException malformed) {
            return null;
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    /// The endpoint as it may be printed: scheme, host and path, without any
    /// user info or query, either of which can hold a credential.
    static String describe(String uri) {
        String text = uri;
        int query = text.indexOf('?');
        if (query >= 0) {
            text = text.substring(0, query);
        }
        int fragment = text.indexOf('#');
        if (fragment >= 0) {
            text = text.substring(0, fragment);
        }
        int scheme = text.indexOf("://");
        int hostStart = scheme < 0 ? 0 : scheme + 3;
        int at = text.indexOf('@', hostStart);
        int slash = text.indexOf('/', hostStart);
        if (at >= 0 && (slash < 0 || at < slash)) {
            text = text.substring(0, hostStart) + text.substring(at + 1);
        }
        return text;
    }
}
