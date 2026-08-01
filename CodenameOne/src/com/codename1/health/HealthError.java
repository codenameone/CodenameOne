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

/// Typed failure reasons carried by [HealthException]. Ports map their
/// platform error codes onto these so cross-platform code can branch on a
/// stable value instead of parsing messages.
public enum HealthError {

    /// The port, device or OS version has no health support at all, or the
    /// requested capability is unavailable on this platform. Returned by
    /// every operation on the fallback [Health] base class.
    NOT_SUPPORTED,

    /// The requested data type is not available on this platform, even
    /// though health support in general is.
    TYPE_NOT_SUPPORTED,

    /// The Health Connect provider app is not installed. Recoverable by
    /// sending the user to [Health#openProviderSetup()].
    PROVIDER_UNAVAILABLE,

    /// The installed Health Connect provider is too old. Also recoverable
    /// via [Health#openProviderSetup()].
    PROVIDER_UPDATE_REQUIRED,

    /// The operation was refused for lack of authorization.
    ///
    /// Note that a **read** denial does not reliably produce this error:
    /// HealthKit reports a denied read as an empty result, by design. See
    /// [HealthStore#getReadAuthorizationStatus(HealthDataType)].
    UNAUTHORIZED,

    /// The user dismissed a platform authorization or setup flow. Only
    /// reported where the platform distinguishes cancellation from denial.
    USER_CANCELED,

    /// A query or write was rejected before reaching the platform because
    /// the request itself was malformed -- an inverted time range, an
    /// instantaneous write of a cumulative type, a negative limit.
    INVALID_ARGUMENT,

    /// A unit was supplied that measures a different dimension than the
    /// data type requires.
    UNIT_MISMATCH,

    /// A payload could not be decoded -- a truncated or malformed GATT
    /// characteristic value, or a platform record the port could not map.
    /// Never surfaces as an unchecked exception from a parser.
    INVALID_DATA,

    /// The platform's health database could not be opened. On iOS this is
    /// `HKErrorDatabaseInaccessible`, raised while the device is locked --
    /// which is exactly when a background observer fires. **Retryable**:
    /// callers should try again once the device is unlocked rather than
    /// treating it as "no data".
    DATABASE_INACCESSIBLE,

    /// A stored anchor or change token was rejected by the platform,
    /// usually because it aged out. The caller must resynchronize with a
    /// full time-range read; see [HealthChangeBatch#isResyncRequired()].
    ANCHOR_EXPIRED,

    /// The operation would return or write more data than the platform
    /// permits in one call. Use paging or a smaller batch.
    QUOTA_EXCEEDED,

    /// The platform rate-limited the request.
    RATE_LIMITED,

    /// A workout session method was called in a state that does not allow
    /// it -- pausing a session that never started, ending one twice.
    SESSION_STATE,

    /// A live BLE sensor dropped its connection mid-session.
    SENSOR_DISCONNECTED,

    /// The operation did not complete within its safety timeout.
    TIMEOUT,

    /// Anything the port could not classify. The message carries the
    /// platform's own text.
    UNKNOWN
}
