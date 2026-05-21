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

/// NXP MIFARE Classic 1K/4K technology view. Block-level read and write
/// with key A or key B authentication.
///
/// **Android-only** -- iOS Core NFC intentionally rejects MIFARE Classic.
/// On iOS, [Tag#getMifareClassic()] returns `null` for the same physical
/// tag and the caller should fall back to [Tag#getNfcA()] or fail
/// gracefully.
///
/// The default factory keys are widely published; use them only on
/// untransitioned demo / blank cards.
public class MifareClassic extends TagTechnology {

    /// Default MIFARE Classic key A used by NXP shipping cards.
    public static final byte[] KEY_DEFAULT = new byte[] {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
    };

    /// MIFARE Application Directory (MAD) key A from NXP AN10787.
    public static final byte[] KEY_MIFARE_APPLICATION_DIRECTORY = new byte[] {
            (byte) 0xA0, (byte) 0xA1, (byte) 0xA2,
            (byte) 0xA3, (byte) 0xA4, (byte) 0xA5
    };

    /// NFC Forum key A for NDEF-formatted MIFARE Classic blocks.
    public static final byte[] KEY_NFC_FORUM = new byte[] {
            (byte) 0xD3, (byte) 0xF7, (byte) 0xD3,
            (byte) 0xF7, (byte) 0xD3, (byte) 0xF7
    };

    /// Total sectors on the tag (16 on Classic 1K, 40 on Classic 4K).
    public int getSectorCount() {
        return 0;
    }

    /// Total addressable blocks (each 16 bytes).
    public int getBlockCount() {
        return 0;
    }

    /// First block index inside the given sector. Sectors 0-31 contain 4
    /// blocks each; sectors 32-39 (4K cards only) contain 16 blocks.
    public int sectorToBlock(int sectorIndex) {
        if (sectorIndex < 32) {
            return sectorIndex * 4;
        }
        return 32 * 4 + (sectorIndex - 32) * 16;
    }

    /// Authenticates a sector with the given key A. Required before any
    /// read/write on the sector. Fails with [NfcError#IO_ERROR] when the
    /// key is wrong.
    public AsyncResource<Boolean> authenticateSectorWithKeyA(int sector,
            byte[] key) {
        return notImplemented();
    }

    /// Authenticates a sector with the given key B.
    public AsyncResource<Boolean> authenticateSectorWithKeyB(int sector,
            byte[] key) {
        return notImplemented();
    }

    /// Reads a single 16-byte data block. The sector containing the block
    /// must have been authenticated first.
    public AsyncResource<byte[]> readBlock(int block) {
        AsyncResource<byte[]> r = new AsyncResource<byte[]>();
        r.error(new NfcException(NfcError.UNSUPPORTED_TAG,
                "MIFARE Classic read not implemented on this port"));
        return r;
    }

    /// Writes the 16-byte payload to the given data block. Fails with
    /// [NfcError#READ_ONLY] when access bits forbid the write.
    public AsyncResource<Boolean> writeBlock(int block, byte[] data) {
        return notImplemented();
    }

    @Override
    public final TagType getType() {
        return TagType.MIFARE_CLASSIC;
    }

    private static AsyncResource<Boolean> notImplemented() {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.error(new NfcException(NfcError.UNSUPPORTED_TAG,
                "MIFARE Classic not implemented on this port"));
        return r;
    }
}
