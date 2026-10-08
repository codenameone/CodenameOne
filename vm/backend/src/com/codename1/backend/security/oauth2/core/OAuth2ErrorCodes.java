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
package com.codename1.backend.security.oauth2.core;

/// The error codes this layer answers with: those of RFC 6749, RFC 6750, RFC
/// 7009 and RFC 8628, and the ones a sign-in through another provider fails with.
public final class OAuth2ErrorCodes {
    /// The request is missing a parameter, repeats one, or is otherwise
    /// malformed.
    public static final String INVALID_REQUEST = "invalid_request";
    /// The access token is expired, revoked, malformed or otherwise not valid.
    public static final String INVALID_TOKEN = "invalid_token";
    /// The request needs more than the access token grants.
    public static final String INSUFFICIENT_SCOPE = "insufficient_scope";
    /// The server met something that stopped it answering.
    public static final String SERVER_ERROR = "server_error";
    /// The client is unknown, or did not authenticate as itself.
    public static final String INVALID_CLIENT = "invalid_client";
    /// The grant -- an authorization code, a refresh token, a device code -- is
    /// not valid, has expired, was used already or was issued to another client.
    public static final String INVALID_GRANT = "invalid_grant";
    /// The client may not use this grant.
    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
    /// The server does not issue tokens for this grant.
    public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
    /// The server does not answer this response type.
    public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
    /// A scope asked for is unknown, malformed or more than the client may have.
    public static final String INVALID_SCOPE = "invalid_scope";
    /// The user, or the server, refused the request.
    public static final String ACCESS_DENIED = "access_denied";
    /// The server cannot answer for now.
    public static final String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";
    /// The token type is one the server does not revoke.
    public static final String UNSUPPORTED_TOKEN_TYPE = "unsupported_token_type";
    /// RFC 8628: the user has not answered yet; ask again after the interval.
    public static final String AUTHORIZATION_PENDING = "authorization_pending";
    /// RFC 8628: the client asks too often; the interval is now five seconds longer.
    public static final String SLOW_DOWN = "slow_down";
    /// RFC 8628: the device code ran out before the user answered.
    public static final String EXPIRED_TOKEN = "expired_token";
    /// OpenID Connect: the user is not signed in and the client cannot show a page.
    public static final String LOGIN_REQUIRED = "login_required";
    /// The redirect address is not one the client registered.
    public static final String INVALID_REDIRECT_URI = "invalid_redirect_uri";
    /// The `state` that came back is not the one that was sent.
    public static final String INVALID_STATE_PARAMETER = "invalid_state_parameter";
    /// No request for authorization was waiting for this answer.
    public static final String AUTHORIZATION_REQUEST_NOT_FOUND = "authorization_request_not_found";
    /// The identity provider's answer could not be used.
    public static final String INVALID_TOKEN_RESPONSE = "invalid_token_response";
    /// The ID token did not verify.
    public static final String INVALID_ID_TOKEN = "invalid_id_token";
    /// The `nonce` of the ID token is not the one that was sent.
    public static final String INVALID_NONCE = "invalid_nonce";
    /// The answer a browser came back with names another issuer than the
    /// provider it was sent to, or none where the provider always names one.
    public static final String INVALID_ISSUER = "invalid_issuer";
    /// The user's attributes could not be read from the identity provider.
    public static final String INVALID_USER_INFO_RESPONSE = "invalid_user_info_response";

    /// A `resource` asked for is not an absolute address, or not one the
    /// client may have tokens for (RFC 8707).
    public static final String INVALID_TARGET = "invalid_target";

    private OAuth2ErrorCodes() {
    }
}
