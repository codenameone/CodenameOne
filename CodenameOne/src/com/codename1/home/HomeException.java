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
package com.codename1.home;

/// The failure delivered through an `AsyncResource` when a smart-home
/// operation does not succeed. Branch on [#getError()] rather than on the
/// message.
///
/// ```java
/// SmartHome.getInstance().write(write).onResult((res, err) -> {
///     if (err instanceof HomeException) {
///         HomeError e = ((HomeException) err).getError();
///         if (e == HomeError.PIN_REQUIRED) {
///             promptForLockPin();
///         }
///     }
/// });
/// ```
public class HomeException extends Exception {

    private final HomeError error;
    private final String accessoryId;

    /// Creates an exception carrying a typed reason.
    ///
    /// #### Parameters
    ///
    /// - `error`: the typed reason; `null` becomes [HomeError#UNKNOWN]
    ///
    /// - `message`: the platform's own text, or an explanation of a request
    ///   this API rejected before it reached the platform
    public HomeException(HomeError error, String message) {
        this(error, message, null, null);
    }

    /// Creates an exception carrying a typed reason and the accessory it
    /// applies to.
    ///
    /// #### Parameters
    ///
    /// - `error`: the typed reason; `null` becomes [HomeError#UNKNOWN]
    ///
    /// - `message`: the platform's own text
    ///
    /// - `accessoryId`: the accessory the failure is about, or `null` when it
    ///   is not about one in particular
    public HomeException(HomeError error, String message, String accessoryId) {
        this(error, message, accessoryId, null);
    }

    /// Creates an exception carrying a typed reason, the accessory it applies
    /// to and an underlying cause.
    ///
    /// #### Parameters
    ///
    /// - `error`: the typed reason; `null` becomes [HomeError#UNKNOWN]
    ///
    /// - `message`: the platform's own text
    ///
    /// - `accessoryId`: the accessory the failure is about, or `null`
    ///
    /// - `cause`: the underlying failure, or `null`
    public HomeException(HomeError error, String message, String accessoryId,
            Throwable cause) {
        super(message, cause);
        this.error = error == null ? HomeError.UNKNOWN : error;
        this.accessoryId = accessoryId;
    }

    /// The typed reason this operation failed.
    ///
    /// #### Returns
    ///
    /// the reason, never `null`
    public HomeError getError() {
        return error;
    }

    /// The accessory this failure is about, when it is about one.
    ///
    /// A batch read or write that fails as a whole has no single accessory
    /// and answers `null` here; per-item outcomes inside a batch that
    /// partially succeeded are reported through [TraitWriteResult] and
    /// [TraitReading] instead, not as exceptions.
    ///
    /// #### Returns
    ///
    /// the accessory id, or `null`
    public String getAccessoryId() {
        return accessoryId;
    }
}
