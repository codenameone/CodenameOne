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
package com.codename1.impl.android;

/// The seam between the Android port and Health Connect.
///
/// #### Why the port does not call Health Connect directly
///
/// `androidx.health.connect` is an AndroidX library whose API surface is
/// Kotlin `suspend` functions, and the Codename One Android port compiles
/// against a fixed, old `android.jar` with no AndroidX, no Kotlin and no
/// coroutines on its classpath. So the port cannot reference it.
///
/// It does not need to. The port ships to app builds as **source**, which
/// the app's own Gradle compiles at a modern `compileSdkVersion` -- and
/// Kotlin sources in that tree are compiled too. So the build server drops
/// a Kotlin implementation of this interface into the app and registers it
/// through [AndroidHealthSupport], exactly as the Android Auto glue is
/// injected for `com.codename1.car`.
///
/// Every method on this boundary therefore speaks only primitives and
/// `String`: no AndroidX type, no Kotlin type and no coroutine escapes into
/// the port. Payloads use the same tab-separated line format the iOS bridge
/// uses, because a year of heart rate is hundreds of thousands of samples
/// and parsing that as JSON is real memory pressure on a phone.
///
/// When no bridge is registered -- an app that never referenced
/// `com.codename1.health`, or a build predating the generator --
/// [AndroidHealthSupport#getDelegate()] returns null and the health API
/// degrades to reporting itself unsupported, exactly as
/// `com.codename1.car` does without Android Auto.
public interface HealthConnectDelegate {

    /// Health Connect is not usable on this device.
    int SDK_UNAVAILABLE = 0;
    /// The provider is installed but too old.
    int SDK_UPDATE_REQUIRED = 1;
    /// The provider is present and usable.
    int SDK_AVAILABLE = 2;

    /// Receives the result of an asynchronous bridge call. The bridge
    /// invokes exactly one of these methods, on an unspecified thread; the
    /// port marshals to the EDT.
    interface Callback {
        /// The call succeeded, carrying whatever payload it produces.
        void onSuccess(String payload);

        /// The call failed. `code` is one of the `ERR_` constants.
        void onError(int code, String message);
    }

    /// The bridge could not classify the failure.
    int ERR_UNKNOWN = 0;
    /// A permission was missing or refused.
    int ERR_AUTH_DENIED = 1;
    /// The provider app was unreachable.
    int ERR_PROVIDER = 2;
    /// The request was rejected as malformed.
    int ERR_INVALID_ARGUMENT = 3;
    /// A change token had expired and the caller must resynchronize.
    int ERR_TOKEN_EXPIRED = 4;

    /// Whether Health Connect is available, as one of the `SDK_` constants.
    int sdkStatus();

    /// The provider package name, for sending the user to install it.
    String providerPackageName();

    /// Reports the permissions currently granted, as a comma-separated
    /// list of portable data-type tokens prefixed `r:` or `w:`.
    void grantedPermissions(Callback cb);

    /// Builds the intent that launches Health Connect's own permission UI.
    ///
    /// Health permissions are not ordinary runtime permissions and must not
    /// go through `ActivityCompat.requestPermissions`.
    android.content.Intent permissionIntent(String permissionsCsv);

    /// Interprets the result of the permission intent, returning the
    /// granted set in the same format as [#grantedPermissions(Callback)].
    String parsePermissionResult(int resultCode,
            android.content.Intent data);

    /// Reads samples. `requestJson` describes types, range, limit and sort;
    /// the payload is tab-separated sample lines.
    void readRecords(String requestJson, Callback cb);

    /// Computes aggregates over the supplied bucket boundaries.
    void aggregate(String requestJson, Callback cb);

    /// Writes samples supplied as tab-separated lines; the payload is the
    /// assigned record ids.
    void insertRecords(String recordsTsv, Callback cb);

    /// Deletes records matching the request; the payload is the count.
    void deleteRecords(String requestJson, Callback cb);

    /// Obtains a changes token for the given types.
    void getChangesToken(String typesCsv, Callback cb);

    /// Drains changes since a token. Fails with [#ERR_TOKEN_EXPIRED] when
    /// the token has aged out, which the port maps to a resync request.
    void getChanges(String token, Callback cb);
}
