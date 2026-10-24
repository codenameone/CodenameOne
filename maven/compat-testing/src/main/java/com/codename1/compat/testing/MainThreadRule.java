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
package com.codename1.compat.testing;

import com.codename1.ui.Display;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/// Runs each test on the UI thread every compatibility layer maps onto --
/// Android's main thread, Swing's event dispatch thread, the JavaFX
/// application thread -- which is Codename One's EDT.
///
/// Until a display exists every thread counts as the main one, so a test
/// that never starts one passes on the JUnit thread. Tests that run
/// activities start the EDT for the whole JVM, though, and from then on a
/// LiveData `setValue` on the JUnit thread is a background-thread call --
/// which made such tests pass or fail with the order the classes ran in.
public final class MainThreadRule implements TestRule {

    /// Lets main-thread work posted so far run (LiveData.postValue, Handler
    /// posts). Without a display it already ran inline. Called from the EDT,
    /// so it blocks off it while a no-op round trip goes through the queue.
    public static void drain() {
        if (!Display.isInitialized()) {
            return;
        }
        final Runnable roundTrip = new Runnable() {
            @Override
            public void run() {
                Display.getInstance().callSeriallyAndWait(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        };
        if (Display.getInstance().isEdt()) {
            Display.getInstance().invokeAndBlock(roundTrip);
        } else {
            roundTrip.run();
        }
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (!Display.isInitialized() || Display.getInstance().isEdt()) {
                    base.evaluate();
                    return;
                }
                final Throwable[] failure = new Throwable[1];
                Display.getInstance().callSeriallyAndWait(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            base.evaluate();
                        } catch (Throwable t) {
                            failure[0] = t;
                        }
                    }
                });
                if (failure[0] != null) {
                    throw failure[0];
                }
            }
        };
    }
}
