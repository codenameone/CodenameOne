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

import com.codename1.util.AsyncResource;

/// ISO 14443-4 / ISO 7816-4 technology view: send APDU command-response
/// pairs to a contactless smart card (EMV payment, ePassport, government
/// ID, transit). Backed by `IsoDep` on Android and by `NFCISO7816Tag` on
/// iOS.
///
/// A typical SELECT-then-READ exchange:
///
/// ```java
/// IsoDep iso = tag.getIsoDep();
/// if (iso == null) {
///     // tag is not ISO-DEP capable
///     return;
/// }
/// byte[] selectAid = new byte[] {
///     0x00, (byte) 0xA4, 0x04, 0x00, 0x07,
///     (byte) 0xA0, 0x00, 0x00, 0x00, 0x04, 0x10, 0x10
/// };
/// iso.transceive(selectAid).onResult((sw, err) -> {
///     // last two bytes of sw are the ISO 7816 SW1/SW2 status word
/// });
/// ```
///
/// All response bytes (including the terminating SW1/SW2 status word) are
/// returned verbatim. Use [#isSuccess(byte[])] to test for the canonical
/// `90 00` success word without slicing.
public class IsoDep extends TagTechnology {

    /// Historical bytes returned during ISO-DEP activation (Android
    /// `IsoDep.getHistoricalBytes()`). Empty when the platform does not
    /// surface them.
    public byte[] getHistoricalBytes() {
        return new byte[0];
    }

    /// Largest single transceive payload the underlying transport accepts.
    /// Some Android implementations top out at 253 bytes for short APDU
    /// frames; Core NFC fragments at 256. Use as an upper bound when
    /// chunking large `READ BINARY` exchanges.
    public int getMaxTransceiveLength() {
        return 256;
    }

    /// `true` when this view exchanges extended-length APDUs (Lc / Le up to
    /// 65535). Most Android devices report `true`; iOS Core NFC reports
    /// `false`.
    public boolean isExtendedLengthSupported() {
        return false;
    }

    @Override
    public final TagType getType() {
        return TagType.ISO_DEP;
    }

    /// Returns `true` when the last two bytes of `response` are the ISO
    /// 7816 success status word (`90 00`). Useful as a quick check after
    /// [#transceive(byte[])].
    public static boolean isSuccess(byte[] response) {
        if (response == null || response.length < 2) {
            return false;
        }
        return response[response.length - 2] == (byte) 0x90
                && response[response.length - 1] == (byte) 0x00;
    }

    @Override
    public AsyncResource<byte[]> transceive(byte[] apdu) {
        AsyncResource<byte[]> r = new AsyncResource<byte[]>();
        r.error(new NfcException(NfcError.UNSUPPORTED_TAG,
                "ISO-DEP transceive not implemented on this port"));
        return r;
    }
}
