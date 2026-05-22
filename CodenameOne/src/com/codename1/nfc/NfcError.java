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
package com.codename1.nfc;

/// Typed error codes returned by [Nfc] and [HostCardEmulationService] when an
/// asynchronous NFC operation fails. Callers branch on these via
/// [NfcException#getError()] instead of string-matching error messages.
public enum NfcError {
    /// The current platform/port does not expose an NFC API at all (desktop
    /// deploy, JavaScript, ports without `getNfc()` overridden). The
    /// fallback [Nfc] base class always fails read/write requests with this
    /// code.
    NOT_AVAILABLE,

    /// Device has NFC hardware but the user has disabled it in system
    /// settings. On Android this corresponds to `NfcAdapter.isEnabled()`
    /// returning `false`; iOS does not expose a runtime toggle so this code
    /// is rarely returned there.
    DISABLED,

    /// The user did not grant the NFC entitlement / permission, or the build
    /// is missing the `NFCReaderUsageDescription` plist entry on iOS / the
    /// `android.permission.NFC` manifest entry on Android.
    NOT_AUTHORIZED,

    /// Tag was removed from the field before the requested operation could
    /// complete. The caller should re-arm the reader and re-prompt the user.
    TAG_LOST,

    /// The NDEF payload is malformed or the tag returned data that does not
    /// parse as a valid NDEF message.
    INVALID_NDEF,

    /// Tag is read-only (already locked, or a vendor tag with no writable
    /// area) and the requested write was rejected.
    READ_ONLY,

    /// The message did not fit in the tag's writable capacity. See
    /// [Tag#getMaxNdefSize()] before constructing large messages.
    CAPACITY_EXCEEDED,

    /// I/O failure during transceive / NDEF read / NDEF write -- typically a
    /// transient field loss that may succeed on retry.
    IO_ERROR,

    /// Tag was discovered but reports a technology that the requested
    /// operation does not support (e.g. asking for [IsoDep] on a NDEF-only
    /// tag, or asking for FeliCa block reads on an NFC-A tag).
    UNSUPPORTED_TAG,

    /// User dismissed the iOS Core NFC system sheet, or the Android
    /// foreground-dispatch overlay was cancelled.
    USER_CANCELED,

    /// The OS / NFC controller cancelled the session -- app backgrounded,
    /// another reader took the radio, or the session timed out without a tag.
    SYSTEM_CANCELED,

    /// HCE-specific: the requested AID is not the one registered for this
    /// service, or the terminal addressed an unknown AID.
    UNKNOWN_AID,

    /// Anything not covered by the more specific codes.
    UNKNOWN
}
