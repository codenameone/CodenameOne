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
package com.codename1.ui;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.test.helpers.DisplayContext;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reporting an exception that escaped the event dispatch loop must never throw a
 * second one.
 *
 * <p>The reporting path calls four things this class does not own -- a registered
 * {@code CrashReporter}, the implementation's {@code handleEDTException}, the
 * application's error handler, and {@code Dialog.show}, which paints and so can fail
 * for any reason painting can. When one of them threw, the throwable propagated out
 * of the catch block, out of the dispatch loop and off the end of the thread. The
 * dispatch thread ended; the process stayed alive because every other thread did;
 * and the application never painted or handled input again.</p>
 *
 * <p>That is not a theoretical ordering. It is how the Linux port's screenshot suite
 * hangs: one exception on the dispatch thread, the error handler throws while
 * reporting it, and the run sits idle until a 40-minute cap kills it with 13 of 100
 * screenshots never taken. For an application it is a permanent freeze.</p>
 *
 * <p>One case per collaborator rather than one representative case, because the
 * hazard is per call site: wrapping three of the four leaves the fourth able to end
 * the thread, and it would look fixed.</p>
 */
class EdtExceptionReportingTest {

    /** The exception that escaped the loop, which reporting is trying to report. */
    private static final Throwable ORIGINAL = new IllegalStateException("original EDT failure");

    private static void report(Display display, Throwable err) throws Exception {
        Method m = Display.class.getDeclaredMethod("reportEdtException", Throwable.class);
        m.setAccessible(true);
        try {
            m.invoke(display, err);
        } catch (InvocationTargetException ite) {
            // Unwrap so the assertion message names the real throwable rather than
            // the reflection wrapper.
            Throwable cause = ite.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private static void set(Display display, String field, Object value) throws Exception {
        Field f = Display.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(display, value);
    }

    /** An error handler whose single listener throws, as an application's can. */
    private static EventDispatcher throwingErrorHandler() {
        EventDispatcher d = new EventDispatcher();
        d.addListener((ActionListener) evt -> {
            throw new IllegalStateException("error handler threw while reporting");
        });
        return d;
    }

    @Test
    void aThrowingErrorHandlerDoesNotEndTheDispatchThread() throws Exception {
        DisplayContext ctx = new DisplayContext();
        Display display = ctx.makeDisplay();
        when(ctx.getImpl().handleEDTException(any(Throwable.class))).thenReturn(false);
        set(display, "errorHandler", throwingErrorHandler());

        assertDoesNotThrow(() -> report(display, ORIGINAL),
                "an error handler that throws must not propagate out of the dispatch "
                + "loop's catch block -- that ends the dispatch thread and hangs the app");
    }

    @Test
    void aThrowingImplementationHandlerDoesNotEndTheDispatchThread() throws Exception {
        DisplayContext ctx = new DisplayContext();
        CodenameOneImplementation impl = mock(CodenameOneImplementation.class);
        when(impl.handleEDTException(any(Throwable.class)))
                .thenThrow(new IllegalStateException("port handler threw while reporting"));
        ctx.setImpl(impl);
        Display display = ctx.makeDisplay();

        assertDoesNotThrow(() -> report(display, ORIGINAL),
                "a port whose handleEDTException throws must not end the dispatch thread");
    }

    @Test
    void aThrowingCrashReporterDoesNotEndTheDispatchThread() throws Exception {
        DisplayContext ctx = new DisplayContext();
        Display display = ctx.makeDisplay();
        when(ctx.getImpl().handleEDTException(any(Throwable.class))).thenReturn(true);
        set(display, "crashReporter", (com.codename1.system.CrashReport) (t) -> {
            throw new IllegalStateException("crash reporter threw while reporting");
        });

        assertDoesNotThrow(() -> report(display, ORIGINAL),
                "a registered CrashReporter that throws must not end the dispatch thread");
    }

    @Test
    void theDefaultDialogPathDoesNotEndTheDispatchThread() throws Exception {
        // No error handler registered, so reporting falls through to Dialog.show,
        // which paints -- and painting in this fixture has no form, no graphics and a
        // mocked implementation, so it fails. That is the point: the fallback is the
        // one path an application gets without opting into anything.
        DisplayContext ctx = new DisplayContext();
        Display display = ctx.makeDisplay();
        when(ctx.getImpl().handleEDTException(any(Throwable.class))).thenReturn(false);
        set(display, "errorHandler", null);

        assertDoesNotThrow(() -> report(display, ORIGINAL),
                "the default Dialog.show reporting path must not end the dispatch thread");
    }
}
