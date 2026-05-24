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

/// Base type for every checked error raised by [LlmClient]. Extends
/// [IOException] so callers' existing network catch blocks pick it up.
/// Inspect the subtype (e.g. [LlmRateLimitException]) for actionable
/// detail; `httpStatus`, `providerErrorCode`, and `rawBody` are
/// populated when available.
public class LlmException extends IOException {
    private final int httpStatus;
    private final String providerErrorCode;
    private final String rawBody;

    public LlmException(String message) {
        this(message, -1, null, null, null);
    }

    public LlmException(String message, Throwable cause) {
        this(message, -1, null, null, cause);
    }

    public LlmException(String message, int httpStatus, String providerErrorCode,
                        String rawBody, Throwable cause) {
        super(message);
        if (cause != null) {
            initCause(cause);
        }
        this.httpStatus = httpStatus;
        this.providerErrorCode = providerErrorCode;
        this.rawBody = rawBody;
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
}
