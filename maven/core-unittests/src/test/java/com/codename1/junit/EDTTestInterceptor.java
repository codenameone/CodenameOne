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
package com.codename1.junit;

import com.codename1.ui.CN;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

public class EDTTestInterceptor  implements InvocationInterceptor {

    private static final long DEFAULT_TIMEOUT_MILLIS = 5000;

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> ctx,
                                    ExtensionContext ext) throws Throwable {
        CN.callSeriallyAndWait(() -> pretest(ctx.getExecutable().getName()));
        runOnMyThread(invocation);
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation,
                                          ReflectiveInvocationContext<Method> ctx,
                                          ExtensionContext ext) throws Throwable {
        beforePretest();
        runOnMyThread(invocation);
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation,
                                         ReflectiveInvocationContext<Method> ctx,
                                         ExtensionContext ext) throws Throwable {
        runOnMyThread(invocation);
    }

    private void runOnMyThread(Invocation<Void> invocation) throws Throwable {
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        final Object lock = new Object();
        final boolean[] completed = new boolean[1];

        CN.callSerially(() -> {
            try {
                invocation.proceed();
            } catch (Throwable t) {
                thrown.set(t);
            }
            synchronized (lock) {
                completed[0] = true;
                lock.notifyAll();
            }
        });

        // A deadline loop, not a single wait. Object.wait may return before the
        // timeout for reasons of its own, and the previous `if` treated any such
        // return as expiry -- reporting "timed out after 5000ms" without having
        // waited 5000ms, from a run that was about to succeed.
        try {
            long deadline = System.currentTimeMillis() + DEFAULT_TIMEOUT_MILLIS;
            synchronized (lock) {
                while (!completed[0]) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        break;
                    }
                    lock.wait(remaining);
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw ie;
        }

        if (!completed[0]) {
            throw new AssertionError("FormTest timed out after " + DEFAULT_TIMEOUT_MILLIS
                    + "ms; edt=" + edtState());
        }

        Throwable t = thrown.get();
        if (t != null) throw t; // preserves the original stack trace
    }

    protected void beforePretest() {}

    protected void pretest(String testName) {
    }

    /// Whether the dispatch thread was even running when the wait expired. A
    /// timeout means one of two very different things -- the work was slow, or it
    /// was never going to run -- and the message could not tell them apart.
    private static String edtState() {
        try {
            if (!com.codename1.ui.Display.isInitialized()) {
                return "display-not-initialized";
            }
            // Serial calls queued but undrained is the signature of a dispatch that
            // was discarded rather than one that ran slowly.
            java.lang.reflect.Field f =
                    com.codename1.ui.Display.class.getDeclaredField("pendingSerialCalls");
            f.setAccessible(true);
            Object pending = f.get(com.codename1.ui.Display.getInstance());
            int queued = pending instanceof java.util.List ? ((java.util.List<?>) pending).size() : -1;
            return "initialized pendingSerialCalls=" + queued;
        } catch (Throwable unavailable) {
            return "unavailable";
        }
    }
}