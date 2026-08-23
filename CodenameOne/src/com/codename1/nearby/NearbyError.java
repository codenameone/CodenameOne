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
package com.codename1.nearby;

/// Typed error codes carried by every [NearbyException] thrown through the
/// failure path of the `com.codename1.nearby` APIs. Callers branch on these
/// via [NearbyException#getError()] rather than string-matching a message.
public enum NearbyError {
    /// The requested feature is not available on this port, this OS version
    /// or this hardware. The capability queries -- `isSupported()` on each
    /// entry point, plus the finer-grained
    /// [com.codename1.nearby.ranging.RangingCapabilities] -- let
    /// cross-platform code branch before ever seeing this code, and the
    /// inert fallback bridges fail every operation with it.
    NOT_SUPPORTED,

    /// A required runtime permission or OS authorization is missing. On
    /// Android that is `UWB_RANGING`, the Bluetooth runtime grants or
    /// location; on iOS it is the Nearby Interaction or local network
    /// authorization the user declined. See the `requestPermissions` method
    /// on the relevant entry point.
    UNAUTHORIZED,

    /// The radio this feature needs is switched off or otherwise
    /// unavailable right now -- UWB disabled in settings, Bluetooth powered
    /// off, Wi-Fi off. Unlike [#NOT_SUPPORTED] this is recoverable: the
    /// same call may succeed once the user turns the radio on.
    RADIO_UNAVAILABLE,

    /// The peer, accessory or endpoint could not be reached, or moved out of
    /// range before the operation completed.
    PEER_UNAVAILABLE,

    /// A ranging or transport session could not be started -- an invalid
    /// configuration, too many concurrent sessions, or a platform-level
    /// refusal.
    SESSION_FAILED,

    /// A running session was invalidated by the platform and cannot be
    /// resumed. Start a new one.
    SESSION_INVALIDATED,

    /// The supplied token, accessory configuration, endpoint identifier or
    /// device filter could not be decoded or used, or came from a different
    /// platform. Tokens are opaque and are not portable between iOS and
    /// Android.
    ///
    /// A device filter reports this rather than being dropped: an
    /// association request whose filter cannot be installed would otherwise
    /// offer the user every visible device instead of the ones asked for,
    /// and they could associate the wrong accessory from a picker that was
    /// never meant to show it.
    INVALID_TOKEN,

    /// The platform never delivered a completion callback within the safety
    /// timeout, or a discovery/connection attempt timed out.
    TIMEOUT,

    /// A conflicting operation is already in progress -- for example a
    /// second association flow while the system chooser is open.
    BUSY,

    /// The user dismissed a system dialog (the device chooser, the
    /// association prompt, a permission request) or the operation was
    /// cancelled through `AsyncResource.cancel()`.
    USER_CANCELED,

    /// Transport-level I/O failure while moving a payload. Blocking stream
    /// payloads throw plain `java.io.IOException` instead.
    IO_ERROR,

    /// Unclassified failure; the exception message carries the details.
    UNKNOWN
}
