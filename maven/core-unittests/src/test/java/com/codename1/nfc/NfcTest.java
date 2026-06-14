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

import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the {@link Nfc} fallback base class returned on platforms
 * without NFC (the test implementation reports no native NFC). Every
 * capability query is {@code false} and every operation fails immediately with
 * {@link NfcError#NOT_AVAILABLE}; the listener / HCE registration calls are
 * silent no-ops.
 */
class NfcTest extends UITestBase {

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
    void getInstanceReturnsTheStableFallbackSingleton() {
        Nfc a = Nfc.getInstance();
        Nfc b = Nfc.getInstance();
        assertNotNull(a);
        assertSame(a, b);
    }

    @Test
    void fallbackReportsEverythingUnsupported() {
        Nfc nfc = Nfc.getInstance();
        assertFalse(nfc.isSupported());
        assertFalse(nfc.canRead());
        assertFalse(nfc.canWrite());
        assertFalse(nfc.canHostEmulate());
        assertFalse(nfc.stopRead());
    }

    @Test
    void readTagFailsNotAvailable() {
        Throwable t = errorOf(Nfc.getInstance().readTag(new NfcReadOptions()));
        assertTrue(t instanceof NfcException);
        assertEquals(NfcError.NOT_AVAILABLE, ((NfcException) t).getError());
    }

    @Test
    void readNdefPropagatesNotAvailable() {
        Throwable t = errorOf(Nfc.getInstance().readNdef(new NfcReadOptions()));
        assertTrue(t instanceof NfcException);
        assertEquals(NfcError.NOT_AVAILABLE, ((NfcException) t).getError());
    }

    @Test
    void writeNdefPropagatesNotAvailable() {
        Throwable t = errorOf(Nfc.getInstance().writeNdef(new NfcReadOptions(), null));
        assertTrue(t instanceof NfcException);
        assertEquals(NfcError.NOT_AVAILABLE, ((NfcException) t).getError());
    }

    @Test
    void listenerAndHceRegistrationAreNoOps() {
        Nfc nfc = Nfc.getInstance();
        // None of these should throw on the fallback base class.
        nfc.addTagListener(null);
        nfc.removeTagListener(null);
        nfc.registerHostCardEmulationService(null);
        nfc.unregisterHostCardEmulationService();
    }
}
