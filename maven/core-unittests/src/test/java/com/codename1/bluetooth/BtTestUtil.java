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
package com.codename1.bluetooth;

import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared assertions for the Bluetooth suites. {@code errorOf} mirrors the
 * pattern of {@code NfcTest}: an {@code except} callback registered off the
 * EDT on an already-failed resource fires synchronously, so no waiting is
 * involved.
 */
final class BtTestUtil {

    private BtTestUtil() {
    }

    static Throwable errorOf(AsyncResource<?> r) {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        r.except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                err.set(t);
            }
        });
        return err.get();
    }

    /**
     * Asserts the resource already failed with a {@link BluetoothException}
     * carrying the expected error code, and returns the exception.
     */
    static BluetoothException assertFailedWith(AsyncResource<?> r,
            BluetoothError expected) {
        assertTrue(r.isDone(), "resource should be done");
        Throwable t = errorOf(r);
        assertNotNull(t, "resource should carry an error");
        assertTrue(t instanceof BluetoothException,
                "expected BluetoothException but was " + t);
        BluetoothException be = (BluetoothException) t;
        assertEquals(expected, be.getError());
        return be;
    }

    static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }
}
