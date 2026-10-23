/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.security;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.io.Util;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The clipboard leftover rules: a read that answers nothing keeps the
/// record, a match clears, a mismatch forgets.
class ProtectTest {
    @Test
    void anUnreadableClipboardKeepsTheRecord() {
        assertEquals(Protect.KEEP, Protect.decide("100:abc", null));
    }

    @Test
    void aMatchClearsAndAMismatchForgets() {
        assertEquals(Protect.CLEAR, Protect.decide("100:abc", "abc"));
        assertEquals(Protect.FORGET, Protect.decide("100:abc", "xyz"));
    }

    @Test
    void nothingIsReadBeforeTheClearFallsDue() {
        assertFalse(Protect.due("5000:abc", 4999));
        assertTrue(Protect.due("5000:abc", 5000));
        assertTrue(Protect.due("abc", 0), "a record without a time is due");
    }

    @Test
    void theDigestIsKeyedAndTheKeyIsKept() {
        Util.setImplementation(new JavaSEPort());
        Keys.useStorage(new MemoryStorage());
        byte[] key = Protect.digestKey();
        assertArrayEquals(key, Protect.digestKey());
        assertEquals(Protect.digest(key, "4029"), Protect.digest(key, "4029"));
        assertNotEquals(Protect.digest(key, "4029"), Protect.digest(new byte[32], "4029"));
    }
}
