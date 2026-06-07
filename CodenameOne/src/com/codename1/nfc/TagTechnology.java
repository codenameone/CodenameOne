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

/// Common surface for the low-level technology views attached to a [Tag] --
/// [IsoDep], [MifareClassic], [MifareUltralight], [NfcA], [NfcB], [NfcF],
/// [NfcV]. Application code never instantiates these directly -- they are
/// returned by accessors on [Tag].
///
/// Each technology exposes a [#transceive(byte[])] method that fires raw
/// bytes at the tag and returns the response. The exact framing depends on
/// the technology -- [IsoDep] expects ISO 7816 APDUs, [MifareClassic]
/// expects single-block commands, etc. Always defer to the technology-
/// specific subclass docs.
public abstract class TagTechnology {

    /// The technology variant this view represents.
    public abstract TagType getType();

    /// Sends the given raw bytes to the tag and resolves with the response.
    /// The base class reports [NfcError#UNSUPPORTED_TAG] -- ports override
    /// per technology.
    public AsyncResource<byte[]> transceive(byte[] payload) {
        AsyncResource<byte[]> r = new AsyncResource<byte[]>();
        r.error(new NfcException(NfcError.UNSUPPORTED_TAG,
                "transceive not supported for " + getType()));
        return r;
    }
}
