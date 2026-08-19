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
package com.codename1.home;

import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Waiting on, and inspecting, a smart-home operation.
 *
 * <p>Two things here that a test would otherwise reinvent per class.</p>
 *
 * <p><b>Settling.</b> Results are delivered on the EDT on every backend, and
 * the local bridge answers after a deliberate few-millisecond delay rather
 * than inline -- see {@code LocalHomeBridge}. So {@code isDone()} immediately
 * after a call is false, and that is the contract working rather than a
 * hang.</p>
 *
 * <p><b>Reading a failure.</b> {@code AsyncResource} has no {@code getError()}:
 * the way to see the failure is to register a callback and look at what it
 * captured, which is the same idiom {@code HealthAwait} and {@code BtTestUtil}
 * use. That works here because {@code except} is deliberately left
 * synchronous -- {@code EdtResult} marshals {@code ready} and {@code onResult}
 * but not {@code except}, precisely so introspecting a failure that has
 * already happened stays a plain read.</p>
 */
final class HomeAwait {

    private static final long LIMIT_MILLIS = 10000L;

    private HomeAwait() {
    }

    /** Blocks until the operation settles, and returns it for chaining. */
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

    /** The failure a settled operation carries, or null when it succeeded. */
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

    /** Asserts the operation failed for a particular reason. */
    static void assertFailedWith(HomeError expected,
            AsyncResource<?> resource) {
        Throwable error = errorOf(resource);
        assertNotNull(error, "this operation was expected to fail with "
                + expected.name() + " and it succeeded");
        assertTrue(error instanceof HomeException,
                "a smart-home failure has to be a HomeException so callers"
                        + " can branch on a typed reason rather than parsing a"
                        + " message; got " + error.getClass().getName());
        assertSame(expected, ((HomeException) error).getError());
    }

    /** Asserts the operation succeeded, naming the failure when it did not. */
    static <T> T value(AsyncResource<T> resource) {
        Throwable error = errorOf(resource);
        if (error != null) {
            throw new AssertionError("the operation failed: " + error);
        }
        return resource.get();
    }
}
