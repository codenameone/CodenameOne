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
package com.codename1.backend.security;

/// The server is doing as much of something expensive as it was told it may,
/// and this request would have been one more: it is turned away at once, rather
/// than queued behind work that is already late.
///
/// Thrown anywhere under a [SecurityFilterChain] it is answered 503 with
/// `Retry-After`. It is deliberately not an [AuthenticationException]: the
/// caller's credentials were never looked at, and a client told they were wrong
/// would stop trying the right ones.
public class ServiceBusyException extends RuntimeException {
    private final int retryAfterSeconds;

    public ServiceBusyException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds < 1 ? 1 : retryAfterSeconds;
    }

    /// What the answer's `Retry-After` says.
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
