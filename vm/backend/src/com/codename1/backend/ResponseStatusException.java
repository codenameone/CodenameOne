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
package com.codename1.backend;

/// Thrown by a handler to answer a request with a status of its choosing.
///
/// A handler that returns a value has no other way to say "409, that e-mail is
/// taken": its return type is the body of a success. That is every method of a
/// server interface generated from a shared `@RestClient` contract, and any
/// controller method that returns its own type and not a `Response`.
///
/// ```java
/// if (taken(email)) {
///     throw new ResponseStatusException(409, "That e-mail already has an account");
/// }
/// ```
///
/// The reason is the body, as plain text, so it is what the caller reads: keep
/// it free of anything the caller should not learn. A status below 500 is an
/// answer and is neither logged nor recorded as a failed request; 500 and above
/// is reported as any other failure of the handler is, and answered with the
/// status given.
///
/// Unchecked, as Spring's is, and for the same reason: it has to pass through
/// methods whose signatures were written without it. Thrown inside a
/// `@Transactional` method it rolls the transaction back, as every unchecked
/// exception does.
public class ResponseStatusException extends RuntimeException {
    private final int status;
    private final String reason;

    /// @param status the status to answer with, 400 to 599
    /// @param reason what to tell the caller; sent as the body
    public ResponseStatusException(int status, String reason) {
        this(status, reason, null);
    }

    /// @param status the status to answer with, 400 to 599
    /// @param reason what to tell the caller; sent as the body
    /// @param cause what led to it, for the log; never sent
    public ResponseStatusException(int status, String reason, Throwable cause) {
        super(status + " " + reason, cause);
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("not an error status: " + status);
        }
        this.status = status;
        this.reason = reason == null ? "" : reason;
    }

    /// The status to answer with.
    public int getStatus() {
        return status;
    }

    /// What the caller is told.
    public String getReason() {
        return reason;
    }
}
