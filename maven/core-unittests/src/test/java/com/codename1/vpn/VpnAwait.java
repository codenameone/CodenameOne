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
package com.codename1.vpn;

import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Waiting on, and inspecting, a VPN operation. See {@code CallAwait}. */
final class VpnAwait {

    private static final long LIMIT_MILLIS = 10000L;

    private VpnAwait() {
    }

    static <T> AsyncResource<T> settled(AsyncResource<T> resource) {
        long limit = System.currentTimeMillis() + LIMIT_MILLIS;
        while (!resource.isDone() && System.currentTimeMillis() < limit) {
            try {
                Thread.sleep(2);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertTrue(resource.isDone(),
                "the operation must settle rather than hang");
        return resource;
    }

    static <T> Throwable errorOf(AsyncResource<T> resource) {
        settled(resource);
        if (resource.isReady()) {
            return null;
        }
        final AtomicReference<Throwable> captured =
                new AtomicReference<Throwable>();
        resource.except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable value) {
                captured.set(value);
            }
        });
        return captured.get();
    }

    static void assertFailedWith(VpnError expected, AsyncResource<?> resource) {
        Throwable error = errorOf(resource);
        assertNotNull(error, "this operation was expected to fail with "
                + expected.name() + " and it succeeded");
        assertTrue(error instanceof VpnException,
                "a VPN failure has to be a VpnException so callers can branch"
                        + " on a typed reason; got " + error.getClass().getName());
        assertSame(expected, ((VpnException) error).getError());
    }

    static <T> T value(AsyncResource<T> resource) {
        Throwable error = errorOf(resource);
        if (error != null) {
            throw new AssertionError("the operation failed: " + error);
        }
        return resource.get();
    }
}
