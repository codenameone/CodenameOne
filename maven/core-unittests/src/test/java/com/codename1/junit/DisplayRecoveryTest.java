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

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.Display;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test class must be able to start against a Display the previous class's
 * dispatch thread has already half torn down.
 *
 * <p>{@code Display.isInitialized()} is {@code codenameOneRunning &&
 * impl.isInitialized()}, and the two halves come apart. A departing dispatch
 * thread clears the flag on the IMPLEMENTATION, whenever it is next scheduled,
 * and leaves {@code codenameOneRunning} set. {@code Display.init()} guards on
 * {@code codenameOneRunning}, so it returns having done nothing, and every test
 * in the next class dispatches onto a thread that is gone and reports
 * {@code FormTest timed out after 5000ms; edt=display-not-initialized}. That is
 * how ValidatorTest failed twelve times on a loaded CI runner while the same
 * 5528 tests passed locally -- the window is only open while the machine is
 * descheduling the old thread.</p>
 *
 * <p>Deliberately a plain {@code @Test} and not a {@code @FormTest}: nothing
 * here may be dispatched onto the dispatch thread, because the whole subject is
 * what happens when that thread is missing. A recovery placed in a
 * {@code @BeforeEach} has the same problem and was tried first -- the
 * interceptor dispatches {@code @BeforeEach} too, so the repair queues behind
 * the failure it exists to repair.</p>
 */
class DisplayRecoveryTest {

    @Test
    void theInterceptorRevivesAHalfTornDownDisplay() throws Exception {
        FormTestInterceptor interceptor = new FormTestInterceptor();
        // Start from a Display this harness would consider healthy.
        interceptor.beforePretest();
        assertTrue(Display.isInitialized(), "the fixture starts from a live Display");

        // Exactly what a departing dispatch thread does, and only that: the flag
        // on the implementation, with codenameOneRunning left alone.
        liveImplementation().deinitialize();
        assertFalse(Display.isInitialized(), "the split state CI reported");

        interceptor.beforePretest();

        assertTrue(Display.isInitialized(),
                "beforePretest must clear codenameOneRunning so its init is real");
    }

    /// The window beforePretest cannot close on its own.
    ///
    /// That hook runs once per test. The teardown belongs to another thread and
    /// lands whenever that thread is next scheduled, so it can arrive after the
    /// check and before the dispatch -- which is why recovering only there took
    /// ValidatorTest from twelve failures to one rather than to none. The
    /// interceptor now repairs immediately before dispatching, and this asserts
    /// that second call is the one that answers when the Display goes down after
    /// the first.
    @Test
    void aDisplayLostAfterBeforePretestIsStillRecovered() throws Exception {
        FormTestInterceptor interceptor = new FormTestInterceptor();
        interceptor.beforePretest();
        assertTrue(Display.isInitialized(), "the fixture starts from a live Display");

        // Exactly the ordering that survived the first fix: the teardown lands
        // after the per-test hook has already looked and been satisfied.
        liveImplementation().deinitialize();
        assertFalse(Display.isInitialized(), "and is taken down behind its back");

        interceptor.ensureDisplayAlive();

        assertTrue(Display.isInitialized(),
                "the pre-dispatch repair has to answer when the per-test one already ran");
    }

    /// The live implementation. Display.getImplementation() is package private to
    /// com.codename1.ui, and this field is the only handle on the object the
    /// departing thread would have called deinitialize() on.
    private static CodenameOneImplementation liveImplementation() throws Exception {
        Field f = Display.class.getDeclaredField("impl");
        f.setAccessible(true);
        return (CodenameOneImplementation) f.get(null);
    }
}
