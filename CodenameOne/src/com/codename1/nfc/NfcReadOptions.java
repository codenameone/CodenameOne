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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// Configures a single call to [Nfc#readTag(NfcReadOptions)] or
/// [Nfc#addTagListener(NfcListener)]. Setters return `this` for fluent
/// chaining; every property has a useful default.
///
/// Not every option is honoured on every platform -- iOS displays the
/// system NFC sheet whose copy comes from [#setAlertMessage(String)] but
/// ignores [#setTechFilter(TagType...)] since Core NFC chooses session type
/// from the entitlement. Unrecognised settings are silently ignored, so
/// callers can set the union without platform `if` statements.
public final class NfcReadOptions {

    private String alertMessage = "Hold your iPhone near the NFC tag";
    private String invalidatedMessage;
    private List<TagType> techFilter = Collections.emptyList();
    private boolean ndefOnly;
    private long timeoutMs;
    private List<String> felicaSystemCodes = Collections.emptyList();
    private List<byte[]> isoSelectAids = Collections.emptyList();

    /// The current message shown on iOS Core NFC's system sheet. Defaults
    /// to `"Hold your iPhone near the NFC tag"`. Ignored on Android (no
    /// system sheet) and on the fallback base class.
    public String getAlertMessage() {
        return alertMessage;
    }

    /// Sets the message shown on iOS Core NFC's modal sheet while the
    /// session is active. Translate this string for your locale before
    /// calling [Nfc#readTag(NfcReadOptions)]. Ignored on Android.
    public NfcReadOptions setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
        return this;
    }

    /// Message shown on the iOS sheet after the session is invalidated
    /// because of an error, or `null` to leave it unset. Ignored on
    /// Android.
    public String getInvalidatedMessage() {
        return invalidatedMessage;
    }

    /// Sets the message shown on the iOS sheet after the session ends in
    /// failure. Ignored on Android.
    public NfcReadOptions setInvalidatedMessage(String invalidatedMessage) {
        this.invalidatedMessage = invalidatedMessage;
        return this;
    }

    /// The currently configured technology filter. Empty list means "any
    /// technology", which is the default.
    public List<TagType> getTechFilter() {
        return techFilter;
    }

    /// Restricts the reader session to the listed technologies. On Android
    /// the foreground-dispatch intent filter is computed from this list.
    /// iOS picks the underlying session type (`NFCNDEFReaderSession` for
    /// [TagType#NDEF] only, otherwise `NFCTagReaderSession`).
    public NfcReadOptions setTechFilter(TagType... types) {
        if (types == null || types.length == 0) {
            techFilter = Collections.emptyList();
            return this;
        }
        techFilter = Collections.unmodifiableList(
                new ArrayList<TagType>(Arrays.asList(types)));
        return this;
    }

    /// `true` when the session is restricted to tags that already carry an
    /// NDEF payload. Shortcut for setting [#setTechFilter(TagType...)] to
    /// `[NDEF]`. iOS uses this to pick `NFCNDEFReaderSession` instead of
    /// `NFCTagReaderSession`.
    public boolean isNdefOnly() {
        return ndefOnly;
    }

    /// Restricts the session to NDEF-formatted tags. This is the fastest /
    /// most permissive iOS Core NFC mode and the only one that does not
    /// require the `NFCReaderSession` entitlement on iOS 13.
    public NfcReadOptions setNdefOnly(boolean ndefOnly) {
        this.ndefOnly = ndefOnly;
        if (ndefOnly && techFilter.isEmpty()) {
            techFilter = Collections.unmodifiableList(
                    new ArrayList<TagType>(Arrays.asList(TagType.NDEF)));
        }
        return this;
    }

    /// Session timeout in milliseconds. `0` means "no timeout" (the
    /// default); the session ends only when the user dismisses it or a tag
    /// is read. On iOS Core NFC the session is hard-capped at 60 seconds
    /// regardless of this value.
    public long getTimeoutMs() {
        return timeoutMs;
    }

    /// Stops the session automatically after the given duration. Honoured
    /// on Android and on the JavaSE simulator; iOS Core NFC always uses
    /// its own 60-second hard limit.
    public NfcReadOptions setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    /// FeliCa system codes to scan for (e.g. `["0003", "8008"]`). Honoured
    /// only on iOS where the codes must also appear in the app's plist
    /// under
    /// `com.apple.developer.nfc.readersession.felica.systemcodes`.
    public List<String> getFelicaSystemCodes() {
        return felicaSystemCodes;
    }

    /// Sets the FeliCa system codes the iOS reader session looks for.
    /// Ignored on Android.
    public NfcReadOptions setFelicaSystemCodes(String... codes) {
        if (codes == null || codes.length == 0) {
            felicaSystemCodes = Collections.emptyList();
            return this;
        }
        felicaSystemCodes = Collections.unmodifiableList(
                new ArrayList<String>(Arrays.asList(codes)));
        return this;
    }

    /// ISO 7816 AIDs the iOS reader session auto-SELECTs on connection.
    /// Each AID must be a 5-16 byte byte array. Honoured only on iOS where
    /// the AIDs must also appear in
    /// `com.apple.developer.nfc.readersession.iso7816.select-identifiers`.
    public List<byte[]> getIsoSelectAids() {
        return isoSelectAids;
    }

    /// Sets the ISO 7816 AIDs the iOS reader session auto-SELECTs on
    /// connection. The bytes are defensively copied. Ignored on Android.
    public NfcReadOptions setIsoSelectAids(byte[]... aids) {
        if (aids == null || aids.length == 0) {
            isoSelectAids = Collections.emptyList();
            return this;
        }
        List<byte[]> copies = new ArrayList<byte[]>(aids.length);
        for (byte[] src : aids) {
            if (src == null) {
                continue;
            }
            byte[] dup = new byte[src.length];
            System.arraycopy(src, 0, dup, 0, src.length);
            copies.add(dup);
        }
        isoSelectAids = Collections.unmodifiableList(copies);
        return this;
    }
}
