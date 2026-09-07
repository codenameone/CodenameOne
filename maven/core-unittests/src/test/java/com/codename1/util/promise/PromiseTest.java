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
package com.codename1.util.promise;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.util.SuccessCallback;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PromiseTest extends UITestBase {

    @FormTest
    void testPromiseResolvesAndTriggersHandlers() {
        final AtomicReference<String> value = new AtomicReference<String>();
        Promise<String> promise = new Promise<String>(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                resolve.call("done");
            }
        });

        promise.onSuccess(new SuccessCallback<String>() {
            public void onSucess(String result) {
                value.set(result);
            }
        });

        flushSerialCalls();
        assertEquals("done", value.get());
        assertEquals("done", promise.getValue());
    }

    @FormTest
    void testPromiseRejectsAndExceptHandlesError() {
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        Promise<String> promise = new Promise<String>(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                reject.call(new RuntimeException("fail"));
            }
        });

        promise.except(new Functor<Throwable, Object>() {
            public Object call(Throwable t) {
                error.set(t);
                return null;
            }
        });

        flushSerialCalls();
        assertNotNull(error.get());
        assertEquals("fail", error.get().getMessage());
    }

    @FormTest
    void testPromiseAllAggregatesResults() {
        Promise<String> first = new Promise<String>(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                resolve.call("A");
            }
        });
        Promise<String> second = new Promise<String>(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                resolve.call("B");
            }
        });

        final AtomicReference<Object[]> results = new AtomicReference<Object[]>();
        Promise.all(first, second).onSuccess(new SuccessCallback<Object>() {
            public void onSucess(Object res) {
                results.set((Object[]) res);
            }
        });

        flushSerialCalls();
        assertNotNull(results.get());
        assertArrayEquals(new Object[]{"A", "B"}, results.get());
    }

    @FormTest
    void testPromiseAlwaysInvoked() {
        final AtomicBoolean alwaysCalled = new AtomicBoolean(false);
        Promise<String> promise = new Promise<String>(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                resolve.call("value");
            }
        });

        promise.always(new Functor<Object, Object>() {
            public Object call(Object value) {
                alwaysCalled.set(true);
                return null;
            }
        });

        flushSerialCalls();
        assertTrue(alwaysCalled.get());
    }

    /// `Promise.reject` used to call itself instead of the reject functor -- the
    /// parameter and the method share the name `reject`, and `reject(err)` is a
    /// method invocation -- so every call recursed until the stack overflowed.
    /// Nothing covered the static factories, so it shipped.
    @FormTest
    void testStaticRejectProducesARejectedPromise() {
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        Throwable cause = new IllegalStateException("nope");

        Promise.reject(cause).except(new Functor<Throwable, Object>() {
            public Object call(Throwable t) {
                error.set(t);
                return null;
            }
        });

        flushSerialCalls();
        assertSame(cause, error.get());
    }

    @FormTest
    void testStaticResolveProducesAFulfilledPromise() {
        final AtomicReference<String> value = new AtomicReference<String>();

        Promise.resolve("ready").onSuccess(new SuccessCallback<String>() {
            public void onSucess(String result) {
                value.set(result);
            }
        });

        flushSerialCalls();
        assertEquals("ready", value.get());
    }

    /// A link in the chain with no rejection handler must pass the ORIGINAL error
    /// down. The old default rethrew it as `(RuntimeException) o`, so a checked
    /// exception arrived at the far end as a ClassCastException with the real
    /// cause gone.
    @FormTest
    void testCheckedExceptionSurvivesAThenWithNoRejectionHandler() {
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final Throwable cause = new java.io.IOException("disk");

        Promise<String> promise = new Promise<String>(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                reject.call(cause);
            }
        });

        promise.then(new Functor<String, Object>() {
            public Object call(String s) {
                return s;
            }
        }).except(new Functor<Throwable, Object>() {
            public Object call(Throwable t) {
                error.set(t);
                return null;
            }
        });

        flushSerialCalls();
        assertSame(cause, error.get(),
                "a then() without a rejection handler must forward the original error");
    }

    /// The unchecked case took the same broken path; it only happened to survive
    /// it, so it is worth pinning alongside the checked one.
    @FormTest
    void testRuntimeExceptionSurvivesAThenWithNoRejectionHandler() {
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final Throwable cause = new IllegalArgumentException("bad");

        Promise<String> promise = new Promise<String>(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                reject.call(cause);
            }
        });

        promise.then(new Functor<String, Object>() {
            public Object call(String s) {
                return s;
            }
        }).except(new Functor<Throwable, Object>() {
            public Object call(Throwable t) {
                error.set(t);
                return null;
            }
        });

        flushSerialCalls();
        assertSame(cause, error.get());
    }

    /// The pass-through must not swallow the rejection either: a handler attached
    /// several links down still has to run.
    @FormTest
    void testRejectionTravelsThroughSeveralHandlerlessLinks() {
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final AtomicBoolean successRan = new AtomicBoolean(false);
        final Throwable cause = new java.io.IOException("deep");

        Promise<String> promise = new Promise<String>(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                reject.call(cause);
            }
        });

        Functor<String, Object> onValue = new Functor<String, Object>() {
            public Object call(String s) {
                successRan.set(true);
                return s;
            }
        };

        promise.then(onValue).then(onValue).then(onValue)
                .except(new Functor<Throwable, Object>() {
                    public Object call(Throwable t) {
                        error.set(t);
                        return null;
                    }
                });

        flushSerialCalls();
        assertSame(cause, error.get());
        assertFalse(successRan.get(), "a rejected promise must not run the success path");
    }
}
