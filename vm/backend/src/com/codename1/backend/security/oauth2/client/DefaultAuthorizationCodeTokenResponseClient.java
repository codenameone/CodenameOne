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

import com.codename1.backend.Base64;
import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException;
import com.codename1.backend.security.oauth2.core.OAuth2Error;
import com.codename1.backend.security.oauth2.core.OAuth2ErrorCodes;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The exchange over the runtime's HTTP client, which parks the request's
/// thread while the provider answers.
///
/// The client authenticates the way its registration says: the id and secret in
/// an `Authorization: Basic` header, as form fields, or -- a public client --
/// the id alone. The PKCE verifier goes with every exchange.
public final class DefaultAuthorizationCodeTokenResponseClient
        implements OAuth2AccessTokenResponseClient {
    @Override
    public OAuth2AccessTokenResponse getTokenResponse(ClientRegistration registration,
            OAuth2AuthorizationRequest authorizationRequest, String code) {
        Map<String, Object> form = new LinkedHashMap<String, Object>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", authorizationRequest.getRedirectUri());
        form.put("code_verifier", authorizationRequest.getCodeVerifier());
        List<String> headers = new ArrayList<String>();
        headers.add("Content-Type: application/x-www-form-urlencoded");
        // GitHub answers with a form unless it is asked for JSON.
        headers.add("Accept: application/json");
        Web.Result result;
        try {
            ClientAuthenticationMethod method = registration.getClientAuthenticationMethod();
            String secret = registration.resolveClientSecret();
            if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(method) && secret != null) {
                headers.add("Authorization: Basic " + Base64.encode(OAuth2Parameters.utf8(
                        OAuth2Parameters.encode(registration.getClientId()) + ":"
                                + OAuth2Parameters.encode(secret))));
            } else {
                form.put("client_id", registration.getClientId());
                if (ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(method)) {
                    form.put("client_secret", secret);
                }
            }
            result = Web.request("POST", registration.getProviderDetails().getTokenUri(), headers,
                    OAuth2Parameters.utf8(OAuth2Parameters.format(form)));
        } catch (IOException err) {
            // What went wrong is the server's to know, not the browser's: the
            // message goes no further than this exception.
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_TOKEN_RESPONSE), "The token endpoint of "
                    + registration.getRegistrationId() + " could not be reached", err);
        }
        Map fields = fields(result);
        if (fields == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_TOKEN_RESPONSE), "The token endpoint of "
                    + registration.getRegistrationId() + " answered " + result.getStatus()
                    + " with something that is not a token response");
        }
        Object error = fields.get("error");
        if (error != null || !result.isSuccess()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(errorCode(error)),
                    "The token endpoint of " + registration.getRegistrationId() + " refused the "
                    + "exchange");
        }
        OAuth2AccessTokenResponse response = new OAuth2AccessTokenResponse(fields);
        if (response.getAccessToken() == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_TOKEN_RESPONSE), "The token endpoint of "
                    + registration.getRegistrationId() + " answered without an access token");
        }
        return response;
    }

    private static Map fields(Web.Result result) {
        String body = result.getBodyAsString();
        if (body == null) {
            return null;
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("{")) {
            try {
                return Json.parseObject(trimmed);
            } catch (IOException malformed) {
                return null;
            } catch (RuntimeException malformed) {
                return null;
            }
        }
        Map<String, String> form = OAuth2Parameters.parse(trimmed);
        return form.isEmpty() ? null : form;
    }

    /// An error code as RFC 6749 spells one, and nothing else of what the
    /// provider sent: a code is a word of letters, digits and underscores.
    static String errorCode(Object error) {
        if (!(error instanceof String)) {
            return OAuth2ErrorCodes.INVALID_TOKEN_RESPONSE;
        }
        String code = (String) error;
        if (code.length() == 0 || code.length() > 64) {
            return OAuth2ErrorCodes.INVALID_REQUEST;
        }
        for (int iter = 0 ; iter < code.length() ; iter++) {
            char c = code.charAt(iter);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')) {
                return OAuth2ErrorCodes.INVALID_REQUEST;
            }
        }
        return code;
    }

    /// The `error` parameter of a callback, reduced to an error code as RFC
    /// 6749 spells one: anything else a provider sent comes back as
    /// `invalid_request`.
    public static String sanitizeErrorCode(String error) {
        return errorCode(error);
    }
}
