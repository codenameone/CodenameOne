/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ai;

import java.io.IOException;

/// The single checked-error type raised by [LlmClient]. Extends
/// [IOException] so callers' existing network catch blocks pick it
/// up. Inspect [#getType()] for the failure category and switch on
/// the [ErrorType] enum -- no separate exception subclass per error.
///
/// ```
/// try {
///     ChatResponse r = client.chat(req).get();
///     // ...
/// } catch (AsyncExecutionException ae) {
///     if (ae.getCause() instanceof LlmException) {
///         LlmException e = (LlmException) ae.getCause();
///         switch (e.getType()) {
///             case RATE_LIMIT:        scheduleRetry(e.getRetryAfterSeconds()); break;
///             case AUTH:              showLoginScreen(); break;
///             case CONTEXT_LENGTH:    trimHistory();    break;
///             case MODEL_OVERLOADED:  scheduleRetry(60); break;
///             case INVALID_REQUEST:   showError(e);     break;
///             case NETWORK:           showOfflineUi();  break;
///             default:                showError(e);
///         }
///     }
/// }
/// ```
///
/// `httpStatus`, `providerErrorCode`, `rawBody`, and (for rate-limit
/// errors) `retryAfterSeconds` are populated when the provider
/// returned them. `getRetryAfterSeconds()` returns `-1` for every
/// non-rate-limit error and for rate-limit errors where the provider
/// didn't send a `Retry-After` header.
public class LlmException extends IOException {

    /// Coarse-grained classification of every failure path the
    /// client can surface. Stable -- new variants are appended at
    /// the end; existing variants do not change semantics.
    public enum ErrorType {
        /// 401 / 403 -- API key invalid, revoked, or lacks access to
        /// the requested model.
        AUTH,

        /// 429 -- rate limit hit. Pair with [#getRetryAfterSeconds]
        /// to honour the provider's backoff hint when available.
        RATE_LIMIT,

        /// 400 / 422 -- malformed request, unsupported parameter,
        /// image too large, etc.
        INVALID_REQUEST,

        /// 400 subtype -- the conversation exceeded the model's
        /// context window. Drop older turns and retry, or switch
        /// to a longer-context model.
        CONTEXT_LENGTH,

        /// 503 / 529 -- the model is temporarily overloaded. Same
        /// recovery as RATE_LIMIT but a different signal source.
        MODEL_OVERLOADED,

        /// 5xx other than 503 / 529 -- provider had an internal error.
        SERVER,

        /// DNS, TLS, read-timeout, connection reset -- the request
        /// did not reach the provider or did not get a response back.
        NETWORK,

        /// Any failure that doesn't fit the categories above (e.g.
        /// JSON parsing of the response failed). Treat as a generic
        /// programming error and log [#getRawBody].
        UNKNOWN
    }

    private final int httpStatus;
    private final String providerErrorCode;
    private final String rawBody;
    private final ErrorType type;
    private final int retryAfterSeconds;

    public LlmException(String message) {
        this(message, -1, null, null, null, ErrorType.UNKNOWN, -1);
    }

    public LlmException(String message, Throwable cause) {
        this(message, -1, null, null, cause, ErrorType.UNKNOWN, -1);
    }

    public LlmException(String message, int httpStatus, String providerErrorCode,
                        String rawBody, Throwable cause, ErrorType type) {
        this(message, httpStatus, providerErrorCode, rawBody, cause, type, -1);
    }

    public LlmException(String message, int httpStatus, String providerErrorCode,
                        String rawBody, Throwable cause, ErrorType type,
                        int retryAfterSeconds) {
        super(message);
        if (cause != null) {
            initCause(cause);
        }
        this.httpStatus = httpStatus;
        this.providerErrorCode = providerErrorCode;
        this.rawBody = rawBody;
        this.type = type == null ? ErrorType.UNKNOWN : type;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /// Coarse-grained category -- the recommended switching point
    /// for error handling. See the class javadoc for the full
    /// pattern.
    public ErrorType getType() {
        return type;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getProviderErrorCode() {
        return providerErrorCode;
    }

    public String getRawBody() {
        return rawBody;
    }

    /// Seconds the provider asked us to wait before retrying, parsed
    /// from a `Retry-After` header. `-1` when not applicable (the
    /// usual case for non-`RATE_LIMIT` errors) or when the provider
    /// did not send the header.
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
