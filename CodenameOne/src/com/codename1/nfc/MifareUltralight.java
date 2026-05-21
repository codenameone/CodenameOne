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

/// NXP MIFARE Ultralight / Ultralight C / NTAG21x technology view.
/// Page-level read/write of 4-byte pages.
///
/// Supported on Android (`MifareUltralight`) and iOS 13+ (subset of
/// `NFCMiFareTag`).
public class MifareUltralight extends TagTechnology {

    /// Number of pages on this tag. 16 for Ultralight, 48 for Ultralight C,
    /// 45 for NTAG213, 135 for NTAG215, 231 for NTAG216. Returns `0` when
    /// the port has not populated this field.
    public int getPageCount() {
        return 0;
    }

    /// Reads 4 pages (16 bytes) starting at `firstPage`. Pages roll over to
    /// page 0 when the request runs past the end.
    public AsyncResource<byte[]> readPages(int firstPage) {
        AsyncResource<byte[]> r = new AsyncResource<byte[]>();
        r.error(new NfcException(NfcError.UNSUPPORTED_TAG,
                "MIFARE Ultralight read not implemented on this port"));
        return r;
    }

    /// Writes a single 4-byte page. Fails with [NfcError#READ_ONLY] for
    /// vendor / OTP / lock pages.
    public AsyncResource<Boolean> writePage(int page, byte[] data) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.error(new NfcException(NfcError.UNSUPPORTED_TAG,
                "MIFARE Ultralight write not implemented on this port"));
        return r;
    }

    @Override
    public final TagType getType() {
        return TagType.MIFARE_ULTRALIGHT;
    }
}
