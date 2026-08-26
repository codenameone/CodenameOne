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
package com.codename1.call;

/// Typed error codes carried by every [CallException] thrown through the
/// failure path of the `com.codename1.call` APIs. Callers branch on these
/// via [CallException#getError()] rather than string-matching a message.
///
/// The set is deliberately shared between iOS and Android even though the
/// two platforms refuse a call for different reasons and at different
/// layers. CallKit answers a refusal through the `NSError` handed to
/// `reportNewIncomingCall`'s completion block; Telecom answers it by calling
/// `onCreateIncomingConnectionFailed`. Both funnel here, because an app that
/// has to ask which platform it is on to find out why a call did not ring
/// would end up with two code paths and would test only one of them.
public enum CallError {
    /// System call integration is not available on this port, this OS
    /// version or this device. Android self-managed calls need API 26;
    /// CallKit is absent from tvOS and watchOS. The capability queries --
    /// `Calls.isSupported()` and [com.codename1.call.session.Calls#getCapabilities()]
    /// -- let cross-platform code branch before ever seeing this code, and
    /// the inert fallback bridge fails every operation with it.
    NOT_SUPPORTED,

    /// A required runtime permission or role is missing: `MANAGE_OWN_CALLS`
    /// on Android, the microphone grant on either platform, or the call
    /// screening role the user declined.
    UNAUTHORIZED,

    /// The system refused to place or ring this call right now, and may
    /// accept the same call later. An emergency call is in progress, another
    /// application holds a self-managed call, or the user has switched this
    /// app's calling off. This is the `onCreateIncomingConnectionFailed`
    /// case, and it is **not** an error in the app.
    CALL_REFUSED,

    /// The system suppressed the call before it ever rang: Do Not Disturb on
    /// iOS, or a number the user has blocked. The call was not shown and no
    /// action will arrive for it.
    CALL_FILTERED,

    /// A call with this identifier is already known to the system. Reporting
    /// the same identifier twice is a hard error on iOS rather than a
    /// no-op, so the ports collapse a duplicate report into an update and
    /// only surface this when the collision cannot be reconciled.
    DUPLICATE_CALL,

    /// The supplied call identifier is not a canonical identifier, or names
    /// a call the system no longer has. Acting on a call that has already
    /// ended reports this rather than failing silently.
    INVALID_ID,

    /// The system asked the app to act on a call and the app neither
    /// fulfilled nor failed the request in time, so the platform timed it
    /// out. See [com.codename1.call.session.CallAction].
    ACTION_TIMEOUT,

    /// The audio session could not be configured or was taken away, so the
    /// call has no audio even though its state says otherwise.
    AUDIO_FAILED,

    /// VoIP push registration failed, or the platform revoked it. On iOS
    /// this is permanent for the installed app once the deadline has been
    /// missed often enough.
    PUSH_UNAVAILABLE,

    /// The caller-identification or blocking data could not be installed.
    /// The extension rejected the file, the entries were out of order, or
    /// the store exceeded the platform limit.
    DIRECTORY_FAILED,

    /// The platform never delivered a completion callback within the safety
    /// timeout.
    TIMEOUT,

    /// A conflicting operation is already in progress.
    BUSY,

    /// Nothing more specific is known. Ports use this only when the platform
    /// itself gave no usable reason.
    UNKNOWN,

    /// The system's call provider was reset while the operation was in
    /// flight, so it can never be answered. See
    /// [com.codename1.call.session.CallActionListener#providerReset()].
    ///
    /// Last deliberately, and **never sent by a port**: it is raised by the
    /// facade itself. Every other value crosses the SPI as an ordinal, so a
    /// new one has to be appended or every port's error mapping shifts by
    /// one.
    PROVIDER_RESET
}
