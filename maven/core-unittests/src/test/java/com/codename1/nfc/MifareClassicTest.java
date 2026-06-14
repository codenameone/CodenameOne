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
package com.codename1.nfc;

import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the {@link MifareClassic} base technology view: the well-known
 * key accessors (and their defensive copying), the sector-to-block arithmetic
 * across the 1K/4K boundary, the type tag, and the not-implemented base-class
 * operations that fail with {@link NfcError#UNSUPPORTED_TAG}.
 */
class MifareClassicTest {

    private static Throwable errorOf(AsyncResource<?> r) {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        r.except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                err.set(t);
            }
        });
        return err.get();
    }

    @Test
    void wellKnownKeysHaveExpectedBytes() {
        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, MifareClassic.keyDefault());
        assertArrayEquals(new byte[]{(byte) 0xA0, (byte) 0xA1, (byte) 0xA2,
                (byte) 0xA3, (byte) 0xA4, (byte) 0xA5}, MifareClassic.keyMifareApplicationDirectory());
        assertArrayEquals(new byte[]{(byte) 0xD3, (byte) 0xF7, (byte) 0xD3,
                (byte) 0xF7, (byte) 0xD3, (byte) 0xF7}, MifareClassic.keyNfcForum());
    }

    @Test
    void keysAreFreshCopiesEachCall() {
        byte[] k = MifareClassic.keyDefault();
        k[0] = 0;
        assertEquals((byte) 0xFF, MifareClassic.keyDefault()[0]);
    }

    @Test
    void typeIsMifareClassic() {
        assertEquals(TagType.MIFARE_CLASSIC, new MifareClassic().getType());
    }

    @Test
    void baseCountsAreZero() {
        MifareClassic m = new MifareClassic();
        assertEquals(0, m.getSectorCount());
        assertEquals(0, m.getBlockCount());
    }

    @Test
    void sectorToBlockUsesFourBlockSectorsUntil32() {
        MifareClassic m = new MifareClassic();
        assertEquals(0, m.sectorToBlock(0));
        assertEquals(4, m.sectorToBlock(1));
        assertEquals(124, m.sectorToBlock(31));
    }

    @Test
    void sectorToBlockSwitchesToSixteenBlockSectorsAt32() {
        MifareClassic m = new MifareClassic();
        // First 32 sectors cover 128 blocks; sector 32 starts there.
        assertEquals(128, m.sectorToBlock(32));
        assertEquals(144, m.sectorToBlock(33));
    }

    @Test
    void readBlockFailsUnsupportedByDefault() {
        Throwable t = errorOf(new MifareClassic().readBlock(0));
        assertTrue(t instanceof NfcException);
        assertEquals(NfcError.UNSUPPORTED_TAG, ((NfcException) t).getError());
    }

    @Test
    void writeBlockFailsUnsupportedByDefault() {
        Throwable t = errorOf(new MifareClassic().writeBlock(0, new byte[16]));
        assertTrue(t instanceof NfcException);
        assertEquals(NfcError.UNSUPPORTED_TAG, ((NfcException) t).getError());
    }

    @Test
    void authenticateFailsUnsupportedByDefault() {
        MifareClassic m = new MifareClassic();
        Throwable a = errorOf(m.authenticateSectorWithKeyA(0, MifareClassic.keyDefault()));
        Throwable b = errorOf(m.authenticateSectorWithKeyB(0, MifareClassic.keyDefault()));
        assertEquals(NfcError.UNSUPPORTED_TAG, ((NfcException) a).getError());
        assertEquals(NfcError.UNSUPPORTED_TAG, ((NfcException) b).getError());
    }
}
