/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.push;

/**
 * Describes a push registration or envelope-processing error.
 *
 * <p>The code is intended for application decisions and logging. The message
 * is diagnostic text and may vary. A retryable error may be attempted again
 * later with backoff; it is not a promise that an immediate retry will work.</p>
 */
public final class PushError {
    private final String code;
    private final String message;
    private final boolean retryable;

    /**
     * Creates an error.
     *
     * @param code stable machine-readable code
     * @param message diagnostic message
     * @param retryable whether retrying later may succeed
     */
    public PushError(String code, String message, boolean retryable) {
        this.code = code;
        this.message = message;
        this.retryable = retryable;
    }

    /**
     * Returns the stable machine-readable error code.
     *
     * @return the error code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the diagnostic error text.
     *
     * @return the diagnostic message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Indicates whether retrying later may succeed.
     *
     * @return {@code true} for a potentially transient failure
     */
    public boolean isRetryable() {
        return retryable;
    }
}
