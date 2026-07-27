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
package com.codename1.health;

/// The failure delivered through an `AsyncResource` when a health
/// operation does not succeed. Branch on [#getError()] rather than on the
/// message.
///
/// ```java
/// store.write(sample).onResult((res, err) -> {
///     if (err instanceof HealthException) {
///         HealthError e = ((HealthException) err).getError();
///         if (e == HealthError.UNAUTHORIZED) {
///             Health.getInstance().openHealthSettings();
///         }
///     }
/// });
/// ```
public class HealthException extends Exception {

    private final HealthError error;

    /// Creates an exception carrying a typed reason.
    public HealthException(HealthError error, String message) {
        super(message);
        this.error = error == null ? HealthError.UNKNOWN : error;
    }

    /// Creates an exception carrying a typed reason and an underlying
    /// cause.
    public HealthException(HealthError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error == null ? HealthError.UNKNOWN : error;
    }

    /// The typed reason this operation failed.
    /// Transient: an exception is Serializable, the write result is not,
    /// and a partial result has no meaning once it has crossed a process
    /// boundary anyway -- the ids it names are scoped to the store that
    /// issued them.
    private transient HealthWriteResult partialResult;

    /// Samples already committed before the failure, when a chunked write
    /// fails partway.
    ///
    /// A write larger than [HealthStore#getMaxWriteBatchSize()] is sent in
    /// chunks, and an earlier chunk can be stored before a later one
    /// fails. Without this the caller sees only the failure, retries the
    /// whole batch, and writes the committed samples a second time --
    /// duplicate records in the user's health store. Null when the failure
    /// was not a partial write.
    public HealthWriteResult getPartialResult() {
        return partialResult;
    }

    void setPartialResult(HealthWriteResult partialResult) {
        this.partialResult = partialResult;
    }

    public HealthError getError() {
        return error;
    }
}
