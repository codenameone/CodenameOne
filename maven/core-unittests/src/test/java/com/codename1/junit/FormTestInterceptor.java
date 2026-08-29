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

import com.codename1.impl.ImplementationFactory;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

public class FormTestInterceptor extends EDTTestInterceptor {
    @Override
    protected void beforePretest() {
        ImplementationFactory.setInstance(new ImplementationFactory() {
            @Override
            public Object createImplementation() {
                return new TestCodenameOneImplementation();
            }
        });
        ensureDisplayAlive();
    }

    /// Brings the Display back if the previous class's dispatch thread has taken
    /// it down, and does nothing at all otherwise.
    ///
    /// Called from beforePretest AND from immediately before every dispatch, and
    /// the second one is what makes it reliable. The teardown belongs to a
    /// thread this one does not control, so checking once per test still leaves
    /// the window between that check and the dispatch -- narrow enough that it
    /// took twelve failures down to one rather than to none.
    @Override
    protected void ensureDisplayAlive() {
        if (!Display.isInitialized()) {
            // The two halves of isInitialized() come apart, and only one of them
            // is recoverable by the init() below. It answers
            // codenameOneRunning && impl.isInitialized(); the previous class's
            // dispatch thread clears the flag on the IMPLEMENTATION on its way
            // out, whenever it is next scheduled, and leaves codenameOneRunning
            // set. init() guards on codenameOneRunning, so it returns having
            // done nothing, and every test in the class then dispatches onto a
            // thread that is gone and reports "FormTest timed out after 5000ms;
            // edt=display-not-initialized". That is how ValidatorTest failed
            // twelve times on a loaded CI runner while the same 5528 tests
            // passed locally.
            //
            // Clearing codenameOneRunning first is what makes the init real. It
            // costs nothing in the ordinary case, where isInitialized() is
            // already true and this branch is not taken at all.
            //
            // Here rather than in a @BeforeEach, which is where it was tried
            // first and cannot work: EDTTestInterceptor dispatches @BeforeEach
            // onto the very dispatch thread that is gone, so the recovery would
            // be queued behind the thing it exists to repair. beforePretest()
            // runs on the test thread, before any dispatch.
            Display.deinitialize();
        }
        Display.init(null);
    }

    @Override
    protected void pretest(String testName) {
        Form form = new Form(testName);
        form.show();
    }
}