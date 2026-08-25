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
package com.codename1.impl.javase;

import com.codename1.testing.junit.CodenameOneTest;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Guards the failure report from {@link JavaSEWindowManager}'s AWT hop.
 *
 * <p>{@code runOnAwtAndWait} logs whatever the AWT task threw and returns normally,
 * which is right for the callers that only adjust an existing window. It was not right
 * for {@code createWindow}: allocating a native window peer can fail -- an exhausted
 * or headless window server is the ordinary way -- and the peer object was returned
 * anyway, with its frame and canvas still null.</p>
 *
 * <p>{@code Window.show()} checks only for null, so it would register that window,
 * publish it through {@code Desktop} and fire {@code Shown} for a window with no frame
 * and no surface behind it. Every later call into the manager would then quietly do
 * nothing against the null frame: a window that exists to the application and to
 * nobody else, failing far from the call that asked for it.</p>
 */
@CodenameOneTest
class JavaSEWindowManagerCreateFailureTest {

    @Test
    void aTaskThatThrowsIsReportedRatherThanLoggedAndForgotten() throws Exception {
        // Off the AWT thread, which is the path that swallows. The whole point of the
        // return value is that the caller can tell this apart from success.
        assumeFalse(SwingUtilities.isEventDispatchThread(),
                "this asserts the invokeAndWait path, which only exists off the AWT thread");
        final AtomicBoolean ran = new AtomicBoolean();
        boolean completed = JavaSEWindowManager.runOnAwtAndWait(new Runnable() {
            @Override
            public void run() {
                ran.set(true);
                throw new IllegalStateException("no native peer available");
            }
        });
        assertTrue(ran.get(), "the task must have been attempted");
        assertFalse(completed,
                "a task that threw did not complete, and saying otherwise is what let "
                        + "createWindow return a peer with no window behind it");
    }

    @Test
    void aTaskThatCompletesIsReportedAsSuccess() throws Exception {
        assumeFalse(SwingUtilities.isEventDispatchThread(),
                "this asserts the invokeAndWait path, which only exists off the AWT thread");
        final AtomicBoolean ran = new AtomicBoolean();
        boolean completed = JavaSEWindowManager.runOnAwtAndWait(new Runnable() {
            @Override
            public void run() {
                ran.set(true);
            }
        });
        assertTrue(ran.get());
        assertTrue(completed, "an ordinary task must not be reported as a failure");
    }
}
