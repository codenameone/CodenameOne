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
import com.codename1.ui.Desktop;
import com.codename1.ui.Display;
import com.codename1.ui.Window;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.WindowEvent;
import com.codename1.ui.layouts.BorderLayout;

import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Guards the JavaSE port against reporting its own show and hide back to the framework
 * as a minimize and a restore.
 *
 * <p>AWT delivers componentShown / componentHidden for every visibility change, however
 * it was caused, and the port turns those into windowShowNotify / windowHideNotify.
 * Those are <em>queued</em> onto the Codename One event dispatch thread, so a show and a
 * hide performed in the same turn both run afterwards, against the state the second one
 * left. The pair reads as a minimize followed by a restore, and in the show-then-hide
 * order the window ends up hidden while still marked iconified -- the state
 * {@code showModal()} waits on.</p>
 *
 * @author Shai Almog
 */
@CodenameOneTest
class WindowVisibilityEventCorrelationTest {

    @Test
    void anExplicitShowAndHideIsNotReportedAsMinimizeAndRestore() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        assumeTrue(Desktop.isSupported(), "needs a windowing system");

        final List<String> lifecycle = new ArrayList<String>();
        Window w = new Window("visibility", new BorderLayout());
        w.setWindowSize(320, 240);
        w.addWindowListener(new ActionListener<WindowEvent>() {
            @Override
            public void actionPerformed(WindowEvent evt) {
                lifecycle.add(String.valueOf(evt.getType()));
            }
        });
        try {
            w.show();
            w.hide();
            // Drain twice: the AWT callbacks queue onto this thread, so the reports
            // arrive after the two explicit calls have already finished.
            drain();
            drain();

            assertNotNull(lifecycle);
            for (int iter = 0; iter < lifecycle.size(); iter++) {
                String type = lifecycle.get(iter);
                assertTrue(!"Minimized".equals(type) && !"Restored".equals(type),
                        "a show followed by a hide is not a minimize or a restore, but "
                                + "the window reported: " + lifecycle);
            }
            assertTrue(!w.isWindowShowing(), "and the window is hidden at the end");
        } finally {
            w.dispose();
            drain();
        }
    }

    /// Lets the event dispatch thread run whatever the AWT callbacks queued onto it.
    private static void drain() throws Exception {
        final Object done = new Object();
        synchronized (done) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    synchronized (done) {
                        done.notifyAll();
                    }
                }
            });
            done.wait(2000);
        }
        Thread.sleep(120);
    }
}
